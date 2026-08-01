# sys_user 用户管理 CRUD 实现计划

## Context（背景）

为流程填报系统（flow 库）实现第一批业务功能：系统用户 `sys_user` 的管理界面与增删改查。
- 后端：引入 MyBatis-Plus（用户明确要求），新建 sys_user 模块，数据源切到 flow 库。
- 前端：参考 `src/views/dashboard` 的列表页+弹窗模式，新建用户管理页面，通过 proxy 调通后端真实接口。
- 决策（已与用户确认）：①后端数据源从 test001 切到 flow 库（旧 house 接口随之失效，前端 dashboard 走 mock 不受影响）；②sys_user 接口放行不鉴权（开发阶段快速调通）。

现有约定（复用）：
- 后端 `Result<T>`（[Result.java](file:///e:/code/flow/flow-service/src/main/java/com/zqk/house/util/Result.java)）：`code/message/data`，静态 `success/fail/notFound`。
- 后端 `PageResult<T>`（[PageResult.java](file:///e:/code/flow/flow-service/src/main/java/com/zqk/house/util/PageResult.java)）：`records/total`。
- 后端 Service 风格：直接 `@Service` 类（参考 [RentPaymentService.java](file:///e:/code/flow/flow-service/src/main/java/com/zqk/house/rentpayment/service/RentPaymentService.java)），不拆 interface/impl。
- 后端 Controller 风格：`@RestController + @RequestMapping + @CrossOrigin`，返回 `Result<T>`（参考 [RentPaymentController.java](file:///e:/code/flow/flow-service/src/main/java/com/zqk/house/rentpayment/controller/RentPaymentController.java)）。
- 前端列表页/弹窗模式：[dashboard/index.vue](file:///e:/code/flow/flow-front/src/views/dashboard/index.vue) + [AddModal.vue](file:///e:/code/flow/flow-front/src/views/dashboard/components/AddModal.vue) + [DeleteModal.vue](file:///e:/code/flow/flow-front/src/views/dashboard/components/DeleteModal.vue)。
- 前端 proxy 已配好：`/dev-api` → `http://localhost:9000`，pathRewrite 到 `/house-service`（[vue.config.js](file:///e:/code/flow/flow-front/vue.config.js)）。

关键兼容性问题：前端 [request.js](file:///e:/code/flow/flow-front/src/utils/request.js) 响应拦截器只认 `code===20000`（mock 约定），且 token 用 `X-Token` 头；后端返回 `code:200`、读 `Authorization` 头。两者不兼容，故 sys_user 前端**新建独立 axios 实例**，不复用 request.js。

---

## 一、后端改动

### 1. pom.xml —— 引入 MyBatis-Plus
- 移除 `org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.4`（及 test 依赖）
- 新增 `com.baomidou:mybatis-plus-spring-boot3-starter:3.5.5`（支持 Spring Boot 3.4.2 / Java 21）
- MP starter 已包含 mybatis，现有 xml mapper 仍可正常工作

### 2. application.yml —— 切数据源 + 改 MP 配置
- 数据源 `url` 改为 `jdbc:mysql://localhost:3306/flow?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true`
- 配置键 `mybatis:` 改为 `mybatis-plus:`（`mapper-locations` / `type-aliases-package` / `configuration.map-underscore-to-camel-case` / `log-impl` 保持不变）

### 3. 新建 MP 分页插件配置
新建 `com.zqk.house.config.MybatisPlusConfig`：注册 `MybatisPlusInterceptor` + `PaginationInnerInterceptor(DbType.MYSQL)`。

### 4. HouseApplication.java —— 扩展 @MapperScan
`@MapperScan` 增加 `com.zqk.house.sysuser.mapper`。

### 5. WebConfig.java —— 放行 sys-user 接口
`addInterceptors` 增加 `.excludePathPatterns("/sys-user/**")`。

### 6. 新建 sys_user 模块（包 `com.zqk.house.sysuser`）

**entity/SysUser.java**
- `@Data` + `@TableName("sys_user")`
- `@TableId(type = IdType.AUTO)` 主键 id（Long）
- 字段：username, empNo, realName, deptId(Long), deptName, password, phone, status(Integer), createTime
- `password` 字段加 `@TableField(select = false)`（默认查询不返回密码；新增时仍可写入）

**mapper/SysUserMapper.java**
- `@Mapper` + `extends com.baomidou.mybatisplus.core.mapper.BaseMapper<SysUser>`（用 MP 全限定 BaseMapper，与现有自定义 `com.zqk.house.util.BaseMapper` 区分，不冲突）
- 自定义分页方法：`IPage<SysUser> selectUserPage(Page<SysUser> page, @Param("ew") Wrapper<SysUser> wrapper)`（或在 service 里用 `mapper.selectPage`，可不写自定义方法）

**service/SysUserService.java**（直接 @Service 类）
- `getPage(page, limit, filters)`：用 `LambdaQueryWrapper` 按 username/empNo/realName/deptName/status 模糊/等值过滤，`mapper.selectPage(new Page<>(page,limit), wrapper)`，结果转 `PageResult`
- `addUser(SysUser)` / `updateUser(SysUser)`（MP 默认 NOT_NULL 策略，password 为空不更新）/ `deleteUser(Long id)` / `getUserById(Long id)`（详情单独查含 password 或不含，按需）

**controller/SysUserController.java**
- `@RestController @RequestMapping("/sys-user") @CrossOrigin`
- 接口（风格对齐 rentpayment）：
  - `POST /sys-user/list` → `Result<PageResult<SysUser>>`，body: `{page, limit, username, empNo, realName, deptName, status}`
  - `POST /sys-user/add` → `Result<Void>`
  - `PUT /sys-user/update` → `Result<Void>`
  - `DELETE /sys-user/delete/{id}` → `Result<Void>`
  - `GET /sys-user/{id}` → `Result<SysUser>`

---

## 二、前端改动

### 1. 新建独立 axios 实例 `src/utils/request-flow.js`
- `baseURL: process.env.VUE_APP_BASE_API`（/dev-api）
- 响应拦截器：`res.code === 200` 视为成功返回 `res`，否则 reject（与后端 Result 对齐）
- 不带 token（接口放行）

### 2. 新建 `src/api/sysuser.js`
- `getUserList(params)` POST `/sys-user/list`
- `addUser(data)` POST `/sys-user/add`
- `updateUser(data)` PUT `/sys-user/update`
- `deleteUser(id)` DELETE `/sys-user/delete/{id}`
- `getUserDetail(id)` GET `/sys-user/{id}`

### 3. 新建页面 `src/views/sysuser/`
- **index.vue**：参考 [dashboard/index.vue](file:///e:/code/flow/flow-front/src/views/dashboard/index.vue) 结构，**简化**（去掉统计卡片、催办、导入导出、进度条）。保留：面包屑+标题、筛选区（用户名/员工号/真实姓名/部门/状态）、新增按钮、数据表格、分页、弹窗调用。
  - 表格列：员工号、用户名、员工姓名、部门ID、部门名称、手机号、状态、创建时间、操作（详情/编辑/删除）
  - `fetchData()` 调 `getUserList({page: currentPage, limit: pageSize, ...filters})`，取 `response.data.records` / `response.data.total`
- **components/AddModal.vue**：参考 dashboard AddModal，字段：用户名*、员工号*、员工姓名*、部门ID、部门名称、密码（新增必填/编辑可空）、手机号、状态（select 正常/禁用）
- **components/DeleteModal.vue**：参考 dashboard DeleteModal，确认删除
- **components/DetailModal.vue**：只读展示字段

### 4. router/index.js —— 加菜单
在 constantRoutes 增加：
```js
{
  path: '/sysuser',
  component: Layout,
  children: [{
    path: 'index',
    name: 'SysUser',
    component: () => import('@/views/sysuser/index'),
    meta: { title: '用户管理', icon: 'el-icon-user' }
  }]
}
```

---

## 三、验证

1. **后端重启**：`mvn spring-boot:run`（禁用沙箱，设 JAVA_HOME）。确认日志：Tomcat 9000 启动、数据源 flow 库、无 MP 报错。
2. **接口直测**：`POST http://localhost:9000/house-service/sys-user/add` 新增一条 → `POST /sys-user/list` 查列表 → 验证返回 `code:200` + records/total。
3. **前端**：dev server 热重载（仅新增文件，vue.config.js 未改）。浏览器开 `http://localhost:9528`，侧边栏点「用户管理」→ 验证列表加载、新增、编辑、删除、详情全流程。
4. **proxy 链路**：确认前端请求经 `/dev-api/sys-user/*` → proxy → 后端 `/house-service/sys-user/*` 正常（bodyParser 已修复，body 不会丢失）。
5. **回归**：确认现有 house 接口失效属预期（已切库）；前端 dashboard 仍走 mock 正常。

---

## 四、风险与注意

- **MP 与现有自定义 BaseMapper 同名**：sys_user 用 MP 的 `BaseMapper`（全限定 import），现有模块继续用 `com.zqk.house.util.BaseMapper`，互不影响。
- **mybatis → mybatis-plus 配置变更**：现有 room/rentpayment mapper 依赖 `classpath:mapper/*.xml`，MP 同样扫描该路径，兼容。引入后验证后端启动无 MapperBinding 异常。
- **password 安全**：明文存储（与现有 house user 一致），`@TableField(select=false)` 避免列表/详情泄露。后续可加加密。
- **旧 house 接口失效**：切 flow 库后 `/user`、`/room`、`/payment` 接口会因表不存在报错，属预期（前端 dashboard 走 mock，不受影响）。
