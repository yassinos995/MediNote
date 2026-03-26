package com.medinote.medinotebackend.dataagent.service;
import com.medinote.medinotebackend.dataagent.dto.TableResponseDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class DynamicQueryService {

    private static final Pattern SAFE_SQL_NAME = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final JdbcTemplate jdbcTemplate;
    private final MetadataService metadataService;
    private final com.medinote.medinotebackend.dataagent.service.AccessPolicyService accessPolicyService;

    public DynamicQueryService(JdbcTemplate jdbcTemplate,
                               MetadataService metadataService,
                               com.medinote.medinotebackend.dataagent.service.AccessPolicyService accessPolicyService) {
        this.jdbcTemplate = jdbcTemplate;
        this.metadataService = metadataService;
        this.accessPolicyService = accessPolicyService;
    }

    public List<String> getAllowedTables(Authentication authentication) {
        String role = extractRole(authentication);

        if (accessPolicyService.hasFullAccess(role)) {
            return metadataService.getAllTables();
        }

        return metadataService.getAllTables().stream()
                .filter(table -> accessPolicyService.canReadTable(role, table))
                .sorted()
                .toList();
    }

    public TableResponseDto readTable(String tableName, int page, int size, Authentication authentication) {
        validateSqlIdentifier(tableName);

        String normalizedTableName = tableName.toLowerCase();
        String role = extractRole(authentication);

        if (!metadataService.tableExists(normalizedTableName)) {
            throw new IllegalArgumentException("Table inexistante : " + normalizedTableName);
        }

        if (!accessPolicyService.canReadTable(role, normalizedTableName)) {
            throw new SecurityException("Accès refusé à la table : " + normalizedTableName);
        }

        List<String> realColumns = metadataService.getTableColumns(normalizedTableName);
        List<String> allowedColumns = accessPolicyService.getAllowedColumns(role, normalizedTableName);

        List<String> selectedColumns;
        if (allowedColumns.contains("*")) {
            selectedColumns = realColumns;
        } else {
            selectedColumns = realColumns.stream()
                    .filter(allowedColumns::contains)
                    .toList();
        }

        if (selectedColumns.isEmpty()) {
            throw new SecurityException("Aucune colonne autorisée pour la table : " + normalizedTableName);
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        int offset = safePage * safeSize;

        String sql = "SELECT " + String.join(", ", selectedColumns) +
                " FROM " + normalizedTableName +
                " LIMIT ? OFFSET ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, safeSize, offset);

        return new TableResponseDto(normalizedTableName, safePage, safeSize, selectedColumns, rows);
    }

    private String extractRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null || authentication.getAuthorities().isEmpty()) {
            return "ADMIN";
        }

        return authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", "").toUpperCase())
                .orElse("ADMIN");
    }

    private void validateSqlIdentifier(String value) {
        if (value == null || !SAFE_SQL_NAME.matcher(value).matches()) {
            throw new IllegalArgumentException("Nom SQL invalide : " + value);
        }
    }
}