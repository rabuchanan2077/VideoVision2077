package org.usfirst.frc.team2077.video.projections;

import org.usfirst.frc.team2077.video.interfaces.VideoSource;

/**
 * Projections based on r = k * f * atan(\u03b8/k).
 * <p>
 * Not a standard lens projection, but may be useful for correcting camera barrel distortion.
 * Not usable as a RenderingProjection.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class InverseTangentProjection extends AbstractProjection {
    
    private static double DEFAULT_FOV = 55;
    
    /**
     * Source projection instance constructor.
     * @param name Configuration property key.
     * @param videoSource Input video source.
     */
    public InverseTangentProjection(String name, VideoSource videoSource) {
        super(name, videoSource, DEFAULT_FOV, 1);
    }
    
    @Override
    public double forwardProjection(double angle) {
        if (angle < 0) return -1;
        return k_ * Math.atan2(angle, k_);
    }
}
