package com.tianji.auth.domain.vo;

import com.tianji.auth.domain.po.Menu;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description = "菜单选项实体")
public class MenuOptionVO {
    @ApiModelProperty(value = "菜单id", example = "1")
    private Long id;

    @ApiModelProperty(value = "父菜单id", example = "0")
    private Long parentId;

    @ApiModelProperty(value = "菜单文本", example = "系统管理")
    private String label;

    @ApiModelProperty(value = "菜单编码", example = "admin_system")
    private String menuCode;

    @ApiModelProperty(value = "菜单类型：1-目录，2-菜单", example = "2")
    private Integer menuType;

    @ApiModelProperty(value = "菜单图标", example = "el-icon-sys")
    private String icon;

    @ApiModelProperty(value = "菜单路径", example = "/system/users")
    private String path;

    @ApiModelProperty(value = "前端组件", example = "admin/system/Users")
    private String component;

    @ApiModelProperty(value = "是否有子菜单", example = "false")
    private Boolean hasChildren;

    @ApiModelProperty(value = "菜单顺序", example = "1")
    private Integer priority;

    @ApiModelProperty(value = "是否可见：1-可见，0-隐藏", example = "1")
    private Integer visible;

    @ApiModelProperty(value = "状态：1-启用，0-停用", example = "1")
    private Integer status;

    @ApiModelProperty(value = "子菜单集合")
    private List<MenuOptionVO> subMenus;

    public MenuOptionVO() {
    }

    public MenuOptionVO(Menu menu) {
        this.id = menu.getId();
        this.parentId = menu.getParentId();
        this.label = menu.getLabel();
        this.menuCode = menu.getMenuCode();
        this.menuType = menu.getMenuType();
        this.icon = menu.getIcon();
        this.path = menu.getPath();
        this.component = menu.getComponent();
        this.hasChildren = menu.getHasChildren();
        this.priority = menu.getPriority();
        this.visible = menu.getVisible();
        this.status = menu.getStatus();
    }
}
