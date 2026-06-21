package com.tianji.user.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ScopedUserStatisticsVO {
    private long accountCount;
    private long adminAccountCount;
    private long teacherAccountCount;
    private long studentAccountCount;
    private long studentAccountWithStudentIdCount;
    private long studentAccountWithoutStudentIdCount;
    private long studentAccountMissingMemberLedgerCount;
    private long standaloneStudentAccountCount;
    private List<String> standaloneStudentAccountSamples;
    private long memberLinkedAccountCount;
    private long standaloneAccountCount;
}
