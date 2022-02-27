package org.bdd.javacordCmd.commands;

import org.bdd.javacordCmd.Arguments;
import org.bdd.javacordCmd.exceptions.CmdError;
import org.bdd.javacordCmd.exceptions.CmdNotFoundError;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.HashMap;
import java.util.function.BiConsumer;

public abstract class Cog implements Command
{
    public interface FuncRef extends BiConsumer<MessageCreateEvent, Arguments>
    {

    }

    public static record Func(String[] aliases, FuncRef cmd)
    {
        public Func
        {
            if(aliases.length == 0)
            {
                throw new CmdError("No command name given");
            }
        }
    }

    public final String cogName;

    public Cog(String name)
    {
        this.cogName = name;
    }

    private final HashMap<String, Func> commands = new HashMap<>();

    public void addCommand(Func cmd)
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

        Func c = commands.get(commandName);

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
