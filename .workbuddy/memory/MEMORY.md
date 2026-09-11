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

## 任务交接模型（2026-09-10 定稿）

- **判定锚点 = 「我在这条任务里的席位」**，不是「我手上还有没有活」。只要这条任务因为我的节点席位出现在我的列表里（可见性 SQL = `t.owner_id = 我 OR tn.handler_user_id = 我`，含 `已结束` 任务），就应当可交接——中间节点处理人调岗时要把经办痕迹一并转给接手人查看。
- 范围两条线：①**期次归属**（`flow_task.owner_id = 我`）整期交出（owner A→B，该期下我名下全部节点含历史已提交的处理人 A→B）；②**节点席位**（该任务配置下我名下的**全部节点**，不论提交状态、不论任务是否结束）仅换节点处理人，**不动期次归属**。两者皆空才报「没有您名下的期次或参与记录」。
- `current_handler_id` 指针只在进行中的任务实例上跟随换人（已结束任务的指针无意义，不改）。
- 触发入口：任务组头「交接任务」按钮**常显**（该组有任务配置/有期次即显示）；我名下是否真可交接由弹窗按 `myPeriodCount` / `myNodeCount` 分流提示。`MyTodoTaskVO` 的 `myPeriodCount`、`myNodeCount` 均由 `selectMyTaskGroups` 聚合得出。
- 审批人 = 创建人所属部门的部门管理员（`dept_admin` 表，含创建人本人）。
- `sync_member` 只在申请人位于 `flow_task_member` 时适用；不适用时审批弹窗隐藏勾选、后端忽略（`isMember()` 兜底），列表用不落库字段 `syncMemberApplicable` 传递。
- **留痕**：`flow_task_node.transfer_from_user_id/name` 记录原处理人，由 `selectMyTodo` / `selectTaskProgress` 带出（`MyTodoVO`/`TaskProgressVO`），前端统一走 `utils.formatHandlerWithTransfer`（`formatNodeHandlers` 亦已追加「（X 移交）」后缀），展示为「接手人（原处理人 移交）」。
- `flow_task_handover` 计数字段：`period_count` = 我名下期次数，`node_count` = 我名下节点席位数（含已提交历史节点）。
- **记录可见性（两条互不相同的口径，勿混用）**：
  - `/flow-task-handover/records`（任务交接页「交接记录」页签）、`/by-dispatch`（某任务下的记录弹窗）→ **当事人口径**：`from_user_id = 我 OR to_user_id = 我`。他人之间的交接不下发（同部门非当事人也看不到）。
  - `/flow-task-handover/approver-records`（交接审批页「本部门交接记录」页签）→ **审批权限口径**：按 `isApproverOf`（本部门管理员 / 超管）过滤，含全部状态。超管返回全量。
  - `listMine()`（我的交接申请）另为 `from_user_id = 我`。
- 列表统一由 `decorate(list, me)` 补四个不落库字段：`syncMemberApplicable`、`myRole`（交接人 / 接手人 / null，审批人非当事人时为 null）、`deptId` / `deptName`（任务配置**创建部门**，取自 `flow_dispatch.dept_id`，名称优先 `dept_admin.dept_options`、兜底创建人 `aut_user.dept_name`；列表按部门折叠分组用）。
- 分页接口返回结构：`my-todo-grouped` 在 `data.records`，`my-todo` 在 `data.records`。

## 前端请求层与提示约定（2026-09-10 定稿，与内网框架 / gzfb 完全一致）

- **两个文件是内网框架的「本地兜底」，除服务名外与 gzfb 逐字一致，不要自行改造**：
  - `src/utils/request.js`：`handleError`（HTTP/网络异常 → `Message.error` 并**吞掉**，`post` 返回 `''`、`get` 返回 undefined）+ `handleResponse`（业务码非 200 → `Message.error`，**res 原样返回、不 reject**）+ 全局 `get/post` 兜底挂到 `window`（内网框架已有同名函数时自动不覆盖）。axios 实例 `timeout: 30000, withCredentials: true`。
  - `src/service/BaseAxios.js`：只导出 `getDg(controller, method, getParam)` / `postDg(controller, method, postParam)`，内部调全局 `get/post`，服务名常量 `flow_service = 'flow-service'`。
- **URL 形态**：`/{system}/{controller}/{method}[/{getParam}]` → 即 `/flow-service/flow-task-handover/mine`。dev 由 `vue.config.js` 的 proxy 把 `/flow-service` 前缀剥掉转发到 9000；**生产由内网网关按服务名路由**。`.env` 里的 `VUE_APP_BASE_API`（/api）已废弃不用。
- **只有 GET（按 id 查详情，参数拼在路径末段）与 POST（其余一切：无参查询、列表查询、写操作、删除）两种**——内网框架没有 put/delete。所以后端 **PUT/DELETE 一律改成了 POST**，无参 GET 也改成了 POST。
- **业务侧统一写法**：`const res = await xxx(); if (!res || res.code !== 200) return;` 再用 res / 弹成功提示。
  - 写操作（要弹 `$message.success` 的）**必须**先判 code，否则请求层不 reject 时会继续往下执行、误弹「成功」；
  - 读操作里**直接取深层属性**的（`res.data.records` 这类）也要先判 —— 异常时 res 是 `''`，`''.data.records` 会抛错；写成 `(res.data && res.data.records) || []` 的天然安全；
  - `catch` 只兜本地异常，统一用 `this.$notifyError(e, '兜底文案')`（main.js 注册，底层 `utils/notify.js`：同文案 800ms 去重）。**不要再写 `this.$message.error(...)`**。
- **多参数 / 参数对象**：后端一律用 `@RequestBody(required = false) Map<String, Object> body` 接收，用 `com.company.flow.sys.base.util.ParamUtil`（str / intv(o) / intv(o, def) / boolv(o) / boolv(o, def)）取值；单参数查询走 `@PathVariable`。
- ESLint 需在 `.eslintrc.js` 的 `globals` 里放行全局 `get` / `post`（否则 no-undef）。axios 已升到 **^0.27.2**（与 gzfb 一致；0.18 不会为 FormData 清除默认 json header，附件上传会失败）。
- **后端全局异常**：`sys/base/exception/GlobalExceptionHandler`（与 gzfb 同款 `@Slf4j` + RuntimeException → `Result.fail`，另保留 Exception 兜底）——service 层 `throw new RuntimeException("提示语")` 即可，前端请求层会弹出来。
- **成功提示：谁动作谁提示**。弹窗组件提交成功后自己弹、再 `$emit('success')`；父组件的 `@success` 回调**只负责关弹窗 / 刷新列表，不要再弹一次**。
- **搬到内网时**：框架已挂载全局 `get/post`，`utils/request.js` 自动不生效，业务代码零改动。

## 搬到内网时的迁移清单（2026-09-10 定，皇上问「是否只移核心业务代码」）

- **要移（业务核心）**：前端 `service/`（含 BaseAxios.js）· `views/` · `components/` · `constants/` · `utils/index.js` + `utils/notify.js` · `router` 与 `store` 的业务部分 · `main.js` 里的 `$notifyError` 注册；后端 `sys/flowtask` · `flowdata` · `flowtemplate` · `base/{attach,autuser,deptAdmin,job,robot,enums}` · `resources/mapper/*.xml` · `flow_schema.sql`。
- **不用移**：前端 `utils/request.js`（内网框架已挂全局 get/post，`if (typeof window.get !== 'function')` 保护会让它自动让位）· `mock/` · `vue.config.js` 的 proxy（内网由网关）· `.env.*` 的 `VUE_APP_BASE_API`；后端 `application.yml` 的本地数据源/端口；以及脚手架工程骨架（build / public / babel / pom 骨架）。
- **要改造对接（内网规矩不同，坑在这里）**：① 登录态 —— flow 用 `utils/auth.js` 的 localStorage 快照 + `permission.js`，内网由框架接管（gzfb 是放 Vuex）；② 布局与菜单 —— 贴进内网 layout，去掉 flow 自带的；③ 附件上传 —— flow 现在是「只记记录不存文件」的模拟实现，内网有真实文件服务；④ `BaseAxios.js` 里的 `flow_service = 'flow-service'` 需与内网网关的项目名一致。
- 判断依据：**业务代码（views/components/service）直接 import 的东西都要跟着移**，凡是「内网框架本来就提供」或「本地开发专用」的都不用。

## 前端列表折叠面板约定（沿用「任务组」样式）

- 折叠壳统一复用 `taskprocess/index.vue` 的类名：`.task-collapse` / `.task-panel`（`.is-open` 控制箭头旋转）/ `.tp-head` / `.ct-icon` / `.ct-main` / `.ct-name` / `.pending-tag` / `.ct-meta` / `.tp-arrow` / `.tp-body`；空态 `.empty-state`、加载 `.loading-bar`。新页面要折叠直接抄这一套，不要另造样式。
- 分组在**前端 computed 里做**（后端只补必要字段），按「后端时间倒序下的首现顺序」排组。展开状态用 `openMap: { tabKey: null }`，`null` 表示未初始化 → 首次加载默认全部展开；`syncOpenState()` 保留用户已收起的分组、丢弃已消失的、**新出现的默认展开**（否则筛选后命中的分组会被历史收起状态藏掉）；写回用 `$set` 保证响应式。
- **页头不放「全部展开 / 收起」按钮（2026-09-10 已撤）**，改放**筛选条件**：`.head-filter` + `.head-filter-label` + `el-select.head-select`（`::v-deep .el-input__inner` 高 36px，刷新按钮同步 36px）。任务交接页筛**状态**（两页签共用）；交接审批页筛**任务**（`filterable clearable`，选项取当前页签数据去重 `dispatchId`，`switchTab` 时重置）。空态分两档：原本为空「暂无…」/ 筛掉为空「没有符合筛选条件的…」。
- **交接的四个列表统一用表格**（2026-09-10 纠偏：曾误做成一堆卡片/卡片两列，皇上要的是表格）：口径一致——主体列（任务名称 / 申请人→接手人）在前、两个时间列（各 **170**）靠后、操作列 `fixed="right"` 最后；姓名列 **128**（姓名 + `.emp-no` 员工号）；操作列任务交接页 **150**（详情/记录）、交接审批页 **170**（同意/拒绝）。**表头列允许各页略有差异**（原话「表头上可以少做区别」）。四个列表：任务交接-我的交接申请 / 任务交接-交接记录 / 交接审批-待我审批 / 交接审批-本部门交接记录。
- **表单式卡片只用在弹窗里**（点「详情」查看单条完整字段）：`.rec-card` = `.rc-head`（申请人→接手人 + 角色标签 + 申请时间 + 状态徽标）+ `.rc-body`（grid **两列**，`gap: 10px 30px`；`.rc-item` = `.rc-label` **70px** + `.rc-value`；整行用 `span-2`）。两个弹窗：单条申请详情（`width="660px"`）、某任务交接记录（`width="820px"`）。`.span-3` 与 `.rec-list.narrow` 已删，不要再引入。说明/意见统一 `opinionText(row)`：拒绝→`rejectReason`，通过→`remark`||'已通过'，待审批→`remark`。
- **交接列表一律「姓名 + 员工号」**（2026-09-10 加）：员工号即 `fromUserId` / `toUserId` / `approverId`（后端 `selectList` 全字段本就返回，无需加字段）。表格/卡片头用 `<span class="who">姓名<span class="emp-no">工号</span></span>`（`.emp-no` = 11px、`#A8B0BF`、`margin-left:5px`，需覆盖 `.who` 的加粗与颜色）；弹窗确认文案用 `whoText(name, id)` → `孙八（emp0006）`。
- **「我的交接申请」表格列序（2026-09-10 定）**：任务名称 min200 → 接手人 132 → 状态 85 → 审批人 132 → 审批意见 min150 → 申请时间 **170** → 审批时间 **170** → 操作 **150**（fixed right：`详情` + `记录`）。**交接范围 / 同步名单不在列表里**，点「详情」开单条申请详情弹窗（`detailVisible/detailRow/detailTitle`，`openDetail(row)` 复用行数据、不发请求；`width="660px"`，两列字段成对排：任务名称(整行) / 交接人·接手人 / 创建部门·交接范围 / 同步名单·审批人 / 申请时间·审批时间 / 说明·意见(整行)）。交接审批页「待我审批」的申请时间列同 170、操作列同 **170**，「交接范围」列保留（审批人要用）。
- **节点处理人展示口径（2026-09-10 定，重要）**：交接审批通过后库里 `handler_user_id` 会变成接手人——这是**既定行为**（否则接手人看不到任务），原处理人记在 `transfer_from_user_id` / `transfer_from_user_name`。所以「**实际处理**」必须显示原处理人：`formatHandlerWithTransfer(node)` → `钱七 emp0005（现 孙八 emp0006）`（未交接受 → `张三 emp0001`，`handlerName` 缺失才回退 fallback）。**严禁再出现「接手人（原处理人 移交）」格式**——皇上明确否掉（有歧义，看着像接手人处理的）。`formatNodeHandlers(nodes, pendingOnly)` 只输出节点**当前归属人**名单（已去掉交接后缀），用于「处理人 / 待处理人」。FlowChain 的 `handledText` = `formatHandlerWithTransfer(latestDone)`；发生交接的节点隐藏「当时分配」行（现归属已在"实际处理"括号里）。
- **留痕字段写入规则（2026-09-11 定，连续交接的坑）**：`transfer_from_*` 只记**最早那一位**，后来的接手人**不能覆盖**
  - 交接 `FlowTaskHandoverService.transferNode()` 与转办 `FlowTaskService.transfer()`：**仅当 `transferFromUserId` 为空时才写**（否则 A→B→C 两段交接后，留痕会从 A 被覆盖成 B，流程链就会显示成 B 处理的 —— 皇上 09-11 报的就是这个）；
  - 节点被**本人提交**时（`FlowTaskService.submit()` 的退回分支与通过分支各一处）：调 `clearTransferFrom(taskNodeId)` 清掉此前的交接/转办留痕 —— 因为提交人就是实际经办人；
  - ⚠️ MyBatis-Plus 的 `updateById` **忽略 null 字段**，置空必须用 `LambdaUpdateWrapper.set(Field, null)`（`clearTransferFrom` 已按此实现）；
  - 语义：`transfer_from` = 已提交节点的**实际经办人**；未提交节点表示「席位原本归属人」。前端只用它渲染已提交节点的「实际处理」，所以两种语义都不会显示错。
- 现有落点：任务交接页按**创建部门**折叠（两个页签都是），交接审批页按**任务**折叠（两个页签都是）。
