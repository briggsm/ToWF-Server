package com.briggs_inc.towf_server;

import static com.briggs_inc.towf_server.PacketConstants.*;

/**
 *
 * @author briggsm
 */
public class SyncPcmAudioDataPayloadStorageContainer {
    private static final String TAG = "SyncPcmStorage";
    byte[][] pcmAudioDataPayloadStorage = new byte[0x10000][UDP_PAYLOAD_SIZE];
    
    SeqId nextSeqIdToAdd = new SeqId(0x0000);
    SeqId nextSeqIdToSend = new SeqId(0x0000);
    
    public SyncPcmAudioDataPayloadStorageContainer() {
        
    }
    
    public synchronized void addPayloadFromAudioData(byte[] audioData) {
        Util.putIntInsideByteArray(nextSeqIdToAdd.intValue, pcmAudioDataPayloadStorage[nextSeqIdToAdd.intValue], ADPL_HEADER_SEQ_ID_START-DG_DATA_HEADER_LENGTH, ADPL_HEADER_SEQ_ID_LENGTH, false);
        Util.putIntInsideByteArray(audioData.length, pcmAudioDataPayloadStorage[nextSeqIdToAdd.intValue], ADPL_HEADER_AUDIO_DATA_ALLOCATED_BYTES_START-DG_DATA_HEADER_LENGTH, ADPL_HEADER_AUDIO_DATA_ALLOCATED_BYTES_LENGTH, false);
        System.arraycopy(audioData, 0, pcmAudioDataPayloadStorage[nextSeqIdToAdd.intValue], ADPL_HEADER_LENGTH, audioData.length);
        nextSeqIdToAdd.incr();
    }
    
    public synchronized Boolean getNextPayloadToSend(byte[] dgData) {
        if (nextSeqIdToSend.isLessThanSeqId(nextSeqIdToAdd)) {
            System.arraycopy(pcmAudioDataPayloadStorage[nextSeqIdToSend.intValue], 0, dgData, DG_DATA_HEADER_LENGTH, UDP_PAYLOAD_SIZE);
            nextSeqIdToSend.incr();
            return true;
        } else {
            return false;
        }
    }
    
    public synchronized void getPayloadCopyToDgData(SeqId seqId, byte[] dgData) {
        // Get's data from pcmAudioDataPayloadStorage & copies it into dgData.
        System.arraycopy(pcmAudioDataPayloadStorage[seqId.intValue], 0, dgData, DG_DATA_HEADER_LENGTH, UDP_PAYLOAD_SIZE);
    }
}
