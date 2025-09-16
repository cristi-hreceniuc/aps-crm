package rotld.apscrm.api.v1.volunteer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.volunteer.dto.VolunteerResponseDto;
import rotld.apscrm.api.v1.volunteer.mapper.VolunteerMapper;
import rotld.apscrm.api.v1.volunteer.repository.Volunteer;
import rotld.apscrm.api.v1.volunteer.repository.VolunteerRepository;

import java.util.List;

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
}
