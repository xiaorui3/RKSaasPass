package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AdminTopologyVO {
    private String namespace;
    private List<Map<String, Object>> nodes;
    private List<Map<String, Object>> edges;
}
