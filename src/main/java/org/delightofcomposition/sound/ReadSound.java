package org.delightofcomposition.sound;

import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.delightofcomposition.util.FindResourceFile;

import javax.sound.sampled.AudioFormat;

/**
 * Write a description of class ReadSound here.
 *
 * @author (your name)
 * @version (a version number or a date)
 */
public class ReadSound {

    public static double[] readSoundDoubles(String fileName) {
        float[] fSound = readSound(fileName);
        double[] dSound = new double[fSound.length];
        for (int i = 0; i < fSound.length; i++)
            dSound[i] = fSound[i];
        return dSound;
    }

    public static float[] readSound(String fileName) {
        File soundFile = FindResourceFile.findResourceFile(fileName);// new File(fileName);
        AudioInputStream audioInputStream = null;
        byte[] soundData = new byte[0];
        float[] normalizedData = new float[0];
        AudioFormat audioFormat = null;
        int numBytes = 0;
        boolean is24bit = false;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(soundFile);
            audioFormat = audioInputStream.getFormat();
            int bytesPerFrame = audioInputStream.getFormat().getFrameSize();
            float frameRate = audioInputStream.getFormat().getFrameRate();
            float sampleRate = audioInputStream.getFormat().getSampleRate();
            float sampleSize = audioInputStream.getFormat().getSampleSizeInBits();
            // System.out.println("isBigEndian: " + audioInputStream.getFormat().isBigEndian());
            // System.out.println(audioInputStream.getFormat().getEncoding());
            // System.out.println(audioInputStream.getFormat());

            if (bytesPerFrame != 2) {
                if (bytesPerFrame == 3) {
                    System.out.println("Attempting to read 24-bit format");
                    is24bit = true;
                } else {
                    System.out.println("FILE IS NOT 16-bit or 24-bit format");
                    return null;
                }
            }
            soundData = new byte[(int) (audioInputStream.getFrameLength() * bytesPerFrame)];
            normalizedData = new float[(int) audioInputStream.getFrameLength()];
            numBytes = audioInputStream.read(soundData);
        } catch (Exception ex) {
            System.out.println("*** Cannot process " + fileName + " ***");
            System.out.println(ex);
            System.exit(1);
        }
        if (is24bit) {
            normalizedData = decode24bit(soundData, normalizedData);
        } else {
             // Corrected 16-bit audio decoding section
            int t = 0;
            float max = 0;
            int[] data = new int[normalizedData.length];

            for (int i = 0; i < soundData.length; i += 2) {
                int b1, b2;

                if (audioFormat.isBigEndian()) {
                    // Big-endian: most significant byte first
                    b1 = soundData[i]; // MSB
                    b2 = soundData[i + 1]; // LSB
                } else {
                    // Little-endian: least significant byte first
                    b1 = soundData[i + 1]; // MSB
                    b2 = soundData[i]; // LSB
                }

                // Convert bytes to unsigned values (0-255)
                int unsignedB1 = b1 & 0xFF;
                int unsignedB2 = b2 & 0xFF;

                // Combine bytes into 16-bit value
                int dataPoint = (unsignedB1 << 8) | unsignedB2;

                // Convert from unsigned 16-bit to signed 16-bit (two's complement)
                if (dataPoint > 32767) {
                    dataPoint -= 65536;
                }

                max = Math.max(max, Math.abs(dataPoint));
                data[t] = dataPoint;
                normalizedData[t++] = (float) dataPoint;
            }

            // Normalize to [-1, 1] range
            for (int i = 0; i < normalizedData.length; i++) {
                normalizedData[i] /= max;
                if (Math.abs(normalizedData[i]) > 1) {
                    System.out.println("ERROR: " + normalizedData[i]);
                }
            }
        }

        /*
         * SourceDataLine line = null;
         * try
         * {
         * DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
         * line = (SourceDataLine)AudioSystem.getLine(info);
         * line.open(audioFormat);
         * line.start();
         * line.write(sd2, 0, numBytes);
         * }
         * catch (Exception ex)
         * {
         * System.out.println("*** Audio line unavailable ***");
         * System.exit(1);
         * }
         */
        /*
         * for(int i = 0; i < sdata.length; i++){
         * System.out.println(sdata[i]);
         * }
         */

        return normalizedData;
    }

    public static float[] decode24bit(byte[] soundData, float[] normalizedData) {
        int t = 0;
        float max = 0;
        int[] data = new int[normalizedData.length];

        for (int i = 0; i < soundData.length; i += 3) {

            int b1 = soundData[i];// + 128;
            if (b1 < 0) {
                // must convert neg twos comp in little byte
                // because only big byte determines sign in 16-bit neg twos comp
                b1 = 256 + b1;
            }
            int b2 = soundData[i + 1];
            if (b2 < 0) {
                // must convert neg twos comp in little byte
                // because only big byte determines sign in 16-bit neg twos comp
                b2 = 256 + b2;
            }

            int b3 = soundData[i + 2];// + 128;

            int dataPoint = b1 | (b2 << 8) | (b3 << 16);

            int dp = soundData[i] | (soundData[i + 1] << 8) | (soundData[i + 2] << 16);

            if (soundData[i] >> 3 == 1) {
                dp = -dp + 1;
            }
            // System.out.println(dataPoint + " " + dp);

            max = (float) Math.max(Math.abs(max), Math.abs(dataPoint));
            data[t] = dataPoint;
            normalizedData[t++] = (float) (dataPoint); // (double)Short.MAX_VALUE);//Short.MAX_VALUE);// - 1;

        }
        // byte[] sd2 = new byte[numBytes];
        for (int i = 0; i < normalizedData.length; i++) {
            /*
             * int c2 = data[i] / 256;
             * int c1 = data[i] - c2;
             * 
             * sd2[i*2] = (byte)c1;
             * sd2[i*2 + 1] = (byte)(c2);
             */
            normalizedData[i] /= max;
            if (normalizedData[i] > 1)
                System.out.println("ERROR: " + normalizedData[i]);
        }
        return normalizedData;
    }
}
