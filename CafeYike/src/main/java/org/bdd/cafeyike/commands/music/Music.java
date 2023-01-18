package org.bdd.cafeyike.commands.music;

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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.managers.AudioManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Music extends Cog
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private AudioPlayerManager playerManager;
    public final int leaveTimeMillis;

    public static final String PREV_BTN = "prev";
    public static final String NEXT_BTN = "next";
    public static final String PLAY_BTN = "play";
    public static final String SHUF_BTN = "shuf";
    public static final String LOOP_BTN = "loop";

    public static final String URL_RE_STR = "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\\\".,<>?«»“”‘’]))";

    public static final Pattern URL_REGEX = Pattern.compile(URL_RE_STR);

    public Music(Bot bot)
    {
        super(bot);
        leaveTimeMillis = CafeConfig.getIntConfig("musicLeaveTimeSec") * 1000;
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
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

        registerBtnFunc(this::nextBtn, NEXT_BTN);
        registerBtnFunc(this::prevBtn, PREV_BTN);
        registerBtnFunc(this::playBtn, PLAY_BTN);
        registerBtnFunc(this::loopBtn, LOOP_BTN);
        registerBtnFunc(this::shuffleBtn, SHUF_BTN);

        return out;
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
            InteractionHook hook = event
                    .replyEmbeds(new EmbedBuilder().setTitle("Music").setDescription("Goodbye").build()).complete();
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

        if(serv == null)
        {
            sendError(event, "Not in a server");
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
            sendError(event, "Not in a voice chat");
        }

        if(textChannel == null)
        {
            sendError(event, "Invalid text channel");
        }

        String query = event.getOption("query").getAsString();

        if(query.isEmpty())
        {
            // Empty query here is error
            // pause/resume/etc will be buttons
            sendError(event, "Empty query");
        }

        InteractionHook hook = event.deferReply().complete();

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

            }

            @Override
            public synchronized void playlistLoaded(AudioPlaylist playlist)
            {
                log.trace("playlistLoaded() isSearch {}", isSearch);

                if(isSearch)
                {
                    loadQuery(playlist);
                }
                else
                {
                    loadPlaylist(playlist);
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
