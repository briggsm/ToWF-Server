
package com.briggs_inc.towf_server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.table.AbstractTableModel;

import static com.briggs_inc.towf_server.PacketConstants.*;

/**
 *
 * @author briggsm
 */
public class ListeningClientsTableModel extends AbstractTableModel {

    private static final int COL_OS_TYPE = 0;
    private static final int COL_OS_VERSION = 1;
    private static final int COL_IP_ADDRESS = 2;
    private static final int COL_HW_MANUFACTURER = 3;
    private static final int COL_HW_MODEL = 4;
    private static final int COL_PORT = 5;
    private static final int COL_USERS_NAME = 6;
            
    // Column Names - NOTE: Make sure this order matches with the COL Constants above!
    List<String> columnNames = Arrays.asList(
            "OS Type",
            "OS Version",
            "IP Address",
            "HW Manufacturer",
            "HW Model",
            "Port",
            "User/Device Name");
    List<ListeningClientInfo> listeningClients = new ArrayList<ListeningClientInfo>();
    
    
    @Override
    public int getRowCount() {
        return listeningClients.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
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
            case COL_PORT:
                return lc.Port;
            case COL_USERS_NAME:
                return lc.UsersName;
            default:
                return null;
        }
    }

    @Override
    public String getColumnName(int column) {
        return columnNames.get(column);
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
}
