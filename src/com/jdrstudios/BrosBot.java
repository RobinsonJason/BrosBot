package com.jdrstudios;

//Twitch Chat API
import com.cavariux.twitchirc.Chat.Channel;
import com.cavariux.twitchirc.Chat.User;
import com.cavariux.twitchirc.Core.TwitchBot;

//Speedrun.com API
import com.tsunderebug.speedrun4j.game.Category;
import com.tsunderebug.speedrun4j.game.Game;
import com.tsunderebug.speedrun4j.game.GameList;
import com.tsunderebug.speedrun4j.game.Leaderboard;
import com.tsunderebug.speedrun4j.game.run.PlacedRun;
import com.tsunderebug.speedrun4j.game.run.Run;

//Other libraries
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * Created by Jason on 9/27/2017.
 */

public class BrosBot extends TwitchBot {
    DatabaseHandler db;
    PropertiesHandler props;
    LogHandler log;
    String mode = "default";
    LocalDateTime startTime;
    LocalDateTime totalUptime;
    LocalDateTime modeUptime;
    Channel channel;
    List<User> mods;

    public Boolean initialize(PropertiesHandler prop, LogHandler logger) {
        startTime = LocalDateTime.now();
        totalUptime = startTime;
        modeUptime = startTime;

        try {
            props = prop;
            log = logger;
            db = new DatabaseHandler();
            db.getConnection(props.dbString, props.dbUser, props.dbPassword, log);
            this.setUsername(props.botName);
            this.setOauth_Key(props.oauth);
        }
        catch(Exception e) {
            log.Exception(e, "Unable to set auth variables!");
            return false;
        }
        try{
            if (!props.clientId.equals(""))
                this.setClientID(props.clientId);
            log.Notice("Connecting bot [" + props.botName + "] to channel: " + props.channel);
            this.connect(props.url, props.port);
            log.Notice("Connection successful!  Listening for messages and commands...");
            log.Debug("Attempting to connect to channel: " + props.channel);
            channel = this.joinChannel(props.channel);
            if(channel == null)
                log.Error("Did not join channel!");
            else
                log.Notice("Joined channel: " + props.channel);
            if (!props.greeting.equals("none")) {
                this.sendMessage(props.greeting, channel);
            }
            mods = channel.getMods();
        }
        catch(Exception e) {
            log.Exception(e, "Unable to connect to and join channel!");
            return false;
        }
        return true;
    }

    @Override
    public void onMessage(User user, Channel channel, String message)
    {
        //Possible future use, but I think replying to messages could be a bad idea
    }

    @Override
    public void onCommand(User user, Channel channel, String message) {
        processMessage(user, channel, message);
    }

    private void processMessage(User user, Channel channel, String message) {
        Integer level = 1; //everyone
        String messageParts[] = message.split(" ");
        Boolean result;

        log.Debug(message);
        try {
            //Determine user level
            //Note: checking user.isMod(channel) and channel.isMod(user) are both time consuming
            //Hence why we are cacheing the mods list and checking if the user is in the list
            //MUCH faster.
            if (user.toString().equals(props.streamerName)) {
                level = 3; //streamer
            }
            else if (user.toString().equals(props.botName)) {
                    level = 0; //bot
            }
            else {
                if (!mods.contains(user)) {
                    //someone is attempting a mod command but they aren't in our cached mod list
                    //let's update it.  this might mean
                    mods = channel.getMods();
                }
                if(mods.contains(user))
                    level = 2; //mod
            }
        }
        catch(Exception e) {
            log.Exception(e, "Unable to determine user level");
        }

        //don't proceed unless we have an attempt of a command
        if(messageParts.length == 0)
            return;

        String command = messageParts[0].trim().toLowerCase();

        //don't allow bot to run commands, unless it's allowed
        if(level == 0 && (!props.botCanRunSpeedrunCommands || !command.equals("speedrun")))
            return;

        try {
            switch(command) {

                ///////////////////////////////////////////
                //            SPEEDRUN COMMANDS          //
                ///////////////////////////////////////////
                case "speedrun":
                    if(level >= props.permissionsSpeedrunSearch){
                        if(messageParts.length > 2) {
                            String subCommand = messageParts[1].trim().toLowerCase();
                            String search = String.join(" ", Arrays.copyOfRange(messageParts, 2, messageParts.length));
                            switch(subCommand) {
                                case "search":
                                    try {
                                        GameList games = new GameList().withName(search);
                                        Game[] results = games.getGames();
                                        String response = "";
                                        for (int x = 0; x < results.length; x++) {
                                            if (x > 0)
                                                response = response + ", ";
                                            response = response + "[" + getGameNameByAbbreviation(results[x].getAbbreviation())
                                                                      + " (" + results[x].getAbbreviation() + ")]";
                                            if (response.length() > 450) {
                                                response = response.substring(0, 446) + "...";
                                                break;
                                            }
                                        }
                                        if(response.equals(""))
                                            response = "No results for '" + search + "'";
                                        else
                                            response = "Searched for '" + search + "':  " + response;
                                        this.sendMessage(response, channel);
                                    } catch (Exception e) {
                                        this.sendMessage("No results for '" + search + "'", channel);
                                    }
                                    break;

                                case "categories":
                                    try {
                                        Game game = Game.fromID(search);
                                        Category[] results = game.getCategories().getCategories();
                                        String response = "";
                                        for (int x = 0; x < results.length; x++) {
                                            if (x > 0)
                                                response = response + ",";
                                            response = response + "[" + results[x].getName() + "]";
                                            if (response.length() > 450) {
                                                response = response.substring(0, 446) + "...";
                                                break;
                                            }
                                        }
                                        if(response.equals(""))
                                            response = "No results for '" + search + "'";
                                        else
                                            response = "Searched for '" + search + "':" + response;
                                        this.sendMessage(response, channel);
                                    } catch (Exception e) {
                                        this.sendMessage("No results for '" + search + "'", channel);
                                    }
                                    break;

                                case "record":
                                    if(messageParts.length > 3) {
                                        try {
                                            String gameAbbreviation = messageParts[2].trim();
                                            String categoryName = String.join("", Arrays.copyOfRange(messageParts, 3, messageParts.length)).replace(" ", "");
                                            String response = getTimeByCategoryNameAndGameAbbreviation(categoryName, gameAbbreviation);
                                            if (!response.equals(""))
                                                response = "World record for " + getGameNameByAbbreviation(gameAbbreviation) + " (" + categoryName + ") is " + response;
                                            else
                                                response = "No results for '" + search + "'.";
                                            this.sendMessage(response, channel);
                                        } catch (Exception e) {
                                            this.sendMessage("No results for '" + search + "'.", channel);
                                        }
                                    }else {
                                        this.sendMessage("Usage: !speedrun record [game abbreviation] [category]    Note: You can use !speedrun search and !speedrun categories to find a game/category.", channel);
                                    }
                                    break;

                                case "runner":
                                    if(messageParts.length > 2){
                                        String name = messageParts[2].trim();
                                        String game = "";
                                        Integer place;
                                        Boolean placeOmitted = false;

                                        //if the optional 'place' value wasn't passed in, let's just default to 1st place
                                        try {
                                            place = Integer.parseInt(messageParts[3].trim());
                                        }
                                        catch(Exception e) {
                                            place = 1;
                                            placeOmitted = true;
                                        }

                                        //'game name/abbreviation' is optional, so we don't need to error grabbing this value
                                        try {
                                            if(placeOmitted) {
                                                //if place was missing, then this is where we need to look for the search text
                                                game = String.join(" ", Arrays.copyOfRange(messageParts, 3, messageParts.length)).trim().toLowerCase();
                                            }
                                            else {
                                                game = String.join(" ", Arrays.copyOfRange(messageParts, 4, messageParts.length)).trim().toLowerCase();
                                            }
                                        }
                                        catch (Exception e){
                                        }

                                        try{
                                            com.tsunderebug.speedrun4j.user.User runner = new com.tsunderebug.speedrun4j.user.User().fromID(name);
                                            PlacedRun[] pb = runner.getPBs().getData();
                                            
                                            String highlights = "";
                                            for(int x = 0; x < pb.length; x++) {
                                                if(pb[x].getPlace() <= place) {
                                                    String gameName = getGameNameByAbbreviation(pb[x].getRun().getCategory().getGame().getAbbreviation());
                                                    if(gameName.trim().toLowerCase().contains(game)) {
                                                        if(!highlights.equals(""))
                                                            highlights = highlights + ", ";
                                                        String gameTime = parseTime(pb[x].getRun().getTimes().getPrimary());
                                                        highlights = highlights + "[" + gameName + " (" + gameTime + ") " + ordinal(pb[x].getPlace()) + "]";
                                                    }
                                                }
                                            }

                                            highlights = "PB's by " + name + ": " + highlights;
                                            if (highlights.length() > 450)
                                                highlights = highlights.substring(0, 446) + "...";
                                            this.sendMessage(highlights, channel);
                                        }
                                        catch (Exception e){
                                            this.sendMessage("No results for '" + name + "'.", channel);
                                        }
                                    }else {
                                        this.sendMessage("Usage: !speedrun runner [runner name] [optional: place 1/2/3] [optional: game name/abbreviation]", channel);
                                    }
                                    break;

                                default:
                                    this.sendMessage("Usage: !speedrun [search/record/categories]", channel);

                            }
                        } else {
                            switch(command) {
                                case "search":
                                    this.sendMessage("Usage: !speedrun search [search term for game name]    Note: This will give you game abbreviations.", channel);
                                    break;
                                case "categories":
                                    this.sendMessage("Usage: !speedrun categories [game name/abbreviation]    Note: This will give you category names.", channel);
                                    break;
                                case "record":
                                    this.sendMessage("Usage: !speedrun record [game abbreviation] [category]    Note: You can use !speedrun search and !speedrun categories to find a game/category.", channel);
                                    break;
                                case "runner":
                                    this.sendMessage("Usage: !speedrun runner [runner name] [optional: place 1/2/3] [optional: game name/abbreviation]", channel);
                                    break;
                            }
                        }
                    }
                    break;


                ///////////////////////////////////////////
                //              LIST COMMANDS            //
                ///////////////////////////////////////////

                //list all commands
                case "commands":
                    if (level >= props.permissionsCommandListAndSearch) {
                        String commands = db.listCommands(mode, "command", level, true, false);
                        if(level >= 2) {
                            if(!commands.equals(""))
                                commands = commands + ", ";
                            commands = commands + String.join(", ", permissionsParser(level));
                        }
                        if(!commands.equals(""))
                            this.sendMessage("Commands: " + commands, channel);
                        else
                            this.sendMessage("You have no power here, " + user.toString() + "... Kappa", channel);
                    }
                    break;

                //list all modes
                case "modes":
                    if (level >= props.permissionsModeListAndSearch) {
                        String modes = db.listModes(true, false);
                        if(modes.trim().equals(""))
                            modes = "default";
                        else
                            modes = "default, " + modes;
                        this.sendMessage(modes, channel);
                    }
                    break;


                ///////////////////////////////////////////
                //               COMMANDS                //
                ///////////////////////////////////////////

                //add a command
                case "add":
                    if (level >= props.permissionsCommandAdd) {
                        List<String> list = Arrays.asList(reserved());
                        if (messageParts.length >= 3) {
                            String subCommand = messageParts[1].trim().toLowerCase();
                            String permission = messageParts[2].toLowerCase();
                            String response = String.join(" ", Arrays.copyOfRange(messageParts, 3, messageParts.length));
                            if(!list.contains(subCommand)) {
                                String commandState = db.findCommand(mode, subCommand,true,true);
                                if(commandState.equals("notexist")) {
                                    Integer setLevel = -1;
                                    switch (permission) {
                                        case "all":
                                            setLevel = 1;
                                            break;
                                        case "mod":
                                            setLevel = 2;
                                            break;
                                        case "streamer":
                                            setLevel = 3;
                                            break;
                                    }
                                    if (setLevel == -1)
                                        this.sendMessage("Permission must be set to one of the following: all, mod, or streamer.  Type '!add' to check usage if necessary.", channel);
                                    else {
                                        result = db.addCommand(mode, "command", subCommand, response, setLevel, user.toString());
                                        if (!result)
                                            this.sendMessage("Unable to add command [" + subCommand + "], please try again in a moment.", channel);
                                        else
                                            this.sendMessage("Command [" + subCommand + "] added!", channel);
                                    }
                                }
                                else
                                    this.sendMessage("This command [" + subCommand + "] already exists, and is " + commandState, channel);
                            } else
                                this.sendMessage("Cannot add this command [" + subCommand + "], as it is a reserved command word.", channel);
                        }
                        else
                            this.sendMessage("Usage: '!add command [permissions: all/mod/streamer] response    Example: !add hello all Howdy!", channel);
                    }
                    break;

                //disable a command
                case "disable":
                    if (level >= props.permissionsCommandDisable) {
                        if (messageParts.length > 1) {
                            String subCommand = messageParts[1].trim().toLowerCase();
                            List<String> list = Arrays.asList(reserved());
                            if(!list.contains(subCommand)) {
                                String commandState = db.findCommand(mode, subCommand, true, true);
                                if(commandState.equals("enabled")) {
                                    result = db.disableCommand(mode, "command", subCommand);
                                    if (!result)
                                        this.sendMessage("Unable to remove command [" + subCommand + "], please try again in a moment.", channel);
                                    else
                                        this.sendMessage("Command [" + subCommand + "] has been disabled.", channel);
                                }
                                else {
                                    if(commandState.equals("disabled"))
                                        this.sendMessage("This command [" + subCommand + "] has already been disabled.", channel);
                                    else
                                        this.sendMessage("This command [" + subCommand + "] does not exist.", channel);
                                }
                            }
                            else
                                this.sendMessage("Cannot disable this command [" + subCommand + "], as it is a keyword.", channel);
                        } else {
                            this.sendMessage("Usage: '!disable command", channel);
                        }
                    }
                    break;

                //enable a command
                case "enable":
                    if (level >= props.permissionsCommandAdd) {
                        if (messageParts.length > 1) {
                            String subCommand = messageParts[1].trim().toLowerCase();
                            List<String> list = Arrays.asList(reserved());
                            if(!list.contains(subCommand)) {
                                String commandState = db.findCommand(mode, subCommand, false, true);
                                if (commandState.equals("disabled")) {
                                    //Mode is currently disabled, so we can enable it
                                    result = db.enableCommand(mode, "command", subCommand);
                                    if (!result)
                                        this.sendMessage("Unable to enable command [" + subCommand + "], please try again in a moment.", channel);
                                    else
                                        this.sendMessage("Command [" + subCommand + "] has been enabled.", channel);
                                } else {
                                    if (db.findCommand(mode, subCommand, true, false).equals("enabled"))
                                        this.sendMessage("This command [" + subCommand + "] is already enabled, silly!", channel);
                                    else
                                        this.sendMessage("Cannot enable command [" + subCommand + "]. This command does not exist.", channel);
                                }
                            }
                            else
                                this.sendMessage("This is a reserved keyword, silly.  It's always enabled!", channel);
                        } else
                            this.sendMessage("Usage: '!enable command", channel);
                    }
                    break;


                //delete a command
                case "remove":
                    if (level >= props.permissionsCommandDelete) {
                        if (messageParts.length > 1) {
                            String subCommand = messageParts[1].trim().toLowerCase();
                            List<String> list = Arrays.asList(reserved());
                            if(!list.contains(subCommand)) {
                                String commandState = db.findCommand(mode, subCommand, true, true);
                                if(commandState.equals("notexist"))
                                    this.sendMessage("This command [" + subCommand + "] does not exist.", channel);
                                else {
                                    result = db.deleteCommand(mode, "command", subCommand);
                                    if (!result)
                                        this.sendMessage("Unable to remove command [" + subCommand + "], please try again in a moment.", channel);
                                    else
                                        this.sendMessage("Command [" + subCommand + "] has been removed.", channel);
                                }
                            }
                            else
                                this.sendMessage("Cannot remove this command [" + subCommand + "], as it is a keyword.", channel);
                        } else {
                            this.sendMessage("Usage: '!remove command", channel);
                        }
                    }
                    break;

                ///////////////////////////////////////////
                //                 MODES                 //
                ///////////////////////////////////////////

                //add a mode
                case "addmode":
                    if (level >= props.permissionsModeAdd) {
                        if (messageParts.length > 1) {
                            String thisMode = messageParts[1].trim().toLowerCase();
                            if(!thisMode.equals("default")) {
                                String modeState = db.findMode(thisMode, true, true);
                                if (modeState.equals("notexist")) {
                                    //Mode does not exist, so we can add it
                                    result = db.addMode(thisMode, user.toString());
                                    if (!result)
                                        this.sendMessage("Unable to add mode [" + thisMode + "], please try again in a moment.", channel);
                                    else
                                        this.sendMessage("Mode [" + thisMode + "] added!", channel);
                                } else {
                                    //Mode already exists
                                    this.sendMessage("This mode [" + thisMode + "] already exists and is " + modeState + ".", channel);
                                }
                            }
                            else
                                this.sendMessage("Don't worry, 'default' is a protected mode type.  It isn't going anywhere.  You don't have to 'add' it. Kappa", channel);
                        } else
                            this.sendMessage("Usage: '!addmode mode", channel);
                    }
                    break;

                //remove a mode
                case "disablemode":
                    if (level >= props.permissionsModeDisable) {
                        if (messageParts.length > 1) {
                            String thisMode = messageParts[1].trim().toLowerCase();
                            if(!thisMode.equals("default")) {
                                String modeState = db.findMode(thisMode, true, true);
                                if (modeState.equals("enabled")) {
                                    //Mode is currently enabled, so we can disable it
                                    result = db.disableMode(thisMode);
                                    if(thisMode.equals(mode)) {
                                        mode = "default";
                                        setMode(mode);
                                    }
                                    if (!result)
                                        this.sendMessage("Unable to disable mode [" + thisMode + "], please try again in a moment.", channel);
                                    else
                                        this.sendMessage("Mode  [" + thisMode + "] disabled.", channel);
                                } else {
                                    if(modeState.equals("disabled"))
                                        this.sendMessage("This mode  [" + thisMode + "] has already been disabled.", channel);
                                    else
                                        this.sendMessage("This mode does not exist.", channel);
                                }
                            }
                            else
                                this.sendMessage("Cannot remove 'default' as it is a protected mode type.", channel);
                        } else {
                            this.sendMessage("Usage: '!disablemode mode", channel);
                        }
                    }
                    break;

                //enable a mode
                case "enablemode":
                    if (level >= props.permissionsModeAdd) {
                        if (messageParts.length > 1) {
                            String thisMode = messageParts[1].trim().toLowerCase();
                            if(!thisMode.equals("default")) {
                                String modeState = db.findMode(thisMode, true, true);
                                if (modeState.equals("disabled")) {
                                    //Mode is currently disabled, so we can enable it
                                    result = db.enableMode(thisMode);
                                    if (!result)
                                        this.sendMessage("Unable to enable mode [" + thisMode + "], please try again in a moment.", channel);
                                    else
                                        this.sendMessage("Mode [" + thisMode + "] has been enabled.", channel);
                                } else if (modeState.equals("enabled")) {
                                    //Mode already exists
                                    this.sendMessage("This mode [" + thisMode + "] is already enabled, silly!", channel);
                                } else {
                                    this.sendMessage("Cannot enable mode [" + thisMode + "]. This mode doesn't exist.", channel);
                                }
                            }
                            else
                                this.sendMessage("You can't disable 'default' mode, it is a protected mode type!", channel);
                        } else
                            this.sendMessage("Usage: '!enablemode mode", channel);
                    }
                    break;


                //delete a mode
                case "removemode":
                    if (level >= props.permissionsModeDelete) {
                        if (messageParts.length > 1) {
                            String thisMode = messageParts[1].trim().toLowerCase();
                            String modeState = db.findMode(thisMode, true, true);
                            if(modeState.equals("notexist")) {
                                if(thisMode.equals("default"))
                                    this.sendMessage("You can't remove 'default' mode, it is a protected mode type!", channel);
                                this.sendMessage("This mode [" + thisMode + "] does not exist.", channel);
                            }
                            else {
                                result = db.deleteMode(thisMode);
                                if(thisMode.equals(mode)) {
                                    mode = "default";
                                    setMode(mode);
                                }
                                if(result)
                                    this.sendMessage("Mode [" + thisMode + "] has been removed.", channel);
                                else
                                    this.sendMessage("Unable to remove mode [" + thisMode + "], please try again in a moment.", channel);
                            }
                        } else {
                            this.sendMessage("Usage: '!removemode mode", channel);
                        }
                    }
                    break;


                ///////////////////////////////////////////
                //                SEARCH                 //
                ///////////////////////////////////////////

                //search both commands and messages
                case "find":
                    if (messageParts.length > 1) {
                        if (level >= props.permissionsCommandListAndSearch) {
                            String search = messageParts[1].trim().toLowerCase();
                            String response = db.findCommands(search, level, true, false);
                            if(response.equals(""))
                                this.sendMessage("Searched for [" + search + "]: Command does not exist for any mode.", channel);
                            else
                                this.sendMessage(response, channel);
                        }
                    } else
                        this.sendMessage("Usage: '!find keyword", channel);
                    break;

                case "findmode":
                    if (messageParts.length > 1) {
                        if (level >= props.permissionsModeListAndSearch) {
                            String search = messageParts[1].trim().toLowerCase();
                            String response = db.findModes(search, true, false);
                            if(response.equals(""))
                                this.sendMessage("Searched for [" + search + "]: Mode does not exist.", channel);
                            else
                                this.sendMessage(response, channel);
                        }
                    } else
                        this.sendMessage("Usage: '!findmode keyword", channel);
                    break;


                ///////////////////////////////////////////
                //                UPTIME                 //
                ///////////////////////////////////////////

                //display uptime
                case "uptime":
                    if (level >= props.permissionsBasic) {
                        String totalTime = calcUptime(totalUptime);
                        String modeTime = calcUptime(modeUptime);
                        String response;
                        if (props.trackModeUptime) {
                            if(totalTime.equals(modeTime))
                                response = "Total Uptime: " + totalTime;
                            else
                                response = "Uptime for " + mode + ":" + modeTime + ", Total Uptime: " + totalTime;
                        } else
                            response = "Uptime: " + totalTime;
                        this.sendMessage(response, channel);
                    }
                    break;

                //set uptime
                case "setuptime":
                    if (level >= props.permissionsModifyUptime){
                        if(messageParts.length > 2) {
                            String modify = messageParts[1].trim().toLowerCase();
                            String time = messageParts[2].trim().toLowerCase();
                            LocalDateTime newTime;
                            try {
                                Integer offset;
                                if(time.equals("now"))
                                    offset = 0;
                                else
                                    offset = Integer.parseInt(time);
                                newTime = LocalDateTime.now().minusMinutes(offset);

                                Boolean success = false;
                                switch(modify){
                                    case "mode":
                                        modeUptime = newTime;
                                        this.sendMessage("Mode uptime has been update.", channel);
                                        success = true;
                                        break;

                                    case "total":
                                        totalUptime = newTime;
                                        this.sendMessage("Total uptime has been updated.", channel);
                                        success = true;
                                        break;

                                    case "both":
                                        modeUptime = newTime;
                                        totalUptime = newTime;
                                        this.sendMessage("Mode/Total uptime has been updated.", channel);
                                        success = true;
                                        break;
                                }
                                if(!success)
                                    this.sendMessage("Must specify 'mode', 'total', or 'both' when using the command.  Use !setuptime to see usage.", channel);
                            }
                            catch(Exception e){
                                this.sendMessage("Time value must be either 'now' or a number, in minutes, to specify the uptime", channel);
                            }
                        }
                        else {
                            this.sendMessage("Usage: !setuptime [mode/total/both] [uptime-in-minutes/now]", channel);
                        }
                    }
                    break;


                ///////////////////////////////////////////
                //             MODE CONTROL              //
                ///////////////////////////////////////////

                //set mode
                case "setmode":
                    if (messageParts.length > 1) {
                        String thisMode = messageParts[1].trim().toLowerCase();
                        if(!thisMode.equals("")) {
                            if (level >= props.permissionsModeChange) {
                                if (!mode.equals(thisMode))
                                    setMode(thisMode);
                                else
                                    this.sendMessage("Mode already set to '" + thisMode + "'", channel);
                            }
                        }
                    } else
                        this.sendMessage("Usage: '!setmode mode", channel);
                    break;

                //get mode
                case "mode":
                    if(level >= props.permissionsBasic)
                        this.sendMessage("Current mode: " + mode, channel);
                    break;


                ///////////////////////////////////////////
                //             USER CONTROL              //
                ///////////////////////////////////////////

                //ban
                case "ban":
                    if(level >= props.permissionsUserBan){
                        if (messageParts.length > 1) {
                            String userToBan = messageParts[1];
                            if(!userToBan.equals(props.botName) && !userToBan.equals(props.streamerName)) {
                                List<User> users = channel.getViewers();
                                for (User usr : users) {
                                    if (usr.toString().equals(userToBan)) {
                                        usr.ban(channel);
                                        if (props.sendMessageOnBanAndUnban.equals("true"))
                                            this.sendMessage(userToBan + " has been banned.", channel);
                                    }
                                }
                            }
                            else
                                this.sendMessage("Streamers and bots can't be banned via bot commands", channel);
                        } else
                            this.sendMessage("Usage: '!ban user_name", channel);
                    }
                    break;

                //unban
                case "unban":
                    if(level >= props.permissionsUserUnban){
                        if (messageParts.length > 1) {
                            String userToUnban = messageParts[1];
                            if(!userToUnban.equals(props.botName) && !userToUnban.equals(props.streamerName)) {
                                List<User> users = channel.getViewers();
                                for (User usr : users) {
                                    if (usr.toString().equals(userToUnban)) {
                                        usr.unBan(channel);
                                        if (props.sendMessageOnBanAndUnban.equals("true"))
                                            this.sendMessage(userToUnban + " is no longer banned.", channel);
                                    }
                                }
                            } else
                                this.sendMessage("Streamers and bots can't be unbanned via bot commands.", channel);
                        } else
                            this.sendMessage("Usage: '!unban user_name", channel);
                    }
                    break;


                ///////////////////////////////////////////
                //            STORED COMMANDS            //
                ///////////////////////////////////////////

                //process user-generated command
                default:
                    String response = db.processCommand(mode, "command", messageParts[0].toLowerCase(), level);
                    if (!response.equals(""))
                        if(response.startsWith("!"))
                            processMessage(user, channel, response.substring(1, response.length()));
                        else
                            this.sendMessage(response, channel);
                    break;

            }

        }
        catch(Exception e){
            log.Exception(e, "Unable to process command!");
        }
    }

    public Boolean setMode(String thisMode){
        try {
            if (thisMode.equals("default") || db.findMode(thisMode, true, true).equals("enabled")) {
                //This mode exists, let's swap modes
                mode = thisMode;

                //if we're swapping modes before our configured 'setuptime', then just set mode time to total time
                if (ChronoUnit.MINUTES.between(totalUptime, startTime) < props.setupTime) {
                    modeUptime = totalUptime;
                } else {
                    modeUptime = LocalDateTime.now();
                }
                this.sendMessage("Mode set to: " + thisMode, channel);
                return true;
            } else
                this.sendMessage("This mode does not exist, or has been disabled.", channel);
        }
        catch (Exception e){
            log.Exception(e, "Unable to change modes!");
        }
        return false;
    }

    public String calcUptime(LocalDateTime thisDate){
        LocalDateTime now = LocalDateTime.now();
        Long millenia = ChronoUnit.MILLENNIA.between(thisDate, now);
        Long centuries = ChronoUnit.CENTURIES.between(thisDate, now) % 10;
        Long decades = ChronoUnit.DECADES.between(thisDate, now) % 10;
        Long years = ChronoUnit.YEARS.between(thisDate, now) % 10;
        Long weeks = ChronoUnit.WEEKS.between(thisDate, now) % 52;
        Long days = ChronoUnit.DAYS.between(thisDate, now) % 7;
        Long hours = ChronoUnit.HOURS.between(thisDate, now) % 24;
        Long minutes = ChronoUnit.MINUTES.between(thisDate, now) % 60;
        String total = "";

        if(minutes > 0) {
            String min = " minutes ";
            if(minutes==1)
                min=" minute ";
            total = minutes.toString() + min;
        }
        if(hours > 0){
            String hour = " hours ";
            if (hours == 1)
                hour = " hour ";
            if (total.equals(""))
                total = hours.toString() + hour;
            else
                total = hours.toString() + hour + total;
        }
        if(days > 0) {
            String day = " days ";
            if(days==1)
                day=" day ";
            if (total.equals(""))
                total = days.toString() + day;
            else
                total = days.toString() + day + total;
        }
        if(weeks > 0) {
            String week = " weeks ";
            if(weeks==1)
                week=" week ";
            if (total.equals(""))
                total = weeks.toString() + week;
            else
                total = weeks.toString() + week + total;
        }
        if(years > 0) {
            String year = " years ";
            if(years==1)
                year=" year ";
            if (total.equals(""))
                total = years.toString() + year;
            else
                total = years.toString() + year + total;
        }
        if(decades > 0) {
            String decade = " decades ";
            if(decades==1)
                decade=" decade ";
            if (total.equals(""))
                total = decades.toString() + decade;
            else
                total = decades.toString() + decade + total;
        }
        if(centuries > 0) {
            String century = " centuries ";
            if(centuries==1)
                century=" century ";
            if (total.equals(""))
                total = centuries.toString() + century;
            else
                total = centuries.toString() + century + total;
        }
        if(millenia > 0) {
            String millennium = " millenia ";
            if(millenia==1)
                millennium =" millenium ";
            if (total.equals(""))
                total = millenia.toString() + millennium;
            else
                total = millenia.toString() + millennium + total;
        }
        if(total.equals(""))
            total = "Just now!";
        return total;
    }

    public String[] reserved(){
        return permissionsParser(99);
    }

    public String[] permissionsParser(Integer level) {
        List<String> myList = new ArrayList<>();
        if(level >= props.permissionsCommandAdd)
            myList.add("add");
        if(level >= props.permissionsCommandDisable)
            myList.add("disable");
        if(level >= props.permissionsCommandAdd)
            myList.add("enable");
        if(level >= props.permissionsCommandDelete)
            myList.add("remove");
        if(level >= props.permissionsModeAdd)
            myList.add("addmode");
        if(level >= props.permissionsModeDisable)
            myList.add("disablemode");
        if(level >= props.permissionsModeAdd)
            myList.add("enablemode");
        if(level >= props.permissionsModeDelete)
            myList.add("removemode");
        if(level >= props.permissionsModeChange)
            myList.add("setmode");
        if(level >= props.permissionsCommandListAndSearch) {
            myList.add("commands");
            myList.add("find");
        }
        if(level >= props.permissionsModeListAndSearch) {
            myList.add("modes");
            myList.add("findmode");
        }
        if(level >= props.permissionsBasic) {
            myList.add("mode");
            myList.add("uptime");
        }
        if(level >= props.permissionsModifyUptime)
            myList.add("setuptime");
        if(level >= props.permissionsSpeedrunSearch)
            myList.add("speedrun");
        if(level >= props.permissionsUserBan)
            myList.add("ban");
        if(level >= props.permissionsUserUnban)
            myList.add("unban");

        return myList.stream().toArray(String[]::new);
    }

    public String getGameIdFromAbbreviation(String search){
        String gameId="";

        try {
            Game[] games = GameList.withName(search).getGames();
            for(int x = 0; x < games.length; x++){
                if(games[x].getAbbreviation().equals(search))
                    gameId = games[x].getId();
            }
        }
        catch (Exception e){
            log.Exception(e, "Could not determine name of game using search string: " + search);
        }
        return gameId;
    }

    public String getCategoryIdByGameAndCategoryName(Game game, String categoryName){
        try {
            Category[] results = game.getCategories().getCategories();
            String catId = "";
            for (int x = 0; x < results.length; x++) {
                if (results[x].getName().trim().replace(" ", "").toLowerCase().equals(categoryName.trim().replace(" ","").toLowerCase())) {
                    catId = results[x].getId();
                    break;
                }
            }
            return catId;
        }
        catch(Exception e){
            log.Exception(e, "Could not retrieve Category ID!");
        }
        return "";
    }

    public String getGameNameByAbbreviation(String gameAbbreviation){
        try {
            Game game = new Game().fromID(getGameIdFromAbbreviation(gameAbbreviation));
            return game.getNames().get("twitch");
        }
        catch(Exception e){
            return "";
        }
    }

    public String parseTime(String time){
        return time.toLowerCase().replace("h","h ").replace("m","m ").replace("s","ms").replace(".","s ").replace("pt","");
    }
    public String getTimeByCategoryNameAndGameAbbreviation(String categoryName, String gameAbbreviation) {
        String response = "";
        try {
            Game game = new Game().fromID(getGameIdFromAbbreviation(gameAbbreviation));
            String catId = getCategoryIdByGameAndCategoryName(game, categoryName);
            if (!catId.equals("")) {
                Category cat = new Category().fromID(catId);
                Leaderboard leaderboard = new Leaderboard().forCategory(cat);
                PlacedRun[] runs = leaderboard.getRuns();
                String runtime = "";
                for (int x = 0; x < 5; x++) {
                    String players = "";
                    String link = "";
                    String date = "";
                    Run run = runs[x].getRun();
                    if(runtime.equals("") || runtime.equals(run.getTimes().getPrimary())) {
                        //Get Runtime
                        if (run.getTimes().getPrimary() != null)
                            runtime = run.getTimes().getPrimary();

                        //Get Date
                        try {
                            date = run.getDate();
                        }
                        catch(Exception e){
                        }

                        //Get Players for this run
                        for (int y = 0; y < run.getPlayers().length; y++) {
                            if (!players.equals(""))
                                players = players + ", ";
                            players = players + run.getPlayers()[y].getName();
                        }

                        //Links might not always be available, so let's soft error on this part
                        try {
                            link = run.getVideos().getLinks()[0].getUri();
                        } catch (Exception e) {
                        }

                        //Build response
                        if(!response.equals(""))
                            response = response + " and by " + players;
                        else
                            response = parseTime(runtime) + " by " + players;
                        if(!date.equals(""))
                            response = response + " on " + date;
                        if(!link.equals(""))
                            response = response + " " + link;
                    }
                    else //No more ties for #1
                        break;
                }
            }
            else {
                log.Error("Could not find game/category: " + gameAbbreviation + "/" + categoryName);
            }
        } catch (Exception e) {
            log.Exception(e, "Could not find times for category: " + categoryName);
        }
        return response;
    }

    //shamelessly grabbed from stackoverflow
    //https://stackoverflow.com/questions/6810336/is-there-a-way-in-java-to-convert-an-integer-to-its-ordinal
    public static String ordinal(int i) {
        String[] sufixes = new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + sufixes[i % 10];

        }
    }
}
