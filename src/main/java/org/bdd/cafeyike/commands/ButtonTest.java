package org.bdd.cafeyike.commands;

import java.util.concurrent.TimeUnit;
import org.bdd.cafeyike.commander.Arguments;
import org.bdd.cafeyike.commander.commands.Cog;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.ButtonInteraction;

public class ButtonTest extends Cog
{
    public ButtonTest()
    {
        super("BtnTest", "Button Tests");

        addCommand(new CmdFunc(new String[] {"test"}, this::btnTest));
    }

    public void btnTest(MessageCreateEvent event, Arguments args)
    {
        Button[] b = new Button[5];

        for(int i = 0; i < b.length; ++i)
        {
            b[i] = Button.secondary("" + i, "" + i);
        }

        Message m = new MessageBuilder()
                        .setContent("This is content")
                        .addComponents(ActionRow.of(b))
                        .send(event.getChannel())
                        .join();

        m.addButtonClickListener(event1 -> {
             ButtonInteraction interaction = event1.getButtonInteraction();

             String id = interaction.getCustomId();

             if(interaction.getUser().equals(event.getMessageAuthor().asUser().orElse(null)))
             {
                 interaction.createImmediateResponder().setContent("You pressed: " + id).respond();
             }
             else
             {
                 interaction.createImmediateResponder().setContent("You aren't allow to press " + id).respond();
             }
         }).removeAfter(5, TimeUnit.MINUTES);
    }

    @Override
    public void shutdown()
    {
        // Pass
    }
}
