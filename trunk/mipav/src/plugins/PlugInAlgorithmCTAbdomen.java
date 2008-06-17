import gov.nih.mipav.model.algorithms.AlgorithmBase;
import gov.nih.mipav.model.algorithms.AlgorithmMorphology2D;
import gov.nih.mipav.model.algorithms.AlgorithmMorphology3D;
import gov.nih.mipav.model.algorithms.AlgorithmRegionGrow;
import gov.nih.mipav.model.algorithms.AlgorithmThresholdDual;
import gov.nih.mipav.model.algorithms.AlgorithmVOIExtractionPaint;
import gov.nih.mipav.model.file.FileVOI;
import gov.nih.mipav.model.structures.CubeBounds;
import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.model.structures.ModelStorageBase;
import gov.nih.mipav.model.structures.Point3Ds;
import gov.nih.mipav.model.structures.VOI;
import gov.nih.mipav.model.structures.VOIContour;
import gov.nih.mipav.model.structures.VOIVector;

import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.ViewJFrameImage;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;



public class PlugInAlgorithmCTAbdomen extends AlgorithmBase {
    
    /** X dimension of the CT image */
    private int xDim;

    /** Y dimension of the CT image */
    private int yDim;

    /** Z dimension of the CT image */
    private int zDim;

    /** Slice size for xDim*yDim */
    private int sliceSize;

    private ModelImage abdomenImage;
    // center-of-mass array for region 1 and 2 (the thresholded bone)
    private int[] x1CMs;
    private int[] y1CMs;
    private int[] x2CMs;
    private int[] y2CMs;
    
    // temp buffer to store slices.  Needed in many member functions.
    private short[] sliceBuffer;

    private short abdomenTissueLabel = 10;
    
    private boolean initializedFlag = false;
    
    private BitSet volumeBitSet;

    /**The final abdomen VOI*/
    private VOI abdomenVOI;
    
    private String imageDir;
    
    private Color voiColor;
    
    /**
     * Constructor.
     *
     * @param  resultImage  Result image model
     * @param  srcImg       Source image model.
     */
    public PlugInAlgorithmCTAbdomen(ModelImage resultImage, ModelImage srcImg, String imageDir, Color color) {
        super(resultImage, srcImg);
        
        this.imageDir = imageDir+File.separator;
        this.voiColor = color;
        
        abdomenVOI = null;
    }

    
    
    /**
     * Starts the algorithm.
     */
    public void runAlgorithm() {
        // Algorithm to determine the outer thigh boundary and boundaries of the bone and bone marrow
        
        if (!initializedFlag) {
            init();
        }
        
        if (!initializedFlag) {
            return;
        }
        
        segmentImage();
    } // end runAlgorithm()
    
    

//  ~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Prepares this class for destruction.
     */
    public void finalize() {
        destImage = null;
        srcImage = null;
        super.finalize();
    }
    
    
    
    /**
     * Create all the data structures that are needed by the various routines to automatically
     * segment the bone, bone marrow, and muscle bundle in 2D and 3D CT images of the thighs.
     */
    private void init() {
        // simple error check up front
        if ((srcImage.getNDims() != 2) && srcImage.getNDims() != 3) {
            MipavUtil.displayError("PlugInAlgorithmNewGeneric2::init() Error image is not 2 or 3 dimensions");
            return;
        }
        
        // set and allocate know values
        xDim = srcImage.getExtents()[0];
        yDim = srcImage.getExtents()[1];

        sliceSize = xDim * yDim;

        try {
            sliceBuffer = new short[sliceSize];
            x1CMs = new int [sliceSize];
            y1CMs = new int [sliceSize];
            x2CMs = new int [sliceSize];
            y2CMs = new int [sliceSize];
        } catch (OutOfMemoryError error) {
            System.gc();
            MipavUtil.displayError("Out of memory: init()");
        }

        // set values that depend on the source image being a 2D or 3D image
        if (srcImage.getNDims() == 2) 
            zDim = 1;
        else if (srcImage.getNDims() == 3) 
            zDim = srcImage.getExtents()[2];

        // make the label images and initialize their resolutions
        abdomenImage = new ModelImage(ModelStorageBase.USHORT, srcImage.getExtents(), "thighTissueImage");
//        muscleBundleImage = new ModelImage(ModelStorageBase.USHORT, srcImage.getExtents(), "muscleBundleImage");
       
        // make the resolutions of the images the same as the source image
        for (int i = 0; i < zDim; i++) {
            abdomenImage.getFileInfo()[i].setResolutions(srcImage.getFileInfo()[i].getResolutions());
        }
        
        volumeBitSet = new BitSet();
               
        // set initialized flag to true so the data structures are not reallocated
        initializedFlag = true;
    } // end init()
    
    
    /**
     * Find the bone, bone marrow, and the thigh tissue
     */
    private void segmentImage() {
        long time = System.currentTimeMillis();
        boolean doVOI = false, completeVOI = false;
        // compute the bone label image
        System.out.println("Bone segmentation: "+(System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        if(doVOI)
        	doVOI = segmentAbdomenTissue();
        System.out.println("Thigh tissue segmentation: "+(System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        if(doVOI)
        	completeVOI = makeAbdomenTissueVOI();
        System.out.println("Thigh tissue VOIs: "+(System.currentTimeMillis() - time));
        if(completeVOI) {
	        
	     // save the VOI to a file(s)
	        String directory = System.getProperty("user.dir");
	        System.out.println("directory: " +imageDir);
	        FileVOI fileVOI;
	        
	        String fileName = "Abdomen.xml";
	        try {
	            fileVOI = new FileVOI(fileName, imageDir, abdomenImage);
	            fileVOI.writeVOI(abdomenVOI, true);
	        } catch (IOException ex) {
	            System.err.println("Error segmentImage():  Opening VOI file");
	            return;
	        }     
        } else
        	System.err.println("Automatic VOIs not created");

   } // create a voi for the outside of the thigh.  Assumes the thighTissueImage has been created.
    private boolean makeAbdomenTissueVOI() {
        // make the volumeBitSet for the thigh tissue
        boolean completedThigh = true;
        int sliceByteOffset;

        for (int volumeIdx = 0, sliceNum = 0; sliceNum < zDim; sliceNum++) {
            sliceByteOffset = sliceNum * sliceSize;
            try {
                abdomenImage.exportData(sliceByteOffset, sliceSize, sliceBuffer);
            } catch (IOException ex) {
                System.err.println("Error exporting data");
                return false;
            }
            for (int sliceIdx = 0; sliceIdx < sliceSize; sliceIdx++, volumeIdx++) {
                if (sliceBuffer[sliceIdx] > 0) {
                    volumeBitSet.set(volumeIdx);
                }
            } // end for (int sliceIdx = 0; ...)
        } // end for(int sliceNum = 0; ...)
        
        // volumeBitSet should be set for the thigh tissue
        short voiID = 0;
        AlgorithmVOIExtractionPaint algoPaintToVOI = new AlgorithmVOIExtractionPaint(abdomenImage,
                volumeBitSet, xDim, yDim, zDim, voiID);

        algoPaintToVOI.setRunningInSeparateThread(false);
        algoPaintToVOI.run();
        setCompleted(true);
        
        // make sure we got one VOI composed of two curves
        VOIVector vois = abdomenImage.getVOIs();
        if(vois.size() != 1) {
            System.err.println("makeThighTissueVOI() Error, did not get 1 VOI");
            return false;
        }
        
        // thighTissueImage has one VOI, lets get it
        VOI theVOI = vois.get(0);
        theVOI.setName("Thigh Tissue");

        // Remove small (10 points or less) curves from the VOI
        int numCurves, numPoints;
        VOIContour curve;
        for (int idx = 0; idx < zDim; idx++) {
            numCurves = theVOI.getCurves()[idx].size();
            
            int idx2 = 0;
            while(idx2 < numCurves) {
                curve = ((VOIContour)theVOI.getCurves()[idx].get(idx2));
                numPoints = curve.size();
                if (numPoints < 10) {
                    // remove the curve
                    theVOI.getCurves()[idx].remove(idx2);
                    numCurves--;
                } else {
                    idx2++;
                }
            } // end while(idx2 < numCurves)
         } // end for (int idx = 0; ...)

/*
        // print out the curves and their sizes
        for (int idx = 0; idx < zDim; idx++) {
            numCurves = theVOI.getCurves()[idx].size();
            System.out.println("slice: " +idx +"  number of curves: " +numCurves);
            
            // print out the size of each curve
            for (int idx2 = 0; idx2 < numCurves; idx2++) {
                curve = ((VOIContour)theVOI.getCurves()[idx].get(idx2));
                numPoints = curve.size();
                System.out.println("  curve: " +idx2 +"  num points: " +numPoints);
            } // end for (int idx2 = 0; ...)
        } // end for (int idx = 0; ...)
*/        

        // split the thigh curves when the legs touch (there are only 3 curves, not 4)
        for (int sliceIdx = 0; sliceIdx < zDim; sliceIdx++) {
            if (theVOI.getCurves()[sliceIdx].size() == 3) {
                
                // find the curve with the greatest area (the thigh curve)
                float maxArea = ((VOIContour)theVOI.getCurves()[sliceIdx].get(0)).area();
                int maxIdx = 0;
                for (int idx = 1; idx < theVOI.getCurves()[sliceIdx].size(); idx++) {
                    if (((VOIContour)theVOI.getCurves()[sliceIdx].get(idx)).area() > maxArea) {
                        maxIdx = idx;
                    }
                }
                
//                System.out.println("Slice num: " +sliceIdx +"   thigh curve idx: " +maxIdx);

                /*
                 The next part of the code may be error prone.  startIdx1 should be between 
                 the zeroth contour point and the minimum point on the top part of the contour,
                 the point a index upperCurveMinIdx.  endIdx2 should be after minIdx.  startIdx2
                 should come next, followed by the maximum point on the bottom part of the contour
                 (lowerCurveMaxIdx) and finally endIdx2, which should be less than the number of points
                 in the contour.  If these assumptions are not correct, this part of the code
                 will fail, and we will not be able to split a joined single thigh contour into
                 a left and right thigh contour
                 
                 */
                // split the curve through its middle with the maxIdx
                VOIContour maxContour = ((VOIContour)theVOI.getCurves()[sliceIdx].get(maxIdx));
                int[] xVals = new int [maxContour.size()];
                int[] yVals = new int [maxContour.size()];
                int[] zVals = new int [maxContour.size()];
                maxContour.exportArrays(xVals, yVals, zVals);
                
                // find the indices of an upper and lower section of the contour that is "close" to the image center
                // find the index of the first contour point whose x-component is "close" to the middle
                // startIdx1 represents the first point on the contour that is within 20 units of the image center
                int startIdx1 = 0;
                while (xVals[startIdx1] <= ((xDim / 2) - 20) || xVals[startIdx1] >= ((xDim / 2) + 20)) {
                    startIdx1++;
                }
                
                // endIdx1 is the last point on the top curve
                int endIdx1 = startIdx1 + 1;
                while (xVals[endIdx1] >= ((xDim / 2) - 20) && xVals[endIdx1] <= ((xDim / 2) + 20)) {
                    endIdx1++;
                }
                // subtract 1 since we indexed one element too far
                endIdx1--;
//                System.out.println("\nStart index 1: " +startIdx1 +"   end index 1: " +endIdx1);

                // find the index of the second contour section whose x-component is "close" to the middle
                int startIdx2 = endIdx1 + 1;
                while (xVals[startIdx2] <= ((xDim / 2) - 20) || xVals[startIdx2] >= ((xDim / 2) + 20)) {
                    startIdx2++;
                }
                
                int endIdx2 = startIdx2 + 1;
                while (xVals[endIdx2] >= ((xDim / 2) - 20) && xVals[endIdx2] <= ((xDim / 2) + 20)) {
                    endIdx2++;
                }
                // subtract 1 since we indexed one element too far
                endIdx2--;
//                System.out.println("Start index 2: " +startIdx2 +"   end index 1: " +endIdx2);

     
                // find the index of the two closest points between these two sections, this is where we will split the contour
                // one contour section goes from startIdx1 to endIdx1
                // the other goes from startIdx2 to endIdx2
                int upperCurveMinIdx = startIdx1;
                int lowerCurveMaxIdx = startIdx2;
                float dx = xVals[startIdx1] - xVals[startIdx2];
                float dy = yVals[startIdx1] - yVals[startIdx2];
                // all computations will be on the same slice, forget about the z-component
                float minDistance = dx*dx + dy*dy;
                float dist;
                
                for (int idx1 = startIdx1; idx1 <= endIdx1; idx1++) {
                    for (int idx2 = startIdx2; idx2 <= endIdx2; idx2++) {
                        dx = xVals[idx1] - xVals[idx2];
                        dy = yVals[idx1] - yVals[idx2];
                        dist = dx*dx + dy*dy;
                        if (dist < minDistance) {
                            minDistance = dist;
                            upperCurveMinIdx = idx1;
                            lowerCurveMaxIdx = idx2;
                        } // end if
                    } // end for (int idx2 = startIdx2; ...
                } // end for (int idx1 = startIdx1; ...
                
//                System.out.println("Slice: " + sliceIdx +"   Minimum distance: " +minDistance);
//                System.out.println(xVals[upperCurveMinIdx] +"  " +yVals[upperCurveMinIdx] +"   and  " +xVals[lowerCurveMaxIdx] +"  " +yVals[lowerCurveMaxIdx]);
                
                // make the two contours resulting from the split
                ArrayList<Integer> x1Arr = new ArrayList<Integer>(maxContour.size());
                ArrayList<Integer> y1Arr = new ArrayList<Integer>(maxContour.size());
                ArrayList<Integer> z1Arr = new ArrayList<Integer>(maxContour.size());
                
                ArrayList<Integer> x2Arr = new ArrayList<Integer>(maxContour.size());
                ArrayList<Integer> y2Arr = new ArrayList<Integer>(maxContour.size());
                ArrayList<Integer> z2Arr = new ArrayList<Integer>(maxContour.size());
                
                if (upperCurveMinIdx < lowerCurveMaxIdx) {
                    // the first contour (left thigh) goes from minIdx1 to minIdx2 to minIdx1
                    int newIdx = 0;
                    for (int idx = upperCurveMinIdx; idx < lowerCurveMaxIdx; idx++, newIdx++) {
                        x1Arr.add(newIdx, xVals[idx]);
                        y1Arr.add(newIdx, yVals[idx]);
                        z1Arr.add(newIdx, zVals[idx]);
                    }

                    // the second contour (right thigh) goes from minIdx2 to maxContour.size(), 0 , 1, to minIdx1 to minIdx2
                    newIdx = 0;
                    for (int idx = lowerCurveMaxIdx; idx < maxContour.size(); idx++, newIdx++) {
                        x2Arr.add(newIdx, xVals[idx]);
                        y2Arr.add(newIdx, yVals[idx]);
                        z2Arr.add(newIdx, zVals[idx]);
                    }
                    for (int idx = 0; idx < upperCurveMinIdx; idx++, newIdx++) {
                        x2Arr.add(newIdx, xVals[idx]);
                        y2Arr.add(newIdx, yVals[idx]);
                        z2Arr.add(newIdx, zVals[idx]);
                    }
                } // end if (minIdx1 < minIdx2)

                int[] x1 = new int[x1Arr.size()];
                int[] y1 = new int[x1Arr.size()];
                int[] z1 = new int[x1Arr.size()];
                for(int idx = 0; idx < x1Arr.size(); idx++) {
                    x1[idx] = x1Arr.get(idx);
                    y1[idx] = y1Arr.get(idx);
                    z1[idx] = z1Arr.get(idx);
                }

                abdomenVOI.importCurve(x1, y1, z1, sliceIdx);
                
                int[] x2 = new int[x2Arr.size()];
                int[] y2 = new int[x2Arr.size()];
                int[] z2 = new int[x2Arr.size()];
                for(int idx = 0; idx < x2Arr.size(); idx++) {
                    x2[idx] = x2Arr.get(idx);
                    y2[idx] = y2Arr.get(idx);
                    z2[idx] = z2Arr.get(idx);
                }
                                               
            } else if (theVOI.getCurves()[0].size() != 4) {
                abdomenImage.unregisterAllVOIs();
                abdomenImage.registerVOI(theVOI);            
                new ViewJFrameImage(abdomenImage).updateImages(true);
                System.err.println("makeThighTissueVOI() Error, did not get 2 curves in the VOI");
                completedThigh = false;
            } else {
                //abdomenVOI = makeAbdomenTissueVOI();
                
                // Right leg VOI is the left most in the image
                int[] rightBoundsX = new int [2];
                int[] rightBoundsY = new int [2];
                int[] rightBoundsZ = new int [2];
                VOIContour rightCurve;
                rightCurve = ((VOIContour)abdomenVOI.getCurves()[0].get(0));
                rightCurve.getBounds(rightBoundsX, rightBoundsY, rightBoundsZ);
                

            

            }

        } // end for (sliceIdx = 0; ...)
        
        
        // show the split VOIs
        //boneImage.unregisterAllVOIs();
        //boneImage.registerVOI(rightThighVOI);            
        //boneImage.registerVOI(leftThighVOI);            
        //new ViewJFrameImage(boneImage).updateImages(true);
        return completedThigh;

        
        


    } // end makeThighTissueVOI()
    
    
    private boolean segmentAbdomenTissue() {
         
        // let's just grow a region
        // we need a seed point
        // walk out along the x-axis from the center-of-mass of the bone on the first slice
        try {
            srcImage.exportData(0, sliceSize, sliceBuffer);
        } catch (IOException ex) {
            System.err.println("Error exporting data");
        }
        
        int xcm = x1CMs[0];
        int ycm = y1CMs[0];
        
        // the center-of-mass should be in the marrow, increment x until we get to bone
        int idx = ycm * xDim + xcm;
        while(sliceBuffer[idx] < 750) {
            xcm++;
            idx++;
        }
        // increment x until we get past the bone
        while(sliceBuffer[idx] > 750) {
            xcm++;
            idx++;
        }
        // use the seed point that is 5 more x units away from the bone
        short seedX = (short)(xcm + 5);
        short seedY = (short)ycm;
        short seedZ = 0;
        short seedVal = sliceBuffer[idx + 5];
        
        BitSet thigh1Bitmap = new BitSet();
        regionGrowMuscle(seedX, seedY, seedZ, seedVal, thigh1Bitmap);
        
        // segment the second muscle bundle
        xcm = x2CMs[0];
        ycm = y2CMs[0];
        
        // the center-of-mass should be in the marrow, increment x until we get to bone
        idx = ycm * xDim + xcm;
        while(sliceBuffer[idx] < 750) {
            xcm++;
            idx++;
        }
        // increment x until we get past the bone
        while(sliceBuffer[idx] > 750) {
            xcm++;
            idx++;
        }
        // use the seed point that is 5 more x units away from the bone
        seedX = (short)(xcm + 5);
        seedY = (short)ycm;
        seedZ = 0;
        seedVal = sliceBuffer[idx + 5];
        
        BitSet thigh2Bitmap = new BitSet();
        regionGrowMuscle(seedX, seedY, seedZ, seedVal, thigh2Bitmap);

        // make the thighTissue label image slice by slice from the 3D region grown BitSet
        // bitSetIdx is a cumulative index into the 3D BitSet
        for (int bitSetIdx = 0, sliceNum = 0; sliceNum < zDim; sliceNum++) {
            for (int sliceIdx = 0; sliceIdx < sliceSize; sliceIdx++, bitSetIdx++) {
                if (thigh1Bitmap.get(bitSetIdx) || thigh2Bitmap.get(bitSetIdx)) {
                    sliceBuffer[sliceIdx] = abdomenTissueLabel;
                } else {
                    sliceBuffer[sliceIdx] = 0;
                }
            } // end for (int sliceIdx = 0; ...)
            
            // save the sliceBuffer into the boneMarrowImage
            try {
                abdomenImage.importData(sliceNum * sliceSize, sliceBuffer, false);
            } catch (IOException ex) {
                System.err.println("segmentThighTissue(): Error importing data");
            }
        } // end for(int bitSetIdx = 0, sliceNum = 0; ...)
        return true;
    } // end segmentThighTissue(...)
    
    
    
    private void regionGrowMuscle(short sX, short sY, short sZ, short seedVal, BitSet muscleBits) {
       try {
           AlgorithmRegionGrow regionGrowAlgo = new AlgorithmRegionGrow(srcImage, 1.0f, 1.0f);

           regionGrowAlgo.setRunningInSeparateThread(false);

           if (srcImage.getNDims() == 2) {
               regionGrowAlgo.regionGrow2D(muscleBits, new Point(sX, sY), -1,
                                           false, false, null, seedVal - 300,
                                           seedVal + 1000, -1, -1, false);
           } else if (srcImage.getNDims() == 3) {
               CubeBounds regionGrowBounds;
               regionGrowBounds = new CubeBounds(xDim, 0, yDim, 0, zDim, 0);
               regionGrowAlgo.regionGrow3D(muscleBits, new Point3Ds(sX, sY, sZ), -1,
                                                   false, false, null, seedVal - 300,
                                                   seedVal + 1000, -1, -1, false,
                                                   0, regionGrowBounds);
//               System.out.println("Muscle Count: " +count);
           }
       } catch (OutOfMemoryError error) {
           System.gc();
           MipavUtil.displayError("Out of memory: regionGrowMuscle");
       }

   } // regionGrowMuscle(...)
   
   public VOI getAbdomenVOI() {
	   return abdomenVOI;
   }
   
   public ModelImage threshold(ModelImage threshSourceImg, float[] thresh) {
        ModelImage resultImage = null;
        resultImage = new ModelImage(ModelStorageBase.UBYTE, threshSourceImg.getExtents(), "threshResultImg");

        AlgorithmThresholdDual threshAlgo = null;
        threshAlgo = new AlgorithmThresholdDual(resultImage, threshSourceImg, thresh, abdomenTissueLabel, AlgorithmThresholdDual.BINARY_TYPE, true, false);
        threshAlgo.run();

        return resultImage;
    } // end threshold(...)

    /**
     * morphological ID_OBJECTS.
     *
     * @param  srcImage  --source image
     * @param  min       --smallest object to let through
     * @param  max       --largest object to let through
     */
    public void IDObjects2D(ModelImage srcImage, int min, int max) {
        AlgorithmMorphology2D MorphIDObj = null;
        MorphIDObj = new AlgorithmMorphology2D(srcImage, 4, 1, AlgorithmMorphology2D.ID_OBJECTS, 0, 0, 0, 0, true);
        MorphIDObj.setMinMax(min, max);
        MorphIDObj.run();
    }

    /**
     * morphological ID_OBJECTS.
     *
     * @param  srcImage  --source image
     * @param  min       --smallest object to let through
     * @param  max       --largest object to let through
     */
    public void IDObjects3D(ModelImage srcImage, int min, int max) {
        AlgorithmMorphology3D MorphIDObj = null;
        MorphIDObj = new AlgorithmMorphology3D(srcImage, 4, 1, AlgorithmMorphology3D.ID_OBJECTS, 0, 0, 0, 0, true);
        MorphIDObj.setMinMax(min, max);
        MorphIDObj.run();
    }

    
    
    /**
     * DOCUMENT ME!
     *
     * @param  sourceImg  DOCUMENT ME!
     * @param  Name       DOCUMENT ME!
     */
    public void ShowImage(ModelImage sourceImg, String Name) {
        ModelImage cloneImg = (ModelImage) sourceImg.clone();
        cloneImg.calcMinMax();
        cloneImg.setImageName(Name);
        new ViewJFrameImage(cloneImg);
    }
    



}
