package org.bdd.cafeyike.commander.commands;

import org.bdd.cafeyike.commander.Arguments;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.javacord.api.event.interaction.InteractionCreateEvent;
;

public abstract class Command
{
    public final boolean botOwnerOnly;
    public final boolean adminOnly;

    public Command(boolean botOwnerOnly, boolean adminOnly)
    {
        this.botOwnerOnly = botOwnerOnly;
        this.adminOnly = adminOnly;
    }

    public Command()
    {
        this(false, false);
    }

    public abstract void call(InteractionCreateEvent event, Arguments args) throws CmdError;

    public abstract String getHelp(boolean showAdmin, boolean showBotOwner);
    public abstract String getUsage();

    /**
     * Returns a list of aliases for this command
     */
    public abstract String[] getAliases();
}
