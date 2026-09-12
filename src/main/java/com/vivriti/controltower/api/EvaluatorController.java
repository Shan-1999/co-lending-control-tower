package com.vivriti.controltower.api;

import com.vivriti.controltower.reconciliation.EvaluationReporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Evaluator controller that compares reconciliation decisions against ground truth.
 * Delegates to {@link EvaluationReporter} to generate Section A (Phase 1 Gate)
 * and Section B (Phase 2 Intelligence) scorecards.
 *
 * <p>CRITICAL: This controller reads ground truth ONLY for post-hoc evaluation.
 * The core reconciliation engine itself NEVER imports or accesses ground truth.</p>
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Evaluator", description = "Post-hoc evaluation of reconciliation accuracy against ground truth")
public class EvaluatorController {

    private final EvaluationReporter evaluationReporter;

    public EvaluatorController(EvaluationReporter evaluationReporter) {
        this.evaluationReporter = evaluationReporter;
    }

    @RequestMapping(value = "/evaluate", method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "Evaluate reconciliation against ground truth",
               description = "Compares reconciliation decisions with the isolated ground truth file to compute " +
                             "Phase 1 Gate metrics (0 false matches), STP %, control totals, and Phase 2 Intelligence metrics.")
    public ResponseEntity<Map<String, Object>> evaluate(
            @RequestParam(defaultValue = "./data/generated") String dataDir,
            @RequestParam(defaultValue = "./data/generated/ground_truth/ground_truth.json") String groundTruthPath) {

        try {
            Path gtPath = Path.of(groundTruthPath);
            if (!Files.exists(gtPath)) {
                // Try default location relative to data dir
                gtPath = Path.of(dataDir, "ground_truth", "ground_truth.json");
            }
            if (!Files.exists(gtPath)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Ground truth file not found",
                        "searchedPaths", List.of(groundTruthPath,
                                Path.of(dataDir, "ground_truth", "ground_truth.json").toString())
                ));
            }

            Map<String, Object> scorecard = evaluationReporter.evaluate(gtPath);
            return ResponseEntity.ok(scorecard);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to read ground truth",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Evaluation failed",
                    "message", e.getMessage()
            ));
        }
    }
}
