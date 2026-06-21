package com.tianji.message.service;

import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.message.domain.dto.NoticeDTO;
import com.tianji.message.domain.po.Notice;

import java.util.List;

/**
 * 通知服务接口
 * 基于master分支的通知功能
 */
public interface INoticeService {

    /**
     * 创建通知
     */
    void createNotice(NoticeDTO noticeDTO);

    /**
     * 更新通知
     */
    void updateNotice(Long id, NoticeDTO noticeDTO);

    /**
     * 删除通知
     */
    void deleteNotice(Long id);

    /**
     * 发布通知
     */
    void publishNotice(Long id);

    void withdrawNotice(Long id);

    void updateTopStatus(Long id, Boolean isTop);

    boolean approveNoticeByManager(Long id, String remark, Long reviewerId, boolean allowProxyReview);

    boolean reviewNoticeByManager(Long id, boolean approved, String remark, Long reviewerId, boolean allowProxyReview);

    boolean approveNoticeByTeacher(Long id, String remark, Long reviewerId, boolean allowProxyReview);

    boolean reviewNoticeByTeacher(Long id, boolean approved, String remark, Long reviewerId, boolean allowProxyReview);

    /**
     * 获取通知详情
     */
    Notice getNoticeDetail(Long id);

    /**
     * 获取通知列表
     */
    List<Notice> getNoticeList();

    PageDTO<Notice> getNoticePage(int page, int pageSize);

    /**
     * 获取已发布通知列表
     */
    List<Notice> getPublishedNotices();

    /**
     * 增加浏览量
     */
    void incrementViewCount(Long id);

    List<GlobalSearchDocumentDTO> exportSearchDocuments();
}
