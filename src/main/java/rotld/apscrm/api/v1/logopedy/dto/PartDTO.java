package rotld.apscrm.api.v1.logopedy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Part - represents a group of lessons within a submodule
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartDTO {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Integer position;
    private List<LessonListItemDTO> lessons;
    private Integer totalLessons;
    private Integer completedLessons;
}
