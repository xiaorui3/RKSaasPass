package com.tianji.user.handler;

import com.tianji.user.domain.vo.AlumniGraduationResultVO;
import com.tianji.user.service.IAlumniGraduationWorkflowService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlumniGraduationJobHandler {

    private final IAlumniGraduationWorkflowService alumniGraduationWorkflowService;

    @XxlJob("alumniAnnualGraduation")
    public void alumniAnnualGraduation() {
        Integer processYear = parseYear(XxlJobHelper.getJobParam());
        AlumniGraduationResultVO result = alumniGraduationWorkflowService.runAnnualGraduation(processYear, null);
        String message = String.format(
                "processYear=%s scanned=%d graduated=%d advanced=%d emails=%d skipped=%d",
                result.getProcessYear(),
                result.getScannedCount(),
                result.getGraduatedCount(),
                result.getGradeAdvancedCount(),
                result.getEmailQueuedCount(),
                result.getSkippedAlreadyProcessedCount()
        );
        log.info("alumni annual graduation finished: {}", message);
        XxlJobHelper.handleSuccess(message);
    }

    private Integer parseYear(String param) {
        if (param == null || param.trim().isEmpty()) {
            return null;
        }
        String trimmed = param.trim();
        try {
            return Integer.valueOf(trimmed);
        } catch (NumberFormatException ignored) {
            log.warn("ignore invalid alumniAnnualGraduation year param: {}", trimmed);
            return null;
        }
    }
}
