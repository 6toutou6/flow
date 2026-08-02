# 流程填报系统 4 个页面前后端实现

## Context

项目 `e:\code\flow` 已有登录（sys_user + JWT + SecurityUtils）和用户管理页。现需按设计稿 `stitch_flow_task_collection_system/axiom_workflow_1-4`（中文版）实现流程填报系统的 4 个核心页面：模板管理、表单设计器、任务下发、数据后台。数据库 `flow` 库的 8 张表已建好（`flow_schema.sql`）。用户决策：4 页全做、前后端打通、红色主题 #C53030、前端目录按 sysUser 模式。

## 关键决策

- **表单设计器**：左栏字段库点击添加到中栏画布 + 选中字段右栏配置属性 + 上下移动/删除（非完整拖拽）
- **enum_options 字段**：实体用 `String` 接收，前端 `JSON.parse/stringify` 处理（避免 MyBatis-Plus TypeHandler 在分页中的兼容坑）
- **趋势图**：纯 CSS bar 实现，零依赖（与设计稿一致，不引 echarts）
- **模板版本快照**：v0.1 简化，编辑模板字段时前端提示「修改将影响已有任务」；任务创建时锁定 `template_version` 号
- **主题色**：改 `variables.scss` + `element-ui.scss` 全局覆盖（不重编译 Element 主题，避免构建复杂度）

## 后端实现（3 个模块，按 sys_user 模式）

每个模块结构：`entity/` + `mapper/`(继承 BaseMapper) + `service/` + `controller/`，返回 `Result<T>`/`PageResult<T>`，分页用 `LambdaQueryWrapper + Page`，登录用户用 `SecurityUtils.getLoginUser()`。

### 模块 1：flowtemplate（模板管理 + 表单设计器）
- entity：`FlowTemplate`、`FlowTemplateField`（enumOptions 用 String）、`FlowTemplateQueryForm`
- vo：`TemplateSaveDTO`(templateId + fields[])、`TemplateDetailVO`(template + fields)、`TemplateStatsVO`(total/active/monthlyUpdateRate)
- service：`FlowTemplateService`（list/detail/save/update/toggleStatus/copy/delete/stats/enabledList）、`saveFields`(@Transactional 删旧+插新+version+1)
- controller `/flow-template`：POST `/list`、GET `/{id}`、POST `/save`、PUT `/update`、PUT `/toggle-status/{id}`、POST `/copy/{id}`、DELETE `/delete/{id}`、GET `/stats`、GET `/enabled-list`、PUT `/save-fields`

### 模块 2：flowtask（任务下发）
- entity：`FlowTask`、`FlowTaskUser`、`FlowTaskQueryForm`
- vo：`TaskCreateDTO`(templateId/taskName/taskDesc/startTime/endTime/userIds[])、`TaskDetailVO`
- service：`FlowTaskService`（list 联表 template_name、create @Transactional 插 task+批量插 task_user、end/cancel/delete）
- controller `/flow-task`：POST `/list`、GET `/{id}`、POST `/create`、PUT `/update`、PUT `/end/{id}`、PUT `/cancel/{id}`、DELETE `/delete/{id}`

### 模块 3：flowdata（数据后台）
- entity：`FlowFormRecord`、`FlowFormData`、`FlowAttachment`
- vo：`FormRecordQueryForm`、`FormRecordListVO`(联表 user.realName/template.templateName)、`FormRecordDetailVO`、`DataStatsVO`(total/pending/completionRate)、`TrendPointVO`(date/count)
- mapper XML：`FlowFormRecordMapper.xml`（联表 sys_user+flow_template 查询、按天分组趋势、统计 SQL）
- controller `/flow-data`：POST `/list`、GET `/record/{id}`、GET `/stats`、GET `/trend?days=30`

### 后端配置修改
- `application.yml`：`mybatis-plus.type-aliases-package` 改为 `com.zqk.house`（覆盖所有子模块）
- `HouseApplication.java` 的 `@MapperScan` 确认含 `com.zqk.house.*.mapper`（已是 `com.zqk.house.**.mapper` 或显式列，需确认覆盖新包）

## 前端实现（4 页面 + API + 路由 + 主题）

### API（3 文件，参考 `api/sysuser.js` 用 request-flow）
- `api/template.js`：getTemplateList/detail/add/update/toggleStatus/copy/delete/stats/enabledList/saveTemplateFields
- `api/task.js`：getTaskList/detail/create/update/end/cancel/delete（选人复用 `sysuser.js` 的 getUserList）
- `api/data.js`：getRecordList/detail/getDataStats/getDataTrend

### 路由（`router/index.js` 的 constantRoutes 加 4 项）
- `/flow-template` 模板管理（icon el-icon-document）
- `/form-designer` 表单设计器（hidden:true，从模板管理跳转，query 带 templateId）
- `/task-dispatch` 任务下发（icon el-icon-s-promotion）
- `/data-admin` 数据后台（icon el-icon-data-analysis）

### 页面（views/模块名/index.vue + components/，参考 sysuser 风格）
- `views/flowtemplate/`：index.vue（3 统计卡 + 筛选 + 表格6列：模板名称/类别/版本/状态开关/最后更新/操作 + 分页）+ components/TemplateFormModal.vue（新建/编辑元数据）+ DeleteModal.vue
- `views/formdesigner/`：index.vue（三栏，集中管理 fields 数组）+ components/FieldLibrary.vue（8 字段类型卡片）+ FormCanvas.vue（字段列表+选中）+ FieldCard.vue（上移/下移/删除）+ PropertyPanel.vue（动态属性：通用/maxLength/enumOptions/file 配置）
- `views/taskdispatch/`：index.vue（el-steps 三步）+ components/TemplateSelect.vue（卡片选模板）+ TaskDetailForm.vue（名称/起止时间/说明）+ UserAssign.vue + UserPickerModal.vue（el-table 多选 sys_user）
- `views/dataadmin/`：index.vue（3 统计卡 + 数据表格：用户姓名/提交时间/模板/状态/操作 + 分页 + CSS bar 趋势图）+ components/RecordDetailModal.vue + TrendChart.vue

### 主题色改造
- `styles/variables.scss`：`$primaryColor: #C53030`（原 #a20513）
- `styles/element-ui.scss`：追加 Element UI 主色覆盖（el-button--primary / el-pagination active / el-step / el-switch / el-checkbox / el-radio / el-tabs / el-input focus / el-tag 等）
- sysuser 页样式 `#a20513` → `#C53030`（保持全站一致）

## 实现顺序

1. **后端基座**：改 application.yml + HouseApplication @MapperScan → flowtemplate 模块全部 → flowtask 模块 → flowdata 模块（含 XML 联表 SQL）
2. **前端模板管理**：api/template.js + flowtemplate 页面 + 弹窗，列表 CRUD 跑通
3. **前端表单设计器**：formdesigner 三栏 + 字段增删改查 + 保存
4. **前端任务下发**：taskdispatch 三步 + 选人
5. **前端数据后台**：dataadmin 统计 + 表格 + 趋势图 + 详情弹窗
6. **主题色统一**：variables.scss + element-ui.scss + sysuser 样式替换
7. **联调 + 测试数据**：造模板→下发任务→SQL 插填报记录→数据后台验证

## 验证方法

1. 启动后端（mvn spring-boot:run）+ 前端（npm run dev），登录 zhangsan/123456
2. 模板管理：创建模板 → 设计表单（添加几个字段保存）→ 启停/复制/删除
3. 任务下发：选模板 → 填任务详情 → 选 sys_user 人员 → 下发
4. 数据后台：SQL 插入 flow_form_record + flow_form_data 测试数据 → 查看列表/统计/趋势/详情
5. 主题色：检查按钮/开关/分页/步骤条均为 #C53030 红色
6. 接口测试：带 token 访问各 /flow-* 接口返回 200，SecurityUtils 能取到登录用户

## 文件清单概览

- 后端新增：3 模块 × (entity/mapper/service/controller/vo) ≈ 25 个 Java + 3 个 Mapper XML
- 后端修改：application.yml、HouseApplication @MapperScan
- 前端新增：3 api + 4 页面目录(约 16 vue)
- 前端修改：router/index.js、styles/variables.scss、styles/element-ui.scss、sysuser 样式
