package org.usfirst.frc.team2077.video.projections;

import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;

/**
 * Orthographic fisheye projection r = f * sin(\u03b8).
 * 
 * <p>\u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class Orthographic extends SineProjection {

    /**
     * Source projection instance constructor.
     * @param name Configuration property key.
     * @param videoSource Input video source.
     */
    public Orthographic(String name, VideoSource videoSource) {
        super(name, videoSource, 1);
    }
    
    /**
     * Rendering projection instance constructor.
     * @param name Configuration property key.
     * @param view Output video view.
     */
    public Orthographic(String name, RenderedView view) {
        super(name, view, 1);
    }
}
