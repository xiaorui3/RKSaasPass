package com.tianji.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.content.domain.po.News;

import java.util.List;

/**
 * 新闻服务接口
 * 基于master分支的核心业务逻辑
 */
public interface INewsService extends IService<News> {

    // 基础查询方法
    News selectNewsById(Long id);
    List<News> selectNewsList(News news);
    List<News> selectLatestNews(Integer limit);
    List<News> selectTopNews(Integer limit);
    List<News> selectSharedNews(Integer limit);
    List<News> selectNewsByCategory(String category, Integer limit);
    List<News> searchNews(String keyword, Integer limit);

    // 增删改方法
    int insertNews(News news);
    int updateNews(News news);
    int deleteNewsById(Long id);
    int deleteNewsByIds(List<Long> ids);

    // 统计方法
    int incrementViewCount(Long id);
    int countNews();

    // 审核方法
    List<News> selectPendingNews();
    boolean approveNewsByManager(Long id, String remark, Long reviewerId, boolean allowProxyReview);
    boolean reviewNewsByManager(Long id, boolean approved, String remark, Long reviewerId, boolean allowProxyReview);
    boolean approveNewsByTeacher(Long id, String remark, Long reviewerId, boolean allowProxyReview);
    boolean reviewNewsByTeacher(Long id, boolean approved, String remark, Long reviewerId, boolean allowProxyReview);
    void approveNews(Long id, String remark);
    void rejectNews(Long id, String rejectReason);

    List<GlobalSearchDocumentDTO> exportSearchDocuments();
}
