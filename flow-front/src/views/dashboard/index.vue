<template>
  <div class="guide-dashboard">
    <div class="guide-header">
      <div class="header-left">
        <nav class="breadcrumb">
          <span>系统首页</span>
          <span>/</span>
          <span class="active">操作指引</span>
        </nav>
        <h3 class="page-heading">操作指引</h3>
        <p class="page-subtitle">线性顺序流转工作流系统 · 使用手册与功能说明</p>
      </div>
      <div class="header-right">
        <a :href="guideSrc" target="_blank" class="btn-open-new">
          <i class="el-icon-full-screen" />
          <span>新窗口打开</span>
        </a>
      </div>
    </div>
    <div class="guide-frame-wrapper">
      <iframe
        ref="guideFrame"
        :src="guideSrc"
        class="guide-frame"
        title="操作指引"
        @load="onFrameLoad"
      />
      <div v-if="loading" class="frame-loading">
        <i class="el-icon-loading" />
        <span>加载操作指引中...</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Dashboard',
  data() {
    return {
      loading: true,
      currentTheme: 'slate'
    }
  },
  computed: {
    // 操作指引地址带上主题参数，供 iframe 内页面同步主题
    guideSrc() {
      return `/guide/index.html?theme=${this.currentTheme}`
    }
  },
  created() {
    const saved = localStorage.getItem('flow-theme')
    if (saved === 'red' || saved === 'slate') {
      this.currentTheme = saved
    }
    this.onThemeChange = e => {
      const theme = e && e.detail
      if (theme === 'red' || theme === 'slate') {
        this.currentTheme = theme
        this.loading = true
      }
    }
    window.addEventListener('flow-theme-change', this.onThemeChange)
  },
  beforeDestroy() {
    if (this.onThemeChange) {
      window.removeEventListener('flow-theme-change', this.onThemeChange)
    }
  },
  methods: {
    onFrameLoad() {
      this.loading = false
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/styles/common.scss";

.guide-dashboard {
  @include page-container;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px);
  overflow: hidden;
}

.guide-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px 24px 16px;
  border-bottom: 1px solid $border-light;
  background: $surface-card;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.breadcrumb {
  @include breadcrumb;
  margin-bottom: 2px;
}

.page-heading {
  @include page-heading;
}

.page-subtitle {
  font-size: $font-size-xs;
  color: $text-secondary;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: $space-2;
}

.btn-open-new {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 14px;
  font-size: $font-size-sm;
  color: var(--color-primary);
  background: $neutral-50;
  border: 1px solid $border-light;
  border-radius: $radius-sm;
  text-decoration: none;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: $neutral-100;
    border-color: var(--color-primary);
  }

  i {
    font-size: 14px;
  }
}

.guide-frame-wrapper {
  position: relative;
  flex: 1;
  overflow: hidden;
}

.guide-frame {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}

.frame-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: $surface-page;

  i {
    font-size: 32px;
    color: var(--color-primary);
  }

  span {
    font-size: $font-size-sm;
    color: $text-secondary;
  }
}
</style>
