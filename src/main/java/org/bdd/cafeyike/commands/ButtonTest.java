package org.bdd.cafeyike.commands;

import java.util.LinkedList;
import java.util.List;
import org.bdd.cafeyike.commander.Cog;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.TextInput;
import org.javacord.api.entity.message.component.TextInputStyle;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.interaction.ModalSubmitEvent;
import org.javacord.api.interaction.ModalInteraction;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.ModalSubmitListener;

public class ButtonTest extends Cog
{
    public ButtonTest()
    {
        // addCommand(new CmdFunc("test", this::btnTest, "test func"));
    }

    public void btnTest(SlashCommandInteraction event)
    {
        event.respondWithModal("test-modal", "this is the thing\nwhat pt2\nok then",
                ActionRow.of(TextInput.create(TextInputStyle.SHORT, "newQuote", "Edit\nwhats all this then")),
                ActionRow.of(TextInput.create(TextInputStyle.SHORT, "asdf", "potat then")));
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

        out.add(SlashCommand.with("test", "test test test test"));

        registerCmdFunc(this::btnTest, "test");

        return out;
    }

    @Override
    public void registerListeners(DiscordApi api)
    {
        api.addModalSubmitListener(new ModalSubmitListener()
        {

            @Override
            public void onModalSubmit(ModalSubmitEvent _event)
            {
                ModalInteraction event = _event.getModalInteraction();
                if(event.getCustomId().equals("test-modal"))
                {
                    event.createImmediateResponder().addEmbed(new EmbedBuilder().addField("Whatever", "asdfasdf"))
                            .setFlags(MessageFlag.EPHEMERAL).respond();
                }
            }

        });
    }
}
