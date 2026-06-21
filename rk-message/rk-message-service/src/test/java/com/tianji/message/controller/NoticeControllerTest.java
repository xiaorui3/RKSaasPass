package com.tianji.message.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.message.domain.po.Notice;
import com.tianji.message.service.INoticeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NoticeControllerTest {

    @Mock
    private INoticeService noticeService;

    @InjectMocks
    private NoticeController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void pageRoute_shouldNotBeHandledAsNoticeId() throws Exception {
        Notice notice = new Notice()
                .setId(10L)
                .setTitle("page route notice");
        when(noticeService.getNoticePage(1, 5)).thenReturn(new PageDTO<>(1L, 1L, List.of(notice)));

        mockMvc.perform(get("/api/notices/page")
                        .param("page", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.pages").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(10))
                .andExpect(jsonPath("$.data.list[0].title").value("page route notice"));
    }
}
