package org.bdd.cafeyike.commander.utils;

import org.bdd.cafeyike.commander.Arguments;
import org.bdd.cafeyike.commander.CommandListener;
import org.bdd.cafeyike.commander.commands.Command;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.interaction.InteractionCreateEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;

public class HelpCommand extends Command
{
    private CommandListener cl;

    private String[] aliases = {"help", "h"};

    public HelpCommand(CommandListener cl)
    {
        this.cl = cl;
    }

    @Override
    public void shutdown()
    {
        // Pass
    }

    @Override
    public String getHelp(boolean showAdmin, boolean showBotOwner)
    {
        return "Shows this help message";
    }

    @Override
    public String getUsage()
    {
        return "";
    }

    @Override
    public void call(InteractionCreateEvent event, Arguments args) throws CmdError
    {
        SlashCommandInteraction i = event.getSlashCommandInteraction().get();
        boolean showAdmin = false;
        boolean showBotOwner = false;
        String command = "";
        while(args.hasNext())
        {
            String arg = args.next();

            if(arg.equals("-a") && event.getMessageAuthor().isServerAdmin())
            {
                showAdmin = true;
                continue;
            }

            if(arg.equals("-b") && event.getMessageAuthor().isBotOwner())
            {
                showBotOwner = true;
                if(event.getMessageAuthor().isServerAdmin())
                {
                    showAdmin = true;
                }

                continue;
            }

            command = arg;
        }

        MessageBuilder msg = new MessageBuilder();

        if(command.isEmpty())
        {
            msg.addEmbed(cl.getHelp(showAdmin, showBotOwner));
        }
        else
        {
            msg.addEmbed(new EmbedBuilder().addField(command, cl.getUsage(command, showAdmin, showBotOwner)));
        }

        Message m = msg.replyTo(event.getMessage()).send(event.getChannel()).join();

        new MsgDeleteAfter(m, 60);
        new MsgDeleteAfter(event.getMessage(), 60);
    }

    @Override
    public String[] getAliases()
    {
        return aliases;
    }
}
