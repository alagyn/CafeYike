package org.bdd.cafeyike.commander.exceptions;

/**
 * Command-Level, error in command syntax
 */
public class UsageError extends CmdError
{
    public UsageError(String message)
    {
        // TODO better message?
        super(message);
    }
}
