package org.bdd.cafeyike.commands;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.bdd.cafeyike.CafeDB;
import org.bdd.cafeyike.commander.Arguments;
import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.commands.Cog;
import org.bdd.cafeyike.commander.exceptions.ArgumentError;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.bdd.cafeyike.commander.exceptions.UsageError;
import org.bdd.cafeyike.commander.utils.MsgDeleteAfter;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.ButtonInteraction;

public class Yike extends Cog
{
    private final int voteTimeSec;

    public Yike()
    {
        super("Yike", "Yike Management");

        voteTimeSec = Bot.getIntConfig("voteTimeSec");

        addCommand(new CmdFunc(new String[] {"yike", "y"}, this::yike, "Yike a user", "yike [user]"));
        addCommand(new CmdFunc(new String[] {"unyike", "uy"}, this::unyike, "Unyike a user", "unyike [user]"));
        /*
        addCommand(new CmdFunc(
            new String[] {"list", "l"}, this::list, "Show a list of all the yikes", "list <user>"));
        */
    }

    public void yike(MessageCreateEvent event, Arguments args)
    {
        User recip;
        try
        {
            recip = args.nextUser();
        }
        catch(ArgumentError e)
        {
            throw new UsageError("yike(): Cannot parse user");
        }

        Server serv = event.getServer().orElse(null);

        if(serv == null)
        {
            throw new CmdError("Cannot yike outside of a server");
        }

        String nick;

        int newval = CafeDB.addYike(serv.getId(), recip.getId());

        nick = recip.getDisplayName(serv);

        event.getChannel().sendMessage(nick + " now has " + newval + " yikes");
    }

    @Override
    public void shutdown()
    {
        // PAss
    }

    public void unyike(MessageCreateEvent event, Arguments args)
    {
        User recip;
        try
        {
            recip = args.nextUser();
        }
        catch(ArgumentError e)
        {
            throw new UsageError("unyike(): Cannot parse user");
        }

        Server serv = event.getServer().orElse(null);

        if(serv == null)
        {
            throw new CmdError("Cannot unyike outside a server");
        }

        Integer curVal = CafeDB.getYikesForUser(serv.getId(), recip.getId());

        if(curVal == null || curVal <= 0)
        {
            //TODO response msg
            event.getChannel().sendMessage("No negative yikes allowed");
            return;
        }

        if(args.hasNext())
        {
            String opt = args.next();
            if(opt.equals("-a") && event.getMessageAuthor().isServerAdmin())
            {
                int newVal = CafeDB.remYike(serv.getId(), recip.getId());
                //TODO message
                event.getChannel().sendMessage("Yike removed, count: " + newVal);
                return;
            }
        }

        Message m = new MessageBuilder()
                        .setContent("The Legion shall decide your fate\nCleanse: 0\nSustain: 0")
                        .addComponents(ActionRow.of(Button.success("unyike", "Cleanse"),
                                                    Button.danger("yike", "Sustain")))
                        .send(event.getChannel())
                        .join();

        //True = unyike, False = yike
        HashMap<Long, Boolean> votemap = new HashMap<>();
        AtomicInteger uyCnt = new AtomicInteger();
        AtomicInteger yCnt = new AtomicInteger();

        m.addButtonClickListener(click -> {
             ButtonInteraction inter = click.getButtonInteraction();
             inter.createImmediateResponder().respond();

             String action = inter.getCustomId();

             User u = inter.getUser();

             boolean vote = action.equals("unyike");

             Boolean old = votemap.put(u.getId(), vote);

             if(old == null || old != vote)
             {
                 if(vote)
                 {
                     uyCnt.incrementAndGet();
                     if(old != null)
                     {
                         yCnt.decrementAndGet();
                     }
                 }
                 else
                 {
                     yCnt.incrementAndGet();
                     if(old != null)
                     {
                         uyCnt.decrementAndGet();
                     }
                 }
             }

             m.edit("The Legion shall decide your fate\n"
                    + "Cleanse: " + uyCnt + "\nSustain: " + yCnt);
         }).removeAfter(voteTimeSec, TimeUnit.SECONDS);

        new MsgDeleteAfter(m, voteTimeSec, () -> {
            int y = yCnt.get();
            int u = uyCnt.get();

            String nick = recip.getDisplayName(serv);

            if(u - 1 > y)
            {
                int newval = CafeDB.remYike(serv.getId(), recip.getId());
                event.getChannel().sendMessage(nick + ", you have been forgiven\nYou now have " + newval + " "
                                               + (newval == 1 ? "yike" : "yikes"));
            }
            else
            {
                event.getChannel().sendMessage("The yike shall stand");
            }
        });
    }

    /*
    public void list(MessageCreateEvent event, Arguments args)
    {
        if(args.hasNext())
        {
            User u;
            try
            {
                u = args.nextUser();
            }
            catch(ArgumentError e)
            {
                throw new UsageError("list(): Cannot parse user");
            }

            Integer cnt = yikemap.getOrDefault(u.getId(), 0);

            String nick = u.getDisplayName(event.getServer().orElse(null));

            //TODO message format
            String x = cnt != 1 ? " yikes." : " yike.";
            event.getChannel().sendMessage(nick + " has " + cnt + x);
            //TOCHANGE add a delete btn?
            return;
        }

        Server s = event.getServer().orElse(null);

        if(s != null)
        {
            Collection<User> m = s.getMembers();

            ArrayList<YikeEntry> list = new ArrayList<>(m.size());

            for(User u : m)
            {
                long id = u.getId();
                Integer cnt = yikemap.getOrDefault(id, 0);
                String d = u.getDisplayName(s);

                if(cnt > 0)
                {
                    list.add(new YikeEntry(id, cnt, d));
                }
            }

            Collections.sort(list);

            StringBuilder out = new StringBuilder();

            for(YikeEntry e : list)
            {
                out.append(e.display).append(": ").append(e.cnt).append('\n');
            }

            EmbedBuilder e = new EmbedBuilder().setTitle("Chronicle of Yikes:").setDescription(out.toString());

            event.getChannel().sendMessage(e);
        }
    }
    */
}
