package org.bdd.cafeyike.commander.utils;

import java.util.function.Consumer;
import org.bdd.cafeyike.commander.exceptions.CmdError;

public class DoAfter extends Thread
{
    private final long millis;
    private Consumer<Integer> cb;

    public DoAfter(long sec, Consumer<Integer> cb)
    {
        if(sec / 60 > 15)
        {
            throw new CmdError("Cannot update response after more than 15 min");
        }
        this.millis = sec * 1000;
        this.cb = cb;

        setDaemon(true);
        start();
    }

    public void run()
    {
        try
        {
            Thread.sleep(millis);
        }
        catch(InterruptedException e)
        {
            // pass
        }

        cb.accept(0);
    }
}
