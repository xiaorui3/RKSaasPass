package com.tianji.learning.service.impl;

import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author 小锐
 * @since 2026-02-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {


    private final CourseClient courseClient;
    @Override
    @Transactional
    public void createLesson(Long userId, List<Long> courseIds) {
        List<CourseSimpleInfoDTO> simpleInfoList = courseClient.getSimpleInfoList(courseIds);
        if (simpleInfoList.isEmpty()){
            log.error("课程信息不存在");
            return;
        }
        log.info("开始创建课程信息");
        List<LearningLesson> lessons = new ArrayList<>(simpleInfoList.size());
        for (CourseSimpleInfoDTO info : simpleInfoList) {
            LearningLesson lesson = new LearningLesson();
            lesson.setUserId(userId);
            lesson.setCourseId(info.getId());
            lesson.setStatus(LessonStatus.NOT_BEGIN);
            Integer validDuration = info.getValidDuration();
            if (validDuration != null){
                LocalDateTime now =LocalDateTime.now();
                lesson.setCreateTime(now);
                lesson.setUpdateTime(now);
                lesson.setExpireTime(now.plusMonths(validDuration));

            }
            lessons.add(lesson);
        }
        saveBatch(lessons);
    }
}
