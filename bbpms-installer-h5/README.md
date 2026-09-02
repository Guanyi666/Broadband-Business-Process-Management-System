# BBPMS Installer H5

> 装维工程师移动工作台 - Vue 3 + Vite + Vant

## 技术栈

- **Vue 3.4** + Composition API + TypeScript 5
- **Vite 5**
- **Vant 4** 移动端 UI
- **Pinia 2** + Vue Router 4
- **Axios** + interceptors
- **AMap JS API 2.0** 高德地图
- **JSEncrypt** RSA 密码加密
- **dayjs** 时间处理

## 快速开始

```bash
npm install
npm run dev      # http://localhost:9002
npm run build
```

## 环境变量 (.env.development)

```
VITE_API_BASE=http://localhost:9001
VITE_AMAP_KEY=your_amap_dev_key
VITE_RSA_PUBLIC_KEY=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQ...
```

## 核心功能

- **登录**:账号 / 密码 (RSA 加密) / 图形验证码
- **工单列表**:3 个 Tab(待接单 / 进行中 / 已完成)+ 下拉刷新 + 上拉加载
- **工单详情**:客户信息 + 地图 + 时间线 + 操作栏
- **装机流程(5 步)**:
  1. 到达现场 (GPS 距离 ≤ 200m)
  2. 装机信息 (ONU MAC / SN / OLT / 光衰)
  3. 现场照片 (≥ 3 张)
  4. 客户签名 (Canvas)
  5. 完工提交 (SAGA)
- **我的**:统计 / 上下班打卡 / 位置上报

## 目录结构

```
src/
├── api/          HTTP 接口
├── assets/       样式
├── components/   通用组件 (MapView, SignaturePad, PhotoUploader, LocationReporter)
├── router/       路由
├── stores/       Pinia
├── types/        TS 类型
├── utils/        工具函数
├── views/        页面
└── App.vue / main.ts
```

## 浏览器兼容

- iOS Safari 12+
- Android Chrome 70+
- 微信内置浏览器
