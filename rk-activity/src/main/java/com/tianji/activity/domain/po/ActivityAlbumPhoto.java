package com.tianji.activity.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("rk_activity_album_photo")
public class ActivityAlbumPhoto extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("activity_id")
    private Long activityId;

    @TableField("photo_url")
    private String photoUrl;

    @TableField("file_name")
    private String fileName;

    private String description;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("uploader_id")
    private Long uploaderId;
}
