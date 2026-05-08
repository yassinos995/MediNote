package com.medinote.medinotebackend.dataagent.service;

import com.medinote.medinotebackend.dataagent.dto.ColumnMetaDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MetadataService {

    private final JdbcTemplate jdbcTemplate;

    public MetadataService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean tableExists(String tableName) {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND table_type = 'BASE TABLE'
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
        return count != null && count > 0;
    }

    public List<String> getTableColumns(String tableName) {
        String sql = """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                ORDER BY ordinal_position
                """;
        return jdbcTemplate.queryForList(sql, String.class, tableName);
    }

    public List<ColumnMetaDto> getTableColumnsWithTypes(String tableName) {
        String sql = """
                SELECT column_name, column_type, is_nullable, column_key
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                ORDER BY ordinal_position
                """;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new ColumnMetaDto(
                        rs.getString("column_name"),
                        rs.getString("column_type"),
                        "YES".equals(rs.getString("is_nullable")),
                        rs.getString("column_key")
                ), tableName);
    }

    public List<String> getAllTables() {
        String sql = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """;
        return jdbcTemplate.queryForList(sql, String.class);
    }
}
