/**
 * 课程管理API
 * 对应后端Controller: CourseController.java, CategoryController.java, CatalogueController.java
 * 基础路径: /courses, /categorys, /catalogues
 */
import request from '@/utils/request'

// ==================== 课程管理接口 ====================

/**
 * 获取课程基础信息
 * @param {number} id - 课程ID
 * @returns {Promise} 课程基础信息
 */
export function getCourseBaseInfo(id) {
  return request({
    url: `/courses/baseInfo/${id}`,
    method: 'get'
  })
}

/**
 * 保存课程基本信息
 * @param {Object} data - 课程信息
 * @returns {Promise} 课程ID
 */
export function saveCourseBaseInfo(data) {
  return request({
    url: '/courses/baseInfo/save',
    method: 'post',
    data
  })
}

/**
 * 获取课程章节目录
 * @param {number} id - 课程ID
 * @returns {Promise} 章节目录列表
 */
export function getCourseCatalogs(id) {
  return request({
    url: `/courses/catas/${id}`,
    method: 'get'
  })
}

/**
 * 保存课程章节
 * @param {number} id - 课程ID
 * @param {number} step - 步骤
 * @param {Object} data - 章节数据
 * @returns {Promise} 操作结果
 */
export function saveCourseCatalog(id, step, data) {
  return request({
    url: `/courses/catas/save/${id}/${step}`,
    method: 'post',
    data
  })
}

/**
 * 保存课程视频
 * @param {number} id - 课程ID
 * @param {Object} data - 视频信息
 * @returns {Promise} 操作结果
 */
export function saveCourseMedia(id, data) {
  return request({
    url: `/courses/media/save/${id}`,
    method: 'post',
    data
  })
}

/**
 * 保存小节题目
 * @param {number} id - 小节ID
 * @param {Object} data - 题目数据
 * @returns {Promise} 操作结果
 */
export function saveSectionSubject(id, data) {
  return request({
    url: `/courses/subjects/save/${id}`,
    method: 'post',
    data
  })
}

/**
 * 获取小节题目
 * @param {number} id - 小节ID
 * @returns {Promise} 题目列表
 */
export function getSectionSubject(id) {
  return request({
    url: `/courses/subjects/get/${id}`,
    method: 'get'
  })
}

/**
 * 获取课程教师信息
 * @param {number} id - 课程ID
 * @returns {Promise} 教师信息
 */
export function getCourseTeachers(id) {
  return request({
    url: `/courses/teachers/${id}`,
    method: 'get'
  })
}

/**
 * 保存课程教师信息
 * @param {Object} data - 教师信息
 * @returns {Promise} 操作结果
 */
export function saveCourseTeachers(data) {
  return request({
    url: '/courses/teachers/save',
    method: 'post',
    data
  })
}

/**
 * 课程上架
 * @param {Object} data - 上架信息
 * @returns {Promise} 操作结果
 */
export function publishCourse(data) {
  return request({
    url: '/courses/upShelf',
    method: 'post',
    data
  })
}

/**
 * 课程上架前校验
 * @param {number} id - 课程ID
 * @returns {Promise} 校验结果
 */
export function checkBeforePublish(id) {
  return request({
    url: `/courses/checkBeforeUpShelf/${id}`,
    method: 'get'
  })
}

/**
 * 课程下架
 * @param {Object} data - 下架信息
 * @returns {Promise} 操作结果
 */
export function unpublishCourse(data) {
  return request({
    url: '/courses/downShelf',
    method: 'post',
    data
  })
}

/**
 * 删除课程
 * @param {number} id - 课程ID
 * @returns {Promise} 操作结果
 */
export function deleteCourse(id) {
  return request({
    url: `/courses/delete/${id}`,
    method: 'delete'
  })
}

/**
 * 获取课程简单信息列表
 * @param {Object} params - 查询参数
 * @returns {Promise} 课程列表
 */
export function getSimpleCourseList(params) {
  return request({
    url: '/courses/simpleInfo/list',
    method: 'get',
    params
  })
}

/**
 * 获取课程章节序号列表
 * @param {number} id - 课程ID
 * @returns {Promise} 章节序号列表
 */
export function getCatalogIndexList(id) {
  return request({
    url: `/courses/catas/index/list/${id}`,
    method: 'get'
  })
}

/**
 * 生成练习ID
 * @returns {Promise} 练习ID
 */
export function generatePracticeId() {
  return request({
    url: '/courses/generator',
    method: 'get'
  })
}

/**
 * 管理端课程搜索
 * @param {Object} params - 查询参数
 * @returns {Promise} 分页数据
 */
export function getCoursePage(params) {
  return request({
    url: '/courses/page',
    method: 'get',
    params
  })
}

/**
 * 校验课程名称是否存在
 * @param {string} name - 课程名称
 * @param {number} id - 课程ID（编辑时传入）
 * @returns {Promise} 是否存在
 */
export function checkCourseName(name, id) {
  return request({
    url: '/courses/checkName',
    method: 'get',
    params: { name, id }
  })
}

/**
 * 查询课程基本信息、目录、学习进度
 * @param {number} id - 课程ID
 * @returns {Promise} 课程详情
 */
export function getCourseWithProgress(id) {
  return request({
    url: `/courses/${id}/catalogs`,
    method: 'get'
  })
}

/**
 * 获取课程信息
 * @param {number} id - 课程ID
 * @returns {Promise} 课程信息
 */
export function getCourseById(id) {
  return request({
    url: `/course/${id}`,
    method: 'get'
  })
}

/**
 * 根据课程名称查询课程ID
 * @param {string} name - 课程名称
 * @returns {Promise} 课程ID
 */
export function getCourseIdByName(name) {
  return request({
    url: '/course/name',
    method: 'get',
    params: { name }
  })
}

/**
 * 根据分类ID列表获取分类名称映射
 * @param {number[]} ids - 分类ID列表
 * @returns {Promise} 名称映射
 */
export function getCategoryNameMap(ids) {
  return request({
    url: '/course/getCateNameMap',
    method: 'get',
    params: { ids: ids.join(',') }
  })
}

// ==================== 课程分类接口 ====================

/**
 * 查询课程分类列表
 * @returns {Promise} 分类列表
 */
export function getCategoryList() {
  return request({
    url: '/categorys/list',
    method: 'get'
  })
}

/**
 * 获取分类详情
 * @param {number} id - 分类ID
 * @returns {Promise} 分类信息
 */
export function getCategoryById(id) {
  return request({
    url: `/categorys/${id}`,
    method: 'get'
  })
}

/**
 * 新增课程分类
 * @param {Object} data - 分类信息
 * @returns {Promise} 操作结果
 */
export function addCategory(data) {
  return request({
    url: '/categorys/add',
    method: 'post',
    data
  })
}

/**
 * 删除课程分类
 * @param {number} id - 分类ID
 * @returns {Promise} 操作结果
 */
export function deleteCategory(id) {
  return request({
    url: `/categorys/${id}`,
    method: 'delete'
  })
}

/**
 * 课程分类停用或启用
 * @param {Object} data - 包含id和status
 * @returns {Promise} 操作结果
 */
export function toggleCategoryStatus(data) {
  return request({
    url: '/categorys/disableOrEnable',
    method: 'put',
    data
  })
}

/**
 * 更新课程分类
 * @param {Object} data - 分类信息
 * @returns {Promise} 操作结果
 */
export function updateCategory(data) {
  return request({
    url: '/categorys/update',
    method: 'put',
    data
  })
}

/**
 * 获取所有课程分类（树形）
 * @returns {Promise} 分类树
 */
export function getAllCategories() {
  return request({
    url: '/categorys/all',
    method: 'get'
  })
}

/**
 * 获取所有课程分类（不分层）
 * @returns {Promise} 分类列表
 */
export function getAllCategoriesFlat() {
  return request({
    url: '/categorys/getAllOfOneLevel',
    method: 'get'
  })
}

// ==================== 章节目录接口 ====================

/**
 * 批量查询章节基础信息
 * @param {number[]} ids - 章节ID列表
 * @returns {Promise} 章节信息列表
 */
export function batchQueryCatalogs(ids) {
  return request({
    url: '/catalogues/batchQuery',
    method: 'get',
    params: { ids: ids.join(',') }
  })
}

/**
 * 获取小节信息
 * @param {number} id - 小节ID
 * @returns {Promise} 小节信息
 */
export function getSectionInfo(id) {
  return request({
    url: `/catalogues/querySectionInfoById/${id}`,
    method: 'get'
  })
}

/**
 * 通过老师ID获取老师负责的课程和出题数量
 * @param {number[]} teacherIds - 老师ID列表
 * @returns {Promise} 课程和出题数量
 */
export function getCourseInfoByTeacherIds(teacherIds) {
  return request({
    url: '/course/infoByTeacherIds',
    method: 'get',
    params: { teacherIds: teacherIds.join(',') }
  })
}

/**
 * 根据小节ID获取小节信息
 * @param {number} id - 小节ID
 * @returns {Promise} 小节信息
 */
export function getSectionById(id) {
  return request({
    url: `/course/section/${id}`,
    method: 'get'
  })
}

/**
 * 根据媒资ID列表查询媒资被引用的次数
 * @param {number[]} mediaIds - 媒资ID列表
 * @returns {Promise} 引用次数
 */
export function getMediaUsageInfo(mediaIds) {
  return request({
    url: '/course/media/useInfo',
    method: 'get',
    params: { mediaIds: mediaIds.join(',') }
  })
}

/**
 * 课程上架时查询课程信息
 * @param {number} id - 课程ID
 * @returns {Promise} 课程信息
 */
export function getCourseSearchInfo(id) {
  return request({
    url: `/course/${id}/searchInfo`,
    method: 'get'
  })
}

// ==================== 课程状态常量 ====================

/**
 * 课程状态
 */
export const COURSE_STATUS = {
  DRAFT: 0,      // 草稿
  PUBLISHED: 1,  // 已上架
  UNPUBLISHED: 2, // 已下架
  DELETED: 3     // 已删除
}

/**
 * 获取课程状态文本
 * @param {number} status - 状态码
 * @returns {string} 状态文本
 */
export function getCourseStatusText(status) {
  const statusMap = {
    [COURSE_STATUS.DRAFT]: '草稿',
    [COURSE_STATUS.PUBLISHED]: '已上架',
    [COURSE_STATUS.UNPUBLISHED]: '已下架',
    [COURSE_STATUS.DELETED]: '已删除'
  }
  return statusMap[status] || '未知'
}
