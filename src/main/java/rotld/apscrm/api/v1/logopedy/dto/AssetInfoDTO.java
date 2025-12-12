package rotld.apscrm.api.v1.logopedy.dto;

/**
 * DTO for asset information needed for offline caching
 */
public record AssetInfoDTO(
        Long assetId,
        String s3Key,         // The S3 path without presigned URL
        String kind,          // IMAGE, AUDIO, VIDEO, etc.
        String mime,          // MIME type
        String checksum,      // For validation
        Long lessonId,        // Which lesson this asset belongs to
        String lessonTitle    // For reference
) {}
