package com.tianji.user.controller;

import com.tianji.common.domain.R;
import com.tianji.user.domain.po.Banner;
import com.tianji.user.service.IBannerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 轮播图接口控制器
 * 提供轮播图列表等接口
 * 
 * 已连接真实数据库，支持多租户隔离
 */
@Slf4j
@Api(tags = "轮播图接口")
@RestController
@RequestMapping("/banners")
@RequiredArgsConstructor
public class BannerController {

    private final IBannerService bannerService;

    /**
     * 获取轮播图列表
     */
    @ApiOperation("获取轮播图列表")
    @GetMapping
    public R<List<Map<String, Object>>> getBanners(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer limit) {
        try {
            List<Banner> banners = bannerService.getBannerList(type, limit);
            
            // 转换为前端需要的格式
            List<Map<String, Object>> result = banners.stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());
            
            return R.ok(result);
        } catch (Exception e) {
            log.error("获取轮播图列表失败", e);
            return R.error("获取轮播图列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取轮播图详情
     */
    @ApiOperation("获取轮播图详情")
    @GetMapping("/{id}")
    public R<Map<String, Object>> getBannerDetail(@PathVariable Long id) {
        try {
            Banner banner = bannerService.getById(id);
            if (banner == null) {
                return R.error("轮播图不存在");
            }
            
            Map<String, Object> result = convertToMap(banner);
            return R.ok(result);
        } catch (Exception e) {
            log.error("获取轮播图详情失败", e);
            return R.error("获取轮播图详情失败：" + e.getMessage());
        }
    }

    /**
     * 转换Banner为Map
     */
    private Map<String, Object> convertToMap(Banner banner) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", banner.getId());
        map.put("title", banner.getTitle());
        map.put("imageUrl", banner.getImageUrl());
        map.put("linkUrl", banner.getLinkUrl());
        map.put("description", banner.getDescription());
        map.put("sort", banner.getSort());
        map.put("type", banner.getType());
        map.put("status", banner.getStatus());
        map.put("startTime", banner.getStartTime());
        map.put("endTime", banner.getEndTime());
        map.put("createTime", banner.getCreateTime());
        return map;
    }
}