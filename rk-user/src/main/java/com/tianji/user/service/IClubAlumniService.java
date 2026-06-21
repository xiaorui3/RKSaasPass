package com.tianji.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.user.domain.po.ClubAlumni;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 校友服务接口
 * 基于master分支的核心业务逻辑
 */
public interface IClubAlumniService extends IService<ClubAlumni> {

    // 基础CRUD操作
    boolean addAlumni(ClubAlumni alumni);
    boolean updateAlumni(ClubAlumni alumni);
    boolean deleteAlumni(Long id);
    boolean restoreAlumni(Long id);
    ClubAlumni getAlumniById(Long id);
    List<ClubAlumni> getAllAlumni();
    List<ClubAlumni> getAllAlumni(Long tenantId);
    List<ClubAlumni> getDeletedAlumni();

    // 查询方法
    List<ClubAlumni> getAlumniByGenerationYear(Integer year);
    List<ClubAlumni> getAlumniByGraduationStatus(String graduationStatus);
    List<ClubAlumni> getAlumniByDepartment(String department);
    List<ClubAlumni> searchAlumni(String keyword);

    // 统计方法
    Map<String, Object> getGraduationStatistics();
    List<Object[]> getGenerationStatistics();
    Map<String, Object> getGraduationDistribution();

    // 批量操作
    int batchUpdateGraduationStatus();
    String importFromExcel(MultipartFile file);

    // 毕业状态计算
    Map<String, Object> calculateGraduationStatus(Integer enrollmentYear, Integer generationYear, String gradeClass);

    // 分组查询（用于前端历史页面）
    Map<Integer, List<ClubAlumni>> getShowAlumniGroupedByGeneration();
    Map<String, Object> getShowAlumniOverview();

    List<GlobalSearchDocumentDTO> exportSearchDocuments();
}
