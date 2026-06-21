package com.tianji.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.api.client.auth.AuthClient;
import com.tianji.common.domain.R;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.dto.ReviewActionTokenDTO;
import com.tianji.user.domain.po.JoinRequest;
import com.tianji.user.domain.po.RegisterReviewRequest;
import com.tianji.user.domain.po.WorkflowActionLog;
import com.tianji.user.domain.vo.ReviewActionPreviewVO;
import com.tianji.user.mapper.RegisterReviewRequestMapper;
import com.tianji.user.mapper.WorkflowActionLogMapper;
import com.tianji.user.service.IAdmissionService;
import com.tianji.user.service.impl.ReviewActionTokenServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Api(tags = "review action")
@RestController
@RequestMapping("/api/admission/review-action")
@RequiredArgsConstructor
public class ReviewActionController {

    private final ReviewActionTokenServiceImpl reviewActionTokenService;
    private final IAdmissionService admissionService;
    private final RegisterReviewRequestMapper registerReviewRequestMapper;
    private final WorkflowActionLogMapper workflowActionLogMapper;
    private final ObjectMapper objectMapper;
    private final AuthClient authClient;

    @ApiOperation("preview review token")
    @GetMapping("/preview")
    public R<ReviewActionPreviewVO> preview(@RequestParam String token) {
        ReviewActionTokenDTO dto;
        WorkflowActionLog handledLog = null;
        try {
            dto = reviewActionTokenService.previewToken(token);
        } catch (BadRequestException e) {
            handledLog = findHandledLog(token);
            if (handledLog == null) {
                throw e;
            }
            dto = new ReviewActionTokenDTO();
            dto.setToken(token);
            dto.setTenantId(handledLog.getTenantId());
            dto.setTargetType(handledLog.getTargetType());
            dto.setTargetId(handledLog.getTargetId());
            dto.setAction(handledLog.getAction());
        }

        ReviewActionPreviewVO vo = new ReviewActionPreviewVO();
        vo.setToken(dto.getToken());
        vo.setBusinessType(resolveBusinessType(dto.getTargetType()));
        vo.setTargetType(dto.getTargetType());
        vo.setTargetId(dto.getTargetId());
        vo.setAction(dto.getAction());
        vo.setExecutable(handledLog == null);
        if (handledLog != null) {
            vo.setFinalState(handledLog.getFinalState());
            vo.setHandledByAuthUserId(handledLog.getActorAuthUserId());
            vo.setHandledTime(handledLog.getCreateTime() == null ? null : handledLog.getCreateTime().toString());
        }

        if ("JOIN".equalsIgnoreCase(dto.getTargetType())) {
            JoinRequest joinRequest = admissionService.getById(dto.getTargetId());
            if (joinRequest == null) {
                throw new BadRequestException("join request not found");
            }
            vo.setTitle("join review");
            vo.setTenantName(String.valueOf(joinRequest.getTenantId()));
            vo.setSummaryHtml(buildSummary(joinRequest.getFormPayloadJson(), joinRequest.getName(), joinRequest.getEmail()));
            return R.ok(vo);
        }

        RegisterReviewRequest request = registerReviewRequestMapper.selectById(dto.getTargetId());
        if (request == null) {
            throw new BadRequestException("register review request not found");
        }
        vo.setTitle("register review");
        vo.setTenantName(String.valueOf(request.getTenantId()));
        vo.setSummaryHtml(buildSummary(request.getFormPayloadJson(), request.getName(), request.getEmail()));
        return R.ok(vo);
    }

    @ApiOperation("execute review token")
    @PostMapping("/execute")
    public R<Boolean> execute(@RequestParam String token) {
        Long authUserId = UserContext.getUser();
        if (authUserId == null) {
            throw new BadRequestException("please login first");
        }
        if (findHandledLog(token) != null) {
            throw new BadRequestException("review action already handled");
        }

        ReviewActionTokenDTO dto = reviewActionTokenService.consumeToken(token, authUserId);
        boolean approved = "APPROVE".equalsIgnoreCase(dto.getAction());
        if ("JOIN".equalsIgnoreCase(dto.getTargetType())) {
            boolean ok = admissionService.reviewApplication(
                    dto.getTargetId(),
                    approved ? "通过" : "未通过",
                    approved ? "quick approve" : "quick reject",
                    authUserId
            );
            if (ok) {
                saveWorkflowActionLog(dto, authUserId, approved ? "APPROVED" : "REJECTED",
                        approved ? "quick approve join request" : "quick reject join request");
            }
            return R.ok(ok);
        }

        boolean ok = admissionService.reviewRegisterRequest(
                dto.getTargetId(),
                approved ? "APPROVED" : "REJECTED",
                approved ? "quick approve" : "quick reject",
                authUserId
        );
        if (ok) {
            saveWorkflowActionLog(dto, authUserId, approved ? "APPROVED" : "REJECTED",
                    approved ? "quick approve register request" : "quick reject register request");
        }
        return R.ok(ok);
    }

    private WorkflowActionLog findHandledLog(String token) {
        return workflowActionLogMapper.selectOne(new LambdaQueryWrapper<WorkflowActionLog>()
                .eq(WorkflowActionLog::getReviewToken, token)
                .orderByDesc(WorkflowActionLog::getId)
                .last("LIMIT 1"));
    }

    private void saveWorkflowActionLog(ReviewActionTokenDTO dto, Long authUserId, String finalState, String comment) {
        WorkflowActionLog log = new WorkflowActionLog()
                .setTenantId(dto.getTenantId())
                .setBusinessType(resolveBusinessType(dto.getTargetType()))
                .setTargetType(dto.getTargetType())
                .setTargetId(dto.getTargetId())
                .setReviewToken(dto.getToken())
                .setAction(dto.getAction())
                .setFinalState(finalState)
                .setActorAuthUserId(authUserId)
                .setComment(comment)
                .setSourceChannel("EMAIL_LINK")
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now())
                .setIsDeleted(0);
        workflowActionLogMapper.insert(log);
    }

    private String resolveBusinessType(String targetType) {
        if ("JOIN".equalsIgnoreCase(targetType)) {
            return "JOIN_REVIEW";
        }
        if ("REGISTER".equalsIgnoreCase(targetType)) {
            return "REGISTER_REVIEW";
        }
        return targetType == null ? "UNKNOWN" : targetType.toUpperCase() + "_REVIEW";
    }

    private String buildSummary(String json, String name, String email) {
        try {
            Map<String, Object> payload = json == null || json.isBlank()
                    ? new LinkedHashMap<>()
                    : objectMapper.readValue(json, LinkedHashMap.class);
            if (!payload.containsKey("name") && name != null) {
                payload.put("name", name);
            }
            if (!payload.containsKey("email") && email != null) {
                payload.put("email", email);
            }
            StringBuilder builder = new StringBuilder("<div>");
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                builder.append("<p><strong>")
                        .append(entry.getKey())
                        .append(":</strong>")
                        .append(entry.getValue() == null ? "-" : entry.getValue())
                        .append("</p>");
            }
            builder.append("</div>");
            return builder.toString();
        } catch (Exception e) {
            return "<div><p>summary parse failed</p></div>";
        }
    }
}
