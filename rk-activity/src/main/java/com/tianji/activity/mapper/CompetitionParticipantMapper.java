package com.tianji.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.activity.domain.po.CompetitionParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 比赛参与者Mapper接口
 */
@Mapper
public interface CompetitionParticipantMapper extends BaseMapper<CompetitionParticipant> {

    /**
     * 根据比赛ID和用户ID查询报名记录
     *
     * @param competitionId 比赛ID
     * @param userId 用户ID
     * @return 报名记录
     */
    @Select("SELECT * FROM competition_participants WHERE competition_id = #{competitionId} AND user_id = #{userId} AND is_deleted = 0")
    CompetitionParticipant findByCompetitionAndUser(@Param("competitionId") Long competitionId, @Param("userId") Long userId);

    /**
     * 根据比赛ID和学号查询报名记录
     *
     * @param competitionId 比赛ID
     * @param studentId 学号
     * @return 报名记录
     */
    @Select("SELECT * FROM competition_participants WHERE competition_id = #{competitionId} AND student_id = #{studentId} AND is_deleted = 0")
    CompetitionParticipant findByCompetitionAndStudentId(@Param("competitionId") Long competitionId, @Param("studentId") String studentId);

    /**
     * 统计比赛的报名人数
     *
     * @param competitionId 比赛ID
     * @return 报名人数
     */
    @Select("SELECT COUNT(*) FROM competition_participants WHERE competition_id = #{competitionId} AND status IN ('registered', 'approved') AND is_deleted = 0")
    int countByCompetitionId(@Param("competitionId") Long competitionId);

    /**
     * 根据用户ID查询报名记录列表
     *
     * @param userId 用户ID
     * @return 报名记录列表
     */
    @Select("SELECT * FROM competition_participants WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY registration_time DESC")
    List<CompetitionParticipant> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM competition_participants " +
            "WHERE competition_id = #{competitionId} AND status <> 'cancelled' AND is_deleted = 0 " +
            "ORDER BY registration_time DESC")
    List<CompetitionParticipant> findActiveByCompetitionId(@Param("competitionId") Long competitionId);

    @Select("SELECT * FROM competition_participants " +
            "WHERE competition_id = #{competitionId} AND status <> 'cancelled' AND is_deleted = 0 " +
            "ORDER BY ranking ASC, score DESC, registration_time ASC")
    List<CompetitionParticipant> findCreditCandidatesByCompetitionId(@Param("competitionId") Long competitionId);
}
