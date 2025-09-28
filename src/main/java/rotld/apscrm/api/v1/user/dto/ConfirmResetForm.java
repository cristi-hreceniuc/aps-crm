package rotld.apscrm.api.v1.user.dto;

import lombok.Data;

@Data
public class ConfirmResetForm {
    private String token;
    private String password;
    private String confirmPassword;
}