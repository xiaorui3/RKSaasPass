package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.dto.AdmissionFormConfigDTO;
import com.tianji.user.domain.dto.AdmissionFormFieldConfigDTO;
import com.tianji.user.domain.dto.AdmissionFormFieldOptionDTO;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.mapper.SystemConfigMapper;
import com.tianji.user.service.IAdmissionFormConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdmissionFormConfigServiceImpl implements IAdmissionFormConfigService {

    private static final String CONFIG_KEY = "admission.form.config";
    private static final String SOURCE_BUILTIN = "builtin";
    private static final String SOURCE_CUSTOM = "custom";

    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AdmissionFormConfigDTO getCurrentTenantConfig() {
        Long tenantId = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L;
        return getTenantConfig(tenantId);
    }

    @Override
    public AdmissionFormConfigDTO getTenantConfig(Long tenantId) {
        return executeWithTenantContext(tenantId, () -> {
            SystemConfig config = queryByTenantAndKey(tenantId);
            if (config == null || config.getConfigValue() == null || config.getConfigValue().isEmpty()) {
                return buildDefaultConfig();
            }

            try {
                AdmissionFormConfigDTO dto = objectMapper.readValue(config.getConfigValue(), AdmissionFormConfigDTO.class);
                return normalizeConfig(dto);
            } catch (Exception e) {
                log.error("parse admission form config failed, tenantId={}", tenantId, e);
                return buildDefaultConfig();
            }
        });
    }

    @Override
    @Transactional
    public AdmissionFormConfigDTO saveCurrentTenantConfig(AdmissionFormConfigDTO dto) {
        Long tenantId = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L;
        try {
            AdmissionFormConfigDTO normalized = normalizeConfig(dto);
            SystemConfig config = queryByTenantAndKey(tenantId);
            if (config == null) {
                config = new SystemConfig();
                config.setTenantId(tenantId);
                config.setConfigKey(CONFIG_KEY);
                config.setDescription("入社表单配置");
                config.setIsEnabled(true);
                config.setCreateTime(LocalDateTime.now());
            }
            config.setConfigValue(objectMapper.writeValueAsString(normalized));
            config.setUpdateTime(LocalDateTime.now());

            if (config.getId() == null) {
                systemConfigMapper.insert(config);
            } else {
                systemConfigMapper.updateById(config);
            }
            return normalized;
        } catch (Exception e) {
            throw new RuntimeException("保存入社表单配置失败: " + e.getMessage(), e);
        }
    }

    private SystemConfig queryByTenantAndKey(Long tenantId) {
        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfig::getTenantId, tenantId)
                .eq(SystemConfig::getConfigKey, CONFIG_KEY)
                .last("LIMIT 1");
        return systemConfigMapper.selectOne(queryWrapper);
    }

    private AdmissionFormConfigDTO normalizeConfig(AdmissionFormConfigDTO raw) {
        AdmissionFormConfigDTO dto = new AdmissionFormConfigDTO();
        dto.setPageTitle(isBlank(raw == null ? null : raw.getPageTitle()) ? "加入我们" : raw.getPageTitle());
        dto.setPageDescription(isBlank(raw == null ? null : raw.getPageDescription())
                ? "选择目标社团后提交入社申请，审核通过后即可进一步参与社团活动。"
                : raw.getPageDescription());
        dto.setSuccessMessage(isBlank(raw == null ? null : raw.getSuccessMessage())
                ? "申请提交成功，请等待负责人审核"
                : raw.getSuccessMessage());

        Map<String, AdmissionFormFieldConfigDTO> builtin = buildBuiltinFieldCatalog().stream()
                .collect(Collectors.toMap(AdmissionFormFieldConfigDTO::getKey, this::cloneField, (left, right) -> left, LinkedHashMap::new));

        List<AdmissionFormFieldConfigDTO> incoming = raw != null && raw.getFields() != null ? raw.getFields() : List.of();
        List<AdmissionFormFieldConfigDTO> customFields = new ArrayList<>();
        for (AdmissionFormFieldConfigDTO field : incoming) {
            if (field == null || isBlank(field.getKey())) {
                continue;
            }
            String source = isBlank(field.getSource()) ? (builtin.containsKey(field.getKey()) ? SOURCE_BUILTIN : SOURCE_CUSTOM) : field.getSource();
            if (SOURCE_CUSTOM.equalsIgnoreCase(source)) {
                customFields.add(normalizeField(field, customFields.size() + 100));
                continue;
            }
            AdmissionFormFieldConfigDTO base = builtin.get(field.getKey());
            if (base != null) {
                mergeField(base, field);
            }
        }

        List<AdmissionFormFieldConfigDTO> fields = new ArrayList<>(builtin.values());
        fields.addAll(customFields);
        fields.sort(Comparator.comparing(field -> field.getSort() == null ? Integer.MAX_VALUE : field.getSort()));
        dto.setFields(fields);
        return dto;
    }

    private void mergeField(AdmissionFormFieldConfigDTO base, AdmissionFormFieldConfigDTO incoming) {
        if (!isBlank(incoming.getLabel())) {
            base.setLabel(incoming.getLabel());
        }
        if (!isBlank(incoming.getPlaceholder())) {
            base.setPlaceholder(incoming.getPlaceholder());
        }
        if (incoming.getEnabled() != null) {
            base.setEnabled(incoming.getEnabled());
        }
        if (incoming.getRequired() != null) {
            base.setRequired(incoming.getRequired());
        }
        if (!isBlank(incoming.getType())) {
            base.setType(incoming.getType());
        }
        if (incoming.getSort() != null) {
            base.setSort(incoming.getSort());
        }
        if (incoming.getOptions() != null) {
            base.setOptions(incoming.getOptions());
        }
    }

    private List<AdmissionFormFieldConfigDTO> buildBuiltinFieldCatalog() {
        List<AdmissionFormFieldConfigDTO> fields = new ArrayList<>();
        fields.add(field("name", "姓名", "请输入姓名", "text", true, true, 10, List.of()));
        fields.add(field("studentId", "学号", "请输入学号", "text", true, true, 20, List.of()));
        fields.add(field("college", "学院", "请输入所在学院", "text", true, false, 30, List.of()));
        fields.add(field("major", "专业", "请输入专业", "text", true, true, 40, List.of()));
        fields.add(field("grade", "年级", "请选择年级", "select", true, true, 50, gradeOptions()));
        fields.add(field("phone", "联系电话", "请输入联系电话", "text", true, true, 60, List.of()));
        fields.add(field("email", "邮箱", "请输入常用邮箱", "text", true, true, 70, List.of()));
        fields.add(field("interests", "技术方向", "请至少选择一个技术方向", "checkbox", true, true, 80, interestOptions()));
        fields.add(field("positionIntent", "职位意向", "请输入想参与的职位或方向", "text", true, false, 90, List.of()));
        fields.add(field("specialty", "个人特长", "请输入你的特长或优势", "textarea", true, false, 100, List.of()));
        fields.add(field("availableTime", "可投入时间", "请输入每周可投入的时间", "text", true, false, 110, List.of()));
        fields.add(field("portfolioUrl", "作品链接", "请输入作品或个人主页链接", "url", true, false, 120, List.of()));
        fields.add(field("intro", "自我介绍", "请介绍你的技术背景、参与经历以及加入原因", "textarea", true, true, 130, List.of()));
        fields.add(field("remark", "补充说明", "可补充其他说明", "textarea", true, false, 140, List.of()));
        return fields;
    }

    private AdmissionFormFieldConfigDTO field(String key, String label, String placeholder, String type,
                                              boolean enabled, boolean required, int sort,
                                              List<AdmissionFormFieldOptionDTO> options) {
        AdmissionFormFieldConfigDTO field = new AdmissionFormFieldConfigDTO();
        field.setKey(key);
        field.setLabel(label);
        field.setPlaceholder(placeholder);
        field.setEnabled(enabled);
        field.setRequired(required);
        field.setSource(SOURCE_BUILTIN);
        field.setType(type);
        field.setSort(sort);
        field.setOptions(options);
        return field;
    }

    private AdmissionFormFieldConfigDTO normalizeField(AdmissionFormFieldConfigDTO raw, int fallbackSort) {
        AdmissionFormFieldConfigDTO field = new AdmissionFormFieldConfigDTO();
        field.setKey(raw.getKey());
        field.setLabel(isBlank(raw.getLabel()) ? raw.getKey() : raw.getLabel());
        field.setPlaceholder(isBlank(raw.getPlaceholder()) ? "" : raw.getPlaceholder());
        field.setEnabled(raw.getEnabled() == null || raw.getEnabled());
        field.setRequired(raw.getRequired() != null && raw.getRequired());
        field.setSource(isBlank(raw.getSource()) ? SOURCE_CUSTOM : raw.getSource());
        field.setType(isBlank(raw.getType()) ? "text" : raw.getType());
        field.setSort(raw.getSort() == null ? fallbackSort : raw.getSort());
        field.setOptions(raw.getOptions() == null ? List.of() : raw.getOptions());
        return field;
    }

    private AdmissionFormFieldConfigDTO cloneField(AdmissionFormFieldConfigDTO source) {
        AdmissionFormFieldConfigDTO field = new AdmissionFormFieldConfigDTO();
        field.setKey(source.getKey());
        field.setLabel(source.getLabel());
        field.setPlaceholder(source.getPlaceholder());
        field.setEnabled(source.getEnabled());
        field.setRequired(source.getRequired());
        field.setSource(source.getSource());
        field.setType(source.getType());
        field.setSort(source.getSort());
        field.setOptions(source.getOptions() == null ? List.of() : new ArrayList<>(source.getOptions()));
        return field;
    }

    private List<AdmissionFormFieldOptionDTO> gradeOptions() {
        return List.of(option("1", "大一"), option("2", "大二"), option("3", "大三"), option("4", "大四"));
    }

    private List<AdmissionFormFieldOptionDTO> interestOptions() {
        return List.of(
                option("frontend", "前端开发"),
                option("backend", "后端开发"),
                option("mobile", "移动开发"),
                option("data", "数据分析"),
                option("ai", "人工智能")
        );
    }

    private AdmissionFormFieldOptionDTO option(String value, String label) {
        AdmissionFormFieldOptionDTO option = new AdmissionFormFieldOptionDTO();
        option.setValue(value);
        option.setLabel(label);
        return option;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private AdmissionFormConfigDTO buildDefaultConfig() {
        return normalizeConfig(new AdmissionFormConfigDTO());
    }

    private <T> T executeWithTenantContext(Long tenantId, Supplier<T> supplier) {
        Long previousTenantId = TenantContext.getTenantId();
        Boolean previousSuperAdmin = TenantContext.isSuperAdmin();
        try {
            if (tenantId == null) {
                TenantContext.removeTenantId();
            } else {
                TenantContext.setTenantId(tenantId);
            }
            TenantContext.setSuperAdmin(false);
            return supplier.get();
        } finally {
            if (previousTenantId == null) {
                TenantContext.removeTenantId();
            } else {
                TenantContext.setTenantId(previousTenantId);
            }
            TenantContext.setSuperAdmin(previousSuperAdmin);
        }
    }
}
