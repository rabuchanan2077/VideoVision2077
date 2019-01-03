package org.usfirst.frc.team2077.vvcommon;

import java.awt.Dimension;
import java.io.Serializable;
import java.nio.ByteOrder;

public class MappedFrameInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;

    public final String byteOrder_;
    public final Dimension resolution_;
    public final String frameFile_;
    public final String overlayFile_;
    public final int overlayNotificationPort_;

    public MappedFrameInfo(ByteOrder byteOrder, Dimension resolution, String frameFile, String overlayFile, int overlayNotificationPort) {
        byteOrder_ = byteOrder == ByteOrder.BIG_ENDIAN ? "BE" : "LE";
        resolution_ = resolution;
        frameFile_ = frameFile;
        overlayFile_ = overlayFile;
        overlayNotificationPort_ = overlayNotificationPort;
    }

    @Override
    public String toString() {
        return byteOrder_ + " " + resolution_ + " " + frameFile_ + " " + overlayFile_ + " " + overlayNotificationPort_;
    }

} // MappedFrameInfo
