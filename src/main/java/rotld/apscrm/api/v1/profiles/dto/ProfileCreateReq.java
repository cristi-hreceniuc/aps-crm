package rotld.apscrm.api.v1.profiles.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ProfileCreateReq(
        String name,
        String avatarUri,
        @NotNull LocalDate birthday,
        @NotNull String gender
) {}
