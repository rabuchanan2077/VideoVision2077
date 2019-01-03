/**
 * Plug-in lens and viewing projections and coordinate transforms.
 * <p>
 * Source projections permit normalizing video output from a wide range of cameras, wide angle
 * and fisheye lenses in particular, into a standardized coordinate system. Rendering projections
 * facilitate merging and reformatting the video into display formats optimized for particular
 * user or image processing tasks.
 * <p>
 * Along with an abstract base class, this package includes a family of classical lens projections.
 * with base classes supporting continuous adjustment between them. Most ordinary camera lenses,
 * from standard to wide angle to fisheye, may be reasonably approximated with one of these. They
 * are also useful for general video display, allowing optimization of viewing width (field of view)
 * vs type and location of shape and distance distortion.
 * <p>
 * Also included are several special purpose projections that don't directly correspond to common
 * camera lenses, but can be useful for particular applications. 
 * <p>
 * While source projections and viewing
 * projections are defined as separate interfaces, most classes here implement both,
 * for purposes of code organization and convenience in testing.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
package org.usfirst.frc.team2077.video.projections;
