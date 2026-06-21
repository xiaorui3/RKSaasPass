package com.tianji.search.controller;

import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.search.domain.query.GlobalSearchQuery;
import com.tianji.search.domain.vo.GlobalSearchRebuildResultVO;
import com.tianji.search.domain.vo.GlobalSearchResultVO;
import com.tianji.search.service.ISearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "全站搜索接口")
@RestController
@RequestMapping("global")
@RequiredArgsConstructor
public class GlobalSearchController {

    private final ISearchService searchService;

    @ApiOperation("全站统一搜索")
    @GetMapping
    public PageDTO<GlobalSearchResultVO> queryGlobalSearch(GlobalSearchQuery query) {
        return searchService.queryGlobalSearch(query);
    }

    @ApiOperation("全站搜索文档写入")
    @PostMapping("/index")
    public Boolean upsertGlobalDocument(@RequestBody GlobalSearchDocumentDTO document) {
        return searchService.upsertGlobalDocument(document);
    }

    @ApiOperation("全站搜索文档批量写入")
    @PostMapping("/index/bulk")
    public Boolean upsertGlobalDocuments(@RequestBody List<GlobalSearchDocumentDTO> documents) {
        return searchService.upsertGlobalDocuments(documents);
    }

    @ApiOperation("全站搜索文档删除")
    @DeleteMapping("/index/{entityType}/{entityId}")
    public Boolean deleteGlobalDocument(@PathVariable("entityType") String entityType,
                                        @PathVariable("entityId") Long entityId,
                                        @RequestParam(value = "tenantId", required = false) Long tenantId) {
        return searchService.deleteGlobalDocument(entityType, entityId, tenantId);
    }

    @ApiOperation("全站搜索索引全量重建")
    @PostMapping("/index/rebuild")
    public GlobalSearchRebuildResultVO rebuildGlobalSearchIndex() {
        return searchService.rebuildGlobalSearchIndex();
    }
}
