package com.tianji.user.service;

import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.user.domain.po.ClubMember;

import java.util.List;
import java.util.Map;

/**
 * 社团成员申请服务接口
 * 基于master分支的核心业务逻辑
 */
public interface IClubMemberService {

    /**
     * 提交社团加入申请
     */
    void submitApplication(ClubMember member);

    /**
     * 获取所有申请列表
     */
    List<ClubMember> getAllApplications();

    List<ClubMember> getAllApplications(Long tenantId);

    List<ClubMember> getDeletedApplications();

    /**
     * 检查申请状态
     */
    Map<String, Object> checkApplicationStatus(String studentId);

    /**
     * 审核申请
     */
    void reviewApplication(Long id, Integer agreeStatus, String reviewComment);

    /**
     * 根据ID获取申请详情
     */
    ClubMember getApplicationById(Long id);

    /**
     * 删除申请
     */
    void deleteApplication(Long id);

    void restoreApplication(Long id);

    /**
     * 获取统计数据
     */
    Map<String, Object> getStatistics();

    /**
     * 更新成员信息
     */
    void updateMember(Long id, ClubMember member);

    List<GlobalSearchDocumentDTO> exportSearchDocuments();
}
