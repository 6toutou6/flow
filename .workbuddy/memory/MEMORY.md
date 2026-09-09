# 项目长期事实与约定

## 技术栈与运行

- 后端：Spring Boot + MyBatis-Plus，`flow-service` 模块，端口 9000，无 context-path。
- 前端：Vue 2 + Element UI + SCSS，`flow-front` 模块，端口 9528，`/api` 前缀代理到后端（剥掉前缀）。
- 数据库：MySQL `localhost:3306/flow`，用户 `root`，密码 `12345`。
- 认证：Session 会话（`HttpSession` + `SESSION_USER_KEY`），不是 JWT。超管白名单在 `application.yml` 的 `system.admin.userIds`（当前 `emp0001`）。
- 后端启动方式：`mvn spring-boot:run`（cwd 在 `flow-service`），**无 spring-boot-devtools 热加载**——改了后端代码/实体字段后必须重启进程才生效，否则运行中的进程仍用旧 class（表现为"字段值不入库"）。重启：`kill <PID>` 后重新 `mvn spring-boot:run`。
- **环境变量坑**：CodeBuddy 会话环境存在 `SERVER__PORT=0`（Spring Boot relaxed binding 会映射为 `server.port=0`），直接 `mvn spring-boot:run` 会让后端跑随机端口而非 9000。重启后端必须 `env -u SERVER__PORT mvn spring-boot:run`。

## 重要约定（踩过的坑）

1. **改 `flow_schema.sql` 加列后，必须同步对本地 `flow` 库执行增量 DDL**。只改 schema.sql 不动库，会导致实体字段有值但 insert/update 报 `Unknown column`，数据静默丢失（表现为「保存了但不见了」）。2026-09-08 曾因此丢失字段显隐配置。
2. **MyBatis-Plus `updateById` 默认忽略 null 字段**。字段从「有值」清空为 null 时不会更新到库（残留旧值）。需要清空的列（如 `visible_when`/`editable_when`、`node_id`）要用 `LambdaUpdateWrapper.set(..., null)` 强制同步。
3. **import 的常量若要在 Vue 模板里直接用，必须暴露到 data/computed**（Vue2 模板里 `FOO.bar` 会解析成实例属性，模块级常量会报 `undefined`）。仅 script 内使用则不需要。
4. **表单类「可增删编辑」状态不要用 computed 承载**（computed 返回值不响应式，push/splice 不触发更新），要用 data 对象 + watch 同步。
5. 数据库存 `1/2/3/4` 编码是正确做法（tinyint），不要为固定语义建字典表；语义丢失在「代码层」，用枚举/常量收敛即可（前端 `src/constants/dict.js`，后端 `sys.base.enums`）。

## 条件/流程模型（最新）

- 条件表达式：递归树 `{"logic":"and|or","children":[叶子{"fieldKey","op","value"} | 子组{...}]}`，向后兼容旧 `conds` 与单条件结构。
- 操作符：eq/neq/gt/gte/lt/lte/in/empty/notempty；`in` 是「值属于逗号分隔列表」（精确匹配，非子串）。
- 可作条件判断依据的字段类型仅 `number/date/radio/checkbox`（`COND_FIELD_TYPES`）。
- **会签模式已整体移除（2026-09-09）**：`approve_mode` 字段/列、审批模式 UI、`waitingMeeting` 均已删。多人被指派同一节点时固定「任一处理人提交即流转、同节点其他待办自动结束」。
- **通知通道（预留）**：统一出口 `RobotService.sendRobot`（sys.base.robot，互联网阶段仅落日志，内网框架接管真推送）。业务通知一律经 `FlowNotifyService`（flowtask.service）的 `notifyDispatch/notifyFlowNext/notifyRejectRedo/notifyUrge`，内部 try-catch 不影响业务；不要在业务 Service 里散调 sendRobot。新通知场景优先加 FlowNotifyService 方法。
