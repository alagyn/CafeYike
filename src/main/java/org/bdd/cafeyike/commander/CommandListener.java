package org.bdd.cafeyike.commander;

import java.util.HashMap;
import java.util.LinkedList;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.bdd.cafeyike.commander.exceptions.UsageError;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandListener extends ListenerAdapter
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
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        String commandName = event.getCommandPath();
        log.debug("onSlashCommandInteraction() Got cmd: {}", commandName);
        Cmd c = commands.get(commandName);

        if(c == null)
        {
            event.replyEmbeds(
                    new EmbedBuilder().addField("Error", "Command \"" + commandName + "\" not found", false).build())
                    .setEphemeral(true).queue();

            return;
        }

        try
        {
            c.func.accept(event);
        }
        catch(UsageError e)
        {
            // Pass
        }
        catch(CmdError e)
        {
            log.error("runCommand() Error Caught, Stacktrace:", e);
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event)
    {
        // Limit to one split, i.e. 2 sections
        String[] data = event.getModalId().split(":", 2);
        log.debug("Got modal: {}", data[0]);

        Mdl m = modals.get(data[0]);

        if(m == null)
        {
            event.replyEmbeds(
                    new EmbedBuilder().addField("Error", "Modal \"" + data[0] + "\" not found", false).build())
                    .setEphemeral(true).queue();
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

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event)
    {
        // Limit to one split, i.e. 2 sections
        String[] data = event.getButton().getId().split(":", 2);
        log.debug("runButton() Got btn: {}", data[0]);

        Btn b = buttons.get(data[0]);

        if(b == null)
        {
            event.replyEmbeds(
                    new EmbedBuilder().addField("Error", "Button \"" + data[0] + "\" not found", false).build())
                    .setEphemeral(true).queue();

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

    public void shutdown()
    {
        for(Cog c : cogs)
        {
            c.shutdown();
        }
    }

    public void registerCommands(JDA api)
    {
        LinkedList<CommandData> toRegister = new LinkedList<>();

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
        api.updateCommands().addCommands(toRegister);
    }
}
