package org.usfirst.frc.team2077.vision.processors;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.usfirst.frc.team2077.vision.Main.FrameProcessor;
import org.usfirst.frc.team2077.vision.NTMain;

import edu.wpi.first.networktables.NetworkTableEntry;

public class BirdseyeVisionOverlay implements FrameProcessor {

    private final String ntKey_;
    private NetworkTableEntry ntEntry_ = null;
    private final String ntTriggerKey_ = "Trigger";
    private NetworkTableEntry ntTriggerEntry_ = null;
    
    protected final double inchesWidth_;
    protected double scale_ = 0;
    protected Point origin_;
    
    protected double distanceToTarget_;
    protected double angleToTarget_;
    protected double angleFromTarget_;
    protected int cameraStatus_ = 0b1111;
    
    protected boolean estimated_;
    protected boolean aligned_;
    
    protected double lastDistanceToTarget_ = 9999;
    protected double lastAngleToTarget_ = 0;
    protected double lastAngleFromTarget_ = 0;
    
    protected final Scalar black_ = new Scalar(0,0,0,255);
    protected final Scalar blue_ = new Scalar(128,0,0,255);
    protected final Scalar blue2_ = new Scalar(255,0,0,255);
    protected final Scalar white_ = new Scalar(255,255,255,255);
    protected final Scalar gray_ = new Scalar(128,128,128,255);
    protected final Scalar green_ = new Scalar(0,255,0,255);
    protected final Scalar red_ = new Scalar(0,0,255,255);
    protected final Scalar gray2_ = new Scalar(90,90,90,255);
    protected final Scalar gray3_ = new Scalar(100,100,100,255);
    protected final Scalar grayT_ = new Scalar(128,128,128,128);
    protected final Scalar greenT_ = new Scalar(0,255,0,128);
    protected final Scalar redT_ = new Scalar(0,0,255,128);
   
    public BirdseyeVisionOverlay(String ntKey, double inchesWidth) {
        ntKey_ = ntKey;
        inchesWidth_ = inchesWidth;
    }

    @Override
    public void processFrame( Mat frameMat, Mat overlayMat ) {
        if ( ntEntry_ == null ) {
            if ( NTMain.networkTable_ == null || (ntEntry_ = NTMain.networkTable_.getEntry( ntKey_ )) == null ) {
                return;
            }
        }
        
        boolean trigger = false;
        if ( ntTriggerEntry_ == null ) {
            if ( NTMain.networkTable_ == null || (ntTriggerEntry_ = NTMain.networkTable_.getEntry( ntTriggerKey_ )) == null ) {
            }
            else trigger = ntTriggerEntry_.getBoolean( false );
        }
        
        if ( scale_ == 0 ) {
            scale_ = overlayMat.cols() / inchesWidth_;
            origin_ = new Point( overlayMat.cols()/2., overlayMat.rows()/2. );
        }
        
        // robot graphic
        Imgproc.line( overlayMat, worldToMat(-14,-16), worldToMat(14,-16), blue_, (int)Math.round(scale_*3*2));
        Imgproc.line( overlayMat, worldToMat(14,-16), worldToMat(14,16), blue_, (int)Math.round(scale_*3*2));
        Imgproc.line( overlayMat, worldToMat(14,16), worldToMat(-14,16), blue_, (int)Math.round(scale_*3*2));
        Imgproc.line( overlayMat, worldToMat(-14,16), worldToMat(-14,-16), blue_, (int)Math.round(scale_*3*2));
        Imgproc.rectangle( overlayMat, worldToMat(-14,-16), worldToMat(14,16), trigger ? white_ : gray_, -1 );
        
        double[] targetData = ntEntry_.getDoubleArray( new double[0] );
        if ( targetData != null && targetData.length >= 3 && targetData[0] > 0 ) {

            //System.out.println( ntKey_ + ":" + (int)Math.round(targetData[0]) + " / " + (int)Math.round(targetData[1]) + " / " + (int)Math.round(targetData[2]) );

            cameraStatus_ = targetData.length >= 4 ? (int)Math.round( targetData[3] ) : -1;
            distanceToTarget_ = targetData[0]; // in inches
            angleToTarget_ = (distanceToTarget_ < 24 && cameraStatus_ != 0b1111) ? 0 : Math.toRadians(targetData[1]);
            angleFromTarget_ = (distanceToTarget_ < 24 && cameraStatus_ != 0b1111) ? 0 : Math.toRadians(targetData[2]);
            
            estimated_ = distanceToTarget_ < 0 && lastDistanceToTarget_ > 0 && lastDistanceToTarget_ < 24;
            if ( estimated_ ) {
                distanceToTarget_ = lastDistanceToTarget_;
                angleToTarget_ = lastAngleToTarget_;
                angleFromTarget_ = lastAngleFromTarget_;
            }
            else {
                lastDistanceToTarget_ = distanceToTarget_;
                lastAngleToTarget_ = angleToTarget_;
                lastAngleFromTarget_ = angleFromTarget_;
            }
            
            double cameraX = 0;
            double cameraY = 2.5; // vision center 2.5" ahead of robot center
            double targetX = distanceToTarget_ * Math.sin(angleToTarget_) + cameraX;
            double targetY = distanceToTarget_ * Math.cos(angleToTarget_) + cameraY;
            
            aligned_ = Math.abs(Math.toDegrees(angleFromTarget_)) < 4 && Math.abs(Math.toDegrees(angleToTarget_)) < 4;
     
            // target graphic
            double targetWidth = 18; // inch width of target / 2
            double t0 = /*angleToTarget_*/ -angleFromTarget_ - Math.PI/2;
            double t1 = /*angleToTarget_*/ -angleFromTarget_ + Math.PI/2;
            double tx0 = targetX + targetWidth * Math.sin(t0);
            double ty0 = targetY + targetWidth * Math.cos(t0);
            double tx1 = targetX + targetWidth * Math.sin(t1);
            double ty1 = targetY + targetWidth * Math.cos(t1);
            Imgproc.line( overlayMat, worldToMat(tx0, ty0), worldToMat(tx1, ty1), estimated_ ? gray_ : aligned_ ? green_ : red_, 8);

            if ( distanceToTarget_ < 72 ) { // && ( distanceToTarget_ > 24 || cameraStatus_ == 0b1111 ) ) {
                
                // alignment lane projected from target       
                double angleLane = /*angleToTarget_*/ -angleFromTarget_ + Math.PI;
                double laneLength = 200; // at least far enough to reach the robot
                double cx = targetX + laneLength * Math.sin(angleLane);
                double cy = targetY + laneLength * Math.cos(angleLane);
                double laneWidth = 42; // inch width of lane at distance laneLength / 2
                double lx0 = cx + laneWidth * Math.sin(t0);
                double ly0 = cy + laneWidth * Math.cos(t0);
                double lx1 = cx + laneWidth * Math.sin(t1);
                double ly1 = cy + laneWidth * Math.cos(t1);
                Imgproc.line( overlayMat, worldToMat(tx0, ty0), worldToMat(lx0, ly0), estimated_ ? grayT_ : aligned_ ? greenT_ : redT_, 4);
                Imgproc.line( overlayMat, worldToMat(tx1, ty1), worldToMat(lx1, ly1), estimated_ ? grayT_ : aligned_ ? greenT_ : redT_, 4);
            }
            
            // camera status graphic
            if ( cameraStatus_ >= 0 ) {
                boolean[] status = { (cameraStatus_ & 0b1000)!=0, (cameraStatus_ & 0b0100)!=0, (cameraStatus_ & 0b0010)!=0, (cameraStatus_ & 0b0001)!=0 };
                int[] x = { -11, -5, 5, 11 };
                int y = 13;
                int r = 14;
                for ( int i = 0; i < 4; i++ ) {
                    Imgproc.circle( overlayMat, worldToMat(x[i], y), r, status[i] ? blue2_ : gray2_, -1);
                }
            }
            
            // heading line
            Imgproc.line( overlayMat, new Point(overlayMat.cols()/2, 0), new Point(overlayMat.cols()/2, overlayMat.rows()/2), aligned_ ? green_ : red_, 4 );
            
            if ( distanceToTarget_ > 0 ) { // vision lock
                Imgproc.rectangle( overlayMat, new Point(0,0), new Point(overlayMat.cols(),overlayMat.rows()), green_, 32 );
            }   
        }
    }
    
    protected Point worldToMat(double x, double y) {
        return new Point( x * scale_ + origin_.x, -y * scale_ + origin_.y );
    }
}

