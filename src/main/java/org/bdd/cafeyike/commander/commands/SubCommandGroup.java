package org.bdd.cafeyike.commander.commands;

import java.util.HashMap;
import java.util.LinkedList;
import org.bdd.cafeyike.commander.Arguments;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.bdd.cafeyike.commander.exceptions.CmdNotFoundError;
import org.javacord.api.event.message.MessageCreateEvent;

public class SubCommandGroup extends Command
{
    private HashMap<String, Command> commandMap;
    public final String groupName;

    private final String[] groupAliases;
    private final LinkedList<String> defaultNames;

    private Command defaultCmd;

    public SubCommandGroup(String[] groupAliases) throws CmdError
    {
        this.groupAliases = groupAliases;
        this.groupName = groupAliases[0];
        defaultNames = new LinkedList<>();

        commandMap = new HashMap<>();
        defaultCmd = null;
    }

    @Override
    public void call(MessageCreateEvent event, Arguments args) throws CmdError
    {
        if(!args.hasNext())
        {
            //TODO incorporate groupname into error for better output
            throw new CmdError("No Command Given");
        }

        String commandName = args.next(true);

        Command c = commandMap.get(commandName);

        if(c == null)
        {
            if(defaultCmd == null)
            {
                throw new CmdNotFoundError(commandName);
            }
            else
            {
                //Rollback args for default command
                args.prev(true);
                c = defaultCmd;
            }
        }

        try
        {
            c.call(event, args);
        }
        catch(CmdError e)
        {
            throw new CmdError(groupName + ": " + e.getMessage());
        }
    }

    @Override
    public void shutdown()
    {
        for(String name : defaultNames)
        {
            commandMap.get(name).shutdown();
        }
    }

    @Override
    public String[] getAliases()
    {
        return groupAliases;
    }

    public void addCommand(Command c) throws CmdError
    {
        String[] names = c.getAliases();

        if(names.length <= 0)
        {
            throw new CmdError("No command name given");
        }

        for(String alias : names)
        {
            if(commandMap.containsKey(alias))
            {
                throw new CmdError(
                    String.format("Repeated command name: '%s' in group: '%s'", alias, groupName));
            }

            commandMap.put(alias, c);
        }

        defaultNames.add(c.getAliases()[0]);
    }

    public Command getCommand(String name)
    {
        Command out = commandMap.get(name);
        if(out == null)
        {
            throw new CmdNotFoundError(name);
        }
        return out;
    }

    public void setDefaultCmd(Command c)
    {
        this.defaultCmd = c;
    }

    @Override
    public String getHelp(boolean showAdmin, boolean showBotOwner)
    {
        StringBuilder out = new StringBuilder();

        for(String name : defaultNames)
        {
            Command c = commandMap.get(name);
            if((!showAdmin && c.adminOnly) || (!showBotOwner && c.botOwnerOnly))
            {
                continue;
            }

            out.append(c.getHelp(showAdmin, showBotOwner));
            out.append("\n");
        }

        return out.toString();
    }

    @Override
    public String getUsage()
    {
        return "na";
    }
}
