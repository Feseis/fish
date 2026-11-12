package com.feseis.fish.logistics.model;

/**
 * 入职结果。岗位与归属网点由凭证决定。
 */
public final class StaffApplyResult {

    private final String staffId;
    private final String staffNo;
    private final String staffName;
    private final int staffRole;
    private final String siteCode;
    private final String siteName;
    private final String appliedAt;

    public StaffApplyResult(String staffId, String staffNo, String staffName, int staffRole,
                            String siteCode, String siteName, String appliedAt) {
        this.staffId = staffId;
        this.staffNo = staffNo;
        this.staffName = staffName;
        this.staffRole = staffRole;
        this.siteCode = siteCode;
        this.siteName = siteName;
        this.appliedAt = appliedAt;
    }

    public String getStaffId() {
        return this.staffId;
    }

    public String getStaffNo() {
        return this.staffNo;
    }

    public String getStaffName() {
        return this.staffName;
    }

    public int getStaffRole() {
        return this.staffRole;
    }

    public String getSiteCode() {
        return this.siteCode;
    }

    public String getSiteName() {
        return this.siteName;
    }

    public String getAppliedAt() {
        return this.appliedAt;
    }
}
