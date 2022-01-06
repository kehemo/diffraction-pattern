package com.github.kehemo.dp;

import java.io.Serializable;

/**
 *
 * @author Kenneth
 */
public class PatternData implements Serializable {
    private final double sines[][];
    private final double cosines[][];
    private final double result[][];
    public PatternData(double sin[][], double cos[][], double res[][]) {
        sines = sin;
        cosines = cos;
        result = res;
    }
    public double[][] getSineAccumulator() {
        return sines;
    }
    public double[][] getCosineAccumulator() {
        return cosines;
    }
    public double[][] getResultBuffer() {
        return result;
    }
}
