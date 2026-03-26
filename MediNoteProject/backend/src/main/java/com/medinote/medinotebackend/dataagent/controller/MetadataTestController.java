package com.medinote.medinotebackend.dataagent.controller;

import com.medinote.medinotebackend.dataagent.service.MetadataService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meta")
public class MetadataTestController {

    private final MetadataService metadataService;

    public MetadataTestController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/columns/{tableName}")
    public List<String> getColumns(@PathVariable String tableName) {
        return metadataService.getTableColumns(tableName);
    }
}