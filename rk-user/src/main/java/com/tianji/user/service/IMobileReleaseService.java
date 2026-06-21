package com.tianji.user.service;

import com.tianji.user.domain.dto.AdminMobileReleaseConfigDTO;
import com.tianji.user.domain.dto.AdminMobileReleaseRollbackDTO;
import com.tianji.user.domain.vo.MobileReleaseBuildLogVO;
import com.tianji.user.domain.vo.MobileReleaseBuildRecordVO;
import com.tianji.user.domain.vo.MobileReleaseConfigVO;
import com.tianji.user.domain.vo.MobileReleaseHistoryVO;

import java.util.List;

public interface IMobileReleaseService {
    MobileReleaseConfigVO getAdminConfig();

    MobileReleaseConfigVO saveAdminConfig(AdminMobileReleaseConfigDTO dto);

    List<MobileReleaseHistoryVO> getAdminHistory();

    MobileReleaseConfigVO withdrawAdminConfig();

    MobileReleaseConfigVO rollbackAdminConfig(AdminMobileReleaseRollbackDTO dto);

    MobileReleaseConfigVO autoBuildAdminConfig();

    List<MobileReleaseBuildRecordVO> getAdminBuildHistory();

    MobileReleaseBuildRecordVO getLatestBuildRecord();

    List<MobileReleaseBuildLogVO> getBuildLogs(Long buildId, Integer afterLineNo);

    MobileReleaseConfigVO getLatestRelease(Long tenantId, Long roleId, Integer versionCode);
}
