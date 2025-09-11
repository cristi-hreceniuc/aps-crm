package rotld.apscrm.api.v1.iban_beneficiari.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IbanBeneficiariResponseDto {
    private Integer id;
    private String  name;
    private String  iban;
    private String  addedAt;
}