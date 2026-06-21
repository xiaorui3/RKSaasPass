package com.tianji.activity.domain.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("competition_competitions")
public class Competition implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    private String title;
    private String subtitle;
    private String description;
    private String content;
    private String organizer;

    @TableField("co_organizer")
    private String coOrganizer;

    @TableField("competition_type")
    private String competitionType;

    private String level;

    @TableField("max_participants")
    private Integer maxParticipants;

    @TableField("registration_start")
    private LocalDateTime registrationStart;

    @TableField("registration_end")
    private LocalDateTime registrationEnd;

    @TableField("competition_start")
    private LocalDateTime competitionStart;

    @TableField("competition_end")
    private LocalDateTime competitionEnd;

    private String location;

    @TableField("online_url")
    private String onlineUrl;

    private String status;

    @TableField("cover_image")
    private String coverImage;

    @TableField("rules_file")
    private String rulesFile;

    @TableField("materials_file")
    private String materialsFile;

    @TableField("results_file")
    private String resultsFile;

    private String summary;
    private String awards;

    @TableField("participation_points")
    private Integer participationPoints;

    @TableField("first_prize_points")
    private Integer firstPrizePoints;

    @TableField("second_prize_points")
    private Integer secondPrizePoints;

    @TableField("third_prize_points")
    private Integer thirdPrizePoints;

    @TableField("excellent_prize_points")
    private Integer excellentPrizePoints;

    @TableField("contact_person")
    private String contactPerson;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("contact_email")
    private String contactEmail;

    private String tags;
    private Integer priority;

    @TableField("is_published")
    private Boolean isPublished;

    @TableField("is_featured")
    private Boolean isFeatured;

    @TableField("is_cross_tenant")
    private Boolean isCrossTenant;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("registration_count")
    private Integer registrationCount;

    @TableField("manager_review_status")
    private Integer managerReviewStatus;

    @TableField("manager_review_comment")
    private String managerReviewComment;

    @TableField("manager_review_time")
    private LocalDateTime managerReviewTime;

    @TableField("manager_reviewer_id")
    private Long managerReviewerId;

    @TableField("teacher_review_status")
    private Integer teacherReviewStatus;

    @TableField("teacher_review_comment")
    private String teacherReviewComment;

    @TableField("teacher_review_time")
    private LocalDateTime teacherReviewTime;

    @TableField("teacher_reviewer_id")
    private Long teacherReviewerId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    @TableField(exist = false)
    private Integer delFlag;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    public static final int REVIEW_PENDING = 0;
    public static final int REVIEW_APPROVED = 1;
    public static final int REVIEW_REJECTED = 2;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public String getCoOrganizer() {
        return coOrganizer;
    }

    public void setCoOrganizer(String coOrganizer) {
        this.coOrganizer = coOrganizer;
    }

    public String getCompetitionType() {
        return competitionType;
    }

    public void setCompetitionType(String competitionType) {
        this.competitionType = competitionType;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public LocalDateTime getRegistrationStart() {
        return registrationStart;
    }

    public void setRegistrationStart(LocalDateTime registrationStart) {
        this.registrationStart = registrationStart;
    }

    public LocalDateTime getRegistrationEnd() {
        return registrationEnd;
    }

    public void setRegistrationEnd(LocalDateTime registrationEnd) {
        this.registrationEnd = registrationEnd;
    }

    public LocalDateTime getCompetitionStart() {
        return competitionStart;
    }

    public void setCompetitionStart(LocalDateTime competitionStart) {
        this.competitionStart = competitionStart;
    }

    public LocalDateTime getCompetitionEnd() {
        return competitionEnd;
    }

    public void setCompetitionEnd(LocalDateTime competitionEnd) {
        this.competitionEnd = competitionEnd;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOnlineUrl() {
        return onlineUrl;
    }

    public void setOnlineUrl(String onlineUrl) {
        this.onlineUrl = onlineUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getRulesFile() {
        return rulesFile;
    }

    public void setRulesFile(String rulesFile) {
        this.rulesFile = rulesFile;
    }

    public String getMaterialsFile() {
        return materialsFile;
    }

    public void setMaterialsFile(String materialsFile) {
        this.materialsFile = materialsFile;
    }

    public String getResultsFile() {
        return resultsFile;
    }

    public void setResultsFile(String resultsFile) {
        this.resultsFile = resultsFile;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getAwards() {
        return awards;
    }

    public void setAwards(String awards) {
        this.awards = awards;
    }

    public Integer getParticipationPoints() {
        return participationPoints;
    }

    public void setParticipationPoints(Integer participationPoints) {
        this.participationPoints = participationPoints;
    }

    public Integer getFirstPrizePoints() {
        return firstPrizePoints;
    }

    public void setFirstPrizePoints(Integer firstPrizePoints) {
        this.firstPrizePoints = firstPrizePoints;
    }

    public Integer getSecondPrizePoints() {
        return secondPrizePoints;
    }

    public void setSecondPrizePoints(Integer secondPrizePoints) {
        this.secondPrizePoints = secondPrizePoints;
    }

    public Integer getThirdPrizePoints() {
        return thirdPrizePoints;
    }

    public void setThirdPrizePoints(Integer thirdPrizePoints) {
        this.thirdPrizePoints = thirdPrizePoints;
    }

    public Integer getExcellentPrizePoints() {
        return excellentPrizePoints;
    }

    public void setExcellentPrizePoints(Integer excellentPrizePoints) {
        this.excellentPrizePoints = excellentPrizePoints;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Boolean getIsCrossTenant() {
        return isCrossTenant;
    }

    public void setIsCrossTenant(Boolean isCrossTenant) {
        this.isCrossTenant = isCrossTenant;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getRegistrationCount() {
        return registrationCount;
    }

    public void setRegistrationCount(Integer registrationCount) {
        this.registrationCount = registrationCount;
    }

    public Integer getTeacherReviewStatus() {
        return teacherReviewStatus;
    }

    public void setTeacherReviewStatus(Integer teacherReviewStatus) {
        this.teacherReviewStatus = teacherReviewStatus;
    }

    public String getTeacherReviewComment() {
        return teacherReviewComment;
    }

    public void setTeacherReviewComment(String teacherReviewComment) {
        this.teacherReviewComment = teacherReviewComment;
    }

    public LocalDateTime getTeacherReviewTime() {
        return teacherReviewTime;
    }

    public void setTeacherReviewTime(LocalDateTime teacherReviewTime) {
        this.teacherReviewTime = teacherReviewTime;
    }

    public Long getTeacherReviewerId() {
        return teacherReviewerId;
    }

    public void setTeacherReviewerId(Long teacherReviewerId) {
        this.teacherReviewerId = teacherReviewerId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}
