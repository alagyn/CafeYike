package org.bdd.cafeyike.commands.music;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.Cog;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;

public class Music extends Cog
{

    private AudioPlayerManager playerManager;

    private HashMap<Long, AudioConnection> connections;

    public Music()
    {
        playerManager = new DefaultAudioPlayerManager();
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());

        connections = new HashMap<>();
    }

    @Override
    public List<SlashCommandBuilder> buildCommands()
    {
        LinkedList<SlashCommandBuilder> out = new LinkedList<>();

        out.add(SlashCommand.with("play", "Start/Queue a video to play").addOption(SlashCommandOption
                .create(SlashCommandOptionType.STRING, "query", "The search query or url to play", true)));

        registerCmdFunc(this::playCmd, "play");

        return out;
    }

    private void playCmd(SlashCommandInteraction event)
    {
        User u = event.getUser();
        Server serv = event.getServer().orElse(null);

        if(serv == null)
        {
            Bot.sendError(event, "Not in a server");
        }

        ServerVoiceChannel channel = u.getConnectedVoiceChannel(serv).orElse(null);

        if(channel == null)
        {
            Bot.sendError(event, "Not in a voice chat");
        }

        String query = event.getOptionStringValueByIndex(0).orElse("");

        if(query.isEmpty())
        {
            // Empty query here is error
            // pause/resume/etc will be buttons
            Bot.sendError(event, "Empty query");
        }

        event.respondLater();

        AudioConnection ac = connections.get(channel.getId());

        if(ac == null)
        {
            ac = channel.connect().join();
            AudioPlayer player = playerManager.createPlayer();
            // TODO add control buttons here?
            Message m = event.createFollowupMessageBuilder()
                    .addEmbed(new EmbedBuilder().addField("Now Playing", "Loading...")).send().join();
            // Create an audio source and add it to the audio connection's queue
            AudioSource source = new MusicPlayer(Bot.inst.getApi(), player, m, channel.getId());
            ac.setAudioSource(source);

            connections.put(channel.getId(), ac);
        }

        try
        {
            playerManager.loadItem(query, (MusicPlayer) ac.getAudioSource().get());
        }
        catch(CmdError e)
        {
            Bot.sendFollowError(event, e.getMessage());
        }
    }

}
