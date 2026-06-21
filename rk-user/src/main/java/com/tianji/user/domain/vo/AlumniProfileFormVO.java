package com.tianji.user.domain.vo;

import lombok.Data;

@Data
public class AlumniProfileFormVO {
    private String token;
    private Boolean submitted;
    private Long alumniId;
    private Long tenantId;
    private String name;
    private String studentId;
    private String email;
    private String major;
    private String department;
    private String position;
    private String workCity;
    private String workUnit;
    private String jobContent;
    private String currentContact;
    private String skills;
    private String honorCertificates;
    private String notes;
    private String advice;
    private Boolean showTable;
}
