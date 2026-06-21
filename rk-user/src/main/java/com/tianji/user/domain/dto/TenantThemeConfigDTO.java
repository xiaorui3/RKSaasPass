package com.tianji.user.domain.dto;

import lombok.Data;

@Data
public class TenantThemeConfigDTO {
    private String frontendTheme;
    private String adminTheme;
    private String frontendStyle;
    private String adminStyle;
    private String frontendBgColor;
    private String frontendBgImage;
    private Integer frontendBgOpacity;
    private String adminBgColor;
    private String adminBgImage;
    private Integer adminBgOpacity;
}
