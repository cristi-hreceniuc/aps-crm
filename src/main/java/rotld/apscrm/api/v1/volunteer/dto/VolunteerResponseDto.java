package rotld.apscrm.api.v1.volunteer.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record VolunteerResponseDto (
        Integer id,
        String name,
        LocalDate date,
        String postName,
        String email,
        String phone,
        String disponibility,
        String domain,
        Integer age,
        String ocupation,
        String link,
        String motivation,
        String experience
){
}

/*
nume dată nume post email telefon disponibilitate domeniu link
 */