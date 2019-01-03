package org.usfirst.frc.team2077.video.interfaces;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.util.Collection;

/**
 * Displayable or exportable view directly comprised of {@link Rendering} objects. Each rendering manages the projection
 * of pixel data from one {@link VideoSource}.
 * <p>
 * Views are created by {@link org.usfirst.frc.team2077.video.Main} and are required to have a constructor taking
 * parameter (String name), where <code>name</code> is its configuration property prefix.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public interface RenderedView extends VideoView {

    // RenderedView(String name);

    /**
     * @return All renderings in this view.
     */
    Collection<Rendering> getRenderings();

    /**
     * @return Color coded mask image. This image is used to allocate pixels in the view to one and only one rendering
     *         in cases where multiple renderings in the view may overlap. Each rendering is assigned its own mask-color
     *         property, and will update only those view pixels whose coordinates correspond to a matching color in the
     *         mask image.
     */
    BufferedImage getLayoutMask();

    /**
     * @param sourcePixels Pixel data from the latest frame update from a video source feeding this view.
     * @param renderingSourceMap A list of pixel index pairs mapping source frame pixels to view pixels.
     */
    void processFrame( IntBuffer sourcePixels, int[] renderingSourceMap );

    /**
     * @return Indicates view is in use and needs to receive source frame updates
     */
    boolean isLive();

    /**
     * @param live Marks view as in use and needing to receive source frame updates.
     */
    void setLive( boolean live );

}
