package com.tianji.data.service;

import com.tianji.data.model.vo.OpenSourceHealthVO;

public interface OpenSourceHealthService {
    OpenSourceHealthVO check(Long tenantId);
}
