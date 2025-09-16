package rotld.apscrm.api.v1.borderou.dto;

import rotld.apscrm.api.v1.borderou.repository.CrmSetting;

public record CrmSettingResponseDto(Integer id, String name, String type, String value, String defaultValue) {
    public static CrmSettingResponseDto of(CrmSetting s) {
        return new CrmSettingResponseDto(s.getId(), s.getName(), s.getType(), s.getValue(), s.getDefaultValue());
    }
}