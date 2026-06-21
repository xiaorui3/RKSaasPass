package com.tianji.activity.domain.converter;

import com.tianji.activity.domain.dto.CompetitionQueryDTO;
import com.tianji.activity.domain.dto.CompetitionSaveDTO;
import com.tianji.activity.domain.dto.CompetitionUpdateDTO;
import com.tianji.activity.domain.po.Competition;
import com.tianji.activity.domain.vo.CompetitionDetailVO;
import com.tianji.activity.domain.vo.CompetitionListVO;
import com.tianji.activity.domain.vo.CompetitionSimpleVO;
import com.tianji.activity.domain.vo.CompetitionStatisticsVO;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 比赛对象转换器
 * 用于DTO、Entity、VO之间的转换
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
public class CompetitionConverter {

    private CompetitionConverter() {
        // 工具类禁止实例化
    }

    // ==================== Entity -> VO 转换 ====================

    /**
     * Competition实体转换为CompetitionListVO
     *
     * @param entity 比赛实体
     * @return 列表VO
     */
    public static CompetitionListVO toListVO(Competition entity) {
        if (entity == null) {
            return null;
        }
        CompetitionListVO vo = new CompetitionListVO();
        BeanUtils.copyProperties(entity, vo);
        
        // 设置是否可以报名
        vo.setCanRegister(canRegister(entity));
        
        return vo;
    }

    /**
     * Competition实体转换为CompetitionDetailVO
     *
     * @param entity 比赛实体
     * @return 详情VO
     */
    public static CompetitionDetailVO toDetailVO(Competition entity) {
        if (entity == null) {
            return null;
        }
        CompetitionDetailVO vo = new CompetitionDetailVO();
        BeanUtils.copyProperties(entity, vo);
        
        // 设置是否可以报名
        vo.setCanRegister(canRegister(entity));
        vo.setCanCancel(canCancel(entity));
        
        return vo;
    }

    /**
     * Competition实体转换为CompetitionSimpleVO
     *
     * @param entity 比赛实体
     * @return 简单VO
     */
    public static CompetitionSimpleVO toSimpleVO(Competition entity) {
        if (entity == null) {
            return null;
        }
        CompetitionSimpleVO vo = new CompetitionSimpleVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * Competition实体列表转换为CompetitionListVO列表
     *
     * @param entities 实体列表
     * @return VO列表
     */
    public static List<CompetitionListVO> toListVOList(List<Competition> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(CompetitionConverter::toListVO)
                .collect(Collectors.toList());
    }

    /**
     * Competition实体列表转换为CompetitionSimpleVO列表
     *
     * @param entities 实体列表
     * @return VO列表
     */
    public static List<CompetitionSimpleVO> toSimpleVOList(List<Competition> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(CompetitionConverter::toSimpleVO)
                .collect(Collectors.toList());
    }

    // ==================== DTO -> Entity 转换 ====================

    /**
     * CompetitionSaveDTO转换为Competition实体
     *
     * @param dto 保存DTO
     * @return 实体
     */
    public static Competition toEntity(CompetitionSaveDTO dto) {
        if (dto == null) {
            return null;
        }
        Competition entity = new Competition();
        BeanUtils.copyProperties(dto, entity);
        
        // 设置初始状态
        if (dto.getIsPublished() != null && dto.getIsPublished()) {
            entity.setStatus("PUBLISHED");
        } else {
            entity.setStatus("DRAFT");
        }
        entity.setIsPublished(dto.getIsPublished() != null ? dto.getIsPublished() : false);
        entity.setIsCrossTenant(dto.getIsCrossTenant() != null ? dto.getIsCrossTenant() : false);
        
        // 初始化计数器
        entity.setViewCount(0);
        entity.setRegistrationCount(0);
        
        return entity;
    }

    /**
     * CompetitionUpdateDTO转换为Competition实体
     *
     * @param dto 更新DTO
     * @return 实体
     */
    public static Competition toEntity(CompetitionUpdateDTO dto) {
        if (dto == null) {
            return null;
        }
        Competition entity = new Competition();
        BeanUtils.copyProperties(dto, entity);
        
        // 如果指定了isPublished，同步更新状态
        if (dto.getIsPublished() != null && dto.getIsPublished()) {
            if (entity.getStatus() == null || "DRAFT".equals(entity.getStatus())) {
                entity.setStatus("PUBLISHED");
            }
        }
        
        return entity;
    }

    /**
     * 使用UpdateDTO更新已存在的实体
     *
     * @param entity 已存在的实体
     * @param dto    更新DTO
     */
    public static void updateEntity(Competition entity, CompetitionUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        
        // 只更新非空字段
        if (dto.getTitle() != null) entity.setTitle(dto.getTitle());
        if (dto.getSubtitle() != null) entity.setSubtitle(dto.getSubtitle());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getContent() != null) entity.setContent(dto.getContent());
        if (dto.getOrganizer() != null) entity.setOrganizer(dto.getOrganizer());
        if (dto.getCoOrganizer() != null) entity.setCoOrganizer(dto.getCoOrganizer());
        if (dto.getCompetitionType() != null) entity.setCompetitionType(dto.getCompetitionType());
        if (dto.getLevel() != null) entity.setLevel(dto.getLevel());
        if (dto.getMaxParticipants() != null) entity.setMaxParticipants(dto.getMaxParticipants());
        if (dto.getRegistrationStart() != null) entity.setRegistrationStart(dto.getRegistrationStart());
        if (dto.getRegistrationEnd() != null) entity.setRegistrationEnd(dto.getRegistrationEnd());
        if (dto.getCompetitionStart() != null) entity.setCompetitionStart(dto.getCompetitionStart());
        if (dto.getCompetitionEnd() != null) entity.setCompetitionEnd(dto.getCompetitionEnd());
        if (dto.getLocation() != null) entity.setLocation(dto.getLocation());
        if (dto.getOnlineUrl() != null) entity.setOnlineUrl(dto.getOnlineUrl());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        if (dto.getCoverImage() != null) entity.setCoverImage(dto.getCoverImage());
        if (dto.getRulesFile() != null) entity.setRulesFile(dto.getRulesFile());
        if (dto.getMaterialsFile() != null) entity.setMaterialsFile(dto.getMaterialsFile());
        if (dto.getResultsFile() != null) entity.setResultsFile(dto.getResultsFile());
        if (dto.getSummary() != null) entity.setSummary(dto.getSummary());
        if (dto.getAwards() != null) entity.setAwards(dto.getAwards());
        if (dto.getParticipationPoints() != null) entity.setParticipationPoints(dto.getParticipationPoints());
        if (dto.getFirstPrizePoints() != null) entity.setFirstPrizePoints(dto.getFirstPrizePoints());
        if (dto.getSecondPrizePoints() != null) entity.setSecondPrizePoints(dto.getSecondPrizePoints());
        if (dto.getThirdPrizePoints() != null) entity.setThirdPrizePoints(dto.getThirdPrizePoints());
        if (dto.getExcellentPrizePoints() != null) entity.setExcellentPrizePoints(dto.getExcellentPrizePoints());
        if (dto.getContactPerson() != null) entity.setContactPerson(dto.getContactPerson());
        if (dto.getContactPhone() != null) entity.setContactPhone(dto.getContactPhone());
        if (dto.getContactEmail() != null) entity.setContactEmail(dto.getContactEmail());
        if (dto.getTags() != null) entity.setTags(dto.getTags());
        if (dto.getPriority() != null) entity.setPriority(dto.getPriority());
        if (dto.getIsFeatured() != null) entity.setIsFeatured(dto.getIsFeatured());
        if (dto.getIsPublished() != null) entity.setIsPublished(dto.getIsPublished());
        if (dto.getIsCrossTenant() != null) entity.setIsCrossTenant(dto.getIsCrossTenant());
        if (dto.getManagerReviewerId() != null) entity.setManagerReviewerId(dto.getManagerReviewerId());
        if (dto.getTeacherReviewerId() != null) entity.setTeacherReviewerId(dto.getTeacherReviewerId());
    }

    // ==================== Entity -> StatisticsVO 转换 ====================

    /**
     * 创建基础统计VO（仅包含比赛基本信息）
     * 完整统计需要额外查询报名数据
     *
     * @param entity 比赛实体
     * @return 统计VO
     */
    public static CompetitionStatisticsVO toStatisticsVO(Competition entity) {
        if (entity == null) {
            return null;
        }
        CompetitionStatisticsVO vo = new CompetitionStatisticsVO();
        vo.setCompetitionId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setMaxParticipants(entity.getMaxParticipants());
        vo.setTotalRegistrations(entity.getRegistrationCount());
        vo.setViewCount(entity.getViewCount());
        vo.setStatus(entity.getStatus());
        
        // 计算报名率
        if (entity.getMaxParticipants() != null && entity.getMaxParticipants() > 0 
                && entity.getRegistrationCount() != null) {
            vo.setRegistrationRate(entity.getRegistrationCount() * 100.0 / entity.getMaxParticipants());
        }
        
        return vo;
    }

    // ==================== 辅助方法 ====================

    /**
     * 判断比赛是否可以报名
     *
     * @param entity 比赛实体
     * @return 是否可以报名
     */
    public static boolean canRegister(Competition entity) {
        if (entity == null) {
            return false;
        }
        
        // 必须已发布
        if (!Boolean.TRUE.equals(entity.getIsPublished())) {
            return false;
        }
        if (!Integer.valueOf(Competition.REVIEW_APPROVED).equals(entity.getTeacherReviewStatus())) {
            return false;
        }
        
        // 状态检查
        if ("CANCELLED".equals(entity.getStatus()) || "COMPLETED".equals(entity.getStatus())) {
            return false;
        }
        
        // 时间检查
        LocalDateTime now = LocalDateTime.now();
        if (entity.getRegistrationStart() != null && now.isBefore(entity.getRegistrationStart())) {
            return false;
        }
        if (entity.getRegistrationEnd() != null && now.isAfter(entity.getRegistrationEnd())) {
            return false;
        }
        
        // 名额检查
        if (entity.getMaxParticipants() != null && entity.getMaxParticipants() > 0) {
            int currentCount = entity.getRegistrationCount() != null ? entity.getRegistrationCount() : 0;
            if (currentCount >= entity.getMaxParticipants()) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 判断是否可以取消报名
     *
     * @param entity 比赛实体
     * @return 是否可以取消报名
     */
    public static boolean canCancel(Competition entity) {
        if (entity == null) {
            return false;
        }
        
        // 比赛未开始才能取消
        LocalDateTime now = LocalDateTime.now();
        if (entity.getCompetitionStart() != null && now.isAfter(entity.getCompetitionStart())) {
            return false;
        }
        
        // 已取消或已结束的比赛不能取消报名
        if ("CANCELLED".equals(entity.getStatus()) || "COMPLETED".equals(entity.getStatus())) {
            return false;
        }
        
        return true;
    }

    /**
     * 获取不能报名的原因
     *
     * @param entity 比赛实体
     * @return 不能报名的原因，如果可以报名则返回null
     */
    public static String getCannotRegisterReason(Competition entity) {
        if (entity == null) {
            return "比赛不存在";
        }
        
        if (!Boolean.TRUE.equals(entity.getIsPublished())) {
            return "比赛未发布";
        }
        
        if ("CANCELLED".equals(entity.getStatus())) {
            return "比赛已取消";
        }
        if ("COMPLETED".equals(entity.getStatus())) {
            return "比赛已结束";
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (entity.getRegistrationStart() != null && now.isBefore(entity.getRegistrationStart())) {
            return "报名尚未开始";
        }
        if (entity.getRegistrationEnd() != null && now.isAfter(entity.getRegistrationEnd())) {
            return "报名已截止";
        }
        
        if (entity.getMaxParticipants() != null && entity.getMaxParticipants() > 0) {
            int currentCount = entity.getRegistrationCount() != null ? entity.getRegistrationCount() : 0;
            if (currentCount >= entity.getMaxParticipants()) {
                return "报名名额已满";
            }
        }
        
        return null;
    }

    /**
     * 根据比赛时间自动更新状态
     *
     * @param entity 比赛实体
     * @return 是否需要更新状态
     */
    public static boolean updateStatusByTime(Competition entity) {
        if (entity == null || !Boolean.TRUE.equals(entity.getIsPublished())) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        String currentStatus = entity.getStatus();
        
        // 已取消或已结束的不自动更新
        if ("CANCELLED".equals(currentStatus) || "COMPLETED".equals(currentStatus)) {
            return false;
        }
        
        // 检查比赛是否已结束
        if (entity.getCompetitionEnd() != null && now.isAfter(entity.getCompetitionEnd())) {
            entity.setStatus("COMPLETED");
            return true;
        }
        
        // 检查比赛是否进行中
        if (entity.getCompetitionStart() != null && now.isAfter(entity.getCompetitionStart())) {
            entity.setStatus("ONGOING");
            return true;
        }
        
        // 已发布但未开始
        if ("DRAFT".equals(currentStatus)) {
            entity.setStatus("PUBLISHED");
            return true;
        }
        
        return false;
    }
}
