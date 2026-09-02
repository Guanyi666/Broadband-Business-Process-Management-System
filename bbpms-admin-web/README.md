# BBPMS Admin Web

PC Admin Frontend for BBPMS (Broadband Package Product Management System).

## Tech Stack

- Vue 3.4 + Vite 5 + TypeScript 5
- Element Plus 2.5
- Pinia 2 + Vue Router 4
- Axios + Interceptors
- ECharts 5
- @vueuse/core
- vue-i18n (zh-CN, en-US)

## Quick Start

```bash
# install
pnpm install   # or npm install / yarn install

# dev
pnpm dev

# build
pnpm build

# preview production build
pnpm preview
```

Backend default address: `http://localhost:9001`. Override via `.env.development` (`VITE_API_BASE`).

## Directory

```
src/
  api/          # axios request modules
  assets/       # styles + static assets
  components/   # shared components
  directives/   # v-permission etc.
  layouts/      # AdminLayout shell
  locales/      # i18n
  router/       # routes + guards
  stores/       # pinia stores
  types/        # TS types
  utils/        # helpers
  views/        # pages
```

## Roles

- CS (客服) — create order, view customers
- AUDITOR (审核) — audit orders
- DISPATCHER (调度) — manual / auto dispatch
- SUPER_ADMIN (超管) — full access

## Notes

- Token is kept in memory + sessionStorage (fallback when backend doesn't set HttpOnly refresh cookie).
- All requests are routed through `src/utils/request.ts`.
- Buttons are permission-controlled via `v-permission` directive or `<PermissionButton>` wrapper.