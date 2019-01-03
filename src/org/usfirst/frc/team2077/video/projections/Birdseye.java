package org.usfirst.frc.team2077.video.projections;

import org.usfirst.frc.team2077.video.Main;
import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;

/**
 * "Bird's Eye" projection.
 * <p>
 * ...
 * <p>
 * \u00A9 2018
 * 
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class Birdseye extends AbstractProjection {

    private final double projectionWidth_;
    private final double projectionOriginX_;
    private final double projectionOriginY_;
    private final double projectionOriginZ_;

    /**
     * Source projection instance constructor.
     * 
     * @param name Configuration property key.
     * @param videoSource Input video source.
     */
    public Birdseye( String name, VideoSource videoSource ) {
        super( name, videoSource );

        projectionWidth_ = Double.parseDouble( Main.getProperties().getProperty( name_ + ".projection-width", "240.0" ) ); // camera-width?
        double scale = 2 / projectionWidth_; // scale width to horizontal FOV

        projectionOriginX_ = -cameraOriginXYZ_[0] * scale;
        projectionOriginY_ = -cameraOriginXYZ_[1] * scale;
        projectionOriginZ_ = cameraOriginXYZ_[2] * scale;
    }

    /**
     * Rendering projection instance constructor.
     * 
     * @param name Configuration property key.
     * @param view Output video view.
     */
    public Birdseye( String name, RenderedView view ) {
        super( name, view );

        projectionWidth_ = Double.parseDouble( Main.getProperties().getProperty( name_ + ".projection-width", "240.0" ) );
        double scale = 2 / projectionWidth_; // scale width to horizontal FOV

        projectionOriginX_ = scale * Double.parseDouble( Main.getProperties().getProperty( name_ + ".projection-origin-x", "0" ) );
        projectionOriginY_ = scale * Double.parseDouble( Main.getProperties().getProperty( name_ + ".projection-origin-y", "0" ) );
        projectionOriginZ_ = scale * Double.parseDouble( Main.getProperties().getProperty( name_ + ".projection-origin-z", "24.0" ) );
    }

    @Override
    public double[] renderingProjection( double x, double y ) {

        double projectionX = (x - projectionOriginX_);// * projectionWidth_/2;
        double projectionY = (y - projectionOriginY_);// * projectionWidth_/2;
        double projectionZ = -projectionOriginZ_;

        double[] rap = AbstractProjection.transformCartesianToSpherical( projectionY, projectionX, projectionZ );

        return new double[] {rap[1], rap[2]};
    }

    @Override
    public double[] sourceProjection( double azimuth, double polar ) {

        double[] xyz = AbstractProjection.transformSphericalToCartesian( 1, azimuth, polar );
        double scale = -projectionOriginZ_ / xyz[2];
        xyz[0] *= scale;
        xyz[1] *= scale;

        xyz[0] += projectionOriginX_;
        xyz[1] += projectionOriginY_;

        if ( xyz[2] > 0 ) {
            return null;
        }

        return new double[] {xyz[1], xyz[0]};
    }

}
