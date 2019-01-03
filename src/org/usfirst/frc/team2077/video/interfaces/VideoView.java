package org.usfirst.frc.team2077.video.interfaces;

import java.awt.Dimension;
import java.util.Collection;

import javax.swing.JComponent;

import org.usfirst.frc.team2077.video.Main;

/**
 * Displayable assembly of one or more {@link RenderedView} components. Each video feed in the view is associated with a
 * {@link VideoSource} via a {@link Rendering}, which also defines how the source pixels are to be mapped into the view
 * image via a {@link RenderingProjection}.
 * <p>
 * Views are created by {@link Main} and are required to have
 * a constructor taking parameter (String name),
 * where <code>name</code> is its configuration property prefix.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public interface VideoView {

    // VideoView(String name);

    /**
     * @return Key for configuration properties and {@link Main#getView}.
     */
    String getName();

    /**
     * @return Output resolution.
     */
    Dimension getResolution();

    /**
     * @return JComponent to which video data is rendered.
     */
    JComponent getJComponent();

    /**
     * @return All displayable views including any nested sub-views.
     */
    Collection<VideoView> getViews();

    /**
     * @return True if the view provides memory-mapped video output frames.
     */
    boolean isMapped();
}
