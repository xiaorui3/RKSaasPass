package com.tianji.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.user.domain.po.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 学员用户表 Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2022-06-28
 */
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT id, tenant_id AS tenantId, mobile AS cellPhone, is_deleted AS isDeleted " +
            "FROM rk_user WHERE tenant_id = #{tenantId} AND mobile = #{cellPhone} LIMIT 1")
    User selectAnyByTenantAndCellPhone(@Param("tenantId") Long tenantId, @Param("cellPhone") String cellPhone);

    @Select("SELECT id, auth_user_id AS authUserId, tenant_id AS tenantId, username, real_name AS realName, " +
            "nickname, email, is_deleted AS isDeleted " +
            "FROM rk_user WHERE tenant_id = #{tenantId} AND username = #{username} " +
            "ORDER BY is_deleted ASC, update_time DESC, id DESC LIMIT 1")
    User selectAnyByTenantAndUsername(@Param("tenantId") Long tenantId, @Param("username") String username);
}
