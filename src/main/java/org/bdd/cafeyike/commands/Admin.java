package org.bdd.cafeyike.commands;

import java.util.List;
import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.Cog;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

public class Admin extends Cog
{
    public Admin()
    {
    }

    public void logout(SlashCommandInteraction interaction)
    {
        interaction.createImmediateResponder()
            .addEmbed(new EmbedBuilder().addField("Done", "Done"))
            .setFlags(MessageFlag.EPHEMERAL)
            .respond()
            .join();
        if(interaction.getUser().isBotOwner())
        {
            Bot.inst.shutdown();
        }
    }

    @Override
    public void shutdown()
    {
        // Pass
    }

    @Override
    public List<SlashCommandBuilder> buildCommands()
    {
        return null;
    }
}
