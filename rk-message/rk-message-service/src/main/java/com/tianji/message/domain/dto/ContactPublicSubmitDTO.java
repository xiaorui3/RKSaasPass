package com.tianji.message.domain.dto;

import lombok.Data;

@Data
public class ContactPublicSubmitDTO {

    private String name;

    private String email;

    private String targetEmail;

    private String verificationCode;

    private String phone;

    private String subject;

    private String message;

    private String shieldToken;

    private Long issuedAt;

    private String honeypot;
}
