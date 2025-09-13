package rotld.apscrm.api.v1.cause.offline_payment.repository;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * wp_frmaster_order + wp_posts (titlu cauza)
 * payment_method este extras, dacă există, din payment_info (JSON) sau din booking_detail.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Immutable
@Subselect("""
  SELECT
    o.id AS id,
    o.user_id AS user_id,
    o.cause_id AS cause_id,
    p.post_title AS cause_title,
    o.booking_date AS booking_date,
    CASE order_status
              WHEN 'pending' THEN 'În așteptare'
              WHEN 'approved' THEN 'Aprobat'
              WHEN 'rejected' THEN 'Respins'
              WHEN 'online-paid' THEN 'Plătit online'
              ELSE order_status
            END AS order_status,
    o.donation_amount AS amount,
    NULLIF(CAST(o.payment_date AS CHAR), '0000-00-00 00:00:00') AS payment_date,    
    COALESCE(
      JSON_UNQUOTE(JSON_EXTRACT(o.payment_info, '$.payment_method')),
      CASE
        WHEN o.booking_detail LIKE '%"donation-method":"online"%'  THEN 'online'
        WHEN o.booking_detail LIKE '%"donation-method":"offline"%' THEN 'offline'
        ELSE NULL
      END
    ) AS payment_method
  FROM wordpress.wp_frmaster_order o
  LEFT JOIN wordpress.wp_posts p ON p.ID = o.cause_id
""")

public class OfflinePaymentView {
    @Id @Column(name="id") private Integer id;
    @Column(name="user_id") private Integer userId;
    @Column(name="cause_id") private Integer causeId;
    @Column(name="cause_title") private String causeTitle;
    @Column(name="booking_date") private LocalDateTime bookingDate;
    @Column(name="order_status") private String orderStatus;
    @Column(name="amount") private Double amount;
    @Column(name="payment_method") private String paymentMethod;
    @Column(name="payment_date") private LocalDateTime paymentDate;

    @JsonProperty("order_status")
    public String getOrderStatus() {
        if (orderStatus == null) return "";
        switch (orderStatus.toLowerCase()) {
            case "pending": return "În așteptare";
            case "approved": return "Aprobat";
            case "rejected": return "Respins";
            case "online-paid": return "Plătit online";
            case "offline": return "Plată offline";
            default: return orderStatus;
        }
    }
}
