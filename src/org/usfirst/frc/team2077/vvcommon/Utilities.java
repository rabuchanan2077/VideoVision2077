package org.usfirst.frc.team2077.vvcommon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Utilities {

    private Utilities() {}
    
    public static InputStream getInputStream(String path) throws IOException {
        File file = new File(path);
        if (file.exists() && file.isFile() && file.canRead()) {
            return new FileInputStream(file);
        }
        else {
            return ClassLoader.getSystemResourceAsStream(path);
        }
    }
    
    public static Properties readProperties(String[] args) {

        Properties properties = new Properties();
        for (String arg : args) {
            try {
                InputStream propertiesInputStream;
                File propertiesFile = new File(arg);
                if (propertiesFile.exists() && propertiesFile.isFile() && propertiesFile.canRead()) {
                    propertiesInputStream = new FileInputStream(propertiesFile);
                    // TODO: log
                }
                else {
                    propertiesInputStream = ClassLoader.getSystemResourceAsStream("resources/" + arg + ".properties");
                    // TODO: log
                }
                properties.load(propertiesInputStream);
                propertiesInputStream.close();
            } catch (Exception ex) {
                System.out.println("WARNING:" + "Exception loading video/vision configuration from " + arg + ".");
                ex.printStackTrace(System.out);
            }
        }
        if (properties.isEmpty()) {
            System.out.println("WARNING:" + "No external video/vision configuration loaded, using built-in defaults.");
            try {
                properties.load(ClassLoader.getSystemResourceAsStream("resources/vv.properties"));

            } catch (Exception ex) {
                System.out.println("WARNING:" + "Exception loading default video/vision configuration.");
                ex.printStackTrace(System.out);
            }
        }
        if (properties.isEmpty()) {
            System.out.println("SEVERE:" + "No video/vision configuration loaded, exiting.");
            System.exit(1);
        }
        return properties;
    }
    
}
