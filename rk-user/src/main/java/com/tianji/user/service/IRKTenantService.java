package com.tianji.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.user.domain.dto.TenantDTO;
import com.tianji.user.domain.po.RKTenant;
import com.tianji.user.domain.query.TenantPageQuery;
import com.tianji.user.domain.vo.TenantVO;

import java.util.List;
import java.util.Map;

/**
 * 租户服务接口
 */
public interface IRKTenantService extends IService<RKTenant> {

    /**
     * 获取所有启用的租户列表
     */
    List<RKTenant> getActiveTenants();

    /**
     * 分页查询租户列表
     */
    Page<TenantVO> pageTenants(TenantPageQuery query);

    /**
     * 获取租户详情
     */
    TenantVO getTenantDetail(Long tenantId);

    /**
     * 创建租户
     */
    Long createTenant(TenantDTO dto);

    /**
     * 更新租户
     */
    boolean updateTenant(Long tenantId, TenantDTO dto);

    /**
     * 更新租户状态
     */
    boolean updateTenantStatus(Long tenantId, Integer status);

    /**
     * 删除租户
     */
    boolean deleteTenant(Long tenantId);

    /**
     * 获取租户初始化状态
     */
    Map<String, Object> getTenantInitStatus(Long tenantId);

    /**
     * 重新初始化租户
     */
    boolean reinitTenant(Long tenantId);

    /**
     * 修复租户数据
     */
    boolean repairTenant(Long tenantId);

    List<GlobalSearchDocumentDTO> exportSearchDocuments();
}
