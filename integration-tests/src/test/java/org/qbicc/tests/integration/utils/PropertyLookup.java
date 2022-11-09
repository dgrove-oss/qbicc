package org.qbicc.tests.integration.utils;

import java.io.File;

/**
 * Utilities to lookup properties
 */
public class PropertyLookup {
    public static String getProperty(String[] alternatives, String defaultValue) {
        String prop = null;
        for (String p : alternatives) {
            String env = System.getenv().get(p);
            if (!isBlank(env)) {
                prop = env;
                break;
            }
            String sys = System.getProperty(p);
            if (!isBlank(sys)) {
                prop = sys;
                break;
            }
        }
        if (prop == null) {
            return defaultValue;
        }
        return prop;
    }

    public static String getBaseDir() {
        final String env = System.getenv().get("basedir");
        final String sys = System.getProperty("basedir");
        final String user = System.getProperty("user.dir");
        if (!isBlank(env)) {
            return new File(env).getParent();
        }
        if (!isBlank(sys)) {
            return new File(sys).getParent();
        }
        if (!isBlank(user)) {
            return new File(user).getParent();
        }
        throw new IllegalArgumentException("Unable to determine project.basedir.");
    }

    public static boolean isBlank(final String s) {
        return s == null || s.isBlank();
    }
}
