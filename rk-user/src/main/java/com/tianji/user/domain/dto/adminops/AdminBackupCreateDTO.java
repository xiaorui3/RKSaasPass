package com.tianji.user.domain.dto.adminops;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AdminBackupCreateDTO {
    private List<String> databases;
    private Map<String, List<String>> tables;
    private String note;
}
