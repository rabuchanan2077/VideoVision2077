package org.usfirst.frc.team2077.video.test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import org.usfirst.frc.team2077.video.projections.*;

/**
 * Generates a color image of a simulated floor grid for use in birdseye projection testing.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class FloorPlaneImageGenarator {

    public static void main( String[] args ) throws Exception {

        int w = 4096;
        int h = 4096;
        BufferedImage image = new BufferedImage( w, h, BufferedImage.TYPE_INT_RGB );
        //double z0 = 256;
        for ( int x = 0; x < image.getWidth(); x++ ) {
            double x0 = x - w/2;
            for ( int y = 0; y < image.getHeight(); y++ ) {
                double y0 = y - h/2;
                //double[] rap = AbstractProjection.transformCartesianToSpherical(-y0, x0, z0);
                //double hue = rap[1] / Math.PI/2;
                //double lightness = rap[2]/Math.PI;
                double[] ra = AbstractProjection.transformCartesianToPolar(x0, y0);
                double hue = ra[1] / Math.PI/2;
                double lightness = ra[0]/w;
                 int[] rgb = hslToRgb( hue, 1f, lightness );
                image.setRGB( x, y, (new Color( rgb[0], rgb[1], rgb[2] )).getRGB() );
            }
        }
//        Graphics2D g = image.createGraphics();
//        g.setColor( new Color(128,128,128) );
//        for ( int x = 0; x < 24*3; x++ ) {
//            for ( int y = 0; y < 12*3; y++ ) {
//                if ((x+y)%2 == 0) {
//                    g.fillRect( x*w/24, y*h/12, w/24, h/12 );
//                }
//            }
//        }
        for ( int x = 0; x < image.getWidth(); x++ ) {
            for ( int y = 0; y < image.getHeight(); y++ ) {
                image.setRGB( x, y, image.getRGB(x,y)^0x00FFFFFF );
                if (y%(h/32) == 1) y += h/32 - 4;
            }
        }
        for ( int x = 0; x < image.getWidth(); x++ ) {
            for ( int y = 0; y < image.getHeight(); y++ ) {
                image.setRGB( x, y, image.getRGB(x,y)^0x00FFFFFF );
            }
            if (x%(w/32) == 1) x += w/32 - 4;
        }
        ImageIO.write( image, "PNG", new File( "src/resources/test/HSL-FloorPlane.png" ) );
    }

    private static int[] hslToRgb( double h, double s, double l ) {

        while ( h < 0 )
            h += 1;
        while ( h > 1 )
            h -= 1;
        s = Math.max( 0, Math.min( 1, s ) );
        l = Math.max( 0, Math.min( 1, l ) );

        double c = (1 - Math.abs( 2 * l - 1 )) * s;
        double h6 = h * 6;
        double x = c * (1 - Math.abs( (h6 % 2) - 1 ));
        double[] rgb = {0, 0, 0};
        if ( h6 >= 0 && h6 <= 1 ) {
            rgb = new double[] {c, x, 0};
        } else if ( h6 >= 1 && h6 <= 2 ) {
            rgb = new double[] {x, c, 0};
        } else if ( h6 >= 2 && h6 <= 3 ) {
            rgb = new double[] {0, c, x};
        } else if ( h6 >= 3 && h6 <= 4 ) {
            rgb = new double[] {0, x, c};
        } else if ( h6 >= 4 && h6 <= 5 ) {
            rgb = new double[] {x, 0, c};
        } else if ( h6 >= 5 ) {
            rgb = new double[] {c, 0, x};
        }
        double m = l - c / 2;
        rgb = new double[] {rgb[0] + m, rgb[1] + m, rgb[2] + m};
        return new int[] {Math.max( 0, Math.min( 255, (int)Math.round( 256 * rgb[0] ) ) ),
                          Math.max( 0, Math.min( 255, (int)Math.round( 256 * rgb[1] ) ) ),
                          Math.max( 0, Math.min( 255, (int)Math.round( 256 * rgb[2] ) ) )};
    }

}
