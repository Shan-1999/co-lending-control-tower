package com.vivriti.controltower;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.vivriti.controltower")
public class ArchUnitGroundTruthIsolationTest {

    @ArchTest
    static final ArchRule no_reconciliation_dependency_on_ground_truth =
        noClasses().that().resideInAPackage("com.vivriti.controltower.reconciliation..")
            .should().dependOnClassesThat().resideInAPackage("com.vivriti.controltower.generator.groundtruth..");

    @ArchTest
    static final ArchRule no_ingestion_dependency_on_ground_truth =
        noClasses().that().resideInAPackage("com.vivriti.controltower.ingestion..")
            .should().dependOnClassesThat().resideInAPackage("com.vivriti.controltower.generator.groundtruth..");

    @ArchTest
    static final ArchRule no_close_dependency_on_ground_truth =
        noClasses().that().resideInAPackage("com.vivriti.controltower.close..")
            .should().dependOnClassesThat().resideInAPackage("com.vivriti.controltower.generator.groundtruth..");

    @ArchTest
    static final ArchRule no_exception_dependency_on_ground_truth =
        noClasses().that().resideInAPackage("com.vivriti.controltower.exception..")
            .should().dependOnClassesThat().resideInAPackage("com.vivriti.controltower.generator.groundtruth..");
}
