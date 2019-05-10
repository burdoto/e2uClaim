package de.kaleidox.e2uClaim;

import java.io.IOException;
import java.util.Properties;

public final class Const {
    public static final String VERSION;

    static {
        try {
            Properties properties = new Properties();
            properties.load(ClassLoader.getSystemResourceAsStream("const.properties"));

            VERSION = properties.getProperty("version");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load resource e2uClaim/econst.properties", e);
        }
    }
}
