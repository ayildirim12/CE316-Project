package com.iae.model;
public class TestCase {

    private int    id;
    private String inputArgs;
    private String expectedOutputFilePath;

    public TestCase() {}

    public TestCase(int id, String inputArgs, String expectedOutputFilePath) {
        this.id                     = id;
        this.inputArgs              = inputArgs;
        this.expectedOutputFilePath = expectedOutputFilePath;
    }

    public int    getId()                      { return id; }
    public void   setId(int id)                { this.id = id; }

    public String getInputArgs()               { return inputArgs; }
    public void   setInputArgs(String s)       { this.inputArgs = s; }

    public String getExpectedOutputFilePath()  { return expectedOutputFilePath; }
    public void   setExpectedOutputFilePath(String s){ this.expectedOutputFilePath = s; }
}
