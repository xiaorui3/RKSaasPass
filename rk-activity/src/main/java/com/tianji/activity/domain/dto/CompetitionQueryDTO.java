package com.tianji.activity.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("比赛查询条件")
public class CompetitionQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("页码")
    private Integer page = 1;

    @ApiModelProperty("每页数量")
    private Integer size = 10;

    @ApiModelProperty("比赛状态")
    private String status;

    @ApiModelProperty("比赛类型")
    private String competitionType;

    @ApiModelProperty("比赛级别")
    private String level;

    @ApiModelProperty("搜索关键字")
    private String keyword;

    @ApiModelProperty("是否仅推荐")
    private Boolean featuredOnly;

    @ApiModelProperty("是否仅已发布")
    private Boolean publishedOnly;

    @ApiModelProperty("报名开始时间起")
    private String registrationStartBegin;

    @ApiModelProperty("报名结束时间止")
    private String registrationEndEnd;

    @ApiModelProperty("排序字段")
    private String sortBy = "create_time";

    @ApiModelProperty("排序方向")
    private String sortOrder = "desc";

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCompetitionType() {
        return competitionType;
    }

    public void setCompetitionType(String competitionType) {
        this.competitionType = competitionType;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Boolean getFeaturedOnly() {
        return featuredOnly;
    }

    public void setFeaturedOnly(Boolean featuredOnly) {
        this.featuredOnly = featuredOnly;
    }

    public Boolean getPublishedOnly() {
        return publishedOnly;
    }

    public void setPublishedOnly(Boolean publishedOnly) {
        this.publishedOnly = publishedOnly;
    }

    public String getRegistrationStartBegin() {
        return registrationStartBegin;
    }

    public void setRegistrationStartBegin(String registrationStartBegin) {
        this.registrationStartBegin = registrationStartBegin;
    }

    public String getRegistrationEndEnd() {
        return registrationEndEnd;
    }

    public void setRegistrationEndEnd(String registrationEndEnd) {
        this.registrationEndEnd = registrationEndEnd;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
