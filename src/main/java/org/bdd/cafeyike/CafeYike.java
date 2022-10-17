package org.bdd.cafeyike;

import java.sql.SQLException;
import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.exceptions.BotError;
import org.bdd.cafeyike.commands.Quote;
import org.bdd.cafeyike.commands.Yike;
import org.bdd.cafeyike.commands.music.Music;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.requests.GatewayIntent;

public class CafeYike
{
    private static final Logger log = LoggerFactory.getLogger(CafeYike.class);

    private static final GatewayIntent[] intents = { GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.GUILD_EMOJIS_AND_STICKERS, GatewayIntent.GUILD_BANS, GatewayIntent.GUILD_MESSAGE_REACTIONS,
            GatewayIntent.GUILD_VOICE_STATES
    };

    public static final String YIKE_LOG = "./dat/yikelog.json";

    public static void main(String[] args)
    {
        Bot bot = Bot.inst;

        log.info("Initializing Database");

        CafeDB db = CafeDB.inst;
        try
        {
            db.init();
        }
        catch(SQLException e)
        {
            log.error("Unable to start database:", e);
            System.exit(-1);
        }
        catch(Exception e)
        {
            log.error("Unable to start database:", e);
            System.exit(-1);
        }

        log.info("Loading Commands");
        bot.addCog(new Yike());
        bot.addCog(new Quote());
        bot.addCog(new Music());

        log.info("Initializing Bot");
        try
        {
            bot.init(intents);
        }
        catch(BotError botError)
        {
            botError.printStackTrace();
        }

    }
}
