package com.iae.logic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ZipExtractor}. Each test builds a real ZIP archive in a
 * {@link TempDir} and extracts it.
 */
class ZipExtractorTest {

    @TempDir
    Path tmp;

    private final ZipExtractor extractor = new ZipExtractor();

    /** Writes a ZIP containing the given entryName→content pairs. */
    private File makeZip(String zipName, String[]... entries) throws IOException {
        File zip = tmp.resolve(zipName).toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            for (String[] e : entries) {
                zos.putNextEntry(new ZipEntry(e[0]));
                if (e.length > 1 && e[1] != null) {
                    zos.write(e[1].getBytes(StandardCharsets.UTF_8));
                }
                zos.closeEntry();
            }
        }
        return zip;
    }

    @Test
    void extractsNestedFileWithContent() throws IOException {
        File zip = makeZip("S100.zip", new String[]{"S100/main.c", "int main(){return 0;}"});
        File target = tmp.resolve("out").toFile();

        extractor.extract(zip, target);

        File extracted = new File(target, "S100/main.c");
        assertTrue(extracted.exists(), "nested file must be extracted");
        assertEquals("int main(){return 0;}", Files.readString(extracted.toPath()));
    }

    @Test
    void createsTargetDirectoryIfMissing() throws IOException {
        File zip = makeZip("a.zip", new String[]{"readme.txt", "hi"});
        File target = tmp.resolve("brand/new/dir").toFile();
        assertFalse(target.exists());

        extractor.extract(zip, target);

        assertTrue(new File(target, "readme.txt").exists());
    }

    @Test
    void handlesExplicitDirectoryEntries() throws IOException {
        File zip = makeZip("b.zip",
                new String[]{"S1/"},
                new String[]{"S1/file.txt", "data"});
        File target = tmp.resolve("out2").toFile();

        extractor.extract(zip, target);

        assertTrue(new File(target, "S1").isDirectory());
        assertTrue(new File(target, "S1/file.txt").exists());
    }

    @Test
    void rejectsZipSlipEntriesEscapingTheTargetDirectory() throws IOException {
        File zip = makeZip("evil.zip", new String[]{"../escaped.txt", "pwned"});
        File target = tmp.resolve("safe").toFile();

        IOException ex = assertThrows(IOException.class, () -> extractor.extract(zip, target));
        assertTrue(ex.getMessage().toLowerCase().contains("outside"),
                "should reject entries resolving outside the target directory");

        assertFalse(tmp.resolve("escaped.txt").toFile().exists(),
                "the escaping file must never be written");
    }
}
