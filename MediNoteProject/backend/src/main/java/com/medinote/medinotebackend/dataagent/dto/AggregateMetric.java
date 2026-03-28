package com.medinote.medinotebackend.dataagent.dto;

public class AggregateMetric {

    /** Column to aggregate. Use "*" only with COUNT. */
    private String column;

    /** Aggregate function: SUM, COUNT, AVG, MIN, MAX */
    private String function;

    /** Output alias — becomes the column name in the response rows. */
    private String alias;

    public AggregateMetric() {}

    public String getColumn()   { return column; }
    public String getFunction() { return function; }
    public String getAlias()    { return alias; }

    public void setColumn(String column)     { this.column = column; }
    public void setFunction(String function) { this.function = function; }
    public void setAlias(String alias)       { this.alias = alias; }
}
