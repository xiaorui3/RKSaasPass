package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.dto.auth.AdminUserProvisionDTO;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.common.enums.UserType;
import com.tianji.common.utils.SearchIndexSyncUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.ClubMember;
import com.tianji.user.domain.po.RKTenant;
import com.tianji.user.domain.po.User;
import com.tianji.user.mapper.ClubMemberMapper;
import com.tianji.user.mapper.RKTenantMapper;
import com.tianji.user.service.IAdmissionService;
import com.tianji.user.service.IClubMemberService;
import com.tianji.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubMemberServiceImpl extends ServiceImpl<ClubMemberMapper, ClubMember> implements IClubMemberService {

    private static final String DEFAULT_MEMBER_NAME = "新成员";
    private static final String DEFAULT_MEMBER_POSITION = "成员";
    private static final String STATUS_ACTIVE = "正常";
    private static final String STATUS_PENDING = "待确认";
    // Historical imported rows may still carry mojibake status or position values.
    private static final String LEGACY_STATUS_ACTIVE_GARBLED = "\u59dd\uff45\u7236";
    private static final String LEGACY_STATUS_LIVELY_GARBLED = "\u5a32\u660f\u7a6c";
    private static final String LEGACY_MANAGER_GARBLED = "\u7ee0\uff04\u608a\u935b";
    private static final String LEGACY_CLUB_OWNER_GARBLED = "\u7ec0\u60e7\u6d1f\u7490\u71bb\u77d7\u6d5c";
    private static final String LEGACY_OWNER_GARBLED = "\u7490\u71bb\u77d7\u6d5c";
    private static final String LEGACY_VICE_PRESIDENT_GARBLED = "\u9353\ue21c\u305e\u95c0";
    private static final String LEGACY_TEACHER_GARBLED = "\u93b8\u56e7\ue1f1\u9470\u4f78\u7b00";
    private static final List<String> ACTIVE_STATUS_KEYWORDS = List.of(
            STATUS_ACTIVE,
            "活跃",
            "active",
            LEGACY_STATUS_ACTIVE_GARBLED,
            LEGACY_STATUS_LIVELY_GARBLED
    );
    private static final List<String> MANAGER_POSITION_KEYWORDS = List.of(
            "管理员",
            "社团负责人",
            "负责人",
            "社长",
            "副社长",
            LEGACY_MANAGER_GARBLED,
            LEGACY_CLUB_OWNER_GARBLED,
            LEGACY_OWNER_GARBLED,
            LEGACY_VICE_PRESIDENT_GARBLED
    );
    private static final List<String> TEACHER_POSITION_KEYWORDS = List.of(
            "指导老师",
            LEGACY_TEACHER_GARBLED
    );

    private final IAdmissionService admissionService;
    private final RKTenantMapper tenantMapper;
    private final IUserService userService;
    private final SearchClient searchClient;
    private static final String SEARCH_ENTITY_TYPE_MEMBER = "MEMBER";

    @Override
    public void submitApplication(ClubMember member) {
        Long tenantId = currentTenantId();
        String studentId = member == null ? null : trimToNull(member.getStudentId());
        if (studentId == null) {
            throw new RuntimeException("学号不能为空");
        }

        LambdaQueryWrapper<ClubMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ClubMember::getStudentId, studentId)
                .eq(ClubMember::getTenantId, tenantId)
                .last("LIMIT 1");

        ClubMember existing = baseMapper.selectOne(queryWrapper);
        if (existing != null) {
            throw new RuntimeException("该学号已经提交过申请，请勿重复申请");
        }

        userService.provisionManagedUser(buildProvisionPayload(member, tenantId, studentId));
        log.info("提交社团申请成功，学号={}", studentId);
    }

    @Override
    public List<ClubMember> getAllApplications() {
        admissionService.reconcileApprovedMembersForCurrentTenant();
        userService.reconcilePeopleDomainForCurrentScope();
        LambdaQueryWrapper<ClubMember> queryWrapper = applyTenantScope(new LambdaQueryWrapper<>());
        queryWrapper.eq(ClubMember::getIsDeleted, 0);
        queryWrapper.orderByDesc(ClubMember::getJoinDate).orderByDesc(ClubMember::getCreateTime);
        return filterOfficialRosterMembers(baseMapper.selectList(queryWrapper));
    }

    @Override
    public List<ClubMember> getAllApplications(Long tenantId) {
        if (tenantId == null) {
            return getAllApplications();
        }
        return runInTenantScope(tenantId, this::getAllApplications);
    }

    @Override
    public List<ClubMember> getDeletedApplications() {
        return baseMapper.selectDeletedByTenantIds(resolveScopedTenantIds());
    }

    @Override
    public Map<String, Object> checkApplicationStatus(String studentId) {
        LambdaQueryWrapper<ClubMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ClubMember::getStudentId, studentId)
                .eq(ClubMember::getTenantId, currentTenantId())
                .last("LIMIT 1");
        ClubMember member = baseMapper.selectOne(queryWrapper);

        Map<String, Object> result = new HashMap<>();
        if (member != null) {
            result.put("applied", true);
            result.put("status", normalizeMemberStatus(member.getStatus()));
            result.put("memberStatus", member.getStatus());
            result.put("applicationTime", member.getJoinDate() != null ? member.getJoinDate() : member.getCreateTime());
        } else {
            result.put("applied", false);
        }
        return result;
    }

    @Override
    public void reviewApplication(Long id, Integer agreeStatus, String reviewComment) {
        ClubMember member = baseMapper.selectById(id);
        if (member == null) {
            throw new RuntimeException("申请不存在");
        }

        member.setAgreeStatus(agreeStatus);
        member.setReviewComment(reviewComment);
        member.setReviewTime(LocalDateTime.now());
        member.setStatus(agreeStatus != null && agreeStatus == 1 ? STATUS_ACTIVE : STATUS_PENDING);
        baseMapper.updateById(member);
        syncMemberSearchIndex(member.getId());
        log.info("申请审核完成，id={}, agreeStatus={}", id, agreeStatus);
    }

    @Override
    public ClubMember getApplicationById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    @Transactional
    public void deleteApplication(Long id) {
        ClubMember existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("member not found or already deleted");
        }
        User linkedUser = findLinkedManagedUser(existing);
        if (linkedUser != null && linkedUser.getId() != null) {
            userService.deleteManagedUser(linkedUser.getId());
            deleteMemberSearchIndex(existing.getId(), existing.getTenantId());
            log.info("member deletion delegated to linked user, memberId={}, linkedUserId={}", id, linkedUser.getId());
            return;
        }
        int result = baseMapper.deleteById(id);
        if (result > 0) {
            deleteMemberSearchIndex(existing.getId(), existing.getTenantId());
            log.info("申请删除成功，id={}", id);
            return;
        }
        throw new RuntimeException("申请不存在或已删除");
    }

    @Override
    @Transactional
    public void restoreApplication(Long id) {
        ClubMember existing = baseMapper.selectAnyById(id);
        if (existing == null) {
            throw new RuntimeException("member not found");
        }
        if (!canAccessTenant(existing.getTenantId())) {
            throw new RuntimeException("member not found");
        }

        existing.setIsDeleted(0);
        existing.setUpdateTime(LocalDateTime.now());
        User linkedUser = findLinkedManagedUser(existing);
        if (linkedUser != null && linkedUser.getId() != null) {
            baseMapper.updateIncludingDeleted(existing);
            syncLinkedManagedUser(existing, existing.getStudentId());
            syncMemberSearchIndex(existing.getId());
            log.info("deleted member restored with active linked user, memberId={}, linkedUserId={}", id, linkedUser.getId());
            return;
        }

        String studentId = trimToNull(existing.getStudentId());
        if (studentId != null) {
            userService.provisionManagedUser(buildProvisionPayload(existing, existing.getTenantId(), studentId));
            syncMemberSearchIndex(existing.getId());
            log.info("deleted member restored through managed user provisioning, memberId={}", id);
            return;
        }

        int updated = baseMapper.updateIncludingDeleted(existing);
        if (updated <= 0) {
            throw new RuntimeException("restore member failed");
        }
        syncMemberSearchIndex(existing.getId());
        log.info("deleted member restored without linked user, memberId={}", id);
    }

    private User findLinkedManagedUser(ClubMember member) {
        if (member == null || member.getTenantId() == null) {
            return null;
        }
        String studentId = trimToNull(member.getStudentId());
        if (studentId == null) {
            return null;
        }
        List<User> users = userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, member.getTenantId())
                .eq(User::getStudentId, studentId)
                .eq(User::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (users == null || users.isEmpty()) {
            return null;
        }
        return users.get(0);
    }

    @Override
    public Map<String, Object> getStatistics() {
        userService.reconcilePeopleDomainForCurrentScope();
        LambdaQueryWrapper<ClubMember> queryWrapper = applyTenantScope(new LambdaQueryWrapper<>());
        queryWrapper.eq(ClubMember::getIsDeleted, 0);
        List<ClubMember> allLedgerMembers = baseMapper.selectList(queryWrapper);
        List<ClubMember> visibleLedgerMembers = filterVisibleMembers(allLedgerMembers);
        List<ClubMember> officialOrdinaryMembers = filterOfficialMembers(allLedgerMembers);

        Map<String, Object> stats = new HashMap<>();
        long pendingCount = visibleLedgerMembers.stream().filter(member -> !isActiveMember(member)).count();
        long managerCount = allLedgerMembers.stream().filter(this::isManagerRecord).filter(this::isActiveMember).count();
        long teacherCount = allLedgerMembers.stream().filter(this::isTeacherRecord).filter(this::isActiveMember).count();
        long activePrivilegedCount = allLedgerMembers.stream()
                .filter(member -> !isOrdinaryMemberRecord(member))
                .filter(this::isActiveMember)
                .count();
        long officialTotal = officialOrdinaryMembers.size() + activePrivilegedCount;

        stats.put("total", (int) officialTotal);
        stats.put("pending", pendingCount);
        stats.put("approved", officialTotal);
        stats.put("rejected", 0L);
        stats.put("activeCount", officialTotal);
        stats.put("inactiveCount", pendingCount);
        stats.put("ledgerTotal", allLedgerMembers.size());
        stats.put("ordinaryMemberCount", officialOrdinaryMembers.size());
        stats.put("ordinaryLedgerCount", visibleLedgerMembers.size());
        stats.put("managerCount", managerCount);
        stats.put("teacherCount", teacherCount);
        return stats;
    }

    private LambdaQueryWrapper<ClubMember> applyTenantScope(LambdaQueryWrapper<ClubMember> queryWrapper) {
        if (canManageAllTenants()) {
            applyActiveTenantScope(queryWrapper);
            return queryWrapper;
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            queryWrapper.eq(ClubMember::getTenantId, tenantId);
            return queryWrapper;
        }
        return queryWrapper;
    }

    @Override
    public void updateMember(Long id, ClubMember member) {
        ClubMember existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("成员不存在");
        }
        String previousStudentId = existing.getStudentId();

        if (member.getName() != null) {
            existing.setName(member.getName());
        }
        if (member.getStudentId() != null) {
            existing.setStudentId(member.getStudentId());
        }
        if (member.getEmail() != null) {
            existing.setEmail(member.getEmail());
        }
        if (member.getPhone() != null) {
            existing.setPhone(member.getPhone());
        }
        if (member.getMajor() != null) {
            existing.setMajor(member.getMajor());
        }
        if (member.getGrade() != null) {
            existing.setGrade(member.getGrade());
        }
        if (member.getDepartment() != null) {
            existing.setDepartment(member.getDepartment());
        }
        if (member.getPosition() != null) {
            existing.setPosition(member.getPosition());
        }
        if (member.getJoinDate() != null) {
            existing.setJoinDate(member.getJoinDate());
        }
        if (member.getStatus() != null) {
            existing.setStatus(member.getStatus());
        }
        existing.setUpdateTime(LocalDateTime.now());

        baseMapper.updateById(existing);
        syncLinkedManagedUser(existing, previousStudentId);
        syncMemberSearchIndex(existing.getId());
        log.info("成员信息更新成功，id={}", id);
    }

    private boolean isActiveMember(ClubMember member) {
        return normalizeMemberStatus(member == null ? null : member.getStatus()) == 1;
    }

    private boolean isPublicMember(ClubMember member) {
        return member != null
                && !Integer.valueOf(1).equals(member.getIsDeleted())
                && isActiveMember(member)
                && !isGraduatedOrdinaryMember(member);
    }

    private void syncMemberSearchIndex(Long id) {
        if (id == null) {
            return;
        }
        SearchIndexSyncUtils.runAfterCommit(() -> {
            ClubMember member = null;
            try {
                member = baseMapper.selectById(id);
                if (member == null || !isPublicMember(member)) {
                    searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_MEMBER, id,
                            member == null ? TenantContext.getTenantId() : member.getTenantId());
                    return;
                }
                searchClient.upsertGlobalDocument(buildMemberSearchDocument(member));
            } catch (Exception e) {
                log.warn("sync member global search index failed, memberId={}, tenantId={}, reason={}",
                        id, member == null ? null : member.getTenantId(), e.getMessage());
            }
        });
    }

    private void deleteMemberSearchIndex(Long id, Long tenantId) {
        if (id == null) {
            return;
        }
        Long resolvedTenantId = tenantId != null ? tenantId : TenantContext.getTenantId();
        SearchIndexSyncUtils.runAfterCommit(() -> {
            try {
                searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_MEMBER, id, resolvedTenantId);
            } catch (Exception e) {
                log.warn("delete member global search index failed, memberId={}, tenantId={}, reason={}",
                        id, resolvedTenantId, e.getMessage());
            }
        });
    }

    @Override
    public List<GlobalSearchDocumentDTO> exportSearchDocuments() {
        LambdaQueryWrapper<ClubMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ClubMember::getIsDeleted, 0)
                .orderByDesc(ClubMember::getUpdateTime);
        return list(queryWrapper).stream()
                .filter(this::isPublicMember)
                .map(this::buildMemberSearchDocument)
                .collect(Collectors.toList());
    }

    private GlobalSearchDocumentDTO buildMemberSearchDocument(ClubMember member) {
        return new GlobalSearchDocumentDTO()
                .setEntityType(SEARCH_ENTITY_TYPE_MEMBER)
                .setEntityId(member.getId())
                .setTenantId(member.getTenantId())
                .setTitle(member.getName())
                .setSummary(joinSearchText(member.getDepartment(), member.getPosition(), member.getMajor()))
                .setContent(joinSearchText(member.getStudentId(), member.getEmail(), member.getPhone(), member.getGrade()))
                .setTags(member.getStatus())
                .setRoute("/alumni")
                .setCoverUrl(null)
                .setUpdatedAt(formatUpdatedAt(member.getUpdateTime(), member.getJoinDate(), member.getCreateTime()))
                .setVisible(isPublicMember(member));
    }

    private String joinSearchText(String... values) {
        if (values == null) {
            return null;
        }
        return java.util.Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }

    private String formatUpdatedAt(LocalDateTime... times) {
        if (times == null) {
            return null;
        }
        for (LocalDateTime time : times) {
            if (time != null) {
                return time.toString();
            }
        }
        return null;
    }

    private List<ClubMember> filterOfficialMembers(List<ClubMember> members) {
        return filterVisibleMembers(members).stream()
                .filter(this::isActiveMember)
                .collect(Collectors.toList());
    }

    private List<ClubMember> filterOfficialRosterMembers(List<ClubMember> members) {
        return members.stream()
                .filter(this::isActiveMember)
                .filter(member -> !isOrdinaryMemberRecord(member) || !isGraduatedOrdinaryMember(member))
                .collect(Collectors.toList());
    }

    private List<ClubMember> filterVisibleMembers(List<ClubMember> members) {
        return members.stream()
                .filter(this::isOrdinaryMemberRecord)
                .filter(member -> !isGraduatedOrdinaryMember(member))
                .collect(Collectors.toList());
    }

    private boolean isGraduatedOrdinaryMember(ClubMember member) {
        Integer enrollmentYear = resolveEnrollmentYear(member);
        if (enrollmentYear == null) {
            return false;
        }
        return LocalDate.now().getYear() - enrollmentYear >= 4;
    }

    private Integer resolveEnrollmentYear(ClubMember member) {
        if (member == null) {
            return null;
        }
        return parseGradeEnrollmentYear(member.getGrade());
    }

    private Integer parseGradeEnrollmentYear(String grade) {
        String normalized = trimToNull(grade);
        if (normalized == null) {
            return null;
        }
        String digitsOnly = normalized.replaceAll("[^0-9]", "");
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

    private boolean isOrdinaryMemberRecord(ClubMember member) {
        return !isManagerRecord(member) && !isTeacherRecord(member);
    }

    private boolean isManagerRecord(ClubMember member) {
        return containsAnyPosition(member, MANAGER_POSITION_KEYWORDS);
    }

    private boolean isTeacherRecord(ClubMember member) {
        return containsAnyPosition(member, TEACHER_POSITION_KEYWORDS);
    }

    private boolean containsAnyPosition(ClubMember member, List<String> expectedPositions) {
        if (member == null || member.getPosition() == null || member.getPosition().isBlank()) {
            return false;
        }
        String position = member.getPosition().trim();
        return expectedPositions.stream()
                .filter(Objects::nonNull)
                .anyMatch(position::contains);
    }

    private int normalizeMemberStatus(String status) {
        if (status == null || status.isBlank()) {
            return 1;
        }
        String normalized = status.trim();
        return matchesAny(normalized, ACTIVE_STATUS_KEYWORDS) ? 1 : 0;
    }

    private boolean matchesAny(String value, List<String> candidates) {
        return candidates.stream()
                .filter(Objects::nonNull)
                .anyMatch(candidate -> candidate.equalsIgnoreCase(value));
    }

    private AdminUserProvisionDTO buildProvisionPayload(ClubMember member, Long tenantId, String studentId) {
        LocalDateTime now = LocalDateTime.now();
        AdminUserProvisionDTO dto = new AdminUserProvisionDTO();
        dto.setTenantId(tenantId);
        dto.setUsername(studentId);
        dto.setStudentId(studentId);
        dto.setName(defaultIfBlank(member.getName(), DEFAULT_MEMBER_NAME));
        dto.setEmail(trimToNull(member.getEmail()));
        dto.setCellPhone(trimToNull(member.getPhone()));
        dto.setMajor(trimToNull(member.getMajor()));
        dto.setGrade(trimToNull(member.getGrade()));
        dto.setDepartment(trimToNull(member.getDepartment()));
        dto.setPosition(defaultIfBlank(member.getPosition(), DEFAULT_MEMBER_POSITION));
        dto.setJoinDate(member.getJoinDate() == null ? now : member.getJoinDate());
        dto.setType(resolveProvisionUserType(member).getValue());
        dto.setStatus(1);
        return dto;
    }

    private UserType resolveProvisionUserType(ClubMember member) {
        if (isTeacherRecord(member)) {
            return UserType.TEACHER;
        }
        return UserType.STUDENT;
    }

    private void syncLinkedManagedUser(ClubMember member, String previousStudentId) {
        if (member == null || member.getTenantId() == null) {
            return;
        }
        String currentStudentId = trimToNull(member.getStudentId());
        String legacyStudentId = trimToNull(previousStudentId);
        if (currentStudentId == null && legacyStudentId == null) {
            return;
        }

        List<User> candidates = userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, member.getTenantId())
                .eq(User::getIsDeleted, 0)
                .and(wrapper -> {
                    if (legacyStudentId != null) {
                        wrapper.eq(User::getStudentId, legacyStudentId);
                        if (currentStudentId != null && !Objects.equals(currentStudentId, legacyStudentId)) {
                            wrapper.or().eq(User::getStudentId, currentStudentId);
                        }
                    } else {
                        wrapper.eq(User::getStudentId, currentStudentId);
                    }
                }));
        if (candidates == null || candidates.isEmpty()) {
            return;
        }

        User linkedUser = candidates.stream()
                .filter(user -> Objects.equals(trimToNull(user.getStudentId()), legacyStudentId))
                .findFirst()
                .orElseGet(() -> candidates.stream()
                        .filter(user -> Objects.equals(trimToNull(user.getStudentId()), currentStudentId))
                        .findFirst()
                        .orElse(candidates.get(0)));
        if (linkedUser.getId() == null) {
            return;
        }

        AdminUserProvisionDTO dto = new AdminUserProvisionDTO();
        dto.setTenantId(member.getTenantId());
        dto.setUsername(currentStudentId != null ? currentStudentId : linkedUser.getUsername());
        dto.setStudentId(currentStudentId);
        dto.setName(defaultIfBlank(member.getName(), linkedUser.getRealName()));
        dto.setEmail(trimToNull(member.getEmail()));
        dto.setCellPhone(trimToNull(member.getPhone()));
        dto.setMajor(trimToNull(member.getMajor()));
        dto.setGrade(trimToNull(member.getGrade()));
        dto.setDepartment(trimToNull(member.getDepartment()));
        dto.setPosition(trimToNull(member.getPosition()));
        dto.setJoinDate(member.getJoinDate());
        dto.setStatus(normalizeMemberStatus(member.getStatus()));
        userService.updateManagedUser(linkedUser.getId(), dto);
    }

    private String defaultIfBlank(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private boolean canManageAllTenants() {
        return Boolean.TRUE.equals(TenantContext.isSuperAdmin());
    }

    private List<Long> resolveScopedTenantIds() {
        if (canManageAllTenants()) {
            return resolveActiveTenantIds();
        }
        return List.of(currentTenantId());
    }

    private boolean canAccessTenant(Long tenantId) {
        if (tenantId == null) {
            return false;
        }
        return resolveScopedTenantIds().contains(tenantId);
    }

    private void applyActiveTenantScope(LambdaQueryWrapper<ClubMember> queryWrapper) {
        List<Long> activeTenantIds = resolveActiveTenantIds();
        if (activeTenantIds.isEmpty()) {
            queryWrapper.eq(ClubMember::getTenantId, -1L);
            return;
        }
        queryWrapper.in(ClubMember::getTenantId, activeTenantIds);
    }

    private List<Long> resolveActiveTenantIds() {
        LambdaQueryWrapper<RKTenant> tenantQuery = new LambdaQueryWrapper<>();
        tenantQuery.select(RKTenant::getId)
                .eq(RKTenant::getStatus, 1)
                .eq(RKTenant::getIsDeleted, 0)
                .and(wrapper -> wrapper.isNull(RKTenant::getExpireTime)
                        .or()
                        .gt(RKTenant::getExpireTime, LocalDateTime.now()));
        return tenantMapper.selectList(tenantQuery).stream()
                .map(RKTenant::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private <T> T runInTenantScope(Long tenantId, java.util.function.Supplier<T> action) {
        Long previousTenantId = TenantContext.getTenantId();
        Boolean previousSuperAdmin = TenantContext.isSuperAdmin();
        try {
            TenantContext.setTenantId(tenantId);
            TenantContext.setSuperAdmin(false);
            return action.get();
        } finally {
            if (previousTenantId == null) {
                TenantContext.removeTenantId();
            } else {
                TenantContext.setTenantId(previousTenantId);
            }
            TenantContext.setSuperAdmin(previousSuperAdmin);
        }
    }
}
