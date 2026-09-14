package com.vivriti.controltower.api;

import com.vivriti.controltower.generator.SyntheticDataGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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

            String qualityReport = "";
            try {
                Path reportPath = Path.of(outputDir, "data_quality_report.md");
                if (Files.exists(reportPath)) {
                    qualityReport = Files.readString(reportPath);
                }
            } catch (Exception ignored) {}

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "GENERATED");
            resp.put("seed", seed);
            resp.put("loanCount", loans);
            resp.put("outputDir", outputDir);
            resp.put("message", "Feeds and ground truth generated successfully");
            resp.put("qualityReport", qualityReport);

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage() != null ? e.getMessage() : "Generation failed"
            ));
        }
    }

    @GetMapping("/quality-report")
    @Operation(summary = "Get synthetic data quality report",
               description = "Retrieves the markdown content of the generated data quality report")
    public ResponseEntity<Map<String, Object>> getQualityReport(
            @RequestParam(required = false, defaultValue = "./data/seed_clean") String outputDir) {

        Path path = Path.of(outputDir, "data_quality_report.md");
        if (!Files.exists(path)) {
            if (Files.exists(Path.of("./data/generated/data_quality_report.md"))) {
                path = Path.of("./data/generated/data_quality_report.md");
            } else if (Files.exists(Path.of("./data/seed_a/data_quality_report.md"))) {
                path = Path.of("./data/seed_a/data_quality_report.md");
            }
        }

        if (!Files.exists(path)) {
            return ResponseEntity.ok(Map.of(
                    "status", "NOT_FOUND",
                    "message", "No quality report found. Generate feeds first."
            ));
        }

        try {
            String content = Files.readString(path);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "path", path.toString(),
                    "report", content
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERROR",
                    "message", "Failed to read report: " + e.getMessage()
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
