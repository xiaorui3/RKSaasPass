package com.tianji.message.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.message.domain.dto.WikiDocumentDTO;
import com.tianji.message.domain.po.WikiDocument;
import com.tianji.message.mapper.WikiDocumentMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Api(tags = "Wiki document API")
@RestController
@RequestMapping("/api/wiki/documents")
@RequiredArgsConstructor
public class WikiDocumentController {

    private final WikiDocumentMapper wikiDocumentMapper;

    @ApiOperation("List wiki documents")
    @GetMapping
    public R<List<WikiDocument>> list(@RequestParam(required = false) String category,
                                      @RequestParam(required = false) String keyword) {
        QueryWrapper<WikiDocument> wrapper = scopedWrapper();
        if (StringUtils.hasText(category)) {
            wrapper.eq("category", category.trim());
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(q -> q.like("title", value).or().like("content", value));
        }
        wrapper.orderByAsc("category")
                .orderByDesc("update_time")
                .orderByDesc("id");
        return R.ok(wikiDocumentMapper.selectList(wrapper));
    }

    @ApiOperation("Get wiki document")
    @GetMapping("/{id}")
    public R<WikiDocument> detail(@PathVariable Long id) {
        WikiDocument document = wikiDocumentMapper.selectOne(scopedWrapper().eq("id", id));
        return document == null ? R.error("document not found") : R.ok(document);
    }

    @ApiOperation("Create wiki document")
    @PostMapping
    public R<WikiDocument> create(@RequestBody WikiDocumentDTO dto) {
        validate(dto);
        LocalDateTime now = LocalDateTime.now();
        Long userId = UserContext.getUser();
        WikiDocument document = new WikiDocument()
                .setTenantId(currentTenantId())
                .setTitle(dto.getTitle().trim())
                .setCategory(normalizeCategory(dto.getCategory()))
                .setContent(dto.getContent())
                .setStatus(dto.getStatus() == null ? 1 : dto.getStatus())
                .setCreateTime(now)
                .setUpdateTime(now)
                .setCreator(userId)
                .setUpdater(userId)
                .setIsDeleted(0);
        wikiDocumentMapper.insert(document);
        return R.ok(document);
    }

    @ApiOperation("Update wiki document")
    @PutMapping("/{id}")
    public R<WikiDocument> update(@PathVariable Long id, @RequestBody WikiDocumentDTO dto) {
        WikiDocument document = wikiDocumentMapper.selectOne(scopedWrapper().eq("id", id));
        if (document == null) {
            return R.error("document not found");
        }
        if (StringUtils.hasText(dto.getTitle())) {
            document.setTitle(dto.getTitle().trim());
        }
        if (dto.getCategory() != null) {
            document.setCategory(normalizeCategory(dto.getCategory()));
        }
        if (dto.getContent() != null) {
            document.setContent(dto.getContent());
        }
        if (dto.getStatus() != null) {
            document.setStatus(dto.getStatus());
        }
        document.setUpdateTime(LocalDateTime.now());
        document.setUpdater(UserContext.getUser());
        wikiDocumentMapper.updateById(document);
        return R.ok(document);
    }

    @ApiOperation("Delete wiki document")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        WikiDocument document = wikiDocumentMapper.selectOne(scopedWrapper().eq("id", id));
        if (document == null) {
            return R.error("document not found");
        }
        wikiDocumentMapper.deleteById(id);
        return R.ok("deleted");
    }

    private QueryWrapper<WikiDocument> scopedWrapper() {
        return new QueryWrapper<WikiDocument>()
                .eq("tenant_id", currentTenantId());
    }

    private void validate(WikiDocumentDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getTitle())) {
            throw new IllegalArgumentException("title is required");
        }
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private String normalizeCategory(String category) {
        return StringUtils.hasText(category) ? category.trim() : "default";
    }
}
