package com.medinote.medinotebackend.dataagent.controller;

import com.medinote.medinotebackend.dataagent.dto.TableResponseDto;
import com.medinote.medinotebackend.dataagent.model.DataModule;
import com.medinote.medinotebackend.dataagent.service.AccessPolicyService;
import com.medinote.medinotebackend.dataagent.service.DynamicQueryService;
import com.medinote.medinotebackend.dataagent.service.ModuleRegistryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
@CrossOrigin("*")
public class GenericDataController {

    private final DynamicQueryService dynamicQueryService;
    private final AccessPolicyService accessPolicyService;
    private final ModuleRegistryService moduleRegistryService;

    public GenericDataController(DynamicQueryService dynamicQueryService,
                                 AccessPolicyService accessPolicyService,
                                 ModuleRegistryService moduleRegistryService) {
        this.dynamicQueryService = dynamicQueryService;
        this.accessPolicyService = accessPolicyService;
        this.moduleRegistryService = moduleRegistryService;
    }

    @GetMapping("/tables")
    public List<String> getAllowedTables(Authentication authentication) {
        return dynamicQueryService.getAllowedTables(authentication);
    }

    @GetMapping("/table/{tableName}")
    public TableResponseDto getTableData(@PathVariable String tableName,
                                         @RequestParam(defaultValue = "0") int page,
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
        String role = extractRole(authentication);
        DataModule module = DataModule.valueOf(moduleName.toUpperCase());

        if (!accessPolicyService.canAccessModule(role, module)) {
            throw new SecurityException("Accès refusé au module : " + module);
        }

        return moduleRegistryService.getTablesForModule(module).stream()
                .filter(table -> accessPolicyService.canReadTable(role, table))
                .sorted()
                .toList();
    }

    private String extractRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null || authentication.getAuthorities().isEmpty()) {
            throw new SecurityException("Utilisateur non authentifié");
        }

        return authentication.getAuthorities()
                .stream()
                .findFirst()
                .orElseThrow(() -> new SecurityException("Aucun rôle trouvé"))
                .getAuthority()
                .replace("ROLE_", "")
                .toUpperCase();
    }

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
}