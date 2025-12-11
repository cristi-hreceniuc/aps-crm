package rotld.apscrm.api.v1.logopedy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

/**
 * Service for generating pre-signed URLs for private S3 assets
 */
@Service
@Slf4j
public class S3Service {

    private final S3Presigner presigner;
    private final S3Client s3Client;
    private final String bucketName;
    private final Duration urlExpiration;

    public S3Service(
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.access-key}") String accessKey,
            @Value("${aws.s3.secret-key}") String secretKey,
            @Value("${aws.s3.presigned-url-expiration-minutes:60}") int expirationMinutes
    ) {
        this.bucketName = bucketName;
        this.urlExpiration = Duration.ofMinutes(expirationMinutes);

        // Create AWS credentials
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        // Create S3 Client for checking object existence
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        // Create S3 Presigner
        this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        log.info("S3Service initialized - Bucket: {}, Region: {}, URL Expiration: {} minutes",
                bucketName, region, expirationMinutes);
    }

    /**
     * Generates a pre-signed URL for an S3 object
     * Handles UTF-8 characters (diacritics) in filenames by ensuring proper encoding.
     * 
     * @param s3Key The S3 key (e.g., "modules/1/submodules/1/lessons/1/image.jpg" or "submodules/s/că.mp3")
     * @return Pre-signed URL valid for the configured duration
     */
    public String generatePresignedUrl(String s3Key) {
        try {
            // Remove leading slash if present
            String cleanKey = s3Key.startsWith("/") ? s3Key.substring(1) : s3Key;
            
            // Normalize Unicode to NFC (Canonical Composition)
            // This ensures diacritics are in composed form (e.g., ș as single character, not s + combining diacritic)
            cleanKey = java.text.Normalizer.normalize(cleanKey, java.text.Normalizer.Form.NFC);
            
            // Log the original key for debugging with detailed character analysis
            StringBuilder charDebug = new StringBuilder();
            for (int i = 0; i < cleanKey.length(); i++) {
                char c = cleanKey.charAt(i);
                if (c > 127) { // Non-ASCII
                    charDebug.append(String.format("'%c'=U+%04X ", c, (int)c));
                }
            }
            log.info("Generating presigned URL for S3 key: {}", cleanKey);
            if (charDebug.length() > 0) {
                log.info("  Non-ASCII chars: {}", charDebug.toString());
            }
            log.info("  UTF-8 bytes: {}", 
                    java.util.Arrays.toString(cleanKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            
            
            // For ș and ț, the file in S3 is likely stored with CEDILLA (wrong but common)
            // Database has COMMA BELOW (correct). Need to convert.
            boolean hasStChars = cleanKey.contains("ș") || cleanKey.contains("ț");
            
            if (hasStChars) {
                // Convert comma below → cedilla for S3
                String cedillaKey = cleanKey
                        .replace('ș', 'ş')  // U+0219 → U+015F
                        .replace('ț', 'ţ'); // U+021B → U+0163
                
                log.info("Key has ș/ț, converting to cedilla form for S3");
                log.info("  Original (comma): {} bytes: {}", 
                        cleanKey,
                        java.util.Arrays.toString(cleanKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                log.info("  Cedilla form: {} bytes: {}", 
                        cedillaKey,
                        java.util.Arrays.toString(cedillaKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                
                cleanKey = cedillaKey;
            }
            
            // AWS SDK v2 should handle UTF-8 encoding automatically
            // The key should be passed as-is (unencoded) to the SDK
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cleanKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(urlExpiration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            String url = presignedRequest.url().toString();

            // Log the generated URL (truncate for readability)
            String urlPreview = url.length() > 150 ? url.substring(0, 150) + "..." : url;
            log.info("✅ Successfully generated presigned URL");
            log.info("  Final key used: {}", cleanKey);
            log.info("  URL preview: {}", urlPreview);
            
            // Verify the URL is properly formed
            if (url.isEmpty() || !url.startsWith("http")) {
                log.error("Generated URL is invalid: {}", url);
                return "";
            }

            return url;

        } catch (Exception e) {
            log.error("Failed to generate pre-signed URL for key '{}': {}", s3Key, e.getMessage(), e);
            return ""; // Return empty string on error
        }
    }

    /**
     * Checks if a URI is an S3 key (not a local app:// asset)
     * 
     * @param uri The URI to check
     * @return true if this is an S3 key, false if it's a local asset
     */
    public boolean isS3Key(String uri) {
        if (uri == null || uri.isEmpty()) {
            return false;
        }
        
        // Local assets start with "app://" or "assets/"
        if (uri.startsWith("app://") || uri.startsWith("assets/")) {
            return false;
        }
        
        // If it already contains a full URL with protocol, it's not a plain S3 key
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            return false;
        }
        
        // Otherwise, assume it's an S3 key (e.g., "modules/1/submodules/1/lessons/1/image.jpg")
        return true;
    }
    
}


