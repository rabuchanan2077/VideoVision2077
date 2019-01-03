package org.usfirst.frc.team2077.video.projections;

import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;

/**
 * Equisolid fisheye projection r = 2 * f * sin(\u03b8/2).
 * 
 * <p>\u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class Equisolid extends SineProjection {

    /**
     * Source projection instance constructor.
     * @param name Configuration property key.
     * @param videoSource Input video source.
     */
    public Equisolid(String name, VideoSource videoSource) {
        super(name, videoSource, 2);
    }

    /**
     * Rendering projection instance constructor.
     * @param name Configuration property key.
     * @param view Output video view.
     */
    public Equisolid(String name, RenderedView view) {
        super(name, view, 2);
    }
}
