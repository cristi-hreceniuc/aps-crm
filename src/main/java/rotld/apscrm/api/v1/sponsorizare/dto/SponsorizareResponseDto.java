package rotld.apscrm.api.v1.sponsorizare.dto;


import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SponsorizareResponseDto {
    private Integer id;
    private String  date;

    private String  companyName;
    private String  fiscalCode;
    private String  email;
    private String  phone;
    private String  iban;

    private String  amount;        // string afișat
    private String  contractDate;

    private String  docUrl;
    private String  jsonUrl;
    private String  signatureUrl;
    private String  detail;
    private String  adminEdit;

    private Boolean downloaded;
    private Boolean verified;
    private Boolean corrupt;
}