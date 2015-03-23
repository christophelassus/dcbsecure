package com.dcbsecure.demo201503.dcbsecure.flow;

public class AllowanceDetails {
    private long timeLeftSeconds;
    private boolean paidAllowance;

    public AllowanceDetails(long timeLeftSeconds, boolean paidAllowance) {
        this.timeLeftSeconds = timeLeftSeconds;
        this.paidAllowance = paidAllowance;
    }

    public long getTimeLeftSeconds() {
        return timeLeftSeconds;
    }

    public boolean isPaidAllowance() {
        return paidAllowance;
    }
}
