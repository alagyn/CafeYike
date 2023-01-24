package org.bdd.cafeyike;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.bdd.cafeyike.commander.exceptions.CmdError;

public class CafeDB
{
    private static final Logger log = LoggerFactory.getLogger(CafeDB.class);

    public static final CafeDB inst = new CafeDB();

    private Connection conn = null;

    private static String ADD_NEW_USER_ST = null;
    private static String INC_YIKE_ST = null;
    private static String REM_YIKE_ST = null;
    private static String GET_YIKE_FOR_USER_ST = null;
    private static String GET_YIKE_FOR_SERVER_ST = null;

    private static String ADD_QUOTE_ST = null;
    private static String GET_QUOTE_FOR_SERVER_ST = null;
    private static String GET_QUOTE_FOR_USER_ST = null;
    private static String GET_QUOTE_BY_ID_ST = null;
    private static String EDIT_QUOTE_ST = null;
    private static String EDIT_QUOTE_TS_ST = null;

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
        File dbFile = new File(CafeConfig.getEnv("CafeYikeDB"));
        boolean needToInit = !dbFile.exists();

        try
        {
            SQLiteConfig sqlConfig = new SQLiteConfig();
            sqlConfig.setTempStoreDirectory("../temp");
            Class.forName("org.sqlite.JDBC");
            StringBuilder ss = new StringBuilder();
            ss.append("jdbc:sqlite:").append(dbFile.getAbsolutePath());
            log.info("CafeDB.init() Loading DB {}", ss.toString());
            conn = DriverManager.getConnection(ss.toString());
        }
        catch(ClassNotFoundException e)
        {
            throw new CmdError("Cannot load SQLite");
        }

        conn.setAutoCommit(true);

        // Yike Statements
        ADD_NEW_USER_ST = loadStatement("yikes/addNewUser.sql");
        INC_YIKE_ST = loadStatement("yikes/incYike.sql");
        REM_YIKE_ST = loadStatement("yikes/remYike.sql");
        GET_YIKE_FOR_USER_ST = loadStatement("yikes/getYikesForUser.sql");
        GET_YIKE_FOR_SERVER_ST = loadStatement("yikes/getYikesForServer.sql");

        // Quote statements
        ADD_QUOTE_ST = loadStatement("quotes/addQuote.sql");
        GET_QUOTE_FOR_SERVER_ST = loadStatement("quotes/getQuotesForServer.sql");
        GET_QUOTE_FOR_USER_ST = loadStatement("quotes/getQuotesForUser.sql");
        GET_QUOTE_BY_ID_ST = loadStatement("quotes/getQuoteByID.sql");
        EDIT_QUOTE_ST = loadStatement("quotes/editQuote.sql");
        EDIT_QUOTE_TS_ST = loadStatement("quotes/editQuoteTs.sql");

        // Init tables
        if(needToInit)
        {
            try
            {
                Statement s = conn.createStatement();
                log.debug("Initting new CafeDatabase");
                s.execute("PRAGMA foreign_keys = ON");
                log.debug("Creating User Table");
                s.execute(loadStatement("schemas/userSchema.sql"));
                log.debug("Creating Quote Table");
                s.execute(loadStatement("schemas/quoteSchema.sql"));
            }
            catch(SQLException err)
            {
                log.error("Cannot init CafeDatabase", err);
                throw err;
            }
        }
    }

    private String loadStatement(String path)
    {
        try
        {
            InputStream is = getClass().getResourceAsStream("/sql/" + path);
            if(is == null)
            {
                throw new CmdError("Cannot load \"" + path + "\"");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch(IOException e)
        {
            throw new CmdError("Cannot load \"" + path + "\"");
        }
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
                int out = r.getInt("yike_count");
                r.close();
                return out;
            }
            r.close();
            throw new CmdError("Unable to update yikes: Unkown error");
        }
        catch(SQLException e)
        {
            log.error("Cannot add yike: ", e.getSQLState());
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
                out = r.getInt("yike_count");
            }
            else
            {
                log.error("Not found, guild: {}, user: {}", guildId, userId);
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
                int out = r.getInt("yike_count");
                r.close();
                return out;
            }
            else
            {
                r.close();
                throw new CmdError("Unkown User/Server ID");
            }
        }
        catch(SQLException e)
        {
            throw new CmdError("Unable to remove yike: " + e.getMessage());
        }
    }

    public static long addQuote(long guildId, long userId, String content)
    {
        try
        {
            PreparedStatement s1 = prepare(ADD_NEW_USER_ST);
            s1.setLong(1, guildId);
            s1.setLong(2, userId);

            s1.execute();

            // Add the new quote
            PreparedStatement s2 = prepare(ADD_QUOTE_ST);
            s2.setString(1, content);
            s2.setLong(2, guildId);
            s2.setLong(3, userId);

            ResultSet r2 = s2.executeQuery();
            r2.next();
            long quoteId = r2.getLong(1);
            r2.close();
            return quoteId;
        }
        catch(SQLException e)
        {
            log.warn("Unable to add quote: " + e.getMessage());
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

    public static List<QuoteEntry> getQuotesForServer(long guildId)
    {
        List<QuoteEntry> out = new LinkedList<>();

        try
        {
            PreparedStatement s = prepare(GET_QUOTE_FOR_SERVER_ST);
            s.setLong(1, guildId);

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

    public static List<QuoteEntry> getQuotesForUser(long guildId, long userId)
    {
        LinkedList<QuoteEntry> out = new LinkedList<>();

        try
        {
            PreparedStatement s = prepare(GET_QUOTE_FOR_USER_ST);
            s.setLong(1, guildId);
            s.setLong(2, userId);
            ResultSet r = s.executeQuery();

            while(r.next())
            {
                out.add(new QuoteEntry(r));
            }

            r.close();
        }
        catch(SQLException err)
        {
            throw new CmdError("Cannot get quotes: " + err.getMessage());
        }

        return out;
    }

    public static QuoteEntry getQuoteByID(long quoteID)
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
}
