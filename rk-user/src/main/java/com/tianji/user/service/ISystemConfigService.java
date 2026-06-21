package com.tianji.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.user.domain.dto.SystemConfigDTO;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.domain.vo.SystemConfigVO;

import java.util.List;

/**
 * 系统配置服务接口
 *
 * @author RK-Web
 * @since 2026-03-29
 */
public interface ISystemConfigService extends IService<SystemConfig> {

    /**
     * 获取当前租户的所有配置列表
     *
     * @return 配置列表
     */
    List<SystemConfigVO> getConfigList();

    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @return 配置值
     */
    String getConfigValue(String configKey);

    /**
     * 根据配置键获取配置对象
     *
     * @param configKey 配置键
     * @return 配置对象
     */
    SystemConfigVO getConfigByKey(String configKey);

    /**
     * 根据ID获取配置对象
     *
     * @param id 配置ID
     * @return 配置对象
     */
    SystemConfigVO getById(Long id);

    /**
     * 创建系统配置
     *
     * @param dto 配置DTO
     * @return 配置ID
     */
    Long createConfig(SystemConfigDTO dto);

    /**
     * 更新系统配置
     *
     * @param id  配置ID
     * @param dto 配置DTO
     * @return 是否成功
     */
    boolean updateConfig(Long id, SystemConfigDTO dto);

    /**
     * 删除系统配置
     *
     * @param id 配置ID
     * @return 是否成功
     */
    boolean deleteConfig(Long id);

    /**
     * 切换配置启用状态
     *
     * @param id 配置ID
     * @return 是否成功
     */
    boolean toggleConfigStatus(Long id);
}
