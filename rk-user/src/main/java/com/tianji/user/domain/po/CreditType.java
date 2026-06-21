package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 学分类型实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("credit_type")
public class CreditType extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 学分类型名称
     */
    private String name;

    /**
     * 学分类型编码
     */
    private String code;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大可获学分
     */
    private BigDecimal maxCredit;
}
