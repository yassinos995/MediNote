package com.medinote.medinotebackend.dataagent.dto;

import java.util.List;
import java.util.Map;

public class TableResponseDto {

    private String table;
    private int page;
    private int size;
    private List<String> columns;
    private List<Map<String, Object>> rows;

    public TableResponseDto(String table, int page, int size, List<String> columns, List<Map<String, Object>> rows) {
        this.table = table;
        this.page = page;
        this.size = size;
        this.columns = columns;
        this.rows = rows;
    }

    public String getTable() {
        return table;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }
}