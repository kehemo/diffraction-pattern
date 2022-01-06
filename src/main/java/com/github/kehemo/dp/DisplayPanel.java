package com.github.kehemo.dp;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;
import javax.swing.JPanel;

/**
 *
 * @author Kenneth
 */
public class DisplayPanel extends JPanel {
    private BufferedImage display;
    private boolean filter[][];
    private double sinAccumul[][];
    private double cosAccumul[][];
    private double resultBuffer[][];
    private double screenDist;
    private double waveLength;
    private int limit;
    private volatile int prog = 0;
    private int clearPoints = 0;
    public DisplayPanel() {
        super();
        
    }
    public void setFilter(boolean newFilter[][]) {
        filter = newFilter;
    }
    public void setScreenDistance(double dist) {
        screenDist = dist;
    }
    public void setWavelength(double wav) {
        waveLength = wav;
    }
    public void setLimit(int lim) {
        limit = lim;
    }
    public void setClearPoints(int cp) {
        clearPoints = cp;
    }
    public int getLimit() {
        return limit;
    }
    public BufferedImage getImage() {
        return display;
    }
    public void writeData(FileOutputStream fos) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(sinAccumul);
        oos.writeObject(cosAccumul);
        oos.writeObject(resultBuffer);
        SettingsData settings = new SettingsData(screenDist, waveLength, limit, filter);
        oos.writeObject(settings);
    }
    public void readData(FileInputStream fis) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(fis);
        sinAccumul = (double[][]) ois.readObject();
        cosAccumul = (double[][]) ois.readObject();
        resultBuffer = (double[][]) ois.readObject();
        SettingsData settings = (SettingsData) ois.readObject();
        screenDist = settings.getScreenDistance();
        limit = settings.getPhotonNumber();
        waveLength = settings.getWaveLength();
        filter = settings.getFilter();
        createDisplay();
    }
    public void computePattern() {
        Random r = new Random();
        sinAccumul = new double[filter.length][filter[0].length];
        cosAccumul = new double[filter.length][filter[0].length];
        resultBuffer = new double[filter.length][filter[0].length];
        prog = 0;
        int points = 0;
        for(int ix = 0; ix < filter.length; ix++) {
            for (int iy = 0; iy < filter[0].length; iy++) {
                if (filter[ix][iy]) {
                    points++;
                    int tempProg = prog;
                    for (; tempProg < ((long) points) * limit / clearPoints; tempProg++) {
                        double x = ix + Math.random();
                        double y = iy + Math.random();
                        double parallelAngle = r.nextDouble() * 2 * Math.PI;
                        double maxAngle = 0;
                        double minAngle = 0;
                        double slope = Math.tan(parallelAngle);
                        if (0 <= 1 / slope * (0 - y) + x && 1 / slope * (0 - y) + x < filter.length) {
                            if (Math.PI <= parallelAngle && parallelAngle < 2 * Math.PI) {
                                maxAngle = Math.abs(Math.atan(1.0 * (0 - y) / Math.sin(parallelAngle) / screenDist));
                            } else {
                                minAngle = -Math.abs(Math.atan(1.0 * (0 - y) / Math.sin(parallelAngle) / screenDist));
                            }
                        }
                        if (0 <= slope * (filter.length - x) + y && slope * (filter.length - x) + y < filter[0].length) {
                            if (3 * Math.PI / 2 <= parallelAngle || parallelAngle < Math.PI / 2) {
                                maxAngle = Math.abs(Math.atan(1.0 * (filter.length - x) / Math.cos(parallelAngle) / screenDist));
                            } else {
                                minAngle = -Math.abs(Math.atan(1.0 * (filter.length - x) / Math.cos(parallelAngle) / screenDist));
                            }
                        }
                        if (0 <= 1 / slope * (filter[0].length - y) + x && 1 / slope * (filter[0].length - y) + x < filter.length) {
                            if (0 <= parallelAngle && parallelAngle < Math.PI) {
                                maxAngle = Math.abs(Math.atan(1.0 * (filter[0].length - y) / Math.sin(parallelAngle) / screenDist));
                            } else {
                                minAngle = -Math.abs(Math.atan(1.0 * (filter[0].length - y) / Math.sin(parallelAngle) / screenDist));
                            }
                        }
                        if (0 <= slope * (0 - x) + y && slope * (0 - x) + y < filter[0].length) {
                            if (Math.PI / 2 <= parallelAngle && parallelAngle < 3 * Math.PI / 2) {
                                maxAngle = Math.abs(Math.atan(1.0 * (0 - x) / Math.cos(parallelAngle) / screenDist));
                            } else {
                                minAngle = -Math.abs(Math.atan(1.0 * (0 - x) / Math.cos(parallelAngle) / screenDist));
                            }
                        }
                        double normalAngle = r.nextDouble() * (maxAngle - minAngle) + minAngle;
                        double waveDisplacement = screenDist * Math.tan(normalAngle);
                        int screenX = (int) (x + waveDisplacement * Math.cos(parallelAngle));
                        int screenY = (int) (y + waveDisplacement * Math.sin(parallelAngle));
                        double amp = (maxAngle - minAngle) / (Math.PI);
                        double k = 2 * Math.PI / waveLength;
                        double phase = -(screenDist / Math.cos(normalAngle) % waveLength);
                        resultBuffer[screenX][screenY] += ((Math.sin(k * phase) * sinAccumul[screenX][screenY]) + (Math.cos(k * phase) * cosAccumul[screenX][screenY])) * 2 * amp * Math.PI / k;
                        resultBuffer[screenX][screenY] += amp * amp * Math.PI / k;
                        sinAccumul[screenX][screenY] += amp * (Math.sin(k * phase));
                        cosAccumul[screenX][screenY] += amp * (Math.cos(k * phase));
                    }
                    prog = tempProg;
                }
            }
        }
        createDisplay();
    }
    public void createDisplay() {
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        for(int x = 0; x < resultBuffer.length; x++) {
            for(int y = 0; y < resultBuffer[0].length; y++) {
                max = Math.max(max, resultBuffer[x][y]);
                min = Math.min(min, resultBuffer[x][y]);
            }
        }
        for(int x = 0; x < resultBuffer.length; x++) {
            for(int y = 0; y < resultBuffer[0].length; y++) {
                (((DataBufferInt) display.getRaster().getDataBuffer()).getData())[x + y * display.getWidth()] = Color.HSBtoRGB(0.3f, 1.0f, (float) ((Math.log(resultBuffer[x][y] - min + 1) / Math.log(max - min + 1))));
            }
        }
    }
    public int getProgress() {
        return prog;
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(display == null || display.getWidth() != this.getWidth() || display.getHeight() != this.getHeight()) {
            display = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_RGB);
            for(int a = 0; a < display.getWidth(); a++) {
                for(int b = 0; b < display.getHeight(); b++) {
                    (((DataBufferInt) display.getRaster().getDataBuffer()).getData())[a + b * display.getWidth()] = 0xFFFFFF;
                }
            }
        }
        g.drawImage(display, 0, 0, this);
    }
}
