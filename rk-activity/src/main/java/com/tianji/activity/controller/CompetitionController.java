package com.tianji.activity.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.activity.domain.converter.CompetitionConverter;
import com.tianji.activity.domain.converter.CompetitionRegistrationConverter;
import com.tianji.activity.domain.dto.CompetitionQueryDTO;
import com.tianji.activity.domain.dto.CompetitionRegisterDTO;
import com.tianji.activity.domain.dto.CompetitionResultSaveDTO;
import com.tianji.activity.domain.dto.CompetitionSaveDTO;
import com.tianji.activity.domain.dto.CompetitionUpdateDTO;
import com.tianji.activity.domain.po.Competition;
import com.tianji.activity.domain.po.CompetitionParticipant;
import com.tianji.activity.domain.vo.CompetitionDetailVO;
import com.tianji.activity.domain.vo.CompetitionListVO;
import com.tianji.activity.domain.vo.CompetitionRegistrationVO;
import com.tianji.activity.domain.vo.CompetitionSimpleVO;
import com.tianji.activity.mapper.CompetitionParticipantMapper;
import com.tianji.activity.service.ICompetitionService;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.CreditGrantDTO;
import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Api(tags = "比赛管理接口")
@RestController
@RequestMapping("/api/competition")
@RequiredArgsConstructor
@Validated
public class CompetitionController {

    private final ICompetitionService competitionService;
    private final CompetitionParticipantMapper participantMapper;
    private final UserClient userClient;
    private final Validator validator;

    @ApiOperation("分页查询比赛列表")
    @GetMapping("/page")
    public R<IPage<CompetitionListVO>> page(CompetitionQueryDTO queryDTO) {
        try {
            Page<Competition> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
            IPage<Competition> entityPage = competitionService.pageCompetitions(page, queryDTO);
            IPage<CompetitionListVO> voPage = entityPage.convert(CompetitionConverter::toListVO);

            Long userId = UserContext.getUser();
            if (userId != null) {
                voPage.getRecords().forEach(vo -> {
                    CompetitionParticipant participant = participantMapper.findByCompetitionAndUser(vo.getId(), userId);
                    vo.setRegistered(participant != null && !CompetitionParticipant.STATUS_CANCELLED.equals(participant.getStatus()));
                });
            }
            return R.ok(voPage);
        } catch (Exception e) {
            log.error("分页查询比赛列表失败", e);
            return R.error("查询失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取所有比赛")
    @GetMapping("/list")
    public R<List<CompetitionListVO>> list() {
        try {
            List<Competition> competitions = competitionService.getAllCompetitions();
            List<CompetitionListVO> voList = CompetitionConverter.toListVOList(competitions);

            Long userId = UserContext.getUser();
            if (userId != null) {
                voList.forEach(vo -> {
                    CompetitionParticipant participant = participantMapper.findByCompetitionAndUser(vo.getId(), userId);
                    vo.setRegistered(participant != null && !CompetitionParticipant.STATUS_CANCELLED.equals(participant.getStatus()));
                });
            }
            return R.ok(voList);
        } catch (Exception e) {
            log.error("获取比赛列表失败", e);
            return R.error("获取比赛列表失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取已发布的比赛")
    @GetMapping("/published")
    public R<List<CompetitionListVO>> getPublished(
            @RequestParam(defaultValue = "create_time") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        try {
            List<Competition> competitions = competitionService.getPublishedCompetitions(sortBy, sortOrder);
            List<CompetitionListVO> voList = CompetitionConverter.toListVOList(competitions);

            Long userId = UserContext.getUser();
            if (userId != null) {
                voList.forEach(vo -> {
                    CompetitionParticipant participant = participantMapper.findByCompetitionAndUser(vo.getId(), userId);
                    vo.setRegistered(participant != null && !CompetitionParticipant.STATUS_CANCELLED.equals(participant.getStatus()));
                });
            }
            return R.ok(voList);
        } catch (Exception e) {
            log.error("获取已发布比赛失败", e);
            return R.error("获取已发布比赛失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取推荐比赛")
    @GetMapping("/featured")
    public R<List<CompetitionListVO>> getFeatured() {
        try {
            List<Competition> competitions = competitionService.getFeaturedCompetitions();
            List<CompetitionListVO> voList = CompetitionConverter.toListVOList(competitions);

            Long userId = UserContext.getUser();
            if (userId != null) {
                voList.forEach(vo -> {
                    CompetitionParticipant participant = participantMapper.findByCompetitionAndUser(vo.getId(), userId);
                    vo.setRegistered(participant != null && !CompetitionParticipant.STATUS_CANCELLED.equals(participant.getStatus()));
                });
            }
            return R.ok(voList);
        } catch (Exception e) {
            log.error("获取推荐比赛失败", e);
            return R.error("获取推荐比赛失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取其他租户共享比赛")
    @GetMapping("/shared")
    public R<List<CompetitionListVO>> getShared() {
        try {
            List<Competition> competitions = competitionService.getSharedCompetitions(10);
            return R.ok(CompetitionConverter.toListVOList(competitions));
        } catch (Exception e) {
            log.error("获取共享比赛失败", e);
            return R.error("获取共享比赛失败：" + e.getMessage());
        }
    }

    @ApiOperation("根据状态获取比赛")
    @GetMapping("/status/{status}")
    public R<List<CompetitionListVO>> getByStatus(@PathVariable String status) {
        try {
            List<Competition> competitions = competitionService.getCompetitionsByStatus(status);
            List<CompetitionListVO> voList = CompetitionConverter.toListVOList(competitions);

            Long userId = UserContext.getUser();
            if (userId != null) {
                voList.forEach(vo -> {
                    CompetitionParticipant participant = participantMapper.findByCompetitionAndUser(vo.getId(), userId);
                    vo.setRegistered(participant != null && !CompetitionParticipant.STATUS_CANCELLED.equals(participant.getStatus()));
                });
            }
            return R.ok(voList);
        } catch (Exception e) {
            log.error("根据状态获取比赛失败", e);
            return R.error("获取比赛列表失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取简单比赛列表")
    @GetMapping("/simple")
    public R<List<CompetitionSimpleVO>> getSimpleList() {
        try {
            List<Competition> competitions = competitionService.getPublishedCompetitions("create_time", "desc");
            return R.ok(CompetitionConverter.toSimpleVOList(competitions));
        } catch (Exception e) {
            log.error("获取简单比赛列表失败", e);
            return R.error("获取比赛列表失败：" + e.getMessage());
        }
    }

    @ApiOperation("搜索比赛")
    @GetMapping("/search")
    public R<List<CompetitionListVO>> search(@RequestParam String keyword) {
        try {
            List<Competition> competitions = competitionService.searchCompetitions(keyword);
            return R.ok(CompetitionConverter.toListVOList(competitions));
        } catch (Exception e) {
            log.error("搜索比赛失败", e);
            return R.error("搜索比赛失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取比赛详情")
    @GetMapping("/{id}")
    public R<CompetitionDetailVO> detail(@PathVariable Long id) {
        try {
            Competition competition = competitionService.getCompetitionById(id);
            if (competition == null) {
                return R.error("比赛不存在");
            }

            CompetitionDetailVO vo = CompetitionConverter.toDetailVO(competition);
            vo.setCannotRegisterReason(CompetitionConverter.getCannotRegisterReason(competition));

            Long userId = UserContext.getUser();
            if (userId != null) {
                CompetitionParticipant participant = participantMapper.findByCompetitionAndUser(id, userId);
                if (participant != null && !CompetitionParticipant.STATUS_CANCELLED.equals(participant.getStatus())) {
                    vo.setRegistered(true);
                    vo.setRegistrationStatus(participant.getStatus());
                } else {
                    vo.setRegistered(false);
                }
            }

            competitionService.incrementViewCount(id);
            return R.ok(vo);
        } catch (Exception e) {
            log.error("获取比赛详情失败", e);
            return R.error("获取比赛详情失败：" + e.getMessage());
        }
    }

    @ApiOperation("创建比赛")
    @PostMapping
    @PreAuthorize("hasAuthority('content:competition:add')")
    public R<Long> create(@Valid @RequestBody CompetitionSaveDTO saveDTO) {
        try {
            Competition competition = CompetitionConverter.toEntity(saveDTO);
            Long id = competitionService.createCompetition(competition);
            return id != null ? R.ok(id) : R.error("创建比赛失败");
        } catch (Exception e) {
            log.error("创建比赛失败", e);
            return R.error("创建比赛失败：" + e.getMessage());
        }
    }

    @ApiOperation("更新比赛")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('content:competition:edit')")
    public R<String> update(@PathVariable Long id, @Valid @RequestBody CompetitionUpdateDTO updateDTO) {
        try {
            Competition existing = competitionService.getCompetitionById(id);
            if (existing == null) {
                return R.error("比赛不存在");
            }
            updateDTO.setId(id);
            CompetitionConverter.updateEntity(existing, updateDTO);
            return competitionService.updateCompetition(existing) > 0 ? R.ok("更新比赛成功") : R.error("更新比赛失败");
        } catch (Exception e) {
            log.error("更新比赛失败", e);
            return R.error("更新比赛失败：" + e.getMessage());
        }
    }

    @ApiOperation("删除比赛")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('content:competition:remove')")
    public R<String> delete(@PathVariable Long id) {
        try {
            return competitionService.deleteCompetition(id) > 0 ? R.ok("删除比赛成功") : R.error("删除比赛失败");
        } catch (Exception e) {
            log.error("删除比赛失败", e);
            return R.error("删除比赛失败：" + e.getMessage());
        }
    }

    @ApiOperation("发布比赛")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('content:competition:edit')")
    public R<String> publish(@PathVariable Long id) {
        try {
            Competition competition = competitionService.getCompetitionById(id);
            if (competition == null) {
                return R.error("比赛不存在");
            }
            competition.setIsPublished(true);
            competition.setStatus("PUBLISHED");
            return competitionService.updateCompetition(competition) > 0 ? R.ok("发布比赛成功") : R.error("发布比赛失败");
        } catch (Exception e) {
            log.error("发布比赛失败", e);
            return R.error("发布比赛失败：" + e.getMessage());
        }
    }

    @ApiOperation("取消比赛")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('content:competition:edit')")
    public R<String> cancel(@PathVariable Long id) {
        try {
            Competition competition = competitionService.getCompetitionById(id);
            if (competition == null) {
                return R.error("比赛不存在");
            }
            competition.setStatus("CANCELLED");
            return competitionService.updateCompetition(competition) > 0 ? R.ok("取消比赛成功") : R.error("取消比赛失败");
        } catch (Exception e) {
            log.error("取消比赛失败", e);
            return R.error("取消比赛失败：" + e.getMessage());
        }
    }

    @ApiOperation("比赛报名")
    @PostMapping("/{id}/register")
    public R<CompetitionRegistrationVO> register(@PathVariable Long id, @RequestBody(required = false) CompetitionRegisterDTO registerDTO) {
        try {
            Long userId = UserContext.getUser();
            if (userId == null) {
                return R.error("请先登录");
            }

            Competition competition = competitionService.getCompetitionById(id);
            if (competition == null) {
                return R.error("比赛不存在");
            }

            String reason = CompetitionConverter.getCannotRegisterReason(competition);
            if (reason != null) {
                return R.error(reason);
            }

            Long currentTenantId = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L;
            boolean crossTenantRequest = !Objects.equals(competition.getTenantId(), currentTenantId);
            if (crossTenantRequest && !Boolean.TRUE.equals(competition.getIsCrossTenant())) {
                return R.error("该比赛不允许跨租户报名");
            }

            String validationError = validateRegisterDTO(registerDTO);
            if (validationError != null) {
                return R.error(validationError);
            }

            CompetitionParticipant existing = participantMapper.findByCompetitionAndUser(id, userId);
            if (existing != null && !CompetitionParticipant.STATUS_CANCELLED.equals(existing.getStatus())) {
                return R.error("您已报名该比赛");
            }

            CompetitionParticipant participant = CompetitionRegistrationConverter.toEntity(registerDTO, id, userId, currentTenantId);
            participant.setRegistrationTime(LocalDateTime.now());
            participant.setStatus(CompetitionParticipant.STATUS_REGISTERED);
            participantMapper.insert(participant);
            competitionService.incrementRegistrationCount(id);

            CompetitionRegistrationVO vo = CompetitionRegistrationConverter.toVO(participant, competition);
            return R.ok(vo);
        } catch (Exception e) {
            log.error("比赛报名失败", e);
            return R.error("报名失败：" + e.getMessage());
        }
    }

    private String validateRegisterDTO(CompetitionRegisterDTO registerDTO) {
        if (registerDTO == null) {
            return "报名信息不能为空";
        }
        Set<ConstraintViolation<CompetitionRegisterDTO>> violations = validator.validate(registerDTO);
        if (violations == null || violations.isEmpty()) {
            return null;
        }
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining("|"));
    }

    @ApiOperation("取消报名")
    @PostMapping("/{id}/register/cancel")
    public R<String> cancelRegistration(@PathVariable Long id) {
        try {
            Long userId = UserContext.getUser();
            if (userId == null) {
                return R.error("请先登录");
            }

            CompetitionParticipant participant = participantMapper.findByCompetitionAndUser(id, userId);
            if (participant == null) {
                return R.error("未找到报名记录");
            }
            if (CompetitionParticipant.STATUS_CANCELLED.equals(participant.getStatus())) {
                return R.error("报名已取消");
            }

            participant.setStatus(CompetitionParticipant.STATUS_CANCELLED);
            participantMapper.updateById(participant);
            return R.ok("取消报名成功");
        } catch (Exception e) {
            log.error("取消报名失败", e);
            return R.error("取消报名失败：" + e.getMessage());
        }
    }

    @ApiOperation("管理员取消报名")
    @PostMapping("/{id}/registrations/{registrationId}/cancel")
    @PreAuthorize("hasAuthority('content:competition:edit')")
    public R<String> adminCancelRegistration(
            @PathVariable Long id,
            @PathVariable Long registrationId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            CompetitionParticipant participant = runWithoutTenantIsolation(() -> participantMapper.selectById(registrationId));
            if (participant == null || !Objects.equals(participant.getCompetitionId(), id)) {
                return R.error("报名记录不存在");
            }
            if (CompetitionParticipant.STATUS_CANCELLED.equals(participant.getStatus())) {
                return R.error("报名已取消");
            }

            participant.setStatus(CompetitionParticipant.STATUS_CANCELLED);
            participant.setRemark(body == null ? null : body.get("reason"));
            runWithoutTenantIsolation(() -> {
                participantMapper.updateById(participant);
                return null;
            });

            Competition competition = competitionService.getCompetitionById(id);
            if (competition != null && competition.getRegistrationCount() != null && competition.getRegistrationCount() > 0) {
                competition.setRegistrationCount(Math.max(0, competition.getRegistrationCount() - 1));
                competitionService.updateCompetition(competition);
            }
            return R.ok("取消报名成功");
        } catch (Exception e) {
            log.error("管理员取消比赛报名失败", e);
            return R.error("取消报名失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取比赛的报名列表")
    @GetMapping("/{id}/registrations")
    public R<List<CompetitionRegistrationVO>> getRegistrations(@PathVariable Long id) {
        try {
            List<CompetitionParticipant> registrations = runWithoutTenantIsolation(() -> participantMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CompetitionParticipant>()
                            .eq(CompetitionParticipant::getCompetitionId, id)
                            .ne(CompetitionParticipant::getStatus, CompetitionParticipant.STATUS_CANCELLED)
                            .orderByDesc(CompetitionParticipant::getRegistrationTime)
            ));
            return R.ok(CompetitionRegistrationConverter.toVOList(registrations));
        } catch (Exception e) {
            log.error("获取报名列表失败", e);
            return R.error("获取报名列表失败：" + e.getMessage());
        }
    }

    @ApiOperation("检查是否已报名")
    @GetMapping("/{id}/registered")
    public R<Map<String, Object>> checkRegistered(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = UserContext.getUser();
            if (userId == null) {
                result.put("registered", false);
                return R.ok(result);
            }
            CompetitionParticipant participant = participantMapper.findByCompetitionAndUser(id, userId);
            result.put("registered", participant != null && !CompetitionParticipant.STATUS_CANCELLED.equals(participant.getStatus()));
            return R.ok(result);
        } catch (Exception e) {
            log.error("检查报名状态失败", e);
            return R.error("检查报名状态失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取我的报名详情")
    @GetMapping("/{id}/register/my")
    public R<CompetitionRegistrationVO> getMyRegistration(@PathVariable Long id) {
        try {
            Long userId = UserContext.getUser();
            if (userId == null) {
                return R.error("请先登录");
            }

            CompetitionParticipant participant = participantMapper.findByCompetitionAndUser(id, userId);
            if (participant == null || CompetitionParticipant.STATUS_CANCELLED.equals(participant.getStatus())) {
                return R.error("未找到报名记录");
            }
            Competition competition = competitionService.getCompetitionById(id);
            return R.ok(CompetitionRegistrationConverter.toVO(participant, competition));
        } catch (Exception e) {
            log.error("获取报名详情失败", e);
            return R.error("获取报名详情失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取我的比赛列表")
    @GetMapping("/my")
    public R<List<Map<String, Object>>> getMyCompetitions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            Long userId = UserContext.getUser();
            if (userId == null) {
                return R.error("请先登录");
            }

            List<CompetitionParticipant> participants = participantMapper.findByUserId(userId);
            List<Long> competitionIds = participants.stream()
                    .filter(p -> !CompetitionParticipant.STATUS_CANCELLED.equals(p.getStatus()))
                    .map(CompetitionParticipant::getCompetitionId)
                    .collect(Collectors.toList());

            if (competitionIds.isEmpty()) {
                return R.ok(new ArrayList<>());
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Long competitionId : competitionIds) {
                Competition competition = competitionService.getCompetitionById(competitionId);
                if (competition == null) {
                    continue;
                }
                CompetitionParticipant participant = participants.stream()
                        .filter(p -> p.getCompetitionId().equals(competitionId))
                        .findFirst()
                        .orElse(null);
                Map<String, Object> item = new HashMap<>();
                item.put("id", competition.getId());
                item.put("competitionName", competition.getTitle());
                item.put("registrationDeadline", competition.getRegistrationEnd());
                item.put("competitionType", competition.getCompetitionType());
                item.put("cover", competition.getCoverImage());
                item.put("status", competition.getStatus());
                item.put("registrationStatus", participant != null ? participant.getStatus() : null);
                item.put("registrationTime", participant != null ? participant.getRegistrationTime() : null);
                result.add(item);
            }
            return R.ok(result);
        } catch (Exception e) {
            log.error("获取我的比赛列表失败", e);
            return R.error("获取我的比赛列表失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取我的比赛报名记录")
    @GetMapping("/my/registrations")
    public R<List<CompetitionRegistrationVO>> getMyCompetitionRegistrations() {
        try {
            Long userId = UserContext.getUser();
            if (userId == null) {
                return R.error("请先登录");
            }

            List<CompetitionParticipant> participants = participantMapper.findByUserId(userId);
            List<CompetitionRegistrationVO> result = new ArrayList<>();
            for (CompetitionParticipant participant : participants) {
                Competition competition = competitionService.getCompetitionById(participant.getCompetitionId());
                if (competition != null) {
                    result.add(CompetitionRegistrationConverter.toVO(participant, competition));
                }
            }
            return R.ok(result);
        } catch (Exception e) {
            log.error("获取我的比赛报名记录失败", e);
            return R.error("获取报名记录失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取比赛成绩列表")
    @GetMapping("/{id}/results")
    @PreAuthorize("hasAuthority('content:competition:edit')")
    public R<List<CompetitionParticipant>> getResults(@PathVariable Long id) {
        try {
            List<CompetitionParticipant> participants = runWithoutTenantIsolation(() -> participantMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CompetitionParticipant>()
                            .eq(CompetitionParticipant::getCompetitionId, id)
                            .ne(CompetitionParticipant::getStatus, CompetitionParticipant.STATUS_CANCELLED)
                            .orderByAsc(CompetitionParticipant::getRanking)
                            .orderByDesc(CompetitionParticipant::getScore)
                            .orderByAsc(CompetitionParticipant::getRegistrationTime)
            ));
            return R.ok(participants);
        } catch (Exception e) {
            log.error("获取比赛成绩列表失败", e);
            return R.error("获取比赛成绩列表失败：" + e.getMessage());
        }
    }

    @ApiOperation("保存比赛成绩")
    @PostMapping("/{id}/results")
    @PreAuthorize("hasAuthority('content:competition:edit')")
    public R<String> saveResults(@PathVariable Long id, @RequestBody List<CompetitionResultSaveDTO> results) {
        try {
            if (results == null || results.isEmpty()) {
                return R.error("成绩数据不能为空");
            }
            for (CompetitionResultSaveDTO item : results) {
                CompetitionParticipant participant = participantMapper.selectById(item.getParticipantId());
                if (participant == null || !Objects.equals(participant.getCompetitionId(), id)) {
                    return R.error("存在无效的报名记录");
                }
                participant.setScore(item.getScore());
                participant.setAward(item.getAward());
                participant.setRanking(item.getRanking());
                participantMapper.updateById(participant);
            }
            return R.ok("保存成绩成功");
        } catch (Exception e) {
            log.error("保存比赛成绩失败", e);
            return R.error("保存比赛成绩失败：" + e.getMessage());
        }
    }

    @ApiOperation("发放比赛学分")
    @PostMapping("/{id}/results/credits/grant")
    @PreAuthorize("hasAuthority('content:competition:edit')")
    public R<String> grantCompetitionCredits(@PathVariable Long id) {
        try {
            Competition competition = competitionService.getCompetitionById(id);
            if (competition == null) {
                return R.error("比赛不存在");
            }
            if (!canGrantCompetitionCredits(competition)) {
                return R.error("比赛结束后才能发放学分");
            }

            List<CompetitionParticipant> participants = runWithoutTenantIsolation(
                    () -> participantMapper.findCreditCandidatesByCompetitionId(id));
            List<CreditGrantDTO> grants = participants.stream()
                    .filter(item -> item.getUserId() != null)
                    .map(item -> toCompetitionCreditGrant(competition, item))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (grants.isEmpty()) {
                return R.error("当前比赛没有可发放学分的参赛人员");
            }

            Integer processed = userClient.grantCredits(grants);
            return R.ok("已为 " + (processed == null ? 0 : processed) + " 名参赛人员发放比赛学分");
        } catch (Exception e) {
            log.error("发放比赛学分失败", e);
            return R.error("发放比赛学分失败: " + e.getMessage());
        }
    }

    private boolean canGrantCompetitionCredits(Competition competition) {
        if (competition == null) {
            return false;
        }
        if ("COMPLETED".equalsIgnoreCase(competition.getStatus())) {
            return true;
        }
        return competition.getCompetitionEnd() != null
                && !competition.getCompetitionEnd().isAfter(LocalDateTime.now());
    }

    private CreditGrantDTO toCompetitionCreditGrant(Competition competition, CompetitionParticipant participant) {
        Integer score = resolveCompetitionPoint(competition, participant);
        if (score == null || score <= 0) {
            return null;
        }
        CreditGrantDTO dto = new CreditGrantDTO();
        dto.setUserId(participant.getUserId());
        dto.setTenantId(competition.getTenantId());
        dto.setSourceType("competition");
        dto.setSourceId(competition.getId());
        dto.setCreditTypeCode("competition");
        dto.setCreditHours(BigDecimal.ZERO);
        dto.setCreditScore(BigDecimal.valueOf(score));
        dto.setDescription("比赛《" + competition.getTitle() + "》" + resolveAwardLabel(participant.getAward()) + "学分发放");
        return dto;
    }

    private Integer resolveCompetitionPoint(Competition competition, CompetitionParticipant participant) {
        Integer award = participant.getAward();
        Integer participationPoints = competition.getParticipationPoints();
        if (award == null) {
            return participationPoints;
        }
        switch (award) {
            case 1:
                return firstPositive(competition.getFirstPrizePoints(), participationPoints);
            case 2:
                return firstPositive(competition.getSecondPrizePoints(), participationPoints);
            case 3:
                return firstPositive(competition.getThirdPrizePoints(), participationPoints);
            case 4:
                return firstPositive(competition.getExcellentPrizePoints(), participationPoints);
            default:
                return participationPoints;
        }
    }

    private Integer firstPositive(Integer primary, Integer fallback) {
        if (primary != null && primary > 0) {
            return primary;
        }
        return fallback;
    }

    private String resolveAwardLabel(Integer award) {
        if (award == null) {
            return "参与奖";
        }
        switch (award) {
            case 1:
                return "一等奖";
            case 2:
                return "二等奖";
            case 3:
                return "三等奖";
            case 4:
                return "优秀奖";
            default:
                return "参与奖";
        }
    }

    private <T> T runWithoutTenantIsolation(Supplier<T> action) {
        Long previousTenantId = TenantContext.getTenantId();
        Boolean previousSuperAdmin = TenantContext.isSuperAdmin();
        try {
            TenantContext.setSuperAdmin(true);
            TenantContext.setTenantId(previousTenantId != null ? previousTenantId : 1L);
            return action.get();
        } finally {
            if (previousTenantId == null) {
                TenantContext.removeTenantId();
            } else {
                TenantContext.setTenantId(previousTenantId);
            }
            TenantContext.setSuperAdmin(previousSuperAdmin);
        }
    }
}
