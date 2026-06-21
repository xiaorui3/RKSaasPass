package com.tianji.user.controller;

import com.tianji.common.domain.R;
import com.tianji.user.domain.po.ClubAlumni;
import com.tianji.user.service.IClubAlumniService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 校友管理控制器
 * 基于master分支的核心业务逻辑，完整保留原接口功能
 *
 * RBAC权限控制:
 * - super_admin: 可管理校友
 * - manager: 可管理校友
 * - teacher: 可查看校友
 * - member: 可查看校友（部分公开接口）
 */
@Slf4j
@Api(tags = "校友管理接口")
@RestController
@RequestMapping("/api/alumni")
@RequiredArgsConstructor
public class ClubAlumniController {

    private final IClubAlumniService clubAlumniService;

    /**
     * 获取所有校友信息
     * 权限: super_admin, manager 可查看
     */
    @ApiOperation("获取所有校友信息")
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('alumni:list', 'alumni:view')")
    public R<List<ClubAlumni>> getAllAlumni(@RequestParam(required = false) Long tenantId) {
        try {
            List<ClubAlumni> alumniList = tenantId == null
                    ? clubAlumniService.getAllAlumni()
                    : clubAlumniService.getAllAlumni(tenantId);
            return R.ok(alumniList);
        } catch (Exception e) {
            return R.error("获取校友列表失败");
        }
    }

    @ApiOperation("获取已删除校友列表")
    @GetMapping("/deleted")
    @PreAuthorize("hasAnyAuthority('alumni:list', 'alumni:view')")
    public R<List<ClubAlumni>> getDeletedAlumni() {
        try {
            return R.ok(clubAlumniService.getDeletedAlumni());
        } catch (Exception e) {
            log.error("获取已删除校友列表失败", e);
            return R.error("获取已删除校友列表失败");
        }
    }

    @ApiOperation("恢复已删除校友")
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('alumni:edit')")
    public R<String> restoreAlumni(@PathVariable Long id) {
        try {
            boolean success = clubAlumniService.restoreAlumni(id);
            return success ? R.ok("恢复成功") : R.error("恢复失败");
        } catch (Exception e) {
            log.error("恢复已删除校友失败，ID: {}", id, e);
            return R.error("恢复失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取校友信息
     * 权限: super_admin, manager 可查看
     */
    @ApiOperation("根据ID获取校友信息")
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyAuthority('alumni:list', 'alumni:view')")
    public R<ClubAlumni> getAlumniById(@PathVariable Long id) {
        try {
            ClubAlumni alumni = clubAlumniService.getAlumniById(id);
            if (alumni != null) {
                return R.ok(alumni);
            } else {
                return R.error("校友不存在");
            }
        } catch (Exception e) {
            return R.error("获取校友信息失败");
        }
    }

    /**
     * 根据届数获取校友信息
     * 权限: super_admin, manager 可查看
     */
    @ApiOperation("根据届数获取校友信息")
    @GetMapping("/generation/{year}")
    @PreAuthorize("hasAnyAuthority('alumni:list', 'alumni:view')")
    public R<List<ClubAlumni>> getAlumniByGenerationYear(@PathVariable Integer year) {
        try {
            List<ClubAlumni> alumniList = clubAlumniService.getAlumniByGenerationYear(year);
            return R.ok(alumniList);
        } catch (Exception e) {
            return R.error("获取该届校友信息失败");
        }
    }

    /**
     * 根据毕业状态获取校友信息
     * 权限: super_admin, manager 可查看
     */
    @ApiOperation("根据毕业状态获取校友信息")
    @GetMapping("/status/{graduationStatus}")
    @PreAuthorize("hasAnyAuthority('alumni:list', 'alumni:view')")
    public R<List<ClubAlumni>> getAlumniByGraduationStatus(@PathVariable String graduationStatus) {
        try {
            List<ClubAlumni> alumniList = clubAlumniService.getAlumniByGraduationStatus(graduationStatus);
            return R.ok(alumniList);
        } catch (Exception e) {
            return R.error("根据毕业状态获取校友信息失败");
        }
    }

    /**
     * 根据部门获取校友信息
     * 权限: super_admin, manager 可查看
     */
    @ApiOperation("根据部门获取校友信息")
    @GetMapping("/department/{department}")
    @PreAuthorize("hasAnyAuthority('alumni:list', 'alumni:view')")
    public R<List<ClubAlumni>> getAlumniByDepartment(@PathVariable String department) {
        try {
            List<ClubAlumni> alumniList = clubAlumniService.getAlumniByDepartment(department);
            return R.ok(alumniList);
        } catch (Exception e) {
            return R.error("根据部门获取校友信息失败");
        }
    }

    /**
     * 添加校友信息
     * 权限: super_admin, manager 可添加
     */
    @ApiOperation("添加校友信息")
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('alumni:add')")
    public R<String> addAlumni(@RequestBody ClubAlumni alumni) {
        try {
            boolean success = clubAlumniService.addAlumni(alumni);
            if (success) {
                log.info("添加校友信息成功");
                return R.ok("添加校友信息成功");
            } else {
                return R.error("添加校友信息失败");
            }
        } catch (Exception e) {
            log.error("添加校友信息失败", e);
            return R.error("添加校友信息失败：" + e.getMessage());
        }
    }

    /**
     * 更新校友信息
     * 权限: super_admin, manager 可编辑
     */
    @ApiOperation("更新校友信息")
    @PutMapping("/update")
    @PreAuthorize("hasAuthority('alumni:edit')")
    public R<String> updateAlumni(@RequestBody ClubAlumni alumni) {
        try {
            boolean success = clubAlumniService.updateAlumni(alumni);
            if (success) {
                log.info("更新校友信息成功");
                return R.ok("更新校友信息成功");
            } else {
                return R.error("更新校友信息失败");
            }
        } catch (Exception e) {
            log.error("更新校友信息失败", e);
            return R.error("更新校友信息失败：" + e.getMessage());
        }
    }

    /**
     * 删除校友信息
     * 权限: super_admin, manager 可删除
     */
    @ApiOperation("删除校友信息")
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('alumni:remove')")
    public R<String> deleteAlumni(@PathVariable Long id) {
        try {
            boolean success = clubAlumniService.deleteAlumni(id);
            if (success) {
                log.info("删除校友信息成功，ID: {}", id);
                return R.ok("删除校友信息成功");
            } else {
                return R.error("删除校友信息失败");
            }
        } catch (Exception e) {
            log.error("删除校友信息失败，ID: {}", id, e);
            return R.error("删除校友信息失败：" + e.getMessage());
        }
    }

    /**
     * 批量更新毕业状态
     * 权限: super_admin, manager 可操作
     */
    @ApiOperation("批量更新毕业状态")
    @PostMapping("/update-graduation-status")
    @PreAuthorize("hasAuthority('alumni:edit')")
    public R<String> batchUpdateGraduationStatus() {
        try {
            int updateCount = clubAlumniService.batchUpdateGraduationStatus();
            return R.ok(String.format("成功更新%d条校友的毕业状态", updateCount));
        } catch (Exception e) {
            return R.error("批量更新毕业状态失败：" + e.getMessage());
        }
    }

    /**
     * 获取毕业状态统计
     * 权限: super_admin, manager 可查看
     */
    @ApiOperation("获取毕业状态统计")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyAuthority('alumni:list', 'alumni:view')")
    public R<Map<String, Object>> getGraduationStatistics() {
        try {
            Map<String, Object> statistics = clubAlumniService.getGraduationStatistics();
            return R.ok(statistics);
        } catch (Exception e) {
            return R.error("获取毕业状态统计失败");
        }
    }

    /**
     * 获取各届统计信息
     * 权限: super_admin, manager 可查看
     */
    @ApiOperation("获取各届统计信息")
    @GetMapping("/generation-statistics")
    @PreAuthorize("hasAnyAuthority('alumni:list', 'alumni:view')")
    public R<List<Object[]>> getGenerationStatistics() {
        try {
            List<Object[]> statistics = clubAlumniService.getGenerationStatistics();
            return R.ok(statistics);
        } catch (Exception e) {
            return R.error("获取各届统计信息失败");
        }
    }

    /**
     * 搜索校友
     * 权限: super_admin, manager 可查看
     */
    @ApiOperation("搜索校友")
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('alumni:list', 'alumni:view')")
    public R<List<ClubAlumni>> searchAlumni(@RequestParam String keyword) {
        try {
            List<ClubAlumni> alumniList = clubAlumniService.searchAlumni(keyword);
            return R.ok(alumniList);
        } catch (Exception e) {
            return R.error("搜索校友失败");
        }
    }

    /**
     * 计算毕业状态API
     * 权限: super_admin, manager 可操作
     */
    @ApiOperation("计算毕业状态")
    @PostMapping("/calculate-graduation-status")
    @PreAuthorize("hasAnyAuthority('alumni:add', 'alumni:edit')")
    public R<Map<String, Object>> calculateGraduationStatus(
            @RequestParam(required = false) Integer enrollmentYear,
            @RequestParam(required = false) Integer generationYear,
            @RequestParam(required = false) String gradeClass) {

        try {
            Map<String, Object> response = clubAlumniService.calculateGraduationStatus(
                    enrollmentYear, generationYear, gradeClass);
            return R.ok(response);
        } catch (Exception e) {
            return R.error("计算毕业状态失败：" + e.getMessage());
        }
    }

    /**
     * 获取毕业状态分布
     * 权限: super_admin, manager 可查看
     */
    @ApiOperation("获取毕业状态分布")
    @GetMapping("/graduation-distribution")
    @PreAuthorize("hasAnyAuthority('alumni:list', 'alumni:view')")
    public R<Map<String, Object>> getGraduationDistribution() {
        try {
            Map<String, Object> distribution = clubAlumniService.getGraduationDistribution();
            return R.ok(distribution);
        } catch (Exception e) {
            return R.error("获取毕业状态分布失败");
        }
    }

    /**
     * 获取按届数分组的校友数据（后台）
     * 权限: super_admin, manager 可查看
     */
    @ApiOperation("获取按届数分组的校友数据")
    @GetMapping("/grouped-by-generation")
    @PreAuthorize("hasAnyAuthority('alumni:list', 'alumni:view')")
    public R<Map<Integer, List<ClubAlumni>>> getAlumniGroupedByGeneration() {
        try {
            Map<Integer, List<ClubAlumni>> groupedData = clubAlumniService.getShowAlumniGroupedByGeneration();
            return R.ok(groupedData);
        } catch (Exception e) {
            return R.error("获取按届数分组的校友数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取校友数据概览（后台）
     * 权限: super_admin, manager 可查看
     */
    @ApiOperation("获取校友数据概览")
    @GetMapping("/overview")
    @PreAuthorize("hasAnyAuthority('alumni:list', 'alumni:view')")
    public R<Map<String, Object>> getAlumniOverview() {
        try {
            Map<String, Object> overview = clubAlumniService.getShowAlumniOverview();
            return R.ok(overview);
        } catch (Exception e) {
            return R.error("获取校友数据概览失败：" + e.getMessage());
        }
    }

    @ApiOperation("公开获取按届数分组的校友风采数据")
    @GetMapping("/show-grouped-by-generation")
    public R<Map<Integer, List<ClubAlumni>>> getPublicAlumniGroupedByGeneration() {
        try {
            Map<Integer, List<ClubAlumni>> groupedData = clubAlumniService.getShowAlumniGroupedByGeneration();
            return R.ok(groupedData);
        } catch (Exception e) {
            return R.error("获取校友风采数据失败：" + e.getMessage());
        }
    }

    @ApiOperation("公开获取校友风采概览")
    @GetMapping("/show-overview")
    public R<Map<String, Object>> getPublicAlumniOverview() {
        try {
            Map<String, Object> overview = clubAlumniService.getShowAlumniOverview();
            return R.ok(overview);
        } catch (Exception e) {
            return R.error("获取校友风采概览失败：" + e.getMessage());
        }
    }

    /**
     * Excel导入校友信息
     * 权限: super_admin, manager 可操作
     */
    @ApiOperation("Excel导入校友信息")
    @PostMapping("/import-excel")
    @PreAuthorize("hasAuthority('alumni:add')")
    public R<String> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            log.info("开始处理Excel导入，文件名: {}", file.getOriginalFilename());

            // 验证文件
            if (file.isEmpty()) {
                return R.error("文件不能为空");
            }

            String filename = file.getOriginalFilename();
            if (filename == null ||
                (!filename.endsWith(".xlsx") && !filename.endsWith(".xls") && !filename.endsWith(".csv"))) {
                return R.error("请上传Excel文件(.xlsx, .xls)或CSV文件(.csv)");
            }

            // 处理Excel导入
            String result = clubAlumniService.importFromExcel(file);

            log.info("Excel导入完成");

            return R.ok(result);

        } catch (Exception e) {
            log.error("Excel导入失败", e);
            return R.error("Excel导入失败：" + e.getMessage());
        }
    }
}
