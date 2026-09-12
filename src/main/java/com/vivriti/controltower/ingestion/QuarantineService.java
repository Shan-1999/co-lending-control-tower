package com.vivriti.controltower.ingestion;

import com.vivriti.controltower.canonical.RawSourceRecordEntity;
import org.springframework.stereotype.Service;

@Service
public class QuarantineService {

    public boolean validateAndQuarantine(ParsedFeedRecord parsed, RawSourceRecordEntity rawEntity) {
        if (parsed.loanReference() == null || parsed.loanReference().trim().isEmpty()) {
            rawEntity.setQuarantined(true);
            rawEntity.setQuarantineReason("Missing loan reference");
            return false;
        }
        
        if (parsed.amountPaise() < 0 && (parsed.reversalReference() == null || parsed.reversalReference().isEmpty())) {
            rawEntity.setQuarantined(true);
            rawEntity.setQuarantineReason("Negative amount without reversal reference");
            rawEntity.setQuarantineReason("Negative amount without reversal flag / reference");
            return false;
        }

        if (parsed.timestamp() == null) {
            rawEntity.setQuarantined(true);
            rawEntity.setQuarantineReason("Missing timestamp");
            return false;
        }

        rawEntity.setQuarantined(false);
        return true;
    }
}
