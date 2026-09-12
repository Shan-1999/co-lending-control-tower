package com.vivriti.controltower.exception;

import com.vivriti.controltower.common.enums.ExceptionClassification;
import com.vivriti.controltower.common.enums.ExceptionPriority;

public record ClassificationResult(
    ExceptionClassification classification,
    String ownerRole,
    ExceptionPriority priority,
    int slaHours,
    String recommendedAction
) {}
