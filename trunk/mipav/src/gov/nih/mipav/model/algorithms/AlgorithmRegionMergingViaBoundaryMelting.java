package gov.nih.mipav.model.algorithms;

import java.io.IOException;

import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.view.MipavUtil;

/**
 * 
 * @author ilb
 * Reference:
 * 1.) Image Processing, Analysis, and Machine Vision 4th edition International Edition by Milan Sonka,
 * Vaclav Hlavac, and Roger Boyle, Section 6.3.1 Region merging, pp. 221-224.
 */

public class AlgorithmRegionMergingViaBoundaryMelting extends AlgorithmBase {
	
	// Three thresholds
	private double t1;
	
	private double t2;
	
	private double t3;
	
	//~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Creates a new AlgorithmRegionMergingViaBoundaryMelting object.
     *
     * @param  destImg
     * @param  srcImg  source image model
     * @param  t1
     * @param  t2
     * @param  t3
     */
    public AlgorithmRegionMergingViaBoundaryMelting(ModelImage destImg, ModelImage srcImg, double t1,
    		double t2, double t3) {
        super(destImg, srcImg);
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
    }
    
    public void runAlgorithm() {
    	int xDim;
    	int yDim;
    	int zDim;
    	int tDim;
    	int nDims;
    	int length;
    	double imgBuffer[];
    	byte wallX[];
    	byte wallY[];
    	int regions[];
    	int x;
    	int y;
    	int z;
    	int t;
    	double diffX;
    	double diffY;
    	int regionNum;
    	boolean newRegion;
    	boolean growRegion;
    	int perimiters[];
    	int currentNum;
    	int weak[][];
    	int common[][];
    	int neighborNum;
    	int i;
    	int j;
    	boolean boundaryRemoved;
    	int numberRemoved = 0;
    	
    	if (srcImage == null) {
            displayError("Source Image is null");
            setCompleted(false);

            return;
        }
    	
    	fireProgressStateChanged(0, srcImage.getImageName(), "Region Merging Via Boundary Melting ...");
    	
    	xDim = srcImage.getExtents()[0];
        yDim = srcImage.getExtents()[1];
        length = xDim * yDim;
        nDims = srcImage.getNDims();
    	
        if (nDims > 2) {
            zDim = srcImage.getExtents()[2];
        } else {
            zDim = 1;
        }
        if (nDims > 3) {
        	tDim = srcImage.getExtents()[3];
        }
        else {
        	tDim = 1;
        }
        
        try {
            imgBuffer = new double[length];
            wallX = new byte[length];
            wallY = new byte[length];
            regions = new int[length];
        } catch (OutOfMemoryError e) {
            displayError("Algorithm VOIExtraction: Out of memory creating buffers");
            setCompleted(false);

            return;
        }

        for (t = 0; t < tDim; t++) {
        for (z = 0; z < zDim; z++) {

            try {
                srcImage.exportData((z + t*zDim)*length, length, imgBuffer);
            } catch (IOException error) {
                displayError("Algorithm VOI Extraction: image bounds exceeded");
                setCompleted(false);
                
                srcImage.releaseLock();

                return;
            }
            
            for (y = 0; y < yDim; y++) {
            	for (x = 0; x < xDim; x++) {
            		regions[x + y*xDim] = -1;
            		if (x < xDim - 1) {
            			diffX = Math.abs(imgBuffer[x+1 + y*xDim] - imgBuffer[x + y*xDim]);
            			if (diffX >= t1) {
            			    wallX[x + y*xDim] = 1;
            			}
            			else {
            				wallX[x + y*xDim] = 0;
            			}
            		}
            		if (y < yDim - 1) {
            			diffY = Math.abs(imgBuffer[x + (y+1)*xDim] - imgBuffer[x + y*xDim]);
            			if (diffY >= t1) {
            				wallY[x + y*xDim] = 1;	
            			}
            			else {
            				wallY[x + y*xDim] = 0;		
            			}
            		}
            	} // for (x = 0; x < xDim; x++)
            } // for (y = 0; y < yDim; y++)
            
            regionNum = 0;
            newRegion = true;
            while (newRegion) {
            	newRegion = false;
	            for (y = 0; y < yDim && (!newRegion); y++) {
	            	for (x = 0; x < xDim && (!newRegion); x++) {
	            		if (regions[x + y*xDim] == -1) {
	            			regions[x + y*xDim] = regionNum++;
	            			newRegion = true;
	            		} // if (regions[x + y*xDim] == -1) 
	            	} // for (x = 0; x < xDim && (!newRegion); x++)
	            } // for (y = 0; y < yDim && (!newRegion); y++)
	            
	            if (newRegion) {
		            growRegion = true;
		            while (growRegion) {
		            	growRegion = false;
		            	for (y = 0; y < yDim; y++) {
		            		for (x = 0; x < xDim; x++) {
		            		    if (x < xDim-1) {
		            		    	if ((regions[x + y*xDim] != -1) && (regions[x+1 + y*xDim] == -1) &&
		            		    		 (imgBuffer[x + y*xDim] == imgBuffer[x+1 + y*xDim])) {
		            		    		regions[x+1 + y*xDim] = regions[x + y*xDim];
		            		    		growRegion = true;
		            		    	}
		            		    } // if (x < xDim - 1)
		            		    if (y < yDim-1) {
		            		    	if ((regions[x + y*xDim] != -1) && (regions[x + (y+1)*xDim] == -1) &&
		            		    			(imgBuffer[x + y*xDim] == imgBuffer[x + (y+1)*xDim])) {
		            		    		regions[x + (y+1)*xDim] = regions[x + y*xDim];
		            		    		growRegion = true;
		            		    	}
		            		    } // if (y < yDim-1)
		            		} // for (x = 0; x < xDim; x++)
		            	} // for (y = 0; y < yDim; y++)
		            } // while (growRegion)
	            } // if (newRegion)
            } // while (newRegion)
            System.out.println(regionNum + " regions initially found for z = " + z + " t = " + t);
            
            boundaryRemoved = true;
            while (boundaryRemoved) {
            	boundaryRemoved = false;
	            perimiters = new int[regionNum];
	            weak = new int[regionNum][regionNum];
	            for (y = 0; y < yDim; y++) {
	            	for (x = 0; x < xDim; x++) {
	            	    currentNum = regions[x + y * xDim];
	            	    if (x == 0) {
	            	    	perimiters[currentNum]++;
	            	    }
	            	    else if (x == xDim-1) {
	            	    	perimiters[currentNum]++;
	            	    }
	            	    if (y == 0) {
	            	    	perimiters[currentNum]++;
	            	    }
	            	    else if (y == yDim-1) {
	            	    	perimiters[currentNum]++;
	            	    }
	            	    if ((x > 0) && (regions[x-1 + y *xDim] != currentNum)) {
	            	    	perimiters[currentNum]++;
	            	    }
	            	    if ((x < xDim-1) && (regions[x+1 + y * xDim] != currentNum)) {
	            	    	perimiters[currentNum]++;
	            	    	if (wallX[x + y*xDim] == 0) {
	            	    	    neighborNum = regions[x+1 + y*xDim];
	            	    	    weak[Math.min(currentNum,neighborNum)][Math.max(currentNum,neighborNum)]++;
	            	    	}
	            	    }
	            	    if ((y > 0) && (regions[x + (y-1)*xDim] != currentNum)) {
	            	    	perimiters[currentNum]++;
	            	    }
	            	    if ((y < yDim-1) && (regions[x + (y+1)*xDim] != currentNum)) {
	            	    	perimiters[currentNum]++;
	            	    	if (wallY[x + y*xDim] == 0) {
	            	    	    neighborNum = regions[x + (y+1)*xDim];
	            	    	    weak[Math.min(currentNum,neighborNum)][Math.max(currentNum,neighborNum)]++;
	            	    	}
	            	    }
	            	} // for (x = 0; x < xDim; x++)
	            } // for (y = 0; y < yDim; y++)
	            
	            for (i = 0; i < regionNum-1 && (!boundaryRemoved); i++) {
	            	for (j = i+1; j < regionNum && (!boundaryRemoved); j++) {
	            		if (weak[i][j] > 0) {
	            			if ((weak[i][j]/Math.min(perimiters[i], perimiters[j])) >= t2) {
	            				boundaryRemoved = true;
	            				numberRemoved = j;
	            			    for (y = 0; y < yDim; y++) {
	            			    	for (x = 0; x < xDim; x++) {
	            			    		if (x < xDim-1) {
	            			    			if (wallX[x+y*xDim] == 1) {
	            			    				currentNum = regions[x + y*xDim];
	            			    				neighborNum = regions[x+1 + y*xDim];
	            			    				if (((currentNum == i) && (neighborNum == j)) ||
	            			    				    ((currentNum == j) && (neighborNum == i))) {
	            			    					wallX[x+y*xDim] = 0;
	            			    					if (regions[x + y*xDim] == j) {
	            			    						regions[x + y*xDim] = i;
	            			    					}
	            			    					else {
	            			    						regions[(x+1) + y*xDim] = i;
	            			    					}
	            			    				}
	            			    			} // if (wallX[x+y*xDim] == 1)
	            			    		} // if (x < xDim-1)
	            			    		if (y < yDim-1) {
	            			    			if (wallY[x+y*xDim] == 1) {
	            			    				currentNum = regions[x + y*xDim];
	            			    				neighborNum = regions[x+1 + y*xDim];
	            			    				if (((currentNum == i) && (neighborNum == j)) ||
	            			    				    ((currentNum == j) && (neighborNum == i))) {
	            			    					wallY[x+y*xDim] = 0;
	            			    					if (regions[x + y*xDim] == j) {
	            			    						regions[x + y*xDim] = i;
	            			    					}
	            			    					else {
	            			    						regions[x + (y+1)*xDim] = i;
	            			    					}
	            			    				}
	            			    			} // if (wallY[x+y*xDim] == 1)
	            			    		} // if (y < yDim-1)
	            			    	}
	            			    }
	            			}
	            		}
	            	}
	            }
	            
	            if (boundaryRemoved) {
	            	regionNum--;
	            	for (y = 0; y < yDim; y++) {
	            		for (x = 0; x < xDim; x++) {
	            			if (regions[x + y*xDim] >= numberRemoved) {
	            				regions[x + y*xDim]--;
	            			}
	            		}
	            	}
	            }
            } // while (boundaryRemoved)
            
            System.out.println(regionNum + " regions after applying threshold 2 for z = " + z + " t = " + t);
            
            boundaryRemoved = true;
            while (boundaryRemoved) {
            	boundaryRemoved = false;
	            common = new int[regionNum][regionNum];
	            weak = new int[regionNum][regionNum];
	            for (y = 0; y < yDim; y++) {
	            	for (x = 0; x < xDim; x++) {
	            	    currentNum = regions[x + y * xDim];
	            	    if ((x < xDim-1) && (regions[x+1 + y * xDim] != currentNum)) {
	            	    	neighborNum = regions[x+1 + y*xDim];
	            	    	common[Math.min(currentNum,neighborNum)][Math.max(currentNum,neighborNum)]++;
	            	    	if (wallX[x + y*xDim] == 0) {
	            	    	    weak[Math.min(currentNum,neighborNum)][Math.max(currentNum,neighborNum)]++;
	            	    	}
	            	    }
	            	    
	            	    if ((y < yDim-1) && (regions[x + (y+1)*xDim] != currentNum)) {
	            	    	neighborNum = regions[x + (y+1)*xDim];
	            	    	common[Math.min(currentNum,neighborNum)][Math.max(currentNum,neighborNum)]++;
	            	    	if (wallY[x + y*xDim] == 0) {
	            	    	    weak[Math.min(currentNum,neighborNum)][Math.max(currentNum,neighborNum)]++;
	            	    	}
	            	    }
	            	} // for (x = 0; x < xDim; x++)
	            } // for (y = 0; y < yDim; y++)
	            
	            for (i = 0; i < regionNum-1 && (!boundaryRemoved); i++) {
	            	for (j = i+1; j < regionNum && (!boundaryRemoved); j++) {
	            		if (weak[i][j] > 0) {
	            			if ((weak[i][j]/common[i][j]) >= t3) {
	            				boundaryRemoved = true;
	            				numberRemoved = j;
	            			    for (y = 0; y < yDim; y++) {
	            			    	for (x = 0; x < xDim; x++) {
	            			    		if (x < xDim-1) {
	            			    			if (wallX[x+y*xDim] == 1) {
	            			    				currentNum = regions[x + y*xDim];
	            			    				neighborNum = regions[x+1 + y*xDim];
	            			    				if (((currentNum == i) && (neighborNum == j)) ||
	            			    				    ((currentNum == j) && (neighborNum == i))) {
	            			    					wallX[x+y*xDim] = 0;
	            			    					if (regions[x + y*xDim] == j) {
	            			    						regions[x + y*xDim] = i;
	            			    					}
	            			    					else {
	            			    						regions[(x+1) + y*xDim] = i;
	            			    					}
	            			    				}
	            			    			} // if (wallX[x+y*xDim] == 1)
	            			    		} // if (x < xDim-1)
	            			    		if (y < yDim-1) {
	            			    			if (wallY[x+y*xDim] == 1) {
	            			    				currentNum = regions[x + y*xDim];
	            			    				neighborNum = regions[x+1 + y*xDim];
	            			    				if (((currentNum == i) && (neighborNum == j)) ||
	            			    				    ((currentNum == j) && (neighborNum == i))) {
	            			    					wallY[x+y*xDim] = 0;
	            			    					if (regions[x + y*xDim] == j) {
	            			    						regions[x + y*xDim] = i;
	            			    					}
	            			    					else {
	            			    						regions[x + (y+1)*xDim] = i;
	            			    					}
	            			    				}
	            			    			} // if (wallY[x+y*xDim] == 1)
	            			    		} // if (y < yDim-1)
	            			    	}
	            			    }
	            			}
	            		}
	            	}
	            }
	            
	            if (boundaryRemoved) {
	            	regionNum--;
	            	for (y = 0; y < yDim; y++) {
	            		for (x = 0; x < xDim; x++) {
	            			if (regions[x + y*xDim] >= numberRemoved) {
	            				regions[x + y*xDim]--;
	            			}
	            		}
	            	}
	            }
            } // while (boundaryRemoved)
            
            System.out.println(regionNum + " regions after applying threshold 3 for z = " + z + " t = " + t);
            
            try {
			    destImage.importData((z + t*zDim)*length, regions, false);
			}
			catch(IOException e) {
				MipavUtil.displayError("IOException " + e + " on destImage.importData");
				setCompleted(false);
				return;
			}
        } // for (z = 0; z < zDim; z++)
        } // for (t = 0; t < tDim; t++)
        destImage.calcMinMax();
        
        setCompleted(true);
        return;
    }
	
}