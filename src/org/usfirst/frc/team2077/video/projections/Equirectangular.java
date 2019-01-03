package org.usfirst.frc.team2077.video.projections;

import java.awt.Dimension;

import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;

/**
 * Equirectangular projection x = \u03c6 (azimuth), y = \u03b8 (polar).
 * <p>
 * In the global spherical coordinate space, center of the image is at
 * {\u03c6,\u03b8} = {0,\u03c0/2}. For a full spherical image,
 * \u03c6 = -\u03c0 is left, \u03c6 = \u03c0 is right,
 * \u03b8 = 0 is up, and \u03b8 = \u03c0 is down.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class Equirectangular extends AbstractProjection {
    
    private static double DEFAULT_FOV = 360;
    
    /**
     * Source projection instance constructor.
     * @param name Configuration property key.
     * @param videoSource Input video source.
     */
    public Equirectangular(String name, VideoSource videoSource) {
        super(name, videoSource, DEFAULT_FOV);
    }
    
    /**
     * Rendering projection instance constructor.
     * @param name Configuration property key.
     * @param view Output video view.
     */
    public Equirectangular(String name, RenderedView view) {
        super(name, view, DEFAULT_FOV);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Source frames are uniformly scaled (degrees/pixel) to the horizontal field of view
     * (default 360 degrees) of the projection. The vertical FOV is determined by the aspect
     * ratio of the frames.
     */
    @Override
    public double[] sourceProjection(double azimuth, double polar) {
        
        double[] ap = transform(azimuth, polar, rotateY(-90));
        azimuth = ap[0];
        polar = ap[1];

        Dimension resolution = videoSource_.getResolution();
        double aspectRatio = resolution.getWidth() / resolution.getHeight();
        double horizontalFOV = fovAngleHorizontal_;
        double verticalFOV = horizontalFOV / aspectRatio;
        double scale = horizontalFOV/2;

        double x = azimuth; // normal range 0 - 2PI
        double y = polar; // normal range 0 - PI

        x -= Math.PI; // -PI - PI
        y -= Math.PI/2; // -PI/2 - PI/2
        
        x = -x; // projection cartesian convention reverses this sign

        // clip out of range values
        if (Math.abs(x) > Math.min(horizontalFOV/2, Math.PI) || Math.abs(y) > Math.min(verticalFOV/2, Math.PI/2)) {
            return null;
        }

        // normalize to horizontal FOV = range -1 to 1
        x /= scale;
        y /= scale;

        return new double[] {x, y};
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * The horizontal field of view (default 360 degrees) of the projection is scaled to fill
     * the width of the rendering. If the vertical FOV is unspecified, it is scaled to the same
     * degrees/pixel as the horizontal centerline, and the rendering clipped or padded as necessary.
     * If the vertical FOV is specified it is scaled to fill the height of the rendering.
     */
    @Override
    public double[] renderingProjection(double x, double y) {

        double aspectRatio = bounds_.getWidth() / bounds_.getHeight();
        double horizontalFOV = fovAngleHorizontal_;
        double verticalFOV = fovAngleVertical_ > 0 ? fovAngleVertical_ : horizontalFOV / aspectRatio;
        double horizontalScale = horizontalFOV / 2;
        double verticalScale = aspectRatio * verticalFOV / 2; // explicit vFOV is scaled to fit height
              
        double azimuth = x; // normal range -1 - 1
        double polar = y; // normal range -1/aspect - 1/aspect

        // scale to range -hFOV/2 - hFOV/2
        azimuth *= horizontalScale;
        polar *= verticalScale;
        
        azimuth = -azimuth; // world spherical convention reverses this sign
        
        // clip out of range values
        if (Math.abs(azimuth) > Math.min(horizontalFOV/2, Math.PI) || Math.abs(polar) > Math.min(verticalFOV/2, Math.PI/2)) {
            return null;
        }

        // normalize to spherical range
        azimuth += Math.PI; // 0 - 2PI
        polar += Math.PI/2; // 0 - PI
        
        double[] ap = transform(azimuth, polar, rotateY(90));
        azimuth = ap[0];
        polar = ap[1];
        
        return new double[] {azimuth, polar};
    }
    
}
