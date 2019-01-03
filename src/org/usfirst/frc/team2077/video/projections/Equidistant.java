package org.usfirst.frc.team2077.video.projections;

import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;

/**
 * Equidistant fisheye projection r = f * \u03b8.
 * 
 * <p>\u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class Equidistant extends AbstractProjection {
    
    private static double DEFAULT_FOV = 180;
    
    /**
     * Source projection instance constructor.
     * @param name Configuration property key.
     * @param videoSource Input video source.
     */
    public Equidistant(String name, VideoSource videoSource) {
        super(name, videoSource, 0, 0);
    }
    
    /**
     * Rendering projection instance constructor.
     * @param name Configuration property key.
     * @param view Output video view.
     */
    public Equidistant(String name, RenderedView view) {
        super(name, view, DEFAULT_FOV, 1);
    }
    
    @Override
    public double forwardProjection(double angle) {
        if (angle < 0 || angle > Math.PI) return -1;
        return angle;
    }
    
    @Override
    public double backProjection(double radius) {
        if (radius < 0 || radius > Math.PI) return -1;
        return radius;
    }
}
