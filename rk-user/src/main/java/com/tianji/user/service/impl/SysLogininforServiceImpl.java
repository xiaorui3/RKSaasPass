package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.user.LoginAuditRecordDTO;
import com.tianji.common.utils.BeanUtils;
import com.tianji.user.domain.po.SysLogininfor;
import com.tianji.user.domain.vo.LogininforVO;
import com.tianji.user.mapper.SysLogininforMapper;
import com.tianji.user.service.ISysLogininforService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class SysLogininforServiceImpl extends ServiceImpl<SysLogininforMapper, SysLogininfor> implements ISysLogininforService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_LOGIN_MSG_LENGTH = 500;

    @Override
    public Page<LogininforVO> queryLogininforPage(Page<LogininforVO> page, String loginName,
                                                  String status, String startTime, String endTime) {
        LambdaQueryWrapper<SysLogininfor> queryWrapper = new LambdaQueryWrapper<>();

        if (loginName != null && !loginName.trim().isEmpty()) {
            queryWrapper.like(SysLogininfor::getLoginName, loginName);
        }

        if (status != null && !status.trim().isEmpty()) {
            queryWrapper.eq(SysLogininfor::getStatus, status);
        }

        if (startTime != null && !startTime.trim().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(startTime, DATE_FORMATTER);
            queryWrapper.ge(SysLogininfor::getLoginTime, start);
        }
        if (endTime != null && !endTime.trim().isEmpty()) {
            LocalDateTime end = LocalDateTime.parse(endTime, DATE_FORMATTER);
            queryWrapper.le(SysLogininfor::getLoginTime, end);
        }

        queryWrapper.orderByDesc(SysLogininfor::getLoginTime);

        Page<SysLogininfor> logininforPage = this.page(new Page<>(page.getCurrent(), page.getSize()), queryWrapper);

        Page<LogininforVO> resultPage = new Page<>(logininforPage.getCurrent(), logininforPage.getSize(), logininforPage.getTotal());
        resultPage.setRecords(BeanUtils.copyList(logininforPage.getRecords(), LogininforVO.class));
        return resultPage;
    }

    @Override
    public void recordLogininfor(LoginAuditRecordDTO dto) {
        if (dto == null || dto.getLoginName() == null || dto.getLoginName().trim().isEmpty()) {
            return;
        }
        SysLogininfor record = new SysLogininfor();
        record.setLoginName(dto.getLoginName());
        record.setIpaddr(dto.getIpaddr());
        record.setBrowser(dto.getBrowser());
        record.setOs(dto.getOs());
        record.setStatus(dto.getStatus());
        record.setMsg(truncate(dto.getMsg(), MAX_LOGIN_MSG_LENGTH));
        record.setLoginTime(LocalDateTime.now());
        this.save(record);
    }

    @Override
    public void deleteLogininfor(Long infoId) {
        log.info("delete login audit, infoId={}", infoId);
        this.removeById(infoId);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
