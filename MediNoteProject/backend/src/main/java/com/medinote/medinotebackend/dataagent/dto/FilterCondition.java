package com.medinote.medinotebackend.dataagent.dto;

/**
 * A single filter condition for a query.
 * Supported operators: eq, neq, gt, gte, lt, lte, like, in
 * For "in", value must be a JSON array e.g. ["A","B","C"]
 */
public class FilterCondition {

    private String column;
    private String operator;
    private Object value;

    public String getColumn()            { return column; }
    public void setColumn(String column) { this.column = column; }

    public String getOperator()               { return operator; }
    public void setOperator(String operator)  { this.operator = operator; }

    public Object getValue()             { return value; }
    public void setValue(Object value)   { this.value = value; }
}
