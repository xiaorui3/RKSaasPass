package com.tianji.message.domain.dto;

import lombok.Data;

@Data
public class ContactPublicShieldDTO {

    private String token;

    private long issuedAt;

    private long minSubmitDelayMs;

    private long expiresInSeconds;
}
