package rotld.apscrm.api.v1.volunteer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.volunteer.dto.VolunteerDetailsDto;
import rotld.apscrm.api.v1.volunteer.dto.VolunteerResponseDto;
import rotld.apscrm.api.v1.volunteer.mapper.VolunteerMapper;
import rotld.apscrm.api.v1.volunteer.repository.Volunteer;
import rotld.apscrm.api.v1.volunteer.repository.VolunteerMeta;
import rotld.apscrm.api.v1.volunteer.repository.VolunteerRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VolunteerService {

    private final VolunteerRepository volunteerRepository;

    public Page<Volunteer> getAll(Pageable pageable) {
        return volunteerRepository.findAll(pageable);
    }

    @Transactional
    public void delete(Integer id){
        volunteerRepository.deleteAllByVolunteerId(id);
        int affected = volunteerRepository.hardDeleteById(id);
        if (affected == 0){
            throw new IllegalArgumentException("Volunteer not found: " + id);
        }
    }

    public List<VolunteerResponseDto> getAll() {
        List<Volunteer> all = volunteerRepository.findAll();
        return all
                .stream()
                .map(VolunteerMapper::toResponseDto)
                .toList();
    }


    public VolunteerDetailsDto getDetails(Integer id){
        var v = volunteerRepository.findByIdWithMeta(id)
                .orElseThrow(() -> new IllegalArgumentException("Volunteer not found: " + id));

        Map<String,String> meta = v.getMeta().stream()
                .collect(Collectors.toMap(VolunteerMeta::getMetaKey, VolunteerMeta::getMetaValue, (a, b)->a));

        String lastName   = meta.getOrDefault("_vol_nume", "");
        String firstName  = meta.getOrDefault("_vol_prenume", "");
        String email      = meta.getOrDefault("_vol_email", "");
        String phone      = meta.getOrDefault("_vol_telefon", "");
        String ageStr     = meta.getOrDefault("_vol_varsta", "");
        Integer age       = ageStr == null || ageStr.isBlank() ? null : safeInt(ageStr);
        String ocupation  = meta.getOrDefault("_vol_ocupatie", "");
        String domain     = meta.getOrDefault("_vol_domeniu", "");
        String disp       = meta.getOrDefault("_vol_disponibilitate", "");
        String motivation = meta.getOrDefault("_vol_motivatie", "");
        String experience = meta.getOrDefault("_vol_experienta", "");
        Boolean gdpr      = "1".equals(meta.getOrDefault("_vol_gdpr", "0"));
        String source     = meta.getOrDefault("_vol_source", "");

        return VolunteerDetailsDto.builder()
                .id(v.getId())
                .postTitle(v.getPostTitle())
                .date(v.getPostDate())
                .firstName(firstName)
                .lastName(lastName)
                .name((firstName + " " + lastName).trim())
                .email(email)
                .phone(phone)
                .age(age)
                .ocupation(ocupation)
                .domain(domain)
                .disponibility(disp)
                .motivation(motivation)
                .experience(experience)
                .gdpr(gdpr)
                .source(source)
                .link(v.getLink())
                .adminEdit("https://actiunepentrusanatate.ro/wp-admin/post.php?post=" + v.getId() + "&action=edit")
                .build();
    }

    private Integer safeInt(String s){
        try { return Integer.valueOf(s.trim()); } catch(Exception e){ return null; }
    }
}
