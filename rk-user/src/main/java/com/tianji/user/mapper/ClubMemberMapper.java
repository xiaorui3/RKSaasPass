package com.tianji.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.user.domain.po.ClubMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ClubMemberMapper extends BaseMapper<ClubMember> {

    @Select("SELECT * FROM club_members WHERE tenant_id = #{tenantId} AND student_id = #{studentId} ORDER BY id DESC LIMIT 1")
    ClubMember selectAnyByTenantAndStudentId(@Param("tenantId") Long tenantId, @Param("studentId") String studentId);

    @Select("SELECT * FROM club_members WHERE id = #{id} LIMIT 1")
    ClubMember selectAnyById(@Param("id") Long id);

    @Select({
            "<script>",
            "SELECT * FROM club_members",
            "WHERE is_deleted = 1",
            "<choose>",
            "  <when test='tenantIds != null and tenantIds.size() > 0'>",
            "    AND tenant_id IN",
            "    <foreach collection='tenantIds' item='tenantId' open='(' close=')' separator=','>#{tenantId}</foreach>",
            "  </when>",
            "  <otherwise>",
            "    AND tenant_id = -1",
            "  </otherwise>",
            "</choose>",
            "ORDER BY update_time DESC, id DESC",
            "</script>"
    })
    List<ClubMember> selectDeletedByTenantIds(@Param("tenantIds") List<Long> tenantIds);

    @Update({
            "<script>",
            "UPDATE club_members",
            "SET",
            "  tenant_id = #{member.tenantId},",
            "  name = #{member.name},",
            "  student_id = #{member.studentId},",
            "  email = #{member.email},",
            "  phone = #{member.phone},",
            "  major = #{member.major},",
            "  grade = #{member.grade},",
            "  department = #{member.department},",
            "  position = #{member.position},",
            "  join_date = #{member.joinDate},",
            "  status = #{member.status},",
            "  update_time = #{member.updateTime},",
            "  is_deleted = #{member.isDeleted}",
            "WHERE id = #{member.id}",
            "</script>"
    })
    int updateIncludingDeleted(@Param("member") ClubMember member);
}
