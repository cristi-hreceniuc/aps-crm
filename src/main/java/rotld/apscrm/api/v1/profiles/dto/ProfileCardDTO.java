package rotld.apscrm.api.v1.profiles.dto;

public record ProfileCardDTO(
        Long id, String name, String avatarUri,
        boolean premium, int progressPercent,
        long completedLessons, long totalLessons
) {}