package com.tianji.api.dto.auth;

import lombok.Data;

@Data
public class CurrentUserPasswordUpdateDTO {

    private String oldPassword;

    private String newPassword;
}
