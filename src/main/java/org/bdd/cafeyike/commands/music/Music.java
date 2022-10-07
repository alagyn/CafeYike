package org.bdd.cafeyike.commands.music;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.Cog;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Music extends Cog
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private AudioPlayerManager playerManager;

    // Maps ServerID -> AudioConnection
    private HashMap<Long, AudioConnection> connections;

    public static final String PREV_BTN = "prev";
    public static final String NEXT_BTN = "next";
    public static final String PLAY_BTN = "play";
    public static final String SHUF_BTN = "shuf";
    public static final String LOOP_BTN = "loop";

    public Music()
    {
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);

        connections = new HashMap<>();
    }

    @Override
    public List<SlashCommandBuilder> buildCommands()
    {
        LinkedList<SlashCommandBuilder> out = new LinkedList<>();

        out.add(SlashCommand.with("play", "Start/Queue a video to play").addOption(SlashCommandOption
                .create(SlashCommandOptionType.STRING, "query", "The search query or url to play", true)));

        registerCmdFunc(this::playCmd, "play");

        out.add(SlashCommand.with("leave", "Stop playing and leave the voice chat"));

        registerCmdFunc(this::leaveCmd, "leave");

        registerBtnFunc(this::nextBtn, NEXT_BTN);
        registerBtnFunc(this::prevBtn, PREV_BTN);
        registerBtnFunc(this::playBtn, PLAY_BTN);
        registerBtnFunc(this::loopBtn, LOOP_BTN);
        registerBtnFunc(this::shuffleBtn, SHUF_BTN);

        return out;
    }

    private void leaveCmd(SlashCommandInteraction event)
    {
        Server serv = event.getServer().orElse(null);
        if(serv == null)
        {
            Bot.sendError(event, "Not in a server");
        }

        AudioConnection ac = connections.remove(serv.getId());

        if(ac != null)
        {
            MusicPlayer mp = (MusicPlayer) ac.getAudioSource().get();
            mp.nowPlayingMsg.delete();
            mp.stop();
            ac.close();
            event.createImmediateResponder().addEmbed(new EmbedBuilder().addField("Music", "Goodbye")).respond();
        }
        else
        {
            event.createImmediateResponder().addEmbed(new EmbedBuilder().addField("Error", "Nothing to do"))
                    .setFlags(MessageFlag.EPHEMERAL).respond();
        }
    }

    private void playCmd(SlashCommandInteraction event)
    {
        User u = event.getUser();
        Server serv = event.getServer().orElse(null);

        if(serv == null)
        {
            Bot.sendError(event, "Not in a server");
        }

        ServerVoiceChannel voiceChannel = u.getConnectedVoiceChannel(serv).orElse(null);
        TextChannel textChannel = event.getChannel().orElse(null);

        if(voiceChannel == null)
        {
            Bot.sendError(event, "Not in a voice chat");
        }

        if(textChannel == null)
        {
            Bot.sendError(event, "Invalid text channel");
        }

        String query = event.getOptionStringValueByIndex(0).orElse("");

        if(query.isEmpty())
        {
            // Empty query here is error
            // pause/resume/etc will be buttons
            Bot.sendError(event, "Empty query");
        }

        event.respondLater();

        AudioConnection ac = connections.get(serv.getId());

        if(ac == null)
        {
            ac = voiceChannel.connect().join();
            AudioPlayer player = playerManager.createPlayer();
            // TODO add control buttons here?
            Message m = event.createFollowupMessageBuilder()
                    .addEmbed(new EmbedBuilder().addField("Now Playing", "Loading...")).send().join();
            // Create an audio source and add it to the audio connection's queue
            MusicPlayer source = new MusicPlayer(Bot.inst.getApi(), player, m, voiceChannel, textChannel, serv.getId());
            // Source gets set as a listener to the player in its constructor
            ac.setAudioSource(source);

            connections.put(serv.getId(), ac);
        }

        MusicPlayer musicPlayer = (MusicPlayer) ac.getAudioSource().get();

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
                    event.createFollowupMessageBuilder()
                            .addEmbed(new EmbedBuilder().addField("Added to Queue", track.getInfo().title)).send();
                    musicPlayer.makeNewNowPlaying();
                }

            }

            @Override
            public synchronized void playlistLoaded(AudioPlaylist playlist)
            {
                log.trace("playlistLoaded()");
                int len = playlist.getTracks().size();
                StringBuilder text = new StringBuilder();
                text.append("Playlist: ").append(playlist.getName()).append("\n");

                text.append(len).append(" ").append(len == 1 ? "item." : "items.");
                event.createFollowupMessageBuilder()
                        .addEmbed(new EmbedBuilder().addField("Added to Queue", text.toString()));

                if(len <= 0)
                {
                    return;
                }

                int nextIdx = musicPlayer.queueLen();

                for(AudioTrack track : playlist.getTracks())
                {
                    musicPlayer.addToQueue(track);
                }

                if(musicPlayer.endOfQueue())
                {
                    musicPlayer.playIdx(nextIdx);
                }

                musicPlayer.makeNewNowPlaying();
            }

            @Override
            public synchronized void noMatches()
            {
                log.warn("No matches found");

                Bot.sendFollowError(event, "No matches found");

                musicPlayer.makeNewNowPlaying();
            }

            @Override
            public synchronized void loadFailed(FriendlyException throwable)
            {
                // Notify the user that everything exploded
                log.warn("Load failed: ", throwable);
                Bot.sendFollowError(event, "Load failed");

                musicPlayer.makeNewNowPlaying();
            }

        });
    }

    private void nextBtn(ButtonInteraction event, String data)
    {
        long serverId = Long.parseLong(data);

        AudioConnection ac = connections.get(serverId);

        if(ac == null)
        {
            event.getMessage().delete();
            event.createImmediateResponder()
                    .addEmbed(new EmbedBuilder().addField("Error", "This player does not exist"))
                    .setFlags(MessageFlag.EPHEMERAL).respond();
            return;
        }

        event.acknowledge();
        ((MusicPlayer) ac.getAudioSource().get()).startNext();
    }

    private void prevBtn(ButtonInteraction event, String data)
    {
        long serverId = Long.parseLong(data);

        AudioConnection ac = connections.get(serverId);

        if(ac == null)
        {
            event.getMessage().delete();
            event.createImmediateResponder()
                    .addEmbed(new EmbedBuilder().addField("Error", "This player does not exist"))
                    .setFlags(MessageFlag.EPHEMERAL).respond();
            return;
        }

        event.acknowledge();

        MusicPlayer mp = (MusicPlayer) ac.getAudioSource().get();

        mp.startPrev();
    }

    private void playBtn(ButtonInteraction event, String data)
    {
        long serverId = Long.parseLong(data);

        AudioConnection ac = connections.get(serverId);
        if(ac == null)
        {
            event.getMessage().delete();
            event.createImmediateResponder()
                    .addEmbed(new EmbedBuilder().addField("Error", "This player does not exist"))
                    .setFlags(MessageFlag.EPHEMERAL).respond();
            return;
        }

        event.acknowledge();

        MusicPlayer mp = (MusicPlayer) ac.getAudioSource().get();

        mp.player.setPaused(!mp.player.isPaused());
    }

    private void loopBtn(ButtonInteraction event, String data)
    {
        long serverId = Long.parseLong(data);

        AudioConnection ac = connections.get(serverId);

        if(ac == null)
        {
            event.getMessage().delete();
            event.createImmediateResponder()
                    .addEmbed(new EmbedBuilder().addField("Error", "This player does not exist"))
                    .setFlags(MessageFlag.EPHEMERAL).respond();
            return;
        }

        event.acknowledge();

        MusicPlayer mp = (MusicPlayer) ac.getAudioSource().get();

        mp.setLooping(!mp.isLooping());
    }

    private void shuffleBtn(ButtonInteraction event, String data)
    {
        long serverId = Long.parseLong(data);

        AudioConnection ac = connections.get(serverId);

        if(ac == null)
        {
            event.getMessage().delete();
            event.createImmediateResponder()
                    .addEmbed(new EmbedBuilder().addField("Error", "This player does not exist"))
                    .setFlags(MessageFlag.EPHEMERAL).respond();
            return;
        }

        event.acknowledge();

        MusicPlayer mp = (MusicPlayer) ac.getAudioSource().get();

        mp.shuffle();
    }
}
