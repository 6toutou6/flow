<template>
  <div v-if="visible" class="modal-overlay" @click.self="handleClose">
    <div class="modal-content">
      <div class="modal-header">
        <h3 class="modal-title">批量导入</h3>
        <button class="modal-close" @click="handleClose">
          <i class="el-icon-close"></i>
        </button>
      </div>
      <div class="modal-body">
        <div class="template-section">
          <button class="btn-template" @click="downloadTemplate">
            <i class="el-icon-download"></i>
            <span>下载导入模板</span>
          </button>
        </div>


        <div class="import-section">
          <div class="upload-area" @click="triggerFileInput" @dragover.prevent @drop="handleDrop">
            <input ref="fileInput" type="file" class="file-input" accept=".xlsx,.xls,.csv" @change="handleFileChange" />
            <div class="upload-icon">
              <i class="el-icon-upload"></i>
            </div>
            <p class="upload-text">{{ selectedFile ? selectedFile.name : '点击或拖拽文件到此处上传' }}</p>
            <p class="upload-hint">支持 .xlsx、.xls、.csv 格式文件</p>
          </div>

        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-secondary" @click="handleClose">取消</button>
        <button class="btn-primary" :disabled="!selectedFile || loading" @click="handleImport">
          <i v-if="loading" class="el-icon-loading"></i>
          <span>{{ loading ? '导入中...' : '确认导入' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ImportModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      selectedFile: null,
      loading: false
    }
  },
  watch: {
    visible(val) {
      if (!val) {
        this.selectedFile = null
        this.loading = false
      }
    }
  },
  methods: {
    handleClose() {
      this.$emit('close')
    },
    triggerFileInput() {
      this.$refs.fileInput.click()
    },
    handleFileChange(e) {
      const file = e.target.files[0]
      if (file) {
        this.selectedFile = file
      }
    },
    handleDrop(e) {
      const file = e.dataTransfer.files[0]
      if (file && (file.name.endsWith('.xlsx') || file.name.endsWith('.xls') || file.name.endsWith('.csv'))) {
        this.selectedFile = file
      }
    },
    downloadTemplate() {
      const headers = ['问题标题', '负责部门', '整改时限(YYYY-MM-DD)', '优先级(紧急/中/低)', '状态(待处理/整改中/已完成)', '问题描述']
      let csvContent = 'data:text/csv;charset=utf-8,\uFEFF' + headers.join(',') + '\n'
      csvContent += ['示例问题标题', '生产技术部', '2026-12-31', '中', '待处理', '这是一个示例问题描述'].join(',') + '\n'

      const encodedUri = encodeURI(csvContent)
      const link = document.createElement('a')
      link.setAttribute('href', encodedUri)
      link.setAttribute('download', '问题导入模板.csv')
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    },
    handleImport() {
      if (!this.selectedFile) return

      this.loading = true
      setTimeout(() => {
        this.$emit('import', this.selectedFile)
        this.loading = false
        this.handleClose()
      }, 1500)
    }
  }
}
</script>

<style lang="scss" scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background-color: white;
  border-radius: 3px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow: hidden;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #CBD5E1;
  background-color: #faf9f9;
}

.modal-title {
  font-size: 20px;
  font-weight: 700;
  color: #1b1c1c;
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #414755;
  padding: 4px;
  border-radius: 4px;

  &:hover {
    background-color: #efeded;
    color: #1b1c1c;
  }
}

.modal-body {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #CBD5E1;
  background-color: #faf9f9;
}

.import-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.upload-area {
  border: 2px dashed #CBD5E1;
  border-radius: 4px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  background-color: #faf9f9;

  &:hover {
    border-color: #334155;
    background-color: #fff5f5;
  }

  &:active {
    transform: scale(0.99);
  }
}

.file-input {
  display: none;
}

.upload-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background-color: #334155;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  font-size: 28px;
  color: white;
}

.upload-text {
  font-size: 16px;
  font-weight: 600;
  color: #1b1c1c;
  margin: 0 0 8px;
}

.upload-hint {
  font-size: 13px;
  color: #727786;
  margin: 0;
}

.template-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: #414755;
  display: block;
}

.btn-template {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background-color: #faf9f9;
  color: #334155;
  border-radius: 3px;
  font-weight: 600;
  border: 1px solid #334155;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  width: fit-content;

  &:hover {
    background-color: rgba(51, 65, 85, 0.1);
  }
}

.btn-primary {
  padding: 10px 24px;
  background-color: #334155;
  color: white;
  border-radius: 3px;
  font-weight: 700;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 8px;

  &:hover:not(:disabled) {
    opacity: 0.9;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.btn-secondary {
  padding: 10px 24px;
  background-color: #faf9f9;
  color: #414755;
  border-radius: 3px;
  font-weight: 600;
  border: 1px solid #CBD5E1;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background-color: #efeded;
  }
}
</style>
