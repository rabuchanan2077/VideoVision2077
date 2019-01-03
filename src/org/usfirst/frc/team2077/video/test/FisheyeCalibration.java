package org.usfirst.frc.team2077.video.test;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.usfirst.frc.team2077.vvcommon.Utilities;

/**
 * Generates fisheye lens projection properties based on pixel measurements from an image of a test object.
 * See the javadoc overview page for a short description of how to use this.
 */
public class FisheyeCalibration {

    public static abstract class CameraProjection {

        public final String name_;
        public final double k_;
        public final double error_;
        public final double errorRotation_;
        public final double focalLengthPixels_;

        public CameraProjection( String name, double k, double error, double errorRotation, double focalLengthPixels ) {
            name_ = name;
            k_ = k;
            error_ = error;
            errorRotation_ = errorRotation;
            focalLengthPixels_ = focalLengthPixels;
        }

        @Override
        public String toString() {
            return name_ + (k_ != 0 ? (", K = " + k_) : "") + "\t\t\t" + error_;
        }

        public abstract double fovDegreesToPixels( double degrees );
        
        public abstract CameraProjection test( List<CalibrationPoint> calibrationPoints );

        public static class Sine extends CameraProjection {
            Sine( double k, double error, double errorRotation, double focalLengthPixels ) {
                super( "SineProjection", k, error, errorRotation, focalLengthPixels );
            }

            @Override
            public double fovDegreesToPixels( double degrees ) {
                double angle = Math.toRadians( degrees ) / 2;
                double r = focalLengthPixels_ * k_ * Math.sin( angle / k_ );
                return 2 * r;
            }
            
            @Override
            public CameraProjection test( List<CalibrationPoint> calibrationPoints ) {
                return testSine( calibrationPoints, k_ );
            }
        }

        public static class Tangent extends CameraProjection {
            Tangent( double k, double error, double errorRotation, double focalLengthPixels ) {
                super( "TangentProjection", k, error, errorRotation, focalLengthPixels );
            }

            @Override
            public double fovDegreesToPixels( double degrees ) {
                double angle = Math.toRadians( degrees ) / 2;
                double r = focalLengthPixels_ * k_ * Math.tan( angle / k_ );
                return 2 * r;
            }
            
            @Override
            public CameraProjection test( List<CalibrationPoint> calibrationPoints ) {
                return testTangent( calibrationPoints, k_ );
            }
        }

        public static class Equidistant extends CameraProjection {
            Equidistant( double error, double errorRotation, double focalLengthPixels ) {
                super( "EquidistantProjection", 0, error, errorRotation, focalLengthPixels );
            }

            @Override
            public double fovDegreesToPixels( double degrees ) {
                double angle = Math.toRadians( degrees ) / 2;
                double r = focalLengthPixels_ * angle;
                return 2 * r;
            }
            
            @Override
            public CameraProjection test( List<CalibrationPoint> calibrationPoints ) {
                return testEquidistant( calibrationPoints );
            }
        }
    }

    public static class CalibrationPoint {

        public final double worldZ_;
        public final double worldX_;
        public final double pixelX_;
        public final double pixelY_;

        private static double pixelOriginX_ = 0;
        private static double pixelOriginY_ = 0;

        private static double yawCorrection_ = 0; // degrees
        
        private static double rollCorrection_ = 0; // degrees

        public static void setYawCorrection( double yawCorrection ) {
            yawCorrection_ = yawCorrection;
        }

        public static void setRollCorrection( double rollCorrection ) {
            rollCorrection_ = rollCorrection;
        }

        public CalibrationPoint( double worldZ, double worldX, double pixelX, double pixelY ) {
            worldZ_ = worldZ;
            worldX_ = worldX;
            pixelX_ = pixelX;
            pixelY_ = pixelY;

            if ( worldX_ == 0 ) {
                pixelOriginX_ = pixelX_;
                pixelOriginY_ = pixelY_;
            }
        }

        public double getPixelX() {
            return pixelX_ - pixelOriginX_;
        }

        public double getPixelY() {
            return pixelY_ - pixelOriginY_;
        }

        public double getHorizontalAngle() {
            return Math.toDegrees( Math.atan2( worldX_, worldZ_ ) ) + yawCorrection_;
        }

        public double getRotationAngle() {
            return rollCorrection_;
        }

       @Override
        public String toString() {
            return "" + getHorizontalAngle() + "\t" + getPixelX() + "," + getPixelY();
        }
    }

    public static CameraProjection testSine( List<CalibrationPoint> calibrationPoints, double k ) {

        int n = calibrationPoints.size();
        double[] r = new double[n];
        double sumR = 0;
        double sumPixels = 0;
        for ( int i = 0; i < n; i++ ) {
            CalibrationPoint cp = calibrationPoints.get( i );
            r[i] = (k * Math.sin( Math.toRadians( cp.getHorizontalAngle() / k ) ));
            sumR += Math.abs( r[i] );
            sumPixels += Math.abs( cp.getPixelX() );
        }
        double focalLengthPixels = sumPixels / sumR;
        double errorSum = 0;
        double errorRotationSum = 0;
        for ( int i = 0; i < n; i++ ) {
            CalibrationPoint cp = calibrationPoints.get( i );

            // compute error term relative to raw image
            // double error = focalLengthPixels * r[i] - cp.getPixelX();

            // compute error term relative to perspective re-projection
            double radius1 = r[i];
            double radius2 = cp.getPixelX() / focalLengthPixels;
            if ( (radius1 > k) || (radius1 < -k) || (radius2 > k) || (radius2 < -k) ) {
                return new CameraProjection.Sine( k, 9999, 9999, roundSignificant( focalLengthPixels, 4 ) );
            }
            double angle1 = k * Math.asin( radius1 / k );
            double angle2 = k * Math.asin( radius2 / k );
            double perspective1 = Math.tan( angle1 );
            double perspective2 = Math.tan( angle2 );
            double error = perspective1 - perspective2;

            errorSum += error * error;
            
            double rotation1 = cp.getRotationAngle();
            double rotation2 = Math.atan2( cp.getPixelY(), perspective2 );
            double errorRotation = rotation1 - rotation2;
            
            errorRotationSum += errorRotation * errorRotation;
        }
        return new CameraProjection.Sine( k, roundSignificant( errorSum, 3 ), roundSignificant( errorRotationSum, 3 ), roundSignificant( focalLengthPixels, 4 ) );
    }

    public static CameraProjection testTangent( List<CalibrationPoint> calibrationPoints, double k ) {

        int n = calibrationPoints.size();
        double[] r = new double[n];
        double sumR = 0;
        double sumPixels = 0;
        for ( int i = 0; i < n; i++ ) {
            CalibrationPoint cp = calibrationPoints.get( i );
            r[i] = (k * Math.tan( Math.toRadians( cp.getHorizontalAngle() / k ) ));
            sumR += Math.abs( r[i] );
            sumPixels += Math.abs( cp.getPixelX() );
        }
        double focalLengthPixels = sumPixels / sumR;
        double errorSum = 0;
        double errorRotationSum = 0;
        for ( int i = 0; i < n; i++ ) {
            CalibrationPoint cp = calibrationPoints.get( i );

            // compute error term relative to raw image
            // double error = focalLengthPixels * r[i] - cp.getPixelX();

            // compute error term relative to perspective re-projection
            double radius1 = r[i];
            double radius2 = cp.getPixelX() / focalLengthPixels;
            double angle1 = k * Math.atan( radius1 / k );
            double angle2 = k * Math.atan( radius2 / k );
            double perspective1 = Math.tan( angle1 );
            double perspective2 = Math.tan( angle2 );
            double error = perspective1 - perspective2;

            errorSum += error * error;
            
            double rotation1 = cp.getRotationAngle();
            double rotation2 = Math.atan2( cp.getPixelY(), perspective2 );
            double errorRotation = rotation1 - rotation2;
            
            errorRotationSum += errorRotation * errorRotation;
       }
        return new CameraProjection.Tangent( k, roundSignificant( errorSum, 3 ), roundSignificant( errorRotationSum, 3 ), roundSignificant( focalLengthPixels, 4 ) );
    }

    public static CameraProjection testEquidistant( List<CalibrationPoint> calibrationPoints ) {

        int n = calibrationPoints.size();
        double[] r = new double[n];
        double sumR = 0;
        double sumPixels = 0;
        for ( int i = 0; i < n; i++ ) {
            CalibrationPoint cp = calibrationPoints.get( i );
            r[i] = Math.toRadians( cp.getHorizontalAngle() );
            sumR += Math.abs( r[i] );
            sumPixels += Math.abs( cp.getPixelX() );
        }
        double focalLengthPixels = sumPixels / sumR;
        double errorSum = 0;
        double errorRotationSum = 0;
        for ( int i = 0; i < n; i++ ) {
            CalibrationPoint cp = calibrationPoints.get( i );

            // compute error term relative to raw image
            // double error = focalLengthPixels * r[i] - cp.getPixelX();

            // compute error term relative to perspective re-projection
            double radius1 = r[i];
            double radius2 = cp.getPixelX() / focalLengthPixels;
            double angle1 = radius1;
            double angle2 = radius2;
            double perspective1 = Math.tan( angle1 );
            double perspective2 = Math.tan( angle2 );
            double error = perspective1 - perspective2;

            errorSum += error * error;
            
            double rotation1 = cp.getRotationAngle();
            double rotation2 = Math.atan2( cp.getPixelY(), perspective2 );
            double errorRotation = rotation1 - rotation2;
            
            errorRotationSum += errorRotation * errorRotation;
        }
        return new CameraProjection.Equidistant( roundSignificant( errorSum, 3 ), roundSignificant( errorRotationSum, 3 ), roundSignificant( focalLengthPixels, 4 ) );
    }

    private static double roundSignificant( double value, int significantDigits ) {
        return new BigDecimal( value ).round( new MathContext( significantDigits ) ).doubleValue();
    }

    public static void main( String[] args ) throws Exception {

        List<Double> kList = new LinkedList<>();
        for ( double x = 2; x > 0; x -= .01 ) {
            double k = 1 / x;
            kList.add( k );
//            System.out.println( "K:" + roundSignificant( k, 4 )
//                + "\tk*sin(Math.PI/2/k):" + roundSignificant( k * Math.sin( Math.PI / 2 / k ), 4 )
//                + "\tk*tan(Math.PI/2/k):" + roundSignificant( k * Math.tan( Math.PI / 2 / k ), 4 )
//                + "\tMath.PI/2:" + roundSignificant( Math.PI / 2, 4 ) );
        }

        String[] inputs = args.length > 0 ? args : new String[] {"resources/calibration/pizza-north-calibration.txt",
                                                                 "resources/calibration/pizza-south-calibration.txt",
                                                                 "resources/calibration/pizza-east-calibration.txt",
                                                                 "resources/calibration/pizza-west-calibration.txt"};

        for ( String input : inputs ) {

            System.out.println( "# source properties calculated by " + FisheyeCalibration.class.getName() + " from " + input );

            String name = "<source>";
            Dimension resolution = null;
            double worldZ = 0;
            double worldWidth = 0; // optional future use, for narrow angle cameras

            List<CalibrationPoint> calibrationPoints = new LinkedList<>();
            // try ( BufferedReader in = new BufferedReader( new FileReader( input ) ) ) {
            try ( BufferedReader in = new BufferedReader( new InputStreamReader( Utilities.getInputStream( input ) ) ) ) {
                for ( String line = in.readLine(); line != null; line = in.readLine() ) {
                    if ( line.startsWith( "#" ) ) {
                        continue; // comment
                    }
                    String[] s = line.split( "\\t" );
                    switch ( s.length ) {
                    case 2:
                        switch ( s[0] ) {
                        case "name": 
                            name = s[1];
                            break;
                        case "resolution": 
                            String[] wh = s[1].split( "x" );
                            resolution = new Dimension( Integer.parseInt( wh[0] ), Integer.parseInt( wh[1] ) );
                            break;
                        case "worldZ":
                            worldZ = Double.parseDouble( s[1] );
                            break;
                        case "worldWidth":
                            worldWidth = Double.parseDouble( s[1] );
                            break;
                        }
                        break;
                    case 3:
                        calibrationPoints.add( new CalibrationPoint( worldZ, Double.parseDouble( s[0] ), Double.parseDouble( s[1] ), Double.parseDouble( s[2] ) ) );
                        break;
                    }
                }
                if ( resolution == null || worldZ == 0 ) {
                        System.out.println( "Missing required properties (resolution or worldZ)." );
                        continue;
                }
            }

            // for ( CalibrationPoint cp : calibrationPoints ) {
            // System.out.println( cp );
            // }
            // System.out.println();

            for ( CalibrationPoint cp : calibrationPoints ) {
                if ( cp.getPixelX() == 0 ) {
                    System.out.println( name + ".camera-fov-center-x\t" + roundSignificant( cp.pixelX_ / resolution.width, 3 ) );
                    System.out.println( name + ".camera-fov-center-y\t" + roundSignificant( cp.pixelY_ / resolution.height, 3 ) );
                }
            }

            double minError = Double.MAX_VALUE;
            double minErrorRotation = Double.MAX_VALUE;
            CameraProjection cameraProjection;
            CameraProjection bestProjection = null;
            double bestYawCorrection = 0;
            double bestRollCorrection = 0;

            for ( double yawCorrectionMagnitude = 0; yawCorrectionMagnitude <= 10; yawCorrectionMagnitude += .1 ) {
                for ( int sign : new int[] {1, -1} ) {
                    double yawCorrection = yawCorrectionMagnitude * sign;
                    CalibrationPoint.setYawCorrection( yawCorrection );
                    for ( double k : kList ) {
                        cameraProjection = testSine( calibrationPoints, k );
                        if ( cameraProjection.error_ < minError ) {
                            minError = cameraProjection.error_;
                            bestProjection = cameraProjection;
                            bestYawCorrection = yawCorrection;
                        }
                        cameraProjection = testTangent( calibrationPoints, k );
                        if ( cameraProjection.error_ < minError ) {
                            minError = cameraProjection.error_;
                            bestProjection = cameraProjection;
                            bestYawCorrection = yawCorrection;
                        }
                    }
                    cameraProjection = testEquidistant( calibrationPoints );
                    if ( cameraProjection.error_ < minError ) {
                        minError = cameraProjection.error_;
                        bestProjection = cameraProjection;
                        bestYawCorrection = yawCorrection;
                    }
                    Collections.reverse( kList );
                    for ( double k : kList ) {
                        cameraProjection = testTangent( calibrationPoints, k );
                        if ( cameraProjection.error_ < minError ) {
                            minError = cameraProjection.error_;
                            bestProjection = cameraProjection;
                            bestYawCorrection = yawCorrection;
                        }
                    }
                }
            }
            CalibrationPoint.setYawCorrection( bestYawCorrection );
            
            
            for ( double rollCorrectionMagnitude = 0; rollCorrectionMagnitude <= 10; rollCorrectionMagnitude += .1 ) {
                for ( int sign : new int[] {1, -1} ) {
                    double rollCorrection = rollCorrectionMagnitude * sign;
                    CalibrationPoint.setRollCorrection( rollCorrection );
                    cameraProjection = bestProjection.test( calibrationPoints );
                    if ( cameraProjection.errorRotation_ < minErrorRotation ) {
                        minErrorRotation = cameraProjection.errorRotation_;
                        bestProjection = cameraProjection;
                        bestRollCorrection = rollCorrection;
                    }
                }
            }
            CalibrationPoint.setRollCorrection( bestRollCorrection );
            
            System.out.println( name + ".projection\t" + "org.usfirst.frc.team2077.video.projections." + bestProjection.name_ );
            if ( bestProjection.k_ > 0 ) {
                System.out.println( name + ".k\t" + roundSignificant( bestProjection.k_, 3 ) );
            }
            double fovAngle = 180;
            double fovPixels = bestProjection.fovDegreesToPixels( fovAngle );
            System.out.println( name + ".camera-fov-diameter\t" + roundSignificant( fovPixels / Math.max( resolution.width, resolution.height ), 3 ) );
            System.out.println( name + ".camera-fov-angle\t" + fovAngle );
            System.out.println( name + ".camera-yaw-correction\t" + bestYawCorrection );
            System.out.println( name + ".camera-roll-correction\t" + bestRollCorrection );
            System.out.println();
        }
    }
}
