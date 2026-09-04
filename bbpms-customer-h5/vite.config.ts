import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'

export default defineConfig({
  plugins: [vue()],
  resolve: { alias: { '@': path.resolve(__dirname, 'src') } },
  server: {
    host: '0.0.0.0', port: 9003,
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } }
  },
  build: { target: 'esnext', outDir: 'dist', sourcemap: false, chunkSizeWarningLimit: 1200 }
})
