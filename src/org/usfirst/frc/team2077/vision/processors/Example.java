package org.usfirst.frc.team2077.vision.processors;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.usfirst.frc.team2077.vision.Main.FrameProcessor;

public class Example implements FrameProcessor {

    @Override
    public void processFrame( Mat frameMat, Mat overlayMat ) {

        long t0 = System.currentTimeMillis();

        // convert video frame to grayscale
        Mat bgr = new Mat();
        Imgproc.cvtColor( frameMat, bgr, Imgproc.COLOR_BGRA2BGR );
        Mat gray = new Mat( frameMat.rows(), frameMat.cols(), CvType.CV_8U );
        Imgproc.cvtColor( bgr, gray, Imgproc.COLOR_BGR2GRAY );
        // pre-filter and line detect
        Imgproc.equalizeHist( gray, gray );
        Imgproc.GaussianBlur( gray, gray, new Size( 15, 15 ), 0, 0 );
        Imgproc.Canny( gray, gray, 20, 10 );
        // copy the processed frame back to the vision frame
        Imgproc.cvtColor( gray, frameMat, Imgproc.COLOR_GRAY2BGRA );

        // do something opencv-ish, like look for lines
        Mat lines = new Mat();
        Imgproc.HoughLinesP(gray, lines, 1, Math.PI/180, 100, 100, 10);
        for (int x = 0; x < lines.rows(); x++) {
            double[] l = lines.get(x, 0);
            // draw lines on the the vision frame and the video overlay, remembering the alpha channel
            Imgproc.line(frameMat, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(0, 0, 255, 255), 2, Imgproc.LINE_AA, 0);
            Imgproc.line(overlayMat, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(0, 0, 255, 128), 2, Imgproc.LINE_AA, 0);
        }

        System.out.println( " processFrame time " + (System.currentTimeMillis() - t0) + "ms" );
    }
}
