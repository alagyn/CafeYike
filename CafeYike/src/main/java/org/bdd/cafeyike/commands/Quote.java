package org.bdd.cafeyike.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.bdd.cafeyike.CafeConfig;
import org.bdd.cafeyike.CafeDB;
import org.bdd.cafeyike.CafeDB.QuoteEntry;
import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.Cog;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.bdd.cafeyike.commander.utils.DoAfter;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;

public class Quote extends Cog
{

    private static final String QUOTE_MODAL = "quoteM";
    private static final String EDIT_BTN = "edit";

    private static final String STR_DATE_FMT = "MM/DD/YY HH:MM";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yy HH:mm");

    private static final String USER_OP = "user", QUOTE_OP = "quote";

    private ConcurrentHashMap<Long, Message> editMessages = new ConcurrentHashMap<>();

    private final int quoteEditTimeSec;

    public Quote(Bot bot)
    {
        super(bot);
        quoteEditTimeSec = CafeConfig.getIntConfig("quoteEditTimeSec");
    }

    @Override
    public void shutdown()
    {
        for(Message m : editMessages.values())
        {
            m.editMessageComponents(new ArrayList<>()).complete();
        }
    }

    public void addQuote(SlashCommandInteractionEvent event)
    {
        InteractionHook hook = event.getHook();

        Member user = event.getOption(USER_OP).getAsMember();
        Guild serv = event.getGuild();

        if(user == null)
        {
            sendError(hook, "Cannot quote user");
        }

        if(serv == null)
        {
            sendError(hook, "Cannot quote outside a server");
        }

        String content = event.getOption(QUOTE_OP).getAsString();

        if(content == null || content.isEmpty())
        {
            sendError(hook, "Quote cannot be empty");
        }

        long _quoteId = 0;
        try
        {
            _quoteId = CafeDB.addQuote(serv.getIdLong(), user.getIdLong(), content);
        }
        catch(CmdError err)
        {
            sendError(hook, err.getMessage());
        }

        final long quoteId = _quoteId;

        EmbedBuilder b = new EmbedBuilder().addField("Quote: " + user.getEffectiveName(), content, false)
                .setFooter("10min to edit");
        Message m = hook.sendMessageEmbeds(b.build()).addActionRow(getEditBtn(quoteId)).complete();
        editMessages.put(quoteId, m);
        new DoAfter(quoteEditTimeSec, x ->
        {
            b.setFooter("Quote Locked");
            m.editMessageComponents(new ArrayList<>()).setEmbeds(b.build()).queue();
            editMessages.remove(quoteId);
        });
    }

    private Button getEditBtn(long quoteId)
    {
        return Button.primary(Bot.makeId(EDIT_BTN, quoteId), "Edit");
    }

    public void getQuotes(SlashCommandInteractionEvent event)
    {
        Member singleUser = event.getOption(USER_OP, OptionMapping::getAsMember);

        List<QuoteEntry> quotes = null;
        String filename = "";

        Guild serv = event.getGuild();

        boolean showIds = event.getOption("show-ids", false, OptionMapping::getAsBoolean);

        InteractionHook hook = event.getHook();

        if(singleUser != null)
        {
            ArrayList<Member> users = new ArrayList<>();
            users.add(singleUser);
            // Get for a single user
            try
            {
                quotes = CafeDB.getQuotesForUser(serv.getIdLong(), singleUser.getIdLong());
            }
            catch(CmdError err)
            {
                sendError(hook, "Unable to get quotes");
            }

            filename = singleUser.getId() + "_quotes.txt";
        }
        else
        {
            // Get for the server
            if(serv == null)
            {
                sendError(hook, "Cannot get quotes, not in server and no user supplied");
            }

            try
            {
                quotes = CafeDB.getQuotesForServer(serv.getIdLong());
            }
            catch(CmdError err)
            {
                sendError(hook, "Unable to get quotes");
            }

            filename = serv.getId() + "_quotes.txt";
        }

        if(quotes.isEmpty())
        {
            hook.sendMessageEmbeds(new EmbedBuilder().addField("Oops", "No quotes to be found", false).build()).queue();
            return;
        }

        HashMap<Long, String> nicknames = new HashMap<>();

        String singleNick = null;
        if(singleUser != null)
        {
            singleNick = singleUser.getEffectiveName();
        }

        try
        {
            FileWriter writer = new FileWriter(filename);
            for(QuoteEntry q : quotes)
            {
                String nick = singleNick;
                if(nick == null)
                {
                    nick = nicknames.get(q.userId);
                    if(nick == null)
                    {
                        Member m = serv.getMemberById(q.userId);
                        if(m != null)
                        {
                            nick = m.getEffectiveName();
                            nicknames.put(q.userId, nick);
                        }
                        else
                        {
                            nick = "[Unkown User]";
                        }
                    }
                }

                StringBuilder s = new StringBuilder();
                if(showIds)
                {
                    s.append("ID: ").append(q.quoteId).append("\n");
                }
                s.append(nick);
                s.append(" ( ").append(q.timestamp.toString()).append(" )\n");
                s.append(q.content).append("\n\n");

                writer.write(s.toString());
            }
            writer.close();
        }
        catch(IOException e)
        {
            sendError(hook, "Cannot create quote file");
        }

        File f = new File(filename);
        hook.sendFiles(FileUpload.fromData(f)).queue(message ->
        {
            f.delete();
        });
    }

    private void editBtn(ButtonInteractionEvent event, String data)
    {
        Long quoteId = Long.parseLong(data);

        TextInput newQuote = TextInput.create("newQuote", "New Quote", TextInputStyle.SHORT).build();

        TextInput newTs = TextInput
                .create("time", "New Timestamp: " + STR_DATE_FMT + " (24 hour clock)", TextInputStyle.SHORT)
                .setRequired(false).build();

        Modal modal = Modal.create(Bot.makeId(QUOTE_MODAL, quoteId), "Edit Quote").addActionRow(newQuote)
                .addActionRow(newTs).build();

        event.replyModal(modal).queue();
    }

    private void editModal(ModalInteractionEvent event, String data)
    {
        // Parse quote ID
        Long quoteId = Long.parseLong(data);

        String newQuote = event.getValue("newQuote").getAsString();
        String newTsString = event.getValue("time").getAsString();

        InteractionHook hook = event.getHook();
        event.deferReply().complete();

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
                sendError(hook, "Cannot parse new timestamp");
            }
        }

        if(newTs == null && newQuote.isEmpty())
        {
            hook.sendMessageEmbeds(new EmbedBuilder().setTitle("Quote Edit").setDescription("Unchanged").build())
                    .queue();
            return;
        }

        QuoteEntry oldQuote = CafeDB.getQuoteByID(quoteId);
        CafeDB.editQuote(quoteId, newQuote, newTs);

        StringBuilder out = new StringBuilder();

        try
        {
            Guild serv = event.getGuild();
            Member m = serv.getMemberById(oldQuote.userId);
            out.append(m.getEffectiveName()).append(" said:\n");
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

        hook.sendMessageEmbeds(new EmbedBuilder().setTitle("Quote Updated").setDescription(out.toString()).build())
                .queue();
    }

    @Override
    public List<CommandData> buildCommands()
    {
        LinkedList<CommandData> out = new LinkedList<>();

        out.add(Commands.slash("quote", "Save a quote for a user").addOption(OptionType.USER, USER_OP, "The user", true)
                .addOption(OptionType.STRING, QUOTE_OP, "The quote", true));

        registerCmdFunc(this::addQuote, "quote");

        out.add(Commands.slash("get-quotes", "Get a list of quotes for the server/user")
                .addOption(OptionType.USER, USER_OP, "The user to lookup")
                .addOption(OptionType.BOOLEAN, "show-ids", "Show quote ids so you can edit a specific quote"));

        registerCmdFunc(this::getQuotes, "get-quotes");

        registerBtnFunc(this::editBtn, EDIT_BTN);

        registerModal(this::editModal, QUOTE_MODAL);
        return out;
    }

}
