package com.tianji.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.R;
import com.tianji.user.domain.dto.ReferralCodeDTO;
import com.tianji.user.domain.dto.ReferralCodeQueryDTO;
import com.tianji.user.domain.vo.ReferralConversionOverviewVO;
import com.tianji.user.service.IReferralCodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Api(tags = "Referral Code")
@RestController
@RequestMapping("/api/referral-codes")
@RequiredArgsConstructor
public class ReferralCodeController {

    private final IReferralCodeService referralCodeService;

    @ApiOperation("Query referral codes")
    @GetMapping
    public R<Page<ReferralCodeDTO>> queryReferralCodes(ReferralCodeQueryDTO queryDTO) {
        try {
            return R.ok(referralCodeService.queryReferralCodes(queryDTO));
        } catch (Exception e) {
            log.error("query referral codes failed", e);
            return R.error("query referral codes failed: " + e.getMessage());
        }
    }

    @ApiOperation("Get current tenant referral code for current user")
    @GetMapping("/current")
    public R<ReferralCodeDTO> getCurrentReferralCode() {
        try {
            return R.ok(referralCodeService.getCurrentTenantReferralCode());
        } catch (Exception e) {
            log.error("get current referral code failed", e);
            return R.error("get current referral code failed: " + e.getMessage());
        }
    }

    @ApiOperation("Refresh referral code for current tenant")
    @PostMapping("/current/refresh")
    public R<ReferralCodeDTO> refreshCurrentReferralCode() {
        try {
            return R.ok(referralCodeService.refreshCurrentTenantReferralCode());
        } catch (Exception e) {
            log.error("refresh referral code failed", e);
            return R.error("refresh referral code failed: " + e.getMessage());
        }
    }

    @ApiOperation("Create referral code")
    @PostMapping
    public R<String> generateReferralCode(@RequestBody ReferralCodeDTO dto) {
        try {
            String code = referralCodeService.generateReferralCode(dto);
            log.info("created referral code {}", code);
            return R.ok(code);
        } catch (Exception e) {
            log.error("create referral code failed", e);
            return R.error("create referral code failed: " + e.getMessage());
        }
    }

    @ApiOperation("Generate unique referral code")
    @GetMapping("/generate-code")
    public R<String> generateUniqueCode() {
        try {
            return R.ok(referralCodeService.generateUniqueCode());
        } catch (Exception e) {
            log.error("generate unique referral code failed", e);
            return R.error("generate unique referral code failed: " + e.getMessage());
        }
    }

    @ApiOperation("Update referral code")
    @PutMapping("/{id}")
    public R<Void> updateReferralCode(@PathVariable Long id, @RequestBody ReferralCodeDTO dto) {
        try {
            referralCodeService.updateReferralCode(id, dto);
            return R.ok();
        } catch (Exception e) {
            log.error("update referral code failed, id={}", id, e);
            return R.error("update referral code failed: " + e.getMessage());
        }
    }

    @ApiOperation("Delete referral code")
    @DeleteMapping("/{id}")
    public R<Void> deleteReferralCode(@PathVariable Long id) {
        try {
            referralCodeService.deleteReferralCode(id);
            return R.ok();
        } catch (Exception e) {
            log.error("delete referral code failed, id={}", id, e);
            return R.error("delete referral code failed: " + e.getMessage());
        }
    }

    @ApiOperation("Get referral code detail")
    @GetMapping("/{id}")
    public R<ReferralCodeDTO> getReferralCodeDetail(@PathVariable Long id) {
        try {
            return R.ok(referralCodeService.getReferralCodeDetail(id));
        } catch (Exception e) {
            log.error("get referral code detail failed, id={}", id, e);
            return R.error("get referral code detail failed: " + e.getMessage());
        }
    }

    @ApiOperation("Get referral conversion overview")
    @GetMapping("/{id}/conversions")
    public R<ReferralConversionOverviewVO> getReferralConversionOverview(@PathVariable Long id) {
        try {
            return R.ok(referralCodeService.getReferralConversionOverview(id));
        } catch (Exception e) {
            log.error("get referral conversion overview failed, id={}", id, e);
            return R.error("get referral conversion overview failed: " + e.getMessage());
        }
    }

    @ApiOperation("Validate referral code")
    @GetMapping("/validate")
    public R<Boolean> validateReferralCode(@RequestParam String code) {
        try {
            return R.ok(referralCodeService.validateReferralCode(code));
        } catch (Exception e) {
            log.error("validate referral code failed, code={}", code, e);
            return R.error("validate referral code failed: " + e.getMessage());
        }
    }

    @ApiOperation("Internal validate referral code")
    @PostMapping("/internal/validate")
    public Boolean validateReferralCodeInternal(@RequestParam String code, @RequestParam Long tenantId) {
        return referralCodeService.validateReferralCode(code, tenantId);
    }

    @ApiOperation("Internal mark register success")
    @PostMapping("/internal/register-success")
    public Boolean markGenericRegisterSuccess(
            @RequestParam Long tenantId,
            @RequestParam String referralCode,
            @RequestParam String targetEmail,
            @RequestParam Long authUserId) {
        return referralCodeService.markGenericRegisterSuccess(tenantId, referralCode, targetEmail, authUserId);
    }
}
