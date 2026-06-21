package com.tianji.user.domain.dto;

import lombok.Data;

@Data
public class AlumniProfileSubmitDTO {
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
