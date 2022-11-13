package org.bdd.cafeyike.commands.music;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.exceptions.CmdError;

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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.AudioManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusicPlayer implements AudioEventListener, AudioSendHandler
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static class LeaveThread implements Runnable
    {
        public final int sleepTime;
        public final AudioManager manager;

        public LeaveThread(int sleepTimeMillis, AudioManager manager)
        {
            this.sleepTime = sleepTimeMillis;
            this.manager = manager;
        }

        @Override
        public void run()
        {
            try
            {
                Thread.sleep(sleepTime);
                Music.leave(manager);
            }
            catch(InterruptedException err)
            {
                //pass
            }
        }

    }

    public final AudioPlayer player;
    private AudioFrame lastFrame;
    private AudioManager manager;
    public Message nowPlayingMsg;
    private final MessageChannel textChannel;
    private long serverId;
    private Thread leaveThread;

    private boolean endOfQueue;
    private boolean looping;

    private ArrayList<AudioTrack> trackQueue;
    private int curIdx;

    private AudioTrack currentTrack;
    private final int leaveTimeMillis;

    public MusicPlayer(AudioManager manager, AudioPlayer audioPlayer, Message msg, MessageChannel textChannel,
            long serverId, int leaveTimeMillis)
    {
        this.manager = manager;
        this.player = audioPlayer;
        this.nowPlayingMsg = msg;
        this.textChannel = textChannel;
        this.leaveTimeMillis = leaveTimeMillis;
        leaveThread = null;
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

    public ActionRow getButtons()
    {
        return ActionRow.of(Button.secondary(Bot.makeId(Music.PREV_BTN, serverId), Emoji.fromUnicode("⏮")),
                Button.secondary(Bot.makeId(Music.PLAY_BTN, serverId), Emoji.fromUnicode("⏯")),
                Button.secondary(Bot.makeId(Music.NEXT_BTN, serverId), Emoji.fromUnicode("⏭")),
                Button.secondary(Bot.makeId(Music.SHUF_BTN, serverId), Emoji.fromUnicode("🔀")),
                Button.secondary(Bot.makeId(Music.LOOP_BTN, serverId), Emoji.fromUnicode("🔁")));
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
        nowPlayingMsg.delete().queue();

        nowPlayingMsg = textChannel
                .sendMessageEmbeds(
                        new EmbedBuilder().setTitle(embedTitle()).setDescription(currentTrack.getInfo().title).build())
                .addComponents(getButtons()).complete();
    }

    public synchronized void setEndOfQueue()
    {
        endOfQueue = true;
        nowPlayingMsg
                .editMessageEmbeds(new EmbedBuilder().setTitle(embedTitle()).setDescription("End of Queue").build());
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
        else if(!trackQueue.isEmpty())
        {
            player.playTrack(trackQueue.get(0));
            player.setPaused(false);
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
        leaveThread = new Thread(new MusicPlayer.LeaveThread(leaveTimeMillis, manager));
        leaveThread.start();
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
        if(player.isPaused())
        {
            player.setPaused(false);
        }
        if(leaveThread != null)
        {
            leaveThread.interrupt();
            leaveThread = null;
        }
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

        if(endReason.mayStartNext)
        {
            if(looping)
            {
                player.playTrack(track.makeClone());
            }
            else
            {
                startNext();
            }
        }
        else
        {
            setEndOfQueue();
            player.setPaused(true);
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
        player.playTrack(track.makeClone());
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
        player.playTrack(track.makeClone());
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

    @Override
    public boolean canProvide()
    {
        lastFrame = player.provide();
        return lastFrame != null;
    }

    @Override
    public ByteBuffer provide20MsAudio()
    {
        return ByteBuffer.wrap(lastFrame.getData());
    }

    @Override
    public boolean isOpus()
    {
        return true;
    }
}
