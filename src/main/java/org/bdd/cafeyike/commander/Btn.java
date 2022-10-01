package org.bdd.cafeyike.commander;

import java.util.function.BiConsumer;

import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.javacord.api.interaction.ButtonInteraction;

public class Btn
{
    public interface Func extends BiConsumer<ButtonInteraction, String>
    {
    }

    String prefix;
    Func func;

    public Btn(Func func, String prefix)
    {
        if(prefix.contains(":"))
        {
            throw new CmdError("Button prefix cannot contain \":\"");
        }

        this.prefix = prefix;
        this.func = func;
    }
}
