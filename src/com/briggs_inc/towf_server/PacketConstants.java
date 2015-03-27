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
    
    public static final int PACKET_SEQ_ID_SIZE = 2;
    public static final int AUDIO_LENGTH_SIZE = 2;
    public static final int UDP_AUDIO_DATA_PAYLOAD_HEADER_SIZE = PACKET_SEQ_ID_SIZE + AUDIO_LENGTH_SIZE;
    public static final int UDP_AUDIO_DATA_AVAILABLE_SIZE = UDP_DATA_SIZE - DG_DATA_HEADER_LENGTH - UDP_AUDIO_DATA_PAYLOAD_HEADER_SIZE; //470-6-4=460 // Need to do this (subtract 6 also, even for MC) so same amt of audio is send via Multicast AND Broadcast packets

    
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
    public static final int DG_DATA_HEADER_PAYLOAD_TYPE_PCM_AUDIO_DATA = 1;
    public static final int DG_DATA_HEADER_PAYLOAD_TYPE_LANG_PORT_PAIR = 2;
    public static final int DG_DATA_HEADER_PAYLOAD_TYPE_CLIENT_LISTENING = 3;
    
    public static final int BCAFDG_SAMPLE_RATE_START = 6;
    public static final int BCAFDG_SAMPLE_RATE_LENGTH = 4;
    public static final int BCAFDG_SAMPLE_SIZE_IN_BITS_START = 10;
    public static final int BCAFDG_SAMPLE_SIZE_IN_BITS_LENGTH = 1;
    public static final int BCAFDG_CHANNELS_START = 11;
    public static final int BCAFDG_CHANNELS_LENGTH = 1;
    public static final int BCAFDG_SIGNED_START = 12;
    public static final int BCAFDG_SIGNED_LENGTH = 1;
    public static final int BCAFDG_BIG_ENDIAN_START = 13;
    public static final int BCAFDG_BIG_ENDIAN_LENGTH = 1;
    
    // OS Constants
    public static final int OS_OTHER = 0;
    public static final int OS_IOS = 1;
    public static final int OS_ANDROID = 2;
    
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
    
    // Lang/Port Pairs Constants
    public static final int LPP_NUM_PAIRS_START = DG_DATA_HEADER_LENGTH + 0;
    public static final int LPP_NUM_PAIRS_LENGTH = 1;
    public static final int LPP_RSVD0_START = DG_DATA_HEADER_LENGTH + 1;
    public static final int LPP_RSVD0_LENGTH = 1;
    public static final int LPP_LANG0_START = DG_DATA_HEADER_LENGTH + 2;
    public static final int LPP_LANG_LENGTH = 16;
    public static final int LPP_PORT0_START = DG_DATA_HEADER_LENGTH + 18;
    public static final int LPP_PORT_LENGTH = 2;
}
