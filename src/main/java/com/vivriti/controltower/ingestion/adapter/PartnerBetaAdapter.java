package com.vivriti.controltower.ingestion.adapter;

import com.vivriti.controltower.common.IdGenerator;
import com.vivriti.controltower.ingestion.ParsedBatch;
import com.vivriti.controltower.ingestion.ParsedFeedRecord;
import com.vivriti.controltower.ingestion.PartnerFeedAdapter;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class PartnerBetaAdapter implements PartnerFeedAdapter {

    @Override
    public boolean supports(String partnerCode) {
        return "PARTNER_BETA".equals(partnerCode);
    }

    @Override
    public ParsedBatch parse(String partnerCode, String sourceSystem, Path filePath) {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String firstLine = reader.readLine();
            if (firstLine == null || !firstLine.startsWith("#")) {
                throw new IllegalArgumentException("Invalid or missing batch header comment line in: " + filePath);
            }

            // Parse key-value header: # batch_id=...,partner_code=...,business_date=...,declared_count=...,declared_amount_paise=...
            String clean = firstLine.replaceFirst("^#\\s*", "");
            String batchId = null;
            LocalDate businessDate = null;
            int declaredCount = 0;
            long declaredAmountPaise = 0L;

            if (clean.contains("=")) {
                String[] tokens = clean.split(",");
                for (String token : tokens) {
                    String[] kv = token.split("=", 2);
                    if (kv.length == 2) {
                        String k = kv[0].trim().toLowerCase();
                        String v = kv[1].trim();
                        switch (k) {
                            case "batch_id" -> batchId = v;
                            case "business_date" -> businessDate = LocalDate.parse(v);
                            case "declared_count" -> declaredCount = Integer.parseInt(v);
                            case "declared_amount_paise" -> declaredAmountPaise = Long.parseLong(v);
                        }
                    }
                }
            } else {
                String[] headerParts = clean.split(",");
                businessDate = LocalDate.parse(headerParts[0].trim());
                declaredCount = Integer.parseInt(headerParts[1].trim());
                declaredAmountPaise = Long.parseLong(headerParts[2].trim());
            }

            if (businessDate == null) businessDate = LocalDate.of(2024, 1, 15);
            if (batchId == null || batchId.isBlank()) batchId = IdGenerator.newBatchId(partnerCode, sourceSystem, businessDate);

            // Skip CSV column headers
            reader.readLine();

            List<ParsedFeedRecord> records = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",", -1);
                ParsedFeedRecord record = new ParsedFeedRecord(
                        parts.length > 0 && !parts[0].isEmpty() ? parts[0] : null,
                        parts.length > 1 && !parts[1].isEmpty() ? Long.parseLong(parts[1]) : 0L,
                        parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null,
                        parts.length > 3 && !parts[3].isEmpty() ? OffsetDateTime.parse(parts[3]) : OffsetDateTime.now(),
                        parts.length > 4 && !parts[4].isEmpty() ? parts[4] : null,
                        parts.length > 5 && !parts[5].isEmpty() ? parts[5] : "DISBURSEMENT_INSTRUCTION",
                        parts.length > 6 && !parts[6].isEmpty() ? parts[6] : null,
                        parts.length > 7 && !parts[7].isEmpty() ? parts[7] : null,
                        partnerCode,
                        sourceSystem,
                        parts.length > 8 && !parts[8].isEmpty() ? parts[8] : null,
                        line
                );
                records.add(record);
            }

            return new ParsedBatch(batchId, partnerCode, sourceSystem, businessDate, declaredCount, declaredAmountPaise, records);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Partner Beta file: " + filePath, e);
        }
    }
}
