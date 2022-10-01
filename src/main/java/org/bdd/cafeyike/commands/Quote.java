package org.bdd.cafeyike.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.bdd.cafeyike.CafeDB;
import org.bdd.cafeyike.CafeDB.QuoteEntry;
import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.Cog;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.component.TextInput;
import org.javacord.api.entity.message.component.TextInputStyle;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.interaction.Interaction;
import org.javacord.api.interaction.ModalInteraction;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

public class Quote extends Cog
{
    private static final String QUOTE_MODAL = "quoteM";
    private static final String EDIT_BTN = "edit";
    private static final String RM_BTN = "rm";

    private static final String STR_DATE_FMT = "MM/DD/YY HH:MM";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yy HH:mm");

    public Quote()
    {
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

        long quoteId = CafeDB.addQuote(user.getId(), content);

        try
        {
            event.createFollowupMessageBuilder().addEmbed(new EmbedBuilder().addField("Quote: " + nick, content))
                    .addComponents(getQuoteBtns(quoteId)).send().get();
        }
        catch(Exception e)
        {
            Bot.sendFollowError(event, "Quote added, but cannot get message: " + e.getMessage());
        }
    }

    public ActionRow getQuoteBtns(long quoteId)
    {
        return ActionRow.of(Button.primary(EDIT_BTN + ":" + quoteId, "Edit"),
                Button.danger(RM_BTN + ":" + quoteId, "Delete"));
    }

    public void getQuotes(SlashCommandInteraction event)
    {
        User singleUser = event.getOptionUserValueByName("user").orElse(null);

        List<QuoteEntry> quotes = null;
        String filename = "";

        Server serv = event.getServer().orElse(null);

        boolean showIds = event.getOptionBooleanValueByName("show-ids").orElse(false);

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
            event.createFollowupMessageBuilder().addEmbed(new EmbedBuilder().addField("Oops", "No quotes to be found"))
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

                    if(showIds)
                    {
                        s.append("ID: ").append(q.quoteId).append("\n");
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

    public void editQuote(SlashCommandInteraction event)
    {
        long quoteId = event.getOptionLongValueByIndex(0).orElse(-1L);
        if(quoteId <= 0)
        {
            Bot.sendError(event, "Invalid Quote ID");
        }

        sendQuoteEditModal((Interaction) event, quoteId);
    }

    private void sendQuoteEditModal(Interaction event, long quoteId)
    {
        event.respondWithModal(QUOTE_MODAL + ":" + quoteId, "Edit Quote",
                ActionRow.of(TextInput.create(TextInputStyle.SHORT, "newQuote", "New Quote")), ActionRow.of(TextInput
                        .create(TextInputStyle.SHORT, "time", "New Timestamp: " + STR_DATE_FMT + " (24 hour clock)")));

    }

    private void rmQuote(SlashCommandInteraction event)
    {
        long quoteId = event.getOptionLongValueByIndex(0).orElse(-1L);
        if(quoteId <= 0)
        {
            Bot.sendError(event, "Invalid Quote ID");
        }

        event.respondLater();

        CafeDB.rmQuote(quoteId);
        // TODO message
        event.createFollowupMessageBuilder().addEmbed(new EmbedBuilder().addField("Quote", "Deleted")).send();
    }

    private void editBtn(ButtonInteraction event, String data)
    {
        Long quoteId = Long.parseLong(data);
        sendQuoteEditModal((Interaction) event, quoteId);
    }

    private void rmBtn(ButtonInteraction event, String data)
    {
        Long quoteId = Long.parseLong(data);
        event.getMessage().delete();

        event.createImmediateResponder().addEmbed(new EmbedBuilder().addField("Quote", "Deleted")).respond();

        CafeDB.rmQuote(quoteId);
    }

    private void editModal(ModalInteraction event, String data)
    {
        // Parse quote ID
        Long quoteId = Long.parseLong(data);

        String newQuote = event.getTextInputValueByCustomId("newQuote").orElse("");
        String newTsString = event.getTextInputValueByCustomId("time").orElse("");

        event.respondLater();

        Timestamp newTs = null;
        if(!newTsString.isEmpty())
        {
            try
            {
                Date newDate = DATE_FORMAT.parse(newTsString);
                newTs = new Timestamp(newDate.getTime());
            }
            catch(ParseException e)
            {
                Bot.sendFollowError((Interaction) event, "Cannot parse new timestamp");
            }
        }

        if(newTs == null && newQuote.isEmpty())
        {
            event.createFollowupMessageBuilder().addEmbed(new EmbedBuilder().addField("Quote Edit", "Unchanged"))
                    .send();
            return;
        }

        QuoteEntry oldQuote = CafeDB.getQuote(quoteId);
        CafeDB.editQuote(quoteId, newQuote, newTs);

        StringBuilder out = new StringBuilder();

        try
        {
            User u = Bot.inst.getApi().getUserById(oldQuote.userId).get();
            Server serv = event.getServer().orElse(null);
            out.append(Bot.getNickname(u, serv)).append(" said:\n");
        }
        catch(Exception e)
        {
            out.append("Unknown user said:\n");
        }

        out.append("Old:\n");
        out.append(oldQuote.timestamp.toString()).append("\n");
        out.append(oldQuote.content).append("\n\n");

        out.append("New:\n");
        if(newTs != null)
        {
            out.append(newTs.toString()).append("\n");
        }
        else
        {
            out.append(oldQuote.timestamp.toString()).append("\n");
        }
        out.append(newQuote);

        event.createFollowupMessageBuilder().addEmbed(new EmbedBuilder().addField("Quote Updated", out.toString()))
                .addComponents(getQuoteBtns(quoteId)).send();

    }

    @Override
    public List<SlashCommandBuilder> buildCommands()
    {
        LinkedList<SlashCommandBuilder> out = new LinkedList<>();

        out.add(SlashCommand.with("quote", "Save a quote for a user")
                .addOption(SlashCommandOption.create(SlashCommandOptionType.USER, "user", "The user", true))
                .addOption(SlashCommandOption.create(SlashCommandOptionType.STRING, "quote", "The quote", true)));

        registerCmdFunc(this::addQuote, "quote");

        out.add(SlashCommand.with("get-quotes", "Get a list of quotes for the server/user")
                .addOption(SlashCommandOption.create(SlashCommandOptionType.USER, "user", "The user to lookup"))
                .addOption(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "show-ids",
                        "Show quote ids so you can edit a specific quote")));

        registerCmdFunc(this::getQuotes, "get-quotes");

        out.add(SlashCommand.with("edit-quote", "Edit a quote").addOption(
                SlashCommandOption.create(SlashCommandOptionType.LONG, "quote-id", "The ID of the quote", true)));

        registerCmdFunc(this::editQuote, "edit-quote");

        out.add(SlashCommand.with("rem-quote", "Remove a quote")
                .addOption(SlashCommandOption.create(SlashCommandOptionType.LONG, "quote-id", "The quote ID", true)));

        registerCmdFunc(this::rmQuote, "rem-quote");

        registerBtnFunc(this::editBtn, EDIT_BTN);
        registerBtnFunc(this::rmBtn, RM_BTN);

        registerModal(this::editModal, QUOTE_MODAL);
        return out;
    }

}
