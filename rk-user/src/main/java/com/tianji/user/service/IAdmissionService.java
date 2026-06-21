package com.tianji.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.user.domain.po.JoinRequest;

import java.util.List;
import java.util.Map;

/**
 * 录取管理服务接口
 * 基于master分支的AdmissionController功能
 */
public interface IAdmissionService extends IService<JoinRequest> {

    // 提交入社申请
    boolean submitApplication(JoinRequest joinRequest);

    // 检查申请状态
    Map<String, Object> checkApplicationStatus(String studentId, String email);

    // 审核申请（通过/拒绝）
    boolean reviewApplication(Long id, String reviewStatus, String reviewComment, Long reviewerId);

    // 获取所有申请
    List<JoinRequest> getAllApplications();

    // 根据状态获取申请
    List<JoinRequest> getApplicationsByStatus(String status);

    // 根据学号获取申请
    JoinRequest getApplicationByStudentId(String studentId);

    // 删除申请
    boolean deleteApplication(Long id);

    // 获取申请统计
    Map<String, Object> getApplicationStatistics();

    // 对账并修复审批已通过但成员记录缺失/被软删的数据
    void reconcileApprovedMembersForCurrentTenant();

    boolean reviewRegisterRequest(Long id, String reviewStatus, String reviewComment, Long reviewerAuthUserId);

    // 验证邮箱与申请是否匹配
    boolean validateEmailMatch(String studentId, String email);

    // 检查用户是否是管理员
    boolean isAdminUser(String studentId, String email);

    boolean notifyRegisterSuccess(Long tenantId, Long authUserId, String username, String name, String email, String referralCode, java.util.Map<String, Object> formPayload);
}
