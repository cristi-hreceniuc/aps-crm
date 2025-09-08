package rotld.apscrm.api.v1.volunteer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rotld.apscrm.api.v1.volunteer.dto.VolunteerResponseDto;
import rotld.apscrm.api.v1.volunteer.mapper.VolunteerMapper;
import rotld.apscrm.api.v1.volunteer.service.VolunteerQueryService;
import rotld.apscrm.api.v1.volunteer.service.VolunteerService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/volunteers")
public class VolunteerController {

    private final VolunteerService volunteerService;
    private final VolunteerQueryService volunteerQueryService;

    @GetMapping
    public Page<VolunteerResponseDto> getPageableRemoteWorkRequests(@PageableDefault(size = 50, sort = "id") Pageable pageable) {
        log.info("Getting all volunteers with pageable: <{}>", pageable);

        var volunteers = volunteerQueryService.findAllPage(pageable)
                .map(VolunteerMapper::toResponseDto);

        log.info("Successfully returned volunteers with pageable: <{}>", pageable);
        return volunteers;
    }
}
