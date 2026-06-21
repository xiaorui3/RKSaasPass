# RK-Web 后端 API 接口文档

> 版本：v3.0
> 更新日期：2026-02-22
> 基础URL：`/api`

## 目录

- [通用说明](#通用说明)
- [新闻模块](#新闻模块)
- [作品模块](#作品模块)
- [比赛模块](#比赛模块)
- [校友模块](#校友模块)
- [社团申请模块](#社团申请模块)
- [联系我们模块](#联系我们模块)
- [用户模块](#用户模块)

---

## 通用说明

### 请求头

```
Content-Type: application/json
Authorization: Bearer {token}  // 需要认证的接口
```

### 响应格式

#### 成功响应

```json
{
  "code": 200,
  "data": { ... },
  "msg": null
}
```

#### 错误响应

```json
{
  "code": 400,
  "data": null,
  "msg": "错误信息"
}
```

---

## 新闻模块

**基础路径**: `/api/news`

### 1. 获取最新新闻

**接口地址：** `GET /api/news/latest`

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| limit | Integer | 否 | 数量限制，默认5 |

**响应示例：**
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "title": "新闻标题",
      "summary": "新闻摘要",
      "content": "新闻内容",
      "category": "活动",
      "image": "图片URL",
      "viewCount": 100,
      "publishTime": "2024-01-15"
    }
  ]
}
```

### 2. 获取置顶新闻

**接口地址：** `GET /api/news/top`

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| limit | Integer | 否 | 数量限制，默认3 |

### 3. 根据分类获取新闻

**接口地址：** `GET /api/news/category/{category}`

**路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| category | String | 是 | 分类名称 |

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| limit | Integer | 否 | 数量限制，默认10 |

### 4. 搜索新闻

**接口地址：** `GET /api/news/search`

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 是 | 搜索关键词 |
| limit | Integer | 否 | 数量限制，默认20 |

### 5. 获取新闻详情

**接口地址：** `GET /api/news/{id}`

**路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 新闻ID |

### 6. 分页获取新闻列表

**接口地址：** `GET /api/news`

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页数量，默认10 |
| category | String | 否 | 分类筛选 |
| search | String | 否 | 搜索关键词 |

**响应示例：**
```json
{
  "code": 200,
  "data": {
    "records": [...],
    "total": 100,
    "current": 1,
    "size": 10,
    "pages": 10
  }
}
```

### 7. 添加新闻 (需认证)

**接口地址：** `POST /api/news/add`

### 8. 修改新闻 (需认证)

**接口地址：** `PUT /api/news/update`

### 9. 删除新闻 (需认证)

**接口地址：** `DELETE /api/news/delete/{id}`

---

## 作品模块

**基础路径**: `/api/works`

### 1. 获取所有作品

**接口地址：** `GET /api/works/list`

### 2. 按分类获取作品

**接口地址：** `GET /api/works/category/{category}`

### 3. 获取精选作品

**接口地址：** `GET /api/works/featured`

### 4. 搜索作品

**接口地址：** `GET /api/works/search`

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| title | String | 否 | 标题关键词 |
| category | String | 否 | 分类 |

### 5. 获取作品详情

**接口地址：** `GET /api/works/{id}`

### 6. 点赞作品

**接口地址：** `POST /api/works/{id}/like`

### 7. 取消点赞

**接口地址：** `POST /api/works/{id}/unlike`

### 8. 获取热门作品

**接口地址：** `GET /api/works/popular`

### 9. 获取最新作品

**接口地址：** `GET /api/works/latest`

### 10. 获取作品统计信息

**接口地址：** `GET /api/works/statistics`

---

## 比赛模块

**基础路径**: `/api/competition`

### 1. 获取所有比赛

**接口地址：** `GET /api/competition/list`

### 2. 获取已发布的比赛

**接口地址：** `GET /api/competition/published`

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sortBy | String | 否 | 排序字段，默认create_time |
| sortOrder | String | 否 | 排序方向，默认desc |

### 3. 获取推荐比赛

**接口地址：** `GET /api/competition/featured`

### 4. 根据状态获取比赛

**接口地址：** `GET /api/competition/status/{status}`

**路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | String | 是 | 状态：ongoing/upcoming/ended |

### 5. 获取比赛详情

**接口地址：** `GET /api/competition/{id}`

### 6. 创建比赛 (需认证)

**接口地址：** `POST /api/competition`

### 7. 更新比赛 (需认证)

**接口地址：** `PUT /api/competition/{id}`

### 8. 删除比赛 (需认证)

**接口地址：** `DELETE /api/competition/{id}`

### 9. 搜索比赛

**接口地址：** `GET /api/competition/search`

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 是 | 搜索关键词 |

---

## 校友模块

**基础路径**: `/api/alumni`

### 1. 获取所有校友信息

**接口地址：** `GET /api/alumni/list`

### 2. 根据ID获取校友信息

**接口地址：** `GET /api/alumni/{id}`

### 3. 根据届数获取校友信息

**接口地址：** `GET /api/alumni/generation/{year}`

### 4. 根据毕业状态获取校友信息

**接口地址：** `GET /api/alumni/status/{graduationStatus}`

### 5. 根据部门获取校友信息

**接口地址：** `GET /api/alumni/department/{department}`

### 6. 添加校友信息 (需认证)

**接口地址：** `POST /api/alumni/add`

### 7. 更新校友信息 (需认证)

**接口地址：** `PUT /api/alumni/update`

### 8. 删除校友信息 (需认证)

**接口地址：** `DELETE /api/alumni/delete/{id}`

### 9. 获取毕业状态统计

**接口地址：** `GET /api/alumni/statistics`

### 10. 获取各届统计信息

**接口地址：** `GET /api/alumni/generation-statistics`

### 11. 搜索校友

**接口地址：** `GET /api/alumni/search`

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 是 | 搜索关键词 |

### 12. 获取按届数分组的校友数据

**接口地址：** `GET /api/alumni/grouped-by-generation`

### 13. 获取校友数据概览

**接口地址：** `GET /api/alumni/overview`

### 14. Excel导入校友信息 (需认证)

**接口地址：** `POST /api/alumni/import-excel`

**请求类型：** `multipart/form-data`

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | Excel文件(.xlsx, .xls)或CSV文件 |

---

## 社团申请模块

**基础路径**: `/api/members`

### 1. 提交社团加入申请

**接口地址：** `POST /api/members/apply`

**请求体：**
```json
{
  "studentId": "20200101",
  "name": "张三",
  "className": "计算机2001班",
  "phone": "13800138000",
  "email": "zhangsan@example.com",
  "department": "技术部",
  "introduction": "自我介绍",
  "reason": "申请理由"
}
```

### 2. 获取所有申请列表

**接口地址：** `GET /api/members/list`

### 3. 检查用户申请状态

**接口地址：** `GET /api/members/check-user-application`

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| studentId | String | 是 | 学号 |

### 4. 审核申请 (需认证)

**接口地址：** `POST /api/members/review/{id}`

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| agreeStatus | Integer | 是 | 审核状态：1通过，0拒绝 |
| reviewComment | String | 否 | 审核意见 |

### 5. 获取申请详情

**接口地址：** `GET /api/members/{id}`

### 6. 删除申请 (需认证)

**接口地址：** `DELETE /api/members/{id}`

### 7. 获取申请统计数据

**接口地址：** `GET /api/members/statistics`

---

## 联系我们模块

**基础路径**: `/api/contact`

### 1. 提交联系消息（JSON格式）

**接口地址：** `POST /api/contact/submit`

**请求体：**
```json
{
  "name": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "subject": "咨询主题",
  "message": "消息内容"
}
```

### 2. 提交联系消息（表单格式）

**接口地址：** `POST /api/contact/submit-form`

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 姓名 |
| email | String | 是 | 邮箱 |
| phone | String | 否 | 电话 |
| subject | String | 是 | 主题 |
| message | String | 是 | 消息内容 |

---

## 用户模块

**基础路径**: `/api/user`

### 1. 用户登录

**接口地址：** `POST /api/auth/login`

**请求体：**
```json
{
  "username": "user001",
  "password": "123456"
}
```

### 2. 用户注册

**接口地址：** `POST /api/auth/register`

### 3. 获取用户信息 (需认证)

**接口地址：** `GET /api/user/info`

### 4. 更新用户信息 (需认证)

**接口地址：** `PUT /api/user/info`

### 5. 用户登出 (需认证)

**接口地址：** `POST /api/auth/logout`

---

## 数据字典

### 新闻分类

| 值 | 说明 |
|----|------|
| 活动 | 社团活动相关 |
| 荣誉 | 获奖信息 |
| 招新 | 招新活动 |
| 培训 | 技术培训 |
| 通知 | 通知公告 |

### 作品分类

| 值 | 说明 |
|----|------|
| Web应用 | Web应用程序 |
| 移动应用 | 移动端应用 |
| 桌面应用 | 桌面应用 |
| 数据分析 | 数据分析项目 |
| 人工智能 | AI相关项目 |

### 比赛状态

| 值 | 说明 |
|----|------|
| upcoming | 即将开始 |
| ongoing | 进行中 |
| ended | 已结束 |

### 审核状态

| 值 | 说明 |
|----|------|
| 0 | 待审核 |
| 1 | 已通过 |
| 2 | 已拒绝 |

### 毕业状态

| 值 | 说明 |
|----|------|
| 在读 | 仍在读学生 |
| 已毕业 | 已毕业校友 |

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权，token无效或过期 |
| 403 | 禁止访问，权限不足 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 500 | 服务器内部错误 |

---

## 更新日志

### v3.0 (2026-02-22)
- 完善所有后端API接口文档
- 添加校友模块接口
- 添加社团申请模块接口
- 添加联系我们模块接口
- 更新接口路径与后端保持一致

---
