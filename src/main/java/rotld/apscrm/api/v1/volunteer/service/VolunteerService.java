package rotld.apscrm.api.v1.volunteer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Service;
import rotld.apscrm.api.v1.volunteer.dto.VolunteerResponseDto;
import rotld.apscrm.api.v1.volunteer.repository.Volunteer;
import rotld.apscrm.api.v1.volunteer.repository.VolunteerRepository;

@Service
@RequiredArgsConstructor
public class VolunteerService {

    private final VolunteerRepository volunteerRepository;

    public Page<Volunteer> getAll(Pageable pageable) {
        return volunteerRepository.findAll(pageable);
    }
}
