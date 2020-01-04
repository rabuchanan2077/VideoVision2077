package org.usfirst.frc.team2077.vision.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class TrackerUI {

    private static JComponent display = null;
    private static BufferedImage image_;

    private static final long[][] resultsArray_ = {new long[512 / 8], new long[512 / 8]};
    private static final AtomicInteger resultsIndex_ = new AtomicInteger( 0 );

    private static byte[] controlBytes_;
    private static DatagramPacket controlPacket_;
    private static DatagramSocket controlSocket_;

    private static void initializeDisplay( Dimension size ) {

        image_ = new BufferedImage( size.width, size.height, BufferedImage.TYPE_INT_RGB );

        display = new JComponent() {
            private static final long serialVersionUID = 1L;
            long[] results_ = new long[resultsArray_[0].length];
            private final IntUnaryOperator updateIndex_ = new IntUnaryOperator() {
                @Override
                public int applyAsInt( int operand ) {
                    return (operand + 1) % 2;
                }
            };
            AffineTransform scaleTransform_ = AffineTransform.getScaleInstance( 4, 4 );
            int width_;
            int height_;
            Point[] center_ = {new Point(), new Point(), new Point()}; // combined, left, right

            @Override
            public void paintComponent( Graphics g ) {

                ((Graphics2D)g).setTransform( scaleTransform_ );

                int idx = resultsIndex_.getAndUpdate( updateIndex_ );
                synchronized ( resultsArray_[idx] ) {
                    System.arraycopy( resultsArray_[idx], 0, results_, 0, (int)resultsArray_[idx][0] );
                }
                // if ((int)results_[1] != 0)
                // for(int i = 0 ; i < Math.min((int)results_[0], 16); i++) {
                // System.out.print("\t" + results_[i]);
                // }
                // System.out.println();
                int packetType = (int)results_[1];
                if ( packetType == 0 ) { // geometry
                    width_ = (int)results_[3];
                    height_ = (int)results_[4];
                    center_[0] = new Point( (int)results_[5], (int)results_[6] );
                    center_[1] = new Point( (int)results_[7], (int)results_[8] );
                    center_[2] = new Point( (int)results_[9], (int)results_[10] );
                }
                if ( packetType > 0 ) {
                    int chunkBase = packetType - 1;
                    for ( int r = 2; r < results_.length; r++ ) {
                        for ( int b = 0; b < 64; b++ ) {
                            boolean pixel = (results_[r] & (0x01L << b)) != 0;
                            int x = b + (64 * (r - 2));
                            if ( x < width_ ) {
                                image_.setRGB( x, chunkBase, pixel ? 0xFFFFFFFF : 0xFF000000 );
                            }
                        }
                    }
                }
                g.drawImage( image_, 0, 0, null );
                g.setColor( Color.red );
                g.drawLine( center_[0].x, 0, center_[0].x, getHeight() );
                // g.drawLine( 0, center_[0].y, getWidth(), center_[0].y );
                // g.setColor( Color.black );
                g.drawLine( center_[1].x, 0, center_[1].x, getHeight() );
                g.drawLine( 0, center_[1].y, center_[0].x, center_[1].y );
                g.drawLine( center_[2].x, 0, center_[2].x, getHeight() );
                g.drawLine( center_[0].x, center_[2].y, getWidth(), center_[2].y );
            }
        };
        display.setPreferredSize( new Dimension( size.width * 4, size.height * 4 ) );
        JFrame f = new JFrame();
        f.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        f.setContentPane( display );
        f.pack();
        f.setVisible( true );
    }

    private static class RemoteSetting extends Setting {
        public RemoteSetting( String name, int min, int max, int value ) {
            super( name, min, max, value );
        }

        @Override
        public void valueChanged() {
            byte[] bytes;
            try {
                bytes = ("SETTING" + " " + toString()).getBytes( "ISO-8859-1" );
                System.arraycopy( bytes, 0, controlBytes_, 0, bytes.length );
                controlPacket_.setLength( bytes.length );
                controlSocket_.send( controlPacket_ );

            } catch ( Exception ex ) {
                ex.printStackTrace();
            }
        }
    }

// TrackerXIV defaults
//    String[] h = System.getProperty( "hue", "100-140" ).split( "-" );
//    String[] s = System.getProperty( "saturation", "150-255" ).split( "-" );
//    String[] v = System.getProperty( "value", "245-255" ).split( "-" );
    static final Setting hMin = new RemoteSetting( "H min", 0, 255, 100 );
    static final Setting hMax = new RemoteSetting( "H max", 0, 255, 140 );
    static final Setting sMin = new RemoteSetting( "S min", 0, 255, 150 );
    static final Setting sMax = new RemoteSetting( "S max", 0, 255, 255 );
    static final Setting vMin = new RemoteSetting( "V min", 0, 255, 245 );
    static final Setting vMax = new RemoteSetting( "V max", 0, 255, 255 );

    public static void main( String[] args ) throws Exception {

        InetAddress address_ = InetAddress.getByName( System.getProperty( "address", "127.0.0.1" ) );
        int port_ = Integer.parseInt( System.getProperty( "port", "5898" ) );
        controlBytes_ = new byte[512];
        controlPacket_ = new DatagramPacket( controlBytes_, controlBytes_.length, address_, port_ );
        controlSocket_ = new DatagramSocket();

        Setting.showFrame( "HSVFilter Settings", hMin, hMax, sMin, sMax, vMin, vMax );

        (new Thread() {
            @Override
            public void run() {
                byte[] resultsBytes_ = new byte[resultsArray_[0].length * 8];
                LongBuffer resultsBuffer_ = ByteBuffer.wrap( resultsBytes_ ).order( ByteOrder.BIG_ENDIAN ).asLongBuffer();
                try ( DatagramSocket resultsSocket_ = new DatagramSocket( Integer.parseInt( System.getProperty( "port", "5899" ) ) ) ) {
                    DatagramPacket packet = new DatagramPacket( resultsBytes_, resultsBytes_.length );
                    for ( ;; ) {
                        try {
                            resultsSocket_.receive( packet );
                            int idx = resultsIndex_.get();
                            synchronized ( resultsArray_[idx] ) {
                                int length = (int)resultsBuffer_.get( 0 );
                                resultsBuffer_.position( 0 );
                                resultsBuffer_.get( resultsArray_[idx], 0, length );
                                if ( (display == null) && ((int)resultsArray_[idx][1] == 0) ) {
                                    initializeDisplay( new Dimension( (int)resultsArray_[idx][3], (int)resultsArray_[idx][4] ) );
                                }
                            }
                        } catch ( Exception ex ) {
                            ex.printStackTrace();
                        }
                        if ( display != null ) {
                            display.repaint();
                        }
                    }
                } catch ( Exception ex ) {
                }
            }
        }).start();
    }

    public static class Setting {

        public String name_;
        public JLabel nameLabel_;
        public JLabel valueLabel_;
        public AtomicInteger value_;
        public JSlider slider_;
        
        public static Map<String,Setting> map_ = new LinkedHashMap<>();

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
                    valueChanged();
                }
            } );
            map_.put( name_,  this );
        }

        public void valueChanged() {
        }

        @Override
        public String toString() {
            return name_ + "," + value_.get();
        }

        public int value() {
            return value_.get();
        }

        public void setValue( int value ) {
            slider_.setValue( value );
            value_.set( slider_.getValue() );

        }

        public static Map<String, Setting> showFrame( final String title, final Setting... settings ) throws Exception {
             SwingUtilities.invokeAndWait( new Runnable() {
                @Override
                public void run() {
                    JComponent panel = new JPanel( new GridLayout( 0, 3 ) );
                    for ( Setting setting : map_.values() ) {
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
            return map_;
        }
    }
}
