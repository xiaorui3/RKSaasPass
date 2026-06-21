package com.tianji.user.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.user.domain.dto.ReviewActionTokenDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewActionTokenServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private ReviewActionTokenServiceImpl service;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        service = new ReviewActionTokenServiceImpl(stringRedisTemplate, objectMapper);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void createToken_shouldPersistRedisPayloadWithExpiry() throws Exception {
        String token = service.createToken(2L, "REGISTER", 18L, "APPROVE");

        assertNotNull(token);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(keyCaptor.capture(), payloadCaptor.capture(), ttlCaptor.capture());

        assertEquals("admission:review:token:" + token, keyCaptor.getValue());
        assertEquals(Duration.ofHours(24), ttlCaptor.getValue());

        ReviewActionTokenDTO dto = objectMapper.readValue(payloadCaptor.getValue(), ReviewActionTokenDTO.class);
        assertEquals(token, dto.getToken());
        assertEquals(2L, dto.getTenantId());
        assertEquals("REGISTER", dto.getTargetType());
        assertEquals(18L, dto.getTargetId());
        assertEquals("APPROVE", dto.getAction());
        assertEquals(Boolean.FALSE, dto.getUsed());
        assertTrue(dto.getExpiresAt().isAfter(LocalDateTime.now().plusHours(23)));
    }

    @Test
    void previewToken_shouldRejectExpiredToken() throws Exception {
        ReviewActionTokenDTO dto = new ReviewActionTokenDTO();
        dto.setToken("expired-token");
        dto.setTenantId(1L);
        dto.setTargetType("JOIN");
        dto.setTargetId(9L);
        dto.setAction("REJECT");
        dto.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(valueOperations.get("admission:review:token:expired-token"))
                .thenReturn(objectMapper.writeValueAsString(dto));

        assertThrows(BadRequestException.class, () -> service.previewToken("expired-token"));
    }

    @Test
    void consumeToken_shouldDeleteRedisValueAndMarkUsage() throws Exception {
        ReviewActionTokenDTO dto = new ReviewActionTokenDTO();
        dto.setToken("consume-token");
        dto.setTenantId(3L);
        dto.setTargetType("REGISTER");
        dto.setTargetId(25L);
        dto.setAction("APPROVE");
        dto.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        dto.setUsed(Boolean.FALSE);

        when(valueOperations.getAndDelete("admission:review:token:consume-token"))
                .thenReturn(objectMapper.writeValueAsString(dto));

        ReviewActionTokenDTO consumed = service.consumeToken("consume-token", 2001L);

        assertEquals(Boolean.TRUE, consumed.getUsed());
        assertEquals(2001L, consumed.getUsedByAuthUserId());
        assertNotNull(consumed.getUsedTime());
        verify(valueOperations).getAndDelete(eq("admission:review:token:consume-token"));
    }

    @Test
    void consumeToken_shouldRejectMissingToken() {
        when(valueOperations.getAndDelete(anyString())).thenReturn(null);

        assertThrows(BadRequestException.class, () -> service.consumeToken("missing-token", 1L));
    }
}
