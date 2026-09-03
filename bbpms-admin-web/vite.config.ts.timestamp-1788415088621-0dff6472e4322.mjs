// vite.config.ts
import { defineConfig, loadEnv } from "file:///G:/2.Study/BBPMS/bbpms-admin-web/node_modules/vite/dist/node/index.js";
import vue from "file:///G:/2.Study/BBPMS/bbpms-admin-web/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import AutoImport from "file:///G:/2.Study/BBPMS/bbpms-admin-web/node_modules/unplugin-auto-import/dist/vite.js";
import Components from "file:///G:/2.Study/BBPMS/bbpms-admin-web/node_modules/unplugin-vue-components/dist/vite.js";
import { ElementPlusResolver } from "file:///G:/2.Study/BBPMS/bbpms-admin-web/node_modules/unplugin-vue-components/dist/resolvers.js";
import { fileURLToPath, URL } from "node:url";
var __vite_injected_original_import_meta_url = "file:///G:/2.Study/BBPMS/bbpms-admin-web/vite.config.ts";
var vite_config_default = defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  return {
    plugins: [
      vue(),
      AutoImport({
        imports: ["vue", "vue-router", "pinia", "@vueuse/core"],
        resolvers: [ElementPlusResolver()],
        dts: "src/auto-imports.d.ts",
        eslintrc: { enabled: true }
      }),
      Components({
        resolvers: [ElementPlusResolver()],
        dts: "src/components.d.ts"
      })
    ],
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", __vite_injected_original_import_meta_url))
      }
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `@use "@/assets/styles/variables.scss" as *;`
        }
      }
    },
    server: {
      host: "0.0.0.0",
      port: 5173,
      open: true,
      proxy: {
        // Pass /api through unchanged — the backend serves every controller under
        // /api/* (including /api/auth). Stripping it here 404'd every business
        // call in dev. Matches the installer H5 proxy which has no rewrite.
        "/api": {
          // Backend origin — VITE_API_BASE is now a relative path (/api), so the
          // proxy target must stay fixed at the backend host.
          target: "http://localhost:8080",
          changeOrigin: true
        }
      }
    },
    build: {
      target: "esnext",
      outDir: "dist",
      sourcemap: false,
      chunkSizeWarningLimit: 1500,
      rollupOptions: {
        output: {
          manualChunks: {
            vue: ["vue", "vue-router", "pinia"],
            element: ["element-plus", "@element-plus/icons-vue"],
            echarts: ["echarts", "vue-echarts"]
          }
        }
      }
    }
  };
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcudHMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJHOlxcXFwyLlN0dWR5XFxcXEJCUE1TXFxcXGJicG1zLWFkbWluLXdlYlwiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiRzpcXFxcMi5TdHVkeVxcXFxCQlBNU1xcXFxiYnBtcy1hZG1pbi13ZWJcXFxcdml0ZS5jb25maWcudHNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL0c6LzIuU3R1ZHkvQkJQTVMvYmJwbXMtYWRtaW4td2ViL3ZpdGUuY29uZmlnLnRzXCI7aW1wb3J0IHsgZGVmaW5lQ29uZmlnLCBsb2FkRW52IH0gZnJvbSAndml0ZSdcbmltcG9ydCB2dWUgZnJvbSAnQHZpdGVqcy9wbHVnaW4tdnVlJ1xuaW1wb3J0IEF1dG9JbXBvcnQgZnJvbSAndW5wbHVnaW4tYXV0by1pbXBvcnQvdml0ZSdcbmltcG9ydCBDb21wb25lbnRzIGZyb20gJ3VucGx1Z2luLXZ1ZS1jb21wb25lbnRzL3ZpdGUnXG5pbXBvcnQgeyBFbGVtZW50UGx1c1Jlc29sdmVyIH0gZnJvbSAndW5wbHVnaW4tdnVlLWNvbXBvbmVudHMvcmVzb2x2ZXJzJ1xuaW1wb3J0IHsgZmlsZVVSTFRvUGF0aCwgVVJMIH0gZnJvbSAnbm9kZTp1cmwnXG5cbmV4cG9ydCBkZWZhdWx0IGRlZmluZUNvbmZpZygoeyBtb2RlIH0pID0+IHtcbiAgY29uc3QgZW52ID0gbG9hZEVudihtb2RlLCBwcm9jZXNzLmN3ZCgpLCAnJylcblxuICByZXR1cm4ge1xuICAgIHBsdWdpbnM6IFtcbiAgICAgIHZ1ZSgpLFxuICAgICAgQXV0b0ltcG9ydCh7XG4gICAgICAgIGltcG9ydHM6IFsndnVlJywgJ3Z1ZS1yb3V0ZXInLCAncGluaWEnLCAnQHZ1ZXVzZS9jb3JlJ10sXG4gICAgICAgIHJlc29sdmVyczogW0VsZW1lbnRQbHVzUmVzb2x2ZXIoKV0sXG4gICAgICAgIGR0czogJ3NyYy9hdXRvLWltcG9ydHMuZC50cycsXG4gICAgICAgIGVzbGludHJjOiB7IGVuYWJsZWQ6IHRydWUgfVxuICAgICAgfSksXG4gICAgICBDb21wb25lbnRzKHtcbiAgICAgICAgcmVzb2x2ZXJzOiBbRWxlbWVudFBsdXNSZXNvbHZlcigpXSxcbiAgICAgICAgZHRzOiAnc3JjL2NvbXBvbmVudHMuZC50cydcbiAgICAgIH0pXG4gICAgXSxcbiAgICByZXNvbHZlOiB7XG4gICAgICBhbGlhczoge1xuICAgICAgICAnQCc6IGZpbGVVUkxUb1BhdGgobmV3IFVSTCgnLi9zcmMnLCBpbXBvcnQubWV0YS51cmwpKVxuICAgICAgfVxuICAgIH0sXG4gICAgY3NzOiB7XG4gICAgICBwcmVwcm9jZXNzb3JPcHRpb25zOiB7XG4gICAgICAgIHNjc3M6IHtcbiAgICAgICAgICBhZGRpdGlvbmFsRGF0YTogYEB1c2UgXCJAL2Fzc2V0cy9zdHlsZXMvdmFyaWFibGVzLnNjc3NcIiBhcyAqO2BcbiAgICAgICAgfVxuICAgICAgfVxuICAgIH0sXG4gICAgc2VydmVyOiB7XG4gICAgICBob3N0OiAnMC4wLjAuMCcsXG4gICAgICBwb3J0OiA1MTczLFxuICAgICAgb3BlbjogdHJ1ZSxcbiAgICAgIHByb3h5OiB7XG4gICAgICAgIC8vIFBhc3MgL2FwaSB0aHJvdWdoIHVuY2hhbmdlZCBcdTIwMTQgdGhlIGJhY2tlbmQgc2VydmVzIGV2ZXJ5IGNvbnRyb2xsZXIgdW5kZXJcbiAgICAgICAgLy8gL2FwaS8qIChpbmNsdWRpbmcgL2FwaS9hdXRoKS4gU3RyaXBwaW5nIGl0IGhlcmUgNDA0J2QgZXZlcnkgYnVzaW5lc3NcbiAgICAgICAgLy8gY2FsbCBpbiBkZXYuIE1hdGNoZXMgdGhlIGluc3RhbGxlciBINSBwcm94eSB3aGljaCBoYXMgbm8gcmV3cml0ZS5cbiAgICAgICAgJy9hcGknOiB7XG4gICAgICAgICAgLy8gQmFja2VuZCBvcmlnaW4gXHUyMDE0IFZJVEVfQVBJX0JBU0UgaXMgbm93IGEgcmVsYXRpdmUgcGF0aCAoL2FwaSksIHNvIHRoZVxuICAgICAgICAgIC8vIHByb3h5IHRhcmdldCBtdXN0IHN0YXkgZml4ZWQgYXQgdGhlIGJhY2tlbmQgaG9zdC5cbiAgICAgICAgICB0YXJnZXQ6ICdodHRwOi8vbG9jYWxob3N0OjgwODAnLFxuICAgICAgICAgIGNoYW5nZU9yaWdpbjogdHJ1ZVxuICAgICAgICB9XG4gICAgICB9XG4gICAgfSxcbiAgICBidWlsZDoge1xuICAgICAgdGFyZ2V0OiAnZXNuZXh0JyxcbiAgICAgIG91dERpcjogJ2Rpc3QnLFxuICAgICAgc291cmNlbWFwOiBmYWxzZSxcbiAgICAgIGNodW5rU2l6ZVdhcm5pbmdMaW1pdDogMTUwMCxcbiAgICAgIHJvbGx1cE9wdGlvbnM6IHtcbiAgICAgICAgb3V0cHV0OiB7XG4gICAgICAgICAgbWFudWFsQ2h1bmtzOiB7XG4gICAgICAgICAgICB2dWU6IFsndnVlJywgJ3Z1ZS1yb3V0ZXInLCAncGluaWEnXSxcbiAgICAgICAgICAgIGVsZW1lbnQ6IFsnZWxlbWVudC1wbHVzJywgJ0BlbGVtZW50LXBsdXMvaWNvbnMtdnVlJ10sXG4gICAgICAgICAgICBlY2hhcnRzOiBbJ2VjaGFydHMnLCAndnVlLWVjaGFydHMnXVxuICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgfVxuICAgIH1cbiAgfVxufSkiXSwKICAibWFwcGluZ3MiOiAiO0FBQTBSLFNBQVMsY0FBYyxlQUFlO0FBQ2hVLE9BQU8sU0FBUztBQUNoQixPQUFPLGdCQUFnQjtBQUN2QixPQUFPLGdCQUFnQjtBQUN2QixTQUFTLDJCQUEyQjtBQUNwQyxTQUFTLGVBQWUsV0FBVztBQUw0SSxJQUFNLDJDQUEyQztBQU9oTyxJQUFPLHNCQUFRLGFBQWEsQ0FBQyxFQUFFLEtBQUssTUFBTTtBQUN4QyxRQUFNLE1BQU0sUUFBUSxNQUFNLFFBQVEsSUFBSSxHQUFHLEVBQUU7QUFFM0MsU0FBTztBQUFBLElBQ0wsU0FBUztBQUFBLE1BQ1AsSUFBSTtBQUFBLE1BQ0osV0FBVztBQUFBLFFBQ1QsU0FBUyxDQUFDLE9BQU8sY0FBYyxTQUFTLGNBQWM7QUFBQSxRQUN0RCxXQUFXLENBQUMsb0JBQW9CLENBQUM7QUFBQSxRQUNqQyxLQUFLO0FBQUEsUUFDTCxVQUFVLEVBQUUsU0FBUyxLQUFLO0FBQUEsTUFDNUIsQ0FBQztBQUFBLE1BQ0QsV0FBVztBQUFBLFFBQ1QsV0FBVyxDQUFDLG9CQUFvQixDQUFDO0FBQUEsUUFDakMsS0FBSztBQUFBLE1BQ1AsQ0FBQztBQUFBLElBQ0g7QUFBQSxJQUNBLFNBQVM7QUFBQSxNQUNQLE9BQU87QUFBQSxRQUNMLEtBQUssY0FBYyxJQUFJLElBQUksU0FBUyx3Q0FBZSxDQUFDO0FBQUEsTUFDdEQ7QUFBQSxJQUNGO0FBQUEsSUFDQSxLQUFLO0FBQUEsTUFDSCxxQkFBcUI7QUFBQSxRQUNuQixNQUFNO0FBQUEsVUFDSixnQkFBZ0I7QUFBQSxRQUNsQjtBQUFBLE1BQ0Y7QUFBQSxJQUNGO0FBQUEsSUFDQSxRQUFRO0FBQUEsTUFDTixNQUFNO0FBQUEsTUFDTixNQUFNO0FBQUEsTUFDTixNQUFNO0FBQUEsTUFDTixPQUFPO0FBQUE7QUFBQTtBQUFBO0FBQUEsUUFJTCxRQUFRO0FBQUE7QUFBQTtBQUFBLFVBR04sUUFBUTtBQUFBLFVBQ1IsY0FBYztBQUFBLFFBQ2hCO0FBQUEsTUFDRjtBQUFBLElBQ0Y7QUFBQSxJQUNBLE9BQU87QUFBQSxNQUNMLFFBQVE7QUFBQSxNQUNSLFFBQVE7QUFBQSxNQUNSLFdBQVc7QUFBQSxNQUNYLHVCQUF1QjtBQUFBLE1BQ3ZCLGVBQWU7QUFBQSxRQUNiLFFBQVE7QUFBQSxVQUNOLGNBQWM7QUFBQSxZQUNaLEtBQUssQ0FBQyxPQUFPLGNBQWMsT0FBTztBQUFBLFlBQ2xDLFNBQVMsQ0FBQyxnQkFBZ0IseUJBQXlCO0FBQUEsWUFDbkQsU0FBUyxDQUFDLFdBQVcsYUFBYTtBQUFBLFVBQ3BDO0FBQUEsUUFDRjtBQUFBLE1BQ0Y7QUFBQSxJQUNGO0FBQUEsRUFDRjtBQUNGLENBQUM7IiwKICAibmFtZXMiOiBbXQp9Cg==
