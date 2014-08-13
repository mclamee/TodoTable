package com.wicky.tdl;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author williamz<quiet_dog@163.com> 2014-08-13
 */
public class ConfigUtil {
    private static final String BUNDLE_NAME = "configuration";

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    private ConfigUtil() {
    }

    public static String get(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public static Integer getInt(String key) {
        try {
            return Integer.valueOf(get(key));
        } catch (NumberFormatException e) {
            return 0;
        } catch (MissingResourceException e) {
            return null;
        }
    }

    public static Double getDouble(String key) {
        try {
            return Double.valueOf(get(key));
        } catch (NumberFormatException e) {
            return 0.0d;
        } catch (MissingResourceException e) {
            return null;
        }
    }
}
