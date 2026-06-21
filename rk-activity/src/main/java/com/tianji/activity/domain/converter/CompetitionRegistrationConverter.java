package com.tianji.activity.domain.converter;

import com.tianji.activity.domain.dto.CompetitionRegisterDTO;
import com.tianji.activity.domain.po.Competition;
import com.tianji.activity.domain.po.CompetitionParticipant;
import com.tianji.activity.domain.vo.CompetitionRegistrationVO;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 比赛报名对象转换器
 * 用于报名相关的DTO、Entity、VO之间的转换
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
public class CompetitionRegistrationConverter {

    private CompetitionRegistrationConverter() {
        // 工具类禁止实例化
    }

    /**
     * CompetitionParticipant实体转换为CompetitionRegistrationVO
     *
     * @param entity 报名实体
     * @return 报名VO
     */
    public static CompetitionRegistrationVO toVO(CompetitionParticipant entity) {
        if (entity == null) {
            return null;
        }
        CompetitionRegistrationVO vo = new CompetitionRegistrationVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * CompetitionParticipant实体转换为CompetitionRegistrationVO（包含比赛信息）
     *
     * @param entity     报名实体
     * @param competition 比赛实体
     * @return 报名VO
     */
    public static CompetitionRegistrationVO toVO(CompetitionParticipant entity, Competition competition) {
        if (entity == null) {
            return null;
        }
        CompetitionRegistrationVO vo = toVO(entity);
        if (competition != null) {
            vo.setCompetitionTitle(competition.getTitle());
        }
        return vo;
    }

    /**
     * 实体列表转换为VO列表
     *
     * @param entities 实体列表
     * @return VO列表
     */
    public static List<CompetitionRegistrationVO> toVOList(List<CompetitionParticipant> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(CompetitionRegistrationConverter::toVO)
                .collect(Collectors.toList());
    }

    /**
     * CompetitionRegisterDTO转换为CompetitionParticipant实体
     *
     * @param dto          报名DTO
     * @param competitionId 比赛ID
     * @param userId       用户ID
     * @param tenantId     租户ID
     * @return 报名实体
     */
    public static CompetitionParticipant toEntity(CompetitionRegisterDTO dto, Long competitionId, 
                                                   Long userId, Long tenantId) {
        if (dto == null) {
            return null;
        }
        CompetitionParticipant entity = new CompetitionParticipant();
        BeanUtils.copyProperties(dto, entity);
        entity.setCompetitionId(competitionId);
        entity.setUserId(userId);
        entity.setTenantId(tenantId);
        return entity;
    }
}
