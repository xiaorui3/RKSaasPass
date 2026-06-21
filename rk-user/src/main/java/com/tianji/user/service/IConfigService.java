package com.tianji.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.user.domain.po.Navigation;
import com.tianji.user.domain.po.SystemConfig;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface IConfigService extends IService<SystemConfig> {

    /**
     * 获取配置值
     * @param configKey 配置键
     * @return 配置值(JSON字符串)
     */
    String getConfigValue(String configKey);

    /**
     * 获取导航菜单列表
     * @return 导航菜单树形结构
     */
    List<Map<String, Object>> getNavigationTree();

    /**
     * 获取系统配置
     * @return 系统配置Map
     */
    Map<String, Object> getSystemConfig();

    /**
     * 更新配置
     * @param configKey 配置键
     * @param configValue 配置值
     * @return 是否成功
     */
    boolean updateConfig(String configKey, String configValue);

    /**
     * 获取所有导航菜单(平铺)
     * @return 导航菜单列表
     */
    List<Navigation> getAllNavigations();
}
