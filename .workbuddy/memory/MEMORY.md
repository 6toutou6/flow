# 项目长期事实与约定

## 技术栈与运行

- 后端：Spring Boot + MyBatis-Plus，`flow-service` 模块，端口 9000，无 context-path。
- 前端：Vue 2 + Element UI + SCSS，`flow-front` 模块，端口 9528，`/flow-service` 前缀代理到后端并剥掉前缀（`.env` 的 `/api` 已废弃）。
- 数据库：MySQL `localhost:3306/flow`，用户 `root`，密码 `12345`。
- 认证：Session 会话（`HttpSession` + `SESSION_USER_KEY`），不是 JWT。超管白名单在 `application.yml` 的 `system.admin.userIds`（当前 `emp0001`）。
- 启动方式（**必须用「独立会话」方式，否则进程会被沙箱随对话轮次回收**）：
  - ✅ 后端：用 Python 脚本 `/tmp/start-flow-backend.py`（`subprocess.Popen(..., start_new_session=True)` = setsid，`env.pop('SERVER__PORT')`，cwd `flow-service`，命令 `java -cp "target/classes:$(cat /tmp/flow-cp.txt)" com.company.flow.base.FlowApplication`），**用普通 Bash 调用**（非 `run_in_background`），Python 打印 PID 后立即返回、服务独立跑。启动约 **5 秒**。
  - ✅ 前端：`/tmp/start-flow-front.py`（同上，`env.pop('PORT')`，cwd `flow-front`，`npm run dev`）。
  - classpath 重建：`cd flow-service && mvn -o -q org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath -Dmdep.outputFile=/tmp/flow-cp.txt`（**必须带 3.6.1**，默认 3.8.1 离线没下载过会报 `Cannot access alimaven`；仓库里只有 3.6.1/3.7.0 完整）。
  - ❌ 反例：`run_in_background` 起的进程（无论 `mvn -o spring-boot:run` 还是 `java`）**几分钟到几十分钟内必被回收**，别再用。
  - **改 Java 代码后先 `mvn -o compile`** 再重跑脚本（Java 方式不自动编译）。
  - **无 spring-boot-devtools 热加载** —— 不改代码也不重启就行，改了必须重启，否则仍用旧 class（表现为「字段值不入库」）。
  - **沙箱限制**：`ps` / `ps aux` 被禁（`operation not permitted`），验进程只能用 `lsof -ti tcp:<port>`。
- **环境变量坑**：CodeBuddy 会话环境存在 `SERVER__PORT=0`（Spring Boot relaxed binding 映射为 `server.port=0`），不清掉会让后端跑**随机端口**而非 9000（实测跑偏到 59365）。启动前必须清掉。

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
- **空态提示配图标**（2026-09-14 定；皇上先反馈纯文字「太单调」，同日又纠正「这个已经有了就不加了呀，一个 icon 就够了」）：
  - ⚠️ **先看容器有没有图标再加**：`.empty-state` 这类容器的样式里已定义 `i { font-size: 48px; display: block; margin-bottom: 12px; }`，模板里也早有「48px 大图标 + `<p>文案</p>`」的结构（`flowdispatch/index.vue` 暂无任务、`ConfigTemplate` 暂无配置模板、`PeriodUsers` 该期次暂无人员、`PersonView`、`TaskLink`、`EditTask` 尚未配置人员、`Handover`/`HandoverApproval` 各空态、`taskprocess/index` 暂无待办、`RejectTargetModal`、`formdesigner` 尚未添加节点、`FlowChain` 暂无流程数据、`HandlerFlowDetail` 暂无流程数据、`PeriodFlow`、`GeneratePeriodModal`、`VersionListModal` 暂无历史版本…）—— **这些一律不要再加**，否则会出现"大图标 + 小图标"两行。
  - **只有纯文字、容器本身无图标的空态才加**：写成 `<i class="el-icon-xxx empty-icon" /> 文案`。本次最终落地 **21 处**（分布在 `dataview` 8、`ProcessDetail`/`VersionListModal`/`FlowChain` 各 2、其余 8 个文件各 1，均为 `.form-empty` / `.history-empty` / `.chart-empty` / `.legend-empty` / `.pp-empty` / `.dialog-empty` / `.period-empty` / `.ver-desc` / `.dept-empty` / 表格 `<td>` 这类**没自带图标**的容器）。
  - `.empty-icon` 定义在 `styles/index.scss`（15px、右距 6px、`vertical-align: -1px`、opacity 0.85），新空态直接复用这个类。
  - 图标语义：流程数据 → `el-icon-share`；操作记录/历史/交接/审批 → `el-icon-time`；任务期次关联 → `el-icon-connection`；人员/提交/参与 → `el-icon-user`；图表数据 → `el-icon-data-line`；其余（任务/期次/模板/节点/部门）→ `el-icon-folder-opened`。
  - **判断坑**（臣踩过）：写在**兄弟元素**里的图标不算容器图标 —— `.sec-title` 的标题图标、`.ver-time` 的时间字段图标、`.dept-loading` 的加载图标、列表项内的内联图标，都容易让「向上找图标」的脚本误判而漏加。
- **跳转页必须有返回按钮**（2026-09-15 定，皇上原话「每个页面跳转之后，除了侧边导航栏绑定的页面，左上方都要有返回按钮」）：
  - 判定口径：**路由里 `hidden: true` 的 = 跳转页**（从别处点进去的），左上角一律要有「返回」按钮；**侧边导航栏绑定的菜单页不要加**（`/login`、`/404` 无侧边栏，也不需要）。
  - 全站跳转页共 **7 个**：`task-process/detail`、`flow-dispatch/config-template/edit`、`flow-dispatch/edit`、`flow-dispatch/period-users`、`flow-dispatch/period-flow`、`flow-dispatch/person-view`、`flow-dispatch/task-link`。**以后新增跳转页要同步加**。
  - 页头结构直接抄 `views/flowdispatch/PeriodUsers.vue`：`.header-left`（flex column / gap 8px）> `.hl-row1`（flex / gap 14px，内含 `.btn-back` + `nav.breadcrumb`，并写 `.hl-row1 .breadcrumb { margin-bottom: 0 }`）> `.page-heading`。
  - `.btn-back` 全站统一（6 处一致）：`display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; background: #fff; border: 1px solid $border; border-radius: 3px; color: var(--color-primary); font-size: 13px;` hover：`background: var(--color-primary-light); border-color: var(--color-primary);`（**白底 + 边框**是皇上点名的样式，别再写回 transparent/none）。
- **任务管理页的「跳出去再返回保持展开」机制**（2026-09-15 修 bug 时摸清，**新增跳转出口务必照做**）：
  - 机制：`views/flowdispatch/index.vue` 的 `created` 调 `restoreExpandState()` —— 只有在 `sessionStorage.flowDispatchJumpOut === '1'` 时才恢复 `flowDispatchExpanded` 里保存的 `activeTasks`，并消费掉标记；否则清掉遗留标记、不做任何高亮（菜单/路由直达不展开）。
  - **所以每个跳转出口都必须先调 `saveFlashId(id, dispatchId)`**（它一次性写入 JumpOut + Expanded + Flash + FlashPeriod）。目前正确调用：`openEdit(t)`、`openPersonView(t)`、`openPeriodUsers(t, p)`、`goTaskLink(t, p)`、`openCopyTask(t)`、`openCreate()`（后三个是 09-15 补的）。**新加跳转出口漏调，就会出现「返回后任务面板被收起」的 bug。**
  - `saveFlashId` 的 `id` 可空（如「新建任务」没有来源任务），此时只清 Flash、不写。
- **数据展示分三个维度**（2026-09-15 定，皇上要求「普通用户 / 部门管理员 / 整个系统」三套）：
  - 三个独立页面 + 独立路由：`data-view`（全系统，`dataview/index.vue` 原有 9 个图表，**未重构、保持不动**）、`data-view-dept`（部门管理员，`dataview/dept.vue`）、`data-view-user`（普通用户，`dataview/user.vue`）。**哪个用户能看到哪个由内网框架按权限挂菜单，前端不做身份判断**。
  - **数据范围一律由后端按登录身份自动过滤**（不是前端传 userId/deptId）：`POST /flow-data/dashboard-user` 在 controller 取 `SecurityUtils.getLoginUser()`；`/flow-data/dashboard-dept` 取 `deptAdminService.deptIdOf()`（**部门归属以 dept_admin 登记为准**）。**deptId 为 null 时返回全空壳**，绝不回退成全量数据。守住「UserContext 仅限 controller 层」铁律 —— 身份在 controller 取好再传给 service。
  - 统计口径（`FlowFormRecordMapper.xml` 的 10 条新 SQL）：个人维度按 `flow_task.owner_id` / `flow_task_node.handler_user_id` / `flow_form_record.user_id` 过滤；部门维度**人员一律 join `aut_user.dept_id`**（与既有的 `selectHandlerDeptRank` 口径一致），任务/节点/提交都按成员 join 进去。
  - **前端图表已抽公共组件**放 `components/charts/`：`BarChart.vue`（柱状趋势 + 峰值/平均/累计）、`DonutChart.vue`（conic-gradient 环形 + 图例）、`RankList.vue`（横向排行，默认前 5 可展开）。**新页面直接用这三个组件，不要再手写一份图表 DOM + 样式**（`dataview/index.vue` 是历史实现，暂未重构）。
  - ⚠️ 曾存在的坑：`restoreExpandState()` 里用 `Number.isInteger(flashId)` 判断，但 `sessionStorage.getItem` 返回**字符串**，导致「返回双闪 + 期次行高亮」从未生效；已改为 `if (flashId && this.activeTasks.indexOf(flashId) >= 0)`（`activeTasks`/`flashTasks`/`flashPeriod` 全用**字符串** id 比较）。
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
- **期次「新增人员」是否重复的判定口径（2026-09-14 定，皇上明确）**：只看**每个成员任务的第一个节点（sortNum 最小）处理人**，「跟其他的都没有关系」—— 第一个处理人才代表该成员，后续节点的协同/审批处理人不算。
  - 落点：`FlowDispatchService.addPeriodMembers()` 里收集 `existing` 的那段（原先错误地收了**该期次所有任务的每个节点**处理人，导致被加过协同节点的人（如赵六）被误判为"已在该期次中"）；
  - **不能用 `flow_task.owner_id` 替代**：`市场部-李四的问题整改处理` 的 `owner_id` 是 `emp0001 张三`（创建人）、而第一个处理人是 `emp0002 李四`，两者不一致；
  - 副作用（皇上已接受）：成员若把自己第一个节点交接出去，他就不再算"在期次中"，可被再次新增。
- **任务状态与「暂存（草稿）」（2026-09-14 定）**：`flow_dispatch.status` 有三个值 —— `启用` / `停用` / `草稿`。
  - 草稿的判定与影响：`getEnabledList()`、`checkDueDispatches()`、`notifyUnsyncedTemplateTasks()` 都只取「启用」，所以草稿**天然不参与下发**；`generatePeriod()` 与 `toggleStatus()` 显式拦截草稿并给业务提示；`getPage()` 的 status 是可选筛选，不传即不过滤 → 草稿会出现在任务列表（带橙色「草稿」标签，且隐藏启停按钮）。
  - `validate()`（`FlowDispatchService`）：**草稿只要求「任务名称 + 模板非空」**，模板完整性（节点/字段）与触发日一律跳过；status 非草稿才走完整校验。注意后端**本就不校验创建人字段与人员**（那是前端 `handleSubmit` 的职责），所以草稿转启用时不要指望后端兜底。
  - 前端：`EditTask.vue` 底部「暂存」（`handleSubmit(true)`，虚线框）与主按钮（`handleSubmit(false)`）分离，暂存成功后回列表、正式创建按原逻辑；编辑草稿时主按钮文案为「创建任务」。
- 现有落点：任务交接页按**创建部门**折叠（两个页签都是），交接审批页按**任务**折叠（两个页签都是）。

## 业务概念：期次 vs 期次任务（2026-09-15 皇上明确）

- **「期次」本身没有流程进度这个概念**，只有具体的**期次任务**（某人在某期次下的成员任务）才有流程进度。
- 所以任何「期次列表」的表头/汇总行**不要给期次配流程节点链**（`per.nodes`），流程进度只出现在展开后的每条待办行上（`td.chains`）。
- 任务处理页（`taskprocess/index.vue`）的表头据此定为 5 列：**期次 / 待办情况 / 起止时间 / 状态 / 操作**。

## 列表展开子项的排版约定（2026-09-15 皇上两次纠正后定稿）

皇上原话：「展开的期次任务，不要隔那么远啊，不要受期次的表头束缚，这里的样式和展示内容是独立的」「具体的期次任务，列宽自己定一下，不要挤在一起了」。据此确立：

1. **父级走表格，子级走独立的一行** —— 期次行按表头 5 列排；待办行整行 `colspan` 合并后**自己排版**，不跟表头列对齐（表头描述的是期次维度）。
2. **子级内容紧凑挨着排，绝不加 `flex-grow: 1`** —— 第一版就是给任务名加了 `flex:1`，把状态/进度/操作顶到很远，被皇上说「更丑了」「隔那么远」。正确做法是**每段固定宽度**（本次：任务名 `flex: 0 0 260px`、状态 `112px`、进度 `240px`、操作按钮自动），`gap: 20px`，多余空间留右边 —— 既整齐（各段起始位置稳定）又不分散。
3. **子级仍要保留主次**：缩进（`padding-left: 34px`）+ 一条 `1px dashed` 从属竖线（`&::before`）+ 淡色底（`#FAFCFE`），父级用更重的蓝底（`#E4EBF5`，展开时 `#DCE7F8`）+ 3px 左侧色条 + `font-weight: 700`。
4. **表格里 `td` 加 `white-space: nowrap` 必须同时设 `table-layout: fixed` + 明确列宽百分比**，否则内容一多就把右侧列挤出可视区（本次「操作」列曾被整个挤出屏幕，加 `fixed` 后消失）。
5. 子级里**不要重复父级已有的信息**（如期次名、期号、起止时间）。
6. **子级行必须比父级行矮**（本次：期次行 40px、待办行 37px）。父不高子高会显得「头轻脚重」，皇上会直接说「太丑了，高度太高」。
7. **⚠️ 同文件 SCSS 里改样式不生效，先量 `getComputedStyle` 再怀疑特异性** —— 本次 `.period-body td { padding: 0 }` 被文件后面同特异性（0,1,1）的 `.period-table td { padding: 10px 12px }` 盖掉，导致行高怎么调都降不下来。修法是给选择器加前缀提特异性（`.period-table .period-body td`）。
8. **状态类小标签按内容自适应，不要写死宽度** —— `.pb-status` 原写死 `flex: 0 0 104px`，「待我处理 + 已超期」需要约 128px 就会溢到邻列。改 `flex: 0 0 auto; white-space: nowrap`，进度段用 `flex: 1 1 auto; min/max-width` 吃余量才是正解。

## 期次人员新增：逐人任务名（2026-09-16 定稿）

- **口径**：期次人员页「新增人员」时，**给每个人单独设任务名**（不是统一前缀拼接）。
- 组件：`components/UserPicker/index.vue` 的 **`perUserTaskName` 模式**（老模式 `showTaskName` 保留，用于统一前缀场景）。勾选的人在表格下方按行列出，每人一个输入框，**placeholder 显示该人的默认名 `下发给{姓名}的任务`**。
- **留空即用默认名**，默认名规则由后端 `FlowDispatchService.memberTaskNameOf(uid, names)` 统一决定（`names` 里没有或为空 → `下发给{姓名}的任务`）。前端 placeholder 只是提示，不做兜底。
- 接口：`POST /flow-dispatch/period/add-members/{dispatchId}`，body `{ members: [{ yyytId, taskName }] }`（**旧格式 `{userIds, taskName}` 已废弃**）。后端把 members 拆成 `userIds` + `Map<String,String> taskNames` 交给 `addMembersToDispatch`。
- **批量导入人员的落点（2026-09-16）**：`components/ImportMemberModal.vue` 是可复用组件（模板下载 + 上传 + 错误清单），导入成功 emit `success` 出 `[{yyytId, userName, deptName, taskName}]`。目前接在 **4 处**：
  1. `views/flowdispatch/EditTask.vue`（新建/编辑任务 → 配置人员）
  2. `views/flowdispatch/components/GeneratePeriodModal.vue`（生成期次弹窗 —— **「按周期自动下发」与「手动临时期次」是同一个弹窗用 `mode` 切换，共用一个按钮**）
  3. `views/flowdispatch/PeriodUsers.vue`（查看期次人员 → 批量操作栏，导入后直接调 `addPeriodMembers`，用 `memberUserIds` 过滤已在本期次的人）
  - 各处的已选人员结构都是 `[{yyytId, userName, deptName, taskName}]`，与导入结果**格式一致**，接入只需「合并 + 按 yyytId 去重」，不需要格式转换。
- ⚠️ **给某个 Vue 文件的 scoped 样式加规则前，先确认该文件自己定义了哪些 `$` 变量** —— 本项目各文件各写各的（`$primary` 普遍有，`$border` 只有部分文件有），从一个文件照抄到另一个文件会直接 `SassError: Undefined variable` 导致整页 `Failed to compile`（本次 `GeneratePeriodModal.vue` 就踩了）。
- **⚠️ 弹窗一律固定高度（2026-09-16 皇上要求「弹窗都要固定好高度」）**：各阶段/各状态切换时**不要跳高跳矮**。落点是 **`.el-dialog__body`**（`::v-deep` 穿透 scoped），不要在内部各块上东设一个西设一个 —— body 一处设好就吸收掉所有状态差异。
  - 纯表单类（导入人员）：`::v-deep .el-dialog__body { height: 300px; overflow-y: auto }`。
  - 选人类（UserPicker）：`height: 424px; max-height: 70vh; overflow-y: auto`。
  - **内容多到可能超屏的**（生成期次）：`height` 保证不跳动 + `max-height: calc(90vh - 150px)` 兜底 + `top="5vh"` 调小（本次整窗 975px → 534px）。
  - **空态与列表必须同高**，否则「0 人 → 有人」会跳一下（`.gpd-empty` 要给和 `.gpd-members` 一样的 `height` + flex 居中，不能用 `padding` 撑）。
  - 实测：导入弹窗 300/300；选人弹窗未选 562 / 已选 3 人 562；生成期次人员列表恒 232px。
- **逐人任务名弹窗的左右布局（2026-09-16 定稿，皇上两轮修改后）**：必须左右分栏 —— 左侧选人表格（`flex: 0 0 456px`，逐人模式隐藏手机号列）、右侧逐人输入框（`flex: 1` 吃掉剩余宽度，把空间都给任务名），弹窗宽 **1000px**。右侧未选人时给引导文案；已选时**每人一行**：`姓名（52px）+ 用户号（78px 灰色）+ 任务名输入框（flex:1）`，行高约 39px。左侧表格 `height="380"`、右侧列表 `height: 347px`。
  - ❌ **不要放表格下方**：皇上原话「不然用户可能没看下面的就直接添加了」—— 用户勾完人通常会直接点确定，下方的填写区等于不存在。
  - 通用经验：**需要用户逐条填写的内容，要和触发它的操作并排**（边选边填），不要放在其后；**逐条列表每项保持一行**（皇上原话「添加的用户，成一行，不要分两行」），信息塞不下就把次要信息（如部门）移到 `title`。
- **经验**：皇上说「XX 没办法设置」时，**先分清是「找不到入口」/「功能缺失」/「填了不生效」再动手** —— 本次功能本就存在，只是埋在筛选框里、样式与筛选条件一样，看起来像第三个筛选条件。先问准诉求（皇上要的是「逐人单独设名」）才没白做。
- **启动相关**：`/tmp/flow-cp.txt` 是**加 POI 依赖之前**生成的，直接拿它启动会 `NoClassDefFoundError`。**只要动过 pom 就必须重建 classpath**（`mvn -o -q org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath -Dmdep.outputFile=/tmp/flow-cp.txt`，重建后从 8590 → 9645 字节）。



