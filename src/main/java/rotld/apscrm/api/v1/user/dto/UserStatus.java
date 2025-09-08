package rotld.apscrm.api.v1.user.dto;

import java.util.Arrays;
import java.util.List;

public enum UserStatus {
    ACTIVE,
    PENDING,
    INACTIVE;

    public static List<String> getNames() {
        return Arrays.stream(UserStatus.values())
                .map(Enum::name)
                .toList();
    }
}