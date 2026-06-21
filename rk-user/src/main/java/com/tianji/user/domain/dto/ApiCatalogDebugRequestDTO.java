package com.tianji.user.domain.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ApiCatalogDebugRequestDTO {
    private String method;
    private String path;
    private String queryJson;
    private String bodyJson;
}
