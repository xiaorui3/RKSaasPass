package com.tianji.content.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.content.domain.dto.CommentCreateDTO;
import com.tianji.content.domain.vo.CommentVO;
import com.tianji.content.service.ICommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentControllerContractTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ICommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = Mockito.mock(ICommentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CommentController(commentService)).build();
    }

    @Test
    @DisplayName("GET /api/comments returns persisted comments for target with management fields")
    void listCommentsByTarget() throws Exception {
        CommentVO comment = new CommentVO();
        comment.setId(11L);
        comment.setTenantId(3L);
        comment.setTargetType("news");
        comment.setTargetId(99L);
        comment.setUserId(7L);
        comment.setUserName("Alice");
        comment.setUserAvatar("/avatars/alice.png");
        comment.setContent("Persisted comment");
        comment.setFeatured(false);
        comment.setStatus(1);
        comment.setCreateTime(LocalDateTime.of(2026, 6, 4, 10, 30));
        comment.setUpdateTime(LocalDateTime.of(2026, 6, 4, 10, 31));

        Mockito.when(commentService.listVisibleComments("news", 99L))
                .thenReturn(Collections.singletonList(comment));

        mockMvc.perform(get("/api/comments")
                        .param("targetType", "news")
                        .param("targetId", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].targetType").value("news"))
                .andExpect(jsonPath("$.data[0].targetId").value(99))
                .andExpect(jsonPath("$.data[0].userName").value("Alice"))
                .andExpect(jsonPath("$.data[0].userAvatar").value("/avatars/alice.png"))
                .andExpect(jsonPath("$.data[0].featured").value(false))
                .andExpect(jsonPath("$.data[0].status").value(1))
                .andExpect(jsonPath("$.data[0].createTime").exists())
                .andExpect(jsonPath("$.data[0].updateTime").exists());
    }

    @Test
    @DisplayName("POST /api/comments persists comment content and returns display identity")
    void createComment() throws Exception {
        CommentVO created = new CommentVO();
        created.setId(12L);
        created.setTargetType("activity");
        created.setTargetId(88L);
        created.setUserId(9L);
        created.setUserName("Bob");
        created.setUserAvatar("/avatars/bob.png");
        created.setContent("I will join");
        created.setFeatured(false);
        created.setStatus(1);

        Mockito.when(commentService.createComment(any(CommentCreateDTO.class))).thenReturn(created);

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetType", "activity");
        payload.put("targetId", 88L);
        payload.put("content", "I will join");

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.targetType").value("activity"))
                .andExpect(jsonPath("$.data.userName").value("Bob"))
                .andExpect(jsonPath("$.data.userAvatar").value("/avatars/bob.png"))
                .andExpect(jsonPath("$.data.featured").value(false))
                .andExpect(jsonPath("$.data.status").value(1));

        Mockito.verify(commentService).createComment(any(CommentCreateDTO.class));
    }

    @Test
    @DisplayName("GET /api/comments supports activity targets as same contract")
    void listActivityCommentsByTarget() throws Exception {
        Mockito.when(commentService.listVisibleComments(eq("activity"), eq(88L)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/comments")
                        .param("targetType", "activity")
                        .param("targetId", "88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
}
