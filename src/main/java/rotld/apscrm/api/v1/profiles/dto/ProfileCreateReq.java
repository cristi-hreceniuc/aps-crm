package rotld.apscrm.api.v1.profiles.dto;

import jakarta.validation.constraints.NotNull;

public record ProfileCreateReq(
        String name,
        String avatarUri,
        @NotNull
        String birthday,
        @NotNull String gender
) {}
