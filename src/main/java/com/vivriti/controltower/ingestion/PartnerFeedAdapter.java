package com.vivriti.controltower.ingestion;

import java.nio.file.Path;

public interface PartnerFeedAdapter {
    boolean supports(String partnerCode);
    ParsedBatch parse(String partnerCode, String sourceSystem, Path filePath);
}
