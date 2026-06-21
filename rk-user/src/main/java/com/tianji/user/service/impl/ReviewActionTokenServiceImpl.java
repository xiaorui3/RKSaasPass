package com.tianji.user.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.user.domain.dto.ReviewActionTokenDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewActionTokenServiceImpl {

    private static final String KEY_PREFIX = "admission:review:token:";
    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public String createToken(Long tenantId, String targetType, Long targetId, String action) {
        try {
            ReviewActionTokenDTO dto = new ReviewActionTokenDTO();
            dto.setToken(UUID.randomUUID().toString().replace("-", ""));
            dto.setTenantId(tenantId);
            dto.setTargetType(targetType);
            dto.setTargetId(targetId);
            dto.setAction(action);
            dto.setExpiresAt(LocalDateTime.now().plusHours(24));
            dto.setUsed(Boolean.FALSE);

            stringRedisTemplate.opsForValue().set(buildKey(dto.getToken()), objectMapper.writeValueAsString(dto), TOKEN_TTL);
            return dto.getToken();
        } catch (Exception e) {
            throw new RuntimeException("create review token failed", e);
        }
    }

    public ReviewActionTokenDTO previewToken(String token) {
        ReviewActionTokenDTO dto = readToken(token);
        if (dto.getExpiresAt() != null && dto.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("审批链接已过期");
        }
        return dto;
    }

    public ReviewActionTokenDTO consumeToken(String token, Long authUserId) {
        try {
            String raw = stringRedisTemplate.opsForValue().getAndDelete(buildKey(token));
            if (raw == null || raw.isBlank()) {
                throw new BadRequestException("审批链接不存在或已失效");
            }
            ReviewActionTokenDTO dto = objectMapper.readValue(raw, ReviewActionTokenDTO.class);
            if (dto.getExpiresAt() != null && dto.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("审批链接已过期");
            }
            dto.setUsed(Boolean.TRUE);
            dto.setUsedByAuthUserId(authUserId);
            dto.setUsedTime(LocalDateTime.now());
            return dto;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("consume review token failed", e);
        }
    }

    private ReviewActionTokenDTO readToken(String token) {
        try {
            String raw = stringRedisTemplate.opsForValue().get(buildKey(token));
            if (raw == null || raw.isBlank()) {
                throw new BadRequestException("审批链接不存在或已失效");
            }
            return objectMapper.readValue(raw, ReviewActionTokenDTO.class);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("read review token failed", e);
        }
    }

    private String buildKey(String token) {
        return KEY_PREFIX + token;
    }
}
