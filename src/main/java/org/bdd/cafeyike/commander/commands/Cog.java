package org.bdd.cafeyike.commander.commands;

import java.util.LinkedList;
import java.util.function.BiConsumer;
import org.bdd.cafeyike.commander.Arguments;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.javacord.api.event.message.MessageCreateEvent;

/**
 * Contains one or more internal commands
 * Used to group commands into a single file
 */
public abstract class Cog
{
    public interface FuncRef extends BiConsumer<MessageCreateEvent, Arguments>
    {
    }

    /**
     * Makes a single function become a command
     */
    public class CmdFunc extends Command
    {
        String[] aliases;
        FuncRef cmd;

        String descr, usage;

        boolean adminOnly;
        boolean botOwnerOnly;

        public CmdFunc(
            String[] aliases, FuncRef cmd, String descr, String usage, boolean adminOnly, boolean botOwnerOnly)
        {
            super(botOwnerOnly, adminOnly);
            if(aliases.length == 0)
            {
                throw new CmdError("No command name given");
            }

            this.aliases = aliases;
            this.cmd = cmd;
            this.descr = descr;
            this.usage = usage;
            this.adminOnly = adminOnly;
            this.botOwnerOnly = botOwnerOnly;
        }

        public CmdFunc(String[] aliases, FuncRef cmd, String descr, String usage, boolean adminOnly)
        {
            this(aliases, cmd, descr, usage, adminOnly, false);
        }

        public CmdFunc(String[] aliases, FuncRef cmd, String descr, String usage)
        {
            this(aliases, cmd, descr, usage, false, false);
        }

        public CmdFunc(String[] aliases, FuncRef cmd)
        {
            this(aliases, cmd, aliases[0], aliases[0], false, false);
        }

        @Override
        public void call(MessageCreateEvent event, Arguments args) throws CmdError
        {
            cmd.accept(event, args);
        }

        @Override
        public void shutdown()
        {
            // Pass?
        }

        @Override
        public String getUsage()
        {
            return usage;
        }

        @Override
        public String getHelp(boolean showAdmin, boolean showBotOwner)
        {
            StringBuilder out = new StringBuilder();
            out.append(aliases[0]).append(": ").append(descr);
            return out.toString();
        }

        @Override
        public String[] getAliases()
        {
            return aliases;
        }
    }

    public final String cogName;
    public final String cogBrief;
    public final LinkedList<Command> commands;

    public Cog(String name, String brief)
    {
        this.cogName = name;
        this.cogBrief = brief;
        this.commands = new LinkedList<>();
    }

    public void addCommand(CmdFunc cmd)
    {
        commands.add(cmd);
    }

    public Iterable<Command> getCommands()
    {
        return commands;
    }

    /*
    @Override
    public final String getHelp(boolean showAdmin, boolean showBotOwner)
    {
        StringBuilder cmdOut = new StringBuilder();
        for(String name : defaultNames)
        {
            Func f = commands.get(name);
            if((!showAdmin && f.adminOnly) || (!showBotOwner && f.botOwnerOnly))
            {
                continue;
            }

            cmdOut.append("\t").append(name).append(": ");
            cmdOut.append(f.getDescr()).append("\n");
        }

        if(cmdOut.length() == 0)
        {
            return "";
        }

        StringBuilder out = new StringBuilder();

        out.append(cogName).append(": ").append(cogBrief).append("\n");
        out.append(cmdOut);

        return out.toString();
    }
    */

    public abstract void shutdown();
}