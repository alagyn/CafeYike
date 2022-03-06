package org.bdd.javacordCmd.exceptions;

/**
 * Raised by the Argument class
 */
public class ArgumentError extends CmdError
{
    public ArgumentError(String msg)
    {
        super(msg);
    }
}
