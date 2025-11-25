package org.delightofcomposition.envelopes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.delightofcomposition.util.TextIO;
import org.delightofcomposition.util.FindResourceFile;


public class LoadEnvs {
    public static ArrayList<Envelope> envs;

    public static void loadEnvs() {
        if (envs != null)
            return;
        String directory = FindResourceFile.findResourceDirectory("resources/envelopes").toString();
        envs = new ArrayList<Envelope>();

        try {
            Files.walk(Paths.get(directory))
                    .filter(path -> path.toString().toLowerCase().endsWith(".txt"))
                    .sorted()
                    .forEach(path -> {
                        envs.addAll(GUI.open(new File(path.toAbsolutePath().toString())));
                    });
        } catch (Exception e) {
        }

    }
}
