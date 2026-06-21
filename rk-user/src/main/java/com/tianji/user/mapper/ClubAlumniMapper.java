package com.tianji.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.user.domain.po.ClubAlumni;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ClubAlumniMapper extends BaseMapper<ClubAlumni> {

    @Select("SELECT * FROM club_alumni WHERE id = #{id} LIMIT 1")
    ClubAlumni selectAnyById(@Param("id") Long id);

    @Select({
            "<script>",
            "SELECT * FROM club_alumni",
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
    List<ClubAlumni> selectDeletedByTenantIds(@Param("tenantIds") List<Long> tenantIds);

    @Update({
            "<script>",
            "UPDATE club_alumni",
            "SET",
            "  tenant_id = #{alumni.tenantId},",
            "  name = #{alumni.name},",
            "  english_name = #{alumni.englishName},",
            "  student_id = #{alumni.studentId},",
            "  email = #{alumni.email},",
            "  generation_year = #{alumni.generationYear},",
            "  position = #{alumni.position},",
            "  department = #{alumni.department},",
            "  grade_class = #{alumni.gradeClass},",
            "  enrollment_year = #{alumni.enrollmentYear},",
            "  expected_graduation_year = #{alumni.expectedGraduationYear},",
            "  actual_graduation_date = #{alumni.actualGraduationDate},",
            "  major = #{alumni.major},",
            "  skills = #{alumni.skills},",
            "  honor_certificates = #{alumni.honorCertificates},",
            "  work_city = #{alumni.workCity},",
            "  work_unit = #{alumni.workUnit},",
            "  job_content = #{alumni.jobContent},",
            "  current_contact = #{alumni.currentContact},",
            "  notes = #{alumni.notes},",
            "  advice = #{alumni.advice},",
            "  show_table = #{alumni.showTable},",
            "  is_active = #{alumni.isActive},",
            "  is_core_member = #{alumni.isCoreMember},",
            "  member_status = #{alumni.memberStatus},",
            "  graduation_status = #{alumni.graduationStatus},",
            "  update_time = #{alumni.updateTime},",
            "  is_deleted = #{alumni.isDeleted}",
            "WHERE id = #{alumni.id}",
            "</script>"
    })
    int updateIncludingDeleted(@Param("alumni") ClubAlumni alumni);
}
