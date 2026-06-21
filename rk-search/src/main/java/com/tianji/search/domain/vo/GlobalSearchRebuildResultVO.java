package com.tianji.search.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class GlobalSearchRebuildResultVO {

    private Integer deletedCount;

    private Integer indexedCount;

    private Map<String, Integer> sourceCounts = new LinkedHashMap<>();

    private String status;
}
