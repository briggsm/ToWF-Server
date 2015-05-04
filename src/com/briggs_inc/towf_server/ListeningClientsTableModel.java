
package com.briggs_inc.towf_server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import static com.briggs_inc.towf_server.PacketConstants.*;
import java.net.Inet4Address;
import java.text.SimpleDateFormat;

/**
 *
 * @author briggsm
 */

interface ListeningClientsTableModelListener {
    public void onListeningClientEnableMPRsChanged(Inet4Address ipAddress, boolean isEnabled);
    public void onListeningClientChatMsgTyped(Inet4Address ipAddress, String msg);
}

public class ListeningClientsTableModel extends AbstractTableModel {
    private static final String TAG = "ListeningClientsTableModel";

    public static final int COL_OS_TYPE = 0;
    public static final int COL_OS_VERSION = 1;
    public static final int COL_IP_ADDRESS = 2;
    public static final int COL_HW_MANUFACTURER = 3;
    public static final int COL_HW_MODEL = 4;
    public static final int COL_LANGUAGE = 5;
    public static final int COL_USERS_NAME = 6;
    public static final int COL_ENABLE_MPRS = 7;
    public static final int COL_NUM_MPRS = 8;
    public static final int COL_CHAT = 9;
    //public static final int COL_LISTENING = 10;
    public static final int COL_TIME = 10;
            
    // Column Names - NOTE: Make sure this order matches with the COL Constants above!
    List<String> columnNames = Arrays.asList(
            "OS Type",
            "OS Ver",
            "IP Address",
            "HW Manufacturer",
            "HW Model",
            "Language",
            "User/Device Name",
            "Enable MPR's",
            "# MPR's",
            "Chat",
            //"Listening"
            "Time"
    );
            
    
    public final Object[] longishValues = {
        "Android",
        "8.8.8",
        "255.255.255.255",
        "Mitsubishi",
        "Device Model Name",
        "Romanian",
        "Michael's iPhone 3GS",
        true,
        "999,999",
        "Hey, how are you?",
        "23:59:00"
    };
    
    List<ListeningClientInfo> listeningClients = new ArrayList<ListeningClientInfo>();
    
    List<ListeningClientsTableModelListener> listeners = new ArrayList<>();
    
    List<LangPortPair> langPortPairsList;
    
    
    @Override
    public int getRowCount() {
        return listeningClients.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    /*
    * JTable uses this method to determine the default renderer/
    * editor for each cell.  If we didn't implement this method,
    * then the last column would contain text ("true"/"false"),
    * rather than a check box.
    */
    public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (listeningClients.size() > 0 && rowIndex < listeningClients.size()) {
            ListeningClientInfo lc = listeningClients.get(rowIndex);
            switch (columnIndex) {
                case COL_OS_TYPE:
                    switch (lc.OsType) {
                        case OS_IOS:
                            return "iOS";
                        case OS_ANDROID:
                            return "Android";
                        default:
                            return "Other";
                    }
                case COL_OS_VERSION:
                    return lc.OsVersion;
                case COL_IP_ADDRESS:
                    return lc.IPAddress.getHostAddress();
                case COL_HW_MANUFACTURER:
                    return lc.HwManufacturer;
                case COL_HW_MODEL:
                    return lc.HwModel;
                case COL_LANGUAGE:
                    for (int i = 0; i < langPortPairsList.size(); i++) {
                        if (lc.Port == langPortPairsList.get(i).Port) {
                            return langPortPairsList.get(i).Language;
                        }
                    }
                    return "???";
                case COL_USERS_NAME:
                    return lc.UsersName;
                case COL_ENABLE_MPRS:
                    return lc.EnableMPRs;
                case COL_NUM_MPRS:
                    return lc.NumMPRs;
                case COL_CHAT:
                    return "";  //!!!
                //case COL_LISTENING:
                //    return lc.listeningToggle;
                case COL_TIME:
                    return new SimpleDateFormat("HH:mm:ss").format(lc.Time);
                default:
                    return null;
            }
        } else {
			//Log.e(TAG, "Shouldn't get here. Looks like something is out of range. listeningClients.size: " + listeningClients.size() + ", rowIndex: " + rowIndex);
            //return 777;  // Shouldn't get here. Though if we do, this value shouldn't show up.
            
            // We get here while setting up column sizes. So will just return 0;
            return 0;
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        // Just for the columns that can be changed

        //data[row][col] = value;
        //fireTableCellUpdated(row, col);
        ListeningClientInfo lc = listeningClients.get(rowIndex);
        switch (columnIndex) {
            case COL_ENABLE_MPRS:
                lc.EnableMPRs = (Boolean) aValue;
                fireTableCellUpdated(rowIndex, columnIndex);
                notifyListenersOnListeningClientEnableMPRsChanged(lc.IPAddress, lc.EnableMPRs);
                break;
            case COL_CHAT:
                if (((String)aValue).length() > 0) {
                    notifyListenersOnListeningClientChatMsgTyped(lc.IPAddress, (String)aValue);
                }
            default:
                break;
        }
    }

    @Override
    public String getColumnName(int column) {
        return columnNames.get(column);
    }
    
    public boolean isCellEditable(int row, int col) {
        switch (col) {
            case COL_OS_TYPE:
                return false;
            case COL_OS_VERSION:
                return false;
            case COL_IP_ADDRESS:
                return false;
            case COL_HW_MANUFACTURER:
                return false;
            case COL_HW_MODEL:
                return false;
            case COL_LANGUAGE:
                return false;
            case COL_USERS_NAME:
                return false;
            case COL_ENABLE_MPRS:
                return true;
            case COL_NUM_MPRS:
                return false;
            case COL_CHAT:
                return true;
            //case COL_LISTENING:
            //    return true;
            case COL_TIME:
                return false;
            default:
                return false;
        }
    }
    
    public void addListeningClient(ListeningClientInfo listeningClientInfo) {
        // But only add if it's unique (same MAC & IP)
        for (ListeningClientInfo lc : listeningClients) {
            if (lc.equals(listeningClientInfo)) {
                return;  // Don't add to list because it already exists.
            }
        }
        
        listeningClients.add(listeningClientInfo);
    }
    
    public void removeListeningClient(ListeningClientInfo listeningClientInfo) {
        for (ListeningClientInfo lc : listeningClients) {
            if (lc.equals(listeningClientInfo)) {
                listeningClients.remove(lc);  // !!! ??? Note: not sure if we can/should remove an item while doing a for-each loop.
                return;
            }
        }
    }
    
    public void removeAllListeningClients() {
        listeningClients.clear();
    }
    
    public void incrListeningClientNumMPRs(Inet4Address ipAddress, int newMPRs) {
        int idx = getListeningClientIdxWithIpAddress(ipAddress);
        if (idx != -1) {
            listeningClients.get(idx).NumMPRs = listeningClients.get(idx).NumMPRs + newMPRs;
        }
    }
    
    private int getListeningClientIdxWithIpAddress (Inet4Address ipAddress) {
        for (int i = 0; i < listeningClients.size(); i++) {
            if (listeningClients.get(i).IPAddress.equals(ipAddress)) {
                return i;
            }
        }
        return -1;  // Not found
    }
    
    public void setLangPortPairsList(List<LangPortPair> list) {
        langPortPairsList = list;
    }
    
    public void globallyEnableMPRs(boolean enable) {
        // Enable/Disable MPR's for all current listening clients
        for (int i = 0; i < listeningClients.size(); i++) {
            listeningClients.get(i).EnableMPRs = enable;
        }
    }
    
    public void addListener(ListeningClientsTableModelListener listener) {
        listeners.add(listener);
    }
    
    private void notifyListenersOnListeningClientEnableMPRsChanged(Inet4Address ipAddress, boolean isEnabled) {
        for (ListeningClientsTableModelListener listener : listeners) {
            listener.onListeningClientEnableMPRsChanged(ipAddress, isEnabled);
        }
    }
    
    private void notifyListenersOnListeningClientChatMsgTyped(Inet4Address ipAddress, String msg) {
        for (ListeningClientsTableModelListener listener : listeners) {
            listener.onListeningClientChatMsgTyped(ipAddress, msg);
        }
    }
}
