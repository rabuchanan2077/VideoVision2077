package org.usfirst.frc.team2077.video.projections;

import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;

/**
 * 3x2 cubemap format similar to one used by a major social media publisher.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class CubeMap3x2 extends AbstractProjection {
    
    private static double DEFAULT_FOV = 360;
    
    /**
     * Source projection instance constructor.
     * @param name Configuration property key.
     * @param videoSource Input video source.
     */
    public CubeMap3x2(String name, VideoSource videoSource) {
        super(name, videoSource, DEFAULT_FOV);
    }
    
    /**
     * Rendering projection instance constructor.
     * @param name Configuration property key.
     * @param view Output video view.
     */
    public CubeMap3x2(String name, RenderedView view) {
        super(name, view, DEFAULT_FOV);
    }
    
    private static double[][][] faceToGlobal_ = new double[6][][];
    private static double[][][] globalToFace_ = new double[6][][];
    static {
        double[] polar = {90, 90, 0, 180, 90, 90};
        double[] heading = {90, 270, 0, 0, 0, 180};
        for (int i = 0; i < 6; i++) {
            globalToFace_[i] = multiply(rotateZ(-heading[i]), rotateY(-polar[i]));
            faceToGlobal_[i] = rotateXYZ(0, polar[i], heading[i]);
        }
    }
   
    /**
     * {@inheritDoc}
     * <p>
     * Divides the projection space into a 3x2 grid, with each element a perspective projection
     * of a 90\u00B0 x 90\u00B0 cubewise subdivision of a sphere.
     */
    @Override
    public double[] sourceProjection(double azimuth, double polar) {
    
        double[] originX = { -2/3., 0, 2/3., -2/3., 0, 2/3.};
        double[] originY = { -1/3., -1/3., -1/3., 1/3., 1/3., 1/3. };
        
        double focalLength = 1 / Math.tan(Math.PI/4);
        
        int nearestFace = 0;
        double nearestRadius = Integer.MAX_VALUE;
        double[] nearestXY = {0,0};
        
        for (int i = 0; i < 6; i++ ) {
            double[] ap = transform(azimuth, polar, faceToGlobal_[i]);
            double azimuth1 = ap[0];
            double polar1 = ap[1];
            
            if (polar1 >= Math.PI/2) {
                continue;
            }

            double radius = Math.tan(polar1);
            
            radius *= focalLength;
            
            if (radius < nearestRadius) {
                nearestFace = i;
                nearestRadius = radius;
                nearestXY = transformPolarToCartesian(radius, azimuth1);
            }
       }
        
        double x = nearestXY[0];
        double y = nearestXY[1];
        x /= 3.;
        y /= 3.;
        x += originX[nearestFace];
        y += originY[nearestFace];

        return new double[] {x, y};
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Divides the projection space into a 3x2 grid, with each element a perspective projection
     * of a 90\u00B0 x 90\u00B0 cubewise subdivision of a sphere.
     */
    @Override
    public double[] renderingProjection(double x, double y) {
    
        double[] columnX = {-1, -1/3., 1/3., 1};
        double[] rowY = {-2/3., 0, 2/3.};
        
        double focalLength = 1 / Math.tan(Math.PI/4);

        int column = -1;
        for ( int c = 0; c < 3; c++ ) {
            if ( x >= columnX[c] && x <= columnX[c + 1] ) {
                column = c;
            }
        }
        int row = -1;
        for ( int r = 0; r < 2; r++ ) {
            if ( y >= rowY[r] && y <= rowY[r + 1] ) {
                row = r;
            }
        }
        
        if (column == -1 || row == -1 || Math.abs(y) > bounds_.getHeight()/bounds_.getWidth()) {
                return null;
        }
        
        double faceX = (x - (columnX[column]+columnX[column+1])/2) * 3;
        double faceY = (y - (rowY[row]+rowY[row+1])/2) * 3;
        int face = column + row*3;
        
        double[] ra = transformCartesianToPolar(faceX, faceY);
        double radius = ra[0];
        double azimuth = ra[1];
        radius /= focalLength;
        double polar = Math.atan2(radius, 1);
        double[] ap = transform(azimuth, polar, globalToFace_[face]);
        azimuth = ap[0];
        polar = ap[1];
        
        return new double[] {azimuth, polar};
    }
    
}
