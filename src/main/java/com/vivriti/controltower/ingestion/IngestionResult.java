package com.vivriti.controltower.ingestion;

public record IngestionResult(
    int totalRecords,
    int processedRecords,
    int quarantinedRecords,
    int skippedDuplicates,
    String batchStatus
) {}
