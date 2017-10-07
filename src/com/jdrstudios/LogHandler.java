package com.jdrstudios;
import java.util.Date;

/**
 * Created by jason on 8/31/16.
 */
public class LogHandler {
    String logLevel = "info";

    public void Notice(String message) {
        Date time = new Date(System.currentTimeMillis());
        System.out.println("[" + time.toString() + "] NOTICE: " + message);
    }

    public void Info(String message) {
        if (!logLevel.toLowerCase().equals("error")) {
            Date time = new Date(System.currentTimeMillis());
            System.out.println("[" + time.toString() + "] INFO: " + message);
        }
    }

    public void Debug(String message) {
        if (logLevel.toLowerCase().equals("debug")) {
            Date time = new Date(System.currentTimeMillis());
            System.out.println("[" + time.toString() + "] DEBUG: " + message);
        }
    }

    public void Error(String message) {
        Date time = new Date(System.currentTimeMillis());
        System.out.println("[" + time.toString() + "] ERROR: " + message);
    }

    public void Exception(Exception e, String message) {
        Date time = new Date(System.currentTimeMillis());
        System.out.println("[" + time.toString() + "] ERROR: [" + e.getStackTrace()[0].getClassName() + ":" + e.getStackTrace()[0].getMethodName() + "] Line(" + e.getStackTrace()[0].getLineNumber() + ") " + message + " " + e.toString());
    }

    public void SetLogLevel(String level) {
        logLevel = level;
    }

}