package com.medinote.medinotebackend.dataagent.dto;

import java.util.List;

public class AggregateRequest {

    /** Columns to group by. At least one is required. */
    private List<String> groupBy;

    /** Aggregate metrics to compute (SUM, COUNT, AVG, MIN, MAX). */
    private List<AggregateMetric> metrics;

    /** Optional pre-aggregation filters (WHERE clause). */
    private List<FilterCondition> filters;

    /** Optional sort — column must be a groupBy column or a metric alias. */
    private SortSpec sort;

    private int page = 0;
    private int size = 20;

    public AggregateRequest() {}

    public List<String>           getGroupBy()  { return groupBy; }
    public List<AggregateMetric>  getMetrics()  { return metrics; }
    public List<FilterCondition>  getFilters()  { return filters; }
    public SortSpec               getSort()     { return sort; }
    public int                    getPage()     { return page; }
    public int                    getSize()     { return size; }

    public void setGroupBy(List<String> groupBy)           { this.groupBy = groupBy; }
    public void setMetrics(List<AggregateMetric> metrics)  { this.metrics = metrics; }
    public void setFilters(List<FilterCondition> filters)  { this.filters = filters; }
    public void setSort(SortSpec sort)                     { this.sort = sort; }
    public void setPage(int page)                          { this.page = page; }
    public void setSize(int size)                          { this.size = size; }
}
