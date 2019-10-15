package gov.nih.mipav.model.algorithms;

import gov.nih.mipav.view.*;

import java.io.*;
import java.util.*;
import java.time.format.DateTimeFormatter;
import java.time.Clock;
import java.time.LocalDateTime; 
import javax.vecmath.*;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import de.jtem.numericalMethods.algebra.linear.decompose.Eigenvalue;

/**
libdt - OpenCV library for Dynamic Textures - version 1.0

Copyright (c) 2011-2014 Antoni B. Chan, Adeel Mumtaz, City University of Hong Kong
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

3. Neither name of copyright holders nor the names of its contributors
may be used to endorse or promote products derived from this software
without specific prior written permission.


THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

public class libdt extends AlgorithmBase {
	private RandomAccessFile raFile;
	private boolean endian = false;
	/** byte array for int * */
    private final byte[] byteIntBuffer = new byte[4];
    /** byte array for double * */
    private final byte[] byteDoubleBuffer = new byte[8];
    private final int CV_8U = 0;
    private final int CV_64F = 6;
    private final int CV_64FC3 = 22;
	
	public libdt() {
		
	}
/*!
 * <Full 20 trial BoS classification on UCLA9 Eight Class Data Set!!>
 * 
 * Copyright (c) 2014 by <Adeel Mumtaz/ VISAL@City University of Hong Kong>
 * libdt - OpenCV library for Dynamic Textures - version 1.0
 */

/**
#include <iostream>
#include <iomanip>
#include <string>
#include <iterator>
#include<fstream>
#include<math.h>
#include <ctime>

#include "opencv/cv.h"
#include "opencv/cxcore.h"
#include "opencv/highgui.h"

#include "options.h"
#include "utils/libutils.h"
#include "stats/libstats.h"
#include "video/libvideo.h"
#include "dytex/libdytex.h"
#include "platInd/platInd.hpp"
#include "bufferer/Bufferer.hpp"

using namespace cv;
using namespace std;
*/


	
	public void runAlgorithm() {
		
	}

    private String getTime() {
    	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
    	   LocalDateTime now = LocalDateTime.now(); 
    	   return dtf.format(now);
    }
    
    public void test_HEM() {
    	File file;
    	System.out.println("Experiment started: " + getTime());
    	file = new File("C:/temporal texture/libdt-v1.0/libdt-v1.0/testdata/HEM/47fa110.dtm");
    	try {
		    raFile = new RandomAccessFile(file, "r");
    	}
    	catch (FileNotFoundException e) {
    		MipavUtil.displayError(e + " ");
    		System.exit(-1);
    	}
    	//load existing	dtm	
    	DytexMix dtm = new DytexMix();
    	read(dtm);
    	try {
    		raFile.close();
    	}
    	catch (IOException e) {
    		MipavUtil.displayError(e + " ");
    		System.exit(-1);
    	}
    	
    	//setting up HEM to reduce mixture to only 4 components
    	DytexRegOptions ropt = new DytexRegOptions (cov_reg_type.COV_REG_MIN,0.01,cov_reg_type.COV_REG_MIN,0.01,cov_reg_type.COV_REG_MIN,0.01,
    			cov_reg_type.COV_REG_ADD,0.999);
    	HEMOptions hopt = new HEMOptions(4,ropt,0.0001,Ymean_type.NONZERO_YMEAN,Verbose_mode.COMPACT);
    	
    	//split schedule of 1,2,4
    	for(int i=1;i<=4;i=i*2)
    	hopt.splitOpt.sched.add(i);
    	
    	//run The HEM 
    	DytexMix emout=reduceWithSplitting(dtm, hopt);
    }
    
    private DytexMix reduceWithSplitting(DytexMix dtm, HEMOptions hopt)
    {
    	int j,k;
    	//reduced mixture
    	DytexMix hembest = new DytexMix(dtm.opt);
    	//OPTIONS
    	double pert=hopt.splitOpt.pert;
    	int Ks=hopt.K;
    	//initialize splitting sequence
    	if(hopt.splitOpt.sched.isEmpty())
    	{
    		for(int i=1;i<=hopt.K;i++)
    			hopt.splitOpt.sched.add(i);
    	}
    	
    	//%%% preprocess %%%
    	System.out.println("preprocessing DT...");	
    	for(int i=0;i<dtm.dt.size();i++)
    	{
    		if ((dtm.dt.get(i).C.dims == 2) && (dtm.dt.get(i).C.type == CV_64F)) {
	    		Matrix cMat = new Matrix(dtm.dt.get(i).C.double2D);
	    		SingularValueDecomposition svd = new SingularValueDecomposition(cMat);
	    		Matrix matV = svd.getV();
	    		double singularValues[] = svd.getSingularValues();
	    		double arrSingular[][] = new double[singularValues.length][singularValues.length];
	    		for (j = 0; j < singularValues.length; j++) {
	    			arrSingular[j][j] = singularValues[j];
	    		}
	    		Matrix matS = new Matrix(arrSingular);
	    		Matrix matVS = matV.times(matS);
	    		double arrVS[][] = matVS.getArray();
	    		dtm.dt.get(i).Cvs.create(arrVS.length, arrVS[0].length, CV_64F);
	    		for (j = 0; j < arrVS.length; j++) {
	    			for (k = 0; k < arrVS[0].length; k++) {
	    				dtm.dt.get(i).Cvs.double2D[j][k] = arrVS[j][k];
	    			}
	    		}
	    		dtm.dt.get(i).isCvs=true;
    		} // if ((dtm.dt.get(i).C.dims == 2) && (dtm.dt.get(i).C.type == CV_64F))
    		else {
    			MipavUtil.displayError("For SVD dims = " + dtm.dt.get(i).C.dims + " type = " + dtm.dt.get(i).C.type);
    			System.exit(-1);
    		}
    	} // for(int i=0;i<dtm.dt.size();i++)
    	
    	//check for valid splitting sched
    	if(hopt.splitOpt.sched.get(0)!=1)
    	{
    		MipavUtil.displayError("schedule must start with 1!");
    		System.exit(-1);
    	}
    	Vector<Integer> tmp = new Vector<Integer>();
    	for(int i=1;i<hopt.splitOpt.sched.size();i++)
    		tmp.add(hopt.splitOpt.sched.get(i)/hopt.splitOpt.sched.get(i-1));
    	
    	for (j = 0; j < tmp.size(); j++) {
    		if (tmp.get(j) > 2) {
    			MipavUtil.displayError("Cannot grow K more than 2 times previous");
    			System.exit(-1);
    		}
    	}

    	System.out.print("Growing schedule: ");
    	for (j = 0; j < hopt.splitOpt.sched.size(); j++) {
    		System.out.print(hopt.splitOpt.sched.get(j) + " ");
    	}
    	System.out.print("\n");
    	System.out.println("Ks: " + Ks);
    	//HEM splitting loop
    	int Kiter=1;
    	while(hembest.dt.size()<hopt.K)
    	{
    		if(Kiter==1)
    		{
    			System.out.println("*** EM: K= "+(hembest.dt.size()+1) + " ***********************");
    		}
    		else
    		{
    			Vector<Integer> mysplits = new Vector<Integer>();
    			//split here
    			while(hembest.dt.size()<hopt.splitOpt.sched.get(Kiter-1))
    			{
    				DytexSplitParams splitopt = new DytexSplitParams();
    				splitopt.crit=hopt.splitOpt.crit;				
    				splitopt.ignore=mysplits;
    				splitopt.target=-1;
    				splitopt.pert=hopt.splitOpt.pert;
    				splitopt.mode=hopt.splitOpt.mode;
    				splitopt.vars=hopt.splitOpt.vars;
    				int c1[] = new int[1];
    				int c2[] = new int[1];
    				dytex_mix_split(hembest, splitopt,c2,c1);
    				mysplits.add(c1[0]);
    				mysplits.add(c2[0]);
    			}
    			//remove pre-cache (since it is invalid after splitting)
    			for(int ii=0;ii<hembest.dt.size();ii++)
    			{
    				hembest.dt.get(ii).isCvs=false;
    			}
    			System.out.println("*** EM: K= " + hembest.dt.size() + " ******************");
    		}
    		Vector<Integer> classes = new Vector<Integer>();
    		//runs HEM algorithm for current mixture
    		runHEM(hembest,hopt,classes);
    		Kiter++;
    	}

    	//RUN HEM again on once on final solution			
    	hopt.termvalue=hopt.termvalBest;	
    	hopt.maxiter=50;  //Can be adjusted to run more iterations
    	runHEM(hembest,hopt,hembest.classes);
    	return hembest;
    }
    
    /*!
     * \brief
     * run iterations of HEM for a mixture of DT
     * 
     * \param hembest
     * input DT mixture.
     * 
     * \param hopt
     * learning option for HEM.
     * 
     * \param classes
     * Class of each input DT that is ID of the new DT
     * 
     * \remarks
     * in general, this should not be called.use reduceWithSplitting instead
     * 
     * \see
     * reduceWithSplitting | HEMOptions
     */
    private void runHEM(DytexMix hembest, HEMOptions hopt, Vector<Integer> classes)
    {
    	//used to display info in change in classes during EM loop
    	Clock elapsedtime;
    	int numlastclasses=5;
        boolean FlagYmean;
        if (hopt.Ymean == Ymean_type.ZERO_YMEAN) {
        	FlagYmean = false;
        }
        else {
        	FlagYmean = true;
        }
        boolean FlagVerbose;
        if (hopt.verbose == Verbose_mode.QUIET) {
        	FlagVerbose = false;
        }
        else {
        	FlagVerbose = true;
        }

    	int Kb=hembest.dt.size();
    	if (FlagVerbose)
    	    System.out.println("Preprocessing " + Kb + " base components...");

    	for(int i=0;i<Kb;i++)
    	{		
    		if(hembest.dt.get(i).dtopt.Yopt!=hopt.Ymean)
    		{
    			System.out.println("** Warning: hemopt.Ymean does not match " + hembest.dt.get(i).dtopt.Yopt);
    		}
    		//Preprocessing already done
    	}

    	//HEM parameters
    	int n=hembest.dt.get(0).dtopt.n;
    	int m=hembest.dt.get(0).dtopt.m;
    	if (FlagVerbose)
    		System.out.println("n = " + n); 
    		System.out.println("m = " + m);
    	    System.out.println("Ymean = " + hembest.dt.get(0).dtopt.Yopt);
    	
    	int Nvs=hopt.N;
    	int tau=hopt.tau;
    	//min total probability for blank cluster
    	double MINPROB =(((double)1.0)/(((double)2.0)*(double)Kb));  

    	//initializations
    	/*if(hembest.dt.size()==0)
    	{
    		if(FlagVerbose)
    			System.out.println("Initializing First DT with Sub-optimal: ");
    		
    		//average of all DTs
    		Dytex tmpC= init_multiple_dt();
    		hembest.dt.push_back(tmpC);
    		hembest.alpha.push_back(1.0);
    	}
    	
    	//current mixture size
    	int Kr=hembest.dt.size(); 

    	//Regularize the initializations
    	for(int i=0;i<Kr;i++)
    	{		
    		hembest.dt[i].setRegularizer(hopt.regopt);
    		hembest.dt[i].regularize(true);			

    		if(hembest.dt[i].isCvs==false)
    		{
    			SVD svd(hembest.dt[i].C);
    			Mat Cu,Cs,Cv;
    			svd.u.copyTo(Cu);
    			svd.vt.copyTo(Cv);	
    			Cv=Cv.t();
    			Cs=Mat::zeros(svd.w.rows,svd.w.rows,svd.w.type());				
    			Mat tmpM=Cs.diag();
    			svd.w.copyTo(tmpM);
    			tmpM=Cv*Cs;			
    			tmpM.copyTo(hembest.dt[i].Cvs);
    			hembest.dt[i].isCvs=true;
    		}
    	}


    	//%%% RUN HEM %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

    	//initialize convergence measures	
    	std::vector<double> datalikelihood(hopt.maxiter+1,0);
    	std::vector<double> ddtall(hopt.maxiter,0);
    	std::vector<double> pdtall(hopt.maxiter,0);
    	std::vector<std::vector<int> > lastclasses(numlastclasses);
    	int lastclassesind = 0;

    	for(int i=0;i<numlastclasses;i++)
    		lastclasses[i]=std::vector<int>(Kb,0);

    	//initializa blanks
    	std::vector<double> blank(Kr,0);
    	for(int j=0;j<Kr;j++)
    	{
    		if(hembest.dt[j].isempty)
    			blank[j]=1;
    	}

    	//initialize loop
    	clock_t starttime=clock();	
    	int iter=0;
    	//hem loop
    	while(1)
    	{
    		//compute statistics between 2 DT for HEM E-step
    		Estats Estat(dt,hembest.dt,tau,FlagYmean);
    		Estat.computeEll();
    		Mat ell=Estat.Ell.clone();
    		Mat tmpM=Mat(alpha).clone();
    		tmpM=tmpM*Nvs;
    		Mat tmpM2;
    		repeat(tmpM,1,Kr,tmpM2);

    		Mat tmpM3(hembest.alpha);
    		Mat tmpM4;
    		log(tmpM3.t(),tmpM4);
    		Mat tmpM5;
    		repeat(tmpM4,Kb,1,tmpM5);
    		multiply(ell,tmpM2,tmpM);

    		//aggregated statistics for dti and dtj
    		ell = tmpM + tmpM5;

    		// soft assignment and data likelihood
    		Mat logZ   = Mat::zeros(Kb,Kr,OPT_MAT_TYPE);  
    		Mat tmp = (logtrick(ell.t())).t();
    		for(int j=0;j<Kr;j++)
    		{
    			logZ.col(j) = ell.col(j) - tmp;
    		}

    		datalikelihood[iter]=sum(tmp)[0];
    		Mat Z;
    		exp(logZ,Z);

    		if(FlagVerbose>=2)
    			cout<<endl;

    		//hard assignment
    		classes.clear();		
    		for(int i=0;i<Z.rows;i++)
    		{
    			double min,max;
    			Point minL,maxL;
    			minMaxLoc(Z.row(i),&min,&max,&minL,&maxL);				
    			classes.push_back(maxL.x+1);
    		}

    		//Check Convergence
    		double ddLL=0;
    		double dpLL=0;
    		
    		if(iter>0)
    		{
    			//compute change in log-likelihood
    			ddLL=datalikelihood[iter]-datalikelihood[iter-1];
    			dpLL=abs(ddLL/datalikelihood[iter-1]);
    		}
    		else
    		{
    			ddLL = INF;			
    			dpLL = INF;
    		}
    		//class assignment info
    		lastclasses[lastclassesind]=classes;

    		//count the number of class changes
    		std::vector<int> dclass;
    		for(int ii=0;ii<numlastclasses;ii++)
    		{
    			int sum=0;
    			for(int i=0;i<lastclasses[0].size();i++)
    			{
    				if(lastclasses[ii][i]!=lastclasses[lastclassesind][i])
    					sum++;
    			}
    			dclass.push_back(sum);
    		}

    		string dclassstr="";			
    		for(int i=lastclassesind+1;i<numlastclasses;i++)
    		{
    			stringstream ss;
    			ss<<dclass[i];
    			dclassstr=dclassstr+ss.str()+" ";
    		}
    		for(int i=0;i<lastclassesind;i++)
    		{
    			stringstream ss;
    			ss<<dclass[i];
    			dclassstr=dclassstr+ss.str()+" ";
    		}

    		//% lastclassind points to the oldest classes
    		lastclassesind = lastclassesind+1;
    		if (lastclassesind>=numlastclasses)
    			lastclassesind = 0;

    		//output strings
    		stringstream ss2;
    		ss2<<"dclass = "<<dclassstr;
    		string outstr2=ss2.str();
    		string outstr1s;
    		string outstr3;
    		stringstream ss3;

    		ss3<<"L= "<<datalikelihood[iter]<<" (pL= "<<dpLL<<")";
    		outstr1s=ss3.str();
    		
    		if(FlagVerbose==1)
    		{
    			stringstream ss3;
    			ss3<<"iter= "<<iter+1<<"; "<<outstr1s<<"; "<<outstr2<<";  ";
    			outstr3=ss3.str();
    			cout<<outstr3<<endl;
    		}
    		else if(FlagVerbose>=2)
    		{			
    			cout<<outstr2<<endl;
    		}

    		// check if negative change in log-likelihood!
    		if (ddLL<0)
    		{
    			cout<<"WARNING -- change in log likelihood is negative???"<<endl;
    		}	
    		//%%% check convergence conditions %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    		int breakout = 0;
    		string termstr;
    		if (iter >= hopt.maxiter)
    		{
    			termstr = "***** done -- max iter reached\n";
    			breakout = 1;
    		}
    		//only this convergence condition
    		if ( (ddLL >= 0) && (dpLL < hopt.termvalue) )
    		{
    			termstr = "***** done -- small percent change in data likelihood\n";
    			breakout = 1;
    		}

    		//%%% convergence condition was reached... %%%%%%%%%%%%%%%%%%%%%%%%%%%%
    		if (breakout)
    		{
    			if (FlagVerbose >= 1)
    			{
    				if (FlagVerbose == 1)
    				{
    					cout<<endl;				
    				}
    				cout<<termstr<<endl;
    			}	
    			break;
    		}

    		//%%% M-Step %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%		

    		//compute Nhat - total soft assignments per cluster
    		//1) update prior probabilities
    		Mat Nhat;
    		reduce(Z,Nhat,0,CV_REDUCE_SUM);
    		tmp=Nhat/((double)Kb);
    		for(int i=0;i<hembest.alpha.size();i++)
    			hembest.alpha[i]=tmp.at<OPT_F_TYPE>(0,i);
    				

    		// compute weights: Zij * alpha(i)
    		repeat(Mat(alpha), 1, Kr,tmpM);
    		Mat W;
    		multiply(Z,tmpM,W);		
    		// normalize weights
    		reduce(W,tmpM,0,CV_REDUCE_SUM);
    		repeat(tmpM, Kb, 1,tmpM2);
    		divide(W,tmpM2,tmpM3);
    		W=tmpM3.clone();
    		Estat.computeAggregateStats(W);

    		//%%% loop through each cluster %%%
    		for(int j=0;j<Kr;j++)
    		{			
    			//check if this is cluster is blank
    			if(hembest.alpha[j]<=MINPROB)
    			{
    				blank[j]=1;
    				hembest.dt[j].isempty=true;
    				if (FlagVerbose >= 1)
    					cout<<"blank";
    			}			
    			else // % --- standard M-step: learn the parameters -------------------------
    			{			

    				Mat xij      = Estat.xij[j];
    				Mat etaj     = Estat.etaj[j];
    				Mat gammaj   = Estat.gammaj[j];
    				Mat Phij     = Estat.Phij[j];
    				Mat varphij  = Estat.varphij[j];
    				Mat phij     = Estat.phij[j];
    				Mat betaj    = Estat.betaj[j];
    				Mat Psij     = Estat.Psij[j];
    				double Lambdaj  = Estat.Lambdaj[j];
    				Mat Gammaj   = Estat.Gammaj[j];
    				
    				//%%% compute new parameters %%%

    				//C parameter
    				Mat iPhij = Phij.inv();
    				Mat newC = Gammaj*iPhij;
    				hembest.dt[j].C=newC;
    				//update preprocessing step
    				SVD svd(newC);
    				Mat test=Mat::zeros(svd.w.rows,svd.w.rows,OPT_MAT_TYPE);				
    				Mat tmpM;
    				tmpM=test.diag();
    				svd.w.copyTo(tmpM);
    				hembest.dt[j].Cvs=svd.vt.t()*test;
    				hembest.dt[j].isCvs=true;

    				// R parameter
    				hembest.dt[j].R.mtx=(Lambdaj - trace(iPhij*(Gammaj.t()*Gammaj))[0]) / (m);

    				// A parameter
    				Mat newA = Psij*(phij.inv());
    				hembest.dt[j].A = newA;

    				// Q parameter
    				hembest.dt[j].Q.mtx = (varphij-newA*Psij.t());

    				//mu parameter
    				Mat newmu=xij.clone();
    				hembest.dt[j].mu0=newmu;


    				// S parameter 
    				Mat newS = etaj - newmu*newmu.t();
    				switch(opt.Sopt)
    				{
    				case CovMatrix::COV_DIAG:			
    					hembest.dt[j].S0.mtx=newS.diag().clone();
    					hembest.dt[j].S0.covopt=CovMatrix::COV_DIAG;
    					break;
    				default:
    					CV_Error(-1,"TO DO");
    				}

    				// Ymean parameter
    				Mat newYmean;
    				if (FlagYmean)
    				{
    					newYmean = (gammaj - newC*betaj);
    				}
    				else
    				{
    					newYmean = Mat::zeros(m,1,OPT_MAT_TYPE);
    				}				
    				hembest.dt[j].Ymean = newYmean;

    				// regularize the new parameters
    				hembest.dt[j].setRegularizer(hopt.regopt);
    				hembest.dt[j].regularize(true);
    			}
    		}
    		if (FlagVerbose >= 2)
    		{
    			cout<<endl;
    		}

    		//%%% handle empty clusters %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    		////find the largest cluster and split it to fill the blank cluster
    		for(int j=0;j<Kr;j++)
    		{
    			if(blank[j])
    			{
    				if(FlagVerbose)
    					cout<<"Cluster "<<j<<"Is blank"<<endl;			

    				DytexSplitParams splitopt;				
    				splitopt.crit=hopt.emptySplitOpt.crit;
    				splitopt.pert=hopt.emptySplitOpt.pert;
    				splitopt.mode=hopt.emptySplitOpt.mode;
    				splitopt.vars=hopt.emptySplitOpt.vars;
    				//				splitopt.len=tau;
    				splitopt.ignore.clear();
    				splitopt.target=j+1;
    				int c1,c2;
    				hembest.dytex_mix_split(splitopt,c1,c2);

    				blank[j]=0;						
    			}
    		}

    		elapsedtime=clock()-starttime;
    		if(FlagVerbose>=2)
    		{
    			cout<<"Elapsed Time: "<<elapsedtime<<endl;
    		}

    		iter=iter+1; //End of HEM iteration

    	}*/ 
    }
    
    /*!
     * \brief
     * initialize a single DT from current mixture
     * 
     * \returns
     * new DT
     * 
     * \see
     * initcluster_doretto
     */
    /*private Dytex init_multiple_dt()
    {
    	//copy template
    	Dytex odt = new dytex(dt[0].dtopt);

    	if(odt.dtopt.Yopt==DytexOptions::ZERO_YMEAN)
    	{
    		odt.Ymean=Mat::zeros(odt.dtopt.m,1,OPT_MAT_TYPE);
    	}
    	dt[0].Cvs.copyTo(odt.Cvs);
    	odt.isCvs=false;
    	//extract Cs
    	Mat Call(odt.dtopt.m,odt.dtopt.n*dt.size(),OPT_MAT_TYPE);
    	Call=Scalar(0);
    	for(int i=0;i<dt.size();i++)
    	{
    		int cstart=i*odt.dtopt.n;
    		Mat tmpM=Call.colRange(cstart,cstart+odt.dtopt.n);
    		dt[i].C.copyTo(tmpM);
    	}
    	if(Call.rows>Call.cols)
    	{
    		SVD svd(Call);		
    		svd.u.colRange(0,odt.dtopt.n).copyTo(odt.C);		
    	}
    	else
    	{
    		Mat CC = Call*Call.t();
    		SVD svd(CC);		
    		svd.u.colRange(0,odt.dtopt.n).copyTo(odt.C);
    	}
    	//initialize accumulators
    	odt.mu0=Scalar(0);
    	odt.S0.mtx=Scalar(0);
    	odt.Ymean=Scalar(0);
    	odt.A=Scalar(0);
    	odt.Q.mtx=Scalar(0);
    	odt.R.mtx=Scalar(0);

    	//compute other parameters by averaging
    	for(int i=0;i<dt.size();i++)
    	{
    		//compute transformation
    		Mat F=odt.C.t()*dt[i].C;

    		//accumulate
    		odt.mu0=odt.mu0+F*dt[i].mu0;		

    		Mat tmpM(dt[i].S0.mtx.rows,dt[i].S0.mtx.rows,OPT_MAT_TYPE);
    		tmpM=Scalar(0);
    		Mat tmpM2=tmpM.diag();
    		dt[i].S0.mtx.copyTo(tmpM2);

    		odt.S0.mtx=odt.S0.mtx+ (F*tmpM*(F.t())).diag();
    		odt.Ymean=odt.Ymean+dt[i].Ymean;
    		odt.A=odt.A+F*dt[i].A*(F.inv());
    		odt.Q.mtx=odt.Q.mtx+ F*dt[i].Q.mtx*(F.t());
    		odt.R.mtx=odt.R.mtx+dt[i].R.mtx;
    	}

    	odt.mu0    = odt.mu0/dt.size();	
    	odt.S0.mtx     = odt.S0.mtx/dt.size();
    	odt.Ymean  = odt.Ymean/dt.size();
    	odt.A      = odt.A/dt.size();
    	odt.Q.mtx  = odt.Q.mtx/dt.size();
    	odt.R.mtx  = odt.R.mtx/dt.size();
    	
    	return odt;
    }*/
    
    /*!
     * \brief
     * splits a component in current DT mixture
     * 
     * \param splitopt
     * options for which component to split and how to split
     * 
     * \param ctg
     * the index of the new component.
     * 
     * \param csp
     * the component that was split
     * 
     * \see
     * ppertC
     */
    private void dytex_mix_split(DytexMix dtm, DytexSplitParams splitopt,int ctg[],int csp[])
    {
    	int i;
    	int K=dtm.alpha.size();
    	int c1 = 1;
    	int c2,newK;
    	if(K==1)
    	{
    		c1=1;
    	}
    	else
    	{
    		Vector<Dytex> olddt=dtm.dt;
    		Vector<Double> olda1=dtm.alpha;
    		Vector<Double> tmpal = new Vector<Double>();
    		System.out.println("*** split criteria ***");

    		Vector<Matrix> myQ = new Vector<Matrix>(K);
    		Vector<Double> myQe= new Vector<Double>(K);
    		Matrix F;
    		boolean flag=true;
    		//vector<OPT_F_TYPE>::iterator result;
    		switch(splitopt.crit)
    		{
    			//%%% split component with largest Q variance %%%
    			case SPLITQ:
    				for(int jjj=0;jjj<K;jjj++)
    				{
    					boolean proceed = false;
    					for (i = 0; i < splitopt.ignore.size() && (!proceed); i++) {
    						if (splitopt.ignore.get(i) == jjj) {
    						    proceed = true;	
    						}
    					}
    					if(proceed)
    					{
    						myQe.set(jjj,-1.0);
    					}
    					else
    					{
    						//check empty
    						if(olddt.get(jjj).isempty)
    						{
    							myQe.set(jjj,-1.0);
    						}
    						else
    						{
    							//normalize Q by forcing C to be orthonormal
    							Matrix cMat = new Matrix(olddt.get(jjj).C.double2D);
    							SingularValueDecomposition svd = new SingularValueDecomposition(cMat);
    				    		Matrix matVt = svd.getV().transpose();
    				    		double singularValues[] = svd.getSingularValues();
    				    		double testArray[][] = new double[singularValues.length][singularValues.length];
    				    		for (i = 0; i < singularValues.length; i++) {
    				    			testArray[i][i] = singularValues[i];
    				    		}
    				    		Matrix test = new Matrix(testArray);
    				    		F = test.times(matVt);
    							myQ.set(jjj,(F.times(new Matrix(olddt.get(jjj).Q.mtx.double2D))).times(F.transpose()));
    							double[] eigenvalue = new double[myQ.get(jjj).getColumnDimension()];
    					        double[][] eigenvector = new double[myQ.get(jjj).getRowDimension()][myQ.get(jjj).getColumnDimension()];
    					        double temp;
    					        double[] tempCol = new double[6];
    					        int m, n, index;
    					        // In EigenvalueDecomposition the columns represent the
    					        // eigenvectors
    					        Eigenvalue.decompose( myQ.get(jjj).getArray(), eigenvector, eigenvalue);
    							double maxVal = -Double.MAX_VALUE;
    							for (i = 0; i < eigenvalue.length; i++) {
    								if (eigenvalue[i] > maxVal) {
    									maxVal = eigenvalue[i];
    								}
    							}
    							myQe.set(jjj,maxVal);								
    						}
    					}
    				}

    				for(i=0;i<myQe.size();i++)
    				{
    					if(myQe.get(i) !=-1)
    					{
    						flag=false;
    						break;
    					}
    				}
    				if(flag)
    				{
    					c1=0;
    					System.out.println("nothing to split");
    				}
    				else
    				{
    					double result = -Double.MAX_VALUE;
    					int index = -1;
    					for (i = 0; i < myQe.size(); i++) {
    						if (myQe.get(i) > result) {
    							result= myQe.get(i);
    							index = i;
    						}
    					}
    					c1 = index + 1;
    				}
    			break;

    			//split component with largest prior
    			case SPLITP:
    				tmpal=olda1;
    				for(i=0;i<splitopt.ignore.size();i++)
    				{
    					tmpal.set(splitopt.ignore.get(i),-1.0);
    				}
    				double result = -Double.MAX_VALUE;
					int index = -1;
					for (i = 0; i < tmpal.size(); i++) {
						if (tmpal.get(i) > result) {
							result= tmpal.get(i);
							index = i;
						}
					}
					c1 = index + 1;

    			break;

    			default:
    				MipavUtil.displayError("TO DO");
                    System.exit(-1);
    		}
    	}

    	// initialize with previous	
    	//adding a new one
    	if(splitopt.target==-1)
    	{
    		c2=K+1;
    		newK=K+1;			
    		dtm.dt.add(new Dytex()); //add one more blank DT in the list
    		dtm.alpha.add(0.0);
    		
    	}
    	//updating existing
    	else
    	{
    		c2=splitopt.target;
    		newK=K;
    	}

    	System.out.println("Spliting Cluster " + c1 + " : new cluster " + c2);
    	
    	//check if there is anything to split
    	if (c1 == 0)
    	{
    		dtm.dt.get(c2-1).isempty=true;		
    		dtm.alpha.set(c2-1,0.0);
    	}
    	else
    	{
    		//duplicate cluster %%% all parameters c
    		copyTo(dtm.dt.get(c1-1).A,dtm.dt.get(c2-1).A);
    		copyTo(dtm.dt.get(c1-1).C,dtm.dt.get(c2-1).C);
    		copyTo(dtm.dt.get(c1-1).Ymean,dtm.dt.get(c2-1).Ymean);	
    		copyTo(dtm.dt.get(c1-1).mu0,dtm.dt.get(c2-1).mu0);
    		copyTo(dtm.dt.get(c1-1).Q.mtx,dtm.dt.get(c2-1).Q.mtx);			
    		dtm.dt.get(c2-1).Q.covopt=dtm.dt.get(c1-1).Q.covopt;
    		dtm.dt.get(c2-1).Q.n=dtm.dt.get(c1-1).Q.n;
    		dtm.dt.get(c2-1).Q.regopt=dtm.dt.get(c1-1).Q.regopt;
    		dtm.dt.get(c2-1).Q.regval=dtm.dt.get(c1-1).Q.regval;
    		copyTo(dtm.dt.get(c1-1).R.mtx,dtm.dt.get(c2-1).R.mtx);			
    		dtm.dt.get(c2-1).R.covopt=dtm.dt.get(c1-1).R.covopt;
    		dtm.dt.get(c2-1).R.n=dtm.dt.get(c1-1).R.n;
    		dtm.dt.get(c2-1).R.regopt=dtm.dt.get(c1-1).R.regopt;
    		dtm.dt.get(c2-1).R.regval=dtm.dt.get(c1-1).R.regval;
    		copyTo(dtm.dt.get(c1-1).S0.mtx,dtm.dt.get(c2-1).S0.mtx);			
    		dtm.dt.get(c2-1).S0.covopt=dtm.dt.get(c1-1).S0.covopt;
    		dtm.dt.get(c2-1).S0.n=dtm.dt.get(c1-1).S0.n;
    		dtm.dt.get(c2-1).S0.regopt=dtm.dt.get(c1-1).S0.regopt;
    		dtm.dt.get(c2-1).S0.regval=dtm.dt.get(c1-1).S0.regval;					
    		dtm.dt.get(c2-1).isempty=dtm.dt.get(c1-1).isempty;
    		dtm.dt.get(c2-1).vrows=dtm.dt.get(c1-1).vrows;
    		dtm.dt.get(c2-1).vcols=dtm.dt.get(c1-1).vcols;
    		dtm.dt.get(c2-1).dtopt=dtm.dt.get(c1-1).dtopt;
    		copyTo(dtm.dt.get(c1-1).Cvs,dtm.dt.get(c2-1).Cvs);
    		dtm.dt.get(c2-1).isCvs=dtm.dt.get(c1-1).isCvs;
    		double tmp     = dtm.alpha.get(c1-1)/((double)2.0);
    		dtm.alpha.set(c1-1,tmp);
    		dtm.alpha.set(c2-1,tmp);
    		
    		//perturb new cluster
    		dytex_perturb(dtm.dt.get(c2-1), splitopt.pert, splitopt.mode, splitopt.vars);

    		// also perturb old cluster (if principal axis split on C, A, x)
    		if(splitopt.mode==Split_mode.MODEP)
    		{
    			if( splitopt.vars==Split_vars.VARC)
    			{
    				dytex_perturb(dtm.dt.get(c1-1), -splitopt.pert, splitopt.mode, splitopt.vars);
    			}
    		}

    	}
    	ctg[0]=c2-1;
    	csp[0]=c1-1;
    }
    
    /*!
     * \brief
     * custom perturbation of C based on max variance of S0
     * 
     * \param dtp
     * Dt to perturb
     * 
     * \param pert
     * pert value
     * 
     * \param mode
     * perturbation mode; 
     * 
     * \param vars
     * variables to perturb
     * 
     * \remark only perturbation based on scale up principal axis and varialble C is implemented
     *
     * \see
     * dytex_mix_split | ppertC
     */
    private void dytex_perturb(Dytex dtp, double pert, Split_mode mode, Split_vars vars)
    {
    	System.out.println("perturbing C by " + pert);

    	switch(mode)
    	{
    		//scale up principal axis
    		case MODEP:
    			switch(vars)
    			{
    				//perturb C
    				case VARC:
    					ppertC(dtp.C,dtp.S0.mtx,pert);	
    					break;
    				default:
    					MipavUtil.displayError("TO DO");
    					System.exit(-1);
    			}
    			break;
    		default:
    			MipavUtil.displayError("TO DO");
    			System.exit(-1);
    	}
    }
    
    /*!
     * \brief
     * custom perturbation of C; normalize S0 by the lengths of C
     * 
     * \param C
     * current C.
     * 
     * \param S0
     * parameter S0 of DT.
     * 
     * \param pert
     * perturbation amount.
     * 
     * \see
     * dytex_perturb
     */
    private void ppertC(Mat C,Mat S0,double pert)
    {	
        int i, x, y;
    	int m=C.rows;
    	int n=C.cols;
    	

    	
    	Matrix matC = new Matrix(C.double2D);
    	Matrix matCT = matC.transpose();
    	Matrix matCC = matCT.times(matC);
    	double array[][] = matCC.getArray();
    	double diag2D[][] = new double[array.length][array.length];
    	for (i = 0; i < array.length; i++) {
    	    diag2D[i][i] = array[i][i];	
    	}
    	Matrix matM = new Matrix(diag2D);
    	Matrix matS0 = new Matrix(S0.double2D);
    	Matrix matM2 = matS0.times(matM);
    	double arrM2[][] = matM2.getArray();
    	int maxLocx = -1;
    	int maxLocy = -1;
    	double maxVal = -Double.MAX_VALUE;
    	for (y = 0; y < arrM2.length; y++) {
    		for (x = 0; x < arrM2[0].length; x++) {
    			if (arrM2[y][x] > maxVal) {
    				maxVal = arrM2[y][x];
    				maxLocx = x;
    				maxLocy = y;
    			}
    		}
    	}
    	for (y = 0; y < C.double2D.length; y++) {
    		C.double2D[y][maxLocy] =(((double)1.0)+pert)*C.double2D[y][maxLocy];
    	}
    }
    
    private void copyTo(Mat A, Mat B) {
    	int i,j;
        B.flags = A.flags;
        B.dims = A.dims;
        B.depth = A.depth;
        B.rows = A.rows;
        if (A.size != null) {
	        B.size = new int[A.size.length];
	        for (i = 0; i <A.size.length; i++) {
	        	B.size[i] = A.size[i];
	        }
        }
        B.type = A.type;
        if ((A.double2D != null) && (A.double2D[0] != null)) {
	        B.double2D = new double[A.double2D.length][A.double2D[0].length];
	        for (i = 0; i < A.double2D.length; i++) {
	        	for (j = 0; j < A.double2D[0].length; j++) {
	        		B.double2D[i][j] = A.double2D[i][j];
	        	}
	        }
        }
    }

    
    /*!
	 * \brief
	 * Verbose modes.	 
	 */
	public enum Verbose_mode{QUIET,COMPACT,VERBOSE};
    
    /*!
     * \brief
     * HEM learninig options.
     *
     * \remarks
     * In HEM implementation few options are not implemented and their default values
     * are used instead
     * \see
     * DytexMix | EMOptions
     */
    public class HEMOptions
    {
    	/*!
    	 * \brief
    	 * number of clusters.	 
    	 */
    	public int K;
    	/*!
    	 * \brief
    	 * number of virtual samples.	 
    	 */
    	public int N;
    	/*!
    	 * \brief
    	 * temporal length of virtual samples.	 
    	 */
    	public int tau;
    	/*!
    	 * \brief
    	 * regularization options.	 
    	 * \see
    	 * DytexRegOptions
    	 */
    	public DytexRegOptions regopt;
    	/*!
    	 * \brief
    	 * termination parameter.	 
    	 */
    	double termvalue;
    	/*!
    	 * \brief
    	 * termination value for the HEMBEST.	 
    	 */
    	double termvalBest;  //termination value for the EMBEST
    	/*!
    	 * \brief
    	 * maximum number of iterations.	 
    	 */
    	int maxiter;
    	/*!
    	 * \brief
    	 * Verbose mode.	 
    	 */
    	Verbose_mode verbose;
    	/*!
    	 * \brief
    	 * empty cluster splitting options.	 
    	 * \see
    	 * DytexSplitParams
    	 */
    	DytexSplitParams emptySplitOpt = new DytexSplitParams();
    	/*!
    	 * \brief
    	 * cluster splitting options.	 
    	 * \see
    	 * DytexSplitParams
    	 */
    	DytexSplitParams splitOpt = new DytexSplitParams();
    	/*!
    	 * \brief
    	 * assume DT are zero-mean or not.	 
    	 */
        public Ymean_type Ymean;

        /*!
         * \brief
         * initialize HEMOptions object.
         * 
         * \param K
         * number of clusters.
         * 
         * \param regopt
         * regularization options.
         * 
         * \param termvalue
         * termination parameter.
         * 
         * \param ymean
         * assume DT are zero-mean or not.	 
         * 
         * \param verbose
         * verbose value.
         * 
         * \see
         * DytexOptions | DytexMix | EMOptions
         */
        public HEMOptions(int K,DytexRegOptions reg,double termvalue,Ymean_type ym,Verbose_mode verbose)
        {
        	//setting parameters
        	this.K=K;
        	this.verbose=verbose;
        	this.termvalue=termvalue;
        	this.termvalBest=1e-5;  //default
        	maxiter=500;

        	//setting empty cluster splitting options
        	emptySplitOpt.crit=Split_crit.SPLITP;	
        	emptySplitOpt.pert=0.01;
        	emptySplitOpt.mode=Split_mode.MODEP;
        	emptySplitOpt.vars=Split_vars.VARC;
        	Ymean=ym;
        	N=1000;
        	tau=20;

        }
    };
    
    /*!
	 * \brief
	 * splitting criteria options.
	 * 'SPLITQ' -- split component with largest Q variance
	 * 'SPLITP' -- split component with largest prior
	 */
	public enum Split_crit{SPLITQ,SPLITP};
	
	/*!
	 * \brief
	 *  perturbation mode options.	 
	 * 'MODEP' -- = scale up principal axis
	 */
	public enum Split_mode{MODEP};
	
	/*!
	 * \brief
	 * perturbation variable option, 'VARC' - observation matrix.	 
	 */
	public enum Split_vars{VARC};
    
    /*!
     * \brief
     * Component splitting options in EM and HEM.
     *
     * \see
     * DytexMix
     */
    public class DytexSplitParams
    {
    	/*!
    	 * \brief
    	 * the growing schedule.	 
    	 */
    	public Vector<Integer> sched = new Vector<Integer>();	
    	
    	/*!
    	 * \brief
    	 * splitting criteria.	 
    	 */
    	public Split_crit crit;
    	
    	/*!
    	 * \brief
    	 * perturbation mode.	 
    	 */
    	public Split_mode mode;
    	/*!
    	 * \brief
    	 * perturbation amount.	 
    	 */
    	public double pert;
    	
    	/*!
    	 * \brief
    	 * variables to perturb.	 
    	 */
    	public Split_vars vars;	
    	/*!
    	 * \brief
    	 * indices of components to ignore for splitting.	 
    	 */
    	Vector<Integer> ignore = new Vector<Integer>();
    	/*!
    	 * \brief
    	 * the index of the new component.	 
    	 */
    	public int target;

    	//initialize DytexSplitParams
    	public DytexSplitParams() {
    		crit=Split_crit.SPLITQ;
    		mode=Split_mode.MODEP;
    		pert=0.01;
    		vars=Split_vars.VARC;	
    		target=-1;	
    	};
    };
    
    /** Options for modeling the observation mean, Ymean.  */
	public enum Ymean_type {ZERO_YMEAN (0),    /**< assume observations are already zero-mean, i.e. Ymean=0. */
	                   NONZERO_YMEAN (1),  /**< model non-zero observation mean. */
	                   ILLEGAL_YMEAN (2);
	       public final int Ymean_code;
	          
	       Ymean_type(int Ymean_code) {
	    		this.Ymean_code = Ymean_code;
	    	}
	}
	
	 public Ymean_type getYmean_type(int Ymean_code) {
	    	if (Ymean_code == 0) {
	    		return Ymean_type.ZERO_YMEAN;
	    	}
	    	else if (Ymean_code == 1) {
	    		return Ymean_type.NONZERO_YMEAN;
	    	}
	    	
	    	else {
	    		MipavUtil.displayError("Illegal number = " + Ymean_code + " for Ymean_type");
	    		return Ymean_type.ILLEGAL_YMEAN;
	    	}
	    }
    
    public class DytexOptions {  	
    	// DT options  
    	public int n;    /**< dimension of the state space, x_t. */
    	public int m;    /**< dimension of the observation space, y_t. */
    	public cov_type Ropt; /**< covariance type for R (usually COV_IID). */
    	public cov_type Sopt; /**< covariance type for S (usually COV_DIAG). */
    	Ymean_type Yopt; /**< option to model observation mean. */
    	public DytexOptions() {
    		
    	}
    }
    
    /** Class for specifying the regularization methods for a Dytex. 
    \sa CovMatrix, Dytex, DytexOptions
    */
    public class DytexRegOptions {
    	//options
    	  public cov_reg_type Ropt,  /**< Regularization method for R. */
    	                          Qopt,  /**< Regularization method for Q. */
    	                          Sopt,  /**< Regularization method for S. */
    							  Aopt;  /**< Regularization method for S. */
    	  public double Rval,  /**< Regularization value for R. */
    	         Qval,  /**< Regularization value for Q. */
    	         Sval,  /**< Regularization value for S. */
    			 Aval;  /**< Regularization value for S. */
    	public DytexRegOptions(cov_reg_type Ropt, double Rval, 
    			  cov_reg_type Qopt, double Qval, 
    			  cov_reg_type Sopt, double Sval,cov_reg_type Aopt, double Aval) {
    		   this.Ropt = Ropt;
    		   this.Rval = Rval;
    		   this.Qopt = Qopt;
    		   this.Qval = Qval;
    		   this.Sopt = Sopt;
    		   this.Sval = Sval;
    		   this.Aopt = Aopt;
    		   this.Aval = Aval;
    	}
    	
    }
    
    /** type of covariance matrix.  */
    public enum cov_type     {COV_FULL   (0),  /**< Full covariance matrix.     */
                       COV_DIAG  (1),  /**< diagonal covariance matrix. */
                       COV_IID (2),
                       COV_ILLEGAL (3);   /**< iid covariance matrix.      */
    	
    	public final int cov_code;
    	
    	cov_type(int cov_code) {
    		this.cov_code = cov_code;
    	}
    } 
    
    public cov_type getCov_type(int cov_code) {
    	if (cov_code == 0) {
    		return cov_type.COV_FULL;
    	}
    	else if (cov_code == 1) {
    		return cov_type.COV_DIAG;
    	}
    	else if (cov_code == 2) {
    		return cov_type.COV_IID;
    	}
    	else {
    		MipavUtil.displayError("Illegal number = " + cov_code + " for cov_type");
    		return cov_type.COV_ILLEGAL;
    	}
    }
    
    /** regularization method. */                     
    public enum cov_reg_type {COV_REG_NONE (0),  /**< no regularization */
                       COV_REG_MIN (1),   /**< enforce a minimum eigenvalue of regval */
                       COV_REG_ADD (2),   /**< add a constant regval to the diagonal */
                       COV_REG_ILLEGAL (3);
    	public final int cov_reg_code;
    	
    	cov_reg_type(int cov_reg_code) {
    		this.cov_reg_code = cov_reg_code;
    	}
    }
    
    public cov_reg_type getCov_reg_type(int cov_reg_code) {
    	if (cov_reg_code == 0) {
    		return cov_reg_type.COV_REG_NONE;
    	}
    	else if (cov_reg_code == 1) {
    		return cov_reg_type.COV_REG_MIN;
    	}
    	else if (cov_reg_code == 2) {
    		return cov_reg_type.COV_REG_ADD;
    	}
    	else {
    		MipavUtil.displayError("Illegal number = " + cov_reg_code + " for cov_reg_type");
    		return cov_reg_type.COV_REG_ILLEGAL;
    	}
    }
    
    public class CovMatrix {
        public int n;    /**< dimension of the (square) covariance matrix. */
        public Mat mtx = new Mat();  /**< storage for the covariance matrix. 
									        full matrix is [n x n]; 
									        diagonal matrix is [n x 1]; 
									        iid matrix is [1 x 1]. */
    	public cov_type     covopt;  /**< type of covariance matrix */
    	public cov_reg_type regopt;  /**< type of regularization to be used */
    	public double regval;  /**< regularization value */
    	public CovMatrix() {
    		
    	}
    }
    
    /** Dynamic Texture class.
    This is the class for a standard dynamic texture.  It serves as the base class
    for the online version of the DT.  It includes functions for:
    1) estimating parameters;
    2) regularizing;
    3) synthesizing video;
    4) pre-processing video for usage with DT (for other classes).
    */
    public class Dytex
    {
    	// DT options  
    	public DytexOptions dtopt = new DytexOptions();   /**< options for the Dytex */
    	// DT parameters
    	  public Mat Ymean = new Mat();    /**< observation mean */
    	  public Mat A = new Mat();        /**< transition matrix */
    	  public Mat C = new Mat();        /**< observation matrix */
    	  public Mat mu0 = new Mat();      /**< initial state mean */
    	  public CovMatrix R = new CovMatrix();       /**< observation noise covariance */
    	  public CovMatrix Q = new CovMatrix();        /**< state noise covariance */
    	  public CovMatrix S0 = new CovMatrix();       /**< initial state covariance */
    	 
    	  // video parameters (for synthesizing)
    	  // set to 0,0 if unknown
    	public int vrows,  /**< for synthesis, number of rows in a frame (0 if unknown).  */
    	      vcols;  /**< for synthesis, number of columns in a frame (0 if unknown). */
    	public boolean isempty; /**< indicates am empty Dt */
    	public Mat Cvs = new Mat();      /**< Cvs precomputed value */
    	public boolean isCvs;   /**< Cvs computed */
    	public Dytex() {
    	    isempty = true;	
    	}
    	
    	// constructor
    	/*public Dytex(DytexOptions opt) {
    	  dtopt = opt;
    	  if (dtopt.Yopt == Ymean_type.NONZERO_YMEAN) {
    		  Ymean.create(dtopt.m,1,CV_64F);
    	  }
    	  else {
    		  Ymean.create(0,0,CV_64F);
    	  }
    	  A.create(dtopt.n, dtopt.n, CV_64F);
    	  C.create(dtopt.m, dtopt.n, CV_64F);
    	  mu0.create(dtopt.n, 1, CV_64F);
    	  R(dtopt.m, dtopt.Ropt),
    	  Q(dtopt.n, CovMatrix::COV_FULL),
    	  S0(dtopt.n, dtopt.Sopt),
    	  vrows = 0;
    	  vcols = 0;
    		isempty=false;
    		isCvs=false;
    	  switch(dtopt.Yopt) {
    	  case DytexOptions::NONZERO_YMEAN:
    	  case DytexOptions::ZERO_YMEAN:
    	    break;
    	  default:
    	    CV_Error(-1, "bad Yopt");
    	  }*/
    }
    
    public class Mat {
    	 /*! includes several bit-fields:
        - the magic signature
        - continuity flag
        - depth
        - number of channels
    */
   public int flags;
   //! the array dimensionality, >= 2
   public int dims;
   //! the number of rows and columns or (-1, -1) when the array has more than 2 dimensions
   public int depth, rows, cols;
   public int size[];
   public int type;
   //! pointer to the data
   public byte data[];
   public byte byte2D[][];
   public double double2D[][];
   public Vector3d Vector3d2D[][];
   public byte byte3D[][][];
   public double double3D[][][];
   public Vector3d Vector3d3D[][][];

   //! pointer to the reference counter;
   // when array points to user-allocated data, the pointer is NULL
   public int refcount[];
    	public Mat() {
    		
    	}
    	
    	public void create(int rows, int cols, int type) {
    		int x, y;
    	    this.rows = rows;
    	    this.cols = cols;
    	    this.type = type;
    	    dims = 2;
    	    size = new int[]{rows,cols};
    	    if (type == CV_8U) {
    	        byte2D = new byte[rows][cols];	
    	    }
    	    else if (type == CV_64F) {
    	        double2D = new double[rows][cols];	
    	    }
    	    else if (type == CV_64FC3) {
    	        Vector3d2D = new Vector3d[rows][cols];	
    	        for (x = 0; x < rows; x++) {
    	        	for (y = 0; y < cols; y++) {
    	        		Vector3d2D[x][y] = new Vector3d();
    	        	}
    	        }
    	    }
    	}
    	
    	public void create(int dims, int size[], int type) {
    		int x, y , z;
    		this.dims = dims;
    		this.size = size;
    		this.type = type;
    		if (dims == 2) {
    			this.rows = size[0];
    			this.cols = size[1];
    		}
    		else if (dims == 3) {
    			this.depth = size[0];
    			this.rows = size[1];
    			this.cols = size[2];
    		}
    		if (dims == 2) {
    			if (type == CV_8U) {
        	        byte2D = new byte[rows][cols];	
        	    }
        	    else if (type == CV_64F) {
        	        double2D = new double[rows][cols];	
        	    }
        	    else if (type == CV_64FC3) {
        	        Vector3d2D = new Vector3d[rows][cols];	
        	        for (x = 0; x < rows; x++) {
        	        	for (y = 0; y < cols; y++) {
        	        		Vector3d2D[x][y] = new Vector3d();
        	        	}
        	        }
        	    }	
    		} // if (dims == 2)
    		else if (dims == 3) {
    			if (type == CV_8U) {
        	        byte3D = new byte[depth][rows][cols];	
        	    }
        	    else if (type == CV_64F) {
        	        double3D = new double[depth][rows][cols];	
        	    }
        	    else if (type == CV_64FC3) {
        	        Vector3d3D = new Vector3d[depth][rows][cols];
        	        for (x = 0; x < depth; x++) {
	        	        for (y = 0; y < rows; y++) {
	        	        	for (z = 0; z < cols; z++) {
	        	        		Vector3d3D[x][y][z] = new Vector3d();
	        	        	}
	        	        }
        	        }
        	    }	
    		} // else if (dims == 3)
    	}
    }
    
    public class DytexMix {
    	public DytexOptions opt = new DytexOptions();
    	/*!
    	 * \brief DT components in the mixture.	 
    	 * \see Dytex
    	 */
    	Vector<Dytex> dt = new Vector<Dytex>();
    	/*!
    	 * \brief DT components priors.	 	 
    	 */
    	public Vector<Double> alpha = new Vector<Double>();
    	/*!
    	 * \brief Class of each training video.	 	 
    	 */
    	public Vector<Integer> classes = new Vector<Integer>();
    	public DytexMix() {
    		
    	}
    	
    	public DytexMix(DytexOptions opt) {
    		this.opt = opt;
    	}
    }
    
    public void read(DytexMix dtm) {
    	readHeader("DytexMix");
    	read(dtm.opt);
    	int K[] = new int[1];
    	read("K",K);
    	dtm.alpha.clear();
    	for(int i=0;i<K[0];i++)
    	{
    		double temp[] = new double[1];
    		read("alpha",temp);
    		dtm.alpha.add(temp[0]);
    	}

    	dtm.dt.clear();
    	for(int i=0;i<K[0];i++)
    	{
    		Dytex tmpd = new Dytex();
    		read(tmpd);
    		dtm.dt.add(tmpd);
    	}
    	dtm.classes.clear();
    	read("classes",dtm.classes);
    }
    
    public void read(String name,Vector<Integer> vec)
    {
    	readHeader(name);		
    	int len;
    	try {
	    	len = getInt(endian);
	
	    	for(int i=0;i<len;i++)
	    	{
	    		int temp;
	    		temp = getInt(endian);
	    		vec.add(temp);
	    	}
    	}
    	catch (IOException e) {
    		MipavUtil.displayError(e + " ");
    		System.exit(-1);	
    	}
    }
    
    public void read(Dytex dt)
    {
    	readHeader("Dytex");
    	read(dt.dtopt); 
    	read("Ymean",dt.Ymean);
    	read("A",dt.A);
    	read("C",dt.C);
    	read("mu0",dt.mu0);
    	read(dt.R);
    	read(dt.Q);
    	read(dt.S0);
    	int vrows[] = new int[1];
    	read("vrows",vrows);
    	dt.vrows = vrows[0];
    	int vcols[] = new int[1];
    	read("vrows",vcols);
    	dt.vcols = vcols[0];
    }
    
    public void read(CovMatrix cm)
    {
    	readHeader("CovMatrix");
    	int n[] = new int[1];
    	read("n",n);
    	cm.n = n[0];
    	byte temp[] = new byte[1];
    	read("covopt",temp);
    	cm.covopt= getCov_type(temp[0]);
    	
    	read("regopt",temp);
    	cm.regopt= getCov_reg_type(temp[0]);

    	double regval[] = new double[1];
    	read("regval",regval);
    	cm.regval = regval[0];
    	read("mtx",cm.mtx);
    }
    
    public void read(String name,double val[])
    {
    	readHeader(name);
    	try {
    	    val[0] = getDouble(endian);
    	}
    	catch (IOException e) {
    		MipavUtil.displayError(e + " ");
    		System.exit(-1);	
    	}
    }
    
    
    public void read(String name,Mat mtx)
    {
    	boolean isempty[] = new boolean[1];

    	Point3i dims = new Point3i(1,1,1);
    	readHeader(name);		
    	read("isempty",isempty);
    	if(isempty[0]) //empty
    	{
    		return;
    	}

    	int type=0;	
    	int els=0;
    	try {
	    	type = getInt(endian);
	
	    	read(dims);
	    	els = getInt(endian);
	    	
	    	int sz[]={dims.x,dims.y,dims.z};
	    	if(dims.x==1)
	    		mtx.create(dims.y,dims.z,type);
	    	else
	    		mtx.create(3,sz,type);
	    		
	
	    	//reading data
	    	double tmpD;
	    	byte tmpU;
	    	Vector3d tmpDV = new Vector3d();
	    	if(dims.x==1)
	    	{
	    		for(int i=0;i<dims.y;i++)
	    		{
	    			for(int j=0;j<dims.z;j++)
	    			{
	    				switch(type)
	    				{
	    				case CV_64F:								
	    					tmpD = getDouble(endian);				
	    					mtx.double2D[i][j] =tmpD;
	    					break;
	    				case CV_8U:								
	    					tmpU = raFile.readByte();				
	    					mtx.byte2D[i][j]=tmpU;
	    					break;
	    				case CV_64FC3:
	    					tmpD = getDouble(endian);
	    					tmpDV.x = tmpD;
	    					tmpD = getDouble(endian);
	    					tmpDV.y = tmpD;
	    					tmpD = getDouble(endian);
	    					tmpDV.z = tmpD;
	    					mtx.Vector3d2D[i][j] = tmpDV;
	    					break;
	    				default:
	    					MipavUtil.displayError("type not handled yet");
	    				}
	    			}
	    		}
	    	}
	    	else
	    	{
	    		for(int i=0;i<dims.x;i++)
	    		{
	    			for(int j=0;j<dims.y;j++)
	    			{
	    				for(int k=0;k<dims.z;k++)
	    				{
	    					switch(type)
	    					{
	    					case CV_64F:								
	    						tmpD = getDouble(endian);				
	    						mtx.double3D[i][j][k] = tmpD;
	    						break;
	    					case CV_8U:								
	    						tmpU = raFile.readByte();				
	    						mtx.byte3D[i][j][k]=tmpU;
	    						break;
	
	    					case CV_64FC3:
	    						tmpD = getDouble(endian);
	        					tmpDV.x = tmpD;
	        					tmpD = getDouble(endian);
	        					tmpDV.y = tmpD;
	        					tmpD = getDouble(endian);
	        					tmpDV.z = tmpD;
	        					mtx.Vector3d3D[i][j][k] = tmpDV;
	    						break;
	    					default:
	    						MipavUtil.displayError("type not handled yet");
	    					}
	    				}
	    			}
	    		}
	    	}
    	}
    	catch (IOException e) {
    		MipavUtil.displayError(e + " ");
    		System.exit(-1);	
    	}

    }
    
    public void read(Point3i p)
    {
    	readHeader("Point3i");
    	int x[] = new int[1];
    	int y[] = new int[1];
    	int z[] = new int[1];
    	read("x",x);
    	p.x = x[0];
    	read("y",y);
    	p.y = y[0];
    	read("z",z);
    	p.z = z[0];
    }
    
    public void read(String name,boolean val[])
    {
    	byte b = 0;
    	readHeader(name);
    	try {
    	    b = raFile.readByte();
    	}
    	catch (IOException e) {
    		MipavUtil.displayError(e + " ");
    		System.exit(-1);	
    	}
    	if (b == 0) {
    		val[0] = false;
    	}
    	else {
    		val[0] = true;
    	}
    	return;
    }
    
    public void read(DytexOptions opt) {
    	readHeader("DytexOptions");
    	int n[] = new int[1];
    	read("n",n);
    	opt.n = n[0];
    	int m[] = new int[1];
    	read("m",m);
    	opt.m = m[0];
    	byte temp[] = new byte[1];
    	read("Ropt",temp);
    	opt.Ropt= getCov_type(temp[0]);
    	read("Sopt",temp);
    	opt.Sopt=getCov_type(temp[0]);
    	read("Yopt",temp);
    	opt.Yopt= getYmean_type(temp[0]);
    }
    
    public void read(String name,int val[])
    {
    	readHeader(name);
    	try {
    	    val[0] = getInt(endian);	
    	}
    	catch (IOException e) {
    		MipavUtil.displayError(e + " ");
    		System.exit(-1);	
    	}
    }
    
    public void read(String name, byte val[])
    {
    	readHeader(name);
    	try {
    	    val[0] = raFile.readByte();	
    	}
    	catch (IOException e) {
    		MipavUtil.displayError(e + " ");
    		System.exit(-1);	
    	}

    }
    
    public void readHeader(String str) {
    	int i;
    	long fileLength = 0;
    	byte b = 0;
    	String tstr;
    	// match header
    	byte tempB[] = new byte[100];
    	try { 
    	    fileLength = raFile.length();
    	}
    	catch (IOException e) {
    		MipavUtil.displayError(e + " ");
    		System.exit(-1);	
    	}
    	// The original source code skips the delimiter,
    	// but this code reads the delimiter
    	// so there is no need to skip one character
    	for (i = 0; i < Math.min(99, fileLength); i++) {
    		try {
    	        b = raFile.readByte();
    		}
    		catch (IOException e) {
        		MipavUtil.displayError(e + " ");
        		System.exit(-1);	
        	}
    		if (b != 0) {
    			tempB[i] = b;
    		}
    		else {
    			break;
    		}
    	}
    	tempB[i] = 0;
    	tstr = new String(tempB,0,i);
    	if ((tstr == null) || (tstr.length() == 0)) {
    		MipavUtil.displayError("No header string found");
    		System.exit(-1);
    	}
    	else if (!str.equals(tstr)) {
    		MipavUtil.displayError("Header string = " + tstr + " instead of the required " + str);
    		System.exit(-1);
    	}
    	
    	//match version	
    	for (i = 0; i < 100; i++) {
    		tempB[i] = 0;
    	}
    	for (i = 0; i < Math.min(99, fileLength); i++) {
    		try {
    	        b = raFile.readByte();
    		}
    		catch (IOException e) {
        		MipavUtil.displayError(e + " ");
        		System.exit(-1);	
        	}
    		if (b != 0) {
    			tempB[i] = b;
    		}
    		else {
    			break;
    		}
    	}
    	tempB[i] = 0;
    	tstr = new String(tempB,0,i);
    	if ((tstr == null) || (tstr.length() == 0)) {
    		MipavUtil.displayError("No version string found");
    		System.exit(-1);
    	}
    	else if (!tstr.equals("1.0")) {
    		MipavUtil.displayError("Version string = " + tstr + " instead of the required 1.0");
    		System.exit(-1);
    	}
    	
    	//read/skip size of the type
    	int size;
    	try {
    	    size = getInt(endian);
    	}
    	catch (IOException e) {
    		MipavUtil.displayError(e + " ");
    		System.exit(-1);	
    	}
    	//System.out.println("size = " + size);
    }
    
    /**
     * Reads four signed bytes from file.
     * 
     * @param bigEndian <code>true</code> indicates big endian byte order, <code>false</code> indicates little
     *            endian.
     * 
     * @return The value of the integer read from the file.
     * 
     * @exception IOException if there is an error reading the file
     */
    public final int getInt(final boolean bigEndian) throws IOException {

        raFile.readFully(byteIntBuffer);

        if (bigEndian) {
            return ( ( (byteIntBuffer[0] & 0xff) << 24) | ( (byteIntBuffer[1] & 0xff) << 16)
                    | ( (byteIntBuffer[2] & 0xff) << 8) | (byteIntBuffer[3] & 0xff)); // Big Endian
        } else {
            return ( ( (byteIntBuffer[3] & 0xff) << 24) | ( (byteIntBuffer[2] & 0xff) << 16)
                    | ( (byteIntBuffer[1] & 0xff) << 8) | (byteIntBuffer[0] & 0xff));
        }
    }
    
    /**
     * Reads eight unsigned bytes from file.
     * 
     * @param bigEndian <code>true</code> indicates big endian byte order, <code>false</code> indicates little
     *            endian.
     * 
     * @return The value of the double read from the file.
     * 
     * @exception IOException if there is an error reading the file
     */
    public final double getDouble(final boolean bigEndian) throws IOException {
        raFile.readFully(byteDoubleBuffer);

        long tmpLong;

        if (bigEndian) {
            tmpLong = ( ( (byteDoubleBuffer[0] & 0xffL) << 56)
                    | ( (byteDoubleBuffer[1] & 0xffL) << 48) | ( (byteDoubleBuffer[2] & 0xffL) << 40)
                    | ( (byteDoubleBuffer[3] & 0xffL) << 32) | ( (byteDoubleBuffer[4] & 0xffL) << 24)
                    | ( (byteDoubleBuffer[5] & 0xffL) << 16) | ( (byteDoubleBuffer[6] & 0xffL) << 8) | (byteDoubleBuffer[7] & 0xffL));

            return (Double.longBitsToDouble(tmpLong));
        } else {
            tmpLong = ( ( (byteDoubleBuffer[7] & 0xffL) << 56)
                    | ( (byteDoubleBuffer[6] & 0xffL) << 48) | ( (byteDoubleBuffer[5] & 0xffL) << 40)
                    | ( (byteDoubleBuffer[4] & 0xffL) << 32) | ( (byteDoubleBuffer[3] & 0xffL) << 24)
                    | ( (byteDoubleBuffer[2] & 0xffL) << 16) | ( (byteDoubleBuffer[1] & 0xffL) << 8) | (byteDoubleBuffer[0] & 0xffL));

            return (Double.longBitsToDouble(tmpLong));
        }
    }
}