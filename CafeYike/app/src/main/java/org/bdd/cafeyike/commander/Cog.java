package org.bdd.cafeyike.commander;

import java.util.LinkedList;
import java.util.List;

import org.bdd.cafeyike.commander.exceptions.UsageError;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

/**
 * Contains one or more internal commands Used to group commands into a single
 * file
 * 
 * Extends listener Adapter so that the cog can register itself as an event listener is needed
 */
public abstract class Cog extends ListenerAdapter
{
    private LinkedList<Cmd> commands = new LinkedList<>();
    private LinkedList<Btn> butttons = new LinkedList<>();
    private LinkedList<Mdl> modals = new LinkedList<>();

    public abstract List<CommandData> buildCommands();

    protected Bot bot;

    public Cog(Bot bot)
    {
        this.bot = bot;
    }

    public void registerCmdFunc(Cmd.Func func, String name)
    {
        commands.add(new Cmd(func, name));
    }

    public void registerBtnFunc(Btn.Func func, String prefix)
    {
        butttons.add(new Btn(func, prefix));
    }

    public void registerModal(Mdl.Func func, String prefix)
    {
        modals.add(new Mdl(func, prefix));
    }

    public void registerNoopBtn(String name)
    {
        butttons.add(new Btn(this::noop, name));
    }

    public List<Cmd> getCommands()
    {
        return commands;
    }

    public List<Btn> getButtons()
    {
        return butttons;
    }

    public List<Mdl> getModals()
    {
        return modals;
    }

    public void shutdown()
    {
        // Pass
    }

    public void registerListeners(JDA api)
    {
        // Pass
    }

    private void noop(ButtonInteractionEvent event, String data)
    {

    }

    public void sendError(IReplyCallback event, String msg)
    {
        event.replyEmbeds(new EmbedBuilder().setTitle("Error").setDescription(msg).build()).setEphemeral(true).queue();
        throw new UsageError(msg);
    }

    public void sendError(InteractionHook hook, String msg)
    {
        hook.sendMessageEmbeds(new EmbedBuilder().setTitle("Error").setDescription(msg).build()).setEphemeral(true)
                .queue();
        throw new UsageError(msg);
    }
}
