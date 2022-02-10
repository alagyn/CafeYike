package org.bdd.javacordCmd;

import org.bdd.javacordCmd.exceptions.ArgumentError;
import org.bdd.javacordCmd.exceptions.CmdError;
import org.javacord.api.entity.user.User;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Arguments
{
    //Idx of next char to read
    private int curIdx;
    public final String str;

    public Arguments(String args) throws CmdError
    {
        if(args.length() == 0)
        {
            throw new CmdError("No Command given");
        }
        this.curIdx = 0;
        this.str = args.strip();
    }

    public boolean hasNext()
    {
        return curIdx < str.length();
    }

    public boolean hasPrev()
    {
        return curIdx > 0;
    }

    private StringBuilder get(int dir, boolean noQuotes) throws ArgumentError
    {
        StringBuilder out = new StringBuilder();

        if(dir < 0 && curIdx >= str.length())
        {
            curIdx = str.length() - 1;
        }

        while(str.charAt(curIdx) == ' ')
        {
            curIdx += dir;
        }

        char endChar = ' ';
        if(str.charAt(curIdx) == '"')
        {
            if(noQuotes)
            {
                throw new ArgumentError("Cannot quote argument");
            }
            else
            {
                endChar = '"';
            }

        }

        while(curIdx < str.length() && curIdx >= 0 && str.charAt(curIdx) != endChar)
        {
            out.append(str.charAt(curIdx));
            curIdx += dir;
        }

        if(curIdx < 0)
        {
            curIdx = 0;
        }

        return out;
    }

    public String prev(boolean noQuotes) throws ArgumentError
    {
        if(!hasPrev())
        {
            throw new ArgumentError("No previous arguments");
        }
        return get(-1, noQuotes).reverse().toString();
    }

    public String next(boolean noQuotes) throws ArgumentError
    {
        if(!hasNext())
        {
            throw new ArgumentError("No more arguments");
        }
        return get(1, noQuotes).toString();
    }

    public String next() throws ArgumentError
    {
        return next(false);
    }

    public String prev() throws ArgumentError
    {
        return prev(false);
    }

    public String peekPrev()
    {
        int x = curIdx;
        String out = prev();
        curIdx = x;
        return out;
    }

    private static final Pattern userPattern = Pattern.compile("<@!(?<id>\\d+)>");

    public User nextUser() throws ArgumentError
    {
        String str = next(true);

        Matcher m = userPattern.matcher(str);

        if(!m.matches())
        {
            throw new ArgumentError("@User expected");
        }

        String sid = m.group("id");
        long id = Long.parseLong(sid);

        return Bot.inst.getApi().getUserById(id).join();
    }

    public String remainder()
    {
        return str.substring(curIdx).trim();
    }

}
