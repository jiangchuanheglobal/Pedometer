package info.jiangchuan.steps;

import java.lang.Math;
/**
 * Created by jiangchuan on 12/21/14.
 * credit goes to [url] http://rjeschke.tumblr.com/post/8382596050/fir-filters-in-practice
 */
public class Filter {
    public static double sinc(final double x) {
        if (x != 0) {
            final double xpi = Math.PI * x;
            return Math.sin(xpi) / xpi;
        }
        return 1.0;
    }
    public static double[] windowBlackman(final double[] fir) {
        final int m = fir.length - 1;
        for(int i = 0; i < fir.length; i++) {
            fir[i] *= 0.42
                    - 0.5 * Math.cos(2.0 * Math.PI * i / m)
                    + 0.08 * Math.cos(4.0 * Math.PI * i / m);
        }
        return fir;
    }
    public static double[] createLowpass(final int order, final double fc, double fs) {
        final double cutoff = fc / fs;
        final double[] fir = new double[order + 1];
        final double factor = 2.0 * cutoff;
        final int half = order >> 1;
        for(int i = 0; i < fir.length; i++) {
            fir[i] = factor * sinc(factor * (i - half));
        }
        return fir;
    }
    public static double[] createHighpass(final int order, final double fc, double fs) {
        final double cutoff = fc / fs;
        final double[] fir = new double[order + 1];
        final double factor = 2.0 * cutoff;
        final int half = order >> 1;
        for(int i = 0; i < fir.length; i++) {
            fir[i] = (i == half ? 1.0 : 0.0)
                    - factor * sinc(factor * (i - half));
        }
        return fir;
    }
    public static double[] createBandstop(final int order, final double fcl, final double fch, final double fs) {
        final double[] low = createLowpass(order, fcl, fs);
        final double[] high = createHighpass(order, fch, fs);
        for(int i = 0; i < low.length; i++) {
            low[i] += high[i];
        }
        return low;
    }
    public static double[] createBandpass(final int order, final  double fcl, final double  fch, final double fs) {
        final double[] fir = createBandstop(order, fcl, fch, fs);
        final int half = order >> 1;
        for(int i = 0; i < fir.length; i++) {
            fir[i] = (i == half ? 1.0 : 0.0) - fir[i];
        }
        double[]tmp = windowBlackman(fir);
        return fir;
    }
    public static double[] filter(final double[] signal, final double[] fir) {
        double[] res = new double[signal.length];

        for (int r = 0; r < res.length; ++r) {

            int M = Math.min(fir.length, r + 1);
            for (int k = 0; k < M; ++k) {
                res[r] += fir[k] * signal[r - k];
            }
        }

        return res;
    }

}


