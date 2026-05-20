# CLAUDE.md — 代事猫 (DaiShi Mao)

校园微任务撮合平台 MVP。学生发跑腿任务，其他学生接单完成，线下自行结算。

## 当前进度（2026-05-19）

| Feature | 状态 | 完成内容 |
|---------|------|----------|
| **F1 任务发布** | ✅ 完成 | TaskController + TaskService + ContentFilterService + 敏感词过滤 + Publish 页面 |
| **F2 任务大厅与接单** | ✅ 完成 | GET /api/tasks 列表 + GET /api/tasks/{id} 详情 + PUT /api/tasks/{id}/accept 接单 + 乐观锁防抢 |
| **F3 任务状态流转** | ✅ 完成 | start/complete/confirm/cancel + 取消扣分(发单-3/接单-5) + 超时自动释放(30min)+自动确认(24h) + EventPublisher 加固 |
| **F4 信用评价** | ✅ 完成 | ReviewController + ReviewService + CreditService + 互评(发布人↔接单人) + 评分映射信用分(1★=-5~5★=+5) + 信用<30冻结 + TaskDetail 评价面板 |
| F5 事件通知 | ⬜ 待开发 | Redis Stream 消费者 |

**下次对话：从 F5 开始。**

## 2026-05-19 变更总结

### Bug 修复（8 个）
- F3 遗留: #6 cancelTask PENDING 不计数、#7 User @Version 乐观锁、#15 @Transactional 自调用
- F4 引入: #2 SecurityConfig wildcard `/**`→`/*` 防止匿名 /check 崩溃
- 通用遗留: #1 Scheduler updateById 返回值检查、#4 MyTasks API 缺失、#5 PUT /user/profile 缺失、#6 异常不记日志、#7 RuntimeException→BusinessException(403)、#8 isNewUser @JsonProperty

### 配置修复
- `application.yml`: JWT secret 默认值改为 64 字符有效密钥
- `spring.config.import: optional:classpath:application-secret.yml` — 敏感配置外置
- `application-secret.yml` 已加入 .gitignore

### 前端适配
- BottomNav → TopNav: 顶部导航栏，PC 端友好
- 页面 max-w-lg → max-w-4xl (896px)
- 修复登录后 localStorage 写入时序导致 isLoggedIn 不生效

### 关键文件
- 后端 40+ Java 文件，前端 22+ TS/TSX 文件
- 测试: 58 个，全部通过
- 新增 UserController, ReviewController, CreditService, ReviewService
- TopNav.tsx 替代 BottomNav.tsx

### 环境注意事项

- 系统默认 JDK 8，项目需要 JDK 17+
- JDK 21 路径：`C:\Program Files\Java\新jdk-21`
- 编译需指定：`JAVA_HOME="/c/Program Files/Java/新jdk-21" mvn compile`
- dev 环境无需 MySQL/Redis 即可编译，但运行需要

## 项目结构

```
daisimao/
├── server/          # Spring Boot 3 后端
├── web-app/         # 用户端移动 Web（React + Vite + Tailwind）
├── web-admin/       # React Web 管理后台（Vite + Ant Design）
└── CLAUDE.md        # 本文件
```

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.3, JDK 17, MyBatis-Plus 3.5, MySQL 8.0, Redis 7 |
| 消息队列 | Redis Stream（MVP 阶段，后期迁 RocketMQ） |
| 用户端 | React 18, TypeScript strict, Vite 6, Tailwind CSS 3, React Router 6 |
| Web 后台 | React 18, TypeScript strict, Vite 6, Ant Design 5, React Router 6 |
| 安全 | Spring Security + JWT (jjwt 0.12), 用户名登录（测试阶段，后续加手机验证） |
| 部署 | Docker Compose, Nginx, Ubuntu 22.04 |

## 架构原则

- **单体优先**：日单量 > 1000 再拆分微服务
- **MySQL 是真相源**：Redis 只做缓存和消息通知，不存关键状态
- **不做资金托管**：纯撮合 + 线下支付，平台不碰钱
- **任务状态机**：待接单(1)→已接单(2)→进行中(3)→待确认(4)→已完成(5) / 已取消(6) / 纠纷(7)
- **幂等处理**：Redis Stream 消息可能重复投递，业务层必须幂等（状态机天然幂等）

## 认证流程

- 用户输入用户名 → 后端查找或自动注册 → 返回 JWT
- JWT 7 天有效，后续请求 Header: `Authorization: Bearer <token>`
- 测试阶段仅用户名登录，后续可加手机号+验证码（SmsService 接口已预留）

## 数据库

4 张核心表：`user`, `task`, `review`, `credit_log`
迁移脚本：`server/src/main/resources/db/migration/`
使用 Flyway，文件命名 `V1__init_schema.sql`, `V2__xxx.sql`

User 表关键字段：`username`(唯一), `nickname`, `campus`, `credit_score`, `status`, `phone`（nullable，预留）

## 代码规范

### Java
- 包结构：`controller → service → repository`，业务逻辑只放在 service 层
- Controller 只做参数校验 + 调用 service + 返回 DTO
- 使用 `@Valid` + Jakarta Validation，不要手动校验
- MyBatis 参数化查询用 `#{}`，禁止 `${}`
- 异常：业务异常抛 `BusinessException`，由 `GlobalExceptionHandler` 统一处理
- 日志用 Lombok `@Slf4j`，只记 ERROR 和关键业务节点（INFO）

### TypeScript / React
- Strict mode 开启
- 类型定义集中放在 `src/types/index.ts`
- 用 `data-testid` 做测试选择器，不用 class/id
- 组件文件命名 PascalCase

### Tailwind CSS
- 优先用 Tailwind 原子类，避免自定义 CSS
- 主题色：primary(#1677ff), danger(#ff4d4f), success(#52c41a), warning(#faad14)

## 关键约束

1. **敏感词过滤**：任务发布时必须过 `ContentFilterService`（Trie 树本地匹配）
2. **信用分阈值**：< 60 限制接单，< 30 冻结账号
3. **同时接单上限**：同一用户最多持有 3 个"进行中"任务
4. **订单超时**：接单后 30 分钟未确认开始自动释放；完成确认 24h 超时自动确认

## 开发命令

```bash
# 后端（需要 JDK 17+，系统默认 JDK 8 需手动指定）
export JAVA_HOME="/c/Program Files/Java/新jdk-21"
cd server
mvn compile                            # 编译
mvn spring-boot:run                    # 启动
mvn test                               # 运行测试

# 用户端 Web App
cd web-app
npm install && npm run dev             # 开发服务器 :3001

# Web 后台
cd web-admin
npm install && npm run dev             # 开发服务器 :3000
npm run build                          # 生产构建
```

## 环境变量 (server/.env.example)

```
DB_HOST / DB_PORT / DB_USERNAME / DB_PASSWORD
REDIS_HOST / REDIS_PORT / REDIS_PASSWORD
JWT_SECRET
SPRING_PROFILES_ACTIVE=dev
```

## PER 开发流程

每个 Feature 走 Planner → Executor → Reviewer 循环：

1. **Planner**：给定 PRD Feature，拆解为 API + 数据库变更 + 前端页面清单
2. **Executor**：逐项实现，每完成一个子任务运行测试
3. **Reviewer**：检查 SQL 注入、XSS、事务边界、测试覆盖率、敏感词过滤

## TDD 要求

- Service 层必须写单元测试（JUnit 5 + Mockito）
- 核心流程（发单→接单→完成）必须有集成测试
- 提交前 `mvn test` 必须全绿

## 相关文档

- 调研补充：`../整理part1/代事猫调研补充-人工核查版.md`
- PRD：`../part2-prd/PRD-代事猫-MVP.md`
- 技术设计：`../part3-tech-design/TechDesign-代事猫-MVP.md`
