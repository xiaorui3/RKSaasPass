package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("alumni_graduation_log")
public class AlumniGraduationLog extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("member_id")
    private Long memberId;

    @TableField("alumni_id")
    private Long alumniId;

    @TableField("student_id")
    private String studentId;

    @TableField("process_year")
    private Integer processYear;

    @TableField("action_type")
    private String actionType;

    @TableField("old_grade")
    private String oldGrade;

    @TableField("new_grade")
    private String newGrade;

    private String status;

    private String remark;
}
