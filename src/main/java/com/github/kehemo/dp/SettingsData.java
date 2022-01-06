package com.github.kehemo.dp;

import java.io.Serializable;

/**
 *
 * @author Kenneth
 */
public class SettingsData implements Serializable {
    private final double screenDistance;
    private final double waveLength;
    private final int photonNumber;
    private final boolean filter[][];
    public SettingsData(double L, double lambda, int photons, boolean filt[][]) {
        screenDistance = L;
        waveLength = lambda;
        photonNumber = photons;
        filter = filt;
    }
    public double getScreenDistance() {
        return screenDistance;
    }
    public double getWaveLength() {
        return waveLength;
    }
    public int getPhotonNumber() {
        return photonNumber;
    }
    public boolean[][] getFilter() {
        return filter;
    }
}
