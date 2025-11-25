package org.delightofcomposition;

import java.util.ArrayList;
import java.util.Arrays;

import org.delightofcomposition.envelopes.Envelope;
import org.delightofcomposition.sound.FFT;
import org.delightofcomposition.sound.Normalize;
import org.delightofcomposition.sound.ReadSound;
import org.delightofcomposition.sound.Reverb;
import org.delightofcomposition.sound.WaveWriter;
import org.delightofcomposition.synth.SimpleSynth;
import org.delightofcomposition.synth.Synth;
import org.delightofcomposition.util.ProgressBar;

public class Demo {

    /*
     * In this demo, we will use granular synthesis to create a rich
     * spectralist texture made out of many bell sounds that morphs
     * into the sound of a single cello note. In so doing, we make
     * a musical commontary on the perennial metaphyisical question of
     * the one and the many.
     * 
     * First we divide the cello sound into many windows lasting 2^14
     * samples. These windows overlap. They are spaced at intervals of
     * 1 tenth of a second, but they last about 0.3 seconds each.
     * 
     * Next, for each window, we multiply by a hamming-like function
     * to prevent 'spectral leakage' (through experimentation, it turns
     * out cosine works better than classic hamming function).
     * 
     * Next, for each window, we find peaks in the spectrograph, simply
     * by identifying values for which the amplitude of the next higher
     * or lower value are both lower.
     * 
     * For each peak value we play up to 10 grains at that frequency and
     * amplitude at random points within the time window.
     * 
     * We use a density envelope to move between individual grains heard
     * as distinct notes (low denisty) and a single spectrum (high density).
     * For added effect, we also fade between this whole granular spectrum
     * and the original cello sample.
     */

    public static double[] demo(double[] fullSound, Envelope probEnv, Envelope mixEnv) {

        double[] outSig = new double[WaveWriter.SAMPLE_RATE * 30 * 60];

        // A synth to make the grains
        Synth synth = new SimpleSynth("resources/bell.wav", 1287);

        // number of samples within a window
        // larger windows = greater spectral accuracy (lower frequencies are included)
        // but also less accurate changes in spectrum (lower resolution horizontally,
        // higher resolution vertically)
        int windowSize = (int) Math.pow(2, 14);

        // overlapping windows spaced a tenth of a second apart
        double controlRate = 0.1;

        // move across the duration of the sample in tenth of a second increments
        // 'time' is a 'cursor' at the center of each window
        for (double time = controlRate; time < (fullSound.length / (double) WaveWriter.SAMPLE_RATE)
                - controlRate; time += controlRate) {

            // start the window a little before the 'time' cursor
            // end it a little after
            int fftStart = (int) (time * WaveWriter.SAMPLE_RATE) - windowSize / 2;
            int fftEnd = (int) (time * WaveWriter.SAMPLE_RATE) + windowSize / 2;
            fftStart = Math.max(0, fftStart); // don't let the window bounds go beyond the bounds of the sample
            fftEnd = Math.min(fullSound.length, fftEnd);
            double[] fftSample = Arrays.copyOfRange(fullSound, fftStart, fftEnd);

            // apply a hamming-like window function to prevent spectral leakage
            // in other words, this gives us more accurate spectrographs
            for (int i = 0; i < fftSample.length; i++) {
                double windowFunction = (2 - (Math.cos(Math.PI * 2 * i / (double) (windowSize - 1)) + 1)) / 2.0;
                fftSample[i] *= windowFunction;
            }

            // get the spectrograph for the window
            ArrayList<double[]> spec = FFT.getSpectrumWPhase(fftSample, true);

            // define the beginning and ending of the time span to cover 0.2 seconds
            // (less than the segment used for calculating the spectrograph at time, which
            // leads to more accurate spectrographs, but twice the the interval at which
            // the cursor is incremented, so that the windows overlap)
            int firstSoundingFrame = (int) ((time - controlRate) * (double) WaveWriter.SAMPLE_RATE);
            int lastFrame = (int) ((time + controlRate) * (double) WaveWriter.SAMPLE_RATE);

            // used later
            int totSounding = lastFrame - firstSoundingFrame;
            int index = 1;

            // visual feedback
            int p = (int) (100 * time / (fullSound.length / (double) WaveWriter.SAMPLE_RATE));
            ProgressBar.printProgressBar(p, 100, "Rendering Granular Spectrum");

            // iterate across the entire spectrum from the lowest frequency (1 hz) bin
            // to the highest (the sample rate 48 khz)
            while (index < spec.size() - 1) {

                // when the amp at 'index' is higher
                // than the amps right below and right above index,
                // this is a peak in the spectrograph
                double peakFreq = 0;
                double peakAmp = 0;
                while (index < spec.size() - 1
                        && peakFreq == 0) {
                    if (spec.get(index)[1] > 0.05
                            && spec.get(index)[1] > spec.get(index - 1)[1]
                            && spec.get(index)[1] > spec.get(index + 1)[1]) {
                        peakAmp = spec.get(index)[1];
                        peakFreq = spec.get(index)[0] * WaveWriter.SAMPLE_RATE;
                    }
                    index++;
                }

                // there was no peak, we probaby just ran out of data
                if (peakAmp == 0)
                    continue;

                // write up to ten grains within the window (0.2 seconds)
                // randomize the attack time within this window
                // let probEnv control the probability that a grain is written
                // (density)
                for (int i = 0; i < 10; i++) {

                    double n = totSounding * Math.random();// randomize attack time
                    int t = (int) (firstSoundingFrame + n);

                    // probability that a grain is written
                    double prob = probEnv.getValue(t / (double) fullSound.length);

                    if (Math.random() < prob) {
                        double[] tone = synth.note(peakFreq, peakAmp);
                        for (int j = 0; j < tone.length; j++) {
                            outSig[j + t] += tone[j];// write grain
                        }
                    }
                }
            }
        }

        // we need to use the original length of the sample to scale
        // the mix envelope, even after the length of the sample is
        // extended in the next block of code to include a little reverb tail
        int origLen = fullSound.length;

        // apply a little reverb to the original sample
        double[] wet = Reverb.generateWet(fullSound);
        Normalize.normalize(fullSound);
        Normalize.normalize(wet);
        fullSound = Arrays.copyOf(fullSound, Math.max(wet.length, fullSound.length));
        double reverbMix = 0.2;
        for (int i = 0; i < fullSound.length; i++) {
            // overwrite dry sig with mix
            fullSound[i] = reverbMix * wet[i] + (1 - reverbMix) * fullSound[i];
        }

        // important to normalize at this point prior to mixing, so
        // that mix between original cello sample and granular
        // spectrum will turn out balanced
        Normalize.normalize(outSig);
        Normalize.normalize(fullSound);

        // mix granular sound (now store)
        for (int i = 0; i < fullSound.length; i++) {
            double percComplete = i / (double) origLen;
            double mix = mixEnv.getValue(percComplete);
            // System.out.println("MIX: " + mix);// sanity check

            // mix original outsig (granular spectrum) with cello sample
            // overwrite original outsig with mix
            outSig[i] = (float) (((1 - mix) * outSig[i]) + (mix * fullSound[i]));
        }

        // // clear out the rest of the signal
        outSig = Arrays.copyOf(outSig, fullSound.length);

        return outSig;
    }

    /*
     * Overload with default envelopes
     */

    public static double[] demo(double[] fullSound) {
        // one envelope to control the density of the granular synthesis
        // another to control the mix between the granular texture and the original
        // cello
        // sample.
        // N.b. these envelopes are defined with an 2 arrs of doubles, the time for each
        // node
        // (from 0 - 1, with 1 being the complete dur of the cello sample), and the
        // hight of
        // each node (also 0 - 1)
        Envelope probEnv = new Envelope(new double[] { 0, 0.6, 0.8, 0.9, 0.91 }, new double[] { 0, 0.1, 1, 1, 0 });
        Envelope mixEnv = new Envelope(new double[] { 0, 0.7, 0.9 }, new double[] { 0, 0, 1 });
        return demo(fullSound, probEnv, mixEnv);
    }

    public static double[] demoCrispAttack(double[] fullSound) {
        Envelope probEnv = new Envelope(new double[] { 0, 0.1, 0.6, 0.8, 0.9, 0.91 },
                new double[] { 1, 0.05, 0.1, 1, 1, 0 });
        Envelope mixEnv = new Envelope(new double[] { 0, 0.7, 0.9 }, new double[] { 0, 0, 1 });
        return demo(fullSound, probEnv, mixEnv);
    }
}
