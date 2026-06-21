package com.tianji.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.user.domain.dto.AdminNotificationSaveDTO;
import com.tianji.user.domain.dto.NotificationDeliveryConfigDTO;
import com.tianji.user.domain.po.Notification;
import com.tianji.user.domain.vo.AdminNotificationManageVO;
import com.tianji.user.domain.vo.AdminNotificationOverviewVO;

import java.util.List;
import java.util.Map;

/**
 * 通知服务接口
 */
public interface INotificationService extends IService<Notification> {

    /**
     * 获取通知列表
     * @param type 类型(可选)
     * @param read 已读状态(可选)
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 通知列表
     */
    List<Map<String, Object>> getNotificationList(String type, Boolean read, Long userId, Integer limit);

    /**
     * 获取未读通知数量
     * @param userId 用户ID
     * @return 统计数据
     */
    Map<String, Object> getUnreadCount(Long userId);

    /**
     * 标记通知为已读
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean markAsRead(Long notificationId, Long userId);

    /**
     * 标记所有通知为已读
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean markAllAsRead(Long userId);

    /**
     * 检查通知是否已读
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @return 是否已读
     */
    boolean isRead(Long notificationId, Long userId);

    /**
     * 获取通知详情
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @return 通知详情
     */
    Map<String, Object> getNotificationDetail(Long notificationId, Long userId);

    AdminNotificationOverviewVO getAdminNotificationOverview(Long tenantId);

    List<AdminNotificationManageVO> listAdminNotifications(Long tenantId, String type, String keyword, Integer limit);

    AdminNotificationManageVO saveAdminNotification(AdminNotificationSaveDTO dto);

    boolean deleteAdminNotification(Long notificationId);

    NotificationDeliveryConfigDTO getDeliveryConfig(Long tenantId);

    NotificationDeliveryConfigDTO saveDeliveryConfig(NotificationDeliveryConfigDTO dto);
}
