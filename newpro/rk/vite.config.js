import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

const proxyTarget =
  process.env.VITE_PROXY_TARGET ||
  process.env.VITE_GATEWAY_BASE_URL ||
  'http://127.0.0.1:10010'

const minioProxyTarget =
  process.env.VITE_MINIO_PROXY_TARGET ||
  'http://127.0.0.1:9000'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/styles/variables.scss" as *;@use "@/styles/mixins.scss" as *;`
      }
    }
  },
  server: {
    port: 5173,
    allowedHosts: true,
    host: true,
    proxy: {
      '/api': {
        target: proxyTarget,
        changeOrigin: true,
        rewrite: (path) => path
      },
      '/roles': {
        target: proxyTarget,
        changeOrigin: true
      },
      '/users': {
        target: proxyTarget,
        changeOrigin: true
      },
      '/tenants': {
        target: proxyTarget,
        changeOrigin: true
      },
      '/auth': {
        target: proxyTarget,
        changeOrigin: true
      },
      '/menus': {
        target: proxyTarget,
        changeOrigin: true
      },
      '/admin/config': {
        target: proxyTarget,
        changeOrigin: true
      },
      '/admin/tenants': {
        target: proxyTarget,
        changeOrigin: true
      },
      '/admin/logs': {
        target: proxyTarget,
        changeOrigin: true
      },
      '/admin/ops': {
        target: proxyTarget,
        changeOrigin: true
      },
      '/notifications/api': {
        target: proxyTarget,
        changeOrigin: true
      },
      '/minio-files': {
        target: minioProxyTarget,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/minio-files\/?/, '/')
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    minify: 'esbuild',
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus'],
          'vue-vendor': ['vue', 'vue-router', 'pinia']
        }
      }
    }
  }
})
