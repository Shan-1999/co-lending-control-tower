package com.vivriti.controltower.api;

import com.vivriti.controltower.canonical.ReconciliationExceptionEntity;
import com.vivriti.controltower.exception.ExceptionService;
import com.vivriti.controltower.exception.RootCauseClusterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * REST controller for exception queue management.
 * Operators can review, update status, and log overrides.
 */
@RestController
@RequestMapping("/api/v1/exceptions")
@Tag(name = "Exception Queue", description = "Exception management, override logging, and status tracking")
public class ExceptionController {

    private final ExceptionService exceptionService;
    private final RootCauseClusterService clusterService;

    public ExceptionController(ExceptionService exceptionService, RootCauseClusterService clusterService) {
        this.exceptionService = exceptionService;
        this.clusterService = clusterService;
    }

    @GetMapping
    @Operation(summary = "List exceptions", description = "Returns all exceptions, optionally filtered by status, priority, or partner code")
    public ResponseEntity<List<ReconciliationExceptionEntity>> listExceptions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String partnerCode) {

        List<ReconciliationExceptionEntity> exceptions;
        if (status != null && !status.isBlank()) {
            exceptions = exceptionService.findByStatus(status);
        } else if (priority != null && !priority.isBlank()) {
            exceptions = exceptionService.findByPriority(priority);
        } else if (partnerCode != null && !partnerCode.isBlank()) {
            exceptions = exceptionService.findByPartnerCode(partnerCode);
        } else {
            exceptions = exceptionService.findAll();
        }
        return ResponseEntity.ok(exceptions);
    }

    @GetMapping("/{exceptionId}")
    @Operation(summary = "Get exception by ID", description = "Returns a single exception record by its ID")
    public ResponseEntity<ReconciliationExceptionEntity> getException(
            @PathVariable String exceptionId) {
        return exceptionService.findById(exceptionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @RequestMapping(value = "/{exceptionId}/status", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Update exception status",
               description = "Transitions exception status (OPEN → IN_PROGRESS → RESOLVED). Uses optimistic locking.")
    public ResponseEntity<ReconciliationExceptionEntity> updateStatus(
            @PathVariable String exceptionId,
            @RequestParam String newStatus,
            Principal principal) {

        String actor = principal != null ? principal.getName() : "operator";
        ReconciliationExceptionEntity updated = exceptionService.updateStatus(
                exceptionId, newStatus, actor);
        return ResponseEntity.ok(updated);
    }

    @RequestMapping(value = "/{exceptionId}/override", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Log an override on an exception",
               description = "Marks exception as OVERRIDDEN with a mandatory reason. Records the actor for maker-checker enforcement.")
    public ResponseEntity<ReconciliationExceptionEntity> override(
            @PathVariable String exceptionId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String reason,
            Principal principal) {

        String effectiveReason = (body != null && body.get("reason") != null && !body.get("reason").isBlank()) 
                ? body.get("reason") 
                : reason;
        if (effectiveReason == null || effectiveReason.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String actor = principal != null ? principal.getName() : (body != null && body.containsKey("actorId") ? body.get("actorId") : "operator");
        ReconciliationExceptionEntity overridden = exceptionService.override(
                exceptionId, effectiveReason, actor);
        return ResponseEntity.ok(overridden);
    }

    @GetMapping("/summary")
    @Operation(summary = "Exception summary", description = "Returns aggregate counts by status, priority, and classification")
    public ResponseEntity<Map<String, Object>> summary() {
        Map<String, Object> summary = exceptionService.getSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/clusters")
    @Operation(summary = "Phase 2 Root-Cause Clusters", description = "Groups open exceptions by partner and classification with root-cause hypotheses and remediations")
    public ResponseEntity<List<RootCauseClusterService.RootCauseCluster>> clusters() {
        return ResponseEntity.ok(clusterService.computeClusters());
    }
}
