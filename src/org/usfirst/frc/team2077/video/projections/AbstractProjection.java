package org.usfirst.frc.team2077.video.projections;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import org.usfirst.frc.team2077.video.Main;
import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.RenderingProjection;
import org.usfirst.frc.team2077.video.interfaces.SourceProjection;
import org.usfirst.frc.team2077.video.interfaces.VideoSource;

/**
 * Base implementation of both {@link SourceProjection} and {@link RenderingProjection}.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public abstract class AbstractProjection implements SourceProjection, RenderingProjection {
    
    protected final String name_;
    
    protected final VideoSource videoSource_; // null for RenderingProjection instances
    
    protected final RenderedView view_; // null for SourceProjection instances
    
    protected final double focalLength_;

    // camera lens properties
    protected final double cameraFOVDiameter_;
    protected final double cameraFOVCenterX_;
    protected final double cameraFOVCenterY_;
    protected final double[] cameraOriginXYZ_;
    
    protected final boolean global_;
    protected final Rectangle2D bounds_;
    protected final boolean[][] mask_;
    protected final AffineTransform viewToRendering_;

    protected final double fovAngleHorizontal_;
    protected final double fovAngleVertical_;
     
    protected final double k_;
    
    protected final double[][] backTransformGlobal_;
    protected final double[][] backTransformNominal_;
    protected final double[][] forwardTransform_;
    protected final double[][] forwardTransformGlobal_;
    
    /**
     * Source projection instance constructor.
     * @param name Configuration property key.
     * @param videoSource Input video source.
     */
    protected AbstractProjection(String name, VideoSource videoSource) {
        this( name, videoSource, 0, 0);
    }
    
    /**
     * Source projection instance constructor.
     * @param name Configuration property key.
     * @param videoSource Input video source.
     * @param defaultFOV Horizontal field of view if not specified in configuration properties (degrees).
     */
    protected AbstractProjection(String name, VideoSource videoSource, double defaultFOV) {
        this( name, videoSource, defaultFOV, 0, 0);
    }
    
    /**
     * Source projection instance constructor.
     * @param name Configuration property key.
     * @param videoSource Input video source.
     * @param defaultFOV Horizontal field of view if not specified in configuration properties (degrees).
     * @param defaultK Projection K value if not specified in configuration properties.
     */
    protected AbstractProjection(String name, VideoSource videoSource, double defaultFOV, double defaultK) {
        this( name, videoSource, defaultFOV, defaultK, 0);
    }
    
    /**
     * Source projection instance constructor.
     * @param name Configuration property key.
     * @param videoSource Input video source.
     * @param defaultFOV Horizontal field of view if not specified in configuration properties (degrees).
     * @param defaultK Projection K value if not specified in configuration properties (ignored in this case, since setK overrides it).
     * @param setK Fixed projection K value.
     */
    protected AbstractProjection(String name, VideoSource videoSource, double defaultFOV, double defaultK, double setK) {
        
        videoSource_ = videoSource;
        name_ = name;
        
        //TODO: try/catch here and other numeric property parsing

        fovAngleHorizontal_ = Double.parseDouble(Main.getProperties().getProperty(name_ + ".camera-fov-angle", "" + defaultFOV)) * Math.PI / 180.;
        cameraFOVDiameter_ = Double.parseDouble(Main.getProperties().getProperty(name_ + ".camera-fov-diameter", "1.0"));
        cameraFOVCenterX_ = Double.parseDouble(Main.getProperties().getProperty(name_ + ".camera-fov-center-x", "0.5"));
        cameraFOVCenterY_ = Double.parseDouble(Main.getProperties().getProperty(name_ + ".camera-fov-center-y", "0.5"));
        k_ = setK > 0 ? setK : Double.parseDouble(Main.getProperties().getProperty(name_ + ".k", "" + defaultK));

        cameraOriginXYZ_ = new double[] {
            Double.parseDouble(Main.getProperties().getProperty(name_ + ".camera-NS-position", "0.0")),
            -Double.parseDouble(Main.getProperties().getProperty(name_ + ".camera-EW-position", "0.0")),
            Double.parseDouble(Main.getProperties().getProperty(name_ + ".camera-height", "24.0"))
        };

        double[][] nominalToGlobal = {{1,0,0},{0,1,0},{0,0,1}};
        // global spherical is polar axis upward, azimuth zero forward, and positive azimuth clockwise from above
        // nominal spherical is polar axis in horizontal plane at a chosen global azimuth, with nominal azimuth zero upward, positive azimuth clockwise looking outward
        // "heading" property sets the global azimuth of the nominal axis: 0 = forward, 90 = rightward, 180 = rearward, 270 or -90 = leftward
        // TODO: fix comments
        String headingString = Main.getProperties().getProperty(name_ + ".camera-heading");
        double heading = headingString != null ? Double.parseDouble(headingString) : 0;
        double polar = Double.parseDouble(Main.getProperties().getProperty(name_ + ".camera-polar-angle", headingString != null ? "90" : "0"));
        // global/nominal transforms
        nominalToGlobal = multiply(/*horizontal to vertical*/rotateY(polar), /*azimuth*/rotateZ(heading));
        
        // TOD: redo as polar/heading/tilt adjustments
        // right(+)/left pan
        double rollCorrection = Double.parseDouble(Main.getProperties().getProperty(name_ + ".camera-roll-correction", "0"));
        // up(+)/down tilt
        double pitchCorrection = Double.parseDouble(Main.getProperties().getProperty(name_ + ".camera-pitch-correction", "0"));
        // cc(+)/cw rotation
        double yawCorrection = Double.parseDouble(Main.getProperties().getProperty(name_ + ".camera-yaw-correction", "0"));

        // TODO: mostly tested with this...
        //double[][] actualToNominal = rotateXYZ(-yawCorrection, -pitchCorrection, -rollCorrection);
        // TODO: ... but this may be better
        double[][] actualToNominal = rotateZYX(-rollCorrection, -pitchCorrection, -yawCorrection);
        
        backTransformGlobal_ = multiply(actualToNominal, nominalToGlobal);
        //backTransformGlobal_ = multiply(nominalToGlobal, actualToNominal);
        backTransformNominal_ = actualToNominal;
        
        forwardTransform_ = null;
        forwardTransformGlobal_ = null;
        
        view_ = null;
        global_ = false;
        mask_ =  null;
        bounds_ = null;
        viewToRendering_ = null;
        fovAngleVertical_ = 0;
        
        focalLength_ = 0;
    }
    
    /**
     * Rendering projection instance constructor.
     * @param name Configuration property key.
     * @param view Output video view.
     */
    protected AbstractProjection(String name, RenderedView view) {
        this(name, view, 0, 0);
    }
        
    /**
     * Rendering projection instance constructor.
     * @param name Configuration property key.
     * @param view Output video view.
     * @param defaultFOV Horizontal field of view if not specified in configuration properties (degrees).
     */
    protected AbstractProjection(String name, RenderedView view, double defaultFOV) {
        this(name, view, defaultFOV, 0, 0);
    }
    
    /**
     * Rendering projection instance constructor.
     * @param name Configuration property key.
     * @param view Output video view.
     * @param defaultFOV Horizontal field of view if not specified in configuration properties (degrees).
     * @param defaultK Projection K value if not specified in configuration properties.
     */
    protected AbstractProjection(String name, RenderedView view, double defaultFOV, double defaultK) {
        this(name, view, defaultFOV, defaultK, 0);
    }
    
    /**
     * Rendering projection instance constructor.
     * @param name Configuration property key.
     * @param view Output video view.
     * @param defaultFOV Horizontal field of view if not specified in configuration properties (degrees).
     * @param defaultK Projection K value if not specified in configuration properties (ignored in this case, since setK overrides it).
     * @param setK Fixed projection K value.
     */
    protected AbstractProjection(String name, RenderedView view, double defaultFOV, double defaultK, double setK) {

        view_ = view;
        name_ = name;

        Dimension viewResolution = view_.getResolution();

        String[] bounds = Main.getProperties().getProperty(name_ + ".bounds", "0,0,1,1").split(",");
        boolean[][] mask = null;
        try {
            int mc = Integer.parseInt(Main.getProperties().getProperty(name_ + ".mask-color"), 16) & 0x00FFFFFF;
            mask = new boolean[viewResolution.width][viewResolution.height];
            if (view_.getLayoutMask() != null) {
                for (int x = 0; x < mask.length; x++) {
                    for (int y = 0; y < mask[x].length; y++) {
                        mask[x][y] = mc == (view_.getLayoutMask().getRGB(x, y) & 0x00FFFFFF);
                    }
                }
            }
        } catch (Exception ex) {
        }
        mask_ = mask;

        bounds_ = new Rectangle2D.Double();
        try {
            bounds_.setRect(Double.parseDouble(bounds[0]) * viewResolution.width, Double.parseDouble(bounds[1]) * viewResolution.height,
                            Double.parseDouble(bounds[2]) * viewResolution.width, Double.parseDouble(bounds[3]) * viewResolution.height);
        } catch (Exception ex) {
            ex.printStackTrace(); // TODO: warning
            bounds_.setRect(0, 0, viewResolution.width, viewResolution.height);
        }
        
        global_ = Boolean.valueOf(Main.getProperties().getProperty(name_ + ".global"));

        String flip = Main.getProperties().getProperty(name_ + ".flip", "");
        String rotate = Main.getProperties().getProperty(name_ + ".rotate", "0");
        viewToRendering_ = new AffineTransform();
        viewToRendering_.translate(bounds_.getWidth()/2, bounds_.getHeight()/2);
        try {
            viewToRendering_.rotate(Math.toRadians(Double.parseDouble(rotate))); // degrees CW
            viewToRendering_.scale("H".equalsIgnoreCase(flip) ? -1 : 1, "V".equalsIgnoreCase(flip) ? -1 : 1);
        } catch (Exception ex) {
            ex.printStackTrace(); // TODO: warning
        }
        viewToRendering_.translate(-bounds_.getWidth()/2, -bounds_.getHeight()/2);
        viewToRendering_.translate(-bounds_.getX(), -bounds_.getY());
        
        double woc = Double.parseDouble(Main.getProperties().getProperty(name_ + ".pan-direction", "0")) * Math.PI / 180;
        double wfc = Double.parseDouble(Main.getProperties().getProperty(name_ + ".pan-amount", "0")) * Math.PI / 180;
        double rot = Double.parseDouble(Main.getProperties().getProperty(name_ + ".pan-rotation", "0")) * Math.PI / 180;
        
        // straight ahead
        //double woc = 0;
        //double wfc = 0;
        
        // 45 degrees up
        //double woc =  0 * Math.PI/180.;
        //double wfc = 45 * Math.PI/180.;
        
        // 60 degrees right
        //double woc = 90 * Math.PI/180.;
        //double wfc = 60 * Math.PI/180.;

        if ((int)100*woc != 0 || (int)100*wfc != 0 || (int)100*rot != 0) {
            double[][] panAzimuth = {{Math.cos(woc), -Math.sin(woc), 0}, {Math.sin(woc), Math.cos(woc), 0}, {0, 0, 1}};
            double[][] panAngle = {{Math.cos(wfc), 0, Math.sin(wfc)}, {0, 1, 0}, {-Math.sin(wfc), 0, Math.cos(wfc)}};
            double[][] panReverseAzimuth = {{Math.cos(-woc), -Math.sin(-woc), 0}, {Math.sin(-woc), Math.cos(-woc), 0}, {0, 0, 1}};
            double[][] panRotation = {{Math.cos(rot), -Math.sin(rot), 0}, {Math.sin(rot), Math.cos(rot), 0}, {0, 0, 1}};
            forwardTransform_ = multiply(multiply(panAzimuth, panAngle), multiply(panReverseAzimuth, panRotation));
        }
        else {
            forwardTransform_ = null;
        }
        
        
        
        double[][] globalToNominal = {{1,0,0},{0,1,0},{0,0,1}};
        // global spherical is polar axis upward, azimuth zero forward, and positive azimuth clockwise from above
        // nominal spherical is polar axis in horizontal plane at a chosen global azimuth, with nominal azimuth zero upward, positive azimuth clockwise looking outward
        // "heading" property sets the global azimuth of the nominal axis: 0 = forward, 90 = rightward, 180 = rearward, 270 or -90 = leftward
        // TODO: fix comments
        String headingString = Main.getProperties().getProperty(name_ + ".view-heading");
        double heading = headingString != null ? Double.parseDouble(headingString) : 0;
        double polar = Double.parseDouble(Main.getProperties().getProperty(name_ + ".view-polar-angle", headingString != null ? "90" : "0"));
        // global/nominal transforms
        globalToNominal = multiply(/*azimuth*/rotateZ(-heading), /*horizontal to vertical*/rotateY(-polar));
        
        forwardTransformGlobal_ = globalToNominal; // multiply(globalToNominal, forwardTransform_); 
        
        
        fovAngleHorizontal_ = Double.parseDouble(Main.getProperties().getProperty(name_ + ".horizontal-fov", "" + defaultFOV)) * Math.PI / 180;
        fovAngleVertical_ = Double.parseDouble(Main.getProperties().getProperty(name_ + ".vertical-fov", "0")) * Math.PI / 180;
        k_ = setK > 0 ? setK : Double.parseDouble(Main.getProperties().getProperty(name_ + ".k", "" + defaultK));
        focalLength_ = Double.parseDouble(Main.getProperties().getProperty(name_ + ".focal-length", "0"));
        
        videoSource_ = null;
        cameraFOVDiameter_ = 0;
        cameraFOVCenterX_ = 0;
        cameraFOVCenterY_ = 0;
        cameraOriginXYZ_ = null;
        backTransformGlobal_ = null;
        backTransformNominal_ = null;
    }
    
    @Override
    public double[] transformPixelToCartesian(double renderingX, double renderingY) {
        
        renderingX -= bounds_.getWidth()/2.;
        renderingY -= bounds_.getHeight()/2.;
        
        renderingX /= bounds_.getWidth()/2.;
        renderingY /= bounds_.getWidth()/2.;
        
        double x = renderingX;
        double y = renderingY;

        return new double[] {x, y};
    }

    @Override
    public double[] transformCartesianToPixel( double x, double y ) {

        Dimension resolution = videoSource_.getResolution();

        double renderingX = x;
        double renderingY = y;

        //renderingX *= cameraFOVDiameter_ * resolution.width / 2.;
        //renderingY *= cameraFOVDiameter_ * resolution.width / 2.;
        double r = (cameraFOVDiameter_ * Math.max(resolution.width, resolution.height)) / 2.;
        renderingX *= r;
        renderingY *= r;

        //renderingX += .5 * resolution.width;
        //renderingY += .5 * resolution.height;
        renderingX += cameraFOVCenterX_ * resolution.width; // [0 to w]
        renderingY += cameraFOVCenterY_ * resolution.height; // [h to 0]

        return new double[] {renderingX, renderingY};
    }
    
    /**
     * @return Focal length if explicitly set, or as computed from horizontal FOV.
     */
    public double getFocalLength() {
        if (focalLength_ > 0) {
                return focalLength_;
        }
        return 1 / forwardProjection(fovAngleHorizontal_/2);
    }

    @Override
    public double[] renderingProjection(double projectionX, double projectionY) { //TODO: warning about overriding this method and forwardProjection/backProjection
        
        double[] rt = transformCartesianToPolar(projectionX, projectionY);
        
        double radius = rt[0];
        double azimuth = rt[1];
        
        radius /= getFocalLength();
        
        double polar = backProjection(radius);
        
        if (polar < 0) return null; // out of range
        
        return new double[] {mod(azimuth, 2*Math.PI), mod(polar, Math.PI)};
    }
    
    @Override
    public double[] sourceProjection(double world_o_clock, double world_from_center) { //TODO: warning about overriding this method and forwardProjection
        
        double azimuth = world_o_clock;
        double radius = forwardProjection(world_from_center);
        
        if (radius < 0) return null; // out of range
       
        radius *= getFocalLength();
        
        return transformPolarToCartesian(radius, azimuth);
    }
    
    @Override
    public double[] transformRenderingToWorld(double azimuth, double polar) {
        return transform(azimuth, polar, isGlobal() ? forwardTransformGlobal_ : forwardTransform_);
    }
    
    @Override
    public double[] transformGlobalWorldToSource(double azimuth, double polar) {
        return transform(azimuth, polar, backTransformGlobal_);
    }

    @Override
    public double[] transformNominalWorldToSource(double azimuth, double polar) {
        return transform(azimuth, polar, backTransformNominal_);
    }
    
    @Override
    public double[] transformViewToRendering(double viewPixelX, double viewPixelY) {
        
        if (viewPixelX < bounds_.getX() || viewPixelX >= (bounds_.getX() + bounds_.getWidth())
         || viewPixelY < bounds_.getY() || viewPixelY >= (bounds_.getY() + bounds_.getHeight())) {
                return null;
        }

        if (mask_ != null) {
            int vpX = Math.max(0, Math.min(mask_.length-1, (int)Math.round(viewPixelX)));
            int vpY = Math.max(0, Math.min(mask_[0].length-1, (int)Math.round(viewPixelY)));
            if (!mask_[vpX][vpY]) {
                        return null;
                }
        }

        double[] renderingPixel = {viewPixelX, viewPixelY};

        viewToRendering_.transform(renderingPixel, 0, renderingPixel, 0, 1);

        return new double[] { renderingPixel[0], renderingPixel[1] };
    }
    
    @Override
    public boolean isGlobal() {
        return global_;
    }
  
    
    
    
    
    
    
    
    
    
    
    
    
    
    /**
     * Computes the normalized distance from the center of the projection space to the projected
     * location of an incident ray at a given polar angle from the view axis.
     * @param angle Polar angle \u03b8 between view axis and incident ray.
     * @return Polar radius between projection center and projected ray, as a fraction of focal length.
     */
    protected double forwardProjection(double angle) {
        throw new UnsupportedOperationException(); //TODO: appropriate message
    }
    
    /**
     * Computes the angle of an incident ray associated with a particular distance from the center of projection space.
     * @param radius Polar radius between projection center and projected ray, as a fraction of focal length.
     * @return Polar angle \u03b8 between view axis and incident ray.
     */
    protected double backProjection(double radius) {
        throw new UnsupportedOperationException(); //TODO: appropriate message
    }

    /**
     * Normalizes angles to range such as 0 - 2\u03c0.
     * @param angle Angle.
     * @param mod Divisor.
     * @return Modulus in the range 0 - mod.
     */
    protected static double mod(double angle, double mod) {
        return ((angle % mod) + mod) % mod;
    }
    
    
    
    protected static double[] transform(double world_o_clock, double world_from_center, double[][] transform) {

        if (transform == null) {
            return new double[] {world_o_clock, world_from_center};
        }
        
        double[] xyz = transformSphericalToCartesian(1, world_o_clock, world_from_center);
        double[][] zyxM = multiply(transform, new double[][] {{xyz[0]}, {xyz[1]}, {xyz[2]}});
        double[] rap = transformCartesianToSpherical(zyxM[0][0], zyxM[1][0], zyxM[2][0]);
        return new double[] {mod(rap[1], 2*Math.PI), mod(rap[2], Math.PI)};
    }
    /**
     * Cartesian to polar coordinate conversion. Uses projection conventions.
     * @param x -left, +right
     * @param y -up, +down
     * @return {
     * <br> radius.
     * <br> azimuth angle (0 up, +cw).
     * <br>}
     */
    public static double[] transformCartesianToPolar(double x, double y) {
        double radius = Math.sqrt(x*x + y*y);
        double azimuth = Math.atan2(x, -y);
        return new double[] {radius, mod(azimuth, 2*Math.PI)};
    }
    /**
     * Polar to cartesian coordinate conversion. Uses projection conventions.
     * @param radius Radius.
     * @param azimuth Azimuth angle (0 up, +cw).
     * @return {
     * <br> x, (-left, +right).
     * <br> y (-up, +down).
     * <br>}
     */
    public static double[] transformPolarToCartesian(double radius, double azimuth) {
        double x = radius * Math.sin(azimuth);
        double y = -radius * Math.cos(azimuth);
        return new double[] {x, y};
    }
    /**
     * Cartesian to spherical coordinate conversion. Uses world conventions.
     * @param x (-back, +front).
     * @param y (-left, +right).
     * @param z (-down, +up).
     * @return {
     * <br> Radius.
     * <br> Azimuth angle (0 forward, +clockwise from above).
     * <br> Polar angle (0 upward, +downward).
     * <br>}
     * @return {radius, azimuth angle, polar angle}
     */
    public static double[] transformCartesianToSpherical(double x, double y, double z) {
        double radius = Math.sqrt(x*x + y*y + z*z);
        double azimuth = Math.atan2(y, x);
        double polar = Math.acos(z/radius);
        return new double[] {radius, mod(azimuth, 2*Math.PI), mod(polar, Math.PI)};
    }
    /**
     * Spherical to cartesian coordinate conversion. Uses world conventions.
     * @param radius Radius
     * @param azimuth Azimuth angle (0 forward, +clockwise from above).
     * @param polar Polar angle (0 upward, +downward).
     * @return {
     * <br> x, (-back, +front).
     * <br> y, (-left, +right).
     * <br> z (-down, +up).
     * <br>}
     */
    public static double[] transformSphericalToCartesian(double radius, double azimuth, double polar) {
        double x = radius * Math.sin(polar) * Math.cos(azimuth);
        double y = radius * Math.sin(polar) * Math.sin(azimuth);
        double z = radius * Math.cos(polar);
        return new double[] {x, y, z};
    }
    /**
     * Matrix multiplication.
     * @param a Matrix a.
     * @param b Matrix a.
     * @return a X b.
     */
    public static double[][] multiply(double[][] a, double[][] b) {
        int n = b.length;
        int rows = a.length;
        int cols = b[0].length;
        double[][] p = new double[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                for (int i = 0; i < n; i++) {
                    p[r][c] += a[r][i] * b[i][c];
                }
            }
        }
        return p;
    }
    
    /**
     * Constructs a 3D rotation matrix.
     * @param degrees Degrees rotation about X axis.
     * @return 3x3 rotation matrix.
     */
    public static double[][] rotateX(double degrees) {
        double radians = Math.toRadians( degrees );
        double[][] matrix = {{1, 0, 0}, {0, Math.cos(radians), -Math.sin(radians)}, {0, Math.sin(radians), Math.cos(radians)}};
        return matrix;
    }

    /**
     * Constructs a 3D rotation matrix.
     * @param degrees Degrees rotation about Y axis.
     * @return 3x3 rotation matrix.
     */
    public static double[][] rotateY(double degrees) {
        double radians = Math.toRadians( degrees );
        double[][] matrix = {{Math.cos(radians), 0, Math.sin(radians)}, {0, 1, 0}, {-Math.sin(radians), 0, Math.cos(radians)}};
        return matrix;
    }

    /**
     * Constructs a 3D rotation matrix.
     * @param degrees Degrees rotation about Z axis.
     * @return 3x3 rotation matrix.
     */
    public static double[][] rotateZ(double degrees) {
        double radians = Math.toRadians( degrees );
        double[][] matrix = {{Math.cos(radians), -Math.sin(radians), 0}, {Math.sin(radians), Math.cos(radians), 0}, {0, 0, 1}};
        return matrix;
    }
    
    /**
     * Constructs a 3D rotation matrix.
     * @param degreesX Degrees rotation about X axis.
     * @param degreesY Degrees rotation about Y axis.
     * @param degreesZ Degrees rotation about Z axis.
     * @return <code>rotateX(degreesX) X rotateY(degreesY) X rotateZ(degreesZ)</code>
     */
    public static double[][] rotateXYZ(double degreesX, double degreesY, double degreesZ) {
        return multiply(rotateX(degreesX), multiply(rotateY(degreesY), rotateZ(degreesZ)));
    }
    
    /**
     * Constructs a 3D rotation matrix.
     * @param degreesZ Degrees rotation about Z axis.
     * @param degreesY Degrees rotation about Y axis.
     * @param degreesX Degrees rotation about X axis.
     * @return <code>rotateX(degreesX) X rotateY(degreesY) X rotateZ(degreesZ)</code>
     */
    public static double[][] rotateZYX(double degreesZ, double degreesY, double degreesX) {
        return multiply(rotateZ(degreesZ), multiply(rotateY(degreesY), rotateX(degreesX)));
    }
}
