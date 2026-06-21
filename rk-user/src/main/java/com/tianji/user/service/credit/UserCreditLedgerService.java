package com.tianji.user.service.credit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.api.dto.user.CreditGrantDTO;
import com.tianji.user.domain.po.CreditType;
import com.tianji.user.domain.po.UserCreditRecord;
import com.tianji.user.domain.po.UserCreditSummary;
import com.tianji.user.domain.po.VolunteerRecord;
import com.tianji.user.domain.vo.CreditTypeBreakdownVO;
import com.tianji.user.mapper.UserCreditRecordMapper;
import com.tianji.user.mapper.UserCreditSummaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserCreditLedgerService {

    private final UserCreditRecordMapper userCreditRecordMapper;
    private final UserCreditSummaryMapper userCreditSummaryMapper;

    public Integer grantCredits(List<CreditGrantDTO> grants, Long verifierId) {
        if (CollectionUtils.isEmpty(grants)) {
            return 0;
        }
        int processed = 0;
        Set<String> impactedUsers = new LinkedHashSet<>();
        for (CreditGrantDTO grant : grants) {
            if (!isValidGrant(grant)) {
                continue;
            }
            Long tenantId = grant.getTenantId() == null ? 1L : grant.getTenantId();
            UserCreditRecord record = upsertGrantRecord(grant, tenantId, verifierId);
            impactedUsers.add(record.getUserId() + ":" + tenantId);
            processed++;
        }
        impactedUsers.forEach(key -> {
            String[] parts = key.split(":");
            rebuildSummary(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
        });
        return processed;
    }

    public void syncVolunteerApproval(VolunteerRecord record, Long verifierId) {
        if (record == null || record.getId() == null || record.getUserId() == null) {
            return;
        }
        CreditGrantDTO dto = new CreditGrantDTO();
        dto.setUserId(record.getUserId());
        dto.setTenantId(record.getTenantId() == null ? 1L : record.getTenantId());
        dto.setSourceType("volunteer");
        dto.setSourceId(record.getId());
        dto.setCreditTypeCode("volunteer");
        dto.setCreditHours(nvl(record.getServiceHours()));
        dto.setCreditScore(nvl(record.getServiceHours()));
        dto.setDescription(buildVolunteerDescription(record));
        grantCredits(List.of(dto), verifierId);
    }

    public List<CreditTypeBreakdownVO> listBreakdown(Long userId, Long tenantId, List<CreditType> configuredTypes) {
        QueryWrapper<UserCreditRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("tenant_id", tenantId)
                .eq("status", 1)
                .orderByAsc("id");
        List<UserCreditRecord> approvedRecords = userCreditRecordMapper.selectList(wrapper);

        Map<String, CreditTypeBreakdownVO> breakdownMap = new LinkedHashMap<>();
        if (!CollectionUtils.isEmpty(configuredTypes)) {
            for (CreditType type : configuredTypes) {
                CreditTypeBreakdownVO vo = new CreditTypeBreakdownVO();
                vo.setTypeId(type.getId());
                vo.setCode(type.getCode());
                vo.setName(type.getName());
                vo.setDescription(type.getDescription());
                vo.setMaxCredit(type.getMaxCredit());
                vo.setTotalCredits(BigDecimal.ZERO);
                vo.setTotalHours(BigDecimal.ZERO);
                vo.setRecordCount(0);
                breakdownMap.put(type.getCode(), vo);
            }
        }

        for (UserCreditRecord record : approvedRecords) {
            String code = StringUtils.hasText(record.getCreditTypeCode()) ? record.getCreditTypeCode() : record.getSourceType();
            CreditTypeBreakdownVO vo = breakdownMap.computeIfAbsent(code, key -> {
                CreditTypeBreakdownVO fallback = new CreditTypeBreakdownVO();
                fallback.setCode(key);
                fallback.setName(key);
                fallback.setTotalCredits(BigDecimal.ZERO);
                fallback.setTotalHours(BigDecimal.ZERO);
                fallback.setRecordCount(0);
                return fallback;
            });
            vo.setTotalCredits(nvl(vo.getTotalCredits()).add(nvl(record.getCreditScore())));
            vo.setTotalHours(nvl(vo.getTotalHours()).add(nvl(record.getCreditHours())));
            vo.setRecordCount((vo.getRecordCount() == null ? 0 : vo.getRecordCount()) + 1);
        }

        return new ArrayList<>(breakdownMap.values());
    }

    public void rebuildSummary(Long userId, Long tenantId) {
        QueryWrapper<UserCreditRecord> approvedWrapper = new QueryWrapper<>();
        approvedWrapper.eq("user_id", userId)
                .eq("tenant_id", tenantId)
                .eq("status", 1);
        List<UserCreditRecord> approvedRecords = userCreditRecordMapper.selectList(approvedWrapper);

        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalHours = BigDecimal.ZERO;
        BigDecimal volunteerHours = BigDecimal.ZERO;
        int activityCount = 0;
        int competitionAwards = 0;

        for (UserCreditRecord record : approvedRecords) {
            totalCredits = totalCredits.add(nvl(record.getCreditScore()));
            totalHours = totalHours.add(nvl(record.getCreditHours()));
            if (Objects.equals("activity", record.getSourceType())) {
                activityCount++;
            } else if (Objects.equals("competition", record.getSourceType())) {
                competitionAwards++;
            } else if (Objects.equals("volunteer", record.getSourceType())) {
                volunteerHours = volunteerHours.add(nvl(record.getCreditHours()));
            }
        }

        QueryWrapper<UserCreditSummary> summaryWrapper = new QueryWrapper<>();
        summaryWrapper.eq("user_id", userId)
                .eq("tenant_id", tenantId)
                .last("LIMIT 1");
        UserCreditSummary summary = userCreditSummaryMapper.selectOne(summaryWrapper);
        if (summary == null) {
            summary = new UserCreditSummary();
            summary.setUserId(userId);
            summary.setTenantId(tenantId);
        }
        summary.setTotalCredits(totalCredits);
        summary.setTotalHours(totalHours);
        summary.setVolunteerHours(volunteerHours);
        summary.setActivityCount(activityCount);
        summary.setCompetitionAwards(competitionAwards);
        summary.setLastUpdated(LocalDateTime.now());
        if (summary.getId() == null) {
            userCreditSummaryMapper.insert(summary);
        } else {
            userCreditSummaryMapper.updateById(summary);
        }
    }

    private UserCreditRecord upsertGrantRecord(CreditGrantDTO grant, Long tenantId, Long verifierId) {
        QueryWrapper<UserCreditRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", grant.getUserId())
                .eq("tenant_id", tenantId)
                .eq("source_type", grant.getSourceType())
                .eq("source_id", grant.getSourceId())
                .orderByDesc("id");
        List<UserCreditRecord> matches = userCreditRecordMapper.selectList(wrapper);

        UserCreditRecord record;
        if (CollectionUtils.isEmpty(matches)) {
            record = new UserCreditRecord();
        } else {
            record = matches.get(0);
            for (int i = 1; i < matches.size(); i++) {
                userCreditRecordMapper.deleteById(matches.get(i).getId());
            }
        }

        record.setTenantId(tenantId);
        record.setUserId(grant.getUserId());
        record.setSourceType(grant.getSourceType());
        record.setSourceId(grant.getSourceId());
        record.setCreditTypeCode(StringUtils.hasText(grant.getCreditTypeCode()) ? grant.getCreditTypeCode() : grant.getSourceType());
        record.setCreditHours(nvl(grant.getCreditHours()));
        record.setCreditScore(nvl(grant.getCreditScore()));
        record.setDescription(grant.getDescription());
        record.setStatus(1);
        record.setRejectReason(null);
        record.setVerifierId(verifierId);
        record.setVerifyTime(LocalDateTime.now());

        if (record.getId() == null) {
            userCreditRecordMapper.insert(record);
        } else {
            userCreditRecordMapper.updateById(record);
        }
        return record;
    }

    private String buildVolunteerDescription(VolunteerRecord record) {
        if (StringUtils.hasText(record.getTitle())) {
            return "志愿服务《" + record.getTitle() + "》学分";
        }
        return "志愿服务学分";
    }

    private boolean isValidGrant(CreditGrantDTO grant) {
        return grant != null
                && grant.getUserId() != null
                && grant.getSourceId() != null
                && StringUtils.hasText(grant.getSourceType());
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
