package rotld.apscrm.api.v1.logopedy.dto;

import lombok.Builder;

@Builder(toBuilder = true)
public record SubmoduleListDTO(
        Long id, String title, String introText, Integer position,
        java.util.List<PartListItemDTO> parts,
        // Deprecated: kept for backward compatibility, will be removed
        @Deprecated
        java.util.List<LessonListItemDTO> lessons
) {
    // Constructor for parts-only response
    public SubmoduleListDTO(Long id, String title, String introText, Integer position, java.util.List<PartListItemDTO> parts) {
        this(id, title, introText, position, parts, null);
    }
}