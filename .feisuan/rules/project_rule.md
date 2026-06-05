
# 开发规范指南

为保证代码质量、可维护性、安全性与可扩展性，请在开发过程中严格遵循以下规范。

## 一、项目基础信息

- **项目名称**：lianxisi
- **工作目录**：`D:\1\lianxisi`
- **代码作者**：zhuxiaoyi
- **操作系统**：Windows 10
- **当前时间**：2026-06-06 07:01:12

## 二、技术栈与构建工具

- **主框架**：Spring Boot 3.x
- **语言版本**：JDK 17.0.15
- **构建工具**：Maven
- **核心依赖**：
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `lombok`
  - MyBatis-Plus (根据目录结构推断存在 `mapper` 目录)

## 三、目录结构规范

项目采用多模块或单体结构，主要包含后端服务 `exam-system-server` 和前端备份 `exam-system-web-backup`。后端核心目录结构如下：

```text
exam-system-server
├── src
│   └── main
│       ├── java
│       │   └── com
│       │       └── exam
│       │           ├── common          # 通用工具、常量、异常处理
│       │           ├── config          # 配置类（WebMvc, Security, DB等）
│       │           ├── controller      # 控制器层
│       │           ├── dto             # 数据传输对象
│       │           │   └── ai          # AI相关DTO
│       │           ├── entity          # 数据库实体
│       │           ├── interceptor     # 拦截器
│       │           ├── mapper          # 数据访问层 (MyBatis Mapper)
│       │           ├── service         # 业务逻辑接口
│       │           │   └── impl        # 业务逻辑实现
│       │           ├── utils           # 工具类
│       │           └── vo              # 视图展示对象
│       └── resources
│           ├── db
│           │   └── migration           # 数据库迁移脚本 (Flyway/Liquibase)
│           └── mapper                  # MyBatis XML映射文件
├── agent                             # 代理或独立模块
└── exam-system-web-backup            # 前端备份目录
    ├── public
    └── src
        ├── api                       # 接口请求
        ├── assets                    # 静态资源
        ├── components                # 公共组件
        ├── router                    # 路由配置
        ├── utils                     # 前端工具
        └── views                     # 页面视图
```

## 四、分层架构规范

| 层级        | 职责说明                         | 开发约束与注意事项                                               |
|-------------|----------------------------------|----------------------------------------------------------------|
| **Controller** | 处理 HTTP 请求与响应，定义 API 接口 | 不得直接访问数据库，必须通过 Service 层调用；需进行参数校验 |
| **Service**    | 实现业务逻辑、事务管理与数据校验   | 接口定义在 `service` 包，实现类在 `service.impl` 包；必须通过 Mapper/Repository 层访问数据库；返回 DTO 而非 Entity |
| **Mapper**     | 数据库访问与持久化操作 (MyBatis)   | 继承 `BaseMapper` 或自定义 XML；避免在 Java 代码中硬编码 SQL，优先使用 XML 或 JPA 注解 |
| **Entity**     | 映射数据库表结构                   | 不得直接返回给前端（需转换为 DTO）；包名统一为 `entity` |

### 接口与实现分离

- 所有接口实现类需放在接口所在包下的 `impl` 子包中（例如 `com.exam.service.impl`）。

## 五、安全与性能规范

### 输入校验

- 使用 `@Valid` 与 JSR-303 校验注解（如 `@NotBlank`, `@Size` 等）。
  - **注意**：Spring Boot 3.x 中校验注解位于 `jakarta.validation.constraints.*` 包下。
- 禁止手动拼接 SQL 字符串，防止 SQL 注入攻击。

### 事务管理

- `@Transactional` 注解仅用于 **Service 层**方法。
- 避免在循环中频繁提交事务，影响性能。

## 六、代码风格规范

### 命名规范

| 类型       | 命名方式             | 示例                  |
|------------|----------------------|-----------------------|
| 类名       | UpperCamelCase       | `UserServiceImpl`     |
| 方法/变量  | lowerCamelCase       | `saveUser()`          |
| 常量       | UPPER_SNAKE_CASE     | `MAX_LOGIN_ATTEMPTS`  |

### 注释规范

- **语言要求**：所有注释必须使用**中文**编写。
- **规范**：所有类、方法、字段需添加 **Javadoc** 注释，说明功能、参数及返回值。

### 类型命名规范（阿里巴巴风格）

| 后缀 | 用途说明                     | 示例         |
|------|------------------------------|--------------|
| DTO  | 数据传输对象                 | `UserDTO`    |
| DO   | 数据库实体对象 (若使用MyBatis) | `UserDO`     |
| BO   | 业务逻辑封装对象             | `UserBO`     |
| VO   | 视图展示对象                 | `UserVO`     |
| Query| 查询参数封装对象             | `UserQuery`  |

### 实体类简化工具

- 使用 Lombok 注解替代手动编写 getter/setter/构造方法：
  - `@Data`
  - `@NoArgsConstructor`
  - `@AllArgsConstructor`

## 七、扩展性与日志规范

### 接口优先原则

- 所有业务逻辑通过接口定义（如 `UserService`），具体实现放在 `impl` 包中（如 `UserServiceImpl`）。

### 日志记录

- 使用 `@Slf4j` 注解代替 `System.out.println`。
- 日志级别遵循：ERROR（错误）、WARN（警告）、INFO（信息）、DEBUG（调试）。

## 八、编码原则总结

| 原则       | 说明                                       |
|------------|--------------------------------------------|
| **SOLID**  | 高内聚、低耦合，增强可维护性与可扩展性     |
| **DRY**    | 避免重复代码，提高复用性                   |
| **KISS**   | 保持代码简洁易懂                           |
| **YAGNI**  | 不实现当前不需要的功能                     |
| **OWASP**  | 防范常见安全漏洞，如 SQL 注入、XSS 等      |
