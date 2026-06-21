package com.tianji.activity.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.activity.domain.dto.ActivityVoteSaveDTO;
import com.tianji.activity.domain.po.Activity;
import com.tianji.activity.domain.po.ActivityRegistration;
import com.tianji.activity.domain.po.ActivityVote;
import com.tianji.activity.domain.po.ActivityVoteOption;
import com.tianji.activity.domain.vo.ActivityVoteVO;
import com.tianji.activity.mapper.ActivityMapper;
import com.tianji.activity.mapper.ActivityRegistrationMapper;
import com.tianji.activity.mapper.ActivityVoteMapper;
import com.tianji.activity.mapper.ActivityVoteOptionMapper;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.NotificationInternalSaveDTO;
import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Api(tags = "Activity vote API")
@RestController
@RequestMapping("/api/activity/votes")
@RequiredArgsConstructor
public class ActivityVoteController {

    private final ActivityMapper activityMapper;
    private final ActivityRegistrationMapper registrationMapper;
    private final ActivityVoteMapper voteMapper;
    private final ActivityVoteOptionMapper optionMapper;
    private final UserClient userClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ApiOperation("List current tenant activities for vote binding")
    @GetMapping("/activities")
    public R<List<Activity>> listActivities(@RequestParam(required = false) String keyword) {
        QueryWrapper<Activity> wrapper = new QueryWrapper<Activity>()
                .eq("tenant_id", currentTenantId());
        if (StringUtils.hasText(keyword)) {
            wrapper.like("activity_name", keyword.trim());
        }
        wrapper.orderByDesc("create_time").last("LIMIT 100");
        return R.ok(activityMapper.selectList(wrapper));
    }

    @ApiOperation("List registrations for vote target")
    @GetMapping("/activities/{activityId}/registrations")
    public R<List<Map<String, Object>>> listRegistrations(@PathVariable Long activityId) {
        List<ActivityRegistration> registrations = registrationMapper.selectList(
                new QueryWrapper<ActivityRegistration>()
                        .eq("activity_id", activityId)
                        .ne("registration_status", ActivityRegistration.STATUS_CANCELLED)
                        .eq("tenant_id", currentTenantId())
                        .orderByDesc("registration_time")
        );
        List<Map<String, Object>> result = registrations.stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("userId", item.getUserId());
            row.put("registrationStatus", item.getRegistrationStatus());
            row.put("registrationTime", item.getRegistrationTime());
            return row;
        }).collect(Collectors.toList());
        return R.ok(result);
    }

    @ApiOperation("List activity votes")
    @GetMapping
    public R<List<ActivityVoteVO>> listVotes(@RequestParam(required = false) Long activityId) {
        QueryWrapper<ActivityVote> wrapper = new QueryWrapper<ActivityVote>()
                .eq("tenant_id", currentTenantId());
        if (activityId != null) {
            wrapper.eq("activity_id", activityId);
        }
        wrapper.orderByDesc("create_time");
        return R.ok(voteMapper.selectList(wrapper).stream().map(this::toVO).collect(Collectors.toList()));
    }

    @ApiOperation("Create activity vote")
    @PostMapping
    public R<ActivityVoteVO> createVote(@RequestBody ActivityVoteSaveDTO dto) {
        validate(dto);
        Activity activity = activityMapper.selectOne(new QueryWrapper<Activity>()
                .eq("id", dto.getActivityId())
                .eq("tenant_id", currentTenantId()));
        if (activity == null) {
            return R.error("activity not found");
        }

        Long operatorId = UserContext.getUser();
        LocalDateTime now = LocalDateTime.now();
        List<Long> targetUserIds = resolveTargetUserIds(dto);
        ActivityVote vote = new ActivityVote();
        vote.setTenantId(currentTenantId());
        vote.setActivityId(activity.getId());
        vote.setTitle(dto.getTitle().trim());
        vote.setDescription(dto.getDescription());
        vote.setStatus(ActivityVote.STATUS_ACTIVE);
        vote.setTargetUserIds(toJson(targetUserIds));
        vote.setNotifySent(Boolean.TRUE.equals(dto.getNotifyUsers()) && !targetUserIds.isEmpty() ? 1 : 0);
        vote.setCreator(operatorId);
        vote.setUpdater(operatorId);
        vote.setCreateTime(now);
        vote.setUpdateTime(now);
        vote.setIsDeleted(0);
        voteMapper.insert(vote);

        int index = 0;
        for (String label : dto.getOptions()) {
            if (!StringUtils.hasText(label)) {
                continue;
            }
            ActivityVoteOption option = new ActivityVoteOption();
            option.setTenantId(currentTenantId());
            option.setVoteId(vote.getId());
            option.setOptionLabel(label.trim());
            option.setVoteCount(0);
            option.setSortOrder(index++);
            option.setCreator(operatorId);
            option.setUpdater(operatorId);
            option.setCreateTime(now);
            option.setUpdateTime(now);
            option.setIsDeleted(0);
            optionMapper.insert(option);
        }

        if (Boolean.TRUE.equals(dto.getNotifyUsers())) {
            notifyVoteCreated(activity, vote, targetUserIds);
        }
        return R.ok(toVO(vote));
    }

    @ApiOperation("Close activity vote")
    @PostMapping("/{id}/close")
    public R<ActivityVoteVO> closeVote(@PathVariable Long id) {
        ActivityVote vote = voteMapper.selectOne(new QueryWrapper<ActivityVote>()
                .eq("id", id)
                .eq("tenant_id", currentTenantId()));
        if (vote == null) {
            return R.error("vote not found");
        }
        vote.setStatus(ActivityVote.STATUS_CLOSED);
        vote.setClosedTime(LocalDateTime.now());
        vote.setUpdater(UserContext.getUser());
        vote.setUpdateTime(LocalDateTime.now());
        voteMapper.updateById(vote);
        return R.ok(toVO(vote));
    }

    private void validate(ActivityVoteSaveDTO dto) {
        if (dto == null || dto.getActivityId() == null) {
            throw new IllegalArgumentException("activity is required");
        }
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new IllegalArgumentException("vote title is required");
        }
        long optionCount = dto.getOptions() == null ? 0 : dto.getOptions().stream().filter(StringUtils::hasText).count();
        if (optionCount < 2) {
            throw new IllegalArgumentException("at least two options are required");
        }
    }

    private ActivityVoteVO toVO(ActivityVote vote) {
        Activity activity = activityMapper.selectById(vote.getActivityId());
        List<ActivityVoteOption> options = optionMapper.selectList(
                new QueryWrapper<ActivityVoteOption>()
                        .eq("vote_id", vote.getId())
                        .orderByAsc("sort_order")
                        .orderByAsc("id")
        );
        ActivityVoteVO vo = new ActivityVoteVO();
        vo.setId(vote.getId());
        vo.setActivityId(vote.getActivityId());
        vo.setActivityName(activity == null ? null : activity.getActivityName());
        vo.setTitle(vote.getTitle());
        vo.setDescription(vote.getDescription());
        vo.setStatus(vote.getStatus());
        vo.setStatusText(Objects.equals(vote.getStatus(), ActivityVote.STATUS_ACTIVE) ? "active" : "closed");
        vo.setTargetCount(parseLongList(vote.getTargetUserIds()).size());
        vo.setCreateTime(vote.getCreateTime());
        vo.setClosedTime(vote.getClosedTime());
        int total = 0;
        for (ActivityVoteOption option : options) {
            ActivityVoteVO.Option row = new ActivityVoteVO.Option();
            row.setId(option.getId());
            row.setLabel(option.getOptionLabel());
            row.setCount(option.getVoteCount() == null ? 0 : option.getVoteCount());
            total += row.getCount();
            vo.getOptions().add(row);
        }
        vo.setTotalVotes(total);
        return vo;
    }

    private List<Long> resolveTargetUserIds(ActivityVoteSaveDTO dto) {
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        registrationMapper.selectList(new QueryWrapper<ActivityRegistration>()
                .eq("activity_id", dto.getActivityId())
                .ne("registration_status", ActivityRegistration.STATUS_CANCELLED)
                .eq("tenant_id", currentTenantId()))
                .forEach(item -> {
                    if (item.getUserId() != null) {
                        userIds.add(item.getUserId());
                    }
                });
        if (dto.getExtraUserIds() != null) {
            dto.getExtraUserIds().stream().filter(Objects::nonNull).forEach(userIds::add);
        }
        return new ArrayList<>(userIds);
    }

    private void notifyVoteCreated(Activity activity, ActivityVote vote, List<Long> targetUserIds) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            log.info("skip activity vote notification because no target users, voteId={}", vote == null ? null : vote.getId());
            return;
        }
        try {
            NotificationInternalSaveDTO notification = new NotificationInternalSaveDTO();
            notification.setTenantId(currentTenantId());
            notification.setTitle("活动投票通知");
            notification.setContent("活动《" + activity.getActivityName() + "》发起了投票：" + vote.getTitle());
            notification.setType("activity");
            notification.setPriority(1);
            notification.setTargetType(1);
            notification.setTargetIds(targetUserIds);
            notification.setSenderName("activity-vote");
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("activityId", activity.getId());
            metadata.put("voteId", vote.getId());
            metadata.put("activityName", activity.getActivityName());
            notification.setMetadata(metadata);
            userClient.saveNotificationInternal(notification);
        } catch (Exception e) {
            log.warn("notify activity vote created failed, voteId={}, reason={}", vote.getId(), e.getMessage());
        }
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<Long> parseLongList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
