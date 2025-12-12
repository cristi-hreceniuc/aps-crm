package rotld.apscrm.api.v1.logopedy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

/**
 * Service for generating pre-signed URLs for private S3 assets.
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

        log.info("S3Service initialized - Bucket: {}, Region: {}, URL Expiration: {} min",
                bucketName, region, expirationMinutes);
    }

    /**
     * Generates a pre-signed URL for an S3 object.
     * 
     * @param s3Key The S3 key from database (e.g., "submodules/s/ca.mp3")
     * @return Pre-signed URL valid for the configured duration, or empty string on error
     */
    public String generatePresignedUrl(String s3Key) {
        try {
            // Remove leading slash if present
            String cleanKey = s3Key.startsWith("/") ? s3Key.substring(1) : s3Key;
            
            log.debug("Generating presigned URL for S3 key: {}", cleanKey);

            // Generate presigned URL
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

            // Log success
            String urlPreview = url.length() > 120 ? url.substring(0, 120) + "..." : url;
            log.debug("Presigned URL: {}", urlPreview);
            
            if (url.isEmpty() || !url.startsWith("http")) {
                log.error("Generated URL is invalid: {}", url);
                return "";
            }

            return url;

        } catch (Exception e) {
            log.error("Failed to generate pre-signed URL for key '{}': {}", s3Key, e.getMessage(), e);
            return "";
        }
    }

    /**
     * Checks if a URI is an S3 key (not a local app:// asset)
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
        
        // Otherwise, assume it's an S3 key
        return true;
    }

    /**
     * Uploads a file to S3 and returns the S3 key.
     * 
     * @param inputStream The file input stream
     * @param contentType The content type of the file (e.g., "image/jpeg")
     * @param contentLength The size of the file in bytes
     * @param folder The folder prefix in S3 (e.g., "profile-images", "user-avatars")
     * @param filename The original filename (will be sanitized and made unique)
     * @return The S3 key of the uploaded file
     */
    public String uploadFile(InputStream inputStream, String contentType, long contentLength, 
                           String folder, String filename) {
        try {
            // Generate a unique filename to avoid collisions
            String uniqueFilename = generateUniqueFilename(filename);
            String s3Key = folder + "/" + uniqueFilename;
            
            log.info("Uploading file to S3: bucket={}, key={}, contentType={}, size={}", 
                    bucketName, s3Key, contentType, contentLength);

            // Create PutObjectRequest
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            // Upload the file
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));
            
            log.info("Successfully uploaded file to S3: {}", s3Key);
            return s3Key;

        } catch (Exception e) {
            log.error("Failed to upload file to S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a unique filename by appending a UUID before the file extension.
     * Example: "profile.jpg" becomes "profile_abc123.jpg"
     */
    private String generateUniqueFilename(String originalFilename) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            String nameWithoutExt = originalFilename.substring(0, lastDotIndex);
            String extension = originalFilename.substring(lastDotIndex);
            return sanitizeFilename(nameWithoutExt) + "_" + uuid + extension;
        } else {
            return sanitizeFilename(originalFilename) + "_" + uuid;
        }
    }

    /**
     * Sanitizes a filename by removing special characters and spaces.
     */
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
