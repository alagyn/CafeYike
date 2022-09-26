package org.bdd.cafeyike.commander.utils;

import org.javacord.api.entity.message.Message;

/**
 * Helper to automatically delete a message after a time delay and run a specified callback
 */
public class MsgDeleteAfter extends Thread
{
    private final Message m;
    private final long millis;
    Runnable cb;

    public void run()
    {
        try
        {
            Thread.sleep(millis);
        }
        catch(InterruptedException e)
        {
            //PASS
        }

        m.delete();

        if(cb != null)
        {
            cb.run();
        }
    }

    public MsgDeleteAfter(Message m, long sec, Runnable cb)
    {
        this.m = m;
        this.millis = sec * 1000;
        this.cb = cb;

        setDaemon(true);
        start();
    }

    public MsgDeleteAfter(Message m, long sec)
    {
        this(m, sec, null);
    }
}
