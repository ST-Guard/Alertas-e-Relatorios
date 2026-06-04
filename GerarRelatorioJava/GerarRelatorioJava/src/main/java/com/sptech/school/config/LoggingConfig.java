package com.sptech.school.config;

import com.openhtmltopdf.util.XRLog;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class LoggingConfig {
    private LoggingConfig() {
    }

    public static void configurar() {
        XRLog.setLoggingEnabled(false);

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        System.setProperty("org.slf4j.simpleLogger.log.com.openhtmltopdf", "warn");
        System.setProperty("org.slf4j.simpleLogger.log.org.apache.pdfbox", "error");

        LogManager logManager = LogManager.getLogManager();
        Logger rootLogger = logManager.getLogger("");
        if (rootLogger != null) {
            rootLogger.setLevel(Level.WARNING);
            for (Handler handler : rootLogger.getHandlers()) {
                handler.setLevel(Level.WARNING);
            }
        }

        Logger.getLogger("com.openhtmltopdf").setLevel(Level.WARNING);
        Logger.getLogger("org.apache.pdfbox").setLevel(Level.SEVERE);
    }
}
