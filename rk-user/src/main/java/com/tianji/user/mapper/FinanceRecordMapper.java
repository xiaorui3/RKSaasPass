package com.tianji.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.user.domain.po.FinanceRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 财务记录 Mapper接口
 */
@Mapper
public interface FinanceRecordMapper extends BaseMapper<FinanceRecord> {

}
