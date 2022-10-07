package org.bdd.cafeyike.commands.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.audio.AudioSourceBase;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import com.sedmelluq.discord.lavaplayer.player.event.PlayerPauseEvent;
import com.sedmelluq.discord.lavaplayer.player.event.PlayerResumeEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackExceptionEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackStuckEvent;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusicPlayer extends AudioSourceBase implements AudioEventListener
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    public final AudioPlayer player;
    private AudioFrame lastFrame;
    public Message nowPlayingMsg;
    private final ServerVoiceChannel voiceChannel;
    private final TextChannel textChannel;
    private long serverId;

    private boolean endOfQueue;
    private boolean looping;

    private ArrayList<AudioTrack> trackQueue;
    private int curIdx;

    private AudioTrack currentTrack;

    public MusicPlayer(DiscordApi api, AudioPlayer audioPlayer, Message msg, ServerVoiceChannel voiceChannel,
            TextChannel textChannel, long serverId)
    {
        super(api);
        this.player = audioPlayer;
        this.nowPlayingMsg = msg;
        this.voiceChannel = voiceChannel;
        this.textChannel = textChannel;
        trackQueue = new ArrayList<>();
        curIdx = -1;

        currentTrack = null;
        endOfQueue = true;
        looping = false;

        this.serverId = serverId;

        player.addListener(this);
    }

    public void stop()
    {
        player.stopTrack();
    }

    public AudioTrack current()
    {
        return currentTrack.makeClone();
    }

    public synchronized void addToQueue(AudioTrack track)
    {
        trackQueue.add(track);
    }

    public synchronized void playIdx(int idx)
    {
        if(idx < 0 || idx >= trackQueue.size())
        {
            throw new CmdError("Index out of bounds");
        }

        curIdx = idx - 1;
        startNext();
    }

    public synchronized boolean endOfQueue()
    {
        return endOfQueue;
    }

    public synchronized int queueLen()
    {
        return trackQueue.size();
    }

    public synchronized void setLooping(boolean l)
    {
        this.looping = l;
    }

    public synchronized boolean isLooping()
    {
        return this.looping;
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
    public boolean hasFinished()
    {
        return false;
    }

    @Override
    public synchronized AudioSource copy()
    {
        return new MusicPlayer(getApi(), player, nowPlayingMsg, voiceChannel, textChannel, serverId);
    }

    public ActionRow getButtons()
    {
        return ActionRow.of(Button.secondary(Bot.makeId(Music.PREV_BTN, serverId), null, "⏮"),
                Button.secondary(Bot.makeId(Music.PLAY_BTN, serverId), null, "⏯"),
                Button.secondary(Bot.makeId(Music.NEXT_BTN, serverId), null, "⏭"),
                Button.secondary(Bot.makeId(Music.SHUF_BTN, serverId), null, "🔀"),
                Button.secondary(Bot.makeId(Music.LOOP_BTN, serverId), null, "🔁"));
    }

    private String embedTitle()
    {
        StringBuilder out = new StringBuilder();
        if(player.isPaused())
        {
            out.append("PAUSED: ");
        }
        else
        {
            out.append("Now Playing: ");
        }

        out.append(curIdx + 1).append("/").append(trackQueue.size());

        if(looping)
        {
            out.append(" LOOPING");
        }

        return out.toString();
    }

    public synchronized void makeNewNowPlaying()
    {
        Message newMsg = new MessageBuilder()
                .addEmbed(new EmbedBuilder().addField(embedTitle(), currentTrack.getInfo().title))
                .addComponents(getButtons()).send(textChannel).join();

        nowPlayingMsg.delete();
        nowPlayingMsg = newMsg;
    }

    public synchronized void setEndOfQueue()
    {
        endOfQueue = true;
        nowPlayingMsg.edit(new EmbedBuilder().addField(embedTitle(), "End of Queue"));
    }

    public synchronized void startNext()
    {
        endOfQueue = false;

        if(curIdx < trackQueue.size() - 1)
        {
            // Preincrement
            currentTrack = trackQueue.get(++curIdx);
            log.trace("Starting Next: {}", currentTrack.getIdentifier());
            player.playTrack(currentTrack.makeClone());
            makeNewNowPlaying();
        }
        else
        {
            log.trace("End of queue");
            player.setPaused(true);
            setEndOfQueue();
        }
    }

    public synchronized void startPrev()
    {
        endOfQueue = false;

        if(curIdx > 0)
        {
            // predecrement
            currentTrack = trackQueue.get(--curIdx);
            log.debug("Starting Prev");
            player.playTrack(currentTrack.makeClone());
            makeNewNowPlaying();
        }
        else
        {
            player.setPaused(true);
            setEndOfQueue();
        }
    }

    public synchronized void shuffle()
    {
        Random rand = new Random();
        for(int i = curIdx + 1; i < queueLen(); ++i)
        {
            int randIdx = rand.nextInt(queueLen() - i - 1) + i;
            Collections.swap(trackQueue, i, randIdx);
        }
    }

    /**
     * @param player Audio player
     */
    public void onPlayerPause(AudioPlayer player)
    {
        log.trace("Player paused");
        // change message from now playing to paused?
    }

    /**
     * @param player Audio player
     */
    public void onPlayerResume(AudioPlayer player)
    {
        log.trace("Player resumed");
        // TODO
    }

    /**
     * @param player Audio player
     * @param track  Audio track that started
     */
    public void onTrackStart(AudioPlayer player, AudioTrack track)
    {
        log.trace("onTrackStart()");
        // noop?
    }

    /**
     * @param player    Audio player
     * @param track     Audio track that ended
     * @param endReason The reason why the track stopped playing
     */
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason)
    {
        log.trace("onTrackEnd() {}", endReason.toString());
        if(endReason == AudioTrackEndReason.REPLACED)
        {
            return;
        }

        if(!looping && endReason.mayStartNext)
        {
            startNext();
        }
    }

    /**
     * @param player    Audio player
     * @param track     Audio track where the exception occurred
     * @param exception The exception that occurred
     */
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception)
    {
        // Pause and wait for a continue button?
        log.error("MusicPlayer::onTrackException(): {} : {} ", track.getIdentifier(), exception.getMessage());
        player.playTrack(track);
    }

    /**
     * @param player      Audio player
     * @param track       Audio track where the exception occurred
     * @param thresholdMs The wait threshold that was exceeded for this event to
     *                    trigger
     */
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs)
    {
        // ^^^^?
        log.error("MusicPlayer::onTrackStuck(): {} : stuck", track.getIdentifier());
    }

    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs, StackTraceElement[] stackTrace)
    {
        onTrackStuck(player, track, thresholdMs);
    }

    @Override
    public void onEvent(AudioEvent event)
    {
        if(event instanceof PlayerPauseEvent)
        {
            onPlayerPause(event.player);
        }
        else if(event instanceof PlayerResumeEvent)
        {
            onPlayerResume(event.player);
        }
        else if(event instanceof TrackStartEvent)
        {
            onTrackStart(event.player, ((TrackStartEvent) event).track);
        }
        else if(event instanceof TrackEndEvent)
        {
            onTrackEnd(event.player, ((TrackEndEvent) event).track, ((TrackEndEvent) event).endReason);
        }
        else if(event instanceof TrackExceptionEvent)
        {
            onTrackException(event.player, ((TrackExceptionEvent) event).track,
                    ((TrackExceptionEvent) event).exception);
        }
        else if(event instanceof TrackStuckEvent)
        {
            TrackStuckEvent stuck = (TrackStuckEvent) event;
            onTrackStuck(event.player, stuck.track, stuck.thresholdMs, stuck.stackTrace);
        }
    }
}
