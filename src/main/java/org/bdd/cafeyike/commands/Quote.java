package org.bdd.cafeyike.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.bdd.cafeyike.CafeDB;
import org.bdd.cafeyike.CafeDB.QuoteEntry;
import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.Cog;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.component.TextInput;
import org.javacord.api.entity.message.component.TextInputStyle;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.interaction.ModalInteraction;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

public class Quote extends Cog
{
    public Quote()
    {
        //addCommand(new CmdFunc("quote", this::addQuote, "Add a quote for a user"));
    }

    public void addQuote(SlashCommandInteraction event)
    {
        User user = event.getOptionUserValueByIndex(0).get();

        if(user == null)
        {
            Bot.sendError(event, "Cannot quote user");
        }

        String content = event.getOptionStringValueByIndex(1).get();

        if(content == null || content.isEmpty())
        {
            Bot.sendError(event, "Quote cannot be empty");
        }

        event.respondLater();

        Server serv = event.getServer().get();
        String nick = "";
        if(serv == null)
        {
            nick = user.getName();
        }
        else
        {
            nick = user.getDisplayName(serv);
        }

        CafeDB.addQuote(user.getId(), content);

        Message m = null;
        try
        {
            m = event.createFollowupMessageBuilder()
                    .addEmbed(new EmbedBuilder().addField("Quote: " + nick, content))
                    .addComponents(ActionRow.of(Button.primary("edit", "Edit")))
                    .send()
                    .get();
        }
        catch(Exception e)
        {
            Bot.sendFollowError(event, "Quote added, but cannot get message: " + e.getMessage());
        }

        m.addButtonClickListener(click -> {
             ButtonInteraction btnInter = click.getButtonInteraction();
             if(btnInter.getCustomId().equals("edit"))
             {
                 // Gen a custom ID for this quote
                 String customId = "quoteModal" + System.currentTimeMillis();

                 btnInter.respondWithModal(customId,
                     "Edit Quote",
                     ActionRow.of(TextInput.create(TextInputStyle.SHORT, "newQuote", "Edit")));

                 // Add a new listener
                 Bot.inst.getApi()
                     .addModalSubmitListener(x -> {
                         ModalInteraction modalInteraction = x.getModalInteraction();
                         modalInteraction.createImmediateResponder().addEmbed(
                             new EmbedBuilder().addField("Asdf", "Asdf"));
                         if(modalInteraction.getCustomId().equals(customId))
                         {
                             String newQuote =
                                 modalInteraction.getTextInputValueByCustomId("newQuote").orElse(null);
                             System.out.println(newQuote);
                             modalInteraction.createImmediateResponder()
                                 .addEmbed(new EmbedBuilder().addField("Quote", "Quote updated\n" + newQuote))
                                 .respond();

                             // TODO update old quote
                         }
                     })
                     .removeAfter(10, TimeUnit.MINUTES);
             }
         }).removeAfter(10, TimeUnit.MINUTES);
    }

    public void getQuotes(SlashCommandInteraction event)
    {
        User singleUser = event.getOptionUserValueByIndex(0).orElse(null);

        List<QuoteEntry> quotes = null;
        String filename = "";

        Server serv = event.getServer().orElse(null);

        event.respondLater();

        if(singleUser != null)
        {
            ArrayList<User> users = new ArrayList<>();
            users.add(singleUser);
            // Get for a single user
            quotes = CafeDB.getQuotes(users);
            filename = singleUser.getIdAsString() + "_quotes.txt";
        }
        else
        {
            // Get for the server
            if(serv == null)
            {
                Bot.sendFollowError(event, "Cannot get quotes, not in server and no user supplied");
            }

            Collection<User> users = serv.getMembers();

            quotes = CafeDB.getQuotes(users);

            filename = serv.getIdAsString() + "_quotes.txt";
        }

        HashMap<Long, String> nicknames = new HashMap<>();
        if(singleUser != null)
        {
            String nick = Bot.getNickname(singleUser, serv);
            nicknames.put(singleUser.getId(), nick);
        }

        if(quotes.isEmpty())
        {
            event.createFollowupMessageBuilder()
                .addEmbed(new EmbedBuilder().addField("Oops", "No quotes to be found"))
                .send();
            return;
        }

        try
        {
            FileWriter writer = new FileWriter(filename);
            for(QuoteEntry q : quotes)
            {
                StringBuilder s = new StringBuilder();
                User u = singleUser;
                if(u == null || !nicknames.containsKey(q.userId))
                {
                    try
                    {
                        u = Bot.inst.getApi().getUserById(q.userId).get();
                    }
                    catch(InterruptedException e)
                    {
                        Bot.inst.logDbg("getQuote() Skipping user " + q.userId + ", interrupted");
                        continue;
                    }
                    catch(ExecutionException e)
                    {
                        Bot.inst.logDbg(
                            "getQuote() Skipping user " + q.userId + ", execution error: " + e.getMessage());
                        continue;
                    }
                }

                if(u != null)
                {
                    String nick = nicknames.get(u.getId());
                    if(nick == null)
                    {
                        nick = Bot.getNickname(u, serv);
                    }

                    s.append(nick);
                    s.append(" ( ").append(q.timestamp.toString()).append(" )\n");
                    s.append(q.content).append("\n\n");

                    writer.write(s.toString());
                }
            }
            writer.close();
        }
        catch(IOException e)
        {
            throw new CmdError("Cannot create quote file: " + e.getMessage());
        }

        File f = new File(filename);
        event.createFollowupMessageBuilder().addAttachment(f).send().join();
        f.delete();
    }

    @Override
    public void shutdown()
    {
        // XXX Auto-generated method stub
        // TODO?
    }

    @Override
    public List<SlashCommandBuilder> buildCommands()
    {
        LinkedList<SlashCommandBuilder> out = new LinkedList<>();

        out.add(
            SlashCommand.with("quote", "Save a quote for a user")
                .addOption(SlashCommandOption.create(SlashCommandOptionType.USER, "user", "The user", true))
                .addOption(
                    SlashCommandOption.create(SlashCommandOptionType.STRING, "quote", "The quote", true)));

        registerCmdFunc(this::addQuote, "quote");

        out.add(SlashCommand.with("get-quotes", "Get a list of quotes for the server/user")
                    .addOption(SlashCommandOption.create(
                        SlashCommandOptionType.USER, "user", "The user to lookup")));

        registerCmdFunc(this::getQuotes, "get-quotes");

        return out;
    }
}
