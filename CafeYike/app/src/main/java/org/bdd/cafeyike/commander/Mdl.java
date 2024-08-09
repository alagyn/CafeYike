package org.bdd.cafeyike.commander;

import java.util.function.BiConsumer;

import org.bdd.cafeyike.commander.exceptions.CmdError;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public class Mdl
{
    public interface Func extends BiConsumer<ModalInteractionEvent, String>
    {
    }

    String prefix;
    Func func;

    public Mdl(Func func, String prefix)
    {
        if(prefix.contains(":"))
        {
            throw new CmdError("Modal prefix cannot contain \":\"");
        }
        this.prefix = prefix;
        this.func = func;
    }
}
