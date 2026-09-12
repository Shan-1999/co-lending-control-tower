package com.vivriti.controltower.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

public class PartnerBetaWriter {

    public void writeFeed(File outputFile, String batchId, LocalDate businessDate, List<FeedRecord> records, boolean schemaBreach, boolean batchTotalMismatch) throws IOException {
        long declaredAmount = records.stream().mapToLong(FeedRecord::amount).sum();
        if (batchTotalMismatch) {
            declaredAmount += 500000;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            writer.printf("# batch_id=%s,partner_code=PARTNER_BETA,business_date=%s,declared_count=%d,declared_amount_paise=%d%n",
                    batchId, businessDate.toString(), records.size(), declaredAmount);
            writer.println("loan_reference,amount_paise,status,timestamp,utr_reference,event_type,correlation_id,business_event_id");

            for (FeedRecord r : records) {
                long amt = r.amount();
                String loanRef = r.loanReference() != null ? r.loanReference() : "";
                
                writer.printf("%s,%d,%s,%s,%s,%s,%s,%s%n",
                        loanRef,
                        amt,
                        r.status().name(),
                        r.timestamp().toString(),
                        r.utrReference() != null ? r.utrReference() : "",
                        r.eventType().name(),
                        r.correlationId() != null ? r.correlationId() : "",
                        r.businessEventId() != null ? r.businessEventId() : ""
                );
            }
        }
    }
}
