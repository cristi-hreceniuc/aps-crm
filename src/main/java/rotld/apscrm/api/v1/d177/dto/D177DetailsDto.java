package rotld.apscrm.api.v1.d177.dto;

import lombok.*;

import java.util.Map;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class D177DetailsDto {
    private Integer id;
    private String  title;
    private String  postDateIso;

    private Map<String,String> firma;        // denumire, cui, regcom, adresa, judet, oras...
    private Map<String,String> corespondenta;// adresa, judet, oras...
    private Map<String,String> reprezentant; // nume, prenume, email, tel, pozitie...
    private Map<String,String> contract;     // data, suma

    private String  docUrl;                  // link fișier
    private String  sigUrl;                  // link semnătură imagine (dacă există)
    private String  docHtml;                 // HTML contract (opțional de afișat în modal)

    private String  adminEdit;               // link wp-admin
    private Boolean downloaded;
    private Boolean verified;
    private Boolean corrupt;
}
