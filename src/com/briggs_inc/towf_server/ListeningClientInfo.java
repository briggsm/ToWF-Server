package com.briggs_inc.towf_server;

import java.net.Inet4Address;
import java.util.Date;

/**
 *
 * @author briggsm
 */

// Note: Used as a STRUCT
public class ListeningClientInfo {
    // Received from Client itself
    public boolean IsListening;
    public int OsType;
    public int Port;
    public Inet4Address IPAddress;
    public String OsVersion;
    public String HwManufacturer;
    public String HwModel;
    public String UsersName;
    
    // Other info
    public Boolean EnableMPRs;
    public int NumMPRs;
    //public Boolean listeningToggle;
    public Date Time;

    @Override
    public boolean equals(Object obj) {
        //if (obj instanceof ListeningClientInfo && this.MACAddress.equalsIgnoreCase(((ListeningClientInfo)obj).MACAddress)) {
        if (obj instanceof ListeningClientInfo && this.IPAddress.equals(((ListeningClientInfo)obj).IPAddress)) {
            return true;
        }
        
        return false;
    }
}
