package com.iae.model;

public class ComparisonResult {

    private final boolean match;
    private final String diffText;

    public ComparisonResult(boolean match, String diffText) {
        this.match    = match;
        this.diffText = diffText != null ? diffText : "";
    }

    public boolean isMatch() { return match; }

    public String getDiff()  { return diffText; }
}
