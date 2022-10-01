package org.bdd.cafeyike;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.bdd.cafeyike.commander.Bot;
import org.bdd.cafeyike.commander.exceptions.CmdError;
import org.javacord.api.entity.user.User;

public class CafeDB
{
    public static final CafeDB inst = new CafeDB();

    private Connection conn = null;

    private static String ADD_YIKE_ST = null;
    private static String INC_YIKE_ST = null;
    private static String SET_YIKE_ST = null;
    private static String REM_YIKE_ST = null;
    private static String GET_YIKE_FOR_USER_ST = null;
    private static String GET_YIKE_FOR_SERVER_ST = null;

    private static String ADD_QUOTE_ST = null;
    private static String GET_QUOTES_ST = null;
    private static String GET_QUOTE_BY_ID_ST = null;
    private static String EDIT_QUOTE_ST = null;
    private static String EDIT_QUOTE_TS_ST = null;
    private static String RM_QUOTE_ST = null;

    private int TIMEOUT_SEC = 10;

    private CafeDB()
    {
    }

    public static class YikeEntry implements Comparable<YikeEntry>
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
        GET_QUOTES_ST = loadStatement("sql/getQuotes.sql");
        GET_QUOTE_BY_ID_ST = loadStatement("sql/getQuoteByID.sql");
        EDIT_QUOTE_ST = loadStatement("sql/editQuote.sql");
        EDIT_QUOTE_TS_ST = loadStatement("sql/editQuoteTs.sql");
        RM_QUOTE_ST = loadStatement("sql/rmQuote.sql");
    }

    private static PreparedStatement prepare(String statement) throws SQLException
    {
        PreparedStatement s = inst.conn.prepareStatement(statement);
        s.setQueryTimeout(inst.TIMEOUT_SEC);
        return s;
    }

    public static int addYike(long guildId, long userId)
    {
        try
        {
            PreparedStatement s = prepare(INC_YIKE_ST);
            s.setLong(1, guildId);
            s.setLong(2, userId);
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
                PreparedStatement s2 = prepare(ADD_YIKE_ST);
                s2.setLong(1, guildId);
                s2.setLong(2, userId);
                s2.executeUpdate();
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
        try
        {
            PreparedStatement s = prepare(SET_YIKE_ST);
            s.setInt(1, count);
            s.setLong(2, guildId);
            s.setLong(3, userId);

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
        try
        {
            PreparedStatement s = prepare(GET_YIKE_FOR_USER_ST);
            s.setLong(1, guildId);
            s.setLong(2, userId);
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
        try
        {
            PreparedStatement s = prepare(GET_YIKE_FOR_SERVER_ST);
            s.setLong(1, guildId);

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
        try
        {
            PreparedStatement s = prepare(REM_YIKE_ST);
            s.setLong(1, guildId);
            s.setLong(2, userId);

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

    public static long addQuote(long userId, String content)
    {
        try
        {
            PreparedStatement s = prepare(ADD_QUOTE_ST);
            s.setLong(1, userId);
            s.setString(2, content);

            ResultSet r = s.executeQuery();
            r.next();
            long out = r.getLong(1);
            r.close();
            return out;
        }
        catch(SQLException e)
        {
            throw new CmdError("Unable to add quote: " + e.getMessage());
        }
    }

    public static class QuoteEntry
    {
        public final long quoteId;
        public final long userId;
        public final String content;
        public final Timestamp timestamp;

        public QuoteEntry(ResultSet r) throws SQLException
        {
            quoteId = r.getLong("quote_id");
            userId = r.getLong("user_id");
            content = r.getString("content");
            timestamp = r.getTimestamp("created");
        }
    }

    public static List<QuoteEntry> getQuotes(Collection<User> users)
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

            String stat = GET_QUOTES_ST.replace("?", idStringB.toString());

            PreparedStatement s = prepare(stat);

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

    public static QuoteEntry getQuote(long quoteID)
    {
        try
        {
            PreparedStatement s = prepare(GET_QUOTE_BY_ID_ST);
            s.setLong(1, quoteID);
            ResultSet r = s.executeQuery();

            if(r.next())
            {
                QuoteEntry out = new QuoteEntry(r);
                r.close();
                return out;
            }
            else
            {
                r.close();
                throw new CmdError("Quote Not Found");
            }
        }
        catch(SQLException e)
        {
            throw new CmdError("Cannot get quote: " + e.getMessage());
        }
    }

    public static void editQuote(long quoteID, String content, Timestamp ts)
    {
        try
        {
            if(content.length() > 0)
            {
                PreparedStatement s = prepare(EDIT_QUOTE_ST);

                s.setString(1, content);
                s.setLong(2, quoteID);
                s.executeUpdate();
            }

            if(ts != null)
            {
                PreparedStatement s = prepare(EDIT_QUOTE_TS_ST);
                s.setTimestamp(1, ts);
                s.setLong(2, quoteID);
                s.executeUpdate();
            }
        }
        catch(SQLException e)
        {
            throw new CmdError("Cannot edit quote: " + e.getMessage());
        }
    }

    public static void rmQuote(long quoteId)
    {
        try
        {
            PreparedStatement s = prepare(RM_QUOTE_ST);
            s.setLong(1, quoteId);
            s.executeUpdate();
        }
        catch(SQLException e)
        {
            throw new CmdError("Cannot delete quote: " + e.getMessage());
        }
    }
}
