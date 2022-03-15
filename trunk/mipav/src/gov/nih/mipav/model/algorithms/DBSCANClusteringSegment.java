package gov.nih.mipav.model.algorithms;

import gov.nih.mipav.model.algorithms.AlgorithmBase;
import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.model.structures.ModelStorageBase;

import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import gov.nih.mipav.view.ViewJProgressBar;

/**
 * % Copyright (c) 2013 Peter Kovesi
% www.peterkovesi.com/matlabfns/
% 
% Permission is hereby granted, free of charge, to any person obtaining a copy
% of this software and associated documentation files (the "Software"), to deal
% in the Software without restriction, subject to the following conditions:
% 
% The above copyright notice and this permission notice shall be included in 
% all copies or substantial portions of the Software.
%
% The Software is provided "as is", without warranty of any kind.
 * 
 * 
 * The image must be a color image
 * First the slic.m software performs a SLIC Simple Linear Iterative Clustering SuperPixels.
 * SLIC is followed by SPDBSCAN.m to perform a DBSCAN clustering of superpixels.  This results in
 * a simple and fast segmentation of an image.
 * 
 * Ported MATLAB files:
 *  slic.m Implementation of Achanta, Shaji, Smith, Lucchi, Fua and Susstrunk's SLIC Superpixels.
	spdbscan.m Implements DBSCAN clustering of superpixels.
	cleanupregions.m Cleans up small regions in a segmentation. Used by slic.m
	mcleanupregions.m Morphological version of cleanupregions.m The output is not quite as nice but the execution is much faster.
	finddisconnected.m Finds groupings of disconnected labeled regions. Used by mcleanupregions.m to reduce execution time.
	makeregionsdistinct.m Ensures labeled regions are distinct.
	renumberregions.m Ensures all regions in labeled image have a unique label and that the label numbering forms a contiguous sequence.
	regionadjacency.m Computes adjacency matrix for an image of labeled segmented regions.
	drawregionboundaries.m Draw boundaries of labeled regions in an image.
	maskimage.m used by drawregionboundaries.m
    circularstruct.m Generates a circular structuring element, used by mcleanupregions.m
	dbscan.m Basic implementation of DBSCAN
	testdbscan.m Function to test/demonstrate dbscan.m


 * 
 * References:
	R. Achanta, A. Shaji, K. Smith, A. Lucchi, P. Fua and S. Susstrunk. "SLIC Superpixels Compared
    to State-of-the-Art Superpixel Methods" PAMI. Vol 34 No 11. November 2012. pp 2274-2281.
	Martin Ester, Hans-Peter Kriegel, Jörg Sander, Xiaowei Xu (1996). "A density-based algorithm
    for discovering clusters in large spatial databases with noise". Proceedings of the Second International
    Conference on Knowledge Discovery and Data Mining (KDD-96). AAAI Press. pp. 226-231.

 */

public class DBSCANClusteringSegment extends AlgorithmBase {
	
	//~ Instance fields ------------------------------------------------------------------------------------------------
	// SLIC input parameters:
    private final int MEAN_CENTER = 1;
    private final int MEDIAN_CENTER = 2;
	// Number of desired superpixels. Note that this is nominal
	// the actual number of superpixels generated will generally
	// be a bit larger, especially if parameter m is small.
	private int k;
	// Weighting factor between colour and spatial
	// differences. Values from about 5 to 40 are useful.  Use a
	// large value to enforce superpixels with more regular and
	// smoother shapes. Try a value of 10 to start with.
	private double m = 10.0;
	// mRegions morphologically smaller than this are merged with
	// adjacent regions. Try a value of 1 or 1.5.  Use 0 to
	// disable.
	private double seRadius = 1.0;
	// String "mean" or "median" indicating how the cluster
	// colour centre should be computed. Defaults to "mean"
	private int center = MEAN_CENTER;
	// Optional median filtering window size.  Image compression
	// can result in noticeable artifacts in the a*b* components
	// of the image.  Median filtering can reduce this. mw can be
	// a single value in which case the same median filtering is
	// applied to each L* a* and b* components.  Alternatively it
	// can be a 2-vector where mw(1) specifies the median
	// filtering window to be applied to L* and mw(2) is the
	// median filtering window to be applied to a* and b*.
	private int mw1 = 0;
	private int mw2 = 0;
	private int nItr = 10;
	
	// SPDBSCAN input parameter:
	// Matching tolerance value/distance threshold that controls which
	// superpixels are clustered together.  This value is in L*a*b*
	// colour units.  Try a value in the range 5-10 to start with.
	// Changing the value of E by just 1 unit can give a significant
	// difference. 
	private double E = 7.5;
	
	//~ Constructors ---------------------------------------------------------------------------------------------------
	
		public DBSCANClusteringSegment(ModelImage destImg, ModelImage srcImg, int k,
				double m, double seRadius, int center, int mw1, int mw2, 
				int nItr, double E) {
			super(destImg, srcImg);
			this.k = k;
			this.m = m;
			this.seRadius = seRadius;
			this.center = center;
			this.mw1 = mw1;
			this.mw2 = mw2;
			this.nItr = nItr;
			this.E = E;
		}
	
	public void runAlgorithm() {
		double scaleMax;
		float buffer[];
		double Labbuf[][];
		int i,j,index,n;
		double varR;
		double varG;
		int x,y;
		double varB;
		double X,Y,Z;
		double varX, varY, varZ;
		// Observer = 2 degrees, Illuminant = D65
        double XN = 95.047;
        double YN = 100.000;
        double ZN = 108.883;
        double L;
        double a;
        double b;
        double minL = Double.MAX_VALUE;
        double maxL = -Double.MAX_VALUE;
        double mina = Double.MAX_VALUE;
        double maxa = -Double.MAX_VALUE;
        double minb = Double.MAX_VALUE;
        double maxb = -Double.MAX_VALUE;
        int halfmw1;
        int halfmw2;
        int ymin;
        int ymax;
        int xmin;
        int xmax;
        int medlength;
        double med[] = null;
        double medarray[];
        int medptr;
        double S;
        int nodeCols;
        int nodeRows;
        double vSpacing;
        double C[][];
        int l[][];
        double d[][];
        int kk;
        double r;
        int ri;
        double c;
        int ci;
        int cc;
        int rr;
        int intS;
        int rmin;
        int rmax;
        int cmin;
        int cmax;
        int rlen;
        int clen;
        double subim[][][];
        double Ckk[];
        double D[][];
        double Am[][];
        if (srcImage == null) {
            displayError("Source Image is null");
            finalize();

            return;
        }
		
		if (!srcImage.isColorImage()) {
			displayError("Source image must be a color image");
			finalize();
			return;
		}
		
		// Convert image to L*a*b* colourspace.  This gives us a colourspace that is
	    // nominally perceptually uniform. This allows us to use the euclidean
	    // distance between colour coordinates to measure differences between
	    // colours.  Note the image becomes double after conversion.  We may want to
	    // go to signed shorts to save memory.
		// The adapted white point is D65.
		// Convert RGB to CIE 1976 L*a*b
		// The three coordinates of CIELAB represent the lightness of the color(L* = 0 yields black and L* = 100 indicates diffuse 
	    // white; specular white may be higher), its position between red/magenta and green(a*, negative values indicate green
	    // while positive values indicate magenta) and its position between yellow and blue(b*, negative values indicate blue 
	    // and positive values indicate yellow).  The asterisk(*) after L, a, and b are part of the full name, since they represent 
	    // L*, a*, and b*, to distinguish them from Hunter's L, a, and b.
	    		  
	    // The L* coordinate ranges from 0 to 100.  The possible range of a* and b* coordinates depends on the color space that one
	    // is converting from.  
	    // R = 0, G = 0, B = 0 => L* = 0, a* = 0, b* = 0
	    // R = 255, G = 0, B = 0 => L* = 53.2, a* = 80.1, b* = 67.22
	    // R = 0, G = 255, B = 0 => L* = 87.7, a* = -86.2, b* = 83.2
	    // R = 0, G = 0, B = 255 => L* = 32.3, a* = 79.2, b* = -107.9
	    // R = 255, G = 255, B = 0 => L* = 97.1, a* = -21.6, b* = 94.5
	    // R = 255, G = 0, B = 255 => L* = 60.3, a* = 98.3, b* = -60.8
	    // R = 0, G = 255, B = 255 => L* = 91.1, a* = -48.1, b* = -14.1
	    // R = 255, G = 255, B = 255 => L* = 100.0, a* = 0.00525, b* = -0.0104
	    // so the range of a* equals about the range of b* and the range of a* equals about twice the range of L*.
	    // The simplest distance metric delta E is CIE76 = sqrt((L2* - L1*)**2 + (a2* - a1*)**2 + (b2* - b1*)**2)
	    		
	    // XW, YW, and ZW (also called XN, YN, ZN or X0, Y0, Z0) are reference white tristimulus values - typically the white
	    // of a perfectly reflecting diffuser under CIE standard D65 illumination(defined by x = 0.3127 and y = 0.3291 in the
	    // CIE chromatcity diagram).  The 2 degrees, D65 reference tristimulus values are: XN = 95.047, YN = 100.000, and ZN = 108.883.
	    
	    // Scale factor used in RGB-CIELab conversions.  255 for ARGB, could be higher for ARGB_USHORT.
		int xDim = srcImage.getExtents()[0];
		int yDim = srcImage.getExtents()[1];
		int length = xDim * yDim;
		scaleMax = Math.max(255.0, srcImage.getMax());
		buffer =  new float[4 * length];
		Labbuf = new double[length][3];
		try {
			srcImage.exportData(0, length, buffer);
		}
		catch(IOException e) {
			e.printStackTrace();
			setCompleted(false);
			return;
		}
		for (i = 0; i < buffer.length; i += 4) {
            varR = buffer[i+1]/scaleMax;
            varG = buffer[i+2]/scaleMax;
            varB = buffer[i+3]/scaleMax;
            
            if (varR <= 0.04045) {
                varR = varR/12.92;
            }
            else {
                varR = Math.pow((varR + 0.055)/1.055, 2.4);
            }
            if (varG <= 0.04045) {
                varG = varG/12.92;
            }
            else {
                varG = Math.pow((varG + 0.055)/1.055, 2.4);
            }
            if (varB <= 0.04045) {
                varB = varB/12.92;
            }
            else {
                varB = Math.pow((varB + 0.055)/1.055, 2.4);
            }
            
            varR = 100.0 * varR;
            varG = 100.0 * varG;
            varB = 100.0 * varB;
            
            // Observer = 2 degrees, Illuminant = D65
            X = 0.4124*varR + 0.3576*varG + 0.1805*varB;
            Y = 0.2126*varR + 0.7152*varG + 0.0722*varB;
            Z = 0.0193*varR + 0.1192*varG + 0.9505*varB;
            
            varX = X/ XN;
            varY = Y/ YN;
            varZ = Z/ ZN;
            
            if (varX > 0.008856) {
                varX = Math.pow(varX, 1.0/3.0);
            }
            else {
                varX = (7.787 * varX) + (16.0/116.0);
            }
            if (varY > 0.008856) {
                varY = Math.pow(varY, 1.0/3.0);
            }
            else {
                varY = (7.787 * varY) + (16.0/116.0);
            }
            if (varZ > 0.008856) {
                varZ = Math.pow(varZ, 1.0/3.0);
            }
            else {
                varZ = (7.787 * varZ) + (16.0/116.0);
            }
            
            L = ((116.0 * varY) - 16.0);
            a = (500.0 * (varX - varY));
            b = (200.0 * (varY - varZ));
            
            if (L < minL) {
                minL = L;
            }
            if (L > maxL) {
                maxL = L;
            }
            if (a < mina) {
                mina = a;
            }
            if (a > maxa) {
                maxa = a;
            }
            if (b < minb) {
                minb = b;
            }
            if (b > maxb) {
                maxb = b;
            }
            
            Labbuf[i/4][0] = L;
            Labbuf[i/4][1] = a;
            Labbuf[i/4][2] = b;
        } // for (i = 0; i < buffer.length; i += 4)
		
		// Apply median filtering to colour components if mw has been supplied
	    // and/or non-zero
		if ((mw1 > 0) || (mw2 > 0)) {
			med = new double[length];
		}
		if (mw1 > 0) {
			halfmw1 = (mw1 -1)/2;
		    for (y = 0; y < yDim; y++) {
		    	for (x = 0; x < xDim; x++) {
		    	    index = x + y * xDim;
		    	    ymin = Math.max(0,y-halfmw1);
		    	    ymax = Math.min(yDim-1,y+halfmw1);
		    	    xmin = Math.max(0,x-halfmw1);
	    	    	xmax = Math.min(xDim-1,x+halfmw1);
	    	    	medlength = (ymax-ymin+1)*(xmax-xmin+1);
	    	    	medarray = new double[medlength];
		    	    for (j = ymin, medptr = 0; j <= ymax; y++) {
		    	    	for (i = xmin; i <= xmax; x++, medptr++) {
		    	    	    medarray[medptr] = Labbuf[i + j * xDim][0];
		    	    	}
		    	    }
		    	    Arrays.sort(medarray);
		    	    if ((medlength % 2) == 1) {
		    	    	med[index] = medarray[(medlength-1)/2];
		    	    }
		    	    else {
		    	    	med[index] = (medarray[medlength/2] + medarray[(medlength/2)-1])/2.0;
		    	    }
		    	}
		    }
		    for (y = 0; y < yDim; y++) {
		    	for (x = 0; x < xDim; x++) {
		    	    index = x + y * xDim;
		    	    Labbuf[index][0] = med[index];
		    	}
		    }
		} // if (mw1 > 0)
		if (mw2 > 0) {
			halfmw2 = (mw2 -1)/2;
			for (n = 1; n <= 2; n++) {
			    for (y = 0; y < yDim; y++) {
			    	for (x = 0; x < xDim; x++) {
			    	    index = x + y * xDim;
			    	    ymin = Math.max(0,y-halfmw2);
			    	    ymax = Math.min(yDim-1,y+halfmw2);
			    	    xmin = Math.max(0,x-halfmw2);
		    	    	xmax = Math.min(xDim-1,x+halfmw2);
		    	    	medlength = (ymax-ymin+1)*(xmax-xmin+1);
		    	    	medarray = new double[medlength];
			    	    for (j = ymin, medptr = 0; j <= ymax; y++) {
			    	    	for (i = xmin; i <= xmax; x++, medptr++) {
			    	    	    medarray[medptr] = Labbuf[i + j * xDim][n];
			    	    	}
			    	    }
			    	    Arrays.sort(medarray);
			    	    if ((medlength % 2) == 1) {
			    	    	med[index] = medarray[(medlength-1)/2];
			    	    }
			    	    else {
			    	    	med[index] = (medarray[medlength/2] + medarray[(medlength/2)-1])/2.0;
			    	    }
			    	}
			    }
			    for (y = 0; y < yDim; y++) {
			    	for (x = 0; x < xDim; x++) {
			    	    index = x + y * xDim;
			    	    Labbuf[index][n] = med[index];
			    	}
			    }
			} // for (n = 1; n <= 2; n++)
		} // if (mw2 > 0)
		
		// Nominal spacing between grid elements assuming hexagonal grid
	    S = Math.sqrt(length / (k * Math.sqrt(3.0)/2.0));
	    
	    // Get nodes per row allowing a half column margin at one end that alternates
	    // from row to row
	    nodeCols = (int)Math.round(xDim/S - 0.5);
	    // Given an integer number of nodes per row recompute S
	    S = xDim/(nodeCols + 0.5); 

	    // Get number of rows of nodes allowing 0.5 row margin top and bottom
	    nodeRows = (int)Math.round(yDim/(Math.sqrt(3.0)/2.0*S));
	    vSpacing = (double)yDim/(double)nodeRows;

	    // Recompute k
	    k = nodeRows * nodeCols;
	    
	    // Allocate memory and initialise clusters, labels and distances.
	    C = new double[6][k];          // Cluster centre data  1:3 is mean Lab value,
	                                   // 4:5 is col, row of centre, 6 is No of pixels
	    Ckk = new double[6];
	    // Pixel labels.
	    l = new int[yDim][xDim];
	    for (y = 0; y < yDim; y++) {
	    	for (x = 0; x < xDim; x++) {
	    		l[y][x] = -1;
	    	}
	    }
	    // Pixel distances from cluster centres.
	    d = new double[yDim][xDim];
	    for (y = 0; y < yDim; y++) {
	    	for (x = 0; x < xDim; x++) {
	    		d[y][x] = Double.POSITIVE_INFINITY;
	    	}
	    }
	    
	    // Initialise clusters on a hexagonal grid
	    kk = 0;
	    r = vSpacing/2;
	    rr = (int)Math.round(r)-1;
	    
	    for (ri = 1; ri <= nodeRows; ri++) {
	        // Following code alternates the starting column for each row of grid
	        // points to obtain a hexagonal pattern. Note S and vSpacing are kept
	        // as doubles to prevent errors accumulating across the grid.
	        if ((ri % 2) == 1) {
	        	c = S/2.0; 
	        }
	        else {
	        	c = S;
	        }
	        
	        for (ci = 1; ci <= nodeCols; ci++) {
	            cc = (int)Math.round(c)-1; 
	            index = cc + rr*xDim;
	            C[0][kk] = Labbuf[index][0];
	            C[1][kk] = Labbuf[index][1];
	            C[2][kk] = Labbuf[index][2];
	            C[3][kk] = cc;
	            C[4][kk] = rr;
	            c = c+S;
	            kk = kk+1;
	        } // for (ci = 1; ci <= nodeCols; ci++)
	        
	        r = r+vSpacing;
	        rr = (int)Math.round(r)-1;
	    } // for (ri = 1; ri <= nodeRows; ri++)
	    
	    
	    // Now perform the clustering.  10 iterations is suggested but I suspect n
	    // could be as small as 2 or even 1
	    intS = (int)Math.round(S);  // We need S to be an integer from now on
	    
	    for (n = 1; n <= nItr; n++) {
	       for (kk = 0; kk < k; kk++) {  // for each cluster

	           // Get subimage around cluster
	           rmin = (int)Math.max(C[4][kk]-intS, 0);   
	           rmax = (int)Math.min(C[4][kk]+S, yDim-1);
	           if (rmax < rmin) {
	        	   System.err.println("rmax < rmin");
	        	   setCompleted(false);
	        	   return;
	           }
	           rlen = rmax-rmin+1;
	           cmin = (int)Math.max(C[3][kk]-S, 0);   
	           cmax = (int)Math.min(C[3][kk]+S, xDim-1);
	           if (cmax < cmin)  {
	        	   System.err.println("cmax < cmin");
	        	   setCompleted(false);
	        	   return;
	           }
	           clen = cmax-cmin+1;
	           subim = new double[rlen][clen][3];
	           for (y = rmin; y <= rmax; y++) {
	        	   for (x = cmin; x <= cmax; x++) {
	        		   index = x + y * xDim;
	        		   for (i = 0; i < 3; i++) {
	        			   subim[y-rmin][x-cmin][i] = Labbuf[index][i];
	        		   }
	        	   }
	           }
	           
	           // Compute distances D between C(:,kk) and subimage
	           for (i = 0; i < 6; i++) {
	        	   Ckk[i] = C[i][kk]; 
	           }
	           D = dist(Ckk, subim, rmin, cmin, S, m);
	           

	           // If any pixel distance from the cluster centre is less than its
	           // previous value update its distance and label
	           for (y = rmin; y <= rmax; y++) {
	        	   for (x = cmin; x <= cmax; x++) {
	        		   if (D[y-rmin][x-cmin] < d[y][x]) {
	        			   d[y][x] = D[y-rmin][x-cmin];
	        			   l[y][x] = kk;
	        		   }
	        	   }
	           }           
	       } // for (kk = 0; kk < k; kk++)
	       
	       // Update cluster centres with mean values
	       for (i = 0; i < 6; i++) {
	    	   for (j = 0; j < k; j++) {
	    		   C[i][j] = 0.0;
	    	   }
	       }
	       for (y = 0; y < yDim; y++) {
	           for (x = 0; x < xDim; x++) {
	        	  index = x + y * xDim;
	        	  C[0][l[y][x]] = C[0][l[y][x]] + Labbuf[index][0];
	        	  C[1][l[y][x]] = C[1][l[y][x]] + Labbuf[index][1];
	        	  C[2][l[y][x]] = C[2][l[y][x]] + Labbuf[index][2];
	        	  C[3][l[y][x]] = C[3][l[y][x]] + x;
	        	  C[4][l[y][x]] = C[4][l[y][x]] + y;
	        	  C[5][l[y][x]] = C[5][l[y][x]] + 1.0;
	           } // for (x = 0; x < xDim; x++)
	       } // for (y = 0; y < yDim; y++)
	       
	       // Divide by number of pixels in each superpixel to get mean values
	       for (kk = 0; kk < k; kk++) { 
	    	   for (i = 0; i < 5; i++) {
	    		   C[i][kk] = Math.round(C[i][kk]/C[5][kk]);
	    	   }
	       } // for (kk = 0; kk < k; kk++)
	       
	       // Note the residual error, E, is not calculated because we are using a
	       // fixed number of iterations 
	       
	       // Cleanup small orphaned regions and 'spurs' on each region using
	       // morphological opening on each labeled region.  The cleaned up regions are
	       // assigned to the nearest cluster. The regions are renumbered and the
	       // adjacency matrix regenerated.  This is needed because the cleanup is
	       // likely to change the number of labeled regions.
	       if (seRadius > 0) {
	           //[l, Am] = mcleanupregions(l, seRadius);
	    	   Am = mcleanupregions(l, seRadius);
	       } // if (seRadius > 0)
	    } // for (n = 1; n <= nItr; n++)
		
    }
	
	private double[][] mcleanupregions(int seg[][], double seRadius) {
		// MCLEANUPREGIONS  Morphological clean up of small segments in an image of segmented regions
		
		// Usage: [seg, Am] = mcleanupregions(seg, seRadius)
		
		// Arguments: seg - A region segmented image, such as might be produced by a
		//                  graph cut algorithm.  All pixels in each region are labeled
		//                  by an integer.
		//       seRadius - Structuring element radius.  This can be set to 0 in which
		//                  case  the function will simply ensure all labeled regions
		//                  are distinct and relabel them if necessary. 
		
		// Returns:   seg - The updated segment image.
		//             Am - Adjacency matrix of segments.  Am(i, j) indicates whether
		//                  segments labeled i and j are connected/adjacent
		
		// Typical application:
		// If a graph cut or superpixel algorithm fails to converge stray segments
		// can be left in the result.  This function tries to clean things up by:
		// 1) Checking there is only one region for each segment label. If there is
		//    more than one region they are given unique labels.
		// 2) Eliminating regions below the structuring element size
		
		// Note that regions labeled 0 are treated as a 'privileged' background region
		// and is not processed/affected by the function.
		
		// See also: REGIONADJACENCY, RENUMBERREGIONS, CLEANUPREGIONS, MAKEREGIONSDISTINCT

		// Copyright (c) 2013 Peter Kovesi
		// www.peterkovesi.com/matlabfns/
		
		// Permission is hereby granted, free of charge, to any person obtaining a copy
		// of this software and associated documentation files (the "Software"), to deal
		// in the Software without restriction, subject to the following conditions:
		 
		// The above copyright notice and this permission notice shall be included in 
		// all copies or substantial portions of the Software.
		
		// The Software is provided "as is", without warranty of any kind.
		
		// March   2013 
		// June    2013  Improved morphological cleanup process using distance map

		// function [seg, Am, mask] = mcleanupregions(seg, seRadius)
		
		int maxlabel;
		boolean se[][];
		// 1) Ensure every segment is distinct 	
		maxlabel = makeregionsdistinct(seg,4);
		
		// 2) Perform a morphological opening on each segment, subtract the opening
	    // from the orignal segment to obtain regions to be reassigned to
	    // neighbouring segments.
	    if (seRadius > 0) {
	    	// Accurate and not noticeably slower
            // if radius is small
	        se = circularstruct(seRadius);
	    }
		return null;
	}
	
	private boolean[][] circularstruct(double radius) {
		// Function to construct a circular structuring element
		// for morphological operations.
		
		// Note radius can be a floating point value though the resulting
		// circle will be a discrete approximation
		
		// Peter Kovesi   March 2000
		if (radius < 1.0) {
			System.err.println("Radius must be >= 1.0");
			System.exit(0);
		}
		
		// Diameter of structuring element
		int dia = (int)Math.ceil(2.0*radius);
		
		// If diameter is an even value, add 1 to generate a center pixel
		if ((dia % 2) == 0) {
			dia = dia + 1;
		}
		
		int r = (dia - 1)/2;
		int y,x;
		boolean strel[][] = new boolean[dia][dia];
		double radiusSquared = radius * radius;
		double rad;
		for (y = -r; y <= r; y++) {
			for (x = -r; x <= r; x++) {
			    rad = x*x + y*y;
			    if (rad <= radiusSquared) {
			    	strel[y+r][x+r] = true;
			    }
			}
		}
		return strel;
	}
	
	private int makeregionsdistinct(int seg[][], int connectivity) {
		// MAKEREGIONSDISTINCT Ensures labeled segments are distinct
		
		// Usage: [seg, maxlabel] = makeregionsdistinct(seg, connectivity)
		
		// Arguments: seg - A region segmented image, such as might be produced by a
		//                  superpixel or graph cut algorithm.  All pixels in each
		//                  region are labeled by an integer.
		//   connectivity - Optional parameter indicating whether 4 or 8 connectedness
		//                  should be used.  Defaults to 4.
		
		// Returns:   seg - A labeled image where all segments are distinct.
		//       maxlabel - Maximum segment label number.
		
		// Typical application: A graphcut or superpixel algorithm may terminate in a few
		// cases with multiple regions that have the same label.  This function
		// identifies these regions and assigns a unique label to them.
		
		// See also: SLIC, CLEANUPREGIONS, RENUMBERREGIONS

		// Copyright (c) 2013 Peter Kovesi
		// www.peterkovesi.com/matlabfns/
	 
		// Permission is hereby granted, free of charge, to any person obtaining a copy
		// of this software and associated documentation files (the "Software"), to deal
		// in the Software without restriction, subject to the following conditions:
		 
		// The above copyright notice and this permission notice shall be included in 
		// all copies or substantial portions of the Software.
		
		// The Software is provided "as is", without warranty of any kind.

		// June 2013

		// if ~exist('connectivity', 'var'), connectivity = 4; end
	    
	    // Ensure every segment is distinct but do not touch segments 
	    // with a label of 0
		int x,y,i;
		int maxlabel;
		int l;
	    //labels = unique(seg(:))';
		int yDim = seg.length;
		int xDim = seg[0].length;
		int length = xDim*yDim;
		int segArray[] = new int[length];
		int index;
		int num = 0;
		int n;
		for (y = 0; y < yDim; y++) {
			for (x = 0; x < xDim; x++) {
				index = x + y * xDim;
				segArray[index] = seg[y][x];
			}
		}
		Arrays.sort(segArray);
		int numUnique = 1;
        for (i = 1; i < length; i++) {
        	if (segArray[i] > segArray[i-1]) {
        		numUnique++;
        	}
        }
        int labels[] = new int[numUnique];
        labels[0] = segArray[0];
        for (i = 1, index = 1; i < length; i++) {
        	if (segArray[i] > segArray[i-1]) {
        		labels[index++] = segArray[i];
        	}
        }
        maxlabel = labels[labels.length-1];
        // Remove 0 from label list
        boolean hasZero = false;
        for (i = 0; i < labels.length && (!hasZero); i++) {
        	if (labels[i] == 0) {
        		hasZero = true;
        	}
        }
        if (hasZero) {
        	int tempLabel[] = new int[labels.length-1];
        	for (i = 0, index = 0; i < labels.length; i++) {
        		if (labels[i] != 0) {
        			tempLabel[index++] = labels[i];
        		}
        	}
        	labels = new int[tempLabel.length];
        	for (i = 0; i < labels.length; i++) {
        		labels[i] = tempLabel[i];
        	}
        	tempLabel = null;
        } // if (hasZero)
        
        int bl[][] = new int[yDim][xDim];
        for (i = 0; i < labels.length; i++) {
            l = labels[i];
            for (y = 0; y < yDim; y++) {
            	for (x = 0; x < xDim; x++) {
            		if (seg[y][x] == l) {
            			bl[y][x] = -1;
            		}
            		else {
            			bl[y][x] = 0;
            		}
            	}
            }
            if (connectivity == 4) {
                num = bwlabel4(bl);	
            }
            if (num > 1) {
            	// We have more than 1 region with the same label
            	for (n = 2; n <= num; n++) {
            	    // Generate a new label
            		maxlabel = maxlabel + 1;
            		// And assign to this segment
            		for (y = 0; y < yDim; y++) {
            			for (x = 0; x < xDim; x++) {
            				if (bl[y][x] == n) {
            				    seg[y][x] = maxlabel;	
            				}
            			}
            		}
            	} // for (n = 2; n <= num; n++)
            } // if (num > 1)
        } // for (i = 0; i < labels.length; i++)
        return maxlabel;
	}
	
	private int bwlabel4(int bl[][]) {
		// All values must initially be zeros and -1
		int yDim = bl.length;
		int xDim = bl[0].length;
		int newLabel = 0;
		int x,y;
		int x2,y2;
		int minY;
		int maxY;
		int minX;
		int maxX;
		int nextMinY;
		int nextMaxY;
		int nextMinX;
		int nextMaxX;
		boolean setLabel = false;
		for (y = 0; y < yDim; y++) {
			for (x = 0; x < xDim; x++) {
				if (bl[y][x] == -1) {
					newLabel++;
					bl[y][x] = newLabel;
					boolean changed = true;
					minY = Math.max(0,y-1);
					nextMinY = minY;
					maxY = Math.min(yDim-1,y+1);
					nextMaxY = maxY;
					minX = Math.max(0,x-1);
					nextMinX = minX;
					maxX = Math.max(xDim-1,x+1);
					nextMaxX = maxX;
					while (changed) {
					    changed = false;
					    for (y2 = minY; y2 <= maxY; y2++) {
					    	for (x2 = minX; x2 <= maxX; x2++) {
					    		setLabel = false;
					    		if (bl[y2][x2] == -1) {
					    			if (y2 > minY) {
					    				if (bl[y2-1][x2] == newLabel) {
					    					bl[y2][x2] = newLabel;
					    					setLabel = true;
					    					changed = true;
					    					if ((y2 == maxY) && (maxY < yDim-1)) {
					    						nextMaxY = maxY+1;
					    					}
					    				}
					    			}
					    			if (!setLabel) {
					    				if (y2 < maxY) {
					    					if (bl[y2+1][x2] == newLabel) {
					    						bl[y2][x2] = newLabel;
					    						setLabel = true;
					    						changed = true;
					    						if ((y2 == minY) && (minY > 0)) {
					    							nextMinY = minY-1;
					    						}
					    					}
					    				}
					    			}
					    			if (!setLabel) {
					    				if (x2 > minX) {
					    					if (bl[y2][x2-1] == newLabel) {
					    						bl[y2][x2] = newLabel;
					    						setLabel = true;
					    						changed = true;
					    						if ((x2 == maxX) && (maxX < xDim-1)) {
					    							nextMaxX = maxX + 1;
					    						}
					    					}
					    				}
					    			}
					    			if (!setLabel) {
					    				if (x2 < maxX) {
					    					if (bl[y2][x2+1] == newLabel) {
					    						bl[y2][x2] = newLabel;
					    						changed = true;
					    						if ((x2 == minX) && (minX > 0)) {
					    							nextMinX = minX-1;
					    						}
					    					}
					    				}
					    			}
					    		} // if (bl[y2][x2] == -1)
					    	}
					    }
					    if (changed) {
					    	minX = nextMinX;
					    	maxX = nextMaxX;
					    	minY = nextMinY;
					    	maxY = nextMaxY;
					    }
					} // while (changed)
				} 
			}
		}
		return newLabel;
	}
	
	private double[][] dist(double C[], double im[][][], int r1, int c1, double S, double m) {
		// Arguments:   C - Cluster being considered
		//             im - sub-image surrounding cluster centre
		//         r1, c1 - row and column of top left corner of sub image within the
		//                  overall image.
		//              S - grid spacing
		//              m - weighting factor between colour and spatial differences.
		
		// Returns:     D - Distance image giving distance of every pixel in the
		//                  subimage from the cluster centre
		
		// Distance = sqrt( dc^2 + (ds/S)^2*m^2 )
		// where:
		// dc = sqrt(dl^2 + da^2 + db^2)  % Colour distance
		// ds = sqrt(dx^2 + dy^2)         % Spatial distance
		
		// m is a weighting factor representing the nominal maximum colour distance
		// expected so that one can rank colour similarity relative to distance
		// similarity.  try m in the range [1-40] for L*a*b* space
		
		// ?? Might be worth trying the Geometric Mean instead ??
		//  Distance = sqrt(dc * ds)
		// but having a factor 'm' to play with is probably handy

		// This code could be more efficient

		// Squared spatial distance
		// ds is a fixed 'image' we should be able to exploit this
		// and use a fixed meshgrid for much of the time somehow...	
		int x,y,n;
		double diff;
		int rows = im.length;
		int cols = im[0].length;
		double X[][] = new double[rows][cols];
		double Y[][] = new double[rows][cols];
		double ds2[][] = new double[rows][cols];
		double dc2[][] = new double[rows][cols];
		double D[][] = new double[rows][cols];
		double Ssquared = S*S;
		double msquared = m*m;
		for (y = 0; y < rows; y++) {
			for (x = 0; x < cols; x++) {
				X[y][x] = c1 + x;
				Y[y][x] = r1 + y;
				// x and y dist form cluster center
				X[y][x] = X[y][x] - C[3];
				Y[y][x] = Y[y][x] - C[4];
				ds2[y][x] = X[y][x]*X[y][x] + Y[y][x]*Y[y][x];
			}
		}
		
		// Squared color differences
		for (n = 0; n < 3; n++) {
			for (y = 0; y < rows; y++) {
				for (x = 0; x < cols; x++) {
					diff = im[y][x][n] - C[n];
					dc2[y][x] += (diff*diff);
				}
			}
		}
		
		for (y = 0; y < rows; y++) {
			for (x = 0; x < cols; x++) {
				D[y][x] = Math.sqrt(dc2[y][x] + ds2[y][x]/Ssquared*msquared);
			}
		}
		return D;
		
	}
	
	
}