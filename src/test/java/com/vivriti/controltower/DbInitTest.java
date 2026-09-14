package com.vivriti.controltower;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;

public class DbInitTest {
    @Test
    void createControlTowerDb() {
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/controltower_test", "postgres", "password")) {
            conn.createStatement().execute("CREATE DATABASE controltower");
            System.out.println("DATABASE controltower created successfully!");
        } catch (Exception e) {
            System.out.println("Note on db creation: " + e.getMessage());
        }
    }

    @Test
    void generateSeedClean() throws Exception {
        com.vivriti.controltower.generator.AnomalyInjector injector = new com.vivriti.controltower.generator.AnomalyInjector();
        com.vivriti.controltower.generator.SyntheticDataGenerator gen = new com.vivriti.controltower.generator.SyntheticDataGenerator(injector);
        gen.generate(42L, 2000, "./data/seed_clean");
        gen.generate(42L, 2000, "./data/seed_a");
        System.out.println("Generated seed_clean and seed_a successfully!");
    }
}
