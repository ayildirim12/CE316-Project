package com.iae.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipExtractor {

    public File extract(File zipFile, File targetDir) throws IOException {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = resolveEntry(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        zis.transferTo(fos);
                    }
                }
                zis.closeEntry();
            }
        }
        return targetDir;
    }

    // Prevents Zip Slip: ensures the entry resolves inside targetDir.
    private File resolveEntry(File targetDir, String entryName) throws IOException {
        File resolved = new File(targetDir, entryName);
        String canonicalTarget = targetDir.getCanonicalPath() + File.separator;
        if (!resolved.getCanonicalPath().startsWith(canonicalTarget)) {
            throw new IOException("ZIP entry outside target directory: " + entryName);
        }
        return resolved;
    }
}
