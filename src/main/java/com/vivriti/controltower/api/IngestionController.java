package com.vivriti.controltower.api;

import com.vivriti.controltower.ingestion.IngestionResult;
import com.vivriti.controltower.ingestion.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.Map;

/**
 * REST controller for feed ingestion operations.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Ingestion", description = "Feed ingestion, validation, and quarantine operations")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    @Operation(summary = "Ingest feed data from directory",
               description = "Scans a data directory for partner feed files, parses, validates, hashes, and persists them. Invalid records are quarantined.")
    public ResponseEntity<IngestionResult> ingest(
            @RequestParam(defaultValue = "./data/generated") String dataDir) {

        IngestionResult result = ingestionService.ingestDirectory(Path.of(dataDir));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/ingest/file")
    @Operation(summary = "Ingest a single feed file",
               description = "Ingests a single partner feed file by path, partner code, and source system")
    public ResponseEntity<IngestionResult> ingestFile(
            @RequestParam String filePath,
            @RequestParam String partnerCode,
            @RequestParam String sourceSystem) {

        IngestionResult result = ingestionService.ingestFile(
                Path.of(filePath), partnerCode, sourceSystem);
        return ResponseEntity.ok(result);
    }
}
