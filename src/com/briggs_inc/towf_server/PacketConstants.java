package com.briggs_inc.towf_server;

/**
 *
 * @author briggsm
 */
public class PacketConstants {
    public static final int DG_DATA_HEADER_LENGTH = 6;  // Bytes
    
    public static final int UDP_PACKET_SIZE = 512;
    public static final int UDP_HEADER_SIZE = 8;
    public static final int IPV4_HEADER_SIZE = 20;
    public static final int ETH_HEADER_SIZE = 14;
    public static final int UDP_DATA_SIZE = UDP_PACKET_SIZE - UDP_HEADER_SIZE - IPV4_HEADER_SIZE - ETH_HEADER_SIZE; //512-42=470
    public static final int UDP_PAYLOAD_SIZE = UDP_DATA_SIZE - DG_DATA_HEADER_LENGTH;  // 470-6=464
    
    public static final int INFO_DST_SOCKET_PORT_NUMBER = 7769;
    public static final int STARTING_STREAM_PORT_NUMBER = 7770;
    
    public static final int ToWF_AS_INT = 0x546F5746;  // "ToWF"
    
    public static final int DG_DATA_HEADER_ID_START = 0;  // "ToWF"
    public static final int DG_DATA_HEADER_ID_LENGTH = 4;
    public static final int DG_DATA_HEADER_CHANNEL_START = 4;  // Rsvd
    public static final int DG_DATA_HEADER_CHANNEL_LENGTH = 1; // Rsvd
    public static final int DG_DATA_HEADER_PAYLOAD_TYPE_START = 5;
    public static final int DG_DATA_HEADER_PAYLOAD_TYPE_LENGTH = 1;
    
    public static final int DG_DATA_HEADER_PAYLOAD_TYPE_PCM_AUDIO_FORMAT = 0;
    public static final int DG_DATA_HEADER_PAYLOAD_TYPE_PCM_AUDIO_DATA_REGULAR = 1;
    public static final int DG_DATA_HEADER_PAYLOAD_TYPE_LANG_PORT_PAIR = 2;
    public static final int DG_DATA_HEADER_PAYLOAD_TYPE_CLIENT_LISTENING = 3;
    public static final int DG_DATA_HEADER_PAYLOAD_TYPE_MISSING_PACKETS_REQUEST = 4;
    public static final int DG_DATA_HEADER_PAYLOAD_TYPE_PCM_AUDIO_DATA_MISSING = 5;  // When Server resends a missing packet, it uses this type of payload.
    public static final int DG_DATA_HEADER_PAYLOAD_TYPE_ENABLE_MPRS = 6;
    public static final int DG_DATA_HEADER_PAYLOAD_TYPE_CHAT_MSG = 7;
            
    // Audio Format Payload Constants
    public static final int AFPL_SAMPLE_RATE_START = DG_DATA_HEADER_LENGTH + 0;
    public static final int AFPL_SAMPLE_RATE_LENGTH = 4;
    public static final int AFPL_TOAL_PAYLOAD_LENGTH = AFPL_SAMPLE_RATE_LENGTH;
    
    // OS Constants
    public static final int OS_OTHER = 0;
    public static final int OS_IOS = 1;
    public static final int OS_ANDROID = 2;
    
    // Audio Data Payload Constants
    public static final int ADPL_HEADER_SEQ_ID_START = DG_DATA_HEADER_LENGTH + 0;
    public static final int ADPL_HEADER_SEQ_ID_LENGTH = 2;
    public static final int ADPL_HEADER_AUDIO_DATA_ALLOCATED_BYTES_START = DG_DATA_HEADER_LENGTH + 2;
    public static final int ADPL_HEADER_AUDIO_DATA_ALLOCATED_BYTES_LENGTH = 2;
    public static final int ADPL_HEADER_LENGTH = ADPL_HEADER_SEQ_ID_LENGTH + ADPL_HEADER_AUDIO_DATA_ALLOCATED_BYTES_LENGTH;
    public static final int ADPL_AUDIO_DATA_START = DG_DATA_HEADER_LENGTH + ADPL_HEADER_LENGTH;
    public static final int ADPL_AUDIO_DATA_AVAILABLE_SIZE = UDP_PAYLOAD_SIZE - ADPL_HEADER_LENGTH; //464-4=460

    // Lang/Port Pairs Constants
    public static final int LPP_NUM_PAIRS_START = DG_DATA_HEADER_LENGTH + 0;
    public static final int LPP_NUM_PAIRS_LENGTH = 1;
    public static final int LPP_RSVD0_START = DG_DATA_HEADER_LENGTH + 1;
    public static final int LPP_RSVD0_LENGTH = 1;
    public static final int LPP_LANG0_START = DG_DATA_HEADER_LENGTH + 2;
    public static final int LPP_LANG_LENGTH = 16;
    public static final int LPP_PORT0_START = DG_DATA_HEADER_LENGTH + 18;
    public static final int LPP_PORT_LENGTH = 2;
    
    // Client Listening Payload
    public static final int CLPL_IS_LISTENING_START = DG_DATA_HEADER_LENGTH + 0;
    public static final int CLPL_IS_LISTENING_LENGTH = 1;
    public static final int CLPL_OS_TYPE_START = DG_DATA_HEADER_LENGTH + 1;
    public static final int CLPL_OS_TYPE_LENGTH = 1;
    public static final int CLPL_PORT_START = DG_DATA_HEADER_LENGTH + 2;
    public static final int CLPL_PORT_LENGTH = 2;
    public static final int CLPL_OS_VERSION_STR_START = DG_DATA_HEADER_LENGTH + 4;
    public static final int CLPL_OS_VERSION_STR_LENGTH = 8;
    public static final int CLPL_HW_MANUFACTURER_STR_START = DG_DATA_HEADER_LENGTH + 12;
    public static final int CLPL_HW_MANUFACTURER_STR_LENGTH = 16;
    public static final int CLPL_HW_MODEL_STR_START = DG_DATA_HEADER_LENGTH + 28;
    public static final int CLPL_HW_MODEL_STR_LENGTH = 16;
    public static final int CLPL_USERS_NAME_START = DG_DATA_HEADER_LENGTH + 44;
    public static final int CLPL_USERS_NAME_LENGTH = 32;
    
    // Missing Packets Request Payload
    public static final int MPRPL_NUM_MISSING_PACKETS_START = DG_DATA_HEADER_LENGTH + 0;
    public static final int MPRPL_NUM_MISSING_PACKETS_LENGTH = 1;
    public static final int MPRPL_RSVD0_START = DG_DATA_HEADER_LENGTH + 1;
    public static final int MPRPL_RSVD0_LENGTH = 1;
    public static final int MPRPL_PORT_START = DG_DATA_HEADER_LENGTH + 2;
    public static final int MPRPL_PORT_LENGTH = 2;
    public static final int MPRPL_PACKET0_SEQID_START = DG_DATA_HEADER_LENGTH + 4;
    public static final int MPRPL_PACKET0_SEQID_LENGTH = 2;
    
    // Enable MPRs (Missing Packet Requests)
    public static final int ENMPRS_ENABLED_START = DG_DATA_HEADER_LENGTH + 0;
    public static final int ENMPRS_ENABLED_LENGTH = 1;
    
    // Chat Msg
    public static final int CHATMSG_MSG_START = DG_DATA_HEADER_LENGTH + 0;
}
