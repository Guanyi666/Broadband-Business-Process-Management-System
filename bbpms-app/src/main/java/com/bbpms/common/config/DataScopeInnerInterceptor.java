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
 * <p>This release implements <b>SELF only</b> ({@code create_by = userId}).
 * DEPT / CUSTOM scopes need the business tables to carry a dept column (not in
 * the current schema) and are skipped with a warning rather than emitting
 * invalid SQL. The seed data uses 1 (SUPER_ADMIN) and 4 (every other role).
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

        if (scope != SCOPE_SELF) {
            log.warn("DataScope: scope={} on {} is unsupported (no dept column on business tables); skipping filter",
                    scope, ms.getId());
            return;
        }

        // System-created rows (auto dispatch, async listeners, schedulers) run
        // without a SecurityContext, so AutoFillHandler leaves create_by NULL.
        // They belong to nobody — treat them as visible to everyone within the
        // caller's scope, otherwise e.g. dispatchers would never see AUTO
        // dispatch records (SA-P2-004 regression).
        String clause = "(" + alias + "create_by = " + user.getUserId()
                + " OR " + alias + "create_by IS NULL)";
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
