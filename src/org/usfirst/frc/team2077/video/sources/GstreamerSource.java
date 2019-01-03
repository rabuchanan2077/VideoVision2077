package org.usfirst.frc.team2077.video.sources;

import java.awt.Dimension;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Buffer;
import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Sample;
import org.freedesktop.gstreamer.Structure;
import org.freedesktop.gstreamer.elements.AppSink;
import org.freedesktop.gstreamer.event.EOSEvent;
import org.usfirst.frc.team2077.video.Main;
import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;

/**
 * Gstreamer pipeline input.
 * <p>
 * <a href=https://gstreamer.freedesktop.org>Gstreamer</a>
 * is a flexible multi-platform framework for media streaming,
 * and supports a wide range of video streaming mechanisms.
 * This class wraps a Gstreamer pipeline, packaging its output as a VideoSource.
 * This means no other video input mechanisms need be implemented here - those
 * that are don't do anything not possible through GStreamer and are included
 * only for testing, convenience, and ease of use.
 * Gstreamer is very powerful, but not simple. 
 * <p>
 * Gstreamer is well established and capable of excellent performance in the areas
 * we care about, but there are many, many ways to build pipelines that perform poorly.
 * It is strongly recommended that pipelines be developed and tuned independently
 * before use with this framework.
 * <p>
 * Configuration properties (in addition to those read by {@link AbstractSource}):
 * <dl>
 * <dt>&lt;source&gt;.pipeline</dt>
 * <dd>The Gstreamer pipeline to be executed to supply video frames to this source.
 * The pipeline must contain (typically as its last element) an "appsink" element,
 * to will be linked to this source to receive video frames. The pipeline may
 * generate video on the same host where this software runs, but for FRC applications
 * would more typically define a client pipeline to receive video streamed from a
 * remote pipeline. The remote server pipeline may be started either using the
 * remote/command properties in AbstractSource, or by other means such as startup
 * scripts on the remote host.</dd>
 * <dt>&lt;source&gt;.capture-file-prefix</dt>
 * <dd>If the pipeline contains the string "$FILE", it is replaced before execution
 * with a file path including the specified prefix and a date/time stamp. If not specified
 * the default is the value of {@link #getName}.</dd>
 * </dl>
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public final class GstreamerSource extends AbstractSource implements VideoSource {
    
    static {
        Gst.init();
    }

    public final String pipelineString_;

    // keep a reference to running pipeline to keep it from getting GCed and crashing
    private Pipeline pipeline_;
    private Thread pipelineThread_;
    private AppSink appSink_;

    /**
     * Reads configuration properties and initializes Gstreamer pipeline.
     * 
     * @param name Key for configuration properties and {@link Main#getSource}.
     */
    public GstreamerSource(String name) {
        
        super(name);

        // local gstreamer pipeline specification
        String pipelineString = Main.getProperties().getProperty(name_ + ".pipeline");
        if (pipelineString.contains("$FILE")) { // optional generated video capture file name if specified in pipeline
            String captureFilePrefix = Main.getProperties().getProperty(name_ + ".capture-file-prefix", name_);
            DateFormat timeStampFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ssZ");
            String fileName = (captureFilePrefix + "-" + timeStampFormat.format(new Date()));
            // Gstreamer apparently assumes Linux-style file paths, so separator character conversion is unnecessary/undesirable
            // fileName = fileName.replaceAll("/", Matcher.quoteReplacement(File.separator));
            System.out.println("INFO:" + name_ + ": CAPTURE FILE:" + fileName);
            File parent = (new File(fileName)).getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            pipelineString = pipelineString.replace("$FILE", fileName);
        }
        pipelineString_ = pipelineString;
        System.out.println("INFO:" + name_ + ": GSTREAMER INPUT PIPELINE:" + pipelineString_);
    }

    @Override
    public synchronized void start() {

        if (remote_ != null) {
            System.out.println("INFO:" + name_ + ": STARTING REMOTE GSTREAMER PIPELINE:" + pipelineString_);
            runRemote();
        }

        System.out.println("INFO:" + name_ + ": STARTING LOCAL GSTREAMER PIPELINE:" + pipelineString_);
        
        try {
            Bin bin = Bin.launch(pipelineString_, true);
            for (Element e : bin.getElementsSorted()) { // debugging output
                System.out.println("INFO:" + name_ + ": ELEMENT:" + e + "(" + e.getSinkPads().size() + "," + e.getSrcPads().size() + ")");
            }
            for (Element e : bin.getElementsSorted()) {
                if (e instanceof AppSink) {
                    appSink_ = (AppSink)e;
                    break;
                }
            }
            if (appSink_ == null) {
                throw new RuntimeException("No AppSink in Gstreamer pipeline.");
            }
            pipeline_ = new Pipeline();
            pipeline_.add(bin);
            appSink_.set("drop", true); // want incoming data dropped if not handled rather than queued
            appSink_.set("max-buffers", 1);
            appSink_.setCaps(new Caps("video/x-raw," + (Main.getByteOrder() == ByteOrder.LITTLE_ENDIAN ? "format=BGRx" : "format=xRGB")));
            pipeline_.play();
        }
        catch(Exception ex) {
            System.out.println("SEVERE:" + "Gstreamer pipeline construction failed. Video source not started.");
            ex.printStackTrace(System.out);
            appSink_ = null;
            return;
        }

        pipelineThread_ = new Thread() {
            @Override
            public void run() {
                System.out.println("INFO:" + name_ + ": GSTREAMER PIPELINE HANDLER THREAD STARTED:" + pipeline_);
                while (pipelineThread_ == this) {
                    Sample sample = appSink_.pullSample();
                    if (pipelineThread_ == this && sample != null) {
                        handleSample(sample);
                    }
                }
                System.out.println("INFO:" + name_ + ": GSTREAMER PIPELINE HANDLER THREAD ENDING:" + pipeline_);
            }
        };
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                GstreamerSource.this.stop();
            }
        });
        //pipelineThread_.setDaemon(true);
        pipelineThread_.setName(name_ + "pipelineThread_");
        pipelineThread_.start();
    }

    @Override
    public void stop() {
        if (appSink_ != null) {
            System.out.println("INFO:" + name_ + ": GSTREAMER PIPELINE END OF STREAM:" + pipeline_ + " " + appSink_.sendEvent(new EOSEvent()));
        }
        pipelineThread_ = null;
        if ( pipeline_ != null ) {
            pipeline_.stop();
            System.out.println( "INFO:" + name_ + ": GSTREAMER PIPELINE STOPPED:" + pipeline_ );
            pipeline_ = null;
        }
    }

    private void handleSample(Sample sample) {

        Main.getFrameCounter().getAndIncrement();

        execBaseTime_ = System.currentTimeMillis();

        if (resolution_ == null) {
            Structure capsStruct = sample.getCaps().getStructure(0);
            resolution_ = new Dimension(capsStruct.getInteger("width"), capsStruct.getInteger("height"));
        }
        // update the output image with pixels from this camera frame
        Buffer buffer = sample.getBuffer();
        ByteBuffer bb = buffer.map(false);
        if (bb != null) { // not sure why null would ever be encountered
            IntBuffer sourceFramePixels = bb.order(Main.getByteOrder()).asIntBuffer();
            synchronized (views_) { // TODO: ??
                for (RenderedView view : views_) {
                    processFrame(view, sourceFramePixels);
                }
            }
            buffer.unmap();
        }
        sample.dispose();
    }

 } // GstreamerSource
