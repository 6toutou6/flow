<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bg-circle bg-circle-1" />
      <div class="bg-circle bg-circle-2" />
      <div class="bg-pattern" />
    </div>

    <!-- 品牌区域 -->
    <div class="brand-area">
      <div class="brand-mark">
        <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
          <rect width="48" height="48" rx="12" fill="white" fill-opacity="0.18"/>
          <path d="M16 24L22 30L32 18" stroke="white" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <h1 class="brand-name">流程管理系统</h1>
      <p class="brand-desc">线性顺序流转工作流平台 · 高效协同 · 精准管控</p>
    </div>

    <!-- 登录卡片 -->
    <el-form
      ref="loginForm"
      :model="loginForm"
      :rules="loginRules"
      class="login-card"
      auto-complete="on"
      label-position="top"
      @keyup.enter.native="handleLogin"
    >
      <div class="card-header">
        <h2 class="card-title">账号登录</h2>
        <p class="card-desc">请输入您的账号信息登录系统</p>
      </div>

      <el-form-item prop="username" label="用户名">
        <el-input
          ref="username"
          v-model="loginForm.username"
          placeholder="请输入用户名"
          name="username"
          type="text"
          tabindex="1"
          auto-complete="on"
          prefix-icon="el-icon-user"
          size="medium"
        />
      </el-form-item>

      <el-form-item prop="password" label="密码">
        <el-input
          :key="passwordType"
          ref="password"
          v-model="loginForm.password"
          :type="passwordType"
          placeholder="请输入密码"
          name="password"
          tabindex="2"
          auto-complete="on"
          prefix-icon="el-icon-lock"
          size="medium"
        >
          <i
            slot="suffix"
            :class="passwordType === 'password' ? 'el-icon-view' : 'el-icon-view pwd-visible'"
            class="pwd-toggle"
            @click="showPwd"
          />
        </el-input>
      </el-form-item>

      <el-button
        :loading="loading"
        type="primary"
        class="login-btn"
        @click.native.prevent="handleLogin"
      >
        {{ loading ? '登录中...' : '登 录' }}
      </el-button>

      <div class="card-footer">
        <span class="footer-hint">测试账号：zhangsan / 123456</span>
      </div>
    </el-form>

    <!-- 底部版权 -->
    <div class="login-footer">
      <span>&copy; 2026 Flow 流程管理系统</span>
    </div>
  </div>
</template>

<script>
import { validUsername } from '@/utils/validate'

export default {
  name: 'Login',
  data() {
    const validateUsername = (rule, value, callback) => {
      if (!validUsername(value)) {
        callback(new Error('请输入正确的用户名'))
      } else {
        callback()
      }
    }
    const validatePassword = (rule, value, callback) => {
      if (value.length < 6) {
        callback(new Error('密码不能少于6位'))
      } else {
        callback()
      }
    }
    return {
      loginForm: {
        username: 'zhangsan',
        password: '123456'
      },
      loginRules: {
        username: [{ required: true, trigger: 'blur', validator: validateUsername }],
        password: [{ required: true, trigger: 'blur', validator: validatePassword }]
      },
      loading: false,
      passwordType: 'password',
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  methods: {
    showPwd() {
      if (this.passwordType === 'password') {
        this.passwordType = ''
      } else {
        this.passwordType = 'password'
      }
      this.$nextTick(() => {
        this.$refs.password.focus()
      })
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          this.$store.dispatch('user/login', this.loginForm).then(() => {
            this.$router.push({ path: this.redirect || '/' })
            this.loading = false
          }).catch(() => {
            this.loading = false
          })
        }
      })
    }
  }
}
</script>

<style lang="scss">
// 修复登录页 Element UI 样式
.login-container {
  .el-input__prefix {
    left: 14px;
    display: flex;
    align-items: center;

    .el-input__icon {
      color: #94A3B8;
      font-size: 16px;
      transition: color 0.2s;
    }
  }

  .el-input__inner {
    padding-left: 40px;
    height: 44px;
    background: #F1F5F9;
    border: 1.5px solid #E2E8F0;
    border-radius: 3px;
    font-size: 14px;
    color: #0F172A;
    transition: all 0.2s;

    &::placeholder {
      color: #94A3B8;
    }

    &:focus {
      background: #fff;
      border-color: #334155;
      box-shadow: 0 0 0 3px rgba(51, 65, 85, 0.08);
    }
  }

  .el-form-item.is-error .el-input__inner {
    border-color: #B91C1C;

    &:focus {
      box-shadow: 0 0 0 3px rgba(153, 27, 27, 0.12);
    }
  }

  .el-input__suffix {
    right: 10px;

    .pwd-toggle {
      cursor: pointer;
      color: #94A3B8;
      font-size: 18px;
      transition: color 0.2s;

      &:hover {
        color: #334155;
      }
      &.pwd-visible { opacity: 0.55; }
    }
  }
}
</style>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  width: 100%;
  background: linear-gradient(145deg, #0F172A 0%, #1E293B 20%, #334155 55%, #1E293B 85%, #0F172A 100%);
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

// ---- 背景装饰 ----
.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.04);

  &-1 {
    width: 600px;
    height: 600px;
    top: -200px;
    right: -150px;
  }

  &-2 {
    width: 400px;
    height: 400px;
    bottom: -100px;
    left: -80px;
  }
}

.bg-pattern {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 20% 80%, rgba(255,255,255,0.03) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255,255,255,0.04) 0%, transparent 50%);
}

// ---- 品牌区域 ----
.brand-area {
  text-align: center;
  margin-bottom: 32px;
  z-index: 1;
}

.brand-mark {
  margin-bottom: 16px;
  display: inline-block;
}

.brand-name {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0;
  letter-spacing: 0.04em;
}

.brand-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.65);
  margin: 8px 0 0;
  letter-spacing: 0.05em;
}

// ---- 登录卡片 ----
.login-card {
  width: 400px;
  max-width: 100%;
  padding: 36px 40px 28px;
  background: #fff;
  border-radius: 18px;
  box-shadow:
    0 2px 4px rgba(0, 0, 0, 0.08),
    0 8px 24px rgba(0, 0, 0, 0.12),
    0 24px 64px rgba(0, 0, 0, 0.16);
  z-index: 1;
  position: relative;
}

.card-header {
  text-align: center;
  margin-bottom: 28px;
}

.card-title {
  font-size: 22px;
  font-weight: 700;
  color: #0F172A;
  margin: 0;
  letter-spacing: 0.03em;
}

.card-desc {
  font-size: 13px;
  color: #64748B;
  margin: 6px 0 0;
}

// ---- 表单 ----
::v-deep .el-form-item__label {
  color: #334155;
  font-weight: 600;
  font-size: 13px;
  line-height: 1.4;
  padding-bottom: 6px;
}

::v-deep .el-form-item {
  margin-bottom: 18px;
}

::v-deep .el-form-item__error {
  font-size: 12px;
  padding-top: 3px;
  color: #B91C1C;
}

// ---- 登录按钮 ----
.login-btn {
  width: 100%;
  height: 44px;
  margin-top: 6px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.12em;
  border-radius: 3px;
  background: linear-gradient(135deg, #334155 0%, #1E293B 100%);
  border: none;
  box-shadow: 0 4px 14px rgba(51, 65, 85, 0.35);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(51, 65, 85, 0.45);
  }

  &:active {
    transform: translateY(0);
    box-shadow: 0 2px 8px rgba(51, 65, 85, 0.3);
  }

  &.is-loading {
    background: linear-gradient(135deg, #334155 0%, #1E293B 100%);
  }
}

// ---- 底部提示 ----
.card-footer {
  text-align: center;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid #E2E8F0;
}

.footer-hint {
  font-size: 12px;
  color: #94A3B8;
  letter-spacing: 0.02em;
}

// ---- 底部版权 ----
.login-footer {
  margin-top: 24px;
  z-index: 1;

  span {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.4);
    letter-spacing: 0.04em;
  }
}
</style>
