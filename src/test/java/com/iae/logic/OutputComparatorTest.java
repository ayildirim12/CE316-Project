package com.iae.logic;

import com.iae.model.ComparisonResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OutputComparator}. The comparator reads the expected
 * output from a file, so each test writes one into a {@link TempDir}.
 */
class OutputComparatorTest {

    @TempDir
    Path tmp;

    private final OutputComparator comparator = new OutputComparator();

    private String expectedFile(String content) throws IOException {
        Path f = tmp.resolve("expected.txt");
        Files.writeString(f, content);
        return f.toString();
    }

    @Test
    void identicalOutputMatches() throws IOException {
        ComparisonResult r = comparator.compare("Hello\nWorld\n", expectedFile("Hello\nWorld\n"));
        assertTrue(r.isMatch());
        assertEquals("", r.getDiff());
    }

    @Test
    void trailingNewlineDifferenceIsIgnored() throws IOException {
        ComparisonResult r = comparator.compare("42", expectedFile("42\n\n"));
        assertTrue(r.isMatch(), "trailing blank lines should be normalized away");
    }

    @Test
    void crlfVsLfIsNormalized() throws IOException {
        ComparisonResult r = comparator.compare("a\r\nb\r\n", expectedFile("a\nb\n"));
        assertTrue(r.isMatch(), "CRLF and LF must be treated equally");
    }

    @Test
    void leadingAndTrailingSpacesPerLineAreStripped() throws IOException {
        ComparisonResult r = comparator.compare("  hello  ", expectedFile("hello"));
        assertTrue(r.isMatch());
    }

    @Test
    void differentContentDoesNotMatchAndProducesDiff() throws IOException {
        ComparisonResult r = comparator.compare("Hello\nMars", expectedFile("Hello\nWorld"));
        assertFalse(r.isMatch());
        assertFalse(r.getDiff().isBlank(), "a diff should be produced");
        assertTrue(r.getDiff().contains("Line 2"), "diff should point at the differing line");
    }

    @Test
    void missingExpectedFileReturnsNoMatchWithMessage() {
        ComparisonResult r = comparator.compare("anything",
                tmp.resolve("does-not-exist.txt").toString());
        assertFalse(r.isMatch());
        assertTrue(r.getDiff().toLowerCase().contains("cannot read"),
                "should explain the file could not be read");
    }

    @Test
    void nullActualOutputIsTreatedAsEmpty() throws IOException {
        ComparisonResult r = comparator.compare(null, expectedFile(""));
        assertTrue(r.isMatch(), "null actual output normalizes to empty and matches empty expected");
    }
}
