package com.tianji.api.client.search;

import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("rk-search")
public interface SearchClient {

    @GetMapping("/courses/name")
    List<Long> queryCoursesIdByName(
            @RequestParam(value = "keyword", required = false) String keyword);

    @PostMapping("/global/index")
    Boolean upsertGlobalDocument(@RequestBody GlobalSearchDocumentDTO document);

    @PostMapping("/global/index/bulk")
    Boolean upsertGlobalDocuments(@RequestBody List<GlobalSearchDocumentDTO> documents);

    @DeleteMapping("/global/index/{entityType}/{entityId}")
    Boolean deleteGlobalDocument(@PathVariable("entityType") String entityType,
                                 @PathVariable("entityId") Long entityId,
                                 @RequestParam(value = "tenantId", required = false) Long tenantId);
}
