package org.bdd.cafeyike;

import java.sql.SQLException;
import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.exceptions.BotError;
import org.bdd.cafeyike.commands.ButtonTest;
import org.bdd.cafeyike.commands.Quote;
import org.bdd.cafeyike.commands.Yike;
import org.bdd.cafeyike.commands.music.Music;
import org.javacord.api.entity.intent.Intent;

public class CafeYike
{
    private static final Intent[] intents = { Intent.GUILD_MEMBERS, Intent.GUILDS, Intent.GUILD_MESSAGES,
            Intent.GUILD_EMOJIS, Intent.GUILD_BANS, Intent.GUILD_MESSAGE_REACTIONS, Intent.GUILD_VOICE_STATES
    };

    public static final String YIKE_LOG = "./dat/yikelog.json";

    public static void main(String[] args)
    {
        Bot bot = Bot.inst;

        bot.logInfo("Initializing Database");

        CafeDB db = CafeDB.inst;
        try
        {
            db.init();
        }
        catch(SQLException e)
        {
            bot.logErr("Unable to start database:");
            bot.logErr(e.getMessage());
            e.printStackTrace();
            bot.logErr(e.getSQLState());
            System.exit(-1);
        }
        catch(Exception e)
        {
            bot.logErr("Unable to start database:");
            bot.logErr(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }

        bot.logInfo("Loading Commands");
        bot.addCog(new Yike());
        bot.addCog(new Quote());
        // bot.addCog(new Admin());
        bot.addCog(new Music());
        bot.addCog(new ButtonTest());

        bot.logInfo("Initializing Bot");
        try
        {
            bot.init("_", intents);
        }
        catch(BotError botError)
        {
            botError.printStackTrace();
        }

        bot.logInfo("Bot Online");
    }
}
