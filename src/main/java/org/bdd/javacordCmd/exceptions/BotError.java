package org.bdd.javacordCmd.exceptions;

public class BotError extends RuntimeException
{
    public BotError(String msg)
    {
        super(msg);
    }
}
