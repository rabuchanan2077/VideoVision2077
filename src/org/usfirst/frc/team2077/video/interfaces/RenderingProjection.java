package org.usfirst.frc.team2077.video.interfaces;

/**
 * Forward projection and coordinate transform math for video rendering.
 * The purpose of the rendering projection is to present the video data in the desired form
 * for presentation to either the human viewers or downstream processing. Wide angle, fisheye,
 * and/or multiple cameras may provide large field of view coverage, but significantly
 * distort all or part of it. In combination with correct source projections configured to
 * provide normalized spherical image data, well chosen rendering projections can present
 * the most important parts of the overall field in the most usable form for the intended
 * purpose. Sources that are normalized to a common global coordinate space may also be
 * rendered into a single view to "stitch" multiple feeds into a single panorama, birdseye,
 * or other projection, even if the cameras that produce them are not perfectly matched.
 * <p>
 * While the interface defines several methods, its core is {@link #renderingProjection(double, double)},
 * which describes the pure projection function by mapping from normalized cartesian projection space
 * to the spherical world coordinates produced by the SourceProjection of the video source.
 * Other functions may typically be handled in a common base class.
 * <p>
 * Rendering projections are created by {@link Rendering}s and are required to have
 * a constructor taking parameters (String name, RenderedView view),
 * where <code>name</code> is its configuration property prefix and <code>view</code>
 * is the containing RenderedView.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public interface RenderingProjection {
    
    // RenderingProjection(String name, RenderedView view);

    /**
     * Maps a point in view image coordinates to rendering image space.
     * @param viewPixelX Horizontal image coordinate in range 0 to width from left.
     * @param viewPixelY Vertical image coordinate in range 0 to width from top.
     * @return {
     * <br> renderingPixelX (Horizontal image coordinate in range 0 to width from left),
     * <br> renderingPixelY (Vertical image coordinate in range 0 to width from top)
     * <br>}.
     */
    double[] transformViewToRendering(double viewPixelX, double viewPixelY);
    
    /**
     * Maps a point in rendering image coordinates to normalized rendering projection space.
     * @param renderingPixelX Horizontal image coordinate in range 0 to width from left.
     * @param renderingPixelY Vertical image coordinate in range 0 to width from top.
     * @return {
     * <br> x (Horizontal projection space coordinate in range -1 to 1),
     * <br> y (Vertical projection space coordinate on same scale)
     * <br>}.
     */
    double[] transformPixelToCartesian(double renderingPixelX, double renderingPixelY);
    
    /**
     * Maps normalized source projection coordinates to source image coordinates.
     * Maps a point in normalized projection coordinates to view-relative spherical vector.
     * @param x Horizontal projection space coordinate in range -1 to 1.
     * @param y Vertical projection space coordinate on same scale.
     * @return {
     * <br> azimuth (Viewer-relative spherical azimuth angle in radians clockwise from upward),
     * <br> polar (Viewer-relative spherical normal angle in radians outward from view axis)
     * <br>}.
     */
    double[] renderingProjection(double x, double y);

    /**
     * Maps source-relative spherical view vector to world-relative spherical coordinates
     * as supplied by source.
     * World coordinate space is defined by the video source, and is relative to either
     * the view axis of the source itself or to a global upward-looking axis.
     * @see SourceProjection#transformGlobalWorldToSource
     * @see SourceProjection#transformNominalWorldToSource
     * @param azimuth Viewer-relative spherical azimuth angle in radians clockwise from upward.
     * @param polar Viewer-relative spherical normal angle in radians outward from view axis.
     * @return {
     * <br> azimuth (Viewer-relative spherical azimuth angle in radians clockwise from upward),
     * <br> polar (Viewer-relative spherical normal angle in radians outward from view axis)
     * <br>}.
     */
    double[] transformRenderingToWorld(double azimuth, double polar);
    
    /**
     * Global source coordinate flag.
     * @return True if the rendering projection expects the source to use global (vs source-relative) coordinates.
     */
    boolean isGlobal();

}
