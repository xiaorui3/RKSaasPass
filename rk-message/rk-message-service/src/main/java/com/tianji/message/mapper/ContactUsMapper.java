package com.tianji.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.message.domain.po.ContactUsMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 联系我们Mapper接口
 * 基于master分支的contact_us_messages表
 */
@Mapper
public interface ContactUsMapper extends BaseMapper<ContactUsMessage> {
}