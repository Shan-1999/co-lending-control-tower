package com.vivriti.controltower.ingestion.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivriti.controltower.common.IdGenerator;
import com.vivriti.controltower.ingestion.ParsedBatch;
import com.vivriti.controltower.ingestion.ParsedFeedRecord;
import com.vivriti.controltower.ingestion.PartnerFeedAdapter;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class PartnerAlphaAdapter implements PartnerFeedAdapter {

    private final ObjectMapper objectMapper;

    public PartnerAlphaAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String partnerCode) {
        return "PARTNER_ALPHA".equals(partnerCode);
    }

    @Override
    public ParsedBatch parse(String partnerCode, String sourceSystem, Path filePath) {
        try {
            JsonNode root = objectMapper.readTree(filePath.toFile());
            LocalDate businessDate = LocalDate.parse(root.get("businessDate").asText());
            int declaredCount = root.get("declaredCount").asInt();
            long declaredAmountPaise = root.get("declaredAmountPaise").asLong();
            
            String batchId = IdGenerator.newBatchId(partnerCode, sourceSystem, businessDate);
            
            List<ParsedFeedRecord> records = new ArrayList<>();
            JsonNode items = root.get("records");
            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    ParsedFeedRecord record = new ParsedFeedRecord(
                            item.path("loanReference").asText(null),
                            item.path("amountPaise").asLong(0),
                            item.path("status").asText(null),
                            item.hasNonNull("timestamp") ? OffsetDateTime.parse(item.get("timestamp").asText()) : null,
                            item.path("utrReference").asText(null),
                            item.path("eventType").asText(null),
                            item.path("correlationId").asText(null),
                            item.path("businessEventId").asText(null),
                            partnerCode,
                            sourceSystem,
                            item.path("reversalReference").asText(null),
                            item.toString()
                    );
                    records.add(record);
                }
            }
            
            return new ParsedBatch(batchId, partnerCode, sourceSystem, businessDate, declaredCount, declaredAmountPaise, records);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Partner Alpha file: " + filePath, e);
        }
    }
}
