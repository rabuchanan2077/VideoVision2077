package org.usfirst.frc.team2077.video.test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

/**
 * Generates a 360 degree equirectangular test image of an HSL color space.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class HSL360ImageGenerator {

    public static void main( String[] args ) throws Exception {

        int w = 3600;
        int h = w / 2;
        BufferedImage image = new BufferedImage( w*3, h*3, BufferedImage.TYPE_INT_RGB );
        for ( int x = 0; x < image.getWidth(); x++ ) {
            double hue = (x%w) / (float)w - .5;
            for ( int y = 0; y < image.getHeight(); y++ ) {
                double lightness = y/h==1 ? (1 - (y%h)/(double)h) : ((y%h)/(double)h);
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
                if (y%(h/12) == 1) y += h/12 - 4;
            }
        }
        for ( int x = 0; x < image.getWidth(); x++ ) {
            for ( int y = 0; y < image.getHeight(); y++ ) {
                image.setRGB( x, y, image.getRGB(x,y)^0x00FFFFFF );
            }
            if (x%(w/24) == 1) x += w/24 - 4;
        }
        ImageIO.write( image.getSubimage( w, h, w, h ), "PNG", new File( "src/resources/test/HSL-Equirectangular.png" ) );
        //ImageIO.write( image, "PNG", new File( "HSL-Equirectangular.png" ) );
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
