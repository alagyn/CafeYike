package org.bdd.cafeyike.commander;

import java.util.HashMap;
import java.util.LinkedList;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.bdd.cafeyike.commander.exceptions.UsageError;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.interaction.InteractionCreateEvent;
import org.javacord.api.interaction.AutocompleteInteraction;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.interaction.Interaction;
import org.javacord.api.interaction.InteractionType;
import org.javacord.api.interaction.MessageComponentInteraction;
import org.javacord.api.interaction.ModalInteraction;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.InteractionCreateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandListener implements InteractionCreateListener
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private HashMap<String, Cmd> commands = new HashMap<>();
    private HashMap<String, Btn> buttons = new HashMap<>();
    private HashMap<String, Mdl> modals = new HashMap<>();
    private LinkedList<Cog> cogs = new LinkedList<>();

    public CommandListener()
    {
    }

    public void addCog(Cog cog)
    {
        cogs.add(cog);
    }

    @Override
    public void onInteractionCreate(InteractionCreateEvent event)
    {
        Interaction i = event.getInteraction();

        InteractionType type = i.getType();

        switch(type)
        {
        case APPLICATION_COMMAND:
            runCommand(i.asSlashCommandInteraction().get());
            break;
        case APPLICATION_COMMAND_AUTOCOMPLETE:
            runCommandAutocomplete(i.asAutocompleteInteraction().get());
            break;
        case MESSAGE_COMPONENT:
            runComponent(i.asMessageComponentInteraction().get());
            break;
        case MODAL_SUBMIT:
            runModal(i.asModalInteraction().get());
            break;
        default:
            break;
        }
    }

    private void runCommand(SlashCommandInteraction interaction)
    {
        String commandName = interaction.getCommandName();
        log.debug("runCommand() Got cmd: {}", commandName);
        Cmd c = commands.get(commandName);

        if(c == null)
        {
            interaction.createImmediateResponder()
                    .addEmbed(new EmbedBuilder().addField("Error", "Command \"" + commandName + "\" not found"))
                    .setFlags(MessageFlag.EPHEMERAL).respond();
            return;
        }

        try
        {
            c.func.accept(interaction);
        }
        catch(UsageError e)
        {
            // Pass
        }
        catch(CmdError e)
        {
            System.out.println("runCommand() Error Caught, Stacktrace:");
            e.printStackTrace();
        }
    }

    private void runModal(ModalInteraction event)
    {
        // Limit to one split, i.e. 2 sections
        String[] data = event.getCustomId().split(":", 2);
        log.debug("Got modal: {}", data[0]);

        Mdl m = modals.get(data[0]);

        if(m == null)
        {
            event.createImmediateResponder()
                    .addEmbed(new EmbedBuilder().addField("Error", "Modal \"" + data[0] + "\" not found"))
                    .setFlags(MessageFlag.EPHEMERAL).respond();
            return;
        }

        try
        {
            m.func.accept(event, data[1]);
        }
        catch(UsageError e)
        {
            // Pass
        }
        catch(CmdError e)
        {
            log.error("runModal() Error Caught, Stacktrace:", e);
        }
    }

    private void runComponent(MessageComponentInteraction event)
    {
        switch(event.getComponentType())
        {
        case BUTTON:
            runButton(event.asButtonInteraction().get());
            break;
        default:
            break;
        }
    }

    private void runButton(ButtonInteraction event)
    {
        // Limit to one split, i.e. 2 sections
        String[] data = event.getCustomId().split(":", 2);
        log.debug("runButton() Got btn: {}", data[0]);

        Btn b = buttons.get(data[0]);

        if(b == null)
        {
            event.createImmediateResponder()
                    .addEmbed(new EmbedBuilder().addField("Error", "Button \"" + data[0] + "\" not found"))
                    .setFlags(MessageFlag.EPHEMERAL).respond();
            return;
        }

        try
        {
            b.func.accept(event, data[1]);
        }
        catch(UsageError e)
        {
            // Pass
        }
        catch(CmdError e)
        {
            log.error("runButton() Error Caught, Stacktrace:", e);
        }
    }

    private void runCommandAutocomplete(AutocompleteInteraction interaction)
    {
        if(interaction == null)
        {
            return;
        }
    }

    public void shutdown()
    {
        for(Cog c : cogs)
        {
            c.shutdown();
        }
    }

    public void registerCommands(DiscordApi api)
    {
        LinkedList<SlashCommandBuilder> toRegister = new LinkedList<>();

        for(Cog cog : cogs)
        {
            toRegister.addAll(cog.buildCommands());
            for(Cmd c : cog.getCommands())
            {
                Cmd old = commands.put(c.name, c);
                if(old != null)
                {
                    throw new CmdError("Duplicate command name: " + c.name);
                }
                log.debug("registerCommands() Registering command: {}", c.name);
            }

            for(Btn b : cog.getButtons())
            {
                Btn old = buttons.put(b.prefix, b);
                if(old != null)
                {
                    throw new CmdError("registerCommands() Duplicate button prefix: " + b.prefix);
                }
                log.debug("registerCommands() Registering button: {}", b.prefix);
            }

            for(Mdl m : cog.getModals())
            {
                Mdl old = modals.put(m.prefix, m);
                if(old != null)
                {
                    throw new CmdError("registerCommands() Duplicate modal prefix: " + m.prefix);
                }
                log.debug("registerCommands() Registering modal: {}", m.prefix);
            }

            cog.registerListeners(api);
        }

        api.bulkOverwriteGlobalApplicationCommands(toRegister).join();
    }
}
