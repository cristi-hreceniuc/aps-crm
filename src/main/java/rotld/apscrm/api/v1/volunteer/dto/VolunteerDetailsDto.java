package rotld.apscrm.api.v1.volunteer.dto;


import lombok.Builder;
import java.time.LocalDate;

@Builder
public record VolunteerDetailsDto(
        Integer id,
        String postTitle,
        LocalDate date,
        String firstName,
        String lastName,
        String name,
        String email,
        String phone,
        Integer age,
        String ocupation,
        String domain,
        String disponibility,
        String motivation,
        String experience,
        Boolean gdpr,
        String source,
        String link,       // guid (public)
        String adminEdit   // link wp-admin
) {}
