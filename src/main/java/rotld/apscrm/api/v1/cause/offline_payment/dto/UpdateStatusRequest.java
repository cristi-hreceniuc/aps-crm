package rotld.apscrm.api.v1.cause.offline_payment.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateStatusRequest {
    private String status; // "approved" | "rejected"
}