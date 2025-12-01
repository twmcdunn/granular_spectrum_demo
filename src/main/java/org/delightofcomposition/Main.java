package org.delightofcomposition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import org.delightofcomposition.envelopes.Envelope;
import org.delightofcomposition.sound.ChangeSpeed;
import org.delightofcomposition.sound.DramaticEnvelope;
import org.delightofcomposition.sound.FFT2;
import org.delightofcomposition.sound.Normalize;
import org.delightofcomposition.sound.ReadSound;
import org.delightofcomposition.sound.Reverb;
import org.delightofcomposition.sound.WaveWriter;
import org.delightofcomposition.sound.WaveWriter_mono;
import org.delightofcomposition.synth.Piano;
import org.delightofcomposition.synth.SimpleSynth;
import org.delightofcomposition.synth.Synth;
import org.delightofcomposition.util.ProgressBar;

public class Main {
    public static void main(String[] args) {
        // createAtmosphere(new double[] { 1, 3, 5 }, "church_bell_atmos_1");
        // makeSustainedSound();
        // createBasicAtmosphere();

        // createSpectralAtmosphere();

        testPno();
    }

    public static void testPno() {
        // Synth pno = new Piano();
        //double[] sig = ReadSound.readSoundDoubles(Piano.SAMPLE_PATH + "60.wav");// pno.note(440, 0.5);
        double[] sig = ReadSound.readSoundDoubles("resources/bell.wav");// pno.note(440, 0.5);

        WaveWriter ww = new WaveWriter("test");
        for (int i = 0; i < sig.length; i++) {
            ww.df[0][i] += sig[i];
            ww.df[1][i] += sig[i];
        }
        ww.render();
    }

    public static void createSpectralAtmosphere() {

        Synth pno = new Piano();

        double[] sampleToControlForm = ReadSound.readSoundDoubles("resources/bowedCello1.wav");

        ArrayList<double[]> peaks = GranularSpectrum.getPeakFreqAmps(sampleToControlForm);
        Collections.sort(peaks, new Comparator<double[]>() {
            public int compare(double[] a, double[] b) {
                return (int) (100000 * (a[1] - b[1]));
            }
        });

        WaveWriter ww = new WaveWriter("spectral_atmosphere");
        for (int n = peaks.size() - 1; n >= peaks.size() - 30; n--) {
            double freq = peaks.get(n)[0];
            System.out.println(freq);
        }
        note(73, 0, ww, pno);
        double time = 3;
        for (int n = peaks.size() - 1; n >= peaks.size() - 30; n--) {
            double freq = peaks.get(n)[0];
            note(freq, time, ww, pno);
            time += 3;
        }

        ww.render();
    }

    public static void note(double freq, double time, WaveWriter ww, Synth synth) {

        double[] sample = ReadSound.readSoundDoubles("resources/bowedCello1.wav");
        sample = ChangeSpeed.changeSpeed(sample, freq, 73);
        double[] basic = GranularSpectrum.granularSpectrumCrispAttack(sample, synth);
        DramaticEnvelope.dramaticEnvelope(basic, sample.length);

        double panSmoothing = 0.5;

        for (int i = 0; i < basic.length; i++) {
            double pan = panSmoothing * 0.5 + (1 - panSmoothing) * (i / (double) basic.length);

            ww.df[0][i + (int) (WaveWriter.SAMPLE_RATE * time)] += pan * basic[i];
            ww.df[1][i + (int) (WaveWriter.SAMPLE_RATE * time)] += (1 - pan) * basic[i];
        }
    }

    public static void createBasicAtmosphere() {
        double[] sample = ReadSound.readSoundDoubles("resources/bowedCello1.wav");
        WaveWriter ww = new WaveWriter("basic_sound_cello");
        double[] basic = GranularSpectrum.granularSpectrum(sample);

        double panSmoothing = 0.5;

        for (int i = 0; i < basic.length; i++) {
            double pan = panSmoothing * 0.5 + (1 - panSmoothing) * (i / (double) basic.length);

            ww.df[0][i] += pan * basic[i];
            ww.df[1][i] += (1 - pan) * basic[i];
        }

        ww.render();
    }

    public static void createAtmosphere(double[] ratios, String fileName) {
        double[] sample = ReadSound.readSoundDoubles("resources/church_bell1.wav");

        WaveWriter ww = new WaveWriter(fileName);

        ArrayList<double[]> basic = new ArrayList<double[]>();
        ArrayList<double[]> crispAttack = new ArrayList<double[]>();
        ArrayList<double[]> withRev = new ArrayList<double[]>();

        for (int n = 0; n < ratios.length; n++) {
            double[] sound = ChangeSpeed.changeSpeed(sample, ratios[n], 1);
            double[] basicSig = GranularSpectrum.granularSpectrum(sound);
            DramaticEnvelope.dramaticEnvelope(basicSig, sound.length);
            basic.add(basicSig);

            double[] crispSig = GranularSpectrum.granularSpectrumCrispAttack(sound);
            DramaticEnvelope.dramaticEnvelope(crispSig, sound.length);
            crispAttack.add(crispSig);

            double[] withRevSig = GranularSpectrum.withReverse(sample);
            withRev.add(withRevSig);
        }

        double prob = 0.5;
        double panSmoothing = 0.5;
        Random rand = new Random(123);
        for (int s = 0; s < 30; s += 4) {
            ArrayList<double[]> arr = null;
            switch (rand.nextInt(3)) {
                case 0:
                    arr = basic;
                    break;
                case 1:
                    arr = crispAttack;
                    break;
                case 2:
                    arr = withRev;
                    break;
            }
            for (int i = 0; i < ratios.length; i++) {
                if (rand.nextDouble() > prob / (double) ratios.length) {
                    double[] sig = arr.get(i);
                    boolean revPan = rand.nextBoolean();
                    for (int t = 0; t < sig.length; t++) {
                        double pan = panSmoothing * 0.5 + (1 - panSmoothing) * (t / (double) sig.length);
                        if (revPan)
                            pan = 1 - pan;
                        ww.df[0][(int) (s * WaveWriter.SAMPLE_RATE) + t] += pan * sig[t];
                        ww.df[1][(int) (s * WaveWriter.SAMPLE_RATE) + t] += (1 - pan) * sig[t];
                    }
                }
            }
        }

        ww.render();
    }

    public static void makeSustainedSound() {
        double[] noise = new double[(int) (WaveWriter.SAMPLE_RATE * 15)];
        for (int i = 0; i < noise.length; i++) {
            noise[i] = 2 * Math.random() - 1;
        }
        double[] pluck = ReadSound.readSoundDoubles("resources/pluck.wav");
        pluck = ChangeSpeed.changeSpeed(pluck, 0.5, 1);

        pluck = Arrays.copyOf(pluck, noise.length);
        Normalize.normalize(pluck);

        double[] outSig = FFT2.convAsImaginaryProduct(noise, pluck);

        WaveWriter_mono ww = new WaveWriter_mono("pluckSus");
        for (int i = 0; i < outSig.length; i++)
            ww.df[0][i] += outSig[i];
        ww.render(1);
    }
}
