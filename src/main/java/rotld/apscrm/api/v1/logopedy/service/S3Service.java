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
            
            // Log the original key for debugging
            log.info("Generating presigned URL for S3 key: {} (bytes: {})", 
                    cleanKey, 
                    java.util.Arrays.toString(cleanKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            
            // Check if key contains non-ASCII characters (diacritics)
            boolean hasNonAscii = !cleanKey.matches("^[\\x00-\\x7F]*$");
            if (hasNonAscii) {
                log.info("S3 key contains non-ASCII characters (diacritics): {}", cleanKey);
            }
            
            // First, check if the object exists in S3
            // Try multiple Unicode forms due to Romanian diacritic issues
            String workingKey = cleanKey;
            boolean objectExists = false;
            
            // Try original key first
            try {
                HeadObjectRequest headRequest = HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(cleanKey)
                        .build();
                s3Client.headObject(headRequest);
                log.info("✅ S3 object EXISTS with original form: {}", cleanKey);
                objectExists = true;
            } catch (NoSuchKeyException e) {
                log.warn("Original key not found, trying alternatives...");
                
                // Try with cedilla variants (wrong but common)
                // ș (comma U+0219) ↔ ş (cedilla U+015F)
                // ț (comma U+021B) ↔ ţ (cedilla U+0163)
                String cedillaKey = cleanKey
                        .replace('\u0219', '\u015F')  // ș → ş
                        .replace('\u021B', '\u0163'); // ț → ţ
                
                if (!cedillaKey.equals(cleanKey)) {
                    log.info("Trying cedilla form: {} (bytes: {})", 
                            cedillaKey, 
                            java.util.Arrays.toString(cedillaKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    try {
                        HeadObjectRequest cedillaRequest = HeadObjectRequest.builder()
                                .bucket(bucketName)
                                .key(cedillaKey)
                                .build();
                        s3Client.headObject(cedillaRequest);
                        log.info("✅ S3 object EXISTS with CEDILLA form: {}", cedillaKey);
                        workingKey = cedillaKey;
                        objectExists = true;
                    } catch (NoSuchKeyException e2) {
                        // Still not found
                    }
                }
                
                // If still not found, try comma variants (correct but might not match)
                if (!objectExists) {
                    String commaKey = cleanKey
                            .replace('\u015F', '\u0219')  // ş → ș
                            .replace('\u0163', '\u021B'); // ţ → ț
                    
                    if (!commaKey.equals(cleanKey) && !commaKey.equals(cedillaKey)) {
                        log.info("Trying comma form: {} (bytes: {})", 
                                commaKey, 
                                java.util.Arrays.toString(commaKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                        try {
                            HeadObjectRequest commaRequest = HeadObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(commaKey)
                                    .build();
                            s3Client.headObject(commaRequest);
                            log.info("✅ S3 object EXISTS with COMMA form: {}", commaKey);
                            workingKey = commaKey;
                            objectExists = true;
                        } catch (NoSuchKeyException e3) {
                            // Still not found
                        }
                    }
                }
                
                if (!objectExists) {
                    log.error("❌ S3 object DOES NOT EXIST in any form: {}", cleanKey);
                    log.error("   Original tried: {} (bytes: {})", 
                            cleanKey, 
                            java.util.Arrays.toString(cleanKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    if (!cedillaKey.equals(cleanKey)) {
                        log.error("   Cedilla tried: {} (bytes: {})", 
                                cedillaKey, 
                                java.util.Arrays.toString(cedillaKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    }
                    log.error("   Please verify the exact filename in S3");
                    return "";
                }
            } catch (Exception e) {
                log.warn("⚠️  Could not verify S3 object existence for key: {} - {}", cleanKey, e.getMessage());
                // Continue anyway and let presigned URL generation handle it
            }
            
            // Use the working key (whichever form exists in S3)
            cleanKey = workingKey;
            
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
            log.info("Generated presigned URL for key '{}': {}", cleanKey, urlPreview);
            
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


