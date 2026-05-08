package com.medinote.medinotebackend.dataagent.dto;

import java.util.List;
import java.util.Map;

public class TableResponseDto {

    private String table;
    private int page;
    private int size;
    private long totalRows;
    private int totalPages;
    private List<String> columns;
    private List<Map<String, Object>> rows;

    public TableResponseDto(String table, int page, int size, long totalRows,
                            List<String> columns, List<Map<String, Object>> rows) {
        this.table = table;
        this.page = page;
        this.size = size;
        this.totalRows = totalRows;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalRows / size) : 0;
        this.columns = columns;
        this.rows = rows;
    }

    public String getTable()                        { return table; }
    public int getPage()                            { return page; }
    public int getSize()                            { return size; }
    public long getTotalRows()                      { return totalRows; }
    public int getTotalPages()                      { return totalPages; }
    public List<String> getColumns()                { return columns; }
    public List<Map<String, Object>> getRows()      { return rows; }
}
