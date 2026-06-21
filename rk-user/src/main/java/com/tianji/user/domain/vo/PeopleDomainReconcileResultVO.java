package com.tianji.user.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@Data
public class PeopleDomainReconcileResultVO {
    private long scannedAccountCount;
    private long scannedMemberCount;
    private long createdMemberCount;
    private long updatedMemberCount;
    private long restoredMemberCount;
    private long deletedMemberCount;
    private long unchangedMemberCount;
    private long skippedAccountWithoutStudentIdCount;
    private long createdUserCount;
    private long updatedUserCount;
    private long createdAuthAccountCount;
    private long skippedSyntheticMemberCount;
    private long skippedMismatchedSyntheticMemberCount;
    private long createdAlumniCount;
    private long updatedAlumniCount;
    private long skippedAlumniCount;
    private long costTimeMs;
    private String scopeLabel;
    private String summary;
    private List<String> details = new ArrayList<>();

    public void addDetail(String detail) {
        if (detail != null && !detail.isBlank()) {
            details.add(detail);
        }
    }

    public void finishSummary() {
        StringJoiner joiner = new StringJoiner("，", "修复完成：", "");
        joiner.add("扫描账号 " + scannedAccountCount);
        joiner.add("扫描成员 " + scannedMemberCount);
        joiner.add("新增成员台账 " + createdMemberCount);
        joiner.add("更新成员台账 " + updatedMemberCount);
        joiner.add("恢复成员台账 " + restoredMemberCount);
        joiner.add("新增用户账号 " + createdUserCount);
        joiner.add("更新用户账号 " + updatedUserCount);
        joiner.add("新增认证账号 " + createdAuthAccountCount);
        joiner.add("新增校友 " + createdAlumniCount);
        joiner.add("跳过无学号账号 " + skippedAccountWithoutStudentIdCount);
        joiner.add("合成管理人员台账 " + skippedSyntheticMemberCount + "（非异常）");
        if (skippedMismatchedSyntheticMemberCount > 0) {
            joiner.add("疑似冲突合成台账 " + skippedMismatchedSyntheticMemberCount + "（需核查）");
        }
        joiner.add("耗时 " + costTimeMs + "ms");
        summary = joiner.toString();
    }
}
