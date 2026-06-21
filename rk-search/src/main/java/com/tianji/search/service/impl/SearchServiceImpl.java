package com.tianji.search.service.impl;

import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.activity.ActivitySearchDocumentClient;
import com.tianji.api.client.content.ContentSearchDocumentClient;
import com.tianji.api.client.message.MessageSearchDocumentClient;
import com.tianji.api.client.user.UserSearchDocumentClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.constants.ErrorInfo;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.CommonException;
import com.tianji.common.utils.*;
import com.tianji.search.config.InterestsProperties;
import com.tianji.search.constants.SearchErrorInfo;
import com.tianji.search.domain.po.Course;
import com.tianji.search.domain.query.CoursePageQuery;
import com.tianji.search.domain.query.GlobalSearchQuery;
import com.tianji.search.domain.vo.CourseVO;
import com.tianji.search.domain.vo.GlobalSearchRebuildResultVO;
import com.tianji.search.domain.vo.GlobalSearchResultVO;
import com.tianji.search.repository.CourseRepository;
import com.tianji.search.service.IInterestsService;
import com.tianji.search.service.ISearchService;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tianji.search.repository.CourseRepository.PUBLISH_TIME;

@Service
public class SearchServiceImpl implements ISearchService {

    public static final String GLOBAL_INDEX_NAME = "rk_global_search";
    private static final int GLOBAL_SEARCH_MAX_PAGE_SIZE = 50;
    private static final String DEFAULT_GLOBAL_SEARCH_ROUTE_FIELD = "route";
    private static final String DEFAULT_GLOBAL_SEARCH_UPDATED_FIELD = "updatedAt";

    @Autowired
    private RestHighLevelClient restClient;

    @Autowired
    private IInterestsService interestsService;

    @Autowired
    private UserClient userClient;

    @Autowired
    private CategoryCache categoryCache;

    @Autowired
    private InterestsProperties interestsProperties;

    @Autowired
    private ContentSearchDocumentClient contentSearchDocumentClient;

    @Autowired
    private ActivitySearchDocumentClient activitySearchDocumentClient;

    @Autowired
    private UserSearchDocumentClient userSearchDocumentClient;

    @Autowired
    private MessageSearchDocumentClient messageSearchDocumentClient;

    @Override
    public List<CourseVO> queryCourseByCateId(Long cateLv2Id) {
        return queryTopNByCategoryIdLv2sAndFree(
                CollUtils.singletonList(cateLv2Id), null, PUBLISH_TIME, false, 10);
    }

    @Override
    public List<CourseVO> queryBestTopN() {
        // 1.获取当前用户
        return queryTopNCourseOnMarketByFree(false, CourseRepository.SOLD);
    }

    @Override
    public List<CourseVO> queryNewTopN() {
        return queryTopNCourseOnMarketByFree(false, PUBLISH_TIME);
    }

    @Override
    public List<CourseVO> queryFreeTopN() {
        return queryTopNCourseOnMarketByFree(true, CourseRepository.SOLD);
    }

    private List<CourseVO> queryTopNCourseOnMarketByFree(boolean isFree, String sortBy) {
        // 1.获取当前用户
        Long id = UserContext.getUser();
        // 2.查询课程
        List<CourseVO> courses = null;
        if (id == null) {
            // 3.未登录，直接查询报名人数最多的
            courses = queryTopNByCategoryIdLv2sAndFree(
                    null, isFree, sortBy, false, interestsProperties.getTopNumber());
        } else {
            // 4.已登录，根据兴趣爱好查询
            List<Long> categoryIds = interestsService.queryMyInterestsIds();
            if (CollUtils.isEmpty(categoryIds)) {
                // 4.1.没有兴趣爱好，直接查询报名人数最多的
                courses = queryTopNByCategoryIdLv2sAndFree(
                        null, isFree, sortBy, false, interestsProperties.getTopNumber());
            } else {
                // 4.2.有爱好.查询爱好课程中报名人数最多的
                courses = queryTopNByCategoryIdLv2sAndFree(
                        categoryIds, isFree, sortBy, false, interestsProperties.getTopNumber());
            }
        }
        return courses;
    }

    private List<CourseVO> queryTopNByCategoryIdLv2sAndFree(
            List<Long> categoryIds, Boolean isFree, String sortBy, boolean isASC, int n) {
        // 1.准备Request
        SearchRequest request = new SearchRequest(CourseRepository.INDEX_NAME);
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        // 1.1.是否免费
        if(isFree != null) {
            queryBuilder.filter(QueryBuilders.termQuery(CourseRepository.FREE, isFree));
        }
        // 1.2.分类id
        if (categoryIds != null) {
            if (categoryIds.size() == 1) {
                queryBuilder.filter(QueryBuilders.termQuery(CourseRepository.CATEGORY_ID_LV2, categoryIds.get(0)));
            } else {
                queryBuilder.filter(QueryBuilders.termsQuery(CourseRepository.CATEGORY_ID_LV2, categoryIds));
            }
        }
        if(isFree != null || categoryIds != null) {
            request.source().query(queryBuilder);
        }
        // 1.3.TopN
        request.source().size(n).sort(sortBy, isASC ? SortOrder.ASC : SortOrder.DESC);
        // 2.发送请求
        SearchResponse response = null;
        try {
            response = restClient.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new CommonException(SearchErrorInfo.QUERY_COURSE_ERROR, e);
        }
        // 3.解析
        SearchHits searchHits = response.getHits();
        SearchHit[] hits = searchHits.getHits();
        if (hits == null || hits.length == 0) {
            return CollUtils.emptyList();
        }
        List<CourseVO> courses = new ArrayList<>(hits.length);
        Set<Long> teacherIds = new HashSet<>(hits.length);
        for (SearchHit hit : hits) {
            // 3.1.数据转换
            CourseVO vo = JsonUtils.toBean(hit.getSourceAsString(), CourseVO.class);
            // 3.2.获取分类id
            teacherIds.add(Long.valueOf(vo.getTeacher()));
            // 3.3.保存
            courses.add(vo);
        }
        teacherIds.remove(0L);
        if (teacherIds.size() == 0) {
            return courses;
        }
        // 4.查询教师
        List<UserDTO> teachers = userClient.queryUserByIds(teacherIds);
        AssertUtils.isNotEmpty(teachers, SearchErrorInfo.TEACHER_NOT_EXISTS);
        Map<String, String> tMap = teachers.stream()
                .collect(Collectors.toMap(t -> t.getId().toString(), UserDTO::getName));
        for (CourseVO c : courses) {
            c.setTeacher(tMap.getOrDefault(c.getTeacher(), "匿名"));
        }
        return courses;
    }

    @Override
    public PageDTO<CourseVO> queryCoursesForPortal(CoursePageQuery query) {
        // 1.搜索数据
        SearchResponse response = searchForResponse(query, CourseVO.EXCLUDE_FIELDS);
        // 2.解析响应
        PageDTO<Course> result = handleSearchResponse(response, query.getPageSize());
        // 3.处理VO
        List<Course> list = result.getList();
        if (CollUtils.isEmpty(list)) {
            return PageDTO.empty(result.getTotal(), result.getPages());
        }
        // 3.1.查询教师信息
        List<Long> teacherIds = list.stream().map(Course::getTeacher).collect(Collectors.toList());
        List<UserDTO> teachers = userClient.queryUserByIds(teacherIds);
        AssertUtils.isNotEmpty(teachers, SearchErrorInfo.TEACHER_NOT_EXISTS);
        Map<Long, String> teacherMap = teachers.stream()
                .collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));
        // 3.2.转换VO
        List<CourseVO> vos = new ArrayList<>(list.size());
        for (Course c : list) {
            CourseVO vo = BeanUtils.toBean(c, CourseVO.class);
            vo.setTeacher(teacherMap.getOrDefault(c.getTeacher(), "未知"));
            vos.add(vo);
        }
        return new PageDTO<>(result.getTotal(), result.getPages(), vos);
    }

    @Override
    public List<Long> queryCoursesIdByName(String keyword) {
        // 1.创建Request
        SearchRequest request = new SearchRequest(CourseRepository.INDEX_NAME);
        // 2.构建DSL
        request.source()
                .query(QueryBuilders.matchPhraseQuery(CourseRepository.DEFAULT_QUERY_NAME, keyword))
                .fetchSource(new String[]{"id"}, null);
        // 3.查询
        SearchResponse response;
        try {
            response = restClient.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new CommonException(SearchErrorInfo.QUERY_COURSE_ERROR, e);
        }
        // 4.解析
        SearchHits searchHits = response.getHits();
        // 4.1.获取hits
        SearchHit[] hits = searchHits.getHits();
        if (hits.length == 0) {
            return CollUtils.emptyList();
        }
        // 4.2.获取id
        return Arrays.stream(hits)
                .map(SearchHit::getId)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    @Override
    public PageDTO<GlobalSearchResultVO> queryGlobalSearch(GlobalSearchQuery query) {
        if (query == null || StringUtils.isBlank(query.getKeyword())) {
            return PageDTO.empty(0L, 0L);
        }
        String keyword = query.getKeyword().trim();
        if (keyword.length() < 2) {
            return PageDTO.empty(0L, 0L);
        }

        int pageNo = query.getPageNo() == null || query.getPageNo() < 1 ? 1 : query.getPageNo();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1
                ? 20
                : Math.min(query.getPageSize(), GLOBAL_SEARCH_MAX_PAGE_SIZE);

        SearchRequest request = new SearchRequest(GLOBAL_INDEX_NAME);
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.multiMatchQuery(keyword, "title^3", "summary^2", "content", "tags")
                        .type(MultiMatchQueryBuilder.Type.BEST_FIELDS));
        boolQuery.filter(QueryBuilders.termQuery("visible", true));

        Long tenantId = query.getTenantId() == null ? TenantContext.getTenantId() : query.getTenantId();
        if (!Boolean.TRUE.equals(query.getIncludeAllTenants()) || !TenantContext.isSuperAdmin()) {
            if (tenantId != null) {
                boolQuery.filter(QueryBuilders.termQuery("tenantId", tenantId));
            }
        }
        if (CollUtils.isNotEmpty(query.getEntityTypes())) {
            List<String> entityTypes = query.getEntityTypes().stream()
                    .filter(StringUtils::isNotBlank)
                    .map(type -> type.trim().toUpperCase(Locale.ROOT))
                    .distinct()
                    .collect(Collectors.toList());
            if (CollUtils.isNotEmpty(entityTypes)) {
                boolQuery.filter(QueryBuilders.termsQuery("entityType", entityTypes));
            }
        }

        request.source()
                .query(boolQuery)
                .from((pageNo - 1) * pageSize)
                .size(pageSize)
                .highlighter(new HighlightBuilder()
                        .field("title")
                        .field("summary")
                        .field("content")
                        .preTags("<em>")
                        .postTags("</em>"))
                .sort(DEFAULT_GLOBAL_SEARCH_UPDATED_FIELD, SortOrder.DESC);

        SearchResponse response;
        try {
            response = restClient.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new CommonException(SearchErrorInfo.QUERY_COURSE_ERROR, e);
        }
        return handleGlobalSearchResponse(response, pageSize);
    }

    @Override
    public Boolean upsertGlobalDocument(GlobalSearchDocumentDTO document) {
        if (document == null) {
            return false;
        }
        String id = globalDocumentId(document.getEntityType(), document.getEntityId(), document.getTenantId());
        if (StringUtils.isBlank(id)) {
            return false;
        }
        try {
            restClient.index(new IndexRequest(GLOBAL_INDEX_NAME)
                    .id(id)
                    .source(JsonUtils.toJsonStr(document), XContentType.JSON), RequestOptions.DEFAULT);
            return true;
        } catch (Exception e) {
            throw new CommonException(ErrorInfo.Msg.SERVER_INTER_ERROR, e);
        }
    }

    @Override
    public Boolean upsertGlobalDocuments(List<GlobalSearchDocumentDTO> documents) {
        if (CollUtils.isEmpty(documents)) {
            return true;
        }
        try {
            BulkRequest bulkRequest = new BulkRequest(GLOBAL_INDEX_NAME);
            for (GlobalSearchDocumentDTO document : documents) {
                if (document == null) {
                    continue;
                }
                String id = globalDocumentId(document.getEntityType(), document.getEntityId(), document.getTenantId());
                if (StringUtils.isBlank(id)) {
                    continue;
                }
                bulkRequest.add(new IndexRequest(GLOBAL_INDEX_NAME)
                        .id(id)
                        .source(JsonUtils.toJsonStr(document), XContentType.JSON));
            }
            if (bulkRequest.numberOfActions() == 0) {
                return true;
            }
            restClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            return true;
        } catch (Exception e) {
            throw new CommonException(ErrorInfo.Msg.SERVER_INTER_ERROR, e);
        }
    }

    @Override
    public Boolean deleteGlobalDocument(String entityType, Long entityId, Long tenantId) {
        if (StringUtils.isBlank(entityType) || entityId == null) {
            return false;
        }
        try {
            restClient.delete(new DeleteRequest(GLOBAL_INDEX_NAME, globalDocumentId(entityType, entityId, tenantId)),
                    RequestOptions.DEFAULT);
            return true;
        } catch (Exception e) {
            throw new CommonException(ErrorInfo.Msg.SERVER_INTER_ERROR, e);
        }
    }

    @Override
    public GlobalSearchRebuildResultVO rebuildGlobalSearchIndex() {
        Map<String, Integer> sourceCounts = new LinkedHashMap<>();
        List<GlobalSearchDocumentDTO> documents = new ArrayList<>();
        documents.addAll(collectGlobalSearchDocuments("content", contentSearchDocumentClient.exportSearchDocuments(), sourceCounts));
        documents.addAll(collectGlobalSearchDocuments("activity", activitySearchDocumentClient.exportSearchDocuments(), sourceCounts));
        documents.addAll(collectGlobalSearchDocuments("user", userSearchDocumentClient.exportSearchDocuments(), sourceCounts));
        documents.addAll(collectGlobalSearchDocuments("message", messageSearchDocumentClient.exportSearchDocuments(), sourceCounts));

        int deletedCount = deleteAllGlobalSearchDocuments();
        upsertGlobalDocuments(documents);
        return new GlobalSearchRebuildResultVO()
                .setDeletedCount(deletedCount)
                .setIndexedCount(documents.size())
                .setSourceCounts(sourceCounts)
                .setStatus("SUCCESS");
    }

    private List<GlobalSearchDocumentDTO> collectGlobalSearchDocuments(String source,
                                                                       List<GlobalSearchDocumentDTO> exported,
                                                                       Map<String, Integer> sourceCounts) {
        List<GlobalSearchDocumentDTO> documents = exported == null ? CollUtils.emptyList() : exported.stream()
                .filter(Objects::nonNull)
                .filter(document -> Boolean.TRUE.equals(document.getVisible()))
                .filter(document -> StringUtils.isNotBlank(document.getEntityType()))
                .filter(document -> document.getEntityId() != null)
                .collect(Collectors.toList());
        sourceCounts.put(source, documents.size());
        return documents;
    }

    private int deleteAllGlobalSearchDocuments() {
        DeleteByQueryRequest request = new DeleteByQueryRequest(GLOBAL_INDEX_NAME);
        request.setConflicts("proceed");
        request.setQuery(QueryBuilders.matchAllQuery());
        try {
            BulkByScrollResponse response = restClient.deleteByQuery(request, RequestOptions.DEFAULT);
            return Math.toIntExact(response.getDeleted());
        } catch (Exception e) {
            String message = e.getMessage();
            if (message != null && message.contains("index_not_found_exception")) {
                return 0;
            }
            throw new CommonException(ErrorInfo.Msg.SERVER_INTER_ERROR, e);
        }
    }


    private SearchResponse searchForResponse(CoursePageQuery query, String[] excludeFields) {
        // 1.创建Request
        SearchRequest request = new SearchRequest(CourseRepository.INDEX_NAME);
        // 2.构建DSL
        // 2.1.构建query
        buildBasicQuery(request, query);
        // 2.2.排序
        String sortBy = query.getSortBy();
        if (StringUtils.isNotBlank(sortBy)) {
            request.source().sort(sortBy, query.getIsAsc() ? SortOrder.ASC : SortOrder.DESC);
        }
        // 2.3.分页
        request.source().from(query.from()).size(query.getPageSize());
        // 2.4.高亮
        request.source().highlighter(new HighlightBuilder().field(CourseRepository.DEFAULT_QUERY_NAME));
        // 2.5.source处理
        request.source().fetchSource(null, excludeFields);
        // 3.发送请求
        SearchResponse response = null;
        try {
            response = restClient.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new CommonException(ErrorInfo.Msg.SERVER_INTER_ERROR, e);
        }
        return response;
    }

    private void buildBasicQuery(SearchRequest request, CoursePageQuery query) {
        // 1.准备bool查询
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        // 2.关键字搜索
        String keyword = query.getKeyword();
        if (StringUtils.isBlank(keyword)) {
            queryBuilder.must(QueryBuilders.matchAllQuery());
        } else {
            queryBuilder.must(QueryBuilders.matchPhraseQuery(CourseRepository.DEFAULT_QUERY_NAME, keyword));
        }
        // 3.其它条件
        if (query.getCategoryIdLv1() != null) {
            queryBuilder.filter(QueryBuilders.termQuery(CourseRepository.CATEGORY_ID_LV1, query.getCategoryIdLv1()));
        }
        if (query.getCategoryIdLv2() != null) {
            queryBuilder.filter(QueryBuilders.termQuery(CourseRepository.CATEGORY_ID_LV2, query.getCategoryIdLv2()));
        }
        if (query.getCategoryIdLv3() != null) {
            queryBuilder.filter(QueryBuilders.termQuery(CourseRepository.CATEGORY_ID_LV3, query.getCategoryIdLv3()));
        }
        if (query.getFree() != null) {
            queryBuilder.filter(QueryBuilders.termQuery(CourseRepository.FREE, query.getFree()));
        }
        if (query.getType() != null) {
            queryBuilder.filter(QueryBuilders.termQuery(CourseRepository.TYPE, query.getType()));
        }
        LocalDateTime beginTime = query.getBeginTime();
        LocalDateTime endTime = query.getEndTime();
        if(beginTime != null || endTime != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(CourseRepository.UPDATE_TIME);
            if (beginTime != null) {
                rangeQuery.gte(beginTime);
            }
            if (endTime != null) {
                rangeQuery.lte(endTime);
            }
            queryBuilder.filter(rangeQuery);
        }
        // 4.写入request
        request.source().query(queryBuilder);
    }

    private PageDTO<Course> handleSearchResponse(SearchResponse response, int pageSize) {
        SearchHits searchHits = response.getHits();
        // 1.总条数
        long total = searchHits.getTotalHits().value;
        // 2.总页数
        long totalPages = (total + pageSize - 1) / pageSize;
        // 3.获取命中的数据
        SearchHit[] hits = searchHits.getHits();
        if (hits.length <= 0) {
            return new PageDTO<>(total, totalPages, CollUtils.emptyList());
        }
        // 4.遍历
        List<Course> list = new ArrayList<>(hits.length);
        for (SearchHit hit : hits) {
            // 5.获取某一条source
            String jsonSource = hit.getSourceAsString();
            // 6.反序列化
            Course course = JsonUtils.toBean(jsonSource, Course.class);
            // 7.处理高亮
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (CollUtils.isNotEmpty(highlightFields)) {
                // 7.1.获取高亮结果
                HighlightField field = highlightFields.get(CourseRepository.DEFAULT_QUERY_NAME);
                Object[] fragments = field.getFragments();
                String value = StringUtils.join(fragments);
                // 7.2.覆盖非高亮结果
                course.setName(value);
            }
            list.add(course);
        }
        return new PageDTO<>(total, totalPages, list);
    }

    private PageDTO<GlobalSearchResultVO> handleGlobalSearchResponse(SearchResponse response, int pageSize) {
        SearchHits searchHits = response.getHits();
        long total = searchHits.getTotalHits().value;
        long totalPages = (total + pageSize - 1) / pageSize;
        SearchHit[] hits = searchHits.getHits();
        if (hits == null || hits.length == 0) {
            return new PageDTO<>(total, totalPages, CollUtils.emptyList());
        }
        List<GlobalSearchResultVO> list = new ArrayList<>(hits.length);
        for (SearchHit hit : hits) {
            Map<String, Object> source = hit.getSourceAsMap();
            GlobalSearchResultVO vo = new GlobalSearchResultVO();
            vo.setEntityType(stringValue(source.get("entityType")));
            vo.setEntityId(longValue(source.get("entityId")));
            vo.setTenantId(longValue(source.get("tenantId")));
            vo.setTitle(stripHtml(firstNonBlank(highlightValue(hit, "title"), stringValue(source.get("title")))));
            vo.setSummary(stripHtml(firstNonBlank(highlightValue(hit, "summary"), stringValue(source.get("summary")), stringValue(source.get("content")))));
            vo.setRoute(firstNonBlank(stringValue(source.get(DEFAULT_GLOBAL_SEARCH_ROUTE_FIELD)), buildDefaultRoute(vo.getEntityType(), vo.getEntityId(), vo.getTenantId())));
            vo.setCoverUrl(stringValue(source.get("coverUrl")));
            vo.setUpdatedAt(firstNonBlank(stringValue(source.get(DEFAULT_GLOBAL_SEARCH_UPDATED_FIELD)), stringValue(source.get("updateTime")), stringValue(source.get("createTime"))));
            vo.setHighlight(stripHtml(firstNonBlank(highlightValue(hit, "content"), highlightValue(hit, "summary"), highlightValue(hit, "title"))));
            list.add(vo);
        }
        return new PageDTO<>(total, totalPages, list);
    }

    private String highlightValue(SearchHit hit, String field) {
        Map<String, HighlightField> highlightFields = hit.getHighlightFields();
        if (CollUtils.isEmpty(highlightFields)) {
            return null;
        }
        HighlightField highlightField = highlightFields.get(field);
        if (highlightField == null || highlightField.getFragments() == null || highlightField.getFragments().length == 0) {
            return null;
        }
        return StringUtils.join(highlightField.getFragments());
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String stripHtml(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        return value.replaceAll("<(?!/?em\\b)[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private String buildDefaultRoute(String entityType, Long entityId, Long tenantId) {
        if (StringUtils.isBlank(entityType)) {
            return "";
        }
        String type = entityType.toUpperCase(Locale.ROOT);
        if ("NEWS".equals(type) && entityId != null) {
            return "/news/" + entityId;
        }
        if ("ACTIVITY".equals(type) && entityId != null) {
            return "/activities/" + entityId;
        }
        if ("COMPETITION".equals(type) && entityId != null) {
            return "/competition/" + entityId;
        }
        if ("WORKS".equals(type) && entityId != null) {
            return "/works/" + entityId;
        }
        if ("TENANT".equals(type)) {
            return tenantId == null ? "/join" : "/join?tenantId=" + tenantId;
        }
        if ("MEMBER".equals(type) || "ALUMNI".equals(type)) {
            return "/alumni";
        }
        if ("HISTORY".equals(type)) {
            return "/history";
        }
        if ("NOTICE".equals(type)) {
            return "/notices";
        }
        return "";
    }

    private String globalDocumentId(String entityType, Long entityId, Long tenantId) {
        if (StringUtils.isBlank(entityType) || entityId == null) {
            return null;
        }
        return entityType.trim().toUpperCase(Locale.ROOT) + ":" + (tenantId == null ? 0L : tenantId) + ":" + entityId;
    }
}
