package rotld.apscrm.api.v1.d177.dto;

import lombok.Builder;

/**
 * @param date         postDateIso
 * @param companyName  din _aps177_firma.denumire sau fallback title
 * @param fiscalCode   firma.cui
 * @param email        reprezentant.email
 * @param phone        reprezentant.tel
 * @param amount       contract.suma
 * @param contractDate contract.data
 * @param docUrl       buton Descarcă
 * @param detail       vezi
 * @param downloaded   din wp_posts_settings
 */
@Builder
public record D177ResponseDto(Integer id, String date, String companyName, String fiscalCode, String email,
                              String phone, String amount, String contractDate, String docUrl,
                              String detail, String adminEdit, Boolean downloaded, Boolean verified, Boolean corrupt) {
}