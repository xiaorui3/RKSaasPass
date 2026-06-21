package com.tianji.message.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.message.domain.dto.WikiDocumentDTO;
import com.tianji.message.domain.po.WikiDocument;
import com.tianji.message.mapper.WikiDocumentMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WikiDocumentControllerTest {

    @Mock
    private WikiDocumentMapper wikiDocumentMapper;

    private WikiDocumentController controller;

    @BeforeEach
    void setUp() {
        controller = new WikiDocumentController(wikiDocumentMapper);
        TenantContext.setTenantId(7L);
        UserContext.setUser(42L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.removeUser();
    }

    @Test
    void create_shouldPersistTenantUserAndDefaultFields() {
        WikiDocumentDTO dto = new WikiDocumentDTO();
        dto.setTitle("  Handbook  ");
        dto.setCategory("");
        dto.setContent("# Guide");

        R<WikiDocument> response = controller.create(dto);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        ArgumentCaptor<WikiDocument> captor = ArgumentCaptor.forClass(WikiDocument.class);
        verify(wikiDocumentMapper).insert(captor.capture());
        WikiDocument saved = captor.getValue();
        assertEquals(7L, saved.getTenantId());
        assertEquals(42L, saved.getCreator());
        assertEquals(42L, saved.getUpdater());
        assertEquals("Handbook", saved.getTitle());
        assertEquals("default", saved.getCategory());
        assertEquals(1, saved.getStatus());
        assertEquals(0, saved.getIsDeleted());
    }

    @Test
    void update_shouldOnlyUpdateCurrentTenantDocument() {
        WikiDocument existing = new WikiDocument()
                .setId(9L)
                .setTenantId(7L)
                .setTitle("Old")
                .setCategory("rules")
                .setContent("old");
        when(wikiDocumentMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        WikiDocumentDTO dto = new WikiDocumentDTO();
        dto.setTitle("New");
        dto.setCategory("tech");
        dto.setContent("new content");

        R<WikiDocument> response = controller.update(9L, dto);

        assertEquals(200, response.getCode());
        ArgumentCaptor<WikiDocument> captor = ArgumentCaptor.forClass(WikiDocument.class);
        verify(wikiDocumentMapper).updateById(captor.capture());
        assertEquals("New", captor.getValue().getTitle());
        assertEquals("tech", captor.getValue().getCategory());
        assertEquals("new content", captor.getValue().getContent());
        assertEquals(42L, captor.getValue().getUpdater());
    }

    @Test
    void list_shouldReturnMapperDocuments() {
        WikiDocument document = new WikiDocument().setId(1L).setTitle("Guide");
        when(wikiDocumentMapper.selectList(any(Wrapper.class))).thenReturn(List.of(document));

        R<List<WikiDocument>> response = controller.list("rules", "guide");

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().size());
        assertEquals("Guide", response.getData().get(0).getTitle());
    }
}
