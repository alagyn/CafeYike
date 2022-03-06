package org.bdd.cafeyike.commands;

import org.bdd.cafeyike.CafeYike;
import org.bdd.javacordCmd.Arguments;
import org.bdd.javacordCmd.Bot;
import org.bdd.javacordCmd.commands.Cog;
import org.bdd.javacordCmd.exceptions.ArgumentError;
import org.bdd.javacordCmd.exceptions.BotError;
import org.bdd.javacordCmd.exceptions.UsageError;
import org.bdd.javacordCmd.utils.MsgDeleteAfter;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.ButtonInteraction;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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

        String input;
        try
        {
            input = Files.readString(Path.of(CafeYike.YIKE_LOG));
        }
        catch(IOException e)
        {
            throw new BotError("Cannot open Yike Log");
        }

        JSONObject json = null;
        try
        {
            json = new JSONObject(input);
        }
        catch(JSONException e)
        {
            //TODO error log
            System.out.println("Cannot Read Yike Log");
        }

        if(json != null)
        {
            for(Iterator<String> it = json.keys(); it.hasNext(); )
            {
                String jkey = it.next();
                long key = Long.parseLong(jkey);

                yikemap.put(key, json.getInt(jkey));
            }
        }

        voteTimeSec = Bot.getIntConfig("voteTimeSec");

        addCommand(new Func(new String[] {"yike", "y"}, this::yike));
        addCommand(new Func(new String[] {"unyike", "uy"}, this::unyike));
        addCommand(new Func(new String[] {"list", "l"}, this::list));
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
            throw new UsageError();
        }

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

    private void writeYikeLog()
    {
        JSONObject out = new JSONObject();

        for(Map.Entry<Long, Integer> e : yikemap.entrySet())
        {
            out.put(e.getKey().toString(), e.getValue());
        }

        try
        {
            System.out.println("Writing Yike Log");
            out.write(new FileWriter(CafeYike.YIKE_LOG)).close();
        }
        catch(IOException e)
        {
            //TODO err
            System.out.println("Cannot Write Yike Log");
        }

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
            throw new UsageError();
        }

        Integer curVal = yikemap.get(recip.getId());

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
                int newval = curVal - 1;
                yikemap.put(recip.getId(), newval);
                //TODO message
                event.getChannel().sendMessage("Yike removed");
                return;
            }
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

    private record YikeEntry(long id, int cnt, String display) implements Comparable<YikeEntry>
    {

        @Override
        public int compareTo(YikeEntry o)
        {
            int out = Integer.compare(cnt, o.cnt) * -1;
            if(out == 0)
            {
                out = display.compareTo(o.display);
            }

            return out;
        }
    }

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
                throw new UsageError();
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

            for(YikeEntry e: list)
            {
                out.append(e.display).append(": ").append(e.cnt).append('\n');
            }

            EmbedBuilder e = new EmbedBuilder().setTitle("Chronicle of Yikes:")
                    .setDescription(out.toString());

            event.getChannel().sendMessage(e);
        }
    }

    @Override
    public void shutdown()
    {
        writeYikeLog();
    }
}
