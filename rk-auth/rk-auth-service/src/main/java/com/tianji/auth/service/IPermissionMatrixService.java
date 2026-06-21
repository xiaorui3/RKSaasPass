package com.tianji.auth.service;

import com.tianji.auth.domain.vo.PermissionMatrixVO;

public interface IPermissionMatrixService {

    PermissionMatrixVO buildMatrix(Long tenantId);
}
