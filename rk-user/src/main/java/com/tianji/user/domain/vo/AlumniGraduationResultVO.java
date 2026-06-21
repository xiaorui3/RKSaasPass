package com.tianji.user.domain.vo;

import lombok.Data;

@Data
public class AlumniGraduationResultVO {
    private Integer processYear;
    private int scannedCount;
    private int graduatedCount;
    private int gradeAdvancedCount;
    private int emailQueuedCount;
    private int skippedWithoutEmailCount;
    private int skippedAlreadyProcessedCount;

    public void increaseScannedCount() {
        scannedCount++;
    }

    public void increaseGraduatedCount() {
        graduatedCount++;
    }

    public void increaseGradeAdvancedCount() {
        gradeAdvancedCount++;
    }

    public void increaseEmailQueuedCount() {
        emailQueuedCount++;
    }

    public void increaseSkippedWithoutEmailCount() {
        skippedWithoutEmailCount++;
    }

    public void increaseSkippedAlreadyProcessedCount() {
        skippedAlreadyProcessedCount++;
    }
}
