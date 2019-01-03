FRC Team 2077 Video/Vision Framework
====================================

History and Purpose
-------------------

This software is a collection of video processing components designed
primarily for use in FIRST Robotics Competition systems. It is based on
previous experiments with a DIY multi-camera overhead "bird's eye" video
system similar to those increasingly common on cars. It was further
developed during the 2018 FRC season and used in competition with
moderate success. This version includes refinements to that code base,
along with pluggable source and projection classes - primarily for
flexibility in handling a wider variety of cameras and video streaming
mechanisms, but also just as an interesting programming exercise. Some
of the code here may be of more academic than practical interest.

Beyond the original challenge of implementing a usable bird's eye video
display within the technical context of the FRC competition environment,
it is meant to explore the the possibility of greatly increasing the
available field of view for both driver and image processing video
feeds.

Design Goals, Constraints, and Practical Limitations
----------------------------------------------------

### Functional Goals ^\*^

-   Maximize field of view.
-   Multi-camera support, including "stitching" multiple video feeds.
-   Projection conversions including, but not limited to, "bird's eye"
    views.
-   Support external processing, e.g. computer vision, using generated
    views.
-   Customizable video display.

#### ^\*^ Why not just use a 360 degree camera?

​1) Generally, they're made to record, not feed video in real time.

​2) Where would you mount it? Vehicles generally have important stuff in
the middle, and cameras have to go somewhere around the perimeter.

​3) Making the most of non-coincident cameras is a big part of the
challenge. It's more fun, and you learn more doing it.

### Performance Priorities

1.  Minimal latency.
2.  Minimal latency.
3.  Minimal latency.
4.  Comfortably within bandwidth limits.
5.  Reliability.
6.  Isolate video system performance from vision processing load.
7.  Reasonable frame rate.
8.  Acceptable image quality.

Generally, acceptable performance thresholds are based on previous
experience - to meet or exceed performance of the multiple Axis camera
setups used in past years, with added functionality. \
 Rationale:

-   Attainable by definition (it's been done before).
-   Acceptable by definition (it's been accepted before),
-   Permits project to focus on functionality. Goal is not improving
    performance, but adding capability without breaking performance.

### Anti-Requirements

Things being deliberately avoided...

-   Video streaming implementation.
-   Real-time projection transform calculations. ^**\***^
-   Significant new load on the main robot computer.
-   Entanglements with FRC/WPI software.

^**\***^ While video projection conversion is quite similar to VR and
video gaming applications where the viewpoint changes with observer
motion, what's done here is a subset of what they use. Cameras here are
considered fixed with respect to the global coordinate system, which is
defined relative to the machine on which they're mounted. They handle
motion because the cameras themselves move. Projections don't change as
they do when the view vectors change. This is important because fixed
(even configurable or multiple) viewpoints allow the projection math to
be executed only once and the mappings re-used as new frame data
arrives. Acceptable performance with constantly moving view vectors
generally requires highly optimized transform code running on graphics
processing hardware, which is outside the scope of this project.

### Key Design Choices

#### Execute most video and vision processing on the drive station computer.

The two main considerations here are robot/DS networking and CPU load.
Moving video processing to the robot in principle permits high bandwidth
local video feeds, with only the final processed video being actually
moved across the wireless/field connection. But no matter what,
significant bandwidth or compression remains necessary. If networking
bandwidth and latency goals can be met, processing on the DS makes it
easier to apply more CPU resources. This choice was made after extensive
experimentation with Gstreamer led to the conclusion that adequate
networking performance for several high resolution (though not
necessarily high quality!) camera feeds was achievable.

#### No networking through main robot computer (RoboRIO), and no direct interaction with standard robot or drive station software.

This preference is based on years of good experience with Axis network
cameras feeding either custom applications or web pages. Keeping video
networking and processing off system with other priorities reduces the
risk of interfering with them, and it's not particularly hard to do.

#### Limit image pixel transforms to opaque nearest-neighbor copies.

High quality image transforms generally use interpolation, and high
quality image blending generally uses either interpolation or alpha
blending. Avoiding these entails a significant sacrifice in final image
quality, but improves speed, sometimes significantly. While the
possibility exists to re-introduce some of these techniques later, the
decision was made early to start with the fastest possible mechanism for
best possible performance. The present implementation is slow processing
the first frame, since all the math must be done at least once, but
subsequent frames go through only a single level integer lookup table
and are quite fast. Pre-loaded or persistent mapping tables would
eliminate most of the first-frame overhead and will likely be added at
some point. Manual pre-loading by starting the system in advance and
clicking through each display option is the best approach now.

Principles of Operation
-----------------------

### Projections

Source (camera lens) and rendering (output or screen display)
projections are the central feature of this framework. Other software
components supply video feeds, display them, or support system
configuration and initialization.

Image projections, such as rectilinear (a.k.a pinhole perspective), wide
angle, panorama, and fisheye, are traditionally understood as properties
of camera lenses, with the final images usually rendered simply as
enlarged versions of what hits the camera's film or digital collector.

Digital image processing can introduce additional steps and
capabilities. Where the lens projection is known, the collected image
data may be mathematically back-projected and re-projected to appear as
though shot through an entirely different lens. Software can also take
multiple images from different cameras or viewing angles and merge them
into composite images with much larger fields of view than the
individual cameras they came from. The key to both operations is correct
mathematical modeling of the input lens projections and separate
application of the downstream viewing projections.

### Video Sources

This system depends on video streaming but explicitly avoids any
requirement to implement it. Streaming input is designed to be supplied
via pluggable video source components linked to external software tools
that handle format conversion, networking, etc, etc, etc. While the
video source interface is meant not to assume a particular
implementation, the system was designed with specific attention to
[Gstreamer](https://gstreamer.freedesktop.org), a well established
multi-platform media framework.

As background for this project a lot of time was spent trying things
with Gstreamer. Many slow configurations are possible, but at least some
can be quite fast. Detailed discussion of Gstreamer pipelines is out of
scope here, but so far best performance has come with highly compressed
H.264 feeds generated either directly on the camera hardware, or in the
GPU of a Raspberry Pi SBC, then networked via RTP/UDP. While this
combination has performed extremely well under a variety of test
conditions, reliability under competition/FMS conditions was often bad.
We haven't figured this out yet.

#### Note regarding USB cameras.

Simply attaching multiple high bandwidth USB cameras to a single host
frequently doesn't work well. There are several reasons for this, but
any such configuration should be tested carefully. USB 3.0, and USB 3.0
cameras should eventually change this.

### Display and Output

Forward projected video goes two places: screen components, and
memory-mapped files. Screen components are JComponents for people to
look at, and are mostly self-explanatory. Memory-mapped files are for
other programs to look at.

The primary design case for the latter is vision processing. The video
system writes the latest frame from each mapped (enabling mapping is an
optional function) video view into an externally accessible file/memory
zone. Other processes can read frames as rapidly or slowly as they
like/can without affecting the operation of the video producer. If
frames are read faster than they're written, they're re-read and
re-processed. In the more common case where the reader is slower, the
reader always gets the latest frame, simply skipping any that have gone
by in in the meantime

#### Views and Renderings

Views are displayable or mappable output components. Renderings are
associations between views and video sources that project into them. If
the view uses only one source, a single rendering is implied and needn't
be explicitly configured. If, as in the case where a view is a stitched
together assembly of several video feeds, the source and projection for
each is specified in its own set of rendering properties.

### Configuration and Startup

For flexibility in building and editing video system configurations,
components are specified not in source code but in configuration files
in standard java.util.Properties format. The main program reads one or
more of these files, and initializes the sources and views it describes,
then starts the sources. The individual source and view components, as
they are constructed, initialize dependent rendering and projection
objects as described in their own properties.

Usage
-----

While it is be possible to use this code as a programming API, it is
designed mainly as a configurable application, i.e. for most system
changes to be made in configuration files rather than in new source
code. In either case starting with simple configuration examples is the
best way to get a feel for how it works.

Run the video system by invoking org.usfirst.frc.team2077.video.Main
\<config1\>, \<config2\>... where the argument strings are paths to
configuration files in java.util.properties format. These paths may be
absolute or relative paths to external properties files, or abbreviated
references to files packaged with the software as resources. In this
case the argument is just the base file name without the directory path
or .properties suffix, e.g. to run the src/resources/hello.properties
example, pass "hello" in the argument list. The src/resources directory
includes several examples. The better ones are commented.

Most of the examples use a single configuration file; multiple files are
simply concatenated. The configuration file(s) must at minimum define a
set of input sources and a set of views. In use, sources are generally
video cameras, and views are either screen display components for use by
the robot operator, or memory-mapped frame buffers for use by computer
vision code in other processes.

Video sources may feed one or more views, and views may use one or more
sources. Sources are associated with mathematical projections describing
how the image pixels map to spherical viewing angles relative to the
camera lens axis, or to a global coordinate system common to all sources
(cameras) in the system. When a source is fed to a view, a rendering
projection describes how those spherical coordinates are mapped into the
view pixels.

Configured sources and views must be named "video0", "video1"... and
"view0", "view1"..., with no numbers skipped. They're loaded by looking
for numbers in order, and when the next number is missing, it stops,
with any later numbers ignored. Otherwise, key prefix names can be
whatever you want, within java.util.Properties rules. It's easier to
learn looking at examples than through rules. Individual property names
are supposed to be listed in the javadoc for the classes that read them.
It's likely there are omissions, though. In that case, look at the
examples or the source code. Property order only matters for human
readability or in case of duplicate keys (not recommended).

The main class by default creates a small UI window for toggling between
the configured views (if there's more than one). In final applications,
this UI control might be replaced by one that switched based on Network
Tables or some other mechanism. Otherwise, the main reason for
rebuilding code would be to add new plug-in classes, e.g. sources or
projections.

### Example Configurations

Views take significant time to initialize before the first frame is
displayed, on startup and when each view is first selected. There's no
progress indicator, so start with the simple ones and get a feel for the
delay. Complex (not realistic in practice) examples, especially with
many mapped views, may take quite a while to come up. Once a view has
been displayed once, re-selecting it should be very quick.

#### hello (see src/resources/hello.properties)

The simplest possible configuration. It depends on default values, and
on the fact that matching source and rendering projections generally
result in output matching input, even if the source projection doesn't
actually match the camera lens.

#### hello-gstreamer

A simple Gstreamer example.

#### hello-windows

Camera input example for Windows.

#### hello-linux

Camera input example for Linux.

#### gstreamer-cube

A fancier Gstreamer example, including some stitched-together renderings
and alternate projections of full 360 degree video.

#### gstreamer-cubemap

Another way to do what's in gstreamer-cube. Simpler, but not working
right. Use with care.

#### globe

This example illustrates some of the things that can be done with
rendering projections, including several that go beyond what's
ordinarily done with camera lenses. Technically, these projections are
inside-out views of an alternate earth, but the math mostly corresponds.
Don't look too closely at these for exact property configurations, lest
you confuse yourself, but they're good illustrations of things that can
be done.

#### test-360

Illustration of different projections of the same 360 degree image
space, viewed from different projections and look vectors. In
particular, different (simulated) 360 degree fisheye lenses, while they
cover the same world space, have better resolution at some angles than
others. When the space is re-projected in ways that enlarge low-res
edges, image quality is poor in those places. This example also includes
an alternate test image source in the comments.

#### views-360

Illustrates the some of the range of variability between different
fisheye lenses (or in this case rendering projections).

#### test-birdseye

An example of stitching together multiple cameras into an integrated
birdseye projection. The (simulated) cameras have deliberately
mismatched lens projections and asymetrical locations. It also
illustrates the use of an image mask to handle stitching together
renderings that overlap. Simply mapping overlapping projections in the
same view results in flickering, as the latest frame overlays whatever
was there. A mask image with colors for each rendering explicitly
resolves conflicts. Another use for a mask image is to provide fixed
objects in the display, here used to indicate camera locations and
chassis outline. Where the mask image pixels don't match any rendering
source, they don't get painted with video data and show through as-is.

#### pizzaTest

Shows the test images used to calibrate the camera feeds on one of our
R&D machines. "Pizza" refers to the name of the robot (low and flat like
a pizza box), not the food or anything else.

#### pizza

The real configuration for the robot/cameras simulated in pizzaTest.
It's worth looking at as an example of a more complex system using real
cameras with remote/local Gstreamer pipelines. It may be out of date
since various properties, etc. have changed since it was last run. But
it won't run anyway without the particular cameras and hosts it's set up
for.

#### vision

A simple example of setting up a video feed to a vision processor
running in a separate process. Launch both
org.usfirst.frc.team2077.video.Main and
org.usfirst.frc.team2077.vision.VisionMain with this file as an
argument. Each will create its own window, but the vision window is
intended more as a development/debugging display; the vision processor
can draw to a transparent overlay frame that is sent back to the video
display.

### Camera Lens Calibration

Doing anything interesting with this framework requires that the input
video streams (cameras) be correctly mapped into a spherical coordinate
system for the scene they're viewing. This is done with the source
projection. There are various ways to configure a video source for a
particular camera, mostly involving a fair amount of trial and error.
The FisheyeCalibration class is a semi-automated way to generate
reasonably accurate (based on limited testing so far) source projection
properties. To use it:

1.  In front of the camera under test, put a test figure with a
    horizontal row of marks evenly spaced along a line level with the
    camera and perpendicular to the camera axis. Note the distance from
    the lens to the line of marks along the camera axis (closer is
    better, so the marks cover a wide view angle in the image), and the
    distance of each mark (negative left, positive right) from the
    camera axis. You can make the test figure with marks on a wall, a
    metal tube, or whatever. Make the marks VERY distinct since they get
    hard to see at extreme angles.
2.  Using a Gtreamer test program (or another program of your choice)
    capture an image of the test figure through the camera.
3.  Here's the tedious part: Find the X, Y pixel coordinates of each
    mark and enter the values in a tab-separated list, with each row
    containing actual mark distance, pixel X, and pixel Y. See
    resources/calibration/pizza-\*-calibration.txt and
    resources/calibration/\*1080.png for examples.
4.  Copy and paste the output into your configuration file (see the end
    of resources/pizzaTest.resources).

This process may be more completely automated one of these days.

Dependencies
------------

#### [Gstreamer](https://gstreamer.freedesktop.org)

Media streaming framework, used for video networking and input.

#### [gst1-java-core](https://github.com/gstreamer-java)

Java bindings for Gstreamer

#### [JNA - Java Native Access](https://github.com/java-native-access)

Used by gst1-java-core to access Gstreamer.

#### [JSch - Java Secure Channel](https://www.jcraft.com/jsch/)

SSH implementation used for execution of remote Gstreamer pipelines or
other commands.

#### [OpenCV](https://opencv.org/)

Computer vision libarary, used in vision processing skeleton code.

Projection Theory
-----------------

### Background

#### Camera Lens Projections

There are many internet resources dealing with lens projections. My
favorite, and the one to which this code most closely follows, is at
[michel.thoby.free.fr/Fisheye\_history\_short/Projections/Models\_of\_classical\_projections.html](http://michel.thoby.free.fr/Fisheye_history_short/Projections/Models_of_classical_projections.html),
with image examples at.
[michel.thoby.free.fr/Fisheye\_history\_short/Projections/Various\_lens\_projection.html](http://michel.thoby.free.fr/Fisheye_history_short/Projections/Various_lens_projection.html).
The SineProjection and TangentProjection classes together cover the full
family of classical lens projections covered in this document, as well
as "in-between" projections generated using K values other than 1 or 2.
Derived convenience classes handle the classical projections under their
familiar names.

The mathematical heart of these projections is the angle-to-radius
projection function describing the way incident light is directed
through the lens to the film (or digital collector), along with the
inverse back projection function that reverses the projection or
reproduces it as a rendering projection.

In addition to the core projection function, there are several more
mundane steps in translating from a pixel image in traditional image
coordinate space through various scales, origins, etc. to reach the
normalized form it expects as input. The projection pipeline used in
this framework separates them into individual methods, most implemented
in the AbstractProjection class.

#### Display Projections

End-to-end processing from source frame to rendered view entails backing
out the source projection using an appropriate and properly calibrated
SourceProjection object, producing a spherical coordinate representation
of the scene before the camera, but this is not a displayable
representation. The problem (and the solution space) is similar to that
of global map projection, which has been analyzed in great detail for
centuries (see
[en.wikipedia.org/wiki/Map\_projection](https://en.wikipedia.org/wiki/Map_projection)).
Display projections implemented or configurable here include not only
the standard lens projections, but others such as map projections,
cubemaps, and the bird's eye projection that inspired this project to
begin with.

The rendering projection is implemented in the second half of the
pipeline is the rendering projection, which mirrors the steps in the
source projection. The rendering and source projections used may match
exactly, in which case the view shows what came from the camera, or be
entirely different. As with camera lens projections AbstractProjection
implements the common coordinate transforms with derived classes
handling the more interesting parts.

#### Global vs Camera-Oriented Coordinate Systems

The two projection sequences "meet in the middle" in a spherical
coordinate system, generally referred to as "world" space, but there are
different conventions as to how that's represented. When dealing with
cameras, and simply re-projecting camera data, the common convention is
a spherical system around the camera lens axis. Since this software also
addresses the problem of unifying video data from multiple cameras
pointing different directions, it is also useful to define a common
coordinate system to which any camera input can be converted, and
treated the same thereafter. Rendering projections may be configured to
accept input in either coordinate system: camera oriented (for
simplicity), or global (for merging with other sources), and source
projections supply whichever is requested.

### Coordinate System Conventions

The source-world-display projection processing sequence described above
corresponds to a set of coordinate conversions. For reference, they're
defined in more detail below. If rendering spherical is the same as
camera spherical, transforms in and out of global spherical are skipped.
It's important to note that this sequence must be processed only once
for any given rendering, i.e. for any use of a source in a view (display
or memory-mapped output).The result of processing the sequence is a
simple integer-to-integer table describing how the rendering is to
obtain pixel data from camera pixel arrays to update pixels. Whenever a
new source frame arrives, the view looks in its table to see which of
its pixels came from that source and where in the camera pixel array
they are, then copies them directly with no further calculation.

#### Camera Pixel Array

Common java idiom: index [n] from top to bottom, left to right. Top left
= [0], bottom right = [resolution.width\*resolution.height-1].

#### Camera Cartesian Pixel

Common java idiom: pairs {x, y} in camera image pixels. Top left =
{0,0}, bottom right = {resolution.width-1,resolution.height-1}.

#### Normalized Camera Cartesian

Pairs {x, y} in source projection space. Center = {0,0}, top left =
{-1,-resolution.height/resolution.width}, bottom right =
{1,resolution.height/resolution.width}

#### Camera Polar

Pairs {azimuth, radius}, in camera projection space. Azimuth is in
radians, clockwise from top center. Radius is relative to half the
projection width. Center = {X,0}, left edge center = {3π/2,1}, right
edge center = {\#x03c0;/2,1}.

#### Camera Spherical

Pairs {azimuth, polar}, in camera-world space (relative to camera look
vector). Azimuth is the same as in Camera Polar, polar is the projected
angle away from the camera look vector in radians. Center = {X,0}, left
edge center = {3π/2,horizontal angular FOV/2}, right edge center =
{\#x03c0;/2,horizontal angular FOV/2}.

#### Global Spherical

Pairs {azimuth, polar}, in global-world space (common across all
cameras). Assuming an observer sitting in a (north facing, by
convention) vehicle looking forward, azimuth is clockwise
forward-right-back-left, polar is angle down from straight up. Forward
(north) parallel to ground = {0,π/2}, upward = {X,0}, rightward =
{π/2,π/2}, downward = {X, π})

#### Rendering Spherical

Like camera spherical

#### Rendering Polar

Like camera polar.

#### Normalized Rendering Cartesian

Like normalized camera cartesian

#### Rendering Cartesian Pixel

Like camera cartesian pixel.

#### Rendering Pixel Array

Like camera pixel array.
