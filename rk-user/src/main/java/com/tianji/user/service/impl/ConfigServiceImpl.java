package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.user.domain.po.Navigation;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.mapper.NavigationMapper;
import com.tianji.user.mapper.SystemConfigMapper;
import com.tianji.user.service.IConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 系统配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements IConfigService {

    private final NavigationMapper navigationMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String getConfigValue(String configKey) {
        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfig::getConfigKey, configKey);
        // MyBatis-Plus的@TableLogic会自动处理isDeleted字段
        
        SystemConfig config = baseMapper.selectOne(queryWrapper);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public List<Map<String, Object>> getNavigationTree() {
        // 获取所有导航菜单
        List<Navigation> navigations = getAllNavigations();
        
        // 构建树形结构
        return buildNavigationTree(navigations, 0L);
    }

    @Override
    public Map<String, Object> getSystemConfig() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取所有配置
        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        // MyBatis-Plus的@TableLogic会自动处理isDeleted字段，无需手动添加条件
        List<SystemConfig> configs = baseMapper.selectList(queryWrapper);
        
        for (SystemConfig config : configs) {
            try {
                String configKey = config.getConfigKey();
                String configValue = config.getConfigValue();
                
                // 尝试解析JSON
                if (configValue != null && configValue.startsWith("{")) {
                    Map<String, Object> valueMap = objectMapper.readValue(configValue, 
                            new TypeReference<Map<String, Object>>() {});
                    result.put(configKey, valueMap);
                } else if (configValue != null && configValue.startsWith("[")) {
                    List<Object> valueList = objectMapper.readValue(configValue, 
                            new TypeReference<List<Object>>() {});
                    result.put(configKey, valueList);
                } else {
                    result.put(configKey, configValue);
                }
            } catch (Exception e) {
                log.warn("解析配置JSON失败: {}", config.getConfigKey(), e);
                result.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        
        return result;
    }

    @Override
    public boolean updateConfig(String configKey, String configValue) {
        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfig::getConfigKey, configKey);
        // MyBatis-Plus的@TableLogic会自动处理isDeleted字段
        
        SystemConfig config = baseMapper.selectOne(queryWrapper);
        if (config == null) {
            // 创建新配置
            config = new SystemConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            return baseMapper.insert(config) > 0;
        } else {
            // 更新配置
            config.setConfigValue(configValue);
            return baseMapper.updateById(config) > 0;
        }
    }

    @Override
    public List<Navigation> getAllNavigations() {
        LambdaQueryWrapper<Navigation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Navigation::getVisible, 1);
        queryWrapper.eq(Navigation::getIsDeleted, 0);
        queryWrapper.orderByAsc(Navigation::getSort);
        
        return navigationMapper.selectList(queryWrapper);
    }

    /**
     * 构建导航树形结构
     */
    private List<Map<String, Object>> buildNavigationTree(List<Navigation> navigations, Long parentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Navigation nav : navigations) {
            if (Objects.equals(nav.getParentId(), parentId)) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", nav.getId());
                node.put("name", nav.getName());
                node.put("path", nav.getPath());
                node.put("icon", nav.getIcon());
                node.put("sort", nav.getSort());
                node.put("visible", nav.getVisible() == 1);
                node.put("target", nav.getTarget());
                
                // 递归获取子节点
                List<Map<String, Object>> children = buildNavigationTree(navigations, nav.getId());
                if (!children.isEmpty()) {
                    node.put("children", children);
                }
                
                result.add(node);
            }
        }
        
        return result;
    }
}
