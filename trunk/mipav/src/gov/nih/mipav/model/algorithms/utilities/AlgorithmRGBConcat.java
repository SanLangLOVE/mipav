package gov.nih.mipav.model.algorithms.utilities;


import gov.nih.mipav.model.algorithms.*;
import gov.nih.mipav.model.file.*;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;

import java.io.*;


/**
 * Simple algorithm that generates an RGB image from three gray images.
 *
 * @version  1.0 Dec 30, 1999
 * @author   Matthew J. McAuliffe, Ph.D.
 */
public class AlgorithmRGBConcat extends AlgorithmBase {

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** Destination image where results are to be stored. Must be of type RGB. */
    private ModelImage destImage;

    /**
     * Flag indicating whether of not to remap the data. If true and srcImage data max is < 255 data will be remapped
     * [0-255] else if image max > 255 data will automatically be remapped [0-255].
     */
    private boolean reMap = false;

    /** Source gray scale image to be stored in the BLUE channel. */
    private ModelImage srcImageB;

    /** Source gray scale image to be stored in the GREEN channel. */
    private ModelImage srcImageG;

    /** Source gray scale image to be stored in the RED channel. */
    private ModelImage srcImageR;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Creates a new AlgorithmRGBConcat object.
     *
     * @param  srcImgR  image model where result image of the Red channel is to be stored
     * @param  srcImgG  image model where result image of the Green channel is to be stored
     * @param  srcImgB  image model where result image of the Blue channel is to be stored
     * @param  remap    if true and srcImage data max is < 255 data will be remapped [0-255] else if image max > 255
     *                  data will automatically be remapped [0-255].
     */
    public AlgorithmRGBConcat(ModelImage srcImgR, ModelImage srcImgG, ModelImage srcImgB, boolean remap) {

        srcImageR = srcImgR; // Put results in red   destination image.
        srcImageG = srcImgG; // Put results in green destination image.
        srcImageB = srcImgB; // Put results in blue  destination image.
        destImage = null;
        reMap = remap;
    }

    /**
     * Creates a new AlgorithmRGBConcat object.
     *
     * @param  srcImgR  image model where result image of the Red channel is to be stored
     * @param  srcImgG  image model where result image of the Green channel is to be stored
     * @param  srcImgB  image model where result image of the Blue channel is to be stored
     * @param  destImg  destination image image model
     * @param  remap    if true and srcImage data max is < 255 data will be remapped [0-255] else if image max > 255
     *                  data will automatically be remapped [0-255].
     */
    public AlgorithmRGBConcat(ModelImage srcImgR, ModelImage srcImgG, ModelImage srcImgB, ModelImage destImg,
                              boolean remap) {

        srcImageR = srcImgR; // Put results in red   destination image.
        srcImageG = srcImgG; // Put results in green destination image.
        srcImageB = srcImgB; // Put results in blue  destination image.
        destImage = destImg;
        reMap = remap;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Prepares this class for destruction.
     */
    public void finalize() {
        srcImageR = null;
        srcImageG = null;
        srcImageB = null;
        destImage = null;
        super.finalize();
    }

    /**
     * Accessor to get imageR.
     *
     * @return  DOCUMENT ME!
     */
    public ModelImage getImageR() {
        return srcImageR;
    }

    /**
     * Starts the program.
     */
    public void runAlgorithm() {

        if ((srcImageR == null) || (srcImageG == null) || (srcImageB == null)) {
            displayError("RGBConcat.run(): Source image is null");

            return;
        }

        if (srcImageR.isColorImage() || srcImageG.isColorImage() || srcImageB.isColorImage()) {
            displayError("RGBConcat.run(): Source image(s) cannot be of RGB type");

            return;
        }

        if ((destImage != null) && (destImage.isColorImage() == false)) {
            displayError("RGBConcat.run(): Destination image must be of RGB type");

            return;
        }

        constructLog();

        if (destImage != null) {
            calcStoreInDest();
        } else {
            calcStoreInPlace();
        }
    }

    /**
     * Concatinate the image and store the results in the destination image.
     */
    private void calcStoreInDest() {

        int i, j;
        int id;

        int length, srcLength; // total number of data-elements (pixels) in image
        float[] buffer; // data-buffer (for pixel data) which is the "heart" of the image\
        float[] bufferR;
        float[] bufferG;
        float[] bufferB;
        float minR = (float) srcImageR.getMin();
        float maxR = (float) srcImageR.getMax();
        float minG = (float) srcImageG.getMin();
        float maxG = (float) srcImageG.getMax();
        float minB = (float) srcImageB.getMin();
        float maxB = (float) srcImageB.getMax();

        int nImages = 1;

        try {
            length = 4 * destImage.getSliceSize();
            srcLength = destImage.getSliceSize();
            buffer = new float[length];
            bufferR = new float[srcLength];
            bufferG = new float[srcLength];
            bufferB = new float[srcLength];
            buildProgressBar(destImage.getImageName(), "Concatinating gray images ...", 0, 100);
        } catch (OutOfMemoryError e) {
            buffer = null;
            bufferR = null;
            bufferG = null;
            bufferB = null;
            System.gc();
            displayError("Algorithm RGBConcat reports: Out of memory when creating image buffer");
            setCompleted(false);
            notifyListeners(this);

            return;
        }

        initProgressBar();

        if (srcImageR.getNDims() == 3) {
            nImages = srcImageR.getExtents()[2];
        } else if (srcImageR.getNDims() == 4) {
            nImages = srcImageR.getExtents()[2] * srcImageR.getExtents()[3];
        } else {
            nImages = 1;
        }

        int mod = (nImages * length) / 20;

        float diffR = maxR - minR;
        float diffG = maxG - minG;
        float diffB = maxB - minB;

        if (diffR == 0) {
            diffR = 1;
        }

        if (diffG == 0) {
            diffG = 1;
        }

        if (diffB == 0) {
            diffB = 1;
        }

        for (j = 0; (j < nImages) && !threadStopped; j++) {

            try {
                srcImageR.exportData(j * srcLength, srcLength, bufferR);
                srcImageG.exportData(j * srcLength, srcLength, bufferG);
                srcImageB.exportData(j * srcLength, srcLength, bufferB);
            } catch (IOException error) {
                displayError("Algorithm RGBConcat reports: Export image(s) locked");
                setCompleted(false);
                notifyListeners(this);

                return;
            }

            if (threadStopped) {
                buffer = null;
                bufferR = null;
                bufferG = null;
                bufferB = null;
                notifyListeners(this);
                finalize();

                return;
            }

            if (reMap == true) {

                for (i = 0, id = 0; (i < length) && !threadStopped; i += 4, id++) {

                    if (((i % mod) == 0) && isProgressBarVisible()) {
                        progressBar.updateValue(Math.round((float) (i + (j * length)) / ((nImages * length) - 1) * 100),
                                                runningInSeparateThread);
                    }

                    buffer[i] = 255;
                    buffer[i + 1] = ((bufferR[id] - minR) / (diffR)) * 255;
                    buffer[i + 2] = ((bufferG[id] - minG) / (diffG)) * 255;
                    buffer[i + 3] = ((bufferB[id] - minB) / (diffB)) * 255;
                }

                if (threadStopped) {
                    buffer = null;
                    bufferR = null;
                    bufferG = null;
                    bufferB = null;
                    notifyListeners(this);
                    finalize();

                    return;
                }

            } else {

                for (i = 0, id = 0; (i < length) && !threadStopped; i += 4, id++) {

                    if (((i % mod) == 0) && isProgressBarVisible()) {
                        progressBar.updateValue(Math.round((float) (i + (j * length)) / ((nImages * length) - 1) * 100),
                                                runningInSeparateThread);
                    }

                    buffer[i] = 255;

                    if (bufferR[id] < 0) {
                        buffer[i + 1] = 0;
                    } else if (bufferR[id] > 255) {
                        buffer[i + 1] = 255;
                    } else {
                        buffer[i + 1] = bufferR[id];
                    }

                    if (bufferG[id] < 0) {
                        buffer[i + 2] = 0;
                    } else if (bufferG[id] > 255) {
                        buffer[i + 2] = 255;
                    } else {
                        buffer[i + 2] = bufferG[id];
                    }

                    if (bufferB[id] < 0) {
                        buffer[i + 3] = 0;
                    } else if (bufferB[id] > 255) {
                        buffer[i + 3] = 255;
                    } else {
                        buffer[i + 3] = bufferB[id];
                    }
                }

                if (threadStopped) {
                    buffer = null;
                    bufferR = null;
                    bufferG = null;
                    bufferB = null;
                    notifyListeners(this);
                    finalize();

                    return;
                }
            }

            if (threadStopped) {
                buffer = null;
                bufferR = null;
                bufferG = null;
                bufferB = null;
                notifyListeners(this);
                finalize();

                return;
            }

            try {
                destImage.importData(j * length, buffer, false);
            } catch (IOException error) {
                displayError("Algorithm RGBConcat: Import image(s): " + error);
                setCompleted(false);
                disposeProgressBar();
                notifyListeners(this);

                return;
            }
        }

        buffer = null;
        bufferR = null;
        bufferG = null;
        bufferB = null;

        if (threadStopped) {
            buffer = null;
            bufferR = null;
            bufferG = null;
            bufferB = null;
            notifyListeners(this);
            finalize();

            return;
        }

        destImage.calcMinMax();
        disposeProgressBar();
        setCompleted(true);
    }

    /**
     * Concatinate the image Must run getImageR after running this routine.
     */
    private void calcStoreInPlace() {

        int i, j, n;
        int id;

        int length, srcLength, totSrcLength; // total number of data-elements (pixels) in image
        int totLength;
        float[] buffer; // data-buffer (for pixel data) which is the "heart" of the image\
        float[] bufferR;
        float[] bufferG;
        float[] bufferB;
        float minR = (float) srcImageR.getMin();
        float maxR = (float) srcImageR.getMax();
        float minG = (float) srcImageG.getMin();
        float maxG = (float) srcImageG.getMax();
        float minB = (float) srcImageB.getMin();
        float maxB = (float) srcImageB.getMax();
        int[] extents;
        String imageName;
        FileInfoBase[] fInfoBase = null;

        int nImages = 1;

        buildProgressBar(srcImageR.getImageName(), "Concatinating gray images ...", 0, 100);

        extents = srcImageR.getExtents();
        imageName = srcImageR.getImageName();

        srcLength = srcImageR.getSliceSize();
        length = 4 * srcLength;

        if (srcImageR.getNDims() == 3) {
            nImages = extents[2];
        } else if (srcImageR.getNDims() == 4) {
            nImages = extents[2] * extents[3];
        } else {
            nImages = 1;
        }

        totSrcLength = nImages * srcLength;
        totLength = nImages * length;

        int mod = (nImages * length) / 20;

        try {
            bufferR = new float[totSrcLength];

            if (srcImageR.getImageName() != srcImageG.getImageName()) {
                bufferG = new float[totSrcLength];
            } else {
                bufferG = bufferR;
            }

            if ((srcImageB.getImageName() != srcImageR.getImageName()) &&
                    (srcImageB.getImageName() != srcImageG.getImageName())) {
                bufferB = new float[totSrcLength];
            } else if (srcImageB.getImageName() == srcImageR.getImageName()) {
                bufferB = bufferR;
            } else {
                bufferB = bufferG;
            }
        } catch (OutOfMemoryError e) {
            bufferR = null;
            bufferG = null;
            bufferB = null;
            System.gc();
            displayError("Algorithm RGBConcat reports: Out of memory when creating image buffer");
            setCompleted(false);
            notifyListeners(this);

            return;
        }

        initProgressBar();

        float diffR = maxR - minR;
        float diffG = maxG - minG;
        float diffB = maxB - minB;

        if (diffR == 0) {
            diffR = 1;
        }

        if (diffG == 0) {
            diffG = 1;
        }

        if (diffB == 0) {
            diffB = 1;
        }

        try {
            srcImageR.exportData(0, totSrcLength, bufferR);
            srcImageG.exportData(0, totSrcLength, bufferG);
            srcImageB.exportData(0, totSrcLength, bufferB);
        } catch (IOException error) {
            displayError("Algorithm RGBConcat reports: Export image(s) locked");
            setCompleted(false);
            notifyListeners(this);

            return;
        }

        fInfoBase = new FileInfoBase[nImages];

        for (n = 0; n < srcImageR.getFileInfo().length; n++) {
            fInfoBase[n] = (FileInfoBase) (srcImageR.getFileInfo(n).clone());
            fInfoBase[n].setDataType(ModelStorageBase.ARGB);
        }

        if (srcImageR.getParentFrame() != null) {
            srcImageR.getParentFrame().close();
        }

        srcImageR.disposeLocal();
        srcImageR = null;

        try {
            buffer = new float[totLength];
        } catch (OutOfMemoryError e) {
            buffer = null;
            bufferR = null;
            bufferG = null;
            bufferB = null;
            System.gc();
            displayError("Algorithm RGBConcat reports: Out of memory when creating image buffer");
            setCompleted(false);
            notifyListeners(this);

            return;
        }


        if (threadStopped) {
            buffer = null;
            bufferR = null;
            bufferG = null;
            bufferB = null;
            notifyListeners(this);
            finalize();

            return;
        }

        if (reMap == true) {

            for (i = 0, id = 0; (i < totLength) && !threadStopped; i += 4, id++) {

                if (((i % mod) == 0) && isProgressBarVisible()) {
                    progressBar.updateValue(Math.round((float) (i) / (totLength - 1) * 100), runningInSeparateThread);
                }

                buffer[i] = 255;
                buffer[i + 1] = ((bufferR[id] - minR) / (diffR)) * 255;
                buffer[i + 2] = ((bufferG[id] - minG) / (diffG)) * 255;
                buffer[i + 3] = ((bufferB[id] - minB) / (diffB)) * 255;
            }

            if (threadStopped) {
                buffer = null;
                bufferR = null;
                bufferG = null;
                bufferB = null;
                notifyListeners(this);
                finalize();

                return;
            }

        } else {

            for (i = 0, id = 0; (i < totLength) && !threadStopped; i += 4, id++) {

                if (((i % mod) == 0) && isProgressBarVisible()) {
                    progressBar.updateValue(Math.round((float) (i) / (totLength - 1) * 100), runningInSeparateThread);
                }

                buffer[i] = 255;

                if (bufferR[id] < 0) {
                    buffer[i + 1] = 0;
                } else if (bufferR[id] > 255) {
                    buffer[i + 1] = 255;
                } else {
                    buffer[i + 1] = bufferR[id];
                }

                if (bufferG[id] < 0) {
                    buffer[i + 2] = 0;
                } else if (bufferG[id] > 255) {
                    buffer[i + 2] = 255;
                } else {
                    buffer[i + 2] = bufferG[id];
                }

                if (bufferB[id] < 0) {
                    buffer[i + 3] = 0;
                } else if (bufferB[id] > 255) {
                    buffer[i + 3] = 255;
                } else {
                    buffer[i + 3] = bufferB[id];
                }
            }

            if (threadStopped) {
                buffer = null;
                bufferR = null;
                bufferG = null;
                bufferB = null;
                notifyListeners(this);
                finalize();

                return;
            }
        }

        if (threadStopped) {
            buffer = null;
            bufferR = null;
            bufferG = null;
            bufferB = null;
            notifyListeners(this);
            finalize();

            return;
        }

        bufferR = null;
        bufferG = null;
        bufferB = null;

        srcImageR = new ModelImage(ModelStorageBase.ARGB, extents, imageName);

        for (n = 0; n < srcImageR.getFileInfo().length; n++) {
            srcImageR.setFileInfo(fInfoBase[n], n);
        }


        try {
            srcImageR.importData(0, buffer, true);
        } catch (IOException error) {
            displayError("Algorithm RGBConcat: Import image(s): " + error);
            setCompleted(false);
            disposeProgressBar();
            notifyListeners(this);

            return;
        }

        buffer = null;
        bufferR = null;
        bufferG = null;
        bufferB = null;

        if (threadStopped) {
            buffer = null;
            bufferR = null;
            bufferG = null;
            bufferB = null;
            notifyListeners(this);
            finalize();

            return;
        }

        disposeProgressBar();
        setCompleted(true);
    }

    /**
     * Constructs a string of the contruction parameters and out puts the string to the messsage frame if the logging
     * procedure is turned on.
     */
    private void constructLog() {
        historyString = new String("RGBConcat()\n");
    }

}
