package com.tianji.user.controller;

import com.tianji.common.domain.R;
import com.tianji.user.domain.dto.ApiCatalogDebugRequestDTO;
import com.tianji.user.domain.vo.ApiCatalogVO;
import com.tianji.user.service.IApiCatalogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "接口目录")
@RestController
@RequestMapping("/api/api-catalog")
@RequiredArgsConstructor
public class ApiCatalogController {

    private final IApiCatalogService apiCatalogService;

    @ApiOperation("获取接口目录")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:config:view', 'system:config:query', 'system:config:list')")
    public R<ApiCatalogVO> getCatalog() {
        return R.ok(apiCatalogService.getCatalog());
    }

    @ApiOperation("使用当前登录态调试接口")
    @PostMapping("/debug")
    @PreAuthorize("hasAuthority('system:config:edit')")
    public R<ApiCatalogVO.DebugResult> debug(@RequestBody ApiCatalogDebugRequestDTO request) {
        return R.ok(apiCatalogService.debug(request));
    }
}
