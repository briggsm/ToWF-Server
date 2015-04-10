/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.briggs_inc.towf_server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author briggsm
 */
public class SyncMissingPacketsSeqIdsContainer {
    private static final String TAG = "SyncMPSeqIdsCont";
    
    // This variable will potentially be accessed by multiple threads - make sure all access is synchronized!
    //List<Integer> missingPacketsList = new ArrayList<Integer>();
    List<SeqId> missingPacketsList = new ArrayList<SeqId>();
    
    public SyncMissingPacketsSeqIdsContainer() {
    
    }
    
    
    public synchronized int getSize() {
        return missingPacketsList.size();
    }
    
    /*
    public synchronized void add(SeqId s) {
        // Add & Sort
        missingPacketsList.add(s);
        Collections.sort(missingPacketsList);
    }
    */
    public synchronized void addList(List<SeqId> seqIdList) {
        Log.v(TAG, "addList()");
        if (seqIdList.size() > 0) {
            Log.v(TAG, " > 0");
            Boolean listChanged = false;
            
            // Add (if not already there)
            for (SeqId s : seqIdList) {
                Log.v(TAG, "  " + String.format("0x%04x", s.intValue));
                if (!missingPacketsList.contains(s)) {
                    Log.v(TAG, "   newly Added");
                    missingPacketsList.add(s);
                    listChanged = true;
                } else {
                    Log.v(TAG, "   already exists");
                }
            }
            
            // Sort (if list has more than 1 element && if listChanged)
            if (missingPacketsList.size() > 1 && listChanged) {
                Log.v(TAG, "  sorting");
                Collections.sort(missingPacketsList);
            }
        }
    }
    
    public synchronized SeqId popFirstSeqId() {
        if (this.getSize() > 0) {
            //SeqId firstSeqId = missingPacketsList.get(0);
            //missingPacketsList.remove(0);
            //return firstSeqId;
            return missingPacketsList.remove(0);  // Remove() also gives back the element that was removed.
        } else {
            return null;
        }
    }
    
    public synchronized String getAllSeqIdsAsHexString() {
        String s = "";
        for (SeqId seqId : missingPacketsList) {
            s += String.format("0x%04x, ", seqId.intValue);
        }
        
        return s;
    }
}
