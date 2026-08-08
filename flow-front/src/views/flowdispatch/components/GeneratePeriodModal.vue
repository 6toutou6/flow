<template>
  <el-dialog
    :title="null"
    :visible.sync="dialogVisible"
    width="720px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    custom-class="gen-period-dialog"
    append-to-body
    @close="handleClose"
  >
    <div class="gpd">
      <!-- 头部 -->
      <div class="gpd-head">
        <div class="gpd-title">
          <i class="el-icon-s-promotion" />
          <div>
            <div class="gpd-name">生成期次</div>
            <div class="gpd-task">{{ task ? task.taskName : '' }}</div>
          </div>
        </div>
        <span v-if="task" :class="task.status === 1 ? 'gpd-badge on' : 'gpd-badge'">{{ task.status === 1 ? '启用中' : '已停用' }}</span>
      </div>

      <!-- 下发配置摘要 -->
      <div v-if="task" class="gpd-config">
        <i class="el-icon-setting" />
        <span class="gpd-config-label">下发配置</span>
        <span class="gpd-config-item">周期：<b>{{ cycleText }}</b></span>
        <span v-if="task.cycleType !== 4" class="gpd-config-item">触发日：<b>{{ cycleDayText }}</b></span>
        <span class="gpd-config-item">截止：<b>触发后 {{ task.deadlineDays || '—' }} 天</b></span>
        <span v-if="task.urgeDays" class="gpd-config-item">催办：<b>提前 {{ task.urgeDays }} 天</b></span>
        <button class="gpd-config-edit" @click="$emit('edit-config')">去调整</button>
      </div>

      <!-- 模式切换 -->
      <div class="gpd-mode">
        <el-radio-group v-model="mode" size="small">
          <el-radio-button label="auto">按周期自动下发</el-radio-button>
          <el-radio-button label="manual">手动临时期次</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 期次信息卡 -->
      <div class="gpd-card">
        <div class="gpd-card-title"><i class="el-icon-date" /> 期次信息</div>
        <template v-if="mode === 'auto'">
          <div class="gpd-switch-row">
            <span class="gpd-switch-label">下发时机</span>
            <div class="gpd-check-group">
              <el-checkbox v-model="immediate" @change="loadPreview">立即下发（补发当期）</el-checkbox>
              <span class="gpd-check-hint">{{ immediate ? '已选：补发当期' : '未勾选：下一期次下发' }}</span>
            </div>
          </div>
          <div v-loading="previewLoading" class="gpd-preview">
            <div v-if="preview" class="gpd-preview-body">
              <div class="gpd-row">
                <span class="gpd-label">期次名称</span>
                <input v-model="periodName" class="gpd-input" :placeholder="'默认：' + (preview.periodName || '')" maxlength="100">
              </div>
              <div class="gpd-row">
                <span class="gpd-label">开始时间</span>
                <span class="gpd-value">{{ preview.startTime || '—' }}</span>
              </div>
              <div class="gpd-row">
                <span class="gpd-label">截止时间</span>
                <span class="gpd-value">{{ preview.endTime || '—' }}</span>
              </div>
              <div v-if="preview.urgeDays" class="gpd-row">
                <span class="gpd-label">提前催办</span>
                <span class="gpd-value">截止前 {{ preview.urgeDays }} 天</span>
              </div>
            </div>
            <div v-else class="gpd-preview-empty"><i class="el-icon-loading" /> 计算期次中...</div>
          </div>
        </template>
        <template v-else>
          <div class="gpd-row">
            <span class="gpd-label"><span class="req">*</span> 期次名称</span>
            <input v-model="periodName" class="gpd-input" placeholder="如：2026-08 临时期次" maxlength="100">
          </div>
          <div class="gpd-row">
            <span class="gpd-label"><span class="req">*</span> 开始时间</span>
            <el-date-picker
              v-model="manualStartTime"
              type="datetime"
              placeholder="选择开始时间"
              format="yyyy-MM-dd HH:mm:ss"
              value-format="yyyy-MM-dd HH:mm:ss"
              class="gpd-date"
              :picker-options="startPickerOptions"
            />
          </div>
          <div class="gpd-row">
            <span class="gpd-label"><span class="req">*</span> 截止时间</span>
            <el-date-picker
              v-model="manualEndTime"
              type="datetime"
              placeholder="选择截止时间"
              format="yyyy-MM-dd HH:mm:ss"
              value-format="yyyy-MM-dd HH:mm:ss"
              class="gpd-date"
              :picker-options="endPickerOptions"
            />
          </div>
          <div class="gpd-tip"><i class="el-icon-info" /> 临时期次的开始与截止时间由你自定义；手动新增不影响后续自动下发逻辑</div>
        </template>
      </div>

      <!-- 本期次人员确认卡 -->
      <div class="gpd-card">
        <div class="gpd-card-title gpd-members-head">
          <span><i class="el-icon-user" /> 本期次人员 <b>{{ tempMembers.length }}</b> 人</span>
          <button class="gpd-add" @click="pickerVisible = true"><i class="el-icon-plus" /> 临时增加人员</button>
        </div>
        <div v-if="tempMembers.length === 0" class="gpd-empty">
          <i class="el-icon-user" />
          <p>尚未选择人员，请点击「临时增加人员」添加</p>
        </div>
        <div v-else class="gpd-members">
          <div v-for="m in tempMembers" :key="m.userId" class="gpd-chip">
            <span class="gpd-avatar">{{ (m.realName || 'U').charAt(0) }}</span>
            <div class="gpd-chip-info">
              <span class="gpd-chip-name">{{ m.realName }}<i v-if="m.empNo" class="gpd-emp">{{ m.empNo }}</i></span>
              <span class="gpd-chip-dept">{{ m.deptName || '—' }}</span>
            </div>
            <i class="el-icon-close gpd-x" @click="removeMember(m.userId)" />
          </div>
        </div>
        <div class="gpd-tip"><i class="el-icon-info" /> 临时增删仅影响本期次，不改动任务配置人员</div>
      </div>
    </div>

    <!-- 底部操作 -->
    <div slot="footer" class="gpd-footer">
      <span v-if="tempMembers.length > 0" class="gpd-sum">将为 {{ tempMembers.length }} 名人员创建独立提交任务</span>
      <div class="gpd-actions">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :disabled="saving || tempMembers.length === 0" @click="handleSubmit">
          <i v-if="saving" class="el-icon-loading" />
          <i v-else class="el-icon-check" /> 生成期次（{{ tempMembers.length }} 人）
        </el-button>
      </div>
    </div>

    <!-- 临时增加人员弹窗 -->
    <UserPicker
      :visible="pickerVisible"
      title="临时增加本期次人员（可多选，不影响任务配置）"
      :exclude-ids="pickerExcludeIds"
      @confirm="confirmPick"
      @close="pickerVisible = false"
    />
  </el-dialog>
</template>

<script>
import { getPreviewPeriod, generatePeriod } from '@/api/flowDispatch'
import UserPicker from '@/components/UserPicker'

export default {
  name: 'GeneratePeriodModal',
  components: { UserPicker },
  props: {
    visible: { type: Boolean, default: false },
    task: { type: Object, default: null },
    members: { type: Array, default: () => [] }
  },
  data() {
    return {
      mode: 'auto',
      immediate: true,
      periodName: '',
      preview: null,
      previewLoading: false,
      /** 临时期次自定义开始/截止时间（字符串 yyyy-MM-dd HH:mm:ss） */
      manualStartTime: '',
      manualEndTime: '',
      tempMembers: [],
      saving: false,
      pickerVisible: false,
      lastAutoName: ''
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(val) { if (!val) this.$emit('close') }
    },
    /** 开始时间选择限制：不早于当天 */
    startPickerOptions() {
      return {
        disabledDate(time) { return time.getTime() < Date.now() - 24 * 3600 * 1000 }
      }
    },
    /** 截止时间选择限制：不早于已选的开始时间 */
    endPickerOptions() {
      const start = this.manualStartTime
      return {
        disabledDate(time) {
          if (!start) return time.getTime() < Date.now() - 24 * 3600 * 1000
          const d = new Date(start)
          d.setDate(d.getDate() - 1)
          return time.getTime() < d.getTime()
        }
      }
    },
    pickerExcludeIds() {
      return this.tempMembers.map(m => m.userId)
    },
    // 下发配置摘要（task 由列表接口 JOIN 配置表带出）
    cycleText() {
      return { 1: '每周', 2: '每月', 3: '每季度', 4: '单次下发' }[this.task && this.task.cycleType] || '—'
    },
    cycleDayText() {
      const t = this.task
      if (!t || t.cycleType === 4) return '—'
      if (t.cycleType === 1) return ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'][t.cycleDay] || '—'
      return `每月 ${t.cycleDay || '—'} 号`
    }
  },
  watch: {
    visible(val) {
      if (val) this.init()
    }
  },
  methods: {
    init() {
      this.mode = 'auto'
      this.immediate = true
      this.periodName = ''
      this.preview = null
      this.lastAutoName = ''
      // 临时期次默认时间：开始=当前，截止=当前 + 配置截止天数（用户可自行修改）
      const now = new Date()
      const days = (this.task && this.task.deadlineDays) || 7
      this.manualStartTime = this.formatDateTime(now)
      this.manualEndTime = this.formatDateTime(new Date(now.getTime() + days * 24 * 3600 * 1000))
      // 默认抄用任务配置人员，可临时增删（不影响任务配置）
      this.tempMembers = (this.members || []).map(m => ({ ...m }))
      this.loadPreview()
    },
    async loadPreview() {
      if (!this.task) return
      this.previewLoading = true
      try {
        const res = await getPreviewPeriod(this.task.id, this.immediate)
        this.preview = res.data || null
        const autoName = (res.data && res.data.periodName) || ''
        // 用户未手动修改期次名时跟随预览默认名
        if (!this.periodName || this.periodName === this.lastAutoName) {
          this.periodName = autoName
        }
        this.lastAutoName = autoName
      } catch (e) {
        console.error(e)
        this.preview = null
      } finally {
        this.previewLoading = false
      }
    },
    confirmPick(users) {
      const existing = new Set(this.tempMembers.map(m => m.userId))
      users.forEach(u => {
        if (!existing.has(u.id)) {
          this.tempMembers.push({ userId: u.id, realName: u.realName, empNo: u.empNo, deptName: u.deptName })
        }
      })
      this.pickerVisible = false
    },
    removeMember(userId) {
      this.tempMembers = this.tempMembers.filter(m => m.userId !== userId)
    },
    /** 日期时间 → yyyy-MM-dd HH:mm:ss 字符串 */
    formatDateTime(d) {
      const p = n => (n < 10 ? '0' + n : '' + n)
      return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
    },
    async handleSubmit() {
      if (this.tempMembers.length === 0) {
        this.$message.warning('请至少选择一名本期次人员')
        return
      }
      if (!this.periodName || !this.periodName.trim()) {
        this.$message.warning('请填写期次名称')
        return
      }
      if (this.mode === 'manual') {
        if (!this.manualStartTime || !this.manualEndTime) {
          this.$message.warning('请选择临时期次的开始与截止时间')
          return
        }
        if (this.manualEndTime <= this.manualStartTime) {
          this.$message.warning('截止时间必须晚于开始时间')
          return
        }
      }
      this.saving = true
      try {
        const payload = {
          immediate: this.mode === 'auto' ? this.immediate : true,
          periodName: this.periodName.trim(),
          manual: this.mode === 'manual',
          memberIds: this.tempMembers.map(m => m.userId)
        }
        if (this.mode === 'manual') {
          payload.startTime = this.manualStartTime
          payload.endTime = this.manualEndTime
        }
        const res = await generatePeriod(this.task.id, payload)
        this.$message.success(res.message || '期次生成成功')
        this.$emit('success', res.data)
      } catch (e) {
        console.error(e)
        this.$message.error((e && e.message) || '生成失败')
      } finally {
        this.saving = false
      }
    },
    handleClose() {
      this.$emit('close')
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #C53030;
.gpd { display: flex; flex-direction: column; gap: 14px; }
.gpd-head { display: flex; justify-content: space-between; align-items: center; padding-bottom: 14px; border-bottom: 1px dashed #e4beba; }
.gpd-title { display: flex; align-items: center; gap: 12px;
  i { width: 42px; height: 42px; border-radius: 10px; background: $primary; color: #fff; font-size: 20px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
}
.gpd-name { font-size: 17px; font-weight: 700; color: #1b1c1c; line-height: 1.3; }
.gpd-task { font-size: 12px; color: #909399; margin-top: 2px; max-width: 420px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.gpd-badge { padding: 3px 12px; border-radius: 12px; font-size: 12px; background: #f0f0f0; color: #909399; flex-shrink: 0;
  &.on { background: rgba(197,48,48,0.1); color: $primary; font-weight: 600; }
}
.gpd-config { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; background: #FFF5F5; border: 1px dashed $primary; border-radius: 0px; padding: 9px 14px; font-size: 12px;
  > i { color: $primary; }
  .gpd-config-label { color: $primary; font-weight: 700; flex-shrink: 0; }
  .gpd-config-item { color: #5b403d; b { color: #1b1c1c; font-weight: 700; } }
  .gpd-config-edit { margin-left: auto; color: $primary; background: none; border: 1px solid $primary; border-radius: 6px; padding: 3px 12px; cursor: pointer; font-size: 12px; flex-shrink: 0;
    &:hover { background: $primary; color: #fff; }
  }
}
.gpd-mode { display: flex; }
.gpd-card { background: #FFFDFD; border: 1px solid #e4beba; border-radius: 10px; padding: 14px 16px; }
.gpd-card-title { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 700; color: #414755; margin-bottom: 12px;
  i { color: $primary; }
}
.gpd-switch-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.gpd-switch-label { font-size: 13px; color: #414755; }
.gpd-check-group { display: flex; align-items: center; gap: 10px; }
.gpd-check-hint { font-size: 12px; color: #909399; }
.gpd-preview { min-height: 40px; background: #fff; border: 1px solid #f0e3e1; border-radius: 8px; }
.gpd-preview-body { padding: 4px 14px; }
.gpd-preview-empty { display: flex; align-items: center; justify-content: center; gap: 6px; padding: 14px; color: #999; font-size: 13px; }
.gpd-row { display: flex; align-items: center; gap: 12px; padding: 8px 0; font-size: 13px;
  &:not(:last-child) { border-bottom: 1px dashed #f0e3e1; }
}
.gpd-label { width: 84px; color: #909399; flex-shrink: 0; }
.gpd-value { color: #1b1c1c; flex: 1; }
.gpd-input { flex: 1; height: 34px; border: 1px solid #dcdfe6; border-radius: 6px; padding: 0 10px; font-size: 13px; outline: none; transition: all .2s;
  &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba(197,48,48,0.15); }
}
.gpd-date { flex: 1; width: 100%; }
.req { color: $primary; }
.gpd-tip { display: flex; align-items: center; gap: 5px; margin-top: 10px; font-size: 12px; color: #8a4b46; background: #FFF5F5; border-radius: 6px; padding: 7px 10px;
  i { color: $primary; }
}
.gpd-members-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;
  b { color: $primary; }
}
.gpd-add { display: flex; align-items: center; gap: 4px; padding: 5px 12px; background: $primary; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; font-weight: 600;
  &:hover { opacity: 0.9; }
}
.gpd-empty { text-align: center; padding: 26px 16px; color: #bbb;
  i { font-size: 34px; display: block; margin-bottom: 8px; }
  p { margin: 0; font-size: 13px; }
}
.gpd-members { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 10px; }
.gpd-chip { display: flex; align-items: center; gap: 10px; background: #fff; border: 1px solid #e4beba; border-radius: 8px; padding: 8px 10px; transition: all .2s;
  &:hover { border-color: $primary; box-shadow: 0 2px 8px rgba(197,48,48,0.1); }
}
.gpd-avatar { width: 34px; height: 34px; border-radius: 50%; background: $primary; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 15px; font-weight: 600; flex-shrink: 0; }
.gpd-chip-info { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.gpd-chip-name { font-size: 13px; font-weight: 700; color: #1b1c1c; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.gpd-emp { font-style: normal; font-size: 11px; color: #909399; font-weight: 400; margin-left: 5px; font-family: monospace; }
.gpd-chip-dept { font-size: 11px; color: #909399; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.gpd-x { color: #ccc; cursor: pointer; font-size: 14px; flex-shrink: 0;
  &:hover { color: $primary; }
}
.gpd-footer { display: flex; justify-content: space-between; align-items: center; }
.gpd-sum { font-size: 12px; color: #909399; }
.gpd-actions { display: flex; gap: 8px; }
</style>
