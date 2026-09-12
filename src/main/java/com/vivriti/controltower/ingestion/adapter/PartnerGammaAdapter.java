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
public class PartnerGammaAdapter implements PartnerFeedAdapter {

    @Override
    public boolean supports(String partnerCode) {
        return "PARTNER_GAMMA".equals(partnerCode);
    }

    @Override
    public ParsedBatch parse(String partnerCode, String sourceSystem, Path filePath) {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String firstLine = reader.readLine();
            if (firstLine == null || !firstLine.startsWith("#HDR")) {
                throw new IllegalArgumentException("Invalid or missing #HDR line in: " + filePath);
            }

            // Format variants:
            //   #HDR|batch_id|partner_code|business_date|declared_count|declared_amount_paise  (6 parts)
            //   #HDR|business_date|declared_count|declared_amount_paise                        (4 parts)
            String[] headerParts = firstLine.split("\\|");
            String batchId = null;
            LocalDate businessDate = null;
            int declaredCount = 0;
            long declaredAmountPaise = 0L;

            if (headerParts.length >= 6) {
                batchId = headerParts[1].trim();
                businessDate = LocalDate.parse(headerParts[3].trim());
                declaredCount = Integer.parseInt(headerParts[4].trim());
                declaredAmountPaise = Long.parseLong(headerParts[5].trim());
            } else if (headerParts.length >= 4) {
                businessDate = LocalDate.parse(headerParts[1].trim());
                declaredCount = Integer.parseInt(headerParts[2].trim());
                declaredAmountPaise = Long.parseLong(headerParts[3].trim());
            }

            if (businessDate == null) businessDate = LocalDate.of(2024, 1, 15);
            if (batchId == null || batchId.isBlank()) batchId = IdGenerator.newBatchId(partnerCode, sourceSystem, businessDate);

            // Skip column headers
            reader.readLine();

            List<ParsedFeedRecord> records = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                ParsedFeedRecord record = new ParsedFeedRecord(
                        parts.length > 0 && !parts[0].isEmpty() ? parts[0] : null,
                        parts.length > 1 && !parts[1].isEmpty() ? Long.parseLong(parts[1]) : 0L,
                        parts.length > 3 && !parts[3].isEmpty() ? parts[3] : null,
                        parts.length > 2 && !parts[2].isEmpty() ? OffsetDateTime.parse(parts[2]) : OffsetDateTime.now(),
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
            throw new RuntimeException("Failed to parse Partner Gamma file: " + filePath, e);
        }
    }
}
