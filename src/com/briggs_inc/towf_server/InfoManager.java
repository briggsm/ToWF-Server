package com.briggs_inc.towf_server;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.briggs_inc.towf_server.PacketConstants.*;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.sound.sampled.AudioFormat;

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
    
    InterfaceAddress networkInterfaceIPv4Address;
    
    DatagramSocket infoSocket;  // for passing info messages (e.g. sending avail. language/port pairs, receiving "client listening" packet)
    DatagramPacket infoRecvDgPk;
    DatagramPacket infoSendDgPk;
    byte recvDgPkData[] = new byte[UDP_DATA_SIZE];
    byte sendDgPkData[] = new byte[UDP_DATA_SIZE];
    
    DatagramPacket audioFormatDgPacket;
    DatagramPacket langPortPairsDgPacket;

    AudioFormat audioFormat;
    List<LangPortPair> langPortPairsList;
    Timer timedPacketsTimer;
    
    boolean isReceiving = false;
    
    List<InfoManagerListener> listeners = new ArrayList<InfoManagerListener>();

    public class SendTimedPacketsTask extends TimerTask {
        @Override
        public void run() {
            try {
                InfoManager.this.infoSocket.send(InfoManager.this.audioFormatDgPacket);
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
            while (InfoManager.this.isReceiving) {
                try {
                    infoSocket.receive(infoRecvDgPk);  // Hangs (blocks) here until packet is received... (but doesn't use CPU resources while blocking) [But since this should be on a thread of it's own, it's ok]
                    if (!InfoManager.this.isReceiving) {
                        return;  // All done.
                    }
                } catch (SocketException ex) {
                    if (ex.getMessage().equalsIgnoreCase("Socket closed")) {
                        Log.e(TAG, "Socket is closed. Done receiving.");
                        return;
                    } else {
                        Logger.getLogger(InfoManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(InfoManager.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                InfoManager.this.onInfoDgPkReceived(infoRecvDgPk);
            }
        }
    }
    
    public InfoManager(InterfaceAddress networkInterfaceIPv4Address, AudioFormat af, List<LangPortPair>lppList) throws SocketException {
        this.networkInterfaceIPv4Address = networkInterfaceIPv4Address;
        this.audioFormat = af;
        this.langPortPairsList = lppList;
        
        infoSocket = new DatagramSocket(INFO_PORT_NUMBER, networkInterfaceIPv4Address.getAddress());
        
        infoRecvDgPk = new DatagramPacket(recvDgPkData, recvDgPkData.length);
        infoSendDgPk = new DatagramPacket(sendDgPkData, sendDgPkData.length);
    }
    
    public void startReceiving() {
        Log.d(TAG, "startReceiving");

        isReceiving = true;
        
        // Better start a new thread, so we don't hang the gui while waiting for packets to be received.
        new Thread(new InfoReceiver()).start();
    }
    
    public void stopReceiving() {
        isReceiving = false;  // Should cause InfoReceiver thread to quit right after its next packet reception.
        if (infoSocket != null) {
            infoSocket.close();
        }
    }
    
    private void onInfoDgPkReceived(DatagramPacket dg) {
        Log.v(TAG, "onInfoDgPkReceived()");
        
        byte dgData[] = new byte[UDP_DATA_SIZE];
        dgData = dg.getData();
        
        // If it's from myself, ignore it
        // ^---- Note: on Windows (or maybe it's dependant on NIC's), the Server gets back the broadcasted packets that it sends. These will get caught here.
        Inet4Address dgIpAddress = (Inet4Address)dg.getAddress();  // get it from the Datagram's header
        //Log.d(TAG, "packet's ipAddress: " + ipAddress2);
        //Log.d(TAG, "my networkInterfaceIPv4Address: " + networkInterfaceIPv4Address);
        if (dgIpAddress.equals((Inet4Address)networkInterfaceIPv4Address.getAddress())) {
            //Log.d(TAG, "HEY, I sent this packet. Ignoring it.");
            return;
        }
        
        // Check for "ToWF" in Header
        int headerId = Util.getIntFromByteArray(dgData, DG_DATA_HEADER_ID_START, DG_DATA_HEADER_ID_LENGTH, true); 
        if (headerId != ToWF_AS_INT) {
            return;  // 'cuz not a packet for our 'ToWF' app
        }
        
        // Get Payload Type
        int payloadType = Util.getIntFromByteArray(dgData, DG_DATA_HEADER_PAYLOAD_TYPE_START, DG_DATA_HEADER_PAYLOAD_TYPE_LENGTH, false);
        
        switch (payloadType) {
            case DG_DATA_HEADER_PAYLOAD_TYPE_CLIENT_LISTENING:
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
                cInfo.Time = new Date();  // defaults to "now"
                
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
                Log.v(TAG, "Missing Packets request (" + numMissingPackets + ") came in from {" + ((Inet4Address)dg.getAddress()).getHostAddress() + "} on Port [" + port + "]: (" + s + ")");
                
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
    
    public void startBroadcastingTimedPackets() {
        // Audio Format
        buildAudioFormatDgPk();
        
        // LangPortPairs
        buildLangPortPairsDgPk();
        
        // Start the Timer
        timedPacketsTimer = new Timer();
        timedPacketsTimer.schedule(new SendTimedPacketsTask(), 0, 2000);  // Send now, and every 2000ms
    }

    public void stopBroadcastingTimedPackets() {
        timedPacketsTimer.cancel();
    }
    
    private void buildAudioFormatDgPk() {
        byte afArr[] = new byte[UDP_DATA_SIZE];
        
        Util.writeDgDataHeaderToByteArray(afArr, DG_DATA_HEADER_PAYLOAD_TYPE_PCM_AUDIO_FORMAT);
        Util.putIntInsideByteArray((int) audioFormat.getSampleRate(), afArr, AFPL_SAMPLE_RATE_START, AFPL_SAMPLE_RATE_LENGTH, false);
        
        int dataLength = DG_DATA_HEADER_LENGTH + AFPL_TOAL_PAYLOAD_LENGTH;
        audioFormatDgPacket = new DatagramPacket(afArr, dataLength, networkInterfaceIPv4Address.getBroadcast(), INFO_PORT_NUMBER);
    }
    
    private void buildLangPortPairsDgPk() {
        // Build langPort pair datagram packet
        byte lppArr[] = new byte[UDP_DATA_SIZE];
        
        // "ToWF" Header
        Util.writeDgDataHeaderToByteArray(lppArr, DG_DATA_HEADER_PAYLOAD_TYPE_LANG_PORT_PAIR);

        // # of Pairs
        Util.putIntInsideByteArray(langPortPairsList.size(), lppArr, LPP_NUM_PAIRS_START, LPP_NUM_PAIRS_LENGTH, false);
        
        // Rsvd bytes
        Util.putIntInsideByteArray(0x00, lppArr, LPP_RSVD0_START, LPP_RSVD0_LENGTH, false);
        
        // Server Version
        Log.d(TAG, "APP_VERSION: " + TowfServerFrame.APP_VERSION);
        Util.putNullTermStringInsideByteArray(TowfServerFrame.APP_VERSION, lppArr, LPP_SERVER_VERSION_START, LPP_SERVER_VERSION_LENGTH);
        
        // Language/Port pairs
        for (int ctr = 0; ctr < langPortPairsList.size(); ctr++) {
            // Language
            Util.putNullTermStringInsideByteArray(langPortPairsList.get(ctr).Language, lppArr, LPP_LANG0_START + (ctr*(LPP_LANG_LENGTH+LPP_PORT_LENGTH)), LPP_LANG_LENGTH);
            
            // Port
            Util.putIntInsideByteArray(langPortPairsList.get(ctr).Port, lppArr, LPP_PORT0_START + (ctr*(LPP_LANG_LENGTH+LPP_PORT_LENGTH)), LPP_PORT_LENGTH, false);
        }
        
        int dataLength = DG_DATA_HEADER_LENGTH + LPP_NUM_PAIRS_LENGTH + LPP_RSVD0_LENGTH + LPP_SERVER_VERSION_LENGTH + ((LPP_LANG_LENGTH + LPP_PORT_LENGTH) * langPortPairsList.size());
        langPortPairsDgPacket = new DatagramPacket(lppArr, dataLength, networkInterfaceIPv4Address.getBroadcast(), INFO_PORT_NUMBER);
    }
    
    public void sendEnableMPRs(Inet4Address ipAddress, boolean enabled) {
        byte enMPRsArr[] = new byte[UDP_DATA_SIZE];
        
        // "ToWF" Header
        Util.writeDgDataHeaderToByteArray(enMPRsArr, DG_DATA_HEADER_PAYLOAD_TYPE_ENABLE_MPRS);
        
        // Enabled / Disabled
        Util.putIntInsideByteArray(enabled ? 1 : 0, enMPRsArr, ENMPRS_ENABLED_START, ENMPRS_ENABLED_LENGTH, false);
        
        int dataLength = DG_DATA_HEADER_LENGTH + 1;
        DatagramPacket dgPk = new DatagramPacket(enMPRsArr, dataLength, ipAddress, INFO_PORT_NUMBER);
        
        // Try to send it out over the network
        try {
            infoSocket.send(dgPk);
        } catch (IOException ex) {
            Logger.getLogger(InfoManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void sendChatMsg(Inet4Address ipAddress, String msg) {
        byte chatMsgArr[] = new byte[UDP_DATA_SIZE];
        
        // "ToWF" Header
        Util.writeDgDataHeaderToByteArray(chatMsgArr, DG_DATA_HEADER_PAYLOAD_TYPE_CHAT_MSG);
        
        // Message
        Util.putNullTermStringInsideByteArray(msg, chatMsgArr, CHATMSG_MSG_START, msg.length() + 1);  // +1 for the null terminator
        
        int dataLength = DG_DATA_HEADER_LENGTH + msg.length() + 1;  // +1 for the null terminator
        DatagramPacket dgPk = new DatagramPacket(chatMsgArr, dataLength, ipAddress, INFO_PORT_NUMBER);
        
        // Try to send it out over the network
        try {
            infoSocket.send(dgPk);
        } catch (IOException ex) {
            Logger.getLogger(InfoManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void broadcastRLSPacket() {
        byte rlsArr[] = new byte[UDP_DATA_SIZE];
        
        // "ToWF" Header
        Util.writeDgDataHeaderToByteArray(rlsArr, DG_DATA_HEADER_PAYLOAD_TYPE_RLS);
        
        // NO PAYLOAD at this point
        
        int dataLength = DG_DATA_HEADER_LENGTH + 0;  // No payload
        DatagramPacket dgPk = new DatagramPacket(rlsArr, dataLength, networkInterfaceIPv4Address.getBroadcast(), INFO_PORT_NUMBER);
        
        // Try to send it out over the network
        try {
            infoSocket.send(dgPk);
        } catch (IOException ex) {
            Logger.getLogger(InfoManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void addListener(InfoManagerListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(InfoManagerListener listener) {
        listeners.remove(listener);
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
