# RK-Web 前端接口联调测试计划

## 测试环境
- 前端地址: `http://localhost:5173`
- 后端网关: `http://localhost:10010`
- 测试页面: `http://localhost:5173/test-api`

## 测试账号
```
用户名: <your-admin-user>
密码: <your-admin-password>
```

## 测试模块

### 1. 用户认证模块
- [ ] 用户登录 (`POST /auth/login`)
- [ ] 用户信息获取 (`GET /users/me`)
- [ ] 用户登出 (`POST /auth/logout`)
- [ ] Token 自动刷新
- [ ] 401 未授权自动跳转

### 2. 新闻管理模块
- [ ] 获取新闻列表 (`GET /api/news/list`)
- [ ] 获取最新新闻 (`GET /api/news/latest`)
- [ ] 获取置顶新闻 (`GET /api/news/top`)
- [ ] 根据分类获取新闻 (`GET /api/news/category/{category}`)
- [ ] 搜索新闻 (`GET /api/news/search`)
- [ ] 获取新闻详情 (`GET /api/news/{id}`)
- [ ] 添加新闻 (`POST /api/news/add`)
- [ ] 更新新闻 (`PUT /api/news/update`)
- [ ] 删除新闻 (`DELETE /api/news/delete/{id}`)

### 3. 作品展示模块
- [ ] 获取作品列表 (`GET /api/works/list`)
- [ ] 按分类获取作品 (`GET /api/works/category/{category}`)
- [ ] 获取精选作品 (`GET /api/works/featured`)
- [ ] 搜索作品 (`GET /api/works/search`)
- [ ] 获取作品详情 (`GET /api/works/{id}`)
- [ ] 点赞作品 (`POST /api/works/{id}/like`)
- [ ] 取消点赞 (`POST /api/works/{id}/unlike`)
- [ ] 获取热门作品 (`GET /api/works/popular`)
- [ ] 获取最新作品 (`GET /api/works/latest`)
- [ ] 获取作品统计 (`GET /api/works/statistics`)

### 4. 校友管理模块
- [ ] 获取校友列表 (`GET /api/alumni/list`)
- [ ] 根据ID获取校友 (`GET /api/alumni/{id}`)
- [ ] 根据届数获取校友 (`GET /api/alumni/generation/{year}`)
- [ ] 根据毕业状态获取校友 (`GET /api/alumni/status/{graduationStatus}`)
- [ ] 根据部门获取校友 (`GET /api/alumni/department/{department}`)
- [ ] 搜索校友 (`GET /api/alumni/search`)
- [ ] 添加校友 (`POST /api/alumni/add`)
- [ ] 更新校友 (`PUT /api/alumni/update`)
- [ ] 删除校友 (`DELETE /api/alumni/delete/{id}`)
- [ ] 获取毕业状态统计 (`GET /api/alumni/statistics`)
- [ ] 获取各届统计 (`GET /api/alumni/generation-statistics`)
- [ ] 获取分组校友 (`GET /api/alumni/grouped-by-generation`)
- [ ] 获取校友概览 (`GET /api/alumni/overview`)
- [ ] Excel导入校友 (`POST /api/alumni/import-excel`)

### 5. 活动管理模块
- [ ] 获取活动列表 (`GET /api/activities/list`)
- [ ] 获取活动详情 (`GET /api/activities/{id}`)
- [ ] 报名参加活动 (`POST /api/activities/{id}/join`)
- [ ] 取消报名 (`POST /api/activities/{id}/cancel`)
- [ ] 获取我报名的活动 (`GET /api/activities/my`)

### 6. 多租户数据隔离
- [ ] 切换租户 ID
- [ ] 验证不同租户数据隔离
- [ ] 验证请求头 `Tenant-Id` 正确传递

## 测试步骤

### 步骤 1: 启动前端开发服务器
```bash
cd newpro/rk
npm run dev
```

### 步骤 2: 访问测试页面
打开浏览器访问: `http://localhost:5173/test-api`

### 步骤 3: 测试登录
1. 输入本地初始化后的管理员用户名
2. 输入本地初始化后的管理员密码
3. 点击"测试登录"按钮
4. 验证登录成功，获取 Token

### 步骤 4: 测试各模块 API
1. 点击"获取新闻列表"按钮
2. 点击"获取作品列表"按钮
3. 点击"获取校友列表"按钮
4. 查看返回的数据格式和内容

### 步骤 5: 测试多租户隔离
1. 输入租户 ID: `1`
2. 点击"切换租户"按钮
3. 重新获取数据，验证数据隔离

## 预期结果

### 登录接口响应
```json
{
  "code": 200,
  "data": {
    "token": "<JWT_TOKEN>",
    "userInfo": {
      "id": 1,
      "username": "<your-admin-user>",
      "realName": "管理员"
    }
  },
  "msg": null
}
```

### 新闻列表响应
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "title": "RK-Web社团举办年度技术交流会",
      "category": "活动",
      "author": "<your-admin-user>",
      "status": "1",
      "viewCount": 100,
      "publishTime": "2024-01-15 10:00:00"
    }
  ],
  "msg": null
}
```

## 常见问题排查

### 1. 登录失败 (404)
- 检查网关路由配置
- 确认认证服务已启动

### 2. Token 无效 (401)
- 检查 Token 是否过期
- 检查 Authorization 请求头格式

### 3. 多租户隔离不生效
- 检查 localStorage 中是否有 `tenant_id`
- 检查请求拦截器是否正确添加 `Tenant-Id` 请求头

### 4. CORS 跨域问题
- 检查 vite.config.js 代理配置
- 检查后端网关跨域配置

## 测试完成标准

- ✅ 所有核心接口返回正确数据
- ✅ 登录认证流程正常
- ✅ 多租户数据隔离生效
- ✅ 错误处理正常显示
- ✅ Token 自动刷新机制工作
