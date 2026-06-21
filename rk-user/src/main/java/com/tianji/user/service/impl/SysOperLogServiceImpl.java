package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.utils.BeanUtils;
import com.tianji.user.domain.po.SysOperLog;
import com.tianji.user.domain.vo.OperLogVO;
import com.tianji.user.mapper.SysOperLogMapper;
import com.tianji.user.service.ISysOperLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements ISysOperLogService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Page<OperLogVO> queryOperLogPage(Page<OperLogVO> page, String title, String operName,
                                            Integer businessType, Integer status,
                                            String startTime, String endTime) {
        LambdaQueryWrapper<SysOperLog> queryWrapper = new LambdaQueryWrapper<>();

        if (title != null && !title.trim().isEmpty()) {
            queryWrapper.like(SysOperLog::getTitle, title);
        }
        if (operName != null && !operName.trim().isEmpty()) {
            queryWrapper.like(SysOperLog::getOperName, operName);
        }
        if (businessType != null) {
            queryWrapper.eq(SysOperLog::getBusinessType, businessType);
        }
        if (status != null) {
            queryWrapper.eq(SysOperLog::getStatus, status);
        }
        if (startTime != null && !startTime.trim().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(startTime, DATE_FORMATTER);
            queryWrapper.ge(SysOperLog::getOperTime, start);
        }
        if (endTime != null && !endTime.trim().isEmpty()) {
            LocalDateTime end = LocalDateTime.parse(endTime, DATE_FORMATTER);
            queryWrapper.le(SysOperLog::getOperTime, end);
        }

        queryWrapper.orderByDesc(SysOperLog::getOperTime);

        Page<SysOperLog> operLogPage = this.page(new Page<>(page.getCurrent(), page.getSize()), queryWrapper);
        Page<OperLogVO> resultPage = new Page<>(operLogPage.getCurrent(), operLogPage.getSize(), operLogPage.getTotal());
        resultPage.setRecords(BeanUtils.copyList(operLogPage.getRecords(), OperLogVO.class));
        return resultPage;
    }

    @Override
    public void recordOperation(String title, String method, String requestMethod, Integer businessType,
                                String operName, String operUrl, String operIp, String operParam,
                                Integer status, String errorMsg, Long costTime) {
        SysOperLog record = new SysOperLog();
        record.setTitle(limit(title, 255));
        record.setBusinessType(businessType);
        record.setMethod(limit(method, 255));
        record.setRequestMethod(limit(requestMethod, 16));
        record.setOperatorType(1);
        record.setOperName(limit(operName, 128));
        record.setOperUrl(limit(operUrl, 500));
        record.setOperIp(limit(operIp, 64));
        record.setOperParam(operParam);
        record.setStatus(status);
        record.setErrorMsg(errorMsg);
        record.setOperTime(LocalDateTime.now());
        record.setCostTime(costTime);
        this.save(record);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    @Override
    public void deleteOperLog(Long operId) {
        log.info("delete operation audit, operId={}", operId);
        this.removeById(operId);
    }
}
