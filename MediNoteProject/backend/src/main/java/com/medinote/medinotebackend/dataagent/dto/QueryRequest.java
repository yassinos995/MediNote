package com.medinote.medinotebackend.dataagent.dto;

import java.util.List;

/**
 * Request body for POST /api/data/query/{tableName}
 *
 * Example:
 * {
 *   "filters": [
 *     { "column": "dlg",  "operator": "eq",  "value": "ABC" },
 *     { "column": "date", "operator": "gte", "value": "2024-01-01" },
 *     { "column": "date", "operator": "lte", "value": "2024-03-31" },
 *     { "column": "zone", "operator": "in",  "value": ["Z1","Z2"] }
 *   ],
 *   "sort":    { "column": "date", "direction": "desc" },
 *   "page":    0,
 *   "size":    50
 * }
 */
public class QueryRequest {

    private List<FilterCondition> filters;
    private SortSpec sort;
    private int page = 0;
    private int size = 20;

    public List<FilterCondition> getFilters()             { return filters; }
    public void setFilters(List<FilterCondition> filters) { this.filters = filters; }

    public SortSpec getSort()              { return sort; }
    public void setSort(SortSpec sort)     { this.sort = sort; }

    public int getPage()           { return page; }
    public void setPage(int page)  { this.page = page; }

    public int getSize()           { return size; }
    public void setSize(int size)  { this.size = size; }
}
