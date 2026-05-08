package com.medinote.medinotebackend.dataagent.dto;

import java.util.List;

/**
 * Request body for POST /api/data/aggregate/{tableName}
 *
 * Full example:
 * {
 *   "groupBy": ["zone"],
 *   "groupByExpressions": [
 *     { "column": "date", "function": "MONTH", "alias": "mois" }
 *   ],
 *   "metrics": [
 *     { "column": "ttc",  "function": "SUM",   "alias": "ca_total" },
 *     { "column": "qte",  "function": "SUM",   "alias": "qte_total" },
 *     { "column": "cl",   "function": "COUNT", "alias": "nb_clients", "distinct": true }
 *   ],
 *   "filters": [
 *     { "column": "date", "operator": "between", "value": ["2023-01-01","2023-12-31"] }
 *   ],
 *   "having": [
 *     { "column": "ca_total", "operator": "gte", "value": 10000 }
 *   ],
 *   "sorts": [
 *     { "column": "mois",     "direction": "asc" },
 *     { "column": "ca_total", "direction": "desc" }
 *   ],
 *   "page": 0,
 *   "size": 50
 * }
 */
public class AggregateRequest {

    /**
     * Raw columns to group by. At least one of groupBy or groupByExpressions is required.
     * Example: ["zone", "dlg"]
     */
    private List<String> groupBy;

    /**
     * Computed GROUP BY expressions using date functions.
     * Use this to group by YEAR, MONTH, WEEK, DAY, QUARTER, or DATE of a column.
     * Example: [{ "column": "date", "function": "YEAR", "alias": "annee" }]
     */
    private List<GroupBySpec> groupByExpressions;

    /** Aggregate metrics to compute (SUM, COUNT, AVG, MIN, MAX). At least one required. */
    private List<AggregateMetric> metrics;

    /**
     * Optional pre-aggregation filters applied as WHERE clause.
     * Supported operators: eq, neq, gt, gte, lt, lte, between, in, notIn,
     *                      like, notLike, startsWith, endsWith, contains,
     *                      isNull, isNotNull
     */
    private List<FilterCondition> filters;

    /**
     * Optional post-aggregation filters applied as HAVING clause.
     * Column must reference a metric alias, a raw groupBy column, or a computed groupBy alias.
     * Same operators as filters.
     * Example: [{ "column": "ca_total", "operator": "gte", "value": 50000 }]
     */
    private List<FilterCondition> having;

    /**
     * Multi-column sort. Each column must be a groupBy column, computed alias, or metric alias.
     * Takes precedence over the legacy single-sort field.
     * Example: [{ "column": "annee", "direction": "asc" }, { "column": "ca_total", "direction": "desc" }]
     */
    private List<SortSpec> sorts;

    /**
     * Legacy single-sort field — kept for backward compatibility.
     * Ignored when sorts is set.
     */
    private SortSpec sort;

    private int page = 0;
    private int size = 20;

    public AggregateRequest() {}

    public List<String>           getGroupBy()             { return groupBy; }
    public List<GroupBySpec>      getGroupByExpressions()  { return groupByExpressions; }
    public List<AggregateMetric>  getMetrics()             { return metrics; }
    public List<FilterCondition>  getFilters()             { return filters; }
    public List<FilterCondition>  getHaving()              { return having; }
    public List<SortSpec>         getSorts()               { return sorts; }
    public SortSpec               getSort()                { return sort; }
    public int                    getPage()                { return page; }
    public int                    getSize()                { return size; }

    public void setGroupBy(List<String> groupBy)                      { this.groupBy = groupBy; }
    public void setGroupByExpressions(List<GroupBySpec> exprs)        { this.groupByExpressions = exprs; }
    public void setMetrics(List<AggregateMetric> metrics)             { this.metrics = metrics; }
    public void setFilters(List<FilterCondition> filters)             { this.filters = filters; }
    public void setHaving(List<FilterCondition> having)               { this.having = having; }
    public void setSorts(List<SortSpec> sorts)                        { this.sorts = sorts; }
    public void setSort(SortSpec sort)                                { this.sort = sort; }
    public void setPage(int page)                                     { this.page = page; }
    public void setSize(int size)                                     { this.size = size; }
}
