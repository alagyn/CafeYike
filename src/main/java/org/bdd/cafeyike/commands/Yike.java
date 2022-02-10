package org.bdd.cafeyike.commands;

import org.bdd.javacordCmd.Arguments;
import org.bdd.javacordCmd.commands.Cog;
import org.bdd.javacordCmd.exceptions.ArgumentError;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.concurrent.ConcurrentHashMap;

public class Yike extends Cog
{
    private final ConcurrentHashMap<Long, Integer> yikemap;

    public Yike()
    {
        super("YikeCog");
        yikemap = new ConcurrentHashMap<>();
        //TODO load yike map

        addCommand(new CogFunc(new String[]{"yike", "y"}, this::yike));
        addCommand(new CogFunc(new String[]{"unyike", "uy"}, this::unyike));
    }

    public void yike(MessageCreateEvent event, Arguments args) throws ArgumentError
    {
        User recip = args.nextUser();
        //Either sets a new entry to one or adds 1 to entry
        int newval = yikemap.merge(recip.getId(), 1, Integer::sum);

        Server serv = event.getServer().orElse(null);

        String nick;

        if(serv != null)
        {
            nick = recip.getDisplayName(serv);
        }
        else
        {
            nick = recip.getName();
        }

        event.getChannel().sendMessage(nick + " now has " + newval + " yikes");
    }

    public void unyike(MessageCreateEvent event, Arguments args)
    {
        User recip = args.nextUser();

        Integer curVal = yikemap.get(recip.getId());

        if(curVal == null || curVal <= 0)
        {
            //TODO response msg
            event.getChannel().sendMessage("No negative yikes allowed");
            return;
        }

        //TODO voting...

        int newval = curVal - 1;
        yikemap.put(recip.getId(), newval);

        Server serv = event.getServer().orElse(null);

        String nick;

        if(serv != null)
        {
            nick = recip.getDisplayName(serv);
        }
        else
        {
            nick = recip.getName();
        }

        //TODO msg
        event.getChannel().sendMessage(nick + " now has " + newval + " yikes");
    }

}
