package com.briggs_inc.towf_server;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.briggs_inc.towf_server.PacketConstants.*;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author briggsm
 */

interface InfoManagerListener {
    public void onClientListening(ListeningClientInfo listeningClientInfo);
    public void onClientNotListening(ListeningClientInfo listeningClientInfo);
}

public class InfoManager {
    private static final String TAG = "InfoManager";
    
    public static int INFO_PORT_NUMBER = 7769;
    
    DatagramSocket infoSocket;  // for passing info messages (e.g. sending avail. language/port pairs, receiving "client listening" packet)
    DatagramPacket infoRecvDgPk;
    DatagramPacket infoSendDgPk;
    byte recvDgPkData[] = new byte[UDP_DATA_SIZE];
    byte sendDgPkData[] = new byte[UDP_DATA_SIZE];
    
    DatagramPacket langPortPairsDgPacket;

    Timer langPortPairsInfoSendTimer;
    
    List<InfoManagerListener> listeners = new ArrayList<InfoManagerListener>();
    
    
    public class SendLangPortPairsTask extends TimerTask {
        @Override
        public void run() {
            try {
                InfoManager.this.infoSocket.send(InfoManager.this.langPortPairsDgPacket);
            } catch (IOException ex) {
                Logger.getLogger(InfoManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    

    public class InfoReceiver implements Runnable {

        public InfoReceiver() {
            
        }
        
        @Override
        public void run() {
            while (true) {
                try {
                    infoSocket.receive(infoRecvDgPk);
                } catch (IOException ex) {
                    Logger.getLogger(InfoManager.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                InfoManager.this.onInfoDgPkReceived(infoRecvDgPk);
            }
        }
    }
    
    public InfoManager() {
        try {
            infoSocket = new DatagramSocket(INFO_PORT_NUMBER);
        } catch (SocketException ex) {
            Logger.getLogger(InfoManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        infoRecvDgPk = new DatagramPacket(recvDgPkData, recvDgPkData.length);
        infoSendDgPk = new DatagramPacket(sendDgPkData, sendDgPkData.length);
    }
    
    public void addListener(InfoManagerListener listener) {
        listeners.add(listener);
    }
    
    public void startReceiving() {
        Log.d(TAG, "startReceiving");

        // Better start a new thread, so we don't hang the gui while waiting for packets to be received.
        new Thread(new InfoReceiver()).start();
    }
    
    private void onInfoDgPkReceived(DatagramPacket dg) {
        Log.v(TAG, "onInfoDgPkReceived()");
        
        byte dgData[] = new byte[UDP_DATA_SIZE];
        dgData = dg.getData();
        
        // Check for "ToWF" in Header
        int headerId = Util.getIntFromByteArray(dgData, DG_DATA_HEADER_ID_START, DG_DATA_HEADER_ID_LENGTH, true); 
        if (headerId != ToWF_AS_INT) {
            return;  // 'cuz not a packet for our 'ToWF' app
        }
        
        // Get Payload Type
        int payloadType = Util.getIntFromByteArray(dgData, DG_DATA_HEADER_PAYLOAD_TYPE_START, DG_DATA_HEADER_PAYLOAD_TYPE_LENGTH, false);
        
        switch (payloadType) {
            case DG_DATA_HEADER_PAYLOAD_TYPE_LANG_PORT_PAIR:
                // Seems that we receive here everything that we (as the server) sends out. Not sure why. But will handle it here (do nothing)
                return;
            case DG_DATA_HEADER_PAYLOAD_TYPE_CLIENT_LISTENING:
                //System.out.println("onInfoDgPkReceived()-DG_DATA_HEADER_PAYLOAD_TYPE_CLIENT_LISTENING");
                
                // === Populate the ListeningClientInfo STRUCT ===
                ListeningClientInfo cInfo = new ListeningClientInfo();
                cInfo.IsListening = Util.getIntFromByteArray(dgData, CLPL_IS_LISTENING_START, CLPL_IS_LISTENING_LENGTH, false) == 1 ? true : false;
                cInfo.OsType = Util.getIntFromByteArray(dgData, CLPL_OS_TYPE_START, CLPL_OS_TYPE_LENGTH, false);
                cInfo.Port = Util.getIntFromByteArray(dgData, CLPL_PORT_START, CLPL_PORT_LENGTH, false);
                cInfo.IPAddress = (Inet4Address) dg.getAddress();  // get it from the Datagram's header
                cInfo.OsVersion = Util.getNullTermStringFromByteArray(dgData, CLPL_OS_VERSION_STR_START, CLPL_OS_VERSION_STR_LENGTH);
                cInfo.HwManufacturer = Util.getNullTermStringFromByteArray(dgData, CLPL_HW_MANUFACTURER_STR_START, CLPL_HW_MANUFACTURER_STR_LENGTH);
                cInfo.HwModel = Util.getNullTermStringFromByteArray(dgData, CLPL_HW_MODEL_STR_START, CLPL_HW_MODEL_STR_LENGTH);
                cInfo.UsersName = Util.getNullTermStringFromByteArray(dgData, CLPL_USERS_NAME_START, CLPL_USERS_NAME_LENGTH);

                if (cInfo.IsListening) {
                    notifyListenersOnClientListening(cInfo);
                } else {
                    notifyListenersOnClientNotListening(cInfo);
                }
                
                break;
            default:
                System.out.println("Hmm, payload type UNKNOWN in onInfoDgPkReceived. Type: " + payloadType);
                return;  // nothing to do
        }
    }
    
    public void startSendingLangPortPairs(List<String> languagesList, InetAddress broadcastAddress) {
        // Broadcast the lang/port pairs
        
        // First, build the datagram packet
        buildLangPortPairsDgPk(languagesList, broadcastAddress);
        
        // Every 2 seconds broadcast the language/port# pairs. Send 1st one now.
        langPortPairsInfoSendTimer = new Timer();
        TimerTask sendLangPortPairTask = new SendLangPortPairsTask();
        langPortPairsInfoSendTimer.schedule(sendLangPortPairTask, 0, 2000);  // Send now, and every 2000ms
    }

    public void stopSendingLangPortPairs() {
        langPortPairsInfoSendTimer.cancel();
    }
    
    private void buildLangPortPairsDgPk(List<String> languagesList, InetAddress broadcastAddress) {
        // Build langPort pair datagram packet
        byte lppArr[] = new byte[UDP_DATA_SIZE];
        
        // "ToWF" Header
        Util.writeDgDataHeaderToByteArray(lppArr, DG_DATA_HEADER_PAYLOAD_TYPE_LANG_PORT_PAIR);

        // # of Pairs
        Util.putIntInsideByteArray(languagesList.size(), lppArr, LPP_NUM_PAIRS_START, LPP_NUM_PAIRS_LENGTH, false);
        
        // Rsvd bytes
        Util.putIntInsideByteArray(0x00, lppArr, LPP_RSVD0_START, LPP_RSVD0_LENGTH, false);
        
        // Language/Port pairs
        for (int ctr = 0; ctr < languagesList.size(); ctr++) {
            // Language
            Util.putNullTermStringInsideByteArray(languagesList.get(ctr), lppArr, LPP_LANG0_START + (ctr*(LPP_LANG_LENGTH+LPP_PORT_LENGTH)), LPP_LANG_LENGTH);
            
            // Port
            Util.putIntInsideByteArray(STARTING_STREAM_PORT_NUMBER + ctr, lppArr, LPP_PORT0_START + (ctr*(LPP_LANG_LENGTH+LPP_PORT_LENGTH)), LPP_PORT_LENGTH, false);
        }
        
        // Build the datagram packet
        langPortPairsDgPacket = new DatagramPacket(lppArr, UDP_DATA_SIZE, broadcastAddress, INFO_DST_SOCKET_PORT_NUMBER);
    }
    
    private void notifyListenersOnClientListening(ListeningClientInfo listeningClientInfo) {
        for (InfoManagerListener listener : listeners) {
            listener.onClientListening(listeningClientInfo);
        }
    }
    
    private void notifyListenersOnClientNotListening(ListeningClientInfo listeningClientInfo) {
        for (InfoManagerListener listener : listeners) {
            listener.onClientNotListening(listeningClientInfo);
        }
    }
}
