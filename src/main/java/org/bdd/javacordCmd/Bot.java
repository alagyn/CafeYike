package org.bdd.javacordCmd;

import org.bdd.javacordCmd.commands.Command;
import org.bdd.javacordCmd.exceptions.BotError;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Bot
{
    public static final Bot inst = new Bot();

    private final Properties config;

    private boolean initted = false;
    private DiscordApi api;
    private final DiscordApiBuilder temp_builder;

    private CommandListener cl = null;

    private Bot()
    {
        config = new Properties();

        try(FileInputStream is = new FileInputStream("system.config"))
        {
            config.load(is);
        }
        catch(IOException ex)
        {
            System.out.println("Cannot load config file");
            System.exit(0);
        }

        temp_builder = new DiscordApiBuilder().setToken(config.getProperty("DISCORD_TOKEN"));
    }

    public static String getConfig(String key)
    {
        return inst.config.getProperty(key);
    }

    public static int getIntConfig(String key)
    {
        return Integer.parseInt(inst.config.getProperty(key));
    }

    public void init(String prefix, Intent[] intents, Command[] cmds) throws BotError
    {
        if(initted)
        {
            throw new BotError("Bot has already been initialized");
        }

        initted = true;

        temp_builder.setIntents(intents);

        api = temp_builder.login().join();

        cl = new CommandListener(prefix);

        for(Command c : cmds)
        {
            cl.addCommand(c);
        }

        api.addMessageCreateListener(cl);
    }

    public DiscordApi getApi()
    {
        return api;
    }

    public void shutdown()
    {
        cl.shutdown();
        System.exit(0);
    }

}
