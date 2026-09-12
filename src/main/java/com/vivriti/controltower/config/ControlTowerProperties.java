package com.vivriti.controltower.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "controltower")
public class ControlTowerProperties {

    private GeneratorProperties generator = new GeneratorProperties();
    private ReconciliationProperties reconciliation = new ReconciliationProperties();
    private CloseProperties close = new CloseProperties();
    private ExceptionProperties exception = new ExceptionProperties();

    public GeneratorProperties getGenerator() { return generator; }
    public void setGenerator(GeneratorProperties generator) { this.generator = generator; }

    public ReconciliationProperties getReconciliation() { return reconciliation; }
    public void setReconciliation(ReconciliationProperties reconciliation) { this.reconciliation = reconciliation; }

    public CloseProperties getClose() { return close; }
    public void setClose(CloseProperties close) { this.close = close; }

    public ExceptionProperties getException() { return exception; }
    public void setException(ExceptionProperties exception) { this.exception = exception; }

    public static class GeneratorProperties {
        private long seed = 42;
        private int loanCount = 1000;
        private int businessDays = 3;
        private int cutoffHour = 18;
        private String cutoffZone = "UTC";
        private String outputDir = "./data";
        private AnomalyProperties anomaly = new AnomalyProperties();

        public long getSeed() { return seed; }
        public void setSeed(long seed) { this.seed = seed; }

        public int getLoanCount() { return loanCount; }
        public void setLoanCount(int loanCount) { this.loanCount = loanCount; }

        public int getBusinessDays() { return businessDays; }
        public void setBusinessDays(int businessDays) { this.businessDays = businessDays; }

        public int getCutoffHour() { return cutoffHour; }
        public void setCutoffHour(int cutoffHour) { this.cutoffHour = cutoffHour; }

        public String getCutoffZone() { return cutoffZone; }
        public void setCutoffZone(String cutoffZone) { this.cutoffZone = cutoffZone; }

        public String getOutputDir() { return outputDir; }
        public void setOutputDir(String outputDir) { this.outputDir = outputDir; }

        public AnomalyProperties getAnomaly() { return anomaly; }
        public void setAnomaly(AnomalyProperties anomaly) { this.anomaly = anomaly; }
    }

    public static class AnomalyProperties {
        private double overallRate = 0.08;
        private double missingBankLegRate = 0.01;
        private double missingLmsLegRate = 0.01;
        private double duplicateBankEventRate = 0.008;
        private double amountMismatchRate = 0.02;
        private double statusMismatchRate = 0.008;
        private double timingDifferenceRate = 0.015;
        private double compositeSplitRate = 0.008;
        private double orphanReversalRate = 0.005;
        private double schemaBreachRate = 0.005;
        private double batchTotalMismatchRate = 0.005;

        public double getOverallRate() { return overallRate; }
        public void setOverallRate(double overallRate) { this.overallRate = overallRate; }

        public double getMissingBankLegRate() { return missingBankLegRate; }
        public void setMissingBankLegRate(double missingBankLegRate) { this.missingBankLegRate = missingBankLegRate; }

        public double getMissingLmsLegRate() { return missingLmsLegRate; }
        public void setMissingLmsLegRate(double missingLmsLegRate) { this.missingLmsLegRate = missingLmsLegRate; }

        public double getDuplicateBankEventRate() { return duplicateBankEventRate; }
        public void setDuplicateBankEventRate(double duplicateBankEventRate) { this.duplicateBankEventRate = duplicateBankEventRate; }

        public double getAmountMismatchRate() { return amountMismatchRate; }
        public void setAmountMismatchRate(double amountMismatchRate) { this.amountMismatchRate = amountMismatchRate; }

        public double getStatusMismatchRate() { return statusMismatchRate; }
        public void setStatusMismatchRate(double statusMismatchRate) { this.statusMismatchRate = statusMismatchRate; }

        public double getTimingDifferenceRate() { return timingDifferenceRate; }
        public void setTimingDifferenceRate(double timingDifferenceRate) { this.timingDifferenceRate = timingDifferenceRate; }

        public double getCompositeSplitRate() { return compositeSplitRate; }
        public void setCompositeSplitRate(double compositeSplitRate) { this.compositeSplitRate = compositeSplitRate; }

        public double getOrphanReversalRate() { return orphanReversalRate; }
        public void setOrphanReversalRate(double orphanReversalRate) { this.orphanReversalRate = orphanReversalRate; }

        public double getSchemaBreachRate() { return schemaBreachRate; }
        public void setSchemaBreachRate(double schemaBreachRate) { this.schemaBreachRate = schemaBreachRate; }

        public double getBatchTotalMismatchRate() { return batchTotalMismatchRate; }
        public void setBatchTotalMismatchRate(double batchTotalMismatchRate) { this.batchTotalMismatchRate = batchTotalMismatchRate; }
    }

    public static class ReconciliationProperties {
        private String ruleVersion = "1.0";
        private int graceWindowHours = 24;
        private double probableMatchThreshold = 0.82;
        private ScoringWeightsProperties scoringWeights = new ScoringWeightsProperties();

        public String getRuleVersion() { return ruleVersion; }
        public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }

        public int getGraceWindowHours() { return graceWindowHours; }
        public void setGraceWindowHours(int graceWindowHours) { this.graceWindowHours = graceWindowHours; }

        public double getProbableMatchThreshold() { return probableMatchThreshold; }
        public void setProbableMatchThreshold(double probableMatchThreshold) { this.probableMatchThreshold = probableMatchThreshold; }

        public ScoringWeightsProperties getScoringWeights() { return scoringWeights; }
        public void setScoringWeights(ScoringWeightsProperties scoringWeights) { this.scoringWeights = scoringWeights; }
    }

    public static class ScoringWeightsProperties {
        private double amountProximity = 0.40;
        private double dateProximity = 0.25;
        private double stringSimilarity = 0.35;

        public double getAmountProximity() { return amountProximity; }
        public void setAmountProximity(double amountProximity) { this.amountProximity = amountProximity; }

        public double getDateProximity() { return dateProximity; }
        public void setDateProximity(double dateProximity) { this.dateProximity = dateProximity; }

        public double getStringSimilarity() { return stringSimilarity; }
        public void setStringSimilarity(double stringSimilarity) { this.stringSimilarity = stringSimilarity; }
    }

    public static class CloseProperties {
        private long unresolvedExceptionThresholdPaise = 0L;

        public long getUnresolvedExceptionThresholdPaise() { return unresolvedExceptionThresholdPaise; }
        public void setUnresolvedExceptionThresholdPaise(long unresolvedExceptionThresholdPaise) { this.unresolvedExceptionThresholdPaise = unresolvedExceptionThresholdPaise; }
    }

    public static class ExceptionProperties {
        private SlaProperties sla = new SlaProperties();

        public SlaProperties getSla() { return sla; }
        public void setSla(SlaProperties sla) { this.sla = sla; }
    }

    public static class SlaProperties {
        private int amountMismatchHours = 4;
        private int statusMismatchHours = 2;
        private int missingFeedLegHours = 12;
        private int duplicateCallbackHours = 1;
        private int orphanReversalHours = 4;
        private int schemaBreachHours = 6;
        private int controlTotalMismatchHours = 0;

        public int getAmountMismatchHours() { return amountMismatchHours; }
        public void setAmountMismatchHours(int amountMismatchHours) { this.amountMismatchHours = amountMismatchHours; }

        public int getStatusMismatchHours() { return statusMismatchHours; }
        public void setStatusMismatchHours(int statusMismatchHours) { this.statusMismatchHours = statusMismatchHours; }

        public int getMissingFeedLegHours() { return missingFeedLegHours; }
        public void setMissingFeedLegHours(int missingFeedLegHours) { this.missingFeedLegHours = missingFeedLegHours; }

        public int getDuplicateCallbackHours() { return duplicateCallbackHours; }
        public void setDuplicateCallbackHours(int duplicateCallbackHours) { this.duplicateCallbackHours = duplicateCallbackHours; }

        public int getOrphanReversalHours() { return orphanReversalHours; }
        public void setOrphanReversalHours(int orphanReversalHours) { this.orphanReversalHours = orphanReversalHours; }

        public int getSchemaBreachHours() { return schemaBreachHours; }
        public void setSchemaBreachHours(int schemaBreachHours) { this.schemaBreachHours = schemaBreachHours; }

        public int getControlTotalMismatchHours() { return controlTotalMismatchHours; }
        public void setControlTotalMismatchHours(int controlTotalMismatchHours) { this.controlTotalMismatchHours = controlTotalMismatchHours; }
    }
}
