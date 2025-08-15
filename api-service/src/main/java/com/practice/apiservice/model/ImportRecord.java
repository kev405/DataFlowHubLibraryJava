package com.practice.apiservice.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public class ImportRecord {
    private String externalId;
    private String userEmail;
    private BigDecimal amount;
    private Instant eventTime;
    private Map<String,String> meta; // opcional

    public ImportRecord() {}

    public ImportRecord(String externalId, String userEmail, BigDecimal amount, Instant eventTime) {
        this.externalId = externalId;
        this.userEmail = userEmail;
        this.amount = amount;
        this.eventTime = eventTime;
    }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    public Map<String, String> getMeta() { return meta; }
    public void setMeta(Map<String, String> meta) { this.meta = meta; }
}
