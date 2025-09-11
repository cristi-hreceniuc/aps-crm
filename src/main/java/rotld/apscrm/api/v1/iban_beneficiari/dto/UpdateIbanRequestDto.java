package rotld.apscrm.api.v1.iban_beneficiari.dto;

import lombok.Builder;

@Builder
public record UpdateIbanRequestDto(
        String nume,
        String iban
) {
}
