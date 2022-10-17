package org.bdd.cafeyike.commands;

import java.util.*;
import org.bdd.cafeyike.CafeDB;
import org.bdd.cafeyike.CafeDB.YikeEntry;
import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.Cog;
import org.bdd.cafeyike.commander.utils.DoAfter;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class Yike extends Cog
{
    private final int voteTimeSec;

    private final String yikeBtn = "yike";
    private final String unyikeBtn = "unyike";

    private static enum Vote
    {
        Yike, Unyike;
    }

    private static class UnyikeVoter
    {
        // True = unyike, False = yike
        public HashMap<Long, Vote> votemap = new HashMap<>();
        public int upvotes = 0, downvotes = 0;

        public void setVote(long userID, Vote vote)
        {
            Vote oldVote = votemap.put(userID, vote);
            if(oldVote == null || oldVote != vote)
            {
                // If unyike
                if(vote == Vote.Unyike)
                {
                    ++upvotes;
                    if(oldVote != null)
                    {
                        --downvotes;
                    }
                }
                // Else yike
                else
                {
                    ++downvotes;
                    if(oldVote != null)
                    {
                        --upvotes;
                    }
                }
            }
        }
    }

    private HashMap<Long, UnyikeVoter> voters = new HashMap<>();

    public Yike()
    {
        voteTimeSec = Bot.getIntConfig("voteTimeSec");
    }

    public void yike(SlashCommandInteractionEvent event)
    {
        Member recip = event.getOption("user").getAsMember();

        if(recip == null)
        {
            sendError(event, "Cannot unyike user");
        }

        Guild serv = event.getGuild();

        if(serv == null)
        {
            sendError(event, "Cannot yike outside of a server");
        }

        String nick;

        event.deferReply().queue();

        int newval = CafeDB.addYike(serv.getIdLong(), recip.getIdLong());

        nick = recip.getEffectiveName();

        event.getHook().sendMessage(recip.getAsMention())
                .addEmbeds(new EmbedBuilder().addField("Yike", nick + " now has " + newval + " yikes", false).build())
                .queue();
    }

    @Override
    public void shutdown()
    {
        // PAss
    }

    private EmbedBuilder unyikeMessage(int up, int down)
    {
        return new EmbedBuilder().addField("Un-Yike",
                "The Legion shall decide your fate\nCleanse: " + up + "\nSustain: " + down, false);
    }

    public void unyike(SlashCommandInteractionEvent event)
    {
        Member recip = event.getOption("user").getAsMember();

        if(recip == null)
        {
            sendError(event, "Cannot unyike user");
        }

        Guild serv = event.getGuild();

        if(serv == null)
        {
            sendError(event, "Cannot unyike outside a server");
        }

        int curVal = CafeDB.getYikesForUser(serv.getIdLong(), recip.getIdLong());

        if(curVal <= 0)
        {
            sendError(event, "No negative yikes allowed");
        }

        Boolean requestAdmin = event.getOption("admin").getAsBoolean();

        if(requestAdmin)
        {
            if(event.getMember().getPermissions().contains(Permission.ADMINISTRATOR))
            {
                int newVal = CafeDB.remYike(serv.getIdLong(), recip.getIdLong());
                event.replyEmbeds(new EmbedBuilder()
                        .addField("Admin Un-Yike", recip.getEffectiveName() + " now has " + newVal + " yikes.", false)
                        .build()).queue();
                return;
            }

            sendError(event, "You must be an admin to use the admin flag");
        }

        if(voters.containsKey(serv.getIdLong()))
        {
            sendError(event, "Cannot unyike, still waiting for previous voting to close");
        }

        UnyikeVoter voter = new UnyikeVoter();
        voters.put(serv.getIdLong(), voter);

        InteractionHook hook = event.reply(recip.getAsMention()).addEmbeds(unyikeMessage(0, 0).build())
                .addActionRow(Button.success(unyikeBtn, "Cleanse"), Button.danger(yikeBtn, "Sustain")).complete();

        new DoAfter(voteTimeSec, x ->
        {
            synchronized(voters)
            {
                voters.remove(serv.getIdLong());
                hook.editOriginalComponents(new ArrayList<>()).queue();
            }

            synchronized(voter)
            {
                // Don't Remove all embeds

                if(voter.upvotes - 1 > voter.downvotes)
                {
                    int newval = CafeDB.remYike(serv.getIdLong(), recip.getIdLong());
                    hook.editOriginalEmbeds(unyikeMessage(voter.upvotes, voter.downvotes).build(),
                            new EmbedBuilder()
                                    .addField("Un-Yike",
                                            recip.getEffectiveName() + ", you have been forgiven\nYou now have "
                                                    + newval + " " + (newval == 1 ? "yike" : "yikes"),
                                            false)
                                    .build())
                            .queue();
                    ;
                }
                else
                {
                    hook.editOriginalEmbeds(unyikeMessage(voter.upvotes, voter.downvotes).build(),
                            new EmbedBuilder().addField("Un-Yike", "The yike shall stand", false).build()).queue();
                    ;
                }
            }
        });
    }

    public void yikeVoteBtn(ButtonInteractionEvent event, String data)
    {
        long guildID = Long.parseLong(data);
        UnyikeVoter voter = null;
        synchronized(voters)
        {
            voter = voters.get(guildID);
        }

        if(voter == null)
        {
            return;
        }
        synchronized(voter)
        {
            long userID = event.getUser().getIdLong();
            voter.setVote(userID, Vote.Yike);
            event.editMessageEmbeds(unyikeMessage(voter.upvotes, voter.downvotes).build()).queue();
        }
    }

    public void unyikeVoteBtn(ButtonInteractionEvent event, String data)
    {
        long guildID = Long.parseLong(data);
        UnyikeVoter voter = voters.get(guildID);
        if(voter == null)
        {
            return;
        }
        synchronized(voter)
        {
            long userID = event.getUser().getIdLong();
            voter.setVote(userID, Vote.Unyike);
            event.editMessageEmbeds(unyikeMessage(voter.upvotes, voter.downvotes).build()).queue();
        }
    }

    @Override
    public List<CommandData> buildCommands()
    {
        LinkedList<CommandData> out = new LinkedList<>();

        out.add(Commands.slash("yike", "Give a user a yike").addOption(OptionType.USER, "user", "The yike recipient",
                true));

        registerCmdFunc(this::yike, "yike");

        out.add(Commands.slash("unyike", "Remove a yike from a user")
                .addOption(OptionType.USER, "user", "The user from which to remove a yike", true)
                .addOption(OptionType.BOOLEAN, "admin", "Admin flag to skip voting", false));

        registerCmdFunc(this::unyike, "unyike");

        out.add(Commands.slash("list", "List the yikes for users in this server").addOption(OptionType.USER, "user",
                "List for a specific user"));

        registerCmdFunc(this::list, "list");
        registerNoopBtn(yikeBtn);
        registerNoopBtn(unyikeBtn);

        return out;
    }

    public void list(SlashCommandInteractionEvent event)
    {
        Member user = event.getOption("user").getAsMember();

        Guild serv = event.getGuild();

        if(serv == null)
        {
            sendError(event, "Cannot get yikes outside a server");
        }

        event.deferReply();
        InteractionHook hook = event.getHook();

        if(user != null)
        {
            int cnt = CafeDB.getYikesForUser(serv.getIdLong(), user.getIdLong());

            String nick = user.getEffectiveName();

            String x = cnt != 1 ? " yikes." : " yike.";
            hook.sendMessageEmbeds(
                    new EmbedBuilder().addField("Chronicle of Yikes:", nick + " has " + cnt + x, false).build())
                    .queue();

            return;
        }

        List<CafeDB.YikeEntry> list = CafeDB.getYikesForServer(serv.getIdLong());

        Collections.sort(list);

        StringBuilder out = new StringBuilder();

        for(int i = 0; i < 15 && i < list.size(); ++i)
        {
            YikeEntry e = list.get(i);
            if(e.count <= 0)
            {
                break;
            }

            Member u = serv.getMemberById(e.ID);
            if(u == null)
            {
                continue;
            }
            out.append(u.getEffectiveName()).append(": ").append(e.count).append("\n");
        }

        hook.sendMessageEmbeds(
                new EmbedBuilder().setTitle("Chronicle of Yikes:").setDescription(out.toString()).build()).queue();
    }
}
