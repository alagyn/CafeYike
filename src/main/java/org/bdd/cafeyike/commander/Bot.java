package org.bdd.cafeyike.commander;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.bdd.cafeyike.commander.exceptions.BotError;
import org.bdd.cafeyike.commander.exceptions.UsageError;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.interaction.Interaction;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.InteractionCreateListener;
import org.javacord.api.util.event.ListenerManager;

public class Bot
{
    public static final Bot inst = new Bot();

    private final Properties config;

    private boolean initted = false;
    private DiscordApi api;
    private final DiscordApiBuilder temp_builder;

    private CommandListener cl = null;
    ListenerManager<InteractionCreateListener> listenManager;

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

        cl.registerCommands(api);

        listenManager = api.addInteractionCreateListener(cl);

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
        api.updateStatus(UserStatus.OFFLINE);
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

    public void logErr(String x)
    {
        System.out.print((char)27);
        System.out.print("[31m");
        System.out.print(x);
        System.out.print((char)27);
        System.out.println("[0m");
    }

    public static String getNickname(User user, Server serv)
    {
        if(serv == null)
        {
            return user.getName();
        }

        return user.getDisplayName(serv);
    }

    public static void sendError(Interaction event, String message)
    {
        event.createImmediateResponder()
            .addEmbed(new EmbedBuilder().addField("Error", message))
            .setFlags(MessageFlag.EPHEMERAL)
            .respond();
        throw new UsageError(message);
    }

    public static void sendError(SlashCommandInteraction event, String message)
    {
        sendError((Interaction)event, message);
    }

    public static void sendFollowError(Interaction event, String message)
    {
        event.createFollowupMessageBuilder()
            .addEmbed(new EmbedBuilder().addField("Error", message))
            .setFlags(MessageFlag.EPHEMERAL)
            .send();
        throw new UsageError(message);
    }

    public static void sendFollowError(SlashCommandInteraction event, String message)
    {
        sendFollowError((Interaction)event, message);
    }
}
