package com.wicky.tdl.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author williamz<quiet_dog@163.com> 2014-08-13
 */
public class ConfigUtil {
    private static final String FILE_PATH = System.getProperty("user.dir") + "/configuration.properties";

    private static Properties CONFIG = new Properties();

    static{
        BufferedInputStream inStream = null;
        try {
            inStream = new BufferedInputStream(new FileInputStream(FILE_PATH));
            CONFIG.load(inStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            if(inStream != null){
                try {
                    inStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private ConfigUtil() {
    }

    public static String get(String key) {
        String property = CONFIG.getProperty(key);
        if (property == null) return "";
        return property;
    }

    public static Integer getInt(String key) {
        try {
            return Integer.valueOf(get(key));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static Double getDouble(String key) {
        try {
            return Double.valueOf(get(key));
        } catch (NumberFormatException e) {
            return 0.0d;
        }
    }
}
