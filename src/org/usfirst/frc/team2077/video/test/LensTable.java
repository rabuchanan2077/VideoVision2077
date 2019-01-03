package org.usfirst.frc.team2077.video.test;

/**
 * Generates a range of view property sets for testing.
 * @author buchanan
 */
public class LensTable {

    public static void main( String[] args ) {
        int i = 0;

        for ( int k2 = -15; k2 <= +10; k2++ ) {
            double angle = Math.toRadians( 90 );
            double k = 1;
            double f;
            double ff = .7;
            if ( k2 == 0 ) {
                f = 1 / angle;
                double a = Math.toDegrees( 1 / ff );
                String v = "View" + i;
                System.out.println( "view" + i++ + "\t" + v );
                System.out.println( v + ".label\t" + k2 + " F=" + f );
                System.out.println( v + ".video\t" + "SOURCE-360" );
                System.out.println( v + ".resolution\t" + "1200x1200" );
                System.out.println( v + ".interpolation\t" + "true" );
                System.out.println( v + ".global\t" + "true" );
                System.out.println( v + ".horizontal-fov\t" + "180" );
                System.out.println( v + ".projection\t" + "org.usfirst.frc.team2077.video.projections.Equidistant" );
                System.out.println( v + ".mask-image\t" + "circle.png" );
                System.out.println( v + ".mask-color\t" + "00FFFFFF" );
                System.out.println();
                v = "View" + i;
                System.out.println( "view" + i++ + "\t" + v );
                System.out.println( v + ".label\t" + k2 + " FOV=" + (2 * a) );
                System.out.println( v + ".video\t" + "SOURCE-360" );
                System.out.println( v + ".resolution\t" + "1200x1200" );
                System.out.println( v + ".interpolation\t" + "true" );
                System.out.println( v + ".global\t" + "true" );
                System.out.println( v + ".focal-length\t" + ff );
                System.out.println( v + ".projection\t" + "org.usfirst.frc.team2077.video.projections.Equidistant" );
                // System.out.println(v + ".mask-image\t" + "circle.png");
                // System.out.println(v + ".mask-color\t" + "00FFFFFF");
                System.out.println();
            } else if ( k2 < 0 ) {
                k = -1 / (k2 / 10.);
                f = 1 / (k * Math.tan( angle / k ));
                double a = Math.toDegrees( k * Math.atan2( (1 / ff), k ) );
                if ( f > .01 ) {
                    String v = "View" + i;
                    System.out.println( "view" + i++ + "\t" + v );
                    System.out.println( v + ".label\t" + k2 + " F=" + f );
                    System.out.println( v + ".video\t" + "SOURCE-360" );
                    System.out.println( v + ".resolution\t" + "1200x1200" );
                    System.out.println( v + ".interpolation\t" + "true" );
                    System.out.println( v + ".global\t" + "true" );
                    System.out.println( v + ".horizontal-fov\t" + "180" );
                    System.out.println( v + ".projection\t" + "org.usfirst.frc.team2077.video.projections.TangentProjection" );
                    System.out.println( v + ".k\t" + k );
                    System.out.println( v + ".mask-image\t" + "circle.png" );
                    System.out.println( v + ".mask-color\t" + "00FFFFFF" );
                    System.out.println();
                    v = "View" + i;
                    System.out.println( "view" + i++ + "\t" + v );
                    System.out.println( v + ".label\t" + k2 + " FOV=" + (2 * a) );
                    System.out.println( v + ".video\t" + "SOURCE-360" );
                    System.out.println( v + ".resolution\t" + "1200x1200" );
                    System.out.println( v + ".interpolation\t" + "true" );
                    System.out.println( v + ".global\t" + "true" );
                    System.out.println( v + ".focal-length\t" + ff );
                    System.out.println( v + ".projection\t" + "org.usfirst.frc.team2077.video.projections.TangentProjection" );
                    System.out.println( v + ".k\t" + k );
                    // System.out.println(v + ".mask-image\t" + "circle.png");
                    // System.out.println(v + ".mask-color\t" + "00FFFFFF");
                    System.out.println();
                }
            } else {
                k = 1 / (k2 / 10.);
                f = 1 / (k * Math.sin( angle / k ));
                double a = Math.toDegrees( k * Math.asin( (1 / ff) / k ) );
                String v = "View" + i;
                System.out.println( "view" + i++ + "\t" + v );
                System.out.println( v + ".label\t" + k2 + " F=" + f );
                System.out.println( v + ".video\t" + "SOURCE-360" );
                System.out.println( v + ".resolution\t" + "1200x1200" );
                System.out.println( v + ".interpolation\t" + "true" );
                System.out.println( v + ".global\t" + "true" );
                System.out.println( v + ".horizontal-fov\t" + "180" );
                System.out.println( v + ".projection\t" + "org.usfirst.frc.team2077.video.projections.SineProjection" );
                System.out.println( v + ".k\t" + k );
                System.out.println( v + ".mask-image\t" + "circle.png" );
                System.out.println( v + ".mask-color\t" + "00FFFFFF" );
                System.out.println();
                v = "View" + i;
                System.out.println( "view" + i++ + "\t" + v );
                System.out.println( v + ".label\t" + k2 + " FOV=" + (2 * a) );
                System.out.println( v + ".video\t" + "SOURCE-360" );
                System.out.println( v + ".resolution\t" + "1200x1200" );
                System.out.println( v + ".interpolation\t" + "true" );
                System.out.println( v + ".global\t" + "true" );
                System.out.println( v + ".focal-length\t" + ff );
                System.out.println( v + ".projection\t" + "org.usfirst.frc.team2077.video.projections.SineProjection" );
                System.out.println( v + ".k\t" + k );
                // System.out.println(v + ".mask-image\t" + "circle.png");
                // System.out.println(v + ".mask-color\t" + "00FFFFFF");
                System.out.println();
            }
            // System.out.println("# K2: " + k2 + "\tK:" + k + "\tF:" + f);

        }

    }
}
