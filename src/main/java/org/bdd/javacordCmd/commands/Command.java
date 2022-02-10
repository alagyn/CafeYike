package org.bdd.javacordCmd.commands;

import org.bdd.javacordCmd.Arguments;
import org.bdd.javacordCmd.exceptions.CmdError;
import org.javacord.api.event.message.MessageCreateEvent;

public interface Command
{
    void call(MessageCreateEvent event, Arguments args) throws CmdError;
    String[] getNames();
}
