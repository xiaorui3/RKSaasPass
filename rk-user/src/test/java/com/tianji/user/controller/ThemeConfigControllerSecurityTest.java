package com.tianji.user.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ThemeConfigControllerSecurityTest {

    @Test
    void getCurrent_shouldRequireAuthenticatedUserInsteadOfAdminConfigPermission() throws Exception {
        Method method = ThemeConfigController.class.getMethod("getCurrent");
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("isAuthenticated()", preAuthorize.value());
    }
}
