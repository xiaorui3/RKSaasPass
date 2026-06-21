package com.tianji.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.api.dto.user.LoginAuditRecordDTO;
import com.tianji.user.domain.po.SysLogininfor;
import com.tianji.user.domain.vo.LogininforVO;

public interface ISysLogininforService extends IService<SysLogininfor> {

    Page<LogininforVO> queryLogininforPage(Page<LogininforVO> page, String loginName,
                                           String status, String startTime, String endTime);

    void recordLogininfor(LoginAuditRecordDTO dto);

    void deleteLogininfor(Long infoId);
}
