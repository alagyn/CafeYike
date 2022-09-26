package org.bdd.cafeyike;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.bdd.cafeyike.commands.Yike;
import org.javacord.api.entity.user.User;

public class CafeDB
{
    public static final CafeDB inst = new CafeDB();

    private Object mutex = new Object();

    private Connection conn = null;

    private String ADD_YIKE_ST = null;
    private String INC_YIKE_ST = null;
    private String SET_YIKE_ST = null;
    private String REM_YIKE_ST = null;
    private String GET_YIKE_FOR_USER_ST = null;
    private String GET_YIKE_FOR_SERVER_ST = null;
    private String ADD_QUOTE_ST = null;
    private String GET_QUOTE_ST = null;

    private int TIMEOUT_SEC = 10;

    private CafeDB()
    {
    }

    public class YikeEntry implements Comparable<YikeEntry>
    {
        public final long ID;
        public int count;

        public YikeEntry(long ID, int count)
        {
            this.ID = ID;
            this.count = count;
        }

        @Override
        public int compareTo(YikeEntry o)
        {
            // Backwards to sort descending
            return Integer.compare(o.count, count);
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
        GET_YIKE_FOR_USER_ST = loadStatement("sql/getYikesForUser.sql");
        GET_YIKE_FOR_SERVER_ST = loadStatement("sql/getYikesForServer.sql");
    }

    private void initQuoteTable() throws SQLException
    {
        Statement s = conn.createStatement();

        s.execute(loadStatement("sql/quoteSchema.sql"));

        ADD_QUOTE_ST = loadStatement("sql/addQuote.sql");
        GET_QUOTE_ST = loadStatement("sql/getQuotes.sql");
    }

    public static int addYike(long guildId, long userId)
    {
        return inst._addYike(guildId, userId);
    }

    private int _addYike(long guildId, long userId)
    {
        try
        {
            PreparedStatement s = conn.prepareStatement(INC_YIKE_ST);
            s.setLong(1, guildId);
            s.setLong(2, userId);
            s.setQueryTimeout(TIMEOUT_SEC);
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
                s2.setQueryTimeout(TIMEOUT_SEC);
                return 1;
            }
        }
        catch(SQLException e)
        {
            Bot.inst.logInfo(e.getSQLState());
            throw new CmdError("Unable to update yikes: " + e.getMessage());
        }
    }

    public static int setYikes(long guildId, long userId, int count)
    {
        return inst._setYikes(guildId, userId, count);
    }

    private int _setYikes(long guildId, long userId, int count)
    {
        try
        {
            PreparedStatement s = conn.prepareStatement(SET_YIKE_ST);
            s.setInt(1, count);
            s.setLong(2, guildId);
            s.setLong(3, userId);
            s.setQueryTimeout(TIMEOUT_SEC);
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

    public static int getYikesForUser(long guildId, long userId)
    {
        return inst._getYikesForUser(guildId, userId);
    }

    private int _getYikesForUser(long guildId, long userId)
    {
        try
        {
            PreparedStatement s = conn.prepareStatement(GET_YIKE_FOR_USER_ST);
            s.setLong(1, guildId);
            s.setLong(2, userId);
            s.setQueryTimeout(TIMEOUT_SEC);
            ResultSet r = s.executeQuery();

            int out = 0;
            if(r.next())
            {
                out = r.getInt("count");
            }
            else
            {
                Bot.inst.logDbg("Not found, guild: " + guildId + ", user: " + userId);
            }

            r.close();
            return out;
        }
        catch(SQLException e)
        {
            throw new CmdError("Unable to read yikes: " + e.getMessage());
        }
    }

    public static List<YikeEntry> getYikesForServer(long guildId)
    {
        return inst._getYikesForServer(guildId);
    }

    private List<YikeEntry> _getYikesForServer(long guildId)
    {
        try
        {
            PreparedStatement s = conn.prepareStatement(GET_YIKE_FOR_SERVER_ST);
            s.setLong(1, guildId);
            s.setQueryTimeout(TIMEOUT_SEC);
            ResultSet r = s.executeQuery();

            ArrayList<YikeEntry> out = new ArrayList<>();

            while(r.next())
            {
                out.add(new YikeEntry(r.getLong(1), r.getInt(2)));
            }

            r.close();

            return out;
        }
        catch(SQLException e)
        {
            throw new CmdError("Cannot get yikes: " + e.getMessage());
        }
    }

    public static int remYike(long guildId, long userId)
    {
        return inst._remYike(guildId, userId);
    }

    private int _remYike(long guildId, long userId)
    {
        try
        {
            PreparedStatement s = conn.prepareStatement(REM_YIKE_ST);
            s.setLong(1, guildId);
            s.setLong(2, userId);
            s.setQueryTimeout(TIMEOUT_SEC);
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

    public static void addQuote(long userId, String content)
    {
        inst._addQuote(userId, content);
    }

    private void _addQuote(long userId, String content)
    {
        try
        {
            PreparedStatement s = conn.prepareStatement(ADD_QUOTE_ST);
            s.setLong(1, userId);
            s.setString(2, content);
            s.setQueryTimeout(TIMEOUT_SEC);
            s.executeUpdate();
        }
        catch(SQLException e)
        {
            throw new CmdError("Unable to add quote: " + e.getMessage());
        }
    }

    public class QuoteEntry
    {
        public final long userId;
        public final String content;
        public final Timestamp timestamp;

        public QuoteEntry(ResultSet r) throws SQLException
        {
            userId = r.getLong(1);
            content = r.getString(2);
            timestamp = r.getTimestamp(3);
        }
    }

    public static List<QuoteEntry> getQuotes(Collection<User> users)
    {
        return inst._getQuotes(users);
    }

    private List<QuoteEntry> _getQuotes(Collection<User> users)
    {
        List<QuoteEntry> out = new LinkedList<>();

        try
        {
            StringBuilder idStringB = new StringBuilder();

            Iterator<User> iter = users.iterator();
            while(iter.hasNext())
            {
                idStringB.append(iter.next().getId());
                if(iter.hasNext())
                {
                    idStringB.append(", ");
                }
            }

            String stat = GET_QUOTE_ST.replace("?", idStringB.toString());

            PreparedStatement s = conn.prepareStatement(stat);
            s.setQueryTimeout(TIMEOUT_SEC);
            ResultSet r = s.executeQuery();

            while(r.next())
            {
                out.add(new QuoteEntry(r));
            }

            r.close();
        }
        catch(SQLException e)
        {
            throw new CmdError("Cannot get quotes: " + e.getMessage());
        }

        return out;
    }
}
