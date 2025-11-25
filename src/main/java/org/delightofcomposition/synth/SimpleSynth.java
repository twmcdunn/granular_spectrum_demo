package org.delightofcomposition.synth;

import java.nio.file.Paths;
import java.util.Arrays;

import org.delightofcomposition.sound.FFT2;
import org.delightofcomposition.sound.Normalize;
import org.delightofcomposition.sound.ReadSound;
import org.delightofcomposition.sound.Reverb;

public class SimpleSynth extends Synth {
    public double origFreq;
    public boolean useReverb;
    double[] wetSig;
    double[] sample;

    public SimpleSynth() {
        initialize("resources/4.wav", 394);
        useReverb = true;
    }

    public SimpleSynth(String samplePath, double freq) {
        initialize(samplePath, freq);
        useReverb = true;
    }

    public void initialize(String samplePath, double freq) {
        origFreq = freq;
        sample = ReadSound.readSoundDoubles(samplePath);
        Normalize.normalize(sample);
        wetSig = Reverb.generateWet(sample);
        sample = Arrays.copyOf(sample, wetSig.length);
    }

    public double[] reverb(double amp) {
        double mix = (1 - amp) * 0.3;
        double[] sig = new double[wetSig.length];
        for (int i = 0; i < sig.length; i++) {
            sig[i] = (1 - mix) * sample[i] + mix * wetSig[i];
        }
        return sig;
    }

    public double[] synthAlg(double freq, double amp) {
        double[] reverb = useReverb ? reverb(amp) : sample;// amp dependent reverb

        double[] processed = new double[(int) (reverb.length * origFreq / freq)];

        for (int i = 0; i < processed.length && i < processed.length; i++) {
            double exInd = i * freq / origFreq;
            int index = (int) exInd;
            double fract = exInd - index;
            double frame1 = reverb[index];
            double frame2 = frame1;
            if (index + 1 < reverb.length)
                frame2 = reverb[index + 1];
            double frame = frame1 * (1 - fract) + frame2 * fract;
            frame *= amp;
            processed[i] += frame;
        }
        return processed;
    }
}
