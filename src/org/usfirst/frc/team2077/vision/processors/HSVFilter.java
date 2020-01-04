package org.usfirst.frc.team2077.vision.processors;

import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.usfirst.frc.team2077.vision.Main.FrameProcessor;

public class HSVFilter implements FrameProcessor {

    private final Map<String, Setting> settings_ = Setting.initializeSettings( "HSVFilter Settings",
        new Setting( "H min", 0, 255, 18 ),
        new Setting( "H max", 0, 255, 49 ),
        new Setting( "S min", 0, 255, 148 ),
        new Setting( "S max", 0, 255, 255 ),
        new Setting( "V min", 0, 255, 106 ),
        new Setting( "V max", 0, 255, 255 ),
        new Setting( "R min", 0, 255, 20 ),
        new Setting( "R max", 0, 255, 100 ),
        new Setting( "Threshold", 0, 255, 100 ) );

    Mat rgb = new Mat();
    Mat hsv = new Mat();
    Mat gray = new Mat();
    Mat tmp = new Mat();

    @Override
    public void processFrame( Mat frameMat, Mat overlayMat ) {

        long t0 = System.currentTimeMillis();

        Imgproc.cvtColor( frameMat, rgb, Imgproc.COLOR_BGRA2RGB );
        Imgproc.cvtColor( rgb, hsv, Imgproc.COLOR_RGB2HSV );

        Imgproc.GaussianBlur( hsv, hsv, new Size( 5, 5 ), 1 );
        // Core.inRange(hsv, new Scalar(27, 75, 120), new Scalar(31, 255, 255), hsv);
        Core.inRange( hsv, new Scalar( settings_.get( "H min" ).value(), settings_.get( "S min" ).value(), settings_.get( "V min" ).value() ),
                      new Scalar( settings_.get( "H max" ).value(), settings_.get( "S max" ).value(), settings_.get( "V max" ).value() ), gray );

        // Imgproc.GaussianBlur(gray, gray, new Size(20, 20), 1);
        Imgproc.GaussianBlur( gray, gray, new Size( 25, 25 ), 0, 0 );

        // Imgproc.cvtColor(hsv, rgb, Imgproc.COLOR_HSV2RGB);
        // Imgproc.cvtColor(rgb, tmp, Imgproc.COLOR_RGB2BGRA);
        // tmp.copyTo( frameMat, gray );

        Imgproc.cvtColor( gray, frameMat, Imgproc.COLOR_GRAY2BGRA );

        Mat circles = new Mat();

        double cannyThreshhold = 10;
        int[] circleSizeRange = {settings_.get( "R min" ).value(), settings_.get( "R max" ).value()};
        int circleProximity = 2 * circleSizeRange[0];
        double circleThreshold = settings_.get( "Threshold" ).value();
        int accumulatorScale = 2;

        // gray 8-bit, single-channel, grayscale input image.
        // circles Output vector of found circles. Each vector is encoded as a 3-element floating-point vector
        // (x,y,radius) .
        // method Detection method, see cv::HoughModes. Currently, the only implemented method is HOUGH_GRADIENT
        // accumulatorScale Inverse ratio of the accumulator resolution to the image resolution. For example, if dp=1 ,
        // the accumulator has the same resolution as the input image. If dp=2 , the accumulator has half as big width
        // and height.
        // circleProximity Minimum distance between the centers of the detected circles. If the parameter is too small,
        // multiple neighbor circles may be falsely detected in addition to a true one. If it is too large, some circles
        // may be missed.
        // cannyThreshhold First method-specific parameter. In case of CV_HOUGH_GRADIENT , it is the higher threshold of
        // the two passed to the Canny edge detector (the lower one is twice smaller).
        // circleThreshold Second method-specific parameter. In case of CV_HOUGH_GRADIENT , it is the accumulator
        // threshold for the circle centers at the detection stage. The smaller it is, the more false circles may be
        // detected. Circles, corresponding to the larger accumulator values, will be returned first.
        // circleSizeRange[0] Minimum circle radius.
        // circleSizeRange[1] Maximum circle radius.
        Imgproc.HoughCircles( gray, circles, Imgproc.CV_HOUGH_GRADIENT, accumulatorScale, circleProximity, cannyThreshhold, circleThreshold, circleSizeRange[0], circleSizeRange[1] );

        // Imgproc.Canny(gray, gray, Math.max(1, cannyThreshhold/2), cannyThreshhold); // approximates internal
        // HoughCircles processing
        Imgproc.cvtColor( gray, frameMat, Imgproc.COLOR_GRAY2BGRA );

        System.out.println( "#rows " + circles.rows() + " #cols " + circles.cols() );
        for ( int i = 0; i < Math.min( 5, circles.cols() ); i++ ) {
            double[] data = circles.get( 0, i );

            double x = data[0];
            double y = data[1];
            int r = (int)data[2];

            Point center = new Point( x, y );
            System.out.println( "CIRCLE " + i + " " + center + " " + r );

            // circle center
            // Imgproc.circle( frameMat, center, 3, new Scalar(0,255,0,128), 3);
            // Imgproc.circle( overlayMat, center, 3, new Scalar(0,255,0,128), 3);
            // circle outline
            Imgproc.circle( frameMat, center, r, new Scalar( 0, 0, 255, 64 ), 5 - i );
            Imgproc.circle( overlayMat, center, r, new Scalar( 0, 0, 255, 128 ), 10 - (2 * i) );
        }

        System.out.println( " processFrame time " + (System.currentTimeMillis() - t0) + "ms" );
    }

    public static class Setting {

        public String name_;
        public JLabel nameLabel_;
        public JLabel valueLabel_;
        public AtomicInteger value_;
        public JSlider slider_;

        public Setting( String name, int min, int max, int value ) {
            name_ = name;
            value_ = new AtomicInteger( value );
            nameLabel_ = new JLabel( name );
            valueLabel_ = new JLabel( "" + value_.get() );
            slider_ = new JSlider( min, max, value );
            slider_.addChangeListener( new ChangeListener() {
                @Override
                public void stateChanged( ChangeEvent e ) {
                    value_.set( slider_.getValue() );
                    valueLabel_.setText( "" + value_.get() );
                }
            } );
        }

        public int value() {
            return value_.get();
        }

        public static Map<String, Setting> initializeSettings( final String title, final Setting... settings ) {
            Map<String, Setting> map = new HashMap<>();
            try {
                SwingUtilities.invokeAndWait( new Runnable() {
                    @Override
                    public void run() {
                        JComponent panel = new JPanel( new GridLayout( 0, 3 ) );
                        for ( Setting setting : settings ) {
                            map.put( setting.name_, setting );
                            panel.add( setting.nameLabel_ );
                            panel.add( setting.slider_ );
                            panel.add( setting.valueLabel_ );
                        }
                        JFrame controlFrame = new JFrame( title );
                        controlFrame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
                        controlFrame.setAlwaysOnTop( true );
                        controlFrame.setContentPane( panel );
                        controlFrame.pack();
                        controlFrame.setVisible( true );
                    }
                } );
            } catch ( Exception ex ) {
                // TODO: complain
            }
            return map;
        }
    }
}
