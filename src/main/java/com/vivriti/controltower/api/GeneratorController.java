package com.vivriti.controltower.api;

import com.vivriti.controltower.generator.SyntheticDataGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.Map;

/**
 * REST controller for triggering synthetic data generation and database reset.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Generator", description = "Synthetic data generation for testing and evaluation")
public class GeneratorController {

    private final SyntheticDataGenerator generator;
    private final JdbcTemplate jdbcTemplate;

    public GeneratorController(SyntheticDataGenerator generator, JdbcTemplate jdbcTemplate) {
        this.generator = generator;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate synthetic feed data",
               description = "Generates seeded synthetic data across 3 partner formats with configurable anomaly injection")
    public ResponseEntity<Map<String, Object>> generate(
            @RequestParam(defaultValue = "42") long seed,
            @RequestParam(defaultValue = "2000") int loans,
            @RequestParam(defaultValue = "./data/generated") String outputDir) {

        try {
            generator.generate(seed, loans, Path.of(outputDir));
            return ResponseEntity.ok(Map.of(
                    "status", "GENERATED",
                    "seed", seed,
                    "loanCount", loans,
                    "outputDir", outputDir,
                    "message", "Feeds and ground truth generated successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage() != null ? e.getMessage() : "Generation failed"
            ));
        }
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset database tables for clean test/demo runs",
               description = "Truncates all transactional tables (match decisions, exceptions, close summaries, audit logs, canonical events, raw records, batches)")
    public ResponseEntity<Map<String, String>> resetDatabase() {
        try {
            jdbcTemplate.execute("TRUNCATE TABLE match_decision, reconciliation_exception, close_summary, audit_log, canonical_event, raw_source_record, source_batch CASCADE;");
            return ResponseEntity.ok(Map.of("status", "RESET", "message", "All transactional tables cleared successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
}
