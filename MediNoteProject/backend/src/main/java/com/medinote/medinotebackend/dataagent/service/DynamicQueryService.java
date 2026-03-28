package com.medinote.medinotebackend.dataagent.service;

import com.medinote.medinotebackend.dataagent.dto.AggregateMetric;
import com.medinote.medinotebackend.dataagent.dto.AggregateRequest;
import com.medinote.medinotebackend.dataagent.dto.FilterCondition;
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

    private static final Pattern SAFE_SQL_NAME = Pattern.compile("^[a-zA-Z0-9_]+$");

    private static final Set<String> ALLOWED_OPERATORS =
            Set.of("eq", "neq", "gt", "gte", "lt", "lte", "like", "in");

    private final JdbcTemplate jdbcTemplate;
    private final MetadataService metadataService;
    private final AccessPolicyService accessPolicyService;

    public DynamicQueryService(JdbcTemplate jdbcTemplate,
                               MetadataService metadataService,
                               AccessPolicyService accessPolicyService) {
        this.jdbcTemplate = jdbcTemplate;
        this.metadataService = metadataService;
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
        String orderClause = buildOrderClause(request.getSort(), selectedColumns);

        String colClause   = String.join(", ", selectedColumns);
        long totalRows     = countRows(normalized, whereClause, filterParams.toArray());

        List<Object> dataParams = new ArrayList<>(filterParams);
        dataParams.add(safeSize);
        dataParams.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT " + colClause + " FROM " + normalized + whereClause + orderClause + " LIMIT ? OFFSET ?",
                dataParams.toArray());

        return new TableResponseDto(normalized, safePage, safeSize, totalRows, selectedColumns, rows);
    }

    // ── Aggregation query ─────────────────────────────────────────────────────

    private static final Set<String> ALLOWED_FUNCTIONS =
            Set.of("SUM", "COUNT", "AVG", "MIN", "MAX");

    public TableResponseDto aggregateTable(String tableName, AggregateRequest request,
                                           Authentication authentication) {
        validateSqlIdentifier(tableName);
        String normalized = tableName.toLowerCase();
        String role = extractRole(authentication);

        checkTableAccess(role, normalized);

        List<String> visibleColumns = resolveColumns(role, normalized);

        // ── Validate groupBy ──────────────────────────────────────────────────
        List<String> groupBy = request.getGroupBy();
        if (groupBy == null || groupBy.isEmpty()) {
            throw new IllegalArgumentException("groupBy est obligatoire et doit contenir au moins une colonne");
        }
        for (String col : groupBy) {
            validateSqlIdentifier(col);
            if (!visibleColumns.contains(col)) {
                throw new SecurityException("Colonne groupBy inaccessible ou inexistante : " + col);
            }
        }

        // ── Validate metrics ──────────────────────────────────────────────────
        List<AggregateMetric> metrics = request.getMetrics();
        if (metrics == null || metrics.isEmpty()) {
            throw new IllegalArgumentException("metrics est obligatoire et doit contenir au moins une fonction");
        }
        Set<String> aliases = new LinkedHashSet<>();
        for (AggregateMetric m : metrics) {
            if (m.getFunction() == null || !ALLOWED_FUNCTIONS.contains(m.getFunction().toUpperCase())) {
                throw new IllegalArgumentException("Fonction invalide : " + m.getFunction()
                        + ". Valeurs acceptées : " + ALLOWED_FUNCTIONS);
            }
            if (m.getAlias() == null || m.getAlias().isBlank()) {
                throw new IllegalArgumentException("Chaque metric doit avoir un alias");
            }
            validateSqlIdentifier(m.getAlias());
            if (!aliases.add(m.getAlias())) {
                throw new IllegalArgumentException("Alias en double : " + m.getAlias());
            }
            // COUNT(*) does not require a column; all others do
            boolean isCountStar = "COUNT".equalsIgnoreCase(m.getFunction())
                    && ("*".equals(m.getColumn()) || m.getColumn() == null);
            if (!isCountStar) {
                if (m.getColumn() == null) {
                    throw new IllegalArgumentException("column est requis pour la fonction " + m.getFunction());
                }
                validateSqlIdentifier(m.getColumn());
                if (!visibleColumns.contains(m.getColumn())) {
                    throw new SecurityException("Colonne metric inaccessible ou inexistante : " + m.getColumn());
                }
            }
        }

        // ── Build SELECT clause ───────────────────────────────────────────────
        List<String> selectParts = new ArrayList<>(groupBy);
        for (AggregateMetric m : metrics) {
            boolean isCountStar = "COUNT".equalsIgnoreCase(m.getFunction())
                    && ("*".equals(m.getColumn()) || m.getColumn() == null);
            String expr = isCountStar
                    ? "COUNT(*)"
                    : m.getFunction().toUpperCase() + "(" + m.getColumn() + ")";
            selectParts.add(expr + " AS " + m.getAlias());
        }
        String selectClause = String.join(", ", selectParts);
        String groupByClause = " GROUP BY " + String.join(", ", groupBy);

        // ── Filters (WHERE) ───────────────────────────────────────────────────
        List<Object> filterParams = new ArrayList<>();
        String whereClause = buildWhereClause(request.getFilters(), filterParams, visibleColumns);

        // ── Sort ──────────────────────────────────────────────────────────────
        // sort column may be a groupBy column or an alias — both are validated above
        String orderClause = buildAggregateOrderClause(request.getSort(), groupBy, aliases);

        // ── Pagination ────────────────────────────────────────────────────────
        int safePage = Math.max(request.getPage(), 0);
        int safeSize = Math.max(1, Math.min(request.getSize(), 500));
        int offset    = safePage * safeSize;

        // Count total groups for pagination
        String countSql = "SELECT COUNT(*) FROM (SELECT " + String.join(", ", groupBy)
                + " FROM " + normalized + whereClause + groupByClause + ") AS _agg";
        Long total = filterParams.isEmpty()
                ? jdbcTemplate.queryForObject(countSql, Long.class)
                : jdbcTemplate.queryForObject(countSql, Long.class, filterParams.toArray());
        long totalRows = total != null ? total : 0L;

        // Data query
        List<Object> dataParams = new ArrayList<>(filterParams);
        dataParams.add(safeSize);
        dataParams.add(offset);

        String dataSql = "SELECT " + selectClause + " FROM " + normalized
                + whereClause + groupByClause + orderClause + " LIMIT ? OFFSET ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(dataSql, dataParams.toArray());

        List<String> outputColumns = new ArrayList<>(groupBy);
        outputColumns.addAll(aliases);

        return new TableResponseDto(normalized, safePage, safeSize, totalRows, outputColumns, rows);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    private String buildWhereClause(List<FilterCondition> filters, List<Object> params,
                                    List<String> visibleColumns) {
        if (filters == null || filters.isEmpty()) return "";

        List<String> conditions = new ArrayList<>();
        for (FilterCondition f : filters) {
            if (f.getColumn() == null || f.getOperator() == null) {
                throw new IllegalArgumentException("Filtre invalide : column et operator sont obligatoires");
            }
            validateSqlIdentifier(f.getColumn());

            if (!visibleColumns.contains(f.getColumn())) {
                throw new SecurityException("Colonne inaccessible ou inexistante : " + f.getColumn());
            }
            if (!ALLOWED_OPERATORS.contains(f.getOperator())) {
                throw new IllegalArgumentException("Opérateur invalide : " + f.getOperator());
            }

            if ("in".equals(f.getOperator())) {
                if (!(f.getValue() instanceof List)) {
                    throw new IllegalArgumentException("L'opérateur 'in' requiert une liste JSON");
                }
                List<?> values = (List<?>) f.getValue();
                if (values.isEmpty()) {
                    throw new IllegalArgumentException("Liste vide pour l'opérateur 'in'");
                }
                String placeholders = values.stream().map(v -> "?").collect(Collectors.joining(", "));
                conditions.add(f.getColumn() + " IN (" + placeholders + ")");
                params.addAll(values);
            } else {
                conditions.add(f.getColumn() + " " + toSqlOperator(f.getOperator()) + " ?");
                params.add(f.getValue());
            }
        }
        return " WHERE " + String.join(" AND ", conditions);
    }

    private String buildOrderClause(SortSpec sort, List<String> visibleColumns) {
        if (sort == null || sort.getColumn() == null || sort.getColumn().isBlank()) return "";
        validateSqlIdentifier(sort.getColumn());
        if (!visibleColumns.contains(sort.getColumn())) {
            throw new SecurityException("Colonne de tri inaccessible ou inexistante : " + sort.getColumn());
        }
        String direction = "desc".equalsIgnoreCase(sort.getDirection()) ? "DESC" : "ASC";
        return " ORDER BY " + sort.getColumn() + " " + direction;
    }

    private String buildAggregateOrderClause(SortSpec sort, List<String> groupBy,
                                              Set<String> aliases) {
        if (sort == null || sort.getColumn() == null || sort.getColumn().isBlank()) return "";
        validateSqlIdentifier(sort.getColumn());
        boolean validGroupByCol = groupBy.contains(sort.getColumn());
        boolean validAlias      = aliases.contains(sort.getColumn());
        if (!validGroupByCol && !validAlias) {
            throw new SecurityException(
                    "Colonne de tri invalide : doit être une colonne groupBy ou un alias metric : "
                            + sort.getColumn());
        }
        String direction = "desc".equalsIgnoreCase(sort.getDirection()) ? "DESC" : "ASC";
        return " ORDER BY " + sort.getColumn() + " " + direction;
    }

    private String toSqlOperator(String operator) {
        return switch (operator) {
            case "eq"   -> "=";
            case "neq"  -> "!=";
            case "gt"   -> ">";
            case "gte"  -> ">=";
            case "lt"   -> "<";
            case "lte"  -> "<=";
            case "like" -> "LIKE";
            default -> throw new IllegalArgumentException("Opérateur invalide : " + operator);
        };
    }

    private String extractRole(Authentication authentication) {
        // ── TESTING MODE: no auth required, default to ADMIN ──
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities() == null
                || authentication.getAuthorities().isEmpty()) {
            return "ADMIN";
        }

        // ── PRODUCTION: uncomment below, remove the return above ──
//        if (authentication == null || !authentication.isAuthenticated()
//                || authentication.getAuthorities() == null
//                || authentication.getAuthorities().isEmpty()) {
//            throw new SecurityException("Utilisateur non authentifié");
//        }

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
