package com.tianji.api.dto.auth;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserProvisionDTO {
    private Long authUserId;
    private Long tenantId;
    private String username;
    private String password;
    private String name;
    private String cellPhone;
    private String email;
    private Integer type;
    private Long roleId;
    private Integer status;
    private String studentId;
    private String college;
    private String major;
    private String grade;
    private String department;
    private String position;
    private LocalDateTime joinDate;
}
