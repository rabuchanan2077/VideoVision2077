package org.usfirst.frc.team2077.video.test;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.usfirst.frc.team2077.video.Main;
import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;
import org.usfirst.frc.team2077.video.sources.AbstractSource;
import org.usfirst.frc.team2077.vvcommon.Utilities;

/**
 * Test source using a fixed GIF image.
 * Feeds image image frames at a specified frame rate.
 * Work in progress, not ready for use.
 * <p>
 * Configuration properties (in addition to those read by {@link AbstractSource}):
 * <dl>
 * <dt>&lt;source&gt;.image</dt>
 * <dd>Path to the image file. Must be a format understood by javax.imageio.ImageIO. The path may specify either an
 * external file or a resource in java.class.path. The image must be in a format readable by javax.imageio.ImageIO.</dd>
 * <dt>&lt;source&gt;.frames-per-second</dt>
 * <dd>Rate at which frame data is refreshed. The default is .25 (one frame every four seconds).</dd>
 * </dl>
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public final class GIFImageSource extends AbstractSource implements VideoSource {

    private final BufferedImage[] frames_;
    private final double fps_;

    private int frameIndex_ = 0;

    private Timer timer_ = null;

    public GIFImageSource( String name ) throws Exception {

        super( name );

        String path = Main.getProperties().getProperty( name_ + ".image" );

        System.out.println( path );

        BufferedImage[] frames = null;
        try {
            ImageReader reader = ImageIO.getImageReadersByFormatName( "gif" ).next();
            System.out.println( reader );
            // File input = new File(path);
            // System.out.println(input.exists());
            ImageInputStream stream = ImageIO.createImageInputStream( Utilities.getInputStream( path ) );
            System.out.println( stream );
            reader.setInput( stream );
            int count = reader.getNumImages( true );
            System.out.println( count );
            frames = new BufferedImage[count];
            int w = 0;
            int h = 0;
            for ( int i = 0; i < count; i++ ) {
                BufferedImage f = reader.read( i );
                if ( i == 0 ) {
                    w = f.getWidth();
                    h = f.getHeight();
                }
                frames[i] = new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );
                for ( int j = 0; j < i; j++ ) {
                    frames[i].createGraphics().drawImage( frames[j], null, 0, 0 );
                }
                frames[i].createGraphics().drawImage( f, null, 0, 0 );
            }
        } catch ( IOException ex ) {
        }

        frames_ = frames;

        fps_ = Double.parseDouble( Main.getProperties().getProperty( name_ + ".frames-per-second", ".25" ) );
    }

    @Override
    public synchronized void start() {

        System.out.println( "INFO: Starting GIFImageSource " + name_ + "." );
        stop();
        timer_ = new Timer( name_, true );
        timer_.schedule( new TimerTask() {
            @Override
            public void run() {
                handleSample( frames_[frameIndex_] );
                frameIndex_ = (frameIndex_ + 1) % frames_.length;
            }
        }, 0, Math.round( 1000 / fps_ ) );
    }

    @Override
    public void stop() {

        if ( timer_ != null ) {
            System.out.println( "INFO: Stopping ImageSource " + name_ + "." );
            timer_.cancel();
            timer_ = null;
        }
    }

    private void handleSample( BufferedImage image ) {

        Main.getFrameCounter().getAndIncrement();

        execBaseTime_ = System.currentTimeMillis();

        if ( resolution_ == null ) {
            resolution_ = new Dimension( image.getWidth(), image.getHeight() );
        }

        int[] pixelData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        ByteBuffer bb = ByteBuffer.allocate( pixelData.length * 4 );
        bb.order( Main.getByteOrder() );
        bb.asIntBuffer().put( IntBuffer.wrap( pixelData ) );

        IntBuffer cameraFramePixels = bb.order( Main.getByteOrder() ).asIntBuffer();
        synchronized ( views_ ) {
            for ( RenderedView view : views_ ) {
                // TODO: no processing for non-display JComponents
                processFrame( view, cameraFramePixels );
            }
        }
    }

}
