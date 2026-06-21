package com.tianji.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.user.domain.po.Banner;

import java.util.List;

/**
 * 轮播图服务接口
 */
public interface IBannerService extends IService<Banner> {

    /**
     * 获取轮播图列表
     * @param type 类型(可选)
     * @param limit 限制数量(可选)
     * @return 轮播图列表
     */
    List<Banner> getBannerList(Integer type, Integer limit);

    /**
     * 获取启用的轮播图列表
     * @return 轮播图列表
     */
    List<Banner> getActiveBanners();
}
