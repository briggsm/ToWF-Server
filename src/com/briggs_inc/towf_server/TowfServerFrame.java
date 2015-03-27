package com.briggs_inc.towf_server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JOptionPane;

import static com.briggs_inc.towf_server.PacketConstants.*;
import java.awt.Frame;
import java.awt.Toolkit;
import static java.lang.System.out;
import java.net.InetAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.prefs.Preferences;


/**
 *
 * @author mark
 */
public class TowfServerFrame extends javax.swing.JFrame implements InfoManagerListener {
    private static final String TAG = "TowfServerFrame";
    
    public static final String APP_VERSION = "1.0beta";
    
    private Preferences prefs;
    
    // Configuration Properties
    //private static final String CFG_PROPS_FILENAME = "config.properties";
    //private static final String CFG_PROPS_FILENAME = "resources/config.properties";
    private static final String AF_SAMPLE_RATE_KEY = "AfSampleRate";
    
    // Audio Format constants
    private static final int AF_SAMPLE_SIZE_IN_BITS = 16;
    private static final int AF_CHANNELS = 1;
    private static final boolean AF_SIGNED = true;
    private static final boolean AF_BIG_ENDIAN = false;
    
    private static final String NETWORK_INTERFACE_NAME = "NetworkInterfaceName";
    private static final String INPUT_LANGUAGE1 = "InputLanguage1";
    private static final String INPUT_LANGUAGE2 = "InputLanguage2";
    private static final String INPUT_LANGUAGE3 = "InputLanguage3";
    private static final String INPUT_LANGUAGE4 = "InputLanguage4";
    private static final String INPUT_SOURCE1 = "InputSource1";
    private static final String INPUT_SOURCE2 = "InputSource2";
    private static final String INPUT_SOURCE3 = "InputSource3";
    private static final String INPUT_SOURCE4 = "InputSource4";
    
    // My Vars
    InterfaceAddress networkInterfaceIPv4Address;
    List<TowfServerThread> tsThreads;
    //File cfgPropsFile;
    //File cfgPropsFile2;
    //InputStream projPropsInputStream;
    InputStream projPropsInputStream;
    //OutputStream cfgPropsOutputStream;
    //Properties cfgProps;
    Properties projProps;
    List<JComboBox<String>> inputSourceCBs;
    List<JTextField> languageTFs;
    InfoManager infoManager;
    DatagramPacket langPortPairDgPacket;
    ListeningClientsTableModel lcTableModel;
    
    
    /**
     * Creates new form MulticastServer
     */
    public TowfServerFrame() {
        Log.i(TAG, "Note: to change LG_LEVEL, edit 'Log.java'");
        initComponents();
        
        
        //Frame f = WindowManager.getDefault().getMainWindow();
        /*
        String PROJ_PROPS_FILENAME = "project.properties";
        projPropsInputStream = getClass().getClassLoader().getResourceAsStream(PROJ_PROPS_FILENAME);
        projProps = new Properties();
        try {
            projProps.load(projPropsInputStream);
            projPropsInputStream.close();
        } catch (FileNotFoundException ex) {
            Log.e(TAG, "ExNote: '" + PROJ_PROPS_FILENAME + "' file not found ().\nExMessage: " + ex.getMessage());
        } catch (IOException ex) {
            Log.e(TAG, "ExNote: IO Exception.\nExMessage: " + ex.getMessage());
        }    
            
        this.setTitle("Hello There - " + projProps.getProperty("version", "7.7"));
        */
        
        prefs = Preferences.userRoot().node(this.getClass().getName());
        
        // create list of all inputSourceCBs & languages
        inputSourceCBs = new ArrayList<JComboBox<String>>();
        inputSourceCBs.add(inputSource1CB);
        inputSourceCBs.add(inputSource2CB);
        inputSourceCBs.add(inputSource3CB);
        inputSourceCBs.add(inputSource4CB);
        
        languageTFs = new ArrayList<JTextField>();
        languageTFs.add(language1TF);
        languageTFs.add(language2TF);
        languageTFs.add(language3TF);
        languageTFs.add(language4TF);
        
        // Setup listeningClientsTable
        lcTableModel = new ListeningClientsTableModel();
        listeningClientsTable.setModel(lcTableModel);
        
        infoManager = new InfoManager();
        infoManager.addListener(this);
        infoManager.startReceiving();
        
        lookupAndDisplayNetIFs();
        populateInputSourceComboBoxes();
        
        tsThreads = new ArrayList<TowfServerThread>();
        
        //cfgPropsFile = new File(CFG_PROPS_FILENAME);
        //cfgPropsFromFile2Gui();
        retrievePreferences();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnStartStop = new javax.swing.JButton();
        runState = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        netIFsCB = new javax.swing.JComboBox<NetworkInterface>();
        afSampleRate = new javax.swing.JComboBox<String>();
        jPanel2 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        inputSource1CB = new javax.swing.JComboBox<String>();
        jLabel7 = new javax.swing.JLabel();
        language1TF = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        inputSource2CB = new javax.swing.JComboBox<String>();
        language2TF = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        inputSource3CB = new javax.swing.JComboBox<String>();
        language3TF = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        inputSource4CB = new javax.swing.JComboBox<String>();
        language4TF = new javax.swing.JTextField();
        jPanel5 = new javax.swing.JPanel();
        removeAllListeningClientsBtn = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        listeningClientsTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("ToWF Server (v" + APP_VERSION + ")");

        btnStartStop.setText("Start");
        btnStartStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartStopActionPerformed(evt);
            }
        });

        runState.setText("Stopped");

        jLabel6.setText("Sample Rate (Hz):");

        jLabel11.setText("Network Interface:");

        netIFsCB.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        afSampleRate.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "8000", "11025", "22050", "44100" }));
        afSampleRate.setSelectedIndex(2);
        afSampleRate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                afSampleRateActionPerformed(evt);
            }
        });

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Input 1"));

        jLabel5.setText("Input Source:");

        inputSource1CB.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel7.setText("Language:");

        language1TF.setText("English");
        language1TF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                language1TFActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(inputSource1CB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(language1TF)))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(inputSource1CB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(language1TF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Input 2"));

        jLabel8.setText("Input Source:");

        jLabel9.setText("Language:");

        inputSource2CB.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        language2TF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                language2TFActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(inputSource2CB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(language2TF)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(inputSource2CB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(language2TF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Input 3"));

        jLabel17.setText("Input Source:");

        jLabel18.setText("Language:");

        inputSource3CB.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        language3TF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                language3TFActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel17, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel18, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(inputSource3CB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 6, Short.MAX_VALUE))
                    .addComponent(language3TF)))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(inputSource3CB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(language3TF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Input 4"));

        jLabel20.setText("Input Source:");

        jLabel21.setText("Language:");

        inputSource4CB.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        language4TF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                language4TFActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel20, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel21, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(inputSource4CB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 6, Short.MAX_VALUE))
                    .addComponent(language4TF)))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(inputSource4CB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(language4TF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Listening Clients"));

        removeAllListeningClientsBtn.setText("Clear List");
        removeAllListeningClientsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeAllListeningClientsBtnActionPerformed(evt);
            }
        });

        listeningClientsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane5.setViewportView(listeningClientsTable);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 979, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(removeAllListeningClientsBtn)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(removeAllListeningClientsBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(netIFsCB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(runState)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnStartStop))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(afSampleRate, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(netIFsCB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnStartStop)
                    .addComponent(runState))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(afSampleRate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnStartStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartStopActionPerformed
        if (btnStartStop.getText().equals("Start")) {
            startThreads();
        } else {
            stopThreads();
        }
    }//GEN-LAST:event_btnStartStopActionPerformed

    private void afSampleRateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_afSampleRateActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_afSampleRateActionPerformed

    private void language1TFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_language1TFActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_language1TFActionPerformed

    private void language2TFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_language2TFActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_language2TFActionPerformed

    private void language3TFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_language3TFActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_language3TFActionPerformed

    private void language4TFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_language4TFActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_language4TFActionPerformed

    private void removeAllListeningClientsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeAllListeningClientsBtnActionPerformed
        // Remove From Table
        lcTableModel.removeAllListeningClients();
        lcTableModel.fireTableDataChanged();
        
        // Remove all listening clients from tsThreads
        for (TowfServerThread tsThread : tsThreads) {
            tsThread.removeAllUnicastClients();
        }
    }//GEN-LAST:event_removeAllListeningClientsBtnActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(TowfServerFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(TowfServerFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(TowfServerFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TowfServerFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new TowfServerFrame().setVisible(true);
            }
        });
    }
    
    
    
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> afSampleRate;
    private javax.swing.JButton btnStartStop;
    private javax.swing.JComboBox<String> inputSource1CB;
    private javax.swing.JComboBox<String> inputSource2CB;
    private javax.swing.JComboBox<String> inputSource3CB;
    private javax.swing.JComboBox<String> inputSource4CB;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTextField language1TF;
    private javax.swing.JTextField language2TF;
    private javax.swing.JTextField language3TF;
    private javax.swing.JTextField language4TF;
    public javax.swing.JTable listeningClientsTable;
    private javax.swing.JComboBox<NetworkInterface> netIFsCB;
    private javax.swing.JButton removeAllListeningClientsBtn;
    private javax.swing.JLabel runState;
    // End of variables declaration//GEN-END:variables

    private void startThreads() {
        Log.d(TAG, "============= Starting =============");
        // Save cfgProps to File
        //cfgPropsFromGui2File();  // If user is starting a thread with these values, they're probably worth saving.
        savePreferences();  // If user is starting a thread with these values, they're probably worth saving.

        // Display Mixer info's
        Mixer.Info minfos[] = AudioSystem.getMixerInfo();
        for (Mixer.Info minfo : minfos) {
            Log.i(TAG, minfo.toString());
        }
        
        // Stop current threads. (don't think this will ever get called, but just in case)
        for (TowfServerThread tsThread : tsThreads) {
            if (tsThread != null) {
                tsThread.setStopped(true);  // To stop thread from doing anything else. ('cuz this old thread will still continue to run, until it exits its run() loop.
            }
        }

        // Get networkInterfaceIPv4Address
        //String networkInterfaceName = netIFsCB.getSelectedItem().toString();
        String networkInterfaceName = ((NetworkInterface)netIFsCB.getSelectedItem()).getName();
        try {
            List<InterfaceAddress> networkInterfaceAddresses = NetworkInterface.getByName(networkInterfaceName).getInterfaceAddresses();
            networkInterfaceIPv4Address = getFirstNetworkInterfaceIPv4Address(networkInterfaceAddresses);
        } catch (SocketException ex) {
            Log.e(TAG, "ExNote: SocketException.\nExMessage: " + ex.getMessage());
        }
        
        // Make sure networkInterfaceIPv4Address is valid
        if (networkInterfaceIPv4Address == null) {
            Log.e(TAG, "NetworkInterfaceAddress is invalid! Not starting ANY threads!");
            return;
        }
        
        AudioFormat audioFormat = new AudioFormat(Float.valueOf(afSampleRate.getSelectedItem().toString()), AF_SAMPLE_SIZE_IN_BITS, AF_CHANNELS, AF_SIGNED, AF_BIG_ENDIAN);
        
        List<String> languagesList = new ArrayList<String>();
        List<String> mixerNamesList = new ArrayList<String>();
        for (int ctr = 0; ctr < languageTFs.size(); ctr++) {
            if (!languageTFs.get(ctr).getText().equals("")) {
                languagesList.add(languageTFs.get(ctr).getText());
                mixerNamesList.add(inputSourceCBs.get(ctr).getSelectedItem().toString());
            }
        }
        
        infoManager.startSendingLangPortPairs(languagesList, networkInterfaceIPv4Address.getBroadcast());
        
        // Start a tsThread for each language
        tsThreads.clear();
        for (int ctr = 0; ctr < languagesList.size(); ctr++) {
            if (!mixerNamesList.get(ctr).equalsIgnoreCase("<None>")) {
                TowfServerThread tsThread = new TowfServerThread(this, audioFormat, mixerNamesList.get(ctr), languagesList.get(ctr), networkInterfaceIPv4Address, STARTING_STREAM_PORT_NUMBER + ctr);
                tsThread.setStopped(false);
                tsThread.start();
                tsThreads.add(tsThread);
            } else {
                JOptionPane.showMessageDialog(this, "'" + languagesList.get(ctr) + "' does not have a valid Input source!\nSelected input source is: '" + mixerNamesList.get(ctr) + "'", "Invalid Input Source", JOptionPane.ERROR_MESSAGE);
            }
        }

        setRunStateGuiText(true);  // "Running"
    }

    private void stopThreads() {
        for (TowfServerThread tsThread : tsThreads) {
            tsThread.setStopped(true);
        }
        tsThreads.clear();
        
        // Cancel the 1/2 second timer
        infoManager.stopSendingLangPortPairs();
        
        setRunStateGuiText(false);  // "Stopped"
    }
    
    /*
    private void cfgPropsFromFile2Gui() {
        // Load cfgProps from file
        try {
            //cfgPropsInputStream = new FileInputStream(cfgPropsFile);
            projPropsInputStream = getClass().getClassLoader().getResourceAsStream(CFG_PROPS_FILENAME);
            cfgProps = new Properties();
            cfgProps.load(projPropsInputStream);
            projPropsInputStream.close();
        } catch (FileNotFoundException ex) {
            Log.e(TAG, "ExNote: '" + CFG_PROPS_FILENAME + "' file not found (propsFile2Gui).\nExMessage: " + ex.getMessage());
        } catch (IOException ex) {
            Log.e(TAG, "ExNote: IO Exception.\nExMessage: " + ex.getMessage());
        }
        
        // Write properties to GUI
        afSampleRate.setSelectedItem(cfgProps.getProperty(AF_SAMPLE_RATE_KEY, "11025"));
        
        netIFsCB.setSelectedItem(cfgProps.getProperty(NETWORK_INTERFACE_NAME, ""));
        language1TF.setText(cfgProps.getProperty(INPUT_LANGUAGE1, ""));
        language2TF.setText(cfgProps.getProperty(INPUT_LANGUAGE2, ""));
        language3TF.setText(cfgProps.getProperty(INPUT_LANGUAGE3, ""));
        language4TF.setText(cfgProps.getProperty(INPUT_LANGUAGE4, ""));
        inputSource1CB.setSelectedItem(cfgProps.getProperty(INPUT_SOURCE1, "<None>"));
        inputSource2CB.setSelectedItem(cfgProps.getProperty(INPUT_SOURCE2, "<None>"));
        inputSource3CB.setSelectedItem(cfgProps.getProperty(INPUT_SOURCE3, "<None>"));
        inputSource4CB.setSelectedItem(cfgProps.getProperty(INPUT_SOURCE4, "<None>"));
    }
    */
    private void retrievePreferences() {
        afSampleRate.setSelectedItem(prefs.get(AF_SAMPLE_RATE_KEY, "22050"));
        try {
            netIFsCB.setSelectedItem(NetworkInterface.getByName(prefs.get(NETWORK_INTERFACE_NAME, "")));
        } catch (SocketException ex) {
            Logger.getLogger(TowfServerFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        language1TF.setText(prefs.get(INPUT_LANGUAGE1, ""));
        language2TF.setText(prefs.get(INPUT_LANGUAGE2, ""));
        language3TF.setText(prefs.get(INPUT_LANGUAGE3, ""));
        language4TF.setText(prefs.get(INPUT_LANGUAGE4, ""));
        inputSource1CB.setSelectedItem(prefs.get(INPUT_SOURCE1, "<None>"));
        inputSource2CB.setSelectedItem(prefs.get(INPUT_SOURCE2, "<None>"));
        inputSource3CB.setSelectedItem(prefs.get(INPUT_SOURCE3, "<None>"));
        inputSource4CB.setSelectedItem(prefs.get(INPUT_SOURCE4, "<None>"));
    }
    
    /*
    private void cfgPropsFromGui2File() {
        // Set cfgProps from GUI
        cfgProps.setProperty(AF_SAMPLE_RATE_KEY, afSampleRate.getSelectedItem().toString());
        
        cfgProps.setProperty(NETWORK_INTERFACE_NAME, netIFsCB.getSelectedItem().toString());
        cfgProps.setProperty(INPUT_LANGUAGE1, language1TF.getText());
        cfgProps.setProperty(INPUT_LANGUAGE2, language2TF.getText());
        cfgProps.setProperty(INPUT_LANGUAGE3, language3TF.getText());
        cfgProps.setProperty(INPUT_LANGUAGE4, language4TF.getText());
        cfgProps.setProperty(INPUT_SOURCE1, inputSource1CB.getSelectedItem().toString());
        cfgProps.setProperty(INPUT_SOURCE2, inputSource2CB.getSelectedItem().toString());
        cfgProps.setProperty(INPUT_SOURCE3, inputSource3CB.getSelectedItem().toString());
        cfgProps.setProperty(INPUT_SOURCE4, inputSource4CB.getSelectedItem().toString());
        
        // Save cfgProps to File
        try {
            //cfgPropsOutputStream = new FileOutputStream(cfgPropsFile);
            //cfgPropsOutputStream = getClass().getClassLoader().getResourceAsStream(CFG_PROPS_FILENAME);
            //URL u = getClass().getResource(CFG_PROPS_FILENAME);
            URL u = getClass().getClassLoader().getResource(CFG_PROPS_FILENAME);
            Log.d(TAG, "url: " + u);
            String s = u.getPath();
            new File()
            File f = new File(u);
            //cfgPropsOutputStream = new FileOutputStream(getClass().getResource(CFG_PROPS_FILENAME).getPath());
            cfgPropsOutputStream = new FileOutputStream(f);
                    
            cfgProps.store(cfgPropsOutputStream, "Program Settings");
            cfgPropsOutputStream.close();
        } catch (FileNotFoundException ex) {
            Log.e(TAG, "ExNote: '" + CFG_PROPS_FILENAME + "' file not found (propsGuiToFile).\nExMessage: " + ex.getMessage());
        } catch (IOException ex) {
            Log.e(TAG, "ExNote: IO Exception.\nExMessage: " + ex.getMessage());
        }
    }
    */
    public void savePreferences() {
        prefs.put(AF_SAMPLE_RATE_KEY, afSampleRate.getSelectedItem().toString());
        prefs.put(NETWORK_INTERFACE_NAME, ((NetworkInterface)netIFsCB.getSelectedItem()).getName());
        prefs.put(INPUT_LANGUAGE1, language1TF.getText());
        prefs.put(INPUT_LANGUAGE2, language2TF.getText());
        prefs.put(INPUT_LANGUAGE3, language3TF.getText());
        prefs.put(INPUT_LANGUAGE4, language4TF.getText());
        prefs.put(INPUT_SOURCE1, inputSource1CB.getSelectedItem().toString());
        prefs.put(INPUT_SOURCE2, inputSource2CB.getSelectedItem().toString());
        prefs.put(INPUT_SOURCE3, inputSource3CB.getSelectedItem().toString());
        prefs.put(INPUT_SOURCE4, inputSource4CB.getSelectedItem().toString());
    }

    public void setRunStateGuiText(boolean setToRunning) {
        if (setToRunning) {
            runState.setText("Running");
            btnStartStop.setText("Stop");
        } else {
            runState.setText("Stopped");
            btnStartStop.setText("Start");
        }
    }

    private void lookupAndDisplayNetIFs() {
        netIFsCB.removeAllItems();
        
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netIf : Collections.list(nets)) {
                //Log.d(TAG, "name: " + netIf.getName());
                //Log.d(TAG, " displayName: " + netIf.getDisplayName());
                //Log.d(TAG, " isUp: " + netIf.isUp());
                //Log.d(TAG, "netIf: " + netIf.toString());
                
                Boolean netIfHasIPv4Address = false;
                Enumeration<InetAddress> inetAddresses = netIf.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    if (inetAddress instanceof Inet4Address) {
                        netIfHasIPv4Address = true;
                    }
                }
                
                /*
                out.printf("Display name: %s\n", netIf.getDisplayName());
                out.printf("Name: %s\n", netIf.getName());
                Enumeration<InetAddress> inetAddresses = netIf.getInetAddresses();

                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    out.printf("InetAddress: %s\n", inetAddress);
                    if (inetAddress instanceof Inet4Address) {
                        out.printf("*IPv4 Address var!\n");
                        netIfHasIPv4Address = true;
                    }
                }

                out.printf("Up? %s\n", netIf.isUp());
                out.printf("Loopback? %s\n", netIf.isLoopback());
                out.printf("PointToPoint? %s\n", netIf.isPointToPoint());
                out.printf("Supports multicast? %s\n", netIf.supportsMulticast());
                out.printf("Virtual? %s\n", netIf.isVirtual());
                out.printf("Hardware address: %s\n",
                            Arrays.toString(netIf.getHardwareAddress()));
                out.printf("MTU: %s\n", netIf.getMTU());

                out.printf("\n");
                */
        
                if (netIf.isUp() && netIfHasIPv4Address && !netIf.isLoopback()) {
                    //netIFsCB.addItem(netIf.getName());
                    netIFsCB.addItem(netIf);
                }
            }
        } catch (SocketException ex) {
            Logger.getLogger(TowfServerFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private List<Mixer.Info> getAvailableInputMixers() {  // Just INPUT Mixers
        // Only return TargetDataLines for INPUT's (e.g. Microphone(s))
        
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        List<Mixer.Info> availableInputMixers = new ArrayList<Mixer.Info>();
        
        // We want to find the TARGET lines (output of the mixer, which could be DataLines or Ports), which are DATA LINES (norrows down to only the DataLines).
        for (Mixer.Info mixerInfo : mixers){
            Mixer m = AudioSystem.getMixer(mixerInfo);
            Line.Info[] lines = m.getTargetLineInfo();  // Lookup just the TARGET line info
            for (Line.Info li : lines){
                // Make sure this "target line" is a "DataTargetLine" (AND not a PORT) 'cuz that means this mixer is getting INPUT (from MICROPHONE, etc) & sending that data IN to this program via this "target line"
                if (li instanceof DataLine.Info) {
                    // If it's usable, add it to our list.
                    try {
                        m.open();
                        availableInputMixers.add(mixerInfo);
                        m.close();
                    } catch (LineUnavailableException e) {
                        //System.out.println("Line unavailable.");
                    }
                }
            }
        }
        
        return availableInputMixers;
    }

    private void populateInputSourceComboBoxes() {
        for (JComboBox<String> cb : inputSourceCBs) {
            cb.removeAllItems();
            
            // Add <None>
            cb.addItem("<None>");
            
            List<Mixer.Info> availableInputMixers = getAvailableInputMixers();
            for (Mixer.Info mixerInfo : availableInputMixers) {
                cb.addItem(mixerInfo.getName());
            }
        }
    }
    
    private InterfaceAddress getFirstNetworkInterfaceIPv4Address(List<InterfaceAddress> networkInterfaceAddresses) {
        for (InterfaceAddress nia : networkInterfaceAddresses) {
            if (nia.getAddress() instanceof Inet4Address) {
                return nia;
            }
        }
        
        return null;
    }
    
    @Override
    public void onClientListening(ListeningClientInfo listeningClientInfo) {
        // Populate table
        lcTableModel.addListeningClient(listeningClientInfo);  // Note: only adds if doesn't exist already
        lcTableModel.fireTableDataChanged();
        
        // Add the Unicast client to appropriate thread's list
        // !!! Keep, in case need to add it back in later !!! (Note: if need this, make sure to also add check if it's Android (or whatever) so EVERYBODY does not get marked as a UnicastClient...
        /*
        int port = listeningClientInfo.Port;
        for (TowfServerThread tsThread : tsThreads) {
            if (tsThread.getDstPortNumber() == port) {
                tsThread.addUnicastClient(listeningClientInfo); // Note: only adds if doesn't exist already
            }
        }
        */
        
    }

    @Override
    public void onClientNotListening(ListeningClientInfo listeningClientInfo) {
        lcTableModel.removeListeningClient(listeningClientInfo);
        lcTableModel.fireTableDataChanged();
        
        // Remove unicast client from it's list in the thread
        int port = listeningClientInfo.Port;
        for (TowfServerThread tsThread : tsThreads) {
            if (tsThread.getDstPortNumber() == port) {
                tsThread.removeUnicastClient(listeningClientInfo); // Note: only adds if doesn't exist already
            }
        }
    }
}

