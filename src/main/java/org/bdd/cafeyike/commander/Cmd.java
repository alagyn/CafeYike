package org.bdd.cafeyike.commander;

import java.util.function.Consumer;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class Cmd
{
    public interface Func extends Consumer<SlashCommandInteractionEvent>
    {
    }

    String name;
    Func func;

    public Cmd(Func func, String name)
    {
        this.name = name;
        this.func = func;
    }
}
