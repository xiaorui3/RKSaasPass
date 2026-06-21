package com.tianji.user.domain.dto.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminK8sImageCleanupDTO {
    private String nodeName;
    private String nodeIp;
    private List<String> images;
    private String keyword;
    private Boolean unusedOnly = true;
    private Boolean dryRun = true;
    private String confirmText;
    private String reason;
}
