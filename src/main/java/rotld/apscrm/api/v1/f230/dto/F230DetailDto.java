package rotld.apscrm.api.v1.f230.dto;


import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class F230DetailDto {
    private Integer id;
    private String  postDateIso;
    private String  title;

    private String  year;
    private String  firstName;
    private String  lastName;
    private String  initiala;
    private String  cnp;

    private String  street;
    private String  number;
    private String  block;
    private String  staircase;
    private String  floor;
    private String  apartment;
    private String  county;
    private String  city;
    private String  postalCode;

    private String  email;
    private String  phone;
    private String  fax;
    private String  iban;
    private String  distrib2;
    private String  acordEmail;

    private String  pdfUrl;
    private Long    nrBorderou;
    private String  adminEdit;
}