package rotld.apscrm.api.v1.logopedy.dto;

import java.util.List;

/**
 * Response containing all asset URLs for a Part, used for prefetching.
 */
public record PartAssetsResponse(List<AssetInfo> assets, int totalCount) {
    
    /**
     * Information about a single asset (image or audio).
     */
    public record AssetInfo(String url, String type, String key) {
        public static AssetInfo image(String url, String key) {
            return new AssetInfo(url, "IMAGE", key);
        }
        
        public static AssetInfo audio(String url, String key) {
            return new AssetInfo(url, "AUDIO", key);
        }
    }
}

