package com.cityjobs.model;

import java.util.UUID;

public class PlayerJobData {
    private final UUID uuid;
    private JobType jobType;
    private long joinTimestamp;
    private double xp;
    private int level;
    private int resourcesCollected;
    private double moneyEarned;

    public PlayerJobData(UUID uuid) {
        this.uuid = uuid;
        this.jobType = null;
        this.joinTimestamp = 0;
        this.xp = 0.0;
        this.level = 1;
        this.resourcesCollected = 0;
        this.moneyEarned = 0.0;
    }

    public UUID getUuid() { return uuid; }
    
    public boolean hasJob() { return jobType != null; }
    
    public JobType getJobType() { return jobType; }
    
    public void setJobType(JobType jobType) { this.jobType = jobType; }
    
    public long getJoinTimestamp() { return joinTimestamp; }
    
    public void setJoinTimestamp(long joinTimestamp) { this.joinTimestamp = joinTimestamp; }
    
    public double getXp() { return xp; }
    
    public void addXp(double amount) { this.xp += amount; }
    
    public void setXp(double xp) { this.xp = xp; }
    
    public int getLevel() { return level; }
    
    public void setLevel(int level) { this.level = level; }
    
    public int getResourcesCollected() { return resourcesCollected; }
    
    public void addResourceCollected() { this.resourcesCollected++; }
    
    public void setResourcesCollected(int resourcesCollected) { this.resourcesCollected = resourcesCollected; }
    
    public double getMoneyEarned() { return moneyEarned; }
    
    public void addMoneyEarned(double amount) { this.moneyEarned += amount; }
    
    public void setMoneyEarned(double moneyEarned) { this.moneyEarned = moneyEarned; }
    
    public void clearJob() {
        this.jobType = null;
        this.joinTimestamp = 0;
        this.xp = 0.0;
        this.level = 1;
        this.resourcesCollected = 0;
        this.moneyEarned = 0.0;
    }
}