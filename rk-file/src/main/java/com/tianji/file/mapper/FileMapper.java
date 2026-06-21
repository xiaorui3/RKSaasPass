package com.tianji.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.file.domain.po.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文件信息Mapper接口
 * 基于master分支的文件上传功能
 */
@Mapper
public interface FileMapper extends BaseMapper<FileInfo> {
    @Select("SELECT COALESCE(SUM(file_size), 0) FROM rk_file WHERE tenant_id = #{tenantId} AND delete_flag = 0")
    Long sumActiveFileSizeByTenant(@Param("tenantId") Long tenantId);
}
