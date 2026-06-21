package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.dto.ReferralCodeDTO;
import com.tianji.user.domain.dto.ReferralCodeQueryDTO;
import com.tianji.user.domain.po.ReferralCode;
import com.tianji.user.domain.po.ReferralConversionRecord;
import com.tianji.user.domain.vo.ReferralConversionOverviewVO;
import com.tianji.user.domain.vo.ReferralConversionRecordVO;
import com.tianji.user.mapper.ReferralCodeMapper;
import com.tianji.user.mapper.ReferralConversionRecordMapper;
import com.tianji.user.service.IReferralCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralCodeServiceImpl extends ServiceImpl<ReferralCodeMapper, ReferralCode> implements IReferralCodeService {

    private static final String CODE_PREFIX = "REF";
    private static final int DEFAULT_MAX_USES = 1;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_INACTIVE = 0;

    private final ReferralCodeMapper referralCodeMapper;
    private final ReferralConversionRecordMapper referralConversionRecordMapper;

    @Override
    public Page<ReferralCodeDTO> queryReferralCodes(ReferralCodeQueryDTO queryDTO) {
        log.info("查询内推码列表，查询条件: {}", queryDTO);

        Page<ReferralCode> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        LambdaQueryWrapper<ReferralCode> queryWrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getCode() != null && !queryDTO.getCode().isEmpty()) {
            queryWrapper.like(ReferralCode::getCode, queryDTO.getCode());
        }
        if (queryDTO.getGeneratorId() != null) {
            queryWrapper.eq(ReferralCode::getGeneratorId, queryDTO.getGeneratorId());
        }
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(ReferralCode::getStatus, queryDTO.getStatus());
        }
        if ("create_time".equals(queryDTO.getSortBy())) {
            if ("desc".equals(queryDTO.getSortOrder())) {
                queryWrapper.orderByDesc(ReferralCode::getCreateTime);
            } else {
                queryWrapper.orderByAsc(ReferralCode::getCreateTime);
            }
        }
        queryWrapper.orderByDesc(ReferralCode::getId);

        Page<ReferralCode> resultPage = referralCodeMapper.selectPage(page, queryWrapper);
        Page<ReferralCodeDTO> dtoPage = new Page<>();
        BeanUtils.copyProperties(resultPage, dtoPage, "records");
        dtoPage.setRecords(resultPage.getRecords().stream().map(code -> {
            ReferralCodeDTO dto = new ReferralCodeDTO();
            BeanUtils.copyProperties(code, dto);
            dto.setGeneratorId(code.getGeneratorId());
            return dto;
        }).collect(Collectors.toList()));
        return dtoPage;
    }

    @Override
    @Transactional
    public String generateReferralCode(ReferralCodeDTO dto) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        LambdaQueryWrapper<ReferralCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReferralCode::getCode, dto.getCode());
        if (referralCodeMapper.selectOne(wrapper) != null) {
            throw new RuntimeException("内推码已存在");
        }

        ReferralCode referralCode = new ReferralCode();
        referralCode.setCode(dto.getCode());
        referralCode.setMaxUses(dto.getMaxUses());
        referralCode.setUsedCount(0);
        referralCode.setExpiresAt(dto.getExpiresAt());
        referralCode.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        referralCode.setGeneratorId(userId);
        referralCode.setTenantId(resolveTenantId());
        referralCodeMapper.insert(referralCode);
        return dto.getCode();
    }

    @Override
    @Transactional
    public void updateReferralCode(Long id, ReferralCodeDTO dto) {
        ReferralCode referralCode = referralCodeMapper.selectById(id);
        if (referralCode == null) {
            throw new RuntimeException("内推码不存在");
        }
        if (dto.getMaxUses() != null) {
            referralCode.setMaxUses(dto.getMaxUses());
        }
        if (dto.getExpiresAt() != null) {
            referralCode.setExpiresAt(dto.getExpiresAt());
        }
        if (dto.getStatus() != null) {
            referralCode.setStatus(dto.getStatus());
        }
        referralCodeMapper.updateById(referralCode);
    }

    @Override
    @Transactional
    public void deleteReferralCode(Long id) {
        ReferralCode referralCode = referralCodeMapper.selectById(id);
        if (referralCode == null) {
            throw new RuntimeException("内推码不存在");
        }
        if (referralCode.getUsedCount() != null && referralCode.getUsedCount() > 0) {
            throw new RuntimeException("内推码已被使用，无法删除");
        }
        referralCodeMapper.deleteById(id);
    }

    @Override
    public ReferralCodeDTO getReferralCodeDetail(Long id) {
        ReferralCode referralCode = referralCodeMapper.selectById(id);
        if (referralCode == null) {
            throw new RuntimeException("内推码不存在");
        }
        ReferralCodeDTO dto = new ReferralCodeDTO();
        BeanUtils.copyProperties(referralCode, dto);
        dto.setGeneratorId(referralCode.getGeneratorId());
        return dto;
    }

    @Override
    public String generateUniqueCode() {
        String year = String.valueOf(LocalDateTime.now().getYear()).substring(2);
        String randomNum = String.format("%05d", new Random().nextInt(100000));
        return CODE_PREFIX + year + randomNum;
    }

    @Override
    public boolean validateReferralCode(String code) {
        try {
            resolveAvailableReferralCode(code, null);
            return true;
        } catch (RuntimeException e) {
            log.warn("内推码校验失败, code={}, error={}", code, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean validateReferralCode(String code, Long tenantId) {
        resolveAvailableReferralCode(code, tenantId);
        return true;
    }

    @Override
    @Transactional
    public boolean markGenericRegisterSuccess(Long tenantId, String referralCode, String targetEmail, Long authUserId) {
        ReferralCode code = resolveAvailableReferralCode(referralCode, tenantId);
        ReferralConversionRecord existing = findGenericConversionRecord(code.getId(), targetEmail, "REGISTER");
        if (existing != null && "REGISTER_SUCCESS".equals(existing.getConversionStatus())) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            ReferralConversionRecord record = new ReferralConversionRecord();
            record.setTenantId(tenantId);
            record.setReferralCodeId(code.getId());
            record.setReferralCode(code.getCode());
            record.setTargetEmail(targetEmail);
            record.setConversionType("REGISTER");
            record.setConversionStatus("REGISTER_SUCCESS");
            record.setAuthUserId(authUserId);
            record.setConvertedTime(now);
            record.setCreateTime(now);
            record.setUpdateTime(now);
            record.setIsDeleted(0);
            referralConversionRecordMapper.insert(record);
        } else {
            existing.setConversionStatus("REGISTER_SUCCESS");
            existing.setAuthUserId(authUserId);
            existing.setConvertedTime(now);
            existing.setUpdateTime(now);
            referralConversionRecordMapper.updateById(existing);
        }
        incrementUsedCount(code);
        return true;
    }

    @Override
    @Transactional
    public boolean markGenericJoinSubmitted(Long tenantId, String referralCode, String targetEmail, Long joinRequestId) {
        ReferralCode code = resolveAvailableReferralCode(referralCode, tenantId);
        ReferralConversionRecord existing = findGenericConversionRecord(code.getId(), targetEmail, "JOIN");
        if (existing != null && "JOIN_APPROVED".equals(existing.getConversionStatus())) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            ReferralConversionRecord record = new ReferralConversionRecord();
            record.setTenantId(tenantId);
            record.setReferralCodeId(code.getId());
            record.setReferralCode(code.getCode());
            record.setTargetEmail(targetEmail);
            record.setConversionType("JOIN");
            record.setConversionStatus("JOIN_SUBMITTED");
            record.setJoinRequestId(joinRequestId);
            record.setConvertedTime(now);
            record.setCreateTime(now);
            record.setUpdateTime(now);
            record.setIsDeleted(0);
            referralConversionRecordMapper.insert(record);
            return true;
        }

        existing.setJoinRequestId(joinRequestId);
        existing.setConvertedTime(now);
        existing.setUpdateTime(now);
        if (existing.getConversionStatus() == null || existing.getConversionStatus().isBlank()) {
            existing.setConversionStatus("JOIN_SUBMITTED");
        }
        referralConversionRecordMapper.updateById(existing);
        return true;
    }

    @Override
    @Transactional
    public boolean markGenericJoinApproved(Long tenantId, String referralCode, String targetEmail, Long authUserId, Long joinRequestId) {
        ReferralCode code = resolveAvailableReferralCode(referralCode, tenantId);
        ReferralConversionRecord existing = findGenericConversionRecord(code.getId(), targetEmail, "JOIN");
        if (existing != null && "JOIN_APPROVED".equals(existing.getConversionStatus())) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            ReferralConversionRecord record = new ReferralConversionRecord();
            record.setTenantId(tenantId);
            record.setReferralCodeId(code.getId());
            record.setReferralCode(code.getCode());
            record.setTargetEmail(targetEmail);
            record.setConversionType("JOIN");
            record.setConversionStatus("JOIN_APPROVED");
            record.setAuthUserId(authUserId);
            record.setJoinRequestId(joinRequestId);
            record.setConvertedTime(now);
            record.setCreateTime(now);
            record.setUpdateTime(now);
            record.setIsDeleted(0);
            referralConversionRecordMapper.insert(record);
        } else {
            existing.setConversionStatus("JOIN_APPROVED");
            existing.setAuthUserId(authUserId);
            existing.setJoinRequestId(joinRequestId);
            existing.setConvertedTime(now);
            existing.setUpdateTime(now);
            referralConversionRecordMapper.updateById(existing);
        }
        incrementUsedCount(code);
        return true;
    }

    @Override
    public ReferralConversionOverviewVO getReferralConversionOverview(Long id) {
        ReferralCode referralCode = referralCodeMapper.selectById(id);
        if (referralCode == null) {
            throw new RuntimeException("内推码不存在");
        }

        QueryWrapper<ReferralConversionRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("referral_code_id", id)
                .orderByDesc("converted_time");
        List<ReferralConversionRecord> records = referralConversionRecordMapper.selectList(queryWrapper);

        ReferralConversionOverviewVO overview = new ReferralConversionOverviewVO();
        overview.setReferralCodeId(referralCode.getId());
        overview.setReferralCode(referralCode.getCode());
        overview.setUsedCount(referralCode.getUsedCount());
        overview.setOpenedCount((int) records.stream().filter(item -> "OPENED".equals(item.getConversionStatus())).count());
        overview.setRegisterSuccessCount((int) records.stream().filter(item -> "REGISTER_SUCCESS".equals(item.getConversionStatus())).count());
        overview.setJoinApprovedCount((int) records.stream().filter(item -> "JOIN_APPROVED".equals(item.getConversionStatus())).count());
        overview.setRecords(records.stream().map(item -> {
            ReferralConversionRecordVO vo = new ReferralConversionRecordVO();
            vo.setId(item.getId());
            vo.setTargetEmail(item.getTargetEmail());
            vo.setConversionType(item.getConversionType());
            vo.setConversionStatus(item.getConversionStatus());
            vo.setAuthUserId(item.getAuthUserId());
            vo.setJoinRequestId(item.getJoinRequestId());
            vo.setConvertedTime(item.getConvertedTime() == null ? null : item.getConvertedTime().toString());
            return vo;
        }).collect(Collectors.toList()));
        return overview;
    }

    @Override
    public ReferralCodeDTO getCurrentTenantReferralCode() {
        Long userId = requireCurrentUserId();
        Long tenantId = resolveTenantId();
        ReferralCode referralCode = findLatestValidCode(userId, tenantId);
        return toDto(referralCode);
    }

    @Override
    @Transactional
    public ReferralCodeDTO refreshCurrentTenantReferralCode() {
        Long userId = requireCurrentUserId();
        Long tenantId = resolveTenantId();
        ReferralCode existing = findLatestValidCode(userId, tenantId);
        if (existing != null) {
            existing.setStatus(STATUS_INACTIVE);
            existing.setUpdateTime(LocalDateTime.now());
            referralCodeMapper.updateById(existing);
        }
        ReferralCode newCode = buildReferralCode(existing, userId, tenantId);
        referralCodeMapper.insert(newCode);
        return toDto(newCode);
    }

    private ReferralCode resolveAvailableReferralCode(String code, Long tenantId) {
        if (code == null || code.isBlank()) {
            throw new RuntimeException("内推码不能为空");
        }

        LambdaQueryWrapper<ReferralCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReferralCode::getCode, code)
                .eq(ReferralCode::getStatus, 1);
        if (tenantId != null) {
            wrapper.eq(ReferralCode::getTenantId, tenantId);
        }

        ReferralCode referralCode = referralCodeMapper.selectOne(wrapper);
        if (referralCode == null) {
            throw new RuntimeException("内推码不存在或已失效");
        }
        if (referralCode.getExpiresAt() != null && referralCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("内推码已过期");
        }
        if (referralCode.getUsedCount() != null && referralCode.getMaxUses() != null
                && referralCode.getUsedCount() >= referralCode.getMaxUses()) {
            throw new RuntimeException("内推码已达最大使用次数");
        }
        return referralCode;
    }

    private ReferralConversionRecord findGenericConversionRecord(Long referralCodeId, String targetEmail, String conversionType) {
        LambdaQueryWrapper<ReferralConversionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReferralConversionRecord::getReferralCodeId, referralCodeId)
                .eq(ReferralConversionRecord::getTargetEmail, targetEmail)
                .eq(ReferralConversionRecord::getConversionType, conversionType)
                .isNull(ReferralConversionRecord::getInvitationId)
                .last("LIMIT 1");
        return referralConversionRecordMapper.selectOne(wrapper);
    }

    private void incrementUsedCount(ReferralCode code) {
        code.setUsedCount((code.getUsedCount() == null ? 0 : code.getUsedCount()) + 1);
        referralCodeMapper.updateById(code);
    }

    private ReferralCode buildReferralCode(ReferralCode previous, Long userId, Long tenantId) {
        ReferralCode referralCode = new ReferralCode();
        referralCode.setGeneratorId(userId);
        referralCode.setTenantId(tenantId);
        referralCode.setCode(generateUniqueReferralCode());
        referralCode.setMaxUses(previous != null && previous.getMaxUses() != null ? previous.getMaxUses() : DEFAULT_MAX_USES);
        referralCode.setUsedCount(0);
        referralCode.setExpiresAt(previous != null ? previous.getExpiresAt() : null);
        referralCode.setStatus(STATUS_ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        referralCode.setCreateTime(now);
        referralCode.setUpdateTime(now);
        return referralCode;
    }

    private ReferralCode findLatestValidCode(Long userId, Long tenantId) {
        LambdaQueryWrapper<ReferralCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReferralCode::getGeneratorId, userId)
                .eq(ReferralCode::getTenantId, tenantId)
                .eq(ReferralCode::getStatus, STATUS_ACTIVE)
                .orderByDesc(ReferralCode::getCreateTime)
                .last("LIMIT 1");
        ReferralCode referralCode = referralCodeMapper.selectOne(wrapper);
        return isReferralCodeValid(referralCode) ? referralCode : null;
    }

    private boolean isReferralCodeValid(ReferralCode referralCode) {
        if (referralCode == null || referralCode.getStatus() == null || !referralCode.getStatus().equals(STATUS_ACTIVE)) {
            return false;
        }
        Integer maxUses = referralCode.getMaxUses();
        Integer usedCount = referralCode.getUsedCount();
        if (maxUses != null && maxUses > 0 && usedCount != null && usedCount >= maxUses) {
            return false;
        }
        LocalDateTime expiresAt = referralCode.getExpiresAt();
        return expiresAt == null || !expiresAt.isBefore(LocalDateTime.now());
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private Long requireCurrentUserId() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new RuntimeException("当前用户信息缺失");
        }
        return userId;
    }

    private ReferralCodeDTO toDto(ReferralCode referralCode) {
        if (referralCode == null) {
            return null;
        }
        ReferralCodeDTO dto = new ReferralCodeDTO();
        BeanUtils.copyProperties(referralCode, dto);
        return dto;
    }

    private String generateUniqueReferralCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = generateUniqueCode();
            LambdaQueryWrapper<ReferralCode> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ReferralCode::getCode, code);
            if (referralCodeMapper.selectCount(wrapper) == 0) {
                return code;
            }
        }
        throw new RuntimeException("内推码生成失败，请稍后重试");
    }
}
