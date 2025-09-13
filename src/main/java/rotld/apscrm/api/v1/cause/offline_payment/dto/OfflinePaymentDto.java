package rotld.apscrm.api.v1.cause.offline_payment.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OfflinePaymentDto {
    private Integer id;
    private Integer causeId;
    private String  causeTitle;
    private LocalDateTime bookingDate;
    private String  status;
    private Double amount;
    private LocalDateTime paymentDate;
    private String  paymentMethod;
}