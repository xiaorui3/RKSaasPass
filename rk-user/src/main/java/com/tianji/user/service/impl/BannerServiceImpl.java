package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.user.domain.po.Banner;
import com.tianji.user.mapper.BannerMapper;
import com.tianji.user.service.IBannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 轮播图服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements IBannerService {

    @Override
    public List<Banner> getBannerList(Integer type, Integer limit) {
        LambdaQueryWrapper<Banner> queryWrapper = new LambdaQueryWrapper<>();
        
        // 只查询启用的轮播图
        queryWrapper.eq(Banner::getStatus, 1);
        
        // 类型过滤
        if (type != null) {
            queryWrapper.eq(Banner::getType, type);
        }
        
        // 时间范围过滤
        LocalDateTime now = LocalDateTime.now();
        queryWrapper.and(w -> w.isNull(Banner::getStartTime).or().le(Banner::getStartTime, now));
        queryWrapper.and(w -> w.isNull(Banner::getEndTime).or().ge(Banner::getEndTime, now));
        
        // 按排序字段升序
        queryWrapper.orderByAsc(Banner::getSort);
        queryWrapper.orderByDesc(Banner::getCreateTime);
        
        List<Banner> banners = baseMapper.selectList(queryWrapper);
        
        // 限制返回数量
        if (limit != null && limit > 0 && banners.size() > limit) {
            banners = banners.stream().limit(limit).collect(Collectors.toList());
        }
        
        return banners;
    }

    @Override
    public List<Banner> getActiveBanners() {
        return getBannerList(null, null);
    }
}
