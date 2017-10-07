package com.jdrstudios;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;

public class DatabaseHandler {
    Connection conn;
    LogHandler log;

    public void begin() {
        try {
            conn.setAutoCommit(false);
        }
        catch (SQLException e) {
            log.Exception(e, "Failed to begin transaction!");
        }
    }

    public void commit() {
        try {
            conn.commit();
        }
        catch (SQLException e) {
            log.Exception(e, "Failed to commit transaction!");
        }
    }

    public void rollback() {
        try {
            conn.rollback();
        }
        catch (SQLException e) {
            log.Exception(e, "Failed to  transaction!");
        }
    }

    public boolean getConnection(String dbConnectionString, String user, String password, LogHandler logger) {
        log = logger;
        try {
            if(dbConnectionString.contains("postrgresql"))
                Class.forName("org.postgresql.Driver");
            if(dbConnectionString.contains("mysql"))
                Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            log.Error("Driver NOT FOUND.  Please include in your project library path and try again. " + e.getMessage());
            return false;
        }

        try {
            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", password);
            conn = DriverManager.getConnection(dbConnectionString, props);
        }
        catch (SQLException e) {
            log.Exception(e, "Failed to establish connection!");
            return false;
        }
        return true;
    }

    public void closeConnection() {
        try {
            conn.close();
        }
        catch (SQLException e) {
            log.Exception(e, "Failed to close connection!");
        }
    }

    public Boolean addCommand(String mode, String type, String command, String response, Integer level, String creator) {
        PreparedStatement preparedStatement;
        String selectSQL = "insert into commands (mode, type, command, response, level, created, creator, enabled) " +
                           "values(?,?,?,?,?,?,?,'true')";

        try {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            preparedStatement = conn.prepareStatement(selectSQL);
            preparedStatement.setString(1, mode);
            preparedStatement.setString(2, type);
            preparedStatement.setString(3, command);
            preparedStatement.setString(4, response);
            preparedStatement.setInt(5, level);
            preparedStatement.setTimestamp(6, now);
            preparedStatement.setString(7, creator);

            // execute select SQL statement
            preparedStatement.execute();
            return true;
        } catch (SQLException e) {
            log.Exception(e, "Failed to process command...");
            return false;
        }
    }

    public Boolean disableCommand(String mode, String type, String command) {
        PreparedStatement preparedStatement;
        String selectSQL = "update commands set enabled='false' where mode=? and type=? and command=?";

        try {
            preparedStatement = conn.prepareStatement(selectSQL);
            preparedStatement.setString(1, mode);
            preparedStatement.setString(2, type);
            preparedStatement.setString(3, command);

            // execute select SQL statement
            preparedStatement.execute();
            return true;
        } catch (SQLException e) {
            log.Exception(e, "Failed to remove command...");
            return false;
        }
    }

    public Boolean deleteCommand(String mode, String type, String command) {
        PreparedStatement preparedStatement;
        String selectSQL = "delete from commands where mode=? and type=? and command=?";

        try {
            preparedStatement = conn.prepareStatement(selectSQL);
            preparedStatement.setString(1, mode);
            preparedStatement.setString(2, type);
            preparedStatement.setString(3, command);

            // execute select SQL statement
            preparedStatement.execute();
            return true;
        } catch (SQLException e) {
            log.Exception(e, "Failed to remove command...");
            return false;
        }
    }

    public Boolean enableCommand(String mode, String type, String command){
        PreparedStatement preparedStatement;
        String selectSQL = "update commands set enabled='true' where mode=? and type=? and command=?";

        try {
            preparedStatement = conn.prepareStatement(selectSQL);
            preparedStatement.setString(1, mode);
            preparedStatement.setString(2, type);
            preparedStatement.setString(3, command);

            // execute select SQL statement
            preparedStatement.execute();
            return true;
        } catch (SQLException e) {
            log.Exception(e, "Failed to enable command...");
            return false;
        }
    }

    public Boolean addMode(String mode, String creator) {
        PreparedStatement preparedStatement;
        String selectSQL = "insert into modes (mode, created, creator, enabled) " +
                "values(?,?,?,'true')";

        try {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            preparedStatement = conn.prepareStatement(selectSQL);
            preparedStatement.setString(1, mode);
            preparedStatement.setTimestamp(2, now);
            preparedStatement.setString(3, creator);

            // execute select SQL statement
            preparedStatement.execute();
            return true;
        } catch (SQLException e) {
            log.Exception(e, "Failed to add mode...");
            return false;
        }
    }

    public Boolean disableMode(String mode) {
        PreparedStatement preparedStatement;
        String selectSQL = "update modes set enabled='false' where mode=?";

        try {
            preparedStatement = conn.prepareStatement(selectSQL);
            preparedStatement.setString(1, mode);

            // execute select SQL statement
            preparedStatement.execute();
            return true;
        } catch (SQLException e) {
            log.Exception(e, "Failed to remove mode...");
            return false;
        }
    }

    public Boolean deleteMode(String mode) {
        PreparedStatement preparedStatement;
        String selectSQL = "delete from modes where mode=?";

        try {
            preparedStatement = conn.prepareStatement(selectSQL);
            preparedStatement.setString(1, mode);

            // execute select SQL statement
            preparedStatement.execute();
            return true;
        } catch (SQLException e) {
            log.Exception(e, "Failed to remove mode...");
            return false;
        }
    }

    public Boolean enableMode(String mode) {
        PreparedStatement preparedStatement;
        String selectSQL = "update commands set enabled='true' where mode=?";

        try {
            preparedStatement = conn.prepareStatement(selectSQL);
            preparedStatement.setString(1, mode);

            // execute select SQL statement
            preparedStatement.execute();
            return true;
        } catch (SQLException e) {
            log.Exception(e, "Failed to enable mode...");
            return false;
        }
    }

    public String processCommand(String mode, String type, String command, Integer level) {
        PreparedStatement preparedStatement;
        String selectSQL = "select response from commands where mode=? and type=? and command=? and level<=? and enabled='true'";
        String response = "";

        try {
            preparedStatement = conn.prepareStatement(selectSQL);
            preparedStatement.setString(1, mode);
            preparedStatement.setString(2, type);
            preparedStatement.setString(3, command);
            preparedStatement.setInt(4, level);

            // execute select SQL statement
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                response = rs.getString("response");
            }


        } catch (SQLException e) {
            log.Exception(e, "Failed to process command...");
        }
        return response;
    }

    public String listCommands(String mode, String type, Integer level, Boolean includeEnabled, Boolean includeDisabled) {
        PreparedStatement preparedStatement;
        String selectSQL = "select command from commands where mode=? and type=? and level<=?";

        if(!includeEnabled && !includeDisabled)
            return "";

        if(includeDisabled && !includeEnabled)
            selectSQL = selectSQL + " and enabled='false'";
        if(!includeDisabled && includeEnabled)
            selectSQL = selectSQL + " and enabled='true'";

        String commands = "";

        try {
            preparedStatement = conn.prepareStatement(selectSQL);
            preparedStatement.setString(1, mode);
            preparedStatement.setString(2, type);
            preparedStatement.setInt(3, level);

            // execute select SQL statement
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                if(commands.equals(""))
                    commands = commands + rs.getString("command").trim();
                else
                    commands = commands + ", " + rs.getString("command").trim();
                log.Debug(rs.getString("command"));
            }


        } catch (SQLException e) {
            log.Exception(e, "Failed to retrieve commands...");
        }
        return commands;
    }

    public String listModes(Boolean includeEnabled, Boolean includeDisabled) {
        PreparedStatement preparedStatement;
        String selectSQL = "select mode from modes";

        if(!includeDisabled && !includeEnabled)
            return "";

        if(includeEnabled && includeDisabled){
            //query is good as-is
        }
        else {
            if (includeDisabled)
                selectSQL = selectSQL + " where enabled='false'";
            if (includeEnabled)
                selectSQL = selectSQL + " where enabled='true'";
        }
        String modes = "";

        try {
            preparedStatement = conn.prepareStatement(selectSQL);

            // execute select SQL statement
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                if(modes.equals(""))
                    modes = modes + rs.getString("mode");
                else
                    modes = modes + ", " + rs.getString("mode");
            }


        } catch (SQLException e) {
            log.Exception(e, "Failed to retrieve modes...");
        }
        return modes;
    }

    public String findCommands(String search, Integer level, Boolean includeEnabled, Boolean includeDisabled) {
        PreparedStatement preparedStatement;
        String selectSQL = "select mode, command, level from commands where command like ? and level <=?";
        if(includeDisabled && !includeEnabled)
            selectSQL = selectSQL + " and enabled='false'";
        if(!includeDisabled && includeEnabled)
            selectSQL = selectSQL + " and enabled='true'";
        selectSQL = selectSQL + " order by mode asc, type asc";

        String response = "";
        String mode = "";

        log.Debug(selectSQL);
        try {
            preparedStatement = conn.prepareStatement(selectSQL);
            preparedStatement.setString(1, "%" + search + "%");
            preparedStatement.setInt(2, level);

            // execute select SQL statement
            ResultSet rs = preparedStatement.executeQuery();
            log.Debug(preparedStatement.toString());
            while (rs.next()) {
                String thisMode = rs.getString("mode");
                String thisCommand = rs.getString("command");
                String audience = "";
                switch (rs.getInt("level")){
                    case 0:
                        audience = "bot";
                        break;
                    case 1:
                        audience = "all";
                        break;
                    case 2:
                        audience = "mod";
                        break;
                    case 3:
                        audience = "streamer";
                        break;
                }

                if(!mode.equals(thisMode)) {
                    if(!response.equals(""))
                        response = response + ", ";
                    response = response + "Mode [" + thisMode + "]: " + thisCommand + " (" + audience + ")";
                }
                else
                    response = response + ", " + thisCommand + " ( " + audience + ")";
                mode = thisMode;
            }
        } catch (SQLException e) {
            log.Exception(e, "Failed to search commands...");
        }
        return response;
    }

    public String findModes(String search, Boolean includeEnabled, Boolean includeDisabled) {
        PreparedStatement preparedStatement;
        String selectSQL = "select mode from modes where mode like ?";

        if(includeDisabled && !includeEnabled)
            selectSQL = selectSQL + " and enabled='false'";
        if(!includeDisabled && includeEnabled)
            selectSQL = selectSQL + " and enabled='true'";

        String modes = "";

        try {
            preparedStatement = conn.prepareStatement(selectSQL);
            preparedStatement.setString(1, "%" + search + "%");

            log.Debug(preparedStatement.toString());
            // execute select SQL statement
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                if(modes.equals(""))
                    modes = modes + rs.getString("mode");
                else
                    modes = modes + ", " + rs.getString("mode");
            }

        } catch (SQLException e) {
            log.Exception(e, "Failed to search modes...");
        }
        return modes;
    }

    public String findCommand(String mode, String command, Boolean includeEnabled, Boolean includeDisabled) {
        PreparedStatement preparedStatement;
        String selectSQL = "select enabled from commands where mode=? and command=?";

        if(includeDisabled && !includeEnabled)
            selectSQL = selectSQL + " and enabled='false'";
        if(!includeDisabled && includeEnabled)
            selectSQL = selectSQL + " and enabled='true'";

        String result;
        String state = "notexist";

        try {
            preparedStatement = conn.prepareStatement(selectSQL);
            preparedStatement.setString(1, mode);
            preparedStatement.setString(2, command);

            // execute select SQL statement
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                result = rs.getString("enabled");
                if(result.equals("true"))
                    state="enabled";
                else
                    state="disabled";
            }
        } catch (SQLException e) {
            log.Exception(e, "Failed to search commands...");
        }
        return state;
    }

    public String findMode(String mode, Boolean includeEnabled, Boolean includeDisabled) {
        PreparedStatement preparedStatement;
        String selectSQL = "select enabled from modes where mode=?";

        if(!includeDisabled && !includeEnabled)
            return "";

        if(includeEnabled && includeDisabled){
            //query is good as-is
        }
        else {
            if (includeDisabled)
                selectSQL = selectSQL + " and enabled='false'";
            if (includeEnabled)
                selectSQL = selectSQL + " and enabled='true'";
        }

        String result;
        String state = "notexist";

        try {
            preparedStatement = conn.prepareStatement(selectSQL);
            preparedStatement.setString(1, mode);

            // execute select SQL statement
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                result = rs.getString("enabled");
                log.Debug(result);
                if(result.equals("true"))
                    state="enabled";
                else
                    state="disabled";
            }
        } catch (SQLException e) {
            log.Exception(e, "Failed to search modes...");
        }
        return state;
    }

    public String findCommandsAddedByUser(String user, Boolean includeEnabled, Boolean includeDisabled) {
        PreparedStatement preparedStatement;
        String selectSQL = "select distinct command from commands where creator=? order by mode asc, command asc";

        if(includeDisabled && !includeEnabled)
            selectSQL = selectSQL + " and enabled='false'";
        if(!includeDisabled && includeEnabled)
            selectSQL = selectSQL + " and enabled='true'";
        String commands = "";

        try {
            preparedStatement = conn.prepareStatement(selectSQL);
            preparedStatement.setString(1, user);
            // execute select SQL statement
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                if(commands.equals(""))
                    commands = commands + rs.getString("command");
                else
                    commands = commands + ", " + rs.getString("command");
            }

        } catch (SQLException e) {
            log.Exception(e, "Failed to search commands...");
        }
        return "Commands created by " + user + ": " + commands;
    }

}