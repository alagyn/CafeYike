package org.bdd.cafeyike.commands;

import org.bdd.cafeyike.commander.Arguments;
import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.commands.Cog;
import org.javacord.api.event.message.MessageCreateEvent;

public class Admin extends Cog
{
    public Admin()
    {
        super("Admin", "Admin Commands");

        addCommand(new CmdFunc(new String[] {"logout", "lo"}, this::logout, "logout", "", false, true));
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
        // Pass
    }
}
