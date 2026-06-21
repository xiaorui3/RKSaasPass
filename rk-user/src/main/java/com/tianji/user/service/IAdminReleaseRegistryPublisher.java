package com.tianji.user.service;

import com.tianji.user.domain.vo.adminops.AdminReleaseRegistryPublishResult;

public interface IAdminReleaseRegistryPublisher {

    AdminReleaseRegistryPublishResult publishCurrentImage(String serviceCode,
                                                          String sourceImage,
                                                          String targetImage,
                                                          boolean dryRun);
}
