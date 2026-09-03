package com.bbpms.common.config;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.bbpms.common.security.SecurityContextHolder;
import com.bbpms.common.security.SecurityUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * Row-level data-scope enforcement for the {@code @DataScope}-annotated
 * paged queries.
 *
 * <p>Only the statements listed in {@link #SCOPED_STATEMENTS} are patched —
 * a whitelist avoids appending scope predicates to tables that lack the
 * columns (the old global approach produced invalid SQL, e.g.
 * {@code dept_id} which no business table has). Scope values (sys_role.data_scope):
 * 1=ALL, 2=DEPT, 3=DEPT_AND_CHILD, 4=SELF, 5=CUSTOM.
 *
 * <p>Business tables do not carry a {@code dept_id} column, so DEPT /
 * DEPT_AND_CHILD are implemented with a sub-select over
 * {@code sys_user.dept_id} (the creator's department) resolved from the
 * caller's {@code dept_id} (JWT {@code dept} claim):
 * <ul>
 *   <li>DEPT(2): {@code create_by IN (SELECT id FROM sys_user WHERE dept_id = ?)}</li>
 *   <li>DEPT_AND_CHILD(3): the department {@code sys_dept.path} is looked up,
 *       then {@code dept_id IN (SELECT id FROM sys_dept WHERE path = ? OR path LIKE ?)}
 *       covers the whole sub-tree.</li>
 * </ul>
 * System-created rows (auto dispatch, async listeners, schedulers) run without a
 * SecurityContext, so AutoFillHandler leaves {@code create_by} NULL — they must
 * remain visible to everyone, exactly like the SELF clause's {@code IS NULL} escape.
 */
@Slf4j
public class DataScopeInnerInterceptor implements InnerInterceptor {

    /** Mapped-statement id -> table alias used to qualify the scope column. */
    private static final Map<String, String> SCOPED_STATEMENTS = Map.of(
            "com.bbpms.workorder.mapper.WorkOrderMapper.selectPageWithScope", "w.",
            "com.bbpms.order.mapper.BroadbandOrderMapper.selectPageWithScope", "o.",
            "com.bbpms.dispatch.mapper.DispatchRecordMapper.selectPageWithScope", "r.",
            "com.bbpms.install.mapper.InstallRecordMapper.selectPageWithScope", "r.",
            "com.bbpms.user.mapper.SysUserMapper.selectList", ""
    );

    /** data_scope values (see sys_role.data_scope comment). */
    private static final int SCOPE_ALL = 1;
    private static final int SCOPE_DEPT = 2;
    private static final int SCOPE_DEPT_AND_CHILD = 3;
    private static final int SCOPE_SELF = 4;

    @Override
    public void beforePrepare(StatementHandler sh, Connection conn, Integer transactionTimeout) {
        PluginUtils.MPStatementHandler mp = PluginUtils.mpStatementHandler(sh);
        MappedStatement ms = mp.mappedStatement();
        if (ms.getSqlCommandType() != SqlCommandType.SELECT) return;

        // Only patch the whitelisted @DataScope statements.
        String alias = SCOPED_STATEMENTS.get(ms.getId());
        if (alias == null) return;

        BoundSql bound = sh.getBoundSql();
        String sql = bound.getSql();
        if (sql == null || !sql.trim().toUpperCase().startsWith("SELECT")) return;

        SecurityUser user = SecurityContextHolder.get();
        if (user == null || user.getUserId() == null) return;
        Integer scope = user.getDataScope();
        if (scope == null || scope == SCOPE_ALL) return; // ALL — no filter

        String clause;
        switch (scope) {
            case SCOPE_SELF -> {
                // System-created rows (auto dispatch, async listeners, schedulers) run
                // without a SecurityContext, so AutoFillHandler leaves create_by NULL.
                // They belong to nobody — treat them as visible to everyone within the
                // caller's scope, otherwise e.g. dispatchers would never see AUTO
                // dispatch records (SA-P2-004 regression).
                clause = "(" + alias + "create_by = " + user.getUserId()
                        + " OR " + alias + "create_by IS NULL)";
            }
            case SCOPE_DEPT -> {
                if (user.getDeptId() == null) {
                    log.warn("DataScope: scope=DEPT but caller {} has no dept_id; skipping filter",
                            user.getUserId());
                    return;
                }
                String deptUsers = "SELECT id FROM sys_user WHERE dept_id = " + user.getDeptId()
                        + " AND deleted = 0";
                clause = "(" + alias + "create_by IN (" + deptUsers + ")"
                        + " OR " + alias + "create_by IS NULL)";
            }
            case SCOPE_DEPT_AND_CHILD -> {
                if (user.getDeptId() == null) {
                    log.warn("DataScope: scope=DEPT_AND_CHILD but caller {} has no dept_id; skipping filter",
                            user.getUserId());
                    return;
                }
                String deptPath = findDeptPath(conn, user.getDeptId());
                if (deptPath == null) {
                    // Fail closed: without the path we cannot prove the sub-tree,
                    // so fall back to the caller's own department users only.
                    String ownDeptUsers = "SELECT id FROM sys_user WHERE dept_id = " + user.getDeptId()
                            + " AND deleted = 0";
                    clause = "(" + alias + "create_by IN (" + ownDeptUsers + ")"
                            + " OR " + alias + "create_by IS NULL)";
                    break;
                }
                String inScopeDepts = "SELECT id FROM sys_dept WHERE (path = '" + deptPath
                        + "' OR path LIKE '" + deptPath + "%') AND deleted = 0";
                String deptUsers = "SELECT id FROM sys_user WHERE dept_id IN (" + inScopeDepts + ")"
                        + " AND deleted = 0";
                clause = "(" + alias + "create_by IN (" + deptUsers + ")"
                        + " OR " + alias + "create_by IS NULL)";
            }
            default -> {
                log.warn("DataScope: scope={} on {} is unsupported (only 2=DEPT, 3=DEPT_AND_CHILD, 4=SELF "
                        + "implemented; 5=CUSTOM needs sys_role_data_scope); skipping filter",
                        scope, ms.getId());
                return;
            }
        }

        try {
            MetaObject meta = SystemMetaObject.forObject(sh);
            String orig = (String) meta.getValue("delegate.boundSql.sql");
            if (orig == null) orig = sql;
            String patched = injectWhere(orig, clause);
            meta.setValue("delegate.boundSql.sql", patched);
        } catch (Exception e) {
            log.warn("DataScopeInnerInterceptor patch failed for {}: {}", ms.getId(), e.getMessage());
        }
    }

    /** Materialized path (e.g. {@code /1/2/}) of a department, or null if missing. */
    private String findDeptPath(Connection conn, Long deptId) {
        if (conn == null) return null;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT path FROM sys_dept WHERE id = " + deptId + " AND deleted = 0")) {
            return rs.next() ? rs.getString(1) : null;
        } catch (SQLException e) {
            log.warn("DataScope: failed to resolve dept path for dept_id={}: {}", deptId, e.getMessage());
            return null;
        }
    }

    /**
     * Insert {@code AND (clause)} / {@code WHERE (clause)} immediately before the
     * first of ORDER BY / GROUP BY / LIMIT (last occurrence, to dodge subqueries),
     * or at the end if none. Never appends after a tail clause — the previous
     * implementation produced invalid SQL such as {@code ... ORDER BY x AND (c=1)}.
     */
    private String injectWhere(String sql, String clause) {
        String upper = sql.toUpperCase();
        int orderIdx = upper.lastIndexOf(" ORDER ");
        int groupIdx = upper.lastIndexOf(" GROUP ");
        int limitIdx = upper.lastIndexOf(" LIMIT ");

        int cut = sql.length();
        if (limitIdx >= 0) cut = Math.min(cut, limitIdx);
        if (orderIdx >= 0) cut = Math.min(cut, orderIdx);
        if (groupIdx >= 0) cut = Math.min(cut, groupIdx);

        int whereIdx = upper.indexOf(" WHERE ");
        String predicate = "(" + clause + ")";
        if (whereIdx >= 0 && whereIdx < cut) {
            return sql.substring(0, cut) + " AND " + predicate + " " + sql.substring(cut);
        }
        return sql.substring(0, cut) + " WHERE " + predicate + " " + sql.substring(cut);
    }
}
