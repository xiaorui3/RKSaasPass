package com.tianji.user.service.impl;

import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.ClubAlumni;
import com.tianji.user.mapper.ClubAlumniMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubAlumniServiceImplTest {

    private static final String STATUS_CURRENT = "\u5728\u6821";
    private static final String STATUS_GRADUATED = "\u5df2\u6bd5\u4e1a";

    @Mock
    private ClubAlumniMapper clubAlumniMapper;

    @Spy
    @InjectMocks
    private ClubAlumniServiceImpl clubAlumniService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(clubAlumniService, "baseMapper", clubAlumniMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.removeTenantId();
        TenantContext.setSuperAdmin(null);
    }

    @Test
    void addAlumni_shouldFillTenantIdFromTenantContextWhenMissing() {
        TenantContext.setTenantId(1L);
        when(clubAlumniMapper.insert(any(ClubAlumni.class))).thenReturn(1);

        ClubAlumni alumni = new ClubAlumni();
        alumni.setName("imported alumni");
        alumni.setGenerationYear(2020);
        alumni.setMajor("software");

        boolean saved = clubAlumniService.addAlumni(alumni);

        assertTrue(saved);
        ArgumentCaptor<ClubAlumni> captor = ArgumentCaptor.forClass(ClubAlumni.class);
        verify(clubAlumniMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getTenantId());
    }

    @Test
    void updateAlumni_shouldKeepExistingTenantIdWhenRequestDoesNotProvideOne() {
        LocalDateTime createTime = LocalDateTime.of(2026, 4, 11, 10, 0);
        ClubAlumni existing = new ClubAlumni();
        existing.setId(11L);
        existing.setTenantId(2L);
        existing.setCreateTime(createTime);
        when(clubAlumniMapper.selectById(11L)).thenReturn(existing);
        when(clubAlumniMapper.updateById(any(ClubAlumni.class))).thenReturn(1);

        ClubAlumni update = new ClubAlumni();
        update.setId(11L);
        update.setName("updated alumni");

        boolean updated = clubAlumniService.updateAlumni(update);

        assertTrue(updated);
        ArgumentCaptor<ClubAlumni> captor = ArgumentCaptor.forClass(ClubAlumni.class);
        verify(clubAlumniMapper).updateById(captor.capture());
        assertEquals(2L, captor.getValue().getTenantId());
        assertEquals(createTime, captor.getValue().getCreateTime());
    }

    @Test
    void getShowAlumniOverview_shouldComputeGraduatedCountsFromDerivedStatus() {
        ClubAlumni graduated = new ClubAlumni();
        graduated.setId(1L);
        graduated.setTenantId(2L);
        graduated.setName("graduated");
        graduated.setGenerationYear(LocalDate.now().getYear() - 5);
        graduated.setMemberStatus(STATUS_GRADUATED);

        ClubAlumni current = new ClubAlumni();
        current.setId(2L);
        current.setTenantId(2L);
        current.setName("current");
        current.setGenerationYear(LocalDate.now().getYear() - 1);
        current.setMemberStatus(STATUS_CURRENT);

        doReturn(List.of(graduated, current)).when(clubAlumniService).listVisibleRosterRecords();

        java.util.Map<String, Object> overview = clubAlumniService.getShowAlumniOverview();

        assertEquals(2, overview.get("totalCount"));
        assertEquals(2, overview.get("rosterCount"));
        assertEquals(1L, overview.get("alumniCount"));
        assertEquals(1L, overview.get("graduatedCount"));
        assertEquals(1L, overview.get("currentCount"));
        assertEquals(1, overview.get("generationCount"));
        assertFalse(((java.util.Map<?, ?>) overview.get("groupedData")).isEmpty());
    }

    @Test
    void getShowAlumniOverview_shouldTreatPastGraduationDateAsGraduatedEvenWhenLegacyStatusSaysCurrent() {
        ClubAlumni legacyGraduated = new ClubAlumni();
        legacyGraduated.setId(3L);
        legacyGraduated.setTenantId(1L);
        legacyGraduated.setName("legacy graduated");
        legacyGraduated.setGenerationYear(LocalDate.now().getYear() - 2);
        legacyGraduated.setExpectedGraduationYear(LocalDate.now().getYear() - 1);
        legacyGraduated.setActualGraduationDate(LocalDate.now().minusYears(1));
        legacyGraduated.setMemberStatus(STATUS_CURRENT);

        doReturn(List.of(legacyGraduated)).when(clubAlumniService).listVisibleRosterRecords();

        java.util.Map<String, Object> overview = clubAlumniService.getShowAlumniOverview();

        assertEquals(1, overview.get("totalCount"));
        assertEquals(1, overview.get("rosterCount"));
        assertEquals(1L, overview.get("alumniCount"));
        assertEquals(1L, overview.get("graduatedCount"));
        assertEquals(0L, overview.get("currentCount"));
    }

    @Test
    void getShowAlumniGroupedByGeneration_shouldExposeGraduationStatusAlias() {
        ClubAlumni graduated = new ClubAlumni();
        graduated.setId(1L);
        graduated.setTenantId(2L);
        graduated.setGenerationYear(LocalDate.now().getYear() - 6);
        graduated.setMemberStatus(STATUS_GRADUATED);

        ClubAlumni current = new ClubAlumni();
        current.setId(2L);
        current.setTenantId(2L);
        current.setGenerationYear(LocalDate.now().getYear() - 1);
        current.setMemberStatus(STATUS_CURRENT);

        doReturn(List.of(graduated)).when(clubAlumniService).listVisibleGraduatedAlumni();

        java.util.Map<Integer, List<ClubAlumni>> result = clubAlumniService.getShowAlumniGroupedByGeneration();

        assertEquals(1, result.size());
        ClubAlumni first = result.values().iterator().next().get(0);
        assertEquals(STATUS_GRADUATED, first.getMemberStatus());
        assertEquals(STATUS_GRADUATED, first.getGraduationStatus());
    }

    @Test
    void calculateGraduationStatus_shouldReturnReadableChineseDescriptions() {
        java.util.Map<String, Object> graduated = clubAlumniService.calculateGraduationStatus(
                LocalDate.now().getYear() - 5,
                null,
                null
        );
        java.util.Map<String, Object> current = clubAlumniService.calculateGraduationStatus(
                LocalDate.now().getYear() - 1,
                null,
                null
        );

        assertEquals(STATUS_GRADUATED, graduated.get("status"));
        assertEquals("已毕业", graduated.get("description"));
        assertEquals(STATUS_CURRENT, current.get("status"));
        assertEquals("当前在校", current.get("description"));
    }

    @Test
    void getShowAlumniOverview_shouldExcludeInactiveRosterRecords() {
        ClubAlumni inactiveGraduated = new ClubAlumni();
        inactiveGraduated.setId(9L);
        inactiveGraduated.setTenantId(1L);
        inactiveGraduated.setGenerationYear(LocalDate.now().getYear() - 6);
        inactiveGraduated.setMemberStatus(STATUS_GRADUATED);
        inactiveGraduated.setIsActive(false);

        doReturn(List.of(inactiveGraduated)).when(clubAlumniService).listScopedRoster();

        java.util.Map<String, Object> overview = clubAlumniService.getShowAlumniOverview();

        assertEquals(0, overview.get("totalCount"));
        assertEquals(0, overview.get("rosterCount"));
        assertEquals(0L, overview.get("alumniCount"));
        assertEquals(0L, overview.get("graduatedCount"));
    }

    @Test
    void getDeletedAlumni_shouldReturnTenantScopedDeletedRows() {
        TenantContext.setTenantId(2L);
        ClubAlumni deleted = new ClubAlumni();
        deleted.setId(31L);
        deleted.setTenantId(2L);
        deleted.setIsDeleted(1);
        when(clubAlumniMapper.selectDeletedByTenantIds(List.of(2L))).thenReturn(List.of(deleted));

        List<ClubAlumni> result = clubAlumniService.getDeletedAlumni();

        assertEquals(List.of(deleted), result);
    }

    @Test
    void restoreAlumni_shouldClearDeletedFlagUsingIncludingDeletedUpdate() {
        TenantContext.setTenantId(2L);
        ClubAlumni deleted = new ClubAlumni();
        deleted.setId(32L);
        deleted.setTenantId(2L);
        deleted.setName("restore alumni");
        deleted.setGenerationYear(2020);
        deleted.setMajor("software");
        deleted.setIsDeleted(1);
        when(clubAlumniMapper.selectAnyById(32L)).thenReturn(deleted);
        when(clubAlumniMapper.updateIncludingDeleted(any(ClubAlumni.class))).thenReturn(1);

        boolean restored = clubAlumniService.restoreAlumni(32L);

        assertTrue(restored);
        ArgumentCaptor<ClubAlumni> captor = ArgumentCaptor.forClass(ClubAlumni.class);
        verify(clubAlumniMapper).updateIncludingDeleted(captor.capture());
        assertEquals(0, captor.getValue().getIsDeleted());
    }

}
