package com.tianji.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.user.domain.po.UserCreditSummary;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户学分汇总 Mapper接口
 */
@Mapper
public interface UserCreditSummaryMapper extends BaseMapper<UserCreditSummary> {

}
