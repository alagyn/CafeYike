package org.bdd.javacordCmd.commands;

import org.bdd.javacordCmd.Arguments;
import org.bdd.javacordCmd.exceptions.CmdError;
import org.bdd.javacordCmd.exceptions.CmdNotFoundError;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.HashMap;

public class SubCommandGroup implements Command
{
    public HashMap<String, Command> commandMap;
    public final String groupName;

    private final String[] groupAliases;

    private Command defaultCmd;

    public SubCommandGroup(String[] groupAliases) throws CmdError
    {
        this.groupAliases = groupAliases;
        this.groupName = groupAliases[0];
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
    public String[] getNames()
    {
        return groupAliases;
    }

    public void addCommand(Command c) throws CmdError
    {
        for(String alias : c.getNames())
        {
            if(commandMap.containsKey(alias))
            {
                throw new CmdError("Repeated command name: '%s' in group: '%s'".formatted(alias, groupName));
            }

            commandMap.put(alias, c);
        }
    }

    public void setDefaultCmd(Command c)
    {
        this.defaultCmd = c;
    }

}
