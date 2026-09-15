<template>
  <section class="chart-card">
    <div class="chart-title"><i :class="icon" /> {{ title }}</div>
    <div v-if="data && data.length" class="rank-list">
      <div v-for="(item, i) in sliceRank" :key="i" class="rank-item">
        <span class="rank-no" :class="{ top: i < 3 }">{{ i + 1 }}</span>
        <span class="rank-name" :style="{ width: nameWidth }" :title="item.name">{{ item.name }}</span>
        <div class="rank-bar-track">
          <div class="rank-bar-fill" :style="{ width: rankWidth(item.value), background: colorAt(i) }" />
        </div>
        <span class="rank-value">{{ item.value }}<span v-if="unit" class="rank-unit"> {{ unit }}</span></span>
      </div>
      <div v-if="data.length > 5" class="rank-toggle" @click="expanded = !expanded">
        {{ expanded ? '收起' : '展开全部（' + data.length + '）' }}
      </div>
    </div>
    <div v-else class="chart-empty"><i class="el-icon-data-line empty-icon" /> {{ emptyText }}</div>
  </section>
</template>

<script>
const STATUS_COLORS = ['#334155', '#15803D', '#B45309', '#2B6CB0', '#6B46C1', '#6366F1']

/** 横向排行榜（数据展示各维度页共用）：传入 [{ name, value }] 即可；默认展示前 5 条，可展开全部 */
export default {
  name: 'RankList',
  props: {
    title: { type: String, default: '排行榜' },
    icon: { type: String, default: 'el-icon-s-data' },
    data: { type: Array, default: () => [] },
    emptyText: { type: String, default: '暂无数据' },
    /** 数值后缀，如「次」「个」 */
    unit: { type: String, default: '' },
    /** 名称列宽度（期次名较长时可调宽） */
    nameWidth: { type: String, default: '140px' }
  },
  data() {
    return { expanded: false }
  },
  computed: {
    sliceRank() {
      return this.expanded ? this.data : this.data.slice(0, 5)
    },
    max() {
      return this.data.reduce((m, i) => Math.max(m, Number(i.value) || 0), 0)
    }
  },
  methods: {
    rankWidth(value) {
      if (!this.max) return '6%'
      return Math.max(6, Math.round((Number(value) || 0) / this.max * 100)) + '%'
    },
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
.chart-empty { text-align: center; padding: 40px 0; color: #bbb; font-size: 13px; }
.rank-list { display: flex; flex-direction: column; gap: 12px; padding: 6px 0; }
.rank-item { display: flex; align-items: center; gap: 10px; font-size: 13px; }
.rank-no { width: 22px; height: 22px; border-radius: 50%; background: #f0f0f0; color: #909399; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; flex-shrink: 0;
  &.top { background: $primary; color: #fff; }
}
.rank-name { width: 140px; color: #414755; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex-shrink: 0; }
.rank-bar-track { flex: 1; height: 10px; background: #f5f5f5; border-radius: 2px; overflow: hidden; min-width: 40px; }
.rank-bar-fill { height: 100%; border-radius: 2px; transition: width .4s; }
.rank-value { width: 62px; text-align: right; font-weight: 700; color: #1b1c1c; flex-shrink: 0; }
.rank-unit { font-size: 10px; color: #aaa; font-weight: 400; }
.rank-toggle { text-align: center; padding: 4px 0; margin-top: 2px; font-size: 12px; color: $primary; cursor: pointer; user-select: none; border-top: 1px dashed #E2E8F0;
  &:hover { color: var(--color-primary-hover); }
}
</style>
