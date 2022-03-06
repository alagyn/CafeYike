package org.bdd.javacordCmd.exceptions;

/**
 * Command-Level, error in command syntax
 */
public class UsageError extends CmdError
{
    public UsageError()
    {
        super("Invalid Command Syntax");
    }
}
