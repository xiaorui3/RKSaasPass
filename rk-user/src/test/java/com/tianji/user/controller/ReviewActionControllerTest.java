package com.tianji.user.controller;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.common.domain.R;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.dto.ReviewActionTokenDTO;
import com.tianji.user.domain.po.WorkflowActionLog;
import com.tianji.user.mapper.RegisterReviewRequestMapper;
import com.tianji.user.mapper.WorkflowActionLogMapper;
import com.tianji.user.service.IAdmissionService;
import com.tianji.user.service.impl.ReviewActionTokenServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewActionControllerTest {

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), WorkflowActionLog.class);
    }

    @Mock
    private ReviewActionTokenServiceImpl reviewActionTokenService;
    @Mock
    private IAdmissionService admissionService;
    @Mock
    private RegisterReviewRequestMapper registerReviewRequestMapper;
    @Mock
    private WorkflowActionLogMapper workflowActionLogMapper;
    @Mock
    private com.tianji.api.client.auth.AuthClient authClient;

    private ReviewActionController controller;

    @BeforeEach
    void setUp() {
        controller = new ReviewActionController(
                reviewActionTokenService,
                admissionService,
                registerReviewRequestMapper,
                workflowActionLogMapper,
                new ObjectMapper(),
                authClient
        );
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    @Test
    void execute_shouldUseReadableJoinReviewStatusWhenApprovingByEmailToken() {
        ReviewActionTokenDTO dto = new ReviewActionTokenDTO();
        dto.setToken("join-approve-token");
        dto.setTenantId(2L);
        dto.setTargetType("JOIN");
        dto.setTargetId(66L);
        dto.setAction("APPROVE");

        when(workflowActionLogMapper.selectOne(any())).thenReturn(null);
        when(reviewActionTokenService.consumeToken("join-approve-token", 1001L)).thenReturn(dto);
        when(admissionService.reviewApplication(66L, "通过", "quick approve", 1001L)).thenReturn(true);

        UserContext.setUser(1001L);
        R<Boolean> result = controller.execute("join-approve-token");

        assertTrue(Boolean.TRUE.equals(result.getData()));
        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        verify(admissionService).reviewApplication(eq(66L), statusCaptor.capture(), eq("quick approve"), eq(1001L));
        assertEquals("通过", statusCaptor.getValue());
    }
}
