package org.bdd.cafeyike.commander;

import java.util.Arrays;

import org.bdd.cafeyike.CafeConfig;
import org.bdd.cafeyike.commander.exceptions.BotError;
import org.bdd.cafeyike.commander.exceptions.CmdError;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bot
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private boolean initted = false;
    private JDA api;
    private final String token;

    public final CommandListener listener;

    public Bot()
    {
        token = CafeConfig.getConfig("token");
        if(token == null || token.isEmpty())
        {
            throw new CmdError("Bot() login token not defined, system.config: DISCORD_TOKEN = [token]");
        }

        listener = new CommandListener();
    }

    public void addCog(Cog cog)
    {
        listener.addCog(cog);
    }

    public void init(GatewayIntent... intents) throws BotError
    {
        if(initted)
        {
            throw new BotError("Bot has already been initialized");
        }

        initted = true;

        JDABuilder temp_builder = JDABuilder.createDefault(token, Arrays.asList(intents));
        temp_builder.addEventListeners(listener);
        temp_builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        temp_builder.setChunkingFilter(ChunkingFilter.ALL);
        temp_builder.disableCache(CacheFlag.SCHEDULED_EVENTS);
        api = temp_builder.build();

        listener.registerCommands(api);

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
        listener.shutdown();
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
