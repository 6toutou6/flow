<template>
  <div class="navbar">
    <hamburger :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />

    <breadcrumb class="breadcrumb-container" />

    <div class="right-menu">
        <el-dropdown class="theme-switch" trigger="click" @command="handleTheme">
          <span class="theme-trigger">
            <i class="el-icon-magic-stick" />
            <span class="theme-label">{{ themeLabel }}</span>
            <i class="el-icon-arrow-down" />
          </span>
          <el-dropdown-menu slot="dropdown" class="user-dropdown theme-dropdown">
            <el-dropdown-item command="slate">
              <span class="theme-dot dot-slate" /> 石板蓝
              <i v-if="theme === 'slate'" class="el-icon-check theme-check" />
            </el-dropdown-item>
            <el-dropdown-item command="red">
              <span class="theme-dot dot-red" /> 中国红
              <i v-if="theme === 'red'" class="el-icon-check theme-check" />
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>

        <div class="user-info">
        <span class="user-item">
          <i class="el-icon-user" />
          <span class="user-label">{{ realName || username }}</span>
        </span>
        <span class="user-divider">|</span>
        <span class="user-item">
          <i class="el-icon-postcard" />
          <span class="user-label">{{ empNo }}</span>
        </span>
        <span class="user-divider">|</span>
        <span class="user-item">
          <i class="el-icon-office-building" />
          <span class="user-label">{{ deptName }}</span>
        </span>
      </div>

      <el-dropdown class="avatar-container" trigger="click" @command="handleCommand">
        <div class="avatar-wrapper">
          <div class="avatar-placeholder">
            {{ (realName || username || '?').charAt(0).toUpperCase() }}
          </div>
          <i class="el-icon-arrow-down" />
        </div>
        <el-dropdown-menu slot="dropdown" class="user-dropdown">
          <el-dropdown-item command="home">
            <i class="el-icon-s-home" /> 首页
          </el-dropdown-item>
          <el-dropdown-item command="logout" divided>
            <i class="el-icon-switch-button" /> 退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import Hamburger from '@/components/Hamburger'

export default {
  components: { Breadcrumb, Hamburger },
  data() {
    return {
      theme: 'slate'
    }
  },
  computed: {
    ...mapGetters([
      'sidebar', 'avatar', 'username',
      'empNo', 'realName', 'deptId', 'deptName'
    ]),
    themeLabel() {
      return this.theme === 'red' ? '中国红' : '石板蓝'
    }
  },
  created() {
    this.initTheme()
  },
  methods: {
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    // 初始化主题：从 localStorage 读取并应用到 html[data-theme]
    initTheme() {
      const saved = localStorage.getItem('flow-theme')
      const theme = saved === 'red' || saved === 'slate' ? saved : 'slate'
      this.theme = theme
      document.documentElement.setAttribute('data-theme', theme)
      if (saved !== theme) localStorage.setItem('flow-theme', theme)
    },
    handleTheme(command) {
      const theme = command === 'red' ? 'red' : 'slate'
      this.theme = theme
      localStorage.setItem('flow-theme', theme)
      document.documentElement.setAttribute('data-theme', theme)
      // 广播主题变更，供操作指引 iframe 等同步
      window.dispatchEvent(new CustomEvent('flow-theme-change', { detail: theme }))
    },
    handleCommand(command) {
      if (command === 'logout') {
        this.logout()
      } else if (command === 'home') {
        this.$router.push('/')
      }
    },
    async logout() {
      await this.$store.dispatch('user/logout')
      this.$router.push(`/login?redirect=${this.$route.fullPath}`)
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 56px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 0 rgba(var(--color-primary-rgb), 0.06), 0 2px 8px rgba(15, 23, 42, 0.04);
  display: flex;
  align-items: center;

  .hamburger-container {
    height: 56px;
    line-height: 56px;
    float: left;
    cursor: pointer;
    padding: 0 12px;
    transition: background 0.2s;
    -webkit-tap-highlight-color: transparent;
    display: flex;
    align-items: center;

    &:hover {
      background: rgba(var(--color-primary-rgb), 0.04);
    }
  }

  .breadcrumb-container {
    float: left;
    margin-left: 4px;
  }

  .right-menu {
    margin-left: auto;
    height: 56px;
    display: flex;
    align-items: center;

    &:focus {
      outline: none;
    }

    // 主题切换
    .theme-switch {
      margin-right: 18px;

      .theme-trigger {
        display: flex;
        align-items: center;
        gap: 6px;
        cursor: pointer;
        padding: 5px 10px;
        border-radius: 14px;
        border: 1px solid rgba(var(--color-primary-rgb), 0.15);
        background: rgba(var(--color-primary-rgb), 0.04);
        color: var(--color-primary);
        font-size: 12px;
        transition: all 0.2s;

        &:hover {
          border-color: var(--color-primary);
          background: rgba(var(--color-primary-rgb), 0.08);
        }

        .el-icon-magic-stick { font-size: 13px; }
        .el-icon-arrow-down { font-size: 11px; color: var(--color-primary); opacity: 0.7; }
      }
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 4px;
      height: 100%;
      line-height: 56px;
      margin-right: 16px;
      font-size: 13px;
      color: var(--color-primary);

      .user-item {
        display: flex;
        align-items: center;
        gap: 4px;
        padding: 0 4px;

        i {
          color: var(--color-primary);
          font-size: 14px;
        }

        .user-label {
          max-width: 120px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .user-divider {
        color: #94A3B8;
        font-size: 12px;
      }
    }

    .avatar-container {
      margin-right: 24px;

      .avatar-wrapper {
        display: flex;
        align-items: center;
        gap: 6px;
        cursor: pointer;
        padding: 4px 8px;
        border-radius: 3px;
        transition: background 0.2s;

        &:hover {
          background: rgba(var(--color-primary-rgb), 0.05);
        }

        .avatar-placeholder {
          width: 36px;
          height: 36px;
          border-radius: 3px;
          background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-deep) 100%);
          color: #fff;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 16px;
          font-weight: 700;
          letter-spacing: 0.02em;
          box-shadow: 0 2px 6px rgba(var(--color-primary-rgb), 0.3);
        }

        .el-icon-arrow-down {
          font-size: 11px;
          color: #94A3B8;
          transition: transform 0.2s;
        }

        &:hover .el-icon-arrow-down {
          color: var(--color-primary);
        }
      }
    }
  }
}

// 下拉菜单样式（非 scoped，影响弹出层）
.user-dropdown {
  border-radius: 3px;
  border: 1px solid rgba(var(--color-primary-rgb), 0.08);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.10);

  .el-dropdown-menu__item {
    padding: 10px 18px;
    font-size: 14px;
    color: var(--color-primary);
    display: flex;
    align-items: center;
    gap: 8px;

    i { font-size: 15px; color: #94A3B8; }

    &:hover {
      background-color: var(--color-primary-light);
      color: var(--color-primary);
      i { color: var(--color-primary); }
    }
  }
}

// 主题下拉菜单：颜色圆点
.theme-dropdown .el-dropdown-menu__item {
  display: flex;
  align-items: center;
  gap: 8px;

  .theme-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    flex: 0 0 auto;
    border: 1px solid rgba(15, 23, 42, 0.1);
  }
  .dot-slate { background: var(--color-primary); }
  .dot-red { background: var(--color-primary); }
  .theme-check { margin-left: auto; color: var(--color-primary); font-size: 13px; }
}
</style>
