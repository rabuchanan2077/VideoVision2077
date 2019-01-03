/**
 * Multi-source video projection and display framework.
 * <p>
 * This software is a collection of video processing components designed primarily for use in
 * FIRST Robotics Competition systems. It is based on an earlier DIY implementation of a
 * multi-camera overhead "bird's eye" video systems similar to those increasingly common
 * on cars. It was further developed during the 2018 FRC season and used in competition
 * with moderate success. This version includes refinements to that code base, along with
 * pluggable source and projection classes - primarily for flexibility in handling a wider
 * variety of cameras and video streaming mechanisms, but also just as an interesting
 * programming exercise. Some of the code here may be of more academic than practical interest.
 * <p>
 * The main class reads a system configuration file defining a set of input sources
 * and a set of views. In use, sources are generally video cameras, and views are either
 * screen display components for use by the robot operator, or memory-mapped frame buffers
 * for use by computer vision code in other processes.
 * <p>
 * Video sources may feed one or more views, and views may use one or more sources.
 * Sources are associated with mathematical projections describing how the image pixels
 * map to spherical viewing angles relative to the camera lens axis, or to
 * a global coordinate system common to all sources (cameras) in the system. When a source
 * is fed to a view, a rendering projection describes how those spherical coordinates
 * are mapped into the view pixels.
 * <p>
 * The source -&gt; source projection -&gt; global -&gt; rendering projection -&gt; view pixel
 * mappings are resolved end-to-end when the first frame is processed, into a source -&gt; view
 * pixel index lookup table. Later frame updates bypass all projection and coordinate transform
 * math, using a single-layer lookup table to update view pixels from source frames.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
package org.usfirst.frc.team2077.video;
