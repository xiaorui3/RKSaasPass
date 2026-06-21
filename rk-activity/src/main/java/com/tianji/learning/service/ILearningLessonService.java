package com.tianji.learning.service;

import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author 小锐
 * @since 2026-02-09
 */
public interface ILearningLessonService extends IService<LearningLesson> {

    void createLesson(Long userId, List<Long> courseIds);
}
