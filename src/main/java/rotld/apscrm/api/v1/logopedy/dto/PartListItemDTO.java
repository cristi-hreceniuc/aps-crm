package rotld.apscrm.api.v1.logopedy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO for Part listing (without lessons)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartListItemDTO {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Integer position;
    private Integer totalLessons;
    private Integer completedLessons;
}
