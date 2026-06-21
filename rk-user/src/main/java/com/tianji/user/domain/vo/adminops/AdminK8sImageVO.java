package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminK8sImageVO {
    private String nodeName;
    private String nodeIp;
    private String image;
    private String repository;
    private String tag;
    private String imageId;
    private String size;
    private Long sizeBytes;
    private Boolean usedByWorkloads;
    private Boolean platformImage;
    private Boolean canPrune;
    private List<String> workloadRefs;
}
