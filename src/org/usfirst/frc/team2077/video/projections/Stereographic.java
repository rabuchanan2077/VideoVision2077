package org.usfirst.frc.team2077.video.projections;

import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;

/**
 * Stereographic fisheye projection r = 2 * f * tan(\u03b8/2).
 * 
 * <p>\u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class Stereographic extends TangentProjection {
    
    private static double DEFAULT_FOV = 180;

    /**
     * Source projection instance constructor.
     * @param name Configuration property key.
     * @param videoSource Input video source.
     */
    public Stereographic(String name, VideoSource videoSource) {
        super(name, videoSource, DEFAULT_FOV, 2);
    }

    /**
     * Rendering projection instance constructor.
     * @param name Configuration property key.
     * @param view Output video view.
     */
    public Stereographic(String name, RenderedView view) {
        super(name, view, DEFAULT_FOV, 2);
    }
}
