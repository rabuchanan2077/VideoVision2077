package org.usfirst.frc.team2077.vision.processors;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.usfirst.frc.team2077.vision.Main.FrameProcessor;

public class BallBirdseyeOverlay extends BirdseyeVisionOverlay implements FrameProcessor {

    public BallBirdseyeOverlay() {
        super( "Ball Target Data", 240 );
    }
    
    @Override
    public void processFrame( Mat frameMat, Mat overlayMat ) {
        super.processFrame( frameMat, overlayMat );

        Imgproc.putText ( overlayMat, "BALL", new Point(20, 100), Core.FONT_HERSHEY_SIMPLEX, 3, new Scalar(255, 255, 255, 255), 4 );
    }
}
