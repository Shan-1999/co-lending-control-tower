package com.vivriti.controltower.generator;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class GeneratorCommand implements CommandLineRunner {

    private final SyntheticDataGenerator generator;

    public GeneratorCommand(SyntheticDataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0 && "generate".equals(args[0])) {
            long seed = 42;
            int loans = 1000;
            String outDir = "./data/seed_a";

            for (String arg : args) {
                if (arg.startsWith("--seed=")) {
                    seed = Long.parseLong(arg.substring(7));
                } else if (arg.startsWith("--loans=")) {
                    loans = Integer.parseInt(arg.substring(8));
                } else if (arg.startsWith("--out=")) {
                    outDir = arg.substring(6);
                }
            }

            System.out.println("Starting synthetic data generation...");
            System.out.println("Seed: " + seed);
            System.out.println("Loans: " + loans);
            System.out.println("Output Directory: " + outDir);

            generator.generate(seed, loans, outDir);

            System.exit(0);
        }
    }
}
