package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.common.utils.SearchIndexSyncUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.dto.TenantDTO;
import com.tianji.user.domain.po.RKTenant;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.domain.query.TenantPageQuery;
import com.tianji.user.domain.vo.TenantVO;
import com.tianji.user.mapper.RKTenantMapper;
import com.tianji.user.mapper.SystemConfigMapper;
import com.tianji.user.service.IRKTenantService;
import com.tianji.user.service.ITenantWorkflowConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RKTenantServiceImpl extends ServiceImpl<RKTenantMapper, RKTenant> implements IRKTenantService {

    private static final String SEARCH_ENTITY_TYPE_TENANT = "TENANT";

    private final ITenantWorkflowConfigService tenantWorkflowConfigService;
    private final SystemConfigMapper systemConfigMapper;
    private final SearchClient searchClient;

    @Override
    public List<RKTenant> getActiveTenants() {
        LambdaQueryWrapper<RKTenant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RKTenant::getStatus, 1)
                .eq(RKTenant::getIsDeleted, 0)
                .and(wrapper -> wrapper
                        .isNull(RKTenant::getExpireTime)
                        .or()
                        .gt(RKTenant::getExpireTime, LocalDateTime.now()))
                .orderByAsc(RKTenant::getDisplayOrder)
                .orderByAsc(RKTenant::getId);
        return list(queryWrapper);
    }

    @Override
    public Page<TenantVO> pageTenants(TenantPageQuery query) {
        Page<RKTenant> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<RKTenant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RKTenant::getIsDeleted, 0);
        if (!TenantContext.isSuperAdmin()) {
            Long currentTenantId = TenantContext.getTenantId();
            if (currentTenantId != null) {
                queryWrapper.eq(RKTenant::getId, currentTenantId);
            }
        }

        if (StringUtils.hasText(query.getTenantName())) {
            queryWrapper.like(RKTenant::getTenantName, query.getTenantName());
        }
        if (StringUtils.hasText(query.getTenantCode())) {
            queryWrapper.eq(RKTenant::getTenantCode, query.getTenantCode());
        }
        if (query.getStatus() != null) {
            queryWrapper.eq(RKTenant::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getContactPerson())) {
            queryWrapper.like(RKTenant::getContactPerson, query.getContactPerson());
        }
        if (StringUtils.hasText(query.getContactPhone())) {
            queryWrapper.like(RKTenant::getContactPhone, query.getContactPhone());
        }

        queryWrapper.orderByAsc(RKTenant::getDisplayOrder)
                .orderByAsc(RKTenant::getId);

        Page<RKTenant> tenantPage = page(page, queryWrapper);
        Page<TenantVO> voPage = new Page<>(tenantPage.getCurrent(), tenantPage.getSize(), tenantPage.getTotal());
        List<TenantVO> voList = tenantPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public TenantVO getTenantDetail(Long tenantId) {
        if (!canAccessTenant(tenantId)) {
            return null;
        }
        RKTenant tenant = getById(tenantId);
        if (tenant == null || tenant.getIsDeleted() == 1) {
            return null;
        }
        return convertToVO(tenant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTenant(TenantDTO dto) {
        LambdaQueryWrapper<RKTenant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RKTenant::getTenantCode, dto.getTenantCode())
                .eq(RKTenant::getIsDeleted, 0);
        if (count(queryWrapper) > 0) {
            throw new RuntimeException("租户编码已存在: " + dto.getTenantCode());
        }

        RKTenant tenant = new RKTenant();
        BeanUtils.copyProperties(dto, tenant);
        tenant.setIsDeleted(0);
        tenant.setCreateTime(LocalDateTime.now());
        tenant.setUpdateTime(LocalDateTime.now());
        if (tenant.getStatus() == null) {
            tenant.setStatus(1);
        }
        if (tenant.getDisplayOrder() == null) {
            tenant.setDisplayOrder(0);
        }

        save(tenant);
        initializeTenantResources(tenant.getId(), true);
        syncTenantSearchIndex(tenant);
        log.info("created tenant successfully: id={}, name={}", tenant.getId(), tenant.getTenantName());
        return tenant.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTenant(Long tenantId, TenantDTO dto) {
        enforceTenantScope(tenantId);
        RKTenant tenant = getById(tenantId);
        if (tenant == null || tenant.getIsDeleted() == 1) {
            throw new RuntimeException("租户不存在: " + tenantId);
        }

        if (!tenant.getTenantCode().equals(dto.getTenantCode())) {
            LambdaQueryWrapper<RKTenant> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RKTenant::getTenantCode, dto.getTenantCode())
                    .eq(RKTenant::getIsDeleted, 0)
                    .ne(RKTenant::getId, tenantId);
            if (count(queryWrapper) > 0) {
                throw new RuntimeException("租户编码已存在: " + dto.getTenantCode());
            }
        }

        BeanUtils.copyProperties(dto, tenant);
        tenant.setId(tenantId);
        tenant.setUpdateTime(LocalDateTime.now());

        boolean result = updateById(tenant);
        if (result) {
            syncTenantSearchIndex(tenant);
            log.info("updated tenant successfully: id={}, name={}", tenantId, tenant.getTenantName());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTenantStatus(Long tenantId, Integer status) {
        enforceTenantScope(tenantId);
        RKTenant tenant = getById(tenantId);
        if (tenant == null || tenant.getIsDeleted() == 1) {
            throw new RuntimeException("租户不存在: " + tenantId);
        }

        tenant.setStatus(status);
        tenant.setUpdateTime(LocalDateTime.now());
        boolean result = updateById(tenant);
        if (result) {
            syncTenantSearchIndex(tenant);
            log.info("updated tenant status successfully: id={}, status={}", tenantId, status);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTenant(Long tenantId) {
        enforceTenantScope(tenantId);
        RKTenant tenant = getById(tenantId);
        if (tenant == null) {
            throw new RuntimeException("租户不存在: " + tenantId);
        }

        tenant.setIsDeleted(1);
        tenant.setUpdateTime(LocalDateTime.now());
        boolean result = updateById(tenant);
        if (result) {
            deleteTenantSearchIndex(tenantId);
            log.info("deleted tenant successfully: id={}, name={}", tenantId, tenant.getTenantName());
        }
        return result;
    }

    @Override
    public Map<String, Object> getTenantInitStatus(Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        RKTenant tenant = getById(tenantId);
        if (tenant == null || tenant.getIsDeleted() == 1) {
            result.put("exists", false);
            result.put("message", "租户不存在");
            return result;
        }

        boolean workflowConfigReady = hasTenantWorkflowConfig(tenantId);
        if (!workflowConfigReady && Integer.valueOf(1).equals(tenant.getStatus())) {
            tenantWorkflowConfigService.saveCurrentConfig(tenantId, null);
            workflowConfigReady = true;
        }
        result.put("exists", true);
        result.put("tenantId", tenantId);
        result.put("tenantName", tenant.getTenantName());
        result.put("status", tenant.getStatus());
        result.put("initStatus", workflowConfigReady ? 1 : 0);
        result.put("initTime", tenant.getCreateTime());
        result.put("workflowConfigReady", workflowConfigReady);
        result.put("message", workflowConfigReady ? "租户初始化完成" : "租户缺少默认工作流配置");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reinitTenant(Long tenantId) {
        RKTenant tenant = getById(tenantId);
        if (tenant == null || tenant.getIsDeleted() == 1) {
            throw new RuntimeException("租户不存在: " + tenantId);
        }

        initializeTenantResources(tenantId, true);
        tenant.setUpdateTime(LocalDateTime.now());
        updateById(tenant);
        syncTenantSearchIndex(tenant);
        log.info("reinitialized tenant successfully: id={}, name={}", tenantId, tenant.getTenantName());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean repairTenant(Long tenantId) {
        RKTenant tenant = getById(tenantId);
        if (tenant == null || tenant.getIsDeleted() == 1) {
            throw new RuntimeException("租户不存在: " + tenantId);
        }

        initializeTenantResources(tenantId, false);
        tenant.setUpdateTime(LocalDateTime.now());
        updateById(tenant);
        syncTenantSearchIndex(tenant);
        log.info("repaired tenant successfully: id={}, name={}", tenantId, tenant.getTenantName());
        return true;
    }

    private void initializeTenantResources(Long tenantId, boolean resetWorkflowConfig) {
        if (tenantId == null) {
            throw new RuntimeException("租户初始化失败: 缺少租户ID");
        }
        if (resetWorkflowConfig) {
            tenantWorkflowConfigService.saveCurrentConfig(tenantId, null);
            return;
        }
        tenantWorkflowConfigService.saveCurrentConfig(tenantId, tenantWorkflowConfigService.loadCurrentConfig(tenantId));
    }

    private boolean hasTenantWorkflowConfig(Long tenantId) {
        SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getTenantId, tenantId)
                .eq(SystemConfig::getConfigKey, TenantWorkflowConfigServiceImpl.CONFIG_KEY)
                .last("LIMIT 1"));
        return config != null && !Boolean.TRUE.equals(config.getIsDeleted());
    }

    private boolean isPublicTenant(RKTenant tenant) {
        return tenant != null
                && Integer.valueOf(1).equals(tenant.getStatus())
                && Integer.valueOf(0).equals(tenant.getIsDeleted())
                && (tenant.getExpireTime() == null || tenant.getExpireTime().isAfter(LocalDateTime.now()));
    }

    private void syncTenantSearchIndex(RKTenant tenant) {
        if (tenant == null || tenant.getId() == null) {
            return;
        }
        SearchIndexSyncUtils.runAfterCommit(() -> {
            try {
                RKTenant latest = getById(tenant.getId());
                if (!isPublicTenant(latest)) {
                    searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_TENANT, tenant.getId(), tenant.getId());
                    return;
                }
                searchClient.upsertGlobalDocument(buildTenantSearchDocument(latest));
            } catch (Exception e) {
                log.warn("sync tenant global search index failed, tenantId={}, reason={}", tenant.getId(), e.getMessage());
            }
        });
    }

    private void deleteTenantSearchIndex(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        SearchIndexSyncUtils.runAfterCommit(() -> {
            try {
                searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_TENANT, tenantId, tenantId);
            } catch (Exception e) {
                log.warn("delete tenant global search index failed, tenantId={}, reason={}", tenantId, e.getMessage());
            }
        });
    }

    @Override
    public List<GlobalSearchDocumentDTO> exportSearchDocuments() {
        LambdaQueryWrapper<RKTenant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RKTenant::getStatus, 1)
                .eq(RKTenant::getIsDeleted, 0)
                .and(wrapper -> wrapper
                        .isNull(RKTenant::getExpireTime)
                        .or()
                        .gt(RKTenant::getExpireTime, LocalDateTime.now()))
                .orderByAsc(RKTenant::getDisplayOrder)
                .orderByAsc(RKTenant::getId);
        return list(queryWrapper).stream()
                .filter(this::isPublicTenant)
                .map(this::buildTenantSearchDocument)
                .collect(Collectors.toList());
    }

    private GlobalSearchDocumentDTO buildTenantSearchDocument(RKTenant tenant) {
        return new GlobalSearchDocumentDTO()
                .setEntityType(SEARCH_ENTITY_TYPE_TENANT)
                .setEntityId(tenant.getId())
                .setTenantId(tenant.getId())
                .setTitle(tenant.getTenantName())
                .setSummary(tenant.getDescription())
                .setContent(joinSearchText(tenant.getTenantCode(), tenant.getContactPerson(), tenant.getContactPhone(), tenant.getContactEmail()))
                .setTags(tenant.getTenantCode())
                .setRoute("/join?tenantId=" + tenant.getId())
                .setCoverUrl(tenant.getLogoUrl())
                .setUpdatedAt(formatUpdatedAt(tenant.getUpdateTime(), tenant.getCreateTime()))
                .setVisible(isPublicTenant(tenant));
    }

    private String joinSearchText(String... values) {
        if (values == null) {
            return null;
        }
        return java.util.Arrays.stream(values)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }

    private String formatUpdatedAt(LocalDateTime... times) {
        if (times == null) {
            return null;
        }
        for (LocalDateTime time : times) {
            if (time != null) {
                return time.toString();
            }
        }
        return null;
    }

    private boolean canAccessTenant(Long tenantId) {
        if (TenantContext.isSuperAdmin()) {
            return true;
        }
        Long currentTenantId = TenantContext.getTenantId();
        return currentTenantId != null && currentTenantId.equals(tenantId);
    }

    private void enforceTenantScope(Long tenantId) {
        if (!canAccessTenant(tenantId)) {
            throw new RuntimeException("无权访问当前租户");
        }
    }

    private TenantVO convertToVO(RKTenant tenant) {
        TenantVO vo = new TenantVO();
        BeanUtils.copyProperties(tenant, vo);
        if (tenant.getStatus() != null) {
            vo.setStatusDesc(tenant.getStatus() == 1 ? "正常" : "禁用");
        }
        if (tenant.getExpireTime() != null) {
            vo.setExpired(tenant.getExpireTime().isBefore(LocalDateTime.now()));
        } else {
            vo.setExpired(false);
        }
        return vo;
    }
}
