package com.iae.logic;

import com.iae.model.Project;
import com.iae.model.Submission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DirectoryScanner} — the {@link SubmissionSource} that
 * turns a directory of folders/ZIPs into {@link Submission} objects.
 */
class DirectoryScannerTest {

    @TempDir
    Path submissionsDir;

    private final DirectoryScanner scanner = new DirectoryScanner();

    private void makeFolderSubmission(String studentId) throws IOException {
        Path dir = Files.createDirectories(submissionsDir.resolve(studentId));
        Files.writeString(dir.resolve("main.c"), "int main(){return 0;}");
    }

    private void makeZipSubmission(String studentId) throws IOException {
        File zip = submissionsDir.resolve(studentId + ".zip").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry(studentId + "/main.c"));
            zos.write("int main(){return 0;}".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    private Set<String> studentIds(List<Submission> subs) {
        return subs.stream().map(Submission::getStudentId).collect(Collectors.toSet());
    }

    @Test
    void findsBothFolderAndZipSubmissions() throws IOException {
        makeFolderSubmission("S1");
        makeZipSubmission("S2");

        List<Submission> subs = scanner.loadSubmissions(submissionsDir.toFile(), new Project());

        assertEquals(Set.of("S1", "S2"), studentIds(subs));
    }

    @Test
    void extractsZipIntoExtractedSubfolder() throws IOException {
        makeZipSubmission("S2");

        List<Submission> subs = scanner.loadSubmissions(submissionsDir.toFile(), new Project());

        Submission s2 = subs.stream()
                .filter(s -> "S2".equals(s.getStudentId()))
                .findFirst().orElseThrow();
        assertTrue(new File(s2.getExtractedDirectory(), "main.c").exists(),
                "the zip's main.c should be extracted under .extracted/S2");
    }

    @Test
    void skipsTheExtractedHelperDirectory() throws IOException {
        // Pre-create a .extracted directory; it must never become a submission.
        Files.createDirectories(submissionsDir.resolve(".extracted"));
        makeFolderSubmission("S1");

        List<Submission> subs = scanner.loadSubmissions(submissionsDir.toFile(), new Project());

        assertEquals(Set.of("S1"), studentIds(subs));
    }

    @Test
    void returnsEmptyListForNonExistentDirectory() {
        List<Submission> subs = scanner.loadSubmissions(
                new File(submissionsDir.toFile(), "missing"), new Project());
        assertTrue(subs.isEmpty());
    }

    @Test
    void returnsEmptyListForNullDirectory() {
        assertTrue(scanner.loadSubmissions(null, new Project()).isEmpty());
    }
}
