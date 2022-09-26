package org.bdd.cafeyike.commander;

import java.util.function.Consumer;
import org.javacord.api.interaction.SlashCommandInteraction;

public class Cmd
{
    public interface Func extends Consumer<SlashCommandInteraction>
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
