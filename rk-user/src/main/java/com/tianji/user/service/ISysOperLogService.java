package com.tianji.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.user.domain.po.SysOperLog;
import com.tianji.user.domain.vo.OperLogVO;

public interface ISysOperLogService extends IService<SysOperLog> {

    Page<OperLogVO> queryOperLogPage(Page<OperLogVO> page, String title, String operName,
                                     Integer businessType, Integer status,
                                     String startTime, String endTime);

    void recordOperation(String title, String method, String requestMethod, Integer businessType,
                         String operName, String operUrl, String operIp, String operParam,
                         Integer status, String errorMsg, Long costTime);

    void deleteOperLog(Long operId);
}
