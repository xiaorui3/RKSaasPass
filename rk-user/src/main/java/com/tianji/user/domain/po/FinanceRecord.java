package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("finance_record")
public class FinanceRecord extends TenantAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 类型：1-收入，2-支出
     */
    private Integer type;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 分类
     */
    private String category;

    /**
     * 单据编号
     */
    private String recordNo;

    /**
     * 业务类型：INCOME/EXPENSE/REIMBURSEMENT/BUDGET/PAYMENT
     */
    private String businessType;

    /**
     * 关联业务ID，如活动、报销或采购单ID
     */
    private Long businessId;

    /**
     * 预算项目
     */
    private String budgetItem;

    /**
     * 会计期间，格式 yyyy-MM
     */
    private String period;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 凭证图片URL
     */
    private String proofImageUrl;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 审核人ID
     */
    private Long reviewerId;

    /**
     * 驳回原因
     */
    private String rejectReason;

    /**
     * 入账状态：0-未入账，1-已入账
     */
    private Integer postedStatus;

    /**
     * 凭证ID，当前用于外部凭证系统或后续凭证表扩展
     */
    private Long voucherId;

    /**
     * 状态：0-待审核，1-已通过，2-已拒绝
     */
    private Integer status;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;
}
