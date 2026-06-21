package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tianji.api.client.search.SearchClient;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.ClubMember;
import com.tianji.user.domain.po.RKTenant;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubMemberServiceImplStatisticsTest {

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
    void getStatistics_shouldIncludeOfficialOrdinaryAndPrivilegedMembersInPrimaryTotals() {
        TenantContext.setTenantId(1L);
        when(clubMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                member(1L, "成员", "正常"),
                member(2L, "成员", "待确认"),
                member(3L, "社团负责人", "正常"),
                member(4L, "指导老师", "正常")
        ));

        Map<String, Object> stats = service.getStatistics();

        verify(userService).reconcilePeopleDomainForCurrentScope();
        assertEquals(3, stats.get("total"));
        assertEquals(3L, stats.get("approved"));
        assertEquals(1L, stats.get("pending"));
        assertEquals(0L, stats.get("rejected"));
        assertEquals(3L, stats.get("activeCount"));
        assertEquals(1L, stats.get("inactiveCount"));
        assertEquals(4, stats.get("ledgerTotal"));
        assertEquals(1, stats.get("ordinaryMemberCount"));
        assertEquals(2, stats.get("ordinaryLedgerCount"));
        assertEquals(1L, stats.get("managerCount"));
        assertEquals(1L, stats.get("teacherCount"));
    }

    @Test
    void getAllApplications_shouldReturnApprovedOrdinaryAndPrivilegedMembers() {
        TenantContext.setTenantId(1L);
        when(clubMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                member(1L, "成员", "正常"),
                member(2L, "成员", "待确认"),
                member(3L, "社团负责人", "正常"),
                member(4L, "指导老师", "正常")
        ));

        List<ClubMember> members = service.getAllApplications();

        verify(admissionService).reconcileApprovedMembersForCurrentTenant();
        verify(userService).reconcilePeopleDomainForCurrentScope();
        assertEquals(3, members.size());
        assertEquals("成员", members.get(0).getPosition());
        assertEquals("正常", members.get(0).getStatus());
    }

    @Test
    void getStatistics_shouldTreatLegacyActiveStatusesAsOfficialMembers() {
        TenantContext.setTenantId(1L);
        when(clubMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                member(1L, "成员", "正常"),
                member(2L, "成员", "活跃"),
                member(3L, "成员", "待确认")
        ));

        Map<String, Object> stats = service.getStatistics();

        assertEquals(2, stats.get("total"));
        assertEquals(2L, stats.get("approved"));
        assertEquals(1L, stats.get("pending"));
        assertEquals(3, stats.get("ordinaryLedgerCount"));
    }

    @Test
    void getStatistics_shouldExcludeGraduatedGradesFromCurrentMemberCounts() {
        TenantContext.setTenantId(1L);
        ClubMember graduated = member(1L, "鎴愬憳", "姝ｅ父");
        graduated.setGrade((LocalDate.now().getYear() - 4) + "级");
        ClubMember current = member(2L, "鎴愬憳", "姝ｅ父");
        current.setGrade((LocalDate.now().getYear() - 1) + "级");
        when(clubMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.of(graduated, current));

        Map<String, Object> stats = service.getStatistics();

        assertEquals(1, stats.get("total"));
        assertEquals(1L, stats.get("approved"));
        assertEquals(1, stats.get("ordinaryLedgerCount"));
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
