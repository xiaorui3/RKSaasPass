package com.tianji.data.controller;

import com.tianji.common.domain.R;
import com.tianji.data.model.vo.ApprovalTaskCenterVO;
import com.tianji.data.service.ApprovalTaskCenterService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data/approval-task-center")
@Api(tags = "统一审批任务中心")
public class ApprovalTaskCenterController {

    @Autowired
    private ApprovalTaskCenterService approvalTaskCenterService;

    @GetMapping
    @ApiOperation("获取统一审批任务中心聚合数据")
    public R<ApprovalTaskCenterVO> getApprovalTaskCenter(
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "taskType", required = false) String taskType,
            @RequestParam(value = "slaStatus", required = false) String slaStatus,
            @RequestParam(value = "status", required = false) String status) {
        return R.ok(approvalTaskCenterService.getApprovalTaskCenter(tenantId, taskType, slaStatus, status));
    }
}
