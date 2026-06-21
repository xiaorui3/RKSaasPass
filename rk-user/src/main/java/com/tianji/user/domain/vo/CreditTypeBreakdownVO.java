package com.tianji.user.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditTypeBreakdownVO {

    private Long typeId;

    private String code;

    private String name;

    private String description;

    private BigDecimal maxCredit;

    private BigDecimal totalCredits;

    private BigDecimal totalHours;

    private Integer recordCount;
}
