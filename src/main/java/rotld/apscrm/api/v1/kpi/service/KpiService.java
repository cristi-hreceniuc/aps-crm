package rotld.apscrm.api.v1.kpi.service;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import rotld.apscrm.api.v1.kpi.dto.KpiResponseDto;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KpiService {

    private final NamedParameterJdbcTemplate jdbc;

    public KpiResponseDto getAll() {
        return KpiResponseDto.builder()
                .volunteers(fetchVolunteers())
                .f177(fetch177())
                .sponsorship(fetchSponsorship())
                .f230(fetch230())
                .iban(fetchIban())
                .causes(fetchCauses())
                .persoane(fetchUsers())
                .build();
    }

    /* ---------- Voluntari ---------- */
    private KpiResponseDto.Volunteers fetchVolunteers() {
        // Ajustează post_type dacă este altul la tine (ex: 'aps_volunteer')
        String sql = """
      SELECT
        COUNT(DISTINCT s.ID)                                                   AS total,
        AVG(CAST(age.meta_value AS UNSIGNED))                                  AS avg_age,
        AVG(
          CASE
            WHEN disp.meta_value REGEXP '^[0-9]+\\+$' THEN CAST(disp.meta_value AS UNSIGNED)
            WHEN disp.meta_value REGEXP '^[0-9]+-[0-9]+' THEN
              (CAST(SUBSTRING_INDEX(disp.meta_value,'-',1) AS UNSIGNED) +
               CAST(SUBSTRING_INDEX(disp.meta_value,'-',-1) AS UNSIGNED))/2
            ELSE NULL
          END
        ) AS avg_hours
      FROM wordpress.wp_posts s
      LEFT JOIN wordpress.wp_postmeta age  ON age.post_id  = s.ID AND age.meta_key  = '_vol_varsta'
      LEFT JOIN wordpress.wp_postmeta disp ON disp.post_id = s.ID AND disp.meta_key = '_vol_disponibilitate'
      WHERE s.post_type IN ('aps_volunteer','volunteer','voluntari')
        AND s.post_status='publish'
    """;
        Map<String, Object> row = jdbc.queryForMap(sql, new MapSqlParameterSource());
        return KpiResponseDto.Volunteers.builder()
                .total(row.get("total") == null ? 0L : ((Number)row.get("total")).longValue())
                .avgAge(row.get("avg_age") == null ? null : ((Number)row.get("avg_age")).doubleValue())
                .avgDisponibilityHours(row.get("avg_hours") == null ? null : ((Number)row.get("avg_hours")).doubleValue())
                .build();
    }

    /* ---------- Declarația 177 ---------- */
    private KpiResponseDto.F177 fetch177() {
        // extractoare fără REGEXP_SUBSTR (compatibile MariaDB/MySQL 5.7+)
        final String SUMA = """
    SUBSTRING_INDEX(
      SUBSTRING_INDEX(
        SUBSTRING_INDEX(contract.meta_value, 's:4:"suma";s:', -1),
        '"', 2
      ),
      '"', -1
    )
  """;
        final String DATA = """
    SUBSTRING_INDEX(
      SUBSTRING_INDEX(
        SUBSTRING_INDEX(contract.meta_value, 's:4:"data";s:', -1),
        '"', 2
      ),
      '"', -1
    )
  """;

        String sql = """
    SELECT
      COUNT(*) AS companies,
      SUM(CAST(""" + SUMA + """ 
      AS UNSIGNED)) AS total_sum,
      SUM(
        CASE
          WHEN STR_TO_DATE(""" + DATA + """
               , '%Y-%m-%d')
               BETWEEN DATE_FORMAT(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH), '%Y-%m-01')
                   AND LAST_DAY(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH))
          THEN CAST(""" + SUMA + """
           AS UNSIGNED)
          ELSE 0
        END
      ) AS last_month_sum
    FROM wordpress.wp_posts s
    LEFT JOIN wordpress.wp_postmeta contract
           ON contract.post_id = s.ID AND contract.meta_key = '_aps177_contract'
    WHERE s.post_type='aps_s177' AND s.post_status='publish'
  """;

        Map<String,Object> row = jdbc.queryForMap(sql, new MapSqlParameterSource());
        return KpiResponseDto.F177.builder()
                .companies(row.get("companies") == null ? 0L : ((Number)row.get("companies")).longValue())
                .totalAmount(row.get("total_sum") == null ? 0L : ((Number)row.get("total_sum")).longValue())
                .lastMonthAmount(row.get("last_month_sum") == null ? 0L : ((Number)row.get("last_month_sum")).longValue())
                .build();
    }

    /* ---------- Sponsorizare (aps_sponsorship) ---------- */
    private KpiResponseDto.Sponsorship fetchSponsorship() {
        final String SUMA = """
    SUBSTRING_INDEX(
      SUBSTRING_INDEX(
        SUBSTRING_INDEX(contract.meta_value, 's:4:"suma";s:', -1),
        '"', 2
      ),
      '"', -1
    )
  """;
        final String DATA = """
    SUBSTRING_INDEX(
      SUBSTRING_INDEX(
        SUBSTRING_INDEX(contract.meta_value, 's:4:"data";s:', -1),
        '"', 2
      ),
      '"', -1
    )
  """;

        String sql = """
    SELECT
      SUM(CAST(""" + SUMA + """
       AS UNSIGNED)) AS total_sum,
      SUM(
        CASE
          WHEN STR_TO_DATE(""" + DATA + """
               , '%Y-%m-%d')
               BETWEEN DATE_FORMAT(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH), '%Y-%m-01')
                   AND LAST_DAY(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH))
          THEN CAST(""" + SUMA + """
           AS UNSIGNED)
          ELSE 0
        END
      ) AS last_month_sum
    FROM wordpress.wp_posts s
    LEFT JOIN wordpress.wp_postmeta contract
           ON contract.post_id = s.ID AND contract.meta_key = '_aps_contract'
    WHERE s.post_type='aps_sponsorship' AND s.post_status='publish'
  """;

        Map<String,Object> row = jdbc.queryForMap(sql, new MapSqlParameterSource());
        return KpiResponseDto.Sponsorship.builder()
                .totalAmount(row.get("total_sum")==null?0L:((Number)row.get("total_sum")).longValue())
                .lastMonthAmount(row.get("last_month_sum")==null?0L:((Number)row.get("last_month_sum")).longValue())
                .build();
    }


    /* ---------- Formular 230 ---------- */
    private KpiResponseDto.F230 fetch230() {
        String sql = """
      SELECT
        SUM(CASE WHEN dist2.meta_value='1' THEN 0 ELSE 1 END) AS one_y,
        SUM(CASE WHEN dist2.meta_value='1' THEN 1 ELSE 0 END) AS two_y,
        SUM(CASE WHEN anul.meta_value = YEAR(CURRENT_DATE) THEN 1 ELSE 0 END) AS exp_year,
        SUM(CASE WHEN YEAR(s.post_date)=YEAR(CURRENT_DATE) AND MONTH(s.post_date)=MONTH(CURRENT_DATE) THEN 1 ELSE 0 END) AS this_month
      FROM wordpress.wp_posts s
      LEFT JOIN wordpress.wp_postmeta dist2 ON dist2.post_id=s.ID AND dist2.meta_key='distribuire2ani'
      LEFT JOIN wordpress.wp_postmeta anul  ON anul.post_id =s.ID AND anul.meta_key='anul'
      WHERE s.post_type='formular230' AND s.post_status='publish'
    """;
        Map<String, Object> row = jdbc.queryForMap(sql, new MapSqlParameterSource());
        return KpiResponseDto.F230.builder()
                .total1y(row.get("one_y") == null ? 0L : ((Number)row.get("one_y")).longValue())
                .total2y(row.get("two_y") == null ? 0L : ((Number)row.get("two_y")).longValue())
                .expiringThisYear(row.get("exp_year") == null ? 0L : ((Number)row.get("exp_year")).longValue())
                .thisMonth(row.get("this_month") == null ? 0L : ((Number)row.get("this_month")).longValue())
                .build();
    }

    /* ---------- IBAN Beneficiari ---------- */
    private KpiResponseDto.Iban fetchIban() {
        String sql = """
      SELECT COUNT(*) AS total
      FROM wordpress.wp_posts s
      WHERE s.post_type='iban_beneficiar' AND s.post_status IN ('publish','draft')
    """;
        Map<String, Object> row = jdbc.queryForMap(sql, new MapSqlParameterSource());
        return KpiResponseDto.Iban.builder()
                .total(row.get("total") == null ? 0L : ((Number)row.get("total")).longValue())
                .build();
    }

    /* ---------- Cauze ---------- */
    private KpiResponseDto.Causes fetchCauses() {
        String sql = """
      SELECT
        COUNT(*) AS total,
        SUM(CASE WHEN CAST(donated.meta_value AS UNSIGNED) >= CAST(goal.meta_value AS UNSIGNED) THEN 1 ELSE 0 END) AS reached,
        AVG( LEAST( CAST(donated.meta_value AS UNSIGNED) / NULLIF(CAST(goal.meta_value AS UNSIGNED),0), 1.0) ) * 100 AS avg_pct,
        DATE_FORMAT(
          COALESCE(
            (SELECT MIN(p2.post_date) FROM wordpress.wp_posts p2 WHERE p2.post_type='cause' AND p2.post_status='future' AND p2.post_date > NOW()),
            (SELECT MAX(p3.post_date) FROM wordpress.wp_posts p3 WHERE p3.post_type='cause' AND p3.post_status IN ('publish','future'))
          ), '%Y-%m-%dT%H:%i:%s'
        ) AS next_date
      FROM wordpress.wp_posts s
      LEFT JOIN wordpress.wp_postmeta goal    ON goal.post_id   = s.ID AND goal.meta_key   = 'frmaster-funding-goal'
      LEFT JOIN wordpress.wp_postmeta donated ON donated.post_id= s.ID AND donated.meta_key= 'frmaster-donated-amount'
      WHERE s.post_type='cause' AND s.post_status IN ('publish','future')
    """;
        Map<String, Object> row = jdbc.queryForMap(sql, new MapSqlParameterSource());
        Double avg = row.get("avg_pct")==null?null:((Number)row.get("avg_pct")).doubleValue();
        return KpiResponseDto.Causes.builder()
                .total(row.get("total")==null?0L:((Number)row.get("total")).longValue())
                .reachedGoal(row.get("reached")==null?0L:((Number)row.get("reached")).longValue())
                .avgProgressPct(avg==null?null:Math.round(avg*10.0)/10.0)
                .nextCampaignDate((String)row.get("next_date"))
                .build();
    }

    /* ---------- Users ---------- */
    private KpiResponseDto.Persoane fetchUsers() {
        String sql = "SELECT COUNT(*) AS total FROM wordpress.wp_users";
        String sql1 = "SELECT COUNT(*) AS total FROM crm.users";
        Map<String, Object> row = jdbc.queryForMap(sql, new MapSqlParameterSource());
        Map<String, Object> row1 = jdbc.queryForMap(sql1, new MapSqlParameterSource());
        return KpiResponseDto.Persoane.builder()
                .users(row.get("total") == null ? 0L : ((Number)row.get("total")).longValue())
                .admins(row1.get("total") == null ? 0L : ((Number)row1.get("total")).longValue())
                .build();
    }
}