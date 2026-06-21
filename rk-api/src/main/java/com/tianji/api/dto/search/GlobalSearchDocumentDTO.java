package com.tianji.api.dto.search;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@ApiModel(description = "global search document")
public class GlobalSearchDocumentDTO {

    @ApiModelProperty("entity type")
    private String entityType;

    @ApiModelProperty("entity id")
    private Long entityId;

    @ApiModelProperty("tenant id")
    private Long tenantId;

    @ApiModelProperty("title")
    private String title;

    @ApiModelProperty("summary")
    private String summary;

    @ApiModelProperty("content")
    private String content;

    @ApiModelProperty("tags")
    private String tags;

    @ApiModelProperty("route")
    private String route;

    @ApiModelProperty("cover url")
    private String coverUrl;

    @ApiModelProperty("updated time")
    private String updatedAt;

    @ApiModelProperty("visible")
    private Boolean visible;
}
