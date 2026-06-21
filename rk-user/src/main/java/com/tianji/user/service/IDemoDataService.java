package com.tianji.user.service;

import com.tianji.user.domain.vo.DemoDataCenterVO;

public interface IDemoDataService {
    DemoDataCenterVO getStatus();

    DemoDataCenterVO generate();

    DemoDataCenterVO cleanup();

    DemoDataCenterVO reset();
}
