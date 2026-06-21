package com.tianji.activity.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.activity.domain.dto.ActivityQueryDTO;
import com.tianji.activity.domain.dto.ActivitySaveDTO;
import com.tianji.activity.domain.po.Activity;
import com.tianji.activity.domain.po.ActivityRegistration;
import com.tianji.activity.domain.vo.ActivityDetailVO;
import com.tianji.activity.domain.vo.ActivityListVO;
import com.tianji.activity.domain.vo.ActivityRegistrationVO;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * 活动服务接口
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
public interface IActivityService {

    /**
     * 获取所有活动列表
     *
     * @return 活动列表
     */
    List<Activity> getAllActivities();

    /**
     * 分页获取活动列表
     *
     * @param page   页码
     * @param size   每页数量
     * @param status 活动状态（可选）
     * @param type   活动类型（可选）
     * @return 分页数据
     */
    Page<Activity> getActivityPage(Integer page, Integer size, Integer status, Integer type);

    /**
     * 管理后台分页获取活动列表
     */
    Page<Activity> getAdminActivityPage(Integer page, Integer size, Integer status, Integer type, String title,
                                        LocalDateTime startTimeBegin, LocalDateTime startTimeEnd);

    Page<Activity> getAdminActivityPage(Integer page, Integer size, Integer status, Integer type, String title,
                                        LocalDateTime startTimeBegin, LocalDateTime startTimeEnd,
                                        String keyword, Integer recentDays);

    /**
     * 获取当前审核人待处理的活动列表
     */
    Page<Activity> getPendingReviewPage(Integer page, Integer size, String title, String applicant, Long roleId);

    /**
     * 分页获取活动列表（使用DTO查询条件）
     *
     * @param queryDTO 查询条件
     * @return 分页VO数据
     */
    Page<ActivityListVO> getActivityPageVO(ActivityQueryDTO queryDTO);

    /**
     * 获取活动详情VO
     *
     * @param id     活动ID
     * @param userId 当前用户ID（可选，用于判断报名状态）
     * @return 活动详情VO
     */
    ActivityDetailVO getActivityDetailVO(Long id, Long userId);

    /**
     * 根据ID获取活动详情
     *
     * @param id 活动ID
     * @return 活动详情
     */
    Activity getActivityById(Long id);

    /**
     * 根据状态获取活动列表
     *
     * @param status 活动状态
     * @return 活动列表
     */
    List<Activity> getActivitiesByStatus(Integer status);

    /**
     * 获取热门活动
     *
     * @param limit 数量限制
     * @return 活动列表
     */
    List<Activity> getHotActivities(Integer limit);

    /**
     * 获取推荐活动（置顶）
     *
     * @param limit 数量限制
     * @return 活动列表
     */
    List<Activity> getTopActivities(Integer limit);

    /**
     * 获取其他租户共享活动
     */
    List<Activity> getSharedActivities(Integer limit);

    /**
     * 搜索活动
     *
     * @param keyword 关键词
     * @return 活动列表
     */
    List<Activity> searchActivities(String keyword);

    /**
     * 创建活动
     *
     * @param activity 活动信息
     * @return 活动ID
     */
    Long createActivity(Activity activity);

    /**
     * 普通成员发起活动申请
     */
    Long submitActivity(Activity activity);

    /**
     * 更新活动
     *
     * @param activity 活动信息
     * @return 影响行数
     */
    int updateActivity(Activity activity);

    /**
     * 负责人审核活动
     */
    boolean reviewActivityByManager(Long activityId, boolean approved, String reviewComment, Long reviewerId);

    boolean reviewActivityByManager(Long activityId, boolean approved, String reviewComment, Long reviewerId, boolean allowProxyReview);

    /**
     * 指导老师审核活动
     */
    boolean reviewActivityByTeacher(Long activityId, boolean approved, String reviewComment, Long reviewerId);

    boolean reviewActivityByTeacher(Long activityId, boolean approved, String reviewComment, Long reviewerId, boolean allowProxyReview);

    /**
     * 删除活动
     *
     * @param id 活动ID
     * @return 影响行数
     */
    int deleteActivity(Long id);

    /**
     * 增加浏览次数
     *
     * @param id 活动ID
     */
    void incrementViewCount(Long id);

    /**
     * 活动报名
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @param remark     备注
     * @return 报名结果
     */
    Map<String, Object> registerActivity(Long activityId, Long userId, String remark);

    /**
     * 取消报名
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @param reason     取消原因
     * @return 取消结果
     */
    Map<String, Object> cancelRegistration(Long activityId, Long userId, String reason);

    /**
     * 活动签到
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return 签到结果
     */
    Map<String, Object> checkIn(Long activityId, Long userId);

    /**
     * 获取活动的报名列表
     *
     * @param activityId 活动ID
     * @return 报名列表
     */
    List<ActivityRegistration> getRegistrationList(Long activityId);

    /**
     * 检查用户是否已报名
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return 是否已报名
     */
    boolean isRegistered(Long activityId, Long userId);

    /**
     * 获取活动统计信息
     *
     * @return 统计信息
     */
    Map<String, Object> getActivityStatistics();

    /**
     * 更新活动状态（根据时间自动更新）
     */
    void updateActivityStatus();

    List<GlobalSearchDocumentDTO> exportSearchDocuments();
}
