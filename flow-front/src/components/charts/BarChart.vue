<template>
  <section class="chart-card">
    <div class="chart-head">
      <div class="chart-title"><i :class="icon" /> {{ title }}</div>
      <div class="chart-meta">
        <span class="meta-item"><b>{{ max }}</b> 峰值</span>
        <span class="meta-item"><b>{{ avg }}</b> 平均</span>
        <span class="meta-item"><b>{{ total }}</b> {{ totalLabel }}</span>
      </div>
    </div>
    <div v-if="data && data.length" class="bar-chart">
      <div v-for="(d, i) in data" :key="i" class="bar-col" :title="`${d.date}：${d.count} 次`">
        <div class="bar-track">
          <div class="bar-fill" :class="{ hot: Number(d.count) === max && Number(d.count) > 0 }" :style="{ height: barHeight(d.count) }" />
        </div>
        <div v-show="d.count > 0" class="bar-val">{{ d.count }}</div>
        <div class="bar-date">{{ shortDate(d.date) }}</div>
      </div>
    </div>
    <div v-else class="chart-empty"><i class="el-icon-data-line empty-icon" /> {{ emptyText }}</div>
  </section>
</template>

<script>
/** 柱状趋势图（数据展示各维度页共用）：传入 [{ date, count }] 即可 */
export default {
  name: 'BarChart',
  props: {
    title: { type: String, default: '提交趋势' },
    icon: { type: String, default: 'el-icon-data-line' },
    data: { type: Array, default: () => [] },
    emptyText: { type: String, default: '暂无趋势数据' },
    /** 第三个指标的后缀文案，如「累计提交」「累计下发」 */
    totalLabel: { type: String, default: '累计提交' }
  },
  computed: {
    max() {
      return this.data.reduce((m, d) => Math.max(m, Number(d.count) || 0), 0)
    },
    total() {
      return this.data.reduce((s, d) => s + (Number(d.count) || 0), 0)
    },
    avg() {
      return this.data.length ? Math.round(this.total / this.data.length) : 0
    }
  },
  methods: {
    barHeight(count) {
      if (!this.max) return '0%'
      return Math.max(4, Math.round((Number(count) || 0) / this.max * 100)) + '%'
    },
    /** 日期轴标签：日粒度只显示 MM-DD，月/季度/年原样显示 */
    shortDate(date) {
      const s = String(date || '')
      return /^\d{4}-\d{2}-\d{2}$/.test(s) ? s.slice(5) : s
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);
$border: #CBD5E1;
.chart-card { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 18px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.chart-head { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
.chart-title { font-size: 15px; font-weight: 700; color: #414755; display: flex; align-items: center; gap: 6px;
  i { color: $primary; }
}
.chart-meta { display: flex; gap: 18px; font-size: 12px; color: #909399;
  .meta-item b { color: $primary; font-size: 15px; margin-right: 3px; }
}
.chart-empty { text-align: center; padding: 40px 0; color: #bbb; font-size: 13px; }
.bar-chart { display: flex; align-items: flex-end; gap: 3px; height: 220px; padding: 8px 4px 0; border-bottom: 1px solid #E2E8F0; overflow-x: auto; }
.bar-col { flex: 1; min-width: 18px; display: flex; flex-direction: column; align-items: center; height: 100%; }
.bar-track { flex: 1; width: 100%; display: flex; align-items: flex-end; justify-content: center; }
.bar-fill { width: 60%; max-width: 22px; min-height: 2px; background: rgba(var(--color-primary-rgb),0.35); border-radius: 3px 3px 0 0; transition: height .4s;
  &:hover { background: $primary; }
  &.hot { background: $primary; box-shadow: 0 0 6px rgba(var(--color-primary-rgb),0.5); }
}
.bar-val { font-size: 10px; color: $primary; font-weight: 700; height: 14px; line-height: 14px; }
.bar-date { font-size: 9px; color: #bbb; height: 16px; line-height: 16px; transform: scale(0.92); white-space: nowrap; }
</style>
