package com.tianji.user.domain.dto;

import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserFormDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void avatarOnlyUpdate_shouldAllowMissingTypeAndBlankCellPhone() {
        UserFormDTO dto = new UserFormDTO();
        dto.setUsername("member_a");
        dto.setCellPhone("");
        dto.setIcon("http://127.0.0.1:9000/rk-bucket/users/avatars/demo-avatar.png");

        assertTrue(validator.validate(dto).isEmpty(), "avatar-only profile update should not require type or a non-blank cellphone");
    }
}
