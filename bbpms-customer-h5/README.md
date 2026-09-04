# BBPMS 客户自助 H5

面向 `CUSTOMER` 角色的独立移动端。登录方式为账号密码，不包含短信验证码。

## 本地运行

```bash
npm install
npm run dev
```

默认地址：`http://localhost:9003`。开发代理会把 `/api` 转发到 `http://localhost:8080`。

演示账号：`customer1 / admin123`。该账号由
`middleware/mysql/init/07-customer-portal-schema.sql` 创建并绑定演示客户 1。

## 功能

- 我的订单、办理进度与时间线
- 自助报装、资源核查、客服退回后修改重提
- 预约改期
- 故障报修与投诉处理跟踪
- 完工评价（评分参与装维人员综合评分）
- 个人资料变更申请与审核状态
- 站内消息、修改登录密码

## 构建

```bash
npm run type-check
npm run build
```
