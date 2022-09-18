package org.bdd.cafeyike.commander.exceptions;

/**
 * Bot level generic error
 */
public class BotError extends RuntimeException
{
    public BotError(String msg)
    {
        super(msg);
    }
}
