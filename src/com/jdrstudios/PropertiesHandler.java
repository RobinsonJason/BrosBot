package com.jdrstudios;

import java.util.Properties;
import java.io.FileInputStream;

public class PropertiesHandler {

    String streamerName;
    String botName;
    String oauth;
    String channel;
    String url;
    Integer port;
    String clientId;
    String greeting;
    String dbString;
    String dbUser;
    String dbPassword;

    Integer permissionsMessageAdd;
    Integer permissionsMessageDisable;
    Integer permissionsMessageDelete;
    Integer permissionsMessageListAndSearch;
    Integer permissionsCommandAdd;
    Integer permissionsCommandDisable;
    Integer permissionsCommandDelete;
    Integer permissionsCommandListAndSearch;
    Integer permissionsModeAdd;
    Integer permissionsModeDisable;
    Integer permissionsModeDelete;
    Integer permissionsModeListAndSearch;
    Integer permissionsModeChange;
    Integer permissionsUserBan;
    Integer permissionsUserUnban;
    Integer permissionsBasic;
    Integer permissionsSpeedrunSearch;
    Integer permissionsModifyUptime;

    String sendMessageOnBanAndUnban;
    Boolean trackModeUptime;
    Integer setupTime;
    Boolean botCanRunSpeedrunCommands;

    Boolean errorsExist = false;
    LogHandler log;

    public Boolean getProperties(LogHandler logger) {
        Properties prop = new Properties();
        String propFileName = "app.properties";
        log=logger;

        try {

            FileInputStream in = new FileInputStream(propFileName);
            prop.load(in);

            //set values from properties file
            String logLevel = prop.getProperty("loglevel");
            log.SetLogLevel(logLevel);
            streamerName = prop.getProperty("streamername");
            botName = prop.getProperty("botname");
            url = prop.getProperty("url");
            if(url.equals(""))
                url = "irc.chat.twitch.tv";
            try {
                port = Integer.parseInt(prop.getProperty("port"));
            }
            catch(Exception e){
                //not specified so just use default
                port = 6667;
            }
            oauth = prop.getProperty("oauth");
            channel = prop.getProperty("channel");
            clientId = prop.getProperty("clientid");
            greeting = prop.getProperty("greeting");
            dbString = prop.getProperty("dbstring");
            dbUser = prop.getProperty("dbuser");
            dbPassword = prop.getProperty("dbpass");

            //Get permissions
            permissionsMessageAdd = getPermissionLevel(prop, "messageadd");
            permissionsMessageDisable = getPermissionLevel(prop, "messagedisable");
            permissionsMessageDelete = getPermissionLevel(prop, "messagedelete");
            permissionsMessageListAndSearch = getPermissionLevel(prop, "messagelistandsearch");
            permissionsCommandAdd = getPermissionLevel(prop, "commandadd");
            permissionsCommandDisable = getPermissionLevel(prop, "commanddisable");
            permissionsCommandDelete = getPermissionLevel(prop, "commanddelete");
            permissionsCommandListAndSearch = getPermissionLevel(prop, "commandlistandsearch");
            permissionsModeAdd = getPermissionLevel(prop, "modeadd");
            permissionsModeDisable = getPermissionLevel(prop, "modedisable");
            permissionsModeDelete = getPermissionLevel(prop, "modedelete");
            permissionsModeListAndSearch = getPermissionLevel(prop, "modelistandsearch");
            permissionsModeChange = getPermissionLevel(prop, "modechange");
            permissionsUserBan = getPermissionLevel(prop, "userban");
            permissionsUserUnban = getPermissionLevel(prop, "userunban");
            permissionsBasic = getPermissionLevel(prop, "basic");
            permissionsSpeedrunSearch = getPermissionLevel(prop, "speedrunsearch");
            permissionsModifyUptime = getPermissionLevel(prop, "modifyuptime");

            //Get other settings
            botCanRunSpeedrunCommands = false;
            trackModeUptime = false;
            sendMessageOnBanAndUnban = prop.getProperty("sendmessageonbanandunban");
            setupTime = Integer.parseInt(prop.getProperty("setuptime"));
            if(prop.getProperty("trackmodeuptime").toLowerCase().equals("true"))
                trackModeUptime = true;
            if(prop.getProperty("botcanrunspeedruncommands").toLowerCase().equals("true"))
                botCanRunSpeedrunCommands = true;

            in.close();
            if(errorsExist)
                return false;
            log.Notice("Properties file \'app.properties\' loaded successfully.");
        } catch (Exception e) {
            log.Exception(e, "Error loading properties!");
            return false;
        }
        return true;
    }

    private Integer getPermissionLevel(Properties prop, String name) {
        Integer result = -1;
        try {
            String permission = prop.getProperty(name);
            if (permission.equals("all"))
                result = 1;
            if (permission.equals("everyone"))
                result = 1;
            if (permission.equals("everybody"))
                result = 1;
            if (permission.equals("mod"))
                result = 2;
            if (permission.equals("streamer"))
                result = 3;
        }
        catch(Exception e){
            errorsExist = true;
            log.Exception(e, "Unable to parse property for '" + name + "'");
        }
        return result;
    }
}