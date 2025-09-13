package rotld.apscrm.api.v1.cause.repository;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Immutable
@Subselect("""
  SELECT
    s.ID                                                   AS id,
    DATE_FORMAT(s.post_date, '%Y-%m-%dT%H:%i:%s')          AS post_date_iso,
    s.post_title                                           AS title,
    s.post_excerpt                                         AS excerpt,

    CAST(goal.meta_value    AS UNSIGNED)                   AS goal,
    CAST(donors.meta_value  AS UNSIGNED)                   AS donors,
    CAST(donated.meta_value AS UNSIGNED)                   AS donated,

    s.guid                                                 AS guid
  FROM wordpress.wp_posts s
  LEFT JOIN wordpress.wp_postmeta goal
         ON goal.post_id = s.ID AND goal.meta_key   = 'frmaster-funding-goal'
  LEFT JOIN wordpress.wp_postmeta donors
         ON donors.post_id = s.ID AND donors.meta_key = 'frmaster-donor-amount'
  LEFT JOIN wordpress.wp_postmeta donated
         ON donated.post_id = s.ID AND donated.meta_key = 'frmaster-donated-amount'
  WHERE s.post_type = 'cause'
    AND s.post_status = 'publish'
""")
@Synchronize({"wp_posts","wp_postmeta"})
public class Cause {
    @Id
    private Integer id;

    @Column(name = "post_date_iso")
    private String date;       // ⇐ numele câmpului din entitate rămâne 'date' ca să se potrivească cu FE

    private String title;
    private String excerpt;

    private Double goal;
    private Double donors;
    private Double donated;

    private String guid;
}