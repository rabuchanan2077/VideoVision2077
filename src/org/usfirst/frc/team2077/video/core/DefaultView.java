package org.usfirst.frc.team2077.video.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.Collection;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import org.usfirst.frc.team2077.video.Main;
import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.Rendering;
import org.usfirst.frc.team2077.video.interfaces.VideoView;
import org.usfirst.frc.team2077.vvcommon.MappedFrameInfo;

/**
 * {@link RenderedView} implementation.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class DefaultView implements RenderedView {

    protected final String name_;

    protected final Dimension resolution_;
    
    protected final BufferedImage layoutMask_;

    protected final BufferedImage frameImage_;
    protected final int[] framePixels_;

    private final BufferedImage overlayImage_;
    private final int[] overlayPixels_;
    
    private final boolean interpolation_;
    
    private final boolean isMapped_;
    
    protected String frameInfoPath_ = null;
    protected RandomAccessFile frameFile_ = null;
    protected FileChannel frameFileChannel_ = null;
    protected ByteBuffer frameMappedBuffer_ = null;
    
    protected RandomAccessFile overlayFile_ = null;
    protected FileChannel overlayFileChannel_ = null;
    protected ByteBuffer overlayMappedBuffer_ = null;
    protected DatagramSocket overlayNotificationSocket_ = null;
    
    protected JComponent jComponent_ = null;
    
    protected final Lock lock_ = new ReentrantLock(true);

    protected final Collection<Rendering> renderings_ = new HashSet<>();

    protected boolean isActive_ = false;
     
    /**
     * @param name Key for configuration properties and {@link Main#getView}.
     */
    public DefaultView(String name) {

        name_ = name;
 
        // output resolution and optional layout mask
        Dimension resolution = null;
        BufferedImage layoutMask = null;
        String layoutMaskFile = Main.getProperties().getProperty(name_ + ".mask-image");
        if (layoutMaskFile != null) {
            try { // TODO: support external file
                // layout mask assigns pixels to renderings by color
                layoutMask = ImageIO.read(ClassLoader.getSystemResourceAsStream("resources/" + layoutMaskFile));
                resolution = new Dimension(layoutMask.getWidth(), layoutMask.getHeight());
            } catch (Exception ex) {
                Main.logger_.log(Level.WARNING, "Problem loading layout-mask " + layoutMaskFile + ".");
                ex.printStackTrace(System.out);
            }
        }
        layoutMask_ = layoutMask;
        if (layoutMask_ == null) {
            try {
                String[] s = Main.getProperties().getProperty(name_ + ".resolution").split("x");
                resolution = new Dimension(Integer.parseInt(s[0]), Integer.parseInt(s[1]));
            } catch (Exception exx) {
                resolution = new Dimension(1280, 720);
            }
        }
        resolution_ = resolution;
        
        interpolation_ = Boolean.valueOf(Main.getProperties().getProperty(name_ + ".interpolation"));

        // output image
        frameImage_ = new BufferedImage(resolution_.width, resolution_.height, BufferedImage.TYPE_INT_RGB);
        frameImage_.setAccelerationPriority(0.0f);
        if (layoutMask_ != null) {
            frameImage_.createGraphics().drawImage(layoutMask_, 0, 0, null);
        }
        framePixels_ = ((DataBufferInt)frameImage_.getRaster().getDataBuffer()).getData();

        // memory-mapped file output
        String frameInfoPath = Main.getProperties().getProperty(name_ + ".frame-info");
        if (isMapped_ = (frameInfoPath != null)) {
            try {
                String frameFileName = Main.getProperties().getProperty(name_ + ".frame-file");
                File frameFile;
                if (frameFileName != null) {
                    frameFile = new File(frameFileName.replaceAll("/", Matcher.quoteReplacement(File.separator)));
                    frameFile.getParentFile().mkdirs();
                } else {
                    frameFile = File.createTempFile(name_ + ".frame-file", ".tmp");
                    frameFile.deleteOnExit();
                }
                frameFileChannel_ = (frameFile_ = new RandomAccessFile(frameFile, "rw")).getChannel();
                frameMappedBuffer_ = frameFileChannel_.map(MapMode.READ_WRITE, 0, resolution_.width * resolution_.height * 4).order(Main.getByteOrder());

                String overlayFileName = Main.getProperties().getProperty(name_ + ".overlay-file");
                File overlayFile;
                if (overlayFileName != null) {
                    overlayFile = new File(overlayFileName.replaceAll("/", Matcher.quoteReplacement(File.separator)));
                    overlayFile.getParentFile().mkdirs();
                } else {
                    overlayFile = File.createTempFile(name_ + ".overlay-file", ".tmp");
                    overlayFile.deleteOnExit();
                }
                overlayFileChannel_ = (overlayFile_ = new RandomAccessFile(overlayFile, "rw")).getChannel();
                overlayMappedBuffer_ = overlayFileChannel_.map(MapMode.READ_ONLY, 0, resolution_.width * resolution_.height * 4).order(Main.getByteOrder());
                overlayNotificationSocket_ = new DatagramSocket();
                Thread overlayUpdateHandler = new Thread() {
                    @Override
                    public void run() {
                        byte[] buffer = new byte[32]; // TODO: ??
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        for (;;) {
                            try {
                                overlayNotificationSocket_.receive(packet);
                                updateOverlay();
                            } catch (Exception ex) {
                            }
                        }
                    }
                };
                overlayUpdateHandler.setDaemon(true);
                overlayUpdateHandler.start();
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        try {
                            overlayNotificationSocket_.close();
                            overlayFile_.close();
                            frameFile_.close();
                        } catch (Exception ex) {
                        }
                    }
                });
                
                File frameInfoFile = new File(frameInfoPath.replaceAll("/", Matcher.quoteReplacement(File.separator)));
                frameInfoFile.getParentFile().mkdirs();
                ObjectOutputStream frameInfo = new ObjectOutputStream(new FileOutputStream(frameInfoFile));
                frameInfo.writeObject(new MappedFrameInfo(Main.getByteOrder(), resolution_, frameFile.getAbsolutePath(), overlayFile.getAbsolutePath(), overlayNotificationSocket_.getLocalPort()));
                frameInfo.close();
            } catch (Exception ex) {
                Main.logger_.log(Level.WARNING, "Problem initializing mapped frame buffer " + frameInfoPath + ".", ex);
            }
        }

        // overlay image
        overlayImage_ = isMapped_ ? new BufferedImage(resolution_.width, resolution_.height, BufferedImage.TYPE_INT_ARGB) : null;
        overlayPixels_ = isMapped_ ? ((DataBufferInt)overlayImage_.getRaster().getDataBuffer()).getData() : null;

        // source renderings
        for (int i = 0;; i++) {
            try {
                String renderingName = Main.getProperties().getProperty(name_ + ".rendering" + i);
                if (renderingName == null) {
                    if (i == 0) {
                        renderingName = name;
                    }
                    else {
                        break;
                    }
                }
                // TODO: plug-in rendering classes?
                renderings_.add(new DefaultRendering(renderingName, this));
            } catch (Exception ex) {
                Main.logger_.log(Level.WARNING, "", ex);
            }
        }
        if (renderings_.isEmpty()) {
            throw new RuntimeException("No renderings configured.");
        }
    }

    /**
     * 
     */
    private void updateOverlay() {
        try {
            FileLock lock = overlayFileChannel_.lock(); // keep source from writing while we're reading
            overlayMappedBuffer_.rewind();
            overlayMappedBuffer_.asIntBuffer().get(overlayPixels_); // byte (int) array to java image buffer
            lock.release();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return name_;
    }

    @Override
    public Dimension getResolution() {
        return resolution_;
    }

    @Override
    public BufferedImage getLayoutMask() {
        return layoutMask_;
    }

    @Override
    public Collection<VideoView> getViews() {
        return Arrays.asList( this );
    }

    @Override
    public Collection<Rendering> getRenderings() {
        return renderings_;
    }

    @Override
    public boolean isLive() {
        return isActive_;
    }

    @Override
    public void setLive( boolean isActive ) {
        isActive_ = isActive;
    }
    
    @Override
    public void processFrame(IntBuffer sourcePixels, int[] renderingSourceMap) {
        
        lock_.lock();
        
        // copy pixels from the current frame to the output image
        for (int i = 0; i < renderingSourceMap.length; i += 2) {
            int renderingPixelIndex = renderingSourceMap[i + 0];
            int sourcePixelIndex = renderingSourceMap[i + 1];
            framePixels_[renderingPixelIndex] = sourcePixels.get(sourcePixelIndex);
        }

        // copy output image to mapped buffer
        if (frameFileChannel_ != null) {
            try {
                frameMappedBuffer_.rewind();
                FileLock lock = frameFileChannel_.tryLock();
                if (lock != null) {
                    frameMappedBuffer_.asIntBuffer().put(framePixels_);
                    lock.release();
                }
            } catch (Exception ex) {
                Main.logger_.log(Level.WARNING, "Problem locking frame file channel for " + name_ + " on Thread " + Thread.currentThread().getName() + ".", ex);
            }
        }

        lock_.unlock();
        
        // schedule component for repaint
        if (jComponent_ != null) {
            jComponent_.repaint();
        }
    }

    @Override
    public JComponent getJComponent() {

        if (jComponent_ == null) {
            jComponent_ = new JComponent() {
                private static final long serialVersionUID = 1L;
                @Override
                protected void paintComponent(Graphics g) {
                
                    Object interpolation = interpolation_ ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
                    ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);

                    g.setColor( getBackground() );
                    g.fillRect( 0,  0,  getWidth(),  getHeight() );

                    double scale = Math.min(((double)getWidth()) / resolution_.width, ((double)getHeight()) / resolution_.height);
                    int w = (int)Math.round(resolution_.width * scale);
                    int h = (int)Math.round(resolution_.height * scale);
                    int x = (int)Math.round((getWidth() - w) / 2.);
                    int y = (int)Math.round((getHeight() - h) / 2.);

                    lock_.lock();
                    g.drawImage(frameImage_, x, y, w, h, null);
                    if (overlayImage_ != null) {
                        g.drawImage(overlayImage_, x, y, w, h, null);
                    }
                    lock_.unlock();
                }
            };
            jComponent_.setOpaque(true);
            jComponent_.setBackground(Color.black);
            jComponent_.setPreferredSize(resolution_);
        }
        return jComponent_;
    }
    
    @Override
    public boolean isMapped() {
        return isMapped_;
    }
    
    //@Override
    //public boolean isDisplayable() {
    //    return jComponent_ != null && jComponent_.isDisplayable();
    //}

}
