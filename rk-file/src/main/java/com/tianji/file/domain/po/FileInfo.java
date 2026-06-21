package com.tianji.file.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件管理实体类
 * 基于master分支的文件上传功能
 */
@Data
@TableName("rk_file")
public class FileInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 原始文件名（兼容字段）
     */
    private String originalName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件URL（兼容字段）
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型（MIME类型）
     */
    private String fileType;

    /**
     * 文件扩展名
     */
    private String extension;

    /**
     * MIME类型（兼容字段）
     */
    private String mimeType;

    /**
     * MinIO存储桶名称
     */
    private String bucketName;

    /**
     * MinIO对象名称
     */
    private String objectName;

    /**
     * 租户ID（企业级多租户标准）
     */
    private Long tenantId;

    /**
     * 上传者ID
     */
    private String uploadedBy;

    /**
     * 下载次数
     */
    private Integer downloadCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除标志（0-未删除，1-已删除）
     */
    @TableLogic
    private Integer deleteFlag;
}