package com.briggs_inc.towf_server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author briggsm
 */
public class DescriptiveNetworkInterface {
    String name;  // According to system
    String description;  // Something more humanly understandable

    public DescriptiveNetworkInterface(String name) {
        this.name = name;
        description = "";

        try {
            // If running on OS X, need to do 'hack' to get a nice description.
            if (System.getProperty("os.name").startsWith("Mac")) {
                description = getHardwarePortForDeviceName(name);
            } else {
                NetworkInterface ni = NetworkInterface.getByName(name);
                if (ni != null) {
                    description = ni.getDisplayName();
                }
            }
        } catch (SocketException ex) {
            Logger.getLogger(DescriptiveNetworkInterface.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DescriptiveNetworkInterface.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getName() {
        return name;
    }
           
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description + " (" + name + ")";
    }

    private String getHardwarePortForDeviceName(String name) throws IOException {
        // Make sure we're running on a Mac
        if (System.getProperty("os.name").startsWith("Mac")) {
            Runtime rt = Runtime.getRuntime();
            String[] commands = {"networksetup", "-listallhardwareports"};
            Process proc = rt.exec(commands);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            
            String lastLine = "";
            String currLine;
            while ((currLine = stdInput.readLine()) != null) {
                if (currLine.equalsIgnoreCase("Device: " + name)) {
                    return lastLine.substring("Hardware Port: ".length(), lastLine.length());
                }
                lastLine = currLine;
            }
        }
        return "";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DescriptiveNetworkInterface) {
            if ( ((DescriptiveNetworkInterface)obj).name.equalsIgnoreCase(this.name) ) {
                return true;
            }
        }
        return false;
    }
}

