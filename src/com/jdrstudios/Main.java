package com.jdrstudios;

public class Main {

    public static void main(String[] args) {
        final PropertiesHandler props = new PropertiesHandler();
        final LogHandler log = new LogHandler();
        final Boolean propsFileFound = props.getProperties(log);
        if(!propsFileFound) {
            log.Error("FATAL ERROR: Unable to load app.properties file!!!");
        }

        try {
            log.Notice("Initializing bot...");
            BrosBot bot = new BrosBot();

            log.Notice("Starting bot...");
            if(bot.initialize(props, log))
                bot.start();
        }
        catch(Exception e){
            log.Exception(e, "Fatal Error during startup!");
        }
    }

}
