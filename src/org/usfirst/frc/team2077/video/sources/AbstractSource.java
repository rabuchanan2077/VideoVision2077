package org.usfirst.frc.team2077.video.sources;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.usfirst.frc.team2077.video.Main;
import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.Rendering;
import org.usfirst.frc.team2077.video.interfaces.SourceProjection;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;
import org.usfirst.frc.team2077.video.interfaces.VideoView;
import org.usfirst.frc.team2077.video.projections.Perspective;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * Base {@link VideoSource} implementation. Includes code to execute a startup command on a remote host as a part of the
 * local startup sequence (to be split into a separate class eventually). The remote command is restarted if the source
 * stops receiving frame updates after some interval. This restart logic is ill-tuned and may be causing more harm than
 * good. It needs work.
 * <p>
 * Configuration properties:
 * <dl>
 * <dt>&lt;source&gt;.projection</dt>
 * <dd>Fully qualified name of a {@link SourceProjection} class. The projection will be instantiated by calling a
 * constructor taking parameters (String name, VideoSource videoSource), where <code>name</code> is its configuration
 * property prefix and <code>videoSource</code> is the calling VideoSource. The proper source projection, with proper
 * configuration values, should implement the camera lens projection function, mapping cartesian source image
 * coordinates to the corresponding spherical vector coordinates being projected. See
 * {@link org.usfirst.frc.team2077.video.test.FisheyeCalibration} for more on how to do this.</dd>
 * <dt>&lt;source&gt;.camera-NS-position</dt>
 * <dd>&nbsp;</dd>
 * <dt>&lt;source&gt;.camera-EW-position</dt>
 * <dd>&nbsp;</dd>
 * <dt>&lt;source&gt;.camera-height</dt>
 * <dd>Camera lens location relative to the global system origin, in physical units. Used by projections that utilize
 * absolute camera position as well as angle, such as {@link org.usfirst.frc.team2077.video.projections.Birdseye}.</dd>
 * <dt>&lt;source&gt;.remote</dt>
 * <dd>Name or address of host on which to on which to execute an optional command associated with starting this source.
 * If not supplied, no remote startup command will be executed.</dd>
 * <dt>&lt;source&gt;.login</dt>
 * <dd>&nbsp;</dd>
 * <dt>&lt;source&gt;.password</dt>
 * <dd>Login credentials for remote startup execution.</dd>
 * <dt>&lt;source&gt;.command</dt>
 * <dd>Remote startup command.</dd>
 * </dl>
 * <p>
 * \u00A9 2018
 * 
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public abstract class AbstractSource implements VideoSource {

    protected static final java.util.Timer timer_ = new Timer();

    protected final String name_;

    protected final String remote_;
    protected final String user_;
    protected final String password_;
    protected final String command_;

    private Session session_ = null;
    private ChannelExec exec_ = null;
    private Object execLock_ = new Object() {
    };

    // camera placement // TODO: remove
    public final double cameraX_;
    public final double cameraY_;
    public final double cameraZ_;

    // camera transform
    protected final SourceProjection projection_;

    protected Dimension resolution_ = null;

    protected Set<RenderedView> views_ = new HashSet<>();

    protected final Map<String, int[]> viewMap_ = new LinkedHashMap<>();

    protected long execBaseTime_;

    // TODO: split remote functions into separate class
    /**
     * Reads configuration properties and initializes optional remote command and camera geometry.
     * 
     * @param name Key for configuration properties and {@link Main#getSource}.
     */
    public AbstractSource( String name ) {

        name_ = name;

        // remote server access
        remote_ = Main.getProperties().getProperty( name_ + ".remote" );
        user_ = Main.getProperties().getProperty( name_ + ".user", "pi" );
        password_ = Main.getProperties().getProperty( name_ + ".password", "raspberry" );
        command_ = Main.getProperties().getProperty( name_ + ".command" );
        if ( user_ != null && remote_ != null && command_ != null ) {
            System.out.println( "INFO:" + name_ + ": " + user_ + "@" + remote_ + " " + command_ );
        }

        // camera location
        char cameraOrientation = Main.getProperties().getProperty( name_ + ".camera-orientation", "N" ).toUpperCase().charAt( 0 ); // N|S|E|W
        AffineTransform worldToCamera = new AffineTransform();
        worldToCamera.rotate( (Math.PI / 180.) * (cameraOrientation == 'E' ? 90. : cameraOrientation == 'S' ? 180. : cameraOrientation == 'W' ? 270. : 0.) );
        double[] cameraPosition = {Double.parseDouble( Main.getProperties().getProperty( name_ + ".camera-EW-position", "0.0" ) ),
                                   Double.parseDouble( Main.getProperties().getProperty( name_ + ".camera-NS-position", "0.0" ) )};
        worldToCamera.transform( cameraPosition, 0, cameraPosition, 0, 1 );
        cameraX_ = cameraPosition[0];
        cameraY_ = cameraPosition[1];
        cameraZ_ = Double.parseDouble( Main.getProperties().getProperty( name_ + ".camera-height", "24.0" ) );

        SourceProjection projection = null;
        try {
            Class<?> projectionClass = Class.forName( Main.getProperties().getProperty( name + ".projection", Perspective.class.getName() ) );
            projection = (SourceProjection)projectionClass.getDeclaredConstructor( String.class, VideoSource.class ).newInstance( name_, this );
        } catch ( Exception ex ) {
            System.out.println( "SEVERE:" + "Camera projection initialization failed." );
            ex.printStackTrace( System.out );
        }
        projection_ = projection;
    }

    @Override
    public SourceProjection getProjection() {
        return projection_;
    }

    /**
     * Chooses a network address through which the local host may be reached from a given remore host. Many remote
     * commands require a network address for sending data back to this program. Where the host has multiple network
     * adapters or addresses, not all may be reachable from the remote. A working address is identified by opening a
     * temporary DatagramSocket and getting its address on the local end.
     * 
     * @param remote
     * @return An address through which the remote host can reach this local host.
     * @throws SocketException
     * @throws UnknownHostException
     */
    private static String getReturnAddress( String remote ) throws SocketException, UnknownHostException {
        try ( final DatagramSocket socket = new DatagramSocket() ) {
            socket.connect( InetAddress.getByName( remote ), 10002 );
            return socket.getLocalAddress().getHostAddress();
        }
    }

    /**
     * Optionally starts a remote video data server in a thread that restarts it if the stream times out. Needs work.
     */
    protected void runRemote() { // TODO: configurable timeout/restart logic, etc, etc

        execBaseTime_ = System.currentTimeMillis();
        (new Thread() {
            {
                setDaemon( true );
            }

            @Override
            public void run() {
                try {
                    JSch jsch = new JSch();
                    // loop to start or restart remote command
                    while ( true ) {
                        // restart may recreate session or reuse old one depending on whether session_ has been cleared
                        Session session = session_;
                        ChannelExec exec = null;
                        while ( session == null ) {
                            System.out.println( "INFO:" + name_ + ": SESSION:" + session_ + " " + session + " " + exec_ + " " + exec );
                            try {
                                session = jsch.getSession( user_, remote_, 5800 ); // TODO: configurable port #
                                session.setUserInfo( new UserInfo() {
                                    @Override
                                    public String getPassphrase() {
                                        return password_;
                                    }

                                    @Override
                                    public String getPassword() {
                                        return password_;
                                    }

                                    @Override
                                    public boolean promptPassphrase( String message ) {
                                        return true;
                                    }

                                    @Override
                                    public boolean promptPassword( String message ) {
                                        return true;
                                    }

                                    @Override
                                    public boolean promptYesNo( String message ) {
                                        return true;
                                    }

                                    @Override
                                    public void showMessage( String message ) {
                                    }
                                } );
                                session.connect(); // TODO: use timeout?
                                System.out.println( "INFO:" + name_ + ": Started remote session @ " + remote_ + "." );
                            } catch ( Exception ex ) {
                                System.out.println( "WARNING:" + name_ + ": Problem starting remote session @ " + remote_ + "." );
                                ex.printStackTrace( System.out );
                                session = null;
                                try {
                                    Thread.sleep( 1000 );
                                } catch ( Exception exx ) {
                                }
                            }
                        }
                        System.out.println( "INFO:" + name_ + ": EXEC:" + session_ + " " + session + " " + exec_ + " " + exec );
                        session_ = session;
                        execBaseTime_ = System.currentTimeMillis();
                        try {
                            exec = (ChannelExec)session.openChannel( "exec" );
                            exec.setCommand( command_.replace( "$LOCALHOST", getReturnAddress( remote_ ) ) );
                            exec.setInputStream( null );
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            ByteArrayOutputStream err = new ByteArrayOutputStream();
                            // exec.setOutputStream(System.err, true);
                            // exec.setErrStream(System.err, true);
                            exec.setOutputStream( out );
                            exec.setErrStream( err );
                            exec.connect();
                            exec_ = exec;
                            System.out.println( "INFO:" + name_ + ": Started remote command " + command_ + "." );
                            // wait until session_ or exec_ is cleared from outside or finishes
                            while ( (exec_ != null) && (session_ != null) && exec.isConnected() && !exec.isEOF() && !exec.isClosed() ) {
                                synchronized ( execLock_ ) {
                                    try {
                                        execLock_.wait( 1000 );
                                    } catch ( Exception ex ) {
                                        // continue;
                                    }
                                }
                            }
                            if ( !exec.isClosed() ) {
                                exec.sendSignal( "KILL" );
                            }
                            System.out.println( "INFO:" + name_ + ": Exited remote command " + command_ + "." );
                            System.out.write( out.toByteArray() );
                            System.out.write( err.toByteArray() );
                            System.out.println( "INFO:" + name_ + ": Remote command exit status: " + exec.getExitStatus() );
                            exec.disconnect();
                            if ( session_ == null ) {
                                session.disconnect();
                            }
                        } catch ( Exception ex ) {
                            System.out.println( "WARNING:" + name_ + ": Problem executing remote command @ " + remote_ + "." );
                            ex.printStackTrace( System.out );
                            session_ = null;
                            exec_ = null;
                            try {
                                Thread.sleep( 1000 );
                            } catch ( Exception exx ) {
                            }
                        }
                    }
                } catch ( Exception ex ) {
                    System.out.println( "SEVERE:" + name_ + ": Problem initializing JSCH for remote command execution @ " + remote_ + "." );
                    ex.printStackTrace( System.out );
                }
            }
        }).start();

        timer_.schedule( new TimerTask() {
            @Override
            public void run() {
                long time = System.currentTimeMillis() - execBaseTime_;
                //if ( (time > (6 * 1000)) && (exec_ != null) ) {
                if ( (time > (30 * 1000)) && (exec_ != null) ) {
                    System.out.println( "WARNING:" + name_ + ": Frame update timeout (" + (time / 1000) + " sec) @ " + remote_ + "." );
                    exec_ = null;
                    synchronized ( execLock_ ) {
                        execLock_.notifyAll();
                    }
                }
            }
        }, 10 * 1000, 1 * 1000 );
    }

    @Override
    public String getName() {
        return name_;
    }

    @Override
    public Dimension getResolution() {
        return resolution_;
    }

    /**
     * Updates a view image with pixels from an incoming source frame.
     * 
     * @param view View to be updated.
     * @param sourceFramePixels Pixel data for one full frame from this video source.
     */
    protected void processFrame( RenderedView view, IntBuffer sourceFramePixels ) {

        if ( !view.isLive() ) {
            return;
        }

        if ( viewMap_.get( view.getName() ) == null ) { // TODO: viewMap_ synchronization
            buildMap( view ); // TODO: run this in a BG thread
        }

        // if (!view.isLive()) {
        // return;
        // }

        int[] viewMap = viewMap_.get( view.getName() );
        if ( viewMap.length == 0 ) {
            // nothing from this video stream appears in the view
            return;
        }

        view.processFrame( sourceFramePixels, viewMap );
    }

    /**
     * Builds table of where pixel data from this source appears in any view that uses it. Each rendering using this
     * source generates its own subtable, and these are merged into a single table of index pairs {viewPixelIndex0,
     * sourcePixelIndex0, viewPixelIndex1, sourcePixelIndex1... viewPixelIndexN, sourcePixelIndexN} to be used to update
     * pixels in the view image whenever a new frame arrives from the source.
     * 
     * @param view View whose pixel source map is being updated.
     */
    protected void buildMap( RenderedView view ) {

        int[] viewMapTmp = new int[view.getResolution().width * view.getResolution().height];
        for ( int i = 0; i < viewMapTmp.length; i++ ) {
            viewMapTmp[i] = -1;
        }
        for ( Rendering rendering : view.getRenderings() ) {
            if ( rendering.getVideoSource() == this ) {
                int[] renderingMap = rendering.getMap();
                for ( int i = 0; i < renderingMap.length; i += 2 ) {
                    viewMapTmp[renderingMap[i]] = renderingMap[i + 1];
                }
            }
        }
        int n = 0;
        for ( int i = 0; i < viewMapTmp.length; i++ ) {
            if ( viewMapTmp[i] != -1 ) {
                n++;
            }
        }
        int[] viewMap = new int[2 * n];
        n = 0;
        for ( int i = 0; i < viewMapTmp.length; i++ ) {
            if ( viewMapTmp[i] != -1 ) {
                viewMap[n++] = i;
                viewMap[n++] = viewMapTmp[i];
            }
        }
        viewMap_.put( view.getName(), viewMap ); // save map even if empty so it won't be recomputed
    }

    @Override
    public void activateViews( Collection<VideoView> views ) { // TODO: clarify the purpose of this

        synchronized ( views_ ) {
            for ( RenderedView view : views_ ) {
                view.setLive( views.contains( view ) );
            }
            views_.clear();
            for ( VideoView view : views ) {
                if ( view instanceof RenderedView ) {
                    views_.add( (RenderedView)view );
                }
            }
            for ( RenderedView view : views_ ) {
                if ( !viewMap_.containsKey( view.getName() ) ) {
                    viewMap_.put( view.getName(), null );
                }
                view.setLive( true );
            }
        }
    }

} // AbstractSource
