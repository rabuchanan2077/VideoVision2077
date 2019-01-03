package org.usfirst.frc.team2077.video.test;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;

import org.usfirst.frc.team2077.video.Main;
import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;
import org.usfirst.frc.team2077.video.sources.AbstractSource;
import org.usfirst.frc.team2077.vvcommon.Utilities;

/**
 * Test source using a fixed image.
 * Feeds a single  static image repeatedly at a specified frame rate.
 * <p>
 * Configuration properties (in addition to those read by {@link AbstractSource}):
 * <dl>
 * <dt>&lt;source&gt;.image</dt>
 * <dd>Path to the image file. Must be a format understood by javax.imageio.ImageIO.
 * The path may specify either an external file or a resource in java.class.path.
 * The image must be in a format readable by javax.imageio.ImageIO.</dd>
 * <dt>&lt;source&gt;.frames-per-second</dt>
 * <dd>Rate at which frame data is refreshed. The default is .25 (one frame every four seconds).</dd>
 * </dl>
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public final class ImageSource extends AbstractSource implements VideoSource {

    private final BufferedImage image_;
    private final double fps_;

    private Timer timer_ = null;

    /**
     * Reads configuration properties.
     * 
     * @param name Key for configuration properties and {@link Main#getSource}.
     * @throws IOException If the image can't be read.
     */
    public ImageSource( String name ) throws IOException {

        super( name );

        String path = Main.getProperties().getProperty( name_ + ".image" );
        BufferedImage image;
        try ( InputStream in = Utilities.getInputStream( path ) ) {
            image = ImageIO.read( in );
        }
        image_ = new BufferedImage( image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB );
        image_.createGraphics().drawImage( image, null, 0, 0 );

        fps_ = Double.parseDouble( Main.getProperties().getProperty( name_ + ".frames-per-second", ".25" ) );
    }

    @Override
    public synchronized void start() {

        System.out.println( "INFO: Starting ImageSource " + name_ + "." );
        stop();
        timer_ = new Timer( name_, true );
        timer_.schedule( new TimerTask() {
            @Override
            public void run() {
                handleSample( image_ );
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
