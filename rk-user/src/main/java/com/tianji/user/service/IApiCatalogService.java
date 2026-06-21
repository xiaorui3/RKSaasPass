package com.tianji.user.service;

import com.tianji.user.domain.dto.ApiCatalogDebugRequestDTO;
import com.tianji.user.domain.vo.ApiCatalogVO;

public interface IApiCatalogService {
    ApiCatalogVO getCatalog();

    ApiCatalogVO.DebugResult debug(ApiCatalogDebugRequestDTO request);
}
