package rotld.apscrm.api.v1.logopedy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
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
     * 
     * @param s3Key The S3 key (e.g., "modules/1/submodules/1/lessons/1/image.jpg")
     * @return Pre-signed URL valid for the configured duration
     */
    public String generatePresignedUrl(String s3Key) {
        try {
            // Remove leading slash if present
            String cleanKey = s3Key.startsWith("/") ? s3Key.substring(1) : s3Key;

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

            log.debug("Generated pre-signed URL for key: {} -> {}", cleanKey, url.substring(0, Math.min(100, url.length())) + "...");
            return url;

        } catch (Exception e) {
            log.error("Failed to generate pre-signed URL for key: {}", s3Key, e);
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
