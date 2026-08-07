<template>
  <aside class="sidebar-container">
    <div class="sidebar-header">
      <div class="logo-section">
        <h1 class="sidebar-title">问题整改管理系统</h1>
        <p class="sidebar-subtitle">政府/企业管理版</p>
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
      <button class="btn-primary" @click="handleAddIssue">
        <i class="el-icon el-icon-plus"></i>
        <span>发布新问题</span>
      </button>
      <div class="divider"></div>
      <a href="#" class="logout-link" @click.prevent="handleLogout">
        <i class="el-icon el-icon-switch-button"></i>
        <span>退出登录</span>
      </a>
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
    ...mapGetters([
      'sidebar'
    ]),
    routes() {
      return this.$router.options.routes
    },
    activeMenu() {
      const route = this.$route
      const { meta, path } = route
      if (meta.activeMenu) {
        return meta.activeMenu
      }
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
    },
    handleLogout() {
      this.$store.dispatch('user/logout').then(() => {
        this.$router.push('/login')
      }).catch(() => {
        this.$router.push('/login')
      })
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
  background-color: #ffffff;
  border-right: 1px solid #e4beba;
  display: flex;
  flex-direction: column;
  z-index: 50;
}

.sidebar-header {
  padding: 24px 16px;
  border-bottom: 1px solid #e4beba;
}

.logo-section {
  display: flex;
  flex-direction: column;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #a20513;
  margin: 0;
  letter-spacing: -0.02em;
}

.sidebar-subtitle {
  font-size: 13px;
  color: #5b403d;
  margin: 4px 0 0;
}

.el-scrollbar {
  flex: 1;
  overflow: hidden;
}

.scrollbar-wrapper {
  overflow-y: auto;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid #e4beba;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.btn-primary {
  width: 100%;
  background-color: #c62828;
  color: #ffffff;
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: bold;
  transition: all 0.2s;

  &:hover {
    background-color: #a20513;
  }

  &:active {
    transform: scale(0.98);
  }
}

.divider {
  height: 1px;
  background-color: rgba(228, 190, 186, 0.3);
  margin: 8px 0;
}

.logout-link {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 8px;
  color: #5b403d;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;

  &:hover {
    background-color: #e5e2e1;
    color: #a20513;
  }
}

.el-menu {
  border-right: none;
}

::v-deep .el-menu-item,
::v-deep .el-submenu__title {
  height: 44px !important;
  line-height: 44px !important;
  margin: 4px 8px !important;
  border-radius: 8px !important;
  color: #5b403d !important;
}

::v-deep .el-menu-item:hover,
::v-deep .el-submenu__title:hover {
  background-color: #f6f3f2 !important;
}

::v-deep .el-menu-item.is-active {
  background-color: #a20513 !important;
  color: #ffffff !important;
  font-weight: bold;
}

// 子菜单项：圆角、边距、缩进
::v-deep .el-submenu .el-menu-item {
  height: 40px !important;
  line-height: 40px !important;
  margin: 2px 12px 2px 32px !important;
  border-radius: 6px !important;
  font-size: 13px !important;
}

// 子菜单展开容器内边距
::v-deep .el-submenu .el-menu {
  padding: 4px 0 !important;
}

::v-deep .el-submenu.is-active > .el-submenu__title {
  color: #a20513 !important;
}
</style>
