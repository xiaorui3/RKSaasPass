package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("alumni_profile_token")
public class AlumniProfileToken extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("alumni_id")
    private Long alumniId;

    @TableField("member_id")
    private Long memberId;

    private String email;

    private String token;

    private String status;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField("submitted_time")
    private LocalDateTime submittedTime;
}
