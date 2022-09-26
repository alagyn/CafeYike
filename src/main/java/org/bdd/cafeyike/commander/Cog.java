package org.bdd.cafeyike.commander;

import java.util.LinkedList;
import java.util.List;
import org.javacord.api.interaction.SlashCommandBuilder;

/**
 * Contains one or more internal commands
 * Used to group commands into a single file
 */
public abstract class Cog
{
    private LinkedList<Cmd> commands = new LinkedList<>();

    public abstract List<SlashCommandBuilder> buildCommands();

    public void registerCmdFunc(Cmd.Func func, String name)
    {
        commands.add(new Cmd(func, name));
    }

    public List<Cmd> getCommands()
    {
        return commands;
    }

    public abstract void shutdown();
}
