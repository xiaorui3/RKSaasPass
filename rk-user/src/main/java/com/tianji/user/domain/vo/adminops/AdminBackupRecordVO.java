package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AdminBackupRecordVO {
    private Long id;
    private String name;
    private String database;
    private List<String> databaseList;
    private Map<String, List<String>> tableSummary;
    private Integer tableCount;
    private String type;
    private String size;
    private String fileName;
    private String createTime;
    private String status;
    private String note;
    private Boolean downloadable;
}
