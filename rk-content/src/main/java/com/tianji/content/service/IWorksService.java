package com.tianji.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.content.domain.po.Work;

import java.util.List;
import java.util.Map;

/**
 * 作品展示服务接口
 * 基于master分支的WorksController功能
 */
public interface IWorksService extends IService<Work> {

    // 获取所有作品（前端展示用）
    List<Map<String, Object>> getAllWorks();

    // 按分类获取作品
    List<Map<String, Object>> getWorksByCategory(String category);

    // 获取精选作品
    List<Map<String, Object>> getFeaturedWorks();

    // 搜索作品
    List<Map<String, Object>> searchWorks(String title, String category);

    // 获取作品详情
    Map<String, Object> getWorkDetail(Long id);

    // 增加浏览量
    int incrementViewCount(Long id);

    // 点赞作品
    boolean likeWork(Long id);

    // 取消点赞
    boolean unlikeWork(Long id);

    // 上传封面图片
    String uploadCoverImage(Long id, String imageUrl);

    // 上传演示视频
    String uploadDemoVideo(Long id, String videoUrl);

    // 获取作品统计
    Map<String, Object> getWorkStatistics();

    // 获取热门作品
    List<Map<String, Object>> getPopularWorks();

    // 获取最新作品
    List<Map<String, Object>> getLatestWorks();

    boolean approveWorkByManager(Long id, String remark, Long reviewerId, boolean allowProxyReview);

    boolean reviewWorkByManager(Long id, boolean approved, String remark, Long reviewerId, boolean allowProxyReview);

    boolean approveWorkByTeacher(Long id, String remark, Long reviewerId, boolean allowProxyReview);

    boolean reviewWorkByTeacher(Long id, boolean approved, String remark, Long reviewerId, boolean allowProxyReview);

    List<GlobalSearchDocumentDTO> exportSearchDocuments();
}
