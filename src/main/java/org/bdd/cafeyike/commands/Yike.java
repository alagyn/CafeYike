package org.bdd.cafeyike.commands;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.bdd.cafeyike.CafeDB;
import org.bdd.cafeyike.CafeDB.YikeEntry;
import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.Cog;
import org.bdd.cafeyike.commander.utils.DoAfter;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

public class Yike extends Cog
{
    private final int voteTimeSec;

    private final String yikeBtn = "yike";
    private final String unyikeBtn = "unyike";

    public Yike()
    {
        voteTimeSec = Bot.getIntConfig("voteTimeSec");
    }

    public void yike(SlashCommandInteraction interaction)
    {
        User recip = interaction.getOptionUserValueByIndex(0).get();

        if(recip == null)
        {
            Bot.sendError(interaction, "Cannot unyike user");
        }

        Server serv = interaction.getServer().get();

        if(serv == null)
        {
            Bot.sendError(interaction, "Cannot yike outside of a server");
        }

        String nick;

        interaction.respondLater();

        int newval = CafeDB.addYike(serv.getId(), recip.getId());

        nick = recip.getDisplayName(serv);

        interaction.createFollowupMessageBuilder().setContent(recip.getMentionTag())
                .addEmbed(new EmbedBuilder().addField("Yike", nick + " now has " + newval + " yikes")).send();
    }

    @Override
    public void shutdown()
    {
        // PAss
    }

    private EmbedBuilder unyikeMessage(int up, int down)
    {
        return new EmbedBuilder().addField("Un-Yike",
                "The Legion shall decide your fate\nCleanse: " + up + "\nSustain: " + down);
    }

    public void unyike(SlashCommandInteraction event)
    {
        User recip = event.getOptionUserValueByIndex(0).get();
        if(recip == null)
        {
            Bot.sendError(event, "Cannot unyike user");
        }

        Server serv = event.getServer().get();

        if(serv == null)
        {
            Bot.sendError(event, "Cannot unyike outside a server");
        }

        int curVal = CafeDB.getYikesForUser(serv.getId(), recip.getId());

        if(curVal <= 0)
        {
            Bot.sendError(event, "No negative yikes allowed");
        }

        Boolean requestAdmin = event.getOptionBooleanValueByIndex(1).orElse(false);

        if(requestAdmin)
        {
            Role r = event.getUser().getRoles(serv).get(0);
            if(r.getPermissions().getAllowedPermission().contains(PermissionType.ADMINISTRATOR))
            {
                int newVal = CafeDB.remYike(serv.getId(), recip.getId());
                event.createFollowupMessageBuilder().addEmbed(new EmbedBuilder().addField("Admin Un-Yike",
                        Bot.getNickname(recip, serv) + " now has " + newVal + " yikes.")).send();
                return;
            }

            Bot.sendError(event, "You must be an admin to use the admin flag");
        }

        InteractionOriginalResponseUpdater updater = event.createImmediateResponder().setContent(recip.getMentionTag())
                .addEmbed(unyikeMessage(0, 0))
                .addComponents(ActionRow.of(Button.success(unyikeBtn, "Cleanse"), Button.danger(yikeBtn, "Sustain")))
                .respond().join();

        Message m = updater.update().join();

        // True = unyike, False = yike
        HashMap<Long, Boolean> votemap = new HashMap<>();
        AtomicInteger upvotes = new AtomicInteger(0);
        AtomicInteger downvotes = new AtomicInteger(0);

        m.addButtonClickListener(click ->
        {
            ButtonInteraction inter = click.getButtonInteraction();
            String action = inter.getCustomId();
            inter.acknowledge();

            User u = inter.getUser();

            boolean vote = action.equals(unyikeBtn);

            Boolean old = votemap.put(u.getId(), vote);

            if(old == null || old != vote)
            {
                synchronized(updater)
                {
                    int up, down;
                    // If unyike
                    if(vote)
                    {
                        up = upvotes.incrementAndGet();
                        if(old != null)
                        {
                            down = downvotes.decrementAndGet();
                        }
                        else
                        {
                            down = downvotes.get();
                        }
                    }
                    // Else yike
                    else
                    {
                        down = downvotes.incrementAndGet();
                        if(old != null)
                        {
                            up = upvotes.decrementAndGet();
                        }
                        else
                        {
                            up = upvotes.get();
                        }
                    }
                    updater.removeAllEmbeds().addEmbed(unyikeMessage(up, down)).update();
                }
            }
        }).removeAfter(voteTimeSec, TimeUnit.SECONDS);

        new DoAfter(voteTimeSec, x ->
        {
            synchronized(updater)
            {
                String nick = recip.getDisplayName(serv);

                // Don't Remove all embeds

                if(upvotes.get() - 1 > downvotes.get())
                {
                    int newval = CafeDB.remYike(serv.getId(), recip.getId());
                    updater.addEmbed(
                            new EmbedBuilder().addField("Un-Yike", nick + ", you have been forgiven\nYou now have "
                                    + newval + " " + (newval == 1 ? "yike" : "yikes")));
                }
                else
                {
                    updater.addEmbed(new EmbedBuilder().addField("Un-Yike", "The yike shall stand"));
                }

                updater.removeAllComponents().update();
            }
        });
    }

    @Override
    public List<SlashCommandBuilder> buildCommands()
    {
        LinkedList<SlashCommandBuilder> out = new LinkedList<>();

        out.add(SlashCommand.with("yike", "Give a user a yike")
                .addOption(SlashCommandOption.create(SlashCommandOptionType.USER, "user", "The yike recipient", true)));

        registerCmdFunc(this::yike, "yike");

        out.add(SlashCommand.with("unyike", "Remove a yike from a user")
                .addOption(SlashCommandOption.create(SlashCommandOptionType.USER, "user",
                        "The user from which to remove a yike", true))
                .addOption(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "admin",
                        "Admin flag to skip voting", false)));

        registerCmdFunc(this::unyike, "unyike");

        out.add(SlashCommand.with("list", "List the yikes for users in this server")
                .addOption(SlashCommandOption.create(SlashCommandOptionType.USER, "user", "List for a specific user")));

        registerCmdFunc(this::list, "list");
        registerNoopBtn(yikeBtn);
        registerNoopBtn(unyikeBtn);

        return out;
    }

    public void list(SlashCommandInteraction event)
    {
        User user = event.getOptionUserValueByIndex(0).orElse(null);

        Server serv = event.getServer().orElse(null);

        if(serv == null)
        {
            Bot.sendError(event, "Cannot get yikes outside a server");
        }

        if(user != null)
        {
            int cnt = CafeDB.getYikesForUser(serv.getId(), user.getId());

            String nick = user.getDisplayName(serv);

            // TODO message format
            String x = cnt != 1 ? " yikes." : " yike.";
            event.createImmediateResponder()
                    .addEmbed(new EmbedBuilder().addField("Chronicle of Yikes:", nick + " has " + cnt + x)).respond();

            return;
        }

        event.respondLater();

        List<CafeDB.YikeEntry> list = CafeDB.getYikesForServer(serv.getId());

        Collections.sort(list);

        StringBuilder out = new StringBuilder();

        for(int i = 0; i < 15 && i < list.size(); ++i)
        {
            YikeEntry e = list.get(i);
            if(e.count <= 0)
            {
                break;
            }

            User u = serv.getMemberById(e.ID).orElse(null);
            if(u == null)
            {
                continue;
            }
            out.append(u.getDisplayName(serv)).append(": ").append(e.count).append("\n");
        }

        event.createFollowupMessageBuilder()
                .addEmbed(new EmbedBuilder().setTitle("Chronicle of Yikes:").setDescription(out.toString())).send();
    }
}
