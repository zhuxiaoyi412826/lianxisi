# 🎓 智能考试系统

基于 Spring Boot + Vue 3 开发的智能考试系统，支持 AI 自动判卷、题库管理、在线考试等功能。

## 📁 项目结构

```
lianxisi/
├── exam-system-server/          # 后端服务（Spring Boot）
│   ├── src/main/java/com/exam/
│   │   ├── controller/          # REST API 控制器
│   │   ├── service/             # 业务逻辑层
│   │   ├── mapper/              # MyBatis Plus 数据访问层
│   │   ├── entity/              # 数据库实体
│   │   ├── dto/                 # 数据传输对象
│   │   ├── config/              # 配置类
│   │   ├── common/              # 通用组件
│   │   ├── utils/               # 工具类
│   │   └── vo/                  # 视图对象
│   ├── src/main/resources/
│   │   ├── mapper/              # MyBatis XML 映射文件
│   │   ├── db/migration/        # 数据库迁移脚本
│   │   └── application.yml      # 应用配置
│   └── pom.xml                  # Maven 依赖管理
└── exam-system-web-backup/      # 前端应用（Vue 3）
    ├── src/
    │   ├── api/                 # API 接口封装
    │   ├── router/              # 路由配置
    │   ├── views/               # 页面组件
    │   └── utils/               # 工具函数
    ├── index.html
    ├── package.json
    └── vite.config.js           # Vite 配置
```

## 🛠️ 技术栈

### 后端
| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.0.5 | 应用框架 |
| Java | 17 | 开发语言 |
| MyBatis Plus | 3.5.3.1 | ORM 框架 |
| MySQL | 8.0.33 | 数据库 |
| Redis | - | 缓存服务 |
| MinIO | 8.5.7 | 对象存储 |
| Knife4j | 4.4.0 | API 文档 |
| Kimi AI | moonshot-v1-32k | AI 判卷服务 |
| Spring Mail | - | 邮件服务 |

### 前端
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.3.4 | 前端框架 |
| Element Plus | 2.3.8 | UI 组件库 |
| Vue Router | 4.2.4 | 路由管理 |
| Pinia | 2.1.6 | 状态管理 |
| Vite | 4.4.5 | 构建工具 |
| ECharts | 5.4.3 | 图表库 |

## 🌟 功能特性

### 📝 核心功能
- **用户管理**：用户注册、登录、信息管理
  - 邮箱验证码登录（自动注册）
  - 短信验证码登录（自动注册）
  - 账号密码登录
  - 自动分配6位学号（从000001开始）
  - 个人信息编辑
  - 头像上传
  - 密码修改
- **题库管理**：题目增删改查、批量导入、分类管理
- **试卷管理**：试卷创建、编辑、发布
- **在线考试**：开始考试、答题、提交、计时
- **AI 判卷**：自动批改主观题，提供评分和评语
- **成绩统计**：考试排名、数据分析、图表展示

### 🎬 视频模块
- 视频上传与管理
- 视频分类
- 视频播放、点赞、观看记录

### 📊 统计分析
- 考试成绩统计
- 用户学习数据
- 题目正确率分析

### 👨‍💼 后台管理
- 用户管理：用户列表、详情、状态管理、删除
- 其他现有管理功能

## 🚀 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Node.js 16+

### 后端启动

1. **进入后端目录**
```bash
cd exam-system-server
```

2. **配置数据库**
   - 创建数据库 `exam_system`
   - 执行数据库初始化脚本：`exam_system_setup.sql`
   - 修改 `src/main/resources/application.yml` 中的数据库配置

3. **配置邮件服务**
   - 已在 `application.yml` 中配置 QQ 邮箱服务
   - 邮箱：2927883590@qq.com
   - 授权码已配置

4. **运行应用**
```bash
mvn spring-boot:run
```

服务启动后访问：
- API 文档：http://localhost:8080/doc.html
- 健康检查：http://localhost:8080/actuator/health

### 前端启动

1. **进入前端目录**
```bash
cd exam-system-web-backup
```

2. **安装依赖**
```bash
npm install
```

3. **启动开发服务器**
```bash
npm run dev
```

前端启动后访问：http://localhost:3001

## 📡 API 接口

### 主要接口模块

| 模块 | 路径 | 说明 |
|------|------|------|
| 用户认证 | `/api/auth` | 用户登录、注册、验证码 |
| 用户 | `/api/user` | 用户信息管理 |
| 考试 | `/api/exam` | 考试管理、答题 |
| 题库 | `/api/question` | 题目管理 |
| 试卷 | `/api/paper` | 试卷管理 |
| 视频 | `/api/video` | 视频管理 |
| 统计 | `/api/stats` | 数据统计 |
| 文件 | `/api/file` | 文件上传 |
| 后台用户 | `/api/admin/user` | 后台用户管理 |

### 用户认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/send-code` | 发送邮箱验证码 |
| POST | `/api/auth/login-email` | 邮箱验证码登录 |
| POST | `/api/auth/send-sms` | 发送短信验证码 |
| POST | `/api/auth/login-sms` | 短信验证码登录 |
| POST | `/api/auth/login-account` | 账号密码登录 |
| GET | `/api/auth/current-user` | 获取当前用户信息 |
| PUT | `/api/auth/update-profile` | 更新用户信息 |
| PUT | `/api/auth/change-password` | 修改密码 |

### 接口文档

启动后端服务后，访问 Knife4j API 文档：
```
http://localhost:8080/doc.html
```

## 🔧 配置说明

### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/exam_system?useUnicode=true&characterEncoding=utf8
    username: root
    password: root
```

### 邮件服务配置
```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 465
    username: 2927883590@qq.com
    password: your-email-auth-code
    properties:
      mail:
        smtp:
          auth: true
          ssl:
            enable: true
          socketFactory:
            class: javax.net.ssl.SSLSocketFactory
            port: 465
```

### Redis 配置
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### Kimi AI 配置
```yaml
kimi:
  api:
    base-url: https://api.moonshot.cn/v1
    api-key: your-api-key
    model: moonshot-v1-32k
```

### MinIO 配置
```yaml
minio:
  endpoint: http://localhost:9000
  bucket-name: exam-bucket
  username: minioadmin
  password: minioadmin
```

### 短信服务配置（阿里云）
```yaml
sms:
  aliyun:
    access-key-id: your-access-key-id
    access-key-secret: your-access-key-secret
    sign-name: 智能学习平台
    template-code: SMS_123456789
```

## 📄 数据库初始化

数据库初始化脚本：
```
d:/1/lianxisi/exam_system_setup.sql
```

在 MySQL 中执行该脚本以创建必要的表：
- `denglu` - 用户登录信息表
- `email_verification` - 邮箱验证码表
- `sms_operation_log` - 短信操作日志表（用于风控审计回溯）

## 📁 项目目录说明

### 后端目录结构
```
src/main/java/com/exam/
├── controller/     # REST API 控制器
│   ├── AuthController.java      # 用户认证接口
│   ├── AdminUserController.java # 后台用户管理接口
│   ├── UserController.java      # 用户管理接口
│   ├── ExamController.java      # 考试管理接口
│   ├── QuestionController.java  # 题库管理接口
│   ├── PaperController.java     # 试卷管理接口
│   ├── VideoController.java     # 视频管理接口
│   └── StatsController.java     # 统计分析接口
├── service/        # 业务逻辑层
│   ├── impl/       # 服务实现类
│   ├── DengluService.java       # 用户登录服务
│   ├── EmailService.java        # 邮件服务
│   └── *.java      # 其他服务接口
├── mapper/         # MyBatis Plus 数据访问层
│   ├── DengluMapper.java
│   ├── EmailVerificationMapper.java
│   └── *.java
├── entity/         # 数据库实体类
│   ├── Denglu.java
│   ├── EmailVerification.java
│   └── *.java
├── dto/            # 数据传输对象
│   └── LoginDTO.java
├── config/         # 配置类（Redis、CORS、MinIO等）
├── common/         # 通用组件（Result、CacheConstants）
└── utils/          # 工具类（ExcelUtil、RedisUtils、IpUtils）
```

### 前端目录结构
```
src/
├── api/            # API 接口封装
├── router/         # 路由配置
├── views/          # 页面组件
│   ├── Login.vue               # 登录页面
│   ├── UserProfile.vue         # 个人中心
│   ├── UserManage.vue          # 后台用户管理
│   └── *.vue
├── utils/          # 工具函数
└── main.js         # 入口文件
```

## 💡 使用说明

### 用户登录
1. 访问首页，点击"登录"按钮
2. 选择"邮箱验证码登录"或"账号密码登录"
3. 邮箱登录：输入邮箱 → 点击"获取验证码" → 输入验证码 → 登录
4. 新用户会自动注册并分配6位学号（从000001开始）

### 个人中心
- 查看和编辑个人信息
- 上传头像
- 修改密码
- 退出登录

### 后台用户管理
- 查看用户列表（支持分页和搜索）
- 查看用户详情
- 修改用户状态（启用/禁用/封禁）
- 删除用户

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 📞 联系方式

如有问题或建议，请通过以下方式联系：
- 项目地址：https://github.com/your-repo/exam-system
