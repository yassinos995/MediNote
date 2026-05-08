package com.medinote.medinotebackend.dataagent.controller;

import com.medinote.medinotebackend.dataagent.dto.AggregateRequest;
import com.medinote.medinotebackend.dataagent.dto.ColumnMetaDto;
import com.medinote.medinotebackend.dataagent.dto.QueryRequest;
import com.medinote.medinotebackend.dataagent.dto.TableResponseDto;
import com.medinote.medinotebackend.dataagent.model.DataModule;
import com.medinote.medinotebackend.dataagent.service.AccessPolicyService;
import com.medinote.medinotebackend.dataagent.service.DynamicQueryService;
import com.medinote.medinotebackend.dataagent.service.MetadataService;
import com.medinote.medinotebackend.dataagent.service.ModuleRegistryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
@CrossOrigin("*")
public class GenericDataController {

    private final DynamicQueryService    dynamicQueryService;
    private final AccessPolicyService    accessPolicyService;
    private final ModuleRegistryService  moduleRegistryService;
    private final MetadataService        metadataService;

    public GenericDataController(DynamicQueryService dynamicQueryService,
                                 AccessPolicyService accessPolicyService,
                                 ModuleRegistryService moduleRegistryService,
                                 MetadataService metadataService) {
        this.dynamicQueryService   = dynamicQueryService;
        this.accessPolicyService   = accessPolicyService;
        this.moduleRegistryService = moduleRegistryService;
        this.metadataService       = metadataService;
    }

    // ── Existing endpoints ────────────────────────────────────────────────────

    @GetMapping("/tables")
    public List<String> getAllowedTables(Authentication authentication) {
        return dynamicQueryService.getAllowedTables(authentication);
    }

    @GetMapping("/table/{tableName}")
    public TableResponseDto getTableData(@PathVariable String tableName,
                                         @RequestParam(defaultValue = "0")  int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         Authentication authentication) {
        return dynamicQueryService.readTable(tableName, page, size, authentication);
    }

    @GetMapping("/modules")
    public Map<DataModule, List<String>> getModules(Authentication authentication) {
        String role = extractRole(authentication);
        return accessPolicyService.getAllowedModulesWithTables(role);
    }

    @GetMapping("/module/{moduleName}/tables")
    public List<String> getTablesByModule(@PathVariable String moduleName,
                                          Authentication authentication) {
        String role   = extractRole(authentication);
        DataModule module = DataModule.valueOf(moduleName.toUpperCase());

        if (!accessPolicyService.canAccessModule(role, module)) {
            throw new SecurityException("Accès refusé au module : " + module);
        }
        return moduleRegistryService.getTablesForModule(module).stream()
                .filter(table -> accessPolicyService.canReadTable(role, table))
                .sorted()
                .toList();
    }

    // ── New: filtered / sorted query ──────────────────────────────────────────

    /**
     * POST /api/data/query/{tableName}
     *
     * Body example:
     * {
     *   "filters": [
     *     { "column": "dlg",  "operator": "eq",  "value": "ABC" },
     *     { "column": "date", "operator": "gte", "value": "2024-01-01" },
     *     { "column": "zone", "operator": "in",  "value": ["Z1","Z2"] }
     *   ],
     *   "sort": { "column": "date", "direction": "desc" },
     *   "page": 0,
     *   "size": 50
     * }
     */
    @PostMapping("/query/{tableName}")
    public TableResponseDto queryTable(@PathVariable String tableName,
                                       @RequestBody QueryRequest request,
                                       Authentication authentication) {
        return dynamicQueryService.queryTable(tableName, request, authentication);
    }

    // ── New: aggregation ──────────────────────────────────────────────────────

    /**
     * POST /api/data/aggregate/{tableName}
     *
     * Body example:
     * {
     *   "groupBy": ["dlg", "zone"],
     *   "metrics": [
     *     { "column": "ttc", "function": "SUM",   "alias": "total_ttc" },
     *     { "column": "qte", "function": "SUM",   "alias": "total_qte" },
     *     { "column": "*",   "function": "COUNT", "alias": "nb_lignes" }
     *   ],
     *   "filters": [
     *     { "column": "date", "operator": "gte", "value": "2024-01-01" }
     *   ],
     *   "sort": { "column": "total_ttc", "direction": "desc" },
     *   "page": 0,
     *   "size": 50
     * }
     */
    @PostMapping("/aggregate/{tableName}")
    public TableResponseDto aggregateTable(@PathVariable String tableName,
                                           @RequestBody AggregateRequest request,
                                           Authentication authentication) {
        return dynamicQueryService.aggregateTable(tableName, request, authentication);
    }

    // ── New: full schema discovery ────────────────────────────────────────────

    /**
     * GET /api/data/schema
     *
     * Returns all modules → tables → columns (with types) visible to the caller's role.
     * Designed for the AI agent to discover the full data model in a single call.
     */
    @GetMapping("/schema")
    public Map<String, Map<String, List<ColumnMetaDto>>> getSchema(Authentication authentication) {
        String role = extractRole(authentication);
        Map<DataModule, List<String>> modulesWithTables =
                accessPolicyService.getAllowedModulesWithTables(role);

        Map<String, Map<String, List<ColumnMetaDto>>> schema = new LinkedHashMap<>();

        for (Map.Entry<DataModule, List<String>> moduleEntry : modulesWithTables.entrySet()) {
            Map<String, List<ColumnMetaDto>> tableSchemas = new LinkedHashMap<>();

            for (String table : moduleEntry.getValue()) {
                List<ColumnMetaDto> allColumns  = metadataService.getTableColumnsWithTypes(table);
                List<String> allowedColumns     = accessPolicyService.getAllowedColumns(role, table);
                List<String> excludedColumns    = accessPolicyService.getExcludedColumns(table);

                List<ColumnMetaDto> visible = allColumns.stream()
                        .filter(c -> allowedColumns.contains("*") || allowedColumns.contains(c.getName()))
                        .filter(c -> !excludedColumns.contains(c.getName()))
                        .toList();

                tableSchemas.put(table, visible);
            }
            schema.put(moduleEntry.getKey().name(), tableSchemas);
        }
        return schema;
    }

    // ── Exception handlers ────────────────────────────────────────────────────

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurity(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleOther(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", ex.getMessage()));
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private String extractRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null
                || authentication.getAuthorities().isEmpty()) {
            throw new SecurityException("Utilisateur non authentifié");
        }
        return authentication.getAuthorities().stream()
                .findFirst()
                .orElseThrow(() -> new SecurityException("Aucun rôle trouvé"))
                .getAuthority()
                .replace("ROLE_", "")
                .toUpperCase();
    }
}
