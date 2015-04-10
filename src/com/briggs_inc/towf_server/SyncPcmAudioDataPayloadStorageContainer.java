/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.briggs_inc.towf_server;

import static com.briggs_inc.towf_server.PacketConstants.*;

/**
 *
 * @author briggsm
 */
public class SyncPcmAudioDataPayloadStorageContainer {
    private static final String TAG = "SyncPcmStorage";
    //byte[][] pcmAudioDataPayloadStorage = new byte[0x10000][UDP_DATA_SIZE];
    byte[][] pcmAudioDataPayloadStorage = new byte[0x10000][UDP_PAYLOAD_SIZE];
    
    SeqId nextSeqIdToAdd = new SeqId(0x0000);
    SeqId nextSeqIdToSend = new SeqId(0x0000);
    
    public SyncPcmAudioDataPayloadStorageContainer() {
        
    }
    
    /*
    public synchronized void addPayloadFromDgData(byte[] dgData) {
        int seqIdInt = Util.getIntFromByteArray(dgData, ADPL_HEADER_SEQ_ID_START, ADPL_HEADER_SEQ_ID_LENGTH, false);
        System.arraycopy(dgData, DG_DATA_HEADER_LENGTH, pcmAudioDataPayloadStorage[seqIdInt], 0, UDP_PAYLOAD_SIZE);
    }
    */
    
    public synchronized void addPayloadFromAudioData(byte[] audioData) {
        /*
        byte tempDgData[] = new byte[UDP_DATA_SIZE];
        Util.writeDgDataHeaderToByteArray(tempDgData, DG_DATA_HEADER_PAYLOAD_TYPE_PCM_AUDIO_DATA_REGULAR);
        Util.putIntInsideByteArray(nextSeqIdToAdd.intValue, tempDgData, ADPL_HEADER_SEQ_ID_START, ADPL_HEADER_SEQ_ID_LENGTH, false);
        Util.putIntInsideByteArray(audioData.length, tempDgData, ADPL_HEADER_AUDIO_DATA_ALLOCATED_BYTES_START, ADPL_HEADER_AUDIO_DATA_ALLOCATED_BYTES_LENGTH, false);
        */
        
        //Util.writeDgDataHeaderToByteArray(pcmAudioDataPayloadStorage[nextSeqIdToAdd.intValue], DG_DATA_HEADER_PAYLOAD_TYPE_PCM_AUDIO_DATA_REGULAR);
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
    
    //public synchronized void getPayloadCopy(SeqId seqId, byte[] dgData, int dgDataOffset, int length) {
    public synchronized void getPayloadCopyToDgData(SeqId seqId, byte[] dgData) {
        // Get's data from pcmAudioDataPayloadStorage & copies it into dgData (at the specified offset & length).
        
        //byte payloadData[] = new byte[UDP_PAYLOAD_SIZE];
        //System.arraycopy(pcmAudioDataPayloadStorage, 0, payloadData, 0, UDP_PAYLOAD_SIZE);
        //return payloadData;
        
        // length should be UDP_PAYLOAD_SIZE. If it's greater, that's a problem. If it's less, it "could" be ok, so we'll trust the caller.
        /*
        if (length > UDP_PAYLOAD_SIZE) {
            Log.e(TAG, "Hey! Can't copy '" + length + "' bytes from pcmAudioDataPayloadStorage. Too many. Not copying anything.");
            return;
        }
        */
        //System.arraycopy(pcmAudioDataPayloadStorage[seqId.intValue], 0, dgData, dgDataOffset, length);
        System.arraycopy(pcmAudioDataPayloadStorage[seqId.intValue], 0, dgData, DG_DATA_HEADER_LENGTH, UDP_PAYLOAD_SIZE);
    }
}
