package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.dto.auth.AdminUserProvisionDTO;
import com.tianji.common.enums.UserType;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.ClubMember;
import com.tianji.user.domain.po.RKTenant;
import com.tianji.user.domain.po.User;
import com.tianji.user.mapper.ClubMemberMapper;
import com.tianji.user.mapper.RKTenantMapper;
import com.tianji.user.service.IAdmissionService;
import com.tianji.user.service.IUserService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubMemberServiceImplTest {

    @Mock
    private ClubMemberMapper clubMemberMapper;
    @Mock
    private IAdmissionService admissionService;
    @Mock
    private RKTenantMapper tenantMapper;
    @Mock
    private IUserService userService;
    @Mock
    private SearchClient searchClient;

    private ClubMemberServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ClubMember.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RKTenant.class);
        service = new ClubMemberServiceImpl(admissionService, tenantMapper, userService, searchClient);
        ReflectionTestUtils.setField(service, "baseMapper", clubMemberMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getStatistics_shouldTreatBlankStatusAsActiveForOrdinaryMembers() {
        TenantContext.setTenantId(1L);

        ClubMember blankStatus = member(1L, "成员", "");
        ClubMember nullStatus = member(2L, "成员", null);
        ClubMember pending = member(3L, "成员", "待确认");

        when(clubMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.of(blankStatus, nullStatus, pending));

        Map<String, Object> stats = service.getStatistics();

        assertEquals(2, stats.get("total"));
        assertEquals(2L, stats.get("approved"));
        assertEquals(1L, stats.get("pending"));
        assertEquals(3, stats.get("ordinaryLedgerCount"));
    }

    @Test
    void getStatistics_shouldIncludeManagerAndTeacherInTotalButExcludeFromOrdinaryCounts() {
        TenantContext.setTenantId(1L);
        when(clubMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                member(1L, "成员", "正常"),
                member(2L, "副社长", "正常"),
                member(3L, "指导老师", "正常")
        ));

        Map<String, Object> stats = service.getStatistics();

        assertEquals(3, stats.get("total"));
        assertEquals(3L, stats.get("approved"));
        assertEquals(1, stats.get("ordinaryLedgerCount"));
        assertEquals(1, stats.get("ordinaryMemberCount"));
        assertEquals(1L, stats.get("managerCount"));
        assertEquals(1L, stats.get("teacherCount"));
    }

    @Test
    void submitApplication_shouldDelegateLegacyMemberAddIntoManagedUserProvisioning() {
        TenantContext.setTenantId(2L);

        ClubMember member = new ClubMember();
        member.setStudentId("20260066");
        member.setName("新成员");
        member.setEmail("member66@example.com");
        member.setPhone("13800138066");
        member.setMajor("Software");
        member.setGrade("2026");
        member.setDepartment("宣传部");
        member.setPosition("成员");

        service.submitApplication(member);

        org.mockito.ArgumentCaptor<AdminUserProvisionDTO> provisionCaptor =
                org.mockito.ArgumentCaptor.forClass(AdminUserProvisionDTO.class);
        verify(userService).provisionManagedUser(provisionCaptor.capture());
        AdminUserProvisionDTO dto = provisionCaptor.getValue();
        assertEquals(2L, dto.getTenantId());
        assertEquals("20260066", dto.getUsername());
        assertEquals("20260066", dto.getStudentId());
        assertEquals("新成员", dto.getName());
        assertEquals("member66@example.com", dto.getEmail());
        assertEquals("13800138066", dto.getCellPhone());
        assertEquals("Software", dto.getMajor());
        assertEquals("2026", dto.getGrade());
        assertEquals("宣传部", dto.getDepartment());
        assertEquals("成员", dto.getPosition());
        assertEquals(UserType.STUDENT.getValue(), dto.getType());
        assertEquals(1, dto.getStatus());
        verify(clubMemberMapper, never()).insert(any(ClubMember.class));
    }

    @Test
    void updateMember_shouldSyncLinkedManagedUserWhenIdentityFieldsChange() {
        ClubMember existing = member(8L, "成员", "正常");
        existing.setTenantId(2L);
        existing.setStudentId("20250001");
        existing.setName("旧成员");
        existing.setEmail("old@example.com");
        existing.setPhone("13800000001");
        existing.setMajor("Math");
        existing.setGrade("2025");
        existing.setDepartment("组织部");

        ClubMember incoming = new ClubMember();
        incoming.setStudentId("20250099");
        incoming.setName("新成员");
        incoming.setEmail("new@example.com");
        incoming.setPhone("13800000099");
        incoming.setMajor("Software");
        incoming.setGrade("2026");
        incoming.setDepartment("宣传部");
        incoming.setPosition("副社长");

        User linkedUser = new User();
        linkedUser.setId(88L);
        linkedUser.setTenantId(2L);
        linkedUser.setUsername("20250001");
        linkedUser.setStudentId("20250001");

        when(clubMemberMapper.selectById(8L)).thenReturn(existing);
        when(userService.list(any(Wrapper.class))).thenReturn(List.of(linkedUser));

        service.updateMember(8L, incoming);

        org.mockito.ArgumentCaptor<AdminUserProvisionDTO> updateCaptor =
                org.mockito.ArgumentCaptor.forClass(AdminUserProvisionDTO.class);
        verify(userService).updateManagedUser(org.mockito.Mockito.eq(88L), updateCaptor.capture());
        AdminUserProvisionDTO dto = updateCaptor.getValue();
        assertEquals(2L, dto.getTenantId());
        assertEquals("20250099", dto.getUsername());
        assertEquals("20250099", dto.getStudentId());
        assertEquals("新成员", dto.getName());
        assertEquals("new@example.com", dto.getEmail());
        assertEquals("13800000099", dto.getCellPhone());
        assertEquals("Software", dto.getMajor());
        assertEquals("2026", dto.getGrade());
        assertEquals("宣传部", dto.getDepartment());
        assertEquals("副社长", dto.getPosition());
    }

    @Test
    void deleteApplication_shouldDeleteLinkedManagedUserToPreventReconcileRestore() {
        ClubMember existing = member(42L, "member", "active");
        existing.setTenantId(2L);
        existing.setStudentId("20260042");

        User linkedUser = new User();
        linkedUser.setId(88L);
        linkedUser.setTenantId(2L);
        linkedUser.setStudentId("20260042");

        when(clubMemberMapper.selectById(42L)).thenReturn(existing);
        when(userService.list(any(Wrapper.class))).thenReturn(List.of(linkedUser));

        service.deleteApplication(42L);

        verify(userService).deleteManagedUser(88L);
        verify(clubMemberMapper, never()).deleteById(42L);
    }

    @Test
    void getDeletedApplications_shouldReturnTenantScopedDeletedMembers() {
        TenantContext.setTenantId(2L);
        ClubMember deleted = member(91L, "member", "active");
        deleted.setTenantId(2L);
        deleted.setIsDeleted(1);
        when(clubMemberMapper.selectDeletedByTenantIds(List.of(2L))).thenReturn(List.of(deleted));

        List<ClubMember> result = service.getDeletedApplications();

        assertEquals(List.of(deleted), result);
    }

    @Test
    void restoreApplication_shouldRestoreDeletedMemberAndProvisionLinkedUser() {
        TenantContext.setTenantId(2L);
        ClubMember deleted = member(92L, "member", "active");
        deleted.setTenantId(2L);
        deleted.setStudentId("20260092");
        deleted.setEmail("restore92@example.com");
        deleted.setIsDeleted(1);
        when(clubMemberMapper.selectAnyById(92L)).thenReturn(deleted);
        when(userService.list(any(Wrapper.class))).thenReturn(List.of());

        service.restoreApplication(92L);

        org.mockito.ArgumentCaptor<AdminUserProvisionDTO> provisionCaptor =
                org.mockito.ArgumentCaptor.forClass(AdminUserProvisionDTO.class);
        verify(userService).provisionManagedUser(provisionCaptor.capture());
        assertEquals(2L, provisionCaptor.getValue().getTenantId());
        assertEquals("20260092", provisionCaptor.getValue().getStudentId());
        assertEquals("restore92@example.com", provisionCaptor.getValue().getEmail());
    }

    private ClubMember member(Long id, String position, String status) {
        ClubMember member = new ClubMember();
        member.setId(id);
        member.setTenantId(1L);
        member.setStudentId("S" + id);
        member.setName("member-" + id);
        member.setPosition(position);
        member.setStatus(status);
        member.setIsDeleted(0);
        return member;
    }
}
