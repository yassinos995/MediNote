package com.medinote.medinotebackend.dataagent.dto;

/**
 * A computed GROUP BY expression — applies a whitelisted date function to a column.
 *
 * Example — group by year:
 *   { "column": "date", "function": "YEAR", "alias": "yr" }
 *   → SELECT YEAR(date) AS yr … GROUP BY YEAR(date)
 *
 * Supported functions: YEAR, MONTH, WEEK, DAY, QUARTER, DATE
 *
 * Plain column grouping (no function) belongs in the regular groupBy list.
 * The alias becomes the output column name and can be referenced in HAVING and sort.
 */
public class GroupBySpec {

    /** Raw column name — must exist and be visible for the caller's role. */
    private String column;

    /**
     * Date extraction function to apply.
     * Allowed values: YEAR, MONTH, WEEK, DAY, QUARTER, DATE
     */
    private String function;

    /**
     * Output alias — becomes the column name in the response rows.
     * Must be a valid SQL identifier and must be unique across all output columns.
     */
    private String alias;

    public GroupBySpec() {}

    public String getColumn()   { return column; }
    public String getFunction() { return function; }
    public String getAlias()    { return alias; }

    public void setColumn(String column)     { this.column = column; }
    public void setFunction(String function) { this.function = function; }
    public void setAlias(String alias)       { this.alias = alias; }
}
