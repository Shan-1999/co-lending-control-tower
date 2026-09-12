package com.vivriti.controltower.ingestion;

import com.vivriti.controltower.canonical.CanonicalEventEntity;
import com.vivriti.controltower.canonical.CanonicalEventRepository;
import com.vivriti.controltower.canonical.MatchDecisionRepository;
import com.vivriti.controltower.canonical.RawSourceRecordRepository;
import com.vivriti.controltower.generator.SyntheticDataGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class IdempotencyIntegrationTest {

    @Autowired
    private SyntheticDataGenerator generator;

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private RawSourceRecordRepository rawRepository;

    @Autowired
    private CanonicalEventRepository canonicalRepository;

    @Autowired
    private MatchDecisionRepository matchDecisionRepository;

    @Autowired
    private com.vivriti.controltower.canonical.SourceBatchRepository batchRepository;

    @TempDir
    Path tempDir;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        matchDecisionRepository.deleteAll();
        canonicalRepository.deleteAll();
        rawRepository.deleteAll();
        batchRepository.deleteAll();
    }

    @Test
    void testIngestionIdempotency() throws Exception {
        long initialRawCount = rawRepository.count();
        long initialCanonicalCount = canonicalRepository.count();
        
        // Generate small set of feed data
        generator.generate(42L, 10, tempDir.toString());

        // First ingestion
        IngestionResult firstResult = ingestionService.ingestDirectory(tempDir);
        assertThat(firstResult.processedRecords()).isGreaterThan(0);

        long rawCountAfterFirst = rawRepository.count();
        long canonicalCountAfterFirst = canonicalRepository.count();

        assertThat(rawCountAfterFirst).isGreaterThan(initialRawCount);
        assertThat(canonicalCountAfterFirst).isGreaterThan(initialCanonicalCount);

        long totalAmountFirst = canonicalRepository.findAll().stream()
                .mapToLong(CanonicalEventEntity::getAmountPaise)
                .sum();

        // Second ingestion
        IngestionResult secondResult = ingestionService.ingestDirectory(tempDir);
        
        // Assert counts are unchanged
        assertThat(rawRepository.count()).isEqualTo(rawCountAfterFirst);
        assertThat(canonicalRepository.count()).isEqualTo(canonicalCountAfterFirst);

        long totalAmountSecond = canonicalRepository.findAll().stream()
                .mapToLong(CanonicalEventEntity::getAmountPaise)
                .sum();

        assertThat(totalAmountSecond).isEqualTo(totalAmountFirst);
        
        // Assert that the second run skipped all duplicates
        assertThat(secondResult.skippedDuplicates()).isEqualTo(firstResult.totalRecords());
    }
}
