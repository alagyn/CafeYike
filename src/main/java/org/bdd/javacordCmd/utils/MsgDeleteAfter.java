package org.bdd.javacordCmd.utils;

import org.bdd.javacordCmd.commands.Cog;
import org.javacord.api.entity.message.Message;

public class MsgDeleteAfter extends Thread
{

    private final Message m;
    private final long s;
    Runnable cb;

    public void run()
    {
        try
        {
            Thread.sleep(s);
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
        this.s = sec * 1000;
        this.cb = cb;

        start();
    }

    public MsgDeleteAfter(Message m, long sec)
    {
        this.m = m;
        this.s = sec;
        this.cb = null;

        start();
    }
}
