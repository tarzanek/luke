package org.getopt.luke;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;


/**
 * @author abial
 */
public class Prefs {

    public static final String LUKE_PREFS_FILE      = ".luke";
    private static final String HOME_DIR = System.getProperty("user.home");

    public static final String P_LAST_PWD       = "luke.last_pwd";
    public static final String P_MRU_ARRAY      = "luke.mru";
    public static final String P_MRU_SIZE       = "luke.mru_size";
    public static final String P_FONT_NAME      = "luke.fontname";
    public static final String P_FONT_SIZE      = "luke.fontsize";
    public static final String P_THEME          = "luke.theme";
    public static final String P_ANALYZER       = "luke.analyzer";
    public static final String P_FIELD          = "luke.field";
    
    private static Properties props = new Properties();
    
    private static String[][] defaults = {
            {P_MRU_SIZE, "10"},
            {P_FONT_NAME, "sansserif.plain"},
            {P_FONT_SIZE, "12"}
    };
    
    private static List<String> mruList = new ArrayList<String>();
    private static int mruMaxSize = 10;
    private static String prefsFile = HOME_DIR + "/" + LUKE_PREFS_FILE;
    
    public static void load() {
        load(prefsFile);
    }
    
    public static void load(String filename) {
        for (int i = 0; i < defaults.length; i++) {
            props.setProperty(defaults[i][0], defaults[i][1]);
        }
        try {
            props.load(new FileInputStream(filename));
            initMruList();
        } catch (Exception e) {
            // not found or corrupted, keep defaults
        }
    }
    
    private static void initMruList() {
        mruMaxSize = getInteger(P_MRU_SIZE, 10);
        String[] mrus = getPropertyArray(P_MRU_ARRAY);
        if (mrus != null && mrus.length > 0) {
            for (int i = 0;
                i < Math.min(mrus.length, mruMaxSize); i++) {
                mruList.add(mrus[i].intern());
            }
        }
    }
    
    public static void addToMruList(String value) {
        if (value == null || value.trim().equals("") ||
            mruList.contains(value.intern())) return;
        if (mruList.size() >= mruMaxSize) {
            mruList.remove(mruList.size() - 1);
        }
        mruList.add(0, value);
    }
    
    public static List<String> getMruList() {
        return Collections.unmodifiableList(mruList);
    }

    public static void save() throws Exception {
        setPropertyArray(P_MRU_ARRAY,
                (String[])mruList.toArray(new String[0]));
        props.store(new FileOutputStream(prefsFile), null);
    }
    
    public static int getInteger(String key, int defVal) {
        String val = props.getProperty(key);
        int iVal = defVal;
        if (val != null) {
            try {
                iVal = Integer.parseInt(val);
            } catch (Exception e) {};
        }
        return iVal;
    }
    
    public static boolean getBoolean(String key, boolean defVal) {
        String val = props.getProperty(key);
        boolean iVal = defVal;
        if (val != null) {
            iVal = Boolean.valueOf(val).booleanValue();
        }
        return iVal;
    }
    
    public static String[] getPropertyArray(String key) {
        int i = 0;
        ArrayList<String> v = new ArrayList<String>();
        String val = null;
        do {
            String iKey = key + "." + i;
            val = props.getProperty(iKey);
            if (val != null) {
                v.add(val);
            }
            i++;
        } while (val != null);
        if (v.size() == 0) return null;
        String[] res = new String[v.size()];
        return (String[])v.toArray(res);
    }
    
    public static void deletePropertyArray(String key) {
        String key1 = key + ".";
        for (Enumeration e = props.propertyNames(); e.hasMoreElements(); ) {
            String k = (String)e.nextElement();
            if (k.startsWith(key1)) {
                // TODO: should check for integer value part
                props.remove(k);
            }
        }
    }
    
    public static void setPropertyArray(String key, String[] values) {
        deletePropertyArray(key);
        if (values == null || values.length == 0) {
            return;
        }
        for (int i = 0; i < values.length; i++) {
            props.setProperty(key + "." + i, values[i]);
        }
    }
    
    public static String getProperty(String key) {
        return getProperty(key, null);
    }
    
    public static String getProperty(String key, String defVal) {
        return props.getProperty(key, defVal);
    }
    
    public static void setProperty(String key, String val) {
        props.setProperty(key, val);
    }
}
