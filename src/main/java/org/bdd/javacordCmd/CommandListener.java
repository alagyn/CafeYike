package org.bdd.javacordCmd;

import org.bdd.javacordCmd.commands.Command;
import org.bdd.javacordCmd.exceptions.CmdError;
import org.bdd.javacordCmd.commands.SubCommandGroup;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;


public class CommandListener implements MessageCreateListener
{
    public final String prefix;
    private final SubCommandGroup commands;

    public CommandListener(String prefix) throws CmdError
    {
        this.prefix = prefix;
        commands = new SubCommandGroup(new String[] {"MAIN"});
    }

    public void addCommand(Command c) throws CmdError
    {
        commands.addCommand(c);
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event)
    {
        //Ignore messages from ourselves
        if(event.getMessageAuthor().isYourself())
        {
            return;
        }

        String content = event.getMessageContent();

        if(content.startsWith(prefix))
        {
            try
            {
                commands.call(event, new Arguments(content.substring(1)));
            }
            catch(CmdError e)
            {
                //TODO better message?
                event.getChannel().sendMessage(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void shutdown()
    {
        commands.shutdown();
    }
}
