package org.bdd.cafeyike.commander;

import java.util.function.BiConsumer;

import org.bdd.cafeyike.commander.exceptions.CmdError;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class Btn
{
    public interface Func extends BiConsumer<ButtonInteractionEvent, String>
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
