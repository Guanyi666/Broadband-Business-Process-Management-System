# BBPMS Sprint B 质量收口报告

**范围**：Sprint A 遗留问题清单（`docs/SPRINT_A_ISSUES.md` 第 5 节）——SA-P1-005、SA-P2-001/002/003/004/006/007/008 + 过程中新发现的 4 个问题（SB-N1 ~ SB-N4）。

**状态**：✅ 全部 12 项已关闭；`mvn verify` 49/49 全绿；6 个 e2e 脚本 73/73 全绿。

---

## 一、做了什么（6 项汇报）

| 项 | 问题 | 修复 |
|---|---|---|
| **SB-1** | SA-P1-005 考勤/派单在线键不一致（签到只写 `attendance:active`，派单候选读 `installers:active` → 已签到装维不参与派单） | `AttendanceServiceImpl.clockIn/clockOut` 同步维护 `installers:active`（`InstallerProfileServiceImpl.ONLINE_KEY` 改为 public 常量）；`AttendanceScheduleJob.autoCheckoutStale` 自动签出时同步移除 |
| **SB-2** | SA-P2-007 `login_log` 永远为空（listener 存在但 AuthServiceImpl 从未发布事件） | `AuthServiceImpl.login` 全分支发布 `LoginLogEvent`（成功 status=1，用户不存在/解密失败/密码错误/禁用 status=0），listener 不动 |
| **SB-3** | SA-P2-006 `OrderQueryReq.orderBy` 直接拼 wrapper 排序（SQL 注入面） | `OrderServiceImpl` 增加 `sanitizeOrderBy` 白名单（6 个真实列 + `asc|desc`），非法输入 → 400 |
| **SB-4** | SA-P2-004 并发派单语义：4 个工单 + 3 条记录（根因比登记更严重） | ① 删除 `OrderEventListener.onOrderAudited` 裸工单创建者（autoDispatch 独占创建）；② `autoDispatch`/`manualDispatch` 在第一个 SELECT **之前** 获取订单级锁 `lock:dispatch-order:{orderId}`（REPEATABLE_READ 快照必须在锁内创建），`afterCompletion`（commit 后）释放；③ 幂等短路：已存在非终态工单 → 200 + 同一 workOrderId。语义定为 **idempotent last-write-wins** |
| **SB-5** | SA-P2-008 `leave_sla_e2e.mjs` 场景 B 永远失败（脚本注释 1min vs 实际配置 30min） | 脚本按登记方案用 SQL 回拨 `dispatch_time` 31 分钟（不动后端配置/不重启），轮询 ≤100s 等扫描器取消；引擎零改动 |
| **SB-6** | SA-P2-001/002/003 考勤汇总缺失、请假 L2 可绕过 L1、休息固定 30 分钟 | 见下节"修改了什么" |

## 二、发现什么（问题定位）

1. **SA-P2-004 根因比登记描述严重**：`OrderAuditedEvent` 被两个 async listener 消费（一个建裸工单 installer=null，一个建完整工单）+ 三个并发执行者（listener + 2 次手动派单）在同一提交窗口内都通过了 `selectByOrderId` 检查 → 4 工单 + 3 记录（可复现）。
2. **快照陷阱（隐蔽）**：第一版修复仍失败——锁在 `findByOrderId` **之后**获取，事务首个 SELECT 已在锁外创建 REPEATABLE_READ 快照，锁内短路查询看到的是陈旧快照 → 必须锁前置到第一个 SELECT 之前。
3. **SB-N1 回归**：签到幂等早退 `existing.getClockInAt() != null` 没检查 `clockOutAt` → 当天签出后无法重新签到（被永远挡住）。
4. **SB-N2（MyBatis-Plus 陷阱）**：`rec.setClockOutAt(null)` 被 null-skip 更新策略吞掉 → 陈旧 `clock_out_at` 让签出永远幂等早退、装维永远在线。必须显式 `LambdaUpdateWrapper.set(col, null)`。
5. **SB-N4（DataScope 可见性）**：AUTO 派单在 async listener（无 SecurityContext）中执行 → 89/117 条 `create_by IS NULL` → SELF 过滤（`create_by = X`）把它们全部滤掉 → 调度员看不到系统自动派单记录（审计回溯数据源不可见）。
6. **SB-N3（MySQL 8 保留字）**：`att_attendance_summary.year_month` 是保留字 `YEAR_MONTH`——读路径 mapper SQL 和 MyBatis-Plus 生成 SQL 在真实调用时抛 1064（此前从未被调通，SA-P2-001 惰性聚合让它第一次可达）。
7. **SA-P2-008 确认是脚本/配置漂移**，引擎行为 Phase 10 已验证（回拨 dispatch_time 31min → 55s 内 AUTO_CANCELLED），不是应用 bug。

## 三、修改什么（文件清单）

| 文件 | 修改 |
|---|---|
| `attendance/service/impl/AttendanceServiceImpl.java` | 在线键联动（SB-1）；重签幂等早退修复 + 显式清 clockOutAt（SB-N1/N2）；breakStartAt 记录/真实时长结算/ON_BREAK 签出结算（SA-P2-003）；`monthly()` 惰性聚合 `rollup()`（SA-P2-001） |
| `attendance/job/AttendanceScheduleJob.java` | autoCheckoutStale 同步移除 `installers:active`（SB-1） |
| `user/service/impl/InstallerProfileServiceImpl.java` | `ONLINE_KEY` private → public 常量（SB-1） |
| `auth/service/impl/AuthServiceImpl.java` | 登录日志发布（SB-2） |
| `order/service/impl/OrderServiceImpl.java` | orderBy 白名单（SB-3） |
| `workorder/event/OrderEventListener.java` | 删除裸工单创建者 onOrderAudited（SB-4） |
| `dispatch/service/impl/DispatchServiceImpl.java` | 订单级锁前置 + 幂等短路 + afterCompletion 释放（SB-4） |
| `common/config/DataScopeInnerInterceptor.java` | SELF 子句 `(create_by = X OR create_by IS NULL)`（SB-N4） |
| `leave/service/impl/LeaveServiceImpl.java` | approve 强制逐级（level == currentLevel + 1）（SA-P2-002） |
| `attendance/entity/AttendanceRecord.java` | `breakStartAt` 字段（SA-P2-003） |
| `attendance/entity/AttendanceSummary.java` | `@TableField("\`year_month\`")` 反引号（SB-N3） |
| `attendance/vo/AttendanceVO.java` | `breakStartAt` 字段（SA-P2-003） |
| `attendance/mapper/AttendanceRecordMapper.java` | `aggregateMonth` 月度聚合 SQL（SA-P2-001） |
| `attendance/mapper/AttendanceSummaryMapper.java` | year_month 反引号（SB-N3） |
| `attendance/dto/AttendanceMonthlyStat.java` | 新增聚合结果 DTO（SA-P2-001） |
| `attendance/config/AttendanceProperties.java` | `offDutyTime`（默认 17:00）早退口径（SA-P2-001） |
| `middleware/mysql/init/03-schema-extensions.sql` | `break_start_at` 幂等加列（SA-P2-003） |
| `e2e/core_e2e.mjs` | 上线前置（先上线再审核——修复前脚本"通过"建立在 installer=null 的畸形工单上）；SA-P2-004 顺序注释 |
| `e2e/leave_sla_e2e.mjs` | 场景 B 回拨 dispatch_time（SA-P2-008） |

## 四、验证结果

| 验证 | 结果 |
|---|---|
| SA-P1-005 | Redis `installers:active` ZSET 签到即入、签出即出；派单候选 API 含仅签到的装维；`mvn verify` 49/49 |
| SA-P2-007 | login_log 0→N 行；`logs/login/page` 显示 登录成功/密码错误/用户不存在 |
| SA-P2-006 | `create_time desc` 200；`create_time desc; DROP TABLE broadband_order` / 无方向 / 未知列 → 400 |
| SA-P2-004 | 并发对（listener + 2 手动 / 仅手动）：响应同 workOrderId，DB 仅 1 工单 + 1 记录；core_e2e 19/19 |
| SA-P2-008 | `leave_sla_e2e.mjs` 7/7（status=AUTO_CANCELLED） |
| SA-P2-003 | 实测 ~65s 休息 → breakMinutes=1（非固定 30）；重复结束/违规开始被拒；ON_BREAK 签出结算 |
| SA-P2-002 | SICK 直 L2 → 400「必须按级别顺序」；L1→PENDING；L2→APPROVED；重复 → 400；单级申请 L2 → 400 |
| SA-P2-001 | 种子 3 天 → workDays=3 / totalWorkMinutes=1550 / lateCount=1 / earlyLeaveCount=1 / absentCount=18（21 工作日−3）；summary 落库；二次读取幂等 |
| 全量 e2e | core 19/19 · auth 20/20 · 403 5/5 · exception 14/14 · phase8 8/8 · leave_sla 7/7 = **73/73** |
| 回归 gate | **`mvn verify` 49 tests / 0 failures / BUILD SUCCESS** |

## 五、剩余问题

- SA-P3-001（DataScope DEPT/CUSTOM 未实现）与 SA-P3-002（`wo_sla_policy` 未接入 SLA 调度器）维持 **Deferred**——需要 dept 归属模型/策略库，属后续架构工作，不在 Sprint B 范围。
- 已知非阻塞：`WorkOrderIntegrationTest` 日志中的乐观锁版本冲突 ERROR 是集成测试异步 listener 的容错路径（`OrderEventListener.onInstallCompleted` 重试竞争），测试本身通过，无功能影响。
- `att_attendance_summary` 的 `late` 口径使用固定 09:00+`lateThresholdMinutes`（可在 `bbpms.attendance.*` 调整）；`absent` 按当月已过工作日（不含今天）计算——口径均为合理默认，如需企业自定义可后续加配置。

## 六、阻塞分析

无阻塞。全程依赖本地 MySQL+Redis（docker compose）与 e2e 脚本，未引入任何外部服务；未新增微服务/中间件；未改动稳定模块的既有契约（新增 `break_start_at` 列向后兼容，旧行在结算时回退 30 分钟）。

---

### 附：Sprint B 会话遗留验证脚本（均已删除）
`sb1_repro.mjs` / `sb1_candidates.mjs` / `sb1_out.mjs` / `sb1_resign.mjs` / `sb2_login.mjs` / `sb2_api.mjs` / `sb3_orderby.mjs` / `sb4_concurrent.mjs` / `sb4b.mjs` / `sb_n2.mjs` / `dr_check.mjs` / `sb6_verify.mjs` —— 一次性验证脚本，验证完成即清理；6 个正式 e2e 脚本保留在 `bbpms-app/e2e/`。
