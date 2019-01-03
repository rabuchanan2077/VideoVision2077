package org.usfirst.frc.team2077.video.core;

import java.awt.Dimension;
import java.util.Arrays;

import org.usfirst.frc.team2077.video.Main;
import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.Rendering;
import org.usfirst.frc.team2077.video.interfaces.RenderingProjection;
import org.usfirst.frc.team2077.video.interfaces.SourceProjection;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;

/**
 * {@link Rendering} implementation.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class DefaultRendering implements Rendering {

    protected final String name_;
    
    protected final RenderedView view_;
    
    protected final VideoSource videoSource_;
    
    protected final RenderingProjection renderingProjection_;
    
    protected final SourceProjection sourceProjection_;

    /**
     * 
     * @param name Key for configuration properties.
     * @param view Parent view.
     */
    public DefaultRendering(String name, RenderedView view) {

        name_ = name;
        view_ = view;
                    
        videoSource_ = Main.getSource(Main.getProperties().getProperty(name_ + ".video"));
        if (videoSource_ == null) {
            System.out.println("SEVERE:" + "No video source for " + name_ + ".");
        }
        sourceProjection_ = videoSource_ != null ? videoSource_.getProjection() : null;
        
        RenderingProjection projection = null;
        try {
            Class<?> projectionClass = Class.forName(Main.getProperties().getProperty(name_ + ".projection", sourceProjection_.getClass().getName()));
            projection = (RenderingProjection)projectionClass.getDeclaredConstructor(String.class, RenderedView.class).newInstance(name_, view_);
        } catch (Exception ex) {
            System.out.println("SEVERE:" + "Projection initialization failed  for " + name_ + ".");
            ex.printStackTrace(System.out);
        }
        renderingProjection_ = projection;
         
    }
    
    @Override
    public String getName() {
        return name_;
    }
    
    @Override
    public RenderedView getView() {
        return view_;
    }
    
    @Override
    public VideoSource getVideoSource() {
        return videoSource_;
    }

    @Override
    public int[] getMap() {
        
        Dimension sourceResolution = videoSource_.getResolution();
        Dimension viewResolution = view_.getResolution();
        
        int[] viewIndexRangeX = {99999, -99999};
        int[] viewIndexRangeY = {99999, -99999};
        double[] viewPixelRangeX = {99999, -99999};
        double[] viewPixelRangeY = {99999, -99999};
        double[] renderingPixelRangeX = {99999, -99999};
        double[] renderingPixelRangeY = {99999, -99999};
        double[] renderingRangeX = {99999, -99999};
        double[] renderingRangeY = {99999, -99999};
        double[] renderingSphericalRange0 = {99999, -99999};
        double[] renderingSphericalRange1 = {99999, -99999};
        double[] worldSphericalRange0 = {99999, -99999};
        double[] worldSphericalRange1 = {99999, -99999};
        double[] sourceSphericalRange0 = {99999, -99999};
        double[] sourceSphericalRange1 = {99999, -99999};
        double[] sourceRangeX = {99999, -99999};
        double[] sourceRangeY = {99999, -99999};
        double[] sourcePixelRangeX = {99999, -99999};
        int[] sourcePixelRangeY = {99999, -99999};

        int[] map = new int[2 * viewResolution.width * viewResolution.height];
        int i = 0;
        for (int viewPixelY = 0; viewPixelY < viewResolution.height; viewPixelY++) {
            for (int viewPixelX = 0; viewPixelX < viewResolution.width; viewPixelX++) {
                
                double[] viewPixel = {viewPixelX+.5, viewPixelY+.5}; // double pixel values are pixel center

                double[] renderingPixel = renderingProjection_.transformViewToRendering(viewPixel[0], viewPixel[1]);
                if (renderingPixel == null) { // TODO: decide which methods may return null
                    continue;
                }
                
                double[] renderingXY = renderingProjection_.transformPixelToCartesian(renderingPixel[0], renderingPixel[1]);
                
                double[] renderingSpherical = renderingProjection_.renderingProjection(renderingXY[0], renderingXY[1]);
                if (renderingSpherical == null) {
                    continue;
                }
                
                double[] worldSpherical = renderingProjection_.transformRenderingToWorld(renderingSpherical[0], renderingSpherical[1]);
                
                double[] sourceSpherical = renderingProjection_.isGlobal()
                        ? sourceProjection_.transformGlobalWorldToSource(worldSpherical[0], worldSpherical[1])
                        : sourceProjection_.transformNominalWorldToSource(worldSpherical[0], worldSpherical[1]);
                
                double[] sourceXY = sourceProjection_.sourceProjection(sourceSpherical[0], sourceSpherical[1]);
                if (sourceXY == null) {
                    continue;
                }
                
                double[] sourcePixel = sourceProjection_.transformCartesianToPixel(sourceXY[0], sourceXY[1]);
                if (sourcePixel == null) {
                    continue;
                }
                int sourcePixelX = (int)Math.round(sourcePixel[0]);
                int sourcePixelY = (int)Math.round(sourcePixel[1]);
                if (sourcePixelX < 0 || sourcePixelX >= sourceResolution.width || sourcePixelY < 0 || sourcePixelY >= sourceResolution.height) {
                    continue;
                }
                
                viewIndexRangeX[0] = Math.min( viewIndexRangeX[0], viewPixelX );
                viewIndexRangeX[1] = Math.max( viewIndexRangeX[1], viewPixelX );
                viewIndexRangeY[0] = Math.min( viewIndexRangeY[0], viewPixelY );
                viewIndexRangeY[1] = Math.max( viewIndexRangeY[1], viewPixelY );
                viewPixelRangeX[0] = Math.min( viewPixelRangeX[0], viewPixel[0] );
                viewPixelRangeX[1] = Math.max( viewPixelRangeX[1], viewPixel[0] );
                viewPixelRangeY[0] = Math.min( viewPixelRangeY[0], viewPixel[1] );
                viewPixelRangeY[1] = Math.max( viewPixelRangeY[1], viewPixel[1] );
                renderingPixelRangeX[0] = Math.min( renderingPixelRangeX[0], renderingPixel[0] );
                renderingPixelRangeX[1] = Math.max( renderingPixelRangeX[1], renderingPixel[0] );
                renderingPixelRangeY[0] = Math.min( renderingPixelRangeY[0], renderingPixel[1] );
                renderingPixelRangeY[1] = Math.max( renderingPixelRangeY[1], renderingPixel[1] );
                renderingRangeX[0] = Math.min( renderingRangeX[0], renderingXY[0] );
                renderingRangeX[1] = Math.max( renderingRangeX[1], renderingXY[0] );
                renderingRangeY[0] = Math.min( renderingRangeY[0], renderingXY[1] );
                renderingRangeY[1] = Math.max( renderingRangeY[1], renderingXY[1] );
                
                renderingSphericalRange0[0] = Math.min( renderingSphericalRange0[0], Math.toDegrees(renderingSpherical[0]) );
                renderingSphericalRange0[1] = Math.max( renderingSphericalRange0[1], Math.toDegrees(renderingSpherical[0]) );
                renderingSphericalRange1[0] = Math.min( renderingSphericalRange1[0], Math.toDegrees(renderingSpherical[1]) );
                renderingSphericalRange1[1] = Math.max( renderingSphericalRange1[1], Math.toDegrees(renderingSpherical[1]) );
                
                worldSphericalRange0[0] = Math.min( worldSphericalRange0[0], Math.toDegrees(worldSpherical[0]) );
                worldSphericalRange0[1] = Math.max( worldSphericalRange0[1], Math.toDegrees(worldSpherical[0]) );
                worldSphericalRange1[0] = Math.min( worldSphericalRange1[0], Math.toDegrees(worldSpherical[1]) );
                worldSphericalRange1[1] = Math.max( worldSphericalRange1[1], Math.toDegrees(worldSpherical[1]) );
                
                sourceSphericalRange0[0] = Math.min( sourceSphericalRange0[0], Math.toDegrees(sourceSpherical[0]) );
                sourceSphericalRange0[1] = Math.max( sourceSphericalRange0[1], Math.toDegrees(sourceSpherical[0]) );
                sourceSphericalRange1[0] = Math.min( sourceSphericalRange1[0], Math.toDegrees(sourceSpherical[1]) );
                sourceSphericalRange1[1] = Math.max( sourceSphericalRange1[1], Math.toDegrees(sourceSpherical[1]) );
               
                
                
                
                
                sourceRangeX[0] = Math.min( sourceRangeX[0], sourceXY[0] );
                sourceRangeX[1] = Math.max( sourceRangeX[1], sourceXY[0] );
                sourceRangeY[0] = Math.min( sourceRangeY[0], sourceXY[1] );
                sourceRangeY[1] = Math.max( sourceRangeY[1], sourceXY[1] );
                sourcePixelRangeX[0] = Math.min( sourcePixelRangeX[0], sourcePixelX );
                sourcePixelRangeX[1] = Math.max( sourcePixelRangeX[1], sourcePixelX );
                sourcePixelRangeY[0] = Math.min( sourcePixelRangeY[0], sourcePixelY );
                sourcePixelRangeY[1] = Math.max( sourcePixelRangeY[1], sourcePixelY );
                
                
                
               
                int viewPixelIndex = (viewPixelY * viewResolution.width) + viewPixelX;
                int sourcePixelIndex = (sourcePixelY * sourceResolution.width) + sourcePixelX;
                
                map[i++] = viewPixelIndex;
                map[i++] = sourcePixelIndex;
            }
        }

        System.out.println( "View Index Range\t" + viewIndexRangeX[0] + " : " + viewIndexRangeX[1] + "\t" + viewIndexRangeY[0] + " : " + viewIndexRangeY[1]);
        System.out.println( "View Pixel Range\t" + viewPixelRangeX[0] + " : " + viewPixelRangeX[1] + "\t" + viewPixelRangeY[0] + " : " + viewPixelRangeY[1]);
        System.out.println( "Rendering Pixel Range\t" + renderingPixelRangeX[0] + " : " + renderingPixelRangeX[1] + "\t" + renderingPixelRangeY[0] + " : " + renderingPixelRangeY[1]);
        System.out.println( "Rendering Range\t" + renderingRangeX[0] + " : " + renderingRangeX[1] + "\t" + renderingRangeY[0] + " : " + renderingRangeY[1]);
        System.out.println( "Rendering Spherical Range\t" + renderingSphericalRange0[0] + " : " + renderingSphericalRange0[1] + "\t" + renderingSphericalRange1[0] + " : " + renderingSphericalRange1[1]);
        System.out.println( "World Spherical Range\t" + worldSphericalRange0[0] + " : " + worldSphericalRange0[1] + "\t" + worldSphericalRange1[0] + " : " + worldSphericalRange1[1]);
        System.out.println( "Source Spherical Range\t" + sourceSphericalRange0[0] + " : " + sourceSphericalRange0[1] + "\t" + sourceSphericalRange1[0] + " : " + sourceSphericalRange1[1]);
        System.out.println( "Source Range\t" + sourceRangeX[0] + " : " + sourceRangeX[1] + "\t" + sourceRangeY[0] + " : " + sourceRangeY[1]);
        System.out.println( "Source Pixel Range\t" + sourcePixelRangeX[0] + " : " + sourcePixelRangeX[1] + "\t" + sourcePixelRangeY[0] + " : " + sourcePixelRangeY[1]);
        
        return Arrays.copyOf(map, i); // discard unused array elements
    }

}
