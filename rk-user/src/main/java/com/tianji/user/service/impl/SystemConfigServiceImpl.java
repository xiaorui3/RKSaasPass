package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.dto.SystemConfigDTO;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.domain.vo.SystemConfigVO;
import com.tianji.user.mapper.SystemConfigMapper;
import com.tianji.user.service.ISystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现（/admin/config API专用）
 *
 * @author RK-Web
 * @since 2026-03-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements ISystemConfigService {

    @Override
    public List<SystemConfigVO> getConfigList() {
        // 获取当前租户ID
        Long tenantId = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L;
        
        // 查询当前租户的所有启用配置
        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfig::getTenantId, tenantId);
        queryWrapper.eq(SystemConfig::getIsEnabled, true);
        queryWrapper.orderByAsc(SystemConfig::getId);
        
        List<SystemConfig> configs = baseMapper.selectList(queryWrapper);
        
        return configs.stream()
                .map(config -> BeanUtils.copyBean(config, SystemConfigVO.class))
                .collect(Collectors.toList());
    }

    @Override
    public String getConfigValue(String configKey) {
        // 获取当前租户ID
        Long tenantId = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L;
        
        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfig::getTenantId, tenantId);
        queryWrapper.eq(SystemConfig::getConfigKey, configKey);
        queryWrapper.eq(SystemConfig::getIsEnabled, true);
        
        SystemConfig config = baseMapper.selectOne(queryWrapper);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public SystemConfigVO getConfigByKey(String configKey) {
        // 获取当前租户ID
        Long tenantId = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L;
        
        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfig::getTenantId, tenantId);
        queryWrapper.eq(SystemConfig::getConfigKey, configKey);
        
        SystemConfig config = baseMapper.selectOne(queryWrapper);
        return config != null ? BeanUtils.copyBean(config, SystemConfigVO.class) : null;
    }

    @Override
    public SystemConfigVO getById(Long id) {
        SystemConfig config = baseMapper.selectById(id);
        return config != null ? BeanUtils.copyBean(config, SystemConfigVO.class) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConfig(SystemConfigDTO dto) {
        // 获取当前租户ID
        Long tenantId = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L;
        
        // 检查配置键是否已存在
        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfig::getTenantId, tenantId);
        queryWrapper.eq(SystemConfig::getConfigKey, dto.getConfigKey());
        
        SystemConfig existingConfig = baseMapper.selectOne(queryWrapper);
        if (existingConfig != null) {
            throw new RuntimeException("配置键已存在: " + dto.getConfigKey());
        }
        
        // 创建新配置
        SystemConfig config = BeanUtils.copyBean(dto, SystemConfig.class);
        config.setTenantId(tenantId);
        
        baseMapper.insert(config);
        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateConfig(Long id, SystemConfigDTO dto) {
        // 检查配置是否存在
        SystemConfig existingConfig = baseMapper.selectById(id);
        if (existingConfig == null) {
            throw new RuntimeException("配置不存在: " + id);
        }
        
        // 如果修改了configKey，检查新键是否已存在
        if (!existingConfig.getConfigKey().equals(dto.getConfigKey())) {
            LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SystemConfig::getTenantId, existingConfig.getTenantId());
            queryWrapper.eq(SystemConfig::getConfigKey, dto.getConfigKey());
            queryWrapper.ne(SystemConfig::getId, id);
            
            SystemConfig duplicateConfig = baseMapper.selectOne(queryWrapper);
            if (duplicateConfig != null) {
                throw new RuntimeException("配置键已存在: " + dto.getConfigKey());
            }
        }
        
        // 更新配置
        SystemConfig config = BeanUtils.copyBean(dto, SystemConfig.class);
        config.setId(id);
        config.setTenantId(existingConfig.getTenantId());
        
        return baseMapper.updateById(config) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConfig(Long id) {
        // 检查配置是否存在
        SystemConfig config = baseMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("配置不存在: " + id);
        }
        
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleConfigStatus(Long id) {
        // 检查配置是否存在
        SystemConfig config = baseMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("配置不存在: " + id);
        }
        
        // 切换启用状态
        config.setIsEnabled(!config.getIsEnabled());
        return baseMapper.updateById(config) > 0;
    }
}
