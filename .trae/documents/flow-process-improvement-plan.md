# 流程处理系统改进实施计划

> 基于用户 12 项需求，对任务处理、数据后台、模板设计器进行重构，并按 `sysuser` 格式拆分复杂弹窗组件。
> 用户已确认两个关键决策：**下一处理人多选（每人独立并行分支）**、**模板新建不自动创建开始/结束节点**。

---

## 一、当前状态分析

### 前端
- [taskprocess/index.vue](file:///e:/code/flow/flow-front/src/views/taskprocess/index.vue)：处理弹窗内联在页面中（未拆分）；下一处理人单选（`highlight-current-row`，选中态易丢失=用户反馈的"选中有点问题"）；通过/退回按钮直接提交无确认；退回固定回开始节点；节点提示在 info-section 顶部。
- [dataadmin/index.vue](file:///e:/code/flow/flow-front/src/views/dataadmin/index.vue)：三级下钻（任务→人员→流程）；流程链用 `latestByNode` 取最新 task_node，退回节点仍以 `rejected` 状态出现在链中；`expandedNodeId` 单值手风琴式展开；无任务下发入口。
- [taskdispatch/index.vue](file:///e:/code/flow/flow-front/src/views/taskdispatch/index.vue)：独立三步下发页，含多选勾选框选人弹窗（可复用）。
- [formdesigner/index.vue](file:///e:/code/flow/flow-front/src/views/formdesigner/index.vue)：`initDefaultNodes()` 预创建名为"开始""结束"的两个节点，用户误以为是不可改的系统节点。

### 后端
- [FlowTaskService.submit()](file:///e:/code/flow/flow-service/src/main/java/com/zqk/house/flowtask/service/FlowTaskService.java)：退回分支硬编码回开始节点；通过分支只创建 1 个下一 task_node。
- [NodeSubmitDTO](file:///e:/code/flow/flow-service/src/main/java/com/zqk/house/flowtask/vo/NodeSubmitDTO.java)：仅 `nextHandlerUserId`（单值）、无退回目标字段。
- [FlowTaskNode](file:///e:/code/flow/flow-service/src/main/java/com/zqk/house/flowtask/entity/FlowTaskNode.java)：已有 `action` 字段（0通过/1退回），已记录每次动作。
- [TaskDetailVO](file:///e:/code/flow/flow-service/src/main/java/com/zqk/house/flowtask/vo/TaskDetailVO.java)：有 `currentFormData` 字段但从未填充（退回重填需要用它）。
- [FlowTaskNodeMapper.xml](file:///e:/code/flow-service/src/main/resources/mapper/FlowTaskNodeMapper.xml) `selectMyTodo`：按 `submit_status=0 AND task.status=1` 查询，不限定处理人（符合"任意登录用户可处理任意任务"约束），分支后天然兼容。

### 关键结论
- **多选分支无需 DB schema 变更**：`flow_task_node` 已有 `action` 字段；分支=在同一 nodeId 创建多条 pending task_node；任务完成判定=无 pending task_node 残留。
- 流程链展示始终基于 `templateNodes`（模板节点链，按 nodeId 唯一），分支不破坏展示。
- "任意用户可处理任意任务"规则使多选分支与现有 my-todo 查询自然兼容。

---

## 二、架构设计：多选分支模型

### 数据模型（无 schema 变更）
- `flow_task_node` 为工作单元。一个任务在同一 nodeId 可有多条 task_node（不同处理人并行）。
- `flow_task.current_node_id` / `current_handler_id` 降级为"代表性最新值"（用于任务列表简略展示）；真实待办以 `task_node.submit_status=0` 为准。
- 任务完成：每次 submit 后检查 `SELECT COUNT(*) FROM flow_task_node WHERE task_id=? AND submit_status=0`，为 0 则 `task.status=2`。
- `finishedNodeCount` 重定义为"有 ≥1 条已 done task_node 的 nodeId 数量"。

### submit(pass) 多人分支流程
1. 校验当前 task_node 未处理、task 进行中、节点一致。
2. 保存表单 → `form_record` + `form_data`，得 `recordId`。
3. 标记当前 task_node：`submit_status=1, action=0, handle_time, form_record_id=recordId`。
4. 当前为结束节点 → 不创建下游；否则查下一模板节点，**为每个选中处理人创建一条 pending task_node**（`submit_status=0, action=0`）。
5. 更新 `task.current_node_id=下一nodeId, current_handler_id=首选处理人, finished_node_count=重算`。
6. 若无 pending task_node 残留 → `task.status=2`。

### submit(reject) 退回到指定节点
1. 标记当前 task_node：`submit_status=1, action=1, handle_time, form_record_id`。
2. 校验 `rejectToNodeId`（模板节点，sortNum < 当前）。
3. 创建一条 pending task_node 于目标 nodeId，`handler_user_id=当前处理人`（退回人自己重做），`submit_status=0`。
4. 更新 `task.current_node_id=目标nodeId, current_handler_id=当前处理人`。
5. 退回不改变 finished_node_count（不算完成进度）。

### 退回表单回填
- `getDetail()` 中，当任务进行中且当前节点有历史已 done task_node 时，取最近一条的 `form_record_id` → 查 `form_data` → 填入 `TaskDetailVO.currentFormData`。
- 前端初始化 `formData` 时优先用 `currentFormData`，无则空表单。

---

## 三、文件级改动清单

### 后端（flow-service）

#### 1. [NodeSubmitDTO.java](file:///e:/code/flow/flow-service/src/main/java/com/zqk/house/flowtask/vo/NodeSubmitDTO.java)
- 新增 `private List<Long> nextHandlerIds;`（多选下一处理人；通过且非结束节点必填）。
- 新增 `private Long rejectToNodeId;`（退回目标模板节点ID；action=reject 时必填）。
- 保留 `nextHandlerUserId`（废弃，向后兼容不删，submit 优先读 nextHandlerIds）。
- 更新字段注释。

#### 2. [FlowTaskService.java](file:///e:/code/flow/flow-service/src/main/java/com/zqk/house/flowtask/service/FlowTaskService.java)
- `submit()` 重写：
  - pass 分支：循环 `nextHandlerIds` 创建多条 task_node；结束节点不创建；末尾调用 `checkAndFinishTask(taskId)`。
  - reject 分支：用 `dto.getRejectToNodeId()` 定位目标节点（校验 sortNum<当前且属于同模板）；创建 pending task_node 于目标节点，handler=当前用户；调用 `checkAndFinishTask`。
  - 抽取 `checkAndFinishTask(Long taskId)`：无 pending task_node → status=2。
  - 抽取 `recomputeFinishedCount(Long taskId)`：重算 finished_node_count。
- `getDetail()`：填充 `currentFormData`（当前节点最近已 done task_node 的表单数据），用于退回重填。
- 校验：开始节点不可退回（保持）；退回目标须为已到达过的节点（sortNum<当前）。

#### 3. [FlowTaskNodeMapper.xml](file:///e:/code/flow/flow-service/src/main/resources/mapper/FlowTaskNodeMapper.xml)
- `selectTaskProgress`：保持返回全部 task_node（分支后同 nodeId 多条），前端聚合。
- 无需新增 SQL（完成判定用 BaseMapper `selectCount`）。

### 前端（flow-front）

#### A. 任务处理页 [taskprocess/](file:///e:/code/flow/flow-front/src/views/taskprocess/)

**新建 `taskprocess/components/` 目录，拆分以下组件：**

##### A1. `components/ProcessDialog.vue`（主处理弹窗，从 index.vue 抽出）
- Props：`visible`, `todo`, `detail`（TaskDetailVO）, `submitting`。
- 包含：任务信息区、流程链、动态表单、下一处理人区（含节点提示）、底部通过/退回按钮。
- **节点提示移到下一处理人区旁边**（需求1）：在"指定下一节点处理人"标题旁显示 `currentTodo.nodeTips`（醒目样式）。
- **表单回填**（需求2）：初始化 `formData` 时优先用 `detail.currentFormData`。
- **多选处理人**（需求7）：`nextHandlers` 为数组；点击"选择处理人"打开 `UserPicker`（多选）。
- **退回目标选择**（需求6）：点"退回"先开 `RejectTargetModal` 选目标节点，再开 `ConfirmActionModal` 确认。
- **确认弹窗**（需求4）：通过/退回前调 `ConfirmActionModal`。
- Emits：`close`, `submit({ action, formData, nextHandlerIds, rejectToNodeId })`。

##### A2. `components/UserPicker.vue`（多选选人弹窗）
- Props：`visible`, `excludeIds`（已选处理人ID，禁用勾选）。
- 用 `el-table` + `type="selection"` 列（勾选框，修复选中bug）。
- 查询/筛选逻辑从 taskdispatch 选人弹窗复用。
- Emits：`confirm(selectedUsers[])`, `close`。

##### A3. `components/RejectTargetModal.vue`（退回目标节点选择）
- Props：`visible`, `processedNodes`（已处理过的节点列表，供选择）。
- 单选列表（radio），展示节点名+处理人+时间。
- Emits：`confirm(targetNodeId)`, `close`。

##### A4. `components/ConfirmActionModal.vue`（通过/退回确认）
- Props：`visible`, `action`（'pass'/'reject'）, `summary`（摘要文案：将流转给 X 人 / 退回到 XX 节点）。
- Emits：`confirm`, `close`。
- 用 `el-dialog` 简单实现（比 `$confirm` 更可控、可复用）。

##### A5. `taskprocess/index.vue`（精简后）
- 仅保留：列表表格、分页、数据加载。
- 引入 `ProcessDialog` 等子组件，父组件管理数据与 submit 调用。
- `handleSubmit` 组装 payload：`{ taskId, taskNodeId, formData, action, nextHandlerIds, rejectToNodeId }`。

#### B. 数据后台页 [dataadmin/](file:///e:/code/flow/flow-front/src/views/dataadmin/)

**新建 `dataadmin/components/` 目录：**

##### B1. `components/DispatchModal.vue`（任务下发弹窗，从 taskdispatch 抽出）
- 三步流程（选模板→任务详情→选首节点处理人）整合为一个 `el-dialog`。
- 复用 taskdispatch 的模板卡片、表单、多选选人逻辑。
- Props：`visible`。Emits：`success(count)`, `close`。
- 下发成功后刷新任务列表。

##### B2. `components/HandlerFlowDetail.vue`（人员流程详情，第三级）
- 展示流程链（去重，需求8）+ 折叠面板多展开（需求9）+ 动作历史右侧展示（需求10）。
- 流程链去重逻辑：仅显示 `templateNodes[0..maxReachedIndex]`（maxReachedIndex=有 task_node 的最大 sortNum）；按 nodeId 聚合状态：任一 pending→current，任一 done&action=0→done，否则→pending；**不显示 rejected 状态的链步骤**（退回仅在动作历史展示）。
- 多展开：`expandedNodeIds` 数组（Set 语义），`toggleNodeForm` 增删。
- 展开内容两栏：左=表单数据，右=动作历史（该 nodeId 所有 task_node 的 action/handler/time 列表，含退回记录）。

##### B3. `dataadmin/index.vue`（改造）
- 第一级新增"下发任务"按钮（需求11），打开 `DispatchModal`。
- 第三级改为渲染 `HandlerFlowDetail` 组件。
- 第二级人员列表保持不变。

#### C. 任务下发页 [taskdispatch/index.vue](file:///e:/code/flow/flow-front/src/views/taskdispatch/index.vue)
- 内容已迁移到 `DispatchModal.vue`。
- 将独立页面路由+菜单项移除（任务下发入口统一为数据后台的按钮）。
- 若路由配置 [router/index.js](file:///e:/code/flow/flow-front/src/router/index.js) 有 `/task-dispatch` 菜单，删除该项。

#### D. 模板设计器 [formdesigner/index.vue](file:///e:/code/flow/flow-front/src/views/formdesigner/index.vue)
- `initDefaultNodes()`：改为 `this.nodes = []`（不自动创建节点）。
- 空状态：节点链区域显示引导文案"点击下方「添加节点」开始设计流程；首位自动标记为「开始」，末位自动标记为「结束」"。
- `fixNodeTypes()`：保持按位置自动判定首位=1、末位=3、中间=2。
- `handleSave()` 校验：节点数 ≥2（首位开始、末位结束）；移除"至少保留开始和结束两个节点"的旧提示，改为"至少添加 2 个节点"。
- `removeNode`：校验改为 `nodes.length <= 2` 时禁止删除最后一个中间节点（保持 ≥2）。
- 节点链为空时"添加节点"按钮醒目引导。

#### E. 前端 API [api/task.js](file:///e:/code/flow/flow-front/src/api/task.js)
- `submitTask` 入参结构变更：`nextHandlerIds`(数组) + `rejectToNodeId`，无需改函数签名（仍传 data）。

---

## 四、需求→改动映射核对

| # | 需求 | 改动位置 |
|---|------|----------|
| 1 | 节点提示放下一处理人旁 | ProcessDialog.vue 下一处理人区 |
| 2 | 退回数据重填表单 | FlowTaskService.getDetail 填充 currentFormData + ProcessDialog 初始化 |
| 3 | 记录展示通过/退回动作 | 已有 action 字段 + HandlerFlowDetail 动作历史栏 |
| 4 | 通过/退回确认弹窗 | ConfirmActionModal.vue |
| 5 | 模板默认节点误解 | formdesigner initDefaultNodes 改空 + 引导文案 |
| 6 | 退回选择目标节点 | RejectTargetModal.vue + NodeSubmitDTO.rejectToNodeId + submit reject 分支 |
| 7 | 多选下一处理人(勾选框) | UserPicker.vue 多选 + NodeSubmitDTO.nextHandlerIds + submit 循环创建 |
| 8 | 数据后台流程链去重 | HandlerFlowDetail flowChain 聚合+截断 |
| 9 | 折叠面板多展开 | HandlerFlowDetail expandedNodeIds 数组 |
| 10 | 动作展示在展开右侧 | HandlerFlowDetail 两栏布局 |
| 11 | 任务下发整合数据后台 | DispatchModal.vue + dataadmin 按钮 + 移除 taskdispatch 路由 |
| 12 | 拆分复杂弹窗 | taskprocess/components/ + dataadmin/components/ |

---

## 五、假设与决策

1. **分支模型**：多选下一处理人=在同一下一 nodeId 为每选一人创建一条 pending task_node，各自独立向下流转；任务完成=无 pending task_node 残留。无 DB schema 变更。
2. **退回重填**：退回到目标节点后，表单预填该节点最近一次已提交数据（供修改重交），而非清空。
3. **退回处理人**：退回创建的 pending task_node 的 handler=退回操作人自己（其重做该节点）；符合"任意用户可处理任意任务"规则。
4. **任务下发页移除**：功能迁移到 dataadmin 的 DispatchModal 后，删除独立 taskdispatch 路由与菜单项。
5. **流程链截断仅 dataadmin**：taskprocess 处理弹窗仍显示完整链（含未到灰色节点，供处理人看全貌）；dataadmin 仅显示开始到当前节点。
6. **finished_node_count 重算**：每次 submit 后重算为"有 done task_node 的 nodeId 数量"，避免分支导致进度失真。
7. **保留 `nextHandlerUserId` 字段**：NodeSubmitDTO 不删旧字段（向后兼容），submit 优先读 `nextHandlerIds`。

---

## 六、实施顺序（建议）

1. **后端先行**：改 NodeSubmitDTO → 改 FlowTaskService.submit/getDetail → 重启验证接口。
2. **模板设计器**：改 formdesigner 默认节点逻辑（独立、低风险）。
3. **任务处理页拆分+多选+确认+退回目标**：新建 components → 重构 index.vue → 联调。
4. **数据后台拆分+流程链去重+多展开+动作历史+下发整合**：新建 components → 改造 index.vue → 移除 taskdispatch 路由。
5. **全链路验证**。

---

## 七、验证步骤

1. **模板设计器**：新建模板 → 节点链为空 → 添加 2 节点 → 保存 → 首位自动标"开始"、末位"结束"。
2. **任务下发**：数据后台点"下发任务" → 选模板/填信息/多选处理人 → 下发成功 → 任务列表出现。
3. **任务处理-多选流转**：处理人 A 处理 → 选下一节点 2 个处理人 B、C → 提交确认弹窗 → B、C 各收到一条待办（独立分支）。
4. **任务处理-退回目标**：处理人 D 退回 → 选目标节点（已处理过的）→ 确认 → 目标节点出现 pending task_node，表单预填上次数据。
5. **任务处理-确认弹窗**：点通过/退回均弹确认框，取消不提交。
6. **数据后台-流程链**：查看人员流程 → 链仅显示开始到当前节点，无退回重复步骤；多个折叠面板可同时展开；展开后左表单右动作历史（含退回记录）。
7. **任务完成**：所有分支到达结束节点 → 无 pending task_node → 任务状态=已完成。
8. **后端接口**：`mvn spring-boot:run` 重启；用 curl/Postman 验证 `/flow-task/submit`（多选 + 退回目标）返回 200。
