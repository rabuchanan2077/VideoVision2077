package org.usfirst.frc.team2077.video.interfaces;

/**
 * Back projection and coordinate transform math for video input. The purpose of the source projection is to normalize
 * input to common world space spherical coordinates regardless of camera lens geometry. This permits merging of video
 * data from multiple cameras (even with differing lens types) and independent specification of viewing projections.
 * <p>
 * While the interface defines several methods, its core is {@link #sourceProjection(double, double)}, which describes
 * the pure lens function by mapping from spherical world coordinates to normalized cartesian projection space. Other
 * functions may typically be handled in a common base class.
 * <p>
 * Source projections are created by {@link VideoSource}s and are required to have a constructor taking parameters
 * (String name, VideoSource videoSource), where <code>name</code> is its configuration property prefix and
 * <code>videoSource</code> is the calling VideoSource.
 * <p>
 * \u00A9 2018
 * 
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public interface SourceProjection {

    // SourceProjection(String name, VideoSource videoSource);

    /**
     * Maps global spherical view vector to source-relative spherical coordinates.
     *
     * @param azimuth World-relative spherical azimuth angle in radians clockwise from upward.
     * @param polar World-relative spherical polar angle in radians outward from view axis.
     * @return { <br>
     *         azimuth (Image-relative spherical azimuth angle in radians clockwise from upward), <br>
     *         polar (Image-relative spherical normal angle in radians outward from view axis) <br>
     *         }.
     */
    double[] transformGlobalWorldToSource( double azimuth, double polar );

    /**
     * Maps nominal source-relative spherical view vector to source-relative spherical coordinates.
     *
     * @param azimuth Nominal source-relative spherical azimuth angle in radians clockwise from upward.
     * @param polar Nominal source spherical polar angle in radians outward from view axis.
     * @return { <br>
     *         azimuth (Image-relative spherical azimuth angle in radians clockwise from upward), <br>
     *         polar (Image-relative spherical normal angle in radians outward from view axis) <br>
     *         }.
     */
    double[] transformNominalWorldToSource( double azimuth, double polar );

    /**
     * Maps source-relative spherical view vector to normalized source projection coordinates.
     *
     * @param azimuth Viewer-relative spherical azimuth angle in radians clockwise from upward.
     * @param polar Viewer-relative spherical polar angle in radians outward from view axis.
     * @return { <br>
     *         x (Horizontal projection space coordinate in range -1 to 1), <br>
     *         y (Vertical projection space coordinate on same scale). <br>
     *         }.
     */
    double[] sourceProjection( double azimuth, double polar );

    /**
     * Maps a point in normalized source projection coordinates to source image space.
     *
     * @param x Horizontal projection space coordinate in range -1 to 1.
     * @param y Vertical projection space coordinate on same scale.
     * @return { <br>
     *         pixelX, (Horizontal image coordinate in range 0 to width from left). <br>
     *         pixelY (Vertical image coordinate in range 0 to width from top). <br>
     *         }
     */
    double[] transformCartesianToPixel( double x, double y );

}
