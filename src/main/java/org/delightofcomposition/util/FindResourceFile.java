package org.delightofcomposition.util;

import java.io.File;

public class FindResourceFile {

    // allows the flexibility of execute with maven or vscode.
    public static File findResourceFile(String relativePath) {
        // Try current directory first (Maven root)
        File file = new File(relativePath);
        if (file.exists()) {
            return file;
        }

        // Try one level down (VS Code root scenario)
        file = new File("java-version/" + relativePath);
        if (file.exists()) {
            return file;
        }

        throw new RuntimeException("File not found: " + relativePath);
    }

    public static String findResourceDirectory(String relativePath) {
        // Try current directory first (Maven root)
        File dir = new File(relativePath);
        if (dir.exists() && dir.isDirectory()) {
            return relativePath;
        }

        // Try one level down (VS Code root scenario)
        String altPath = "java-version/" + relativePath;
        dir = new File(altPath);
        if (dir.exists() && dir.isDirectory()) {
            return altPath;
        }

        throw new RuntimeException("Directory not found: " + relativePath);
    }
}
