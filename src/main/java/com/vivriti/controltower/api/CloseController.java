package com.vivriti.controltower.api;

import com.vivriti.controltower.canonical.CloseSummaryEntity;
import com.vivriti.controltower.canonical.CloseSummaryRepository;
import com.vivriti.controltower.close.CloseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * REST controller for close/hold decision management.
 * Operators evaluate close; approvers approve (with maker-checker enforcement).
 */
@RestController
@RequestMapping("/api/v1/close")
@Tag(name = "Close Control", description = "Batch close/hold decisions with control equation verification")
public class CloseController {

    private final CloseService closeService;
    private final CloseSummaryRepository closeSummaryRepository;
    private final com.vivriti.controltower.canonical.SourceBatchRepository sourceBatchRepository;

    public CloseController(CloseService closeService,
                           CloseSummaryRepository closeSummaryRepository,
                           com.vivriti.controltower.canonical.SourceBatchRepository sourceBatchRepository) {
        this.closeService = closeService;
        this.closeSummaryRepository = closeSummaryRepository;
        this.sourceBatchRepository = sourceBatchRepository;
    }

    @GetMapping("/batches")
    @Operation(summary = "List all source batches", description = "Returns all batches with declared amounts and status")
    public ResponseEntity<List<com.vivriti.controltower.canonical.SourceBatchEntity>> listBatches() {
        return ResponseEntity.ok(sourceBatchRepository.findAll());
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate close decision for a batch",
               description = "Runs the control equation engine on a batch to determine CLOSE or HOLD. " +
                             "If HOLD, returns the list of blocking records with exposure amounts.")
    public ResponseEntity<CloseSummaryEntity> evaluate(
            @RequestParam String batchId,
            Principal principal) {

        CloseSummaryEntity summary = closeService.evaluateClose(batchId, principal.getName());
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/approve")
    @Operation(summary = "Approve a close decision",
               description = "Approver confirms a CLOSE decision. Enforces maker-checker: " +
                             "if the approver overrode any exception in this batch, returns 403.")
    public ResponseEntity<?> approve(
            @RequestParam String closeId,
            Principal principal) {

        try {
            CloseSummaryEntity approved = closeService.approveClose(closeId, principal.getName());
            return ResponseEntity.ok(approved);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error", "SEGREGATION_VIOLATION",
                            "message", e.getMessage()
                    ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "INVALID_STATE",
                            "message", e.getMessage()
                    ));
        }
    }

    @GetMapping
    @Operation(summary = "List all close summaries", description = "Returns all close/hold decisions")
    public ResponseEntity<List<CloseSummaryEntity>> listAll() {
        return ResponseEntity.ok(closeSummaryRepository.findAll());
    }

    @GetMapping("/{closeId}")
    @Operation(summary = "Get close summary by ID")
    public ResponseEntity<CloseSummaryEntity> getById(@PathVariable String closeId) {
        return closeSummaryRepository.findById(closeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
