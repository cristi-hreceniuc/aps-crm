package rotld.apscrm.api.v1.sponsorizare.dto;


import lombok.*;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class SponsorizareResponseDto {
    private Integer id;
    private String  date;

    // grid (existente)
    private String  companyName;
    private String  fiscalCode;
    private String  email;   // reprez
    private String  phone;   // reprez
    private String  iban;
    private String  amount;
    private String  contractDate;

    private String  docUrl;
    private String  jsonUrl;
    private String  signatureUrl;
    private String  detail;
    private String  adminEdit;

    private Boolean downloaded;
    private Boolean verified;
    private Boolean corrupt;

    // ✨ noi – pentru “view”
    private String  companyRegCom;     // Jxx/....
    private String  companyAddress;    // firmă
    private String  companyCounty;     // firmă
    private String  companyCity;       // firmă

    private String  corrAddress;       // corespondență
    private String  corrCounty;
    private String  corrCity;

    private String  repFirstName;      // prenume
    private String  repLastName;       // nume
    private String  repRole;           // poziție

    private String  bankName;          // ex: BT

    private String  signatureB64;      // din _aps_signature_b64 (data:image/png;base64,...)
    private Boolean sendEmail;         // din _aps_send_email
}
