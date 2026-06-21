package com.tianji.message.domain.query;

import com.tianji.common.domain.query.PageQuery;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(description = "邮件模板分页查询")
public class EmailTemplatePageQuery extends PageQuery {
    private Integer status;
    private String keyword;
}
