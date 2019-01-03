package org.usfirst.frc.team2077.video.projections;

import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;

/**
 * Projections based on r = k * f * sin(\u03b8/k).
 * <p>
 * These include two classical projections and approach a third:
 * <ul>
 * <li>Orthographic Fisheye (k = 1)</li>
 * <li>Equisolid Fisheye (k = 2)</li>
 * <li>Equidistant Fisheye (k = \u221E)</li>
 * </ul>
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class SineProjection extends AbstractProjection {
    
    private static double DEFAULT_FOV = 180;
    
    /**
     * Source projection instance constructor.
     * @param name Configuration property key.
     * @param videoSource Input video source.
     */
    public SineProjection(String name, VideoSource videoSource) {
        super(name, videoSource, DEFAULT_FOV, 1);
    }
    
    /**
     * Source projection instance constructor.
     * @param name Configuration property key.
     * @param videoSource Input video source.
     * @param k Fixed projection K value.
     */
    protected SineProjection(String name, VideoSource videoSource, double k) {
        super(name, videoSource, DEFAULT_FOV, k, k);
    }
    
    /**
     * Rendering projection instance constructor.
     * @param name Configuration property key.
     * @param view Output video view.
     */
    public SineProjection(String name, RenderedView view) {
        super(name, view, DEFAULT_FOV, 1);
    }

    /**
     * Rendering projection instance constructor.
     * @param name Configuration property key.
     * @param view Output video view.
     * @param k Projection K value.
     */
   protected SineProjection(String name, RenderedView view, double k) {
        super(name, view, DEFAULT_FOV, k, k);
    }
    
    @Override
    public double forwardProjection(double angle) {
        if (angle < 0 || angle/k_ > Math.PI/2 || angle > Math.PI*2) return -1;
        return k_ * Math.sin(angle / k_);
    }
    
    @Override
    public double backProjection(double radius) {
        if (radius < 0 || (radius / k_) > 1) return -1;
        return k_ * Math.asin(radius / k_);
    }

}
