package com.tianji.user.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionFilterPublicEndpointTest {

    @Test
    void isPublicEndpoint_shouldAllowAlumniProfileForm() {
        PermissionFilter filter = new PermissionFilter();

        Boolean result = ReflectionTestUtils.invokeMethod(
                filter,
                "isPublicEndpoint",
                "/api/alumni/profile-form/token-1"
        );

        assertTrue(Boolean.TRUE.equals(result));
    }
}
