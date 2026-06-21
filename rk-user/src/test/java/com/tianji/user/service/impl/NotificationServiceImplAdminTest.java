package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.dto.AdminNotificationSaveDTO;
import com.tianji.user.domain.po.Notification;
import com.tianji.user.domain.po.NotificationRead;
import com.tianji.user.domain.vo.AdminNotificationManageVO;
import com.tianji.user.domain.vo.AdminNotificationOverviewVO;
import com.tianji.user.mapper.NotificationMapper;
import com.tianji.user.mapper.NotificationReadMapper;
import com.tianji.user.mapper.SystemConfigMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplAdminTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationReadMapper notificationReadMapper;

    @Mock
    private SystemConfigMapper systemConfigMapper;

    @InjectMocks
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Notification.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), NotificationRead.class);
        ReflectionTestUtils.setField(service, "baseMapper", notificationMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.removeUser();
    }

    @Test
    void saveAdminNotification_shouldCreateUpgradeNotificationWithMetadataAndOperator() {
        TenantContext.setTenantId(1L);
        UserContext.setUser(9L);
        when(systemConfigMapper.selectOne(any())).thenReturn(null);
        AdminNotificationSaveDTO dto = new AdminNotificationSaveDTO();
        dto.setTenantId(2L);
        dto.setTitle("Android 1.0.1 released");
        dto.setContent("Install the latest package.");
        dto.setType("upgrade");
        dto.setPriority(2);
        dto.setTargetType(0);
        dto.setUpgradeVersion("1.0.1");
        dto.setForceUpgrade(true);
        dto.setDownloadUrl("https://example.test/app.apk");
        dto.setReleaseNotes("Bug fixes");

        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(101L);
            return 1;
        }).when(notificationMapper).insert(any(Notification.class));

        AdminNotificationManageVO result = service.saveAdminNotification(dto);

        assertEquals(101L, result.getId());
        assertEquals(2L, result.getTenantId());
        assertEquals("upgrade", result.getType());
        assertEquals("1.0.1", result.getUpgradeVersion());
        assertTrue(Boolean.TRUE.equals(result.getForceUpgrade()));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(9L, saved.getSenderId());
        assertEquals(9L, saved.getCreator());
        assertTrue(saved.getMetadata().contains("\"upgradeVersion\":\"1.0.1\""));
        assertTrue(saved.getMetadata().contains("\"forceUpgrade\":true"));
    }

    @Test
    void getNotificationList_shouldOnlyReturnTenantVisibleAndTargetedRows() {
        TenantContext.setTenantId(2L);
        UserContext.setUser(8L);

        Notification global = notification(1L, 1L, "system", 0, null);
        Notification tenant = notification(2L, 2L, "system", 0, null);
        Notification targetedToCurrent = notification(3L, 2L, "message", 1, "[8,9]");
        Notification targetedToOther = notification(4L, 2L, "message", 1, "[99]");
        Notification otherTenant = notification(5L, 3L, "system", 0, null);

        when(notificationMapper.selectList(any())).thenReturn(List.of(global, tenant, targetedToCurrent, targetedToOther, otherTenant));
        when(notificationReadMapper.selectList(any())).thenReturn(List.of(new NotificationRead().setNotificationId(2L)));

        List<Map<String, Object>> result = service.getNotificationList(null, null, 8L, 20);

        assertEquals(List.of(1L, 2L, 3L), result.stream().map(item -> (Long) item.get("id")).collect(Collectors.toList()));
        assertEquals(Boolean.TRUE, result.get(1).get("read"));
        assertEquals(Boolean.FALSE, result.get(2).get("read"));
    }

    @Test
    void getAdminNotificationOverview_shouldCountTenantUpgradeAndHighPriorityNotifications() {
        Notification upgrade = notification(1L, 2L, "upgrade", 0, null);
        upgrade.setPriority(2);
        Notification system = notification(2L, 2L, "system", 0, null);
        system.setPriority(0);
        Notification otherTenant = notification(3L, 9L, "upgrade", 0, null);
        otherTenant.setPriority(2);
        when(notificationMapper.selectList(any())).thenReturn(List.of(upgrade, system, otherTenant));

        AdminNotificationOverviewVO overview = service.getAdminNotificationOverview(2L);

        assertEquals(2, overview.getTotalCount());
        assertEquals(1, overview.getUpgradeCount());
        assertEquals(1, overview.getHighPriorityCount());
        assertEquals(2, overview.getSentCount());
    }

    @Test
    void deleteAdminNotification_shouldSoftDeleteById() {
        Notification existing = notification(11L, 1L, "system", 0, null);
        when(notificationMapper.selectById(11L)).thenReturn(existing);
        when(notificationMapper.softDeleteById(any(), any(), any())).thenReturn(1);

        boolean result = service.deleteAdminNotification(11L);

        assertTrue(result);
        verify(notificationMapper).softDeleteById(eq(11L), isNull(), any());
    }

    @Test
    void deleteAdminNotification_shouldFailWhenUpdateAffectsNoRows() {
        Notification existing = notification(11L, 1L, "system", 0, null);
        when(notificationMapper.selectById(11L)).thenReturn(existing);
        when(notificationMapper.softDeleteById(any(), any(), any())).thenReturn(0);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.deleteAdminNotification(11L));

        assertEquals("delete notification failed: no rows affected", error.getMessage());
    }

    @Test
    void deleteAdminNotification_shouldReturnFalseWhenMissing() {
        when(notificationMapper.selectById(11L)).thenReturn(null);

        boolean result = service.deleteAdminNotification(11L);

        assertFalse(result);
    }

    private Notification notification(Long id, Long tenantId, String type, Integer targetType, String targetIds) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setTenantId(tenantId);
        notification.setTitle("notice " + id);
        notification.setContent("content " + id);
        notification.setType(type);
        notification.setPriority(0);
        notification.setTargetType(targetType);
        notification.setTargetIds(targetIds);
        notification.setIsDeleted(0);
        return notification;
    }
}
