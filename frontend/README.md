# 前端模块说明（Vue 3 + Tailwind CSS）

当前前端已包含两个主要模块：

- 管理后台（登录 + 视频审核 + 举报管理）
- 视频投稿页面（原有）

## 管理后台路由

- `/manager/login`：管理员登录页（无游客入口）
- `/manager`：管理后台主页（受登录保护）

未登录或 token 失效访问 `/manager` 会自动跳转到 `/manager/login`。

## 已对接的后台管理接口

- `POST /graduation-project/user/login`
- `POST /graduation-project/user/logout`
- `POST /graduation-project/manager/video/audit/list`
- `POST /graduation-project/manager/video/audit`
- `POST /graduation-project/manager/report/list`
- `POST /graduation-project/manager/report/review`
- `POST /graduation-project/manager/user/ban/{userId}`
- `POST /graduation-project/manager/user/unban/{userId}`
- `POST /graduation-project/manager/video/ban/{videoId}`
- `POST /graduation-project/manager/video/unban/{videoId}`

## 快速运行

```bash
npm install
npm run dev
```

默认开发地址：`http://localhost:5173`

## 构建验证

```bash
npm run build
```

## 关键文件

- 路由与守卫：`src/router/index.js`
- 登录页：`src/components/manager/ManagerLoginPage.vue`
- 管理主页：`src/components/ManagerPage.vue`
- 视频审核页：`src/components/manager/ManagerVideoAuditTab.vue`
- 举报管理页：`src/components/manager/ManagerReportTab.vue`
- 通用请求：`src/api/http.js`
- 管理接口：`src/api/manager.js`
- 用户接口：`src/api/user.js`
- 登录态工具：`src/utils/auth.js`

# 视频投稿页面（Vue 3 + Tailwind CSS）

该目录是一个最小可运行前端，提供了参考 B 站布局的视频投稿页面组件，接口已对齐后端：

- `POST /graduation-project/upload/multipart/new`
- `POST /graduation-project/upload/multipart/part`
- `POST /graduation-project/upload/multipart/complete`
- `POST /graduation-project/upload/image`
- `POST /graduation-project/video/submit`
- `POST /graduation-project/video/publish/{videoId}`

## 功能点

- 分片上传视频（默认分片 5MB）
- 自动读取视频时长并回填 `duration`
- 上传封面并回填 `coverUrl`
- 按后端 `VideoSubmitRequest` 发送投稿参数：
  - `videoId`
  - `title`
  - `description`
  - `coverUrl`
  - `duration`
  - `partitionId`
  - `tagList`
- 提交审核（发布）请求：`/video/publish/{videoId}`
- 日志面板展示交互过程和错误信息

## 运行

```bash
npm install
npm run dev
```

默认开发地址：`http://localhost:5173`

## 后端联调说明

- 默认通过 Vite 代理转发 `/graduation-project` 到 `http://localhost:8080`
- 登录 token 会放在请求头 `token`（与后端拦截器一致）
- 如果后端地址不是 `8080`，可设置环境变量：

```bash
VITE_API_BASE_URL=http://your-host:port npm run dev
```

## 组件位置

- 页面组件：`src/components/VideoSubmitPage.vue`
- 入口：`src/App.vue`
