package org.bdd.cafeyike.commands.music;

import java.util.Iterator;
import java.util.LinkedList;

import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.audio.AudioSourceBase;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.callback.InteractionMessageBuilderBase;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

public class MusicPlayer extends AudioSourceBase implements AudioLoadResultHandler, AudioEventListener
{

    private final AudioPlayer player;
    private AudioFrame lastFrame;
    private Message nowPlayingMsg;
    private final long channelId;

    private LinkedList<AudioTrack> trackQueue;
    private Iterator<AudioTrack> trackIter;
    private AudioTrack currentTrack;

    public MusicPlayer(DiscordApi api, AudioPlayer audioPlayer, Message msg, long channelId)
    {
        super(api);
        this.player = audioPlayer;
        this.nowPlayingMsg = msg;
        this.channelId = channelId;

        trackQueue = new LinkedList<>();
        trackIter = null;
        currentTrack = null;
    }

    public synchronized void makeNewNowPlaying(SlashCommandInteraction event)
    {
        Message newMsg = event.createFollowupMessageBuilder()
                .addEmbed(new EmbedBuilder().addField("Added to queue", "todo").addField("Now Playing", "todo")).send()
                .join();

        nowPlayingMsg.delete();
        nowPlayingMsg = newMsg;
    }

    public void addControls(InteractionMessageBuilderBase builder)
    {
        // builder.addComponents(ActionRow.of(Button.primary("m-prev", emoji)))
    }

    @Override
    public synchronized byte[] getNextFrame()
    {
        if(lastFrame == null)
        {
            return null;
        }
        return applyTransformers(lastFrame.getData());
    }

    @Override
    public synchronized boolean hasNextFrame()
    {
        lastFrame = player.provide();
        return lastFrame != null;
    }

    @Override
    public synchronized AudioSource copy()
    {
        return new MusicPlayer(getApi(), player, nowPlayingMsg, channelId);
    }

    @Override
    public synchronized void trackLoaded(AudioTrack track)
    {
        trackQueue.add(track);
        // TODO message?
        nowPlayingMsg.edit(new EmbedBuilder().addField("Now Playing", track.getInfo().title));

        // TODO buttons?
    }

    @Override
    public synchronized void playlistLoaded(AudioPlaylist playlist)
    {
        for(AudioTrack track : playlist.getTracks())
        {
            trackQueue.add(track);
            // TODO message?
        }
    }

    @Override
    public synchronized void noMatches()
    {
        throw new CmdError("No matches found");
    }

    @Override
    public synchronized void loadFailed(FriendlyException throwable)
    {
        // Notify the user that everything exploded
    }

    @Override
    public synchronized void onEvent(AudioEvent arg0)
    {
        // XXX Auto-generated method stub

    }
}
