package org.bdd.cafeyike;

import org.bdd.cafeyike.commands.Admin;
import org.bdd.cafeyike.commands.ButtonTest;
import org.bdd.javacordCmd.commands.Command;
import org.bdd.cafeyike.commands.Yike;
import org.bdd.javacordCmd.exceptions.BotError;
import org.bdd.javacordCmd.exceptions.CmdError;
import org.bdd.javacordCmd.Bot;
import org.javacord.api.entity.intent.Intent;

public class CafeYike
{
    private static final Intent[] intents = {Intent.GUILD_MEMBERS, Intent.GUILDS, Intent.GUILD_MESSAGES, Intent.GUILD_EMOJIS, Intent.GUILD_BANS, Intent.GUILD_MESSAGE_REACTIONS, Intent.GUILD_VOICE_STATES};

    public static final String YIKE_LOG = "./dat/yikelog.json";

    public static void main(String[] args)
    {
        Bot bot = Bot.inst;

        Command[] cmds = null;

        try
        {
            cmds = new Command[] {
                    new Yike(),
                    new Admin(),
                    new ButtonTest()
            };
        }
        catch(CmdError cmdError)
        {
            cmdError.printStackTrace();
        }

        try
        {
            bot.init("_", intents, cmds);
        }
        catch(BotError botError)
        {
            botError.printStackTrace();
        }

    }


}
