package org.usfirst.frc.team2077.video.projections;

import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;

/**
 * Rectilinear perspective "pinhole camera" projection r = f * tan(\u03b8).
 * 
 * <p>\u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class Perspective extends TangentProjection {
    
    private static double DEFAULT_FOV = 55;

    /**
     * Source projection instance constructor.
     * @param name Configuration property key.
     * @param videoSource Input video source.
     */
    public Perspective(String name, VideoSource videoSource) {
        super(name, videoSource, DEFAULT_FOV, 1);
    }

    /**
     * Rendering projection instance constructor.
     * @param name Configuration property key.
     * @param view Output video view.
     */
    public Perspective(String name, RenderedView view) {
        super(name, view, DEFAULT_FOV, 1);
    }
}
