package com.github.kehemo.dp;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import javax.swing.JPanel;

/**
 *
 * @author Kenneth
 */
public final class DrawPanel extends JPanel {
    private BufferedImage display;
    private boolean filterBuffer[][];
    private int penSize;
    private int prevX;
    private int prevY;
    private int clearPoints;
    private boolean changed;
    public DrawPanel() {
        super();
        penSize = 10;
        prevX = -1;
        prevY = -1;
        changed = true;
        this.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }
            @Override
            public void mousePressed(MouseEvent e) {
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                prevX = -1;
                prevY = -1;
            }
            @Override
            public void mouseEntered(MouseEvent e) {
            }
            @Override
            public void mouseExited(MouseEvent e) {
            }
            
        });
        this.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                if(!inbounds(x, y)) {
                    prevX = -1;
                    prevY = -1;
                    return;
                }
                if(prevX == -1) {
                    for(int a = -penSize / 2; a < (penSize + 1) / 2; a++) {
                        for(int b = -penSize / 2; b < (penSize + 1) / 2; b++) {
                            if(inbounds(x + a, y + b)) {
                                (((DataBufferInt) display.getRaster().getDataBuffer()).getData())[(x + a) + (y + b) * display.getWidth()] = 0;
                            }
                        }
                    }
                } else {
                    double slope = 1.0 * (prevY - y) / (prevX - x);
                    if(Math.abs(slope) < 1) {
                        double ny = ((x < prevX) ? y : prevY);
                        for (int nx = Math.min(x, prevX); nx <= Math.max(x, prevX) && slope * ny <= slope * ((x < prevX) ? prevY : y); nx++, ny += slope) {
                            for (int a = -penSize / 2; a < (penSize + 1) / 2; a++) {
                                for (int b = -penSize / 2; b < (penSize + 1) / 2; b++) {
                                    if (inbounds(nx + a, (int) ny + b)) {
                                        (((DataBufferInt) display.getRaster().getDataBuffer()).getData())[(nx + a) + ((int) ny + b) * display.getWidth()] = 0;
                                    }
                                }
                            }
                        }
                    } else {
                        double nx = ((y < prevY) ? x : prevX);
                        for (int ny = Math.min(y, prevY); ny <= Math.max(y, prevY) && slope * nx <= slope * ((y < prevY) ? prevX : x); ny++, nx += 1 / slope) {
                            for (int a = -penSize / 2; a < (penSize + 1) / 2; a++) {
                                for (int b = -penSize / 2; b < (penSize + 1) / 2; b++) {
                                    if (inbounds((int) nx + a, ny + b)) {
                                        (((DataBufferInt) display.getRaster().getDataBuffer()).getData())[((int) nx + a) + (ny + b) * display.getWidth()] = 0;
                                    }
                                }
                            }
                        }
                    }
                }
                prevX = x;
                prevY = y;
                changed = true;
            }
            @Override
            public void mouseMoved(MouseEvent e) {
            }
        });
    }
    public boolean[][] getFilter() {
        if(!changed) {
            return null;
        }
        clearPoints = 0;
        filterBuffer = new boolean[display.getWidth()][display.getHeight()];
        for(int a = 0; a < display.getWidth(); a++) {
            for(int b = 0; b < display.getHeight(); b++) {
                filterBuffer[a][b] = ((((DataBufferInt) display.getRaster().getDataBuffer()).getData())[a + b * display.getWidth()] == 0xFFFFFF);
                if(filterBuffer[a][b]) {
                    clearPoints++;
                }
            }
        }
        changed = false;
        return filterBuffer;
    }
    public int getClearPoints() {
        return clearPoints;
    }
    public void useExampleFilter(ExampleFilter ex) {
        for(int a = 0; a < display.getWidth(); a++) {
            for(int b = 0; b < display.getHeight(); b++) {
                (((DataBufferInt) display.getRaster().getDataBuffer()).getData())[a + b * display.getWidth()] = (ex.filled(a, b) ? 0 : 0xFFFFFF);
            }
        }
        changed = true;
    }
    public void setPenSize(int sz) {
        penSize = sz;
    }
    public void invert() {
        for(int a = 0; a < display.getWidth(); a++) {
            for(int b = 0; b < display.getHeight(); b++) {
                (((DataBufferInt) display.getRaster().getDataBuffer()).getData())[a + b * display.getWidth()] = ~((((DataBufferInt) display.getRaster().getDataBuffer()).getData())[a + b * display.getWidth()]) & (0xFFFFFF);
            }
        }
        changed = true;
    }
    public void clear() {
        for(int a = 0; a < display.getWidth(); a++) {
            for(int b = 0; b < display.getHeight(); b++) {
                (((DataBufferInt) display.getRaster().getDataBuffer()).getData())[a + b * display.getWidth()] = 0xFFFFFF;
            }
        }
        changed = true;
    }
    private boolean inbounds(int x, int y) {
        return 0 <= x && x < display.getWidth() && 0 <= y && y < display.getHeight();
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
