package gov.nih.mipav.model.algorithms.utilities;

import WildMagic.LibFoundation.Mathematics.Vector3f;

import gov.nih.mipav.model.algorithms.*;
import gov.nih.mipav.model.file.*;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;

import java.awt.*;

import java.io.*;

import java.util.*;


/**
 * Flips 2D, 3D or 4D grays scale or color dataset about X, Y, or Z axis (when applicable) when AlgorithmFlip.IMAGE is
 * passed to the constructor. An option is given to flip all VOIs at this time. When AlgorithmFlip.VOI is passed, only
 * the selected VOI is flipped about the specified axis.
 *
 * @version  1.0 July 14, 2000
 * @author   Matthew J. McAuliffe, Ph.D.
 */

public class AlgorithmFlip extends AlgorithmBase {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Flip along Y axis. */
    public static final int Y_AXIS = 0;

    /** Flip along X axis. */
    public static final int X_AXIS = 1;

    /** Flip along Z axis. */
    public static final int Z_AXIS = 2;

    /** Image and all VOIs should be flipped. */
    public static final int IMAGE_AND_VOI = 2;

    /** Denotes image should be flipped without VOI. */
    public static final int IMAGE = 0;

    /** Denotes selected VOI should be flipped. */
    public static final int VOI_TYPE = 1;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** Axis to flip along. */
    private int flipAxis = Y_AXIS;

    /** Type of object to flip. */
    private int flipObject;

    /** Whether all VOIs shouold be flipped when the image is flipped. */

    private boolean flipVoiWithImage;
    
    /** Whether orientation and origin should change with flipping. */
    private boolean changeOrientationOrigin;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Flips 2D, 3D or 4D grays scale or color dataset about X or Y axis.
     *
     * @param  srcImg      source image model
     * @param  flipMode    flip about which axis
     * @param  flipObject  DOCUMENT ME!
     * @param  changeOrientationOrigin
     */
    public AlgorithmFlip(ModelImage srcImg, int flipMode, int flipObject, boolean changeOrientationOrigin) {
        super(null, srcImg);
        this.flipObject = flipObject;
        this.changeOrientationOrigin = changeOrientationOrigin;

        if ((flipMode == Y_AXIS) || (flipMode == X_AXIS) || (flipMode == Z_AXIS)) {
            flipAxis = flipMode;
        } else {
            flipAxis = Y_AXIS;
        }

        flipVoiWithImage = false;
    }

    /**
     * Flips 2D, 3D or 4D grays scale or color dataset about X or Y axis.
     *
     * @param  srcImg      source image model
     * @param  flipMode    flip about which axis
     * @param  progress    mode of progress bar (see AlgorithmBase)
     * @param  flipObject  DOCUMENT ME!
     * @param  chnageOrientationOrigin
     */
    public AlgorithmFlip(ModelImage srcImg, int flipMode, int progress, int flipObject, boolean changeOrientationOrigin) {
        this(srcImg, flipMode, flipObject, changeOrientationOrigin);
        // progressMode = progress;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Prepares this class for destruction.
     */
    public void finalize() {
        super.finalize();
    }


    /**
     * Runs the flip algorithm.
     */
    public void runAlgorithm() {

        if (srcImage == null) {
            displayError("Source Image is null");

            return;
        }

        

        if (srcImage.getNDims() == 2) {
            calcInPlace(1);
        } else if (srcImage.getNDims() == 3) {
            calcInPlace(srcImage.getExtents()[2]);
        } else if (srcImage.getNDims() == 4) {
            calcInPlace(srcImage.getExtents()[2] * srcImage.getExtents()[3]);
        }
    }

    /**
     * Generates the flipped image and replaces the source image with the flippeded image.
     *
     * @param  nImages  Number of images to be flipped. If 2D image then nImage = 1, if 3D or 4D image where each image
     *                  is to processed independently then nImages equals the number of images in the volume.
     */
    private void calcInPlace(int nImages) {
        int slice;
        float[] sliceBuffer;
        float[] sliceBufferTemp = null;
        int buffFactor;
        boolean logMagDisplay = false;
        boolean evenNumberZSlices = true;
        int i;

        try {

            if (srcImage.isColorImage()) {
                buffFactor = 4;
            } else if ((srcImage.getType() == ModelStorageBase.COMPLEX) ||
                           (srcImage.getType() == ModelStorageBase.DCOMPLEX)) {
                buffFactor = 2;
                logMagDisplay = srcImage.getLogMagDisplay();
            } else {
                buffFactor = 1;
            }

            slice = buffFactor * srcImage.getSliceSize();
            sliceBuffer = new float[slice];
            fireProgressStateChanged(srcImage.getImageName(), "Flipping image ...");
        } catch (OutOfMemoryError e) {
            System.gc();
            displayError("Algorithm Flip: Out of memory");
            setCompleted(false);

            return;
        }
        
        if (srcImage.getNDims() >= 3) {
            if (srcImage.getExtents()[2]/2 * 2 != srcImage.getExtents()[2]) {
               evenNumberZSlices = false;    
            }
        }

        int mod = nImages / 10; // mod is 10 percent of length

        if (mod == 0) {

            // since % mod gives a divide by zero error for mod = 0
            mod = 1;
        }

        /* axisOrder is always the default: (no coordinate axis remapping) */
        int[] axisOrder = { 0, 1, 2 };

        /* axisFlip depends on flipAxis: */
        boolean[] axisFlip = { false, false, false };
        int index = 2;

        if (flipAxis == Y_AXIS) {
            index = 0;
        } else if (flipAxis == X_AXIS) {
            index = 1;
        }

        axisFlip[index] = true;

        int tDim = 1;
        int volume = 1;
        int zDim = 1;
        int xDim = 1;
        int yDim = 1;

        if (srcImage.getNDims() > 1) {
            xDim = srcImage.getExtents()[0];
            yDim = srcImage.getExtents()[1];
        }

        if (srcImage.getNDims() == 4) {
            zDim = srcImage.getExtents()[2];
            tDim = srcImage.getExtents()[3];
            volume = slice * zDim;
        } else if (srcImage.getNDims() == 3) {
            zDim = srcImage.getExtents()[2];
        }

        if ((flipObject == AlgorithmFlip.IMAGE) || (flipObject == AlgorithmFlip.IMAGE_AND_VOI)) {


            /* If flipping the z-axis, then loop 1/2 the times and swap the z slices... */
            if (index == 2) {
                zDim /= 2;
                sliceBufferTemp = new float[slice];
            }

            /* For each slice: */
            for (int t = 0; (t < tDim) && !threadStopped; t++) {

                for (int z = 0; (z < zDim) && !threadStopped; z++) {

                    if ((nImages > 1) && ((((t * zDim) + z) % mod) == 0)) {
                        fireProgressStateChanged(Math.round((float) ((t * zDim) + z) / (nImages - 1) * 100));
                    }

                    try {
                        srcImage.export(axisOrder, axisFlip, t, z, sliceBuffer);

                        if (index == 2) {
                            int zTemp = (srcImage.getExtents()[2] - 1 - z);
                            srcImage.export(axisOrder, axisFlip, t, zTemp, sliceBufferTemp);
                            srcImage.importData((t * volume) + (zTemp * slice), sliceBufferTemp, false);
                        }

                        srcImage.importData((t * volume) + (z * slice), sliceBuffer, false);
                    } catch (IOException error) {
                        displayError("AlgorithmSubset reports: Destination image already locked.");
                        setCompleted(false);

                        return;
                    }
                }
            }

            if (threadStopped) {
                sliceBuffer = null;
                sliceBufferTemp = null;
                finalize();

                return;
            }

            if (buffFactor == 2) {
                srcImage.setLogMagDisplay(logMagDisplay);
                srcImage.calcMinMaxMag(logMagDisplay);
            }

            /* Update FileInfo for mapping into DICOM space: */
            if (changeOrientationOrigin) {
                /**
                 * For y axis flipping:
                 * xp = a00*x + a01*yold + a02*z + xoldorigin
                 * xp = a00*x - a01*ynew + a02*z + xneworigin
                 * ynew = ydim - 1 - yold
                 * xneworigin - a01*ynew = xoldorigin + a01*yold
                 * xneworigin = a01*(yold + ynew) + xoldorigin
                 * xneworigin = a01*(ydim - 1) + xoldorigin
                 * 
                 * yp = a10*x + a11*yold + a12*z + yoldorigin
                 * yp = a10*x - a11*ynew + a12*z + yneworigin
                 * yneworigin - a11*ynew = yoldorigin + a11*yold
                 * yneworigin = a11*(yold + ynew) + yoldorigin
                 * yneworigin = a11*(ydim - 1) + yoldorigin
                 * 
                 * zp = a20*x + a21*yold + a22*z + zoldorigin
                 * zp = a20*x - a21*ynew + z22*z + zneworigin
                 * zneworigin - a21*ynew = zoldorigin + a21*yold
                 * zneworigin = a21*(yold + ynew) + zoldorigin
                 * zneworigin = a21*(ydim - 1) + zoldorigin
                 */
                FileInfoBase[] fileInfo = srcImage.getFileInfo();
                float origin[] = fileInfo[0].getOrigin();
                int orient = fileInfo[0].getAxisOrientation(index);
                TransMatrix matrix = srcImage.getMatrix();
    
                
                for (i = 0; i <= 2; i++) {
                    if (Math.abs(matrix.get(i, i)) != fileInfo[0].getResolutions()[i]) {
                        if (matrix.get(i, i) < 0.0) {
                            matrix.Set(i, i, -fileInfo[0].getResolutions()[i]);
                        }
                        else {
                            matrix.Set(i, i, fileInfo[0].getResolutions()[i]);
                        }
                    }
                    origin[i] = matrix.get(i, index)*(fileInfo[0].getExtents()[index] - 1) + origin[i];
                }
    
                orient = FileInfoBase.oppositeOrient(orient);
    
                for (i = 0; i < fileInfo.length; i++) {
                    fileInfo[i].setAxisOrientation(orient, index);
                    fileInfo[i].setOrigin(origin);
    
                    if (index == 2) {
                        fileInfo[i].setOrigin(origin[0] + (matrix.get(0, 2) * i), 0);
                        fileInfo[i].setOrigin(origin[1] + (matrix.get(1, 2) * i), 1);
                        fileInfo[i].setOrigin(origin[2] + (matrix.get(2, 2) * i), 2);
                    }
                }
            } // if (changeOrientationOrigin)

            if (flipObject == AlgorithmFlip.IMAGE_AND_VOI) {
                VOIVector vec = srcImage.getVOIs();
                Iterator vecIter = vec.iterator();

                if (flipAxis == X_AXIS) {

                    while (vecIter.hasNext()) {
                        VOI nextVoi = (VOI) vecIter.next();

                        for (i = 0; i < zDim; i++) {
                            Polygon[] polyList = nextVoi.exportPolygons(i);
                            nextVoi.removeCurves(i);

                            for (int j = 0; j < polyList.length; j++) {
                                Polygon poly = polyList[j];
                                int[] points = poly.ypoints;

                                for (int k = 0; k < points.length; k++) {
                                    points[k] = -points[k] + yDim;
                                }

                                nextVoi.importPolygon(poly, i);
                            }
                        }
                    }
                }

                if (flipAxis == Y_AXIS) {

                    while (vecIter.hasNext()) {
                        VOI nextVoi = (VOI) vecIter.next();

                        for (i = 0; i < zDim; i++) {
                            Polygon[] polyList = nextVoi.exportPolygons(i);
                            nextVoi.removeCurves(i);

                            for (int j = 0; j < polyList.length; j++) {
                                Polygon poly = polyList[j];
                                int[] points = poly.xpoints;

                                for (int k = 0; k < points.length; k++) {
                                    points[k] = -points[k] + xDim;
                                }

                                nextVoi.importPolygon(poly, i);
                            }
                        }
                    }
                }

                if ((flipAxis == Z_AXIS) && (srcImage.getNDims() > 2)) {
                    Object[] shapes = null;
                    ViewJComponentEditImage compImage = srcImage.getParentFrame().getComponentImage();
                    zDim *= 2;
                    if (!evenNumberZSlices) {
                        zDim += 1;
                    }

                    while (vecIter.hasNext()) {
                        ShapeHolder shapeHolder = new ShapeHolder();
                        VOI nextVoi = (VOI) vecIter.next();

                        for (int voiSlice = 0; voiSlice < zDim; voiSlice++) {
                            shapes = null;

                            if ((nextVoi.getCurveType() == VOI.CONTOUR) || (nextVoi.getCurveType() == VOI.POLYLINE)) {
                                shapes = nextVoi.exportPolygons(voiSlice);
                            } else {
                                shapes = nextVoi.exportPoints(voiSlice);
                            }

                            if (shapes.length > 0) {

                                if (shapeHolder.addShape(shapes, voiSlice)) {
                                    ;
                                }

                                nextVoi.removeCurves(voiSlice);
                            }
                        }
                        for (int voiSlice = 0; voiSlice < zDim; voiSlice++) {
                            int z = voiSlice, direction = (z >= (zDim / 2)) ? -1 : 1;
                            int distance, scope;
                            if (!evenNumberZSlices) {
                                distance = Math.abs(z - (zDim / 2));
                                scope = 2 * distance;
                            }
                            else if (evenNumberZSlices && z < zDim/2) {
                                distance = (zDim/2 - 1 - z);
                                scope = 2 * distance + 1;
                            }
                            else {
                                distance = z - zDim/2;
                                scope = 2 * distance + 1;
                            }
                            Object[] shapesAtSlice = shapeHolder.getShapesAtSlice(voiSlice);
                            for (int k = 0; k < shapesAtSlice.length; k++) {

                                if (shapesAtSlice[k] instanceof Polygon[]) {
                                    Polygon[] polyTemp = ((Polygon[]) shapesAtSlice[k]);

                                    for (int m = 0; m < polyTemp.length; m++) {
                                        nextVoi.importPolygon(polyTemp[m], voiSlice + (scope * direction));
                                    }
                                } else if (shapesAtSlice[k] instanceof Vector3f[]) {
                                    nextVoi.importPoints((Vector3f[]) shapesAtSlice[k], voiSlice + (scope * direction));
                                }

                            }
                        }

                        compImage.getVOIHandler().fireVOISelectionChange(nextVoi, null);
                    }

                    int z = compImage.getSlice(), direction = (z >= (zDim / 2)) ? -1 : 1;
                    int distance, scope;
                    if (!evenNumberZSlices) {
                        distance = Math.abs(z - (zDim / 2));
                        scope = 2 * distance;
                    }
                    else if (evenNumberZSlices && z < zDim/2) {
                        distance = (zDim/2 - 1 - z);
                        scope = 2 * distance + 1;
                    }
                    else {
                        distance = z - zDim/2;
                        scope = 2 * distance + 1;
                    }
                    compImage.show(compImage.getTimeSlice(), compImage.getSlice() + (scope * direction), null, null,
                                   true, compImage.getInterpMode());
                    compImage.getActiveImage().getParentFrame().setSlice(compImage.getSlice());
                    compImage.getActiveImage().getParentFrame().updateImages(true);
                }
            }
        } else if (flipObject == AlgorithmFlip.VOI_TYPE) {
            boolean activeVoi = false;
            ViewVOIVector vec = srcImage.getVOIs();
            Iterator vecIter = vec.iterator();

            if (flipAxis == X_AXIS) {

                while (vecIter.hasNext()) {
                    VOI nextVoi = (VOI) vecIter.next();

                    if (nextVoi.isActive()) {
                        activeVoi = true;

                        for (i = 0; i < zDim; i++) {
                            VOIBase base = nextVoi.getActiveContour(i);

                            if (base != null) {
                                Iterator itr = base.iterator();

                                while (itr.hasNext()) {
                                    Vector3f point = (Vector3f) itr.next();
                                    point.Y = -point.Y + yDim;
                                }
                            }
                        }
                    }
                }
            }

            if (flipAxis == Y_AXIS) {

                while (vecIter.hasNext()) {
                    VOI nextVoi = (VOI) vecIter.next();

                    if (nextVoi.isActive()) {
                        activeVoi = true;

                        for (i = 0; i < zDim; i++) {
                            VOIBase base = nextVoi.getActiveContour(i);

                            if (base != null) {
                                Iterator itr = base.iterator();

                                while (itr.hasNext()) {
                                    Vector3f point = (Vector3f) itr.next();
                                    point.X = -point.X + xDim;
                                }
                            }
                        }
                    }
                }
            }

            if ((flipAxis == Z_AXIS) && (srcImage.getNDims() > 2)) {
                int voiSlice = -1;
                ViewJComponentEditImage compImage = srcImage.getParentFrame().getComponentImage();
                int z = compImage.getSlice(), direction = (z >= (zDim / 2)) ? -1 : 1;
                int distance, scope;
                if (!evenNumberZSlices) {
                    distance = Math.abs(z - (zDim / 2));
                    scope = 2 * distance;
                }
                else if (evenNumberZSlices && z < zDim/2) {
                    distance = (zDim/2 - 1 - z);
                    scope = 2 * distance + 1;
                }
                else {
                    distance = z - zDim/2;
                    scope = 2 * distance + 1;
                }

                while (vecIter.hasNext()) {
                    VOI nextVoi = (VOI) vecIter.next();

                    if (nextVoi.isActive()) {

                        if (voiSlice == -1) {
                            voiSlice = compImage.getSlice();
                        }

                        VOIBase base = nextVoi.getActiveContour(voiSlice);
                        int[] xpoints = new int[base.size()];
                        int[] ypoints = new int[base.size()];

                        for (i = 0; i < xpoints.length; i++) {
                            Vector3f tempPoint = (Vector3f) base.get(i);
                            xpoints[i] = (int) tempPoint.X;
                            ypoints[i] = (int) tempPoint.Y;
                        }

                        Polygon gon = new Polygon(xpoints, ypoints, xpoints.length);
                        nextVoi.removeCurve(nextVoi.getActiveContourIndex(voiSlice), voiSlice);
                        nextVoi.importPolygon(gon, voiSlice + (direction * scope));
                        compImage.show(compImage.getTimeSlice(), voiSlice + (scope * direction), null, null, true,
                                compImage.getInterpMode());
                        compImage.getVOIHandler().fireVOISelectionChange(nextVoi, null);
                        compImage.getActiveImage().getParentFrame().setSlice(compImage.getSlice());
                        compImage.getActiveImage().getParentFrame().updateImages(true);
                    }

                }


            }
        }

        setCompleted(true);
    }
    
    //~ Inner Classes --------------------------------------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     */
    private class ShapeHolder {

        /** DOCUMENT ME! */
        private boolean isConstructed;

        /** DOCUMENT ME! */
        private ArrayList shapeList;

        /** DOCUMENT ME! */
        private ArrayList sliceList;

        /**
         * Creates a new ShapeHolder object.
         */
        private ShapeHolder() {
            shapeList = new ArrayList();
            sliceList = new ArrayList();
            isConstructed = true;
        }

        /**
         * Creates a new ShapeHolder object.
         *
         * @param  shapeArr     DOCUMENT ME!
         * @param  sliceLocArr  DOCUMENT ME!
         */
        private ShapeHolder(Object[] shapeArr, int[] sliceLocArr) {
            this();

            if (shapeArr.length == sliceLocArr.length) {

                for (int i = 0; i < shapeArr.length; i++) {
                    shapeList.add(shapeArr[i]);
                    sliceList.add(new Integer(sliceLocArr[i]));
                }
            } else {
                isConstructed = false;
            }
        }

        /**
         * DOCUMENT ME!
         *
         * @param   shape  DOCUMENT ME!
         * @param   slice  DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        private boolean addShape(Object shape, int slice) {

            if ((shapeList.size() == sliceList.size()) && isConstructed) {
                shapeList.add(shape);
                sliceList.add(new Integer(slice));

                return true;
            } else {
                return false;
            }
        }

        /**
         * DOCUMENT ME!
         *
         * @param   index  DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        private Object getShape(int index) {
            return shapeList.get(index);
        }


        /**
         * DOCUMENT ME!
         *
         * @param   slice  DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        private Object[] getShapesAtSlice(int slice) {
            ArrayList tempShapes = new ArrayList();

            for (int i = 0; i < sliceList.size(); i++) {

                if (getSlice(i) == slice) {
                    tempShapes.add(getShape(i));
                }
            }

            Object[] shapesAtSlice = new Object[tempShapes.size()];

            for (int i = 0; i < tempShapes.size(); i++) {
                shapesAtSlice[i] = tempShapes.get(i);
            }

            return shapesAtSlice;
        }

        /**
         * DOCUMENT ME!
         *
         * @param   index  DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        private int getSlice(int index) {
            Integer tempInt = ((Integer) sliceList.get(index));

            return tempInt.intValue();
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        private boolean isConstructed() {
            return isConstructed;
        }
    }


}
