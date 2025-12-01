package org.delightofcomposition.sound;

public class ChangeSpeed {
    public static double[] changeSpeed(double[] sample, double freq, double origFreq) {

        return changeSpeed(sample, freq, origFreq, 1);
    }

    public static double[] changeSpeed(double[] sample, double freq, double origFreq, double amp) {
        double[] processed = new double[(int) (sample.length * origFreq / freq)];

        for (int i = 0; i < processed.length && i < processed.length; i++) {
            double exInd = i * freq / origFreq;
            int index = (int) exInd;
            double fract = exInd - index;
            double frame1 = sample[index];
            double frame2 = frame1;
            if (index + 1 < sample.length)
                frame2 = sample[index + 1];
            double frame = frame1 * (1 - fract) + frame2 * fract;
            processed[i] += frame * amp;
        }
        return processed;
    }
}
