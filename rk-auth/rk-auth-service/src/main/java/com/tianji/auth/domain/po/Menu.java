package com.tianji.auth.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tianji.auth.domain.dto.MenuDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("menu")
@NoArgsConstructor
public class Menu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    @TableField("parent_id")
    private Long parentId;

    @TableField(exist = false)
    private Boolean hasChildren;

    @TableField("menu_name")
    private String label;

    @TableField("menu_code")
    private String menuCode;

    @TableField("menu_type")
    private Integer menuType;

    private String path;

    @TableField("component")
    private String component;

    private String icon;

    @TableField("sort")
    private Integer priority;

    private Integer visible;

    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    public Menu(MenuDTO dto) {
        this.id = dto.getId();
        this.parentId = dto.getParentId();
        this.label = dto.getLabel();
        this.menuCode = dto.getMenuCode();
        this.menuType = dto.getMenuType();
        this.path = dto.getPath();
        this.component = dto.getComponent();
        this.icon = dto.getIcon();
        this.priority = dto.getPriority();
        this.visible = dto.getVisible();
        this.status = dto.getStatus();
    }

    public MenuDTO toDTO() {
        MenuDTO dto = new MenuDTO();
        dto.setId(id);
        dto.setPath(path);
        dto.setParentId(parentId);
        dto.setLabel(label);
        dto.setMenuCode(menuCode);
        dto.setMenuType(menuType);
        dto.setComponent(component);
        dto.setIcon(icon);
        dto.setPriority(priority);
        dto.setVisible(visible);
        dto.setStatus(status);
        return dto;
    }
}
