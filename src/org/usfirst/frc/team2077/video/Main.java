package org.usfirst.frc.team2077.video;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.*;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.usfirst.frc.team2077.video.core.DefaultView;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;
import org.usfirst.frc.team2077.video.interfaces.VideoView;
import org.usfirst.frc.team2077.video.sources.GstreamerSource;
import org.usfirst.frc.team2077.vvcommon.Utilities;

/**
 * Main class to read configuration. construct sources, views, and renderings, and start the video.
 * <p>
 * The system configuration file(s) are in java properties (String key/value) format,
 * and should contain at minimum one source and one view, keyed as "source0" and "view0".
 * Additional sources and views are tagged "source1", "source2", etc and "view1", etc.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class Main {

    public static final Logger logger_ = Logger.getLogger(Main.class.getName());

    private static Properties properties_;

    private static ByteOrder byteOrder_;

    private static AtomicInteger frames_ = new AtomicInteger();

    private static Map<String, VideoSource> sources_ = new LinkedHashMap<>();
    private static Map<String, VideoView> views_ = new LinkedHashMap<>();
    private static Collection<VideoView> mappedViews_ = new HashSet<>();

    /**
     * @param args List of configuration property paths. Each path may be either an external
     * file path or a resource name where "resources/" + name + ".properties" is in java.class.path.
     */
    public static void main(String[] args) {
        
        properties_ = Utilities.readProperties(args);
         
        String bo = properties_.getProperty("byte-order");
        // TODO: tested only for LE
        byteOrder_ = "BE".equalsIgnoreCase(bo) ? ByteOrder.BIG_ENDIAN : "LE".equalsIgnoreCase(bo) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.nativeOrder();
        // TODO: check BufferedImage byte order
        
        initSources();
        
        initViews();
        
        startSources();

        // frame rate monitor
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                logger_.log(Level.INFO, "FPS:" + frames_.getAndSet(0) / 10);
            }
        }, 10000, 10000);

        final JFrame videoFrame_ = new JFrame("Video");
        videoFrame_.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        videoFrame_.addWindowListener(new WindowAdapter() {
            @Override
			public void windowClosing(WindowEvent we) {
                for (VideoSource vs : sources_.values()) {
                    vs.stop();
                }
            }
        });
        
        JToggleButton button0 = null;
        JComponent panel = new JPanel(new GridLayout(0,2));
        ButtonGroup group = new ButtonGroup();
        for (VideoView v : views_.values()) {
            final VideoView view = v;
            JToggleButton button = new JToggleButton(new AbstractAction(properties_.getProperty(view.getName() + ".label", view.getName())) {
                private static final long serialVersionUID = 1L;
                @Override
                public void actionPerformed(ActionEvent ae) {
                    videoFrame_.setContentPane(view.getJComponent());
                    videoFrame_.revalidate();
                    videoFrame_.repaint();
                    Set<VideoView> views = new HashSet<>();
                    for (VideoView view : view.getViews()) {
                        views.add(view);
                    }
                    views.addAll(mappedViews_);
                    for (VideoSource source : sources_.values()) {
                        source.activateViews(views);
                    }
                }
            });
            group.add(button);
            panel.add(button);
            if (button0 == null) {
                button0 = button;
            }
        }

        if (button0 != null) {
            if (views_.size() > 1) {
                final JFrame controlFrame_ = new JFrame("Video Selector");
                controlFrame_.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                controlFrame_.setAlwaysOnTop(true);
                controlFrame_.setContentPane(panel);
                controlFrame_.pack();
                controlFrame_.setVisible(true);
            }
             button0.doClick();
        }
        
        videoFrame_.pack();
        videoFrame_.setVisible(true);
    }

    private static void initSources() {

        // input video sources
        for (int i = 0;; i++) {
            String name = properties_.getProperty("video" + i);
            if (name == null) {
                break;
            }
            try {
                Class<?> sourceClass = Class.forName(properties_.getProperty(name + ".class", GstreamerSource.class.getName()));
                sources_.put(name, (VideoSource)sourceClass.getDeclaredConstructor(String.class).newInstance(name));
            } catch (Exception ex) {
                logger_.log(Level.WARNING, "Video source " + name + " initialization failed.", ex);
            }
        }
        if (sources_.isEmpty()) {
            logger_.log(Level.SEVERE, "No valid video sources loaded, exiting.");
            System.exit(1);
        }
    }

    private static void initViews() {

        // display views
        for (int i = 0;; i++) {
            String name = properties_.getProperty("view" + i);
            if (name == null) {
                break;
            }
            try {
                Class<?> viewClass = Class.forName(properties_.getProperty(name + ".class", DefaultView.class.getName()));
                VideoView view = (VideoView)viewClass.getDeclaredConstructor(String.class).newInstance(name);
                views_.put(name, view);
                if (view.isMapped()) {
                    mappedViews_.add(view);
                }
            } catch (Exception ex) {
                logger_.log(Level.WARNING, "View " + name + " initialization failed.", ex);
            }
        }
        if (views_.isEmpty()) {
            logger_.log(Level.SEVERE, "No views configured, exiting.");
            System.exit(1);
        }
    }

    private static void startSources() {

        // start video sources
        for (VideoSource vs : sources_.values()) {
            vs.start(); // TODO: start on first use?
            vs.activateViews(mappedViews_);
        }
    }

    public static Properties getProperties() {
        return properties_;
    }
    
    public static ByteOrder getByteOrder() {
        return byteOrder_;
    }
   
    public static AtomicInteger getFrameCounter() {
        return frames_;
    }
   
    public static VideoView getView(String name) {
        return views_.get( name );
    }

    public static VideoSource getSource(String name) {
        return sources_.get( name );
    }
}
