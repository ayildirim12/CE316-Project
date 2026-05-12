package com.iae.model;

public class Configuration {

    private int    id;
    private String name;
    private String language;
    private String sourceFile;
    private String compileCommand;
    private String compileArgs;
    private String runCommand;
    private String runArgs;
    private boolean needsCompilation;

    public Configuration() {}

    public int     getId()             { return id; }
    public void    setId(int id)       { this.id = id; }

    public String  getName()           { return name; }
    public void    setName(String n)   { this.name = n; }

    public String  getLanguage()       { return language; }
    public void    setLanguage(String l){ this.language = l; }

    public String  getSourceFile()     { return sourceFile; }
    public void    setSourceFile(String s){ this.sourceFile = s; }

    public String  getCompileCommand() { return compileCommand; }
    public void    setCompileCommand(String c){ this.compileCommand = c; }

    public String  getCompileArgs()    { return compileArgs; }
    public void    setCompileArgs(String a){ this.compileArgs = a; }

    public String  getRunCommand()     { return runCommand; }
    public void    setRunCommand(String r){ this.runCommand = r; }

    public String  getRunArgs()        { return runArgs; }
    public void    setRunArgs(String a){ this.runArgs = a; }

    public boolean isNeedsCompilation()         { return needsCompilation; }
    public void    setNeedsCompilation(boolean b){ this.needsCompilation = b; }
}
