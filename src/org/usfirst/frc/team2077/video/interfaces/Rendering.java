package org.usfirst.frc.team2077.video.interfaces;

/**
 * Rendered video feed from one source to one view.
 * <p>
 * A {@link RenderedView} may contain multiple renderings, each of which is an association between the view
 * and a single {@link VideoSource}. The rendering defines a mapping from pixels in the view's image back to
 * the pixels its source video frames from which they are to be populated. Whenever a new frame from arrives
 * from the source, it is used to update the whichever pixels in the view image are mapped from its rendering.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public interface Rendering {
    
    /**
     * @return Key for configuration properties.
     */
    String getName();
    
    /**
     * @return Video view of which this rendering is a part.
     */
    RenderedView getView();
    
    /**
     * @return Video source that supplies this rendering.
     */
    VideoSource getVideoSource();
    
    /**
     * Computes a table mapping view image pixels to source frame pixels.
     * @return Array {viewIndex0,sourceIndex0,viewIndex1,sourceIndex1... viewIndexN,sourceIndexN}.
     */
    int[] getMap();
}
