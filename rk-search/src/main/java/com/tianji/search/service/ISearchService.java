package com.tianji.search.service;

import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.search.domain.query.CoursePageQuery;
import com.tianji.search.domain.query.GlobalSearchQuery;
import com.tianji.search.domain.vo.CourseVO;
import com.tianji.search.domain.vo.GlobalSearchRebuildResultVO;
import com.tianji.search.domain.vo.GlobalSearchResultVO;

import java.util.List;

public interface ISearchService {

    List<CourseVO> queryCourseByCateId(Long cateLv2Id);

    List<CourseVO> queryBestTopN();

    List<CourseVO> queryNewTopN();

    List<CourseVO> queryFreeTopN();

    PageDTO<CourseVO> queryCoursesForPortal(CoursePageQuery query);

    List<Long> queryCoursesIdByName(String keyword);

    PageDTO<GlobalSearchResultVO> queryGlobalSearch(GlobalSearchQuery query);

    Boolean upsertGlobalDocument(GlobalSearchDocumentDTO document);

    Boolean upsertGlobalDocuments(List<GlobalSearchDocumentDTO> documents);

    Boolean deleteGlobalDocument(String entityType, Long entityId, Long tenantId);

    GlobalSearchRebuildResultVO rebuildGlobalSearchIndex();
}
