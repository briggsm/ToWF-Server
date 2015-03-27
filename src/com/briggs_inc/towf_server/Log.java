package com.briggs_inc.towf_server;

/**
 *
 * @author briggsm
 */
public class Log {
    private static final String TAG = "Log";
    
    public static final int LG_V = 0;
    public static final int LG_D = 10;
    public static final int LG_I = 20;
    public static final int LG_W = 30;
    public static final int LG_E = 40;
    
    
    // !!! Chage this value based on how much debugging info you want to see in the Output window !!!
    public static final int LG_LEVEL = LG_D;
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    

    private static void printToConsole(String tag, String msg, Boolean newLine) {
        System.out.print("<" + tag + ">" + msg);
        if (newLine) {
            System.out.println();
        }
    }
    
    public static void v(String tag, String msg) {
        v(tag, msg, true);
    }
    public static void v(String tag, String msg, Boolean newLine) {
        if (LG_V >= LG_LEVEL) {
            printToConsole(tag, "(V): " + msg, newLine);
        }
    }
    
    public static void d(String tag, String msg) {
        d(tag, msg, true);
    }
    public static void d(String tag, String msg, Boolean newLine) {
        if (LG_D >= LG_LEVEL) {
            printToConsole(tag, "(D): " + msg, newLine);
        }
    }
    
    public static void i(String tag, String msg) {
        i(tag, msg, true);
    }
    public static void i(String tag, String msg, Boolean newLine) {
        if (LG_I >= LG_LEVEL) {
            printToConsole(tag, "(I): " + msg, newLine);
        }
    }
    
    public static void w(String tag, String msg) {
        w(tag, msg, true);
    }
    public static void w(String tag, String msg, Boolean newLine) {
        if (LG_W >= LG_LEVEL) {
            printToConsole(tag, "(WARNING): " + msg, newLine);
        }
    }
    
    public static void e(String tag, String msg) {
        e(tag, msg, true);
    }
    public static void e(String tag, String msg, Boolean newLine) {
        if (LG_E >= LG_LEVEL) {
            printToConsole(tag, "(ERROR): " + msg, newLine);
        }
    }
}
