package org.usfirst.frc.team2077.vision;

import java.util.function.Consumer;

import edu.wpi.first.networktables.EntryNotification;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;


public class NTMain extends org.usfirst.frc.team2077.vision.Main {

    public static NetworkTableInstance networkTable_;

    public static void main( String[] args ) {

        init( args );

        networkTable_ = NetworkTableInstance.getDefault();
        networkTable_.startClient(properties_.getProperty("network-tables-server", "127.0.0.1"));
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                networkTable_.stopClient();
            }
        });
        
        NetworkTableEntry visionView = networkTable_.getEntry("VisionView");
        visionView.addListener(new Consumer<EntryNotification>() {
            @Override
            public void accept(EntryNotification en) {
                VisionView view = views_.get(en.getEntry().getString(null));
                visionFrame_.setContentPane(view.getJComponent());
                if (!visionFrame_.isVisible()) {
                    visionFrame_.pack();
                    visionFrame_.setVisible(true);
                }
                visionFrame_.revalidate();
                visionFrame_.repaint();
            }
        }, -1);
        
        String viewName = visionView.getString(null);
        VisionView view = views_.get(viewName);
        if (view != null) {
            visionFrame_.setContentPane(view.getJComponent());
            if (!visionFrame_.isVisible()) {
                visionFrame_.pack();
                visionFrame_.setVisible(true);
            }
            visionFrame_.revalidate();
            visionFrame_.repaint();
        }
        else {
            (new Thread() {
                public void run() {

                    for ( ;; ) {
                        try {
                            Thread.sleep( 10 * 1000 );
                        } catch ( Exception ex ) {
                        }
                    }
                }
            }).start();
        }
    }
}
