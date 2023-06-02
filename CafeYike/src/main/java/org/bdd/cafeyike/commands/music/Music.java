package org.bdd.cafeyike.commands.music;

import java.security.cert.PKIXRevocationChecker.Option;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bdd.cafeyike.CafeConfig;
import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.Cog;
import org.bdd.cafeyike.commander.utils.DoAfter;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.AudioManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Music extends Cog
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int SELECT_TIMEOUT = 30 * 1000;

    private class QueryResult implements Runnable
    {

        public final AudioPlaylist playlist;
        public final InteractionHook hook;
        public Thread waitThread;
        public final MusicPlayer player;
        public final long guildID;
        public final Message selectMessage;

        public QueryResult(AudioPlaylist playlist, InteractionHook hook, MusicPlayer player, long guildID,
                Message selectMessage)
        {
            this.playlist = playlist;
            this.hook = hook;
            this.player = player;
            this.waitThread = null;
            this.guildID = guildID;
            this.selectMessage = selectMessage;
        }

        public void startThread()
        {
            waitThread = new Thread(this);
            waitThread.start();
        }

        public void stopThread()
        {
            if(waitThread.isAlive())
            {
                waitThread.interrupt();
            }
        }

        @Override
        public void run()
        {
            try
            {
                Thread.sleep(SELECT_TIMEOUT);
            }
            catch(InterruptedException err)
            {
                // PASS
            }

            queryMap.remove(guildID);
            if(selectMessage != null)
            {
                selectMessage.delete().queue();
            }
        }
    }

    private HashMap<Long, QueryResult> queryMap = new HashMap<>();

    private AudioPlayerManager playerManager;
    public final int leaveTimeMillis;

    public static final String PREV_BTN = "prev";
    public static final String NEXT_BTN = "next";
    public static final String PLAY_BTN = "play";
    public static final String SHUF_BTN = "shuf";
    public static final String LOOP_BTN = "loop";
    public static final String SELECT_BTN = "sel";

    public static final String URL_RE_STR = "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\\\".,<>?«»“”‘’]))";

    public static final String SEEK_RE_STR = "(?<delta>[+-])?((?<min>\\d*):(?<sec>\\d{2})|(?<allsec>\\d+))";

    public static final Pattern URL_REGEX = Pattern.compile(URL_RE_STR);
    public static final Pattern SEEK_REGEX = Pattern.compile(SEEK_RE_STR);

    public Music(Bot bot)
    {
        super(bot);
        leaveTimeMillis = CafeConfig.getIntConfig("musicLeaveTimeSec") * 1000;
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event)
    {
        AudioChannelUnion channel = event.getChannelLeft();
        if(channel == null)
        {
            // User did not leave a channel, ignore
            return;
        }

        Guild serv = event.getGuild();
        AudioManager am = serv.getAudioManager();
        if(!am.isConnected())
        {
            // Not connected to VC, ignore
            return;
        }

        AudioChannelUnion playerChannel = am.getConnectedChannel();

        if(playerChannel == null || !playerChannel.equals(channel))
        {
            // User left a different channel, ignore
            return;
        }

        VoiceChannel vc = channel.asVoiceChannel();

        if(vc.getMembers().size() != 1)
        {
            // There are other users in the VC, ignore
            return;
        }

        MusicPlayer player = (MusicPlayer) am.getSendingHandler();
        log.trace("Empty VC, starting leave thread");
        player.startLeaveThread();
    }

    @Override
    public List<CommandData> buildCommands()
    {
        LinkedList<CommandData> out = new LinkedList<>();

        out.add(Commands.slash("play", "Start/Queue a video to play").addOption(OptionType.STRING, "query",
                "The search query or url to play", true));

        registerCmdFunc(this::playCmd, "play");

        out.add(Commands.slash("leave", "Stop playing and leave the voice chat"));

        registerCmdFunc(this::leaveCmd, "leave");

        out.add(Commands.slash("time", "Get time info on the current playing track"));

        registerCmdFunc(this::timeCmd, "time");

        out.add(Commands.slash("seek", "Seek controls for the current track").addOption(OptionType.STRING, "time",
                "Formats: [+/-][min:]sec. Using without the +/- seeks to the absolute time", true));

        registerCmdFunc(this::seekCmd, "seek");

        registerBtnFunc(this::nextBtn, NEXT_BTN);
        registerBtnFunc(this::prevBtn, PREV_BTN);
        registerBtnFunc(this::playBtn, PLAY_BTN);
        registerBtnFunc(this::loopBtn, LOOP_BTN);
        registerBtnFunc(this::shuffleBtn, SHUF_BTN);
        registerBtnFunc(this::selectBtn, SELECT_BTN);

        return out;
    }

    @Override
    public void registerListeners(JDA api)
    {
        api.addEventListener(this);
    }

    public static void leave(AudioManager am)
    {
        MusicPlayer mp = (MusicPlayer) am.getSendingHandler();
        mp.nowPlayingMsg.delete();
        mp.stop();
        am.closeAudioConnection();
    }

    private void leaveCmd(SlashCommandInteractionEvent event)
    {
        Guild serv = event.getGuild();
        if(serv == null)
        {
            sendError(event, "Not in a server");
        }

        AudioManager am = serv.getAudioManager();

        if(am.isConnected())
        {
            leave(am);
            InteractionHook hook = event.getHook();
            hook.sendMessageEmbeds(new EmbedBuilder().setTitle("Music").setDescription("Goodbye").build()).complete();
            new DoAfter(60, x ->
            {
                hook.deleteOriginal();
            });
        }
        else
        {
            event.replyEmbeds(new EmbedBuilder().setTitle("Error").setDescription("Nothing to do").build())
                    .setEphemeral(true).queue();
        }
    }

    private void playCmd(SlashCommandInteractionEvent event)
    {
        Member u = event.getMember();
        Guild serv = event.getGuild();

        InteractionHook hook = event.getHook();

        if(serv == null)
        {
            sendError(hook, "Not in a server");
        }

        VoiceChannel voiceChannel = null;
        try
        {
            voiceChannel = u.getVoiceState().getChannel().asVoiceChannel();
        }
        catch(NullPointerException e)
        {
            // pass
        }

        MessageChannel textChannel = event.getChannel();

        if(voiceChannel == null)
        {
            sendError(hook, "Not in a voice chat");
        }

        if(textChannel == null)
        {
            sendError(hook, "Invalid text channel");
        }

        String query = event.getOption("query").getAsString();

        if(query.isEmpty())
        {
            // Empty query here is error
            // pause/resume/etc will be buttons
            sendError(hook, "Empty query");
        }

        Matcher matcher = URL_REGEX.matcher(query);

        boolean _isSearch = false;
        if(!matcher.matches())
        {
            // If it isn't a URL, make it a query
            query = "ytsearch:" + query;
            _isSearch = true;
        }

        AudioManager am = serv.getAudioManager();

        if(!am.isConnected())
        {
            Message m = hook
                    .sendMessageEmbeds(new EmbedBuilder().setTitle("Now Playing").setDescription("Loading...").build())
                    .complete();
            AudioPlayer player = playerManager.createPlayer();
            // Create an audio source and add it to the audio connection's queue
            MusicPlayer mp = new MusicPlayer(am, player, m, textChannel, serv.getIdLong(), leaveTimeMillis);
            am.setSendingHandler(mp);
            am.openAudioConnection(voiceChannel);
        }
        else if(!am.getConnectedChannel().asVoiceChannel().equals(voiceChannel))
        {
            // User is in a different VC
            sendError(hook, "Not in the same voice channel as bot");
        }

        // This has to be final to store the up-value
        final MusicPlayer musicPlayer = (MusicPlayer) am.getSendingHandler();
        final boolean isSearch = _isSearch;

        playerManager.loadItem(query, new AudioLoadResultHandler()
        {

            @Override
            public synchronized void trackLoaded(AudioTrack track)
            {
                log.trace("Loaded track: {}", track.getIdentifier());
                musicPlayer.addToQueue(track);

                if(musicPlayer.endOfQueue())
                {
                    musicPlayer.playIdx(musicPlayer.queueLen() - 1);
                }
                else
                {
                    hook.sendMessageEmbeds(
                            new EmbedBuilder().setTitle("Added to Queue").setDescription(track.getInfo().title).build())
                            .complete();
                    musicPlayer.makeNewNowPlaying();
                }
            }

            private void loadPlaylist(AudioPlaylist playlist)
            {
                int len = playlist.getTracks().size();
                StringBuilder text = new StringBuilder();
                text.append("Playlist: ").append(playlist.getName()).append("\n");

                text.append(len).append(" ").append(len == 1 ? "item." : "items.");

                int nextIdx = musicPlayer.queueLen();

                for(AudioTrack track : playlist.getTracks())
                {
                    musicPlayer.addToQueue(track);
                }

                if(musicPlayer.endOfQueue())
                {
                    musicPlayer.playIdx(nextIdx);
                }

                hook.sendMessageEmbeds(
                        new EmbedBuilder().setTitle("Added to Queue").setDescription(text.toString()).build())
                        .complete();
                musicPlayer.makeNewNowPlaying();
            }

            private void loadQuery(AudioPlaylist playlist)
            {
                long guildID = serv.getIdLong();

                StringBuilder text = new StringBuilder();
                int numResults = playlist.getTracks().size();
                int limit = numResults < 5 ? numResults : 5;

                Button[] btns = new Button[limit];

                for(int i = 0; i < limit; ++i)
                {
                    AudioTrack track = playlist.getTracks().get(i);
                    text.append(i + 1).append(") ").append(track.getInfo().title).append("\n");

                    StringBuilder idBuilder = new StringBuilder();
                    idBuilder.append(SELECT_BTN).append(":").append(guildID).append(":").append(i);
                    btns[i] = (Button.primary(idBuilder.toString(), "" + (i + 1)));
                }

                Message selectMessage = hook
                        .editOriginalEmbeds(
                                new EmbedBuilder().setTitle("Select Item:").setDescription(text.toString()).build())
                        .setComponents(ActionRow.of(btns)).complete();

                QueryResult q = new QueryResult(playlist, hook, musicPlayer, guildID, selectMessage);
                queryMap.put(guildID, q);

                q.startThread();
            }

            @Override
            public synchronized void playlistLoaded(AudioPlaylist playlist)
            {
                log.trace("playlistLoaded() isSearch {}", isSearch);

                try
                {
                    if(isSearch)
                    {
                        loadQuery(playlist);
                    }
                    else
                    {
                        loadPlaylist(playlist);
                    }
                }
                catch(Exception err)
                {
                    log.error("Music.playlistLoaded() Error:", err);
                }
            }

            @Override
            public synchronized void noMatches()
            {
                log.warn("No matches found");

                sendError(hook, "No matches found");

                musicPlayer.makeNewNowPlaying();
            }

            @Override
            public synchronized void loadFailed(FriendlyException throwable)
            {
                // Notify the user that everything exploded
                log.warn("Load failed: ", throwable);
                sendError(hook, "Load failed");

                musicPlayer.makeNewNowPlaying();
            }

        });
    }

    private void timeCmd(SlashCommandInteractionEvent event)
    {
        Guild serv = event.getGuild();
        AudioManager am = serv.getAudioManager();
        InteractionHook hook = event.getHook();
        if(!am.isConnected())
        {
            sendError(hook, "Bot not connected");
        }

        MusicPlayer player = (MusicPlayer) am.getSendingHandler();

        StringBuilder msg = new StringBuilder("```\n");
        AudioTrack track = player.player.getPlayingTrack();
        msg.append(track.getInfo().title).append("\n");

        // These are in ms, convert to s
        long duration = track.getDuration() / 1000;
        long pos = track.getPosition() / 1000;

        long durMin = duration / 60;
        long durSec = duration % 60;

        long posMin = pos / 60;
        long posSec = pos % 60;

        msg.append(posMin).append(":").append(posSec).append("/").append(durMin).append(":").append(durSec)
                .append("\n");

        final long LINE_SIZE = 48;
        float percent = (float) pos / duration;
        int spot = (int) (percent * (float) LINE_SIZE);

        msg.append("|");
        for(int i = 0; i < LINE_SIZE; ++i)
        {
            if(i != spot)
            {
                msg.append("-");
            }
            else
            {
                msg.append("O");
            }
        }
        msg.append("|");

        msg.append("```");
        hook.sendMessageEmbeds(new EmbedBuilder().setTitle("Time:").setDescription(msg.toString()).build()).queue();

    }

    private void seekCmd(SlashCommandInteractionEvent event)
    {
        Guild serv = event.getGuild();
        AudioManager am = serv.getAudioManager();
        InteractionHook hook = event.getHook();
        if(!am.isConnected())
        {
            sendError(hook, "Bot not connected");
        }

        String query = event.getOption("time").getAsString();
        Matcher matcher = SEEK_REGEX.matcher(query);

        if(!matcher.matches())
        {
            sendError(hook, "Invalid time");
        }

        MusicPlayer player = (MusicPlayer) am.getSendingHandler();
        if(!player.player.getPlayingTrack().isSeekable())
        {
            sendError(hook, "Track is not seekable");
        }

        String minuteStr = matcher.group("min");

        long minutes = 0;
        long secs = 0;
        if(minuteStr != null)
        {
            minutes = Long.parseLong(minuteStr);
            secs = Long.parseLong(matcher.group("sec"));
        }
        else
        {
            secs = Long.parseLong(matcher.group("allsec"));
        }

        long seekPos = (60 * minutes + secs) * 1000;

        String delta = matcher.group("delta");
        if(delta != null)
        {
            long curPos = player.player.getPlayingTrack().getPosition();
            log.debug("CurPos {}", curPos);
            if(delta.equals("+"))
            {
                seekPos = curPos + seekPos;
            }
            else
            {
                seekPos = curPos - seekPos;
            }
        }

        seekPos = Math.max(0, seekPos);

        player.player.getPlayingTrack().setPosition(seekPos);

        long outMin = seekPos / 1000 / 60;
        long outSec = seekPos / 1000 % 60;

        StringBuilder msg = new StringBuilder("Seeked to ");
        msg.append(outMin).append(":").append(outSec);

        hook.sendMessageEmbeds(new EmbedBuilder().setTitle("Seek").setDescription(msg.toString()).build()).queue();
    }

    /*
     * Get the player for an event, should only be used by buttons
     */
    private MusicPlayer getPlayer(ButtonInteractionEvent event)
    {
        AudioManager am = event.getGuild().getAudioManager();

        if(!am.isConnected())
        {
            event.getMessage().delete();
            event.replyEmbeds(new EmbedBuilder().setTitle("Error").setDescription("This player does not exist").build())
                    .setEphemeral(true).queue();

            return null;
        }

        return (MusicPlayer) am.getSendingHandler();
    }

    private void selectBtn(ButtonInteractionEvent event, String data)
    {
        event.deferEdit().queue();
        String[] args = data.split(":");
        long guildID = Long.parseLong(args[0]);
        int selected = Integer.parseInt(args[1]);

        QueryResult q = queryMap.get(guildID);
        // This will delete the msg and remove from the map
        q.stopThread();

        AudioTrack track = q.playlist.getTracks().get(selected);
        q.player.addToQueue(track);

        q.hook.sendMessageEmbeds(
                new EmbedBuilder().setTitle("Added to Queue").setDescription(track.getInfo().title).build()).complete();

        if(q.player.endOfQueue())
        {
            q.player.playIdx(q.player.queueLen() - 1);
        }
        else
        {
            q.player.makeNewNowPlaying();
        }
    }

    private void nextBtn(ButtonInteractionEvent event, String data)
    {
        event.deferEdit().queue();
        MusicPlayer mp = getPlayer(event);
        if(mp != null)
        {
            mp.startNext();
        }
    }

    private void prevBtn(ButtonInteractionEvent event, String data)
    {
        event.deferEdit().queue();
        MusicPlayer mp = getPlayer(event);
        if(mp != null)
        {
            mp.startPrev();
        }
    }

    private void playBtn(ButtonInteractionEvent event, String data)
    {
        event.deferEdit().queue();
        MusicPlayer mp = getPlayer(event);
        if(mp != null)
        {
            mp.player.setPaused(!mp.player.isPaused());
            mp.makeNewNowPlaying();
        }
    }

    private void loopBtn(ButtonInteractionEvent event, String data)
    {
        event.deferEdit().queue();
        MusicPlayer mp = getPlayer(event);
        if(mp != null)
        {
            mp.setLooping(!mp.isLooping());
            mp.makeNewNowPlaying();
        }
    }

    private void shuffleBtn(ButtonInteractionEvent event, String data)
    {
        event.deferEdit().queue();
        MusicPlayer mp = getPlayer(event);
        if(mp != null)
        {
            mp.shuffle();
        }
    }
}
