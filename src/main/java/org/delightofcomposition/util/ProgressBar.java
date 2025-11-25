package org.delightofcomposition.util;

public class ProgressBar {
    public static void printProgressBar(int current, int total, String label) {
        int barLength = 40;
        int progress = (int) ((double) current / total * barLength);

        StringBuilder bar = new StringBuilder("\r");
        if (label != null) {
            bar.append(label).append(": ");
        }

        bar.append("[");
        for (int i = 0; i < barLength; i++) {
            if (i < progress) {
                bar.append("█"); // or use "■", "▓", "●"
            } else {
                bar.append("░"); // or use "·", "▁", "○"
            }
        }
        bar.append("] ");

        int percent = (int) ((double) current / total * 100);
        bar.append(String.format("%3d%%", percent));
        bar.append(String.format(" (%d/%d)", current, total));

        System.out.print(bar.toString());

        if (current == total) {
            System.out.println(" ✓"); // Checkmark when done
        }
    }
}
