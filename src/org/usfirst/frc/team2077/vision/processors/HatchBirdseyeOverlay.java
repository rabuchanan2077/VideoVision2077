package org.usfirst.frc.team2077.vision.processors;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.usfirst.frc.team2077.vision.NTMain;
import org.usfirst.frc.team2077.vision.Main.FrameProcessor;

import edu.wpi.first.networktables.NetworkTableEntry;

public class HatchBirdseyeOverlay extends BirdseyeVisionOverlay implements FrameProcessor {

    private final String ntKey_ = "Hatch Line Follower";
    private NetworkTableEntry ntEntry_ = null;

    public HatchBirdseyeOverlay() {
        super( "Hatch Target Data", 240 );
    }
    
    @Override
    public void processFrame( Mat frameMat, Mat overlayMat ) {
    
        super.processFrame( frameMat, overlayMat );

        if ( ntEntry_ == null ) {
            if ( NTMain.networkTable_ == null || ( ntEntry_ = NTMain.networkTable_.getEntry( ntKey_ ) ) == null ) {
                return;
            }
        }

        double[] lineData = ntEntry_.getDoubleArray( new double[0] );
        if ( lineData == null || lineData.length < 8 ) { //|| distanceToTarget_< 0 || distanceToTarget_ > 20 ) {
            return;
        }
        
//        for (double d : lineData) {
//            System.out.print( d + " " );
//        }
//        System.out.println();
        
        int w = 40;
        int h = 60;
        int x = overlayMat.cols()/2 - 4*w;
        int y = 10 + h ;
        for (double d : lineData) {
            Imgproc.rectangle( overlayMat, new Point(x, y), new Point(x+w, y+h), new Scalar(0, 255-(int)d, (int)d, 255), -1 );
            Imgproc.rectangle( overlayMat, new Point(x, y), new Point(x+w, y+h), new Scalar(0, 0, 0, 255), 2 );
            x += w;
        }
        
        Imgproc.putText ( overlayMat, "HATCH", new Point(20, 100), Core.FONT_HERSHEY_SIMPLEX, 3, new Scalar(255, 255, 255, 255), 4 );
    }
}
