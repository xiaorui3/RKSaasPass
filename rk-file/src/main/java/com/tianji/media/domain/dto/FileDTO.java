package com.tianji.media.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@ApiModel(description = "文件信息实体")
@NoArgsConstructor
@AllArgsConstructor
public class FileDTO {
    @ApiModelProperty(value = "文件id", example = "1")
    private Long id;

    @ApiModelProperty(value = "文件名称", example = "image.jpg")
    private String filename;

    @ApiModelProperty(value = "兼容字段：公开访问地址", example = "http://minio/bucket/a.jpg")
    private String path;

    @ApiModelProperty(value = "兼容字段：公开访问地址", example = "http://minio/bucket/a.jpg")
    private String fileUrl;

    @ApiModelProperty(value = "兼容字段：公开访问地址", example = "http://minio/bucket/a.jpg")
    private String url;

    @ApiModelProperty(value = "相对路径", example = "rk-user/avatar/2026/04/23/a.jpg")
    private String relativePath;

    public static FileDTO of(Long id, String filename, String path) {
        FileDTO dto = new FileDTO();
        dto.setId(id);
        dto.setFilename(filename);
        dto.setPath(path);
        dto.setFileUrl(path);
        dto.setUrl(path);
        dto.setRelativePath(path);
        return dto;
    }

    public static FileDTO of(Long id, String filename, String relativePath, String publicUrl) {
        FileDTO dto = new FileDTO();
        dto.setId(id);
        dto.setFilename(filename);
        dto.setRelativePath(relativePath);
        dto.setPath(relativePath);
        dto.setFileUrl(publicUrl);
        dto.setUrl(publicUrl);
        return dto;
    }
}
