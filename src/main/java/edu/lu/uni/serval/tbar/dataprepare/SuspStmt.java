package edu.lu.uni.serval.tbar.dataprepare;

public class SuspStmt {
    public String issueID;
    public String ruleID;
    public String status;
    public String suspCode;
    public String longName;
    public int lineNum;

    public SuspStmt(String issueID, String status, String ruleID, String suspCode) {
        this.issueID = issueID;
        this.ruleID = ruleID;
        this.status = status;
        this.suspCode = suspCode;
        this.longName = suspCode.split("@")[0];
        if (!suspCode.endsWith("@"))
            this.lineNum = Integer.parseInt(suspCode.split("@")[1]);
        else {
            this.lineNum = 0;
            this.suspCode += "0";
        }
    }
}