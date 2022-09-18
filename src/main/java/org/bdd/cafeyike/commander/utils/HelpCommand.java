package org.bdd.cafeyike.commander.utils;

import org.bdd.cafeyike.commander.Arguments;
import org.bdd.cafeyike.commander.CommandListener;
import org.bdd.cafeyike.commander.commands.Command;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

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
        return "help: Shows this help message";
    }

    @Override
    public String getUsage()
    {
        return "na";
    }

    @Override
    public void call(MessageCreateEvent event, Arguments args) throws CmdError
    {
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

        String out;
        if(command.isEmpty())
        {
            out = cl.getHelp(showAdmin, showBotOwner);
        }
        else
        {
            out = cl.getUsage(command, showAdmin, showBotOwner);
        }

        MessageBuilder msg = new MessageBuilder();

        msg.addEmbed(new EmbedBuilder().addField("ASDF", out));
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
