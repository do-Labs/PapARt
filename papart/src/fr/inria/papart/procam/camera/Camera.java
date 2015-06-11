/* 
 * Copyright (C) 2014 Jeremy Laviole <jeremy.laviole@inria.fr>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package fr.inria.papart.procam.camera;

/**
 *
 * @author jeremylaviole
 */
import fr.inria.papart.graph.Node;
import fr.inria.papart.procam.MarkerBoard;
import fr.inria.papart.procam.ProjectiveDeviceP;
import fr.inria.papart.procam.Utils;
import fr.inria.papart.procam.camera.CamImage;
import fr.inria.papart.procam.camera.CamImageColor;
import fr.inria.papart.procam.camera.CamImageGray;
import org.bytedeco.javacpp.opencv_core.CvMat;
import org.bytedeco.javacpp.opencv_core.IplImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PMatrix3D;
import processing.core.PVector;

public abstract class Camera extends Node implements PConstants {

    public static Camera INVALID_CAMERA = new CameraOpenCV(-1);

    // Images
    protected IplImage currentImage, copyUndist;

    protected CamImage camImage = null;

    public enum Type {

        OPENCV, PROCESSING, OPEN_KINECT, FLY_CAPTURE
    }

    public enum PixelFormat {

        RGB, BGR, ARGB, RGBA, GRAY, DEPTH_KINECT_MM
    }

    protected PixelFormat format;

    // Parameters
    // One or the other. 
    protected String cameraDescription = null;
    protected int systemNumber = -1;

    protected int width, height;
    protected int frameRate;
    protected boolean trackSheets = false;
    private boolean isClosing = false;
    protected boolean isConnected = false;

    private boolean undistort = false;

    // Properties files
    private String calibrationFile = null;

    // Properties (instanciated)
    protected ProjectiveDeviceP pdp = null;
    protected PMatrix3D camIntrinsicsP3D;

    // Instance variables
    protected PApplet parent = null;

    private List<MarkerBoard> sheets = null;

    protected final Semaphore sheetsSemaphore = new Semaphore(1);

    // ARToolkit 
    protected String calibrationARToolkit;

    private CameraThread thread = null;

    abstract public void start();

    public PImage getImage() {
        return getPImage();
    }

    public abstract PImage getPImage();

    
    protected void checkParameters() {
        if (width == 0 || height == 0) {
            throw new RuntimeException("Camera: Width or Height are 0, set them or load a calibration.");
        }
        if (this.parent == null) {
            throw new RuntimeException("Camera: the parent (current applet) is not set.");
        }
        if (this.systemNumber == -1 && cameraDescription == null) {
            throw new RuntimeException("Camera: initalization failed");
        }

    }

    public void setCalibration(String calibrationYAML) {
        try {
            this.calibrationFile = calibrationYAML;

            pdp = ProjectiveDeviceP.loadCameraDevice(calibrationYAML, 0);
            camIntrinsicsP3D = pdp.getIntrinsics();
            this.width = pdp.getWidth();
            this.height = pdp.getHeight();
        } catch (Exception e) {
            e.printStackTrace();

            System.err.println("Camera: error reading the calibration " + pdp
                    + "file" + calibrationYAML + " \n" + e);
        }
    }

    public PImage getPImageCopy() {
        PImage out = parent.createImage(this.width, this.height, RGB);
        Utils.IplImageToPImage(currentImage, out);
        return out;
    }

    /**
     * Works if the source is IPLImage ?.
     *
     * @param context
     * @return
     */
    public PImage getPImageCopy(PApplet context) {
        PImage out = context.createImage(this.width, this.height, RGB);
        Utils.IplImageToPImage(currentImage, out);
        return out;
    }

    public PImage getPImageCopyTo(PImage out) {
        Utils.IplImageToPImage(currentImage, this.format, out);
        return out;
    }

    /**
     * Description of the camera, the number if using OpenCV or OpenKinect, and
     * a name or file if using Processing.
     *
     * @param description
     */
    public void setCameraDevice(String description) {
        this.cameraDescription = description;
    }

    protected String getCameraDevice() {
        return this.cameraDescription;
    }

    public void setParent(PApplet applet) {
        this.parent = applet;
    }

    public void setSystemNumber(int systemNumber) {
        this.systemNumber = systemNumber;
    }

    public void setSize(int width, int height) {
        // TODO: error handling while running. 
        this.width = width;
        this.height = height;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int getFrameRate() {
        return this.frameRate;
    }

    public boolean isUndistort() {
        return undistort;
    }

    public void setUndistort(boolean undistort) {
        this.undistort = undistort;
    }

    public boolean isCalibrated() {
        return this.calibrationFile != null;
    }

    public String getCalibrationFile() {
        return this.calibrationFile;
    }

    /**
     * Initialize the tracking with ARToolkit plus.
     *
     * @param calibrationARToolkit
     */
    public void initMarkerDetection(String calibrationARToolkit) {
        // Marker Detection and view
        this.calibrationARToolkit = calibrationARToolkit;
        this.sheets = Collections.synchronizedList(new ArrayList<MarkerBoard>());
    }

    public String getCalibrationARToolkit() {
        return calibrationARToolkit;
    }

    /**
     * Add a markerboard to track with this camera.
     *
     * @param sheet
     */
    public void trackMarkerBoard(MarkerBoard sheet) {
        sheet.addTracker(parent, this);
        try {
            sheetsSemaphore.acquire();
            this.sheets.add(sheet);
            sheetsSemaphore.release();
        } catch (InterruptedException ex) {
            Logger.getLogger(Camera.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullPointerException e) {
            throw new RuntimeException("Marker detection not initialized.");
        }

    }

    /**
     * If the video is threaded, this sets if the tracking is on or not.
     *
     * @param auto automatic Tag detection: ON if true.
     */
    public void trackSheets(boolean auto) {
        this.trackSheets = auto;

        if (thread != null) {
            thread.setCompute(auto);
        } else {
            System.err.println("Camera: Error AutoCompute only if threaded.");
        }
    }

    public boolean tracks(MarkerBoard board) {
        return this.sheets.contains(board);
    }

    public List<MarkerBoard> getTrackedSheets() {
        return this.sheets;
    }

    /**
     * It makes the camera update continuously.
     */
    public void setThread() {
        if (thread == null) {
            thread = new CameraThread(this);
            thread.setCompute(this.trackSheets);
            thread.start();
        } else {
            System.err.println("Camera: Error Thread already launched");
        }
    }

    /**
     * Stops the update thread.
     */
    public void stopThread() {
        if (thread != null) {
            thread.stopThread();
            thread = null;
        }
    }

    public boolean useThread() {
        return thread != null;
    }

    /**
     * Update the current Image, from the specific grabber, lens distorsions are
     * handled here.
     *
     * @param img
     */
    protected void updateCurrentImage(IplImage img) {
        if (undistort) {
            if (copyUndist == null) {
                copyUndist = img.clone();
            }
            // Workaround for crash when the java program is closing
            // to avoid native code to continue to run...
            if (isClosing()) {
                return;
            }
            pdp.getDevice().undistort(img, copyUndist);
            currentImage = copyUndist;
        } else {
            currentImage = img;
        }
    }

    /**
     * Check the memory allocation of the CamImage.
     */
    protected void checkCamImage() {
        if (camImage == null) {

            if (this.isPixelFormatGray()) {
                camImage = new CamImageGray(parent, width(), height());
            }
            if (this.isPixelFormatColor()) {
                camImage = new CamImageColor(parent, width(), height());
            }
        }
    }

    protected boolean isPixelFormatGray() {
        PixelFormat pixelFormat = getPixelFormat();
        return pixelFormat == PixelFormat.GRAY
                || pixelFormat == PixelFormat.DEPTH_KINECT_MM;
    }

    protected boolean isPixelFormatColor() {
        PixelFormat pixelFormat = getPixelFormat();
        return pixelFormat == PixelFormat.ARGB
                || pixelFormat == PixelFormat.BGR
                || pixelFormat == PixelFormat.RGB
                || pixelFormat == PixelFormat.RGBA;
    }

// Public API 
    public abstract void grab();

    public IplImage getIplImage() {
        return currentImage;
    }

    public ProjectiveDeviceP getProjectiveDevice() {
        return this.pdp;
    }

    public abstract void close();

    protected void setClosing() {
        this.isClosing = true;
        this.stopThread();
    }

    public boolean isClosing() {
        return isClosing || !isConnected;
    }

    public PixelFormat getPixelFormat() {
        return format;
    }

    public void setPixelFormat(PixelFormat format) {
        this.format = format;
    }

    /**
     * To use instead of getCamViewpoint
     *
     * @param point
     * @return
     */
    public PVector getViewPoint(PVector point) {
        return getCamViewPoint(point);
    }

    /**
     * Gets the 2D location in the image of a 3D point. TODO: undistort ?
     *
     * @param pt 3D point seen by the camera.
     * @return 2D location of the 3D point in the image.
     */
    @Deprecated
    public PVector getCamViewPoint(PVector pt) {
        PVector tmp = new PVector();
        camIntrinsicsP3D.mult(new PVector(pt.x, pt.y, pt.z), tmp);
        //TODO: lens distorsion ?
        return new PVector(tmp.x / tmp.z, tmp.y / tmp.z);
    }
    private CvMat internalParams = null;

    @Deprecated
    public PMatrix3D estimateOrientation(PVector[] objectPoints,
            PVector[] imagePoints) {
        return pdp.estimateOrientation(objectPoints, imagePoints);
    }

    static public void convertARParams(PApplet parent, String calibrationYAML,
            String calibrationARtoolkit) {
        convertARParams(parent, calibrationYAML, calibrationARtoolkit, 0, 0);
    }

    static public void convertARParams(PApplet parent, String calibrationYAML,
            String calibrationARtoolkit, int width, int height) {
        try {
            // ARToolkit Plus 2.1.1
//            fr.inria.papart.procam.Utils.convertARParam(parent, calibrationYAML, calibrationData, width, height);
            // ARToolkit Plus 2.3.0
            fr.inria.papart.procam.Utils.convertARParam2(parent, calibrationYAML, calibrationARtoolkit);
        } catch (Exception e) {
            PApplet.println("Conversion error. " + e);
        }
    }

}
