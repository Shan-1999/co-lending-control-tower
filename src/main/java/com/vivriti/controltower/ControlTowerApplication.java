package com.vivriti.controltower;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Co-Lending Control Tower — Main Application Entry Point.
 *
 * <p>A production-grade reconciliation engine for co-lending disbursements
 * that reconciles data across Originator, Bank, and LMS feeds with
 * zero tolerance for false matches or silent write-offs.</p>
 */
@SpringBootApplication
public class ControlTowerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ControlTowerApplication.class, args);
    }
}

