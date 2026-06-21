package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tianji.user.domain.dto.AlumniProfileSubmitDTO;
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
import com.tianji.user.service.IEmailCenterService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlumniGraduationWorkflowServiceTest {

    @Mock
    private ClubMemberMapper clubMemberMapper;
    @Mock
    private ClubAlumniMapper clubAlumniMapper;
    @Mock
    private AlumniProfileTokenMapper alumniProfileTokenMapper;
    @Mock
    private AlumniGraduationLogMapper alumniGraduationLogMapper;
    @Mock
    private RKTenantMapper tenantMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private IEmailCenterService emailCenterService;

    private AlumniGraduationWorkflowService service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ClubMember.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ClubAlumni.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AlumniProfileToken.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AlumniGraduationLog.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RKTenant.class);
        service = new AlumniGraduationWorkflowService(
                clubMemberMapper,
                clubAlumniMapper,
                alumniProfileTokenMapper,
                alumniGraduationLogMapper,
                tenantMapper,
                userMapper,
                emailCenterService
        );
    }

    @Test
    void runAnnualGraduation_shouldCreateAlumniAndSendOneTimeProfileForm() {
        int currentYear = LocalDate.now().getYear();
        ClubMember member = member(10L, 1L, "20220001", "graduated@example.com", (currentYear - 4) + "级");
        when(tenantMapper.selectList(any(Wrapper.class))).thenReturn(List.of(activeTenant(1L)));
        when(clubMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.of(member));
        when(alumniGraduationLogMapper.selectCount(any(Wrapper.class))).thenReturn(0);
        when(clubAlumniMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(clubAlumniMapper.insert(any(ClubAlumni.class))).thenAnswer(invocation -> {
            ClubAlumni alumni = invocation.getArgument(0);
            alumni.setId(100L);
            return 1;
        });
        when(alumniProfileTokenMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(alumniProfileTokenMapper.insert(any(AlumniProfileToken.class))).thenAnswer(invocation -> {
            AlumniProfileToken token = invocation.getArgument(0);
            token.setId(200L);
            return 1;
        });
        when(emailCenterService.send(any())).thenReturn(Map.of("queuedCount", 1));

        AlumniGraduationResultVO result = service.runAnnualGraduation(currentYear, null);

        assertEquals(1, result.getScannedCount());
        assertEquals(1, result.getGraduatedCount());
        assertEquals(1, result.getEmailQueuedCount());

        ArgumentCaptor<ClubAlumni> alumniCaptor = ArgumentCaptor.forClass(ClubAlumni.class);
        verify(clubAlumniMapper).insert(alumniCaptor.capture());
        assertEquals("20220001", alumniCaptor.getValue().getStudentId());
        assertEquals("已毕业", alumniCaptor.getValue().getMemberStatus());
        assertEquals(currentYear, alumniCaptor.getValue().getExpectedGraduationYear());

        ArgumentCaptor<AlumniProfileToken> tokenCaptor = ArgumentCaptor.forClass(AlumniProfileToken.class);
        verify(alumniProfileTokenMapper).insert(tokenCaptor.capture());
        assertEquals(100L, tokenCaptor.getValue().getAlumniId());
        assertEquals(10L, tokenCaptor.getValue().getMemberId());
        assertEquals("SENT", tokenCaptor.getValue().getStatus());

        verify(emailCenterService).send(argThat(dto ->
                dto.getManualEmails().contains("graduated@example.com")
                        && dto.getSubject().contains("校友信息")
                        && dto.getContent().contains("/alumni/profile-form?token=")
        ));
        verify(clubMemberMapper).updateById(argThat(updated ->
                updated.getId().equals(10L) && "已毕业".equals(updated.getStatus())
        ));
        verify(alumniGraduationLogMapper).insert(any(AlumniGraduationLog.class));
    }

    @Test
    void runAnnualGraduation_shouldNotResendWhenSameYearAlreadyProcessed() {
        int currentYear = LocalDate.now().getYear();
        ClubMember member = member(10L, 1L, "20220001", "graduated@example.com", (currentYear - 4) + "级");
        when(tenantMapper.selectList(any(Wrapper.class))).thenReturn(List.of(activeTenant(1L)));
        when(clubMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.of(member));
        when(alumniGraduationLogMapper.selectCount(any(Wrapper.class))).thenReturn(1);

        AlumniGraduationResultVO result = service.runAnnualGraduation(currentYear, null);

        assertEquals(1, result.getSkippedAlreadyProcessedCount());
        verify(clubAlumniMapper, never()).insert(any());
        verify(alumniProfileTokenMapper, never()).insert(any());
        verify(emailCenterService, never()).send(any());
    }

    @Test
    void runAnnualGraduation_shouldAdvanceClassStandingButKeepCohortGrade() {
        int currentYear = LocalDate.now().getYear();
        ClubMember freshman = member(11L, 1L, "20260001", "freshman@example.com", "大一");
        ClubMember cohort = member(12L, 1L, "20260002", "cohort@example.com", currentYear + "级");
        when(tenantMapper.selectList(any(Wrapper.class))).thenReturn(List.of(activeTenant(1L)));
        when(clubMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.of(freshman, cohort));
        when(alumniGraduationLogMapper.selectCount(any(Wrapper.class))).thenReturn(0);

        AlumniGraduationResultVO result = service.runAnnualGraduation(currentYear, null);

        assertEquals(2, result.getScannedCount());
        assertEquals(1, result.getGradeAdvancedCount());
        verify(clubMemberMapper).updateById(argThat(updated ->
                updated.getId().equals(11L) && "大二".equals(updated.getGrade())
        ));
        verify(clubMemberMapper, never()).updateById(argThat(updated -> updated.getId().equals(12L)));
    }

    @Test
    void submitProfileForm_shouldUpdateAlumniAndConsumeTokenOnce() {
        AlumniProfileToken token = new AlumniProfileToken()
                .setId(1L)
                .setAlumniId(100L)
                .setEmail("alumni@example.com")
                .setToken("token-1")
                .setStatus("SENT")
                .setExpiresAt(LocalDateTime.now().plusDays(3));
        token.setTenantId(1L);
        token.setIsDeleted(0);
        ClubAlumni alumni = new ClubAlumni()
                .setId(100L)
                .setTenantId(1L)
                .setName("alumni")
                .setEmail("alumni@example.com")
                .setIsDeleted(0);
        when(alumniProfileTokenMapper.selectOne(any(Wrapper.class))).thenReturn(token);
        when(clubAlumniMapper.selectById(100L)).thenReturn(alumni);

        AlumniProfileFormVO form = service.getProfileForm("token-1");
        assertEquals("alumni", form.getName());

        AlumniProfileSubmitDTO submit = new AlumniProfileSubmitDTO();
        submit.setWorkCity("杭州");
        submit.setWorkUnit("示例公司");
        submit.setCurrentContact("alumni@example.com");

        service.submitProfileForm("token-1", submit);

        verify(clubAlumniMapper).updateById(argThat(updated ->
                "杭州".equals(updated.getWorkCity()) && "示例公司".equals(updated.getWorkUnit())
        ));
        verify(alumniProfileTokenMapper).updateById(argThat(updated ->
                "SUBMITTED".equals(updated.getStatus()) && updated.getSubmittedTime() != null
        ));
    }

    @Test
    void submitProfileForm_shouldRejectAlreadySubmittedToken() {
        AlumniProfileToken token = new AlumniProfileToken()
                .setId(1L)
                .setAlumniId(100L)
                .setToken("token-1")
                .setStatus("SUBMITTED")
                .setExpiresAt(LocalDateTime.now().plusDays(3));
        token.setTenantId(1L);
        token.setIsDeleted(0);
        when(alumniProfileTokenMapper.selectOne(any(Wrapper.class))).thenReturn(token);

        assertThrows(RuntimeException.class, () -> service.submitProfileForm("token-1", new AlumniProfileSubmitDTO()));
    }

    private ClubMember member(Long id, Long tenantId, String studentId, String email, String grade) {
        ClubMember member = new ClubMember();
        member.setId(id);
        member.setTenantId(tenantId);
        member.setStudentId(studentId);
        member.setName("member-" + id);
        member.setEmail(email);
        member.setMajor("Software");
        member.setDepartment("研发部");
        member.setPosition("成员");
        member.setGrade(grade);
        member.setStatus("正常");
        member.setIsDeleted(0);
        return member;
    }

    private RKTenant activeTenant(Long id) {
        RKTenant tenant = new RKTenant();
        tenant.setId(id);
        tenant.setStatus(1);
        tenant.setIsDeleted(0);
        return tenant;
    }
}
