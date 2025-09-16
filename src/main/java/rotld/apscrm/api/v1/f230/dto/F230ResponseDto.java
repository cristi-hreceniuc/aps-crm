package rotld.apscrm.api.v1.f230.dto;


import lombok.*;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class F230ResponseDto {
    private Integer id;
    private String  year;               // „anul”
    private String  submittedAt;        // post_date
    private String  period;             // "1 an" / "2 ani"

    private String  iban;
    private String  beneficiary;        // nume + prenume
    private String  emailContrib;       // email
    private String  nrBorderou;         // gol
    private Boolean downloaded;
    private Boolean verified;
    private Boolean corrupt;
    private String acordDate;          // acordComunicare (acord date ident.)
    private String  pdfUrl;             // download pdf
    private String  detail;             // admin edit
}