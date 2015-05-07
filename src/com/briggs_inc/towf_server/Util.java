
package com.briggs_inc.towf_server;

import static com.briggs_inc.towf_server.PacketConstants.*;
import java.nio.charset.Charset;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

/**
 *
 * @author briggsm
 */
public class Util {
	private static final String TAG = "Util";
    
	public static int getIntFromByteArray(byte[] b, int offset, int length, boolean bigEndian) {
        int i = 0;
        
        if (length > 4) {
            System.out.println("*Warning! length > 4 bytes. Data might not fit in an int! Data loss possible! length: " + length + ", offset: " + offset);
        }
        
        for (int ctr = 0; ctr < length; ctr++) {
            int pos = bigEndian ? length-1 - ctr : ctr;
            i += ((b[offset+pos] & 0xFF) << (8*ctr));
        }
        
        return i;
    }
	
	public static void writeDgDataHeaderToByteArray(byte[] ba, int payloadType) {
		putIntInsideByteArray(ToWF_AS_INT, ba, DG_DATA_HEADER_ID_START, DG_DATA_HEADER_ID_LENGTH, true);  // "ToWF"
        putIntInsideByteArray(0x00, ba, DG_DATA_HEADER_CHANNEL_START, DG_DATA_HEADER_CHANNEL_LENGTH, false);  // Rsvd (not used)
        putIntInsideByteArray(payloadType, ba, DG_DATA_HEADER_PAYLOAD_TYPE_START, DG_DATA_HEADER_PAYLOAD_TYPE_LENGTH, false);
	}
	
	public static void putIntInsideByteArray(int i, byte[] b, int offset, int length, boolean bigEndian) {
        // Note: if "i" is bigger than what will fit in "length" bytes, data will be lost
        // e.g. i=0x89ABCDEF, offset=8, length=4, bigEndian=false

        // Check for possible data loss & warn user if so
        if (i >= Math.pow(2, length * 8)) {
            Log.w(TAG, "*WARNING! Full int will not fit inside byte array! Data lost! i: " + i + ", length: " + length + ", boundary: " + Math.pow(2, length * 8));
        }

        for (int ctr = 0; ctr < length; ctr++) {
            int pos = bigEndian ? length - 1 - ctr : ctr;  // e.g. bigEndian=>4, littleEndian=0
            b[offset + pos] = (byte) ((i & (0xFF << (8 * ctr))) >> (8 * ctr));  // e.g. 1st time thru loop => i & 0xFF, 2nd time => i & 0xFF00, etc., then shift back to right same amt.
        }
    }
	
	public static void putNullTermStringInsideByteArray(String s, byte[] b, int offset, int maxLength) {
		// Assumes US-ASCII string
		
		int length = Math.min(s.length(), maxLength);
		int i;
		for (i = 0; i < length; i++) {
			b[offset + i] = (byte)s.charAt(i);
		}
		// Null-terminate it, if there's room.
		if (i < maxLength) {
			b[offset + i] = 0x00;
		}
	}

    public static String getNullTermStringFromByteArray(byte[] b, int offset, int maxLength) {
        // Assumes US-ASCII
        byte byt;
        int itr = 0;
        byte allBytes[] = new byte[512];  // Plenty big enough

        while ((byt = b[offset + itr]) != 0 && itr < maxLength) {
            allBytes[itr++] = byt;
        }
        
        return new String(allBytes, 0, itr, Charset.forName("US-ASCII"));
    }
    
    public static boolean isThisMixerInfoAnInputMixer (Mixer.Info mixerInfo) {
        Mixer m = AudioSystem.getMixer(mixerInfo);
        Line.Info[] lines = m.getTargetLineInfo();  // Lookup just the TARGET line info
        for (Line.Info li : lines){
            // Make sure this "target line" is a "DataTargetLine" (AND not a PORT) 'cuz that means this mixer is getting INPUT (from MICROPHONE, etc) & sending that data IN to this program via this "target line"
            if (li instanceof DataLine.Info) {
                // If it's usable, add it to our list.
                try {
                    m.open();
                    m.close();
                    // If we can open & close without exception, this is an input line which can be obtained.
                    return true;
                } catch (LineUnavailableException e) {
                    //System.out.println("Line unavailable.");
                    // Keep searching...
                }
            }
        }
        return false;
    }
}