package com.briggs_inc.towf_server;

// Multicast AND Broadcast Audio Server

import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

import static com.briggs_inc.towf_server.PacketConstants.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TowfServerThread extends Thread implements LineListener {
    private static final String TAG = "TowfServerThread";
    
    // Note: need to use "small" MIC_LINE_BUFFER_SIZE_BYTES, so line.read() doesn't take
    //      too long if not enough bytes available(). For 2.0F sec buffer, it would
    //      take 0.5 sec to read 466 bytes of data when available was <466 bytes.
    //      So, we'll multiply the audioDataMaxValidSize by this value to get a good
    //      balance between a buffer that's too small (and miss mic data because we didn't read it fast enough)
    //      and too big (causing too long of a delay when available bytes is less than what we want to read in).
    private static final int MIC_LINE_FUDGE_FACTOR = 8;

    // Socket-related member variables
    protected DatagramSocket bcSock;  // broadcast socket
    
    DatagramPacket bcAudioDatagramPacket;
    
    DatagramPacket bcPcmAudioFormatDatagramPacket;
    
    // audio format derived
    int afSampleSizeInBytes = 0;
    int afFrameSize = 0;
    int audioDataMaxValidSize = 0;

    boolean stopped = false;
    int numPacketsSent = 0;
    File fileOut;
    FileOutputStream fos;

    AudioFormat audioFormat;
    DataLine.Info info;
    TargetDataLine line;

    TowfServerFrame f;
    
    String mixerName;
    String language;
    InterfaceAddress networkInterfaceAddress;
    int dstSocketPort;

    int lineAvailable = 0;
    int micLineAvailableMax = 0;
    int micLineBufferSizeBytes;

    byte[] dgData = new byte[UDP_DATA_SIZE];
    byte micData[];
    int micDataLength = 0;

    // Amplitude
    int minAmp = 0;
    int maxAmp = 0;

    // Timings
    long hsStartTimeMS;
    long hsEndTimeMS;
    
    // Unicast Clients
    List<Inet4Address> unicastClients = new ArrayList<Inet4Address>();

    //public TowfServerThread(TowfServerFrame f, AudioFormat af, String multicastStreamIp, int multicastStreamPort, String broadcastStreamIp, int broadcastStreamPort) {
    public TowfServerThread(TowfServerFrame f, AudioFormat af, String mixerName, String language, InterfaceAddress networkInterfaceAddress, int dstSocketPort) {
        this.f = f;
        this.audioFormat = af;
        
        this.mixerName = mixerName;
        this.language = language;
        this.networkInterfaceAddress = networkInterfaceAddress;
        this.dstSocketPort = dstSocketPort;

        buildPcmAudioFormatDatagramPacket();

        // Set Audio Format & Multicast Derived Variables
        setAfMcDerivedVariables();

        /*
        // Display Mixer info's
        Mixer.Info minfos[] = AudioSystem.getMixerInfo();
        for (Mixer.Info minfo : minfos) {
            Log.i(TAG, minfo.toString());
            //System.out.println("  Name: " + minfo.getName() + ", \t");
//            System.out.print("Name: " + minfo.getName() + ", \t");
//             System.out.print("Version: " + minfo.getVersion() + ", \t");
//             System.out.print("Vendor: " + minfo.getVendor() + ", \t");
//             Log.v(TAG, "[" + language + "]Description: " + minfo.getDescription());
        }
        */
        
        Log.i(TAG, "*[" + language + "]Destination Socket Port: " + dstSocketPort);
        
        // Setup TargetDataLine (Microphone)
        Mixer mixer = getMixerWithThisName(mixerName);
        
        if (mixer != null) {
            info = new DataLine.Info(TargetDataLine.class, audioFormat, micLineBufferSizeBytes);
            try {
                line = (TargetDataLine) mixer.getLine(info);
                line.addLineListener(this);
                line.open(audioFormat, micLineBufferSizeBytes);
            } catch (LineUnavailableException ex) {
                Log.e(TAG, "[" + language + "]ExNote: Line Unavailable!\nExMessage: " + ex.getMessage() + "\nQuitting.");
                f.setRunStateGuiText(false);  // "Stopped"
                cleanUp();
                return;  // If line is unavailable, no sense in continuing.
            } catch (IllegalArgumentException ex) {
                // E.g.: No line matching interface TargetDataLine supporting format PCM_SIGNED 44100.0 Hz, 32 bit, stereo, 8 bytes/frame, little-endian, and buffers of 3712 to 3712 bytes is supported.
                Log.e(TAG, "[" + language + "]ExNote: Illegal Argument!\nExMessage: " + ex.getMessage() + "\nQuitting.");
                f.setRunStateGuiText(false);  // "Stopped"
                cleanUp();
                return;  // If illegal argument, no sense in continuing.
            }
            
            Log.d(TAG, "[" + language + "]line.getBufferSize() (CONST): " + line.getBufferSize());
            assert (line.getBufferSize() == micLineBufferSizeBytes);

            micData = new byte[audioDataMaxValidSize];
            Log.d(TAG, "[" + language + "]micData.length (CONST): " + micData.length);

            // Setup Datagram Socket & Datagram Packet
            try {
                bcSock = new DatagramSocket(dstSocketPort, networkInterfaceAddress.getAddress());  // Note: port (srcSocketPort) doesn't really matter (I think), will just use same as dstSocketPort.
                bcSock.setTrafficClass(0xC0);  // Voice (<10ms latency) [http://wiki.ubnt.com/Main_Page/QoS_DSCP/TOS_Mappings]
                byte tempArr[] = new byte[UDP_DATA_SIZE];
                bcAudioDatagramPacket = new DatagramPacket(tempArr, UDP_DATA_SIZE, networkInterfaceAddress.getBroadcast(), dstSocketPort);
            } catch (SocketException ex) {
                Log.e(TAG, "[" + language + "]ExNote: bcSock Socket Exception.\nExMessage: " + ex.getMessage());
            }
            Log.d(TAG, "[" + language + "]dgData.length (CONST): " + dgData.length);

            // Send out audioFormatDatagram, so first thing recv'd by client is audio format (then send it out again every 1/2 second after this)
            try {
                Log.i(TAG, "[" + language + "]*** Sending Initial Audio Format Datagram ***");
                bcSock.send(bcPcmAudioFormatDatagramPacket);
            } catch (IOException ex) {
                Logger.getLogger(TowfServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void run() {
        // 1/2 second timer
        hsStartTimeMS = System.currentTimeMillis();

        // Setup Timer vars to aid in debugging timing issues.
        long dbgStartTime;
        long dbgEndTime;
        dbgStartTime = System.nanoTime();

        // === Sanity Checks ===
        // Make sure we've got a line to work with.
        if (line == null) {
            Log.e(TAG, "[" + language + "]Thread has to Line to work with! Quitting Thread.");
            return;  // Nothing to do. This thread is done.
        }
        
        // Make sure we've got a socket to work with.
        if (bcSock == null) {
            Log.e(TAG, "[" + language + "]Thread has to Socket to work with! Quitting Thread.");
            return;  // Nothing to do. This thread is done.
        }
        
        line.start();
        while (!stopped) {
            // Check for trigger of 1/2 second timer
            hsEndTimeMS = System.currentTimeMillis();
            if (hsEndTimeMS - hsStartTimeMS >= 500) {
                // 1/2 second passed
                hsStartTimeMS = hsEndTimeMS;

                // Send Audio Format Datagram (broadcast)
                bcPcmAudioFormatDatagramPacket.setAddress(networkInterfaceAddress.getBroadcast());
                try {
                    Log.v(TAG, "[" + language + "]*** Sending Update Audio Format Datagram ***");
                    bcSock.send(bcPcmAudioFormatDatagramPacket);
                } catch (IOException ex) {
                    Logger.getLogger(TowfServerThread.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                // Send Audio Format Datagram (to each unicast client)
                // !!! Keep, in case need to add it back in later !!!
                /*
                for (Inet4Address unicastClient : unicastClients) {
                    //System.out.println("Audio Format for-each loop: " + unicastClient.getHostAddress());
                    Log.v(TAG, "[" + language + "]Audio Format for-each loop: " + unicastClient.getHostAddress());
                    bcPcmAudioFormatDatagramPacket.setAddress(unicastClient);
                    try {
                        // Have a nice trip!
                        //mcSock.send(audioDatagram);
                        bcSock.send(bcPcmAudioFormatDatagramPacket);
                    } catch (IOException ex) {
                        Log.e(TAG, "[" + language + "]ExNote: Sending audioDatagram over socket FAILED!\nExMessage: " + ex.getMessage());
                    }
                }
                */
                
            }

            readAudioDataFromMic();

            dbgEndTime = System.nanoTime();
            Log.v(TAG, "[" + language + "]Time from last MicDataRead to this MicDataRead (us): " + (dbgEndTime - dbgStartTime) / 1000);
            dbgStartTime = System.nanoTime();

            sendAudioDataToSocket();
        }

        // Clean up resource usage
        cleanUp();
    }

    void readAudioDataFromMic() {
        Log.v(TAG, "[" + language + "]--------------------------");
        Log.v(TAG, "[" + language + "]readAudioDataFromMic()");

        // Debug timer vars
        long dbgStartTime;
        long dbgEndTime;
        dbgStartTime = System.nanoTime();

        lineAvailable = line.available();
        Log.v(TAG, " [" + language + "]lineAvailable: " + lineAvailable);

        if (lineAvailable > micLineAvailableMax) {
            micLineAvailableMax = lineAvailable;
        }

        dbgEndTime = System.nanoTime();
        Log.v(TAG, "[" + language + "]Time to BEFORE line.read (us): " + (dbgEndTime - dbgStartTime) / 1000);

        Log.v(TAG, " [" + language + "]*line.read()");
        micDataLength = line.read(micData, 0, micData.length);

        dbgEndTime = System.nanoTime();
        Log.v(TAG, "[" + language + "]Time to AFTER line.read (us): " + (dbgEndTime - dbgStartTime) / 1000);

        //Log.v(TAG, "[" + language + "] micDataLength: " + micDataLength);
        Log.v(TAG, "[" + language + "] micDta: ", false);
        outputDataSummary(micData, micDataLength, 8);

        if (micDataLength != micData.length) {
            Log.w(TAG, "[" + language + "] -=-=-=-= Hey! micDataLength != micData.length What happened?! micDataLength: " + micDataLength + ", micData.length: " + micData.length + " -=-=-=-=");
        }
    }

    void sendAudioDataToSocket() {
        Log.v(TAG, "[" + language + "]sendAudioDataToSocket()");

        int audioDataCtr = 0;
        int currAudioDataLength = micDataLength;

        long dbgStartTime;
        long dbgEndTime;
        dbgStartTime = System.nanoTime();

        while (micDataLength > 0) {
            currAudioDataLength = Math.min(micDataLength, audioDataMaxValidSize);

            Util.writeDgDataHeaderToByteArray(dgData, DG_DATA_HEADER_PAYLOAD_TYPE_PCM_AUDIO_DATA);
            dgData[DG_DATA_HEADER_LENGTH + 0] = (byte) (numPacketsSent & 0xFF);
            dgData[DG_DATA_HEADER_LENGTH + 1] = (byte) ((numPacketsSent & 0xFF00) >> 8);
            dgData[DG_DATA_HEADER_LENGTH + 2] = (byte) (currAudioDataLength & 0xFF);
            dgData[DG_DATA_HEADER_LENGTH + 3] = (byte) ((currAudioDataLength & 0xFF00) >> 8);
            
            // !! Keep for debug.
            //dbgEndTime = System.nanoTime();
            //Log.v(TAG, "[" + language + "] Time from start sending packets to BEFORE array copy loop (us)" + (dbgEndTime - dbgStartTime) / 1000);
            // Now copy in the audio data
            for (int ctr = 0; ctr < currAudioDataLength; ctr++) {
                dgData[ctr + DG_DATA_HEADER_LENGTH + PACKET_SEQ_ID_SIZE + AUDIO_LENGTH_SIZE] = micData[audioDataCtr];

                // Save to file...
                // !!!!! Save this in case need to debug later !!!!!
                //fos.write(audioData[audioDataCtr]);
                audioDataCtr++;
            }

            // !! Keep for debug.
            //dbgEndTime = System.nanoTime();
            //Log.v(TAG, "[" + language + "] Time from start sending packets to AFTER array copy loop (us)" + (dbgEndTime - dbgStartTime) / 1000);
            
            Log.v(TAG, "[" + language + "] dgData: ", false);
            outputDataSummary(dgData, dgData.length, 10);

            //????? Do I even need to do this?!!!!! ??????
            bcAudioDatagramPacket.setData(dgData);
            bcAudioDatagramPacket.setLength(dgData.length);

            // !! Keep for debug.
            //dbgEndTime = System.nanoTime();
            //Log.v(TAG, "[" + language + "] Time from start sending packets to BEFORE sock.send(packet) (us)" + (dbgEndTime - dbgStartTime) / 1000);
            //System.out.println("broadcast bcSock.send()");
            Log.v(TAG, "[" + language + "]broadcast bcSock.send()");
            bcAudioDatagramPacket.setAddress(networkInterfaceAddress.getBroadcast());
            try {
                // Have a nice trip!
                bcSock.send(bcAudioDatagramPacket);
            } catch (IOException ex) {
                Log.e(TAG, "[" + language + "]ExNote: Sending audioDatagram over socket FAILED!\nExMessage: " + ex.getMessage());
            }
            
            // Now also send to all unicastClients
            // !!! Keep, in case need to add it back in later !!!
            /*
            for (Inet4Address unicastClient : unicastClients) {
                System.out.println("for-each loop: " + unicastClient.getHostAddress());
                bcAudioDatagramPacket.setAddress(unicastClient);
                try {
                    // Have a nice trip!
                    //mcSock.send(audioDatagram);
                    bcSock.send(bcAudioDatagramPacket);
                } catch (IOException ex) {
                    Log.v(TAG, "[" + language + "]ExNote: Sending audioDatagram over socket FAILED!\nExMessage: " + ex.getMessage());
                }
            }
            */

            // !! Keep for debug.
            //dbgEndTime = System.nanoTime();
            //Log.v(TAG, "[" + language + "] Time from start sending packets to AFTER sock.send(packet) (us)" + (dbgEndTime - dbgStartTime) / 1000);
            numPacketsSent++;
            Log.v(TAG, "[" + language + "] numPacketsSent: " + numPacketsSent);

            micDataLength -= currAudioDataLength;
        }
    }
    
    private void outputDataSummary(byte[] data, int dataLength, int numSummaryBytes) {
        // Make sure numSummaryBytes isn't too big
        if (numSummaryBytes * 2 > dataLength) {
            numSummaryBytes = dataLength / 2;
        }

        // Build the string
        String outStr = "";
        for (int ctr = 0; ctr < numSummaryBytes; ctr++) {
            outStr += String.format("%02X", (data[ctr] & 0xFF)) + ",";
        }
        outStr += " . . . ";
        for (int ctr = dataLength - numSummaryBytes; ctr < dataLength; ctr++) {
            outStr += String.format("%02X", (data[ctr] & 0xFF)) + ",";
        }

        // Output the string
        Log.v(TAG, outStr);
    }

    void setStopped(boolean b) {
        stopped = b;
    }

    public void resetMinMaxAmplitude() {
        minAmp = 0;
        maxAmp = 0;
    }

    public void resetMicLineAvailableMax() {
        micLineAvailableMax = 0;
    }

    @Override
    public void update(LineEvent event) {
        
    }
    
    void cleanUp() {
        Log.i(TAG, "[" + language + "]This thread is done running. Cleaning up...!");

        if (bcSock != null) {
            bcSock.close();
            bcSock = null;
        }

        if (fos != null) {
            try {
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(TowfServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            fos = null;
        }

        if (line != null) {
            line.drain();
            line.close();
            line = null;
        }
    }

    private void buildPcmAudioFormatDatagramPacket() {
        byte bcafdgData[] = new byte[UDP_DATA_SIZE];
        writePcmAudioFormatByteArray(bcafdgData);
        bcPcmAudioFormatDatagramPacket = new DatagramPacket(bcafdgData, bcafdgData.length, networkInterfaceAddress.getBroadcast(), dstSocketPort);
    }
    
    private void writePcmAudioFormatByteArray(byte[] afb) {
        Util.writeDgDataHeaderToByteArray(afb, DG_DATA_HEADER_PAYLOAD_TYPE_PCM_AUDIO_FORMAT);
        Util.putIntInsideByteArray((int) audioFormat.getSampleRate(), afb, BCAFDG_SAMPLE_RATE_START, BCAFDG_SAMPLE_RATE_LENGTH, false);
        Util.putIntInsideByteArray(audioFormat.getSampleSizeInBits(), afb, BCAFDG_SAMPLE_SIZE_IN_BITS_START, BCAFDG_SAMPLE_SIZE_IN_BITS_LENGTH, false);
        Util.putIntInsideByteArray(audioFormat.getChannels(), afb, BCAFDG_CHANNELS_START, BCAFDG_CHANNELS_LENGTH, false);
        Util.putIntInsideByteArray(audioFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) ? 1 : 0, afb, BCAFDG_SIGNED_START, BCAFDG_SIGNED_LENGTH, false);
        Util.putIntInsideByteArray(audioFormat.isBigEndian() ? 1 : 0, afb, BCAFDG_BIG_ENDIAN_START, BCAFDG_BIG_ENDIAN_LENGTH, false);
    }
    
    private void setAfMcDerivedVariables() {
        afSampleSizeInBytes = audioFormat.getSampleSizeInBits() / 8;
        if (audioFormat.getSampleSizeInBits() % 8 > 0) {
            afSampleSizeInBytes++;
        }
        afFrameSize = afSampleSizeInBytes * audioFormat.getChannels();
        audioDataMaxValidSize = (UDP_AUDIO_DATA_AVAILABLE_SIZE - (UDP_AUDIO_DATA_AVAILABLE_SIZE % afFrameSize));
        micLineBufferSizeBytes = audioDataMaxValidSize * MIC_LINE_FUDGE_FACTOR;
    }
    
    private Mixer getMixerWithThisName(String mixerName) {
        // Search through all mixers until find one with matching "name"
        
        Mixer.Info mixerInfos[] = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixerInfos) {
            if (mixerInfo.getName().equals(mixerName)) {
                return AudioSystem.getMixer(mixerInfo);
            }
        }
        return null;
    }
    
    public int getDstPortNumber() {
        return dstSocketPort;
    }
    
    public void addUnicastClient(ListeningClientInfo listeningClientInfo) {
        for (Inet4Address unicastClient : unicastClients) {
            if (unicastClient.equals(listeningClientInfo.IPAddress)) {
                return;  // Already exists
            }
        }
        
        if (!listeningClientInfo.IPAddress.getHostAddress().equals("0.0.0.0")) {  // Just make sure 0.0.0.0 doesn't get added in case we receive that.
            unicastClients.add(listeningClientInfo.IPAddress);
        }
    }
    
    public void removeUnicastClient(ListeningClientInfo listeningClientInfo) {
        for (Inet4Address unicastClient : unicastClients) {
            if (unicastClient.equals(listeningClientInfo.IPAddress)) {
                unicastClients.remove(unicastClient);
                return;
            }
        }
    }
    
    public void removeAllUnicastClients() {
        unicastClients.clear();
    }
};
