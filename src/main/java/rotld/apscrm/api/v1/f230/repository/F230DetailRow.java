package rotld.apscrm.api.v1.f230.repository;


public interface F230DetailRow {
    Integer getId();
    String  getPostDateIso();
    String  getTitle();

    String  getYear();
    String  getFirstName();
    String  getLastName();
    String  getInitiala();
    String  getCnp();

    String  getStreet();
    String  getNumber();
    String  getBlock();
    String  getStaircase();
    String  getFloor();
    String  getApartment();
    String  getCounty();
    String  getCity();
    String  getPostalCode();

    String  getEmail();
    String  getPhone();
    String  getFax();
    String  getIban();
    String  getDistrib2();
    String  getAcordEmail();

    String  getPdfUrl();
    Long    getNrBorderou();
    String  getAdminEdit();
}