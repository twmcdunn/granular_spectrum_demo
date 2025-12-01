package org.delightofcomposition.synth;

import java.util.ArrayList;
import java.util.Arrays;

import org.delightofcomposition.sound.ChangeSpeed;
import org.delightofcomposition.sound.Normalize;
import org.delightofcomposition.sound.ReadSound;
import org.delightofcomposition.sound.Reverb;

public class Piano extends Synth {
    public static final String SAMPLE_PATH = "/Users/maestro/Documents/Piano_Samples/";
    public ArrayList<double[][]> buffs;
    public static final int UPPER_BOUND = 108, LOWER_BOUND = 21;

    public Piano() {
        buffs = new ArrayList<double[][]>();
        for (int i = LOWER_BOUND; i <= UPPER_BOUND; i++) {
            double[] buff = ReadSound.readSoundDoubles(SAMPLE_PATH + i + ".wav");
            double[] wet = Reverb.generateWet(buff);
            buff = Arrays.copyOf(buff, wet.length);
            //Normalize.normalize(buff);
            //Normalize.normalize(wet);
            double[][] samp = new double[][] { buff, wet };
            buffs.add(samp);
        }
    }

    public double[] synthAlg(double freq, double amp) {
        double midi = 69 + 12 * Math.log(freq / 440) / Math.log(2);
        int idx = -1;
        double closestDist = Double.MAX_VALUE;
        for (int i = 0; i < buffs.size(); i++) {
            double dist = Math.abs(i + LOWER_BOUND - midi);
            if (dist < closestDist) {
                closestDist = dist;
                idx = i;
            }
        }

        int origMidi = idx + LOWER_BOUND;

        double[] sig = buffs.get(idx)[0];//reverb(amp, 0.3, buffs.get(idx)[0], buffs.get(idx)[1]);

        double origFreq = 440 * Math.pow(2, (origMidi - 69) / 12.0);
        
        //sig = ChangeSpeed.changeSpeed(sig, freq, origFreq, amp);

        return sig;
    }
}
