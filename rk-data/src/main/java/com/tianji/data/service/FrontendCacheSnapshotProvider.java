package com.tianji.data.service;

import com.tianji.data.service.impl.FrontendCacheWarmupServiceImpl.FrontendCacheItem;

import java.util.List;

public interface FrontendCacheSnapshotProvider {

    List<Long> listTenantIds();

    List<FrontendCacheItem> loadVisibleContent(Long tenantId);
}
