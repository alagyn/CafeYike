package org.bdd.cafeyike.commands;

import org.bdd.javacordCmd.Arguments;
import org.bdd.javacordCmd.Bot;
import org.bdd.javacordCmd.commands.Cog;
import org.javacord.api.event.message.MessageCreateEvent;

public class Admin extends Cog
{
    public Admin()
    {
        super("Admin");

        addCommand(new CogFunc(
                new String[]{"logout", "lo"},
                this::logout
        ));
    }

    public void logout(MessageCreateEvent event, Arguments args)
    {
        if(event.getMessageAuthor().isBotOwner())
        {
            Bot.inst.shutdown();
        }
    }

    @Override
    public void shutdown()
    {

    }
}
