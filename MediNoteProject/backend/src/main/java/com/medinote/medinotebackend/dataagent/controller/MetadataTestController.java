package com.medinote.medinotebackend.dataagent.controller;

import com.medinote.medinotebackend.dataagent.dto.ColumnMetaDto;
import com.medinote.medinotebackend.dataagent.service.AccessPolicyService;
import com.medinote.medinotebackend.dataagent.service.MetadataService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/meta")
public class MetadataTestController {

    private static final Pattern SAFE_SQL_NAME = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final MetadataService metadataService;
    private final AccessPolicyService accessPolicyService;

    public MetadataTestController(MetadataService metadataService,
                                  AccessPolicyService accessPolicyService) {
        this.metadataService   = metadataService;
        this.accessPolicyService = accessPolicyService;
    }

    @GetMapping("/columns/{tableName}")
    public List<ColumnMetaDto> getColumns(@PathVariable String tableName,
                                          Authentication authentication) {
        // ── TESTING MODE: no auth required, default to ADMIN ──
        if (!SAFE_SQL_NAME.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Nom de table invalide : " + tableName);
        }

        String role = extractRole(authentication);

        if (!accessPolicyService.canReadTable(role, tableName.toLowerCase())) {
            throw new SecurityException("Accès refusé à la table : " + tableName);
        }

        // Return only columns the role is allowed to see
        List<ColumnMetaDto> all = metadataService.getTableColumnsWithTypes(tableName.toLowerCase());
        List<String> allowed    = accessPolicyService.getAllowedColumns(role, tableName.toLowerCase());
        List<String> excluded   = accessPolicyService.getExcludedColumns(tableName.toLowerCase());

        if (allowed.contains("*")) {
            return all.stream()
                    .filter(c -> !excluded.contains(c.getName()))
                    .toList();
        }
        return all.stream()
                .filter(c -> allowed.contains(c.getName()) && !excluded.contains(c.getName()))
                .toList();
    }

    private String extractRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities() == null
                || authentication.getAuthorities().isEmpty()) {
            throw new SecurityException("Utilisateur non authentifie");
        }
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "").toUpperCase())
                .orElseThrow(() -> new SecurityException("Aucun role trouve"));
    }
}
