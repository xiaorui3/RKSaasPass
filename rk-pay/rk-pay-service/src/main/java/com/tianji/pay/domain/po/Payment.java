package com.tianji.pay.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单实体类
 */
@Data
@TableName("rk_payment")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private String businessType;
    private Long businessId;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus; // PENDING, SUCCESS, FAILED, REFUNDED
    private String transactionId;
    /**
     * 租户ID（企业级多租户标准）
     */
    private Long tenantId;
    private String userId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleteFlag;
}