package org.bdd.cafeyike.commander;

import java.util.HashMap;
import java.util.LinkedList;
import org.bdd.cafeyike.commander.commands.Cog;
import org.bdd.cafeyike.commander.commands.Command;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.bdd.cafeyike.commander.exceptions.CmdNotFoundError;
import org.bdd.cafeyike.commander.utils.HelpCommand;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

public class CommandListener implements MessageCreateListener
{
    public String prefix;
    private HashMap<String, Command> commands = new HashMap<>();
    private LinkedList<Cog> cogs = new LinkedList<>();
    private LinkedList<String> uncatCmds = new LinkedList<>();

    public CommandListener() throws CmdError
    {
        this.prefix = "";

        addCommand(new HelpCommand(this));
    }

    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    private void insertCmd(Command c)
    {
        for(String alias : c.getAliases())
        {
            commands.put(alias, c);
        }
    }

    public void addCommand(Command c) throws CmdError
    {
        uncatCmds.add(c.getAliases()[0]);
        insertCmd(c);
    }

    public void addCog(Cog cog)
    {
        cogs.add(cog);
        for(Command c : cog.getCommands())
        {
            insertCmd(c);
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

            Arguments args = new Arguments(content.substring(prefix.length()));
            if(!args.hasNext())
            {
                event.getChannel().sendMessage("No command specified");
                return;
            }

            String commandName = args.next();
            Command c = commands.get(commandName);

            if(c == null)
            {
                event.getChannel().sendMessage("Command \"" + commandName + "\" not found");
                return;
            }
            try
            {
                c.call(event, args);
            }
            catch(CmdNotFoundError e)
            {
                event.getChannel().sendMessage("Command \"" + commandName + "\" not found");
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

    public EmbedBuilder getHelp(boolean showAdmin, boolean showBotOwner)
    {
        EmbedBuilder out = new EmbedBuilder();

        for(Cog cog : cogs)
        {
            StringBuilder x = new StringBuilder();
            x.append(cog.cogName).append(": ").append(cog.cogBrief);
            String cmdHelp = cog.getHelp(showAdmin, showBotOwner);
            if(!cmdHelp.isEmpty())
            {
                out.addField(x.toString(), cog.getHelp(showAdmin, showBotOwner));
            }
        }

        StringBuilder x = new StringBuilder();
        for(String name : uncatCmds)
        {
            Command c = commands.get(name);
            x.append(c.getHelp(showAdmin, showBotOwner)).append("\n");
        }

        out.addField("Uncategorized", x.toString());

        return out;
    }

    public String getUsage(String name, boolean showAdmin, boolean showBotOwner)
    {
        StringBuilder out = new StringBuilder();
        Command c = commands.get(name);
        out.append(c.getHelp(showAdmin, showBotOwner)).append("\n");
        out.append(prefix).append(c.getUsage());
        return out.toString();
    }

    public void shutdown()
    {
        for(Cog c : cogs)
        {
            c.shutdown();
        }

        for(String name : uncatCmds)
        {
            commands.get(name).shutdown();
        }
    }
}
