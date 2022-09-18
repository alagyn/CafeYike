package org.bdd.cafeyike.commander;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;
import org.bdd.cafeyike.commander.commands.Cog;
import org.bdd.cafeyike.commander.commands.Command;
import org.bdd.cafeyike.commander.exceptions.BotError;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.event.ListenerManager;

public class Bot
{
    public static final Bot inst = new Bot();

    private final Properties config;

    private boolean initted = false;
    private DiscordApi api;
    private final DiscordApiBuilder temp_builder;

    private CommandListener cl = null;
    ListenerManager<MessageCreateListener> listenManager;

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

        cl = new CommandListener();
    }

    public static String getConfig(String key)
    {
        return inst.config.getProperty(key);
    }

    public static int getIntConfig(String key)
    {
        return Integer.parseInt(inst.config.getProperty(key));
    }

    public void addCommand(Command command)
    {
        cl.addCommand(command);
    }

    public void addCog(Cog cog)
    {
        cl.addCog(cog);
    }

    public void init(String prefix, Intent[] intents) throws BotError
    {
        if(initted)
        {
            throw new BotError("Bot has already been initialized");
        }

        initted = true;

        temp_builder.setIntents(intents);

        api = temp_builder.login().join();

        cl.setPrefix(prefix);

        listenManager = api.addMessageCreateListener(cl);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run()
            {
                Bot.inst.intShutdown();
            }
        });
    }

    public DiscordApi getApi()
    {
        return api;
    }

    public void shutdown()
    {
        new Thread() {
            public void run()
            {
                Bot.inst._shutdown();
            }
        }.start();
    }

    private void intShutdown()
    {
        logInfo("Shutting down Command Listener");
        cl.shutdown();
        logInfo("Exitting");
    }

    private void _shutdown()
    {
        logInfo("Removing listener");
        listenManager.remove();
        logInfo("Disconnecting API");
        api.disconnect().join();
    }

    public void logInfo(String x)
    {
        // TODO
        System.out.println(x);
    }

    public void logDbg(String x)
    {
        System.out.println(x);
    }
}
