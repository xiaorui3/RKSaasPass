package com.tianji.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.po.VolunteerRecord;
import com.tianji.user.mapper.VolunteerRecordMapper;
import com.tianji.user.service.credit.UserCreditLedgerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 志愿服务管理接口控制器
 */
@Slf4j
@Api(tags = "志愿服务管理接口")
@RestController
@RequestMapping("/api/volunteer")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerRecordMapper volunteerRecordMapper;
    private final UserCreditLedgerService creditLedgerService;

    /**
     * 获取当前用户的志愿服务记录
     */
    @ApiOperation("获取我的志愿服务记录")
    @GetMapping("/my")
    public R<List<VolunteerRecord>> myRecords() {
        Long userId = UserContext.getUser();
        LambdaQueryWrapper<VolunteerRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VolunteerRecord::getUserId, userId)
                .eq(VolunteerRecord::getTenantId, resolveTenantId())
                .orderByDesc(VolunteerRecord::getCreateTime);
        return R.ok(volunteerRecordMapper.selectList(wrapper));
    }

    /**
     * 提交志愿服务记录
     */
    @ApiOperation("提交志愿服务记录")
    @PostMapping
    public R<String> submitRecord(@RequestBody VolunteerRecord record) {
        record.setUserId(UserContext.getUser());
        record.setTenantId(resolveTenantId());
        record.setStatus(0);
        volunteerRecordMapper.insert(record);
        return R.ok("提交成功");
    }

    /**
     * 管理员：分页查询所有志愿服务记录
     */
    @ApiOperation("管理员查询志愿服务记录")
    @GetMapping("/records")
    public R<Page<VolunteerRecord>> listRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer status) {
        Page<VolunteerRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<VolunteerRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VolunteerRecord::getTenantId, resolveTenantId());
        if (status != null) {
            wrapper.eq(VolunteerRecord::getStatus, status);
        }
        wrapper.orderByDesc(VolunteerRecord::getCreateTime);
        return R.ok(volunteerRecordMapper.selectPage(pageParam, wrapper));
    }

    /**
     * 管理员：审核通过志愿服务记录
     */
    @ApiOperation("审核通过志愿服务记录")
    @PutMapping("/{id}/approve")
    public R<String> approveRecord(@PathVariable Long id) {
        VolunteerRecord record = volunteerRecordMapper.selectById(id);
        if (record == null || !resolveTenantId().equals(record.getTenantId())) {
            return R.error("记录不存在");
        }
        record.setStatus(1);
        record.setVerifierId(UserContext.getUser());
        record.setVerifyTime(LocalDateTime.now());
        volunteerRecordMapper.updateById(record);
        creditLedgerService.syncVolunteerApproval(record, UserContext.getUser());
        return R.ok("审核通过");
    }

    /**
     * 管理员：审核拒绝志愿服务记录
     */
    @ApiOperation("审核拒绝志愿服务记录")
    @PutMapping("/{id}/reject")
    public R<String> rejectRecord(@PathVariable Long id,
                                  @RequestParam(required = false) String reason) {
        VolunteerRecord record = volunteerRecordMapper.selectById(id);
        if (record == null || !resolveTenantId().equals(record.getTenantId())) {
            return R.error("记录不存在");
        }
        record.setStatus(2);
        record.setVerifierId(UserContext.getUser());
        record.setVerifyTime(LocalDateTime.now());
        record.setRejectReason(reason);
        volunteerRecordMapper.updateById(record);
        return R.ok("已拒绝");
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
