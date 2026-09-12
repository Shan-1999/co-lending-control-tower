package com.vivriti.controltower.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class PartnerAlphaWriter {

    private final ObjectMapper mapper;

    public PartnerAlphaWriter() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.configure(com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    public void writeFeed(File outputFile, String batchId, LocalDate businessDate, List<FeedRecord> records, boolean schemaBreach, boolean batchTotalMismatch) throws IOException {
        long declaredAmount = records.stream().mapToLong(FeedRecord::amount).sum();
        if (batchTotalMismatch) {
            declaredAmount += 500000;
        }

        Map<String, Object> payload = new java.util.TreeMap<>();
        payload.put("batchId", batchId);
        payload.put("partnerCode", "PARTNER_ALPHA");
        payload.put("businessDate", businessDate.toString());
        payload.put("declaredCount", records.size());
        payload.put("declaredAmountPaise", declaredAmount);

        List<Map<String, Object>> mappedRecords = records.stream().map(r -> {
            Map<String, Object> map = new java.util.TreeMap<>();
            map.put("loanReference", r.loanReference());
            map.put("amountPaise", r.amount());
            map.put("status", r.status().name());
            map.put("timestamp", r.timestamp().toString());
            map.put("utrReference", r.utrReference());
            map.put("sourceSystem", r.sourceSystem().name());
            map.put("eventType", r.eventType().name());
            if (r.reversalReference() != null) {
                map.put("reversalReference", r.reversalReference());
            }
            map.put("correlationId", r.correlationId());
            map.put("businessEventId", r.businessEventId());
            return map;
        }).toList();

        payload.put("records", mappedRecords);

        mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, payload);
    }
}
