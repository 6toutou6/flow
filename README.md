# flow · 线性顺序流转工作流系统

面向"任务 → 期次 → 人员"模型的**顺序流转工作流平台**：管理员设计流程模板（有序节点链 + 各节点表单），创建主任务并按周期自动/手动生成期次，每个期次为每名任务人员创建一条独立成员任务，从开始节点按链逐节点流转，直到结束节点完成。

> 区别于并行表单收集：不是所有人同时填一张表，而是一步一步往前走，每步处理人不同、每步填写的字段不同，提交时由处理人指定下一节点处理人。

---

## 一、功能总览

| 模块 | 说明 | 前端目录 | 后端模块 |
|---|---|---|---|
| 模板管理 | 流程模板：节点链设计 + 节点级表单字段配置、启用/停用、复制、版本化 | `flow-front/src/views/flowtemplate`、`flow-front/src/views/formdesigner` | `flow-service/.../sys/flowtemplate` |
| 下发配置 | 配置周期/触发规则模板，可设为样例供各任务复用 | `flow-front/src/views/flowdispatch` | `flow-service/.../sys/flowtask`（ConfigTemplate） |
| 任务管理 | 创建/复制主任务（复制基本信息与人员配置、不含期次）、生成/复制期次（复制期次=生成期次弹窗回填成员、名称/起止时间自填）、期次人员查看与催办 | `flow-front/src/views/flowdispatch` | `flow-service/.../sys/flowtask` |
| 任务处理 | 处理人待办：填当前节点表单 → 指定下一处理人 → 通过流转；支持退回重做 | `flow-front/src/views/taskprocess` | `flow-service/.../sys/flowtask`（`FlowTaskService.submit`） |
| 数据查看 | 任务数据报表与明细 | `flow-front/src/views/dataview`、`flowadmin` | `flow-service/.../sys/flowdata` |
| 系统管理员 | dept_admin 部门管理员与部门维护 | `flow-front/src/views/deptadmin` | `flow-service/.../sys/base/deptAdmin` |
| 用户 | aut_user 全量用户表与登录 | `flow-front/src/views/sysuser`、`login` | `flow-service/.../sys/base/autuser` |

## 二、核心概念

### 领域对象链

```
流程模板 flow_template（节点链 flow_template_node + 字段 flow_template_field）
   └─ 主任务 flow_dispatch（任务级字段、周期、人员配置 flow_task_user）
        └─ 期次 flow_task_dispatch（快照节点 flow_task_dispatch_node，锁定期次下发时的模板版本）
             └─ 成员任务 flow_task（每人一条独立流程，含流程指针/进度）
                  └─ 节点记录 flow_task_node（含表单留痕 flow_form_record → flow_form_data）
```

### 关键机制

- **节点类型**：开始(1) / 中间(2) / 结束(3)；**多处理人**节点"任一完成即可"，其余并行分支自动消解（sibling 自动 submit）。
- **退回重做**：可退回到已到达过的前置节点；目标节点表单回填该节点最近一次真实提交的数据，可修改重交；提交确认弹窗会**回填上次分配的全部下一处理人**。
- **进度口径**：期次人员卡片展示 `已完成节点数 / 总节点数`（处于第一个节点未完成即 `0/5`）。
- **样例机制**：`is_sample=1` 的数据公共可见、可复制复用；"设为样例/取消样例"仅超管。**样例任务对所有用户开放编辑入口**（点开编辑可查看/试改，供学习参考），但**保存落库仅超管允许**（后端拒绝，前端提示"样例仅供学习参考、修改不可保存"）。
- **人员/部门权限口径**：以 `dept_admin` 登记的部门为准——取当前登录人 `yyyt_id` 反查 `dept_admin.admin_yst_id` 得到其部门；`aut_user.dept_id`（全量用户表）**不作为权限依据**；部门管理员=在该部门登记的用户，可管理/创建本部门数据。

## 三、目录结构

```
flow/
├── README.md                  # 本文档（总览 + 文档导航）
├── flow_schema.sql            # 数据库初始化建表脚本
├── flow_template5_import.sql  # 测试模板数据导入脚本
├── readme.txt                 # 早期业务方案说明（任务→期次→人员模型）
├── docs/
│   └── 项目开发文档.md          # 全量开发文档：架构/领域/ER 图/API/启动/维护要点
├── .trae/documents/           # 历史改造/实施计划（按需求迭代，见文末索引）
├── .workbuddy/memory/         # 开发会话日志（按日期），详见文末索引
├── flow-service/              # 后端 Spring Boot 3.4 + Java 21 + MyBatis-Plus
│   └── src/main/java/com/company/flow/sys/
│       ├── base/              # autuser 用户 / deptAdmin 部门管理员 / job 周期任务
│       ├── flowtemplate/      # 模板 / 节点 / 字段
│       ├── flowtask/          # 任务 / 期次 / 成员 / 流转（核心 FlowTaskService）
│       └── flowdata/          # 数据查看与报表
├── flow-front/                # 前端 Vue 2.6 + Element UI（vue-admin-template 改造）
└── stitch_flow_task_collection_system/  # 独立样例（Axiom Workflow，含 DESIGN.md，不参与主工程构建）
```

## 四、本地启动

### 环境要求

JDK 21 · Maven 3.9 · Node 16+ · MySQL 8

### 1. 初始化数据库

```bash
mysql -uroot -p < flow_schema.sql    # 建库 flow + 全部表
mysql -uroot -p flow < flow_template5_import.sql   # 导入测试模板（可选）
```

开发环境数据源见 `flow-service/src/main/resources/application.yml`：`jdbc:mysql://localhost:3306/flow`（root/12345）。

### 2. 启动后端（端口 9000）

```bash
cd flow-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9000
```

### 3. 启动前端（dev 默认 9528；本仓库调试常以 9532 运行）

```bash
cd flow-front
npm install
npm run dev -- --port 9532     # 或 npm run dev（默认 9528）
```

前端访问 `http://localhost:9532`；接口经 `vue.config.js` 代理转发到 `http://localhost:9000`。

### 4. 测试账号（统一密码 `123456`）

| 账号 | 姓名 | 说明 |
|---|---|---|
| emp0001 | 张三 | 超管（yml `system.admin.userIds: emp0001`），技术部部门管理员 |
| emp0002 | 李四 | 市场部部门管理员 |
| emp0003 | 王五 | 技术部部门管理员（aut_user 亦属技术部） |

> 部门管理员以 `dept_admin` 表为准，系统管理员页可增删改登记。

## 五、核心服务与关键代码位置

| 关注点 | 位置 |
|---|---|
| 提交并流转 / 退回（核心事务） | `FlowTaskService.submit(NodeSubmitDTO)`，`flow-service/.../flowtask/service/FlowTaskService.java` |
| 任务管理服务（样例/期次/统计） | `FlowDispatchService.java` |
| 模板服务 | `FlowTemplateService.java` |
| 部门管理员服务（反查部门） | `DeptAdminService.deptIdOf(yyytId)` / `isDeptAdmin(...)` |
| 成员列表（进度/节点链组装） | `FlowTaskService` + `mapper/FlowTaskMapper.xml`（`selectMembersPage`） |
| 超管白名单 | `application.yml` → `system.admin.userIds`（`AdminProperties`） |

## 六、开发约定（务必遵守）

1. **前端每改完文件跑一次 lint**：dev 开启 `lintOnSave`，eslint error 会阻塞热更新导致页面"刷不出来"：
   ```bash
   cd flow-front && npx eslint src/views/<你的文件>   # 确保 0 error
   ```
2. **权限判断一律走后端**：前端不依据 `userInfo` 做权限分支；需要隐藏按钮时由后端列表返回标识。
3. **部门口径**：使用 `dept_admin` 反查，勿用 `aut_user.dept_id`。
4. **MyBatis-Plus 链式 wrapper 坑**：`.ge(cond, col, expr)` 的 `expr` 是调用即求值，空字符串 `.trim()` 会 NPE——先判空取局部变量再用。
5. **任务进度**：成员卡片进度展示来自 `nodeSteps` 的 done 计数，勿直接展示 `flow_task.finished_node_count`（该字段在退回后不会随历史提交清理，会虚高）。
6. **退回回填**：节点表单回填只认"确有表单内容"的提交（退回空留痕不挡回填）；下一处理人整组回填优先读 `flow_task_node.next_handler_ids`（JSON），旧数据由前端从下游分支反推。

## 七、文档导航（README 汇总）

| 文档 | 内容 | 备注 |
|---|---|---|
| `docs/项目开发文档.md` | 架构 / ER 图 / 表说明 / API / 启动 / 维护要点（部分章节为早期版本，字段与包名以代码为准） | 开发主文档 |
| `readme.txt` | 业务方案完整说明（任务→期次→人员模型 + 示例） | 早期方案 |
| `.trae/documents/线性顺序流转工作流改造.md` | 从表单收集改造为顺序流转的方案 | 历史改造 |
| `.trae/documents/flow-process-improvement-plan.md` | 任务处理/数据后台/模板设计器改进实施计划（12 项需求） | 历史计划 |
| `.trae/documents/flow-process-improvement.md` | 流程系统体验改进方案 | 历史计划 |
| `.trae/documents/flow-form-system-4-pages.md` | 流程填报系统 4 个页面实现说明 | 历史 |
| `.trae/documents/sys_user_crud_implementation.md` | 用户管理 CRUD 实现计划 | 历史 |
| `.workbuddy/memory/2026-09-07.md` | 当日开发日志：部门口径切换、统计卡改造、筛选扩列、退回/进度/回填系列修复 | 近期迭代记录 |
| `.workbuddy/memory/2026-09-0x.md` 等 | 更早按日会话日志 | 按需查阅 |
| `flow-front/README-zh.md` | vue-admin-template 基础说明（上游模板） | 前端脚手架来源 |
| `stitch_flow_task_collection_system/axiom_workflow_system/DESIGN.md` | 独立样例的设计文档 | 参考样例 |

---

*维护提示：大版本改动后请同步更新本文档与 `docs/项目开发文档.md` 的相关章节；按日开发日志持续追加到 `.workbuddy/memory/` 对应日期文件。*
