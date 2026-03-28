package com.medinote.medinotebackend.dataagent.dto;

public class SortSpec {

    private String column;
    private String direction = "asc";

    public String getColumn()                 { return column; }
    public void setColumn(String column)      { this.column = column; }

    public String getDirection()                  { return direction; }
    public void setDirection(String direction)    { this.direction = direction; }
}
