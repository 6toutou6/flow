<template>
  <aside class="sidebar-container">
    <div class="sidebar-header">
      <div class="logo-section">
        <div class="logo-mark">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="7" fill="#C53030"/>
            <path d="M7 14L12 19L21 9" stroke="white" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="logo-text">
          <h1 class="sidebar-title">流程管理系统</h1>
          <p class="sidebar-subtitle">线性顺序流转工作流平台</p>
        </div>
      </div>
    </div>

    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :background-color="variables.menuBg"
        :text-color="variables.menuText"
        :unique-opened="false"
        :active-text-color="variables.menuActiveText"
        :collapse-transition="false"
        mode="vertical"
      >
        <sidebar-item v-for="route in routes" :key="route.path" :item="route" :base-path="route.path" />
      </el-menu>
    </el-scrollbar>

    <div class="sidebar-footer">
      <button class="btn-new-task" @click="handleAddIssue">
        <i class="el-icon-plus" />
        <span>新建任务</span>
      </button>
      <div class="footer-meta">
        <span class="meta-version">v2.0</span>
      </div>
    </div>
  </aside>
</template>

<script>
import { mapGetters } from 'vuex'
import SidebarItem from './SidebarItem'
import variables from '@/styles/variables.scss'

export default {
  components: { SidebarItem },
  computed: {
    ...mapGetters(['sidebar']),
    routes() {
      return this.$router.options.routes
    },
    activeMenu() {
      const route = this.$route
      const { meta, path } = route
      if (meta.activeMenu) return meta.activeMenu
      return path
    },
    variables() {
      return variables
    },
    isCollapse() {
      return !this.sidebar.opened
    }
  },
  methods: {
    handleAddIssue() {
      this.$router.push('/task-process/index')
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebar-container {
  width: 240px;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  background-color: #fff;
  border-right: 1px solid rgba(197, 48, 48, 0.08);
  display: flex;
  flex-direction: column;
  z-index: 50;
}

// ---- 头部 Logo ----
.sidebar-header {
  padding: 20px 16px 16px;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-mark {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}

.sidebar-title {
  font-size: 15px;
  font-weight: 700;
  color: #1E1A19;
  margin: 0;
  letter-spacing: -0.01em;
  white-space: nowrap;
}

.sidebar-subtitle {
  font-size: 11px;
  color: #9A8F8C;
  margin: 0;
  white-space: nowrap;
  letter-spacing: 0.02em;
}

// ---- 滚动区 ----
.el-scrollbar {
  flex: 1;
  overflow: hidden;
}

.scrollbar-wrapper {
  overflow-y: auto;
}

// ---- 菜单项样式 ----
::v-deep .el-menu-item,
::v-deep .el-submenu__title {
  height: 44px !important;
  line-height: 44px !important;
  margin: 3px 8px !important;
  border-radius: 8px !important;
  color: #5C514E !important;
  font-size: 14px;
  transition: all 0.18s ease;
}

::v-deep .el-menu-item:hover,
::v-deep .el-submenu__title:hover {
  background-color: #F7F4F3 !important;
}

::v-deep .el-menu-item.is-active {
  background: linear-gradient(135deg, #C53030 0%, #A20513 100%) !important;
  color: #ffffff !important;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(197, 48, 48, 0.25);
}

// 子菜单项
::v-deep .el-submenu .el-menu-item {
  height: 38px !important;
  line-height: 38px !important;
  margin: 2px 12px 2px 36px !important;
  border-radius: 6px !important;
  font-size: 13px !important;
}

::v-deep .el-submenu .el-menu {
  padding: 2px 0 !important;
}

::v-deep .el-submenu.is-active > .el-submenu__title {
  color: #C53030 !important;
  font-weight: 600;
}

// 展开箭头颜色
::v-deep .el-submenu__icon-arrow {
  color: #9A8F8C;
}

// ---- 底部 ----
.sidebar-footer {
  padding: 12px 16px 16px;
  border-top: 1px solid rgba(197, 48, 48, 0.08);
}

.btn-new-task {
  width: 100%;
  background: linear-gradient(135deg, #C53030 0%, #A20513 100%);
  color: #fff;
  padding: 10px 16px;
  border-radius: 10px;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.02em;
  box-shadow: 0 2px 8px rgba(197, 48, 48, 0.25);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    background: linear-gradient(135deg, #B52828 0%, #8A0410 100%);
    box-shadow: 0 4px 14px rgba(197, 48, 48, 0.35);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0) scale(0.98);
    box-shadow: 0 1px 4px rgba(197, 48, 48, 0.2);
  }

  i {
    font-size: 16px;
    font-weight: 700;
  }
}

.footer-meta {
  margin-top: 10px;
  display: flex;
  justify-content: center;
}

.meta-version {
  font-size: 11px;
  color: #BEB4B2;
  letter-spacing: 0.05em;
}

// ---- el-menu 基础 ----
::v-deep .el-menu {
  border-right: none !important;
}
</style>
