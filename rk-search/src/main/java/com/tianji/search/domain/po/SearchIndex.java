package com.tianji.search.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 搜索索引实体类
 */
@Data
@TableName("rk_search_index")
public class SearchIndex {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String content;
    private String entityType; // NEWS, ACTIVITY, MEMBER等
    private Long entityId;
    /**
     * 租户ID（企业级多租户标准）
     */
    private Long tenantId;
    private String tags;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}