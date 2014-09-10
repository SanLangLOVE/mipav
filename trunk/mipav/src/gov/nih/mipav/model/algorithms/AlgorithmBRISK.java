package gov.nih.mipav.model.algorithms;


import gov.nih.mipav.model.structures.*;
import gov.nih.mipav.view.*;
import gov.nih.mipav.view.dialogs.*;

import java.io.*;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Vector;

import javax.vecmath.Point2d;
import javax.vecmath.Point2i;

import WildMagic.LibFoundation.Mathematics.Vector3f;

/**
BRISK - Binary Robust Invariant Scalable Keypoints
Reference implementation of
[1] Stefan Leutenegger,Margarita Chli and Roland Siegwart, BRISK:
	Binary Robust Invariant Scalable Keypoints, in Proceedings of
	the IEEE International Conference on Computer Vision (ICCV2011).

Copyright (C) 2011  The Autonomous Systems Lab (ASL), ETH Zurich,
Stefan Leutenegger, Simon Lynen and Margarita Chli.

This file is part of BRISK.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
   * Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
   * Neither the name of the ASL nor the names of its contributors may be 
     used to endorse or promote products derived from this software without 
     specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

public class AlgorithmBRISK extends AlgorithmBase {
	
	// List of images to be matched
	private ModelImage destImage[] = null;
	
	// FAST/AGAST detection threshold.  The default value is 60.
	private double threshold = 60.0;
	
	// Number of octaves for the detection. The default value is 4
	private int octaves = 4;
	
	private boolean rotationInvariant = true;
	
	private boolean scaleInvariant = true;
	
	// Scale factor for the BRISK pattern.  The default value is 1.0.
	private double patternScale = 1.0;
	
	private static final int TYPE_STANDARD = 1;
	
	private static final int TYPE_S = 2;
	
	private static final int TYPE_U = 3;
	
	private static final int TYPE_SU = 4;
	
	// BRISK special types are 'S', 'U', and 'SU'.  By default, the standard BRISK is used.
	private int type = TYPE_STANDARD;
	
	// First intialize BRISK.  This will create the pattern look-up table so this
	// may take some fraction of a second.  Do not rerun!
	// After initialization stages are:
	// 1.) Setting up the detector and detecting the keypoints.  Optionally get the points back.
	// 2.) Descriptor extraction.  Construct the extractor.  Make sure to do this only once:
	//     this will build up the look-up tables, which is consuming a considerable amount of time.
	// Constructor variants for arbitrary costumization available.
	// Get the descriptors and arbitrary keypoints.
	// 3.) Matching.  Construct the matcher and process an arbitrary number of images.
	//     Use radiusMatch, or alternatively use knnMatch (or match for k == 1)
	
	// Detect the keypoints.  Optionally get the keypoints back.
	private boolean detect = true;
	
	// Get the descriptors and the corresponding keypoints.
	private boolean describe = true;
	
	private static final int NO_MATCH = 1;
	
	private static final int radiusMatch = 2;
	
	private static final int knnMatch = 3;
	
	private int match = radiusMatch;
	
	private Vector<Double>radiusList = null;
	
	private Vector<Integer>numberList = null;
	
	// Short pair maximum distance
	private double dMax = 5.85;
	
	// Long pair maximum distance
	private double dMin = 8.2;
	
	private Vector<Integer>indexChange = new Vector<Integer>();
	
	private static final double basicSize = 12.0;
	
	// Scales discretization
	private static final int scales = 64;
	
	// 40->4 Octaves - else, this needs to be adjusted
	private static final double scaleRange = 30.0;
	
    // Discretization of the rotation look-up pairs
    private final int n_rot = 1204;
    
    private final double safetyFactor = 1.0;
    
    // Total number of collocation  points
    private int points;
    
    // Pattern properties
    private BriskPatternPoint patternPoints[]; // [i][rotation][scale]
    
    // Lists the scaling per scale index [scale]
    private double scaleList[];
    
    // Lists the total pattern size per scale index [scale]
    private int sizeList[];
    
    // d < dMax
    private BriskShortPair shortPairs[];
    
    // d > dMin
    private  BriskLongPair longPairs[];
    
    // Number of shortPairs
    private int numShortPairs;
    
    // Number of longPairs
	private int numLongPairs;
	
	// Number of uchars the descriptor consists of
	private int strings;
	
	// The image pyramids
	private int layers;
	private Vector<BriskLayer> pyramid = new Vector<BriskLayer>();
	
	private static final int HALFSAMPLE = 0;
	
	private static final int TWOTHIRDSAMPLE = 1;
	
	private double safeThreshold;
	
	
	//~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * AlgorithmBRISK - default constructor.
     */
    public AlgorithmBRISK() { }
    
    public AlgorithmBRISK(ModelImage srcImg) {
    	super(null, srcImg);
    }
    
    /**
     * @param destImage List of images to be matched
     * @param srcImg Source image
     * @param threshold FAST/AGAST detection threshold
     * @param octaves Number of octaves for the detection
     * @param rotationInvariant
     * @param scaleInvariant
     * @param patternScale Scale factor for the BRISK pattern.
     * @param type
     * @param detect Detect the keypoints.  Optionally get the keypoints back.
     * @param describe  Get the descriptors and the corresponding keypoints.
     * @param match NO_MATCH, radiusMatch, or knnMatch
     * @param radiusList
     * @param numberList
     * @param dMax Short pair maximum distance
     * @param dMin Long pair maximum distance
     * @param indexChange
     */
    public AlgorithmBRISK(ModelImage destImage[], ModelImage srcImg, double threshold, int octaves,
    		boolean rotationInvariant, boolean scaleInvariant,
    		double patternScale, int type, boolean detect, boolean describe, int match,
    		Vector<Double>radiusList, Vector<Integer>numberList, double dMax, double dMin, 
    		Vector<Integer>indexChange) {
    	super(null, srcImg);
    	this.destImage = destImage;
    	this.threshold = threshold;
    	this.octaves = octaves;
    	this.rotationInvariant = rotationInvariant;
    	this.scaleInvariant = scaleInvariant;
    	this.patternScale = patternScale;
    	this.type = type;
    	this.detect = detect;
    	this.describe = describe;
    	this.match = match;
    	this.radiusList = radiusList;
    	this.numberList = numberList;
    	this.dMax = dMax;
    	this.dMin = dMin;
    	this.indexChange = indexChange;
    }
    
    /**
     * finalize -
     */
    public void finalize() {
    	patternPoints = null;
    	shortPairs = null;
    	longPairs = null;
    	scaleList = null;
    	sizeList = null;
        super.finalize();
    }
    
    /**
     * Starts the program.
     */
    public void runAlgorithm() {
    	int xDim;
    	int yDim;
    	int sliceSize;
    	
    	if (srcImage == null) {
            displayError("Source Image is null");
            finalize();

            return;
        }
    	

        fireProgressStateChanged(srcImage.getImageName(), "BRISK ...");

        xDim = srcImage.getExtents()[0];
        yDim = srcImage.getExtents()[1];
        sliceSize = xDim * yDim;
        
        if (describe) {
        	if ((radiusList != null)  && (radiusList.size() != 0) && (numberList != null) &&
        		(radiusList.size() == numberList.size())) {
        	    generateKernel();	
        	}
        	else {
        	    BriskDescriptorExtractor();
        	}
        }
        
     // Construct the matcher
        //cv::Ptr<cv::DescriptorMatcher> descriptorMatcher;
        //descriptorMatcher = new cv::BruteForceMatcher<cv::HammingSse>();

        // process an arbitrary number of images:
        //detector->detect(grayImage1,keypoints1);
        //descriptorExtractor->compute(imgGray1,keypoints1,descriptors1);
        //detector->detect(grayImageN,keypointsN);
        //descriptorExtractor->compute(imgGrayN,keypointsN,descriptorsN);
  
        //descriptorMatcher->radiusMatch(descriptorsI,descriptorsJ,
        		//matches,hammingMax);
        // alternatively use knnMatch (or match for k:=1).
        
        Vector<KeyPoint> keypoints = new Vector<KeyPoint>();
        // Create keypoints
        detectImpl(srcImage, keypoints, null);
        
        short descriptors[][] = null;
        // Create descriptors
        descriptors = computeImpl(srcImage, keypoints);
        System.out.println("keypoints.size() = " + keypoints.size());
        int numNull = 0;
        for (int k = 0; k < keypoints.size(); k++) {
        	if (keypoints.get(k)  == null) {
        		numNull++;
        	}
        }
        System.out.println("Number of null keypoints = " + numNull);
        
        System.out.println("descriptors.length = " + descriptors.length);
        System.out.println("descriptors[0].length = " + descriptors[0].length);
        
        setCompleted(true);
        return;
    }
    
    // Create a descriptor with a standard pattern
    private void BriskDescriptorExtractor() {
        radiusList = new Vector<Double>();
        numberList = new Vector<Integer>();
        double f = 0.85 * patternScale;
        
        // This is the standard pattern found to be suitable
        radiusList.add(0, Double.valueOf(0.0));
        radiusList.add(1, Double.valueOf(f * 2.9));
        radiusList.add(2, Double.valueOf(f * 4.9));
        radiusList.add(3, Double.valueOf(f * 7.4));
        radiusList.add(4, Double.valueOf(f * 10.8));
        
        numberList.add(0, Integer.valueOf(1));
        numberList.add(1, Integer.valueOf(10));
        numberList.add(2, Integer.valueOf(14));
        numberList.add(3, Integer.valueOf(15));
        numberList.add(4, Integer.valueOf(20));
        dMax = 5.85 * patternScale;
        dMin = 8.2 * patternScale;
        indexChange = null;
        generateKernel();
    }
    
    // Call this to generate the kernel:
    // Circle of radius r (pixels), with n points;
    // short pairings with dMax, long pairings with dMin
    private void generateKernel() {
        // Get the total number of points
    	final int rings = radiusList.size();
    	// Remember the total number of points
    	points = 0;
    	for (int ring = 0; ring < rings; ring++) {
    		points += numberList.get(ring);
    	}
    	// Set up the patterns
    	patternPoints = new BriskPatternPoint[points * scales * n_rot];
    	
        int patternIterator = 0;
    	
    	// Define the scale discretization
    	final double lb_scale = Math.log(scaleRange)/Math.log(2.0);
    	final double lb_scale_step = lb_scale/scales;
    	
    	scaleList = new double[scales];
    	sizeList = new int[scales];
    	
    	final double sigma_scale = 1.3;
    	
    	for (int scale = 0; scale < scales; ++scale) {
    		scaleList[scale] = Math.pow(2.0, scale * lb_scale_step);
    		sizeList[scale] = 0;
    		
    		// Generate the pattern points look-up
    		double alpha, theta;
    		for (int rot = 0; rot <n_rot; ++rot) {
    			// This is the rotation of the feature
    		    theta = (double)rot * 2.0 * Math.PI/(double)n_rot;
    		    for (int ring = 0; ring < rings; ++ring) {
    		        for (int num = 0; num < numberList.get(ring); ++num) {
    		            alpha = ((double)num) * 2.0 * Math.PI/(double)numberList.get(ring);
    		            patternPoints[patternIterator] = new BriskPatternPoint();
    		            // Feature rotation plus angle of the point
    		            patternPoints[patternIterator].setX(scaleList[scale] * radiusList.get(ring) * Math.cos(alpha + theta));
    		            patternPoints[patternIterator].setY(scaleList[scale] * radiusList.get(ring) * Math.sin(alpha + theta));
    		            // and the gaussian kernel sigma
    		            if (ring == 0) {
    		            	patternPoints[patternIterator].setSigma(sigma_scale * scaleList[scale] * 0.5);
    		            }
    		            else {
    		            	patternPoints[patternIterator].setSigma(sigma_scale * scaleList[scale] * 
    		            			((double)radiusList.get(ring))* Math.sin(Math.PI/numberList.get(ring)));
    		            }
    		            // Adapt the sizeList if necessary
    		            final int size = (int)Math.ceil(((scaleList[scale] * radiusList.get(ring)) + 
    		            		patternPoints[patternIterator].getSigma())) +1;
    		            if (sizeList[scale] < size) {
    		            	sizeList[scale] = size;
    		            }
    		            
    		            // Increment the iterator
    		            ++patternIterator;
    		        } // for (int num = 0; num < numberList.get(ring); ++num)
    		    } // for (int ring = 0; ring < rings; ++ring)
    		} // for (int rot = 0; rot <n_rot; ++rot)
    	} // for (int scale = 0; scale < scales; ++scale)
    	
    	// Now also generate the pairings
    	shortPairs = new BriskShortPair[points * (points - 1)/2];
    	longPairs = new BriskLongPair[points * (points - 1)/2];
    	numShortPairs = 0;
    	numLongPairs = 0;
    	
    	// Fill indexChange with 0..n if empty
    	if (indexChange == null) {
    		indexChange = new Vector<Integer>();
    	}
    	int indSize = indexChange.size();
    	if (indSize == 0) {
    		indexChange.setSize(points * (points-1)/2);
    		indSize = indexChange.size();
    	}
    	for (int i = 0; i < indSize; i++) {
    		indexChange.add(i, Integer.valueOf(i));
    	}
    	final double dMin_sq = dMin * dMin;
    	final double dMax_sq = dMax * dMax;
    	for (int i = 1; i < points; i++) {
    		// Find all the pairs
    	    for (int j = 0; j < i; j++) {
    	        // Point pair distance
    	    	final double dx = patternPoints[j].getX() - patternPoints[i].getX();
    	    	final double dy = patternPoints[j].getY() - patternPoints[i].getY();
    	    	final double norm_sq = (dx*dx + dy*dy);
    	    	if (norm_sq > dMin_sq) {
    	    	    // Save to long pairs
    	    		longPairs[numLongPairs] = new BriskLongPair();
    	    		longPairs[numLongPairs].setWeighted_dx((int)((dx/norm_sq)*2048.0 + 0.5));
    	    		longPairs[numLongPairs].setWeighted_dy((int)((dy/norm_sq)*2048.0 + 0.5));
    	    		longPairs[numLongPairs].setI(i);
    	    		longPairs[numLongPairs].setJ(j);
    	    		++numLongPairs;
    	    	} // if (norm_sq > dMin_sq)
    	    	else if (norm_sq < dMax_sq) {
    	    	    // Save to short pairs
    	    		// Make sure the user passes something sensible
    	    		if (numShortPairs >= indSize) {
    	    			MipavUtil.displayError("numShortPairs = " + numShortPairs + " >= " + " indSize = " + indSize);
    	    			setCompleted(false);
    	    			return;
    	    		}
    	    		shortPairs[indexChange.get(numShortPairs)] = new BriskShortPair();
    	    		shortPairs[indexChange.get(numShortPairs)].setJ(j);
    	    		shortPairs[indexChange.get(numShortPairs)].setI(i);
    	    		++numShortPairs;
    	    	} // else if (norm_sq < dMax_sq)
    	    } // for (int j = 0; j < i; j++)
    	} // for (int i = 1; i < points; i++)
    	
    	// no bits:
    	strings =  (int)Math.ceil(((float)numShortPairs)/128.0)*4 *4;
    } // private void (generateKernel)
    
    private int smoothedIntensity(ModelImage image, ModelImage integral, final double key_x, final double key_y,
    		final int scale, final int rot, final int point) {
    	// Get the float position
    	final BriskPatternPoint briskPoint = patternPoints[scale * n_rot * points + rot * points + point];
    	final double xf = briskPoint.getX() + key_x;
    	final double yf = briskPoint.getY() + key_y;
    	final int x = (int)xf;
    	final int y = (int)yf;
    	int xDim = image.getExtents()[0];
    	int yDim = image.getExtents()[1];
    	int sliceSize = xDim * yDim;
    	double doubleBuffer[] = new double[sliceSize];
    	try {
    		image.exportData(0, sliceSize, doubleBuffer);
    	}
    	catch (IOException e) {
    		MipavUtil.displayError("IOException " + e + " on image.exportData(0, sliceSize, doubleBuffer) in smoothedIntensity");
    		setCompleted(false);
    		return -1;
    	}
    	
    	// integralXDim = xDim + 1
    	int integralXDim = integral.getExtents()[0];
    	int integralYDim = integral.getExtents()[1];
    	int  integralSlice = integralXDim * integralYDim;
    	double integralBuffer[] = new double[integralSlice];
    	try {
    		integral.exportData(0, integralSlice, integralBuffer);
    	}
    	catch (IOException e) {
    		MipavUtil.displayError("IOException " + e + " on integral.exportData(0, integralSlice, integralBuffer) in smoothedIntensity");
    		setCompleted(false);
    		return -1;
    	}
    	
    	// Get the sigma
    	final double sigma_half = briskPoint.getSigma();
    	final double area = 4.0 * sigma_half * sigma_half;
    	
    	// Calculate output
    	int ret_val;
    	if (sigma_half < 0.5) {
    		// Interpolation multipliers
    		final int r_x = (int)((xf -x)*1024);
    		final int r_y = (int)((yf - y)*1024);
    		final int r_x_1 = (1024 - r_x);
    		final int r_y_1 = (1024 - r_y);
    		int ptr = x + y*xDim;
    		// Just interpolate
    		ret_val = (r_x_1*r_y_1*(int)doubleBuffer[ptr]);
    		ptr++;
    		ret_val += (r_x*r_y_1*(int)doubleBuffer[ptr]);
    		ptr += xDim;
    		ret_val += (r_x*r_y*(int)doubleBuffer[ptr]);
    		ptr--;
    		ret_val += (r_x_1*r_y*(int)doubleBuffer[ptr]);
    		return (ret_val + 512)/1024;
    	} // if (sigma_half < 0.5)
    	
    	// This is the standard case (simple, not speed optimized yet):
    	
    	// Scaling
    	final int scaling = (int)(4194304.0/area);
    	final int scaling2 = (int)(((double)scaling) * area/1024.0);
    	
    	// Calculate borders
    	final double x_1 = xf - sigma_half;
    	final double x1 = xf + sigma_half;
    	final double y_1 = yf - sigma_half;
    	final double y1 = yf + sigma_half;
    	
    	final int x_left = (int)(x_1 + 0.5);
    	final int y_top = (int)(y_1 + 0.5);
    	final int x_right = (int)(x1 + 0.5);
    	final int y_bottom = (int)(y1 + 0.5);
    	
    	// Overlap area - multiplication factors:
    	final double r_x_1 = (double) x_left - x_1 + 0.5;
    	final double r_y_1 = (double) y_top - y_1 + 0.5;
    	final double r_x1 = x1 - (double)x_right + 0.5;
    	final double r_y1 = y1 - (double)y_bottom + 0.5;
    	final int dx = x_right - x_left - 1;
    	final int dy = y_bottom - y_top - 1;
    	final int A = (int)((r_x_1*r_y_1)*scaling);
    	final int B = (int)((r_x1 * r_y_1)*scaling);
    	final int C = (int)((r_x1*r_y1)*scaling);
    	final int D = (int)((r_x_1*r_y1)*scaling);
    	final int r_x_1_i = (int)(r_x_1*scaling);
    	final int r_y_1_i = (int)(r_y_1*scaling);
    	final int r_x1_i = (int)(r_x1*scaling);
    	final int r_y1_i = (int)(r_y1*scaling);
    	
    	if (dx + dy > 2) {
    		// Now the calculation
    		int ptr = x_left + xDim * y_top;
    		// First the corners
    		ret_val = A*(int)doubleBuffer[ptr];
    		ptr += dx + 1;
    		ret_val += B * (int)doubleBuffer[ptr];
    		ptr += dy*xDim + 1;
    		ret_val += C*(int)doubleBuffer[ptr];
    		ptr -= dx+1;
    		ret_val += D*(int)doubleBuffer[ptr];
    		
    		// Next the edges
    		int ptr_integral = x_left + integralXDim*y_top + 1;
    		// Find a simple path through the different surface corners
    		final int tmp1 = ptr_integral;
    		ptr_integral += dx;
    		final int tmp2 = ptr_integral;
    		ptr_integral += integralXDim;
    		final int tmp3 = ptr_integral;
    		ptr_integral++;
    		final int tmp4 = ptr_integral;
    		ptr_integral += dy*integralXDim;
    		final int tmp5 = ptr_integral;
    		ptr_integral--;
    		final int tmp6 = ptr_integral;
    		ptr_integral += integralXDim;
    		final int tmp7 = ptr_integral;
    		ptr_integral -= dx;
    		final int tmp8 = ptr_integral;
    		ptr_integral -= integralXDim;
    		final int tmp9 = ptr_integral;
    		ptr_integral--;
    		final int tmp10 = ptr_integral;
    		ptr_integral -= dy*integralXDim;
    		final int tmp11 = ptr_integral;
    		ptr_integral++;
    		final int tmp12 = ptr_integral;
    		
    		// Assign the wieghted surface integrals
    		final int upper = (tmp3 - tmp2 + tmp1 - tmp12) * r_y_1_i;
    		final int middle = (tmp6 - tmp3 + tmp12 - tmp9) * scaling;
    		final int left = (tmp9 - tmp12 + tmp11 - tmp10) * r_x_1_i;
    		final int right = (tmp5 - tmp4 + tmp3 - tmp6) * r_x1_i;
    		final int bottom = (tmp7 - tmp6 + tmp9 - tmp8) * r_y1_i;
    		return (ret_val+upper+middle+left+right+bottom+scaling2/2)/scaling2;
    	} // if (dx + dy > 2)
    	
    	// Now the calculation
    	int ptr = x_left + xDim * y_top;
    	// First row
    	ret_val = A * (int)doubleBuffer[ptr];
    	ptr++;
    	final int end1 = ptr + dx;
    	for (; ptr < end1; ptr++) {
    		ret_val += r_y_1_i * (int)doubleBuffer[ptr];
    	}
    	ret_val += B * (int)doubleBuffer[ptr];
    	// Middle ones
    	ptr += xDim - dx - 1;
    	int end_j = ptr + dy * xDim;
    	for (; ptr < end_j; ptr += xDim-dx-1) {
    		ret_val += r_x_1_i * (int)doubleBuffer[ptr];
    		ptr++;
    		final int end2 = ptr + dx;
    		for (; ptr < end2; ptr++) {
    			ret_val += (int)doubleBuffer[ptr]*scaling;
    		}
    		ret_val += r_x1_i * (int)doubleBuffer[ptr];
    	}
    	// Last row
    	ret_val += D * (int)doubleBuffer[ptr];
    	ptr++;
    	final int end3 = ptr + dx;
    	for (; ptr < end3; ptr++) {
    		ret_val += r_y1_i * (int)doubleBuffer[ptr];
    	}
    	ret_val += C * (int)doubleBuffer[ptr];
    	
    	return (ret_val + scaling2/2)/scaling2;
    } // private int smoothedIntensity
    
    private boolean RoiPredicate(final double minX, final double minY, final double maxX, final double maxY,
    		                     final KeyPoint keyPt) {
        Point2d pt = keyPt.getPt();
        return (pt.x < minX) || (pt.x >= maxX) || (pt.y < minY) || (pt.y >= maxY);
    }
    
    // This is the subclass keypoint computation implementation
    private short[][] computeImpl(ModelImage image, Vector<KeyPoint>keypoints) {
    	int xDim = image.getExtents()[0];
    	int yDim = image.getExtents()[1];
    	int sliceSize = xDim * yDim;
    	double doubleBuffer[] = new double[sliceSize];
    	try {
    		image.exportData(0, sliceSize, doubleBuffer);
    	}
    	catch (IOException e) {
    		MipavUtil.displayError("IOException " + e + " on image.exportData(0, sliceSize, doubleBuffer) in computeImp1");
    		setCompleted(false);
    		return null;
    	}
        // Remove keypoints very close to the border
    	int ksize = keypoints.size();
    	// Remember the scale per keypoint
    	Vector<Integer>kscales = new Vector<Integer>();
    	kscales.setSize(ksize);
    	final double log2 = 0.693147180559945;
    	final double lb_scaleRange = Math.log(scaleRange)/log2;
    	//Iterator <KeyPoint> beginning = keypoints.iterator();
    	int beginning = 0;
    	//Iterator <Integer> beginningkscales = kscales.iterator();
    	int beginningkscales = 0;
    	final double basicSize06 = basicSize * 0.6;
    	int basicScale = 0;
    	if (!scaleInvariant) {
    	    basicScale = Math.max((int)(scales/lb_scaleRange*(Math.log(1.45*basicSize/basicSize06)/log2)+0.5), 0);
    	}
	    for (int k = 0; k < ksize; k++) {
	        int scale;
	        if (scaleInvariant) {
	        	scale = Math.max((int)(scales/lb_scaleRange*(Math.log(keypoints.get(k).getSize()/basicSize06)/log2)+0.5), 0);
	        	// Saturate
	        	if (scale >= scales) {
	        		scale = scales - 1;
	        	}
	        	kscales.set(k, Integer.valueOf(scale));
	        } // if (scaleInvariant)
	        else {
	        	scale = basicScale;
	        	kscales.set(k, Integer.valueOf(scale));
	        }
	        final int border = sizeList[scale];
	        final int border_x = xDim - border;
	        final int border_y = yDim - border;
	        if (RoiPredicate(border, border, border_x, border_y, keypoints.get(k))) {
	        	keypoints.remove(beginning+k);
	        	kscales.remove(beginningkscales + k);
	        	if (k == 0) {
	        		beginning = 0;
	        		beginningkscales = 0;
	        	}
	        	ksize--;
	        	k--;
	        } // if (RoiPredicate(border, border, border_x, border_y, keypoints.get(k)))
	    } // for (int k = 0; k < ksize; k++)
	    
	    // First, calculate the integral image over the whole image:
	    // Current integral image
	    int extents[] = new int[2];
	    extents[0] = xDim + 1;
	    extents[1] = yDim+1;
	    double integralBuffer[] = new double[extents[0]*extents[1]];
	    for (int y = 0; y < yDim+1; y++) {
	    	for (int x = 0; x < xDim + 1; x++) {
	    		for (int y2 = 0; y2 < y; y2++) {
	    			for (int x2 = 0; x2 < x; x2++) {
	    				integralBuffer[x + y*(xDim+1)] += doubleBuffer[x2 + y2*xDim];
	    			}
	    		}
	    	}
	    }
	    ModelImage integral = new ModelImage(ModelStorageBase.DOUBLE, extents, "integral");
	    try {
	    	integral.importData(0, integralBuffer, true);
	    }
	    catch (IOException e){
	    	MipavUtil.displayError("IOexception " + e + " on integral.importData(0, integralBuffer, true) in computeImpl");
	    	setCompleted(false);
	    	return null;
	    }
	    
	    // For temporary use
	    int values[] = new int[points];
	    
	    // Create the descriptors
	    // ksize is the number of descriptors
	    // strings is the number of shorts the descriptor consists of
	    short descriptors[][] = new short[ksize][strings];
	    
	    // Now do the extraction for all keypoints:
	    
	    // Temporary variables containing gray values at sample pointsZZ
	    int t1;
	    int t2;
	    
	    // The feature orientation
	    int direction0;
	    int direction1;
	    
	    // Points to the start of descriptors
	    int ptr = 0;
	    for (int k = 0; k < ksize; k++) {
	        int theta;
	        KeyPoint kp = keypoints.get(k);
	        final int scale = kscales.get(k);
	        int shifter = 0;
	        // Points to start of values
	        int pvalues = 0;
	        final double x = kp.getPt().x;
	        final double y = kp.getPt().y;
	        if (true /* kp.getAngle() == -1) */) {
	            if (!rotationInvariant) {
	            	// Don't compute the gradient direction, just assign a rotation of 0 degrees
	            	theta = 0;
	            }
	            else {
	            	// Get the gray values in the unrotated pattern
	            	for (int i = 0; i < points; i++) {
	            		values[pvalues++] = smoothedIntensity(image, integral, x, y, scale, 0 , i);
	            	}
	            	
	            	direction0 = 0;
	            	direction1 = 0;
	            	// Now iterate through the long pairings
	            	for (int iter = 0; iter <numLongPairs; ++iter) {
	            	    t1 = values[longPairs[iter].getI()];
	            	    t2 = values[longPairs[iter].getJ()];
	            	    final int delta_t = t1 - t2;
	            	    // Update the direction
	            	    final int tmp0 = delta_t*longPairs[iter].getWeighted_dx()/1024;
	            	    final int tmp1 = delta_t*longPairs[iter].getWeighted_dy()/1024;
	            	    direction0 += tmp0;
	            	    direction1 += tmp1;
	            	} // for (int iter = 0; iter <numLongPairs; ++iter)
	            	kp.setAngle(Math.atan2((double)direction1, (double)direction0)/Math.PI*180.0);
	            	theta = (int)((n_rot*kp.getAngle())/360.0 + 0.5);
	            	if (theta < 0) {
	            		theta += n_rot;
	            	}
	            	if (theta >= (int)n_rot) {
	            		theta -= n_rot;
	            	}
	            } // else
	        } // if (true /* kp.getAngle() == -1) */)
	        else { // Should never enter here after an if (true)
	        	// Figure out the direction
	        	if (!rotationInvariant) {
	        		theta = 0;
	        	}
	        	else {
	        		theta = (int)(n_rot * (kp.getAngle()/360.0) + 0.5);
	        		if (theta < 0) {
	        			theta += n_rot;
	        		}
	        		if (theta >= (int)n_rot) {
	        			theta -= n_rot;
	        		}
	        	}
	        } // else
	        // Now also extract the stuff for the actual direction:
	        // Let us compute the smoothed values
	        shifter = 0;
	        // pvalues is pointer for values
	        pvalues = 0;
	        // Get the gray values in the rotated pattern
	        for (int i = 0; i < points; i++) {
	        	values[pvalues++] = smoothedIntensity(image, integral, x, y, scale, theta, i);
	        }
	        // Now iterate through all the pairings
	        int ptr2 = ptr;
	        for (int iter = 0; iter < numShortPairs; ++iter) {
	            t1 = values[shortPairs[iter].getI()];
	            t2 = values[shortPairs[iter].getJ()];
	            if (t1 > t2) {
	            	descriptors[ptr/strings][ptr2-ptr] |= (1 << shifter);
	            } // else already initialized with zero
	            ++shifter;
	            if (shifter == 32) {
	            	shifter = 0;
	            	++ptr2;
	            }
	        } // for (int iter = 0; iter < numShortPairs; ++iter)
	        ptr += strings;
	    } // for (int k = 0; k < ksize; k++)
	    // Clean up
	    integral.disposeLocal();
	    integral = null;
	    values = null;
	    return descriptors;
    } // private void computeImp1
    
    private int descriptorSize() {
    	return strings;
    }
    
    private int descriptorType() {
    	return ModelStorageBase.SHORT;
    }
    
    public void detectImpl(ModelImage image, Vector<KeyPoint> keypoints, BitSet mask) {
    	int xDim = image.getExtents()[0];
    	int x;
    	int y;
    	int index;
    	
    	briskScaleSpace();
    	constructPyramid(image);
    	getKeypoints(keypoints);
    	// Remove invalid points
    	// Remove keypoints that are not in the mask
    	if (mask != null) {
	    	for (int i = keypoints.size()-1; i >= 0; i--) {
	    		KeyPoint kp = keypoints.get(i);
	    		x = (int)Math.round(kp.getPt().x);
	    		y = (int)Math.round(kp.getPt().y);
	    		index = x + y * xDim;
	    		if (!mask.get(index)) {
	    			keypoints.remove(i);
	    		}
	    	}
    	} // if (mask != null)
    }
	
	private void briskScaleSpace() {
    	if (octaves == 0) {
    		layers = 1;
    	}
    	else {
    		layers = 2 * octaves;
    	}
    }
    
    private void constructPyramid(ModelImage image) {
    
        // Set correct size
    	pyramid.clear();
    	
    	// Fill the pyramid
    	pyramid.add(new BriskLayer((ModelImage)image.clone())); 
    	if (layers > 1) {
    		pyramid.add(new BriskLayer(pyramid.lastElement(),TWOTHIRDSAMPLE));
    	}
    	
    	for (int i = 2; i < layers; i += 2) {
    		pyramid.add(new BriskLayer(pyramid.get(i-2), HALFSAMPLE));
    		pyramid.add(new BriskLayer(pyramid.get(i-1), HALFSAMPLE));
    	}
    }
    
    private void getKeypoints(Vector<KeyPoint> keypoints) {
        // Make sure keypoints is empty
    	keypoints.clear();
    	// Gives 2000 null keypoints before nonnull ones
    	//keypoints.setSize(2000);
    	
    	// Assign thresholds
    	safeThreshold = threshold * safetyFactor;
    	Vector<Vector<Point2d>> agastPoints = new Vector<Vector<Point2d>>();
    	for (int i = 0; i < layers; i++) {
    		agastPoints.addElement(new Vector<Point2d>());
    	}
    	
    	// Go through the octaves and intra layers and calculate 
    	// fast corner scores
    	for (int i = 0; i < layers; i++) {
    		// Call OAST16_9 without nms
    		BriskLayer l = pyramid.get(i);
    		l.getAgastPoints(safeThreshold, agastPoints.get(i));
    	} 
    	
    	if (layers == 1) {
    	  // Just do a simple 2D subpixel refinement
    		final int num = agastPoints.get(0).size();
    		for (int n = 0; n < num; n++) {
    			final Point2d point = agastPoints.get(0).get(n);
    			// First check if it is a maximum
    			if (!pyramid.get(0).isMax2D((int)Math.round(point.x), (int)Math.round(point.y))) {
    				continue;
    			}
    			
    			// Let's do the subpixel and double scale refinement
    			BriskLayer l = pyramid.get(0);
    			int s_0_0 = (int)Math.round(l.getAgastScore((int)Math.round(point.x)-1,(int)Math.round(point.y)-1, 1.0));
    			int s_1_0 = (int)Math.round(l.getAgastScore((int)Math.round(point.x), (int)Math.round(point.y)-1, 1.0));
    			int s_2_0 = (int)Math.round(l.getAgastScore((int)Math.round(point.x)+1, (int)Math.round(point.y)-1, 1.0));
    			int s_2_1 = (int)Math.round(l.getAgastScore((int)Math.round(point.x)+1, (int)Math.round(point.y), 1.0));
    			int s_1_1 = (int)Math.round(l.getAgastScore((int)Math.round(point.x), (int)Math.round(point.y), 1.0));
    			int s_0_1 = (int)Math.round(l.getAgastScore((int)Math.round(point.x)-1, (int)Math.round(point.y), 1.0));
    			int s_0_2 = (int)Math.round(l.getAgastScore((int)Math.round(point.x)-1, (int)Math.round(point.y)+1, 1.0));
    			int s_1_2 = (int)Math.round(l.getAgastScore((int)Math.round(point.x), (int)Math.round(point.y)+1, 1.0));
    			int s_2_2 = (int)Math.round(l.getAgastScore((int)Math.round(point.x)+1, (int)Math.round(point.y)+1, 1.0));
    			double delta_x[] = new double[1];
    			double delta_y[] = new double[1];
    			double max = subpixel2D(s_0_0, s_0_1, s_0_2, s_1_0, s_1_1, s_1_2, s_2_0, s_2_1, s_2_2, delta_x, delta_y);
    			// Store
    			keypoints.add(new KeyPoint(point.x + delta_x[0], point.y + delta_y[0], basicSize, -1, max, 0));
    		} // for (int n = 0; n < num; n++)
    		return;
    	} // if (layers == 1)
    	
    	double x[] = new double[1];
    	double y[] = new double[1];
    	double scale[] = new double[1];
    	double score;
    	for (int i = 0; i < layers; i++) {
    	    BriskLayer l = pyramid.get(i);
    	    final int num = agastPoints.get(i).size();
    	    if (i == layers - 1) {
    	    	for (int n = 0; n < num; n++) {
    	    		final Point2d point = agastPoints.get(i).get(n);
        			// Consider only 2D maxima
        			if (!pyramid.get(i).isMax2D((int)Math.round(point.x), (int)Math.round(point.y))) {
        				continue;
        			}
        			boolean ismax[] = new boolean[1];
        			double dx[] = new double[1];
        			double dy[] = new double[1];
        			getScoreMaxBelow(i, (int)Math.round(point.x), (int)Math.round(point.y),
        					l.getAgastScore((int)Math.round(point.x), (int)Math.round(point.y), safeThreshold),
        					ismax, dx, dy);
        			if (!ismax[0]) {
        				continue;
        			}
        			
        			// Get the patch on this layer
        			int s_0_0 = (int)Math.round(l.getAgastScore((int)Math.round(point.x)-1,(int)Math.round(point.y)-1, 1.0));
        			int s_1_0 = (int)Math.round(l.getAgastScore((int)Math.round(point.x), (int)Math.round(point.y)-1, 1.0));
        			int s_2_0 = (int)Math.round(l.getAgastScore((int)Math.round(point.x)+1, (int)Math.round(point.y)-1, 1.0));
        			int s_2_1 = (int)Math.round(l.getAgastScore((int)Math.round(point.x)+1, (int)Math.round(point.y), 1.0));
        			int s_1_1 = (int)Math.round(l.getAgastScore((int)Math.round(point.x), (int)Math.round(point.y), 1.0));
        			int s_0_1 = (int)Math.round(l.getAgastScore((int)Math.round(point.x)-1, (int)Math.round(point.y), 1.0));
        			int s_0_2 = (int)Math.round(l.getAgastScore((int)Math.round(point.x)-1, (int)Math.round(point.y)+1, 1.0));
        			int s_1_2 = (int)Math.round(l.getAgastScore((int)Math.round(point.x), (int)Math.round(point.y)+1, 1.0));
        			int s_2_2 = (int)Math.round(l.getAgastScore((int)Math.round(point.x)+1, (int)Math.round(point.y)+1, 1.0));
        			double delta_x[] = new double[1];
        			double delta_y[] = new double[1];
        			double max = subpixel2D(s_0_0, s_0_1, s_0_2, s_1_0, s_1_1, s_1_2, s_2_0, s_2_1, s_2_2, delta_x, delta_y);
        			// Store
        			keypoints.add(new KeyPoint((point.x + delta_x[0])*l.getScale() + l.getOffset(), 
        					(point.y + delta_y[0])*l.getScale() + l.getOffset(), basicSize*l.getScale(), -1, max, i));
    	    	} // for (int n = 0; n < num; n++)
    	    } // if (i == layers - 1)
    	    else { // not the last layer
    	    	for (int n = 0; n < num; n++) {
    	    	    final Point2d point	= agastPoints.get(i).get(n);
    	    	    
    	    	    // First check if it is a maximum
    	    	    if (!pyramid.get(i).isMax2D((int)Math.round(point.x), (int)Math.round(point.y))) {
        				continue;
        			}
    	    	    
    	    	    // Let's do subpixel and double scale refinement
    	    	    boolean ismax[] = new boolean[1];
    	    	    score = refine3D(i, (int)Math.round(point.x), (int)Math.round(point.y), x, y, scale, ismax);
    	    	    if (!ismax[0]) {
    	    	    	continue;
    	    	    }
    	    	    
    	    	    // Finally store the detected keypoint
    	    	    if (score > threshold) {
    	    	    	keypoints.add(new KeyPoint(x[0], y[0], basicSize * scale[0], -1 , score, i));
    	    	    }
    	    	} // for (int n = 0; n < num; n++)
    	    } // else not the last layer
    	} // for (int i = 0; i < layers; i++)
    } // getKeyPoints(Vector <KeyPoint) keypoints)
    
 // 3D maximum refinement centered around (x_layer,y_layer)
 private double refine3D(final int layer, final int x_layer, final int y_layer,
    		double x[], double y[], double scale[], boolean ismax[]){
    	ismax[0]=true;
    	BriskLayer thisLayer=pyramid.get(layer);
    	final double center = thisLayer.getAgastScore(x_layer,y_layer,1);

    	// check and get above maximum:
    	double delta_x_above[] = new double[1];
    	double delta_y_above[] = new double[1];
 
    	double max_above = getScoreMaxAbove(layer,x_layer, y_layer,
    					center, ismax,
    					delta_x_above, delta_y_above);

    	if(!ismax[0]) return 0.0;

    	double max[] = new double[1]; // to be returned

    	if(layer%2==0){ // on octave
    		// treat the patch below:
    		double delta_x_below[] = new double[1];
    		double delta_y_below[] = new double[1];
    		double max_below_float;
    		int max_below_uchar=0;
    		if(layer==0){
    			// guess the lower intra octave...
    			BriskLayer l=pyramid.get(0);
    			int s_0_0 = (int)Math.round(l.getAgastScore_5_8(x_layer-1, y_layer-1, 1));
    			max_below_uchar=s_0_0;
    			int s_1_0 = (int)Math.round(l.getAgastScore_5_8(x_layer,   y_layer-1, 1));
    			if(s_1_0>max_below_uchar) max_below_uchar=s_1_0;
    			int s_2_0 = (int)Math.round(l.getAgastScore_5_8(x_layer+1, y_layer-1, 1));
    			if(s_2_0>max_below_uchar) max_below_uchar=s_2_0;
    			int s_2_1 = (int)Math.round(l.getAgastScore_5_8(x_layer+1, y_layer,   1));
    			if(s_2_1>max_below_uchar) max_below_uchar=s_2_1;
    			int s_1_1 = (int)Math.round(l.getAgastScore_5_8(x_layer,   y_layer,   1));
    			if(s_1_1>max_below_uchar) max_below_uchar=s_1_1;
    			int s_0_1 = (int)Math.round(l.getAgastScore_5_8(x_layer-1, y_layer,   1));
    			if(s_0_1>max_below_uchar) max_below_uchar=s_0_1;
    			int s_0_2 = (int)Math.round(l.getAgastScore_5_8(x_layer-1, y_layer+1, 1));
    			if(s_0_2>max_below_uchar) max_below_uchar=s_0_2;
    			int s_1_2 = (int)Math.round(l.getAgastScore_5_8(x_layer,   y_layer+1, 1));
    			if(s_1_2>max_below_uchar) max_below_uchar=s_1_2;
    			int s_2_2 = (int)Math.round(l.getAgastScore_5_8(x_layer+1, y_layer+1, 1));
    			if(s_2_2>max_below_uchar) max_below_uchar=s_2_2;

    			max_below_float = subpixel2D(s_0_0, s_0_1, s_0_2,
    							s_1_0, s_1_1, s_1_2,
    							s_2_0, s_2_1, s_2_2,
    							delta_x_below, delta_y_below);
    			max_below_float = max_below_uchar;
    		}
    		else{
    			max_below_float = getScoreMaxBelow(layer,x_layer, y_layer,
    								center, ismax,
    								delta_x_below, delta_y_below);
    			if(!ismax[0]) return 0;
    		}

    		// get the patch on this layer:
    		int s_0_0 = (int)Math.round(thisLayer.getAgastScore(x_layer-1, y_layer-1,1));
    		int s_1_0 = (int)Math.round(thisLayer.getAgastScore(x_layer,   y_layer-1,1));
    		int s_2_0 = (int)Math.round(thisLayer.getAgastScore(x_layer+1, y_layer-1,1));
    		int s_2_1 = (int)Math.round(thisLayer.getAgastScore(x_layer+1, y_layer,1));
    		int s_1_1 = (int)Math.round(thisLayer.getAgastScore(x_layer,   y_layer,1));
    		int s_0_1 = (int)Math.round(thisLayer.getAgastScore(x_layer-1, y_layer,1));
    		int s_0_2 = (int)Math.round(thisLayer.getAgastScore(x_layer-1, y_layer+1,1));
    		int s_1_2 = (int)Math.round(thisLayer.getAgastScore(x_layer,   y_layer+1,1));
    		int s_2_2 = (int)Math.round(thisLayer.getAgastScore(x_layer+1, y_layer+1,1));
    		double delta_x_layer[] = new double[1];
    		double delta_y_layer[] = new double[1];
    		double max_layer = subpixel2D(s_0_0, s_0_1, s_0_2,
    				s_1_0, s_1_1, s_1_2,
    				s_2_0, s_2_1, s_2_2,
    				delta_x_layer, delta_y_layer);

    		// calculate the relative scale (1D maximum):
    		if(layer==0){
    			scale[0]=refine1D_2(max_below_float,
    					Math.max(center,max_layer),
    					max_above,max);
    		}
    		else
    			scale[0]=refine1D(max_below_float,
    					Math.max(center,max_layer),
    					max_above,max);

    		if(scale[0]>1.0){
    			// interpolate the position:
    			final double r0=(1.5-scale[0])/.5;
    			final double r1=1.0-r0;
    			x[0]=(r0*delta_x_layer[0]+r1*delta_x_above[0]+(double)(x_layer))
    					*thisLayer.getScale()+thisLayer.getOffset();
    			y[0]=(r0*delta_y_layer[0]+r1*delta_y_above[0]+(double)(y_layer))
    					*thisLayer.getScale()+thisLayer.getOffset();
    		}
    		else{
    			if(layer==0){
    				// interpolate the position:
    				final double r0=(scale[0]-0.5)/0.5;
    				final double r_1=1.0-r0;
    				x[0]=r0*delta_x_layer[0]+r_1*delta_x_below[0]+(double)(x_layer);
    				y[0]=r0*delta_y_layer[0]+r_1*delta_y_below[0]+(double)(y_layer);
    			}
    			else{
    				// interpolate the position:
    				final double r0=(scale[0]-0.75)/0.25;
    				final double r_1=1.0-r0;
    				x[0]=(r0*delta_x_layer[0]+r_1*delta_x_below[0]+(double)(x_layer))
    						*thisLayer.getScale()+thisLayer.getOffset();
    				y[0]=(r0*delta_y_layer[0]+r_1*delta_y_below[0]+(double)(y_layer))
    						*thisLayer.getScale()+thisLayer.getOffset();
    			}
    		}
    	}
    	else{
    		// on intra
    		// check the patch below:
    		double delta_x_below[] = new double[1];
    		double delta_y_below[] = new double[1];
    		double max_below = getScoreMaxBelow(layer,x_layer, y_layer,
    					center, ismax,
    					delta_x_below, delta_y_below);
    		if(!ismax[0]) return 0.0;

    		// get the patch on this layer:
    		int s_0_0 = (int)Math.round(thisLayer.getAgastScore(x_layer-1, y_layer-1,1));
    		int s_1_0 = (int)Math.round(thisLayer.getAgastScore(x_layer,   y_layer-1,1));
    		int s_2_0 = (int)Math.round(thisLayer.getAgastScore(x_layer+1, y_layer-1,1));
    		int s_2_1 = (int)Math.round(thisLayer.getAgastScore(x_layer+1, y_layer,1));
    		int s_1_1 = (int)Math.round(thisLayer.getAgastScore(x_layer,   y_layer,1));
    		int s_0_1 = (int)Math.round(thisLayer.getAgastScore(x_layer-1, y_layer,1));
    		int s_0_2 = (int)Math.round(thisLayer.getAgastScore(x_layer-1, y_layer+1,1));
    		int s_1_2 = (int)Math.round(thisLayer.getAgastScore(x_layer,   y_layer+1,1));
    		int s_2_2 = (int)Math.round(thisLayer.getAgastScore(x_layer+1, y_layer+1,1));
    		double delta_x_layer[] = new double[1];
    		double delta_y_layer[] = new double[1];
    		double max_layer = subpixel2D(s_0_0, s_0_1, s_0_2,
    				s_1_0, s_1_1, s_1_2,
    				s_2_0, s_2_1, s_2_2,
    				delta_x_layer, delta_y_layer);

    		// calculate the relative scale (1D maximum):
    		scale[0]=refine1D_1(max_below,
    				Math.max(center,max_layer),
    				max_above,max);
    		if(scale[0]>1.0){
    			// interpolate the position:
    			final double r0=4.0-scale[0]*3.0;
    			final double r1=1.0-r0;
    			x[0]=(r0*delta_x_layer[0]+r1*delta_x_above[0]+(double)(x_layer))
    					*thisLayer.getScale()+thisLayer.getOffset();
    			y[0]=(r0*delta_y_layer[0]+r1*delta_y_above[0]+(double)(y_layer))
    					*thisLayer.getScale()+thisLayer.getOffset();
    		}
    		else{
    			// interpolate the position:
    			final double r0=scale[0]*3.0-2.0;
    			final double r_1=1.0-r0;
    			x[0]=(r0*delta_x_layer[0]+r_1*delta_x_below[0]+(double)(x_layer))
    					*thisLayer.getScale()+thisLayer.getOffset();
    			y[0]=(r0*delta_y_layer[0]+r_1*delta_y_below[0]+(double)(y_layer))
    					*thisLayer.getScale()+thisLayer.getOffset();
    		}
    	}

    	// calculate the absolute scale:
    	scale[0]*=thisLayer.getScale();

    	// that's it, return the refined maximum:
    	return max[0];
    }

    
    
    private double refine1D(final double s_05,
			final double s0, final double s05, double max[]){
int i_05=(int)(1024.0*s_05+0.5);
int i0=(int)(1024.0*s0+0.5);
int i05=(int)(1024.0*s05+0.5);

//   16.0000  -24.0000    8.0000
//  -40.0000   54.0000  -14.0000
//   24.0000  -27.0000    6.0000

int three_a=16*i_05-24*i0+8*i05;
// second derivative must be negative:
if(three_a>=0){
	if(s0>=s_05 && s0>=s05){
		max[0]=s0;
		return 1.0;
	}
	if(s_05>=s0 && s_05>=s05){
		max[0]=s_05;
		return 0.75;
	}
	if(s05>=s0 && s05>=s_05){
		max[0]=s05;
		return 1.5;
	}
}

int three_b=-40*i_05+54*i0-14*i05;
// calculate max location:
double ret_val=-(double)(three_b)/(double)(2*three_a);
// saturate and return
if(ret_val<0.75) ret_val= 0.75;
else if(ret_val>1.5) ret_val= 1.5; // allow to be slightly off bounds ...?
int three_c = +24*i_05  -27*i0    +6*i05;
max[0]=(double)(three_c)+(double)(three_a)*ret_val*ret_val+(double)(three_b)*ret_val;
max[0]/=3072.0;
return ret_val;
}

private double refine1D_1(final double s_05,
			final double s0, final double s05, double max[]){
int i_05=(int)(1024.0*s_05+0.5);
int i0=(int)(1024.0*s0+0.5);
int i05=(int)(1024.0*s05+0.5);

//  4.5000   -9.0000    4.5000
//-10.5000   18.0000   -7.5000
//  6.0000   -8.0000    3.0000

int two_a=9*i_05-18*i0+9*i05;
// second derivative must be negative:
if(two_a>=0){
	if(s0>=s_05 && s0>=s05){
		max[0]=s0;
		return 1.0;
	}
	if(s_05>=s0 && s_05>=s05){
		max[0]=s_05;
		return 0.6666666666666666666666666667;
	}
	if(s05>=s0 && s05>=s_05){
		max[0]=s05;
		return 1.3333333333333333333333333333;
	}
}

int two_b=-21*i_05+36*i0-15*i05;
// calculate max location:
double ret_val=-(double)(two_b)/(double)(2*two_a);
// saturate and return
if(ret_val<0.6666666666666666666666666667) ret_val= 0.666666666666666666666666667;
else if(ret_val>1.33333333333333333333333333) ret_val= 1.333333333333333333333333333;
int two_c = +12*i_05  -16*i0    +6*i05;
max[0]=(double)(two_c)+(double)(two_a)*ret_val*ret_val+(double)(two_b)*ret_val;
max[0]/=2048.0;
return ret_val;
}

private double refine1D_2(final double s_05,
			final double s0, final double s05, double[] max){
int i_05=(int)(1024.0*s_05+0.5);
int i0=(int)(1024.0*s0+0.5);
int i05=(int)(1024.0*s05+0.5);

//   18.0000  -30.0000   12.0000
//  -45.0000   65.0000  -20.0000
//   27.0000  -30.0000    8.0000

int a=2*i_05-4*i0+2*i05;
// second derivative must be negative:
if(a>=0){
	if(s0>=s_05 && s0>=s05){
		max[0]=s0;
		return 1.0;
	}
	if(s_05>=s0 && s_05>=s05){
		max[0]=s_05;
		return 0.7;
	}
	if(s05>=s0 && s05>=s_05){
		max[0]=s05;
		return 1.5;
	}
}

int b=-5*i_05+8*i0-3*i05;
// calculate max location:
double ret_val=-(double)(b)/(double)(2*a);
// saturate and return
if(ret_val<0.7) ret_val= 0.7;
else if(ret_val>1.5) ret_val= 1.5; // allow to be slightly off bounds ...?
int c = +3*i_05  -3*i0    +1*i05;
max[0]=(double)(c)+(double)(a)*ret_val*ret_val+(double)(b)*ret_val;
max[0]/=1024;
return ret_val;
}


    
    private double getScoreMaxBelow(final int layer,
    		final int x_layer, final int y_layer,
    		final double threshold, boolean[] ismax,
    		double[] dx, double[] dy){
    	ismax[0]=false;

    	// relevant floating point coordinates
    	double x_1;
    	double x1;
    	double y_1;
    	double y1;

    	if(layer%2==0){
    		// octave
    		x_1=(double)(8*(x_layer)+1-4)/6.0;
    		x1=(double)(8*(x_layer)+1+4)/6.0;
    		y_1=(double)(8*(y_layer)+1-4)/6.0;
    		y1=(double)(8*(y_layer)+1+4)/6.0;
    	}
    	else{
    		x_1=(double)(6*(x_layer)+1-3)/4.0;
    		x1=(double)(6*(x_layer)+1+3)/4.0;
    		y_1=(double)(6*(y_layer)+1-3)/4.0;
    		y1=(double)(6*(y_layer)+1+3)/4.0;
    	}

    	// the layer below
    	BriskLayer layerBelow=pyramid.get(layer-1);

    	// check the first row
    	int max_x = (int)x_1+1;
    	int max_y = (int)y_1+1;
    	double tmp_max;
    	double max=layerBelow.getAgastScore(x_1,y_1,1);
    	if(max>threshold) return 0;
    	for(int x=(int)x_1+1; x<=(int)(x1); x++){
    		tmp_max=layerBelow.getAgastScore((double)(x),y_1,1);
    		if(tmp_max>threshold) return 0;
    		if(tmp_max>max){
    			max=tmp_max;
    			max_x = x;
    		}
    	}
    	tmp_max=layerBelow.getAgastScore(x1,y_1,1);
    	if(tmp_max>threshold) return 0;
    	if(tmp_max>max){
    		max=tmp_max;
    		max_x = (int)(x1);
    	}

    	// middle rows
    	for(int y=(int)y_1+1; y<=(int)(y1); y++){
    		tmp_max=layerBelow.getAgastScore(x_1,(double)(y),1);
    		if(tmp_max>threshold) return 0;
    		if(tmp_max>max){
    			max=tmp_max;
    			max_x = (int)(x_1+1);
    			max_y = y;
    		}
    		for(int x=(int)x_1+1; x<=(int)(x1); x++){
    			tmp_max=layerBelow.getAgastScore(x,y,1);
    			if(tmp_max>threshold) return 0;
    			if(tmp_max==max){
    				final double t1=2*(
    						layerBelow.getAgastScore(x-1,y,1)
    						+layerBelow.getAgastScore(x+1,y,1)
    						+layerBelow.getAgastScore(x,y+1,1)
    						+layerBelow.getAgastScore(x,y-1,1))
    						+(layerBelow.getAgastScore(x+1,y+1,1)
    						+layerBelow.getAgastScore(x-1,y+1,1)
    						+layerBelow.getAgastScore(x+1,y-1,1)
    						+layerBelow.getAgastScore(x-1,y-1,1));
    				final double t2=2*(
    						layerBelow.getAgastScore(max_x-1,max_y,1)
    						+layerBelow.getAgastScore(max_x+1,max_y,1)
    						+layerBelow.getAgastScore(max_x,max_y+1,1)
    						+layerBelow.getAgastScore(max_x,max_y-1,1))
    						+(layerBelow.getAgastScore(max_x+1,max_y+1,1)
    						+layerBelow.getAgastScore(max_x-1,max_y+1,1)
    						+layerBelow.getAgastScore(max_x+1,max_y-1,1)
    						+layerBelow.getAgastScore(max_x-1,max_y-1,1));
    				if(t1>t2){
    					max_x = x;
    					max_y = y;
    				}
    			}
    			if(tmp_max>max){
    				max=tmp_max;
    				max_x = x;
    				max_y = y;
    			}
    		}
    		tmp_max=layerBelow.getAgastScore(x1,(double)(y),1);
    		if(tmp_max>threshold) return 0;
    		if(tmp_max>max){
    			max=tmp_max;
    			max_x = (int)(x1);
    			max_y = y;
    		}
    	}

    	// bottom row
    	tmp_max=layerBelow.getAgastScore(x_1,y1,1);
    	if(tmp_max>max){
    		max=tmp_max;
    		max_x = (int)(x_1+1);
    		max_y = (int)(y1);
    	}
    	for(int x=(int)x_1+1; x<=(int)(x1); x++){
    		tmp_max=layerBelow.getAgastScore((double)(x),y1,1);
    		if(tmp_max>max){
    			max=tmp_max;
    			max_x = x;
    			max_y = (int)(y1);
    		}
    	}
    	tmp_max=layerBelow.getAgastScore(x1,y1,1);
    	if(tmp_max>max){
    		max=tmp_max;
    		max_x = (int)(x1);
    		max_y = (int)(y1);
    	}

    	//find dx/dy:
    	int s_0_0 = (int)Math.round(layerBelow.getAgastScore(max_x-1, max_y-1,1));
    	int s_1_0 = (int)Math.round(layerBelow.getAgastScore(max_x,   max_y-1,1));
    	int s_2_0 = (int)Math.round(layerBelow.getAgastScore(max_x+1, max_y-1,1));
    	int s_2_1 = (int)Math.round(layerBelow.getAgastScore(max_x+1, max_y,1));
    	int s_1_1 = (int)Math.round(layerBelow.getAgastScore(max_x,   max_y,1));
    	int s_0_1 = (int)Math.round(layerBelow.getAgastScore(max_x-1, max_y,1));
    	int s_0_2 = (int)Math.round(layerBelow.getAgastScore(max_x-1, max_y+1,1));
    	int s_1_2 = (int)Math.round(layerBelow.getAgastScore(max_x,   max_y+1,1));
    	int s_2_2 = (int)Math.round(layerBelow.getAgastScore(max_x+1, max_y+1,1));
    	double dx_1[] = new double[1];
    	double dy_1[] = new double[1];
    	double refined_max=subpixel2D(s_0_0, s_0_1,  s_0_2,
    			s_1_0, s_1_1, s_1_2,
    			s_2_0, s_2_1, s_2_2,
    			dx_1, dy_1);

    	// calculate dx/dy in above coordinates
    	double real_x = (double)(max_x)+dx_1[0];
    	double real_y = (double)(max_y)+dy_1[0];
    	boolean returnrefined=true;
    	if(layer%2==0){
    		dx[0]=(real_x*6.0+1.0)/8.0-(double)(x_layer);
    		dy[0]=(real_y*6.0+1.0)/8.0-(double)(y_layer);
    	}
    	else{
    		dx[0]=(real_x*4.0-1.0)/6.0-(double)(x_layer);
    		dy[0]=(real_y*4.0-1.0)/6.0-(double)(y_layer);
    	}

    	// saturate
    	if(dx[0]>1.0) {dx[0]=1.0;returnrefined=false;}
    	if(dx[0]<-1.0) {dx[0]=-1.0;returnrefined=false;}
    	if(dy[0]>1.0) {dy[0]=1.0;returnrefined=false;}
    	if(dy[0]<-1.0) {dy[0]=-1.0;returnrefined=false;}

    	// done and ok.
    	ismax[0]=true;
    	if(returnrefined){
    		return Math.max(refined_max,max);
    	}
    	return max;
    }

    
 // return the maximum of score patches above or below
 private double getScoreMaxAbove(final int layer,
    		final int x_layer, final int y_layer,
    		final double threshold, boolean ismax[],
    		double dx[], double dy[]){

    	ismax[0]=false;
    	// relevant floating point coordinates
    	double x_1;
    	double x1;
    	double y_1;
    	double y1;

    	// the layer above
    	//assert(layer+1<layers_);
    	BriskLayer layerAbove=pyramid.get(layer+1);

    	if(layer%2==0) {
    		// octave
    		x_1=(double)(4*(x_layer)-1-2)/6.0;
    		x1=(double)(4*(x_layer)-1+2)/6.0;
    		y_1=(double)(4*(y_layer)-1-2)/6.0;
    		y1=(double)(4*(y_layer)-1+2)/6.0;
    	}
    	else{
    		// intra
    		x_1=(double)(6*(x_layer)-1-3)/8.0;
    		x1=(double)(6*(x_layer)-1+3)/8.0;
    		y_1=(double)(6*(y_layer)-1-3)/8.0;
    		y1=(double)(6*(y_layer)-1+3)/8.0;
    	}


    	// check the first row
    	int max_x = (int)x_1+1;
    	int max_y = (int)y_1+1;
    	double tmp_max;
    	double max=layerAbove.getAgastScore(x_1, y_1,1.0);
    	if(max>threshold) return 0;
    	for(int x=(int)x_1+1; x<=(int)(x1); x++){
    		tmp_max=layerAbove.getAgastScore((double)(x),y_1,1);
    		if(tmp_max>threshold) return 0;
    		if(tmp_max>max){
    			max=tmp_max;
    			max_x = x;
    		}
    	}
    	tmp_max=layerAbove.getAgastScore(x1,y_1,1);
    	if(tmp_max>threshold) return 0;
    	if(tmp_max>max){
    		max=tmp_max;
    		max_x = (int)(x1);
    	}

    	// middle rows
    	for(int y=(int)y_1+1; y<=(int)(y1); y++){
    		tmp_max=layerAbove.getAgastScore(x_1,(double)(y),1);
    		if(tmp_max>threshold) return 0;
    		if(tmp_max>max){
    			max=tmp_max;
    			max_x = (int)(x_1+1);
    			max_y = y;
    		}
    		for(int x=(int)x_1+1; x<=(int)(x1); x++){
    			tmp_max=layerAbove.getAgastScore(x,y,1);
    			if(tmp_max>threshold) return 0;
    			if(tmp_max>max){
    				max=tmp_max;
    				max_x = x;
    				max_y = y;
    			}
    		}
    		tmp_max=layerAbove.getAgastScore(x1,(double)(y),1);
    		if(tmp_max>threshold) return 0;
    		if(tmp_max>max){
    			max=tmp_max;
    			max_x = (int)(x1);
    			max_y = y;
    		}
    	}

    	// bottom row
    	tmp_max=layerAbove.getAgastScore(x_1,y1,1);
    	if(tmp_max>max){
    		max=tmp_max;
    		max_x = (int)(x_1+1);
    		max_y = (int)(y1);
    	}
    	for(int x=(int)x_1+1; x<=(int)(x1); x++){
    		tmp_max=layerAbove.getAgastScore((double)(x),y1,1);
    		if(tmp_max>max){
    			max=tmp_max;
    			max_x = x;
    			max_y = (int)(y1);
    		}
    	}
    	tmp_max=layerAbove.getAgastScore(x1,y1,1);
    	if(tmp_max>max){
    		max=tmp_max;
    		max_x = (int)(x1);
    		max_y = (int)(y1);
    	}

    	//find dx/dy:
    	int s_0_0 = (int)Math.round(layerAbove.getAgastScore(max_x-1, max_y-1,1));
    	int s_1_0 = (int)Math.round(layerAbove.getAgastScore(max_x,   max_y-1,1));
    	int s_2_0 = (int)Math.round(layerAbove.getAgastScore(max_x+1, max_y-1,1));
    	int s_2_1 = (int)Math.round(layerAbove.getAgastScore(max_x+1, max_y,1));
    	int s_1_1 = (int)Math.round(layerAbove.getAgastScore(max_x,   max_y,1));
    	int s_0_1 = (int)Math.round(layerAbove.getAgastScore(max_x-1, max_y,1));
    	int s_0_2 = (int)Math.round(layerAbove.getAgastScore(max_x-1, max_y+1,1));
    	int s_1_2 = (int)Math.round(layerAbove.getAgastScore(max_x,   max_y+1,1));
    	int s_2_2 = (int)Math.round(layerAbove.getAgastScore(max_x+1, max_y+1,1));
    	double dx_1[] = new double[1];
    	double dy_1[] = new double[1];
    	double refined_max=subpixel2D(s_0_0, s_0_1,  s_0_2,
    			s_1_0, s_1_1, s_1_2,
    			s_2_0, s_2_1, s_2_2,
    			dx_1, dy_1);

    	// calculate dx/dy in above coordinates
    	double real_x = (double)(max_x)+dx_1[0];
    	double real_y = (double)(max_y)+dy_1[0];
    	boolean returnrefined=true;
    	if(layer%2==0){
    		dx[0]=(real_x*6.0+1.0)/4.0-(double)(x_layer);
    		dy[0]=(real_y*6.0+1.0)/4.0-(double)(y_layer);
    	}
    	else{
    		dx[0]=(real_x*8.0+1.0)/6.0-(double)(x_layer);
    		dy[0]=(real_y*8.0+1.0)/6.0-(double)(y_layer);
    	}

    	// saturate
    	if(dx[0]>1.0) {dx[0]=1.0;returnrefined=false;}
    	if(dx[0]<-1.0) {dx[0]=-1.0;returnrefined=false;}
    	if(dy[0]>1.0) {dy[0]=1.0;returnrefined=false;}
    	if(dy[0]<-1.0) {dy[0]=-1.0;returnrefined=false;}

    	// done and ok.
    	ismax[0]=true;
    	if(returnrefined){
    		return Math.max(refined_max,max);
    	}
    	return max;
    }

    
    private double subpixel2D(final int s_0_0, final int s_0_1, final int s_0_2,
			final int s_1_0, final int s_1_1, final int s_1_2,
			final int s_2_0, final int s_2_1, final int s_2_2,
			double[] delta_x, double[] delta_y){

		// the coefficients of the 2d quadratic function least-squares fit:
		int tmp1 =        s_0_0 + s_0_2 - 2*s_1_1 + s_2_0 + s_2_2;
		int coeff1 = 3*(tmp1 + s_0_1 - ((s_1_0 + s_1_2)<<1) + s_2_1);
		int coeff2 = 3*(tmp1 - ((s_0_1+ s_2_1)<<1) + s_1_0 + s_1_2 );
		int tmp2 =                                  s_0_2 - s_2_0;
		int tmp3 =                         (s_0_0 + tmp2 - s_2_2);
		int tmp4 =                                   tmp3 -2*tmp2;
		int coeff3 =                    -3*(tmp3 + s_0_1 - s_2_1);
		int coeff4 =                    -3*(tmp4 + s_1_0 - s_1_2);
		int coeff5 =            (s_0_0 - s_0_2 - s_2_0 + s_2_2)<<2;
		int coeff6 = -(s_0_0  + s_0_2 - ((s_1_0 + s_0_1 + s_1_2 + s_2_1)<<1) - 5*s_1_1  + s_2_0  + s_2_2)<<1;
		
		
		// 2nd derivative test:
		int H_det=4*coeff1*coeff2 - coeff5*coeff5;
		
		if(H_det==0){
		delta_x[0]=0.0;
		delta_y[0]=0.0;
		return (double)(coeff6)/18.0;
		}
		
		if(!(H_det>0&&coeff1<0)){
		// The maximum must be at the one of the 4 patch corners.
		int tmp_max=coeff3+coeff4+coeff5;
		delta_x[0]=1.0; delta_y[0]=1.0;
		
		int tmp = -coeff3+coeff4-coeff5;
		if(tmp>tmp_max){
		tmp_max=tmp;
		delta_x[0]=-1.0; delta_y[0]=1.0;
		}
		tmp = coeff3-coeff4-coeff5;
		if(tmp>tmp_max){
		tmp_max=tmp;
		delta_x[0]=1.0; delta_y[0]=-1.0;
		}
		tmp = -coeff3-coeff4+coeff5;
		if(tmp>tmp_max){
		tmp_max=tmp;
		delta_x[0]=-1.0; delta_y[0]=-1.0;
		}
		return (double)(tmp_max+coeff1+coeff2+coeff6)/18.0;
		}
		
		// this is hopefully the normal outcome of the Hessian test
		delta_x[0]=(double)(2*coeff2*coeff3 - coeff4*coeff5)/(double)(-H_det);
		delta_y[0]=(double)(2*coeff1*coeff4 - coeff3*coeff5)/(double)(-H_det);
		// TODO: this is not correct, but easy, so perform a real boundary maximum search:
		boolean tx=false; boolean tx_=false; boolean ty=false; boolean ty_=false;
		if(delta_x[0]>1.0) tx=true;
		else if(delta_x[0]<-1.0) tx_=true;
		if(delta_y[0]>1.0) ty=true;
		if(delta_y[0]<-1.0) ty_=true;
		
		if(tx||tx_||ty||ty_){
		// get two candidates:
		double delta_x1=0.0, delta_x2=0.0, delta_y1=0.0, delta_y2=0.0;
		if(tx) {
		delta_x1=1.0;
		delta_y1=-(double)(coeff4+coeff5)/(double)(2*coeff2);
		if(delta_y1>1.0) delta_y1=1.0; else if (delta_y1<-1.0) delta_y1=-1.0;
		}
		else if(tx_) {
		delta_x1=-1.0;
		delta_y1=-(double)(coeff4-coeff5)/(double)(2*coeff2);
		if(delta_y1>1.0) delta_y1=1.0; else if (delta_y1<-1.0) delta_y1=-1.0;
		}
		if(ty) {
		delta_y2=1.0;
		delta_x2=-(double)(coeff3+coeff5)/(double)(2*coeff1);
		if(delta_x2>1.0) delta_x2=1.0; else if (delta_x2<-1.0) delta_x2=-1.0;
		}
		else if(ty_) {
		delta_y2=-1.0;
		delta_x2=-(double)(coeff3-coeff5)/(double)(2*coeff1);
		if(delta_x2>1.0) delta_x2=1.0; else if (delta_x2<-1.0) delta_x2=-1.0;
		}
		// insert both options for evaluation which to pick
		double max1 = (coeff1*delta_x1*delta_x1+coeff2*delta_y1*delta_y1
		+coeff3*delta_x1+coeff4*delta_y1
		+coeff5*delta_x1*delta_y1
		+coeff6)/18.0;
		double max2 = (coeff1*delta_x2*delta_x2+coeff2*delta_y2*delta_y2
		+coeff3*delta_x2+coeff4*delta_y2
		+coeff5*delta_x2*delta_y2
		+coeff6)/18.0;
		if(max1>max2) {
		delta_x[0]=delta_x1;
		delta_y[0]=delta_x1;
		return max1;
		}
		else{
		delta_x[0]=delta_x2;
		delta_y[0]=delta_x2;
		return max2;
		}
		}
		
		// this is the case of the maximum inside the boundaries:
		return (coeff1*delta_x[0]*delta_x[0]+coeff2*delta_y[0]*delta_y[0]
		+coeff3*delta_x[0]+coeff4*delta_y[0]
		+coeff5*delta_x[0]*delta_y[0]
		+coeff6)/18.0;
}

    
    // Some helper classes for the Brisk pattern representation
    private class BriskPatternPoint {
    	private double x;  // x coordinate relative to center
    	private double y;  // y coordinate relative to center
    	private double sigma; // Gaussian smoothing sigma
    	
    	public BriskPatternPoint() {
    		
    	}
    	
    	public BriskPatternPoint(double x, double y, double sigma) {
    		this.x = x;
    		this.y = y;
    		this.sigma = sigma;
    	}
    	
    	public void setX(double x) {
    		this.x = x;
    	}
    	
    	public void setY(double y) {
    		this.y = y;
    	}
    	
    	public void setSigma(double sigma) {
    		this.sigma = sigma;
    	}
    	
    	public double getX() {
    		return x;
    	}
    	
    	public double getY() {
    		return y;
    	}
    	
    	public double getSigma() {
    		return sigma;
    	}
    }
    
    private class BriskShortPair {
    	private int i; // index of the first pattern point
    	private int j; // index of other pattern point
    	
    	public BriskShortPair() {
    		
    	}
    	
    	public BriskShortPair(int i, int j) {
    		this.i = i;
    		this.j = j;
    	}
    	
    	public void setI(int i) {
    		this.i = i;
    	}
    	
    	public void setJ(int j) {
    		this.j = j;
    	}
    	
    	public int getI() {
    		return i;
    	}
    	
    	public int getJ() {
    		return j;
    	}
    }
    
    private class BriskLongPair {
    	private int i; // index of the first pattern point
    	private int j; // index of other pattern point
    	private int weighted_dx; // 1024.0/dx
    	private int weighted_dy; // 1024.0/dy
    	
    	public BriskLongPair() {
    		
    	}
    	
    	public BriskLongPair(int i, int j, int weighted_dx, int weighted_dy) {
    		this.i = i;
    		this.j = j;
    		this.weighted_dx = weighted_dx;
    		this.weighted_dy = weighted_dy;
    	}
    	
    	public void setI(int i) {
    		this.i = i;
    	}
    	
    	public void setJ(int j) {
    		this.j = j;
    	}
    	
    	public void setWeighted_dx(int weighted_dx) {
    		this.weighted_dx = weighted_dx;
    	}
    	
    	public void setWeighted_dy(int weighted_dy) {
    		this.weighted_dy = weighted_dy;
    	}
    	
    	public int getI() {
    		return i;
    	}
    	
    	public int getJ() {
    		return j;
    	}
    	
    	public int getWeighted_dx() {
    		return weighted_dx;
    	}
    	
    	public int getWeighted_dy() {
    		return weighted_dy;
    	}
    }
    
    // Data structure for salient point detectors
    private class KeyPoint {
    	// Coordinates of the keypoint
    	private Point2d pt;
    	// Diameter of meaningful keypoint neighborhood
    	private double size;
    	// Computed orientation of the keypoint (-1 if not applicable)
    	// Its possible values are in the range [0,360) degrees.
    	// It is measured relative to the image coordinate system
    	// (y-axis is directed downward), i.e. clockwise
    	private double angle = -1.0;
    	// The response by which the most strong keypoints have been selected.
    	// Can be used for further sorting or subsampling
    	private double response = 0.0;
    	// Octave (pyramid layer) from which the keypoint has been selected
    	private int octave = 0;
    	// Object id that can be used to cluster keypoint by an object they belong to
    	private int class_id = -1;
    	
    	public KeyPoint(Point2d pt, double size) {
    		this.pt = pt;
    		this.size = size;
    		this.angle = -1.0;
    		this.response = 0.0;
    		this.octave = 0;
    		this.class_id = -1;
    	}
    	
    	public KeyPoint(double x, double y, double size, double angle, double response, int octave) {
    		this.pt = new Point2d(x, y);
    		this.size = size;
    		this.angle = angle;
    		this.response = response;
    		this.octave = octave;
    		this.class_id = -1;
    	}
    	
    	public void setAngle(double angle) {
    		this.angle = angle;
    	}
    	
    	public Point2d getPt() {
    		return pt;
    	}
    	
    	public double getSize() {
    		return size;
    	}
    	
    	public double getAngle() {
    		return angle;
    	}
    }
    
    
    // Construct a layer
    private class BriskLayer {
    	private ModelImage image;
    	private double scale = 1.0;
    	private double offset = 0.0;
    	private int scores[][] = null;
    	private int xDim;
    	private int yDim;
    	OastDetector9_16 oastDetector;
    	AgastDetector5_8 agastDetector_5_8;
    	
    	// attention: this means that the passed image reference must point to persistent memory
    	public BriskLayer(ModelImage image) {
    		this.image = image;
    		xDim = image.getExtents()[0];
    		yDim = image.getExtents()[1];
    		scores = new int[yDim][xDim];
    		// create an agast detector
    		oastDetector = new OastDetector9_16(xDim, yDim, 0);
    		agastDetector_5_8 = new AgastDetector5_8(xDim, yDim, 0);

    	}
    	
    	// Derive a layer
    	public BriskLayer(BriskLayer layer, int mode) {
    		final boolean doClip = true;
            final boolean doVOI = false;
            final boolean doRotateCenter = false;
            final Vector3f center = new Vector3f();
            final float fillValue = 0.0f;
            final boolean doUpdateOrigin = false;
            final boolean doPad = false;
            TransMatrix xfrm = new TransMatrix(3);
			xfrm.identity();
			float iXres = layer.getImage().getFileInfo(0).getResolutions()[0];
			float iYres = layer.getImage().getFileInfo(0).getResolutions()[1];
			int interp = AlgorithmTransform.BILINEAR;
		    int units[] = layer.getImage().getUnitsOfMeasure();
    		if(mode == HALFSAMPLE){
    			xDim = layer.getXDim()/2;
    			yDim = layer.getYDim()/2;
    			scale = layer.getScale()*2;
    		}
    		else {
    			xDim = 2 * (layer.getXDim()/3);
    			yDim = 2 * (layer.getYDim()/3);
    			scale = layer.getScale()*1.5;
    		}
    		offset = 0.5*scale-0.5;
    		float oXres = (iXres * layer.getXDim())/xDim;
			float oYres = (iYres * layer.getYDim())/yDim;
			AlgorithmTransform algoTrans = new AlgorithmTransform(layer.getImage(), xfrm, interp, oXres,
					 oYres, xDim, yDim, units, doVOI, doClip,
                     doPad, doRotateCenter, center);
            algoTrans.setFillValue(fillValue);
            algoTrans.setUpdateOriginFlag(doUpdateOrigin);
            algoTrans.setSuppressProgressBar(true);
            algoTrans.run();
            image = algoTrans. getTransformedImage();
            algoTrans.disposeLocal();
            algoTrans = null;
    		scores = new int[yDim][xDim];
    		oastDetector = new OastDetector9_16(xDim, yDim, 0);
    		agastDetector_5_8 = new AgastDetector5_8(xDim, yDim, 0);
    	}
    	
    	public double getScale() {
    		return scale;
    	}
    	
    	public double getOffset() {
    		return offset;
    	}
    	
    	public ModelImage getImage() {
    		return image;
    	}
    	
    	public int getXDim() {
    		return xDim;
    	}
    	
    	public int getYDim() {
    		return yDim;
    	}
    	
    	public int[][] getScores() {
    		return scores;
    	}
    	
    	 public void getAgastPoints(double threshold, Vector<Point2d> keypoints){
         	oastDetector.setThreshold(threshold);
         	int sliceSize = xDim * yDim;
         	double doubleBuffer[] = new double[sliceSize];
         	try {
         		image.exportData(0, sliceSize, doubleBuffer);
         	}
         	catch (IOException e) {
         		MipavUtil.displayError("IOException " + e + " on image.exportData(0, sliceSize, doubleBuffer) in getAgastPoints()");
         		setCompleted(false);
         		return;
         	}
         	oastDetector.detect(doubleBuffer,keypoints);

         	// also write scores
         	final int num=keypoints.size();

         	for(int i=0; i<num; i++){
         		int x = (int)Math.round(keypoints.get(i).x);
         		int y = (int)Math.round(keypoints.get(i).y);
         		final int offs= x+ y*xDim;
         		scores[y][x]=(int)Math.round(oastDetector.getCornerScore(doubleBuffer, offs, image.getMax()));
         	}
         }
    	 
    	 public boolean isMax2D(final int x_layer, final int y_layer){
    			// decision tree:
    			final int center = scores[y_layer][x_layer];
    			final int s_10= scores[y_layer][x_layer-1];
    			if(center<s_10) return false;
    			final int s10= scores[y_layer][x_layer+1];
    			if(center<s10) return false;
    			final int s0_1= scores[y_layer-1][x_layer];
    			if(center<s0_1) return false;
    			final int s01= scores[y_layer+1][x_layer];
    			if(center<s01) return false;
    			final int s_11= scores[y_layer+1][x_layer-1];
    			if(center<s_11) return false;
    			final int s11= scores[y_layer+1][x_layer+1];
    			if(center<s11) return false;
    			final int s1_1= scores[y_layer-1][x_layer+1];
    			if(center<s1_1) return false;
    			final int s_1_1= scores[y_layer-1][x_layer-1];
    			if(center<s_1_1) return false;

    			// reject neighbor maxima
    			Vector<Integer> delta = new Vector<Integer>();
    			// put together a list of 2d-offsets to where the maximum is also reached
    			if(center==s_1_1) {
    				delta.add(-1);
    				delta.add(-1);
    			}
    			if(center==s0_1) {
    				delta.add(0);
    				delta.add(-1);
    			}
    			if(center==s1_1) {
    				delta.add(1);
    				delta.add(-1);
    			}
    			if(center==s_10) {
    				delta.add(-1);
    				delta.add(0);
    			}
    			if(center==s10) {
    				delta.add(1);
    				delta.add(0);
    			}
    			if(center==s_11) {
    				delta.add(-1);
    				delta.add(1);
    			}
    			if(center==s01) {
    				delta.add(0);
    				delta.add(1);
    			}
    			if(center==s11) {
    				delta.add(1);
    				delta.add(1);
    			}
    			final int deltasize=delta.size();
    			if(deltasize!=0){
    				// in this case, we have to analyze the situation more carefully:
    				// the values are gaussian blurred and then we really decide
    				int smoothedcenter=4*center+2*(s_10+s10+s0_1+s01)+s_1_1+s1_1+s_11+s11;
    				for(int i=0; i<deltasize;i+=2){
    					int othercenter= scores[y_layer-1+delta.get(i+1)][x_layer+delta.get(i)-1];
    					othercenter+=2*(scores[y_layer-1+delta.get(i+1)][x_layer+delta.get(i)]);
    					othercenter+= scores[y_layer-1+delta.get(i+1)][x_layer+delta.get(i)+1];
    					othercenter+=2*(scores[y_layer+delta.get(i+1)][x_layer+delta.get(i)+1]);
    					othercenter+=4*(scores[y_layer+delta.get(i+1)][x_layer+delta.get(i)]);
    					othercenter+=2*(scores[y_layer+delta.get(i+1)][x_layer+delta.get(i)-1]);
    					othercenter+= scores[y_layer+delta.get(i+1)+1][x_layer+delta.get(i)-1];
    					othercenter+=2*(scores[y_layer+delta.get(i+1)+1][x_layer+delta.get(i)]);
    					othercenter+= scores[y_layer+delta.get(i+1)+1][x_layer+delta.get(i)+1];
    					if(othercenter>smoothedcenter) return false;
    				}
    			}
    			return true;
    		}
    	 
    	 public double getAgastScore(double xf, double yf, double threshold) {
    		  return getAgastScore(xf, yf, threshold, 1.0);
    	 }
    	 
    	 public double getAgastScore(double xf, double yf, double threshold, double scale){
    			if(scale<=1.0f){
    				// just do an interpolation inside the layer
    				final int x=(int)(xf);
    				final double rx1=xf-(double)(x);
    				final double rx=1.0-rx1;
    				final int y=(int)(yf);
    				final double ry1=yf-(double)(y);
    				final double ry=1.0-ry1;

    				return rx*ry*getAgastScore(x, y, threshold)+
    						rx1*ry*getAgastScore(x+1, y, threshold)+
    						rx*ry1*getAgastScore(x, y+1, threshold)+
    						rx1*ry1*getAgastScore(x+1, y+1, threshold);
    			}
    			else{
    				// this means we overlap area smoothing
    				final double halfscale = scale/2.0;
    				// get the scores first:
    				for(int x=(int)(xf-halfscale); x<=(int)(xf+halfscale+1.0f); x++){
    					for(int y=(int)(yf-halfscale); y<=(int)(yf+halfscale+1.0f); y++){
    						getAgastScore(x, y, threshold);
    					}
    				}
    				// get the smoothed value
    				return value(scores,xf,yf,scale);
    			}
    		}

    	 
    	// get scores - attention, this is in layer coordinates, not scale=1 coordinates!
    	 public double getAgastScore(int x, int y, double threshold){
    			if(x<3||y<3) return 0;
    			if(x>=xDim-3||y>=yDim-3) return 0;
    			double score = scores[y][x];
    			if(score>2) { return score; }
    			oastDetector.setThreshold(threshold-1);
    			int sliceSize = xDim * yDim;
             	double doubleBuffer[] = new double[sliceSize];
             	try {
             		image.exportData(0, sliceSize, doubleBuffer);
             	}
             	catch (IOException e) {
             		MipavUtil.displayError("IOException " + e + " on image.exportData(0, sliceSize, doubleBuffer) in getAgastScore()");
             		setCompleted(false);
             		return -1;
             	}
             	int offs= x+ y*xDim;
    			score = oastDetector.getCornerScore(doubleBuffer, offs, image.getMax());
    			if (score<threshold) score = 0;
    			return score;
    		}
    	 
    	 public double getAgastScore_5_8(int x, int y, double threshold){
    			if(x<2||y<2) return 0;
    			if(x>=xDim-2||y>=yDim-2) return 0;
    			agastDetector_5_8.setThreshold(threshold-1);
    			int sliceSize = xDim * yDim;
    			double doubleBuffer[] = new double[sliceSize];
             	try {
             		image.exportData(0, sliceSize, doubleBuffer);
             	}
             	catch (IOException e) {
             		MipavUtil.displayError("IOException " + e + " on image.exportData(0, sliceSize, doubleBuffer) in getAgastScore_5_8()");
             		setCompleted(false);
             		return -1;
             	}
    			int offs= x+ y*xDim;
    			double score = agastDetector_5_8.getCornerScore(doubleBuffer, offs, image.getMax());
    			if (score<threshold) score = 0;
    			return score;
    		}

    	 
    	// access gray values (smoothed/interpolated)
    	private double value(int scores[][], double xf, double yf, double scale){
    	 	// get the position
    	 	final int x = (int)Math.floor(xf);
    	 	final int y = (int)Math.floor(yf);

    	 	// get the sigma_half:
    	 	final double sigma_half=scale/2;
    	 	final double area=4.0*sigma_half*sigma_half;
    	 	// calculate output:
    	 	int ret_val;
    	 	if(sigma_half<0.5){
    	 		//interpolation multipliers:
    	 		final int r_x=(int)((xf-x)*1024);
    	 		final int r_y=(int)((yf-y)*1024);
    	 		final int r_x_1=(1024-r_x);
    	 		final int r_y_1=(1024-r_y);
    	 		// just interpolate:
    	 		ret_val=(r_x_1*r_y_1*scores[y][x]);
    	 		ret_val+=(r_x*r_y_1*scores[y][x+1]);
    	 		ret_val+=(r_x*r_y*scores[y+1][x+1]);
    	 		ret_val+=(r_x_1*r_y*scores[y+1][x]);
    	 		return ((ret_val+512)/1024/1024);
    	 	}

    	 	// this is the standard case (simple, not speed optimized yet):

    	 	// scaling:
    	 	final int scaling = (int)(4194304.0/area);
    	 	final int scaling2= (int)(scaling*area/1024.0);

    	 	// calculate borders
    	 	final double x_1=xf-sigma_half;
    	 	final double x1=xf+sigma_half;
    	 	final double y_1=yf-sigma_half;
    	 	final double y1=yf+sigma_half;

    	 	final int x_left=(int)(x_1+0.5);
    	 	final int y_top=(int)(y_1+0.5);
    	 	final int x_right=(int)(x1+0.5);
    	 	final int y_bottom=(int)(y1+0.5);

    	 	// overlap area - multiplication factors:
    	 	final double r_x_1=(double)(x_left)-x_1+0.5;
    	 	final double r_y_1=(double)(y_top)-y_1+0.5;
    	 	final double r_x1=x1-(double)(x_right)+0.5;
    	 	final double r_y1=y1-(double)(y_bottom)+0.5;
    	 	final int A=(int)((r_x_1*r_y_1)*scaling);
    	 	final int B=(int)((r_x1*r_y_1)*scaling);
    	 	final int C=(int)((r_x1*r_y1)*scaling);
    	 	final int D=(int)((r_x_1*r_y1)*scaling);
    	 	final int r_x_1_i=(int)(r_x_1*scaling);
    	 	final int r_y_1_i=(int)(r_y_1*scaling);
    	 	final int r_x1_i=(int)(r_x1*scaling);
    	 	final int r_y1_i=(int)(r_y1*scaling);

    	 	// now the calculation:
    	 	// first row:
    	 	ret_val=A*scores[y_top][x_left];
    	 	int xptr = x_left + 1;
    	 	int endx = x_right;
    	 	for(; xptr<endx; xptr++){
    	 		ret_val+=r_y_1_i*scores[y_top][xptr];
    	 	}
    	 	ret_val+=B*scores[y_top][x_right];
    	 	// middle ones:
    	 	int yptr = y_top + 1;
    	 	xptr = x_left;
    	 	for(; yptr < y_bottom; yptr++, xptr = x_left){
    	 		ret_val+=r_x_1_i*scores[yptr][x_left];
    	 		xptr++;
    	 		for(; xptr<x_right; xptr++){
    	 			ret_val+=scores[yptr][xptr]*scaling;
    	 		}
    	 		ret_val+=r_x1_i*scores[yptr][x_right];
    	 	}
    	 	// last row:
    	 	ret_val+=D*scores[y_bottom][x_left];
    	 	xptr = x_left+1;
    	 	for(; xptr<x_right; xptr++){
    	 		ret_val+=r_y1_i*scores[y_bottom][xptr];
    	 	}
    	 	ret_val+=C*scores[y_bottom][x_right];

    	 	return ((ret_val+scaling2/2)/scaling2/1024);
    	 }



    }
    
    /**#ifndef HAMMINGSSE_HPP_
    #define HAMMINGSSE_HPP_

    #include <emmintrin.h>
    #include <tmmintrin.h>

    namespace cv{

    #ifdef __GNUC__
    static const char __attribute__((aligned(16))) MASK_4bit[16] = {0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf};
    static const uint8_t __attribute__((aligned(16))) POPCOUNT_4bit[16] = { 0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4};
    static const __m128i shiftval = _mm_set_epi32 (0,0,0,4);
    #endif
    #ifdef _MSC_VER
    __declspec(align(16)) static const char MASK_4bit[16] = {0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf};
    __declspec(align(16)) static const uint8_t POPCOUNT_4bit[16] = { 0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4};
    static const __m128i shiftval = _mm_set_epi32 (0,0,0,4);
    #endif

    __inline__ // - SSSE3 - better alorithm, minimized psadbw usage - adapted from http://wm.ite.pl/articles/sse-popcount.html
    uint32_t HammingSse::ssse3_popcntofXORed(const __m128i* signature1, const __m128i* signature2, const int numberOf128BitWords) {

    	uint32_t result = 0;

    	register __m128i xmm0;
    	register __m128i xmm1;
    	register __m128i xmm2;
    	register __m128i xmm3;
    	register __m128i xmm4;
    	register __m128i xmm5;
    	register __m128i xmm6;
    	register __m128i xmm7;

    	//__asm__ volatile ("movdqa (%0), %%xmm7" : : "a" (POPCOUNT_4bit) : "xmm7");
    	xmm7 = _mm_load_si128 ((__m128i *)POPCOUNT_4bit);
    	//__asm__ volatile ("movdqa (%0), %%xmm6" : : "a" (MASK_4bit) : "xmm6");
    	xmm6 = _mm_load_si128 ((__m128i *)MASK_4bit);
    	//__asm__ volatile ("pxor    %%xmm5, %%xmm5" : : : "xmm5"); // xmm5 -- global accumulator
    	xmm5 = _mm_setzero_si128();

    	const size_t end=(size_t)(signature1+numberOf128BitWords);

    	//__asm__ volatile ("movdqa %xmm5, %xmm4"); // xmm4 -- local accumulator
    	xmm4 = xmm5;//_mm_load_si128(&xmm5);

    	//for (n=0; n < numberOf128BitWords; n++) {
    	do{
    		//__asm__ volatile ("movdqa (%0), %%xmm0" : : "a" (signature1++) : "xmm0"); //slynen load data for XOR
    		//		__asm__ volatile(
    		//				"movdqa	  (%0), %%xmm0	\n"
    		//"pxor      (%0), %%xmm0   \n" //slynen do XOR
    		xmm0 = _mm_xor_si128 ( (__m128i)*signature1++, (__m128i)*signature2++); //slynen load data for XOR and do XOR
    		//				"movdqu    %%xmm0, %%xmm1	\n"
    		xmm1 = xmm0;//_mm_loadu_si128(&xmm0);
    		//				"psrlw         $4, %%xmm1	\n"
    		xmm1 = _mm_srl_epi16 (xmm1, shiftval);
    		//				"pand      %%xmm6, %%xmm0	\n"	// xmm0 := lower nibbles
    		xmm0 = _mm_and_si128 (xmm0, xmm6);
    		//				"pand      %%xmm6, %%xmm1	\n"	// xmm1 := higher nibbles
    		xmm1 = _mm_and_si128 (xmm1, xmm6);
    		//				"movdqu    %%xmm7, %%xmm2	\n"
    		xmm2 = xmm7;//_mm_loadu_si128(&xmm7);
    		//				"movdqu    %%xmm7, %%xmm3	\n"	// get popcount
    		xmm3 = xmm7;//_mm_loadu_si128(&xmm7);
    		//				"pshufb    %%xmm0, %%xmm2	\n"	// for all nibbles
    		xmm2 = _mm_shuffle_epi8(xmm2, xmm0);
    		//				"pshufb    %%xmm1, %%xmm3	\n"	// using PSHUFB
    		xmm3 = _mm_shuffle_epi8(xmm3, xmm1);
    		//				"paddb     %%xmm2, %%xmm4	\n"	// update local
    		xmm4 = _mm_add_epi8(xmm4, xmm2);
    		//				"paddb     %%xmm3, %%xmm4	\n"	// accumulator
    		xmm4 = _mm_add_epi8(xmm4, xmm3);
    		//				:
    		//				: "a" (buffer++)
    		//				: "xmm0","xmm1","xmm2","xmm3","xmm4"
    		//		);
    	}while((size_t)signature1<end);
    	// update global accumulator (two 32-bits counters)
    	//	__asm__ volatile (
    	//			/*"pxor	%xmm0, %xmm0		\n"*/
    
    
    	//			"psadbw	%%xmm5, %%xmm4		\n"
    	/**xmm4 = _mm_sad_epu8(xmm4, xmm5);
    	//			"paddd	%%xmm4, %%xmm5		\n"
    	xmm5 = _mm_add_epi32(xmm5, xmm4);
    	//			:
    	//			:
    	//			: "xmm4","xmm5"
    	//	);
    	// finally add together 32-bits counters stored in global accumulator
//    	__asm__ volatile (
//    			"movhlps   %%xmm5, %%xmm0	\n"
    	xmm0 = _mm_cvtps_epi32(_mm_movehl_ps(_mm_cvtepi32_ps(xmm0), _mm_cvtepi32_ps(xmm5))); //TODO fix with appropriate intrinsic
//    			"paddd     %%xmm5, %%xmm0	\n"
    	xmm0 = _mm_add_epi32(xmm0, xmm5);
//    			"movd      %%xmm0, %%eax	\n"
    	result = _mm_cvtsi128_si32 (xmm0);
//    			: "=a" (result) : : "xmm5","xmm0"
//    	);
    	return result;
    }

    }
    #endif /* HAMMINGSSE_HPP_ */

    
}