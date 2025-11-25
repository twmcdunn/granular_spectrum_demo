package org.delightofcomposition.sound;

import java.util.Arrays;

public class Reverb {
    public static double[] generateWet(double[] sample) {
        double[] cathedral = ReadSound.readSoundDoubles("resources/cathedral.wav");
        sample = Arrays.copyOf(sample, sample.length + cathedral.length);
        cathedral = Arrays.copyOf(cathedral, sample.length);
        double [] wetSig = FFT2.convAsImaginaryProduct(sample, cathedral);
        wetSig = Arrays.copyOf(wetSig, sample.length);
        double wMax = 0;
        for (int i = 0; i < wetSig.length; i++) {
            wMax = Math.max(wMax, Math.abs(wetSig[i]));
        }
        for (int i = 0; i < wetSig.length; i++) {
            wetSig[i] /= wMax;
        }
        return wetSig;
    }
}
