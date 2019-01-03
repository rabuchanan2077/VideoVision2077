package org.usfirst.frc.team2077.video.interfaces;

import java.awt.Dimension;
import java.util.Collection;

import org.usfirst.frc.team2077.video.Main;

/**
 * Video input stream.
 * <p>
 * Sources are created by {@link org.usfirst.frc.team2077.video.Main} and are required to have a constructor taking
 * parameter (String name), where <code>name</code> is its configuration property prefix.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public interface VideoSource {

    // VideoSource(String name);

    /**
     * @return Key for configuration properties and {@link Main#getSource}.
     */
    String getName();

    /**
     * @return Source image resolution, based on the first received frame. Null if no video frames have been received.
     */
    Dimension getResolution();

    /**
     * @return Source projection.
     */
    SourceProjection getProjection();

    /**
     * Turns on video feed to views.
     * @param views Views to which this source should supply video frames.
     */
    void activateViews( Collection<VideoView> views );

    /**
     * Tells this source to start supplying video frames.
     */
    void start();

    /**
     * Tells this source to stop supplying video frames.
     */
    void stop();

}
