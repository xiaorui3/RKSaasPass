package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminK8sOverviewVO {
    private Boolean kubectlInstalled;
    private Boolean clusterReachable;
    private String clientVersion;
    private String clusterMessage;
    private List<AdminK8sNodeVO> nodes;
    private List<String> namespaces;
    private List<AdminContainerVO> containers;
}
