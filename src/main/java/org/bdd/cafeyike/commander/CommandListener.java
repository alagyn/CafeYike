package org.bdd.cafeyike.commander;

import java.util.LinkedList;
import org.bdd.cafeyike.commander.commands.Cog;
import org.bdd.cafeyike.commander.commands.Command;
import org.bdd.cafeyike.commander.commands.SubCommandGroup;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.bdd.cafeyike.commander.exceptions.CmdNotFoundError;
import org.bdd.cafeyike.commander.utils.HelpCommand;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

public class CommandListener implements MessageCreateListener
{
    public String prefix;
    private final SubCommandGroup commands;
    private LinkedList<Cog> cogs = new LinkedList<>();

    public CommandListener() throws CmdError
    {
        this.prefix = "";
        commands = new SubCommandGroup(new String[] {"MAIN"});

        commands.addCommand(new HelpCommand(this));
    }

    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    public void addCommand(Command c) throws CmdError
    {
        commands.addCommand(c);
    }

    public void addCog(Cog cog)
    {
        cogs.add(cog);
        for(Command c : cog.getCommands())
        {
            addCommand(c);
        }
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event)
    {
        //Ignore messages from ourselves
        if(event.getMessageAuthor().isYourself())
        {
            return;
        }

        String content = event.getMessageContent();

        // TODO remove
        System.out.println("\"" + content + "\"");
        //

        if(content.startsWith(prefix))
        {
            //TODO remove
            System.out.println("Running Command");
            //

            try
            {
                commands.call(event, new Arguments(content.substring(1)));
            }
            catch(CmdNotFoundError e)
            {
                event.getChannel().sendMessage(e.getMessage());
            }
            catch(CmdError e)
            {
                //TODO better message?
                event.getChannel().sendMessage(e.getMessage());
                System.out.println("CommandListener:onMessageCreate() Error Caught, Stacktrace:");
                e.printStackTrace();
            }
        }
    }

    public String getHelp(boolean showAdmin, boolean showBotOwner)
    {
        // TODO sort by cogs?
        return commands.getHelp(showAdmin, showBotOwner);
    }

    public String getUsage(String name, boolean showAdmin, boolean showBotOwner)
    {
        StringBuilder out = new StringBuilder();
        Command c = commands.getCommand(name);
        out.append(c.getHelp(showAdmin, showBotOwner)).append("\n");
        out.append(prefix).append(commands.getCommand(name).getUsage());
        return out.toString();
    }

    public void shutdown()
    {
        commands.shutdown();
        for(Cog c : cogs)
        {
            c.shutdown();
        }
    }
}
