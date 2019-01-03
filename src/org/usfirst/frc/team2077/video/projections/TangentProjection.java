package org.usfirst.frc.team2077.video.projections;

import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;

/**
 * Projections based on r = k * f * tan(\u03b8/k).
 * <p>
 * These include two classical projections and approach a third:
 * <ul>
 * <li>Rectilinear Perspective (k = 1)</li>
 * <li>Stereographic Fisheye (k = 2)</li>
 * <li>Equidistant Fisheye (k = \u221E)</li>
 * </ul>
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class TangentProjection extends AbstractProjection {
    
    private static double DEFAULT_FOV = 55;
    
    /**
     * Source projection instance constructor.
     * @param name Configuration property key.
     * @param videoSource Input video source.
     */
    public TangentProjection(String name, VideoSource videoSource) {
        super(name, videoSource, DEFAULT_FOV, 1);
    }   
    
    /**
     * Source projection instance constructor.
     * @param name Configuration property key.
     * @param videoSource Input video source.
     * @param defaultFov Horisontal FOV unless specified in a property.
     * @param k Projection K value.
     */
    protected TangentProjection(String name, VideoSource videoSource, double defaultFov, double k) {
        super(name, videoSource, defaultFov, k, k);
    }
    
    /**
     * Rendering projection instance constructor.
     * @param name Configuration property key.
     * @param view Output video view.
     */
    public TangentProjection(String name, RenderedView view) {
        super(name, view, DEFAULT_FOV, 1);
    }

    /**
     * Rendering projection instance constructor.
     * @param name Configuration property key.
     * @param view Output video view.
     * @param defaultFOV Horizontal FOV unless specified in a property.
     * @param k Fixed projection K value.
     */
    protected TangentProjection(String name, RenderedView view, double defaultFOV, double k) {
        super(name, view, defaultFOV, k, k);
    }
    
    @Override
    public double forwardProjection(double angle) {
        if (angle < 0 || angle/k_ >= Math.PI/2) return -1;
        return k_ * Math.tan(angle / k_);
    }
    
    @Override
    public double backProjection(double radius) {
        if (radius < 0) return -1;
        return k_ * Math.atan2(radius, k_);
    }

}
