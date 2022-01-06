package com.github.kehemo.dp;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author Kenneth
 */
public class GUI {
    private JFrame win;
    private JPanel main;
    private DrawPanel in;
    private DisplayPanel out;
    private JPanel settings;
    private final JSplitPane jsp;
    
    private final JButton computeBtn;
    private final JButton invertBtn;
    private final JButton clearBtn;
    private JButton toggleSettingsBtn;
    private final JSpinner penSz;
    private final JTextField screenDist;
    private final JTextField wavelength;
    private final JTextField photons;
    private final JComboBox examples;
    private final JLabel progress;
    private Thread compThread;
    private static final String EXAMPLE_NAMES[] = {
        "Diffraction grating",
        "Circular double slit",
        "Two holes",
        "Diffraction slit",
        "K",
        "Diffraction hole",
    };
    private final ExampleFilter exampleSettings[] = {
        (x, y) -> (Math.abs(x - in.getWidth() / 2) > 100 || x % 10 < 9),
        (x, y) -> !(((x - in.getWidth() / 2) * (x - in.getWidth() / 2) + (y - in.getHeight() / 2) * (y - in.getHeight() / 2) <= 201 * 201)
                && ((x - in.getWidth() / 2) * (x - in.getWidth() / 2) + (y - in.getHeight() / 2) * (y - in.getHeight() / 2) > 199 * 199)
            || ((x - in.getWidth() / 2) * (x - in.getWidth() / 2) + (y - in.getHeight() / 2) * (y - in.getHeight() / 2) <= 101 * 101)
                && ((x - in.getWidth() / 2) * (x - in.getWidth() / 2) + (y - in.getHeight() / 2) * (y - in.getHeight() / 2) > 99 * 99)),
        (x, y) -> !((x == in.getHeight() / 2 - 100) && (y == in.getHeight() / 2 - 100)
                || (x == in.getWidth() / 2 + 100) && (y == in.getHeight() / 2 + 100)),
        (x, y) -> !(Math.abs(x - in.getWidth() / 2) < 5 && Math.abs(y - in.getHeight() / 2) < 5),
        (x, y) -> !((x == in.getWidth() / 2 - 100 && y < in.getHeight() / 2 + 200 && y >= in.getHeight() / 2 - 200)
                || (x < in.getWidth() / 2 + 100 && x > in.getWidth() / 2 - 100
                && (x - (in.getWidth() / 2 - 100) == y - in.getHeight() / 2 || (in.getWidth() / 2 - 100) - x == y - in.getHeight() / 2))),
        (x, y) -> !((x - in.getWidth() / 2) * (x - in.getWidth() / 2) + (y - in.getHeight() / 2) * (y - in.getHeight() / 2) <= 10 * 10),
    };
    public static final Font DEFAULT_FONT = new Font("Calibri Light", Font.PLAIN, 12);
    
    public GUI() {
        win = new JFrame("Diffraction Pattern Generator");
        main = new JPanel();
        in = new DrawPanel();
        out = new DisplayPanel();
        settings = new JPanel();
        jsp = new JSplitPane() {
            @Override
            public int getDividerLocation() {
                super.getDividerLocation();
                return this.getSize().height;
            }
            @Override
            public Dimension getPreferredSize() {
                Component parent = this.getParent();
                if(parent == null) {
                    return new Dimension(100, 50);
                } else {
                    int w = parent.getWidth() - 20;
                    int h = parent.getHeight();
                    return new Dimension((2 * h < w ? 2 * h : w), (2 * h < w ? h : w / 2));
                }
            }
        };
        invertBtn = new JButton("Invert Filter");
        clearBtn = new JButton("Clear Filter");
        penSz = new JSpinner(new SpinnerNumberModel(5, 1, 40, 1));
        screenDist = new JTextField("100", 9);
        wavelength = new JTextField("10", 9);
        photons = new JTextField("1000000", 15);
        examples = new JComboBox<>(EXAMPLE_NAMES);
        toggleSettingsBtn = new JButton("Show options");
        Box wrapper = Box.createHorizontalBox();
        Box controls = Box.createHorizontalBox();
        
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        settings.setLayout(new BoxLayout(settings, BoxLayout.Y_AXIS));
        
        jsp.setLeftComponent(out);
        jsp.setRightComponent(in);
        jsp.setEnabled(false);
        
        wrapper.add(smallArea());
        wrapper.add(jsp);
        wrapper.add(smallArea());
        
        Box patternData = Box.createHorizontalBox();
        JLabel patternLabel = new JLabel("Save: ");
        patternLabel.setFont(DEFAULT_FONT);
        JButton saveImage = new JButton("Save Pattern Image");
        saveImage.setFont(DEFAULT_FONT);
        saveImage.addActionListener((e) -> {
            JFileChooser jfc = new JFileChooser();
            jfc.setDialogTitle("Folder to save to");
            jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int state = jfc.showSaveDialog(win);
            if(state == JFileChooser.APPROVE_OPTION) {
                JPanel message = new JPanel(new GridLayout(0, 1));
                JLabel nameTag = new JLabel("Name of new file: ");
                nameTag.setFont(DEFAULT_FONT);
                JTextField nameField = new JTextField();
                nameField.setFont(DEFAULT_FONT);
                message.add(nameTag);
                message.add(nameField);
                if(JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(win, message, "Name the Image", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)) {
                    File im = new File(jfc.getSelectedFile(), nameField.getText() + ".png");
                    try {
                        im.createNewFile();
                        ImageIO.write(out.getImage(), "png", im);
                    } catch (IOException ex) {
                        Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        JButton savePatternData = new JButton("Save Pattern Data");
        savePatternData.setFont(DEFAULT_FONT);
        savePatternData.addActionListener((e) -> {
            JFileChooser jfc = new JFileChooser();
            jfc.setDialogTitle("Folder to save to");
            jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int state = jfc.showSaveDialog(win);
            if(state == JFileChooser.APPROVE_OPTION) {
                JPanel message = new JPanel(new GridLayout(0, 1));
                JLabel nameTag = new JLabel("Name of new file: ");
                nameTag.setFont(DEFAULT_FONT);
                JTextField nameField = new JTextField();
                nameField.setFont(DEFAULT_FONT);
                message.add(nameTag);
                message.add(nameField);
                if(JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(win, message, "Name the data file", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)) {
                    File dat = new File(jfc.getSelectedFile(), nameField.getText() + ".dat");
                    try {
                        dat.createNewFile();
                        FileOutputStream fos = new FileOutputStream(dat);
                        out.writeData(fos);
                        fos.close();
                    } catch (IOException ex) {
                        Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        JButton loadPatternData = new JButton("Load Pattern Data");
        loadPatternData.setFont(DEFAULT_FONT);
        loadPatternData.addActionListener((e) -> {
            JFileChooser jfc = new JFileChooser();
            jfc.setDialogTitle("Select a file");
            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            jfc.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    String s = f.getName();
                    String match = "tad.";
                    boolean res = true;
                    for(int a = s.length() - 1; a >= 0; a--) {
                        if(a < s.length() - match.length()) {
                            break;
                        }
                        if(match.charAt(s.length() - 1 - a) != s.charAt(a)) {
                            res = false;
                            break;
                        }
                    }
                    return res;
                }
                @Override
                public String getDescription() {
                    return "DAT files";
                }
            }
            );
            int state = jfc.showSaveDialog(win);
            if(state == JFileChooser.APPROVE_OPTION) {
                File data = jfc.getSelectedFile();
                try(FileInputStream fis = new FileInputStream(jfc.getSelectedFile());) {
                    out.readData(fis);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        patternData.add(patternLabel);
        patternData.add(saveImage);
        patternData.add(savePatternData);
        patternData.add(loadPatternData);
        
        settings.add(patternData);
        settings.setMinimumSize(in.getMinimumSize());
        settings.setMaximumSize(in.getMaximumSize());
        settings.setPreferredSize(in.getPreferredSize());
        
        computeBtn = new JButton("Compute");
        computeBtn.setFont(DEFAULT_FONT);
        compThread = new Thread(() -> {
            out.computePattern();
        });
        computeBtn.addActionListener((e) -> {
            boolean filter[][] = in.getFilter();
            if(filter != null) {
                out.setFilter(filter);
                out.setClearPoints(in.getClearPoints());
            }
            out.setWavelength(Double.parseDouble(wavelength.getText()));
            out.setScreenDistance(Double.parseDouble(screenDist.getText()));
            out.setLimit(Integer.parseInt(photons.getText()));
            compThread = new Thread(() -> {
                out.computePattern();
            });
            compThread.start();
        });
        invertBtn.setFont(DEFAULT_FONT);
        invertBtn.addActionListener((e) -> {
            in.invert();
        });
        clearBtn.addActionListener((e) -> {
            in.clear();
        });
        clearBtn.setFont(DEFAULT_FONT);
        JLabel szLabel = new JLabel("Pen size: ");
        szLabel.setFont(DEFAULT_FONT);
        penSz.setFont(DEFAULT_FONT);
        penSz.addChangeListener((e) -> {
            in.setPenSize((int) (((JSpinner) e.getSource()).getValue()));
        });
        penSz.setMaximumSize(new Dimension(40, 40));
        JLabel distLabel = new JLabel("Screen distance: ");
        distLabel.setFont(DEFAULT_FONT);
        screenDist.setFont(DEFAULT_FONT);
        screenDist.setMaximumSize(new Dimension(50, 40));
        JLabel waveLabel = new JLabel("Wavelength: ");
        waveLabel.setFont(DEFAULT_FONT);
        wavelength.setFont(DEFAULT_FONT);
        wavelength.setMaximumSize(new Dimension(50, 40));
        JLabel photonLabel = new JLabel("Photon count: ");
        photonLabel.setFont(DEFAULT_FONT);
        photons.setFont(DEFAULT_FONT);
        photons.setMaximumSize(new Dimension(50, 40));
        progress = new JLabel("Progress: 0%");
        JLabel exLabel = new JLabel("Examples: ");
        exLabel.setFont(DEFAULT_FONT);
        examples.setFont(DEFAULT_FONT);
        examples.addActionListener((e) -> {
            in.useExampleFilter(exampleSettings[examples.getSelectedIndex()]);
        });
        progress.setFont(DEFAULT_FONT);
        toggleSettingsBtn.setFont(DEFAULT_FONT);
        toggleSettingsBtn.addActionListener((e) -> {
            if(toggleSettingsBtn.getText().charAt(0) == 'S') {
                toggleSettingsBtn.setText("Hide options");
                jsp.setRightComponent(settings);
                
            } else {
                toggleSettingsBtn.setText("Show options");
                jsp.setRightComponent(in);
            }
        });
        
        controls.add(smallArea());
        controls.add(computeBtn);
        controls.add(smallArea());
        controls.add(invertBtn);
        controls.add(smallArea());
        controls.add(clearBtn);
        controls.add(smallArea());
        controls.add(szLabel);
        controls.add(penSz);
        controls.add(smallArea());
        controls.add(distLabel);
        controls.add(screenDist);
        controls.add(smallArea());
        controls.add(waveLabel);
        controls.add(wavelength);
        controls.add(smallArea());
        controls.add(photonLabel);
        controls.add(photons);
        controls.add(smallArea());
        controls.add(exLabel);
        controls.add(examples);
        controls.add(smallArea());
        controls.add(toggleSettingsBtn);
        controls.add(smallArea());
        controls.add(progress);
        controls.add(smallArea());
        controls.add(Box.createGlue());
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        
        main.add(wrapper);
        main.add(controls);
        win.setContentPane(main);
        win.setExtendedState(JFrame.MAXIMIZED_BOTH | win.getExtendedState());
        win.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        win.setVisible(true);
    }
    
    public void initUpdateCycles() {
        Thread redraw = new Thread(() -> {
            while(true) {
                if(jsp.getSize().width != jsp.getPreferredSize().width || jsp.getSize().height != jsp.getPreferredSize().height) {
                    jsp.setMinimumSize(jsp.getPreferredSize());
                    jsp.setMaximumSize(jsp.getPreferredSize());
                    main.revalidate();
                }
                if(computeBtn.isEnabled() && compThread.isAlive()) {
                    computeBtn.setEnabled(false);
                }
                if(!computeBtn.isEnabled() && !compThread.isAlive()) {
                    computeBtn.setEnabled(true);
                }
                in.repaint();
                out.repaint();
                if(out.getLimit() == 0) {
                    progress.setText("Progress: 0%");
                } else {
                    progress.setText("Progress: " + out.getProgress() * 100l / out.getLimit() + "%");
                }
                progress.repaint();
            }
        });
        redraw.start();
    }
    
    public static Component smallArea() {
        return Box.createRigidArea(new Dimension(10, 10));
    }
    
    public static Component largeArea() {
        return Box.createRigidArea(new Dimension(20, 20));
    }
}
