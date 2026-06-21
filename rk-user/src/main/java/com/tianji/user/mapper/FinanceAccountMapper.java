package com.tianji.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.user.domain.po.FinanceAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 财务账户 Mapper接口
 */
@Mapper
public interface FinanceAccountMapper extends BaseMapper<FinanceAccount> {

}
