package rotld.apscrm.api.v1.profiles.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ProfileCreateReq(
        String name,
        String avatarUri,
        @NotNull
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate birthday,
        @NotNull String gender
) {}
