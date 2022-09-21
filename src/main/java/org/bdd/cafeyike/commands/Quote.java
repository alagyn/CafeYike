package org.bdd.cafeyike.commands;

import org.bdd.cafeyike.commander.Arguments;
import org.bdd.cafeyike.commander.commands.Cog;
import org.bdd.cafeyike.commander.exceptions.ArgumentError;
import org.bdd.cafeyike.commander.exceptions.UsageError;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

public class Quote extends Cog
{
    public Quote()
    {
        super("Quotes", "User Quote Management");

        addCommand(new CmdFunc(
            new String[] {"quote", "q"}, this::addQuote, "Add a quote for a user", "[user] [message...]"));
    }

    public void addQuote(MessageCreateEvent event, Arguments args)
    {
        User user;
        try
        {
            user = args.nextUser();
        }
        catch(ArgumentError e)
        {
            throw new UsageError("yike(): Cannot parse user");
        }

        String content = args.remainder();
    }

    @Override
    public void shutdown()
    {
        // XXX Auto-generated method stub
    }
}
