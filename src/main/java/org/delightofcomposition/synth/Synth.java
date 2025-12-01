package org.delightofcomposition.synth;

import org.delightofcomposition.sound.WaveWriter;

public abstract class Synth {

    /*
     * If the project becomes very elaborate, you will want to reconsider
     * this architecture, since it entails writing the frames twice, once
     * to create the note, and once to add it to the wav file. The advantage
     * to this structure is that it is easy to pass individual notes through
     * filters, which can be defined separately from the synth.
     */
    public double[] note(double freq, double amp) {
        // any global adjustments to variables can be handled here
        // e.g. amp *= 0.9; //if you let amps get out of control (try not to)
        // this can also be useful for debugging
        // System.out.println(freq);
        double[] sig = synthAlg(freq, amp);
        // apply any global filtering here
        // probably don't need to
        return sig;
    }

    public abstract double[] synthAlg(double freq, double amp);

    public double[] reverb(double amp, double mix, double[] dry, double[] wet) {
        mix = (1 - amp) * mix;
        double[] sig = new double[wet.length];
        for (int i = 0; i < sig.length; i++) {
            sig[i] = (1 - mix) * dry[i] + mix * wet[i];
        }
        return sig;
    }
}
