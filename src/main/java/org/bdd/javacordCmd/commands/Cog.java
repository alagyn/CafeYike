package org.bdd.javacordCmd.commands;

import org.bdd.javacordCmd.Arguments;
import org.bdd.javacordCmd.exceptions.CmdError;
import org.bdd.javacordCmd.exceptions.CmdNotFoundError;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.HashMap;
import java.util.function.BiConsumer;

public abstract class Cog implements Command
{
    public interface CogFuncRef extends BiConsumer<MessageCreateEvent, Arguments>
    {

    }

    public record CogFunc(String[] aliases, CogFuncRef cmd)
    {
    }

    public final String cogName;

    public Cog(String name)
    {
        this.cogName = name;
    }

    private final HashMap<String, CogFunc> commands = new HashMap<>();

    public void addCommand(CogFunc cmd)
    {
        for(String a : cmd.aliases)
        {
            if(commands.containsKey(a))
            {
                throw new CmdError("Duplicate command alias: " + a);
            }

            commands.put(a, cmd);
        }
    }

    @Override
    public void call(MessageCreateEvent event, Arguments args) throws CmdError
    {
        String commandName = args.peekPrev();

        CogFunc c = commands.get(commandName);

        if(c == null)
        {
            throw new CmdNotFoundError(commandName);
        }

        c.cmd.accept(event, args);
    }

    @Override
    public String[] getNames()
    {
        return commands.keySet().toArray(new String[0]);
    }
}
