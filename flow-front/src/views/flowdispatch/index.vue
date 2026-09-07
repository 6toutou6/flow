<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- 页头 -->
        <div class="page-header">
          <div>
            <nav class="breadcrumb">
              <span>系统首页</span>
              <span>/</span>
              <span class="active">任务管理</span>
            </nav>
            <h3 class="page-heading">任务管理</h3>
          </div>
          <div class="header-actions">
            <button class="btn-tpl" @click="$router.push('/flow-dispatch/config-template')"><i class="el-icon-setting" /> 下发配置模板</button>
            <button class="btn-create" @click="openCreate"><i class="el-icon-plus" /> 新建任务</button>
          </div>
        </div>

        <!-- 统计卡（全部可点击筛选，可叠加：状态与样例不同维） -->
        <section class="stats-grid">
          <div class="stat-card clickable" :class="{ active: statActive('all') }" title="点击显示全部任务" @click="toggleStat('all')">
            <div class="stat-icon icon-total"><i class="el-icon-s-order" /></div>
            <div class="stat-body">
              <div class="stat-label">任务总数</div>
              <div class="stat-value">{{ stats.taskCount || 0 }}</div>
            </div>
          </div>
          <div class="stat-card clickable" :class="{ active: statActive('active') }" title="点击筛选启用中的任务，再点取消" @click="toggleStat('active')">
            <div class="stat-icon icon-active"><i class="el-icon-circle-check" /></div>
            <div class="stat-body">
              <div class="stat-label">启用中</div>
              <div class="stat-value">{{ stats.activeCount || 0 }}</div>
            </div>
          </div>
          <div class="stat-card clickable" :class="{ active: statActive('stopped') }" title="点击筛选停用中的任务，再点取消" @click="toggleStat('stopped')">
            <div class="stat-icon icon-stopped"><i class="el-icon-remove-outline" /></div>
            <div class="stat-body">
              <div class="stat-label">停用中</div>
              <div class="stat-value">{{ stats.stoppedCount || 0 }}</div>
            </div>
          </div>
          <div class="stat-card clickable" :class="{ active: statActive('sample') }" title="点击筛选样例任务（公共可见），再点取消" @click="toggleStat('sample')">
            <div class="stat-icon icon-sample"><i class="el-icon-star-on" /></div>
            <div class="stat-body">
              <div class="stat-label">样例任务</div>
              <div class="stat-value">{{ stats.sampleCount || 0 }}</div>
            </div>
          </div>
        </section>

        <!-- 提示条 -->
        <section class="tip-bar">
          <i class="el-icon-info" />
          展开任务可查看期次情况与下一期次下发时间；点「生成期次」可在弹窗中确认本期次人员后下发。
        </section>

        <!-- 到期待下发提示 -->
        <section v-if="dueList.length > 0" class="due-bar">
          <i class="el-icon-alarm-clock" />
          <span class="due-text">有 <b>{{ dueList.length }}</b> 个周期期次已到下发时间：</span>
          <span class="due-item" v-for="(d, i) in dueList" :key="d.taskId + '-' + i">
            「{{ d.taskName }}」· {{ d.periodName || '第' + d.periodNo + '期' }}
          </span>
          <button class="due-btn" :disabled="dispatching" @click="handleAutoDispatch">
            <i v-if="dispatching" class="el-icon-loading" /><i v-else class="el-icon-s-promotion" /> 一键下发
          </button>
        </section>

        <!-- 筛选区 -->
        <section class="filter-section">
          <div class="filter-grid">
            <div class="filter-item">
              <label class="filter-label">任务名称</label>
              <input v-model="filters.taskName" class="filter-input" placeholder="请输入任务名称" @keyup.enter="handleSearch">
            </div>
            <div class="filter-item">
              <label class="filter-label">状态</label>
              <select v-model="filters.status" class="filter-select">
                <option value="">全部状态</option>
                <option value="启用">启用</option>
                <option value="停用">停用</option>
              </select>
            </div>
            <div class="filter-item">
              <label class="filter-label">样例</label>
              <select v-model="filters.sample" class="filter-select">
                <option value="">全部</option>
                <option value="1">仅样例</option>
                <option value="0">仅普通</option>
              </select>
            </div>
            <div class="filter-item">
              <label class="filter-label">创建人</label>
              <input v-model="filters.creatorName" class="filter-input" placeholder="请输入创建人姓名" @keyup.enter="handleSearch">
            </div>
            <div class="filter-item">
              <label class="filter-label">创建开始</label>
              <input v-model="filters.createStart" type="date" class="filter-input">
            </div>
            <div class="filter-item">
              <label class="filter-label">创建结束</label>
              <input v-model="filters.createEnd" type="date" class="filter-input">
            </div>
          </div>
          <div class="filter-actions">
            <div class="filter-actions-right">
              <button class="btn-reset" :disabled="loading" @click="resetFilters">重置</button>
              <button class="btn-search" :disabled="loading" @click="handleSearch">
                <i v-if="loading" class="el-icon-loading" /><span v-else>查询</span>
              </button>
            </div>
          </div>
        </section>

        <!-- 任务折叠面板 -->
        <div v-if="!loading && taskList.length === 0" class="empty-state">
          <i class="el-icon-s-order" />
          <p>暂无任务，点击右上角「新建任务」创建</p>
        </div>
        <div v-else class="task-collapse">
          <div v-if="loading" class="loading-bar"><i class="el-icon-loading" /> 加载中...</div>
          <div v-for="t in taskList" :key="t.id" class="task-panel" :class="{ 'is-open': isTaskOpen(t.id), 'is-disabled': t.status !== '启用', 'flash-open': flashTasks.indexOf(t.id) >= 0 }">
            <div class="tp-head" @click="toggleTask(t.id)">
              <div class="ct-icon"><i class="el-icon-s-order" /></div>
              <div class="ct-main">
                <div class="ct-name-row">
                  <span class="ct-name">{{ t.taskName }}</span>
                  <span v-if="t.isSample === 1" class="sample-tag">样例</span>
                  <span :class="t.status === '启用' ? 'ct-status on' : 'ct-status off'">{{ t.status === '启用' ? '启用' : '停用' }}</span>
                </div>
                <div class="ct-sub">{{ cycleText(t) }}<template v-if="t.cycleType !== 4"> · {{ cycleDayText(t) }}</template> · 已下发 {{ t.periodCount || 0 }} 期 · {{ t.memberCount || 0 }} 人 · 创建人 {{ t.creatorName || '—' }}（{{ t.deptName || '—' }}）</div>
              </div>
              <i class="el-icon-arrow-down tp-arrow" />
            </div>

            <!-- 展开内容 -->
            <div v-show="isTaskOpen(t.id)" class="tp-body">
              <div class="task-detail">
                <!-- 概览 -->
                <div v-if="t.status !== '启用'" class="disabled-tip">
                  <i class="el-icon-warning-outline" />
                  <span><b>该任务已停用</b>：不能生成新期次，自动下发暂停；已下发的期次与处理进度不受影响，点击「启用」可恢复。</span>
                </div>
                <div class="overview-grid">
                  <div class="ov-item">
                    <i class="ov-icon el-icon-document" />
                    <div>
                      <span class="ov-label">流程模板</span>
                      <span class="ov-value">{{ tplName(t.templateId) }}</span>
                    </div>
                  </div>
                  <div class="ov-item">
                    <i class="ov-icon el-icon-time" />
                    <div>
                      <span class="ov-label">周期</span>
                      <span class="ov-value">{{ cycleText(t) }}<template v-if="t.cycleType !== 4"> · {{ cycleDayText(t) }}</template></span>
                    </div>
                  </div>
                  <div class="ov-item">
                    <i class="ov-icon el-icon-alarm-clock" />
                    <div>
                      <span class="ov-label">截止 / 催办</span>
                      <span class="ov-value">{{ t.deadlineDays ? '触发后 ' + t.deadlineDays + ' 天' : '—' }}<template v-if="t.urgeDays"> · 提前 {{ t.urgeDays }} 天催办</template></span>
                    </div>
                  </div>
                  <div class="ov-item">
                    <i class="ov-icon el-icon-tickets" />
                    <div>
                      <span class="ov-label">下一期次下发</span>
                      <span class="ov-value">{{ nextPeriodText(t) }}</span>
                    </div>
                  </div>
                  <div class="ov-item">
                    <i class="ov-icon el-icon-user" />
                    <div>
                      <span class="ov-label">配置人员</span>
                      <span class="ov-value">{{ t.memberCount || 0 }} 人</span>
                    </div>
                  </div>
                  <div class="ov-item">
                    <i class="ov-icon el-icon-info" />
                    <div>
                      <span class="ov-label">任务说明</span>
                      <span class="ov-value" :title="t.taskDesc">{{ t.taskDesc || '—' }}</span>
                    </div>
                  </div>
                </div>

                <!-- 操作行 -->
                <div class="action-row">
                  <template v-if="isSuperAdmin">
                    <button class="btn-ghost" @click="toggleSample(t)"><i :class="t.isSample === 1 ? 'el-icon-star-on star-on' : 'el-icon-star-off'" /> {{ t.isSample === 1 ? '取消样例' : '设为样例' }}</button>
                  </template>
                  <button class="btn-gen" :disabled="t.status !== '启用' || isSampleLocked(t)" :title="t.status !== '启用' ? '请先启用任务' : (isSampleLocked(t) ? '样例任务仅超管可操作' : '生成期次')" @click="openGen(t)">
                    <i class="el-icon-s-promotion" /> 生成期次
                  </button>
                  <button class="btn-ghost btn-person" @click="openPersonView(t)"><i class="el-icon-user" /> 按人员查看</button>
                  <button class="btn-ghost" :disabled="isSampleLocked(t)" :title="isSampleLocked(t) ? '样例任务仅超管可修改' : ''" @click="openEdit(t)"><i class="el-icon-edit" /> 编辑</button>
                  <button class="btn-ghost" :disabled="isSampleLocked(t)" :title="isSampleLocked(t) ? '样例任务仅超管可操作' : ''" @click="toggleStatus(t)"><i class="el-icon-refresh" /> {{ t.status === '启用' ? '停用' : '启用' }}</button>
                  <button class="btn-ghost text-error" :disabled="isSampleLocked(t)" :title="isSampleLocked(t) ? '样例任务仅超管可删除' : ''" @click="onDelete(t)"><i class="el-icon-delete" /> 删除</button>
                </div>

                <!-- 期次列表 -->
                <div class="period-section">
                  <div class="sec-title"><i class="el-icon-tickets" /> 期次列表 <span class="sec-sub">共 {{ taskData[t.id] ? taskData[t.id].periods.length : 0 }} 期</span></div>
                  <!-- 拉取期次时的骨架占位（含展开后尚未加载完成的情况） -->
                  <div v-if="!taskData[t.id] || taskData[t.id].loading" class="period-skeleton"><i class="el-icon-loading" /> 正在加载期次...</div>
                  <div v-else-if="taskData[t.id] && taskData[t.id].periods.length === 0" class="period-empty">暂无期次，点击上方「生成期次」下发</div>
                  <table v-else-if="taskData[t.id] && taskData[t.id].periods.length > 0" class="period-table">
                    <thead>
                      <tr>
                        <th>期次</th>
                        <th class="text-center">成员</th>
                        <th class="text-center">状态</th>
                        <th class="text-center">进度</th>
                        <th>开始时间</th>
                        <th>截止时间</th>
                        <th>催办时间</th>
                        <th>下发时间</th>
                        <th class="text-right">操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="p in periodSlice(t)" :key="p.dispatchId" class="hover-row" :class="{ 'flash-period': flashPeriod === p.dispatchId }">
                        <td class="font-bold">
                          {{ p.periodName || p.taskName }}
                          <span v-if="p.manualFlag === 1" class="tag-manual">临时</span>
                        </td>
                        <td class="text-center">{{ p.memberCount || 0 }} 人</td>
                        <td class="text-center">
                          <span :class="taskStatusClass(p.status)">{{ taskStatusText(p.status) }}</span>
                        </td>
                        <td class="text-center">
                          <template v-if="p.memberCount > 0">
                            <div class="pd-track"><div class="pd-fill" :style="{ width: periodProgress(p) + '%' }" /></div>
                            <span class="pd-text">{{ p.finishedCount || 0 }}/{{ p.memberCount }} 人</span>
                          </template>
                          <span v-else class="text-muted">—</span>
                        </td>
                        <td>{{ p.startTime || '—' }}</td>
                        <td>{{ p.endTime || '—' }}</td>
                        <td>
                          <span v-if="p.urgeTime" class="urge-time">{{ p.urgeTime }}</span>
                          <span v-else class="text-muted">—</span>
                        </td>
                        <td>{{ p.dispatchTime }}</td>
                        <td class="text-right">
                          <button class="action-link" @click="openEndTimeDialog(t, p)"><i class="el-icon-time" /> 改截止</button>
                          <button class="action-link" @click="openPeriodUsers(t, p)"><i class="el-icon-user" /> 查看人员</button>
                          <button class="action-link text-error" @click="onDeletePeriod(t, p)"><i class="el-icon-delete" /> 删除</button>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                  <!-- 期次分页（每任务独立，最小 5/页） -->
                  <div v-if="taskData[t.id] && taskData[t.id].periods.length > 0" class="period-pagination">
                    <el-pagination
                      small
                      background
                      layout="total, sizes, prev, pager, next"
                      :total="taskData[t.id].periods.length"
                      :current-page.sync="taskData[t.id].periodPage"
                      :page-size="taskData[t.id].periodSize"
                      :page-sizes="[5, 10, 20]"
                      @size-change="s => onPeriodSizeChange(t.id, s)"
                      @current-change="c => onPeriodPageChange(t.id, c)"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div class="pagination">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next"
              :total="total"
              :current-page.sync="page"
              :page-size="limit"
              :page-sizes="[5, 10, 20]"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </section>
    </main>

    <!-- 生成期次弹窗（可临时增删本期次人员，不影响任务配置） -->
    <GeneratePeriodModal
      :visible="genVisible"
      :task="genTask"
      :members="genMembers"
      @success="onGenSuccess"
      @close="genVisible = false"
      @edit-config="onEditConfig"
    />

    <!-- 修改期次截止时间弹窗 -->
    <el-dialog title="修改期次截止时间" :visible.sync="endTimeVisible" width="440px" :close-on-click-modal="false">
      <div v-if="endTimeTarget" class="et-info">
        <div class="et-row"><span class="et-label">任务</span>{{ endTimeTarget._taskName || '—' }}</div>
        <div class="et-row"><span class="et-label">期次</span>{{ endTimeTarget.periodName || endTimeTarget.taskName || '第' + (endTimeTarget.periodNo || '—') + '期' }}</div>
        <div class="et-row"><span class="et-label">开始时间</span>{{ endTimeTarget.startTime || '—' }}</div>
        <div class="et-row"><span class="et-label">当前截止</span>{{ endTimeTarget.endTime || '—' }}</div>
      </div>
      <el-form label-width="70px" style="margin-top: 14px;">
        <el-form-item label="截止时间">
          <el-date-picker
            v-model="endTimeValue"
            type="datetime"
            placeholder="请选择新的截止时间"
            format="yyyy-MM-dd HH:mm:ss"
            value-format="yyyy-MM-dd HH:mm:ss"
            :picker-options="endTimePickerOptions"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <div class="et-tip"><i class="el-icon-info" /> 保存后将同步更新该期次下所有成员任务的截止时间。</div>
      <span slot="footer">
        <el-button @click="endTimeVisible = false">取消</el-button>
        <el-button type="primary" :loading="endTimeSaving" @click="confirmEndTime">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getDispatchTaskList, getDispatchStats, toggleDispatchPlanStatus, toggleDispatchSample, deleteDispatchPlan, getTaskMembers, getPreviewPeriod, checkDueDispatches, autoDispatchDuePeriods, updatePeriodEndTime } from '@/service/sys/FlowDispatchService'
import { getTaskList, deleteTaskGroup } from '@/service/sys/TaskService'
import { getTemplateList } from '@/service/sys/TemplateService'
import GeneratePeriodModal from './components/GeneratePeriodModal.vue'

export default {
  name: 'FlowDispatch',
  components: { GeneratePeriodModal },
  data() {
    return {
      loading: false,
      taskList: [],
      total: 0,
      page: 1,
      limit: 5,
      filters: { taskName: '', status: '', sample: '', creatorName: '', createStart: '', createEnd: '' },
      templates: [],
      activeTasks: [],
      // 统计卡
      stats: {},
      // 跳转返回后需要红色双闪高亮的面板（来自 sessionStorage 恢复）
      flashTasks: [],
      // 跳转返回后需要高亮的期次（点「查看人员」跳转，返回时定位到对应期次行）
      flashPeriod: null,
      taskData: {},
      // 生成期次弹窗
      genVisible: false,
      genTask: null,
      genMembers: [],
      // 到期待下发的期次
      dueList: [],
      dueLoading: false,
      dispatching: false,
      // 修改期次截止时间弹窗
      endTimeVisible: false,
      endTimeTarget: null,
      endTimeValue: '',
      endTimeSaving: false
    }
  },
  computed: {
    isSuperAdmin() {
      return !!(this.$store.state.user.userInfo && this.$store.state.user.userInfo.superAdmin)
    },
    // 截止时间选择器：不能早于期次开始时间
    endTimePickerOptions() {
      const start = this.endTimeTarget && this.endTimeTarget.startTime ? new Date(this.endTimeTarget.startTime) : null
      return {
        disabledDate: t => start ? t.getTime() < start.getTime() - 86400000 : false
      }
    }
  },
  created() {
    // 恢复上次跳转前展开的面板（跳去编辑/数据后台再返回时保持展开，并红色双闪提示）
    this.restoreExpandState()
    this.fetchList()
    this.fetchStats()
    this.loadTemplates()
    this.fetchDue()
  },
  methods: {
    async fetchStats() {
      try {
        const res = await getDispatchStats()
        this.stats = res.data || {}
      } catch (e) {
        console.error(e)
      }
    },
    /** 样例锁定：非超管用户对样例任务不可改/删 */
    isSampleLocked(t) {
      return !this.isSuperAdmin && t.isSample === 1
    },
    async toggleSample(t) {
      const wasSample = t.isSample === 1
      try {
        await toggleDispatchSample(t.id)
        // 本地翻转样例标记即可，不整表刷新（避免列表/期次闪烁跳动）
        // eslint-disable-next-line require-atomic-updates
        t.isSample = wasSample ? 0 : 1
        this.$message.success(!wasSample ? '已设为样例' : '已取消样例')
      } catch (e) {
        this.$message.error((e && e.message) || '操作失败')
      }
    },
    /** 恢复展开/双闪状态：仅当「从本页点击按钮跳转到其他页面再返回」时才恢复展开并双闪，
        直接进入（菜单/路由直达）不做任何高亮，清除遗留标记 */
    restoreExpandState() {
      try {
        if (sessionStorage.getItem('flowDispatchJumpOut') !== '1') {
          sessionStorage.removeItem('flowDispatchFlash')
          sessionStorage.removeItem('flowDispatchFlashPeriod')
          return
        }
        sessionStorage.removeItem('flowDispatchJumpOut')
        const saved = sessionStorage.getItem('flowDispatchExpanded')
        if (saved) {
          const ids = JSON.parse(saved)
          if (Array.isArray(ids) && ids.length) this.activeTasks = ids
        }
        // 双闪跳转来源任务面板
        const flashId = sessionStorage.getItem('flowDispatchFlash')
        if (Number.isInteger(flashId) && this.activeTasks.indexOf(flashId) >= 0) {
          this.flashTasks = [flashId]
          setTimeout(() => { this.flashTasks = [] }, 2200)
        }
        // 高亮跳转来源期次（如点「查看人员」跳转，返回时定位到该期次行）
        const flashPeriod = sessionStorage.getItem('flowDispatchFlashPeriod')
        if (Number.isInteger(flashPeriod)) {
          this.flashPeriod = flashPeriod
          setTimeout(() => { this.flashPeriod = null }, 2200)
        }
      } catch (e) {
        console.error(e)
      }
    },
    /** 保存当前展开状态（跳转其他页面返回后恢复） */
    saveExpandState() {
      try {
        sessionStorage.setItem('flowDispatchExpanded', JSON.stringify(this.activeTasks))
      } catch (e) {
        console.error(e)
      }
    },
    /** 记录跳转来源任务 id（返回本页时该面板红色双闪）与期次 id（对应期次行高亮） */
    saveFlashId(id, dispatchId) {
      try {
        sessionStorage.setItem('flowDispatchJumpOut', '1')
        sessionStorage.setItem('flowDispatchExpanded', JSON.stringify(this.activeTasks))
        sessionStorage.setItem('flowDispatchFlash', String(id))
        if (dispatchId) sessionStorage.setItem('flowDispatchFlashPeriod', String(dispatchId))
        else sessionStorage.removeItem('flowDispatchFlashPeriod')
      } catch (e) {
        console.error(e)
      }
    },
    async loadTemplates() {
      try {
        const res = await getTemplateList({ page: 1, limit: 500 })
        this.templates = (res.data && res.data.records) || []
      } catch (e) {
        console.error(e)
        this.templates = []
      }
    },
    tplName(id) {
      const t = this.templates.find(x => x.id === id)
      return t ? t.templateName : '—'
    },
    cycleText(row) {
      return { 1: '每周', 2: '每月', 3: '每季度', 4: '单次下发' }[row.cycleType] || '单次下发'
    },
    cycleDayText(row) {
      if (row.cycleType === 4) return '—'
      if (row.cycleType === 1) return ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'][row.cycleDay] || '—'
      return `每月 ${row.cycleDay} 号`
    },
    taskStatusText(s) { return { 0: '空', 1: '进行中', 2: '已完成', 3: '已作废' }[s] || '—' },
    taskStatusClass(s) { return { 1: 'tag-running', 2: 'tag-done', 3: 'tag-cancel', 0: 'tag-empty' }[s] || '' },
    /** 期次整体完成进度：已完成人数 / 总人数 */
    periodProgress(p) {
      if (!p.memberCount) return 0
      return Math.round(((Number(p.finishedCount) || 0) / Number(p.memberCount)) * 100)
    },
    nextPeriodText(t) {
      const d = this.taskData[t.id]
      if (!d) return '展开查看'
      if (t.status !== '启用') return '任务已停用'
      if (t.cycleType === 4) return '单次下发，生成时自定期次名'
      if (d.nextPreview && d.nextPreview.periodName) {
        return `${d.nextPreview.periodName} · ${d.nextPreview.startTime || '—'} 下发`
      }
      return '计算中...'
    },
    /** 统计卡是否选中：以当前筛选状态判定（状态/样例与卡一一对应） */
    statActive(k) {
      if (k === 'all') return !this.filters.status && this.filters.sample === ''
      if (k === 'active') return this.filters.status === '启用'
      if (k === 'stopped') return this.filters.status === '停用'
      if (k === 'sample') return this.filters.sample === '1'
      return false
    },
    /** 点击统计卡：状态/样例即点即设（与筛选下拉同源），点「任务总数」恢复全部 */
    toggleStat(k) {
      if (k === 'all') {
        this.filters.status = ''
        this.filters.sample = ''
      } else if (k === 'active') {
        this.filters.status = this.filters.status === '启用' ? '' : '启用'
      } else if (k === 'stopped') {
        this.filters.status = this.filters.status === '停用' ? '' : '停用'
      } else if (k === 'sample') {
        this.filters.sample = this.filters.sample === '1' ? '' : '1'
      }
      this.page = 1
      this.fetchList()
    },
    async fetchList() {
      this.loading = true
      try {
        const p = { page: this.page, limit: this.limit }
        if (this.filters.taskName && this.filters.taskName.trim()) p.taskName = this.filters.taskName.trim()
        if (this.filters.status) p.status = this.filters.status
        if (this.filters.sample !== '') p.sample = Number(this.filters.sample)
        if (this.filters.creatorName && this.filters.creatorName.trim()) p.creatorName = this.filters.creatorName.trim()
        if (this.filters.createStart) p.createStart = this.filters.createStart
        if (this.filters.createEnd) p.createEnd = this.filters.createEnd
        const res = await getDispatchTaskList(p)
        this.taskList = (res.data && res.data.records) || []
        this.total = (res.data && res.data.total) || 0
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
        // 恢复/保持展开的任务：自动补加载期次详情（避免返回本页时展开但无数据）
        this.activeTasks.forEach(id => {
          if (!this.taskData[id]) this.loadTaskDetail(id)
        })
      }
    },
    handleSearch() {
      this.page = 1
      this.fetchList()
    },
    /** 检索当前是否有任务的期次应下发 */
    async fetchDue() {
      this.dueLoading = true
      try {
        const res = await checkDueDispatches()
        this.dueList = (res && res.data) || []
      } catch (e) {
        console.error(e)
        this.dueList = []
      } finally {
        this.dueLoading = false
      }
    },
    /** 一键下发所有到期待下发的期次 */
    async handleAutoDispatch() {
      if (this.dispatching) return
      this.dispatching = true
      try {
        const res = await autoDispatchDuePeriods()
        const n = (res && res.data) || 0
        this.$message.success(n > 0 ? `已自动下发 ${n} 个期次` : '暂无到期待下发的期次')
        this.fetchDue()
        this.fetchList()
      } catch (e) {
        this.$message.error((e && e.message) || '下发失败')
      } finally {
        this.dispatching = false
      }
    },
    resetFilters() {
      this.filters = { taskName: '', status: '', sample: '', creatorName: '', createStart: '', createEnd: '' }
      this.page = 1
      this.fetchList()
    },
    /** 任务分页：切换页大小（最小 5） */
    handleSizeChange(size) {
      this.limit = size
      this.page = 1
      this.fetchList()
    },
    handleCurrentChange(p) {
      this.page = p
      this.fetchList()
    },
    isTaskOpen(id) {
      return this.activeTasks.indexOf(id) >= 0
    },
    toggleTask(id) {
      const idx = this.activeTasks.indexOf(id)
      if (idx >= 0) {
        this.activeTasks = this.activeTasks.filter(x => x !== id)
      } else {
        this.activeTasks = this.activeTasks.concat(id)
        this.loadTaskDetail(id)
      }
      this.saveExpandState()
    },
    /** 展开任务时加载：期次 + 下一期次预览 + 任务配置人员 */
    async loadTaskDetail(taskId) {
      const t = this.taskList.find(x => x.id === taskId)
      if (!t) return
      if (!this.taskData[taskId]) {
        this.$set(this.taskData, taskId, {
          loaded: false,
          loading: true,
          periods: [],
          nextPreview: null,
          members: [],
          periodPage: 1,
          periodSize: 5
        })
      }
      const d = this.taskData[taskId]
      d.loading = true
      // 重新加载时回到第 1 页
      d.periodPage = 1
      try {
        const [periodRes, previewRes, memberRes] = await Promise.all([
          getTaskList({ page: 1, limit: 100, taskId }),
          getPreviewPeriod(taskId, false),
          getTaskMembers(taskId)
        ])
        d.periods = (periodRes.data && periodRes.data.records) || []
        d.nextPreview = previewRes.data || null
        d.members = (memberRes.data || []).map(m => ({ ...m }))
        d.loaded = true
      } catch (e) {
        console.error(e)
      } finally {
        d.loading = false
      }
    },
    /** 期次切片显示（前端分页） */
    periodSlice(t) {
      const d = this.taskData[t.id]
      if (!d || !d.periods) return []
      const ps = d.periodSize || 5
      const pp = d.periodPage || 1
      return d.periods.slice((pp - 1) * ps, pp * ps)
    },
    onPeriodSizeChange(taskId, size) {
      const d = this.taskData[taskId]
      if (d) {
        d.periodSize = size
        d.periodPage = 1
      }
    },
    onPeriodPageChange(taskId, p) {
      const d = this.taskData[taskId]
      if (d) d.periodPage = p
    },
    /** 打开生成期次弹窗：加载本期次人员（默认抄用任务配置人员） */
    async openGen(t) {
      let members = []
      const d = this.taskData[t.id]
      if (d && d.loaded) {
        members = d.members
      } else {
        try {
          const res = await getTaskMembers(t.id)
          members = (res.data || []).map(m => ({ ...m }))
        } catch (e) {
          console.error(e)
        }
      }
      this.genTask = t
      this.genMembers = members
      this.genVisible = true
    },
    /** 生成成功：刷新该任务期次数据与任务列表 */
    onGenSuccess() {
      const taskId = this.genTask ? this.genTask.id : null
      this.genVisible = false
      this.genTask = null
      this.genMembers = []
      if (taskId && this.taskData[taskId]) {
        this.loadTaskDetail(taskId)
      }
      this.fetchList()
    },
    openEdit(t) {
      this.saveFlashId(t.id)
      this.$router.push({ path: '/flow-dispatch/edit', query: { id: t.id }})
    },
    /** 打开「按人员查看」界面（该任务下所有人员的历史提交，人员→期次→节点折叠展示） */
    openPersonView(t) {
      this.saveExpandState()
      this.saveFlashId(t.id)
      this.$router.push({
        path: '/flow-dispatch/person-view',
        query: { taskId: t.id, taskName: t.taskName || '' }
      })
    },
    /** 生成期次弹窗内点击「去调整」：关闭弹窗并跳转编辑任务页（步骤2下发配置） */
    onEditConfig() {
      const t = this.genTask
      this.genVisible = false
      this.genTask = null
      this.genMembers = []
      if (t) this.openEdit(t)
    },
    openCreate() {
      this.$router.push('/flow-dispatch/edit')
    },
    openPeriodUsers(t, p) {
      this.saveFlashId(t.id, p.dispatchId)
      this.$router.push({
        path: '/flow-dispatch/period-users',
        query: {
          dispatchId: p.dispatchId,
          periodName: p.periodName || p.taskName || '',
          taskName: t.taskName || ''
        }
      })
    },
    /** 打开修改期次截止时间弹窗 */
    openEndTimeDialog(t, p) {
      this.endTimeTarget = {
        dispatchId: p.dispatchId,
        periodName: p.periodName,
        periodNo: p.periodNo,
        taskName: p.taskName,
        startTime: p.startTime,
        endTime: p.endTime,
        _taskId: t.id,
        _taskName: t.taskName
      }
      this.endTimeValue = p.endTime || ''
      this.endTimeVisible = true
    },
    /** 保存新的期次截止时间 */
    async confirmEndTime() {
      if (!this.endTimeValue) {
        this.$message.warning('请选择新的截止时间')
        return
      }
      this.endTimeSaving = true
      try {
        await updatePeriodEndTime(this.endTimeTarget.dispatchId, this.endTimeValue)
        this.$message.success('截止时间已更新，该期次下所有成员任务同步更新')
        this.endTimeVisible = false
        await this.loadTaskDetail(this.endTimeTarget._taskId)
      } catch (e) {
        this.$message.error((e && e.message) || '更新失败')
      } finally {
        this.endTimeSaving = false
      }
    },
    async onDeletePeriod(t, p) {
      const cnt = p.memberCount || 0
      if (cnt > 0) {
        this.$message.warning(`该期次下还有 ${cnt} 名人员，请先在「数据后台」查看该期次并清空所有人员，再回来删除期次。`)
        return
      }
      try {
        await this.$confirm(`确定删除期次「${p.periodName || p.taskName}」吗？删除后不可恢复。`, '删除期次确认', {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--primary'
        })
      } catch (e) {
        return
      }
      try {
        await deleteTaskGroup(p.dispatchId)
        this.$message.success('删除成功')
        await this.loadTaskDetail(t.id)
        this.fetchList()
      } catch (e) {
        this.$message.error((e && e.message) || '删除失败')
      }
    },
    toggleStatus(t) {
      const toDisable = t.status === '启用'
      this.$confirm(
        toDisable
          ? '停用后：不能生成新期次，自动下发将暂停；已下发的期次与处理进度不受影响，可随时重新启用。确定停用「' + t.taskName + '」吗？'
          : '启用后：恢复生成期次与自动下发功能。确定启用「' + t.taskName + '」吗？',
        toDisable ? '停用确认' : '启用确认',
        {
          confirmButtonText: toDisable ? '停用' : '启用',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--primary'
        }
      ).then(() => {
        toggleDispatchPlanStatus(t.id).then(res => {
          this.$message.success(res.message || '操作成功')
          this.fetchList()
        }).catch(e => {
          console.error(e)
          this.$message.error('操作失败')
        })
      }).catch(() => {})
    },
    onDelete(t) {
      this.$confirm(`确定删除任务「${t.taskName}」吗？仅当任务下无任何期次时可删除。`, '删除确认', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteDispatchPlan(t.id).then(res => {
          this.$message.success(res.message || '删除成功')
          this.fetchList()
        }).catch(e => {
          console.error(e)
          this.$message.error((e && e.message) || '删除失败')
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);
$border: #CBD5E1;
.dashboard-container { display: flex; min-height: 100vh; background-color: var(--color-primary-surface);  color: #1b1c1c; }
.main-content { width: 100%; display: flex; flex-direction: column; min-height: 100vh; }
.page-content { padding: 24px; display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; }
.breadcrumb { display: flex; gap: 8px; font-size: 12px; line-height: 20px; color: #414755; margin-bottom: 8px;
  .active { color: $primary; font-weight: 600; }
}
.page-heading { font-size: 24px; line-height: 32px; font-weight: 600; color: #1b1c1c; }

// 统计卡
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
  @media (max-width: 1100px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width: 600px) { grid-template-columns: 1fr; }
}
.stat-card { display: flex; align-items: center; gap: 16px; background: #fff; border: 1px solid $border; border-radius: 3px; padding: 18px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); transition: all .2s;
  &.clickable { cursor: pointer;
    &:hover { border-color: $primary; box-shadow: 0 2px 8px rgba(var(--color-primary-rgb), 0.12); }
    &.active { border-color: $primary; background: var(--color-primary-light); box-shadow: inset 0 0 0 1px rgba(var(--color-primary-rgb), 0.5); }
  }
}
.stat-icon { width: 48px; height: 48px; border-radius: 3px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #fff; flex-shrink: 0;
  &.icon-total { background: $primary; }
  &.icon-active { background: #15803D; }
  &.icon-stopped { background: #B45309; }
  &.icon-sample { background: #7C5CBF; }
  &.icon-period { background: #B45309; }
  &.icon-member { background: #545f72; }
}
.stat-body { flex: 1; }
.stat-label { font-size: 13px; color: #757575; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1b1c1c; line-height: 1.1; }
.btn-create { display: flex; align-items: center; gap: 5px; padding: 10px 22px; background: $primary; color: #fff; border: none; border-radius: 3px; font-weight: 600; font-size: 13px; cursor: pointer; box-shadow: 0 2px 8px rgba(var(--color-primary-rgb),0.25); transition: all .2s;
  &:hover { opacity: 0.9; transform: translateY(-1px); }
}
.header-actions { display: flex; align-items: center; gap: 10px; }
.btn-tpl { display: flex; align-items: center; gap: 5px; padding: 10px 16px; background: #fff; border: 1px solid $border; border-radius: 3px; color: var(--color-primary); font-weight: 600; font-size: 13px; cursor: pointer; transition: all .2s;
  i { color: $primary; }
  &:hover { border-color: $primary; color: $primary; background: var(--color-primary-surface); }
}
.tip-bar { display: flex; align-items: center; gap: 8px; background: var(--color-primary-light); border: 1px solid $border; color: var(--color-primary-hover); font-size: 13px; border-radius: 3px; padding: 10px 14px;
  i { color: $primary; }
}
// 到期待下发提示条
.due-bar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; background: linear-gradient(90deg, var(--color-primary-light), #EFF6FF); border: 1px solid rgba(180, 83, 9,0.4); border-radius: 3px; padding: 10px 14px; font-size: 13px; color: var(--color-primary);
  > i { color: #B45309; font-size: 16px; }
  .due-text b { color: #B45309; font-size: 15px; }
  .due-item { background: #fff; border: 1px solid rgba(180, 83, 9,0.3); color: var(--color-primary); padding: 2px 10px; border-radius: 4px; font-size: 12px; }
  .due-btn { margin-left: auto; display: flex; align-items: center; gap: 4px; padding: 6px 16px; background: #B45309; color: #fff; border: none; border-radius: 2px; font-size: 13px; font-weight: 600; cursor: pointer;
    &:hover { opacity: 0.9; }
    &:disabled { opacity: 0.5; cursor: not-allowed; }
  }
}
.filter-section { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 16px; box-shadow: 0 1px 4px rgba(0,0,0,0.03); }
.filter-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
  @media (max-width: 768px) { grid-template-columns: 1fr; }
}
.filter-item { display: flex; flex-direction: column; gap: 4px; }
.filter-label { font-size: 13px; color: #757575; }
.filter-select, .filter-input { height: 36px; border: 1px solid #dcdfe6; border-radius: 2px; padding: 0 10px; font-size: 13px; outline: none; background: #fff; transition: all .2s;
  &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb),0.12); }
}
.filter-actions { display: flex; justify-content: flex-end; align-items: center; gap: 8px; margin-top: 16px; padding-top: 16px; border-top: 1px solid rgba(var(--color-primary-rgb),0.08); }
.filter-actions-right { display: flex; gap: 8px; }
.btn-reset { padding: 0 16px; height: 36px; border: 1px solid $border; border-radius: 2px; font-size: 13px; color: var(--color-primary); background: #fff; cursor: pointer;
  &:hover { background: var(--color-primary-light); }
}
.btn-search { padding: 0 16px; height: 36px; border: none; border-radius: 2px; font-size: 13px; font-weight: bold; color: #fff; background: $primary; cursor: pointer; display: flex; align-items: center; gap: 4px;
  &:hover { opacity: 0.9; }
}
.empty-state { text-align: center; padding: 60px 20px; color: #bbb; background: #fff; border: 1px solid $border; border-radius: 3px;
  i { font-size: 48px; display: block; margin-bottom: 12px; }
  p { font-size: 14px; margin: 0; }
}
.task-collapse { position: relative; }
.loading-bar { display: flex; align-items: center; gap: 6px; justify-content: center; padding: 16px; color: $primary; font-size: 13px; }

// 折叠面板（手写实现，样式完全可控）
.task-panel { background: #fff; border: 1px solid $border; border-radius: 3px; margin-bottom: 12px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.03);
  // 返回本页时红色双闪高亮（记忆展开的面板）
  &.flash-open { animation: flashRed 0.55s ease-in-out 2; }
}
@keyframes flashRed {
  0%, 100% { border-color: $border; box-shadow: 0 1px 3px rgba(0,0,0,0.03); }
  30% { border-color: $primary; box-shadow: 0 0 0 4px rgba(var(--color-primary-rgb),0.3); }
  60% { border-color: $primary; box-shadow: 0 0 0 7px rgba(var(--color-primary-rgb),0.12); }
}
.tp-head { display: flex; align-items: center; gap: 12px; padding: 14px 18px; cursor: pointer; transition: background .2s; }
.tp-head:hover { background: var(--color-primary-surface); }
.tp-arrow { color: #94A3B8; font-size: 14px; flex-shrink: 0; transition: transform .25s; cursor: pointer; }
.task-panel:hover .tp-arrow { color: $primary; }
.task-panel.is-open .tp-arrow { transform: rotate(180deg); }
.tp-body { border-top: 1px solid $border; }
.ct-icon { width: 40px; height: 40px; border-radius: 3px; background: var(--color-primary-light); color: $primary; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.ct-main { flex: 1; min-width: 0; }
.ct-name-row { display: flex; align-items: center; gap: 8px; min-width: 0; }
.ct-name { font-size: 15px; font-weight: 700; color: #1b1c1c; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ct-sub { font-size: 12px; color: #909399; margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ct-status { padding: 2px 12px; border-radius: 4px; font-size: 12px; flex-shrink: 0;
  &.on { background: rgba(var(--color-primary-rgb),0.1); color: $primary; font-weight: 600; }
  &.off { background: #5c5f66; color: #fff; font-weight: 600; }
}
.sample-tag { padding: 2px 8px; background: rgba(180, 83, 9,0.14); color: #B45309; border-radius: 3px; font-size: 11px; font-weight: 600; flex-shrink: 0; }

// 停用任务面板整体置灰，状态区分明显
.task-panel.is-disabled {
  border-color: #d9dadd;
  .tp-head { background: #f7f7f8; }
  .tp-head:hover { background: #f1f1f3; }
  .ct-icon { background: #e9eaec; color: #9a9da3; }
  .ct-name { color: #909399; }
  .overview-grid { background: #f4f4f5; border-color: #e4e5e7; }
}

// 展开区停用提示条
.disabled-tip { display: flex; align-items: center; gap: 8px; background: #f5f5f6; border: 1px solid #d5d6d8; color: #4b4e55; font-size: 13px; border-radius: 3px; padding: 10px 14px;
  i { color: #6b6f76; font-size: 15px; }
  b { color: #3a3d43; }
}

.task-detail { padding: 16px; display: flex; flex-direction: column; gap: 16px; }
.overview-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; background: var(--color-primary-light); border: 1px solid #E2E8F0; border-radius: 3px; padding: 14px 16px;
  @media (max-width: 900px) { grid-template-columns: 1fr; }
}
.ov-item { display: flex; align-items: flex-start; gap: 10px; min-width: 0; }
.ov-icon { color: $primary; font-size: 16px; margin-top: 1px; flex-shrink: 0; }
.ov-item > div { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.ov-label { font-size: 12px; color: #909399; }
.ov-value { font-size: 13px; color: #1b1c1c; word-break: break-all; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.action-row { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.btn-gen { display: flex; align-items: center; gap: 4px; padding: 8px 18px; background: $primary; color: #fff; border: none; border-radius: 3px; cursor: pointer; font-size: 13px; font-weight: 600; box-shadow: 0 2px 6px rgba(var(--color-primary-rgb),0.2);
  &:hover { opacity: 0.9; }
  &:disabled { opacity: 0.5; cursor: not-allowed; box-shadow: none; }
}
.btn-ghost { display: flex; align-items: center; gap: 4px; padding: 8px 16px; background: #fff; border: 1px solid $border; border-radius: 3px; color: var(--color-primary); cursor: pointer; font-size: 13px;
  &:hover { background: var(--color-primary-light); }
}
.btn-person { color: var(--color-primary-hover); border-color: rgba(var(--color-primary-rgb),0.35);
  i { color: $primary; }
  &:hover { background: var(--color-primary-light); border-color: $primary; }
}
.text-error { color: #DC2626; }
.star-on { color: #D97706; }
// 修改期次截止时间弹窗
.et-info { display: flex; flex-direction: column; gap: 6px; background: var(--color-primary-light); border: 1px solid #E2E8F0; border-radius: 3px; padding: 10px 14px; font-size: 13px; }
.et-row { display: flex; gap: 8px; color: #1b1c1c;
  .et-label { width: 64px; color: #909399; flex-shrink: 0; }
}
.et-tip { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--color-primary-hover); background: var(--color-primary-light); border: 1px dashed rgba(var(--color-primary-rgb),0.35); border-radius: 2px; padding: 8px 10px;
  i { color: $primary; }
}
.period-section { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 14px 16px; }
.sec-title { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 700; color: #1b1c1c; margin-bottom: 12px;
  i { color: $primary; }
}
.sec-sub { font-size: 12px; color: #999; font-weight: 400; }
.period-empty { text-align: center; padding: 24px; color: #bbb; font-size: 13px; }
.period-skeleton { display: flex; align-items: center; gap: 8px; padding: 22px 4px; color: $primary; font-size: 13px; }
.period-table { width: 100%; text-align: left; border-collapse: collapse;
  th { padding: 10px 12px; font-weight: 700; color: #414755; background: var(--color-primary-light); border-bottom: 1px solid $border; font-size: 13px; }
  td { padding: 10px 12px; border-bottom: 1px solid $border; font-size: 13px; }
  tbody tr:last-child td { border-bottom: none; }
  .hover-row:hover { background: var(--color-primary-light); }
  /* 从「查看人员」跳转返回时，定位并高亮跳转来源期次行 */
  .flash-period td { background: #E2E8F0; }
}
.font-bold { font-weight: 700; }
.sub-text { display: block; font-size: 12px; color: #909399; font-weight: 400; margin-top: 2px; }
.text-center { text-align: center; }
.text-right { text-align: right; }
.tag-manual { padding: 1px 8px; background: rgba(180, 83, 9,0.14); color: #B45309; border-radius: 3px; font-size: 11px; margin-left: 6px; font-weight: 600; }
.tag-running { padding: 2px 10px; background: rgba(var(--color-primary-rgb),0.1); color: $primary; border-radius: 3px; font-size: 12px; font-weight: 600; }
.tag-done { padding: 2px 10px; background: rgba(21, 128, 61,0.1); color: #15803D; border-radius: 3px; font-size: 12px; font-weight: 600; }
.tag-cancel { padding: 2px 10px; background: rgba(220,38,38,0.08); color: #DC2626; border-radius: 3px; font-size: 12px; font-weight: 600; }
.tag-empty { padding: 2px 10px; background: rgba(144,147,153,0.1); color: #909399; border-radius: 3px; font-size: 12px; }
// 期次完成进度（任务管理期次列表）
.pd-track { display: inline-block; width: 72px; height: 6px; background: #E2E8F0; border-radius: 3px; overflow: hidden; vertical-align: middle; }
.pd-fill { height: 100%; background: $primary; border-radius: 3px; transition: width .4s; }
.pd-text { font-size: 11px; color: #909399; margin-left: 6px; vertical-align: middle; font-weight: 600; }
.action-link { color: $primary; background: none; border: none; cursor: pointer; font-size: 13px; margin-left: 8px;
  &:hover { text-decoration: underline; }
  &:disabled { color: #bbb; cursor: not-allowed; text-decoration: none; }
}
.pagination { display: flex; justify-content: flex-end; align-items: center; padding: 14px 16px; background: #fff; border: 1px solid $border; border-radius: 3px; margin-top: 12px; }
.period-pagination { display: flex; justify-content: flex-end; align-items: center; padding-top: 12px; }
</style>
