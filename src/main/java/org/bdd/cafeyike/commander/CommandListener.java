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
import org.javacord.api.interaction.Interaction;
import org.javacord.api.interaction.InteractionType;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.InteractionCreateListener;

public class CommandListener implements InteractionCreateListener
{
    private HashMap<String, Cmd> commands = new HashMap<>();
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
        default:
            break;
        }
    }

    private void runCommand(SlashCommandInteraction interaction)
    {
        if(interaction == null)
        {
            return;
        }

        String commandName = interaction.getCommandName();
        Bot.inst.logDbg("Got cmd: " + commandName);
        Cmd c = commands.get(commandName);

        if(c == null)
        {
            interaction.createImmediateResponder()
                .addEmbed(new EmbedBuilder().addField("Error", "Command \"" + commandName + "\" not found"))
                .setFlags(MessageFlag.EPHEMERAL)
                .respond();
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
            System.out.println("CommandListener:runCommand() Error Caught, Stacktrace:");
            e.printStackTrace();
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
                Bot.inst.logDbg("Registering command: " + c.name);
            }
        }

        api.bulkOverwriteGlobalApplicationCommands(toRegister).join();
    }
}
