package com.vivriti.controltower.generator;

import com.vivriti.controltower.common.HashUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class GeneratorDeterminismTest {

    @TempDir
    Path dir1;

    @TempDir
    Path dir2;

    @Test
    void testDeterminism() throws Exception {
        AnomalyInjector anomalyInjector = new AnomalyInjector();
        // Since we are not running in a Spring context, the @Value annotations are not processed.
        // However, the fallback logic in AnomalyInjector ensures anomalies are still injected.
        
        SyntheticDataGenerator generator1 = new SyntheticDataGenerator(anomalyInjector);
        generator1.generate(42L, 100, dir1.toString());

        SyntheticDataGenerator generator2 = new SyntheticDataGenerator(anomalyInjector);
        generator2.generate(42L, 100, dir2.toString());

        List<File> files1 = Files.walk(dir1)
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .sorted(Comparator.comparing(File::getName))
                .collect(Collectors.toList());

        List<File> files2 = Files.walk(dir2)
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .sorted(Comparator.comparing(File::getName))
                .collect(Collectors.toList());

        assertThat(files1).hasSize(files2.size());
        assertThat(files1).isNotEmpty();

        for (int i = 0; i < files1.size(); i++) {
            File f1 = files1.get(i);
            File f2 = files2.get(i);

            assertThat(f1.getName()).isEqualTo(f2.getName());

            String hash1 = HashUtils.sha256(Files.readString(f1.toPath()));
            String hash2 = HashUtils.sha256(Files.readString(f2.toPath()));

            assertThat(hash1).as("Hashes for file " + f1.getName() + " should be identical")
                             .isEqualTo(hash2);
        }
    }
}
