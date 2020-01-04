package org.usfirst.frc.team2077.video;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.usfirst.frc.team2077.video.interfaces.VideoSource;
import org.usfirst.frc.team2077.video.interfaces.VideoView;

import edu.wpi.first.networktables.EntryNotification;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

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
public class NTMain extends Main {

    private static NetworkTableInstance networkTable_;

    /**
     * @param args List of configuration property paths. Each path may be either an external
     * file path or a resource name where "resources/" + name + ".properties" is in java.class.path.
     */
    public static void main(String[] args) {
        
        init(args);
        
        networkTable_ = NetworkTableInstance.getDefault();
        networkTable_.startClient(properties_.getProperty("network-tables-server", "127.0.0.1"));
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                networkTable_.stopClient();
            }
        });
        NetworkTableEntry videoView = networkTable_.getEntry("VideoView");
        videoView.addListener(new Consumer<EntryNotification>() {
            @Override
            public void accept(EntryNotification en) {
                VideoView view = views_.get(en.getEntry().getString(null));
                if (view != null) {
                    videoFrame_.setContentPane( view.getJComponent() );
                    videoFrame_.revalidate();
                    videoFrame_.repaint();
                    Set<VideoView> views = new HashSet<>();
                    for ( VideoView v : view.getViews() ) {
                        views.add( v );
                    }
                    views.addAll( mappedViews_ );
                    for ( VideoSource source : sources_.values() ) {
                        source.activateViews( views );
                    }
                }
            }
        }, -1);
        
        String viewName = videoView.getString(views_.keySet().iterator().next());
        VideoView view = views_.get(viewName);
        if (view != null) {
            videoFrame_.setContentPane(view.getJComponent());
            videoFrame_.revalidate();
            videoFrame_.repaint();
            Set<VideoView> views = new HashSet<>();
            for (VideoView v : view.getViews()) {
                views.add(v);
            }
            views.addAll(mappedViews_);
            for (VideoSource source : sources_.values()) {
                source.activateViews(views);
            }
        }
        
        videoFrame_.pack();
        videoFrame_.setVisible(true);
    }
}
