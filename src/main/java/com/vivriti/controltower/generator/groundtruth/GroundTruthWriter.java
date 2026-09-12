package com.vivriti.controltower.generator.groundtruth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GroundTruthWriter {

    private final ObjectMapper mapper;

    public GroundTruthWriter() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.configure(com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    public void writeGroundTruth(File outputDir, List<GroundTruthRecord> records) throws IOException {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        File file = new File(outputDir, "ground_truth.json");
        mapper.writeValue(file, records);
    }
}
