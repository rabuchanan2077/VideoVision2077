package org.usfirst.frc.team2077.vision;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.usfirst.frc.team2077.vvcommon.MappedFrameInfo;
import org.usfirst.frc.team2077.vvcommon.Utilities;


public class VisionMain {

    static {
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
    }

    private static final Logger logger_ = Logger.getLogger(VisionMain.class.getName());

    private static JFrame videoFrame_;
    private static Properties properties_;
    //private static MappedFrameInfo videoInfo_;

    //private static String visionView_ = "Equidistant180";
    
    private static final Timer timer_ = new Timer(true);

    public static Map<String,VisionView> views_ = new LinkedHashMap<>();
    
    private static class VisionView {
        private final String name_;
        private final String label_;
        
        private final MappedFrameInfo videoInfo_;
        private final FrameProcessor frameProcessor_; 

        private final Dimension resolution_;
        private final BufferedImage[] videoImage_ = new BufferedImage[2];
        private final AtomicInteger videoImageIndex_ = new AtomicInteger(0);
        
        private final FileChannel videoFileChannel_;
        private final ByteBuffer videoMappedBuffer_;
        private final ByteBuffer videoByteBuffer_;
        private final byte[] videoBytes_;
        private final Mat videoMat_;
        private final BufferedImage overlayImage_;
        private final FileChannel overlayFileChannel_;
        private final ByteBuffer overlayMappedBuffer_;
        private final ByteBuffer overlayByteBuffer_;
        private final byte[] overlayBytes_;
        private final Mat overlayMat_;
        private final DatagramPacket overlayNotifyPacket_;
        private final DatagramSocket overlayNotifySocket_;
        
        private JComponent jComponent_ = null;
        
        public JComponent getJComponent() {
            if (jComponent_ == null) {
                jComponent_ = new JComponent() {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public Dimension getPreferredSize() {
                        return videoInfo_.resolution_;
                    }
                    @Override
                    public void paintComponent(Graphics g) {
                        BufferedImage image_ = videoImage_[videoImageIndex_.get()];
                        double scale = Math.min(((double)getWidth()) / resolution_.width, ((double)getHeight()) / resolution_.height);
                        int w = (int) Math.round(resolution_.width * scale);
                        int h = (int) Math.round(resolution_.height * scale);
                        int x = (getWidth() - w) / 2;
                        int y = (getHeight() - h) / 2;
                        // scales from rendered size to component size
                        g.drawImage(image_, x, y, w, h, null);
                     }
                };

            }
            return jComponent_;
        }
        
        private void processFrame() throws IOException {
        
            // read video frame from mapped file
            FileLock videoLock = videoFileChannel_.lock(); // keep source from writing while we're reading
            videoMappedBuffer_.rewind();
            videoMappedBuffer_.get(videoBytes_); // mapped file buffer -> byte array
            videoLock.release();
            videoMat_.put(0, 0, videoBytes_); // byte array -> openCV

            // clear overlay
            int[] overlayPixels = ((DataBufferInt) overlayImage_.getRaster().getDataBuffer()).getData();
            Arrays.fill(overlayPixels, 0);

            overlayByteBuffer_.asIntBuffer().put(overlayPixels);
            overlayMat_.put(0, 0, overlayBytes_);

            frameProcessor_.processFrame(videoMat_, overlayMat_);

            overlayMat_.get(0,  0, overlayBytes_); // openCV -> byte array
            overlayByteBuffer_.asIntBuffer().get(overlayPixels);

            // write overlay to mapped file
            FileLock overlayLock = overlayFileChannel_.tryLock();
            if (overlayLock != null) {
                overlayMappedBuffer_.rewind();
                overlayMappedBuffer_.asIntBuffer().put(overlayPixels);
                overlayLock.release();
                overlayNotifySocket_.send(overlayNotifyPacket_);
            }

            // put processed video frame onto the screen
            videoMat_.get(0, 0, videoBytes_); // openCV -> byte array
            int[] videoPixels = ((DataBufferInt) videoImage_[(videoImageIndex_.get() + 1) % 2].getRaster().getDataBuffer()).getData();
            videoByteBuffer_.rewind();
            videoByteBuffer_.asIntBuffer().get(videoPixels); // byte (int) array to java image buffer
            videoImageIndex_.set((videoImageIndex_.get() + 1) % 2); // mark the updated java image as current
            if (jComponent_ != null) {
                jComponent_.repaint(); // schedule screen repaint
            }

            System.gc(); // OpenCV may not clean up native memory allocations without some nudging
        }
       
        public VisionView(String name, String label, String frameInfoPath, String frameProcessor) throws FileNotFoundException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
 
            name_ = name;
            label_ = label;
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(frameInfoPath.replaceAll("/", Matcher.quoteReplacement(File.separator))))) {
                videoInfo_ = (MappedFrameInfo)in.readObject();
            }
            frameProcessor_ = (FrameProcessor)Class.forName(frameProcessor).newInstance();
            
            ByteOrder byteOrder = videoInfo_.byteOrder_ == "BE" ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
            resolution_ = videoInfo_.resolution_;;
            videoFileChannel_ = new RandomAccessFile(new File(videoInfo_.frameFile_), "rw").getChannel();
            videoMappedBuffer_ = videoFileChannel_.map(MapMode.READ_ONLY, 0, resolution_.width * resolution_.height * 4).order(byteOrder);
            videoByteBuffer_ = ByteBuffer.allocate(resolution_.width * resolution_.height * 4).order(byteOrder);
            videoBytes_ = videoByteBuffer_.array(); // used to transfer data back and forth to openCV
            videoMat_ = new Mat(resolution_.height, resolution_.width, CvType.CV_8UC4);
            videoImage_[0] = new BufferedImage(resolution_.width, resolution_.height, BufferedImage.TYPE_INT_RGB);
            videoImage_[1] = new BufferedImage(resolution_.width, resolution_.height, BufferedImage.TYPE_INT_RGB);

            (new File(videoInfo_.overlayFile_)).getParentFile().mkdirs();
            overlayFileChannel_ = new RandomAccessFile(new File(videoInfo_.overlayFile_), "rw").getChannel();
            overlayMappedBuffer_ = overlayFileChannel_.map(MapMode.READ_WRITE, 0, resolution_.width * resolution_.height * 4).order(byteOrder);

            overlayMat_ = new Mat(resolution_.height,resolution_.width, CvType.CV_8UC4);
            overlayByteBuffer_ = ByteBuffer.allocate(resolution_.width * resolution_.height * 4).order(byteOrder);
            overlayBytes_ = overlayByteBuffer_.array(); // used to transfer data back and forth to openCV
            overlayImage_ = new BufferedImage(resolution_.width, resolution_.height, BufferedImage.TYPE_INT_ARGB);

            overlayNotifyPacket_ = new DatagramPacket(new byte[64], 64, InetAddress.getLoopbackAddress(), videoInfo_.overlayNotificationPort_);
            overlayNotifySocket_ = new DatagramSocket();
            
            timer_.schedule( new TimerTask() {
                @Override
                public void run() {
                    try {
                        processFrame();
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }, 1000, 1000); // TODO: make frame rate or duty cycle configurable
        }
    }

    private static void initViews() {

        // display views
        for (int i = 0;; i++) {
            String name = properties_.getProperty("view" + i);
            if (name == null) {
                break;
            }
            String frameInfoPath = properties_.getProperty(name + ".frame-info");
            String frameProcessor = properties_.getProperty(name + ".frame-processor");
            if (frameInfoPath == null || frameProcessor == null) {
                continue;
            }
            String label = properties_.getProperty(name + ".label", name);
            try {
                views_.put(name, new VisionView(name, label, frameInfoPath, frameProcessor));
            }
            catch (Exception ex) {
                logger_.log( Level.WARNING,  "Vision processor for view " + name + " startup failed.",  ex );
                continue;
            }
        }
        if (views_.isEmpty()) {
            logger_.log(Level.SEVERE, "No mapped views configured, exiting.");
            System.exit(1);
        }
    }
   public static void main( String[] args ) {

        properties_ = Utilities.readProperties(args);
        
        initViews();
        
        videoFrame_ = new JFrame("Vision");
        videoFrame_.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JToggleButton button0 = null;
        JComponent panel = new JPanel(new GridLayout(0,2));
        ButtonGroup group = new ButtonGroup();
        for (VisionView v : views_.values()) {
            final VisionView view = v;
            JToggleButton button = new JToggleButton(new AbstractAction(view.label_) {
                private static final long serialVersionUID = 1L;
                @Override
                public void actionPerformed(ActionEvent ae) {
                    videoFrame_.setContentPane(view.getJComponent());
                    videoFrame_.revalidate();
                    videoFrame_.repaint();
                }
            });
            group.add(button);
            panel.add(button);
            if (button0 == null) {
                button0 = button;
            }
        }

        if (button0 != null) {
            if (views_.size() > 1) {
                final JFrame controlFrame_ = new JFrame("Vision Selector");
                controlFrame_.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                controlFrame_.setAlwaysOnTop(true);
                controlFrame_.setContentPane(panel);
                controlFrame_.pack();
                controlFrame_.setVisible(true);
            }
             button0.doClick();
        }
        
        videoFrame_.pack();
        videoFrame_.setVisible(true);
    }






    public static interface FrameProcessor {

        void processFrame(Mat frameMat, Mat overlayMat);

    } // Frameprocessor


    public static class EmptyFrameProcessor implements FrameProcessor {

        @Override
        public void processFrame(Mat frameMat, Mat overlayMat) {
        }

    } //EmptyFrameProcessor


    public static class FrameProcessorSequence implements FrameProcessor {

        private final FrameProcessor[] frameProcessors_;

        public FrameProcessorSequence(FrameProcessor... frameProcessors) {
            frameProcessors_ = frameProcessors;
        }

        public FrameProcessorSequence(java.util.List<FrameProcessor> frameProcessors) {
            frameProcessors_ = frameProcessors.toArray( new FrameProcessor[0] );
        }

        @Override
        public void processFrame(Mat frameMat, Mat overlayMat) {
            for (FrameProcessor fp : frameProcessors_) {
                fp.processFrame(frameMat, overlayMat);
            }
        }

    } // FrameProcessorSequence            

}
