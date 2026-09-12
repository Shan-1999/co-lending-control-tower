package com.vivriti.controltower.generator;

import com.vivriti.controltower.common.enums.CanonicalStatus;
import java.time.OffsetDateTime;
import java.util.List;

public class LoanScenario {
    private String loanReference;
    private String partnerCode;
    private int businessDay;
    private long originatorAmount;
    private Long bankAmount;
    private Long lmsAmount;
    private CanonicalStatus originatorStatus;
    private CanonicalStatus bankStatus;
    private CanonicalStatus lmsStatus;
    private OffsetDateTime originatorTimestamp;
    private OffsetDateTime bankTimestamp;
    private OffsetDateTime lmsTimestamp;
    private AnomalyType anomalyType;
    private String utrReference;
    private List<Long> splitAmounts;
    private String reversalReference;
    private boolean duplicateBankEvent;
    private boolean schemaBreach;
    private boolean batchTotalMismatch;

    public String getLoanReference() { return loanReference; }
    public void setLoanReference(String loanReference) { this.loanReference = loanReference; }

    public String getPartnerCode() { return partnerCode; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }

    public int getBusinessDay() { return businessDay; }
    public void setBusinessDay(int businessDay) { this.businessDay = businessDay; }

    public long getOriginatorAmount() { return originatorAmount; }
    public void setOriginatorAmount(long originatorAmount) { this.originatorAmount = originatorAmount; }

    public Long getBankAmount() { return bankAmount; }
    public void setBankAmount(Long bankAmount) { this.bankAmount = bankAmount; }

    public Long getLmsAmount() { return lmsAmount; }
    public void setLmsAmount(Long lmsAmount) { this.lmsAmount = lmsAmount; }

    public CanonicalStatus getOriginatorStatus() { return originatorStatus; }
    public void setOriginatorStatus(CanonicalStatus originatorStatus) { this.originatorStatus = originatorStatus; }

    public CanonicalStatus getBankStatus() { return bankStatus; }
    public void setBankStatus(CanonicalStatus bankStatus) { this.bankStatus = bankStatus; }

    public CanonicalStatus getLmsStatus() { return lmsStatus; }
    public void setLmsStatus(CanonicalStatus lmsStatus) { this.lmsStatus = lmsStatus; }

    public OffsetDateTime getOriginatorTimestamp() { return originatorTimestamp; }
    public void setOriginatorTimestamp(OffsetDateTime originatorTimestamp) { this.originatorTimestamp = originatorTimestamp; }

    public OffsetDateTime getBankTimestamp() { return bankTimestamp; }
    public void setBankTimestamp(OffsetDateTime bankTimestamp) { this.bankTimestamp = bankTimestamp; }

    public OffsetDateTime getLmsTimestamp() { return lmsTimestamp; }
    public void setLmsTimestamp(OffsetDateTime lmsTimestamp) { this.lmsTimestamp = lmsTimestamp; }

    public AnomalyType getAnomalyType() { return anomalyType; }
    public void setAnomalyType(AnomalyType anomalyType) { this.anomalyType = anomalyType; }

    public String getUtrReference() { return utrReference; }
    public void setUtrReference(String utrReference) { this.utrReference = utrReference; }

    public List<Long> getSplitAmounts() { return splitAmounts; }
    public void setSplitAmounts(List<Long> splitAmounts) { this.splitAmounts = splitAmounts; }

    public String getReversalReference() { return reversalReference; }
    public void setReversalReference(String reversalReference) { this.reversalReference = reversalReference; }
    
    public boolean isDuplicateBankEvent() { return duplicateBankEvent; }
    public void setDuplicateBankEvent(boolean duplicateBankEvent) { this.duplicateBankEvent = duplicateBankEvent; }
    
    public boolean isSchemaBreach() { return schemaBreach; }
    public void setSchemaBreach(boolean schemaBreach) { this.schemaBreach = schemaBreach; }
    
    public boolean isBatchTotalMismatch() { return batchTotalMismatch; }
    public void setBatchTotalMismatch(boolean batchTotalMismatch) { this.batchTotalMismatch = batchTotalMismatch; }
}
