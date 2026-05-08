package com.medinote.medinotebackend.dataagent.service;

import com.medinote.medinotebackend.dataagent.dto.AggregateMetric;
import com.medinote.medinotebackend.dataagent.dto.AggregateRequest;
import com.medinote.medinotebackend.dataagent.dto.FilterCondition;
import com.medinote.medinotebackend.dataagent.dto.GroupBySpec;
import com.medinote.medinotebackend.dataagent.dto.QueryRequest;
import com.medinote.medinotebackend.dataagent.dto.SortSpec;
import com.medinote.medinotebackend.dataagent.dto.TableResponseDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DynamicQueryService {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final Pattern SAFE_SQL_NAME = Pattern.compile("^[a-zA-Z0-9_]+$");

    private static final Set<String> ALLOWED_OPERATORS = Set.of(
            // equality / comparison
            "eq", "neq", "gt", "gte", "lt", "lte",
            // range
            "between",
            // set membership
            "in", "notIn",
            // pattern matching
            "like", "notLike", "startsWith", "endsWith", "contains",
            // null checks
            "isNull", "isNotNull"
    );

    private static final Set<String> ALLOWED_FUNCTIONS =
            Set.of("SUM", "COUNT", "AVG", "MIN", "MAX");

    /** Whitelisted functions for computed GROUP BY expressions. */
    private static final Set<String> ALLOWED_DATE_FUNCTIONS =
            Set.of("YEAR", "MONTH", "WEEK", "DAY", "QUARTER", "DATE");

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final JdbcTemplate        jdbcTemplate;
    private final MetadataService     metadataService;
    private final AccessPolicyService accessPolicyService;

    public DynamicQueryService(JdbcTemplate jdbcTemplate,
                               MetadataService metadataService,
                               AccessPolicyService accessPolicyService) {
        this.jdbcTemplate        = jdbcTemplate;
        this.metadataService     = metadataService;
        this.accessPolicyService = accessPolicyService;
    }

    // ── Simple paginated read ─────────────────────────────────────────────────

    public List<String> getAllowedTables(Authentication authentication) {
        String role = extractRole(authentication);
        if (accessPolicyService.hasFullAccess(role)) {
            return metadataService.getAllTables();
        }
        return metadataService.getAllTables().stream()
                .filter(table -> accessPolicyService.canReadTable(role, table))
                .sorted()
                .toList();
    }

    public TableResponseDto readTable(String tableName, int page, int size,
                                      Authentication authentication) {
        validateSqlIdentifier(tableName);
        String normalized = tableName.toLowerCase();
        String role = extractRole(authentication);

        checkTableAccess(role, normalized);

        List<String> selectedColumns = resolveColumns(role, normalized);

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 500));
        int offset    = safePage * safeSize;

        String colClause = String.join(", ", selectedColumns);
        long totalRows   = countRows(normalized, "", new Object[0]);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT " + colClause + " FROM " + normalized + " LIMIT ? OFFSET ?",
                safeSize, offset);

        return new TableResponseDto(normalized, safePage, safeSize, totalRows, selectedColumns, rows);
    }

    // ── Filtered / sorted query ───────────────────────────────────────────────

    public TableResponseDto queryTable(String tableName, QueryRequest request,
                                       Authentication authentication) {
        validateSqlIdentifier(tableName);
        String normalized = tableName.toLowerCase();
        String role = extractRole(authentication);

        checkTableAccess(role, normalized);

        List<String> selectedColumns = resolveColumns(role, normalized);

        int safePage = Math.max(request.getPage(), 0);
        int safeSize = Math.max(1, Math.min(request.getSize(), 500));
        int offset    = safePage * safeSize;

        List<Object> filterParams = new ArrayList<>();
        String whereClause = buildWhereClause(request.getFilters(), filterParams, selectedColumns);
        String orderClause = buildQueryOrderClause(request.getSort(), selectedColumns);

        String colClause = String.join(", ", selectedColumns);
        long totalRows   = countRows(normalized, whereClause, filterParams.toArray());

        List<Object> dataParams = new ArrayList<>(filterParams);
        dataParams.add(safeSize);
        dataParams.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT " + colClause + " FROM " + normalized + whereClause + orderClause + " LIMIT ? OFFSET ?",
                dataParams.toArray());

        return new TableResponseDto(normalized, safePage, safeSize, totalRows, selectedColumns, rows);
    }

    // ── Aggregation query ─────────────────────────────────────────────────────

    public TableResponseDto aggregateTable(String tableName, AggregateRequest request,
                                           Authentication authentication) {
        validateSqlIdentifier(tableName);
        String normalized = tableName.toLowerCase();
        String role = extractRole(authentication);

        checkTableAccess(role, normalized);

        List<String> visibleColumns = resolveColumns(role, normalized);

        // ── Validate raw groupBy ──────────────────────────────────────────────
        List<String> rawGroupBy = request.getGroupBy() != null
                ? request.getGroupBy() : new ArrayList<>();
        for (String col : rawGroupBy) {
            validateSqlIdentifier(col);
            if (!visibleColumns.contains(col)) {
                throw new SecurityException("Colonne groupBy inaccessible ou inexistante : " + col);
            }
        }

        // ── Validate computed groupBy expressions (YEAR/MONTH/WEEK/…) ─────────
        List<GroupBySpec> exprGroupBy = request.getGroupByExpressions() != null
                ? request.getGroupByExpressions() : new ArrayList<>();
        List<String> computedAliases = new ArrayList<>();
        for (GroupBySpec spec : exprGroupBy) {
            if (spec.getColumn() == null) {
                throw new IllegalArgumentException("column est requis dans chaque groupByExpression");
            }
            validateSqlIdentifier(spec.getColumn());
            if (!visibleColumns.contains(spec.getColumn())) {
                throw new SecurityException(
                        "Colonne inaccessible dans groupByExpression : " + spec.getColumn());
            }
            String fn = spec.getFunction() != null ? spec.getFunction().toUpperCase() : null;
            if (fn == null || !ALLOWED_DATE_FUNCTIONS.contains(fn)) {
                throw new IllegalArgumentException(
                        "Fonction groupByExpression invalide : " + spec.getFunction()
                        + ". Valeurs acceptées : " + ALLOWED_DATE_FUNCTIONS);
            }
            if (spec.getAlias() == null || spec.getAlias().isBlank()) {
                throw new IllegalArgumentException("alias est requis pour chaque groupByExpression");
            }
            validateSqlIdentifier(spec.getAlias());
            if (rawGroupBy.contains(spec.getAlias()) || computedAliases.contains(spec.getAlias())) {
                throw new IllegalArgumentException(
                        "Alias groupByExpression en double : " + spec.getAlias());
            }
            computedAliases.add(spec.getAlias());
        }

        if (rawGroupBy.isEmpty() && exprGroupBy.isEmpty()) {
            throw new IllegalArgumentException(
                    "groupBy ou groupByExpressions est obligatoire et doit contenir au moins une entrée");
        }

        // ── Validate metrics ──────────────────────────────────────────────────
        List<AggregateMetric> metrics = request.getMetrics();
        if (metrics == null || metrics.isEmpty()) {
            throw new IllegalArgumentException(
                    "metrics est obligatoire et doit contenir au moins une fonction");
        }
        List<String> metricAliases = new ArrayList<>();
        for (AggregateMetric m : metrics) {
            String fn = m.getFunction() != null ? m.getFunction().toUpperCase() : null;
            if (fn == null || !ALLOWED_FUNCTIONS.contains(fn)) {
                throw new IllegalArgumentException(
                        "Fonction invalide : " + m.getFunction()
                        + ". Valeurs acceptées : " + ALLOWED_FUNCTIONS);
            }
            if (m.getAlias() == null || m.getAlias().isBlank()) {
                throw new IllegalArgumentException("Chaque metric doit avoir un alias");
            }
            validateSqlIdentifier(m.getAlias());
            if (rawGroupBy.contains(m.getAlias())
                    || computedAliases.contains(m.getAlias())
                    || metricAliases.contains(m.getAlias())) {
                throw new IllegalArgumentException("Alias metric en double : " + m.getAlias());
            }
            metricAliases.add(m.getAlias());

            boolean isCountStar = "COUNT".equalsIgnoreCase(fn)
                    && ("*".equals(m.getColumn()) || m.getColumn() == null);
            if (m.isDistinct() && isCountStar) {
                throw new IllegalArgumentException(
                        "DISTINCT ne peut pas être utilisé avec COUNT(*)");
            }
            if (!isCountStar) {
                if (m.getColumn() == null) {
                    throw new IllegalArgumentException(
                            "column est requis pour la fonction " + m.getFunction());
                }
                validateSqlIdentifier(m.getColumn());
                if (!visibleColumns.contains(m.getColumn())) {
                    throw new SecurityException(
                            "Colonne metric inaccessible ou inexistante : " + m.getColumn());
                }
            }
        }

        // ── Build SELECT and GROUP BY expressions ─────────────────────────────
        List<String> selectParts  = new ArrayList<>(rawGroupBy);
        List<String> groupByExprs = new ArrayList<>(rawGroupBy); // expressions used in GROUP BY

        for (GroupBySpec spec : exprGroupBy) {
            String expr = spec.getFunction().toUpperCase() + "(" + spec.getColumn() + ")";
            selectParts.add(expr + " AS " + spec.getAlias());
            groupByExprs.add(expr);
        }

        for (AggregateMetric m : metrics) {
            boolean isCountStar = "COUNT".equalsIgnoreCase(m.getFunction())
                    && ("*".equals(m.getColumn()) || m.getColumn() == null);
            String colExpr;
            if (isCountStar) {
                colExpr = "COUNT(*)";
            } else if (m.isDistinct()) {
                colExpr = m.getFunction().toUpperCase() + "(DISTINCT " + m.getColumn() + ")";
            } else {
                colExpr = m.getFunction().toUpperCase() + "(" + m.getColumn() + ")";
            }
            selectParts.add(colExpr + " AS " + m.getAlias());
        }

        String selectClause  = String.join(", ", selectParts);
        String groupByClause = " GROUP BY " + String.join(", ", groupByExprs);

        // ── Filters — pre-aggregation (WHERE) ────────────────────────────────
        List<Object> filterParams = new ArrayList<>();
        String whereClause = buildWhereClause(request.getFilters(), filterParams, visibleColumns);

        // ── HAVING — post-aggregation filter ─────────────────────────────────
        // Valid references: metric aliases + raw groupBy cols + computed groupBy aliases
        Set<String> havingValidRefs = new LinkedHashSet<>();
        havingValidRefs.addAll(rawGroupBy);
        havingValidRefs.addAll(computedAliases);
        havingValidRefs.addAll(metricAliases);

        List<Object> havingParams = new ArrayList<>();
        String havingClause = buildHavingClause(request.getHaving(), havingParams, havingValidRefs);

        // ── Multi-sort (sorts preferred; falls back to legacy sort) ───────────
        List<SortSpec> sortList = resolveAggregateSortList(request);
        Set<String> sortValidRefs = new LinkedHashSet<>();
        sortValidRefs.addAll(rawGroupBy);
        sortValidRefs.addAll(computedAliases);
        sortValidRefs.addAll(metricAliases);
        String orderClause = buildMultiOrderClause(sortList, sortValidRefs);

        // ── Pagination ────────────────────────────────────────────────────────
        int safePage = Math.max(request.getPage(), 0);
        int safeSize = Math.max(1, Math.min(request.getSize(), 500));
        int offset    = safePage * safeSize;

        // ── Count total groups ────────────────────────────────────────────────
        // When HAVING is present the subquery must expose the metrics too so
        // the HAVING predicates are evaluated correctly inside the subquery.
        String countInnerSql = havingClause.isEmpty()
                ? "SELECT " + String.join(", ", groupByExprs)
                  + " FROM " + normalized + whereClause + groupByClause
                : "SELECT " + selectClause
                  + " FROM " + normalized + whereClause + groupByClause + havingClause;

        String countSql = "SELECT COUNT(*) FROM (" + countInnerSql + ") AS _agg_count";

        List<Object> countParams = new ArrayList<>(filterParams);
        if (!havingClause.isEmpty()) countParams.addAll(havingParams);

        Long total = countParams.isEmpty()
                ? jdbcTemplate.queryForObject(countSql, Long.class)
                : jdbcTemplate.queryForObject(countSql, Long.class, countParams.toArray());
        long totalRows = total != null ? total : 0L;

        // ── Data query ────────────────────────────────────────────────────────
        List<Object> dataParams = new ArrayList<>(filterParams);
        dataParams.addAll(havingParams);
        dataParams.add(safeSize);
        dataParams.add(offset);

        String dataSql = "SELECT " + selectClause
                + " FROM " + normalized
                + whereClause + groupByClause + havingClause + orderClause
                + " LIMIT ? OFFSET ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(dataSql, dataParams.toArray());

        // ── Output column list (matches SELECT order) ─────────────────────────
        List<String> outputColumns = new ArrayList<>(rawGroupBy);
        for (GroupBySpec spec : exprGroupBy) outputColumns.add(spec.getAlias());
        outputColumns.addAll(metricAliases);

        return new TableResponseDto(normalized, safePage, safeSize, totalRows, outputColumns, rows);
    }

    // ── WHERE / HAVING builders ───────────────────────────────────────────────

    /**
     * Builds a parameterised WHERE clause.
     * validColumns is the security gate — only listed column names are accepted.
     */
    private String buildWhereClause(List<FilterCondition> filters, List<Object> params,
                                    List<String> validColumns) {
        if (filters == null || filters.isEmpty()) return "";
        List<String> parts = buildConditions(filters, params, validColumns, "filter");
        return " WHERE " + String.join(" AND ", parts);
    }

    /**
     * Builds a parameterised HAVING clause.
     * validRefs: metric aliases + groupBy column names + computed groupBy aliases.
     */
    private String buildHavingClause(List<FilterCondition> conditions, List<Object> params,
                                     Set<String> validRefs) {
        if (conditions == null || conditions.isEmpty()) return "";
        List<String> parts = buildConditions(conditions, params, new ArrayList<>(validRefs), "having");
        return " HAVING " + String.join(" AND ", parts);
    }

    /**
     * Core condition builder shared by WHERE and HAVING.
     * All conditions are AND-combined. All parameters are bound via JDBC (no string injection).
     */
    private List<String> buildConditions(List<FilterCondition> filters, List<Object> params,
                                         List<String> validColumns, String context) {
        List<String> conditions = new ArrayList<>();

        for (FilterCondition f : filters) {
            if (f.getColumn() == null || f.getOperator() == null) {
                throw new IllegalArgumentException(
                        "Condition invalide (" + context + ") : column et operator sont obligatoires");
            }
            validateSqlIdentifier(f.getColumn());
            if (!validColumns.contains(f.getColumn())) {
                throw new SecurityException(
                        "Colonne inaccessible ou inexistante (" + context + ") : " + f.getColumn()
                        + ". Colonnes autorisées : " + validColumns);
            }

            String op = f.getOperator();
            if (!ALLOWED_OPERATORS.contains(op)) {
                throw new IllegalArgumentException(
                        "Opérateur invalide : " + op
                        + ". Valeurs acceptées : " + ALLOWED_OPERATORS);
            }

            switch (op) {
                case "in" -> {
                    List<?> values = requireList(f, "in");
                    String holders = values.stream().map(v -> "?").collect(Collectors.joining(", "));
                    conditions.add(f.getColumn() + " IN (" + holders + ")");
                    params.addAll(values);
                }
                case "notIn" -> {
                    List<?> values = requireList(f, "notIn");
                    String holders = values.stream().map(v -> "?").collect(Collectors.joining(", "));
                    conditions.add(f.getColumn() + " NOT IN (" + holders + ")");
                    params.addAll(values);
                }
                case "between" -> {
                    List<?> values = requireList(f, "between");
                    if (values.size() != 2) {
                        throw new IllegalArgumentException(
                                "'between' requiert exactement 2 valeurs [min, max], reçu : "
                                + values.size());
                    }
                    conditions.add(f.getColumn() + " BETWEEN ? AND ?");
                    params.add(values.get(0));
                    params.add(values.get(1));
                }
                case "isNull"    -> conditions.add(f.getColumn() + " IS NULL");
                case "isNotNull" -> conditions.add(f.getColumn() + " IS NOT NULL");
                case "like" -> {
                    conditions.add(f.getColumn() + " LIKE ?");
                    params.add(f.getValue());
                }
                case "notLike" -> {
                    conditions.add(f.getColumn() + " NOT LIKE ?");
                    params.add(f.getValue());
                }
                case "startsWith" -> {
                    conditions.add(f.getColumn() + " LIKE ?");
                    params.add(requireString(f, "startsWith") + "%");
                }
                case "endsWith" -> {
                    conditions.add(f.getColumn() + " LIKE ?");
                    params.add("%" + requireString(f, "endsWith"));
                }
                case "contains" -> {
                    conditions.add(f.getColumn() + " LIKE ?");
                    params.add("%" + requireString(f, "contains") + "%");
                }
                default -> {
                    // eq, neq, gt, gte, lt, lte
                    conditions.add(f.getColumn() + " " + toSqlOperator(op) + " ?");
                    params.add(f.getValue());
                }
            }
        }
        return conditions;
    }

    // ── Order clause builders ─────────────────────────────────────────────────

    /** Single-sort for queryTable — validates against the table's visible columns. */
    private String buildQueryOrderClause(SortSpec sort, List<String> visibleColumns) {
        if (sort == null || sort.getColumn() == null || sort.getColumn().isBlank()) return "";
        validateSqlIdentifier(sort.getColumn());
        if (!visibleColumns.contains(sort.getColumn())) {
            throw new SecurityException("Colonne de tri inaccessible : " + sort.getColumn());
        }
        return " ORDER BY " + sort.getColumn() + " " + direction(sort);
    }

    /**
     * Multi-column sort for aggregateTable.
     * validRefs: groupBy cols + computed aliases + metric aliases.
     */
    private String buildMultiOrderClause(List<SortSpec> sorts, Set<String> validRefs) {
        if (sorts == null || sorts.isEmpty()) return "";
        List<String> parts = new ArrayList<>();
        for (SortSpec s : sorts) {
            if (s.getColumn() == null || s.getColumn().isBlank()) continue;
            validateSqlIdentifier(s.getColumn());
            if (!validRefs.contains(s.getColumn())) {
                throw new SecurityException(
                        "Colonne de tri invalide : " + s.getColumn()
                        + ". Colonnes autorisées : " + validRefs);
            }
            parts.add(s.getColumn() + " " + direction(s));
        }
        return parts.isEmpty() ? "" : " ORDER BY " + String.join(", ", parts);
    }

    /** Prefers sorts (multi-column), falls back to the legacy sort (single-column). */
    private List<SortSpec> resolveAggregateSortList(AggregateRequest request) {
        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            return request.getSorts();
        }
        if (request.getSort() != null) {
            return List.of(request.getSort());
        }
        return List.of();
    }

    // ── Shared private helpers ────────────────────────────────────────────────

    private void checkTableAccess(String role, String tableName) {
        if (!metadataService.tableExists(tableName)) {
            throw new IllegalArgumentException("Table inexistante : " + tableName);
        }
        if (!accessPolicyService.canReadTable(role, tableName)) {
            throw new SecurityException("Accès refusé à la table : " + tableName);
        }
    }

    private List<String> resolveColumns(String role, String tableName) {
        List<String> realColumns    = metadataService.getTableColumns(tableName);
        List<String> allowedColumns = accessPolicyService.getAllowedColumns(role, tableName);
        List<String> excluded       = accessPolicyService.getExcludedColumns(tableName);

        List<String> selected = allowedColumns.contains("*")
                ? new ArrayList<>(realColumns)
                : realColumns.stream().filter(allowedColumns::contains).collect(Collectors.toList());

        if (!excluded.isEmpty()) {
            selected = selected.stream().filter(c -> !excluded.contains(c)).toList();
        }
        if (selected.isEmpty()) {
            throw new SecurityException("Aucune colonne autorisée pour la table : " + tableName);
        }
        return selected;
    }

    private long countRows(String tableName, String whereClause, Object[] params) {
        String sql = "SELECT COUNT(*) FROM " + tableName + whereClause;
        Long count = params.length == 0
                ? jdbcTemplate.queryForObject(sql, Long.class)
                : jdbcTemplate.queryForObject(sql, Long.class, params);
        return count != null ? count : 0L;
    }

    private String toSqlOperator(String operator) {
        return switch (operator) {
            case "eq"  -> "=";
            case "neq" -> "!=";
            case "gt"  -> ">";
            case "gte" -> ">=";
            case "lt"  -> "<";
            case "lte" -> "<=";
            default -> throw new IllegalArgumentException("Opérateur invalide : " + operator);
        };
    }

    private String direction(SortSpec sort) {
        return "desc".equalsIgnoreCase(sort.getDirection()) ? "DESC" : "ASC";
    }

    private List<?> requireList(FilterCondition f, String operator) {
        if (!(f.getValue() instanceof List)) {
            throw new IllegalArgumentException(
                    "L'opérateur '" + operator + "' requiert une liste JSON, ex: [\"A\",\"B\"]");
        }
        List<?> list = (List<?>) f.getValue();
        if (list.isEmpty()) {
            throw new IllegalArgumentException(
                    "La liste pour l'opérateur '" + operator + "' ne peut pas être vide");
        }
        return list;
    }

    private String requireString(FilterCondition f, String operator) {
        if (f.getValue() == null) {
            throw new IllegalArgumentException(
                    "L'opérateur '" + operator + "' requiert une valeur non nulle");
        }
        return f.getValue().toString();
    }

    private String extractRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities() == null
                || authentication.getAuthorities().isEmpty()) {
            throw new SecurityException("Utilisateur non authentifié");
        }
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "").toUpperCase())
                .orElseThrow(() -> new SecurityException("Aucun rôle trouvé"));
    }

    private void validateSqlIdentifier(String value) {
        if (value == null || !SAFE_SQL_NAME.matcher(value).matches()) {
            throw new IllegalArgumentException("Identifiant SQL invalide : " + value);
        }
    }
}
