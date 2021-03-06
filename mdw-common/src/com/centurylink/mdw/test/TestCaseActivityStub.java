package com.centurylink.mdw.test;

import java.util.Map;

import groovy.lang.Closure;

public class TestCaseActivityStub {

    private Closure<Boolean> matcher;
    public Closure<Boolean> getMatcher() { return matcher; }
    public void setMatcher(Closure<Boolean> matcher) { this.matcher = matcher; }

    private Closure<String> completer;
    public Closure<String> getCompleter() { return completer; }
    public void setCompleter(Closure<String> completer) { this.completer = completer; }

    private Map<String,Object> variables;
    public Map<String,Object> getVariables() { return variables; }
    public void setVariables(Map<String,Object> variables) { this.variables = variables; }

    /**
     * Represents a server-side sleep.  Support either "sleep" or "delay" syntax.
     */
    private int sleep;
    public int getSleep() { return sleep; }
    public void setSleep(int sleep) { this.sleep = sleep; }
    private int delay;
    public int getDelay() { return delay; }
    public void setDelay(int delay) { this.delay = delay; }

    public TestCaseActivityStub(Closure<Boolean> matcher, Closure<String> completer) {
        this.matcher = matcher;
        this.completer = completer;
    }
}