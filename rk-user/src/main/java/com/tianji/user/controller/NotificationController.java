package com.tianji.user.controller;

import com.tianji.common.domain.R;
import com.tianji.api.dto.user.NotificationInternalSaveDTO;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.dto.AdminNotificationSaveDTO;
import com.tianji.user.domain.dto.NotificationDeliveryConfigDTO;
import com.tianji.user.domain.vo.AdminNotificationManageVO;
import com.tianji.user.domain.vo.AdminNotificationOverviewVO;
import com.tianji.user.service.INotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "Notification API")
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    @ApiOperation("List current user notifications")
    @GetMapping("/api/notifications")
    public R<List<Map<String, Object>>> getNotifications(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        return R.ok(notificationService.getNotificationList(type, read, getCurrentUserId(), limit));
    }

    @ApiOperation("Get current user unread notification count")
    @GetMapping("/api/notifications/unread-count")
    public R<Map<String, Object>> getUnreadCount() {
        return R.ok(notificationService.getUnreadCount(getCurrentUserId()));
    }

    @ApiOperation("Mark notification as read")
    @PutMapping("/api/notifications/{notificationId}/read")
    public R<String> markAsRead(@PathVariable Long notificationId) {
        boolean success = notificationService.markAsRead(notificationId, getCurrentUserId());
        return success ? R.ok("marked") : R.error("notification not found");
    }

    @ApiOperation("Mark all notifications as read")
    @PutMapping("/api/notifications/read-all")
    public R<String> markAllAsRead() {
        notificationService.markAllAsRead(getCurrentUserId());
        return R.ok("marked");
    }

    @ApiOperation("Delete notification")
    @DeleteMapping("/api/notifications/{notificationId}")
    public R<String> deleteNotification(@PathVariable Long notificationId) {
        boolean success = notificationService.removeById(notificationId);
        return success ? R.ok("deleted") : R.error("notification not found");
    }

    @ApiOperation("Get notification detail")
    @GetMapping("/api/notifications/{notificationId}")
    public R<Map<String, Object>> getNotificationDetail(@PathVariable Long notificationId) {
        Map<String, Object> notification = notificationService.getNotificationDetail(notificationId, getCurrentUserId());
        return notification == null ? R.error("notification not found") : R.ok(notification);
    }

    @ApiOperation("Admin notification overview")
    @GetMapping("/admin/notifications/overview")
    public R<AdminNotificationOverviewVO> getAdminNotificationOverview(@RequestParam(required = false) Long tenantId) {
        return R.ok(notificationService.getAdminNotificationOverview(tenantId));
    }

    @ApiOperation("Admin notification delivery config")
    @GetMapping("/admin/notifications/delivery-config")
    public R<NotificationDeliveryConfigDTO> getDeliveryConfig(@RequestParam(required = false) Long tenantId) {
        return R.ok(notificationService.getDeliveryConfig(tenantId));
    }

    @ApiOperation("Save admin notification delivery config")
    @PostMapping("/admin/notifications/delivery-config")
    public R<NotificationDeliveryConfigDTO> saveDeliveryConfig(@RequestBody NotificationDeliveryConfigDTO dto) {
        return R.ok(notificationService.saveDeliveryConfig(dto));
    }

    @ApiOperation("Admin notification list")
    @GetMapping("/admin/notifications")
    public R<List<AdminNotificationManageVO>> listAdminNotifications(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        return R.ok(notificationService.listAdminNotifications(tenantId, type, keyword, limit));
    }

    @ApiOperation("Save or replace admin notification")
    @PostMapping("/admin/notifications")
    public R<AdminNotificationManageVO> saveAdminNotification(@RequestBody AdminNotificationSaveDTO dto) {
        return R.ok(notificationService.saveAdminNotification(dto));
    }

    @ApiOperation("Save notification for internal service calls")
    @PostMapping("/api/notifications/internal/save")
    public Boolean saveNotificationInternal(@RequestBody NotificationInternalSaveDTO dto) {
        try {
            AdminNotificationSaveDTO target = new AdminNotificationSaveDTO();
            if (dto != null) {
                target.setTenantId(dto.getTenantId());
                target.setTitle(dto.getTitle());
                target.setContent(dto.getContent());
                target.setType(dto.getType());
                target.setPriority(dto.getPriority());
                target.setTargetType(dto.getTargetType());
                target.setTargetIds(dto.getTargetIds());
                target.setSenderName(dto.getSenderName());
                target.setMetadata(dto.getMetadata());
            }
            notificationService.saveAdminNotification(target);
            return true;
        } catch (Exception e) {
            log.warn("internal save notification failed, title={}, reason={}",
                    dto == null ? null : dto.getTitle(), e.getMessage());
            return false;
        }
    }

    @ApiOperation("Delete admin notification")
    @PostMapping("/admin/notifications/{notificationId}/delete")
    public R<Boolean> deleteAdminNotification(@PathVariable Long notificationId) {
        boolean success = notificationService.deleteAdminNotification(notificationId);
        return success ? R.ok(true) : R.error("notification not found");
    }

    private Long getCurrentUserId() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new IllegalStateException("current user is not logged in");
        }
        return userId;
    }
}
