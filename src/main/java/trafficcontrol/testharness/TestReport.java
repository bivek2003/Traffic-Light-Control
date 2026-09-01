package trafficcontrol.testharness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Collects pass/fail details for one test harness run. */
public final class TestReport {
    private final String scriptName;
    private final List<String> entries = new ArrayList<>();
    private int passed;
    private int failed;

    public TestReport(String scriptName) {
        this.scriptName = scriptName;
    }

    public void pass(String detail) {
        passed++;
        entries.add("PASS " + detail);
    }

    public void fail(String detail) {
        failed++;
        entries.add("FAIL " + detail);
    }

    public String getScriptName() {
        return scriptName;
    }

    public int getPassed() {
        return passed;
    }

    public int getFailed() {
        return failed;
    }

    public boolean passed() {
        return failed == 0;
    }

    public List<String> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void print() {
        System.out.println("Test script: " + scriptName);
        for (String entry : entries) {
            System.out.println("  " + entry);
        }
        System.out.println("Result: " + passed + " passed, " + failed + " failed");
    }
}
