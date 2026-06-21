package com.tianji.api.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class CreditGrantDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long tenantId;

    private String sourceType;

    private Long sourceId;

    private String creditTypeCode;

    private BigDecimal creditHours;

    private BigDecimal creditScore;

    private String description;
}
