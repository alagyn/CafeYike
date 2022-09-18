package org.bdd.cafeyike;

import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.exceptions.BotError;
import org.bdd.cafeyike.commands.Admin;
import org.bdd.cafeyike.commands.ButtonTest;
import org.bdd.cafeyike.commands.Yike;
import org.javacord.api.entity.intent.Intent;

public class CafeYike
{
    private static final class ShutdownThread extends Thread
    {
        @Override
        public void run()
        {
            Bot.inst.logInfo("Shutting down");
            Bot.inst.shutdown();
            System.exit(0);
        }
    }

    private static final Intent[] intents = {Intent.GUILD_MEMBERS,
                                             Intent.GUILDS,
                                             Intent.GUILD_MESSAGES,
                                             Intent.GUILD_EMOJIS,
                                             Intent.GUILD_BANS,
                                             Intent.GUILD_MESSAGE_REACTIONS,
                                             Intent.GUILD_VOICE_STATES};

    public static final String YIKE_LOG = "./dat/yikelog.json";

    public static void main(String[] args)
    {
        Bot bot = Bot.inst;

        System.out.println("Loading Commands");
        bot.addCog(new Yike());
        bot.addCog(new Admin());
        bot.addCog(new ButtonTest());

        System.out.println("Initializing Interrupts");
        //Runtime.getRuntime().addShutdownHook(new ShutdownThread());

        System.out.println("Initializing Bot");
        try
        {
            bot.init("_", intents);
        }
        catch(BotError botError)
        {
            botError.printStackTrace();
        }

        System.out.println("Done");
    }
}
