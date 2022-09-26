package org.bdd.cafeyike.commands;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.bdd.cafeyike.commander.Cog;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

public class ButtonTest extends Cog
{
    public ButtonTest()
    {
        //addCommand(new CmdFunc("test", this::btnTest, "test func"));
    }

    public void btnTest(SlashCommandInteraction event)
    {
        Button[] b = new Button[5];

        for(int i = 0; i < b.length; ++i)
        {
            b[i] = Button.secondary("" + i, "" + i);
        }

        event.createImmediateResponder().respond();

        Message m = new MessageBuilder()
                        .setContent("This is content")
                        .addComponents(ActionRow.of(b))
                        .send(event.getChannel().get())
                        .join();

        m.addButtonClickListener(event1 -> {
             ButtonInteraction interaction = event1.getButtonInteraction();

             String id = interaction.getCustomId();

             if(interaction.getUser().equals(event.getUser()))
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

    @Override
    public List<SlashCommandBuilder> buildCommands()
    {
        LinkedList<SlashCommandBuilder> out = new LinkedList<>();

        return out;
    }
}
