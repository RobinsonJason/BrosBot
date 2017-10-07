This is a project that I did for fun, and I don't maintain it.  If you find the code useful, great!  Have fun!  I recommend that you take the project and just run with it.  There's a lot that can be added to it, like polling your audience for example.

To set this project up, you'll need to use cavariux's twitchirc, and tsunderebug's speedrun4j.  Both of which are on github.  I just downloaded and built them as jar files, then added them to this project as dependencies.

Once you're able to build the project, you'll need to configure your app.properties file.  Rename app.properties.default to app.properties and edit the values as needed.  There are properties for authenticated to Twitch, permissions for commands, and many other.  Most of the properties are self explanatory, I think.  However I did add comments to the ones that clearly needed more explanation.

Now that you have your project built, and your properties file configured, it's now time to run the app!  I'm assuming you are familiar with how to run a java jar file, so I won't waste detail on that.  However I will add that the app is configured to log notice, info, error, and debug level messages (and you can set the level in the app.properties file to your desired level).  These messages are written in a log file friendly format.  So when you run your jar file, you could either allow messages to go to the screen if desired, or you can pipe that output to a file if you prefer to have those messages stored in a log file.

Now that you have the app up and running, go to your Channel and type !commands.  That's all you need to get yourself started using your new Twitch bot.  From there, just run with your imagination.  If you build something cool, please let me know - I'd love to hear about it!

Please note that the client ID for Twitch isn't necessary as of today 10/7/2017.  However, it will be mandatory at a future date so I would recommend registering your app to the Twitch Developer site (you can do this without making it public, and you can put in 'dummy data' for the callback url during registration'.  Furthermore, Twitch is developing a new API (which isn't complete yet).  Once that API is complete, you'll probably need to get the latest version of cavariux's Twitch IRC and rebuild the library and add the newer version to your project.  This is because many of the underlying url's will be changing.

Since Twitch sometimes changes the steps required for getting a client id, and for getting the oauth token for a Twitch account, I won't include the steps to do that, as it will quickly become outdated.  Instead, just do a google search for "obtaining an oauth token" on Twitch to complete the 'oauth' property in app.properties, and do a google search for "generating a Client ID on Twitch" to get your client id.

That's it!  I hope this jump starts development for your next Twitch bot!

Commands Available:
Add/Remove/Enable/Disable commands
Add/Remove/Enable/Disable modes
Ban/Unban users
Check, or manually set mode uptime and total uptime
Search Games/Categories/Runners/Records

Example speedrun command:
!speedrun record smb1 any%
This will return the current WR holder for Super Mario Bros. in the any% category.

Fun tip: Create a command that does a search for the WR for a game and category.
!add record !speedrun record smb1 any%
Now, whenever someone types !record, the Bot will reply with the current WR according to speedrun.com!  And you can do this for each mode.  So if you play different categories, or even different games, you can create a mode for each and create a !record command that does the appropriate speedrun search!
All commands will display usage instructions if you type just !command_name without any parameters passed in.
When you create a command, it gets added to the 'mode' that you are currently in.  Therefore, switching modes means you can maintain different sets of commands based on the mode you're in.