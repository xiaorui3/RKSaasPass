package com.tianji.activity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.activity.domain.dto.CompetitionQueryDTO;
import com.tianji.activity.domain.po.Competition;

import java.util.List;

public interface ICompetitionService extends IService<Competition> {

    IPage<Competition> pageCompetitions(Page<Competition> page, CompetitionQueryDTO queryDTO);

    List<Competition> getAllCompetitions();

    List<Competition> getPublishedCompetitions(String sortBy, String sortOrder);

    List<Competition> getFeaturedCompetitions();

    List<Competition> getSharedCompetitions(Integer limit);

    List<Competition> getCompetitionsByStatus(String status);

    Competition getCompetitionById(Long id);

    Page<Competition> getPendingReviewPage(Integer page, Integer size, String title);

    Page<Competition> getPendingReviewPage(Integer page, Integer size, String title, Long roleId);

    Long createCompetition(Competition competition);

    int updateCompetition(Competition competition);

    int deleteCompetition(Long id);

    boolean reviewCompetitionByManager(Long id, boolean approved, String reviewComment, Long reviewerId);

    boolean reviewCompetitionByManager(Long id, boolean approved, String reviewComment, Long reviewerId, boolean allowProxyReview);

    boolean reviewCompetitionByTeacher(Long id, boolean approved, String reviewComment, Long reviewerId);

    boolean reviewCompetitionByTeacher(Long id, boolean approved, String reviewComment, Long reviewerId, boolean allowProxyReview);

    List<Competition> searchCompetitions(String keyword);

    int incrementViewCount(Long id);

    int incrementRegistrationCount(Long id);

    List<GlobalSearchDocumentDTO> exportSearchDocuments();
}
