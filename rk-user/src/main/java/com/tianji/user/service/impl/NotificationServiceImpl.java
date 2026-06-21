package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.dto.AdminNotificationSaveDTO;
import com.tianji.user.domain.dto.NotificationDeliveryConfigDTO;
import com.tianji.user.domain.po.Notification;
import com.tianji.user.domain.po.NotificationRead;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.domain.vo.AdminNotificationManageVO;
import com.tianji.user.domain.vo.AdminNotificationOverviewVO;
import com.tianji.user.mapper.NotificationMapper;
import com.tianji.user.mapper.NotificationReadMapper;
import com.tianji.user.mapper.SystemConfigMapper;
import com.tianji.user.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {

    private final NotificationReadMapper notificationReadMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DELIVERY_CONFIG_KEY = "notification.delivery.config";

    @Override
    public List<Map<String, Object>> getNotificationList(String type, Boolean read, Long userId, Integer limit) {
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(type)) {
            queryWrapper.eq(Notification::getType, type);
        }
        queryWrapper.eq(Notification::getIsDeleted, 0);
        queryWrapper.orderByDesc(Notification::getCreateTime);
        if (limit != null && limit > 0) {
            queryWrapper.last("LIMIT " + Math.min(limit, 200));
        }

        List<Notification> notifications = baseMapper.selectList(queryWrapper);
        Set<Long> readIds = getReadNotificationIds(userId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Notification notification : notifications) {
            if (!isTenantVisible(notification, currentTenantId()) || !isTargetVisible(notification, userId)) {
                continue;
            }
            boolean isRead = readIds.contains(notification.getId());
            if (read != null && read != isRead) {
                continue;
            }
            Map<String, Object> map = convertToMap(notification);
            map.put("read", isRead);
            result.add(map);
        }
        return result;
    }

    @Override
    public Map<String, Object> getUnreadCount(Long userId) {
        List<Map<String, Object>> visible = getNotificationList(null, null, userId, 200);
        long read = visible.stream().filter(item -> Boolean.TRUE.equals(item.get("read"))).count();
        Map<String, Object> result = new HashMap<>();
        result.put("total", visible.size());
        result.put("unread", visible.size() - read);
        result.put("read", read);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAsRead(Long notificationId, Long userId) {
        Notification notification = baseMapper.selectById(notificationId);
        if (notification == null || !isTenantVisible(notification, currentTenantId()) || !isTargetVisible(notification, userId)) {
            return false;
        }
        if (isRead(notificationId, userId)) {
            return true;
        }

        NotificationRead notificationRead = new NotificationRead();
        notificationRead.setNotificationId(notificationId);
        notificationRead.setUserId(userId);
        notificationRead.setReadTime(LocalDateTime.now());
        notificationRead.setTenantId(notification.getTenantId());

        int rows = notificationReadMapper.insert(notificationRead);
        log.info("marked notification as read: notificationId={}, userId={}", notificationId, userId);
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAllAsRead(Long userId) {
        List<Map<String, Object>> notifications = getNotificationList(null, false, userId, 200);
        int count = 0;
        for (Map<String, Object> item : notifications) {
            Long notificationId = (Long) item.get("id");
            NotificationRead notificationRead = new NotificationRead();
            notificationRead.setNotificationId(notificationId);
            notificationRead.setUserId(userId);
            notificationRead.setReadTime(LocalDateTime.now());
            Object tenantId = item.get("tenantId");
            notificationRead.setTenantId(tenantId instanceof Long ? (Long) tenantId : currentTenantId());
            notificationReadMapper.insert(notificationRead);
            count++;
        }
        log.info("marked all notifications as read: userId={}, count={}", userId, count);
        return true;
    }

    @Override
    public boolean isRead(Long notificationId, Long userId) {
        LambdaQueryWrapper<NotificationRead> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NotificationRead::getNotificationId, notificationId);
        queryWrapper.eq(NotificationRead::getUserId, userId);
        return notificationReadMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public Map<String, Object> getNotificationDetail(Long notificationId, Long userId) {
        Notification notification = baseMapper.selectById(notificationId);
        if (notification == null || !isTenantVisible(notification, currentTenantId()) || !isTargetVisible(notification, userId)) {
            return null;
        }
        markAsRead(notificationId, userId);
        Map<String, Object> result = convertToMap(notification);
        result.put("read", true);
        return result;
    }

    @Override
    public AdminNotificationOverviewVO getAdminNotificationOverview(Long tenantId) {
        Long resolvedTenantId = tenantId == null ? currentTenantId() : tenantId;
        List<Notification> notifications = baseMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getIsDeleted, 0)
                        .orderByDesc(Notification::getCreateTime)
        );
        List<Notification> visible = notifications.stream()
                .filter(item -> isAdminTenantMatch(item, resolvedTenantId))
                .collect(Collectors.toList());
        AdminNotificationOverviewVO vo = new AdminNotificationOverviewVO();
        vo.setTotalCount(visible.size());
        vo.setSentCount(visible.size());
        vo.setUpgradeCount((int) visible.stream().filter(item -> "upgrade".equalsIgnoreCase(item.getType())).count());
        vo.setHighPriorityCount((int) visible.stream().filter(item -> item.getPriority() != null && item.getPriority() >= 2).count());
        return vo;
    }

    @Override
    public List<AdminNotificationManageVO> listAdminNotifications(Long tenantId, String type, String keyword, Integer limit) {
        Long resolvedTenantId = tenantId == null ? currentTenantId() : tenantId;
        int resolvedLimit = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
        List<Notification> notifications = baseMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getIsDeleted, 0)
                        .orderByDesc(Notification::getCreateTime)
                        .last("LIMIT " + resolvedLimit)
        );
        return notifications.stream()
                .filter(item -> isAdminTenantMatch(item, resolvedTenantId))
                .filter(item -> StringUtils.isBlank(type) || type.equalsIgnoreCase(item.getType()))
                .filter(item -> matchesKeyword(item, keyword))
                .map(this::toAdminVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminNotificationManageVO saveAdminNotification(AdminNotificationSaveDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getTitle())) {
            throw new IllegalArgumentException("notification title is required");
        }
        if (StringUtils.isBlank(dto.getContent())) {
            throw new IllegalArgumentException("notification content is required");
        }
        Long resolvedTenantId = dto.getTenantId() == null ? currentTenantId() : dto.getTenantId();
        String resolvedType = StringUtils.isBlank(dto.getType()) ? "system" : dto.getType().trim();
        if (!isDeliveryEnabled(resolvedTenantId, resolvedType)) {
            throw new IllegalStateException("notification delivery is disabled for type: " + resolvedType);
        }

        Notification notification = dto.getId() == null ? new Notification() : baseMapper.selectById(dto.getId());
        if (notification == null) {
            throw new IllegalArgumentException("notification does not exist");
        }

        LocalDateTime now = LocalDateTime.now();
        Long operatorId = UserContext.getUser();
        notification.setTenantId(resolvedTenantId);
        notification.setTitle(dto.getTitle().trim());
        notification.setContent(dto.getContent());
        notification.setType(resolvedType);
        notification.setPriority(dto.getPriority() == null ? 0 : dto.getPriority());
        notification.setTargetType(dto.getTargetType() == null ? 0 : dto.getTargetType());
        notification.setTargetIds(toJson(dto.getTargetIds()));
        notification.setMetadata(toJson(buildMetadata(dto)));
        notification.setSenderId(operatorId);
        notification.setSenderName(StringUtils.isBlank(dto.getSenderName()) ? "admin" : dto.getSenderName());
        notification.setUpdater(operatorId);
        notification.setUpdateTime(now);
        notification.setIsDeleted(0);

        if (notification.getId() == null) {
            notification.setCreator(operatorId);
            notification.setCreateTime(now);
            baseMapper.insert(notification);
        } else {
            baseMapper.updateById(notification);
        }
        return toAdminVO(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAdminNotification(Long notificationId) {
        Notification notification = notificationId == null ? null : baseMapper.selectById(notificationId);
        if (notification == null) {
            return false;
        }
        int rows = baseMapper.softDeleteById(notificationId, UserContext.getUser(), LocalDateTime.now());
        if (rows <= 0) {
            throw new IllegalStateException("delete notification failed: no rows affected");
        }
        return true;
    }

    @Override
    public NotificationDeliveryConfigDTO getDeliveryConfig(Long tenantId) {
        Long resolvedTenantId = tenantId == null ? currentTenantId() : tenantId;
        SystemConfig config = findDeliveryConfig(resolvedTenantId);
        NotificationDeliveryConfigDTO dto = parseDeliveryConfig(config == null ? null : config.getConfigValue());
        dto.setTenantId(resolvedTenantId);
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationDeliveryConfigDTO saveDeliveryConfig(NotificationDeliveryConfigDTO dto) {
        Long resolvedTenantId = dto == null || dto.getTenantId() == null ? currentTenantId() : dto.getTenantId();
        NotificationDeliveryConfigDTO normalized = normalizeDeliveryConfig(dto);
        normalized.setTenantId(resolvedTenantId);
        SystemConfig config = findDeliveryConfig(resolvedTenantId);
        LocalDateTime now = LocalDateTime.now();
        if (config == null) {
            config = new SystemConfig();
            config.setTenantId(resolvedTenantId);
            config.setConfigKey(DELIVERY_CONFIG_KEY);
            config.setDescription("notification delivery switches");
            config.setCreateTime(now);
            config.setIsDeleted(false);
        }
        config.setConfigValue(toJson(normalized));
        config.setIsEnabled(Boolean.TRUE.equals(normalized.getEnabled()));
        config.setUpdateTime(now);
        if (config.getId() == null) {
            systemConfigMapper.insert(config);
        } else {
            systemConfigMapper.updateById(config);
        }
        return normalized;
    }

    private Set<Long> getReadNotificationIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<NotificationRead> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NotificationRead::getUserId, userId);
        queryWrapper.select(NotificationRead::getNotificationId);
        return notificationReadMapper.selectList(queryWrapper).stream()
                .map(NotificationRead::getNotificationId)
                .collect(Collectors.toSet());
    }

    private Map<String, Object> convertToMap(Notification notification) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", notification.getId());
        map.put("tenantId", notification.getTenantId());
        map.put("title", notification.getTitle());
        map.put("content", notification.getContent());
        map.put("type", notification.getType());
        map.put("priority", notification.getPriority());
        map.put("senderId", notification.getSenderId());
        map.put("senderName", notification.getSenderName());
        map.put("createTime", notification.getCreateTime());
        map.put("metadata", parseMap(notification.getMetadata()));
        return map;
    }

    private AdminNotificationManageVO toAdminVO(Notification notification) {
        Map<String, Object> metadata = parseMap(notification.getMetadata());
        AdminNotificationManageVO vo = new AdminNotificationManageVO();
        vo.setId(notification.getId());
        vo.setTenantId(notification.getTenantId());
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setType(notification.getType());
        vo.setPriority(notification.getPriority());
        vo.setTargetType(notification.getTargetType() == null ? 0 : notification.getTargetType());
        vo.setTargetIds(parseLongList(notification.getTargetIds()));
        vo.setSenderName(notification.getSenderName());
        vo.setStatus("SENT");
        vo.setReadCount(0);
        vo.setCreateTime(notification.getCreateTime() == null ? null : notification.getCreateTime().toString());
        vo.setUpdateTime(notification.getUpdateTime() == null ? null : notification.getUpdateTime().toString());
        vo.setMetadata(metadata);
        vo.setUpgradeVersion(asString(metadata.get("upgradeVersion")));
        vo.setForceUpgrade(asBoolean(metadata.get("forceUpgrade")));
        vo.setDownloadUrl(asString(metadata.get("downloadUrl")));
        vo.setReleaseNotes(asString(metadata.get("releaseNotes")));
        return vo;
    }

    private boolean isTenantVisible(Notification notification, Long tenantId) {
        Long notificationTenantId = notification.getTenantId() == null ? 1L : notification.getTenantId();
        Long currentTenantId = tenantId == null ? 1L : tenantId;
        return Objects.equals(notificationTenantId, currentTenantId)
                || (Objects.equals(notificationTenantId, 1L)
                && ("system".equalsIgnoreCase(notification.getType()) || "upgrade".equalsIgnoreCase(notification.getType())));
    }

    private boolean isAdminTenantMatch(Notification notification, Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            return true;
        }
        Long notificationTenantId = notification.getTenantId() == null ? 1L : notification.getTenantId();
        return Objects.equals(notificationTenantId, tenantId);
    }

    private boolean isTargetVisible(Notification notification, Long userId) {
        Integer targetType = notification.getTargetType() == null ? 0 : notification.getTargetType();
        if (targetType == 0 || targetType == 2) {
            return true;
        }
        if (targetType != 1 || userId == null) {
            return false;
        }
        return parseLongList(notification.getTargetIds()).contains(userId);
    }

    private boolean isDeliveryEnabled(Long tenantId, String type) {
        NotificationDeliveryConfigDTO config = getDeliveryConfig(tenantId);
        if (Boolean.FALSE.equals(config.getEnabled())) {
            return false;
        }
        String normalizedType = type == null ? "system" : type.trim().toLowerCase();
        if ("activity".equals(normalizedType)) {
            return !Boolean.FALSE.equals(config.getActivityEnabled());
        }
        if ("upgrade".equals(normalizedType)) {
            return !Boolean.FALSE.equals(config.getUpgradeEnabled());
        }
        if ("message".equals(normalizedType)) {
            return !Boolean.FALSE.equals(config.getMessageEnabled());
        }
        return !Boolean.FALSE.equals(config.getSystemEnabled());
    }

    private SystemConfig findDeliveryConfig(Long tenantId) {
        return systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getTenantId, tenantId == null ? 1L : tenantId)
                        .eq(SystemConfig::getConfigKey, DELIVERY_CONFIG_KEY)
                        .last("LIMIT 1")
        );
    }

    private NotificationDeliveryConfigDTO parseDeliveryConfig(String json) {
        if (StringUtils.isBlank(json)) {
            return normalizeDeliveryConfig(null);
        }
        try {
            return normalizeDeliveryConfig(objectMapper.readValue(json, NotificationDeliveryConfigDTO.class));
        } catch (Exception e) {
            return normalizeDeliveryConfig(null);
        }
    }

    private NotificationDeliveryConfigDTO normalizeDeliveryConfig(NotificationDeliveryConfigDTO source) {
        NotificationDeliveryConfigDTO dto = source == null ? new NotificationDeliveryConfigDTO() : source;
        if (dto.getEnabled() == null) dto.setEnabled(true);
        if (dto.getSystemEnabled() == null) dto.setSystemEnabled(true);
        if (dto.getMessageEnabled() == null) dto.setMessageEnabled(true);
        if (dto.getActivityEnabled() == null) dto.setActivityEnabled(true);
        if (dto.getUpgradeEnabled() == null) dto.setUpgradeEnabled(true);
        return dto;
    }

    private boolean matchesKeyword(Notification notification, String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase();
        return String.valueOf(notification.getTitle()).toLowerCase().contains(normalized)
                || String.valueOf(notification.getContent()).toLowerCase().contains(normalized);
    }

    private Map<String, Object> buildMetadata(AdminNotificationSaveDTO dto) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (dto.getMetadata() != null) {
            metadata.putAll(dto.getMetadata());
        }
        putIfNotBlank(metadata, "upgradeVersion", dto.getUpgradeVersion());
        if (dto.getForceUpgrade() != null) {
            metadata.put("forceUpgrade", dto.getForceUpgrade());
        }
        putIfNotBlank(metadata, "downloadUrl", dto.getDownloadUrl());
        putIfNotBlank(metadata, "releaseNotes", dto.getReleaseNotes());
        return metadata;
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value);
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Collection && ((Collection<?>) value).isEmpty()) {
            return null;
        }
        if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("serialize notification metadata failed", e);
        }
    }

    private Map<String, Object> parseMap(String json) {
        if (StringUtils.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private List<Long> parseLongList(String json) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Boolean asBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
