package com.medinote.medinotebackend.dataagent.dto;

/**
 * A single aggregate metric to compute.
 *
 * Examples:
 *   { "column": "ttc", "function": "SUM",   "alias": "total_ttc" }
 *   { "column": "ttc", "function": "AVG",   "alias": "avg_ttc" }
 *   { "column": "*",   "function": "COUNT", "alias": "nb_lignes" }
 *   { "column": "cl",  "function": "COUNT", "alias": "clients_uniques", "distinct": true }
 *
 * distinct=true generates COUNT(DISTINCT col) or SUM(DISTINCT col).
 * distinct cannot be combined with COUNT(*).
 */
public class AggregateMetric {

    /** Column to aggregate. Use "*" or null only with COUNT. */
    private String column;

    /** Aggregate function: SUM, COUNT, AVG, MIN, MAX */
    private String function;

    /** Output alias — becomes the column name in the response rows. */
    private String alias;

    /**
     * When true, applies DISTINCT inside the aggregate function.
     * Example: COUNT(DISTINCT cl) counts unique clients.
     * Cannot be combined with COUNT(*).
     * Default: false.
     */
    private boolean distinct = false;

    public AggregateMetric() {}

    public String  getColumn()   { return column; }
    public String  getFunction() { return function; }
    public String  getAlias()    { return alias; }
    public boolean isDistinct()  { return distinct; }

    public void setColumn(String column)      { this.column = column; }
    public void setFunction(String function)  { this.function = function; }
    public void setAlias(String alias)        { this.alias = alias; }
    public void setDistinct(boolean distinct) { this.distinct = distinct; }
}
