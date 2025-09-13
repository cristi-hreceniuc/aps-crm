package rotld.apscrm.api.v1.cause.dto;

import lombok.*;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class CauseResponseDto {
    private Integer id;
    private String  date;      // ISO (yyyy-MM-ddTHH:mm:ss)
    private String  title;
    private String  excerpt;
    private Double    goal;      // frmaster-funding-goal
    private Double    donors;    // frmaster-donor-amount
    private Double    donated;   // frmaster-donated-amount
    private String  guid;      // link
}