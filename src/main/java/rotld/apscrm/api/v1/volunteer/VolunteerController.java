package rotld.apscrm.api.v1.volunteer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.volunteer.dto.VolunteerResponseDto;
import rotld.apscrm.api.v1.volunteer.mapper.VolunteerMapper;
import rotld.apscrm.api.v1.volunteer.repository.Volunteer;
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

    @GetMapping("/search")
    public Page<VolunteerResponseDto> searchVolunteers(
            @RequestParam(name = "q", required = false, defaultValue = "") String q,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        // dacă q e gol, returnăm lista standard (păstrăm același endpoint pentru FE)
        Page<Volunteer> page = q == null || q.isBlank()
                ? volunteerQueryService.findAllPage(pageable)
                : volunteerQueryService.search(q, pageable);

        return page.map(VolunteerMapper::toResponseDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVolunteer(@PathVariable Integer id){
        log.info("Deleting volunteer <{}>", id);
        volunteerService.delete(id);
        log.info("Deleted volunteer <{}>", id);
        return ResponseEntity.noContent().build();
    }

}
