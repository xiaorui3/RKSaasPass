package com.tianji.user.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsReadPermissionTest {

    @Test
    void scopedUserStatistics_shouldAllowContentStatisticsViewPermission() throws Exception {
        Method method = UserController.class.getMethod("queryScopedUserStatistics", Long.class);

        assertAllowsContentStatisticsView(method);
    }

    @Test
    void admissionStatistics_shouldAllowContentStatisticsViewPermission() throws Exception {
        Method method = AdmissionController.class.getMethod("getApplicationStatistics");

        assertAllowsContentStatisticsView(method);
    }

    @Test
    void registerReviewStatistics_shouldAllowContentStatisticsViewPermission() throws Exception {
        Method method = RegisterReviewAdminController.class.getMethod("statistics");

        assertAllowsContentStatisticsView(method);
    }

    private static void assertAllowsContentStatisticsView(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertTrue(preAuthorize != null, "statistics endpoint must declare @PreAuthorize");
        assertTrue(
                preAuthorize.value().contains("content:statistics:view"),
                "statistics endpoint must be readable by dashboard roles"
        );
    }
}
