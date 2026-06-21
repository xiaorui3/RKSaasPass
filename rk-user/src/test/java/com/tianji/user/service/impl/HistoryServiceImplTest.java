package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tianji.api.client.search.SearchClient;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.HistoryEvent;
import com.tianji.user.mapper.HistoryEventMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryServiceImplTest {

    @Mock
    private HistoryEventMapper historyEventMapper;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private SearchClient searchClient;

    private HistoryServiceImpl historyService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), HistoryEvent.class);
        historyService = new HistoryServiceImpl(redisTemplate, searchClient);
        ReflectionTestUtils.setField(historyService, "baseMapper", historyEventMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getTimelineData_shouldUseTenantScopedCacheKeyAndQueryCondition() {
        TenantContext.setTenantId(2L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("history:timeline:2")).thenReturn(null);
        when(historyEventMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        historyService.getTimelineData();

        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(historyEventMapper).selectList(wrapperCaptor.capture());
        verify(valueOperations).set(eq("history:timeline:2"), any(), eq(30L), eq(TimeUnit.MINUTES));
        assertTrue(wrapperCaptor.getValue().getCustomSqlSegment().contains("tenant_id"));
    }
}
