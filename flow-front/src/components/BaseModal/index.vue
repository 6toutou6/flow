<template>
  <transition name="modal-fade">
    <div v-if="visible" class="base-modal-overlay" @click.self="handleClose">
      <div class="base-modal-content" :style="{ maxWidth: maxWidth }">
        <!-- Header -->
        <div class="base-modal-header">
          <h3 class="base-modal-title">{{ title }}</h3>
          <button class="base-modal-close" @click="handleClose" aria-label="关闭">
            <i class="el-icon-close"></i>
          </button>
        </div>

        <!-- Body -->
        <div class="base-modal-body">
          <slot />
        </div>

        <!-- Footer -->
        <div v-if="$slots.footer" class="base-modal-footer">
          <slot name="footer" />
        </div>
      </div>
    </div>
  </transition>
</template>

<script>
export default {
  name: 'BaseModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: ''
    },
    maxWidth: {
      type: String,
      default: '600px'
    }
  },
  methods: {
    handleClose() {
      this.$emit('close')
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/styles/variables.scss";

.base-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.base-modal-content {
  background: $surface-card;
  border-radius: $radius-lg;
  width: 90%;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.base-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $space-5 $space-6;
  border-bottom: 1px solid $border-light;
  background: $neutral-50;
  flex-shrink: 0;
}

.base-modal-title {
  font-size: $font-size-2xl;
  font-weight: 700;
  color: $text-primary;
  margin: 0;
}

.base-modal-close {
  background: none;
  border: none;
  cursor: pointer;
  color: $text-tertiary;
  padding: $space-1;
  border-radius: $radius-sm;
  font-size: $font-size-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all $transition-fast;

  &:hover {
    background: $neutral-200;
    color: $text-primary;
  }
}

.base-modal-body {
  padding: $space-6;
  overflow-y: auto;
  flex: 1;
}

.base-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: $space-3;
  padding: $space-5 $space-6;
  border-top: 1px solid $border-light;
  background: $neutral-50;
  flex-shrink: 0;
}

// Transition
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;

  .base-modal-content {
    transition: transform 0.25s ease, opacity 0.25s ease;
  }
}

.modal-fade-enter,
.modal-fade-leave-to {
  opacity: 0;

  .base-modal-content {
    transform: translateY(-20px);
    opacity: 0;
  }
}
</style>
