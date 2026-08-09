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
        <a href="/guide/index.html" target="_blank" class="btn-open-new">
          <i class="el-icon-full-screen" />
          <span>新窗口打开</span>
        </a>
      </div>
    </div>
    <div class="guide-frame-wrapper">
      <iframe
        ref="guideFrame"
        src="/guide/index.html"
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
      loading: true
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
  color: $color-primary;
  background: $neutral-50;
  border: 1px solid $border-light;
  border-radius: $radius-sm;
  text-decoration: none;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: $neutral-100;
    border-color: $color-primary;
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
    color: $color-primary;
  }

  span {
    font-size: $font-size-sm;
    color: $text-secondary;
  }
}
</style>
