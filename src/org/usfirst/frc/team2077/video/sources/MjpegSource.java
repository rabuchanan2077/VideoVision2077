package org.usfirst.frc.team2077.video.sources;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.PushbackInputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import org.usfirst.frc.team2077.video.Main;
import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;

/**
 * MJPG input from an IP camera.
 * This code has had only limited testing,
 * and only with Axis M1011 and D-Link DCS-930L cameras.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public final class MjpegSource extends AbstractSource implements VideoSource {

    private final URL url_;
    
    private Thread networkThread_;

    /**
     * Reads configuration properties.
     * 
     * @param name Key for configuration properties and {@link Main#getSource}.
     * @throws MalformedURLException If the URL is bad.
     */
    public MjpegSource(String name) throws MalformedURLException {
        
        super(name);
        
        String ip = Main.getProperties().getProperty(name_ + ".ip");
        url_ = new URL(Main.getProperties().getProperty(name_ + ".url", "http://" + ip + "/axis-cgi/mjpg/video.cgi"));
        System.out.println("INFO:" + name_ + ": MJPG stream URL:" + url_);
    }

    @Override
    public synchronized void start() {

        System.out.println( "INFO:" + name_ + ": Opening IP camera connection:" + url_ );

        networkThread_ = new Thread() {
            {
                setDaemon( true );
            }

            @Override
            public void run() {

                System.out.println( "INFO:" + name_ + ": Starting camera connection thread:" + url_ );
                try {

                    // open camera stream
                    PushbackInputStream iis = new PushbackInputStream( url_.openStream(), 1 );
                    StringWriter headerWriter = new StringWriter();
                    String header;
                    boolean haveJpgHeader = false;
                    for ( int b = iis.read(); networkThread_ == this && b != -1; b = iis.read() ) {
                        if ( haveJpgHeader && b == 255 ) {
                            iis.unread( b );
                            
                            
                            //System.out.println(">$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");                            
                            //System.out.println(headerWriter.toString());                            
                            //System.out.println("<$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");                            
                            
                            
                            
                            header = headerWriter.toString().toUpperCase();
                            headerWriter = new StringWriter();
                            haveJpgHeader = false;

                            // read content length from header
                            int indexOfContentLength = header.indexOf( "CONTENT-LENGTH:" );
                            int valueStartPos = indexOfContentLength + "CONTENT-LENGTH:".length();
                            int indexOfEOL = header.indexOf( '\n', indexOfContentLength );
                            String lengthValStr = header.substring( valueStartPos, indexOfEOL ).trim();
                            int contentLength = Integer.parseInt( lengthValStr );

                            // read bytes
                            byte[] buffer = new byte[contentLength];
                            for ( int read = 0; read < buffer.length; ) {
                                read += iis.read( buffer, read, buffer.length - read );
                            }
                            BufferedImage image = ImageIO.read( ImageIO.createImageInputStream( new ByteArrayInputStream( buffer ) ) );
                            
                            BufferedImage image2 = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                            image2.createGraphics().drawImage( image, null, 0, 0 );

                            handleSample(image2);
                        } else {
                            headerWriter.write( b );
                            haveJpgHeader = haveJpgHeader || headerWriter.toString().toUpperCase().indexOf("CONTENT-TYPE: IMAGE/JPEG") >= 0;
                        }
                    }
                } catch ( Exception ex ) {
                    ex.printStackTrace();
                }
                System.out.println("INFO:" + name_ + ": Exiting camera connection thread:" + url_);
            }
        };
        networkThread_.start();
    }

    @Override
    public void stop() {
        System.out.println("INFO:" + name_ + ": Closing IP camera connection:" + url_ );
        networkThread_ = null;
    }

    private void handleSample(BufferedImage image) {

        Main.getFrameCounter().getAndIncrement();

        execBaseTime_ = System.currentTimeMillis();

        if ( resolution_ == null ) {
            resolution_ = new Dimension( image.getWidth(), image.getHeight() );
        }

        //byte[] pixelData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
        //ByteBuffer bb = ByteBuffer.wrap( pixelData );
        int[] pixelData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        ByteBuffer bb = ByteBuffer.allocate(pixelData.length * 4);
        bb.order(Main.getByteOrder());
        bb.asIntBuffer().put(IntBuffer.wrap(pixelData));

        IntBuffer cameraFramePixels = bb.order(Main.getByteOrder()).asIntBuffer();
        synchronized ( views_ ) { // TODO: ??
            for ( RenderedView view : views_ ) {
                // TODO: no processing for non-display JComponents
                processFrame( view, cameraFramePixels );
            }
        }
    }

 }
 