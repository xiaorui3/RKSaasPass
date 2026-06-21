package com.tianji.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.RKTenant;
import com.tianji.user.domain.po.RegisterReviewRequest;
import com.tianji.user.mapper.RKTenantMapper;
import com.tianji.user.mapper.RegisterReviewRequestMapper;
import com.tianji.user.service.IAdmissionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Api(tags = "注册审核管理")
@RestController
@RequestMapping("/api/admission/register-review")
@RequiredArgsConstructor
public class RegisterReviewAdminController {

    private final RegisterReviewRequestMapper registerReviewRequestMapper;
    private final RKTenantMapper tenantMapper;
    private final IAdmissionService admissionService;

    @ApiOperation("获取全部注册审核记录")
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('audit:member:*', 'audit:member:list')")
    public R<List<RegisterReviewRequest>> list() {
        admissionService.reconcileApprovedMembersForCurrentTenant();
        LambdaQueryWrapper<RegisterReviewRequest> queryWrapper = new LambdaQueryWrapper<>();
        applyTenantScope(queryWrapper);
        queryWrapper.orderByDesc(RegisterReviewRequest::getCreateTime);
        return R.ok(registerReviewRequestMapper.selectList(queryWrapper));
    }

    @ApiOperation("按状态获取注册审核记录")
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('audit:member:*', 'audit:member:list')")
    public R<List<RegisterReviewRequest>> listByStatus(@PathVariable String status) {
        admissionService.reconcileApprovedMembersForCurrentTenant();
        LambdaQueryWrapper<RegisterReviewRequest> queryWrapper = new LambdaQueryWrapper<>();
        applyTenantScope(queryWrapper);
        queryWrapper.eq(RegisterReviewRequest::getReviewStatus, status)
                .orderByDesc(RegisterReviewRequest::getCreateTime);
        return R.ok(registerReviewRequestMapper.selectList(queryWrapper));
    }

    @ApiOperation("获取注册审核统计")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyAuthority('audit:member:*', 'audit:member:list', 'content:statistics:view')")
    public R<Map<String, Object>> statistics() {
        admissionService.reconcileApprovedMembersForCurrentTenant();
        LambdaQueryWrapper<RegisterReviewRequest> queryWrapper = new LambdaQueryWrapper<>();
        applyTenantScope(queryWrapper);
        List<RegisterReviewRequest> records = registerReviewRequestMapper.selectList(queryWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("total", records.size());
        result.put("pendingCount", records.stream().filter(item -> "PENDING".equalsIgnoreCase(item.getReviewStatus())).count());
        result.put("approvedCount", records.stream().filter(item -> "APPROVED".equalsIgnoreCase(item.getReviewStatus())).count());
        result.put("rejectedCount", records.stream().filter(item -> "REJECTED".equalsIgnoreCase(item.getReviewStatus())).count());
        return R.ok(result);
    }

    @ApiOperation("审核注册请求")
    @PostMapping("/review")
    @PreAuthorize("hasAnyAuthority('audit:member:*', 'audit:member:review')")
    public R<String> review(@RequestBody Map<String, Object> payload) {
        try {
            Long id = Long.valueOf(String.valueOf(payload.get("id")));
            String reviewStatus = String.valueOf(payload.get("reviewStatus"));
            String reviewComment = payload.get("reviewComment") == null ? null : String.valueOf(payload.get("reviewComment"));
            Long reviewerId = payload.get("reviewerId") == null ? null : Long.valueOf(String.valueOf(payload.get("reviewerId")));

            RegisterReviewRequest request = registerReviewRequestMapper.selectById(id);
            if (request == null) {
                return R.error("注册审核记录不存在");
            }
            if (!canAccessTenant(request.getTenantId())) {
                return R.error("无权处理该租户的注册审核记录");
            }

            if (!admissionService.reviewRegisterRequest(id, reviewStatus, reviewComment, reviewerId)) {
                return R.error("注册审核处理失败");
            }
            return R.ok("注册审核处理成功");
        } catch (Exception e) {
            log.error("review register request failed", e);
            return R.error("注册审核处理失败: " + e.getMessage());
        }
    }

    private void applyTenantScope(LambdaQueryWrapper<RegisterReviewRequest> queryWrapper) {
        if (Boolean.TRUE.equals(TenantContext.isSuperAdmin())) {
            List<Long> activeTenantIds = resolveActiveTenantIds();
            if (activeTenantIds.isEmpty()) {
                queryWrapper.eq(RegisterReviewRequest::getTenantId, -1L);
                return;
            }
            queryWrapper.in(RegisterReviewRequest::getTenantId, activeTenantIds);
            return;
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            queryWrapper.eq(RegisterReviewRequest::getTenantId, tenantId);
            return;
        }
        queryWrapper.eq(RegisterReviewRequest::getTenantId, 1L);
    }

    private boolean canAccessTenant(Long targetTenantId) {
        if (Boolean.TRUE.equals(TenantContext.isSuperAdmin())) {
            return true;
        }
        Long tenantId = TenantContext.getTenantId();
        return Objects.equals(tenantId == null ? 1L : tenantId, targetTenantId);
    }

    private List<Long> resolveActiveTenantIds() {
        LambdaQueryWrapper<RKTenant> tenantQuery = new LambdaQueryWrapper<>();
        tenantQuery.select(RKTenant::getId)
                .eq(RKTenant::getStatus, 1)
                .eq(RKTenant::getIsDeleted, 0)
                .and(query -> query.isNull(RKTenant::getExpireTime)
                        .or()
                        .gt(RKTenant::getExpireTime, LocalDateTime.now()));
        return tenantMapper.selectList(tenantQuery).stream()
                .map(RKTenant::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
