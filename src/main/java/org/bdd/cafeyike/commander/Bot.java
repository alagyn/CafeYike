package org.bdd.cafeyike.commander;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.bdd.cafeyike.commander.exceptions.BotError;
import org.bdd.cafeyike.commander.exceptions.CmdError;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bot
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Properties config;

    private boolean initted = false;
    private JDA api;
    private final String token;

    private CommandListener cl = null;

    public Bot()
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
        token = config.getProperty("DISCORD_TOKEN");
        if(token == null || token.isEmpty())
        {
            throw new CmdError("Bot() login token not defined, system.config: DISCORD_TOKEN = [token]");
        }

        cl = new CommandListener();
    }

    public String getConfig(String key)
    {
        return config.getProperty(key);
    }

    public int getIntConfig(String key)
    {
        return Integer.parseInt(config.getProperty(key));
    }

    public void addCog(Cog cog)
    {
        cl.addCog(cog);
    }

    public void init(GatewayIntent... intents) throws BotError
    {
        if(initted)
        {
            throw new BotError("Bot has already been initialized");
        }

        initted = true;

        JDABuilder temp_builder = JDABuilder.createDefault(token, Arrays.asList(intents));
        temp_builder.addEventListeners(cl);
        api = temp_builder.build();

        cl.registerCommands(api);

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            public void run()
            {
                intShutdown();
            }
        });

        SelfUser me = api.getSelfUser();
        log.info("Bot {} is now online", me.getAsTag());
    }

    public JDA getApi()
    {
        return api;
    }

    private void intShutdown()
    {
        log.info("Shutting down Command Listener");
        cl.shutdown();
        api.getPresence().setPresence(OnlineStatus.OFFLINE, false);
        log.info("Exitting");
    }

    public static String makeId(String prefix, Object data)
    {
        StringBuilder out = new StringBuilder();
        out.append(prefix).append(":").append(data.toString());
        return out.toString();
    }
}
