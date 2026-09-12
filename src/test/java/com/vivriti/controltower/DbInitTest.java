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
}
