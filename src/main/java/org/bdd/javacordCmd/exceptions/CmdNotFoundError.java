package org.bdd.javacordCmd.exceptions;

public class CmdNotFoundError extends CmdError
{
    public final String cmdName;

    public CmdNotFoundError(String cmdName)
    {
        super("Command not found: " + cmdName);
        this.cmdName = cmdName;
    }
}
