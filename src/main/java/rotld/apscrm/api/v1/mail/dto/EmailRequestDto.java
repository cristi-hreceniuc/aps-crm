package rotld.apscrm.api.v1.mail.dto;

import lombok.Builder;

@Builder
public record EmailRequestDto (
        String sendTo,
        String subject,
        String message
){
}
