package gov.nih.mipav.model.algorithms;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.jtem.numericalMethods.algebra.linear.decompose.Eigenvalue;
import gov.nih.mipav.model.algorithms.SchwarzChristoffelMapping.dpfun;
import gov.nih.mipav.model.algorithms.SchwarzChristoffelMapping.polygon;
import gov.nih.mipav.model.algorithms.SchwarzChristoffelMapping.qlgraph;
import gov.nih.mipav.model.algorithms.SchwarzChristoffelMapping.scmap;
import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.model.structures.jama.ComplexLinearEquations;
import gov.nih.mipav.model.structures.jama.GeneralizedEigenvalue;
import gov.nih.mipav.model.structures.jama.LinearEquations2;
import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.Preferences;
import gov.nih.mipav.view.ViewJComponentBase;
import gov.nih.mipav.view.ViewJComponentGraph;
import gov.nih.mipav.view.ViewJFrameGraph;

public class SchwarzChristoffelMapping2 extends AlgorithmBase {

	// This is a port of portions of the Schwarz-Christoffel Toolbox from MATLAB
	// to Java
	// with the kind permission of Professor Toby Driscoll. The original code
	// is:
	// Version 2.3 January 15, 2003
	// Copyright (c) 1994-2003 by Toby Driscoll (driscoll@math.udel.edu).

	// How much progress information to show during and after the solution to
	// the parameter problem.
	// private boolean traceSolution = false;

	// Desired accuracy in the map. This may not be met exactly.
	private double tolerance = 1.0E-8;

	// eps returns the distance from 1.0 to the next larger double-precision
	// number, that is, eps = 2^-52.
	private double eps;

	private double w[][];

	private boolean testRoutine = true;

	private SchwarzChristoffelMapping scm = new SchwarzChristoffelMapping();

	public SchwarzChristoffelMapping2() {

	}

	public SchwarzChristoffelMapping2(ModelImage destImg, ModelImage srcImg, double w[][]) {
		super(destImg, srcImg);
		this.w = w;
	}

	public void runAlgorithm() {
		// eps returns the distance from 1.0 to the next larger double-precision
		// number, that is, eps = 2^-52.
		// epsilon = D1MACH(4)
		// Machine epsilon is the smallest positive epsilon such that
		// (1.0 + epsilon) != 1.0.
		// epsilon = 2**(1 - doubleDigits) = 2**(1 - 53) = 2**(-52)
		// epsilon = 2.2204460e-16
		// epsilon is called the largest relative spacing
		eps = 1.0;
		double neweps = 1.0;

		while (true) {

			if (1.0 == (1.0 + neweps)) {
				break;
			} else {
				eps = neweps;
				neweps = neweps / 2.0;
			}
		} // while(true)
		scm.setEps(eps);

		if (testRoutine) {

			return;
		}

	}

	public scmap hplmap(polygon poly) {
		// hplmap constructs a Schwarz-Christoffel half-plane object for the
		// polygon poly.
		// The parameter problem is solved using default options for the
		// prevertices and
		// the multiplicative constant.
		// Dervied from original hlpmap MATLAB rotuine copyright 1998-2001 by
		// Toby Driscoll.
		double z[][] = null;
		int i, j;
		double wn[][];
		double betan[];
		double alpha2[];
		int nqpts;
		double qdata[][] = null;
		double c[] = null;
		double z0[] = null;

		// Get data for the low-level functions
		double w[][] = poly.vertex;
		double beta[] = new double[poly.angle.length];
		for (i = 0; i < poly.angle.length; i++) {
			beta[i] = poly.angle[i] - 1.0;
		}

		scmap map = scm.new scmap();

		if ((z == null) || (z.length == 0)) {
			wn = new double[w.length + 2][2];
			betan = new double[w.length + 2];
			// Number of vertices added by scfix
			int verticesAdded[] = new int[1];
			int initialVertices = w.length;
			for (i = 0; i < w.length; i++) {
				wn[i][0] = w[i][0];
				wn[i][1] = w[i][1];
				betan[i] = beta[i];
			}
			scm.scfix(wn, betan, verticesAdded, null, "hp", w, beta, null);
			double wn2[][];
			double betan2[];
			if ((verticesAdded[0] == 0) || (verticesAdded[0] == 1)) {
				wn2 = new double[initialVertices + verticesAdded[0]][2];
				betan2 = new double[initialVertices + verticesAdded[0]];
				for (i = 0; i < initialVertices + verticesAdded[0]; i++) {
					wn2[i][0] = wn[i][0];
					wn2[i][1] = wn[i][1];
					betan2[i] = betan[i];
				}
			} else {
				wn2 = wn;
				betan2 = betan;
			}
			double xn[] = new double[wn2.length];
			double yn[] = new double[wn2.length];
			for (i = 0; i < wn2.length; i++) {
				xn[i] = wn2[i][0];
				yn[i] = wn2[i][1];
			}
			alpha2 = new double[betan2.length];
			for (i = 0; i < betan2.length; i++) {
				alpha2[i] = betan2[i] + 1.0;
			}
			poly = scm.new polygon(xn, yn, alpha2);
			c = new double[2];
			z = new double[wn2.length][2];
			nqpts = Math.max((int) Math.ceil(-Math.log10(tolerance)), 4);
			qdata = new double[nqpts][2 * betan2.length + 2];
			hpparam(z, c, qdata, wn2, betan2, z0, tolerance);
		} // if ((z == null) || (z.length == 0))
		map.prevertex = z;
		map.constant = c;
		map.qdata = qdata;

		// Now fill in apparent accuracy
		return map;
	}

	private void hpparam(double z[][], double c[], double qdat[][], double w[][], double beta[], double z0[],
			double tol) {
		// hpparam solves the Schwarz-Christoffel parameter problem with the
		// upper half-plane
		// as fundamental domain and the interior of the polygon as the target.
		// w must be a
		// vector of the vertices of the polygon, specified in counterclockwise
		// order. beta
		// is a vector of turning angles; see scangles. If successful, hpparam
		// will return z,
		// a vector of pre-images of w; c, the multiplicative constant of the
		// conformal map;
		// and qdat, an optional matrix of quadrature data used by some of the
		// other routines.
		// If z0 is supplied, it is used as an initial guess for z. hpparam
		// attempts to find
		// an answer within tolerance tol.
		// Original hpparam MATLAB routine copyright 1998-2001 by Toby Driscoll.
		int i, j, k;
		double cr[] = new double[1];
		double ci[] = new double[1];
		int n = w.length; // number of vertices

		// Check input data
		int err = scm.sccheck("hp", w, beta, null);
		if (err == -1) {
			return;
		}
		if (err == 1) {
			MipavUtil.displayError("Use scfix to make polygon obey requirements");
			return;
		}

		int nqpts = (int) Math.max(Math.ceil(-Math.log10(tol)), 4);
		scm.scqdata(qdat, beta, nqpts); // quadrature data

		boolean atinf[] = new boolean[beta.length];
		for (i = 0; i < beta.length; i++) {
			if (beta[i] <= -1) {
				atinf[i] = true;
			}
		} // for (i = 0; i < beta.length; i++)

		// Find prevertices (solve param problem)
		if (n == 3) {
			z[0][0] = -1;
			z[0][1] = 0;
			z[1][0] = 1;
			z[1][1] = 0;
			z[2][0] = Double.POSITIVE_INFINITY;
			z[2][1] = 0;
		} // if (n == 3)
		else { // n != 3
				// Set up normalized lengths for nonlinear equations:
				// indices of left and right integration endpoints
			int numleft = 0;
			for (i = 0; i < n - 2; i++) {
				if (!atinf[i]) {
					numleft++;
				}
			}
			int left[] = new int[numleft];
			for (i = 0, j = 0; i < n - 2; i++) {
				if (!atinf[i]) {
					left[j++] = i;
				}
			}
			// numright should equal numleft
			int numright = 0;
			for (i = 1; i < n - 1; i++) {
				if (!atinf[i]) {
					numright++;
				}
			}
			int right[] = new int[numright];
			for (i = 1, j = 0; i < n - 1; i++) {
				if (!atinf[i]) {
					right[j++] = i;
				}
			}
			boolean cmplx[] = new boolean[numleft];
			int numcmplx = 0;
			for (i = 0; i < numleft; i++) {
				cmplx[i] = ((right[i] - left[i]) == 2);
				if (cmplx[i]) {
					numcmplx++;
				}
			}
			int numnotcmplx = numleft - numcmplx;
			// Normalize lengths by w[1]-w[0]
			double denomR = w[1][0] - w[0][0];
			double denomI = w[1][1] - w[0][1];
			double nmlen[][] = new double[numleft][2];
			for (i = 0; i < numleft; i++) {
				scm.zdiv(w[right[i]][0] - w[left[i]][0], w[right[i]][1] - w[left[i]][1], denomR, denomI, cr, ci);
				nmlen[i][0] = cr[0];
				nmlen[i][1] = ci[0];
			}
			double nmlen2[] = new double[numnotcmplx + 2 * numcmplx];
			for (i = 0, j = 0, k = 0; i < numleft; i++) {
				if (!cmplx[i]) {
					nmlen2[j++] = scm.zabs(nmlen[i][0], nmlen[i][1]);
				} else {
					nmlen2[numnotcmplx + k] = nmlen[i][0];
					nmlen2[numnotcmplx + numcmplx + k] = nmlen[i][1];
					k++;
				}
			}
			// First entry is useless (=1)
			double nmlen3[] = new double[nmlen2.length - 1];
			for (i = 0; i < nmlen3.length; i++) {
				nmlen3[i] = nmlen2[i + 1];
			}

			// Set up initial guess
			if ((z0 == null) || (z0.length == 0)) {
				z0 = new double[n - 1];
				z0[0] = -1;
				z0[n - 2] = 1;
				for (i = 1; i < n - 2; i++) {
					z0[i] = i * 2.0 / (n - 2.0);
				}
			} // if ((z0 == null) || (z0.length == 0))
			else {
				double denom = z0[n - 2] - z0[0];
				for (i = 0; i < n - 2; i++) {
					z0[i] = 2.0 * z0[i] / denom;
				}
				for (i = 0; i < n - 2; i++) {
					z0[i] = z0[i] - z0[0] - 1.0;
				}
			}
			double y0[] = new double[n - 2];
			for (i = 0; i < n - 2; i++) {
				y0[i] = Math.log((z0[i + 1] - z0[i]) / (z0[i + 2] - z0[i + 1]));
			}

			// Solve nonlinear system of equations
			double betan[] = new double[n - 1];
			for (i = 0; i < n - 1; i++) {
				betan[i] = beta[i];
			}
			hppfun fm = new hppfun(y0, n, betan, nmlen3, left, right, cmplx, qdat);
			fm.driver();
			fm.dumpResults();
			int exitStatus = fm.getExitStatus();
			if (exitStatus < 0) {
				System.out.println("Error in NLConstrainedEngine during hpparam call to hppfun");
				scm.printExitStatus(exitStatus);
				System.exit(-1);
			}
			double y[] = fm.getParameters();
			
			// Convert y values to z
			double cumprod[] = new double[n-2];
			cumprod[0] = 1;
			for (i = 1; i <= n-3; i++) {
				cumprod[i] = cumprod[i-1]*Math.exp(-y[i-1]);
			}
			double cs[] = new double[n-2];
			cs[0] = cumprod[0];
			for (i = 1; i <= n-3; i++) {
				cs[i] = cs[i-1] + cumprod[i];
			}
			double flipcs[] = new double[n-2];
			flipcs[0] = cumprod[n-3];
			for (i = 1; i <= n-3; i++) {
				flipcs[i] = flipcs[i-1] + cumprod[n-3-i];
			}
			double flipflipcs[] = new double[n-2];
			for (i = 0; i <= n-3; i++) {
				flipflipcs[i] = flipcs[n-3-i];
			}
			z = new double[n][2];
			z[0][0] = -flipflipcs[0];
			for (i = 1; i <= n-3; i++) {
				z[i][0] = cs[i-1] - flipflipcs[i];
			}
			z[n-2][0] = cs[n-3];
			for (i = 0; i < n-1; i++) {
				z[i][0] = z[i][0]/z[n-2][0];
			}
			z[n-1][0] = Double.POSITIVE_INFINITY;
		} // else n != 3
	}

	class hppfun extends NLConstrainedEngine {
		int n;
		double beta[];
		double nmlen[];
		int left[];
		int right[];
		boolean cmplx[];
		double qdat[][];

		public hppfun(double y0[], int n, double beta[], double nmlen[], int left[], int right[], boolean cmplx[],
				double qdat[][]) {
			// nPoints, params
			super(y0.length, y0.length);
			this.n = n;
			this.beta = beta;
			this.nmlen = nmlen;
			this.left = left;
			this.right = right;
			this.cmplx = cmplx;
			this.qdat = qdat;

			bounds = 0; // bounds = 0 means unconstrained

			// bounds = 1 means same lower and upper bounds for
			// all parameters
			// bounds = 2 means different lower and upper bounds
			// for all parameters

			// The default is internalScaling = false
			// To make internalScaling = true and have the columns of the
			// Jacobian scaled to have unit length include the following line.
			// internalScaling = true;
			// Suppress diagnostic messages
			outputMes = false;
			for (int i = 0; i < y0.length; i++) {
				gues[i] = y0[i];
			}
		}

		/**
		 * Starts the analysis.
		 */
		public void driver() {
			super.driver();
		}

		/**
		 * Display results of displaying exponential fitting parameters.
		 */
		public void dumpResults() {
			Preferences.debug(" ******* Fit Elsunc Schwarz-Christoffel hpparam ********* \n\n",
					Preferences.DEBUG_ALGORITHM);
			Preferences.debug("Number of iterations: " + String.valueOf(iters) + "\n", Preferences.DEBUG_ALGORITHM);
			Preferences.debug("Chi-squared: " + String.valueOf(getChiSquared()) + "\n", Preferences.DEBUG_ALGORITHM);
			for (int i = 0; i < a.length; i++) {
				Preferences.debug("a" + i + " " + String.valueOf(a[i]) + "\n", Preferences.DEBUG_ALGORITHM);
			}
		}

		public void fitToFunction(double[] a, double[] residuals, double[][] covarMat) {
    		int ctrl;
    		int i, j;
    		double z[][];
    		double I1[][];
    		double I2[][];
    		double cr[] = new double[1];
    		double ci[] = new double[1];
    		try {
				ctrl = ctrlMat[0];

				if ((ctrl == -1) || (ctrl == 1)) {
                    // Returns residual for solution of nonlinear equations
					
					// Convert a values (unconstrained variables) to z (prevertices)
					// n-3 y0, so n-2 cumprod, so n-1 z
					double cumprod[] = new double[a.length+1];
					cumprod[0] = 1;
					for (i = 1; i <= a.length; i++) {
						cumprod[i] = cumprod[i-1]*Math.exp(-a[i-1]);
					}
					double cs[] = new double[a.length+1];
					cs[0] = cumprod[0];
					for (i = 1; i <= a.length; i++) {
						cs[i] = cs[i-1] + cumprod[i];
					}
					double flipcs[] = new double[a.length+1];
					flipcs[0] = cumprod[a.length];
					for (i = 1; i <= a.length; i++) {
						flipcs[i] = flipcs[i-1] + cumprod[a.length-i];
					}
					double flipflipcs[] = new double[a.length+1];
					for (i = 0; i <= a.length; i++) {
						flipflipcs[i] = flipcs[a.length-i];
					}
					z = new double[a.length+2][2];
					z[0][0] = -flipflipcs[0];
					for (i = 1; i <= a.length; i++) {
						z[i][0] = cs[i-1] - flipflipcs[i];
					}
					z[a.length+1][0] = cs[a.length];
					for (i = 0; i < n-1; i++) {
						z[i][0] = z[i][0]/z[n-2][0];
					}
					
					// Compute the integrals
					double zleft[][] = new double[left.length][2];
					for (i = 0; i < left.length; i++) {
						zleft[i][0] = z[left[i]][0];	
					}
					double zright[][] = new double[right.length][2];
					for (i = 0; i < right.length; i++) {
						zright[i][0] = z[right[i]][0];	
					}
					double mid[][] = new double[left.length][2];
				    for (i = 0; i < left.length; i++) {
				    	mid[i][0] = (zleft[i][0] + zright[i][0])/2.0;
				    }
				    // For integrals between nonadjacent singularities, choose intermiediate
				    // points in the upper half-plane
				    int numcmplx = 0;
				    for (i = 0; i < cmplx.length; i++) {
				        if (cmplx[i]) {
				        	numcmplx++;
				        	mid[i][1] = (zright[i][0] - zleft[i][0])/2.0;
				        }
				    } // for (i = 0; i < cmplx.length; i++)
				    int numnotcmplx = cmplx.length - numcmplx;
				    I1 = hpquad(zleft, mid, left, z, beta, qdat);
				    I2 = hpquad(zright, mid, right, z, beta, qdat);
				    double ints[][] = new double[left.length][2];
				    int numintszero = 0;
				    for (i = 0; i < left.length; i++) {
				    	ints[i][0] = I1[i][0] - I2[i][0];
				    	ints[i][1] = I1[i][1] - I2[i][1];
				    	if ((ints[i][0] == 0) && (ints[i][1] == 0)) {
				        	numintszero++;
				        }
				    }
				    
				    if (numintszero > 0) {
				    	// Singularities were too crowded in practice
				    	MipavUtil.displayWarning("Severe crowding");
				    }
				    
				    // Compute nonlinear equation residual values.
				    double F1[] = new double[numnotcmplx];
				    for (i = 0, j = 0; i < cmplx.length; i++) {
				    	if (!cmplx[i]) {
				    		F1[j++] = scm.zabs(ints[i][0], ints[i][1]);
				    	}
				    }
				    double F11[] = new double[numnotcmplx-1];
				    for (i = 1; i < numnotcmplx; i++) {
				    	F11[i-1] = F1[i]/F1[0];
				    }
				    double F2[][] = new double[numcmplx][2];
				    double denomR = ints[0][0];
				    double denomI = ints[0][1];
				    for (i = 0, j = 0; i < cmplx.length; i++) {
				    	if (cmplx[i]) {
				    	    scm.zdiv(ints[i][0], ints[i][1], denomR, denomI, cr, ci);
				    	    F2[j][0] = cr[0];
				    	    F2[j++][1] = ci[0];
				    	}
				    }
				    double F[] = new double[numnotcmplx-1 + 2*numcmplx];
				    for (i = 0; i < numnotcmplx-1; i++) {
				    	F[i] = F11[i];
				    }
				    for (i = 0; i < numcmplx; i++) {
				    	F[numnotcmplx-1+i] = F2[i][0];
				    	F[numnotcmplx-1+numcmplx+i] = F2[i][1];
				    }
				    for (i = 0; i < F.length; i++) {
				    	residuals[i] = F[i] - nmlen[i];
				    }
				} // if ((ctrl == -1) || (ctrl == 1))

				// Calculate the Jacobian numerically
				else if (ctrl == 2) {
					ctrlMat[0] = 0;
				}
			} catch (Exception e) {
				Preferences.debug("function error: " + e.getMessage() + "\n",
						Preferences.DEBUG_ALGORITHM);
			}

			return;
    		
    	}
	}

	private double[][] hpquad(double z1[][], double z2[][], int sing1[], double z[][], double beta[], double qdat[][]) {
		// Numerical quadrature for the half-plane map.
		// z1, z2 are vectors of left and right endpoints. sing1 is a vector of
		// integer indices which label the singularities in z1. So if sing1[5] =
		// 3,
		// then z1[]5 = z[3],. A -1 means no singularity. A is the vector of
		// finite
		// singularities; beta is the vector of associated turning angles. qdata
		// is
		// quadrature data from scqdata.

		// hpquad integrates from a possible singularity at the left end to a
		// regular point at the right. If both endpoints are singularities,
		// you must break the integral into two pieces and make two calls, or
		// call hpquad(z1,z2,sing1,sing2,z,beta,qdat) and accept an automatic
		// choice.

		// The integral is subdivided, if necessary, so that no singularity
		// lies closer to the left endpoint than 1/2 the length of the
		// integration (sub)interval.

		// Original MATLAB routine copyright 1998 by Toby Driscoll.
		int i, j, k, m;
		double cr[] = new double[1];
		double ci[] = new double[1];
		int nqpts = qdat.length;
		// Note: Here n is the total number of *finite* singularities; i.e., the
		// number of terms in the product appearing in the integrand.
		int n = z.length;
		double bigz[][][] = new double[n][nqpts][2];
		for (i = 0; i < n; i++) {
			for (j = 0; j < nqpts; j++) {
				bigz[i][j][0] = z[i][0];
				bigz[i][j][1] = z[i][1];
			}
		}
		double bigbeta[][] = new double[beta.length][nqpts];
		for (i = 0; i < beta.length; i++) {
			for (j = 0; j < nqpts; j++) {
				bigbeta[i][j] = beta[i];
			}
		} // for (i = 0; i < beta.length; i++)
		if ((sing1 == null) || (sing1.length == 0)) {
			sing1 = new int[z1.length];
			for (i = 0; i < z1.length; i++) {
				sing1[i] = -1;
			}
		} // if ((sing1 == null) || (sing1.length == 0))
		double I[][] = new double[z1.length][2];
		int numnontriv = 0;
		for (i = 0; i < z1.length; i++) {
			if ((z1[i][0] != z2[i][0]) || (z1[i][1] != z2[i][1])) {
				numnontriv++;
			}
		} // for (i = 0; i < z1.length; i++)
		int nontriv[] = new int[numnontriv];
		for (i = 0, j = 0; i < z1.length; i++) {
			if ((z1[i][0] != z2[i][0]) || (z1[i][1] != z2[i][1])) {
				nontriv[j++] = i;
			}
		} // for (i = 0, j = 0; i < z1.length; i++)

		double za[] = new double[2];
		double zb[] = new double[2];
		double nd[][] = new double[nqpts][2];
		double wt[][] = new double[nqpts][2];
		double terms[][][] = new double[n][nqpts][2];
		double zr[] = new double[2];
		double zl[] = new double[2];
		double logterms[] = new double[2];
		double prod[] = new double[2];
		double expSum[] = new double[2];
		double expTerm;
		double termsr;
		double termsi;

		for (i = 0; i < nontriv.length; i++) {
			k = nontriv[i];
			za[0] = z1[k][0];
			za[1] = z1[k][1];
			zb[0] = z2[k][0];
			zb[1] = z2[k][1];
			int sng = sing1[k];

			// Allowable integration step, based on nearest singularity.
			double dist = 1.0;
			double denom = scm.zabs(zb[0] - za[0], zb[1] - za[1]);
			double minVal = Double.MAX_VALUE;
			double absDiff;
			for (j = 0; j <= sng - 1; j++) {
				absDiff = scm.zabs(z[j][0] - za[0], z[j][1] - za[1]);
				if (absDiff < minVal) {
					minVal = absDiff;
				}
			} // for (j = 0; j <=sng-1; j++)
			for (j = sng + 1; j <= n - 1; j++) {
				absDiff = scm.zabs(z[j][0] - za[0], z[j][1] - za[1]);
				if (absDiff < minVal) {
					minVal = absDiff;
				}
			} // for (j = sng+1; j <= n-1; j++)
			minVal = 2.0 * minVal / denom;
			if (minVal < dist) {
				dist = minVal;
			}
			zr[0] = za[0] + dist * (zb[0] - za[0]);
			zr[1] = za[1] + dist * (zb[1] - za[1]);
			// Adjust Gauss-Jacobi nodes and weights to interval.
			int ind = (sng + n + 1) % (n + 1);
			for (j = 0; j < nqpts; j++) {
				nd[j][0] = ((zr[0] - za[0]) * qdat[j][ind] + zr[0] + za[0]) / 2.0; // G-J
																					// nodes
				nd[j][1] = ((zr[1] - za[1]) * qdat[j][ind] + zr[1] + za[1]) / 2.0;
				wt[j][0] = ((zr[0] - za[0]) / 2.0) * qdat[j][ind + n + 1];
				wt[j][1] = ((zr[1] - za[1]) / 2.0) * qdat[j][ind + n + 1];// G-J
																			// weights
			} // for (j = 0; j < nqpts; j++)
			int zeroterms = 0;
			for (j = 0; j < n; j++) {
				for (m = 0; m < nqpts; m++) {
					terms[j][m][0] = nd[m][0] - bigz[j][m][0];
					terms[j][m][1] = nd[m][1] - bigz[j][m][1];
					if ((terms[j][m][0] == 0) && (terms[j][m][1] == 0)) {
						zeroterms++;
					}
				}
			} // for (j = 0; j < n; j++)
			if (zeroterms > 0) {
				// Endpoints are practically coincident
				I[k][0] = 0;
				I[k][1] = 0;
			} else {
				// Use Gauss-Jacobi on first subinterval, if necessary.
				if (sng >= 0) {
					double fac = scm.zabs(zr[0] - za[0], zr[1] - za[1]) / 2.0;
					double fac2 = Math.pow(fac, beta[sng]);
					for (m = 0; m < nqpts; m++) {
						denom = scm.zabs(terms[sng][m][0], terms[sng][m][1]);
						terms[sng][m][0] = terms[sng][m][0] / denom;
						terms[sng][m][1] = terms[sng][m][1] / denom;
						wt[m][0] = wt[m][0] * fac2;
						wt[m][1] = wt[m][1] * fac2;
					}
				} // if (sng >= 0)
				I[k][0] = 0;
				I[k][1] = 0;
				for (m = 0; m < nqpts; m++) {
					expSum[0] = 0;
					expSum[1] = 0;
					for (j = 0; j < n; j++) {
						logterms[0] = Math.log(scm.zabs(terms[j][m][0], terms[j][m][1]));
						logterms[1] = Math.atan2(terms[j][m][1], terms[j][m][0]);
						prod[0] = logterms[0] * bigbeta[j][m];
						prod[1] = logterms[1] * bigbeta[j][m];
						expSum[0] += prod[0];
						expSum[1] += prod[1];
					} // for (j = 0; j < n; j++)
					expTerm = Math.exp(expSum[0]);
					scm.zmlt(expTerm * Math.cos(expSum[1]), expTerm * Math.sin(expSum[1]), wt[m][0], wt[m][1], cr, ci);
					I[k][0] += cr[0];
					I[k][1] += ci[0];
				} // for (m = 0; m < nqpts; m++)
				while (dist < 1) {
					// Do regular Gaussian quad on other subintervals.
					zl[0] = zr[0];
					zl[1] = zr[1];
					dist = 1.0;
					minVal = Double.MAX_VALUE;
					denom = scm.zabs(zl[0] - zb[0], zl[1] - zb[1]);
					for (j = 0; j < n; j++) {
						double num = scm.zabs(z[j][0] - zl[0], z[j][1] - zl[1]);
						if (num < minVal) {
							minVal = num;
						}
					} // (j = 0; j < n; j++)
					minVal = 2.0 * minVal / denom;
					if (minVal < dist) {
						dist = minVal;
					}
					zr[0] = zl[0] + dist * (zb[0] - zl[0]);
					zr[1] = zl[1] + dist * (zb[1] - zl[1]);
					for (j = 0; j < nqpts; j++) {
						nd[j][0] = ((zr[0] - zl[0]) * qdat[j][n] + zr[0] + zl[0]) / 2.0;
						nd[j][1] = ((zr[1] - zl[1]) * qdat[j][n] + zr[1] + zl[1]) / 2.0;
						wt[j][0] = ((zr[0] - zl[0]) / 2.0) * qdat[j][2 * n + 1];
						wt[j][1] = ((zr[1] - zl[1]) / 2.0) * qdat[j][2 * n + 1];
					} // for (j = 0; j < nqpts; j++)
					for (m = 0; m < nqpts; m++) {
						expSum[0] = 0;
						expSum[1] = 0;
						for (j = 0; j < n; j++) {
							termsr = nd[m][0] - bigz[j][m][0];
							termsi = nd[m][1] - bigz[j][m][1];
							logterms[0] = Math.log(scm.zabs(termsr, termsi));
							logterms[1] = Math.atan2(termsi, termsr);
							prod[0] = logterms[0] * bigbeta[j][m];
							prod[1] = logterms[1] * bigbeta[j][m];
							expSum[0] += prod[0];
							expSum[1] += prod[1];
						}
						expTerm = Math.exp(expSum[0]);
						scm.zmlt(expTerm * Math.cos(expSum[1]), expTerm * Math.sin(expSum[1]), wt[m][0], wt[m][1], cr,
								ci);
						I[k][0] += cr[0];
						I[k][1] += ci[0];
					} // for (m = 0; m < nqpts; m++)
				} // while (dist < 1)
			} // else
		} // for (i = 0; i < nontriv.length; i++)
		return I;
	}

}