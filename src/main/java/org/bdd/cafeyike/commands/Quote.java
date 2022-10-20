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

import org.bdd.cafeyike.CafeConfig;
import org.bdd.cafeyike.CafeDB;
import org.bdd.cafeyike.CafeDB.QuoteEntry;
import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.Cog;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.bdd.cafeyike.commander.utils.DoAfter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.utils.FileUpload;

public class Quote extends Cog
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String QUOTE_MODAL = "quoteM";
    private static final String EDIT_BTN = "edit";

    private static final String STR_DATE_FMT = "MM/DD/YY HH:MM";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yy HH:mm");

    private static final String USER_OP = "user", QUOTE_OP = "quote";

    private final int quoteEditTimeSec;

    public Quote(Bot bot)
    {
        super(bot);
        quoteEditTimeSec = CafeConfig.getIntConfig("quoteEditTimeSec");
    }

    public void addQuote(SlashCommandInteractionEvent event)
    {
        Member user = event.getOption(USER_OP).getAsMember();

        if(user == null)
        {
            sendError(event, "Cannot quote user");
        }

        String content = event.getOption(QUOTE_OP).getAsString();

        if(content == null || content.isEmpty())
        {
            sendError(event, "Quote cannot be empty");
        }

        event.deferReply().queue();
        InteractionHook hook = event.getHook();

        long quoteId = CafeDB.addQuote(user.getIdLong(), content);

        EmbedBuilder b = new EmbedBuilder().addField("Quote: " + user.getEffectiveName(), content, false)
                .setFooter("10min to edit");
        Message m = hook.sendMessageEmbeds(b.build()).addActionRow(getEditBtn(quoteId)).complete();
        new DoAfter(quoteEditTimeSec, x ->
        {
            b.setFooter("Quote Locked");
            m.editMessageComponents(new ArrayList<>()).setEmbeds(b.build()).queue();
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

        event.deferReply().queue();
        InteractionHook hook = event.getHook();

        if(singleUser != null)
        {
            ArrayList<Member> users = new ArrayList<>();
            users.add(singleUser);
            // Get for a single user
            quotes = CafeDB.getQuotes(users);
            filename = singleUser.getId() + "_quotes.txt";
        }
        else
        {
            // Get for the server
            if(serv == null)
            {
                sendFollowError(hook, "Cannot get quotes, not in server and no user supplied");
            }

            Collection<Member> users = serv.getMembers();

            quotes = CafeDB.getQuotes(users);

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
                        nick = m.getEffectiveName();
                        nicknames.put(q.userId, nick);
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
            log.error("Cannot create quote file : {}", e.getMessage());
            throw new CmdError("Cannot create quote file: " + e.getMessage());
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
                .create("time", "New Timestamp: " + STR_DATE_FMT + " (24 hour clock)", TextInputStyle.SHORT).build();

        Modal modal = Modal.create(Bot.makeId(QUOTE_MODAL, quoteId), "Edit Quote")
                .addActionRows(ActionRow.of(newQuote), ActionRow.of(newTs)).build();

        event.replyModal(modal).queue();
    }

    private void editModal(ModalInteractionEvent event, String data)
    {
        // Parse quote ID
        Long quoteId = Long.parseLong(data);

        String newQuote = event.getValue("newQuote").getAsString();
        String newTsString = event.getValue("time").getAsString();

        event.deferReply().queue();
        InteractionHook hook = event.getHook();

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
                sendFollowError(hook, "Cannot parse new timestamp");
            }
        }

        if(newTs == null && newQuote.isEmpty())
        {
            hook.sendMessageEmbeds(new EmbedBuilder().setTitle("Quote Edit").setDescription("Unchanged").build())
                    .queue();
            return;
        }

        QuoteEntry oldQuote = CafeDB.getQuote(quoteId);
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
