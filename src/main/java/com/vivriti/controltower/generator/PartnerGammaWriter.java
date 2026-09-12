package com.vivriti.controltower.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

public class PartnerGammaWriter {

    public void writeFeed(File outputFile, String batchId, LocalDate businessDate, List<FeedRecord> records, boolean schemaBreach, boolean batchTotalMismatch) throws IOException {
        long declaredAmount = records.stream().mapToLong(FeedRecord::amount).sum();
        if (batchTotalMismatch) {
            declaredAmount += 500000;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            writer.printf("#HDR|%s|PARTNER_GAMMA|%s|%d|%d%n",
                    batchId, businessDate.toString(), records.size(), declaredAmount);
            writer.println("LOAN_ID|DISB_AMT_PAISE|TS|STATUS|UTR|EVT_TYPE|CORR_ID|BIZ_EVT_ID");

            for (FeedRecord r : records) {
                long amt = r.amount();
                String loanRef = r.loanReference() != null ? r.loanReference() : "";

                writer.printf("%s|%d|%s|%s|%s|%s|%s|%s%n",
                        loanRef,
                        amt,
                        r.timestamp().toString(),
                        r.status().name(),
                        r.utrReference() != null ? r.utrReference() : "",
                        r.eventType().name(),
                        r.correlationId() != null ? r.correlationId() : "",
                        r.businessEventId() != null ? r.businessEventId() : ""
                );
            }
        }
    }
}
