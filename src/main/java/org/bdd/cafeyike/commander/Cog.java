package org.bdd.cafeyike.commander;

import java.util.LinkedList;
import java.util.List;

import org.javacord.api.DiscordApi;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.interaction.SlashCommandBuilder;

/**
 * Contains one or more internal commands Used to group commands into a single
 * file
 */
public abstract class Cog
{
    private LinkedList<Cmd> commands = new LinkedList<>();
    private LinkedList<Btn> butttons = new LinkedList<>();
    private LinkedList<Mdl> modals = new LinkedList<>();

    public abstract List<SlashCommandBuilder> buildCommands();

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

    public void registerListeners(DiscordApi api)
    {
        // Pass
    }

    private void noop(ButtonInteraction event, String data)
    {

    }
}
