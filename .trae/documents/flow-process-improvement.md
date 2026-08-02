# 流程系统体验改进方案

## Context（背景）

用户体验系统后提出 6 项改进需求：处理步骤点击查看历史表单、除第一步外支持通过/退回、数据后台改为任务→人员→流程三级下钻、表单展示用节点名称+字段标签、删除无用的节点标识 nodeKey、处理人能看到完整流程链（未到节点灰色不可点击）、默认首页改为任务下发。

当前系统：任务处理弹窗的流转进度只显示节点名/处理人，无法点击查看历史表单；submit 仅支持正向流转无退回；数据后台按填报记录维度展示；流程链只显示已流转节点，未到节点不显示；默认首页是 Dashboard。

已与用户确认 3 个关键决策：**退回退回到开始节点（流程重走）**、**数据后台任务→人员→流程三级**、**默认首页改为任务下发**。

---

## 一、数据库变更

### 1. flow_task_node 增加 action 字段
```sql
ALTER TABLE flow_task_node ADD COLUMN action tinyint DEFAULT 0 COMMENT '操作类型 0通过 1退回';
```
用途：区分该节点是"通过"还是"退回"，前端展示退回标记。

---

## 二、后端改动

### 1. NodeSubmitDTO 增加 action 字段
文件：`flow-service/src/main/java/com/zqk/house/flowtask/vo/NodeSubmitDTO.java`
```java
/** 操作类型 pass=通过(默认) reject=退回到开始节点 */
private String action;
```

### 2. TaskProgressVO 增加 formDataList
文件：`flow-service/src/main/java/com/zqk/house/flowtask/vo/TaskProgressVO.java`
```java
/** 该节点提交的表单数据（已处理时回填，用于点击查看历史） */
private List<FormDataItemVO> formDataList;
/** 操作类型 0通过 1退回 */
private Integer action;
```
新增 `FormDataItemVO`（fieldLabel, fieldValue），放在 flowtask/vo 下。

### 3. TaskDetailVO 增加 templateNodes
文件：`flow-service/src/main/java/com/zqk/house/flowtask/vo/TaskDetailVO.java`
```java
/** 模板完整节点链（用于展示未到节点的灰色骨架） */
private List<FlowTemplateNode> templateNodes;
```

### 4. FlowTaskService 改造（核心）
文件：`flow-service/src/main/java/com/zqk/house/flowtask/service/FlowTaskService.java`

#### getDetail() 改造
- 查询模板完整节点链 `flow_template_node where templateId=? order by sortNum` → 填入 `templateNodes`
- 对已处理 task_node（有 formRecordId），批量查询 form_data 关联 fieldLabel：
  - 收集所有 formRecordId
  - 一次查询 `flow_form_data where recordId in (...)`
  - 一次查询关联的 `flow_template_field` 取 fieldLabel（按 fieldId 映射）
  - 回填到各 TaskProgressVO.formDataList

#### submit() 改造 —— 增加退回分支
当 `dto.action == "reject"` 时走退回逻辑（否则走现有正向流转）：
1. 校验：登录、task_node 存在未重复提交、是当前处理人、任务进行中、节点一致
2. 校验当前节点**不是开始节点**（nodeType != 1）—— 第一步不能退回
3. 校验必填字段（退回也要填表单作为退回意见）
4. 插 form_record + form_data（同正向）
5. 当前 task_node：submitStatus=1, action=1, handleTime, formRecordId, nextHandlerUserId=null
6. 查开始节点模板节点（nodeType=1）+ 该任务开始节点原处理人（最早的 nodeType=1 的 task_node.handlerUserId）
7. 新建开始节点 task_node（submitStatus=0, handlerUserId=原处理人, action=0）
8. 更新 task：currentNodeId=开始节点ID, currentHandlerId=原处理人, finishedNodeCount=0
9. 不删除历史 task_node（保留完整流转记录）

### 5. FlowTaskNode 实体增加 action 字段
文件：`flow-service/src/main/java/com/zqk/house/flowtask/entity/FlowTaskNode.java`
```java
private Integer action;
```

### 6. mapper XML 调整
文件：`flow-service/src/main/resources/mapper/FlowTaskNodeMapper.xml`（selectTaskProgress、selectMyTodo）
- selectTaskProgress 增加 select `action` 字段

### 7. submit 接口返回
退回成功返回提示"已退回到开始节点，流程将重新开始"。

---

## 三、前端改动

### 1. 默认首页改为任务下发
文件：`flow-front/src/router/index.js`
- `redirect: '/dashboard'` → `redirect: '/task-dispatch'`

### 2. 任务处理页 taskprocess/index.vue（重点重构）
文件：`flow-front/src/views/taskprocess/index.vue`

#### 流转进度 → 完整流程链展示
- 基于 `taskDetail.templateNodes`（模板完整节点链）作为骨架
- 合并 `taskDetail.taskNodes`：按 nodeId 分组取**最新**一条（id 最大）
- 每个节点状态：
  - 有 task_node 且 submitStatus=1 且 action=0 → 已通过（绿色，可点击查看表单）
  - 有 task_node 且 action=1 → 已退回（橙色，可点击查看退回意见）
  - 有 task_node 且 submitStatus=0 → 当前处理中（蓝色高亮）
  - 无 task_node → 未到（灰色，不可点击）

#### 历史表单查看
- 点击已处理节点（绿色/橙色），展开该节点的 `formDataList`（fieldLabel + fieldValue）
- 第一步处理人无历史可看（其上无已处理节点）

#### 通过/退回按钮
- 开始节点（nodeType=1）：仅显示"提交并流转"（通过）
- 非开始节点：显示"通过"（需指定下一处理人）+ "退回"（无需指定下一处理人，退回到开始）
- 退回提交时 payload 增加 `action: 'reject'`，不传 nextHandlerUserId

### 3. 数据后台 dataadmin/index.vue 重构为三级下钻
文件：`flow-front/src/views/dataadmin/index.vue`

#### 第一级：任务列表（首页）
- 调用 `/flow-task/list` 展示所有任务（任务名、模板、状态、当前处理人、下发时间）
- 保留统计卡（总任务/进行中/已完成/我的待办）
- 点击任务行 → 进入第二级

#### 第二级：任务的处理人员
- 调用 `getTaskDetail(taskId)` 获取 taskNodes
- 从 taskNodes 提取处理人列表（去重：handlerUserId, handlerName, 处理的节点名, 状态）
- 展示为人员卡片列表
- 点击人员 → 进入第三级

#### 第三级：流程详情
- 展示完整流程链（同任务处理页的节点链展示）
- 各已处理节点可点击查看表单（fieldLabel + fieldValue，按节点名分组）
- 面包屑导航：任务列表 / 任务名称 / 处理人 / 流程详情，支持返回上级

### 4. 设计流程页 formdesigner/index.vue 删除节点标识
文件：`flow-front/src/views/formdesigner/index.vue`
- 移除右栏"节点标识"输入框（nodeKey 的 el-input）
- 后端 saveFlow 保留 nodeKey 自动生成逻辑（不影响存储），仅前端不展示

### 5. 表单展示统一用 fieldLabel
- 数据后台详情、任务处理历史表单：展示 fieldLabel（字段标签）而非 fieldKey

### 6. api/task.js 无需新增接口
- getTaskDetail 已返回所需数据（改造后含 templateNodes + 各节点 formDataList）
- submitTask 传参增加 action 字段即可

---

## 四、关键复用点

- `FlowTaskService.getDetail()`：在其基础上扩展，复用 selectTaskProgress
- `FlowFormDataMapper`（BaseMapper）：用 selectList 查 form_data，无需新方法
- `FlowTemplateFieldMapper`：查 fieldLabel 映射
- `FlowTemplateNodeMapper`：查模板完整节点链
- 前端 `getTaskDetail`、`getTaskList`、`submitTask` 接口签名不变

---

## 五、验证方法

1. **重启后端**：`mvn spring-boot:run`（flow-service 目录），确认 79 个源文件编译通过
2. **数据库执行** ALTER TABLE 增加 action 字段
3. **前端热更新**：dev-server 自动生效
4. **端到端测试**（PowerShell 脚本）：
   - 登录 → save-flow 设计 3 节点流程
   - 批量下发任务给 2 个处理人
   - 处理人 A 提交第一步（通过，流转到第二步）
   - 处理人 B 在第二步选择"退回" → 验证任务回到开始节点、A 收到重新处理待办
   - 处理人 A 重新提交 → B 处理 → 完成
   - 查看任务详情：完整流程链展示，已处理节点可点击查看表单
   - 数据后台：任务列表 → 点击任务看人员 → 点击人员看流程
5. **UI 验证**：登录后默认进任务下发页；设计流程页无节点标识输入框；未到节点灰色不可点击
