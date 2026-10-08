import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 后端接口与上传文件代理
      '/api': 'http://localhost:8081',
      '/upload': 'http://localhost:8081'
    }
  }
})
