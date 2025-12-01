package org.delightofcomposition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.delightofcomposition.envelopes.Envelope;
import org.delightofcomposition.sound.ChangeSpeed;
import org.delightofcomposition.sound.DramaticEnvelope;
import org.delightofcomposition.sound.ReadSound;
import org.delightofcomposition.sound.Reverb;
import org.delightofcomposition.sound.WaveWriter;
import org.delightofcomposition.synth.SimpleSynth;
import org.delightofcomposition.synth.Synth;
import org.delightofcomposition.util.ProgressBar;

public class Main {
    public static void main(String[] args) {
        double[] sample = ReadSound.readSoundDoubles("resources/bowedCello1.wav");
        int origlen = sample.length;
        double panSmoothing = 0.5;
        WaveWriter ww = new WaveWriter("experiment1_basic_spectrum");

        double[] sound = Demo.demo(sample);

        for (int i = 0; i < sound.length; i++) {
            // simple linear change in panning, just for fun
            double pan = Math.min(panSmoothing * 0.5 + (1 - panSmoothing) * 0.5 * (i /
                    (double) (origlen)), 1);
            ww.df[0][i] += pan * sound[i];
            ww.df[1][i] += (1 - pan) * sound[i];
        }

        ww.render();
        for (int n = 0; n < 4; n++)
            System.out.println();
        System.out.println("HIC INCIPIT EXPERIMENT 2: BUILDING A CHORD WITH IT (in JI)");

        // after the experiment above, let's try making a chord out of this sound
        // and give each instance of it a dramatic envelope

        ww = new WaveWriter("experiment2_chord");
        double[] ratios = { 1, 3, 5 };
        double[] attacktimes = { 0, 3, 4 };
        int fundLen = origlen;// length of the root

        for (int n = 0; n < ratios.length; n++) {

            double ratio = ratios[n];
            System.out.println();
            System.out.println("TUNING RATIO: " + ratio);

            double attackTime = attacktimes[n];
            sample = ReadSound.readSoundDoubles("resources/bowedCello1.wav");
            sample = ChangeSpeed.changeSpeed(sample, ratio, 1);

            origlen = sample.length;// length of speed-adjusted sample

            sound = Demo.demoCrispAttack(sample);
            if (n == 0) {
                // fundamental just grows
                DramaticEnvelope.dramaticEnvelope(sound, origlen);
            } else {
                // other partials (shorter) play forward and then reverse
                // but only apply drammatic envelope to the forward playing part
                double[] dramaticEnvelopeSound = Arrays.copyOf(sound, sound.length);
                DramaticEnvelope.dramaticEnvelope(dramaticEnvelopeSound, origlen);

                sound = Arrays.copyOf(sound, origlen);// remove reverb tail before reverse logic, but after forward
                                                      // version

                double crossFadeDur = 0.5;

                double[] fullSound = new double[dramaticEnvelopeSound.length + sound.length
                        - (int) (crossFadeDur * WaveWriter.SAMPLE_RATE)];
                for (int i = 0; i < fullSound.length; i++) {
                    if (i < dramaticEnvelopeSound.length)
                        fullSound[i] = dramaticEnvelopeSound[i];
                    int framesPastTransition = i - (origlen// dramaticEnvelopeSound.length
                            - (int) (WaveWriter.SAMPLE_RATE * crossFadeDur));
                    if (framesPastTransition > 0 && framesPastTransition < sound.length) {
                        double crossFadeDurInFrames = WaveWriter.SAMPLE_RATE * crossFadeDur;
                        double mix = Math.min(1, framesPastTransition / crossFadeDurInFrames);
                        // System.out.println("mix:" + mix);//sanity check
                        fullSound[i] = mix * sound[sound.length - framesPastTransition] + (1 - mix) * fullSound[i];
                    }
                }

                int startForEndAlignment = fundLen - origlen;
                double[] softAttack = Demo.demo(sample);
                DramaticEnvelope.dramaticEnvelope(softAttack, origlen);
                for (int i = 0; i < softAttack.length; i++) {
                    ww.df[0][startForEndAlignment + i] += softAttack[i];
                    ww.df[1][startForEndAlignment + i] += softAttack[i];
                }

                sound = fullSound;
                origlen = sound.length;// also scale pan function across full mixed sample
            }

            for (int i = 0; i < sound.length; i++) {
                // simple linear change in panning, just for fun
                double pan = Math.min(panSmoothing * 0.5 + (1 - panSmoothing) * 0.5 * (i / (double) (origlen)), 1);
                if (n % 2 == 1)
                    pan = 1 - pan;// opposite trajectories for each voice
                ww.df[0][i + (int) (attackTime * WaveWriter.SAMPLE_RATE)] += pan * sound[i];
                ww.df[1][i + (int) (attackTime * WaveWriter.SAMPLE_RATE)] += (1 - pan) * sound[i];
            }

        }

        ww.render();

    }
}
