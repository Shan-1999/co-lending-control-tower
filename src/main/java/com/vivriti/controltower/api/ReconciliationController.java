package com.vivriti.controltower.api;

import com.vivriti.controltower.canonical.MatchDecisionEntity;
import com.vivriti.controltower.canonical.MatchDecisionRepository;
import com.vivriti.controltower.reconciliation.ReconciliationService;
import com.vivriti.controltower.reconciliation.ReconciliationSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for reconciliation operations and match decision queries.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reconciliation", description = "Reconciliation engine execution and match decision queries")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;
    private final MatchDecisionRepository matchDecisionRepository;

    public ReconciliationController(ReconciliationService reconciliationService,
                                     MatchDecisionRepository matchDecisionRepository) {
        this.reconciliationService = reconciliationService;
        this.matchDecisionRepository = matchDecisionRepository;
    }

    @PostMapping("/reconcile")
    @Operation(summary = "Run reconciliation engine",
               description = "Executes the full reconciliation pipeline across all ingested canonical events using the 5-level matcher hierarchy")
    public ResponseEntity<ReconciliationSummary> reconcile() {
        ReconciliationSummary summary = reconciliationService.reconcileAll();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/match-decisions")
    @Operation(summary = "List all match decisions",
               description = "Returns all match decisions, optionally filtered by match level")
    public ResponseEntity<List<MatchDecisionEntity>> getMatchDecisions(
            @RequestParam(required = false) String matchLevel) {
        List<MatchDecisionEntity> decisions;
        if (matchLevel != null && !matchLevel.isBlank()) {
            decisions = matchDecisionRepository.findByMatchLevel(matchLevel);
        } else {
            decisions = matchDecisionRepository.findAll();
        }
        return ResponseEntity.ok(decisions);
    }

    @GetMapping("/match-decisions/{businessEventId}")
    @Operation(summary = "Get match decisions for a business event",
               description = "Returns all match decisions associated with a specific business event ID")
    public ResponseEntity<List<MatchDecisionEntity>> getMatchDecisionsByEvent(
            @PathVariable String businessEventId) {
        List<MatchDecisionEntity> decisions =
                matchDecisionRepository.findByBusinessEventId(businessEventId);
        return ResponseEntity.ok(decisions);
    }

    @GetMapping("/match-decisions/loan/{loanReference}")
    @Operation(summary = "Get match decisions for a loan reference",
               description = "Returns reconciliation summary for a specific loan by tracing through canonical events")
    public ResponseEntity<ReconciliationSummary> getMatchDecisionsByLoan(
            @PathVariable String loanReference) {
        ReconciliationSummary summary = reconciliationService.reconcileLoan(loanReference);
        return ResponseEntity.ok(summary);
    }
}
