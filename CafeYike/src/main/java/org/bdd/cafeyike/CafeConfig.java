package org.bdd.cafeyike;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Map;

import org.bdd.cafeyike.commander.exceptions.BotError;
import org.bdd.twig.Twig;
import org.bdd.twig.TwigConfig;
import org.bdd.twig.Twig.NameAlign;
import org.bdd.twig.Twig.NameMode;
import org.bdd.twig.branch.StreamBranch;

public class CafeConfig implements TwigConfig
{
    private static final Properties configs = new Properties();
    private static final Map<String, String> env = System.getenv();

    @Override
    public void config()
    {
        try
        {
            FileInputStream is = new FileInputStream(getEnv("YM_SYS_CFG"));
            configs.load(is);
        }
        catch(IOException ex)
        {
            System.out.println("Cannot load config file");
            System.out.println(ex.toString());
            System.exit(0);
        }

        Twig.addBranch(new StreamBranch(System.out, "{event.time} [{event.level}] {event.name} {event.message}\n"));

        String loglevel = getConfig("loglevel");
        Twig.setLevel(loglevel);
        Twig.addBlock("com.sedmelluq");
        Twig.addBlock("net.dv8");
        Twig.setNameMode(NameMode.ClassName);
        Twig.setNameAlign(NameAlign.Center);
    }

    public static String getConfig(String key)
    {
        String val = configs.getProperty(key);
        check(key, val);
        return val;
    }

    private static void check(final String key, final String val)
    {
        if(val == null)
        {
            StringBuilder ss = new StringBuilder();
            ss.append("Config not found: \"").append(key).append("\"");
            throw new BotError(ss.toString());
        }
    }

    public static int getIntConfig(String key)
    {
        String val = configs.getProperty(key);
        check(key, val);
        return Integer.parseInt(val);
    }

    public static String getEnv(String key)
    {
        String val = env.get(key);
        if(val == null)
        {
            StringBuilder ss = new StringBuilder();
            ss.append("Env var not found: \"").append(key).append("\"");
            throw new BotError(ss.toString());
        }

        return val;
    }

}
