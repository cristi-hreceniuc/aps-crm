package rotld.apscrm.api.v1.kpi.dto;

import lombok.*;

import lombok.*;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class KpiResponseDto {
    private Volunteers volunteers;
    private F177 f177;
    private Sponsorship sponsorship;
    private F230 f230;
    private Iban iban;
    private Causes causes;
    private Persoane persoane;

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Volunteers {
        private Long total;
        private Double avgAge;
        private Double avgDisponibilityHours;
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class F177 {
        private Long companies;
        private Long totalAmount;       // to date
        private Long lastMonthAmount;   // previous calendar month
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Sponsorship {
        private Long totalAmount;       // to date (aps_sponsorship)
        private Long lastMonthAmount;
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class F230 {
        private Long total1y;
        private Long total2y;
        private Long expiringThisYear;  // anul == current year
        private Long thisMonth;         // post_date in current month
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Iban {
        private Long total;
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Causes {
        private Long total;
        private Long reachedGoal;       // donated >= goal
        private Double avgProgressPct;  // avg(donated/goal)*100  (cap at 100)
        private String nextCampaignDate; // min(post_date) > now OR latest if none in future (ISO)
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Persoane {
        private Long users;
        private Long admins;
    }
}