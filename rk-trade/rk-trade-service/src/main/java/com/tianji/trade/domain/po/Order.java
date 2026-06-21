package com.tianji.trade.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易订单实体类
 */
@Data
@TableName("rk_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private String productType;
    private Long productId;
    private String productName;
    private BigDecimal amount;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String orderStatus; // PENDING, PAID, SHIPPED, COMPLETED, CANCELLED
    /**
     * 租户ID（企业级多租户标准）
     */
    private Long tenantId;
    private String userId;
    private String consignee;
    private String phone;
    private String address;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleteFlag;
}