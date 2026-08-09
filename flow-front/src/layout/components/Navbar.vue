<template>
  <div class="navbar">
    <hamburger :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />

    <breadcrumb class="breadcrumb-container" />

    <div class="right-menu">
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
  computed: {
    ...mapGetters([
      'sidebar', 'avatar', 'username',
      'empNo', 'realName', 'deptId', 'deptName'
    ])
  },
  methods: {
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
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
  box-shadow: 0 1px 0 rgba(51, 65, 85, 0.06), 0 2px 8px rgba(15, 23, 42, 0.04);
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
      background: rgba(51, 65, 85, 0.04);
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

    .user-info {
      display: flex;
      align-items: center;
      gap: 4px;
      height: 100%;
      line-height: 56px;
      margin-right: 16px;
      font-size: 13px;
      color: #334155;

      .user-item {
        display: flex;
        align-items: center;
        gap: 4px;
        padding: 0 4px;

        i {
          color: #334155;
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
          background: rgba(51, 65, 85, 0.05);
        }

        .avatar-placeholder {
          width: 36px;
          height: 36px;
          border-radius: 3px;
          background: linear-gradient(135deg, #334155 0%, #1E293B 100%);
          color: #fff;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 16px;
          font-weight: 700;
          letter-spacing: 0.02em;
          box-shadow: 0 2px 6px rgba(51, 65, 85, 0.3);
        }

        .el-icon-arrow-down {
          font-size: 11px;
          color: #94A3B8;
          transition: transform 0.2s;
        }

        &:hover .el-icon-arrow-down {
          color: #334155;
        }
      }
    }
  }
}

// 下拉菜单样式（非 scoped，影响弹出层）
.user-dropdown {
  border-radius: 3px;
  border: 1px solid rgba(51, 65, 85, 0.08);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.10);

  .el-dropdown-menu__item {
    padding: 10px 18px;
    font-size: 14px;
    color: #334155;
    display: flex;
    align-items: center;
    gap: 8px;

    i { font-size: 15px; color: #94A3B8; }

    &:hover {
      background-color: #F1F5F9;
      color: #334155;
      i { color: #334155; }
    }
  }
}
</style>
