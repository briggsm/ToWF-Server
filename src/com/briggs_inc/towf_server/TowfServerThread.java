package com.briggs_inc.towf_server;

// Multicast AND Broadcast Audio Server

import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

import static com.briggs_inc.towf_server.PacketConstants.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
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
    
    //PcmAudioDataPayload pcmAudioDataPayloadStorage[] = new PcmAudioDataPayload[0xFFFF];
    //byte[][] pcmAudioDataPayloadStorage = new byte[0x10000][UDP_DATA_SIZE];
    SyncPcmAudioDataPayloadStorageContainer syncPcmAudioDataPayloadStorageContainer = new SyncPcmAudioDataPayloadStorageContainer();
    
    //int[] missingPacketsList; //?????
    //List<Integer> missingPacketsList = new CopyOnWriteArrayList<Integer>();
    //List<Integer> missingPacketsList = new ArrayList<Integer>();
    
    //Boolean comeAndGetMissingPackets = false;
    SyncMissingPacketsSeqIdsContainer syncMissingPacketsSeqIdsContainer = new SyncMissingPacketsSeqIdsContainer();
    
    Timer sendPacketTimer = new Timer();
    long sendPacketRateMS;
    
    
    class SendPacketTask extends TimerTask {
        @Override
        public void run() {
            
            //sendPacketToSocket();
            TowfServerThread.this.sendPacketToSocket();
        }
    }

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
        
        
        sendPacketTimer.scheduleAtFixedRate(new SendPacketTask(), 0, sendPacketRateMS);
        
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

            //sendAudioDataToSocket();
        }

        // Clean up resource usage
        cleanUp();
    }

    /*
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

        //Log.v(TAG, "[" + language + "] micDta: ", false);
        //outputDataSummary(micData, micDataLength, 8);

        if (micDataLength != micData.length) {
            Log.w(TAG, "[" + language + "] -=-=-=-= Hey! micDataLength != micData.length What happened?! micDataLength: " + micDataLength + ", micData.length: " + micData.length + " -=-=-=-=");
        }
    }
    */
    void readAudioDataFromMic() {
        Log.v(TAG, "[" + language + "]--------------------------");
        Log.v(TAG, "[" + language + "]readAudioDataFromMic()");
        
        lineAvailable = line.available();
        Log.v(TAG, " [" + language + "]lineAvailable: " + lineAvailable);
        
        // Have to have at least 1 payload's worth of audio data...
        if (lineAvailable < audioDataMaxValidSize) {
            
            try {
                sleep(sendPacketRateMS);  // To give the sendPacketTimer, and other threads, CPU time do things. Otherwise, this thread runs at full blast, doing nothing but using 99% CPU.
            } catch (InterruptedException ex) {
                Logger.getLogger(TowfServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            return;
        }
        
        int numPayloadsToRead = lineAvailable / audioDataMaxValidSize;
        
        /*
        int lengthRead = 0;
        byte tempMicData[] = new byte[lineAvailable];
        lengthRead = line.read(tempMicData, 0, lineAvailable);
        Log.d(TAG, " [" + language + "]lengthRead: " + lengthRead);
        */
        //byte tempDgData[] = new byte[UDP_DATA_SIZE];
        byte tempAudioData[] = new byte[audioDataMaxValidSize];
        //Util.writeDgDataHeaderToByteArray(tempDgData, DG_DATA_HEADER_PAYLOAD_TYPE_PCM_AUDIO_DATA_REGULAR);
        line.read(tempAudioData, 0, audioDataMaxValidSize);
        //Util.putIntInsideByteArray();
        //syncPcmAudioDataPayloadStorageContainer.addPayloadFromDgData(tempDgData);
        syncPcmAudioDataPayloadStorageContainer.addPayloadFromAudioData(tempAudioData);
    }

    void sendPacketToSocket() {
        Log.v(TAG, "[" + language + "]<TimerThread>sendPacketToSocket()");
        // Note: this will be running from a different thread (the timer thread), so be careful (though, the timer thread [timer task] will only call this function 1 at a time, sequentially).
        // First, send Missing Packets (if any)
        if (syncMissingPacketsSeqIdsContainer.getSize() > 0) {
            Log.d(TAG, "[" + language + "]<TimerThread>(" + syncMissingPacketsSeqIdsContainer.getSize() + ") MISSING PACKETS we've heard about: {" + syncMissingPacketsSeqIdsContainer.getAllSeqIdsAsHexString() + "} (though, as we start sending them more MAY be added to missingPacketsList). Resending them now.");
            SeqId mpSeqId;
            while ((mpSeqId = syncMissingPacketsSeqIdsContainer.popFirstSeqId()) != null) {
                Log.v(TAG, String.format("  [" + language + "]<TimerThread>mpSeqId: 0x%04x", mpSeqId.intValue));
                Util.writeDgDataHeaderToByteArray(dgData, DG_DATA_HEADER_PAYLOAD_TYPE_PCM_AUDIO_DATA_MISSING);
                syncPcmAudioDataPayloadStorageContainer.getPayloadCopyToDgData(mpSeqId, dgData);

                //Log.v(TAG, "  [" + language + "]<TimerThread>MissingPackets-dgData: ", false);
                //outputDataSummary(dgData, dgData.length, 14);

                bcAudioDatagramPacket.setData(dgData);
                bcAudioDatagramPacket.setLength(dgData.length);
                bcAudioDatagramPacket.setAddress(networkInterfaceAddress.getBroadcast());
                Log.v(TAG, "  [" + language + "]<TimerThread>broadcast bcSock.send() <Missing> (" + String.format("0x%04x", mpSeqId.intValue) + "). Time (ms): " + System.nanoTime() / 1000000);
                try {
                    // Have a nice trip!
                    bcSock.send(bcAudioDatagramPacket);
                } catch (IOException ex) {
                    Log.e(TAG, "  [" + language + "]<TimerThread>ExNote: Sending <Missing> audioDatagram over socket FAILED!\nExMessage: " + ex.getMessage());
                }
            }
        }
        
        // Then send the regularly scheduled packets
        if (syncPcmAudioDataPayloadStorageContainer.getNextPayloadToSend(dgData)) {
            Util.writeDgDataHeaderToByteArray(dgData, DG_DATA_HEADER_PAYLOAD_TYPE_PCM_AUDIO_DATA_REGULAR);
            
            //Log.d(TAG, "  [" + language + "]<TimerThread> getNextPayload-dgData: ", false);
            //outputDataSummary(dgData, dgData.length, 14);
            
            bcAudioDatagramPacket.setData(dgData);
            bcAudioDatagramPacket.setLength(dgData.length);
            bcAudioDatagramPacket.setAddress(networkInterfaceAddress.getBroadcast());

            Log.v(TAG, " [" + language + "]<TimerThread>broadcast bcSock.send() <Regular> (" + String.format("0x%04x", Util.getIntFromByteArray(dgData, ADPL_HEADER_SEQ_ID_START, ADPL_HEADER_SEQ_ID_LENGTH, false)) + ") Time (ms): " + System.nanoTime() / 1000000);
            try {
                // Have a nice trip!
                bcSock.send(bcAudioDatagramPacket);
            } catch (IOException ex) {
                Log.e(TAG, " [" + language + "]<TimerThread>ExNote: Sending <Regular> audioDatagram over socket FAILED!\nExMessage: " + ex.getMessage());
            }
        } else {
            Log.v(TAG, " [" + language + "]<TimerThread>No Regular Payload To send this time (MS): " + System.nanoTime() / 1000000);
        }
    }
    
    void sendAudioDataToSocket123() {
        // FIRST, send the Missing Packets (if any),
        // THEN, send the regularly scheduled packet(s) AND save the packet(s) to SavedPacketsStorage
        Log.v(TAG, "[" + language + "]sendAudioDataToSocket()");

        int audioDataCtr = 0;
        int seqId = 0;
        int currAudioDataLength = micDataLength;

        long dbgStartTime;
        long dbgEndTime;
        dbgStartTime = System.nanoTime();

        while (micDataLength > 0) {
            // First, send Missing Packets (if any)
            
            //if (comeAndGetMissingPackets) {
            //}
            
            //if (missingPacketsList.size() > 0) {
            if (syncMissingPacketsSeqIdsContainer.getSize() > 0) {
                //Log.d(TAG, "We've heard there are some (" + missingPacketsList.size() + ") MISSING PACKETS (" + getMissingPacketsSeqIdsAsHexString(missingPacketsList) + "). Resending them now.");
                Log.d(TAG, "We've heard there are some (" + syncMissingPacketsSeqIdsContainer.getSize() + ") MISSING PACKETS (though, as we send, more MAY be added). Start Resending now.");

                // Copy the Array List so if the original if modified by another thread, we'll be ok
                //List<Integer> missingPacketsListCopy = new ArrayList<Integer>(missingPacketsList);  // Shallow copy should be enough.

                //Log.d(TAG, " MISSING PACKETS2: " + getMissingPacketsSeqIdsAsHexString(missingPacketsListCopy));
                
                // Sort the list
                //Collections.sort(missingPacketsListCopy);
                
                //Log.d(TAG, " MISSING PACKETS3: " + getMissingPacketsSeqIdsAsHexString(missingPacketsListCopy));

                // Send each missing packet
                //Iterator<Integer> mpIter = missingPacketsListCopy.iterator();
                //while (mpIter.hasNext()) {
                //while (syncMissingPacketsSeqIdsContainer.getSize() > 0) {
                SeqId mpSeqId;
                while ((mpSeqId = syncMissingPacketsSeqIdsContainer.popFirstSeqId()) != null) {
                    //int mpSeqId = mpIter.next();
                    //SeqId mpSeqId = syncMissingPacketsSeqIdsContainer.popFirstSeqId();  // Get it AND remove it from list.
                    Log.d(TAG, String.format("mpSeqId: 0x%04x", mpSeqId.intValue));
                    //dgData = pcmAudioDataPayloadStorage[mpSeqId.intValue];
                    //dgData = syncPcmAudioDataPayloadStorageContainer.getPayloadCopy(mpSeqId);
                    Util.writeDgDataHeaderToByteArray(dgData, DG_DATA_HEADER_PAYLOAD_TYPE_PCM_AUDIO_DATA_MISSING);
                    syncPcmAudioDataPayloadStorageContainer.getPayloadCopyToDgData(mpSeqId, dgData);
                            
                    
                    
                    Log.d(TAG, "MP-dgData: ", false);
                    outputDataSummary(dgData, dgData.length, 14);

                    bcAudioDatagramPacket.setData(dgData);
                    bcAudioDatagramPacket.setLength(dgData.length);
                    bcAudioDatagramPacket.setAddress(networkInterfaceAddress.getBroadcast());
                    Log.d(TAG, "[" + language + "]broadcast bcSock.send() <Missing> (" + String.format("0x%04x", mpSeqId.intValue) + ")");
                    try {
                        // Have a nice trip!
                        bcSock.send(bcAudioDatagramPacket);
                    } catch (IOException ex) {
                        Log.e(TAG, "[" + language + "]ExNote: Sending <Missing> audioDatagram over socket FAILED!\nExMessage: " + ex.getMessage());
                    }
                }

                // Delete the original Missing Packets list
                //missingPacketsList.clear();  // Note: "IF" another thread adds a packet to this list while we're iterating above, this call will blast away that change, but it's ok 'cuz out protocol will handle it (the Receiver will just just another Missing Pakcet Request)
            }
        
            // Then send the regularly scheduled packets
            seqId = numPacketsSent & 0xFFFF;
            Log.d(TAG, "seqId of this Regulary Scheduled Packet: " + String.format("0x%04x", seqId));
            currAudioDataLength = Math.min(micDataLength, audioDataMaxValidSize);

            Util.writeDgDataHeaderToByteArray(dgData, DG_DATA_HEADER_PAYLOAD_TYPE_PCM_AUDIO_DATA_REGULAR);
            dgData[DG_DATA_HEADER_LENGTH + 0] = (byte) (seqId & 0xFF);
            dgData[DG_DATA_HEADER_LENGTH + 1] = (byte) ((seqId & 0xFF00) >> 8);
            dgData[DG_DATA_HEADER_LENGTH + 2] = (byte) (currAudioDataLength & 0xFF);
            dgData[DG_DATA_HEADER_LENGTH + 3] = (byte) ((currAudioDataLength & 0xFF00) >> 8);
            
            // !! Keep for debug.
            //dbgEndTime = System.nanoTime();
            //Log.v(TAG, "[" + language + "] Time from start sending packets to BEFORE array copy loop (us)" + (dbgEndTime - dbgStartTime) / 1000);
            // Now copy in the audio data
            for (int ctr = 0; ctr < currAudioDataLength; ctr++) {
                //dgData[ctr + DG_DATA_HEADER_LENGTH + PACKET_SEQ_ID_SIZE + AUDIO_LENGTH_SIZE] = micData[audioDataCtr];
                //dgData[ctr + DG_DATA_HEADER_LENGTH + ADPL_HEADER_SEQ_ID_LENGTH + ADPL_HEADER_AUDIO_DATA_ALLOCATED_BYTES_LENGTH] = micData[audioDataCtr];
                dgData[ctr + ADPL_AUDIO_DATA_START] = micData[audioDataCtr];

                // Save to file...
                // !!!!! Save this in case need to debug later !!!!!
                //fos.write(audioData[audioDataCtr]);
                audioDataCtr++;
            }

            // !! Keep for debug.
            //dbgEndTime = System.nanoTime();
            //Log.v(TAG, "[" + language + "] Time from start sending packets to AFTER array copy loop (us)" + (dbgEndTime - dbgStartTime) / 1000);
            
            //Log.v(TAG, "[" + language + "] dgData: ", false);
            //outputDataSummary(dgData, dgData.length, 10);
            //Log.d(TAG, "MPR2-dgData: ", false);
            //outputDataSummary(dgData, dgData.length, 14);

            // Save dgData to Storage, in case any Receiver(s) find it Missing
            //Log.d(TAG, "seqId2 of this Regulary Scheduled Packet: " + String.format("0x%04x", seqId));
            
            //System.arraycopy(dgData, 0, pcmAudioDataPayloadStorage[seqId], 0, dgData.length);
            //syncPcmAudioDataPayloadStorageContainer.addPayloadFromDgData(dgData);
            syncPcmAudioDataPayloadStorageContainer.addPayloadFromAudioData(micData);
            
            //????? Do I even need to do this?!!!!! ??????
            bcAudioDatagramPacket.setData(dgData);
            bcAudioDatagramPacket.setLength(dgData.length);

            // !! Keep for debug.
            //dbgEndTime = System.nanoTime();
            //Log.v(TAG, "[" + language + "] Time from start sending packets to BEFORE sock.send(packet) (us)" + (dbgEndTime - dbgStartTime) / 1000);
            //System.out.println("broadcast bcSock.send()");
            bcAudioDatagramPacket.setAddress(networkInterfaceAddress.getBroadcast());
            Log.v(TAG, "[" + language + "]broadcast bcSock.send() <Regular>");
            try {
                // Have a nice trip!
                bcSock.send(bcAudioDatagramPacket);
            } catch (IOException ex) {
                Log.e(TAG, "[" + language + "]ExNote: Sending <Regular> audioDatagram over socket FAILED!\nExMessage: " + ex.getMessage());
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
        Log.d(TAG, outStr);
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

        sendPacketTimer.cancel();
        
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
        audioDataMaxValidSize = (ADPL_AUDIO_DATA_AVAILABLE_SIZE - (ADPL_AUDIO_DATA_AVAILABLE_SIZE % afFrameSize));
        micLineBufferSizeBytes = audioDataMaxValidSize * MIC_LINE_FUDGE_FACTOR;
        sendPacketRateMS = (long)(1.0 / (audioFormat.getSampleRate() * afFrameSize / audioDataMaxValidSize) * 1000);  // cast to long will round down always causing packets to be sent slightly faster than theoretical. But all will still balance out.
        Log.d(TAG, "sendPacketRateMS: " + sendPacketRateMS);
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
    
    //public void handleMissingPacketsRequest(int[] missingPacketsArr) {
    public void handleMissingPacketsRequest(List<SeqId> mprList) {
        // Copy to our local missingPacketsList (will get sent right before the next regularly scheduled packet gets sent out)
        // Note: the func that resends these missing packets is on another thread, and when finished will clear() this.missingPacketsList - usually this
        //      will be fine, but there'll be a small window the it'll get deleted while we're adding missing packets to the list.  But that's ok because
        //      out protocol will handle it (when the Receiver sees that it didn't receive the missing packets again, it will resend the Missing Packet Request)
        
        //comeAndGetMissingPackets = true;
        
        /*
        //Log.d(TAG, "handleMissingPacketsRequest(" + missingPacketsArr.length + ")");
        Log.d(TAG, "handleMissingPacketsRequest [" + missingPacketsArr.length + "]");
        Log.d(TAG, "hMPR() - ThreadName running this code: " + Thread.currentThread().getName());
        for (int i = 0; i < missingPacketsArr.length; i++) {
            Log.d(TAG, " loop[" + i + "] => " + String.format("0x%04x", missingPacketsArr[i]));
            if (!this.missingPacketsList.contains(new Integer(missingPacketsArr[i]))) {
                Log.d(TAG, "  adding it (" + String.format("0x%04x", missingPacketsArr[i]) + ") to missingPacketsList (" + getMissingPacketsSeqIdsAsHexString(missingPacketsList) + ")");
                this.missingPacketsList.add(new Integer(missingPacketsArr[i]));
            }
        }
        */
        
        /*
        for (int i = 0; i < missingPacketsArr.length; i++) {
            syncMissingPacketsSeqIdsContainer.add(missingPacketsArr[i]);
        }
        */
        
        syncMissingPacketsSeqIdsContainer.addList(mprList);
    }

    private String getMissingPacketsSeqIdsAsHexString(List<Integer> mpSeqIdsList) {
        String s = "";
        for (Integer mpSeqId : mpSeqIdsList) {
            s += String.format("0x%04x, ", mpSeqId);
        }
        
        return s;
    }
};
