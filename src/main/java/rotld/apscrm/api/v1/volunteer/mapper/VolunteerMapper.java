package rotld.apscrm.api.v1.volunteer.mapper;

import rotld.apscrm.api.v1.volunteer.dto.VolunteerResponseDto;
import rotld.apscrm.api.v1.volunteer.repository.Volunteer;

public class VolunteerMapper {
    public static VolunteerResponseDto toResponseDto(Volunteer volunteer) {
        return VolunteerResponseDto.builder()
                .id(volunteer.getId())
                .name(volunteer.getMeta().get(0).getMetaValue() + " " + volunteer.getMeta().get(1).getMetaValue())
                .age(Integer.valueOf(volunteer.getMeta().get(4).getMetaValue()))
                .date(volunteer.getPostDate())
                .phone(volunteer.getMeta().get(3).getMetaValue())
                .email(volunteer.getMeta().get(2).getMetaValue())
                .disponibility(volunteer.getMeta().get(7).getMetaValue())
                .domain(volunteer.getMeta().get(6).getMetaValue())
                .ocupation(volunteer.getMeta().get(5).getMetaValue())
                .postName(volunteer.getPostTitle())
                .motivation(volunteer.getMeta().get(8).getMetaValue())
                .experience(volunteer.getMeta().get(9).getMetaValue())
                .link(volunteer.getLink())
                .build();
    }
}
