package org.bdd.cafeyike;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.bdd.twig.Twig;
import org.bdd.twig.TwigConfig;
import org.bdd.twig.branch.FileBranch;
import org.bdd.twig.branch.StreamBranch;

public class CafeConfig implements TwigConfig
{
    private static final Properties configs = new Properties();

    @Override
    public void config()
    {
        try(FileInputStream is = new FileInputStream("system.config"))
        {
            configs.load(is);
        }
        catch(IOException ex)
        {
            System.out.println("Cannot load config file");
            System.exit(0);
        }

        String logdest = configs.getProperty("LOG_DEST");
        switch(logdest.toLowerCase())
        {
        case "console":
            Twig.addBranch(new StreamBranch(System.out,
                    "{event.time} [{color.level}{event.level}{color.end}] {event.name} {event.message}\n"));
            break;
        case "file":
            try
            {
                Twig.addBranch(new FileBranch("bot.log", false, "{event.time} [{event.level}] {event.message}\n"));
            }
            catch(FileNotFoundException err)
            {
                System.out.println("CafeConfig: Unable to open log file");
                throw new RuntimeException();
            }

            break;
        }

        String loglevel = configs.getProperty("LOG_LVL");
        Twig.setLevel(loglevel);
        Twig.addBlock("com.sedmelluq");
        Twig.addBlock("net.dv8");

    }

    public static String getConfig(String key)
    {
        return configs.getProperty(key);
    }

    public static int getIntConfig(String key)
    {
        return Integer.parseInt(configs.getProperty(key));
    }

}
