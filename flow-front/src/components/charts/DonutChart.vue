<template>
  <div class="chart-card">
    <div class="chart-title"><i :class="icon" /> {{ title }}</div>
    <div class="donut-wrap">
      <div class="donut" :style="donutStyle">
        <div class="donut-hole">
          <b>{{ total }}</b>
          <span>{{ unit }}</span>
        </div>
      </div>
      <div class="donut-legend">
        <div v-for="(item, i) in data" :key="i" class="legend-item">
          <span class="legend-dot" :style="{ background: colorAt(i) }" />
          <span class="legend-name">{{ item.name }}</span>
          <span class="legend-value">{{ item.value }}</span>
        </div>
        <div v-if="!data || data.length === 0" class="legend-empty"><i class="el-icon-data-line empty-icon" /> {{ emptyText }}</div>
      </div>
    </div>
  </div>
</template>

<script>
const STATUS_COLORS = ['#334155', '#15803D', '#B45309', '#2B6CB0', '#6B46C1', '#6366F1']

/** 环形分布图（数据展示各维度页共用）：传入 [{ name, value }] 即可 */
export default {
  name: 'DonutChart',
  props: {
    title: { type: String, default: '状态分布' },
    icon: { type: String, default: 'el-icon-pie-chart' },
    data: { type: Array, default: () => [] },
    /** 环心数值下方的单位文案，如「任务」「期次」「节点」 */
    unit: { type: String, default: '条' },
    emptyText: { type: String, default: '暂无数据' }
  },
  computed: {
    total() {
      return this.data.reduce((s, i) => s + (Number(i.value) || 0), 0)
    },
    /** 环形图样式（conic-gradient 按占比分色） */
    donutStyle() {
      const list = this.data || []
      const sum = this.total
      if (sum <= 0) return { background: '#f0f0f0' }
      let start = 0
      const stops = list.map((item, i) => {
        const pct = (Number(item.value) / sum) * 100
        const from = start
        start += pct
        return `${STATUS_COLORS[i % STATUS_COLORS.length]} ${from}% ${start}%`
      })
      return { background: `conic-gradient(${stops.join(', ')})` }
    }
  },
  methods: {
    colorAt(i) {
      return STATUS_COLORS[i % STATUS_COLORS.length]
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);
$border: #CBD5E1;
.chart-card { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 18px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.chart-title { font-size: 15px; font-weight: 700; color: #414755; display: flex; align-items: center; gap: 6px; margin-bottom: 14px;
  i { color: $primary; }
}
.donut-wrap { display: flex; align-items: center; gap: 14px; padding: 10px 0 6px; }
.donut { width: 118px; height: 118px; border-radius: 50%; flex-shrink: 0; position: relative; display: flex; align-items: center; justify-content: center; }
.donut-hole { width: 68px; height: 68px; border-radius: 50%; background: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center;
  b { font-size: 19px; color: #1b1c1c; line-height: 1.1; }
  span { font-size: 10px; color: #909399; }
}
.donut-legend { flex: 1; display: flex; flex-direction: column; gap: 8px; min-width: 0; }
.legend-item { display: flex; align-items: center; gap: 6px; font-size: 12px; }
.legend-dot { width: 9px; height: 9px; border-radius: 3px; flex-shrink: 0; }
.legend-name { color: #414755; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.legend-value { font-weight: 700; color: #1b1c1c; }
.legend-empty { color: #bbb; font-size: 12px; }
</style>
