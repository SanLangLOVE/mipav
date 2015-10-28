package gov.nih.mipav.model.algorithms;

import gov.nih.mipav.model.structures.*;
import gov.nih.mipav.view.*;

import java.io.*;
import java.util.*;

public class AlgorithmFuzzyConnectednessSegmentation extends AlgorithmBase {
	/**
	 * This code is ported from MATLAB routines adjacency, affinity, afc, and irfc written by Joakim Lindblad
	 * References:
	 * 1.) "Fuzzy Connectedness and Object Definition: Theory, Algorithms, and Applications in Image Segmentation"
	 *     by Jayaram K. Udupa and Supun Samarasekera, Graphical Models and Image Processing, Vol. 58, No. 3,
	 *     May, 1996, pp. 246-261.
	 * 2.) "Fuzzy Connected Object Delineation: Axiomatic Path Strength Definition and the Case of Multiple Seeds"
	 *     by Punam K. Saha and Jayaram K. Udupa, Computer Vision and Image Understanding, 83, 2001, pp. 275-295. 
	 * 3.) "Iterative relative fuzzy connectedness for multiple objects with multiple seeds" by 
	 *     Krzysztof Chris Ciesielski, Jayaram K. Udupa, Punam K. Saha, and Ying Zhuge, Computer Vision and Image
	 *     Understanding, 107, 2007, pp. 160-182.
	 */
	
	//~ Static fields/initializers -------------------------------------------------------------------------------------
	private static int ABSOLUTE_FUZZY_CONNECTEDNESS = 1;
	private static int ITERATIVE_RELATIVE_FUZZY_CONNECTEDNESS = 2;
	
    //~ Instance fields ------------------------------------------------------------------------------------------------
	private int L1Distance; // L1 distance of neighborhood, L1Distance = 1 in segmentation example
	
	private double distanceDecline; // distance decline factor, distanceDecline = 0.1 in segmentation example
	
	private double gradientWeight; // gradientWeight = 2.0 in segmentation example
	
	// The index in the image array of the seed
    private Vector<Integer> index_seeds;
    
    
	// Seed labels, are numbered from 1 and up
    // In example coat = 1, hand = 1, leg = 1, sky = 2, grass = 3
    private Vector<Short> index_labels;
    
    private int method;
    
    private int sliceSize;
    private boolean error = false;
    private ViewUserInterface UI = ViewUserInterface.getReference();
	
	//~ Constructors ---------------------------------------------------------------------------------------------------

	
	public AlgorithmFuzzyConnectednessSegmentation(ModelImage destImg, ModelImage srcImg, 
			int method, int L1Distance, double distanceDecline,
			double gradientWeight, Vector<Integer> index_seeds, Vector<Short> index_labels) {
		super(destImg, srcImg);
		this.method = method;
		this.L1Distance = L1Distance;
		this.distanceDecline = distanceDecline;
		this.gradientWeight = gradientWeight;
		this.index_seeds = index_seeds;
		this.index_labels = index_labels;
	}
	
	/**
     * Prepares this class for destruction.
     */
    public void finalize() {
        destImage = null;
        srcImage = null;

        
        super.finalize();
    }
    
    /**
     * Starts the program.
     */
    public void runAlgorithm() {
    	int xDim = srcImage.getExtents()[0];
        int yDim = srcImage.getExtents()[1];
        sliceSize = xDim * yDim;
        int x, y;
        int xa, ya;
        Vector<Integer>index1 = new Vector<Integer>();
        Vector<Integer>index2 = new Vector<Integer>();
        Vector<Double>affinity = new Vector<Double>();
        int yDist;
        int xDist;
        double adjacency;
        double aff;
        double f[] = new double[sliceSize];
        int p1;
        int p2;
        int i;
        double FC[] = null;
        double fmin;
        double fmax;
        double FCmin;
        double FCmax;
        double a;
        double b;
        short Sout[] = null;
        
        fireProgressStateChanged("Graph Based Segmentation ...");
        
        // Read image and scale to [0,1]
        try {
        	srcImage.exportData(0, sliceSize, f);
        }
        catch (IOException e) {
        	MipavUtil.displayError("IOException " + e + " on srcImage.exportData(0, sliceSize, f)");
        	setCompleted(false);
        	return;
        }
        
        
        fmin = srcImage.getMin();
        fmax = srcImage.getMax();
        // Scale to 0, 1
        // a * fmax + b = 1
        // a * fmin + b = 0
        // a * (fmax - Fmin) = 1  
        a = 1.0/(fmax - fmin);
        b = -fmin/(fmax - fmin);
        for (i = 0; i < sliceSize; i++) {
        	f[i] = a * f[i] + b;
        }
        
        // Compute adjacency according to (2.8) in Udupa '96.
        // Compute affinity according to (2.9) in Udupa '96.
        for (y = 0; y < yDim; y++) {
        	for (x = 0; x < xDim; x++) {
        		p1 = x + y * xDim;
        	    for (ya = Math.max(0, y - L1Distance); ya <= Math.min(yDim-1, y + L1Distance); ya++) {
        	        yDist = Math.abs(y - ya);
        	        xDist = L1Distance - yDist;
        	        for (xa = Math.max(0, x - xDist); xa <= Math.min(xDim-1, x + xDist); xa++) {
        	        	p2 = xa + ya * xDim;
        	            adjacency = 1.0/(1.0 + distanceDecline*Math.sqrt(yDist*yDist + xDist*xDist));
        	            aff = adjacency/(1.0 + gradientWeight*Math.abs(f[p1] -f[p2]));
        	            index1.add(p1);
        	            index2.add(p2);
        	            affinity.add(aff);
        	        }
        	    }
        	}
        }
        
        if (method == ABSOLUTE_FUZZY_CONNECTEDNESS) {
        	FC = afc(index_seeds, index_labels, index1, index2, affinity);
        	FCmin = Double.MAX_VALUE;
            FCmax = -Double.MAX_VALUE;
            for (i = 0; i < sliceSize; i++) {
            	if (FC[i] < FCmin) {
            		FCmin = FC[i];
            	}
            	if (FC[i] > FCmax) {
            		FCmax = FC[i];
            	}
            }
            // Scale to 0, 1
            // a * Fcmax + b = 1
            // a * Fcmin + b = 0
            // a * (Fcmax - Fcmin) = 1
            a = 1.0/(FCmax - FCmin);
            b = -FCmin/(FCmax - FCmin);
            for (i = 0; i < sliceSize; i++) {
            	FC[i] = a * FC[i] + b;
            }
            
            try {
            	destImage.importData(0, FC, true);
            }
            catch (IOException e) {
            	MipavUtil.displayError("IOException " + e + " on destImage.importData(0, FC, true)");
            	setCompleted(false);
            	return;
            }
        }
        else { // ITERATIVE_RELATIVE_FUZZY_CONNECTEDNESS
        	Sout = irfc(index_seeds, index_labels, index1, index2, affinity);
        	try {
            	destImage.importData(0, Sout, true);
            }
            catch (IOException e) {
            	MipavUtil.displayError("IOException " + e + " on destImage.importData(0, Sout, true)");
            	setCompleted(false);
            	return;
            }
        	
        }
        		
        setCompleted(true);
        return;
    }
    
    private double[] afc(Vector<Integer> index_seeds, Vector<Short>index_labels, Vector<Integer>index1,
    		Vector<Integer>index2, Vector<Double>affinity) {
    	// Absolute Fuzzy Connectedness (kFOEMS) according to Saha and Udupa 2001
        // Dijkstra version
        // Pushing values instead of pulling
    	int i;
    	double FC[] = new double[sliceSize];
    	double fc;
    	Vector<Integer>pick = new Vector<Integer>();
    	int j;
    	double maxK;
    	boolean havePick;
    	int k;
    	double f[] = new double[sliceSize];
        
        for (i = 0; i < index_seeds.size(); i++) {
        	FC[index_seeds.get(i)] = index_labels.get(i);
        }
        
        while(!index_seeds.isEmpty()) {
        	// Pick strongest fc kin index_seeds
            fc = FC[index_seeds.get(0)];
            for (i = 1; i < index_seeds.size(); i++) {
            	if (FC[index_seeds.get(i)] > fc) {
            		fc = FC[index_seeds.get(i)];
            	} 
            } // for (i = 1; i < index_seeds.size(); i++)
            // Find all of the same fc and remove from index_seeds
            pick.clear();
            for (i = index_seeds.size()-1; i >= 0; i--) {
            	if (fc == FC[index_seeds.get(i)]) {
            		pick.add(index_seeds.get(i));
            		index_seeds.remove(i);
            	}
            }
            
            // Propagate affinity one layer
            // Compute fc for adjacent pixels
            j = 0;
            for (i = 0; i < sliceSize; i++) {
            	maxK = -Double.MAX_VALUE;
            	while (index1.get(j) == i) {
            		havePick = false;
            		for (k = 0; k < pick.size() && (!havePick); k++) {
            			if (pick.get(k) == index2.get(j)) {
            				havePick = true;
            		        maxK = Math.max(affinity.get(j), maxK);
            			}
            		} // for (k = 0; k < pick.size(); k++)
            		j++;
            	} // while (index1.get(j) == i)
            	f[i] = Math.min(fc, maxK);
            } // for (i = 0; i < sliceSize; i++)
            // Find those with real change
            // Update FC
            // Push all updated pixel indices
            for (i = 0; i < sliceSize; i++) {
                if (f[i] > FC[i]) {	
                	FC[i] = f[i];
                	index_seeds.add(i);
                }
            } // for (i = 0; i < sliceSize; i++)
        } //  while(!index_seeds.isEmpty())
        return FC;
    	
    }
    
    private short[] irfc(Vector<Integer> index_seeds, Vector<Short>index_labels, Vector<Integer>index1,
    		Vector<Integer>index2, Vector<Double>affinity) {
    	// Iterative Relative Fuzzy Connectedness (kIRMOFC) according to Ciesielski et al 2007
    	// Sout is segmented into same classes as index_labels
    	// Number of classes
    	int n;
    	int i;
    	int j;
    	short Sout[] = new short[sliceSize];
    	Vector<Integer> index_seeds_i = new Vector<Integer>();
    	Vector<Short> index_labels_i = new Vector<Short>();
    	Vector<Integer> index_seeds_W = new Vector<Integer>();
    	Vector<Short> index_labels_W = new Vector<Short>();
    	short Fi[] = new short[sliceSize];
    	double FCs[];
    	Vector<Integer>index1s = new Vector<Integer>();
    	Vector<Integer>index2s = new Vector<Integer>();
    	Vector<Double>affinitys = new Vector<Double>();
    	int count;
    	double FCw[];
    	
    	n = index_labels.get(0);
    	for (i = 1; i < index_labels.size(); i++) {
    		if (index_labels.get(i) > n) {
    			n = index_labels.get(i);
    		}
    	} // for (i = 1; i < index_labels.size(); i++)
    	
    	for (i = 1; i <= n; i++) {
    	    UI.setDataText("Class " + i + "\n");
    	    index_seeds_i.clear();
    	    index_labels_i.clear();
    	    index_seeds_W.clear();
    	    index_labels_W.clear();
    	    for (j = 0; j < index_labels.size(); j++) {
    	    	if (index_labels.get(j) == i) {
    	    		index_labels_i.add(index_labels.get(j));
    	    		index_seeds_i.add(index_seeds.get(j));
    	    	}
    	    	else {
    	    		index_labels_W.add(index_labels.get(j));
    	    		index_seeds_W.add(index_seeds.get(j));
    	    	}
    	    } // for (j = 0; j < index_labels.size(); j++)
    	    
    	    for (j = 0; j < sliceSize; j++) {
    	    	Fi[j] = 0;
    	    }
    	    
    	    FCs = afc(index_seeds_i, index_labels_i, index1, index2, affinity);
    	    index1s.clear();
    	    index2s.clear();
    	    affinitys.clear();
    	    index1s = (Vector<Integer>)index1.clone();
    	    index2s = (Vector<Integer>)index2.clone();
    	    affinitys = (Vector<Double>)affinity.clone();
    	    count = 0;
    	    while (true) {
    	        FCw = afc(index_seeds_W, index_labels_W, index1s, index2s, affinitys);	
    	    } // while (true)
    	} // for (i = 1; i <= n; i++)
    	
    	return Sout;
    }
}