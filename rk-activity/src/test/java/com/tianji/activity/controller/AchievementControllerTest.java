package com.tianji.activity.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.activity.domain.po.MemberAchievement;
import com.tianji.activity.mapper.MemberAchievementMapper;
import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchievementControllerTest {

    @Mock
    private MemberAchievementMapper memberAchievementMapper;

    @InjectMocks
    private AchievementController controller;

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
        TenantContext.clear();
    }

    @Test
    void listAchievements_shouldReturnPagedRecords() {
        MemberAchievement achievement = new MemberAchievement();
        achievement.setId(1L);
        achievement.setAchievementTitle("National Programming Prize");

        Page<MemberAchievement> page = new Page<>(1, 10);
        page.setRecords(List.of(achievement));
        page.setTotal(1);
        when(memberAchievementMapper.selectPage(any(Page.class), any())).thenReturn(page);

        R<Page<MemberAchievement>> response = controller.listAchievements(1, 10, null, null, null, null);

        assertEquals(200, response.getCode());
        assertSame(page, response.getData());
    }

    @Test
    void addAchievement_shouldRejectMissingReviewers() {
        MemberAchievement achievement = new MemberAchievement();
        achievement.setAchievementTitle("Prize");

        R<String> response = controller.addAchievement(achievement);

        assertTrue(response.failed());
        verify(memberAchievementMapper, never()).insert(any(MemberAchievement.class));
    }

    @Test
    void addAchievement_shouldInitializePendingDualReview() {
        UserContext.setUser(12L);
        TenantContext.setTenantId(3L);
        MemberAchievement achievement = new MemberAchievement();
        achievement.setAchievementTitle("Prize");
        achievement.setManagerReviewerId(21L);
        achievement.setTeacherReviewerId(31L);

        R<String> response = controller.addAchievement(achievement);

        assertEquals(200, response.getCode());
        ArgumentCaptor<MemberAchievement> captor = ArgumentCaptor.forClass(MemberAchievement.class);
        verify(memberAchievementMapper).insert(captor.capture());
        MemberAchievement inserted = captor.getValue();
        assertEquals(1, inserted.getStatus());
        assertEquals(MemberAchievement.REVIEW_PENDING, inserted.getManagerReviewStatus());
        assertEquals(MemberAchievement.REVIEW_PENDING, inserted.getTeacherReviewStatus());
        assertEquals(12L, inserted.getUserId());
        assertEquals(3L, inserted.getTenantId());
    }

    @Test
    void reviewAchievementByManager_shouldRejectNonDesignatedReviewer() {
        UserContext.setUser(99L);
        MemberAchievement achievement = pendingAchievement();
        when(memberAchievementMapper.selectById(5L)).thenReturn(achievement);

        R<String> response = controller.reviewAchievementByManager(5L, "ok", 7L);

        assertTrue(response.failed());
        verify(memberAchievementMapper, never()).updateById(any(MemberAchievement.class));
    }

    @Test
    void reviewAchievementByTeacher_shouldRequireManagerApprovalFirst() {
        UserContext.setUser(31L);
        MemberAchievement achievement = pendingAchievement();
        when(memberAchievementMapper.selectById(5L)).thenReturn(achievement);

        R<String> response = controller.reviewAchievementByTeacher(5L, "ok", 8L);

        assertTrue(response.failed());
        verify(memberAchievementMapper, never()).updateById(any(MemberAchievement.class));
    }

    @Test
    void reviewAchievementByTeacher_shouldApproveAfterManagerApproval() {
        UserContext.setUser(31L);
        MemberAchievement achievement = pendingAchievement();
        achievement.setManagerReviewStatus(MemberAchievement.REVIEW_APPROVED);
        when(memberAchievementMapper.selectById(5L)).thenReturn(achievement);

        R<String> response = controller.reviewAchievementByTeacher(5L, "ok", 8L);

        assertEquals(200, response.getCode());
        ArgumentCaptor<MemberAchievement> captor = ArgumentCaptor.forClass(MemberAchievement.class);
        verify(memberAchievementMapper).updateById(captor.capture());
        MemberAchievement updated = captor.getValue();
        assertEquals(MemberAchievement.REVIEW_APPROVED, updated.getTeacherReviewStatus());
        assertEquals(2, updated.getStatus());
        assertEquals(31L, updated.getAuditUserId());
    }

    private MemberAchievement pendingAchievement() {
        MemberAchievement achievement = new MemberAchievement();
        achievement.setId(5L);
        achievement.setStatus(1);
        achievement.setManagerReviewerId(21L);
        achievement.setTeacherReviewerId(31L);
        achievement.setManagerReviewStatus(MemberAchievement.REVIEW_PENDING);
        achievement.setTeacherReviewStatus(MemberAchievement.REVIEW_PENDING);
        return achievement;
    }
}
