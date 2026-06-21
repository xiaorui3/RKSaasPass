package com.tianji.content.converter;

import com.tianji.common.utils.BeanUtils;
import com.tianji.content.domain.dto.NewsSaveDTO;
import com.tianji.content.domain.dto.NewsUpdateDTO;
import com.tianji.content.domain.po.News;
import com.tianji.content.domain.vo.NewsDetailVO;
import com.tianji.content.domain.vo.NewsListVO;
import com.tianji.content.domain.vo.NewsSimpleVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 新闻对象转换器
 * 用于DTO、Entity、VO之间的转换
 *
 * @author RK-Web
 * @since 2026/03/13
 */
@Component
public class NewsConverter {

    /**
     * NewsSaveDTO 转 News Entity
     *
     * @param dto 保存DTO
     * @return News实体
     */
    public News toEntity(NewsSaveDTO dto) {
        if (dto == null) {
            return null;
        }
        News news = new News();
        news.setTitle(dto.getTitle());
        news.setSummary(dto.getSummary());
        news.setContent(dto.getContent());
        news.setCoverImage(dto.getCoverImage());
        news.setVideoUrl(dto.getVideoUrl());
        news.setAttachmentUrl(dto.getAttachmentUrl());
        news.setAuthor(dto.getAuthor());
        news.setCategory(dto.getCategory());
        news.setTags(dto.getTags());
        news.setIsPublished(dto.getIsPublished());
        news.setIsFeatured(dto.getIsFeatured());
        news.setIsCrossTenant(dto.getIsCrossTenant());
        news.setManagerReviewerId(dto.getManagerReviewerId());
        news.setTeacherReviewerId(dto.getTeacherReviewerId());
        news.setViewCount(0);
        
        // 如果是发布状态，设置发布时间
        if (dto.getIsPublished() != null && dto.getIsPublished() == 1) {
            news.setPublishTime(LocalDateTime.now());
        }
        
        return news;
    }

    /**
     * NewsUpdateDTO 转 News Entity
     *
     * @param dto 更新DTO
     * @return News实体
     */
    public News toEntity(NewsUpdateDTO dto) {
        if (dto == null) {
            return null;
        }
        News news = new News();
        news.setId(dto.getId());
        news.setTitle(dto.getTitle());
        news.setSummary(dto.getSummary());
        news.setContent(dto.getContent());
        news.setCoverImage(dto.getCoverImage());
        news.setVideoUrl(dto.getVideoUrl());
        news.setAttachmentUrl(dto.getAttachmentUrl());
        news.setAuthor(dto.getAuthor());
        news.setCategory(dto.getCategory());
        news.setTags(dto.getTags());
        news.setIsPublished(dto.getIsPublished());
        news.setIsFeatured(dto.getIsFeatured());
        news.setIsCrossTenant(dto.getIsCrossTenant());
        news.setApprovalStatus(dto.getApprovalStatus());
        news.setManagerReviewerId(dto.getManagerReviewerId());
        news.setTeacherReviewerId(dto.getTeacherReviewerId());

        return news;
    }

    /**
     * News Entity 转 NewsListVO
     *
     * @param news News实体
     * @return 列表VO
     */
    public NewsListVO toListVO(News news) {
        if (news == null) {
            return null;
        }
        return BeanUtils.copyBean(news, NewsListVO.class);
    }

    /**
     * News Entity 列表 转 NewsListVO 列表
     *
     * @param newsList News实体列表
     * @return 列表VO列表
     */
    public List<NewsListVO> toListVO(List<News> newsList) {
        if (newsList == null) {
            return null;
        }
        return BeanUtils.copyList(newsList, NewsListVO.class);
    }

    /**
     * News Entity 转 NewsDetailVO
     *
     * @param news News实体
     * @return 详情VO
     */
    public NewsDetailVO toDetailVO(News news) {
        if (news == null) {
            return null;
        }
        return BeanUtils.copyBean(news, NewsDetailVO.class);
    }

    /**
     * News Entity 转 NewsDetailVO（带上下篇）
     *
     * @param news     News实体
     * @param prevNews 上一篇新闻
     * @param nextNews 下一篇新闻
     * @return 详情VO
     */
    public NewsDetailVO toDetailVO(News news, News prevNews, News nextNews) {
        if (news == null) {
            return null;
        }
        NewsDetailVO vo = BeanUtils.copyBean(news, NewsDetailVO.class);
        
        if (prevNews != null) {
            vo.setPrevId(prevNews.getId());
            vo.setPrevTitle(prevNews.getTitle());
        }
        
        if (nextNews != null) {
            vo.setNextId(nextNews.getId());
            vo.setNextTitle(nextNews.getTitle());
        }
        
        return vo;
    }

    /**
     * News Entity 转 NewsSimpleVO
     *
     * @param news News实体
     * @return 简单VO
     */
    public NewsSimpleVO toSimpleVO(News news) {
        if (news == null) {
            return null;
        }
        return BeanUtils.copyBean(news, NewsSimpleVO.class);
    }

    /**
     * News Entity 列表 转 NewsSimpleVO 列表
     *
     * @param newsList News实体列表
     * @return 简单VO列表
     */
    public List<NewsSimpleVO> toSimpleVO(List<News> newsList) {
        if (newsList == null) {
            return null;
        }
        return BeanUtils.copyList(newsList, NewsSimpleVO.class);
    }

    /**
     * 更新News实体（部分更新）
     *
     * @param news 需要更新的实体
     * @param dto  更新DTO
     */
    public void updateEntity(News news, NewsUpdateDTO dto) {
        if (news == null || dto == null) {
            return;
        }
        
        if (dto.getTitle() != null) {
            news.setTitle(dto.getTitle());
        }
        if (dto.getSummary() != null) {
            news.setSummary(dto.getSummary());
        }
        if (dto.getContent() != null) {
            news.setContent(dto.getContent());
        }
        if (dto.getCoverImage() != null) {
            news.setCoverImage(dto.getCoverImage());
        }
        if (dto.getVideoUrl() != null) {
            news.setVideoUrl(dto.getVideoUrl());
        }
        if (dto.getAttachmentUrl() != null) {
            news.setAttachmentUrl(dto.getAttachmentUrl());
        }
        if (dto.getAuthor() != null) {
            news.setAuthor(dto.getAuthor());
        }
        if (dto.getCategory() != null) {
            news.setCategory(dto.getCategory());
        }
        if (dto.getTags() != null) {
            news.setTags(dto.getTags());
        }
        if (dto.getIsPublished() != null) {
            // 从草稿变为发布，设置发布时间
            if (news.getIsPublished() == 0 && dto.getIsPublished() == 1) {
                news.setPublishTime(LocalDateTime.now());
            }
            news.setIsPublished(dto.getIsPublished());
        }
        if (dto.getIsFeatured() != null) {
            news.setIsFeatured(dto.getIsFeatured());
        }
        if (dto.getIsCrossTenant() != null) {
            news.setIsCrossTenant(dto.getIsCrossTenant());
        }
        if (dto.getApprovalStatus() != null) {
            news.setApprovalStatus(dto.getApprovalStatus());
        }
        if (dto.getManagerReviewerId() != null) {
            news.setManagerReviewerId(dto.getManagerReviewerId());
        }
        if (dto.getTeacherReviewerId() != null) {
            news.setTeacherReviewerId(dto.getTeacherReviewerId());
        }
    }
}
