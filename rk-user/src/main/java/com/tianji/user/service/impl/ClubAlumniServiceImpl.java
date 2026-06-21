package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.common.utils.SearchIndexSyncUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.ClubAlumni;
import com.tianji.user.domain.po.RKTenant;
import com.tianji.user.mapper.ClubAlumniMapper;
import com.tianji.user.mapper.RKTenantMapper;
import com.tianji.user.service.IClubAlumniService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubAlumniServiceImpl extends ServiceImpl<ClubAlumniMapper, ClubAlumni>
        implements IClubAlumniService {

    private final RKTenantMapper tenantMapper;
    private final SearchClient searchClient;
    private static final String SEARCH_ENTITY_TYPE_ALUMNI = "ALUMNI";

    private static final String STATUS_CURRENT = "在校";
    private static final String STATUS_GRADUATED = "已毕业";

    @Override
    @Transactional
    public boolean addAlumni(ClubAlumni alumni) {
        prepareAlumni(alumni, null);
        boolean result = save(alumni);
        if (result) {
            syncAlumniSearchIndex(alumni.getId());
        }
        return result;
    }

    @Override
    @Transactional
    public boolean updateAlumni(ClubAlumni alumni) {
        ClubAlumni existing = getById(alumni.getId());
        prepareAlumni(alumni, existing);
        boolean result = updateById(alumni);
        if (result) {
            syncAlumniSearchIndex(alumni.getId());
        }
        return result;
    }

    @Override
    @Transactional
    public boolean deleteAlumni(Long id) {
        ClubAlumni existing = baseMapper.selectById(id);
        Long tenantId = existing == null ? null : existing.getTenantId();
        boolean result = removeById(id);
        if (result) {
            deleteAlumniSearchIndex(id, tenantId);
        }
        return result;
    }

    @Override
    @Transactional
    public boolean restoreAlumni(Long id) {
        ClubAlumni alumni = baseMapper.selectAnyById(id);
        if (alumni == null || !canAccessTenant(alumni.getTenantId())) {
            return false;
        }
        alumni.setIsDeleted(0);
        alumni.setIsActive(true);
        if (alumni.getShowTable() == null) {
            alumni.setShowTable(true);
        }
        alumni.setUpdateTime(LocalDateTime.now());
        prepareAlumni(alumni, alumni);
        alumni.setIsDeleted(0);
        alumni.setIsActive(true);
        alumni.setUpdateTime(LocalDateTime.now());
        boolean result = baseMapper.updateIncludingDeleted(alumni) > 0;
        if (result) {
            syncAlumniSearchIndex(alumni.getId());
        }
        return result;
    }

    @Override
    public ClubAlumni getAlumniById(Long id) {
        return getById(id);
    }

    @Override
    public List<ClubAlumni> getAllAlumni() {
        return listVisibleGraduatedAlumni();
    }

    @Override
    public List<ClubAlumni> getAllAlumni(Long tenantId) {
        if (tenantId == null) {
            return getAllAlumni();
        }
        return runInTenantScope(tenantId, this::getAllAlumni);
    }

    @Override
    public List<ClubAlumni> getDeletedAlumni() {
        return enrichComputedFields(baseMapper.selectDeletedByTenantIds(resolveScopedTenantIds()));
    }

    @Override
    public List<ClubAlumni> getAlumniByGenerationYear(Integer year) {
        List<ClubAlumni> alumni = listVisibleGraduatedAlumni().stream()
                .filter(item -> Objects.equals(item.getGenerationYear(), year))
                .collect(Collectors.toList());
        alumni.sort((a, b) -> {
            boolean aCore = Boolean.TRUE.equals(a.getIsCoreMember());
            boolean bCore = Boolean.TRUE.equals(b.getIsCoreMember());
            if (aCore != bCore) {
                return aCore ? -1 : 1;
            }
            return String.valueOf(a.getName()).compareTo(String.valueOf(b.getName()));
        });
        return alumni;
    }

    @Override
    public List<ClubAlumni> getAlumniByGraduationStatus(String graduationStatus) {
        return getAllAlumni().stream()
                .filter(alumni -> matchesGraduationStatus(alumni.getMemberStatus(), graduationStatus))
                .collect(Collectors.toList());
    }

    @Override
    public List<ClubAlumni> getAlumniByDepartment(String department) {
        return listVisibleGraduatedAlumni().stream()
                .filter(alumni -> Objects.equals(alumni.getDepartment(), department))
                .collect(Collectors.toList());
    }

    @Override
    public List<ClubAlumni> searchAlumni(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (!StringUtils.hasText(normalizedKeyword)) {
            return listVisibleGraduatedAlumni();
        }
        return listVisibleGraduatedAlumni().stream()
                .filter(alumni -> containsIgnoreCase(alumni.getName(), normalizedKeyword)
                        || containsIgnoreCase(alumni.getStudentId(), normalizedKeyword)
                        || containsIgnoreCase(alumni.getMajor(), normalizedKeyword)
                        || containsIgnoreCase(alumni.getPosition(), normalizedKeyword)
                        || containsIgnoreCase(alumni.getDepartment(), normalizedKeyword)
                        || containsIgnoreCase(alumni.getWorkUnit(), normalizedKeyword))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getGraduationStatistics() {
        List<ClubAlumni> allAlumni = listVisibleRosterRecords();
        Map<String, Long> statusCount = allAlumni.stream()
                .collect(Collectors.groupingBy(
                        alumni -> StringUtils.hasText(alumni.getMemberStatus()) ? alumni.getMemberStatus() : "未知",
                        Collectors.counting()
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("total", allAlumni.size());
        result.put("statusDistribution", statusCount);
        return result;
    }

    @Override
    public List<Object[]> getGenerationStatistics() {
        List<ClubAlumni> allAlumni = listVisibleGraduatedAlumni();
        Map<Integer, Long> generationCount = allAlumni.stream()
                .filter(alumni -> alumni.getGenerationYear() != null)
                .collect(Collectors.groupingBy(ClubAlumni::getGenerationYear, Collectors.counting()));

        List<Object[]> result = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : generationCount.entrySet()) {
            result.add(new Object[]{entry.getKey(), entry.getValue()});
        }
        result.sort((a, b) -> ((Integer) a[0]).compareTo((Integer) b[0]));
        return result;
    }

    @Override
    public Map<String, Object> getGraduationDistribution() {
        List<ClubAlumni> allAlumni = listVisibleRosterRecords();
        Map<String, Long> statusDistribution = allAlumni.stream()
                .collect(Collectors.groupingBy(
                        alumni -> StringUtils.hasText(alumni.getMemberStatus()) ? alumni.getMemberStatus() : "未知",
                        Collectors.counting()
                ));

        Map<String, Object> result = new HashMap<>();
        long total = allAlumni.size();
        for (Map.Entry<String, Long> entry : statusDistribution.entrySet()) {
            double percentage = total == 0 ? 0 : (entry.getValue() * 100.0) / total;
            result.put(entry.getKey(), Map.of(
                    "count", entry.getValue(),
                    "percentage", String.format("%.2f%%", percentage)
            ));
        }
        return result;
    }

    @Override
    @Transactional
    public int batchUpdateGraduationStatus() {
        List<ClubAlumni> allAlumni = listScopedRoster();
        int updateCount = 0;
        for (ClubAlumni alumni : allAlumni) {
            String newStatus = calculateGraduationStatus(alumni);
            if (!newStatus.equals(alumni.getMemberStatus())) {
                alumni.setMemberStatus(newStatus);
                alumni.setUpdateTime(LocalDateTime.now());
                updateById(alumni);
                updateCount++;
            }
        }
        return updateCount;
    }

    @Override
    public Map<String, Object> calculateGraduationStatus(Integer enrollmentYear, Integer generationYear, String gradeClass) {
        if (enrollmentYear != null || generationYear != null) {
            String resolvedStatus = enrollmentYear != null
                    ? (LocalDate.now().getYear() - enrollmentYear >= 4 ? STATUS_GRADUATED : STATUS_CURRENT)
                    : deriveGraduationStatus(generationYear);
            Map<String, Object> resolved = new HashMap<>();
            resolved.put("status", resolvedStatus);
            resolved.put("description", STATUS_GRADUATED.equals(resolvedStatus) ? "已毕业" : "当前在校");
            resolved.put("enrollmentYear", enrollmentYear);
            resolved.put("generationYear", generationYear);
            resolved.put("gradeClass", gradeClass);
            return resolved;
        }
        String status = "在校";
        String description = "当前在校";
        if (enrollmentYear != null) {
            int currentYear = LocalDate.now().getYear();
            int yearsSinceEnrollment = currentYear - enrollmentYear;
            if (yearsSinceEnrollment >= 4) {
                status = "毕业";
                description = "已毕业";
            } else if (yearsSinceEnrollment >= 3) {
                status = "工作";
                description = "实习或工作中";
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", status);
        result.put("description", description);
        result.put("enrollmentYear", enrollmentYear);
        result.put("generationYear", generationYear);
        result.put("gradeClass", gradeClass);
        return result;
    }

    private String calculateGraduationStatus(ClubAlumni alumni) {
        if (alumni.getActualGraduationDate() != null) {
            return STATUS_GRADUATED;
        }
        if (alumni.getExpectedGraduationYear() != null) {
            return LocalDate.now().getYear() >= alumni.getExpectedGraduationYear() ? STATUS_GRADUATED : STATUS_CURRENT;
        }
        if (alumni.getEnrollmentYear() != null) {
            return LocalDate.now().getYear() - alumni.getEnrollmentYear() >= 4 ? STATUS_GRADUATED : STATUS_CURRENT;
        }
        if (alumni.getGenerationYear() != null) {
            return deriveGraduationStatus(alumni.getGenerationYear());
        }
        if (isGraduatedStatus(alumni.getMemberStatus()) || isCurrentStatus(alumni.getMemberStatus())) {
            return normalizeGraduationStatus(alumni.getMemberStatus());
        }
        if (StringUtils.hasText(alumni.getMemberStatus())) {
            return alumni.getMemberStatus();
        }
        return STATUS_CURRENT;
    }

    @Override
    public Map<Integer, List<ClubAlumni>> getShowAlumniGroupedByGeneration() {
        return groupByGeneration(listVisibleGraduatedAlumni());
    }

    @Override
    public Map<String, Object> getShowAlumniOverview() {
        return buildVisibleAlumniOverview();
    }

    @Override
    @Transactional
    public String importFromExcel(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        List<ClubAlumni> rows;
        try {
            if (filename.endsWith(".csv")) {
                rows = parseCsv(file);
            } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                rows = parseWorkbook(file);
            } else {
                throw new IllegalArgumentException("仅支持 .csv / .xlsx / .xls 文件");
            }
        } catch (IOException e) {
            throw new RuntimeException("读取导入文件失败", e);
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("导入文件中没有有效数据");
        }

        int created = 0;
        int updated = 0;
        for (ClubAlumni alumni : rows) {
            ClubAlumni existing = findExistingAlumni(alumni);
            if (existing == null) {
                prepareAlumni(alumni, null);
                save(alumni);
                syncAlumniSearchIndex(alumni.getId());
                created++;
            } else {
                alumni.setId(existing.getId());
                alumni.setTenantId(existing.getTenantId());
                prepareAlumni(alumni, existing);
                updateById(alumni);
                syncAlumniSearchIndex(alumni.getId());
                updated++;
            }
        }
        return String.format("导入完成：新增 %d 条，更新 %d 条，共 %d 条", created, updated, rows.size());
    }

    private List<ClubAlumni> parseCsv(MultipartFile file) throws IOException {
        List<ClubAlumni> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (!StringUtils.hasText(headerLine)) {
                return result;
            }
            Map<String, Integer> headerIndex = buildHeaderIndex(splitCsvLine(headerLine));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                String[] values = splitCsvLine(line);
                ClubAlumni alumni = buildAlumni(headerIndex, idx -> idx < values.length ? values[idx].trim() : "");
                if (alumni != null) {
                    result.add(alumni);
                }
            }
        }
        return result;
    }

    private List<ClubAlumni> parseWorkbook(MultipartFile file) throws IOException {
        List<ClubAlumni> result = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return result;
            }
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return result;
            }
            String[] headers = new String[headerRow.getLastCellNum()];
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.getCell(i);
                headers[i] = cell == null ? "" : formatter.formatCellValue(cell).trim();
            }
            Map<String, Integer> headerIndex = buildHeaderIndex(headers);
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                ClubAlumni alumni = buildAlumni(headerIndex, idx -> {
                    Cell cell = row.getCell(idx);
                    return cell == null ? "" : formatter.formatCellValue(cell).trim();
                });
                if (alumni != null) {
                    result.add(alumni);
                }
            }
        } catch (Exception e) {
            throw new IOException("解析Excel失败", e);
        }
        return result;
    }

    private Map<String, Integer> buildHeaderIndex(String[] headers) {
        Map<String, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            if (StringUtils.hasText(headers[i])) {
                indexMap.put(headers[i].trim().toLowerCase(Locale.ROOT), i);
            }
        }
        return indexMap;
    }

    private ClubAlumni buildAlumni(Map<String, Integer> headerIndex, java.util.function.IntFunction<String> valueProvider) {
        String name = read(headerIndex, valueProvider, "name", "姓名");
        String generationYear = read(headerIndex, valueProvider, "generationyear", "generation_year", "入学年份", "届数");
        String major = read(headerIndex, valueProvider, "major", "专业");
        if (!StringUtils.hasText(name) || !StringUtils.hasText(generationYear) || !StringUtils.hasText(major)) {
            return null;
        }

        ClubAlumni alumni = new ClubAlumni();
        alumni.setName(name);
        alumni.setGenerationYear(parseInteger(generationYear));
        alumni.setEnrollmentYear(parseInteger(read(headerIndex, valueProvider, "enrollmentyear", "enrollment_year", "入学年份")));
        alumni.setStudentId(read(headerIndex, valueProvider, "studentid", "student_id", "学号"));
        alumni.setEmail(read(headerIndex, valueProvider, "email", "邮箱"));
        alumni.setMajor(major);
        alumni.setDepartment(read(headerIndex, valueProvider, "department", "部门"));
        alumni.setPosition(read(headerIndex, valueProvider, "position", "职位"));
        alumni.setWorkUnit(read(headerIndex, valueProvider, "workunit", "work_unit", "工作单位"));
        alumni.setCurrentContact(read(headerIndex, valueProvider, "currentcontact", "current_contact", "联系方式"));
        alumni.setHonorCertificates(read(headerIndex, valueProvider, "honorcertificates", "honor_certificates", "荣誉证书"));
        alumni.setNotes(read(headerIndex, valueProvider, "notes", "备注"));
        return alumni;
    }

    private String read(Map<String, Integer> headerIndex, java.util.function.IntFunction<String> valueProvider, String... aliases) {
        for (String alias : aliases) {
            Integer idx = headerIndex.get(alias.toLowerCase(Locale.ROOT));
            if (idx != null) {
                String value = valueProvider.apply(idx);
                if (value != null) {
                    return value.trim();
                }
            }
        }
        return "";
    }

    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.replace(".0", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String[] splitCsvLine(String line) {
        return line.split(",", -1);
    }

    private ClubAlumni findExistingAlumni(ClubAlumni alumni) {
        LambdaQueryWrapper<ClubAlumni> wrapper = applyTenantScope(new LambdaQueryWrapper<>());
        if (StringUtils.hasText(alumni.getStudentId())) {
            wrapper.eq(ClubAlumni::getStudentId, alumni.getStudentId());
        } else if (StringUtils.hasText(alumni.getEmail())) {
            wrapper.eq(ClubAlumni::getEmail, alumni.getEmail());
        } else {
            wrapper.eq(ClubAlumni::getName, alumni.getName())
                    .eq(ClubAlumni::getGenerationYear, alumni.getGenerationYear());
        }
        wrapper.last("LIMIT 1");
        return getOne(wrapper, false);
    }

    private void prepareAlumni(ClubAlumni alumni, ClubAlumni existing) {
        if (existing != null && existing.getTenantId() != null) {
            alumni.setTenantId(existing.getTenantId());
        } else if (alumni.getTenantId() == null) {
            Long tenantId = TenantContext.getTenantId();
            alumni.setTenantId(tenantId != null ? tenantId : 1L);
        }
        if (alumni.getShowTable() == null) {
            alumni.setShowTable(true);
        }
        if (alumni.getIsActive() == null) {
            alumni.setIsActive(true);
        }
        if (alumni.getIsCoreMember() == null) {
            alumni.setIsCoreMember(false);
        }
        if (existing != null && alumni.getCreateTime() == null) {
            alumni.setCreateTime(existing.getCreateTime());
        }
        if (alumni.getCreateTime() == null) {
            alumni.setCreateTime(LocalDateTime.now());
        }
        if (alumni.getUpdateTime() == null) {
            alumni.setUpdateTime(LocalDateTime.now());
        }
        if (alumni.getIsDeleted() == null) {
            alumni.setIsDeleted(0);
        }
        alumni.setMemberStatus(calculateGraduationStatus(alumni));
        alumni.setGraduationStatus(alumni.getMemberStatus());
    }

    private LambdaQueryWrapper<ClubAlumni> applyTenantScope(LambdaQueryWrapper<ClubAlumni> wrapper) {
        if (canManageAllTenants()) {
            wrapper.isNotNull(ClubAlumni::getTenantId);
            applyActiveTenantScope(wrapper);
            return wrapper;
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            wrapper.eq(ClubAlumni::getTenantId, tenantId);
            return wrapper;
        }
        return wrapper;
    }

    List<ClubAlumni> listScopedRoster() {
        return enrichComputedFields(list(applyTenantScope(new LambdaQueryWrapper<ClubAlumni>())
                .orderByDesc(ClubAlumni::getGenerationYear)));
    }

    List<ClubAlumni> listVisibleRosterRecords() {
        return listScopedRoster().stream()
                .filter(this::isVisibleRosterRecord)
                .collect(Collectors.toList());
    }

    List<ClubAlumni> listVisibleGraduatedAlumni() {
        return listVisibleRosterRecords().stream()
                .filter(alumni -> isGraduatedStatus(alumni.getMemberStatus()))
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildVisibleAlumniOverview() {
        List<ClubAlumni> visibleRoster = enrichComputedFields(listVisibleRosterRecords());
        List<ClubAlumni> visibleGraduatedAlumni = visibleRoster.stream()
                .filter(alumni -> isGraduatedStatus(alumni.getMemberStatus()))
                .collect(Collectors.toList());
        Map<Integer, List<ClubAlumni>> groupedData = groupByGeneration(visibleGraduatedAlumni);
        long currentCount = visibleRoster.stream()
                .filter(item -> !isGraduatedStatus(item.getMemberStatus()))
                .count();

        Map<String, Object> overview = new HashMap<>();
        overview.put("totalCount", visibleRoster.size());
        overview.put("rosterCount", visibleRoster.size());
        overview.put("alumniCount", (long) visibleGraduatedAlumni.size());
        overview.put("generations", groupedData.size());
        overview.put("groupedData", groupedData);
        overview.put("currentCount", currentCount);
        overview.put("graduatedCount", (long) visibleGraduatedAlumni.size());
        overview.put("generationCount", groupedData.size());
        return overview;
    }

    private boolean isVisibleRosterRecord(ClubAlumni alumni) {
        return alumni != null
                && (alumni.getShowTable() == null || alumni.getShowTable())
                && (alumni.getIsActive() == null || alumni.getIsActive())
                && alumni.getGenerationYear() != null;
    }

    private Map<Integer, List<ClubAlumni>> groupByGeneration(List<ClubAlumni> alumniList) {
        return enrichComputedFields(alumniList).stream().collect(Collectors.groupingBy(
                ClubAlumni::getGenerationYear,
                LinkedHashMap::new,
                Collectors.collectingAndThen(Collectors.toList(), list -> {
                    list.sort((a, b) -> {
                        boolean aCore = Boolean.TRUE.equals(a.getIsCoreMember());
                        boolean bCore = Boolean.TRUE.equals(b.getIsCoreMember());
                        if (aCore != bCore) {
                            return aCore ? -1 : 1;
                        }
                        return String.valueOf(a.getName()).compareTo(String.valueOf(b.getName()));
                    });
                    return list;
                })
        ));
    }

    private List<ClubAlumni> enrichComputedFields(List<ClubAlumni> alumniList) {
        return alumniList.stream().map(this::enrichComputedFields).collect(Collectors.toList());
    }

    private ClubAlumni enrichComputedFields(ClubAlumni alumni) {
        if (alumni == null) {
            return null;
        }
        if (alumni.getShowTable() == null) {
            alumni.setShowTable(true);
        }
        if (alumni.getIsActive() == null) {
            alumni.setIsActive(!Objects.equals(alumni.getIsDeleted(), 1));
        }
        if (alumni.getIsCoreMember() == null) {
            alumni.setIsCoreMember(false);
        }
        alumni.setMemberStatus(calculateGraduationStatus(alumni));
        alumni.setGraduationStatus(alumni.getMemberStatus());
        return alumni;
    }

    private boolean isPublicAlumni(ClubAlumni alumni) {
        return alumni != null
                && !Integer.valueOf(1).equals(alumni.getIsDeleted())
                && Boolean.TRUE.equals(alumni.getIsActive())
                && Boolean.TRUE.equals(alumni.getShowTable())
                && isGraduatedStatus(alumni.getMemberStatus());
    }

    private void syncAlumniSearchIndex(Long id) {
        if (id == null) {
            return;
        }
        SearchIndexSyncUtils.runAfterCommit(() -> {
            ClubAlumni alumni = null;
            try {
                alumni = baseMapper.selectById(id);
                if (alumni == null || !isPublicAlumni(alumni)) {
                    searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_ALUMNI, id,
                            alumni == null ? TenantContext.getTenantId() : alumni.getTenantId());
                    return;
                }
                searchClient.upsertGlobalDocument(buildAlumniSearchDocument(alumni));
            } catch (Exception e) {
                log.warn("sync alumni global search index failed, alumniId={}, tenantId={}, reason={}",
                        id, alumni == null ? null : alumni.getTenantId(), e.getMessage());
            }
        });
    }

    private void deleteAlumniSearchIndex(Long id, Long tenantId) {
        if (id == null) {
            return;
        }
        Long resolvedTenantId = tenantId != null ? tenantId : TenantContext.getTenantId();
        SearchIndexSyncUtils.runAfterCommit(() -> {
            try {
                searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_ALUMNI, id, resolvedTenantId);
            } catch (Exception e) {
                log.warn("delete alumni global search index failed, alumniId={}, tenantId={}, reason={}",
                        id, resolvedTenantId, e.getMessage());
            }
        });
    }

    @Override
    public List<GlobalSearchDocumentDTO> exportSearchDocuments() {
        LambdaQueryWrapper<ClubAlumni> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ClubAlumni::getIsDeleted, 0)
                .orderByDesc(ClubAlumni::getUpdateTime);
        return list(queryWrapper).stream()
                .map(this::enrichComputedFields)
                .filter(this::isPublicAlumni)
                .map(this::buildAlumniSearchDocument)
                .collect(Collectors.toList());
    }

    private GlobalSearchDocumentDTO buildAlumniSearchDocument(ClubAlumni alumni) {
        return new GlobalSearchDocumentDTO()
                .setEntityType(SEARCH_ENTITY_TYPE_ALUMNI)
                .setEntityId(alumni.getId())
                .setTenantId(alumni.getTenantId())
                .setTitle(alumni.getName())
                .setSummary(joinSearchText(alumni.getDepartment(), alumni.getPosition(), alumni.getMajor()))
                .setContent(joinSearchText(alumni.getWorkUnit(), alumni.getWorkCity(), alumni.getJobContent(), alumni.getNotes()))
                .setTags(joinSearchText(alumni.getMemberStatus(), alumni.getGraduationStatus(), alumni.getSkills()))
                .setRoute("/alumni")
                .setCoverUrl(null)
                .setUpdatedAt(formatUpdatedAt(alumni.getUpdateTime(), alumni.getCreateTime()))
                .setVisible(isPublicAlumni(alumni));
    }

    private String joinSearchText(String... values) {
        if (values == null) {
            return null;
        }
        return java.util.Arrays.stream(values)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }

    private String formatUpdatedAt(LocalDateTime... times) {
        if (times == null) {
            return null;
        }
        for (LocalDateTime time : times) {
            if (time != null) {
                return time.toString();
            }
        }
        return null;
    }

    private String deriveGraduationStatus(Integer generationYear) {
        if (generationYear != null) {
            int currentYear = LocalDate.now().getYear();
            if (currentYear - generationYear >= 4) {
                return STATUS_GRADUATED;
            }
        }
        return STATUS_CURRENT;
    }

    private boolean matchesGraduationStatus(String actual, String expected) {
        return normalizeGraduationStatus(actual).equals(normalizeGraduationStatus(expected));
    }

    private boolean isGraduatedStatus(String status) {
        return STATUS_GRADUATED.equals(normalizeGraduationStatus(status));
    }

    private boolean isCurrentStatus(String status) {
        return STATUS_CURRENT.equals(normalizeGraduationStatus(status));
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private String normalizeGraduationStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return STATUS_CURRENT;
        }
        String normalized = status.trim();
        if ("毕业".equals(normalized) || "已毕业".equals(normalized)) {
            return STATUS_GRADUATED;
        }
        if ("在校".equals(normalized) || "工作".equals(normalized)) {
            return STATUS_CURRENT;
        }
        return normalized;
    }

    private boolean canManageAllTenants() {
        return Boolean.TRUE.equals(TenantContext.isSuperAdmin());
    }

    private List<Long> resolveScopedTenantIds() {
        if (canManageAllTenants()) {
            return resolveActiveTenantIds();
        }
        Long tenantId = TenantContext.getTenantId();
        return List.of(tenantId == null ? 1L : tenantId);
    }

    private boolean canAccessTenant(Long tenantId) {
        if (tenantId == null) {
            return false;
        }
        return resolveScopedTenantIds().contains(tenantId);
    }

    private void applyActiveTenantScope(LambdaQueryWrapper<ClubAlumni> wrapper) {
        List<Long> activeTenantIds = resolveActiveTenantIds();
        if (activeTenantIds.isEmpty()) {
            wrapper.eq(ClubAlumni::getTenantId, -1L);
            return;
        }
        wrapper.in(ClubAlumni::getTenantId, activeTenantIds);
    }

    private List<Long> resolveActiveTenantIds() {
        LambdaQueryWrapper<RKTenant> tenantQuery = new LambdaQueryWrapper<>();
        tenantQuery.select(RKTenant::getId)
                .eq(RKTenant::getStatus, 1)
                .eq(RKTenant::getIsDeleted, 0)
                .and(query -> query.isNull(RKTenant::getExpireTime)
                        .or()
                        .gt(RKTenant::getExpireTime, LocalDateTime.now()));
        return tenantMapper.selectList(tenantQuery).stream()
                .map(RKTenant::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private <T> T runInTenantScope(Long tenantId, java.util.function.Supplier<T> action) {
        Long previousTenantId = TenantContext.getTenantId();
        Boolean previousSuperAdmin = TenantContext.isSuperAdmin();
        try {
            TenantContext.setTenantId(tenantId);
            TenantContext.setSuperAdmin(false);
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
