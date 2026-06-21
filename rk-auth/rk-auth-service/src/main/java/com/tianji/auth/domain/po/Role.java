package com.tianji.auth.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import com.tianji.api.dto.auth.RoleDTO;
import com.tianji.common.utils.TenantContext;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 角色表
 * </p>
 *
 * @author 虎哥
 * @since 2022-07-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("role")
@NoArgsConstructor
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final java.util.Set<String> BUILT_IN_ROLE_CODES = java.util.Set.of("ADMIN", "USER", "CLUB_MANAGER", "TEACHER");

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 角色代号，例如：admin
     */
    private String code;

    /**
     * 角色编码
     */
    @TableField("role_code")
    private String roleCode;

    /**
     * 角色名称
     */
    @TableField("role_name")
    private String name;

    /**
     * 角色类型：0-固定角色（不可选）1-自定义角色
     */
    private Integer type;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建者id
     */
    @TableField("creator")
    private Long creater;

    /**
     * 更新者id
     */
    @TableField("updater")
    private Long updater;
    /**
     * 部门id
     */
    private Long depId;

    /**
     * 逻辑删除，默认0
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    public Role(RoleDTO dto) {
        this.id = dto.getId();
        this.code = dto.getCode();
        this.roleCode = dto.getCode(); // roleCode与code相同
        this.name = dto.getName();
        this.type = 1; // 默认设置为自定义角色
        this.tenantId = TenantContext.getTenantId(); // 从租户上下文获取租户ID
    }

    public RoleDTO toDTO(){
        RoleDTO dto = new RoleDTO();
        dto.setId(id);
        dto.setCode(code);
        dto.setName(name);
        dto.setTenantId(tenantId);
        dto.setDepId(depId);
        dto.setType(type);
        String normalizedCode = code == null ? "" : code.trim().toUpperCase(java.util.Locale.ROOT);
        String normalizedRoleCode = roleCode == null ? "" : roleCode.trim().toUpperCase(java.util.Locale.ROOT);
        dto.setBuiltIn(RoleType.CONSTANT.getValue() == (type == null ? RoleType.CUSTOM.getValue() : type)
                || BUILT_IN_ROLE_CODES.contains(normalizedCode)
                || BUILT_IN_ROLE_CODES.contains(normalizedRoleCode));
        return dto;
    }

    @Getter
    public enum RoleType{
        CONSTANT(0, "固定角色"),
        CUSTOM(1, "自定义角色"),
        ;
        @EnumValue
        int value;
        String desc;

        RoleType(int value, String desc) {
            this.value = value;
            this.desc = desc;
        }
    }
}
