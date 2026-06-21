/**
 * 题库管理API
 * 对应后端Controller: QuestionController.java, QuestionBizController.java
 * 基础路径: /questions, /question-biz
 */
import request from '@/utils/request'

// ==================== 题目管理接口 ====================

/**
 * 新增题目
 * @param {Object} data - 题目信息
 * @returns {Promise} 题目ID
 */
export function addQuestion(data) {
  return request({
    url: '/questions',
    method: 'post',
    data
  })
}

/**
 * 修改题目
 * @param {number} id - 题目ID
 * @param {Object} data - 题目信息
 * @returns {Promise} 操作结果
 */
export function updateQuestion(id, data) {
  return request({
    url: `/questions/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除题目
 * @param {number} id - 题目ID
 * @returns {Promise} 操作结果
 */
export function deleteQuestion(id) {
  return request({
    url: `/questions/${id}`,
    method: 'delete'
  })
}

/**
 * 分页查询题目
 * @param {Object} params - 查询参数
 * @param {number} params.pageNo - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {string} params.name - 题目名称（可选）
 * @param {number} params.type - 题目类型（可选）
 * @returns {Promise} 分页数据
 */
export function getQuestionPage(params) {
  return request({
    url: '/questions/page',
    method: 'get',
    params
  })
}

/**
 * 查询题目详情
 * @param {number} id - 题目ID
 * @returns {Promise} 题目详情
 */
export function getQuestionDetail(id) {
  return request({
    url: `/questions/${id}`,
    method: 'get'
  })
}

/**
 * 根据ID列表查询题目
 * @param {number[]} ids - 题目ID列表
 * @returns {Promise} 题目列表
 */
export function getQuestionList(ids) {
  return request({
    url: '/questions/list',
    method: 'get',
    params: { ids: ids.join(',') }
  })
}

/**
 * 查询题目分值
 * @param {number[]} ids - 题目ID列表
 * @returns {Promise} 分值映射
 */
export function getQuestionScores(ids) {
  return request({
    url: '/questions/scores',
    method: 'get',
    params: { ids: ids.join(',') }
  })
}

/**
 * 查询老师出题数量
 * @param {number[]} teacherIds - 老师ID列表
 * @returns {Promise} 出题数量
 */
export function countTeacherQuestions(teacherIds) {
  return request({
    url: '/questions/numOfTeacher',
    method: 'get',
    params: { teacherIds: teacherIds.join(',') }
  })
}

/**
 * 查询业务关联的题目列表
 * @param {number} bizId - 业务ID
 * @param {string} bizType - 业务类型
 * @returns {Promise} 题目列表
 */
export function getQuestionsByBiz(bizId, bizType) {
  return request({
    url: '/questions/listOfBiz',
    method: 'get',
    params: { bizId, bizType }
  })
}

/**
 * 校验题目名称是否有效
 * @param {string} name - 题目名称
 * @param {number} id - 题目ID（编辑时传入）
 * @returns {Promise} 是否有效
 */
export function checkQuestionName(name, id) {
  return request({
    url: '/questions/checkName',
    method: 'get',
    params: { name, id }
  })
}

// ==================== 题目业务关联接口 ====================

/**
 * 批量保存题目业务关系
 * @param {Object[]} data - 业务关系列表
 * @returns {Promise} 操作结果
 */
export function saveQuestionBizBatch(data) {
  return request({
    url: '/question-biz/list',
    method: 'post',
    data
  })
}

/**
 * 查询业务关联的题目ID列表
 * @param {number} bizId - 业务ID
 * @returns {Promise} 题目ID列表
 */
export function getQuestionIdsByBiz(bizId) {
  return request({
    url: `/question-biz/biz/${bizId}`,
    method: 'get'
  })
}

/**
 * 批量查询业务关联的题目ID
 * @param {number[]} bizIds - 业务ID列表
 * @returns {Promise} 题目ID映射
 */
export function getQuestionIdsByBizIds(bizIds) {
  return request({
    url: '/question-biz/biz/list',
    method: 'get',
    params: { bizIds: bizIds.join(',') }
  })
}

/**
 * 查询题目分数和
 * @param {number[]} bizIds - 业务ID列表
 * @returns {Promise} 分数和
 */
export function getQuestionScoreSum(bizIds) {
  return request({
    url: '/question-biz/scores',
    method: 'get',
    params: { bizIds: bizIds.join(',') }
  })
}

// ==================== 题目类型常量 ====================

/**
 * 题目类型
 */
export const QUESTION_TYPE = {
  SINGLE_CHOICE: 1,   // 单选题
  MULTI_CHOICE: 2,    // 多选题
  JUDGE: 3,           // 判断题
  FILL_BLANK: 4,      // 填空题
  SHORT_ANSWER: 5     // 简答题
}

/**
 * 题目难度
 */
export const QUESTION_DIFFICULTY = {
  EASY: 1,      // 简单
  MEDIUM: 2,    // 中等
  HARD: 3       // 困难
}

/**
 * 获取题目类型文本
 * @param {number} type - 类型码
 * @returns {string} 类型文本
 */
export function getQuestionTypeText(type) {
  const typeMap = {
    [QUESTION_TYPE.SINGLE_CHOICE]: '单选题',
    [QUESTION_TYPE.MULTI_CHOICE]: '多选题',
    [QUESTION_TYPE.JUDGE]: '判断题',
    [QUESTION_TYPE.FILL_BLANK]: '填空题',
    [QUESTION_TYPE.SHORT_ANSWER]: '简答题'
  }
  return typeMap[type] || '未知'
}

/**
 * 获取题目难度文本
 * @param {number} difficulty - 难度码
 * @returns {string} 难度文本
 */
export function getQuestionDifficultyText(difficulty) {
  const difficultyMap = {
    [QUESTION_DIFFICULTY.EASY]: '简单',
    [QUESTION_DIFFICULTY.MEDIUM]: '中等',
    [QUESTION_DIFFICULTY.HARD]: '困难'
  }
  return difficultyMap[difficulty] || '未知'
}
