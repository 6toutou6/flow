<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- 页头 -->
        <div class="page-header">
          <div class="header-left">
            <div class="hl-row1">
            <button class="btn-back" @click="goList"><i class="el-icon-arrow-left" /> 返回列表</button>
            <nav class="breadcrumb">
              <span class="link" @click="goList">下发配置模板</span>
              <span>/</span>
              <span class="active">{{ isEdit ? '编辑模板' : '新增模板' }}</span>
            </nav>
          </div>
          <h3 class="page-heading">{{ isEdit ? '编辑下发配置模板' : '新增下发配置模板' }}</h3>
          </div>
        </div>

        <!-- 说明条 -->
        <section class="tip-bar">
          <i class="el-icon-info" />
          模板保存后可长期复用；新建/编辑任务时选中本模板即可一键拉取下发配置，避免反复配置出错。
        </section>
        <div v-if="isReadonly" class="readonly-tip-bar">
          <i class="el-icon-view" /> 样例模板 · 只读查看，可完整查看配置内容；如需修改请先「复制」模板。
        </div>

        <div class="form-wrap">
          <!-- 基本信息 -->
          <div class="form-card">
            <div class="card-title"><i class="el-icon-document" /> 基本信息</div>
            <div class="form-row">
              <label class="form-label"><span class="req">*</span> 配置名称</label>
              <input v-model="form.configName" class="form-input" placeholder="给这套配置起个名字，如：季度整改 / 月度巡查" maxlength="50">
            </div>
            <div class="form-row">
              <label class="form-label">备注</label>
              <textarea v-model="form.remark" class="form-textarea" rows="2" placeholder="备注说明（选填）" maxlength="200" />
            </div>
          </div>

          <!-- 触发规则 -->
          <div class="form-card">
            <div class="card-title"><i class="el-icon-alarm-clock" /> 触发规则 <span class="card-sub">决定每期什么时候开始下发</span></div>
            <div class="form-row">
              <label class="form-label"><span class="req">*</span> 周期类型</label>
              <el-radio-group v-model="form.cycleType" @change="onCycleChange">
                <el-radio-button :label="1">每周</el-radio-button>
                <el-radio-button :label="2">每月</el-radio-button>
                <el-radio-button :label="3">每季度</el-radio-button>
                <el-radio-button :label="4">单次下发</el-radio-button>
              </el-radio-group>
            </div>
            <div v-if="form.cycleType === 1" class="form-row">
              <label class="form-label"><span class="req">*</span> 每周几触发</label>
              <el-select v-model="form.cycleDay" placeholder="选择触发日（周几）" style="width:100%">
                <el-option v-for="d in weekDays" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </div>
            <div v-else-if="form.cycleType === 2 || form.cycleType === 3" class="form-row">
              <label class="form-label"><span class="req">*</span> 每月几号触发</label>
              <el-select v-model="form.cycleDay" placeholder="选择触发日（几号）" style="width:100%">
                <el-option v-for="n in 31" :key="n" :label="n + ' 号'" :value="n" />
              </el-select>
            </div>
            <div v-if="form.cycleType === 4" class="rule-tip"><i class="el-icon-info" /> 单次下发：不按周期，每次手动生成期次为一个独立期次</div>
            <!-- 效果预览 -->
            <div v-if="form.cycleType !== 4" class="preview-bar">
              <i class="el-icon-right" />
              <span>下期触发：<b>{{ triggerPreview || '—' }}</b></span>
            </div>
          </div>

          <!-- 截止规则 -->
          <div class="form-card">
            <div class="card-title"><i class="el-icon-time" /> 截止规则 <span class="card-sub">决定每期任务最晚什么时候完成</span></div>
            <div class="form-row">
              <label class="form-label"><span class="req">*</span> 截止天数</label>
              <div class="inline-control">
                <el-input-number v-model="form.deadlineDays" :min="1" :max="365" />
                <span class="field-tip">{{ form.cycleType === 4 ? '下发后 N 天内完成' : '触发后 N 天内完成' }}</span>
              </div>
            </div>
            <!-- 效果预览 -->
            <div v-if="form.cycleType !== 4" class="preview-bar">
              <i class="el-icon-right" />
              <span>下期截止：<b>{{ deadlinePreview || '—' }}</b>（触发后 {{ form.deadlineDays }} 天）</span>
            </div>
          </div>

          <!-- 催办规则 -->
          <div class="form-card">
            <div class="card-title"><i class="el-icon-bell" /> 催办规则 <span class="card-sub">截止前提前提醒处理人（仅记录日志，不真实通知）</span></div>
            <div class="form-row">
              <label class="form-label">提前催办</label>
              <div class="inline-control">
                <el-input-number v-model="form.urgeDays" :min="0" :max="180" />
                <span class="field-tip">截止前 N 天开始提醒（0 = 不催办）</span>
              </div>
            </div>
            <!-- 效果预览 -->
            <div v-if="form.cycleType !== 4 && form.urgeDays > 0" class="preview-bar">
              <i class="el-icon-right" />
              <span>下期提醒：<b>{{ urgePreview || '—' }}</b>（截止前 {{ form.urgeDays }} 天）</span>
            </div>
            <div v-else-if="form.cycleType !== 4" class="preview-bar muted">
              <i class="el-icon-minus" />
              <span>不催办</span>
            </div>
          </div>

          <!-- 操作 -->
          <div class="form-actions">
            <button class="btn-prev" @click="goList"><i class="el-icon-arrow-left" /> 取消</button>
            <button class="btn-submit" :disabled="saving || isReadonly" @click="handleSubmit">
              <i v-if="saving" class="el-icon-loading" />
              <i v-else class="el-icon-check" /> {{ isEdit ? '保存修改' : '保存模板' }}
            </button>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script>
import { getConfigTemplate, saveConfigTemplate } from '@/service/sys/FlowDispatchService'

export default {
  name: 'ConfigTemplateEdit',
  data() {
    return {
      loading: false,
      saving: false,
      weekDays: [
        { value: 1, label: '周一' },
        { value: 2, label: '周二' },
        { value: 3, label: '周三' },
        { value: 4, label: '周四' },
        { value: 5, label: '周五' },
        { value: 6, label: '周六' },
        { value: 7, label: '周日' }
      ],
      form: {
        id: null, configName: '', cycleType: 2, cycleDay: 1, deadlineDays: 7, urgeDays: 0, remark: ''
      }
    }
  },
  computed: {
    isEdit() { return !!this.$route.query.id },
    /** 只读查看（样例模板且非超管） */
    isReadonly() { return Number(this.$route.query.readonly) === 1 },
    templateId() { return this.$route.query.id || null },
    /** 下期触发时间（与后端周期窗口计算口径一致） */
    triggerDate() {
      const d = calcTrigger(this.form.cycleType, this.form.cycleDay)
      return d
    },
    triggerPreview() {
      if (!this.triggerDate) return null
      return formatDate(this.triggerDate) + ' ' + weekName(this.triggerDate)
    },
    deadlinePreview() {
      if (!this.triggerDate || !this.form.deadlineDays) return null
      const end = addDays(this.triggerDate, this.form.deadlineDays)
      return formatDate(end) + ' ' + weekName(end)
    },
    urgePreview() {
      if (!this.deadlinePreview || !this.form.urgeDays) return null
      const end = addDays(this.triggerDate, this.form.deadlineDays)
      const urge = addDays(end, -this.form.urgeDays)
      return formatDate(urge) + ' ' + weekName(urge)
    }
  },
  created() {
    if (this.isEdit) this.initEdit()
  },
  methods: {
    async initEdit() {
      this.loading = true
      try {
        const res = await getConfigTemplate(this.templateId)
        const row = res.data
        this.form = {
          id: row.id,
          configName: row.configName || '',
          cycleType: row.cycleType || 2,
          cycleDay: row.cycleDay || 1,
          deadlineDays: row.deadlineDays || 7,
          urgeDays: row.urgeDays || 0,
          remark: row.remark || ''
        }
        this.onCycleChange()
      } catch (e) {
        console.error(e)
        this.$message.error('模板加载失败')
      } finally {
        this.loading = false
      }
    },
    onCycleChange() {
      if (this.form.cycleType === 4) this.form.cycleDay = null
      else if (!this.form.cycleDay) this.form.cycleDay = 1
    },
    async handleSubmit() {
      if (this.isReadonly) { this.$message.warning('样例模板仅可查看'); return }
      if (!this.form.configName || !this.form.configName.trim()) {
        this.$message.warning('请填写配置名称')
        return
      }
      if (this.form.cycleType !== 4 && !this.form.cycleDay) {
        this.$message.warning('请选择触发日')
        return
      }
      if (!this.form.deadlineDays || this.form.deadlineDays <= 0) {
        this.$message.warning('请填写截止天数')
        return
      }
      this.saving = true
      try {
        const payload = {
          id: this.form.id,
          configName: this.form.configName.trim(),
          cycleType: this.form.cycleType,
          cycleDay: this.form.cycleType === 4 ? null : this.form.cycleDay,
          deadlineDays: this.form.deadlineDays,
          urgeDays: this.form.urgeDays,
          remark: this.form.remark
        }
        const res = await saveConfigTemplate(payload)
        this.$message.success(res.message || '保存成功')
        if (!this.isEdit) {
          this.goList()
        } else {
          // 编辑模式：保存后留在本页（回写返回的最新配置）
          if (res && res.data) this.form = { ...this.form, ...res.data }
        }
      } catch (e) {
        console.error(e)
        this.$message.error((e && e.message) || '保存失败')
      } finally {
        this.saving = false
      }
    },
    goList() {
      this.$router.push('/flow-dispatch/config-template')
    }
  }
}

// ===== 周期触发日期计算（与后端 FlowDispatchService.nextWindow 口径一致） =====
function calcTrigger(cycleType, cycleDay) {
  if (cycleType === 4 || !cycleDay) return null
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const day = Math.max(1, cycleDay)
  if (cycleType === 1) {
    const dow = (today.getDay() + 6) % 7 // 周一=0..周日=6
    const monday = new Date(today)
    monday.setDate(today.getDate() - dow)
    let d = new Date(monday)
    d.setDate(monday.getDate() + Math.min(day, 7) - 1)
    if (d < today) {
      d = new Date(monday)
      d.setDate(monday.getDate() + 7 + Math.min(day, 7) - 1)
    }
    return d
  }
  if (cycleType === 2) {
    const y = today.getFullYear()
    const m = today.getMonth()
    const last = new Date(y, m + 1, 0).getDate()
    let d = new Date(y, m, Math.min(day, last))
    if (d < today) {
      const nextLast = new Date(y, m + 2, 0).getDate()
      d = new Date(y, m + 1, Math.min(day, nextLast))
    }
    return d
  }
  // 季度
  const y = today.getFullYear()
  const q = Math.floor(today.getMonth() / 3)
  const fm = q * 3
  const last = new Date(y, fm + 1, 0).getDate()
  let d = new Date(y, fm, Math.min(day, last))
  if (d < today) {
    const nq = q === 3 ? 0 : q + 1
    const ny = q === 3 ? y + 1 : y
    const nm = nq * 3
    const nLast = new Date(ny, nm + 1, 0).getDate()
    d = new Date(ny, nm, Math.min(day, nLast))
  }
  return d
}

function addDays(date, n) {
  const d = new Date(date)
  d.setDate(d.getDate() + n)
  return d
}

function formatDate(d) {
  const pad = n => (n < 10 ? '0' + n : '' + n)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function weekName(d) {
  return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);
$border: #CBD5E1;
.dashboard-container { display: flex; min-height: 100vh; background-color: var(--color-primary-surface);  color: #1b1c1c; }
.main-content { width: 100%; display: flex; flex-direction: column; min-height: 100vh; }
.page-content { padding: 24px; display: flex; flex-direction: column; gap: 16px; width: 100%; box-sizing: border-box; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; }
.header-left { display: flex; flex-direction: column; align-items: flex-start; gap: 8px; }
.hl-row1 { display: flex; align-items: center; gap: 14px; }
.hl-row1 .breadcrumb { margin-bottom: 0; }
.breadcrumb { display: flex; gap: 8px; font-size: 12px; line-height: 20px; color: #414755; margin-bottom: 8px;
  .active { color: $primary; font-weight: 600; }
  .link { color: $primary; cursor: pointer;
    &:hover { text-decoration: underline; }
  }
}
.page-heading { font-size: 24px; line-height: 32px; font-weight: 600; color: #1b1c1c; }
.btn-back { display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; background: transparent; border: none; color: var(--color-primary); cursor: pointer; font-size: 13px; transition: background .2s;
  &:hover { background: rgba(var(--color-primary-rgb), 0.08); }
  &:hover { background: var(--color-primary-light); }
}
.tip-bar { display: flex; align-items: center; gap: 8px; background: var(--color-primary-light); border: 1px solid $border; color: var(--color-primary-hover); font-size: 13px; border-radius: 3px; padding: 10px 14px;
  i { color: $primary; }
}
.form-wrap { display: flex; flex-direction: column; gap: 14px; }
.form-card { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 18px 20px; }
.card-title { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 700; color: #1b1c1c; margin-bottom: 14px;
  i { color: $primary; }
}
.card-sub { font-size: 12px; color: #999; font-weight: 400; }
.form-row { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px;
  &:last-child { margin-bottom: 0; }
}
.form-label { font-size: 13px; color: #414755; font-weight: 600; }
.req { color: $primary; }
.form-input { height: 36px; border: 1px solid #dcdfe6; border-radius: 2px; padding: 0 10px; font-size: 13px; outline: none; transition: all .2s;
  &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb),0.15); }
}
.form-textarea { border: 1px solid #dcdfe6; border-radius: 2px; padding: 8px 10px; font-size: 13px; outline: none; resize: vertical; font-family: inherit;
  &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb),0.15); }
}
.inline-control { display: flex; align-items: center; gap: 10px; }
.field-tip { font-size: 12px; color: #909399; }
.rule-tip { display: flex; align-items: center; gap: 5px; font-size: 12px; color: var(--color-primary-hover); background: var(--color-primary-light); border-radius: 2px; padding: 7px 10px;
  i { color: $primary; }
}
.preview-bar { display: flex; align-items: center; gap: 6px; margin-top: 12px; padding: 9px 12px; background: var(--color-primary-surface); border: 1px dashed rgba(var(--color-primary-rgb),0.35); border-radius: 3px; font-size: 13px; color: var(--color-primary);
  i { color: $primary; }
  b { color: #1b1c1c; }
  &.muted { background: var(--color-primary-light); color: #999; border-style: solid; i { color: #bbb; } }
}
.form-actions { display: flex; justify-content: flex-end; gap: 10px; }
.btn-prev { display: flex; align-items: center; gap: 4px; padding: 9px 20px; background: #fff; border: 1px solid $border; border-radius: 3px; color: var(--color-primary); cursor: pointer; font-size: 13px;
  &:hover { background: var(--color-primary-light); }
}
.btn-submit { display: flex; align-items: center; gap: 6px; padding: 9px 26px; background: $primary; color: #fff; border: none; border-radius: 3px; cursor: pointer; font-size: 14px; font-weight: 700; box-shadow: 0 2px 6px rgba(var(--color-primary-rgb),0.2);
  &:hover { opacity: 0.9; }
  &:disabled { opacity: 0.5; cursor: not-allowed; box-shadow: none; }
}

.readonly-tip-bar { display: flex; align-items: center; gap: 6px; padding: 8px 12px; margin-top: 10px; background: rgba(180, 83, 9, 0.1); color: #B45309; border-radius: 3px; font-size: 13px; }
</style>
