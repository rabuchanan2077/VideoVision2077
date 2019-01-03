package org.usfirst.frc.team2077.video.test;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;

import org.usfirst.frc.team2077.video.Main;
import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;
import org.usfirst.frc.team2077.video.sources.AbstractSource;
import org.usfirst.frc.team2077.vvcommon.MappedFrameInfo;

/**
 * Test source using a mapped view to simulate camera input.
 * Intended primarily for static test images, as performance may be insufficient for realistic video.
 * <p>
 * Configuration properties (in addition to those read by {@link AbstractSource}):
 * <dl>
 * <dt>&lt;source&gt;.view</dt>
 * <dd>View name. The view must be memory-mapped.</dd>
 * <dt>&lt;source&gt;.frames-per-second</dt>
 * <dd>Rate at which video frames are forwarded. The default is .25 (one frame every four seconds).</dd>
 * </dl>
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public final class MappedViewSource extends AbstractSource implements VideoSource {
    
    private final String frameInfoPath_;
    
    private RandomAccessFile videoFile_;
    private FileChannel videoFileChannel_;
    private ByteBuffer videoMappedBuffer_;
    //private final ByteBuffer videoByteBuffer_;
    private BufferedImage videoImage_;

    private final double fps_;

    private Timer timer_ = null;

    /**
     * Reads configuration properties.
     * 
     * @param name Key for configuration properties and {@link Main#getSource}.
     */
    public MappedViewSource(String name) {
        
        super(name);

        String viewName = Main.getProperties().getProperty(name_ + ".view");
        frameInfoPath_ = Main.getProperties().getProperty(viewName + ".frame-info").replaceAll("/", Matcher.quoteReplacement(File.separator));

        fps_ = Double.parseDouble(Main.getProperties().getProperty(name_ + ".frames-per-second", ".25"));
     }

    @Override
    public synchronized void start() {

        System.out.println("INFO: Starting MappedViewSource " + name_ + "." );
        stop();
        timer_ = new Timer(name_, true);
        timer_.schedule( new TimerTask() {
            public void run() {
                try {
                    if (videoFile_ == null) {
                        try {
                            ObjectInputStream in = new ObjectInputStream(new FileInputStream(frameInfoPath_));
                            MappedFrameInfo frameInfo = (MappedFrameInfo)in.readObject();
                            in.close();
                            ByteOrder byteOrder = frameInfo.byteOrder_ == "BE" ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
                            Dimension resolution = frameInfo.resolution_;
                            videoFile_ = new RandomAccessFile(new File(frameInfo.frameFile_), "rw");
                            videoFileChannel_ = videoFile_.getChannel();
                            videoMappedBuffer_ = videoFileChannel_.map(MapMode.READ_ONLY, 0, resolution.width * resolution.height * 4).order(byteOrder);
                            //videoByteBuffer_ = ByteBuffer.allocate(resolution.width * resolution.height * 4).order(byteOrder);
                            videoImage_ = new BufferedImage(resolution.width, resolution.height, BufferedImage.TYPE_INT_RGB);
                            Runtime.getRuntime().addShutdownHook(new Thread() {
                                public void run() {
                                    try {
                                        videoFile_.close();
                                    } catch (Exception ex) {
                                    }
                                }
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            return;
                        }
                    }
                    
                    FileLock lock = videoFileChannel_.lock(); // keep source from writing while we're reading
                    videoMappedBuffer_.rewind();
                    int[] pixels = ((DataBufferInt) videoImage_.getRaster().getDataBuffer()).getData();
                    videoMappedBuffer_.asIntBuffer().get(pixels); // byte (int) array to java image buffer
                    lock.release();
                    handleSample(videoImage_);
                } catch (Exception ex) {
                }
            }
        }, 0, (long)Math.round(1000/fps_));
    }

    @Override
    public void stop() {
               
        if (timer_ != null) {
            System.out.println("INFO: Stopping MappedViewSource " + name_ + "." );
            timer_.cancel();
            timer_ = null;
        }
    }

    private void handleSample(BufferedImage image) {

        Main.getFrameCounter().getAndIncrement();

        execBaseTime_ = System.currentTimeMillis();

        if ( resolution_ == null ) {
            resolution_ = new Dimension( image.getWidth(), image.getHeight() );
        }

        // TODO: may not need to do all this depending on BufferedImage type
        int[] pixelData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        ByteBuffer bb = ByteBuffer.allocate(pixelData.length * 4);
        bb.order(Main.getByteOrder());
        bb.asIntBuffer().put(IntBuffer.wrap(pixelData));

        IntBuffer cameraFramePixels = bb.order(Main.getByteOrder()).asIntBuffer();
        synchronized ( views_ ) {
            for ( RenderedView view : views_ ) {
                // TODO: no processing for non-display JComponents
                processFrame( view, cameraFramePixels );
            }
        }
    }

 }
