package com.vivriti.controltower.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class IdGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private IdGenerator() {
        // Prevent instantiation
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    public static String newBatchId(String partnerCode, String sourceSystem, LocalDate businessDate) {
        String dateStr = businessDate.format(DATE_FORMATTER);
        return String.format("B-%s-%s-%s", partnerCode, sourceSystem, dateStr).toUpperCase();
    }

    public static String newRecordId(String batchId, int index) {
        return String.format("%s-R%06d", batchId, index);
    }
}
