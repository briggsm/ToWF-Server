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
    public void onMissingPacketsRequestReceived(Inet4Address ipAddress, int port, List<SeqId> mprList);
    public void onChatMsgReceived(Inet4Address ipAddress, String msg);
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
                    infoSocket.receive(infoRecvDgPk);  // Hangs (blocks) here until packet is received... (but doesn't use CPU resources while blocking) [But since this should be on a thread of it's own, it's ok]
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
                // Received from Client itself
                cInfo.IsListening = Util.getIntFromByteArray(dgData, CLPL_IS_LISTENING_START, CLPL_IS_LISTENING_LENGTH, false) == 1 ? true : false;
                cInfo.OsType = Util.getIntFromByteArray(dgData, CLPL_OS_TYPE_START, CLPL_OS_TYPE_LENGTH, false);
                cInfo.Port = Util.getIntFromByteArray(dgData, CLPL_PORT_START, CLPL_PORT_LENGTH, false);
                cInfo.IPAddress = (Inet4Address) dg.getAddress();  // get it from the Datagram's header
                cInfo.OsVersion = Util.getNullTermStringFromByteArray(dgData, CLPL_OS_VERSION_STR_START, CLPL_OS_VERSION_STR_LENGTH);
                cInfo.HwManufacturer = Util.getNullTermStringFromByteArray(dgData, CLPL_HW_MANUFACTURER_STR_START, CLPL_HW_MANUFACTURER_STR_LENGTH);
                cInfo.HwModel = Util.getNullTermStringFromByteArray(dgData, CLPL_HW_MODEL_STR_START, CLPL_HW_MODEL_STR_LENGTH);
                cInfo.UsersName = Util.getNullTermStringFromByteArray(dgData, CLPL_USERS_NAME_START, CLPL_USERS_NAME_LENGTH);

                // Other info
                cInfo.EnableMPRs = new Boolean(true);
                cInfo.NumMPRs = 0;
                //cInfo.listeningToggle = new Boolean(false);
                
                if (cInfo.IsListening) {
                    notifyListenersOnClientListening(cInfo);
                } else {
                    notifyListenersOnClientNotListening(cInfo);
                }
                
                break;
            case DG_DATA_HEADER_PAYLOAD_TYPE_MISSING_PACKETS_REQUEST:
                //Log.d(TAG, "Missing Packets request came in...");
                
                // Add all missing packets to packetsToResend list (all will get sent just before the next 'regular' packet gets sent out)
                int numMissingPackets = Util.getIntFromByteArray(dgData, MPRPL_NUM_MISSING_PACKETS_START, MPRPL_NUM_MISSING_PACKETS_LENGTH, false);
                int port = Util.getIntFromByteArray(dgData, MPRPL_PORT_START, MPRPL_PORT_LENGTH, false);
                Inet4Address ipAddress = (Inet4Address)dg.getAddress();  // get it from the Datagram's header
                
                String s = "";
                List<SeqId> mprList = new ArrayList<SeqId>();
                for (int i = 0; i < numMissingPackets; i++) {
                    mprList.add(new SeqId(Util.getIntFromByteArray(dgData, MPRPL_PACKET0_SEQID_START + (i*2), MPRPL_PACKET0_SEQID_LENGTH, false)));
                    s += String.format("0x%04x, ", mprList.get(i).intValue);
                }
                Log.d(TAG, "Missing Packets request (" + numMissingPackets + ") came in from {" + ((Inet4Address)dg.getAddress()).getHostAddress() + "}: (" + s + ")");
                
                notifyListenersOnMissingPacketsRequestReceived(ipAddress, port, mprList);
                
                break;
            case DG_DATA_HEADER_PAYLOAD_TYPE_CHAT_MSG:
                String msg = Util.getNullTermStringFromByteArray(dgData, CHATMSG_MSG_START, (dgData.length - CHATMSG_MSG_START));
                
                notifyListenersOnChatMsgReceived((Inet4Address)dg.getAddress(), msg);
                break;
            default:
                Log.e(TAG, "Hmm, payload type UNKNOWN in onInfoDgPkReceived. Type: " + payloadType);
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
    
    public void sendEnableMPRs(Inet4Address ipAddress, boolean enabled) {
        byte enMPRsArr[] = new byte[DG_DATA_HEADER_LENGTH + 1];
        
        // "ToWF" Header
        Util.writeDgDataHeaderToByteArray(enMPRsArr, DG_DATA_HEADER_PAYLOAD_TYPE_ENABLE_MPRS);
        
        // Enabled / Disabled
        Util.putIntInsideByteArray(enabled ? 1 : 0, enMPRsArr, ENMPRS_ENABLED_START, ENMPRS_ENABLED_LENGTH, false);
        
        // Build the Datagram Packet
        DatagramPacket dgPk = new DatagramPacket(enMPRsArr, enMPRsArr.length, ipAddress, INFO_DST_SOCKET_PORT_NUMBER);
        
        // Try to send it out over the network
        try {
            infoSocket.send(dgPk);
        } catch (IOException ex) {
            Logger.getLogger(InfoManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void sendChatMsg(Inet4Address ipAddress, String msg) {
        byte chatMsgArr[] = new byte[DG_DATA_HEADER_LENGTH + msg.length() + 1];  // +1 for the null terminator
        
        // "ToWF" Header
        Util.writeDgDataHeaderToByteArray(chatMsgArr, DG_DATA_HEADER_PAYLOAD_TYPE_CHAT_MSG);
        
        // Message
        Util.putNullTermStringInsideByteArray(msg, chatMsgArr, CHATMSG_MSG_START, msg.length() + 1);  // +1 for the null terminator
        
        // Build the Datagram Packet
        DatagramPacket dgPk = new DatagramPacket(chatMsgArr, chatMsgArr.length, ipAddress, INFO_DST_SOCKET_PORT_NUMBER);
        
        // Try to send it out over the network
        try {
            infoSocket.send(dgPk);
        } catch (IOException ex) {
            Logger.getLogger(InfoManager.class.getName()).log(Level.SEVERE, null, ex);
        }
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
    
    private void notifyListenersOnMissingPacketsRequestReceived(Inet4Address ipAddress, int port, List<SeqId> mprList) {
        for (InfoManagerListener listener : listeners) {
            listener.onMissingPacketsRequestReceived(ipAddress, port, mprList);
        }
    }
    
    private void notifyListenersOnChatMsgReceived(Inet4Address ipAddress, String msg) {
        for (InfoManagerListener listener : listeners) {
            listener.onChatMsgReceived(ipAddress, msg);
        }
    }
}
