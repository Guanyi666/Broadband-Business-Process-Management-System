# BBPMS 项目文档

> 文档入口。从这份开始，按需跳转。

## 项目定位

**BBPMS（Broadband Business Process Management System）** —— 参考国内三大运营商宽带装机业务流程的企业级 **模块化单体（Modular Monolith）**：1 个 Spring Boot 3.2.5 后端 + MySQL 8 + Redis 7 + 2 个 Vue 3 前端（PC 管理后台 + 装维 H5）。

业务主链路：客户申请 → 客服建单 → 审核 → 自动派单 → 装维接单 → 上门装机 → 拍照签名 → 完工归档 → 通知客户。

## 文档导航

| 文档 | 面向 | 内容 |
|---|---|---|
| [`../README.md`](../README.md) | 所有人 | 项目简介、技术栈、快速开始、默认账号 |
| [`../CLAUDE.md`](../CLAUDE.md) | 开发者 / AI | 架构关键点、命令、横切模式、注意事项 |
| [`PROJECT_TOUR.md`](PROJECT_TOUR.md) | 交接者 | ★ 项目导览：模块 / 表 / API / 流程 / FAQ / 扩展 |
| [`requirements.md`](requirements.md) | 项目经理 | 项目需求与功能范围 |
| [`architecture.md`](architecture.md) | 架构师 | 系统架构、模块划分、核心流程 |
| [`database-design.md`](database-design.md) | 数据库 | 表结构、索引、设计约定 |
| [`api-document.md`](api-document.md) | 前后端 | 接口约定与接口清单 |
| [`deployment.md`](deployment.md) | 运维 | 环境要求、部署流程、运维说明 |
| [`project-plan.md`](project-plan.md) | 项目经理 | 阶段规划、当前状态、已知问题、路线图 |

> 历史归档：重构前（11 微服务方案）的设计文档已移入 `temporary_backup/docs-legacy/`，仅供历史参考，**不再适用**。
