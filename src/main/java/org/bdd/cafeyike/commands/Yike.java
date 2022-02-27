package org.bdd.cafeyike.commands;

import org.bdd.javacordCmd.Arguments;
import org.bdd.javacordCmd.Bot;
import org.bdd.javacordCmd.commands.Cog;
import org.bdd.javacordCmd.utils.MsgDeleteAfter;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.ButtonInteraction;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Yike extends Cog
{
    private final ConcurrentHashMap<Long, Integer> yikemap;

    private final int voteTimeSec;

    public Yike()
    {
        super("YikeCog");
        yikemap = new ConcurrentHashMap<>();
        //TODO load yike map

        voteTimeSec = Integer.parseInt(Bot.inst.getConfig("voteTimeSec"));

        addCommand(new Func(new String[] {"yike", "y"}, this::yike));
        addCommand(new Func(new String[] {"unyike", "uy"}, this::unyike));
    }

    public void yike(MessageCreateEvent event, Arguments args)
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

        //TODO admin unyike

        if(curVal == null || curVal <= 0)
        {
            //TODO response msg
            event.getChannel().sendMessage("No negative yikes allowed");
            return;
        }

        Message m = new MessageBuilder().setContent(
                "The Legion shall decide your fate\nCleanse: 0\nSustain: 0")
                .addComponents(ActionRow.of(
                        Button.success("unyike", "Cleanse"),
                        Button.danger("yike", "Sustain")
                ))
                .send(event.getChannel())
                .join();

        //True = unyike, False = yike
        HashMap<Long, Boolean> votemap = new HashMap<>();
        AtomicInteger uyCnt = new AtomicInteger();
        AtomicInteger yCnt = new AtomicInteger();

        m.addButtonClickListener(click ->
        {
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

            m.edit("The Legion shall decide your fate\n" +
                    "Cleanse: " + uyCnt + "\nSustain: " + yCnt);
        }).removeAfter(voteTimeSec, TimeUnit.SECONDS);

        new MsgDeleteAfter(m, voteTimeSec, () ->
        {
            int y = yCnt.get();
            int u = uyCnt.get();

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

            if(u - 1 > y)
            {
                int newval = curVal - 1;
                yikemap.put(recip.getId(), newval);
                event.getChannel().sendMessage(nick +
                        ", you have been forgiven\nYou now have " + newval + " " +
                        (newval == 1 ? "yike": "yikes"));
            }
            else
            {
                event.getChannel().sendMessage("The yike shall stand");
            }

        });


    }

    @Override
    public void shutdown()
    {
        //TODO save yikelog?
    }
}
