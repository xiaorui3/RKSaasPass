package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.dto.AlumniProfileSubmitDTO;
import com.tianji.user.domain.dto.EmailCenterSendDTO;
import com.tianji.user.domain.po.AlumniGraduationLog;
import com.tianji.user.domain.po.AlumniProfileToken;
import com.tianji.user.domain.po.ClubAlumni;
import com.tianji.user.domain.po.ClubMember;
import com.tianji.user.domain.po.RKTenant;
import com.tianji.user.domain.po.User;
import com.tianji.user.domain.vo.AlumniGraduationResultVO;
import com.tianji.user.domain.vo.AlumniProfileFormVO;
import com.tianji.user.mapper.AlumniGraduationLogMapper;
import com.tianji.user.mapper.AlumniProfileTokenMapper;
import com.tianji.user.mapper.ClubAlumniMapper;
import com.tianji.user.mapper.ClubMemberMapper;
import com.tianji.user.mapper.RKTenantMapper;
import com.tianji.user.mapper.UserMapper;
import com.tianji.user.service.IAlumniGraduationWorkflowService;
import com.tianji.user.service.IEmailCenterService;
import com.tianji.user.utils.PublicBaseUrlResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlumniGraduationWorkflowService implements IAlumniGraduationWorkflowService {

    private static final String ACTION_GRADUATE = "GRADUATE";
    private static final String ACTION_ADVANCE_GRADE = "ADVANCE_GRADE";
    private static final String STATUS_DONE = "DONE";
    private static final String TOKEN_STATUS_SENT = "SENT";
    private static final String TOKEN_STATUS_SUBMITTED = "SUBMITTED";
    private static final String GRADUATED_STATUS = "已毕业";

    private final ClubMemberMapper clubMemberMapper;
    private final ClubAlumniMapper clubAlumniMapper;
    private final AlumniProfileTokenMapper alumniProfileTokenMapper;
    private final AlumniGraduationLogMapper alumniGraduationLogMapper;
    private final RKTenantMapper tenantMapper;
    private final UserMapper userMapper;
    private final IEmailCenterService emailCenterService;

    @Value("${rk.email.alumni-profile.base-url:}")
    private String alumniProfileBaseUrl;

    @Value("${rk.alumni.profile-token.expire-days:30}")
    private Integer profileTokenExpireDays;

    @Override
    @Transactional
    public AlumniGraduationResultVO runAnnualGraduation(Integer processYear, Long tenantId) {
        int resolvedYear = processYear == null ? LocalDate.now().getYear() : processYear;
        AlumniGraduationResultVO result = new AlumniGraduationResultVO();
        result.setProcessYear(resolvedYear);

        List<Long> tenantIds = resolveTenantIds(tenantId);
        if (tenantIds.isEmpty()) {
            return result;
        }

        List<ClubMember> members = clubMemberMapper.selectList(new LambdaQueryWrapper<ClubMember>()
                .in(ClubMember::getTenantId, tenantIds)
                .eq(ClubMember::getIsDeleted, 0));
        if (members == null || members.isEmpty()) {
            return result;
        }

        for (ClubMember member : members) {
            if (!isProcessableMember(member)) {
                continue;
            }
            result.increaseScannedCount();
            GraduationDecision decision = resolveGraduationDecision(member.getGrade(), resolvedYear);
            if (decision.graduated) {
                processGraduatedMember(member, decision, resolvedYear, result);
                continue;
            }
            processGradeAdvancement(member, resolvedYear, result);
        }
        return result;
    }

    @Override
    public AlumniProfileFormVO getProfileForm(String token) {
        AlumniProfileToken profileToken = requireUsableToken(token, false);
        ClubAlumni alumni = clubAlumniMapper.selectById(profileToken.getAlumniId());
        if (alumni == null || Objects.equals(alumni.getIsDeleted(), 1)) {
            throw new BadRequestException("校友档案不存在");
        }
        return toProfileFormVO(profileToken, alumni);
    }

    @Override
    @Transactional
    public Boolean submitProfileForm(String token, AlumniProfileSubmitDTO dto) {
        AlumniProfileToken profileToken = requireUsableToken(token, true);
        ClubAlumni alumni = clubAlumniMapper.selectById(profileToken.getAlumniId());
        if (alumni == null || Objects.equals(alumni.getIsDeleted(), 1)) {
            throw new BadRequestException("校友档案不存在");
        }

        applyProfileSubmit(alumni, dto == null ? new AlumniProfileSubmitDTO() : dto);
        alumni.setUpdateTime(LocalDateTime.now());
        clubAlumniMapper.updateById(alumni);

        profileToken.setStatus(TOKEN_STATUS_SUBMITTED);
        profileToken.setSubmittedTime(LocalDateTime.now());
        profileToken.setUpdateTime(LocalDateTime.now());
        alumniProfileTokenMapper.updateById(profileToken);
        return true;
    }

    private void processGraduatedMember(ClubMember member, GraduationDecision decision, int processYear, AlumniGraduationResultVO result) {
        if (hasProcessed(member, processYear, ACTION_GRADUATE)) {
            result.increaseSkippedAlreadyProcessedCount();
            return;
        }

        ClubAlumni alumni = upsertAlumni(member, decision);
        markMemberGraduated(member);
        boolean emailQueued = ensureProfileTokenAndEmail(member, alumni, result);
        recordGraduationLog(member, alumni.getId(), processYear, ACTION_GRADUATE, member.getGrade(), member.getGrade(),
                emailQueued ? "profile email queued" : "profile email skipped");
        result.increaseGraduatedCount();
    }

    private void processGradeAdvancement(ClubMember member, int processYear, AlumniGraduationResultVO result) {
        String advancedGrade = advanceGrade(member.getGrade());
        if (StringUtils.isBlank(advancedGrade) || Objects.equals(advancedGrade, member.getGrade())) {
            return;
        }
        if (hasProcessed(member, processYear, ACTION_ADVANCE_GRADE)) {
            result.increaseSkippedAlreadyProcessedCount();
            return;
        }
        String oldGrade = member.getGrade();
        ClubMember update = new ClubMember();
        update.setId(member.getId());
        update.setGrade(advancedGrade);
        update.setUpdateTime(LocalDateTime.now());
        clubMemberMapper.updateById(update);
        syncLinkedUserGrade(member, advancedGrade);
        recordGraduationLog(member, null, processYear, ACTION_ADVANCE_GRADE, oldGrade, advancedGrade, "grade advanced");
        result.increaseGradeAdvancedCount();
    }

    private ClubAlumni upsertAlumni(ClubMember member, GraduationDecision decision) {
        ClubAlumni alumni = findExistingAlumni(member);
        boolean creating = alumni == null;
        if (creating) {
            alumni = new ClubAlumni();
            alumni.setCreateTime(LocalDateTime.now());
        }
        alumni.setTenantId(member.getTenantId());
        alumni.setName(member.getName());
        alumni.setStudentId(member.getStudentId());
        alumni.setEmail(member.getEmail());
        alumni.setMajor(member.getMajor());
        alumni.setDepartment(member.getDepartment());
        alumni.setPosition(member.getPosition());
        alumni.setGenerationYear(decision.enrollmentYear);
        alumni.setEnrollmentYear(decision.enrollmentYear);
        alumni.setExpectedGraduationYear(decision.expectedGraduationYear);
        alumni.setActualGraduationDate(LocalDate.of(decision.expectedGraduationYear, 6, 30));
        alumni.setShowTable(true);
        alumni.setIsActive(true);
        alumni.setIsCoreMember(Boolean.TRUE.equals(alumni.getIsCoreMember()));
        alumni.setMemberStatus(GRADUATED_STATUS);
        alumni.setGraduationStatus(GRADUATED_STATUS);
        alumni.setIsDeleted(0);
        alumni.setUpdateTime(LocalDateTime.now());
        if (creating) {
            clubAlumniMapper.insert(alumni);
        } else {
            clubAlumniMapper.updateById(alumni);
        }
        return alumni;
    }

    private boolean ensureProfileTokenAndEmail(ClubMember member, ClubAlumni alumni, AlumniGraduationResultVO result) {
        if (StringUtils.isBlank(member.getEmail())) {
            result.increaseSkippedWithoutEmailCount();
            return false;
        }
        AlumniProfileToken existingToken = findExistingProfileToken(alumni.getId(), member.getId());
        if (existingToken != null && TOKEN_STATUS_SENT.equalsIgnoreCase(existingToken.getStatus())) {
            return false;
        }
        AlumniProfileToken profileToken = existingToken == null ? createProfileToken(member, alumni) : existingToken;
        sendProfileEmail(member, profileToken);
        result.increaseEmailQueuedCount();
        return true;
    }

    private AlumniProfileToken createProfileToken(ClubMember member, ClubAlumni alumni) {
        AlumniProfileToken profileToken = new AlumniProfileToken()
                .setAlumniId(alumni.getId())
                .setMemberId(member.getId())
                .setEmail(member.getEmail().trim().toLowerCase())
                .setToken(UUID.randomUUID().toString().replace("-", ""))
                .setStatus(TOKEN_STATUS_SENT)
                .setExpiresAt(LocalDateTime.now().plusDays(profileTokenExpireDays == null ? 30 : profileTokenExpireDays));
        profileToken.setTenantId(member.getTenantId());
        profileToken.setCreateTime(LocalDateTime.now());
        profileToken.setUpdateTime(LocalDateTime.now());
        profileToken.setIsDeleted(0);
        alumniProfileTokenMapper.insert(profileToken);
        return profileToken;
    }

    private void sendProfileEmail(ClubMember member, AlumniProfileToken profileToken) {
        EmailCenterSendDTO dto = new EmailCenterSendDTO();
        dto.setManualEmails(List.of(profileToken.getEmail()));
        dto.setSubject("请完善校友信息");
        String profileUrl = PublicBaseUrlResolver.resolve(alumniProfileBaseUrl)
                + "/alumni/profile-form?token=" + profileToken.getToken();
        dto.setContent("<p>" + safeName(member.getName()) + "，你已自动转入校友档案。</p>"
                + "<p>请点击下面链接完善校友信息，该链接只能提交一次：</p>"
                + "<p><a href=\"" + profileUrl + "\" target=\"_blank\">填写校友信息表单</a></p>"
                + "<p style=\"color:#888;font-size:12px;\">" + profileUrl + "</p>");
        dto.setHtml(true);
        runWithTenant(member.getTenantId(), () -> emailCenterService.send(dto));
    }

    private void markMemberGraduated(ClubMember member) {
        ClubMember update = new ClubMember();
        update.setId(member.getId());
        update.setStatus(GRADUATED_STATUS);
        update.setUpdateTime(LocalDateTime.now());
        clubMemberMapper.updateById(update);
    }

    private void syncLinkedUserGrade(ClubMember member, String grade) {
        if (member.getTenantId() == null || StringUtils.isBlank(member.getStudentId())) {
            return;
        }
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, member.getTenantId())
                .eq(User::getStudentId, member.getStudentId())
                .eq(User::getIsDeleted, 0));
        if (users == null || users.isEmpty()) {
            return;
        }
        for (User user : users) {
            User update = new User();
            update.setId(user.getId());
            update.setGrade(grade);
            update.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(update);
        }
    }

    private void recordGraduationLog(ClubMember member, Long alumniId, int processYear, String actionType,
                                     String oldGrade, String newGrade, String remark) {
        AlumniGraduationLog logRecord = new AlumniGraduationLog()
                .setMemberId(member.getId())
                .setAlumniId(alumniId)
                .setStudentId(member.getStudentId())
                .setProcessYear(processYear)
                .setActionType(actionType)
                .setOldGrade(oldGrade)
                .setNewGrade(newGrade)
                .setStatus(STATUS_DONE)
                .setRemark(remark);
        logRecord.setTenantId(member.getTenantId());
        logRecord.setCreateTime(LocalDateTime.now());
        logRecord.setUpdateTime(LocalDateTime.now());
        logRecord.setIsDeleted(0);
        alumniGraduationLogMapper.insert(logRecord);
    }

    private boolean hasProcessed(ClubMember member, int processYear, String actionType) {
        Integer count = alumniGraduationLogMapper.selectCount(new LambdaQueryWrapper<AlumniGraduationLog>()
                .eq(AlumniGraduationLog::getTenantId, member.getTenantId())
                .eq(AlumniGraduationLog::getMemberId, member.getId())
                .eq(AlumniGraduationLog::getProcessYear, processYear)
                .eq(AlumniGraduationLog::getActionType, actionType)
                .eq(AlumniGraduationLog::getIsDeleted, 0));
        return count != null && count > 0;
    }

    private AlumniProfileToken findExistingProfileToken(Long alumniId, Long memberId) {
        LambdaQueryWrapper<AlumniProfileToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlumniProfileToken::getIsDeleted, 0)
                .and(query -> {
                    boolean hasCondition = false;
                    if (alumniId != null) {
                        query.eq(AlumniProfileToken::getAlumniId, alumniId);
                        hasCondition = true;
                    }
                    if (memberId != null) {
                        if (hasCondition) {
                            query.or();
                        }
                        query.eq(AlumniProfileToken::getMemberId, memberId);
                    }
                })
                .last("LIMIT 1");
        return alumniProfileTokenMapper.selectOne(wrapper);
    }

    private ClubAlumni findExistingAlumni(ClubMember member) {
        LambdaQueryWrapper<ClubAlumni> wrapper = new LambdaQueryWrapper<ClubAlumni>()
                .eq(ClubAlumni::getTenantId, member.getTenantId())
                .eq(ClubAlumni::getIsDeleted, 0);
        if (StringUtils.isNotBlank(member.getStudentId())) {
            wrapper.eq(ClubAlumni::getStudentId, member.getStudentId());
        } else if (StringUtils.isNotBlank(member.getEmail())) {
            wrapper.eq(ClubAlumni::getEmail, member.getEmail());
        } else {
            wrapper.eq(ClubAlumni::getName, member.getName());
        }
        wrapper.last("LIMIT 1");
        return clubAlumniMapper.selectOne(wrapper);
    }

    private AlumniProfileToken requireUsableToken(String token, boolean rejectSubmitted) {
        if (StringUtils.isBlank(token)) {
            throw new BadRequestException("校友表单链接无效");
        }
        AlumniProfileToken profileToken = alumniProfileTokenMapper.selectOne(new LambdaQueryWrapper<AlumniProfileToken>()
                .eq(AlumniProfileToken::getToken, token.trim())
                .eq(AlumniProfileToken::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (profileToken == null) {
            throw new BadRequestException("校友表单链接不存在或已失效");
        }
        if (profileToken.getExpiresAt() != null && profileToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("校友表单链接已过期");
        }
        if (rejectSubmitted && TOKEN_STATUS_SUBMITTED.equalsIgnoreCase(profileToken.getStatus())) {
            throw new BadRequestException("校友表单已提交，链接只能使用一次");
        }
        return profileToken;
    }

    private AlumniProfileFormVO toProfileFormVO(AlumniProfileToken profileToken, ClubAlumni alumni) {
        AlumniProfileFormVO vo = new AlumniProfileFormVO();
        vo.setToken(profileToken.getToken());
        vo.setSubmitted(TOKEN_STATUS_SUBMITTED.equalsIgnoreCase(profileToken.getStatus()));
        vo.setAlumniId(alumni.getId());
        vo.setTenantId(alumni.getTenantId());
        vo.setName(alumni.getName());
        vo.setStudentId(alumni.getStudentId());
        vo.setEmail(alumni.getEmail());
        vo.setMajor(alumni.getMajor());
        vo.setDepartment(alumni.getDepartment());
        vo.setPosition(alumni.getPosition());
        vo.setWorkCity(alumni.getWorkCity());
        vo.setWorkUnit(alumni.getWorkUnit());
        vo.setJobContent(alumni.getJobContent());
        vo.setCurrentContact(alumni.getCurrentContact());
        vo.setSkills(alumni.getSkills());
        vo.setHonorCertificates(alumni.getHonorCertificates());
        vo.setNotes(alumni.getNotes());
        vo.setAdvice(alumni.getAdvice());
        vo.setShowTable(alumni.getShowTable());
        return vo;
    }

    private void applyProfileSubmit(ClubAlumni alumni, AlumniProfileSubmitDTO dto) {
        if (dto.getWorkCity() != null) {
            alumni.setWorkCity(dto.getWorkCity());
        }
        if (dto.getWorkUnit() != null) {
            alumni.setWorkUnit(dto.getWorkUnit());
        }
        if (dto.getJobContent() != null) {
            alumni.setJobContent(dto.getJobContent());
        }
        if (dto.getCurrentContact() != null) {
            alumni.setCurrentContact(dto.getCurrentContact());
        }
        if (dto.getSkills() != null) {
            alumni.setSkills(dto.getSkills());
        }
        if (dto.getHonorCertificates() != null) {
            alumni.setHonorCertificates(dto.getHonorCertificates());
        }
        if (dto.getNotes() != null) {
            alumni.setNotes(dto.getNotes());
        }
        if (dto.getAdvice() != null) {
            alumni.setAdvice(dto.getAdvice());
        }
        if (dto.getShowTable() != null) {
            alumni.setShowTable(dto.getShowTable());
        }
    }

    private GraduationDecision resolveGraduationDecision(String grade, int processYear) {
        Integer enrollmentYear = parseEnrollmentYear(grade);
        if (enrollmentYear != null) {
            return new GraduationDecision(enrollmentYear, enrollmentYear + 4,
                    processYear - enrollmentYear >= 4);
        }
        Integer classStanding = parseClassStanding(grade);
        if (classStanding != null) {
            int resolvedEnrollmentYear = processYear - classStanding;
            return new GraduationDecision(resolvedEnrollmentYear, resolvedEnrollmentYear + 4,
                    classStanding >= 4);
        }
        return new GraduationDecision(null, processYear, false);
    }

    private Integer parseEnrollmentYear(String grade) {
        if (StringUtils.isBlank(grade)) {
            return null;
        }
        String digitsOnly = grade.replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 4) {
            return null;
        }
        int candidate = Integer.parseInt(digitsOnly.substring(0, 4));
        int currentYear = LocalDate.now().getYear();
        if (candidate < 2000 || candidate > currentYear + 1) {
            return null;
        }
        return candidate;
    }

    private Integer parseClassStanding(String grade) {
        String normalized = grade == null ? "" : grade.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.contains("大一") || normalized.contains("一年级")) {
            return 1;
        }
        if (normalized.contains("大二") || normalized.contains("二年级")) {
            return 2;
        }
        if (normalized.contains("大三") || normalized.contains("三年级")) {
            return 3;
        }
        if (normalized.contains("大四") || normalized.contains("四年级")) {
            return 4;
        }
        if (normalized.matches("[1-4](年级|级)?")) {
            return Integer.parseInt(normalized.substring(0, 1));
        }
        return null;
    }

    private String advanceGrade(String grade) {
        String normalized = grade == null ? "" : grade.trim();
        if (normalized.isEmpty() || parseEnrollmentYear(normalized) != null) {
            return null;
        }
        if (normalized.contains("大一")) {
            return normalized.replace("大一", "大二");
        }
        if (normalized.contains("大二")) {
            return normalized.replace("大二", "大三");
        }
        if (normalized.contains("大三")) {
            return normalized.replace("大三", "大四");
        }
        if (normalized.contains("一年级")) {
            return normalized.replace("一年级", "二年级");
        }
        if (normalized.contains("二年级")) {
            return normalized.replace("二年级", "三年级");
        }
        if (normalized.contains("三年级")) {
            return normalized.replace("三年级", "四年级");
        }
        if (normalized.matches("[1-3](年级|级)?")) {
            int value = Integer.parseInt(normalized.substring(0, 1)) + 1;
            return value + normalized.substring(1);
        }
        return null;
    }

    private List<Long> resolveTenantIds(Long tenantId) {
        if (tenantId != null) {
            return List.of(tenantId);
        }
        List<RKTenant> tenants = tenantMapper.selectList(new LambdaQueryWrapper<RKTenant>()
                .select(RKTenant::getId)
                .eq(RKTenant::getStatus, 1)
                .eq(RKTenant::getIsDeleted, 0)
                .and(query -> query.isNull(RKTenant::getExpireTime)
                        .or()
                        .gt(RKTenant::getExpireTime, LocalDateTime.now())));
        if (tenants == null) {
            return List.of();
        }
        return tenants.stream().map(RKTenant::getId).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private boolean isProcessableMember(ClubMember member) {
        return member != null
                && member.getId() != null
                && member.getTenantId() != null
                && StringUtils.isNotBlank(member.getStudentId())
                && !GRADUATED_STATUS.equals(member.getStatus());
    }

    private String safeName(String name) {
        return StringUtils.isNotBlank(name) ? name : "同学";
    }

    private void runWithTenant(Long tenantId, Runnable action) {
        Long previousTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            action.run();
        } finally {
            if (previousTenantId == null) {
                TenantContext.removeTenantId();
            } else {
                TenantContext.setTenantId(previousTenantId);
            }
        }
    }

    private static class GraduationDecision {
        private final Integer enrollmentYear;
        private final Integer expectedGraduationYear;
        private final boolean graduated;

        private GraduationDecision(Integer enrollmentYear, Integer expectedGraduationYear, boolean graduated) {
            this.enrollmentYear = enrollmentYear;
            this.expectedGraduationYear = expectedGraduationYear;
            this.graduated = graduated && enrollmentYear != null;
        }
    }
}
