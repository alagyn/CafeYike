package org.bdd.javacordCmd.commands;

import org.bdd.javacordCmd.Arguments;
import org.bdd.javacordCmd.exceptions.CmdError;
import org.bdd.javacordCmd.exceptions.CmdNotFoundError;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.HashMap;
import java.util.LinkedList;


public class SubCommandGroup implements Command
{
    public HashMap<String, Command> commandMap;
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
            throw new CmdError(groupName + ":" + e.getMessage());
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
    public String[] getNames()
    {
        return groupAliases;
    }

    public void addCommand(Command c) throws CmdError
    {
        String[] names = c.getNames();

        if(names.length <= 0)
        {
            throw new CmdError("No command name given");
        }

        for(String alias : names)
        {
            if(commandMap.containsKey(alias))
            {
                throw new CmdError("Repeated command name: '%s' in group: '%s'".formatted(alias, groupName));
            }

            commandMap.put(alias, c);
        }

        defaultNames.add(c.getNames()[0]);
    }

    public void setDefaultCmd(Command c)
    {
        this.defaultCmd = c;
    }

}
