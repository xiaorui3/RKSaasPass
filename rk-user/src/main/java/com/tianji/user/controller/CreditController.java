package com.tianji.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.dto.user.CreditGrantDTO;
import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.po.CreditType;
import com.tianji.user.domain.po.UserCreditRecord;
import com.tianji.user.domain.po.UserCreditSummary;
import com.tianji.user.domain.vo.CreditTypeBreakdownVO;
import com.tianji.user.mapper.CreditTypeMapper;
import com.tianji.user.mapper.UserCreditRecordMapper;
import com.tianji.user.mapper.UserCreditSummaryMapper;
import com.tianji.user.service.credit.UserCreditLedgerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Api(tags = "学分管理接口")
@RestController
@RequestMapping("/api/credit")
@RequiredArgsConstructor
public class CreditController {

    private final CreditTypeMapper creditTypeMapper;
    private final UserCreditRecordMapper userCreditRecordMapper;
    private final UserCreditSummaryMapper userCreditSummaryMapper;
    private final UserCreditLedgerService creditLedgerService;

    @ApiOperation("获取学分类型列表")
    @GetMapping("/types")
    public R<List<CreditType>> listCreditTypes() {
        try {
            QueryWrapper<CreditType> wrapper = new QueryWrapper<>();
            Long tenantId = resolveTenantId();
            wrapper.eq("tenant_id", tenantId).orderByAsc("id");
            List<CreditType> types = creditTypeMapper.selectList(wrapper);
            if (types == null || types.isEmpty()) {
                return R.ok(defaultCreditTypes(tenantId));
            }
            return R.ok(types);
        } catch (Exception e) {
            log.warn("load credit types failed, fallback to defaults: {}", e.getMessage());
            return R.ok(defaultCreditTypes(resolveTenantId()));
        }
    }

    @ApiOperation("保存学分类型")
    @PostMapping("/types")
    public R<String> saveCreditType(@RequestBody CreditType type) {
        if (type == null || !StringUtils.hasText(type.getName()) || !StringUtils.hasText(type.getCode())) {
            return R.error("学分类型名称和编码不能为空");
        }
        type.setTenantId(resolveTenantId());
        if (type.getMaxCredit() == null) {
            type.setMaxCredit(BigDecimal.ZERO);
        }
        if (type.getId() == null) {
            creditTypeMapper.insert(type);
        } else {
            creditTypeMapper.updateById(type);
        }
        return R.ok("保存成功");
    }

    @ApiOperation("删除学分类型")
    @DeleteMapping("/types/{id}")
    public R<String> deleteCreditType(@PathVariable Long id) {
        CreditType type = creditTypeMapper.selectById(id);
        if (type == null || !resolveTenantId().equals(type.getTenantId())) {
            return R.error("学分类型不存在");
        }
        creditTypeMapper.deleteById(id);
        return R.ok("删除成功");
    }

    @ApiOperation("获取我的学分记录")
    @GetMapping("/my/records")
    public R<Page<UserCreditRecord>> myRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserContext.getUser();
        Page<UserCreditRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<UserCreditRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCreditRecord::getUserId, userId)
                .eq(UserCreditRecord::getTenantId, resolveTenantId())
                .orderByDesc(UserCreditRecord::getCreateTime);
        return R.ok(userCreditRecordMapper.selectPage(pageParam, wrapper));
    }

    @ApiOperation("获取我的学分汇总")
    @GetMapping("/my/summary")
    public R<UserCreditSummary> mySummary() {
        Long userId = UserContext.getUser();
        LambdaQueryWrapper<UserCreditSummary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCreditSummary::getUserId, userId)
                .eq(UserCreditSummary::getTenantId, resolveTenantId());
        return R.ok(userCreditSummaryMapper.selectOne(wrapper));
    }

    @ApiOperation("获取我的学分分类汇总")
    @GetMapping("/my/breakdown")
    public R<List<CreditTypeBreakdownVO>> myBreakdown() {
        return R.ok(creditLedgerService.listBreakdown(
                UserContext.getUser(),
                resolveTenantId(),
                loadConfiguredTypes(resolveTenantId())
        ));
    }

    @ApiOperation("分页查询学分记录")
    @GetMapping("/records")
    public R<Page<UserCreditRecord>> listRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId) {
        Page<UserCreditRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<UserCreditRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCreditRecord::getTenantId, resolveTenantId());
        if (status != null) {
            wrapper.eq(UserCreditRecord::getStatus, status);
        }
        if (userId != null) {
            wrapper.eq(UserCreditRecord::getUserId, userId);
        }
        wrapper.orderByDesc(UserCreditRecord::getCreateTime);
        return R.ok(userCreditRecordMapper.selectPage(pageParam, wrapper));
    }

    @ApiOperation("新增学分记录")
    @PostMapping("/records")
    public R<String> addRecord(@RequestBody UserCreditRecord record) {
        record.setStatus(0);
        if (record.getTenantId() == null) {
            record.setTenantId(resolveTenantId());
        }
        userCreditRecordMapper.insert(record);
        return R.ok("添加成功");
    }

    @ApiOperation("通过学分记录")
    @PutMapping("/records/{id}/approve")
    public R<String> approveRecord(@PathVariable Long id) {
        UserCreditRecord record = userCreditRecordMapper.selectById(id);
        if (record == null || !resolveTenantId().equals(record.getTenantId())) {
            return R.error("记录不存在");
        }
        record.setStatus(1);
        record.setVerifierId(UserContext.getUser());
        record.setVerifyTime(java.time.LocalDateTime.now());
        userCreditRecordMapper.updateById(record);
        creditLedgerService.rebuildSummary(record.getUserId(), record.getTenantId());
        return R.ok("审核通过");
    }

    @ApiOperation("拒绝学分记录")
    @PutMapping("/records/{id}/reject")
    public R<String> rejectRecord(@PathVariable Long id, @RequestParam(required = false) String reason) {
        UserCreditRecord record = userCreditRecordMapper.selectById(id);
        if (record == null || !resolveTenantId().equals(record.getTenantId())) {
            return R.error("记录不存在");
        }
        record.setStatus(2);
        record.setVerifierId(UserContext.getUser());
        record.setVerifyTime(java.time.LocalDateTime.now());
        record.setRejectReason(reason);
        userCreditRecordMapper.updateById(record);
        creditLedgerService.rebuildSummary(record.getUserId(), record.getTenantId());
        return R.ok("已拒绝");
    }

    @ApiIgnore
    @PostMapping("/internal/grants/batch")
    public Integer grantCreditsInternal(@RequestBody List<CreditGrantDTO> grants) {
        return creditLedgerService.grantCredits(grants, UserContext.getUser());
    }

    private List<CreditType> loadConfiguredTypes(Long tenantId) {
        QueryWrapper<CreditType> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId).orderByAsc("id");
        List<CreditType> types = creditTypeMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(types)) {
            return defaultCreditTypes(tenantId);
        }
        return types;
    }

    private List<CreditType> defaultCreditTypes(Long tenantId) {
        Long resolvedTenantId = tenantId == null ? 1L : tenantId;
        List<CreditType> defaults = new ArrayList<>();
        defaults.add(buildCreditType(1L, resolvedTenantId, "活动学分", "activity", "活动参与学分", new BigDecimal("10.0")));
        defaults.add(buildCreditType(2L, resolvedTenantId, "竞赛学分", "competition", "竞赛获奖学分", new BigDecimal("20.0")));
        defaults.add(buildCreditType(3L, resolvedTenantId, "志愿服务", "volunteer", "志愿服务学分", new BigDecimal("20.0")));
        defaults.add(buildCreditType(4L, resolvedTenantId, "社会实践", "practice", "社会实践学分", new BigDecimal("15.0")));
        return defaults;
    }

    private CreditType buildCreditType(Long id, Long tenantId, String name, String code, String description, BigDecimal maxCredit) {
        CreditType type = new CreditType();
        type.setId(id);
        type.setTenantId(tenantId);
        type.setName(name);
        type.setCode(code);
        type.setDescription(description);
        type.setMaxCredit(maxCredit);
        return type;
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
