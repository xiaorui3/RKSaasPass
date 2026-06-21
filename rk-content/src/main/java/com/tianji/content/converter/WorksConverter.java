package com.tianji.content.converter;

import com.tianji.common.utils.BeanUtils;
import com.tianji.content.domain.dto.WorksSaveDTO;
import com.tianji.content.domain.dto.WorksUpdateDTO;
import com.tianji.content.domain.po.Work;
import com.tianji.content.domain.vo.WorksDetailVO;
import com.tianji.content.domain.vo.WorksListVO;
import com.tianji.content.domain.vo.WorksSimpleVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作品对象转换器
 * 用于DTO、Entity、VO之间的转换
 *
 * @author RK-Web
 * @since 2026/03/13
 */
@Component
public class WorksConverter {

    /**
     * WorksSaveDTO 转 Work Entity
     *
     * @param dto 保存DTO
     * @return Work实体
     */
    public Work toEntity(WorksSaveDTO dto) {
        if (dto == null) {
            return null;
        }
        Work work = new Work();
        work.setTitle(dto.getTitle());
        work.setDescription(dto.getDescription());
        work.setContent(dto.getContent());
        work.setCategory(dto.getCategory());
        work.setCoverImage(dto.getCoverImage());
        work.setDemoVideo(dto.getDemoVideo());
        work.setProjectLinks(dto.getProjectLinks());
        work.setTechnologies(dto.getTechnologies());
        work.setAuthors(dto.getAuthors());
        work.setIsFeatured(dto.getIsFeatured());
        work.setDisplayOrder(dto.getDisplayOrder());
        work.setManagerReviewerId(dto.getManagerReviewerId());
        work.setTeacherReviewerId(dto.getTeacherReviewerId());
        work.setViewCount(0);
        work.setLikeCount(0);
        work.setIsDeleted(0);
        
        return work;
    }

    /**
     * WorksUpdateDTO 转 Work Entity
     *
     * @param dto 更新DTO
     * @return Work实体
     */
    public Work toEntity(WorksUpdateDTO dto) {
        if (dto == null) {
            return null;
        }
        Work work = new Work();
        work.setId(dto.getId());
        work.setTitle(dto.getTitle());
        work.setDescription(dto.getDescription());
        work.setContent(dto.getContent());
        work.setCategory(dto.getCategory());
        work.setCoverImage(dto.getCoverImage());
        work.setDemoVideo(dto.getDemoVideo());
        work.setProjectLinks(dto.getProjectLinks());
        work.setTechnologies(dto.getTechnologies());
        work.setAuthors(dto.getAuthors());
        work.setIsFeatured(dto.getIsFeatured());
        work.setDisplayOrder(dto.getDisplayOrder());
        work.setManagerReviewerId(dto.getManagerReviewerId());
        work.setTeacherReviewerId(dto.getTeacherReviewerId());
        
        return work;
    }

    /**
     * Work Entity 转 WorksListVO
     *
     * @param work Work实体
     * @return 列表VO
     */
    public WorksListVO toListVO(Work work) {
        if (work == null) {
            return null;
        }
        return BeanUtils.copyBean(work, WorksListVO.class);
    }

    /**
     * Work Entity 列表 转 WorksListVO 列表
     *
     * @param workList Work实体列表
     * @return 列表VO列表
     */
    public List<WorksListVO> toListVO(List<Work> workList) {
        if (workList == null) {
            return null;
        }
        return BeanUtils.copyList(workList, WorksListVO.class);
    }

    /**
     * Work Entity 转 WorksDetailVO
     *
     * @param work Work实体
     * @return 详情VO
     */
    public WorksDetailVO toDetailVO(Work work) {
        if (work == null) {
            return null;
        }
        return BeanUtils.copyBean(work, WorksDetailVO.class);
    }

    /**
     * Work Entity 转 WorksDetailVO（带相关作品推荐）
     *
     * @param work          Work实体
     * @param relatedWorks  相关作品列表
     * @return 详情VO
     */
    public WorksDetailVO toDetailVO(Work work, List<WorksSimpleVO> relatedWorks) {
        if (work == null) {
            return null;
        }
        WorksDetailVO vo = BeanUtils.copyBean(work, WorksDetailVO.class);
        vo.setRelatedWorks(relatedWorks);
        return vo;
    }

    /**
     * Work Entity 转 WorksSimpleVO
     *
     * @param work Work实体
     * @return 简单VO
     */
    public WorksSimpleVO toSimpleVO(Work work) {
        if (work == null) {
            return null;
        }
        return BeanUtils.copyBean(work, WorksSimpleVO.class);
    }

    /**
     * Work Entity 列表 转 WorksSimpleVO 列表
     *
     * @param workList Work实体列表
     * @return 简单VO列表
     */
    public List<WorksSimpleVO> toSimpleVO(List<Work> workList) {
        if (workList == null) {
            return null;
        }
        return BeanUtils.copyList(workList, WorksSimpleVO.class);
    }

    /**
     * 更新Work实体（部分更新）
     *
     * @param work 需要更新的实体
     * @param dto  更新DTO
     */
    public void updateEntity(Work work, WorksUpdateDTO dto) {
        if (work == null || dto == null) {
            return;
        }
        
        if (dto.getTitle() != null) {
            work.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            work.setDescription(dto.getDescription());
        }
        if (dto.getContent() != null) {
            work.setContent(dto.getContent());
        }
        if (dto.getCategory() != null) {
            work.setCategory(dto.getCategory());
        }
        if (dto.getCoverImage() != null) {
            work.setCoverImage(dto.getCoverImage());
        }
        if (dto.getDemoVideo() != null) {
            work.setDemoVideo(dto.getDemoVideo());
        }
        if (dto.getProjectLinks() != null) {
            work.setProjectLinks(dto.getProjectLinks());
        }
        if (dto.getTechnologies() != null) {
            work.setTechnologies(dto.getTechnologies());
        }
        if (dto.getAuthors() != null) {
            work.setAuthors(dto.getAuthors());
        }
        if (dto.getIsFeatured() != null) {
            work.setIsFeatured(dto.getIsFeatured());
        }
        if (dto.getDisplayOrder() != null) {
            work.setDisplayOrder(dto.getDisplayOrder());
        }
        if (dto.getManagerReviewerId() != null) {
            work.setManagerReviewerId(dto.getManagerReviewerId());
        }
        if (dto.getTeacherReviewerId() != null) {
            work.setTeacherReviewerId(dto.getTeacherReviewerId());
        }
        
        // 更新时间
        work.setUpdateTime(LocalDateTime.now());
    }
}
