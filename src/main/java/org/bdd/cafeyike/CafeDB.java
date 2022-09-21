package org.bdd.cafeyike;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.exceptions.CmdError;

public class CafeDB
{
    public static final CafeDB inst = new CafeDB();

    private Object mutex = new Object();

    private Connection conn = null;

    private String ADD_YIKE_ST = null;
    private String INC_YIKE_ST = null;
    private String SET_YIKE_ST = null;
    private String REM_YIKE_ST = null;
    private String GET_YIKE_ST = null;
    private String ADD_QUOTE_ST = null;

    private CafeDB()
    {
    }

    public class YikeLog
    {
        public final long ID;
        public int count;

        public YikeLog(long ID, int count)
        {
            this.ID = ID;
            this.count = count;
        }
    }

    public void init() throws SQLException
    {
        try
        {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:dat/cafe.db");
        }
        catch(ClassNotFoundException e)
        {
            throw new CmdError("Cannot load SQLite");
        }

        conn.setAutoCommit(true);

        initYikeTable();
        initQuoteTable();
    }

    private String loadStatement(String path)
    {
        try
        {
            return Files.readString(Path.of(path));
        }
        catch(IOException e)
        {
            throw new CmdError("Cannot load \"" + path + "\"");
        }
    }

    private void initYikeTable() throws SQLException
    {
        Statement s = conn.createStatement();
        s.execute(loadStatement("sql/yikeSchema.sql"));

        ADD_YIKE_ST = loadStatement("sql/addYikeLog.sql");
        INC_YIKE_ST = loadStatement("sql/incYike.sql");
        SET_YIKE_ST = loadStatement("sql/setYikes.sql");
        REM_YIKE_ST = loadStatement("sql/remYike.sql");
        GET_YIKE_ST = loadStatement("sql/getYikes.sql");
    }

    private void initQuoteTable() throws SQLException
    {
        Statement s = conn.createStatement();

        s.execute(loadStatement("sql/quoteSchema.sql"));

        ADD_QUOTE_ST = loadStatement("sql/addQuote.sql");
    }

    public static int addYike(long guildId, long userId)
    {
        return inst._addYike(guildId, userId);
    }

    private int _addYike(long guildId, long userId)
    {
        synchronized(mutex)
        {
            try
            {
                PreparedStatement s = conn.prepareStatement(INC_YIKE_ST);
                s.setLong(1, guildId);
                s.setLong(2, userId);
                s.setQueryTimeout(5);
                ResultSet r = s.executeQuery();
                if(r.next())
                {
                    // If this returned, the item already existed and was incremented
                    int out = r.getInt("count");
                    r.close();
                    return out;
                }
                // Else add a new statement
                else
                {
                    PreparedStatement s2 = conn.prepareStatement(ADD_YIKE_ST);
                    s2.setLong(1, guildId);
                    s2.setLong(2, userId);
                    s2.executeUpdate();
                    s2.setQueryTimeout(5);
                    return 1;
                }
            }
            catch(SQLException e)
            {
                Bot.inst.logInfo(e.getSQLState());
                throw new CmdError("Unable to update yikes: " + e.getMessage());
            }
        }
    }

    public static int setYikes(long guildId, long userId, int count)
    {
        return inst._setYikes(guildId, userId, count);
    }

    private int _setYikes(long guildId, long userId, int count)
    {
        synchronized(mutex)
        {
            try
            {
                PreparedStatement s = conn.prepareStatement(SET_YIKE_ST);
                s.setInt(1, count);
                s.setLong(2, guildId);
                s.setLong(3, userId);
                s.setQueryTimeout(5);
                ResultSet r = s.executeQuery();
                int out = r.getInt("count");
                r.close();
                return out;
            }
            catch(SQLException e)
            {
                throw new CmdError("Unable to update yikes: " + e.getMessage());
            }
        }
    }

    public static int getYikesForUser(long guildId, long userId)
    {
        return inst._getYikesForUser(guildId, userId);
    }

    private int _getYikesForUser(long guildId, long userId)
    {
        synchronized(mutex)
        {
            try
            {
                PreparedStatement s = conn.prepareStatement(SET_YIKE_ST);
                s.setQueryTimeout(5);
                ResultSet r = s.executeQuery();

                int out = 0;
                if(r.next())
                {
                    out = r.getInt("count");
                }

                r.close();
                return out;
            }
            catch(SQLException e)
            {
                throw new CmdError("Unable to read yikes: " + e.getMessage());
            }
        }
    }

    public static int remYike(long guildId, long userId)
    {
        return inst._remYike(guildId, userId);
    }

    private int _remYike(long guildId, long userId)
    {
        synchronized(mutex)
        {
            try
            {
                PreparedStatement s = conn.prepareStatement(REM_YIKE_ST);
                s.setLong(1, guildId);
                s.setLong(2, userId);
                s.setQueryTimeout(5);
                ResultSet r = s.executeQuery();
                if(r.next())
                {
                    return r.getInt("count");
                }
                else
                {
                    throw new CmdError("Unkown User/Server ID");
                }
            }
            catch(SQLException e)
            {
                throw new CmdError("Unable to remove yike: " + e.getMessage());
            }
        }
    }
}
