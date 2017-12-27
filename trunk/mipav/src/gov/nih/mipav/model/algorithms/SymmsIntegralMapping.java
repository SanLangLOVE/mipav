package gov.nih.mipav.model.algorithms;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;

import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.Preferences;

public class SymmsIntegralMapping extends AlgorithmBase  {
	String fileDir;
	
	// EPS returns the distance from 1.0 to the next larger double-precision number, that is, eps = 2^-52.
    private double EPS;
    // Filename to receive FORTRAN output code
    private String FORTFL = null;
    // Does the domain have any symmetry?
    private boolean SYMTY;
    // If SYMTY is true, are there any reflectional symmetries?
    private boolean REFLN;
    // If SYMTY is true, what are the coordinates of the center of symmetry?
    private final int MNARC = 100;
    private double CENSY[] = new double[2];
    // If SYMTY is true, number of arcs on the fundamental boundary section
    // If SYMTY is false, number of arcs on the boundary
    // NARCS <= MNARC-1 = 99
    private int NARCS;
    // NUMDER is of length MNARC
    // NUMDER is initially false
    private boolean NUMDER[] = new boolean[MNARC];
    // ARCTY is of length MNARC
    // Type of arc, 1 = LINE SEGMENT, 2 = CIRCULAR ARC SEGMENT, 3 = CARTESIAN PARAMETRIC FUNCTION
    // 4 = POLAR FUNCTION
    private int ARCTY[] = new int[MNARC];
    // STAPT is of length MNARC,2
    // Initial point on line, initial point on circle, or initial point on curve
    // IF (SYMTY) THEN
    // WRITE(*,*) 'COORDINATES OF FINAL POINT ON THIS ARC?'
    //STAPT[NARCS]=CMPLX(FINAL X point, FINAL Y point)
    //ELSE
    // STAPT[NARCS]=STAPT[1]
    //ENDIF
    private double STAPT[][] = new double[MNARC][2];
    // Start with GMCO = 0
    // For types 2, 3, and 4 increment GMCO and put
    //GMCO=GMCO+1
	//PGM[IA-1]=GMCO where IA is the number of the curve from 1 to NARCS
    // For type 2 circular arc:
    // RGM[GMCO-1]= X center of circle
	// GMCO=GMCO+1
	// RGM[GMCO-1]= Y center of circle
	//GMCO=GMCO+1
	//RGM[GMCO-1]=ALPHA*PI where alpha is the signed angle subtended at center in units of PI
    // ALPHA is positive for CCW and negative for CW.
    // For type 3 CARTESIAN PARAMETRIC FUNCTION:
    // RGM[GMCO-1]= initial parameter value
    // GMCO=GMCO+1
    // RGM[GMCO-1]= final parameter value
    // For type 4 polar function:
    // RGM[GMCO-1]= (initial polar angle in units of PI) *PI
    // GMCO=GMCO+1
    // RGM[GMCO-1]= (final polar angle in units of PI) *PI
    // PGM is of length MNARC
    private int PGM[] = new int[MNARC];
    // RGM is of length 3*MNARC
    private double RGM[] = new double[3*MNARC];
    // Start with TXCO = 0
    // For types 3 and 4
    // For each IA = 1 to NARCS do J = 1,2
    //TXCO=TXCO+1
    //PTX[IA-1+(J-1)*MNARC]=TXCO
    // DEFN[TXCO-1][0]= TXT for real part
    // DEFN[TXCO-1][1] = TXT for imaginary part
    // where TXT = A COMPLEX EXPRESSION for TYPE 3 and a REAL EXPRESSION FOR TYPE 4
    // for J = 1 and TYPE = 3 JAVA EXPRESSION FOR PARFUN
    // for J = 2 and TYPE = 3 JAVA EXPRESSION FOR DPARFN
    // for J = 1 and TYPE = 4 JAVA EXPRESSION FOR RADIUS
    // for J = 2 and TYPE = 4 JAVA EXPRESSION FOR RADIUS DERIVATIVE
    // PTX is of length 2*MNARC
    private int PTX[] = new int[2*MNARC];
    private int NTX[]= new int[2*MNARC];
    //CHARACTER DEFN(MNARC*2)*72
    // Holds text for real for types 3 and 4 and imaginary parts for type 3
    // Start imaginary text with ui.  All text following ui is imaginary.
    String DEFN[]= new String[2*MNARC];
    private boolean traditionalInput = false;
    Scanner input = new Scanner(System.in);
    private double zzset[][] = new double[400][2];
    
	public SymmsIntegralMapping() {
		
	}
	
	public SymmsIntegralMapping(ModelImage destImg, ModelImage srcImg, String FORTFL, boolean SYMTY,
			boolean REFLN, double CENSY[], int NARCS, boolean NUMDER[], int ARCTY[], double STAPT[][],
			int PGM[], double RGM[], int PTX[], int NTX[], String DEFN[]) {
	    super(destImg, srcImg);
	    this.FORTFL = FORTFL;
	    this.SYMTY = SYMTY;
	    this.REFLN = REFLN;
	    this.CENSY = CENSY;
	    this.NARCS = NARCS;
	    this.NUMDER = NUMDER;
	    this.ARCTY = ARCTY;
	    this.STAPT = STAPT;
	    this.PGM = PGM;
	    this.RGM = RGM;
	    this.PTX = PTX;
	    this.NTX = NTX;
	    this.DEFN = DEFN;
	}
	
	public void runAlgorithm() {
	    fileDir = srcImage.getFileInfo(0).getFileDirectory();
	    
	 // eps returns the distance from 1.0 to the next larger double-precision number, that is, eps = 2^-52.
    	// epsilon = D1MACH(4)
        // Machine epsilon is the smallest positive epsilon such that
        // (1.0 + epsilon) != 1.0.
        // epsilon = 2**(1 - doubleDigits) = 2**(1 - 53) = 2**(-52)
        // epsilon = 2.2204460e-16
        // epsilon is called the largest relative spacing
        EPS = 1.0;
        double neweps = 1.0;


        while (true) {

            if (1.0 == (1.0 + neweps)) {
                break;
            } else {
                EPS = neweps;
                neweps = neweps / 2.0;
            }
        } // while(true)
	}
	
	public void PARGEN() {
	//.......................................................................
	//     AN INTERACTIVE PREPROCESSOR TO HELP THE  C O N F P A C K  USER 
	//     GENERATE THE FORTRAN CODE DEFINING THE BOUNDARY PARAMETRISATION 
	//     AND ITS DERIVATIVE.
	//
	//     THE FOLLOWING CONVENTIONS ARE ASSUMED:
	//
	//  ** IF A QUESTION REQUIRES A YES OR NO ANSWER, THE DETECTION OF 'Y' OR
	//     'y' IN THE FIRST 6 INPUT CHARACTERS IS TAKEN AS YES, ANYTHING ELSE
	//     AS NO.
	//
	//  ** WHEN ASKED FOR COORDINATES, THIS ALWAYS MEANS CARTESIAN COORDIN-
	//     ATES AND THESE SHOULD BE SUPPLIED AS TWO REAL NUMBERS, AS EITHER 
	//
	//                      X,Y     OR      X Y
	//
	//     WITHOUT PARENTHESES. 
	//
	//  ** FOUR TYPES OF ARCS ARE CURRENTLY TREATED, WITH NUMERICAL CODES TO 
	//     DENOTE THE TYPE AS FOLLOWS:
	//       1:= LINE SEGMENT.
	//       2:= CIRCULAR ARC SEGMENT.
	//           CONVENTIONS:
	//           1 - THE ANGLE SUBTENDED AT THE CENTRE IS POSITIVE FOR
	//               ANTICLOCKWISE TRAVERSAL OF THE ARC AND NEGATIVE FOR
	//               CLOCKWISE TRAVERSAL.
	//       3:= THE USER IS ASKED TO SUPPLY THE FORTRAN 77 ARITHMETIC 
	//           EXPRESSIONS WHICH DEFINE THE CARTESIAN PARAMETRIC FUNCTION  
	//           AND THE DERIVATIVE OF THIS FUNCTION.
	//           CONVENTIONS:
	//           1 - THE PARAMETER MUST BE DENOTED BY T.
	//           2 - THE REAL CONSTANT PI=3.14159.. AND THE COMPLEX CONSTANT
	//               UI=(0.0,1.0) MAY BE USED IN THE ARITHMETIC EXPRESSIONS.
	//       4:= THE USER IS ASKED TO SUPPLY THE FORTRAN 77 ARITHMETIC 
	//           EXPRESSIONS WHICH DEFINE THE POLAR COORDINATE AS A FUNCTION
	//           OF POLAR ANGLE AND THE DERIVATIVE OF THIS FUNCTION.
	//           CONVENTIONS:
	//           1 - THE POLAR ANGLE MUST BE DENOTED BY T.
	//           2 - THE REAL CONSTANT PI=3.14159.. MAY BE USED IN THE ARITH-
	//               METIC EXPRESSIONS. 
	//           3 - PARGEN ASSIGNS THE EXPRESSION FOR THE RADIUS TO THE 
	//               COMPLEX VARIABLE ZRAD;  IF REQUIRED THE USER MAY THERE- 
	//               FORE USE THE VARIABLE ZRAD IN THE EXPRESSION FOR THE 
	//               DERIVATIVE OF THE RADIUS WRT POLAR ANGLE
	//      IN ADDITION, FOR TYPES 3 AND 4, THE FOLLOWING CONVENTIONS HOLD:
	//           1 - ONLY USE UP TO 66 CHARACTERS PER LINE.
	//           2 - IF THE EXPRESSION OCCUPIES MORE THAN ONE LINE THEN THERE
	//               IS NO NEED TO SUPPLY ANY CONTINUATION CHARACTER.
	//           3 - ONLY USE THOSE FORTRAN 77 INTRINSIC MATHS FUNCTIONS  
	//               WHICH ACCEPT COMPLEX ARGUMENTS AND ARE ANALYTIC; I.E.,
	//               IN STANDARD FORTRAN,
	//                    SQRT, EXP, LOG, SIN, COS
	//           4 - THE WHOLE EXPRESSION SHOULD BE TERMINATED WITH A
	//               REPEATED DIVISION SIGN, I.E. //.
	//
	// ** THE CODE ISN'T VERY ROBUST IN THAT THERE IS LITTLE PROVISION FOR
	//    INTERACTIVE CORRECTION OF ERRORS.  HOWEVER, ALL THE USER'S INPUT
	//     IS AUTOMATICALLY OUTPUT TO A FILE NAMED pgenin.  IF THE USER
	//     REALISES THAT AN ITEM HAS BEEN INPUT INCORRECTLY, THE BEST POLICY
	//     IS TO CARRY ON TO THE END OF THE INPUT PHASE AND THEN TERMINATE 
	//     THE EXECUTION;  THE FILE pgenin CAN BE EDITED, RENAMED AND 
	//     SUBMITTED AS STANDARD INPUT FOR A SECOND RUN OF PARGEN.   
	//     (THE SUGGESTION TO RENAME IS TO AVOID ANY POSSIBLE DIFFICULTY 
	//    ARISING FROM READING THE FILE AS STANDARD INPUT WHILST ALSO 
	//     WRITING OUTPUT TO THE SAME FILE.) 
	//     IF THE USER REALISES ONLY AT A LATER STAGE (E.G. IN PLOTTING THE 
	//     BOUNDARY) THAT AN ITEM HAS BEEN INPUT INCORRECTLY THEN pgenin 
	//     MAY STILL BE AVAILABLE FOR RE-USE AS ABOVE.
	//  
	//     SUBROUTINES OR FUNCTIONS NEEDED
	//              - THE CONFPACK LIBRARY.
	//              - THE REAL FUNCTION R1MACH.
	//
	//.......................................................................
	//     AUTHOR: DAVID HOUGH, COVENTRY POLYTECHNIC, UK
	//     LAST UPDATE: 15 FEB 1991
	//.......................................................................
	//
	//     LOCAL VARIABLES
	//
	      int GMCO,IA,I,J,L,SW,
	          TXCO;
	      int TYPE = 1;
	      int IER[] = new int[1];
	      int ORDRG[] = new int[1];
	      int ORDSG[] = new int[1];
	      double ALPHA,PI,X,Y;
	      //COMPLEX CENSY,RTUNI,U2
	      double RTUNI[] = new double[2];
	      double U2[] = new double[2];
	     // CHARACTER TXT*72,TABC*6,FORTFL*72,CH*2,SIG(10)*2,WID(10)*2,REDD*6,
	     //+FMT1*8,FMT2*9
	      String TXT;
	      String CH;
	      String SIG[] = new String[]{"7","8","9","10","11","12","13","14","15","16"};
	      String WID[] = new String[]{"15","16","17","18","19","20","21","22","23","24"};
	      String REDD;
	      //String FMT1;
	      //String FMT2;
	
	      //PARAMETER (MNARC=100,TABC='     +',CHNL=20,CHIN=21)
	      final String TABC = "     +";
	      final int CHNL = 20;
	      //final int CHIN = 21;
	      
	      File file;
	      RandomAccessFile raFile = null;
	      boolean validInput;
	
	      //EXTERNAL CHRIN,HEADER,R1MACH,SYINF1,WRFUN1,WRFUN2,WRHEAD,WRSYM1,
	      //+WRSYM2,WRSYM3,WRTAIL
	
	      WRHEAD(6,0, null);
	
	      PI= Math.PI;
	      
	      //**** DETERMINE NUMBER OF SIGNIFICANT FIGURES REQUIRED TO MATCH MACHINE 
          //**** PRECISION AND SET UP POINTER SW TO SIG AND WID
	
	      SW=(int)(-Math.log10(EPS))+2;
	      if (SW <= 7) {
	        SW=1;
	      }
	      else if (SW >= 16) {
	        SW=10;
	      }
	      else {
	        SW=SW-6;
	      }
	      
	  	  //**** SET UP THE EDIT DESCRIPTOR AND FORMAT SPECIFICATION FOR FLOATING 
	  	  //**** POINT OUTPUT
	  	
	  	  REDD="E"+WID[SW-1]+"."+SIG[SW-1]; 
	  	  //FMT1="("+REDD+")";
	  	  //FMT2="(2"+REDD+")";
	  	  
	  	  if (traditionalInput) {
	  	      System.out.println("ENTER FILENAME TO RECEIVE OUTPUT JAVA CODE");  
	  	      FORTFL = input.next();
	  	  } // if (traditionalInput)
	      
	      //**** WRITE THE SOURCE CODE FOR PARFUN
	      file = new File(fileDir + FORTFL);
	      try {
	          raFile = new RandomAccessFile(file, "rw");
	      }
	      catch (IOException e) {
	    	  MipavUtil.displayError("IOException " + e + " on raFile = new RandomAccessFile(file, rw)");
	    	  System.exit(-1);
	      }
	      // Necessary so that if this is an overwritten file there isn't any
	      // junk at the end
	      try {
	          raFile.setLength(0);
	      }
	      catch (IOException e) {
	    	  MipavUtil.displayError("IOException " + e + " on raFile.setLength(0)");
	    	  System.exit(-1);  
	      }

	      //OPEN(CHNL,FILE=FORTFL)
	      
	      if (traditionalInput) {
	    	  validInput = false;
	    	  while (!validInput) {
	    	      System.out.println("DOES THE DOMAIN HAVE ANY SYMMETRY (Y/N)?");
	    	      String sym = input.next();
	    	      String firstSym = sym.substring(0,1);
	    	      if (firstSym.equalsIgnoreCase("Y")) {
	    	    	  SYMTY = true;
	    	    	  validInput = true;
	    	      }
	    	      else if (firstSym.equalsIgnoreCase("N")) {
	    	    	  SYMTY = false;
	    	    	  validInput = true;
	    	      }
	    	      else {
	    	    	  System.out.println(sym + " is not a valid response");
	    	      }
	    	  } // while (!validInput)
	    	  
	    	  if (SYMTY) {
	    		  validInput = false;
		    	  while (!validInput) {
		    	      System.out.println("ARE THERE ANY REFLECTIONAL SYMMETRIES (Y/N)?");
		    	      String ref = input.next();
		    	      String firstRef = ref.substring(0,1);
		    	      if (firstRef.equalsIgnoreCase("Y")) {
		    	    	  REFLN = true;
		    	    	  validInput = true;
		    	      }
		    	      else if (firstRef.equalsIgnoreCase("N")) {
		    	    	  REFLN = false;
		    	    	  validInput = true;
		    	      }
		    	      else {
		    	    	  System.out.println(ref + " is not a valid response");
		    	      }
		    	  } // while (!validInput)	
		    	  
		    	  System.out.println("What are the coordinates of the center of symmetry (X Y)?");
		    	  CENSY[0] = input.nextDouble();
		    	  CENSY[1] = input.nextDouble();
		    	  validInput = false;
		    	  while (!validInput) {
		    	      System.out.println("How many arcs are there on the fundamental boundary section?");
		    	      NARCS = input.nextInt();
		    	      if (NARCS <= MNARC-1) {
		    	    	  validInput = true;
		    	      }
		    	      else {
		    	    	  System.out.println("NARCS must be <= " + (MNARC-1));
		    	      }
		    	  } // while (!validInput)
	    	  } // if (SYMTY)
	    	  else { // !SYMTY
	    		  validInput = false;
		    	  while (!validInput) {
		    		  System.out.println("How many arcs are there on the boundary?");
		    		  NARCS = input.nextInt();
		    		  if (NARCS <= MNARC-1) {
		    	    	  validInput = true;
		    	      }
		    	      else {
		    	    	  System.out.println("NARCS must be <= " + (MNARC-1));
		    	      }
		    	  } // while (!validInput)
	    	  } // else !SYMTY
	    	  
	    	  GMCO = 0;
	    	  TXCO = 0;
	    	  
	    	  for (IA = 1; IA <= NARCS; IA++) {
	    		    NUMDER[IA-1] = false; 
	    		    validInput = false;
	    		    while (!validInput) {
	    		        System.out.println("ENTER THE TYPE OF ARC(1-4) for ARC NUMBER " + IA);
	    		        TYPE = input.nextInt();
	    		        if ((TYPE >= 1) && (TYPE <= 4)) {
	    		        	validInput = true;
	    		        }
	    		        else {
	    		        	System.out.println("TYPE MUST BE BETWEEN 1 and 4");
	    		        }
	    		    } // while (!validInput)
	    		    if (TYPE == 1) {
	    		        ARCTY[IA-1] = TYPE;	
	    		        System.out.println("What are the coordinates of the initial point on the line (X Y)?");
	    		        STAPT[IA-1][0] = input.nextDouble();
	    		        STAPT[IA-1][1] = input.nextDouble();
	    		    } // if (TYPE == 1)
	    		    else if (TYPE == 2) {
	    		    	ARCTY[IA-1] = TYPE;	
	    		        System.out.println("What are the coordinates of the initial point on the circle (X Y)?");
	    		        STAPT[IA-1][0] = input.nextDouble();
	    		        STAPT[IA-1][1] = input.nextDouble();
	    		        System.out.println("What are the coordinates of the center of the circle (X Y)?");
	    		        X = input.nextDouble();
	    		        Y = input.nextDouble();	
	    		        System.out.println("What is the signed angle subtended at center (in units of PI)?");
	    		        ALPHA = input.nextDouble();
	    		        GMCO=GMCO+1;
		                PGM[IA-1]=GMCO;
		                RGM[GMCO-1]=X;
		                GMCO=GMCO+1;
		                RGM[GMCO-1]=Y;
		                GMCO=GMCO+1;
		                RGM[GMCO-1]=ALPHA*PI;
	    		    } // else if (TYPE == 2)
	    		    else if ((TYPE == 3) || (TYPE == 4)) {
	    		    	ARCTY[IA-1] = TYPE;	
	    		        System.out.println("What are the coordinates of the initial point on the curve (X Y)?");
	    		        STAPT[IA-1][0] = input.nextDouble();
	    		        STAPT[IA-1][1] = input.nextDouble();
	    		        if (TYPE == 3) {
	    		        	System.out.println("Enter the initial and final parameter values (X Y)");
	    		        }
	    		        else {
	    		        	System.out.println("Enter the initial and final polar values (in angles of PI) (X Y)");
	    		        }
	    		        X = input.nextDouble();
	    		        Y = input.nextDouble();
	    		        GMCO=GMCO+1;
		                PGM[IA-1]=GMCO;
		                if (TYPE == 4) {
		                  RGM[GMCO-1]=X*PI;
		                  GMCO=GMCO+1;
		                  RGM[GMCO-1]=Y*PI;
		                }
		                else {
		                  RGM[GMCO-1]=X;
		                  GMCO=GMCO+1;
		                  RGM[GMCO-1]=Y;
		                }
		                for (J = 1; J <= 2; J++) {
		                	if (J == 1 && TYPE == 3) {
		                        System.out.println("ENTER JAVA EXPRESSION ENDING IN // FOR PARFUN");
		                        System.out.println("PUT REAL PART ui IMAGINARY PART");
		                	}
		                	else if (J == 2 && TYPE == 3) {          
		                        System.out.println("ENTER JAVA EXPRESSION ENDING IN // FOR DPARFN");
		                        System.out.println("PUT REAL PART ui IMAGINARY PART");
		                	}
		                	else if (J == 1 && TYPE == 4) {
		                        System.out.println("ENTER JAVA EXPRESSION ENDING IN // FOR RADIUS");
		                	}
		                	else {
		                        System.out.println("ENTER JAVA EXPRESSION ENDING IN // FOR RADIUS DERIVATIVE");
		                	}
		      
		                    TXCO=TXCO+1;
		                    PTX[IA-1+(J-1)*MNARC]=TXCO;
		                    I=1;
	
		                    TXT = input.next();
		                    L = -1;
		                    while (L == -1) {
			                    L = TXT.indexOf("//");
			                    if (L == -1) {
			                    	DEFN[TXCO-1]=TABC+TXT;
			                        I=I+1;
			                        TXCO=TXCO+1;
			                    } // if (L == -1)
		                    } // while (L == -1)
		                    NTX[IA-1+(J-1)*MNARC]=I;
		                    if (L == 0) {
		                        DEFN[TXCO-1]=TABC;
		                        NUMDER[IA-1] = true;
		                    }
		                    else {
		                        DEFN[TXCO-1]=TABC+TXT.substring(0,L);
		                    }
                            if ((J == 1) && (TYPE == 4)) {
                            	System.out.println("(... = ZRAD)");
                            }
		                } // for (J = 1; J <= 2; J++)
	    		    } // else if ((TYPE == 3) || (TYPE == 4))
	    	  } // for (IA = 1; IA <= NARCS; IA++)
	    	  
	    	  if (SYMTY) {
	              System.out.println("ENTER THE COORDINATES OF FINAL POINT ON THIS LAST ARC (X Y)");
	              STAPT[NARCS][0] = input.nextDouble();
	              STAPT[NARCS][1] = input.nextDouble();
	    	  }
	    	  else {
	              STAPT[NARCS][0]=STAPT[0][0];
	              STAPT[NARCS][1]=STAPT[0][1];
	    	  }
              
	    	  validInput = false;
  		      while (!validInput) {
	    	      System.out.println("END OF INPUT PHASE; CONTINUE WITH PROCESSING (Y/N)?");
	    	      String term = input.next();
	    	      String firstTerm = term.substring(0,1);
	    	      if (firstTerm.equalsIgnoreCase("Y")) {
	    	    	  validInput = true;
	    	      }
	    	      else if (firstTerm.equalsIgnoreCase("N")) {
	    	    	  validInput = true;
	    	    	  setCompleted(false);
	    	    	  return;
	    	      }
	    	      else {
	    	    	  System.out.println(term + " is not a valid response");
	    	      }
	    	  } // while (!validInput)
	      } // if (traditionalInput)
	      HEADER("PARFUN",REDD,raFile);
	      if (SYMTY) {
	        SYINF1(ORDRG,ORDSG,RTUNI,U2,REFLN,CENSY,STAPT[0],
	                   STAPT[NARCS],IER);
	        if (IER[0] > 0) {
	        	WRTAIL(6,0,IER[0],null);
	        	return;
	        }
	        System.out.println("\nN O T E : THE ORDER OF THE SYMMETRY GROUP IS " + ORDSG);
	        if (REFLN) {
	            System.out.println("          ISYGP = " + (-ORDSG[0]));	
	        }
	        else {
	        	System.out.println("          ISYGP = " + (ORDSG[0]));		
	        }
	        WRSYM1(NARCS,ORDRG[0],ORDSG[0],RTUNI,U2,CENSY,REFLN,true,REDD,CHNL,raFile);
	        if (REFLN) {
	          CH = "TS";
	        }
	        else {
	          CH = "TT";
	        }
	        WRFUN1(NARCS,STAPT,ARCTY,PGM,RGM,PTX,NTX,DEFN,CHNL,
	                 "IB",CH,"ZETA  ",REDD, raFile);
	        WRSYM2(NARCS,ORDRG[0],CENSY,REFLN,CHNL, raFile);
	      }
	      else {
	        WRFUN1(NARCS,STAPT,ARCTY,PGM,RGM,PTX,NTX,DEFN,CHNL,
	               "IA","TT","PARFUN",REDD,raFile);
	      }
	
	      try {
	          raFile.writeBytes("//\n");
	          raFile.writeBytes("}\n");
	  
	         // **** WRITE THE SOURCE CODE FOR DPARFN
	     
	         raFile.writeBytes("//...........................................\n");
	      }
	      catch (IOException e) {
	    	  MipavUtil.displayError("IOException " + e + " in PARGEN");
	    	  System.exit(-1);
	      }
	      HEADER("DPARFN",REDD,raFile);
	      if (SYMTY) {
	        WRSYM1(NARCS,ORDRG[0],ORDSG[0],RTUNI,U2,CENSY,REFLN,false,REDD,
	               CHNL, raFile);
	        if (REFLN) {
	          CH="TS";
	        }
	        else {
	          CH="TT";
	        }
	        WRFUN2(NARCS,MNARC,STAPT,ARCTY,PGM,RGM,PTX,NTX,DEFN,
	                   CHNL,"IB",CH,"ZETA  ",NUMDER,REDD, raFile);
	        WRSYM3(NARCS,ORDRG[0],REFLN,CHNL, raFile);
	      } // if (SYMTY)
	      else {
	          WRFUN2(NARCS,MNARC,STAPT,ARCTY,PGM,RGM,PTX,NTX,DEFN,
	                 CHNL,"IA","TT","DPARFN",NUMDER,REDD, raFile);
	      }
	
	      try {
		      raFile.writeBytes("//\n");
		      raFile.writeBytes("}\n");
		      raFile.close();
	      }
	      catch (IOException e) {
	    	  MipavUtil.displayError("IOException " + e + " in PARGEN");
	    	  System.exit(-1);
	      }
	      WRTAIL(6,0,IER[0], null);
	
	} // public void PARGEN
    private void HEADER(String TXT, String REDD, RandomAccessFile raFile) {

    String TAB6="      ";

    String LINE=TAB6+"private void "+TXT+"(int IA, double TT[]) {\n";
    try {
    raFile.writeBytes(LINE);

    LINE="//" + TAB6+"IMPLICIT REAL(A-H,O-S),INTEGER(I-N),COMPLEX(T-Z)\n";
    raFile.writeBytes(LINE);

    raFile.writeBytes("      double PI = "+Math.PI+ ";\n");
    raFile.writeBytes("      double UI[] = new double[]{0.0,1.0};\n");
    raFile.writeBytes("//\n");
    }
    catch (IOException e) {
    	MipavUtil.displayError("IOException " + e + " in HEADER");
    	System.exit(-1);
    }

    } // private void HEADER
    
    private void SYINF1(int ORDRG[],int ORDSG[],double RTUNI[], double U2[],boolean REFLN,
    		double Z0[], double Z1[], double Z2[], int IER[]) {
    //COMPLEX RTUNI,U2,Z0,Z1,Z2

//**** GIVEN Z0,THE CENTRE OF SYMMETRY, Z1 AND Z2, THE INITIAL AND FINAL
//**** POINTS ON THE FUNDAMENTAL BOUNDARY SECTION, REFLN, WHICH IS TRUE
//**** IF THE SYMMETRY GROUP HAS IMPROPER  ROTATIONAL ELEMENTS
//**** (I.E. REFLECTIONAL SYMMETRIES), THIS ROUTINE COMPUTES 
//**** ORDRG - THE ORDER OF THE SUBGROUP OF PROPER ROTATIONS (THIS IS THE
//****         ORDER OF THE SYMMETRY GROUP IF REFLN=.FALSE.)
//**** ORDSG - THE ORDER OF THE FULL SYMMETRY GROUP,  EITHER ORDRG OR 
//****         2*ORDRG DEPENDING ON WHETHER REFLN IS .FALSE. OR .TRUE.
//**** RTUNI - THE ROOT OF UNITY FROM WHICH THE PROPER ROTATIONAL SUBROUP
//****         IS GENERATED
//**** U2    - THE ADDITIONAL IN-PLANE ROTATION WHICH, WHEN COMBINED WITH
//****         CONJUGATION, DEFINES THE IMPROPER ROTATION FOR THE CASE
//****         REFLN=.TRUE.

//     LOCAL VARIABLES

    double ALPHA, PI,SQRTEPS;
    //COMPLEX CT,U
    double CT[] = new double[2];
    double U[] = new double[2];
    double cr[] = new double[1];
    double ci[] = new double[1];

    PI=Math.PI;
    SQRTEPS=Math.sqrt(EPS);
    CT[0]=Z2[0]-Z0[0];
    CT[1] = Z2[1] - Z0[1];
    double ABSCT = zabs(CT[0], CT[1]);
    if (ABSCT < SQRTEPS) {
      IER[0]=56;
      return;
    }
    U[0]=CT[0]/ABSCT;
    U[1]= CT[1]/ABSCT;
    zmlt(U[0],U[1],U[0],U[1], cr, ci);
    U2[0] = cr[0];
    U2[1] = ci[0];

    zmlt(Z1[0]-Z0[0],Z1[1]-Z0[1],U[0],-U[1],cr,ci);
    CT[0] = cr[0];
    CT[1] = ci[0];
    ABSCT = zabs(CT[0],CT[1]);
    if (ABSCT < SQRTEPS) {
      IER[0]=57;
      return;
    }
    ALPHA=Math.atan2(CT[1],CT[0]);
    ALPHA=Math.abs(ALPHA);

    if (REFLN) {
      ORDRG[0]=(int)Math.round(PI/ALPHA);
      ORDSG[0]=2*ORDRG[0];
    }
    else {
      ORDRG[0]=2*(int)Math.round(PI/ALPHA);
      ORDSG[0]=ORDRG[0];
    }

    ALPHA=2.0*PI/(double)(ORDRG[0]);
    RTUNI[0] = Math.cos(ALPHA);
    RTUNI[1] = Math.sin(ALPHA);

    // NORMAL EXIT

    IER[0]=0;
    return;
    } // private void SYINF1


	
      //COMPLEX FUNCTION PARFUN(I,T)
      //INTEGER I
      //COMPLEX T
      double[] PARFUN(int I, double T[]) {

          // DUMMY FUNCTION TO AID LINK-LOADING OF PARGEN

          double result[] =new double[]{1.0,0.0};
          return result;
      } // double[] PARFUN     
	
	  //COMPLEX FUNCTION DPARFN(I,T)
	  //INTEGER I
	  //COMPLEX T
      double[] DPARFN(int I, double T[]) {
	
	      // DUMMY FUNCTION TO AID LINK-LOADING OF PARGEN
	
    	  double result[] =new double[]{1.0,0.0};
          return result;
      } // double[] DPARFN

      private void WRHEAD(int I,int CHNL, RandomAccessFile raFile) {
   

      //**** WRITE A HEADING FOR THE MAIN CONFPACK MODULES JAPHYC (I=1), 
      //**** GQPHYC (I=2), JACANP (I=3), GQCANP (I=4), CNDPLT (I=5), THE
      //**** PARAMETRIC FUNCTION GENERATOR PARGEN (I=6),THE PARAMETRIC FUNCTION
      //**** TESTER TSTPLT (I=7) AND THE LEVEL CURVE ROUTINE LEVCUR (I=8).  IF 
      //**** CHNL=0 THEN WRITE ON THE STANDARD OUTPUT CHANNEL, OTHERWISE WRITE 
      //**** ON THE CHANNEL SPECIFIED BY CHNL.
      //
      //    LOCAL VARIABLES
      //
    	  String DOTS = ".................................................";
    	  String CPHEAD = ": C O N F P A C K    M O D U L E    ";
    	  String MOD[] = new String[]{"J A P H Y C :",
    	                              "G Q P H Y C :",
    	                              "J A C A N P :",
    	                              "G Q C A N P :",
    	                              "C N D P L T :",
    	                              "P A R G E N :",
    	                              "T S T P L T :",
    	                              "L E V C U R :"};
    	  String TXT = CPHEAD + MOD[I-1];
            
          if (CHNL == 0 || CHNL == 6) {
        	  System.out.println("\n\n      "+DOTS+"\n      "+TXT+"\n      "+DOTS);
          }
          else {
        	  try {
        	      raFile.writeBytes("\n\n      //"+DOTS+"\n      //"+TXT+"\n      //"+DOTS+"\n");
        	  }
        	  catch (IOException e) {
        		  MipavUtil.displayError("IOException " + e + " on raFile.writeBytes in WRHEAD");
        		  System.exit(-1);
        	  }
          }
          return;
      } // private void WRHEAD
      
      private void WRTAIL(int I, int CHNL, int IER, RandomAccessFile raFile) {
     
//
//**** WRITE A CLOSING MESSAGE FOR THE MAIN CONFPACK MODULES JAPHYC (I=1)
//**** GQPHYC (I=2), JACANP (I=3), GQCANP (I=4), CNDPLT (I=5), THE PARA-
//**** METRIC FUNCTION GENERATOR PARGEN (I=6), THE PARAMETRIC FUNCTION 
//**** TESTER TSTPLT (I=7) AND THE LEVEL CURVE ROUTINE LEVCUR (I=8).  IF 
//**** CHNL=0 THEN WRITE ON THE STANDARD OUTPUT CHANNEL, OTHERWISE WRITE 
//**** ON THE CHANNEL SPECIFIED BY CHNL.  THE TEXT OF THE MESSAGE IS 
//**** DETERMINED BY THE ERROR NUMBER IER VIA THE SUBROUTINE IERTXT.

//     LOCAL VARIABLES
     
      String MOD[] = new String[]{"J A P H Y C :",	  
                      "G Q P H Y C :","J A C A N P :",
                "G Q C A N P :","C N D P L T :","P A R G E N :",
                "T S T P L T :","L E V C U R :"};
     String GOOD = "  NORMAL EXIT";
     String BAD= "  ABNORMAL EXIT";
     String LINE = "__________________________________________________________________&";

     String TXT, TXT2;
     if (IER == 0) {
        TXT=MOD[I-1]+GOOD;
     }
     else{
        TXT=MOD[I-1]+BAD;
     }
     TXT2=IERTXT(IER);

      if ((CHNL == 0) || (CHNL == 6)) {
    	System.out.println("\n\n      "+TXT);
    	System.out.println("      "+TXT2);
        System.out.println(LINE);
     }
     else {
    	try {
            raFile.writeBytes("\n\n      //"+TXT+"\n");
            raFile.writeBytes("      //"+TXT2+"\n");
            raFile.writeBytes("//"+LINE+"\n");
    	}
    	catch(IOException e) {
    		MipavUtil.displayError("IOException " + e + " in WRTAIL");
    		System.exit(-1);
    	}
     }
     return;
      }
      
      private String IERTXT(int IER) {
    	  
//**** SUPPLY ERROR MESSAGE TEXT FOR ERROR NUMBER IER
      String result = null;
      if (IER == 0) {
          result = " ";
      }
      else if (IER == 1) {
          result="PARAMETER IBNDS[0] IS TOO SMALL AT START OF JAPHYC";
      }
      else if (IER == 2) {
          result = "PARAMETER IBNDS[1] IS TOO SMALL AT START OF JAPHYC";
      }
      else if (IER == 3) {
          result = "NQPTS < 1 AT START OF JAPHYC";
      }
      else if (IER == 4) {
          result="FAILURE TO CONVERGE IN EIGSYS; CAN''T SET UP BASIC QUADRATURE RULES";
      }
      else if (IER == 5) {
          result="PARAMETER MNQPT IN IGNLVL MUST BE INCREASED TO AT LEAST NQPTS";
      }
      else if (IER == 6) {
          result="FAILURE TO CONVERGE IN IMTQLH; CAN''T SET UP IGNORE LEVELS";
      }
      else if (IER == 7) {
          result="FAILURE TO CONVERGE IN IMTQLH; CAN''T SET UP COLLOCATION POINTS";
      }
      else if (IER == 8) {
          result="ARGUMENT MNEQN IS TOO SMALL AT START OF JAPHYC";
      }
      else if (IER == 9) {
          result="PARAMETER IBNDS[3] IS TOO SMALL AT START OF JAPHYC";
      }
      else if (IER == 10) {
          result="PARAMETER NMAX IN SUBIN7 MUST BE INCREASED TO AT LEAST 2*NQPTS";
      }
      else if (IER == 11) {
          result="PARAMETER IBNDS[2] IS TOO SMALL AT START OF JAPHYC";
      }
      else if (IER == 12) {
          result="PARAMETER NC IN DEJAC7 AND DELEG7 MUST BE INCREASED";
      }
      else if (IER == 13) {
          result="PARAMETER NR IN DEJAC7 AND DELEG7 MUST BE >= (NQPTS -1)";
      }
      else if (IER == 14) {
          result="A CORNER ANGLE IS TOO SMALL; MAY CAUSE OVERFLOW IN GAMMA FUNCTION";
      }
      else if (IER == 15) {
          result="SINGULAR COLLOCATION MATRIX";
      }
      else if (IER == 16) {
          result="COLLOCATION MATRIX IS EFFECTIVELY SINGULAR";
      }
      else if (IER == 17) {
          result="NUMBER OF SUBARCS EXCEEDS IBNDS[0] DURING REFINEMENT";
      }
      else if (IER == 18) {
          result="NUMBER OF EQUATIONS EXCEEDS MNEQN DURING REFINEMENT";
      }
      else if (IER == 19) {
          result="TOTAL NUMBER OF QUADRATURE PTS EXCEEDS IBNDS[3] DURING REFINEMENT";
      }
      else if (IER == 20) {
          result="NUMBER OF QUADRATURE PANELS EXCEEDS IBNDS[2] DURING REFINEMENT";
      }
      else if (IER == 21) {
          result="FAILURE TO CONVERGE IN IMTQLH; CAN''T SET UP TEST POINTS";
      }
      else if (IER == 22) {
          result="ARGUMENT MQUPH OF GQPHYC MUST BE INCREASED";
      }
      else if (IER == 23) {
          result="PARAMETER MNCOF IN POPQF1 MUST BE >= NQPTS";
      }
      else if (IER == 24) {
          result="NUMBER OF QUADRATURE PANELS EXCEEDS MQIN1 IN GQPHYC";
      }
      else if (IER == 25) {
          result="PARAMETER MNXI IN DEPPJ8 AND DEPPL8 MUST BE INCREASED";
      }
      else if (IER == 26) {
          result="PARAMETER MAXNZ IN DEPPJ9 AND DEPPL9 MUST BE INCREASED";
      }
      else if (IER == 27) {
          result="PARAMETER MXNQD IN PHTCA1 MUST BE INCREASED";
      }
      else if (IER == 28) {
          result="PARAMETER MXCOF IN PHTCA1 MUST BE INCREASED";
      }
      else if (IER == 29) {
          result="PARAMETER MQIN1 IN PHTCA1 MUST BE INCREASED";
      }
      else if (IER == 30) {
          result="PARAMETER MNDG IN JCFIM5 MUST BE INCREASED";
      }
      else if (IER == 31) {
          result="PARAMETER MNQD IN JCFIM5 MUST BE INCREASED";
      }
      else if (IER == 32) {
          result="ARGUMENT IBNDS[1] SUPPLIED TO JACANP MUST BE INCREASED";
      }
      else if (IER == 33) {
          result="ARGUMENT IBNDS[0] SUPPLIED TO JACANP MUST BE INCREASED";
      }
      else if (IER == 34) {
          result="FN HAS SAME SIGN AT INTERVAL ENDS IN BISNEW; CAN''T SOLVE BCF EQN";
      }
      else if (IER == 35) {
          result="DERIVATIVE OF BCF IS ZERO IN BISNEW; CAN''T SOLVE BCF EQN";
      }
      else if (IER == 36) {
          result="ELEMENT OF ARGUMENT ARRAY SVAL IN RHOFN IS +-1; CAN''T CONTINUE";
      }
      else if (IER == 37) {
          result="PARAMETER MXNQD IN CINRAD MUST BE INCREASED";
      }
      else if (IER == 38) {
          result="PARAMETER MXCOF IN CINRAD MUST BE INCREASED";
      }
      else if (IER == 39) {
          result="CENTRE POINT IS PATHOLOGICALLY CLOSE TO BOUNDARY;CAN''T CONTINUE";
      }
      else if (IER == 40) {
          result="PARAMETER MQIN1 IN CINRAD MUST BE INCREASED";
      }
      else if (IER == 41) {
          result="ARGUMENT MQUCA OF GQCANP MUST BE INCREASED";
      }
      else if (IER == 42) {
          result="PARAMETER MNCOF IN POPQG1 MUST BE >= NQPTS";
      }
      else if (IER == 43) {
          result="NUMBER OF QUADRATURE PANELS EXCEEDS MQIN1 IN GQCANP";
      }
      else if (IER == 44) {
          result="PARAMETER MNCOF IN BMPHC1 MUST BE >= NQPTS";
      }
      else if (IER == 45) {
          result="ARGUMENTS IARC, PHYPT OF BMPHYC DON''T DEFINE A BOUNDARY POINT";
      }
      else if (IER == 46) {
          result="PARAMETER MNCOF IN BMCAP1 MUST BE >= NQPTS";
      }
      else if (IER == 47) {
          result="PARAMETER MXNQD IN CATPH4 MUST BE INCREASED";
      }
      else if (IER == 48) {
          result="PARAMETER MNCOF IN CATPH4 MUST BE >= NQPTS";
      }
      else if (IER == 49) {
          result="PARAMETER MQIN1 IN CATPH4 MUST BE INCREASED";
      }
      else if (IER == 50) {
          result="PARAMETER MXCOF IN DIAGN3 MUST BE >= NQPTS";
      }
      else if (IER == 51) {
          result="NON-ANALYTIC ARC DETECTED IN DIAGN3";
      }
      else if (IER == 52) {
          result="PARAMETER MAXSA IN CNDPLT MUST BE INCREASED";
      }
      else if (IER == 53) {
          result="OVERFLOW EXPECTED IN IGNLVL; A CORNER ANGLE IS TOO SMALL";
      }
      else if (IER == 54) {
          result="PARAMETER MXCO IN AXION1 MUST BE INCREASED";
      }
      else if (IER == 55) {
          result="NARCS ISN''T AN INTEGER MULTIPLE OF THE ORDER OF THE SYMMETRY GROUP";
      }
      else if (IER == 56) {
          result="CENTRE OF SYMMETRY IS PATHOLOGICALLY CLOSE TO LAST POINT ON FBS";
      }
      else if (IER == 57) {
          result="CENTRE OF SYMMETRY IS PATHOLOGICALLY CLOSE TO FIRST POINT ON FBS";
      }
      else if (IER == 58) {
          result="NUMBER OF ARCS IS TOO BIG; INCREASE PARAMETER MNARC IN PARGEN";
      }
      else if (IER == 59) {
          result="NUMBER OF ARCS IS TOO BIG; INCREASE PARAMETER MNARC IN TSTPLT";
      }
      else if (IER == 60) {
          result="NON-ANALYTIC ARC (DPARFN=(0.,0.)) DETECTED IN TSTPLT";
      }
      else {
          result="UNRECOGNIZED ERROR NUMBER IN IERTXT ROUTINE !!";
      }
      return result;
}
      
      private void WRSYM1(int NARCS,int ORDRG,int ORDSG,double[] RTUNI,double[] U2,double[] CENSY,boolean REFLN,boolean PARFUN,
    		     String REDD,int CHNL, RandomAccessFile raFile) {
    		      //COMPLEX RTUNI,U2,CENSY
    		
    		//**** TO WRITE THE DIMENSION AND PARAMETER STATEMENTS AND THE CODE TO
    		//**** TO REDUCE A GIVEN ARC NUMBER TO ITS SYMMETRIC COUNTERPART ON THE
    		//**** FUNDAMENTAL BOUNDARY SECTION.
    		
    		//.......................................................................
    		//     AUTHOR: DAVID HOUGH, ETH, ZUERICH
    		//     LAST UPDATE: 4 AUG 1990
    		//.......................................................................C
    		//     LOCAL VARIABLES
    		
    		int I;
    		double R,A;
    		//COMPLEX ZT
    		double ZT[] = new double[2];
    		boolean NEEDC;
    		//String FMT;
    		double cr[] = new double[1];
    		double ci[] = new double[1];
    		
    		//FMT="(A12,"+REDD+",A1,"+REDD+",A2)";
    		
    		try {
	    		if (PARFUN) {
	    			NEEDC = ((CENSY[0] != 0.0) || (CENSY[1] != 0.0));
	    		    if (NEEDC || REFLN) {
	    		    	raFile.writeBytes("      PARAMETER (\n");
	    		        if (NEEDC && REFLN) {
	    		            R=U2[0];
	    		            A=U2[1];
	    		            raFile.writeBytes("U2[0] = " + R + ";\n");
	    		            raFile.writeBytes("U2[1] = " + A + ";\n");
	    		            R=CENSY[0];
	    		            A=CENSY[1];
	    		            raFile.writeBytes("ZCEN[0] = " + R +";\n");
	    		            raFile.writeBytes("ZCEN[1] = " + A + ";)\n");
	    		        } // if (NEEDC && REFLN)
	    		        else if (NEEDC && (!REFLN)) {
	    		            R=CENSY[0];
	    		            A=CENSY[1];
	    		            raFile.writeBytes("ZCEN[0] = " + R + ";\n");
	    		            raFile.writeBytes("ZCEN[1] = " + A + ";)\n");
	    		        } // else if (NEEDC && (!REFLN)) 
	    		        else {
	    		            R=U2[0];
	    		            A=U2[1];
	    		            raFile.writeBytes("U2[0] = " + R + ";\n");
	    		            raFile.writeBytes("U2[1] = " + A + ";)\n");
	    		        } // else
	    		        raFile.writeBytes("//\n");
	    		    } // if (NEEDC || REFLN)
	    		}
	    		else if (REFLN) {
	    		    R=U2[0];
	    		    A=U2[1];
	    		    raFile.writeBytes("      PARAMETER (\n");
	    		    raFile.writeBytes("U2[0] = " + R + ";\n");
	    		    raFile.writeBytes("U2[1] = " + A + ";)\n");
	    		    raFile.writeBytes("//\n");
	    		}
	    		
	    		//FMT="(A7,"+REDD+",A1,"+REDD+",A2)";
	    		
	    	    if (ORDRG >= 2) {
	    	    	raFile.writeBytes("double WW[] = new double["+(ORDRG-1)+"];\n");
	    	    	ZT[0] = 1.0;
	    	    	ZT[1] = 0.0;
	    		    for (I=0; I < ORDRG-2; I++) {
	    		    	zmlt(ZT[0],ZT[1],RTUNI[0],RTUNI[1],cr,ci);
	    		    	ZT[0] = cr[0];
	    		    	ZT[1] = ci[0];
	    		    	raFile.writeBytes("WW["+I+"][0] = " + ZT[0] + ";\n");
	    		    	raFile.writeBytes("WW["+I+"][1] = " + ZT[1] + ";\n");
	    		    }
	    		    zmlt(ZT[0],ZT[1],RTUNI[0],RTUNI[1],cr,ci);
			    	ZT[0] = cr[0];
			    	ZT[1] = ci[0];
			    	raFile.writeBytes("WW["+I+"][0] = " + ZT[0] + ";\n");
			    	raFile.writeBytes("WW["+I+"][1] = " + ZT[1] + ";)\n");
			    	raFile.writeBytes("//\n");
	    	    } // if (ORDRG >= 2)
	    	    
	    		if (ORDRG > 19) {
	    		    System.out.println("\n");
	    		    System.out.println("             ****WARNING****");
	    		    System.out.println("MORE THAN 19 CONTINUTATION LINES HAVE BEEN WRITTEN");
	    		}
	    		
	    		if (REFLN) {
	    	        if (ORDRG > 1) {
	    		        if (NARCS > 1) {
	    		            I=2*NARCS;
	    		            raFile.writeBytes("IB = IA%"+I+";\n");
	    		            raFile.writeBytes("if (IB == 0) IB = " + I + ";\n");
	    		            I=I+1;
	    		            raFile.writeBytes("if (IB > " + NARCS + ") {\n");
	    		            raFile.writeBytes("    IB = " + I + " - IB;\n");
	    		            raFile.writeBytes("    TS[0] = -TT[0];\n");
	    		            raFile.writeBytes("    TS[1] = TT[1]);\n");
	    		            raFile.writeBytes("}\n");
	    		            raFile.writeBytes("else {\n");
	    		            raFile.writeBytes("    TS[0] = TT[0];\n");
	    		            raFile.writeBytes("    TS[1] = TT[1];\n");
	    		            raFile.writeBytes("}\n");
	    		        } // if (NARCS > 1)
	    		        else {
	    		        	raFile.writeBytes("if ((IA%2) == 0) {\n");
	    		            raFile.writeBytes("    TS[0] = -TT[0];\n");
	    		            raFile.writeBytes("    TS[1] = TT[1];\n");
	    		            raFile.writeBytes("}\n");
	    		            raFile.writeBytes("else {\n");
	    		            raFile.writeBytes("    TS[0] = TT[0];\n");
	    		            raFile.writeBytes("    TS[1] = TT[1];\n");
	    		            raFile.writeBytes("}\n");
	    		        } // else
	    	        } // if (ORDRG > 1)
	    	        else {
	    		        if (NARCS > 1) {
	    		            I=2*NARCS+1;
	    		            raFile.writeBytes("if (IA > " + NARCS + "){\n");
	    		            raFile.writeBytes("    IB = " + I + " -IA;\n");
	    		            raFile.writeBytes("    TS[0] = -TT[0];\n");
	    		            raFile.writeBytes("    TS[1] = TT[1]);\n");
	    		            raFile.writeBytes("}\n");
	    		            raFile.writeBytes("else {\n");
	    		            raFile.writeBytes("    IB = IA;\n");
	    		            raFile.writeBytes("    TS[0] = TT[0];\n");
	    		            raFile.writeBytes("    TS[1] = TT[1];\n");
	    		            raFile.writeBytes("}\n");
	    		        } // if (NARCS)
	    		        else {
	    		        	raFile.writeBytes("if (IA == 2) {\n");
	    		        	raFile.writeBytes("    TS[0] = -TT[0];\n");
	    		            raFile.writeBytes("    TS[1] = TT[1]);\n");
	    		            raFile.writeBytes("}\n");
	    		            raFile.writeBytes("else {\n");
	    		            raFile.writeBytes("    TS[0] = TT[0];\n");
	    		            raFile.writeBytes("    TS[1] = TT[1];\n");
	    		            raFile.writeBytes("}\n");     
	    		        } // else
	    	        } // else
	    		} // if (REFLN)
	    		else if (NARCS > 1) {
	    			raFile.writeBytes("IB = IA%" + NARCS + ";\n");
	    		    raFile.writeBytes("if (IB == 0) IB = " + NARCS + ";\n");
	    		} // else if (NARCS > 1)
	    		
	    		raFile.writeBytes("//\n");
    		} // try
    		catch (IOException e) {
    			MipavUtil.displayError("IOException " + e + " in WRSYM1");
    			System.exit(-1);
    		}
    		
      }

      private void WRFUN1(int NARCS,double STAPT[][],int ARCTY[],int PGM[], 
    		  double RGM[], int PTX[], int NTX[], String DEFN[],
    		  int CHNL, String CHIA, String CHTT, String VAR, String REDD, RandomAccessFile raFile) {
          //COMPLEX STAPT(*)
          //CHARACTER DEFN(*)*72,CHIA*2,CHTT*2,VAR*6,REDD*6
    		
          //**** TO WRITE THE SOURCE CODE FOR PARFUN IN THE CASE WHERE NO
    	  //**** SYMMETRY IS INVOLVED.
    		
    	  //.......................................................................
    	  //     AUTHOR: DAVID HOUGH, ETH, ZUERICH
    	  //     LAST UPDATE: 4 AUG 1990
    	  //.......................................................................
    	
    	  //     LOCAL VARIABLES
    	
          int IA,I,J,K;
    	  //CHARACTER TX1*16,TX2*21,FMT1*11,FMT2*11
          String TX1,TX2;
          //String FMT1,FMT2;
    	  // EXTERNAL PTFUN1
    	  TX1 = "     if("+CHIA+ " == ";
    	  TX2 = "     else if("+CHIA+ " == ";
          //FMT1="(A16,I3,A6)";
          //FMT2="(A21,I3,A6)";
          double STAPT2[][];
          double RGM2[];
          String DEFN2[];
    		
    	  try {
	          for (IA=1; IA <= NARCS; IA++) {
	              I=PGM[IA-1];
	    		  J=PTX[IA-1];
	    		  STAPT2 = new double[STAPT.length-IA+1][2];
	    		  for (K = IA; K <= STAPT.length; K++) {
	    			  STAPT2[K-IA] = STAPT[K-1];
	    		  }
	    		  RGM2 = new double[RGM.length-I+1];
	    		  for (K = I; K <= RGM.length; K++) {
	    			  RGM2[K-I] = RGM[K-1];
	    		  }
	    		  DEFN2 = new String[DEFN.length-J+1];
	    		  for (K = J; K <= DEFN.length; K++) {
	    			  DEFN2[K-J] = DEFN[K-1];
	    		  }
	    		  if (NARCS == 1) {
	    		      PTFUN1(ARCTY[IA-1],STAPT2,RGM2,NTX[IA-1],DEFN2,
	    		                 CHNL,CHTT,VAR,REDD, raFile);
	    		  }
	    		  else {
		    		  if (IA == 1) {
		    			  raFile.writeBytes(TX1 + IA + ") {\n");
		    		  }
		    		  else if (IA == NARCS) {
		    			  raFile.writeBytes("      else {\n");
		    		  }
		    		  else {
		    			  raFile.writeBytes(TX2 + IA + ") {\n");
		    		  }
		    		  PTFUN1(ARCTY[IA-1],STAPT2,RGM2,NTX[IA-1],DEFN2,
		    		         CHNL,CHTT,VAR,REDD, raFile);
		    		  if (IA == NARCS) raFile.writeBytes("      }\n");
	    		  } // else
	    	  } // for (IA=1; IA <= NARCS; IA++)
    	  } // try
    	  catch (IOException e) {
    	      MipavUtil.displayError("IOException " + e + " in WRFUN1");
    	      System.exit(-1);
    	  }
    		
      } // private void WRFUN1

      private void PTFUN1(int TYPE, double STAPT[][], double RGM[],int NTX,
    		  String TXT[],int CHNL, String CHTT,String VAR, String REDD,
    		  RandomAccessFile raFile) {
      
          //COMPLEX STAPT(*)
          //CHARACTER TXT(*)*72,CHTT*2,VAR*6,REDD*6

          //.......................................................................
          // AUTHOR: DAVID HOUGH, ETH, ZUERICH
          // LAST UPDATE: 8 AUG 1990
          // .......................................................................C
          //**** LOCAL VARIABLES

      int I;
      double HA,MD,RAD;
      double C1[] = new double[2];
      double C2[] = new double[2];
      double CENTR[] = new double[2];
      //COMPLEX C1,C2,CENTR
      //String TX1, TX1B, TX2;
      //String TX2B, CTX1B, FMT1, FMT2, FMT3, FMT4, FMT5;
      //CHARACTER TX1*4,TX1B*5,TX2*13,TX2B*14,CTX1B*10,
      //+FMT1*25,FMT2*25,FMT3*14,FMT4*25,FMT5*24

      //TX1 = "+"+CHTT+"*";
      //TX1B = TX1 + "(";
      //CTX1B="     " + TX1B;
      //TX2="      "+VAR+" = ";
      //TX2B=TX2+"(";

      //FMT1='(A14,'//REDD//',A1,'//REDD//',A2)'
      //FMT2='(A10,'//REDD//',A1,'//REDD//',A1)'
      //FMT3='(A6,'//REDD//',A1)'
      //FMT4='(A14,'//REDD//',A5,'//REDD//',A3)'
      //FMT5='(A8,'//REDD//',A5,'//REDD//',A1)'

      try {
      if (TYPE == 1) {
    	C1[0] = 0.5*(STAPT[1][0] + STAPT[0][0]);
    	C1[1] = 0.5*(STAPT[1][1] + STAPT[0][1]);
        C2[0] = 0.5*(STAPT[1][0] - STAPT[0][0]);
        C2[1] = 0.5*(STAPT[1][1] - STAPT[0][1]);
        raFile.writeBytes("//\n");
        raFile.writeBytes(VAR+"[0] = " + C1[0] + "+" + CHTT+"[0]*"+C2[0]+" - "+CHTT+"[1]*"+C2[1]+";\n");
        raFile.writeBytes(VAR+"[1] = " + C1[1] + "+" + CHTT+"[0]*"+C2[1]+" + "+CHTT+"[1]*"+C2[0]+";\n");
        raFile.writeBytes("//\n");
      } // if (TYPE == 1)
      else if (TYPE == 2) {
    	CENTR[0] = RGM[0];
    	CENTR[1] = RGM[1];
        C1[0] = STAPT[0][0] - CENTR[0];
        C1[1] = STAPT[0][1] - CENTR[1];
        HA=0.5*RGM[2];
        MD = Math.atan2(C1[1], C1[0]) + HA;
        RAD=zabs(C1[0],C1[1]);
        raFile.writeBytes("//\n");
        raFile.writeBytes(VAR+"[0] = " + CENTR[0] + "+" + RAD + " * " + "Math.exp(-"+CHTT+"[1]*"+HA+")*"
        +"Math.cos("+MD+CHTT+"[0]*"+HA+");\n");
        raFile.writeBytes(VAR+"[1] = " + CENTR[1] + "+" + RAD + " * " + "Math.exp(-"+CHTT+"[1]*"+HA+")*"
                +"Math.sin("+MD+CHTT+"[0]*"+HA+");\n");
        raFile.writeBytes("//\n");
      } // else if (TYPE == 2)
      else if (TYPE == 3) {
        MD=0.5*(RGM[1]+RGM[0]);
        HA=0.5*(RGM[1]-RGM[0]);
        raFile.writeBytes("//\n");
        raFile.writeBytes("T[0] = " + MD + "+" + CHTT + "[0] * " + HA + ";\n");
        raFile.writeBytes("T[1] = " + MD + "+" + CHTT + "[1] * " + HA + ";\n");
        
        raFile.writeBytes(VAR + "[0] = ");
        // NTX = 1 if statements are entered without newlines for multiple lines
        for (I  = 1; I <= NTX; I++) {
        	int index = TXT[I-1].indexOf("ui");
        	String realString = null;
        	if (index == -1) {
        		realString = TXT[I-1];
        	}
        	else {
        		realString = TXT[I-1].substring(0, index);
        	}
        	if ((index == -1) || (index > 0)) {
        	    raFile.writeBytes(realString);
        	}
        	if (I == NTX) {
        		raFile.writeBytes(";\n");
        	}
        }
        raFile.writeBytes(VAR + "[1] = ");
        // NTX = 1 if statements are entered without newlines for multiple lines
        for (I  = 1; I <= NTX; I++) {
        	int index = TXT[I-1].indexOf("ui");
        	String imagString = null;
        	if ((index >= 0) && (index+2 < TXT[I-1].length())) {
        		imagString = TXT[I-1].substring(index+2);
        		raFile.writeBytes(imagString);
        	}
        	if (I == NTX) {
        		raFile.writeBytes(";\n");
        	}
        }
        raFile.writeBytes("//\n");
      } // else if (TYPE == 3)
      else {
    	MD=0.5*(RGM[1]+RGM[0]);
        HA=0.5*(RGM[1]-RGM[0]);
        raFile.writeBytes("//\n");
        raFile.writeBytes("T[0] = " + MD + "+" + CHTT + "[0] * " + HA + ";\n");
        raFile.writeBytes("T[1] = " + MD + "+" + CHTT + "[1] * " + HA + ";\n");
        raFile.writeBytes("     ZRAD = ");
        // NTX = 1 if statements are entered without newlines for multiple lines
        for (I  = 1; I <= NTX; I++) {
        	raFile.writeBytes(TXT[I-1]);
        }
        raFile.writeBytes(VAR + "[0] = Math.exp(-T[1])*(ZRAD * Math.cos(T[0]));\n");
        raFile.writeBytes(VAR + "[1] = Math.exp(-T[1])*(ZRAD * Math.sin(T[0]));\n");
        raFile.writeBytes("//\n");
      }
      } // try 
      catch (IOException e) {
    	  MipavUtil.displayError("IOException " + e + " in PTFUN1");
    	  System.exit(-1);
      }

      } // private void PTFUN1
      

      private void WRSYM2(int NARCS,int ORDRG, double CENSY[], boolean REFLN,int CHNL, RandomAccessFile raFile) {
     
          //COMPLEX CENSY

          // **** TO WRITE THE CODE TO RECOVER THE BOUNDARY POINT FROM ITS SYMMETRIC
          // **** COUNTERPART ON THE FUNDAMENTAL BOUNDARY SECTION.

          //.......................................................................
          // AUTHOR: DAVID HOUGH, ETH, ZUERICH
          // LAST UPDATE: 4 AUG 1990
          // .......................................................................C
          // LOCAL VARIABLES

          int I;
          boolean NEEDC;
  
          NEEDC = ((CENSY[0] != 0.0) || (CENSY[1] != 0.0));
          try {
        	  raFile.writeBytes("//\n");

              if (REFLN) {
                  if (ORDRG > 1) {
                      I=2*NARCS;
                      if (NARCS > 1) {
                    	  raFile.writeBytes("IS = (IA-IB)%"+I+";\n");
                          raFile.writeBytes("IR = (IA-IB-IS)/" + I + ";\n");
                      } // if (NARCS > 1)
                      else {
                    	  raFile.writeBytes("IS = (IA-1)%2;\n");
                          raFile.writeBytes("IR = (IA-1-IS)/2;\n");
                      }
                      raFile.writeBytes("if ((IR == 0) && (IS == 0)) {\n");
                      raFile.writeBytes("    PARFUN[0] = ZETA[0];\n");
                      raFile.writeBytes("    PARFUN[1] = ZETA[1];\n");
                      raFile.writeBytes("}\n");
                      raFile.writeBytes("else if ((IR > 0) && (IS == 0)) {\n");
                      if (NEEDC) {
                    	  raFile.writeBytes("    PARFUN[0] = ZCEN[0] + WW[IR-1][0]*(ZETA[0] - ZCEN[0]) - "
                    	  		+ "WW[IR-1][1]*(ZETA[1] - ZCEN[1]);\n");
                    	  raFile.writeBytes("    PARFUN[1] = ZCEN[1] + WW[IR-1][0]*(ZETA[1] - ZCEN[1]) + "
                      	  		+ "WW[IR-1][1]*(ZETA[0] - ZCEN[0]);\n");
                       } // if (NEEDC)
                      else {
                    	  raFile.writeBytes("PARFUN[0] = WW[IR-1][0]*ZETA[0] - WW[IR-1][1]*ZETA[1];\n");
                      }
                      raFile.writeBytes("else if ((IR == 0) && (IS > 0)) {\n");
                      if (NEEDC) {
                          raFile.writeBytes("    PARFUN[0] = ZCEN[0] + U2[0]*(ZETA[0]-ZCEN[0]) + " +
                        		  "U2[1]*(ZETA[1]-ZCEN[1]);\n");
                          raFile.writeBytes("    PARFUN[1] = ZCEN[1] - U2[0]*(ZETA[1]-ZCEN[1]) + " +
                        		  "U2[1]*(ZETA[0]-ZCEN[0]);\n");
                      }
                      else {
                    	  raFile.writeBytes("    PARFUN[0] = U2[0]*ZETA[0] + U2[1]*ZETA[1];\n");
                    	  raFile.writeBytes("    PARFUN[1] = -U2[0]*ZETA[1] + U2[1]*ZETA[0];\n");
                      }
                      raFile.writeBytes("}\n");
                      raFile.writeBytes("else {\n");
                      if (NEEDC) {
                          raFile.writeBytes("double realPart = U2[0]*WW[IR-1][0] - U2[1]*WW[IR-1][1];\n");
                          raFile.writeBytes("double imagPart = U2[0]*WW[IR-1][1] + U2[1]*WW[IR-1][0];\n");
                          raFile.writeBytes("PARFUN[0] = ZCEN[0] + realPart*(ZETA[0]-ZCEN[0]) + " +
                          		"imagPart*(ZETA[1]-ZCEN[1]);\n");
                          raFile.writeBytes("PARFUN[1] = ZCEN[1] - realPart*(ZETA[1]-ZCEN[1]) + " +
                            		"imagPart*(ZETA[0]-ZCEN[0]);\n");
                      }
                      else {
                    	  raFile.writeBytes("double realPart = U2[0]*WW[IR-1][0] - U2[1]*WW[IR-1][1];\n");
                          raFile.writeBytes("double imagPart = U2[0]*WW[IR-1][1] + U2[1]*WW[IR-1][0];\n");
                          raFile.writeBytes("PARFUN[0] = realPart * ZETA[0] + imagPart * ZETA[1];\n");
                          raFile.writeBytes("PARFUN[1] = -realPart * ZETA[1] + imagPart * ZETA[0];\n");
                      }
                      raFile.writeBytes("}\n");
                  } // if (ORDRG > 1)
                  else { // ORDRG <= 1
                      if (NARCS > 1) {
                    	  raFile.writeBytes("IS = IA - IB;\n");
                      }
                      else {
                    	  raFile.writeBytes("IS = IA - 1;\n");
                      }
                      raFile.writeBytes("if (IS == 0) {\n");
                      raFile.writeBytes("    PARFUN[0] = ZETA[0];\n");
                      raFile.writeBytes("    PARFUN[1] = ZETA[1];\n");
                      raFile.writeBytes("}\n");
                      raFile.writeBytes("else {\n");
                      if (NEEDC) {
                    	  raFile.writeBytes("    PARFUN[0] = ZCEN[0] + U2[0]*(ZETA[0]-ZCEN[0]) + " +
                        		  "U2[1]*(ZETA[1]-ZCEN[1]);\n");
                          raFile.writeBytes("    PARFUN[1] = ZCEN[1] - U2[0]*(ZETA[1]-ZCEN[1]) + " +
                        		  "U2[1]*(ZETA[0]-ZCEN[0]);\n");
                      }
                      else {
                    	  raFile.writeBytes("    PARFUN[0] = U2[0]*(ZETA[0]-ZCEN[0]) + " +
                        		  "U2[1]*(ZETA[1]-ZCEN[1]);\n");
                          raFile.writeBytes("    PARFUN[1] =  -U2[0]*(ZETA[1]-ZCEN[1]) + " +
                        		  "U2[1]*(ZETA[0]-ZCEN[0]);\n");
                      }
                      raFile.writeBytes("}\n");
                  } // else ORDRG <= 1
              } // if (REFLN)
              else { // !REFLN
                  if (NARCS > 1) {
                	  raFile.writeBytes("IR = (IA - IB)/" + NARCS + ";\n");
                  }
                  else {
                	  raFile.writeBytes("IR = IA - 1;\n");
                  }
                  raFile.writeBytes("if (IR == 0) {\n");
                  raFile.writeBytes("PARFUN[0] = ZETA[0]);\n");
                  raFile.writeBytes("PARFUN[1] = ZETA[1]);\n");
                  raFile.writeBytes("}\n");
                  raFile.writeBytes("else {\n");
                  if (NEEDC) {
                	  raFile.writeBytes("    PARFUN[0] = ZCEN[0] + WW[IR-1][0]*(ZETA[0] - ZCEN[0]) - "
                  	  		+ "WW[IR-1][1]*(ZETA[1] - ZCEN[1]);\n");
                  	  raFile.writeBytes("    PARFUN[1] = ZCEN[1] + WW[IR-1][0]*(ZETA[1] - ZCEN[1]) + "
                    	  		+ "WW[IR-1][1]*(ZETA[0] - ZCEN[0]);\n");
                  }
                  else {
                	  raFile.writeBytes("    PARFUN[0] = WW[IR-1][0]*(ZETA[0] - ZCEN[0]) - "
                    	  		+ "WW[IR-1][1]*(ZETA[1] - ZCEN[1]);\n");
                      raFile.writeBytes("    PARFUN[1] = WW[IR-1][0]*(ZETA[1] - ZCEN[1]) + "
                      	  		+ "WW[IR-1][1]*(ZETA[0] - ZCEN[0]);\n");
                  }
                  raFile.writeBytes("}\n");
              } // else !REFLN
          } // try
          catch(IOException e) {
        	  MipavUtil.displayError("IOException " + e + " in WRSYM2");
        	  System.exit(-1);
          }

      } // private void WRSYM2

      private void WRFUN2(int NARCS,int MNARC, double STAPT[][], int ARCTY[], int PGM[],double RGM[],
    		  int PTX[],int NTX[],String DEFN[],int CHNL,String CHIA,String CHTT, String VAR,
    		  boolean NUMDER[], String REDD, RandomAccessFile raFile) {
  
          // COMPLEX STAPT(*)
    	  // CHARACTER DEFN(*)*72,CHIA*2,CHTT*2,VAR*6,REDD*6
    		
          //**** TO WRITE THE SOURCE CODE FOR DPARFN IN THE CASE WHERE NO
    	  //**** SYMMETRY IS INVOLVED.
    		
    	  //.......................................................................
    	  //     AUTHOR: DAVID HOUGH, ETH, ZUERICH
    	  //     LAST UPDATE: 4 AUG 1990
    	  //.......................................................................
    		
    	  //     LOCAL VARIABLES
    		
    	  int IA,I,J1,J2,N1,N2;
    	  String TX1, TX2;
    	  //String FMT1, FMT2;
    	  //CHARACTER TX1*16,TX2*21,FMT1*11,FMT2*11
    	  //EXTERNAL PTFUN2
    	  double STAPT2[][];
    	  double RGM2[];
    	  String DEFN2[];
    	  String DEFN3[];
    	  int K;
    		
          TX1="      if ("+CHIA+" == ";
    	  TX2="      else if ("+CHIA+" == ";
    	  //FMT1="(A16,I3,A6)";
    	  //FMT2="(A21,I3,A6)";
    		
    	  try {
    	  for (IA=1; IA <= NARCS; IA++) {
    		        I=PGM[IA-1];
    		        J1=PTX[IA-1];
    		        J2=PTX[IA+MNARC-1];
    		        N1=NTX[IA-1];
    		        N2=NTX[IA+MNARC-1];
    		        STAPT2 = new double[STAPT.length-IA+1][2];
    		        for (K = IA; K <= STAPT.length; K++) {
    		        	STAPT2[K-IA][0] = STAPT[K-1][0];
    		        	STAPT2[K-IA][1] = STAPT[K-1][1];
    		        }
    		        RGM2 = new double[RGM.length-I+1];
    		        for (K = I; K <= RGM.length; K++) {
    		        	RGM2[K-I] = RGM[K-1];
    		        }
    		        DEFN2 = new String[DEFN.length-J1+1];
    		        for (K = J1; K <= DEFN.length; K++) {
    		        	DEFN2[K-J1] = DEFN[K-1];
    		        }
    		        DEFN3 = new String[DEFN.length-J2+1];
    		        for (K = J2; K <= DEFN.length; K++) {
    		        	DEFN3[K-J2] = DEFN[K-1];
    		        }
    		        if (NARCS == 1) {
    		          PTFUN2(ARCTY[IA-1],STAPT2,RGM2,N1,DEFN2,
    		                    N2,DEFN3,CHNL,CHTT,VAR," 1",NUMDER[IA-1],REDD,raFile);
    		        }
    		        else { 
    		          if (IA == 1) {
    		        	raFile.writeBytes(TX1 + IA + ") {\n");
    		          }
    		          else if (IA == NARCS) {
    		        	raFile.writeBytes("      else {\n");
    		          }
    		          else {
    		        	raFile.writeBytes(TX2 + IA + ") {\n");
    		          }
    		          PTFUN2(ARCTY[IA-1],STAPT2,RGM2,N1,DEFN2,
    		                 N2,DEFN3,CHNL,CHTT,VAR,CHIA,NUMDER[IA-1],REDD,raFile);
    		          if (IA == NARCS) {
    		        	  raFile.writeBytes("      }\n");
    		          }
    		        } // else
    	  } // for (IA=1; IA <= NARCS; IA++)
    	  } // try 
    	  catch (IOException e) {
    		  MipavUtil.displayError("IOException " + e + " in WRFUN2");
    		  System.exit(-1);
    	  }
    		
      } // private void WRFUN2
      
      private void PTFUN2(int TYPE,double STAPT[][], double RGM[],int NTX1, String TXT1[],int NTX2,
          String TXT2[],int CHNL, String CHTT, String VAR, String CHIA, boolean NUMDER, String REDD,
          RandomAccessFile raFile) {
          //COMPLEX STAPT(*)
          //CHARACTER TXT1(*)*72,TXT2(*)*72,CHTT*2,VAR*6,CHIA*2,REDD*6
    		
    	  //.......................................................................
    	  //     AUTHOR: DAVID HOUGH, ETH, ZUERICH
    	  //     LAST UPDATE: 8 AUG 1990
    	  //.......................................................................C
    	  // **** LOCAL VARIABLES
    		
    	  int I;
    	  double HA,MD,RAD,A,R;
    	  double C1[] = new double[2];
    	  double CENTR[] = new double[2];
    	  //COMPLEX C1,CENTR
    	  //String TX1, TX1B, TX2, TX2B, TX3, FMT1, FMT2, FMT3, FMT4, FMT5;
    	  //CHARACTER TX1*4,TX1B*5,TX2*13,TX2B*14,TX3*39,
          //FMT1*25,FMT2*15,FMT3*15,FMT4*25,FMT5*24
    	  //String TX3R, TX3I;
    		
    	  //TX1 = "+"+CHTT+"*";
    	  //TX1B = TX1 + "(";
    	  //TX2 = "      "+VAR+" = ";
    	  //TX2B = TX2 + "(";
    	  //TX3R = "      "+VAR+"[0] = (ZDER * Math.cos(T) - ZRAD * Math.sin(T))*(";
    	  //TX3I = "      "+VAR+"[1] = (ZDER * Math.sin(T) + ZRAD * Math.cos(T))*(";
          //TX3=TX2//'(ZDER+UI*ZRAD)*EXP(UI*T)*('
    	 
    	  //FMT1='(A14,'//REDD//',A1,'//REDD//',A2)'
    	  //FMT2='(A39,'//REDD//',A1)'
    	  //FMT3='(A13,'//REDD//',A2)'
    	  //FMT4='(A14,'//REDD//',A5,'//REDD//',A3)'
    	  //FMT5='(A8,'//REDD//',A5,'//REDD//',A1)'
    		
	     try {
    	    if (TYPE == 1) {
	    		 C1[0] = 0.5*(STAPT[1][0] - STAPT[0][0]);
	    		 C1[1] = 0.5*(STAPT[1][1] - STAPT[0][1]);
	    		 raFile.writeBytes("//\n");
	    		 R=C1[0];
	    		 A=C1[1];
	    		 raFile.writeBytes("      "+VAR+"[0] = " + R + ";\n");
	    		 raFile.writeBytes("      "+VAR+"[1] = " + A + ";\n");
	    		 raFile.writeBytes("//\n");
	    	 } // if (TYPE == 1)
	    	 else if (TYPE == 2) {
	    		 CENTR[0] = RGM[0];
	    		 CENTR[1] = RGM[1];
	    		 C1[0] = STAPT[0][0] - CENTR[0];
	    		 C1[1] = STAPT[0][1] - CENTR[1];
	    		 HA = 0.5 * RGM[2];
	    		 MD = Math.atan2(C1[1], C1[0]) + HA;
	    		 RAD = zabs(C1[0],C1[1]);
	    		 raFile.writeBytes("//\n");
	    		 raFile.writeBytes(VAR+"[0] = -" + RAD+"*"+HA+"*Math.sin("+MD+"+"+CHTT+"*"+HA+");\n");
	    		 raFile.writeBytes(VAR+"[1] = " + RAD+"*"+HA+"*Math.cos("+MD+"+"+CHTT+"*"+HA+");\n");
	    		 raFile.writeBytes("//\n");
	    	 } // else if (TYPE == 2)
	    	 else if (NUMDER) {
	    		 raFile.writeBytes("//\n");
	    		 raFile.writeBytes("      "+VAR+" = ZDPARF("+CHIA+","+CHTT+");\n");
	    		 raFile.writeBytes("//\n");  
	    	 } // else if (NUMDER)
	    	 else if (TYPE == 3) {
	    		 MD = 0.5*(RGM[1] + RGM[0]);
	    		 HA = 0.5*(RGM[1] - RGM[0]);
	    		 raFile.writeBytes("//\n");  
	    		 raFile.writeBytes("      T = " + MD+"+"+CHTT+"*"+"("+HA+");\n");
	    		 raFile.writeBytes("      "+VAR+" = " +HA + "*(");
	    		 for (I = 1; I <= NTX2; I++) {
	    			 raFile.writeBytes(TXT2[I-1]);
	    		 }
	    		 raFile.writeBytes(");\n");
	    		 raFile.writeBytes("//\n");      
	    	 } // else if (TYPE == 3)
	    	 else {
	    		 MD = 0.5*(RGM[1] + RGM[0]);
	    		 HA = 0.5*(RGM[1] - RGM[0]);
	    		 raFile.writeBytes("//\n");  
	    		 raFile.writeBytes("      T = " + MD+"+"+CHTT+"*"+"("+HA+");\n");
	    		 raFile.writeBytes("      ZRAD = ");
	    		 for (I = 1; I <= NTX1; I++) {
	    			 raFile.writeBytes(TXT1[I-1]);
	    		 }
	    		 raFile.writeBytes(";\n");
	    		 raFile.writeBytes("      ZDEr = ");
	    		 for (I = 1; I <= NTX2; I++) {
	    			 raFile.writeBytes(TXT2[I-1]);
	    		 }
	    		 raFile.writeBytes(";\n");      
	    		 raFile.writeBytes("      "+VAR+"[0] = (ZDER * Math.cos(T) - ZRAD * Math.sin(T))*("+HA+");\n");  
	    		 raFile.writeBytes("      "+VAR+"[1] = (ZDER * Math.sin(T) + ZRAD * Math.cos(T))*("+HA+");\n");
	    		 raFile.writeBytes("//\n");      
	    	 } // else
	     } // try
	     catch(IOException e) {
	    	 MipavUtil.displayError("IOException " + e + " in PTFUN2");
	    	 System.exit(-1);
	     }
    		
    }
      
   private void WRSYM3(int NARCS,int ORDRG, boolean REFLN, int CHN, RandomAccessFile raFile) {

    //**** TO WRITE THE CODE TO RECOVER THE DERIVATIVE FROM ITS SYMMETRIC
    //**** COUNTERPART ON THE FUNDAMENTAL BOUNDARY SECTION.

    //.......................................................................
    // AUTHOR: DAVID HOUGH, ETH, ZUERICH
    // LAST UPDATE: 4 AUG 1990
    //.......................................................................C
    //     LOCAL VARIABLES

    int I;


    try {
	    raFile.writeBytes("//\n"); 
	
	    if (REFLN) {
	        if (ORDRG > 1) {
	            I=2*NARCS;
	            if (NARCS > 1) {
	                raFile.writeBytes("      IS = (IA-IB)%"+I+";\n");
	                raFile.writeBytes("      IR=(IA-IB-IS)/"+I+";\n");
	            }
	            else {
	                raFile.writeBytes("      IS=(IA-1)%2;\n");
	                raFile.writeBytes("      IR=(IA-1-IS)/2;\n");
	            }
	            raFile.writeBytes("      if ((IR == 0) && (IS == 0)) {\n");
	            raFile.writeBytes("          DPARFN[0] =ZETA[0];\n");
	            raFile.writeBytes("          DPARFN[1] =ZETA[1];\n");
	            raFile.writeBytes("      }\n");
	            raFile.writeBytes("      else if ((IR > 0) && (IS == 0)) {\n");
	            raFile.writeBytes("          DPARFN[0] = WW[IR-1][0]*ZETA[0] - WW[IR-1][1]*ZETA[1];\n");
	            raFile.writeBytes("          DPARFN[1] = WW[IR-1][0]*ZETA[1] + WW[IR-1][1]*ZETA[0];\n");
	            raFile.writeBytes("      }\n");
	            raFile.writeBytes("       else if ((IR == 0 && (IS > 0)) {\n");
	            raFile.writeBytes("           DPARFN[0] = -U2[0]*ZETA[0] -U2[1]*ZETA[1];\n");
	            raFile.writeBytes("           DPARFN[1] = U2[0]*ZETA[1] -U2[1]*ZETA[0];\n");
	            raFile.writeBytes("       }\n");
	            raFile.writeBytes("       else {\n");
	            raFile.writeBytes("           double realPart = -U2[0]*WW[IR-1][0] + U2[1]*WW[IR-1][1]);\n");
	            raFile.writeBytes("           double imagPart = -U2[0]*WW[IR-1][1] + U2[1]*WW[IR-1][0]);\n");
	            raFile.writeBytes("           DPARFN[0] = realPart*ZETA[0] + imagPart*ZETA[1]);\n");
	            raFile.writeBytes("           DPARFN[1] = imagPart*ZETA[0] - realPart*ZETA[1]);\n"); 
	            raFile.writeBytes("       }\n");
	        } // if (ORDRG > 1)
	        else {
	            if (NARCS > 1) {
	                raFile.writeBytes("      IS=IA-IB;\n");
	            }
	            else {
	                raFile.writeBytes("      IS=IA-1;\n");
	            }
	            raFile.writeBytes("      if (IS == 0) {\n");
	            raFile.writeBytes("          DPARFN[0] = ZETA[0]\n");
	            raFile.writeBytes("          DPARFN[0] = ZETA[0]\n");
	            raFile.writeBytes("      }\n");
	            raFile.writeBytes("         else {\n");
	            raFile.writeBytes("             DPARFN[0] = -U2[0]*ZETA[0] + U2[1]*ZETA[1]);\n");
	            raFile.writeBytes("             DPARFN[1] = U2[0]*ZETA[1] - U2[1]*ZETA[0]);\n");
	            raFile.writeBytes("         }\n");
	        } // else
	    } // if (REFLN)
	    else {
	        if (NARCS > 1) {
	            raFile.writeBytes("      IR=(IA-IB)/"+NARCS+";\n");
	        }
	        else {
	            raFile.writeBytes("      IR=IA-1;\n");
	        }
	        raFile.writeBytes("      if (IR == 0) {\n");
	        raFile.writeBytes("          DPARFN[0] = ZETA[0];\n");
	        raFile.writeBytes("          DPARFN[1] = ZETA[1];\n");
	        raFile.writeBytes("      }\n");
	        raFile.writeBytes("      else {\n");
	        raFile.writeBytes("          DPARFN[0]= WW[IR-1][0]*ZETA[0] - WW[IR-1][1]*ZETA[1];\n");
	        raFile.writeBytes("          DPARFN[1]= WW[IR-1][0]*ZETA[1] + WW[IR-1][1]*ZETA[0];\n");
	        raFile.writeBytes("      }\n");
	    }
    } // try
    catch(IOException e) {
    	MipavUtil.displayError("IOException " + e + " in WRSYM3");
    	System.exit(-1);
    }

    } // private void WRSYM3
   
   private void TSTPLT(String JBNM, double MXMIS, double MXDIF, int NARCS, double PSD,
		   double MINPD, double MAXPD, int CHNL, int IER[]) {
   //CHARACTER*4 JBNM

    //......................................................................

    // 1.     TSTPLT
    //           TESTS THE PARAMETRIC FUNCTION ROUTINES PARFUN AND DPARFN
    //           FOR CONSISTENCY AND OUTPUTS BOUNDARY POINTS FOR PLOTTING.
           

    // 2.     PURPOSE
    //           THE ROUTINE FIRST CHECKS THAT THE PARAMETRIC FUNCTION
    //           ROUTINE PARFUN IS CONSISTENT WITH RESPECT TO ITS DEFINITION
    //           OF ANY CORNERS ON THE BOUNDARY.  THIS IS DONE BY CHECKING 
    //           THAT THE COMPUTED POINT AT THE END OF EACH ARC MATCHES THE 
    //           COMPUTED POINT AT THE START OF THE NEXT ONE.  IF ALL THE
    //           RELATIVE MISFIT ERRORS AT CORNERS ARE LESS THAN
    //           10*(UNIT ROUNDOFF) THEN ALL CORNERS ARE CONSIDERED TO FIT
    //           SATISFACTORILY, OTHERWISE THE MAXIMUM RELATIVE MISFIT
    //           ERROR IS REPORTED.

    //           THE SECOND PURPOSE OF THE ROUTINE IS TO OUTPUT TO A DATA 
    //           FILE THE COORDINATES OF A NUMBER OF POINTS ON THE BOUNDARY. 
    //           THE BOUNDARY POINTS ARE SELECTED ADAPTIVELY TO MEET THE 
    //           PLOTTING RESOLUTION SPECIFICATIONS DEFINED BY THE ARGUMENTS 
    //           PSD,MINPD,MAXPD (SEE BELOW).  THE HOPE IS THAT THE USER MAY 
    //           EASILY FEED THESE DATA POINTS TO HIS LOCAL GRAPH PLOTTING  
    //           ROUTINES SO AS TO CONSTRUCT A PLOT OF THE BOUNDARY.  THIS   
    //           WILL PROVIDE AN ESSENTIAL VISUAL CHECK ON THE VALIDITY OF 
    //           THE ROUTINE PARFUN.  THE OUTPUT DATA FILE IS AUTOMATICALLY 
    //           NAMED <JBNM>zz.
 
    //           THE THIRD PURPOSE OF THE ROUTINE IS TO CHECK PARFUN AND 
    //           DPARFN FOR MUTUAL CONSISTECY.  THIS IS DONE BY COMPUTING 
    //           TWO POINT FINITE DIFFERENCE APPROXIMATIONS TO DPARFN.  
    //           THESE DIFFERENCE APPROXIMATIONS ARE COMPUTED AT EACH BOUND- 
    //           ARY POINT THAT IS OUTPUT FOR PLOTTING AND ALSO AT NEARBY 
    //           POINTS WHICH LIE JUST O F F THE BOUNDARY.  THIS LATTER 
    //           COMPARISON ALSO TESTS PARFUN AND DPARFN FOR CORRECTNESS IN 
    //           ACCEPTING COMPLEX PARAMETER VALUES.   A RELATIVE ERROR IN 
    //           THE FINITE DIFFERENCE APPROXIMATION GREATER THAN 0.1 IS 
    //           REPORTED AS A POSSIBLE LOGICAL INCONSISTENCY BETWEEN PARFUN 
    //           AND DPARFN.  (THE CRITICAL RELATIVE ERROR VALUE OF 0.1 CAN 
    //           BE ALTERED BY ADJUSTING THE LOCAL PARAMETER DTOL).

    // 3.     CALLING SEQUENCE
    //           CALL TSTPLT(JBNM,MXMIS,MXDIF,NARCS,PSD,MINPD,MAXPD,CHNL,IER)

    //        PARAMETERS
    //         ON ENTRY
    //            JBNM   - CHARACTER*4
    //                     THE JOB NAME.  THIS IS USED TO CREATE THE OUTPUT 
    //                     FILE WITH FILENAME

    //                                <JBNM>zz ,

    //                     WHERE <JBNM> DENOTES THE VALUE OF VARIABLE JBNM
    //                     WITH ANY TRAILING SPACES DELETED.

    //            NARCS  - INTEGER
    //                     THE NUMBER OF ANALYTIC ARCS THAT MAKE UP THE 
    //                     W H O L E BOUNDARY OF THE PHYSICAL DOMAIN.

    //            PSD    - REAL
    //                     THE PLOTTING SIZE FOR THE DOMAIN IN ANY APPROPR-
    //                     IATE UNITS.  IF PSD .LE. 0.0 THEN IT IS ASSIGNED
    //                     THE DEFAULT VALUE OF 160.0 (A REASONBLE WIDTH IN
    //                     MM FOR PLOTTING ON A4 PAPER).

    //            MINPD  - REAL
    //                     THE MINIMUM SIGNIFICANT PLOTTING DISTANCE, IN THE
    //                     SAME UNITS AS PSD.  IF PSD .LE. 0.0 THEN IT IS
    //                     ASSIGNED THE DEFAULT VALUE OF 2.0.

    //            MAXPD  - REAL
    //                     THE MAXIMUM ALLOWED PLOTTING DISTANCE, IN THE
    //                     SAME UNITS AS PSD.  IF PSD .LE. 0.0 THEN IT IS
    //                     ASSIGNED THE DEFAULT VALUE OF 5.0.  THE LARGER
    //                     MAXPD, THE COARSER WILL BE THE RESOLUTION OF THE
    //                     BOUNDARY POINTS OUTPUT TO <JBNM>zz, BUT THE
    //                     QUICKER THEY WILL BE COMPUTED. 

    //            CHNL   - INTEGER
    //                     DEFINES AN OUTPUT CHANNEL THAT MAY BE USED FOR
    //                     WRITING THE FILE <JBNM>zz.
    //         ON EXIT
    //            MXMIS  - REAL
    //                     THE MAXIMUM RELATIVE MISFIT ERROR OVER ALL
    //                     CORNER POINTS

    //            MXDIF  - REAL
    //                     THE MAXIMUM RELATIVE ERROR IN FINITE DIFFERENCE
    //                     APPROXIMATIONS TO DPARFN OVER ALL BOUNDARY 
    //                     POINTS OUTPUT TO <JBNM>zz AND NEARBY POINTS OFF
    //                     THE BOUNDARY.

    //            PSD    - REAL
    //                     IF PSD .LE. 0.0 ON ENTRY THEN IT WILL HAVE
    //                     THE DEFAULT VALUE OF 160.0 ON  EXIT.

    //            MINPD  - REAL
    //                     IF PSD .LE. 0.0 ON ENTRY THEN MINPD WILL HAVE
    //                     THE DEFAULT VALUE OF 2.0 ON EXIT

    //            MAXPD  - REAL
    //                     IF PSD .LE. 0.0 ON ENTRY THEN MAXPD WILL HAVE
    //                     THE DEFAULT VALUE OF 5.0 ON EXIT

    //            IER    - INTEGER
    //                     IF IER > 0 THEN AN ABNORMAL EXIT HAS OCCURRED;
    //                     A MESSAGE TO DESCRIBE THE ERROR IS AUTOMATICALLY
    //                     WRITTEN ON THE STANDARD OUTPUT CHANNEL.
    //                     IER=0 - NORMAL EXIT.
    //                     IER>0 - ABNORMAL EXIT; THE ERROR MESSAGE SHOULD
    //                             BE SELF EXPLANATORY.


    // 4.     SUBROUTINES OR FUNCTIONS NEEDED
    //              - THE CONFPACK LIBRARY.
    //              - THE REAL FUNCTION R1MACH.
    //              - THE USER SUPPLIED COMPLEX FUNCTIONS PARFUN AND DPARFN.


    // 5.     FURTHER COMMENTS
    //              - A SUMMARY LISTING IS AUTOMATICALLY WRITTEN ON THE 
    //                STANDARD OUTPUT CHANNEL.
    //              - THE OUTPUT FILE <JBNM>zz CONTAINS COORDINATE PAIRS

    //                                 X Y

    //                FOR POINTS ON THE PHYSICAL BOUNDARY, WITH ONE PAIR
    //                PER LINE.

    // ......................................................................
    //     AUTHOR: DAVID HOUGH, ETH, ZUERICH
    //     LAST UPDATE: 6 JULY 1990
    // ......................................................................C
    //     LOCAL VARIABLES

    int I,IA;
    int IMX = 0;
    double TINC = 0.0;
    double TMX = 0.0;
    double A1,DIFF,ERR,HH,MINC,RMAX,RMEAN,RMIN,T,TOL1,TSD;
    double TT[] = new double[2];
    //REAL TT(2)
    double C1[] = new double[2];
    double C2[] = new double[2];
    double CENTR[] = new double[2];
    double ZZ0[] = new double[2];
    double DZZ[] = new double[2];
    double NDZZ[] = new double[2];
    //COMPLEX C1,C2,CENTR,ZZ0,DZZ,NDZZ;
    double ZZ[][] = new double[2][2];
    //COMPLEX ZZ(2)
    //CHARACTER OFL*6
    final int MNARC = 200;
    final double DTOL = 1.0E-1;
    final int NH = 4;
    boolean ATEND,FIRST,WARND;
    boolean LNSEG[] = new boolean[MNARC];
    double PIN[] = new double[2];
    double PAROUT[];
    int zzindex;
    double cr[] = new double[1];
    double ci[] = new double[1];
    //EXTERNAL DPARFN,LINSEG,PARFUN,R1MACH,WRHEAD,WRTAIL,ZDPARF
    //COMPLEX PARFUN,DPARFN,ZDPARF

    //**** WRITE CONFPACK HEADING

    WRHEAD(7,0,null);

    if (NARCS > MNARC) {
        IER[0]=59;
        WRTAIL(7,0,IER[0], null);
    }

    //1     FORMAT(A45)
    //2     FORMAT(A45,I4)
    //3     FORMAT(A45,E10.3)
    //4     FORMAT(//,T17,A)

    TOL1= 10.0*EPS;

    //**** CHECK THAT ALL ARCS MEET AT CORNER POINTS

    IER[0]=0;
    CENTR[0] = 0.0;
    CENTR[1] = 0.0;
    MXMIS=0.0;
    for (IA=1; IA <= NARCS; IA++) {
        if (IA == 1) {
            I=NARCS;
        }
        else {
            I=IA-1;
        }
        PIN[0] = -1.0;
        PIN[1] = 0.0;
        C1 = PARFUN(IA, PIN);
        CENTR[0] = CENTR[0] + C1[0];
        CENTR[1] = CENTR[1] + C1[1];
        A1 = zabs(C1[0],C1[1]);
        PIN[0] = 1.0;
        PIN[1] = 0.0;
        C2=PARFUN(I,PIN);
        ERR = zabs(C1[0]-C2[0],C1[1]-C2[1]);
        if (A1 >= 1.0) {
            ERR=ERR/A1;
        }
        if (ERR > MXMIS) {
            IMX=IA;
            MXMIS=ERR;
        }
    } // for (IA=1; IA <= NARCS; IA++)
	if (MXMIS >= TOL1) {
        System.out.println("MAXIMUM CORNER MISFIT: " + MXMIS);
        System.out.println("OCCURS AT CORNER: " + IMX);
    }
    else {
        System.out.println("ALL ARCS FIT AT CORNERS:");
    }	

    // **** ESTIMATE THE DIAMETER (TSD) OF THE PHYSICAL DOMAIN

    CENTR[0]=CENTR[0]/NARCS;
    CENTR[1]=CENTR[1]/NARCS;
    TSD=0.0;
    HH=2.0/(double)(NH);
    for (IA=1; IA <= NARCS; IA++) {
        T=-1.0;
        for (I=1; I <= NH; I++) {
            T=T+HH;
            PIN[0] = T;
            PIN[1] = 0;
            PAROUT = PARFUN(IA,PIN);
            C1[0] = PAROUT[0] - CENTR[0];
            C1[1] = PAROUT[1] - CENTR[1];
            A1=zabs(C1[0],C1[1]);
            TSD=Math.max(TSD,A1);
        } // for (I=1; I <= NH; I++)
    } // for (IA=1; IA <= NARCS; IA++)
   TSD=2.0*TSD;

   //**** DETERMINE WHICH ARCS (IF ANY) ARE LINE SEGMENTS

   LINSEG(LNSEG,NARCS);

   //**** OPEN FILE TO RECEIVE BOUNDARY DATA POINTS FOR PLOTTING

   //L=INDEX(JBNM,' ')-1
   //IF (L.EQ.-1) L=4
   //OFL=JBNM(1:L)//'zz'
   //OPEN(CHNL,FILE=OFL)
   // Use global zzset instead

   // **** SET DEFAULT PLOTTING DISTANCES, IF NECESSARY

   if (PSD <= 0.0) {
       PSD=1.6E+2;
       MINPD=2.0;
       MAXPD=5.0;
   }
   RMIN=MINPD*TSD/PSD;
   RMAX=MAXPD*TSD/PSD;
   RMEAN=0.5*(RMIN+RMAX);
   MINC=Math.sqrt(EPS);
   
   // **** START EVALUATING BOUNDARY POINTS AND DERIVATIVES FOR PLOTTING AND
   // **** TESTING

   MXDIF=0.0;
   zzindex = 0;
   for (IA=1; IA <= NARCS; IA++) {
       TT[0]=-1.0;
       PIN[0] = TT[0];
       PIN[1] = 0.0;
       ZZ[0]=PARFUN(IA,PIN);
       zzset[zzindex][0] = ZZ[0][0];
       zzset[zzindex++][1] = ZZ[0][1];
       if (IA == 1) {
    	   ZZ0[0] = ZZ[0][0];
    	   ZZ0[1] = ZZ[0][1];
       }
       FIRST = true;
       WARND= false;
       while (true) {
	   
           //****   TEST THE COMPATIBILTY OF PARFUN AND DPARFN BY ESTIMATING DPARFN
           //****   NUMERICALLY AT BOTH REAL AND COMPLEX PARAMETER VALUES.

	         for (I=1; I <= 2; I++) {
	             if (I == 1) {
	                 C1[0]=TT[0];
	                 C1[1] = 0.0;
	             }
	             else {
	                 C1[0]=TT[0];
                     C1[1] = MINC;
	             }
	             DZZ=DPARFN(IA,C1);
	             NDZZ=ZDPARF(IA,C1);
	             A1=zabs(DZZ[0],DZZ[1]);
	
	             if (A1 == 0.0) {
	                 IER[0]=60;
	                 System.out.println();
	                 System.out.println("              ***DPARFN=(0.,0.)***");
	                 System.out.println("                             ARC: " + IA); 
	                 System.out.println(" STANDARDISED PARAMETER VALUE: " + TT[0]);
	                 WRTAIL(7,0,IER[0],null);
	                 return;
	             } // if (A1 == 0.0)
	
	             if (A1 <=TOL1 && !WARND) {
	                 System.out.println("*** W A R N I N G  ***");
	                 System.out.println("PATHOLOGICALLY SMALL DERIVATIVE ON ARC" + IA);
	                 WARND=true;
	             } // if (A1 <=TOL1 && !WARND)
	
	             if (FIRST) {
	                 TINC=RMEAN/A1;
	                 TINC=Math.max(TINC,MINC);
	                 FIRST=false;
	             } // if (FIRST)
	
	             zdiv(1.0-NDZZ[0],-NDZZ[1],DZZ[0],DZZ[1],cr,ci);
	             ERR = zabs(cr[0],ci[0]);
	             if (ERR > MXDIF) {
	                 MXDIF=ERR;
	                 IMX=IA;
	                 TMX=TT[0];
	             } // if (ERR > MXDIF)
	         } // for (I=1; I <= 2; I++)
	
	     if (!LNSEG[IA-1]) {
	
	    	 //DETERMINE THE NEXT BOUNDARY POINT TO BE PLOTTED
	         while (true) {
	             TT[1]=TT[0]+TINC;
	             if (TT[1] >= 1.0) {
	                 TT[1]=1.0;
	                 ATEND=true;
	             }
	             else {
	                 ATEND=false;
	             }
	       
	             PIN[0] = TT[1];
	             PIN[1] = 0.0;
	             ZZ[1] = PARFUN(IA,PIN);
	             DIFF=zabs(ZZ[1][0]-ZZ[0][0],ZZ[1][1]-ZZ[0][1]);
	             if (DIFF == 0.0 && !ATEND) {
	                 TINC=Math.max(MINC,2*TINC);
	                 continue;
	             } // if (DIFF == 0.0 && !ATEND)
	
	             if (DIFF > RMAX || (DIFF < RMIN && !ATEND)) {
	                 TINC=RMEAN*TINC/DIFF;
	                 TINC=Math.max(TINC,MINC);
	                 continue;
	             } // if (DIFF > RMAX || (DIFF < RMIN && !ATEND))
	             break;
	         } // while (true)
	
	         zzset[zzindex][0] = ZZ[1][0];
	         zzset[zzindex++][1] = ZZ[1][1];
	         if (!ATEND) {
	             ZZ[0][0]=ZZ[1][0];
	             ZZ[0][1] = ZZ[1][1];
	             TT[0]=TT[1];
	       }
	       else {
	    	   break;
	       }
	     } // if (!LNSEG[IA-1])
	     else {
	    	 break;
	     }
       } // while (true)

   } // for (IA=1; IA <= NARCS; IA++)
   if (LNSEG[NARCS-1]) {
	   zzset[zzindex][0] = ZZ0[0];
       zzset[zzindex++][1] = ZZ0[1];
   }   

   if (MXDIF > DTOL) {
       System.out.println();
       System.out.println("POSSIBLE PARFUN/DPARFN INCONSISTECY ON ARC: " + IMX);
       System.out.println("OCCURS AT STANDARDISED PARAMETER VALUE: " + TMX);
       System.out.println("RELATIVE FINITE DIFF ERROR: " + MXDIF);
   }
   else {
       System.out.println();
       System.out.println("PARFUN AND DPARFN ARE CONSISTENT:");
   }

   WRTAIL(7,0,IER[0],null);

   } // private void TSTPLOT
   
   private void LINSEG(boolean LNSEG[], int NARCS) {

       //**** TO DETERMINE THE ARRAY LNSEG, WHERE LNSEG(I) IS SET TO TRUE IF THE
       //**** I'TH ARC IS A LINE SEGMENT, I=1,...,NARCS.

       //**** LOCAL VARIABLES

       final int NPTS = 9;
	   int IA,J,NINTS;
       double DIFF,HH,MXDIF,TOL;
       double SUM[] = new double[2];
       double TT[] = new double[2];
       // COMPLEX SUM,TT
       double DF[][] = new double[NPTS][2];
       //COMPLEX DF(NPTS)
       //EXTERNAL DPARFN,R1MACH
       // COMPLEX DPARFN

       NINTS=NPTS-1;
       HH=2.0/NINTS;
       TOL=10.0*EPS;

       for (IA=1; IA <= NARCS; IA++) {
    	   SUM[0] = 0.0;
    	   SUM[1] = 0.0;
           for (J=1; J <= NPTS; J++) {
               TT[0]=-1.0+(J-1)*HH;
               TT[1] = 0.0;
               DF[J-1]=DPARFN(IA,TT);
               SUM[0]=SUM[0]+DF[J-1][0];
               SUM[1]=SUM[1]+DF[J-1][1];
           } // for (J=1; J <= NPTS; J++)
           SUM[0]=SUM[0]/NPTS;
           SUM[1]=SUM[1]/NPTS;

           MXDIF=0.0;
           for (J=1; J <= NPTS; J++) {
               DIFF=zabs(SUM[0]-DF[J-1][0],SUM[1]-DF[J-1][1]);
               MXDIF = Math.max(MXDIF,DIFF);
           } // for (J=1; J <= NPTS; J++)

           if (MXDIF <= TOL) {
               LNSEG[IA-1] = true;
           }
           else {
               LNSEG[IA-1] = false;
           }
       } // for (IA=1; IA <= NARCS; IA++)

   } // private void LINSEG
   

   //COMPLEX FUNCTION ZDPARF(I,T)
   private double[] ZDPARF(int I, double T[]) {
       //COMPLEX T

       //**** NUMERICAL ESTIMATION OF THE DERIVATIVE OF THE PARAMETRIC FUNCTION
       //**** USING 2- OR 4-POINT TRAPEZOIDAL RULE ESTIMATES IN CAUCHY'S
       //**** FORMULA.  THE 2-POINT ESTIMATE IS THE STANDARD CENTRAL DIFFERENCE
       //**** IN THE REAL AXIS DIRECTION.

       double EPSZ;
       //final double IM[] = new double[]{0.0,1.0};
       final boolean FOUR = false;
       double SUM[] = new double[2];
       //COMPLEX IM,SUM
       double POUT1[];
       double POUT2[];
       double result[] = new double[2];
       double PIN[] = new double[2];

       //EXTERNAL PARFUN
       // COMPLEX PARFUN

       EPSZ = Math.pow(EPS, 0.3333);
       PIN[0] = T[0] + EPSZ;
       PIN[1] = T[1];
       POUT1 = PARFUN(I,PIN);
       PIN[0] = T[0] - EPSZ;
       PIN[1] = T[1];
       POUT2 = PARFUN(I,PIN);
       SUM[0] = (POUT1[0] - POUT2[0])/2.0/EPSZ;
       SUM[1] = (POUT1[1] - POUT2[1])/2.0/EPSZ;

       if (FOUR) {
    	    PIN[0] = T[0];
    	    PIN[1] = T[1]+EPSZ;
    	    POUT1 = PARFUN(I,PIN);
    	    PIN[0] = T[0];
    	    PIN[1] = T[1]-EPSZ;
    	    POUT2 = PARFUN(I,PIN);
            result[0] = SUM[0]/2.0 + (POUT1[1] - POUT2[1])/4.0/EPSZ;
            result[1] = SUM[1]/2.0 - (POUT1[0] - POUT2[0])/4.0/EPSZ;
       }
       else {
            result[0]=SUM[0];
            result[1] = SUM[1];
       }
       return result;
   }

   private void JAPHYC(String JBNM, String HEAD, double MAXER,boolean INTER, int NARCS,
		      int ISYGP, int NQPTS, boolean INCST,
		      int RFARC, double RFARG[], double CENTR[], int TSTNG,int OULVL, int IBNDS[],
		      int MNEQN, double MATRX[][][], int IWORK[], double RWORK[],
		      double ZWORK[][], boolean LWORK[], int OCH, int IGEOM[], double RGEOM[],
		      int ISNPH[], double RSNPH[], int IER[]) {
		
		      //INTEGER IBNDS(*),IGEOM(*),ISNPH(*),IWORK(*)
		      //REAL RGEOM(*),MATRX(MNEQN,MNEQN,2),RSNPH(*),RWORK(*)
		      //COMPLEX CENTR
		      //COMPLEX ZWORK(*)
		      //LOGICAL LWORK(*)
		      //CHARACTER JBNM*4,HEAD*72
		
		// ......................................................................
		
		// 1.     JAPHYC
		//           COMPUTATION OF PIECEWISE ORTHOGONAL JACOBI POLYNOMIAL 
		//           APPROXIMATIONS TO THE BOUNDARY CORRESPONDENCE DERIVATIVE FOR
		//           THE MAP:PHYSICAL --> CANONICAL.
		
		// 2.     PURPOSE
		//           THE MAIN PURPOSE IS TO CALCULATE THE COEFFICIENTS IN THE
		//           PIECEWISE ORTHOGONAL JACOBI POLYNOMIAL APPROXIMATIONS TO THE
		//           BOUNDARY CORRESPONDENCE DERIVATIVE FOR THE CONFORMAL MAP OF
		//           A GIVEN SIMPLY CONNECTED PHYSICAL DOMAIN (WITH PIECEWISE
		//           ANALYTIC BOUNDARY) ONTO A CANONICAL DOMAIN (WITH UNIT CIRCLE
		//           AS BOUNDARY).  AN INTERIOR PHYSICAL DOMAIN IS MAPPED TO THE 
		//           UNIT DISC, AN EXTERIOR PHYSICAL DOMAIN TO THE COMPLEMENT OF
		//           THE CLOSED UNIT DISC.
		//           THE METHOD USED IS AN ADAPTIVE COLLOCATION SOLUTION OF
		//           SYMM'S INTEGRAL EQUATION.  
		//           A NUMBER OF DATA ARRAYS ASSOCIATED WITH THE POLYNOMIAL
		//           APPROXIMATIONS ARE ALSO COMPUTED AND MAY BE USED FOR SUBSE-
		//           QUENT PROCESSING.  IN ADDITION TO BEING RETURNED AS 
		//           PARAMETERS OF THE SUBROUTINE THESE ARE ALSO AUTOMATICALLY
		//           OUTPUT TO DATA FILES.
		
		// 3.     CALLING SEQUENCE
		//           CALL JAPHYC(JBNM,HEAD,MAXER,INTER,NARCS,ISYGP,NQPTS,INCST,
		//                       RFARC,RFARG,CENTR,TSTNG,OULVL,IBNDS,MATRX,IWORK,
		//                       RWORK,ZWORK,LWORK,OCH,IGEOM,RGEOM,ISNPH,RSNPH,
		//                       IER)
		
		//        PARAMETERS
		//         ON ENTRY
		//            JBNM   - CHARACTER*4
		//                     THE JOB NAME.  THIS IS USED TO CREATE THREE OUT-
		//                     PUT FILES WITH FILENAMES
		
		//                         <JBNM>pl, <JBNM>gm, <JBNM>ph,
		
		//                     WHERE <JBNM> DENOTES THE VALUE OF VARIABLE JBNM
		//                     WITH ANY TRAILING SPACES DELETED.  THE FIRST OF
		//                     THESE IS A LISTING FILE RECORDING THE PROGRESS
		//                     AND RESULTS OF THE CALCULATION FOR LATER READING
		//                     BY THE USER.  THE TWO FILES <JBNM>gm AND <JBNM>ph
		//                     ARE DATA FILES, NOT REALLY INTENDED TO BE READ
		//                     BY THE USER.
		//                     THE VALUE OF JBNM IS ALSO THE ONLY ITEM IN A
		//                     FOURTH OUTPUT FILE NAMED (LITERALLY) jbnm.
		
		//            HEAD   - CHARACTER*72
		//                     A HEADING FOR THE PROBLEM, TO APPEAR ON THE
		//                     LISTING FILE <JBNM>pl.
		
		//            MAXER  - REAL
		//                     RELATIVE ACCURACY REQUESTED FOR THE CONFORMAL MAP;
		//                     THIS IS THE SAME AS THE ABSOLUTE ACCURACY ON THE
		//                     BOUNDARY OF THE PHYSICAL DOMAIN.
		
		//            INTER  - LOGICAL
		//                     TRUE IF THE PHYSICAL DOMAIN IS INTERIOR, FALSE 
		//                     OTHERWISE.
		
		//            NARCS  - INTEGER
		//                     THE NUMBER OF ANALYTIC ARCS THAT MAKE UP THE 
		//                     W H O L E BOUNDARY OF THE PHYSICAL DOMAIN.
		
		//            ISYGP  - INTEGER
		//                     THE MAGNITUDE OF ISYGP IS THE ORDER OF THE
		//                     SYMMETRY GROUP OF THE PHYSICAL DOMAIN.
		//                     ISYGP.EQ.1  -THE SYMMETRY GROUP HAS ONLY ONE ELE-
		//                                  MENT,THE IDENTITY TRANSFORMATION;  IN
		//                                  OTHER WORDS, THE DOMAIN HAS 'NO 
		//                                  SYMMETRY'.
		//                     ISYGP.GT.1  -THE SYMMETRY GROUP CONTAINS ONLY
		//                                  PROPER (IN-PLANE) ROTATIONS; IN OTHER
		//                                  WORDS, THE DOMAIN HAS ONLY ROTATIONAL
		//                                  SYMMETRIES.
		//                     ISYGP.LT.-1 -THE SYMMETRY GROUP CONTAINS IMPROPER
		//                                  (OUT-OF-PLANE) ROATIONS; IN OTHER
		//                                  WORDS, THE DOMAIN HAS REFLECTIONAL
		//                                  SYMMETRY AND MAY ALSO HAVE ROTATIONAL
		//                                  SYMMETRIES.
		//                     AN INPUT VALUE OF -1 OR 0 IS TREATED AS IF IT WERE
		//                     1.
		
		//            NQPTS  - INTEGER
		//                     PLAYS TWO ROLES.
		//                     1. THE NUMBER OF QUADRATURE POINTS TO BE USED IN 
		//                        AN ELEMENTARY GAUSS-JACOBI RULE;  COMPOSITE 
		//                        RULES ARE CONSTRUCTED FROM PANELS OF NQPTS-
		//                        POINT RULES.
		//                     2. THE MAXIMUM DEGREE OF POLYNOMIAL APPROXIMATION
		//                        IS FIXED AT NQPTS-1.
		//                     NQPTS SHOULD BE REASONABLY LARGE; A PRACTICAL RULE
		//                     OF THUMB IS THAT IF MACHINE PRECISION IS X*1E-N,
		//                    1<X<10, THEN NQPTS=N+1.
		
		//            INCST  - LOGICAL
		//                     IF INCST IS TRUE THEN AN INCREMENTAL STRATEGY IS
		//                     USED TO TRY TO ACHIEVE THE ACCURACY SPECIFIED BY
		//                     MAXER; VERY ROUGHLY SPEAKING, THIS MEANS THAT THE 
		//                     METHOD SUCCESSIVELY ACHIEVES THE TARGET ACCURACIES
		//                     1E-1,1E-2,...UNTIL MAXER HAS BEEN ACHIEVED.  IF 
		//                     THE PROBLEM IS THOUGHT TO BE EITHER PARTICULARLY
		//                     DIFFICULT OR PARTICULARLY SIMPLE, THEN INCST 
		//                     SHOULD BE SET TO .TRUE.  FOR PROBLEMS OF 'AVERAGE'
		//                     DIFFICULTY, SETTING INCST TO .FALSE. IS USUALLY 
		//                     MORE EFFICIENT.
		
		//            RFARC  - INTEGER
		//                     THE REFERENCE ARC USED TO DEFINE THE ORIENTATION 
		//                     THAT IS GIVEN TO THE MAP.  THE CONVENTION IS THAT 
		//                     THE POINT AT THE START OF ANALYTIC ARC NUMBER 
		//                     RFARC IS MAPPED TO THE POINT WITH ARGUMENT 
		//                     RFARG*PI ON THE UNIT DISC.
		
		//            RFARG  - REAL
		//                     THE REFERENCE ARGUMENT/PI USED TO DEFINE THE 
		//                     ORIENTATION THAT IS GIVEN TO THE MAP.  SEE RFARC 
		//                     ABOVE.
		
		//            CENTR  - COMPLEX
		//                     THE POINT IN THE PHYSICAL PLANE THAT IS TO BE
		//                     MAPPED TO THE CENTRE OF THE UNIT DISC.  FOR
		//                     EXTERIOR DOMAINS CENTR MUST BE SOME POINT IN THE
		//                     COMPLEMENTARY INTERIOR  PHYSICAL DOMAIN.
		//                     IN CASE ABS(ISYGP).GT.1 THEN CENTR MUST ALSO BE
		//                     A CENTRE OF SYMMETRY FOR THE PHYSICAL DOMAIN.
		
		//            TSTNG  - INTEGER
		//                     EITHER 0 OR 1.
		//                     ON SUCCESSFUL COMPLETION OF THE NUMERICAL SOLUTION
		//                     OF SYMM'S EQUATION, A MODULE IS PROVIDED FOR
		//                     TESTING THE ERROR IN THE MODULUS OF THE COMPUTED
		//                     MAP ON THE BOUNDARY OF THE DOMAIN.
		//                     TSTNG=0 - TEST ONLY AT SUB-ARC END POINTS
		//                     TSTNG=1 - IN ADDITION TO TESTING AT SUB-ARC END
		//                               POINTS TEST ALSO AT INTERIOR POINTS
		//                               ON EACH SUB-ARC.
		
		//            OULVL  - INTEGER
		//                     EITHER 0,1,2,3,4 OR 5.
		//                     CONTROLS THE AMOUNT OF OUTPUT IN THE LISTING FILE
		//                     <JBNM>pl.
		//                     OULVL=0 - OUTPUT A SOLUTION SUMMARY AT EACH STAGE
		//                               IN THE ADAPTIVE PROCESS AND A SHORT
		//                               SUMMARY OF THE ERRORS IN MODULUS.
		//                     OULVL=1 - AS 0, BUT ALSO OUTPUT A DETAILED LIST OF
		//                               THE ERRORS IN MODULUS.
		//                     OULVL=2 - AS 0, BUT ALSO OUTPUT FULL DETAILS OF 
		//                               THE FINAL COMPUTED JACOBI COEFFICIENTS 
		//                               ON SUCCESSFUL COMPLETION.
		//                     OULVL=3 - AS 2, BUT ALSO OUTPUT A DETAILED LIST OF
		//                               THE ERRORS IN MODULUS.
		//                     OULVL=4 - OUTPUT FULL DETAILS OF THE OF THE COMPU-
		//                               TED JACOBI COEFFICIENTS AT EVERY STAGE
		//                               IN THE ADAPTIVE PROCESS AND A SHORT
		//                               SUMMARY OF THE ERRORS IN MODULUS.
		//                     OULVL=5 - AS 4, BUT ALSO OUTPUT A DETAILED LIST OF
		//                               THE ERRORS IN MODULUS.
		
		//            IBNDS  - INTEGER ARRAY
		//                     INTEGER VECTOR OF SIZE AT LEAST 5.
		//                     IBNDS(K), K=1,2,3,4,5, DEFINE VARIOUS UPPER LIMITS
		//                     THAT HAVE BEEN SET IN THE CALLING PROGRAM AND 
		//                     WHICH CONTROL THE SIZES OF THE ARRAYS IGEOM,RGEOM,
		//                     MATRX,ISNPH,RSNPH,IWORK,RWORK,ZWORK,LWORK. 
		//                     THEIR MEANINGS ARE AS FOLLOWS:
		//                     IBNDS(1) - THE MAXIMUM NUMBER OF SUB-ARCS ALLOWED.
		//                     IBNDS(2) - THE MAXIMUM NUMBER OF JACOBI INDECES
		//                                ALLOWED (WHICH IS ALSO THE 1 + THE
		//                                MAXIMUM NUMBER OF CORNERS ALLOWED ON 
		//                                PHYSICAL BOUNDARY).
		//                     IBNDS(3) - 1 + THE MAXIMUM NUMBER OF PANELS
		//                                ALLOWED IN A SINGLE COMPOSITE GAUSSIAN
		//                                RULE.
		//                     IBNDS(4) - THE MAXIMUM TOTAL NUMBER OF QUADRATURE 
		//                                POINTS ALLOWED OVER ALL COMPOSITE 
		//                                GAUSSIAN RULES.
		//                                (IBNDS(4)<=(IBNDS(3)-1)*IBNDS(2)*NQPTS)
		
		//            MNEQN  - INTEGER
		//                     THE MAXIMUM NUMBER OF EQUATIONS ALLOWED IN THE 
		//                     LINEAR ALGEBRAIC SYSTEM RESULTING FROM THE 
		//                     COLLOCATION METHOD. (MNEQN <= 1+IBNDS(1)*NQPTS)
		
		//            MATRX  - REAL ARRAY
		//                     A 3-DIMENSIONAL MATRIX OF SIZE 
		//                          MNEQN X MNEQN X 2 .
		//                     (IN THE ADAPTIVE PROCESS, MATRX(*,*,2) WILL STORE 
		//                     THE COEFFICIENT MATRIX OF THE CURRENT COLLOCATION 
		//                     SYSTEM AND MATRX(*,*,1) WILL STORE THE COEFFICIENT
		//                     MATRIX OF THE PREVIOUS SYSTEM)
		
		//            IWORK  - INTEGER ARRAY
		//                     A WORKING VECTOR OF SIZE AT LEAST
		//                        8*IBNDS(1)+MNEQN+2*IBNDS(2) .
		
		//            RWORK  - REAL ARRAY
		//                     A WORKING VECTOR OF SIZE AT LEAST
		//                       (4 + 3*NQPTS + 5*IBNDS(2))*NQPTS + 2*IBNDS(1) +
		//                       2*MNEQN + IBNDS(3) + 5*IBNDS(2) + 2*IBNDS(4)
		
		//            ZWORK  - COMPLEX ARRAY
		//                     A WORKING VECTOR OF SIZE AT LEAST
		//                         MNEQN + 2*IBNDS(2)
		
		//            LWORK  - LOGICAL ARRAY
		//                     A WORKING VECTOR OF SIZE AT LEAST
		//                         3*IBNDS(1) + IBNDS(2)
		
		//            OCH    - INTEGER
		//                     DEFINES AN OUTPUT CHANNEL THAT MAY BE USED FOR
		//                     WRITING THE FILES <JBNM>pl, <JBNM>gm, <JBNM>ph.
		
		//         ON EXIT
		//            RFARG  - REAL
		//                     EXIT VALUE IS PI*(ENTRY VALUE)
		
		//            IGEOM  - INTEGER ARRAY
		//                     A VECTOR OF SIZE AT LEAST 
		//                          IBNDS(1) + 4;
		//                     STORES DATA RELATING TO THE ARC SUBDIVISIONS THAT
		//                     HAVE TAKEN PLACE.
		
		//            RGEOM  - REAL ARRAY
		//                     A VECTOR OF SIZE AT LEAST 
		//                          3*IBNDS(1)+2;
		//                     STORES DATA RELATING TO THE ARC SUBDIVISIONS THAT
		//                     HAVE TAKEN PLACE AND THE ARGUMENTS OF SUB-ARC END
		//                     POINTS ON THE UNIT DISC.
		
		//            ISNPH  - INTEGER ARRAY
		//                     A SOLUTION VECTOR OF SIZE AT LEAST 
		//                          3*IBNDS(1)+6;
		//                     STORES DATA DEFINING THE FINAL POLYNOMIAL DEGREES
		//                     ON THE SUB-ARCS, THE JACOBI INDEX FOR EACH SUB-ARC
		//                     AND POINTERS TO THE SOLUTIONS STORED IN RSNPH.
		
		//            RSNPH  - REAL ARRAY
		//                     A SOLUTION VECTOR OF SIZE AT LEAST
		//                         IBNDS(1)+2*MNEQN+3*IBNDS(2)*(1+2*NQPTS);
		//                     STORES DATA DEFINING THREE-TERM RECURRENCE
		//                     SCHEMES, ELEMENTARY GAUSS-JACOBI QUADRATURE RULES,
		//                     THE JACOBI COEFFICIENTS FOR THE BOUNDARY
		//                     CORRESPONDENCE FUNCTION AND ITS DERIVATIVE AND
		//                     THE ERRORS IN MODULUS ON EACH SUB-ARC.
		
		//            IER    - INTEGER
		//                     IF IER > 0 THEN AN ABNORMAL EXIT HAS OCCURRED;
		//                     A MESSAGE TO DESCRIBE THE ERROR IS AUTOMATICALLY
		//                     WRITTEN ON THE STANDARD OUTPUT CHANNEL AND THE
		//                     LISTING FILE <JBNM>pl.
		//                     IER=0 - NORMAL EXIT.
		//                     IER>0 - ABNORMAL EXIT; THE ERROR MESSAGE SHOULD
		//                             BE SELF EXPLANATORY.
		                                  
		
		// 4.     SUBROUTINES OR FUNCTIONS NEEDED
		//              - THE CONFPACK LIBRARY.
		//              - THE REAL FUNCTION R1MACH, WHICH IS A MACHINE CONSTANTS 
		//                ROUTINE OBTAINED FROM THE PORT LIBRARY.   
		//                IT MUST BE ADJUSTED TO SUIT EACH PARTICULAR MACHINE.  
		//                IF YOUR MACHINE ISN'T LISTED IN R1MACH THEN YOU'LL  
		//                HAVE TO WRITE YOUR OWN VERSION, BUT NOTE THAT CONFPACK 
		//                ONLY USES R1MACH(1), R1MACH(2) AND R1MACH(4).
		//              - THE FOLLOWING LINPACK ROUTINES:
		//                    ISAMAX   SASUM    SAXPY   SDOT    SGECO
		//                    SGEFA    SGEDI    SGESL   SSCAL   SSWAP
		//              - THE FOLLOWING QUADPACK ROUTINES:
		//                    QAWS     QAWSE    QC25S   QCHEB   QK15W
		//                    QMAC     QMOMO    QSORT   QWGTS
		//              - THE USER SUPPLIED COMPLEX FUNCTIONS PARFUN AND DPARFN
		//                WHICH DEFINE THE PARAMETRIC FUNCTION FOR THE PHYSICAL
		//                BOUNDARY AND THE DERIVATIVE OF THE PARAMETRIC FUNCTION.
		//                THE PARAMETRIC FUNCTION DEFINING THE K'TH ANALYTIC ARC
		//                SHOULD HAVE THE SUBROUTINE HEADING
		
		//                    COMPLEX FUNCTION PARFUN(K,T)
		//                    INTEGER K
		//                    COMPLEX T
		
		//                WITH THE REAL PARAMETER INTERVAL -1 < REAL(T) < +1
		//                BEING MAPPED TO THE PHYSICAL ARC.  A SIMILAR HEADING
		//                SHOULD BE GIVEN FOR THE DERIVATIVE DPARFN.  THE PRE-
		//                PROCESSING PROGRAM PARGEN IS AVAILABLE TO HELP WITH
		//                THE CREATION OF PARFUN AND DPARFN.
		
		
		// 5.     FURTHER COMMENTS
		//             A SUMMARY LISTING OF ACTIONS TAKEN IS AUTOMATICALLY
		//             WRITTEN ON THE STANDARD OUTPUT CHANNEL.
		
		// ......................................................................
		//     AUTHOR: DAVID HOUGH, ETH, ZUERICH
		//     LAST UPDATE: 15 JULY 1990
		// ......................................................................
		     
		//     LOCAL VARAIBLES
		
		//**** POINTERS FOR IGEOM,RGEOM,ISNPH,RSNPH
		
		int ACOEF,AICOF,BCFSN,BCOEF,BICOF,DGPOL,ERARC,H0VAL,HALEN,
		     HIVAL,JACIN,JATYP,LOSUB,MIDPT,PARNT,QUPTS,QUWTS,SOLUN,VTARG;
		
		//**** POINTERS FOR IWORK,RWORK,ZWORK,LWORK
		
		int A1COF,AQCOF,AXION,B1COF,BQCOF,COLPR,COLSC,CQCOF,DIAG,
		     HIOLD,HISUB,HITES,ICOPY,IPIVT,LCOPY,LNSEG,LOOLD,LOQSB,LOTES,NEWDG,
		     NEWHL,NEWQU,NQUAD,PNEWQ,QCOMW,QCOMX,RCOPY,RIGLL,SDIAG,TOLOU,WORK2,
		     WORKQ,WORK,WORKT,XENPT,XIDST,XIVAL,ZCOLL;
		
		//**** OTHER LOCAL VARIABLES
		
		int I,IMXER,INDEG,J,L,MDGPO,MNJXS,MNQUA,MNSUA,MQIN1,NCOLL,
		     NEFF,NEQNS,NJIND,NROWS,NTEST,
		     TNSUA,TNGQP,ORDSG;
		int SOLCO = 0;
		int QIERC[] = new int[7];
		int QIERR[] = new int[7];
		//int QIERC(0:6),QIERR(0:6)
		//DATA QIERC/7*0E+0/
		
		final double SFACT = 0.8;
		final double QFACT = 0.1;
		double AQTOL,CONST,GAQTL,GLGTL,GRQTL,GSUPE,GTGTE,LGTOL,ESTOL,
		     MCHEP,MCQER,MQERR,MXERM,PI,R1MACH,RCOND,RQTOL,SSUPE,
		     TGTER,TOLNR;
		
		double ZMXER[] = new double[2];
		//COMPLEX ZMXER
		
		final boolean INIBT = true;
		boolean ACCPT,GACPT,NUQTL,REFLN;
		
		String OFL;
		//CHARACTER OFL*6
	
		//EXTERNAL AXION1,ANGLE7,ASQUC7,BCFVTF,CPJAC3,CSCAL3,ICOQR1,IGNLVL,
		//         LINSEG,LNSY11,OPQUD1,OUPTGM,OUPTPH,R1MACH,RECON,RESCAL,RSLT80,
		//         RSLT71,RSLT72,RSLT83,RSLT84,SETIGL,SGECO,SGEDI,SGESL,TESMD9,
		//         TSJAC3,UPCOQ1,UPJAC1,WRHEAD,WRTAIL
		
		//C**** DEFINE SOME OUTPUT FORMATS
		
		//1     FORMAT(A45)
		//3     FORMAT(A45,2X,E9.2)
		
		//**** NAME AND OPEN THE MAIN LISTING FILE AND OUTPUT THE JOBNAME TO FILE
		//**** jbnm.
		
		//OPEN(OCH,FILE='jbnm')
		//WRITE(OCH,'(A4)') JBNM
		//CLOSE(OCH)
		//L=INDEX(JBNM,' ')-1
		//IF (L.EQ.-1) L=4
		//OFL=JBNM(1:L)//'pl'
		//OPEN(OCH,FILE=OFL)
		
		//**** OUTPUT CONFPACK HEADING
		
		WRHEAD(1,0,null);
		//WRHEAD(1,OCH)
		
		if (NQPTS < 1) {
		    IER[0]=3;
		    WRTAIL(1,0,IER[0],null);
		    return;
		} 
		
		//**** INITIALISE SOME VARIABLES
		
		if (ISYGP == 0 || ISYGP == -1) {
		    ORDSG=1;
		    REFLN=false;
		}
		else {
		    ORDSG=Math.abs(ISYGP);
		    REFLN=(ISYGP < -1);
		}
		
		if ((NARCS % ORDSG) != 0) {
		    IER[0]=55;
		    WRTAIL(1,0,IER[0],null);
		    return;
		}
		
		SOLCO=0;
		NEFF=0;
		MCHEP=EPS;
		TOLNR=Math.sqrt(MCHEP);
		NJIND=NARCS+1;
		TNGQP=NQPTS*NJIND;
		MDGPO=NQPTS-1;
		MNSUA=IBNDS[0];
		MNJXS=IBNDS[1];
		MQIN1=IBNDS[2];
		MNQUA=IBNDS[3];
		if (2*NARCS > MNSUA) {
		    IER[0]=1;
		    WRTAIL(1,0,IER[0],null);
		    return;
		}
		if (NARCS+1 > MNJXS) {
		    IER[0]=2;
		    WRTAIL(1,0,IER[0],null);
		    return;            
		}
		if (TSTNG != 1) {
		    TSTNG=0;
		}
		GSUPE=MAXER;
		GTGTE=GSUPE*SFACT;
		GAQTL=QFACT*GTGTE;
		if (GAQTL < 5*MCHEP) {
		    GAQTL=5*MCHEP;
		    GTGTE=GAQTL/QFACT;
		    GSUPE=GTGTE/SFACT;
		}
		GLGTL=Math.log(1+GTGTE);
		GRQTL=GAQTL;
		IGEOM[0]=NARCS;
		IGEOM[1]=NQPTS;
		IGEOM[3]=MNSUA;
		ISNPH[0]=NARCS;
		ISNPH[1]=NQPTS;
		ISNPH[4]=MNSUA;
		ISNPH[5]=MNEQN;
		RGEOM[0]=GSUPE;
		RGEOM[1]=GLGTL;
		
		//**** SET UP THE POINTERS TO ELEMENTS IN ARRAYS IGEOM AND RGEOM
		
		PARNT=5;
		HALEN=3;
		MIDPT=MNSUA+3;
		VTARG=2*MNSUA+3; 
		
		//**** SET UP THE POINTERS TO ELEMENTS IN ARRAYS ISNPH AND RSNPH
		
		DGPOL=7;
		JATYP=MNSUA+7;
		LOSUB=2*MNSUA+7;
		ACOEF=1;
		BCOEF=TNGQP+1;
		AICOF=2*TNGQP+1;
		BICOF=3*TNGQP+1;
		QUPTS=4*TNGQP+1;
		QUWTS=5*TNGQP+1;
		H0VAL=6*TNGQP+1;
		HIVAL=NJIND+6*TNGQP+1;
		JACIN=2*NJIND+6*TNGQP+1;
		ERARC=3*NJIND+6*TNGQP+1;
		BCFSN=MNSUA+3*NJIND+6*TNGQP+1;
		SOLUN=MNEQN+MNSUA+3*NJIND+6*TNGQP+1;
		
		//**** SET UP THE POINTERS TO ELEMENTS IN ARRAYS IWORK,RWORK,ZWORK AND 
		//**** LWORK
		
		IPIVT=1;
		LOQSB=MNEQN+1;
		NQUAD=MNJXS+MNEQN+1;
		HISUB=2*MNJXS+MNEQN+1;
		LOTES=MNSUA+2*MNJXS+MNEQN+1;
		HITES=2*MNSUA+2*MNJXS+MNEQN+1;
		AXION=3*MNSUA+2*MNJXS+MNEQN+1;
		NEWDG=4*MNSUA+2*MNJXS+MNEQN+1;
		ICOPY=5*MNSUA+2*MNJXS+MNEQN+1;
		LOOLD=6*MNSUA+2*MNJXS+MNEQN+1;
		HIOLD=7*MNSUA+2*MNJXS+MNEQN+1;
		WORK2=1;
		COLPR=MNEQN+1;
		A1COF=2*MNEQN+1;
		B1COF=MNJXS+2*MNEQN+1;
		TOLOU=2*MNJXS+2*MNEQN+1;
		XIDST=3*MNJXS+2*MNEQN+1;
		XENPT=5*MNJXS+2*MNEQN+1;
		QCOMX=MQIN1+5*MNJXS+2*MNEQN+1;
		QCOMW=MNQUA+MQIN1+5*MNJXS+2*MNEQN+1;
		RCOPY=2*MNQUA+MQIN1+5*MNJXS+2*MNEQN+1;
		NEWHL=MNSUA+2*MNQUA+MQIN1+5*MNJXS+2*MNEQN+1;
		AQCOF=2*MNSUA+2*MNQUA+MQIN1+5*MNJXS+2*MNEQN+1;
		BQCOF=TNGQP+2*MNSUA+2*MNQUA+MQIN1+5*MNJXS+2*MNEQN+1;
		CQCOF=2*TNGQP+2*MNSUA+2*MNQUA+MQIN1+5*MNJXS+2*MNEQN+1;
		COLSC=3*TNGQP+2*MNSUA+2*MNQUA+MQIN1+5*MNJXS+2*MNEQN+1;
		RIGLL=4*TNGQP+2*MNSUA+2*MNQUA+MQIN1+5*MNJXS+2*MNEQN+1;
		WORK=5*TNGQP+2*MNSUA+2*MNQUA+MQIN1+5*MNJXS+2*MNEQN+1;
		DIAG=2*NQPTS+5*TNGQP+2*MNSUA+2*MNQUA+MQIN1+5*MNJXS+2*MNEQN+1;
		SDIAG=3*NQPTS+5*TNGQP+2*MNSUA+2*MNQUA+MQIN1+5*MNJXS+2*MNEQN+1;
		WORKT=4*NQPTS+5*TNGQP+2*MNSUA+2*MNQUA+MQIN1+5*MNJXS+2*MNEQN+1;
		WORKQ=2*NQPTS*NQPTS+4*NQPTS+5*TNGQP+2*MNSUA+2*MNQUA+MQIN1+5*MNJXS+
		      2*MNEQN+1;
		ZCOLL=1;
		XIVAL=MNEQN+1;
		NEWQU=1;
		LCOPY=MNJXS+1;
		PNEWQ=MNSUA+MNJXS+1;
		LNSEG=2*MNSUA+MNJXS+1;
		
		//**** ASSIGN THE JACOBI INDECES FOR EACH ARC.
		
		//ANGLE7(RSNPH(JACIN),NARCS,INTER);
		double BE[] = new double[NARCS];
		ANGLE7(BE, NARCS, INTER);
		for (I = 0; I < NARCS; I++) {
			RSNPH[JACIN + I - 1] = BE[I];
		}
		RSNPH[JACIN+NJIND-2]=0.0;
		
		//**** SET SUB-TOLERANCES AND INDEG
		
		if (INCST && GSUPE <= 3.16E-2) {
		
		    //****   FOLLOW INCREMENTAL STRATEGY
		
		    SSUPE=0.1;
		    TGTER=SSUPE*SFACT;
		    AQTOL=TGTER*QFACT;
		    LGTOL=Math.log(1.0+TGTER);
		    RQTOL=AQTOL;
		    INDEG=Math.min(3,NQPTS-1);
		}
		else {
		
		    //****   SUB-TOLERANCES SAME AS GLOBAL TOLERANCES, INDEG DETERMINED
		    // ****   ACCORDING TO ACCURACY REQUESTED
		
		    SSUPE=GSUPE;
		    TGTER=GTGTE;
		    AQTOL=GAQTL;
		    LGTOL=GLGTL;
		    RQTOL=GRQTL;
		    INDEG=(int)Math.round((-Math.log10(TGTER)))+2;
		    INDEG=Math.min(INDEG,NQPTS-1);
		}
		
		//**** ASSIGN THE LOGICAL LINE SEGMENT TYPE TO EACH ARC.
		
		boolean linout[] = new boolean[NARCS];
		LINSEG(linout, NARCS);
		for (I = 0; I < NARCS; I++) {
			LWORK[LNSEG-1+I]= linout[I];
		}
		//LINSEG(LWORK(LNSEG),NARCS);
		
		//**** LIST THE INPUT ARGUMENTS AND ASSOCIATED QUANTITIES
		
		double beta[] = new double[NARCS];
		boolean linear[] = new boolean[NARCS];
		for (I = 0; I < NARCS; I++) {
			beta[I] = RSNPH[JACIN-1+I];
			linear[I] = LWORK[LNSEG-1+I];
		}
		RSLT80(JBNM,HEAD,GSUPE,MAXER,GAQTL,INTER,NARCS,ORDSG,NQPTS,
		     INCST,INDEG,RFARC,RFARG[0],CENTR,beta,linear,
		     TSTNG,OULVL,IBNDS,MNEQN,OCH);
		PI=Math.PI;
		RFARG[0]=RFARG[0]*PI;
		
		//**** SET UP THE GAUSS-JACOBI AND GAUSS-LEGENDRE QUADRATURE DATA AND 
		//**** STORE IN ARRAYS QUPTS AND QUWTS.  SET UP THREE TERM RECURRENCE
		//**** COEFFICIENTS AND STORE IN ACOEF, BCOEF.  DETERMINE ZEROTH
		//**** MOMENTS OF JACOBI DISTRIBUTIONS AND STORE IN H0VAL. 
		//**** ALSO SET UP THREE TERM RECURRENCE COEFFICIENTS AND ZEROTH MOMENTS
		//**** FOR THE INTEGRATED POLYNOMIALS, STORING RESULTS IN AICOF,BICOF
		//**** AND HIVAL.
		      
		double JAC[] = new double[NJIND];
		for (I = 0; I < NJIND; I++) {
			JAC[I] = RSNPH[JACIN+I-1];
		}
		double ACO[] = new double[NJIND*NQPTS];
		double BCO[] = new double[NJIND*NQPTS];
		double H0V[] = new double[NJIND];
		double AIC[] = new double[NJIND*NQPTS+1];
		double BIC[] = new double[NJIND*NQPTS+1];
		double HIV[] = new double[NJIND];
		double QUP[] = new double[NJIND*NQPTS];
		double QUW[] = new double[NJIND*NQPTS];
		double WOR[] = new double[NQPTS];
		OPQUD1(NJIND,NQPTS,JAC,ACO,BCO,
		     H0V,AIC,BIC,HIV,QUP,
		     QUW,WOR,IER);
		for (I = 0; I < NJIND*NQPTS; I++) {
			RSNPH[ACOEF+I-1] = ACO[I];
			RSNPH[BCOEF+I-1] = BCO[I];
		}
		for (I = 0; I < NJIND; I++) {
			RSNPH[H0VAL+I-1] = H0V[I];
		}
		for (I = 0; I < NJIND*NQPTS+1; I++) {
			RSNPH[AICOF+I-1] = AIC[I];
			RSNPH[BICOF+I-1] = BIC[I];
		}
		for (I = 0; I < NJIND; I++) {
			RSNPH[HIVAL+I-1] = HIV[I];
		}
		for (I = 0; I < NJIND*NQPTS; I++) {
			RSNPH[QUPTS+I-1] = QUP[I];
			RSNPH[QUWTS+I-1] = QUW[I];
		}
		for (I = 0; I < NQPTS; I++) {
			RWORK[WORK+I-1] = WOR[I];
		}
		if (IER[0] > 0) {
			WRTAIL(1,0,IER[0],null);
		    return;            
		}
		J=1-NQPTS;
		for (I=1; I <= NJIND; I++) {
		    J=J+NQPTS;
		    RWORK[A1COF+I-2]=RSNPH[ACOEF+J-2];
		    RWORK[B1COF+I-2]=RSNPH[BCOEF+J-2];
		} // for (I=1; I <= NJIND; I++)
		System.out.println("BASIC GAUSS QUADRATURE DATA DONE:");
		
		//**** SET UP THE COEFFICIENTS IN THE THREE TERM RECURRENCE FORMULAE
		//**** FOR THE PRINCIPAL SINGULAR INTEGRALS ASSOCIATED WITH THE VARIOUS
		//**** JACOBI WEIGHT FUNCTIONS AND THEIR ORTHONORMAL POLYNOMIALS; STORE
		//**** THESE COEFFICIENTS IN AQCOF, BQCOF AND CQCOF
		
/*		      CALL ASQUC7(RWORK(AQCOF),RWORK(BQCOF),RWORK(CQCOF),RSNPH(JACIN),
		     +NJIND,NQPTS)
		      WRITE(*,1) 'DATA FOR SINGULAR INTEGRALS DONE:'
		C
		C**** SET UP THE A PRIORI COLUMN SCALE FACTORS, STORED IN COLSC.
		C
		      CALL CSCAL3(RWORK(COLSC),NQPTS,NJIND,RSNPH(ACOEF),RSNPH(BCOEF),
		     +RSNPH(H0VAL),RSNPH(QUPTS),RSNPH(QUWTS),RSNPH(JACIN),RWORK(WORK),
		     +RWORK(WORKT),RWORK(WORKQ))
		C
		C**** SET UP THE ARRAY RIGLL OF REFERENCE IGNORE LEVELS.
		C
		      CALL IGNLVL(RWORK(RIGLL),RWORK(COLSC),RSNPH(ACOEF),RSNPH(BCOEF),
		     +RSNPH(H0VAL),RSNPH(JACIN),NJIND,NQPTS,IER)
		      IF (IER .GT. 0) THEN
		        GOTO 999
		      ENDIF
		C
		C**** SET UP THE ARRAY OF COLLOCATION POINTS PARAMETER VALUES, COLPR,
		C**** THE ARRAY OF COLLOCATION POINTS ZCOLL AND THE ARRAYS LOSUB AND 
		C**** HISUB NEEDED TO ACCESS COLPR AND ZCOLL CORRECTLY.  INITIALISE
		C**** DGPOL AND UPDATE LNSEG FOR ARC HALVING. 
		C
		      CALL CPJAC3(NARCS,NQPTS,INDEG,ISNPH(DGPOL),RSNPH(JACIN),
		     +RSNPH(ACOEF),RSNPH(BCOEF),RWORK(DIAG),RWORK(SDIAG),TNSUA,
		     +ISNPH(LOSUB),IWORK(HISUB),ISNPH(JATYP),IGEOM(PARNT),RGEOM(MIDPT),
		     +RGEOM(HALEN),RWORK(COLPR),ZWORK(ZCOLL),LWORK(LNSEG),IWORK(LOOLD),
		     +IWORK(HIOLD),EPS,IER,INIBT)
		      IF (IER .GT. 0) THEN
		        GOTO 999
		      ENDIF
		      NCOLL=IWORK(HISUB+TNSUA-1)
		      NEQNS=NCOLL+1
		      NROWS=NCOLL/ORDSG+1
		      IF (NEQNS .GT. MNEQN) THEN
		        IER=8
		        GOTO 999
		      ENDIF
		      WRITE(*,1) 'COLLOCATION POINT CHOICE DONE:'
		C
		C**** SET UP THE COMPOSITE GAUSSIAN QUADRATURE RULES, STORING ABSCISSAE
		C**** AND WEIGHTS IN QCOMX AND QCOMW.  SET UP ARRAYS NQUAD,LOQSB
		C**** NEEDED TO ACCESS THESE DATA.  RECORD MAXIMUM QUADRATURE ERRORS
		C**** FOR COLUMN SCALED INTEGRALS IN ARRAY TOLOU.
		C
		      CALL ICOQR1(NARCS,NJIND,NQPTS,MDGPO,MQIN1,AQTOL,RSNPH(QUPTS),
		     +RSNPH(QUWTS),RSNPH(JACIN),RGEOM(MIDPT),RGEOM(HALEN),RSNPH(ACOEF),
		     +RSNPH(BCOEF),RSNPH(H0VAL),RWORK(COLSC),IWORK(NQUAD),IWORK(LOQSB),
		     +RWORK(QCOMX),RWORK(QCOMW),MNQUA,RWORK(TOLOU),MCQER,RWORK(XENPT),
		     +ZWORK(XIVAL),RWORK(XIDST),IER)
		      NUQTL=.FALSE.
		      IF (IER .GT. 0) THEN
		        GOTO 999
		      ENDIF
		      WRITE(*,1) 'COMPOSITE GAUSSIAN RULES DONE:'
		C
		C**** SET UP LINEAR ALGEBRAIC SYSTEM.
		C
		23    CONTINUE
		      SOLCO=SOLCO+1
		      WRITE(*,24) '********SOLUTION',SOLCO,'********',NROWS,'EQUATIONS'
		24    FORMAT(/,T18,A,1X,I2,1X,A,/,T25,I3,1X,A)
		C
		      CALL LNSY11(MATRX,RSNPH(SOLUN),MNEQN,NCOLL,ORDSG,REFLN,NQPTS,
		     +TNSUA,ISNPH(JATYP),IGEOM(PARNT),ISNPH(DGPOL),ISNPH(LOSUB),
		     +IWORK(HISUB),IWORK(NQUAD),IWORK(LOQSB),TOLNR,RGEOM(MIDPT),
		     +RGEOM(HALEN),RSNPH(H0VAL),RWORK(COLSC),RSNPH(ACOEF),RSNPH(BCOEF),
		     +RWORK(COLPR),RWORK(QCOMX),RWORK(QCOMW),CENTR,ZWORK(ZCOLL),INTER,
		     +LWORK(LNSEG),RWORK(WORK),QIERR,MQERR,RSNPH(JACIN),RWORK(A1COF),
		     +RWORK(B1COF),AQTOL,RQTOL,RWORK(AQCOF),RWORK(BQCOF),RWORK(CQCOF),
		     +IWORK(LOOLD),IWORK(HIOLD))
		C
		      DO 25 I=0,6
		        QIERC(I)=QIERC(I)+QIERR(I)
		25    CONTINUE
		      WRITE(*,1) 'LINEAR SYSTEM SET UP DONE:'
		C
		C**** SOLVE LINEAR SYSTEM BY GAUSSIAN ELIMINATION USING LINPACK
		C
		      CALL SGECO(MATRX(1,1,2),MNEQN,NROWS,IWORK(IPIVT),RCOND,
		     +RWORK(WORK2))
		      IF (RCOND .EQ. 0E+0) THEN
		        IER=15
		        SOLCO=SOLCO-1
		        GOTO 999
		      ENDIF    
		      CALL SGESL(MATRX(1,1,2),MNEQN,NROWS,IWORK(IPIVT),RSNPH(SOLUN),0)
		      NEFF=NEFF+NROWS**3
		      WRITE(*,1) 'LINEAR SYSTEM SOLUTION DONE:'
		C
		C**** RECONSTITUTE FULL SOLUTION VECTOR
		C
		      IF (ORDSG.GT.1) THEN
		        CALL RECON(ORDSG,REFLN,NCOLL,TNSUA,ISNPH(LOSUB),IWORK(HISUB),
		     +  RSNPH(SOLUN))
		      ENDIF
		      CONST=RSNPH(SOLUN+NEQNS-1)
		C
		C**** SET UP THE ARRAY WORK2 OF ACTUAL COEFFICIENT IGNORE LEVELS
		C
		      CALL SETIGL(RWORK(WORK2),IWORK(HISUB),ISNPH(JATYP),ISNPH(LOSUB),
		     +NQPTS,RWORK(RIGLL),TNSUA)
		C
		C**** DETERMINE THE ACTIONS THAT HAVE TO BE TAKEN ON EACH ARC
		C
		      CALL AXION1(IWORK(AXION),IWORK(NEWDG),RSNPH(SOLUN),MDGPO,TNSUA,
		     +ISNPH(DGPOL),ISNPH(LOSUB),IWORK(HISUB),RWORK(RIGLL),LGTOL,ACCPT,
		     +RSNPH(JACIN),ISNPH(JATYP),NJIND,RWORK(NEWHL),ESTOL,IER)
		      ESTOL=ESTOL/SFACT
		      IF (IER.GT.0) THEN
		        GOTO 999
		      ENDIF
		      WRITE(*,1) 'DECISIONS FOR EACH ARC DONE:'
		      WRITE(*,3) 'EFFECTIVE STOPPING TOLERANCE:',ESTOL
		      IF (ACCPT .AND. ESTOL.LE.GSUPE) THEN
		        GACPT=.TRUE.
		      ELSE
		        GACPT=.FALSE.
		      ENDIF
		C
		      IF (GACPT) THEN
		C
		C****   OUTPUT RESULTS
		C
		        IF (OULVL .LT. 4) THEN
		          CALL RSLT72(QIERC,RCOND,CONST,NROWS,ISNPH(DGPOL),ISNPH(JATYP),
		     +                IGEOM(PARNT),TNSUA,INTER,MQERR,MCQER,IWORK(AXION),
		     +                IWORK(NEWDG),NJIND,IWORK(NQUAD),RWORK(TOLOU),
		     +                LGTOL,SOLCO,OCH)
		        ELSE 
		          CALL RSLT71(QIERC,RCOND,RSNPH(SOLUN),NEQNS,ISNPH(LOSUB),
		     +                IWORK(HISUB),RWORK(COLSC),NQPTS,ISNPH(JATYP),
		     +                IGEOM(PARNT),TNSUA,INTER,MQERR,MCQER,RWORK(WORK2),
		     +                IWORK(AXION),IWORK(NEWDG),NJIND,RSNPH(JACIN),
		     +                IWORK(NQUAD),RWORK(TOLOU),LGTOL,SOLCO,OCH)
		        ENDIF
		        WRITE(OCH,*) 'EFFECTIVE STOPPING TOLERANCE :',ESTOL
		        NEFF=NINT(REAL(NEFF)**3.3333333E-1)
		        WRITE(*,54) '****THE SOLUTION IS ACCEPTED****'
		        WRITE(*,55) 'EFFECTIVE SIZE OF ALL SYSTEMS:',NEFF
		        IF (INTER) THEN
		          WRITE(*,3) 'ZERO:',CONST
		        ELSE
		          WRITE(*,56) 'CAPACITY:',EXP(-CONST)
		        ENDIF
		54      FORMAT(/,T17,A)
		55      FORMAT(/,A45,I4)
		56      FORMAT(A45,E16.8)

		        WRITE(OCH,*)
		        WRITE(OCH,*) '****THE SOLUTION IS ACCEPTED****'
		        WRITE(OCH,*) 'EFFECTIVE SIZE OF ALL SYSTEMS : ',NEFF
		        WRITE(OCH,*) 
		      ELSE
		        IF (ACCPT .OR. ESTOL.LE.SSUPE) THEN
		C
		C****     SOLUTION AT INTERMEDIATE ACCURACY IS ACCEPTED; SET TOLERANCES
		C****     FOR GREATER ACCURACY AND RE-ASSESS UPDATING ACTIONS BEFORE 
		C****     CONTINUING
		C
		          SSUPE=1E-1*MIN(SSUPE,ESTOL)
		          TGTER=SFACT*SSUPE
		          IF (TGTER .LE. 2E+0*GTGTE) THEN
		            TGTER=GTGTE
		          ENDIF
		          AQTOL=TGTER*QFACT
		          NUQTL=.TRUE.
		          LGTOL=LOG(1E+0+TGTER)
		          RQTOL=AQTOL
		          I=NINT(-LOG10(TGTER))+2
		          INDEG=MIN(I,MDGPO)
		C
		C****     DETERMINE THE ACTIONS THAT HAVE TO BE TAKEN ON EACH ARC
		C
		          CALL AXION1(IWORK(AXION),IWORK(NEWDG),RSNPH(SOLUN),MDGPO,
		     +                TNSUA,ISNPH(DGPOL),ISNPH(LOSUB),IWORK(HISUB),
		     +                RWORK(RIGLL),LGTOL,ACCPT,RSNPH(JACIN),
		     +                ISNPH(JATYP),NJIND,RWORK(NEWHL),ESTOL,IER)
		          ESTOL=ESTOL/SFACT
		          IF (IER.GT.0) THEN
		            GOTO 999
		          ENDIF
		          WRITE(*,1) 'DECISIONS FOR EACH ARC RE-DONE:'
		        ENDIF
		C
		C****   OUTPUT RESULTS
		C
		        IF (OULVL .LT. 4) THEN
		          CALL RSLT72(QIERC,RCOND,CONST,NROWS,ISNPH(DGPOL),ISNPH(JATYP),
		     +                IGEOM(PARNT),TNSUA,INTER,MQERR,MCQER,IWORK(AXION),
		     +                IWORK(NEWDG),NJIND,IWORK(NQUAD),RWORK(TOLOU),
		     +                LGTOL,SOLCO,OCH)
		        ELSE 
		          CALL RSLT71(QIERC,RCOND,RSNPH(SOLUN),NEQNS,ISNPH(LOSUB),
		     +                IWORK(HISUB),RWORK(COLSC),NQPTS,ISNPH(JATYP),
		     +                IGEOM(PARNT),TNSUA,INTER,MQERR,MCQER,RWORK(WORK2),
		     +                IWORK(AXION),IWORK(NEWDG),NJIND,RSNPH(JACIN),
		     +                IWORK(NQUAD),RWORK(TOLOU),LGTOL,SOLCO,OCH)
		        ENDIF
		        WRITE(OCH,*) 'EFFECTIVE STOPPING TOLERANCE :',ESTOL
		        IF (RCOND .LT. 5E+0*MCHEP) THEN
		          IER=16
		          GOTO 999
		        ELSE IF (RCOND .LT. AQTOL) THEN
		          NUQTL=.TRUE.
		          AQTOL=1E-1*RCOND
		          IF (AQTOL .LT. 5E+0*MCHEP) AQTOL=5E+0*MCHEP
		        ENDIF
		C
		C****   IMPLEMENT UPDATING PROCEDURES.
		C****   FIRST UPDATE THE COLLOCATION PARAMETERS AND OTHER DATA
		C****   RELATING TO SUB-ARC DEFINITIONS.
		C
		        CALL UPJAC1(NQPTS,NJIND,INDEG,IWORK(AXION),ISNPH(DGPOL),
		     +  IWORK(NEWDG),RSNPH(ACOEF),RSNPH(BCOEF),RWORK(DIAG),
		     +  RWORK(SDIAG),TNSUA,MNSUA,ISNPH(LOSUB),IWORK(HISUB),
		     +  ISNPH(JATYP),IGEOM(PARNT),RGEOM(MIDPT),RGEOM(HALEN),
		     +  RWORK(COLPR),ZWORK(ZCOLL),LWORK(LNSEG),LWORK(PNEWQ),EPS,IER,
		     +  RWORK(WORK),RWORK(NEWHL),RWORK(RCOPY),IWORK(ICOPY),
		     +  LWORK(LCOPY),IWORK(LOOLD),IWORK(HIOLD))
		        IF (IER .GT. 0) THEN
		          GOTO 999
		        ENDIF
		        WRITE(*,1) 'ARC REFINEMENTS DONE:'
		        NCOLL=IWORK(HISUB+TNSUA-1)
		        NEQNS=NCOLL+1
		        NROWS=NCOLL/ORDSG+1
		        IF (NEQNS .GT. MNEQN) THEN
		          IER=18
		          GOTO 999
		        ENDIF
		C
		C****   NEXT UPDATE THE COMPOSITE QUADRATURE RULES
		C
		        CALL UPCOQ1(NARCS,NJIND,NQPTS,MDGPO,MQIN1,AQTOL,RSNPH(QUPTS),
		     +  RSNPH(QUWTS),RSNPH(JACIN),RGEOM(MIDPT),RGEOM(HALEN),
		     +  RSNPH(ACOEF),RSNPH(BCOEF),RSNPH(H0VAL),RWORK(COLSC),
		     +  IWORK(NQUAD),IWORK(LOQSB),RWORK(QCOMX),RWORK(QCOMW),
		     +  MNQUA,RWORK(TOLOU),MCQER,RWORK(XENPT),ZWORK(XIVAL),
		     +  RWORK(XIDST),TNSUA,LWORK(PNEWQ),LWORK(NEWQU),ISNPH(JATYP),
		     +  IGEOM(PARNT),NUQTL,IER)
		        IF (IER .GT. 0) THEN
		          GOTO 999
		        ENDIF
		        WRITE(*,1) 'QUADRATURE UPDATES DONE:'
		        GOTO 23
		      ENDIF 
		C
		      IF (OULVL.EQ.2 .OR. OULVL.EQ.3) THEN
		        CALL RSLT71(QIERC,RCOND,RSNPH(SOLUN),NEQNS,ISNPH(LOSUB),
		     +  IWORK(HISUB),RWORK(COLSC),NQPTS,ISNPH(JATYP),IGEOM(PARNT),TNSUA,
		     +  INTER,MQERR,MCQER,RWORK(WORK2),IWORK(AXION),IWORK(NEWDG),NJIND,
		     +  RSNPH(JACIN),IWORK(NQUAD),RWORK(TOLOU),LGTOL,SOLCO,OCH)
		      ENDIF
		C
		C**** ESTIMATE MAXIMUM ERROR IN MODULUS.
		C
		        WRITE(*,*)
		        WRITE(*,1) 'ERRORS IN MODULUS STARTED:'
		C
		      CALL TSJAC3(IWORK(LOTES),IWORK(HITES),RWORK(COLPR),ZWORK(ZCOLL),
		     +NQPTS,NTEST,ORDSG,TNSUA,TSTNG,ISNPH(DGPOL),ISNPH(JATYP),
		     +IGEOM(PARNT),RSNPH(AICOF),RSNPH(BICOF),RWORK(DIAG),RGEOM(HALEN),
		     +RSNPH(JACIN),RGEOM(MIDPT),RWORK(SDIAG),IER)
		      IF (IER .GT. 0) THEN
		        GOTO 999
		      ENDIF
		C
		      CALL TESMD9(RWORK(WORK2),MATRX(1,1,2),RSNPH(SOLUN),MNEQN,NCOLL,
		     +NTEST,NQPTS,TNSUA,ISNPH(JATYP),IGEOM(PARNT),ISNPH(DGPOL),
		     +ISNPH(LOSUB),IWORK(HISUB),IWORK(LOTES),IWORK(HITES),IWORK(NQUAD),
		     +IWORK(LOQSB),TOLNR,RGEOM(MIDPT),RGEOM(HALEN),RSNPH(H0VAL),
		     +RWORK(COLSC),RSNPH(ACOEF),RSNPH(BCOEF),RWORK(COLPR),RWORK(QCOMX),
		     +RWORK(QCOMW),CENTR,ZWORK(ZCOLL),INTER,LWORK(LNSEG),RWORK(WORK),
		     +QIERR,MQERR,RSNPH(JACIN),RWORK(A1COF),RWORK(B1COF),AQTOL,RQTOL,
		     +RWORK(AQCOF),RWORK(BQCOF),RWORK(CQCOF),MXERM,IMXER,ZMXER,
		     +RSNPH(ERARC),ORDSG,REFLN)
		C
		      WRITE(*,1) 'ERRORS IN MODULUS DONE:'
		      WRITE(*,3) 'MAXIMUM ERROR AT TEST POINTS:',MXERM
		      DO 60 I=0,6
		        QIERC(I)=QIERC(I)+QIERR(I)
		60    CONTINUE
		      IF (MOD(OULVL,2) .EQ. 1) THEN
		        CALL RSLT84(RWORK(WORK2),TNSUA,MXERM,ZMXER,IMXER,IWORK(LOTES),
		     +  IWORK(HITES),QIERC,IGEOM(PARNT),ORDSG,OCH)
		      ELSE
		        CALL RSLT83(RSNPH(ERARC),TNSUA,MXERM,ZMXER,IMXER,QIERC,
		     +  IGEOM(PARNT),ORDSG,OCH)
		      ENDIF
		C
		C**** RESCALE SOLUTIONS TO OBTAIN STANDARD JACOBI COEFFICIENTS
		      CALL RESCAL(NQPTS,TNSUA,ISNPH(LOSUB),IWORK(HISUB),ISNPH(JATYP),
		     +RSNPH(SOLUN),RWORK(COLSC))

		999   CONTINUE
		C
		      CALL WRTAIL(1,OCH,IER)
		      CLOSE(OCH)
		C
		      IF (SOLCO .GE. 1) THEN
		C
		C****   COMPUTE THE BOUNDARY CORRESPONDENCE COEFFICIENTS BCFSN AND THE
		C****   ARGUMENTS OF ALL SUBARC END POINTS ON THE UNIT DISC,
		C****   AS REQUIRED BY SUBSEQUENT PROCESSING ROUTINES.
		C
		        CALL BCFVTF(RSNPH(BCFSN),RGEOM(VTARG),ISNPH(DGPOL),ISNPH(JATYP),
		     +  ISNPH(LOSUB),IGEOM(PARNT),RFARC,TNSUA,RSNPH(H0VAL),RSNPH(JACIN),
		     +  RFARG,RSNPH(SOLUN))
		C
		C****   OUTPUT DATA REQUIRED FOR POST-PROCESSING.
		C
		        IGEOM(3)=TNSUA
		        ISNPH(3)=TNSUA
		        ISNPH(4)=NEQNS
		C
		        OFL=JBNM(1:L)//'gm'
		        OPEN(OCH,FILE=OFL)
		        CALL OUPTGM(IGEOM,RGEOM,CENTR,INTER,OCH)
		        CLOSE(OCH)
		C
		        OFL=JBNM(1:L)//'ph'
		        OPEN(OCH,FILE=OFL)
		        CALL OUPTPH(ISNPH,RSNPH,OCH)
		        CLOSE(OCH)
		      ENDIF
		  
		      CALL WRTAIL(1,0,IER)*/
		
    } // private void JAPHYC

    private void ANGLE7(double BE[], int NA, boolean IN) {
        // REAL BE(*)

        // **** BE=JACIN,NA=NARCS,IN=INTER

        // **** TO COMPUTE THE ARRAY OF JACOBI INDECES CORRESPONDING TO THE
        // **** CORNER ANGLES ON THE BOUNDARY

        // **** LOCAL VARIABLES
        int K,B0,B1,B2;
        double X,Y,ANG,PI,R1MACH,EPSA,XI,APP;
        double U[] = new double[2];
        double V[] = new double[2];
        double DIN[] = new double[2];
        double absu;
        double absv;
        double cr[] = new double[1];
        double ci[] = new double[1];
        //COMPLEX U,V,DPARFN
        //EXTERNAL DPARFN

        PI=Math.PI;
        EPSA=Math.sqrt(EPS);
        for (K=1; K <= NA; K++) {
        	DIN[0] = 1.0;
        	DIN[1] = 0.0;
            U=DPARFN(K,DIN);
            absu = zabs(U[0],U[1]);
            U[0] = -U[0]/absu;
            U[1] = -U[1]/absu;
            DIN[0] = -1.0;
        	DIN[1] = 0.0;
            if (K == NA) {
                V=DPARFN(1,DIN);
            }
            else {
                V=DPARFN(K+1,DIN);
            }
            absv = zabs(V[0],V[1]);
            V[0] = V[0]/absv;
            V[1] = V[1]/absv;
            zdiv(U[0],U[1],V[0],V[1],cr,ci);
            V[0]= cr[0];
            V[1] = ci[0];
            X = V[0];
            Y = V[1];
            ANG=Math.atan2(Y,X);
            if (ANG < 0) {
                ANG=ANG+2.0*PI;
            }
            ANG=ANG/PI;
            if (!IN) {
                ANG=2.0-ANG;
            }
            ANG=-1.0+1.0/ANG;

            // ****   TRY TO DETECT SIMPLE RATIONAL INDECES AND FORCE BEST REAL
            // ****   APPROXIMATIONS

            if (Math.abs(ANG) < EPSA) {
                ANG=0.0;
            }
            else {
                XI=Math.abs(ANG);
                B0=(int)(XI);
                XI=XI-(double)(B0);
                if (Math.abs(XI) < EPSA) {
                    APP=(double)(B0);
                }
                else {
                    XI=1.0/XI;
                    B1=(int)(XI);
                    XI=XI-(double)(B1);
                    if (Math.abs(XI) < EPSA) {
                        APP=(double)(1+B0*B1)/(double)(B1);
                    }
                    else {
                        XI=1.0/XI;
                        B2=(int)(XI);
                        APP=(double)(B0*(1+B1*B2)+B2)/(double)(1+B1*B2);   
                    } // else
                } // else
                if (ANG < 0.0) {
                	APP = -APP;
                }
                if (Math.abs(ANG-APP) < EPSA) {
                	ANG=APP;
                }
            } // else

            if (K == NA) {
                BE[0]=ANG;
            }
            else {
                BE[K]=ANG;
            }
  
        } // for (K=1; K <= NA; K++)

    } // private void ANGLE7
    
    private void RSLT80(String JBNM, String HEAD, double SUPER, double MAXER, double AQTOL, boolean INTER, int NARCS, int ORDSG,
        int NQPTS, boolean INCST, int INDEG, int RFARC, double RFARG,double CENTR[], double BETA[], boolean LINEAR[],
        int TSTNG, int OULVL, int IBNDS[], int MNEQN,int OCH) {
    	
    	//INTEGER IBNDS(*)
    	//REAL BETA(*)
    	//COMPLEX CENTR
    	//LOGICAL LINEAR(*)
    	//CHARACTER JBNM*4,HEAD*72
    	
    	//**** WRITE THE MAIN ARGUMENTS OF JAPHYC AND ASSOCIATED QUANTITIES ON  
    	//**** THE LISTING FILE.
    	
    	// LOCAL VARIABLES
    	
    	int I;
    	final String TXT1 = "REQUESTED ACCURACY UNREALISTIC";


    	Preferences.debug("HEAD\n",Preferences.DEBUG_ALGORITHM);
        Preferences.debug("JOB NAME : " + JBNM + "\n",Preferences.DEBUG_ALGORITHM);
    	
    	if (INTER) {
    	    Preferences.debug("INTERIOR DOMAIN WITH " + NARCS + " ARCS\n",Preferences.DEBUG_ALGORITHM);
    	}
    	else {
    	    Preferences.debug("EXTERIOR DOMAIN WITH " + NARCS + " ARCS\n",Preferences.DEBUG_ALGORITHM);
    	}
    	if (ORDSG > 1) {
    	    Preferences.debug("\n",Preferences.DEBUG_ALGORITHM);
    	    Preferences.debug("ORDER OF SYMMETRY GROUP IS : " + ORDSG + "\n",Preferences.DEBUG_ALGORITHM);
    	    Preferences.debug("NUMBER OF ARCS ON FBS IS   : " + (NARCS/ORDSG)  + "\n",Preferences.DEBUG_ALGORITHM);
    	}
    	
    	Preferences.debug("ACCURACY REQUESTED            : " + MAXER  + "\n",Preferences.DEBUG_ALGORITHM);
    	if (MAXER < SUPER) {
    	    Preferences.debug("WORKING ACCURACY              : " + SUPER + "  " + TXT1 + "\n",Preferences.DEBUG_ALGORITHM);
    	}
    	Preferences.debug("ABSOLUTE QUADRATURE TOLERENCE : " + AQTOL + "\n",Preferences.DEBUG_ALGORITHM);
    	
    	Preferences.debug("\n",Preferences.DEBUG_ALGORITHM);
    	Preferences.debug("MAXIMUM NUMBER OF SUBARCS           : " + IBNDS[0] + "\n",Preferences.DEBUG_ALGORITHM);
    	Preferences.debug("MAXIMUM NUMBER OF EQUATIONS         : " + MNEQN + "\n",Preferences.DEBUG_ALGORITHM);
    	Preferences.debug("MAXIMUM NUMBER OF QUADRATURE PANELS : " + (IBNDS[2]-1) + "\n",Preferences.DEBUG_ALGORITHM);
    	Preferences.debug("MAXIMUM TOTAL  OF QUADRATURE POINTS : " + IBNDS[3] + "\n",Preferences.DEBUG_ALGORITHM);
    	
    	Preferences.debug("\n",Preferences.DEBUG_ALGORITHM);
    	Preferences.debug("MINIMUM NUMBER OF QUADRATURE POINTS : " + NQPTS + "\n",Preferences.DEBUG_ALGORITHM);
    	Preferences.debug("MAXIMUM DEGREE OF POLYNOMIAL        : " + (NQPTS-1) + "\n",Preferences.DEBUG_ALGORITHM);
    	Preferences.debug("INITIAL DEGREE OF POLYNOMIAL        : " + INDEG + (NQPTS-1) + "\n",Preferences.DEBUG_ALGORITHM);
    	Preferences.debug("INCREMENTAL STRATEGY                : " + INCST + "\n",Preferences.DEBUG_ALGORITHM);
    	
    	Preferences.debug("\n",Preferences.DEBUG_ALGORITHM);
    	Preferences.debug("REFERENCE ARC         : " + RFARC + "\n",Preferences.DEBUG_ALGORITHM);
    	Preferences.debug("REFERENCE ARGUMENT/PI : " + RFARG + "\n",Preferences.DEBUG_ALGORITHM);
    	Preferences.debug("CENTRE POINT          : " + CENTR[0] + ", " + CENTR[1] + "\n",Preferences.DEBUG_ALGORITHM);
    	
    	Preferences.debug("\n",Preferences.DEBUG_ALGORITHM);
    	Preferences.debug("CORNER        ANGLE/PI          JACOBI INDEX      LINEAR\n",Preferences.DEBUG_ALGORITHM);
    	for (I = 1; I <= NARCS; I++) {
    		Preferences.debug("  " + I + "     " + (1.0/(1.0 + BETA[I-1])) + "    " + BETA[I-1] + "      " + LINEAR[I-1] + "\n",Preferences.DEBUG_ALGORITHM);
    	}
    	
    	
    	Preferences.debug("\n",Preferences.DEBUG_ALGORITHM);
    	Preferences.debug("TESTING LEVEL : " + TSTNG + "\n",Preferences.DEBUG_ALGORITHM);
    	Preferences.debug("OUTPUT  LEVEL : " + OULVL + "\n",Preferences.DEBUG_ALGORITHM);
    } // private void RSLT80
    
    private void OPQUD1(int NJIND, int NQPTS,double JACIN[], double ACOEF[], double BCOEF[],
        double H0VAL[], double AICOF[], double BICOF[], double HIVAL[], double QUPTS[], 
        double QUWTS[], double WORK[], int IER[]) {
    	//REAL JACIN(*),ACOEF(*),BCOEF(*),H0VAL(*),QUPTS(*),QUWTS(*),
    	//+WORK(*),AICOF(*),BICOF(*),HIVAL(*)
    	
    	//**** TO SET UP THE THREE TERM RECURRENCE COEFFICIENTS (ACOEF AND BCOEF)
    	//**** FOR THE ON JACOBI POLYNOMIALS (UP TO DEGREE NQPTS) ASSOCIATED WITH
    	//**** THE JACOBI INDECES GIVEN IN JACIN AND TO STORE THE ZEROTH MOMENTS
    	//**** OF THE JACOBI DISTRIBUTIONS IN H0VAL.  ALSO TO REPEAT THESE
    	//**** CALCULATIONS FOR THE INCREMENTED JACOBI INDECES ARISING IN THE
    	//**** EXPRESSIONS FOR THE BOUNDARY CORRESPONDENCE FUNCTION, STORING
    	//**** RESULTS IN AICOF, BICOF AND HIVAL.
    	
    	//**** ALSO TO SET UP THE NQPT POINT GAUSS JACOBI QUADRATURE RULES,
    	//**** STORING THE ABSCISSAE IN QUPTS AND THE WEIGHTS IN QUWTS.
    	
    	//**** IER=0 - NORMAL EXIT
    	//**** IER=4 - FAILURE TO CONVERGE IN EIGSYS; CAN'T SET UP BASIC
    	//****         QUADRATURE RULES 
    	
    	//     LOCAL VARIABLES
    	
    	int I,J,K,LOSUB,M;
    	int IFAIL[] = new int[1];
    	double BETA,BETA1,C,PROD,S,T;
    	//EXTERNAL ASONJ7,EIGSYS
    	
    	for (I=1; I <= NJIND; I++) {
    	    BETA=JACIN[I-1];
    	    BETA1=BETA+1.0;
    	    PROD=BETA*BETA;
    	
    	    // CALCULATE THE ZEROTH MOMENT FOR THIS BETA
    	
    	    H0VAL[I-1]=Math.pow(2.0,BETA1)/BETA1;
    	
    	    // START ON THE 3-TERM ORTHONORMAL RECURRENCE COEFFICIENTS ACOEF
    	    // AND BCOEF FOR THIS BETA
    	
    	    T=2.0+BETA;
    	    S=T*T;
    	    C=4.0*BETA1/S/(T+1.0);
    	    LOSUB=(I-1)*NQPTS+1;
    	    ACOEF[LOSUB-1]=Math.sqrt(C);
    	    BCOEF[LOSUB-1]=BETA/T;
    	
    	    for (K=2; K <= NQPTS; K++) {
    	        J=LOSUB+K-1;
    	        BCOEF[J-1]=PROD/T/(T+2.0);
    	        T=2.0*K+BETA;
    	        S=T*T;
    	        C=4.0*K*K*(BETA+K)*(BETA+K)/S/(S-1.0);
    	        ACOEF[J-1]=Math.sqrt(C);
    	    } // for (K=2; K <= NQPTS; K++)
    	
    	    // START ON THE QUADRATURE POINTS AND WEIGHTS FOR THIS BETA
    	
    	    for (K=1; K <= NQPTS; K++) {
    	        J=LOSUB+K-1;
    	        QUPTS[J-1]=BCOEF[J-1];
    	        QUWTS[J-1]=ACOEF[J-1];
    	        WORK[K-1]=0.0;
    	    } // for (K=1; K <= NQPTS; K++)
    	    WORK[0]=1.0;
    	
    	    // AT THIS POINT THE LOCAL SEGMENTS OF QUPTS AND QUWTS ARE THE 
    	    // DIAGONAL AND SUBDIAGONAL OF A SYMMETRIC TRIDIAGONAL MATRIX 
    	    // WHOSE EIGENVALUES ARE THE QUADRATURE POINTS AND WHOSE 
    	    // EIGENVECTORS GIVE THE QUADRATURE WEIGHTS.
    	
    	    double D[] = new double[NQPTS];
    	    double E[] = new double[NQPTS];
    	    for (M = 0; M < NQPTS; M++) {
    	    	D[M] = QUPTS[LOSUB+M-1];
    	        E[M] = QUWTS[LOSUB+M-1];
    	    }
    	    EIGSYS(NQPTS,EPS,D,E,WORK,IFAIL);
    	    for (M = 0; M < NQPTS; M++) {
    	    	QUPTS[LOSUB+M-1] = D[M];
    	        QUWTS[LOSUB+M-1] = E[M];
    	    }
    	    if (IFAIL[0] > 0) {
    	        IER[0]=4;
    	        return;
    	    }
    	
    	    for (K=1; K <= NQPTS; K++) {
    	        QUWTS[LOSUB+K-2]=WORK[K-1]*WORK[K-1]*H0VAL[I-1];
    	    } // for (K=1; K <= NQPTS; K++)
    	
    	    // SET UP THE THREE TERM RECURRENCE COEFFICIENTS AIVAL,BIVAL AND
    	    // THE ZEROTH MOMENT HIVAL FOR THE INTEGRATED POLYNOMIALS.
    	
    	    double A[] = new double[NQPTS];
    	    double B[] = new double[NQPTS];
    	    double H[] = new double[1];
    	    ASONJ7(1.0,BETA1,A,B,H,NQPTS);
    	    for (M = 0; M < NQPTS; M++) {
    	    	AICOF[LOSUB+M-1] = A[M];
    	    	BICOF[LOSUB+M-1] = B[M];
    	    }
    	    HIVAL[I-1] = H[0];
    	
    	} // for (I=1; I <= NJIND; I++)
    	
    	//     NORMAL TERMINATION
    	
    	IER[0]=0;
    	
    } // private void OPQUD1
    
    private void EIGSYS(int N, double EPS, double D[], double E[],
    		double Z[],int IER[]) { 
        // REAL D(N),E(N),Z(N)      
      
        // THIS IS A MODIFIED VERSION OF THE EISPACK ROUTINE IMTQL2.     
        // IT FINDS THE EIGENVALUES AND FIRST COMPONENTS OF THE
        // EIGENVECTORS OF A SYMMETRIC TRIDIAGONAL MATRIX BY THE IMPLICIT QL
        // METHOD.     

        // **** LOCAL VARIABLES

        int L,J,M ,MML,II,K,I;
        double P,G,R,S,C,F,B;  
  
        IER[0] = 0;    

        if (N == 1) {
        	return;
        }

        E[N-1] = 0.0;  
        loopL: for (L = 1; L <= N; L++) {       
            J = 0;    

            //****    LOOK FOR SMALL SUB-DIAGONAL ELEMENT     

            while (true) {
		        for (M = L; M <= N; M++) {    
		            if (M == N) break;   
		            if (Math.abs(E[M-1]) <= EPS * (Math.abs(D[M-1]) + Math.abs(D[M])))  break;
		        } // for (M = L; M <= N; M++) 
		
		       P = D[L-1]; 
		       if (M == L) continue loopL;      
		       if (J == 30) {
		    	   // **** SET ERROR -- NO CONVERGENCE TO AN EIGENVALUE AFTER 30 ITERATIONS
		    	   IER[0] = L;
		    	   return;
		       }
		       J = J + 1;
		
		       // ****    FORM SHIFT
		
		       G = (D[L] - P) / (2. * E[L-1]);
		       R = Math.sqrt(G*G+1.0);
		       double sig;
		       if (G >= 0) {
		    	   sig = Math.abs(R);
		       }
		       else {
		    	   sig = -Math.abs(R);
		       }
		       G = D[M-1] - P + E[L-1] / (G + sig); 
		       S = 1.0;  
		       C = 1.0 ;  
		       P = 0.0;  
		       MML = M - L;
		       
		       for (II = 1; II <= MML; II++) { 
		           I = M - II;      
		           F = S * E[I-1];    
		           B = C * E[I-1];    
		           if (Math.abs(F) >= Math.abs(G)) {   
		               C = G / F;       
		               R = Math.sqrt(C*C+1.0);
		               E[I] = F * R;  
		               S = 1.0 / R;     
		               C = C * S;       
		           } // if (Math.abs(F) >= Math.abs(G))
		           else {
		               S = F / G;       
		               R = Math.sqrt(S*S+1.0);
		               E[I] = G * R;  
		               C = 1.0 / R;      
		               S = S * C; 
		           } // else
		           G = D[I] - P;  
		           R = (D[I-1] - G) * S + 2. * C * B;     
		           P = S * R;       
		           D[I] = G + P;  
		           G = C * R - B;  
		 
		           //****       FORM FIRST COMPONENT OF VECTOR
		
		           F = Z[I];      
		           Z[I] = S * Z[I-1] + C * F; 
		           Z[I-1] = C * Z[I-1] - S * F;  
		       } // for (II = 1; II <= MML; II++)
		
		       D[L-1] = D[L-1] - P;    
		       E[L-1] = G; 
		       E[M-1] = 0.0;
            } // while (true)
        } // loopL: for (L = 1; L <= N; L++)   
       
        // **** ORDER EIGENVALUES AND EIGENVECTORS      

        for (II = 2; II <= N; II++) {      
            I = II - 1; 
            K = I;    
            P = D[I-1]; 
            for (J = II; J <= N; J++) {   
                if (D[J-1] >= P) continue;
                K = J; 
                P = D[J-1];
            } // for (J = II; J <= N; J++)
       
            if (K == I) continue;      
            D[K-1] = D[I-1];
            D[I-1] = P; 
            P = Z[I-1]; 
            Z[I-1] = Z[K-1];
            Z[K-1] = P; 
        } // for (II = 2; II <= N; II++) 
        return;

    } // private void EIGSYS

    private void ASONJ7(double ALFA,double BETA, double A[], double B[],
    		double H[], int N) {
        //REAL A(*),B(*),H,ALFA,BETA

        // ..TO ASSIGN THE COEFFICIENTS A(K) AND B(K) , K=1(1)N, IN THE
        // ..3-TERM RECURRENCE FORMULA FOR THE ORTHONORMAL JACOBI POLYNOMIALS
        // ..WHERE
        // ..
        // ..     A(K)P (X) = (X - B(K))P   (X) - A(K-1)P   (X) , K=1,2,..,N,
        // ..          K                 K-1             K-2
        // ..
        // ..         P  (X) = 0 , P (X) = 1/SQRT(H)
        // ..          -1           0 
        // .. 
        // ..AND H IS THE ZEROTH MOMENT OF THE JACOBI WEIGHT FUNCTION
        // ..(1-X)**ALFA*(1+X)**BETA ON [-1,1].

        // **** AUTHOR: DAVID HOUGH
        // **** LAST UPDATE: 15.09.89

        // **** ..LOCAL VARIABLES..
        double SUM,DIFF,PROD,TC,T,SC,S,C;
        int K;
        // EXTERNAL GAMMA
        // double GAMMA;

        SUM=ALFA+BETA;
        DIFF=BETA-ALFA;
        PROD=SUM*DIFF;

        // ..CALCULATE H.
        TC=SUM+1.0;
        SC=Math.pow(2.0,TC);
        S=GAMMA(ALFA+1.0);
        T=GAMMA(BETA+1.0);
        C=GAMMA(TC+1.0);
        H[0]=SC*S*T/C;

        // ..START ON A,B ARRAYS.
        if (N > 0) {
            T=2.0+SUM;
            S=T*T;
            C=4.0*(ALFA+1.0)*(BETA+1.0)/S/(T+1.0);
            A[0]=Math.sqrt(C);
            B[0]=DIFF/T;

            for (K=2; K<= N; K++) {
                B[K-1]=PROD/T/(T+2.0);
                T=2.0*K+SUM;
                S=T*T;
                C=4.0*K*(ALFA+K)*(BETA+K)*(SUM+K)/S/(S-1.0);
                A[K-1]=Math.sqrt(C);
            } // for (K=2; K<= N; K++) 
        } // if (N > 0)

    } // private void ASONJ7
    
    private double GAMMA( double U) {

        // TO COMPUTE THE GAMMA FUNCTION FOR REAL ARGUMENT U BY USING
        // THE CHEBYSHEV EXPANSION GIVEN IN TABLE 1.3 OF "MATHEMATICAL
        // FUNCTIONS AND THEIR APPROXIMATION" BY Y.L. LUKE ,ACADEMIC PRESS,
        // NEW YORK, 1975.
        // SINCE GAMMA HAS POLES AT U=0,-1,-2,-3,... DIVISION BY ZERO WILL
        // OCCUR FOR THESE ARGUMENT VALUES.

        // LOCAL VARIABLES

        int N;
        double UWORK,FACTOR;
        double B0 = 0.0;
        double X,B1,B2;
        double A[] = new double[]{3.65738772508338243850,
                              1.95754345666126826928,
                              0.33829711382616038916,0.4208951276557549199E-1,
                              0.42876504821290877E-2,0.36521216929461767E-3,
                              0.27400642226422E-4,0.181240233365124E-5,
                              0.10965775865997E-6,0.598718404552E-8,
                              0.30769080535E-9,0.143179303E-10,
                              0.65108773E-12,0.259585E-13,0.110789E-14,
                              0.3547E-16,0.169E-17,0.3E-19};  
        double result;

        UWORK=U;
        FACTOR=1.0;

	    while (true) {
	        if (UWORK > 4.0) {
	            UWORK=UWORK-1.0;
	            FACTOR=FACTOR*UWORK;
	            continue;
	        }
	        else if (UWORK < 3.0) {
	            FACTOR=FACTOR/UWORK;
	            UWORK=UWORK+1.0;
	            continue;
	        }
	        else {
	        	break;
	        }
	    } // while (true)
	
	    X=UWORK-3.0;      
	    X=4.0*X-2.0;
	    B2=0.0;
	    B1=0.0;
	    for (N=17; N >= 0; N--) {
	        B0=X*B1-B2+A[N];
	        if (N > 0) {
	            B2=B1;
	            B1=B0;
	        }
	    } // for (N=17; N >= 0; N--)
	
	    result=5E-1*(B0+A[0]-B2)*FACTOR;
	    return result;
    } // private double GAMMA


    	     /* SUBROUTINE RSLT71(QIERC,RCOND,SOLUN,NEQNS,LOSUB,HISUB,COLSC,
    	     +NQPTS,JATYP,PARNT,TNSUA,INTER,MQERR,MCQER,CINFN,ACTIN,
    	     +NEWDG,NJIND,JACIN,NQUAD,TOLOU,LGTOL,SOLCO,OUCH1)
    	      INTEGER NEQNS,TNSUA,OUCH1,NQPTS,NJIND,NEWDG(*),NQUAD(*),LOSUB(*),
    	     +HISUB(*),QIERC(0:6),JATYP(*),PARNT(*),ACTIN(*),SOLCO
    	      REAL SOLUN(*),RCOND,COLSC(*),MQERR,MCQER,LGTOL,
    	     +CINFN(*),JACIN(*),TOLOU(*)
    	      LOGICAL INTER
    	      CHARACTER QTEXT(0:6)*22,LINE*72
    	      PARAMETER (LINE='_________________________________________________
    	     +________________')
    	C 
    	C     LOCAL VARIABLES
    	C
    	      INTEGER I,J,JI,K,L,LOD,N,H
    	      REAL S,CAP
    	C
    	      QTEXT(0)='...........NORMAL EXIT'
    	      QTEXT(1)='.....MAX. SUBDIVISIONS'
    	      QTEXT(2)='....ROUNDOFF DETECTION'
    	      QTEXT(3)='.........BAD INTEGRAND'
    	      QTEXT(6)='.........INVALID INPUT'
    	C
    	      WRITE(OUCH1,*) LINE
    	      WRITE(OUCH1,*) '             SOLUTION NUMBER =',SOLCO
    	      WRITE(OUCH1,*) '                       NEQNS =',NEQNS 
    	      WRITE(OUCH1,*) 'RECIPROCAL COND NO. ESTIMATE =',RCOND
    	      WRITE(OUCH1,*) '   CONDITION NO. LOWER BOUND =',1E+0/RCOND
    	C
    	      WRITE(OUCH1,*) 
    	      WRITE(OUCH1,997) 'JACOBI INDEX','POINTS','TOLERANCE ACHIEVED'
    	      DO 10 I=1,NJIND
    	        WRITE(OUCH1,998) I,NQUAD(I),TOLOU(I)
    	10    CONTINUE
    	C
    	      WRITE(OUCH1,*) 
    	      WRITE(OUCH1,*) 'QAWS TERMINATIONS WITH......' 
    	      DO 20 I=0,6
    	        IF (QIERC(I) .GT. 0) THEN
    	          WRITE(OUCH1,1000) QTEXT(I),QIERC(I)
    	        ENDIF
    	20    CONTINUE
    	C
    	      WRITE(OUCH1,*) 
    	      WRITE(OUCH1,999) '              MAXIMUM QAWS ERROR =',MQERR
    	      WRITE(OUCH1,999) 'MAXIMUM COMPOSITE GAUSSIAN ERROR =',MCQER
    	      WRITE(OUCH1,*) 
    	      DO 40 I=1,TNSUA
    	          WRITE(OUCH1,*)
    	          WRITE(OUCH1,*) 'SUB ARC =',I,' ON PARENT ARC',PARNT(I)
    	          WRITE(OUCH1,990) 'N','SCALED SOLUN','UNSCALED SOLUN','IGNORE L
    	     +EVEL'
    	          L=LOSUB(I)
    	          H=HISUB(I)
    	          JI=ABS(JATYP(I))
    	          LOD=(JI-1)*NQPTS+1
    	          DO 30 J=L,H
    	              N=J-L
    	              K=LOD+N
    	              S=SOLUN(J)
    	              WRITE(OUCH1,991) N,S,S*COLSC(K),LGTOL/CINFN(J)
    	30        CONTINUE
    	          IF (ACTIN(I) .EQ. -1) THEN
    	              WRITE(OUCH1,*)'ACTION: REDUCE DEGREE TO ',NEWDG(I),' ***'
    	          ELSE IF (ACTIN(I) .EQ. 0) THEN
    	              WRITE(OUCH1,*)'ACTION: NONE            ***'
    	          ELSE IF (ACTIN(I) .EQ. 1) THEN
    	              WRITE(OUCH1,*)'ACTION: INCREASE DEGREE TO ',NEWDG(I)
    	          ELSE
    	              WRITE(OUCH1,*)'ACTION: SUBDIVIDE THIS ARC'
    	          ENDIF
    	40    CONTINUE
    	C
    	      WRITE(OUCH1,*) 'KAPPA =',SOLUN(NEQNS)
    	      IF (.NOT.INTER) THEN
    	          CAP=EXP(-SOLUN(NEQNS))
    	          WRITE(OUCH1,*) 'CAPACITY = ',CAP
    	      ENDIF
    	C
    	990   FORMAT(A,T7,A,T26,A,T44,A)
    	991   FORMAT(I3,T6,E15.8,T25,E15.8,T44,E10.3)
    	992   FORMAT(E15.8)
    	993   FORMAT(I3,T8,E15.8,'  (',E14.7,',',E14.7,')')
    	994   FORMAT(A,T8,A,T34,A)
    	995   FORMAT(A,T6,A,T23,A,T36,A)
    	996   FORMAT(I2,T6,E14.7,T23,F10.5,T36,E14.7)
    	997   FORMAT(A,T24,A,T40,A)
    	998   FORMAT(T5,I3,T26,I3,T45,E9.2)
    	999   FORMAT(A,E10.2)
    	1000  FORMAT(A,1X,I5)
    	C
    }*/

 
      /**
       * zabs computes the absolute value or magnitude of a double precision complex variable zr + j*zi.
       * 
       * @param zr double
       * @param zi double
       * 
       * @return double
       */
      public double zabs(final double zr, final double zi) {
          double u, v, q, s;
          u = Math.abs(zr);
          v = Math.abs(zi);
          s = u + v;

          // s * 1.0 makes an unnormalized underflow on CDC machines into a true
          // floating zero
          s = s * 1.0;

          if (s == 0.0) {
              return 0.0;
          } else if (u > v) {
              q = v / u;

              return (u * Math.sqrt(1.0 + (q * q)));
          } else {
              q = u / v;

              return (v * Math.sqrt(1.0 + (q * q)));
          }
      }
      
      /**
       * complex multiply c = a * b.
       * 
       * @param ar double
       * @param ai double
       * @param br double
       * @param bi double
       * @param cr double[]
       * @param ci double[]
       */
      public void zmlt(final double ar, final double ai, final double br, final double bi, final double[] cr,
              final double[] ci) {
          double ca, cb;

          ca = (ar * br) - (ai * bi);
          cb = (ar * bi) + (ai * br);
          cr[0] = ca;
          ci[0] = cb;

          return;
      }
      
      /**
       * complex divide c = a/b.
       * 
       * @param ar double
       * @param ai double
       * @param br double
       * @param bi double
       * @param cr double[]
       * @param ci double[]
       */
      public void zdiv(final double ar, final double ai, final double br, final double bi, final double[] cr,
              final double[] ci) {
          double bm, cc, cd, ca, cb;

          bm = 1.0 / zabs(br, bi);
          cc = br * bm;
          cd = bi * bm;
          ca = ( (ar * cc) + (ai * cd)) * bm;
          cb = ( (ai * cc) - (ar * cd)) * bm;
          cr[0] = ca;
          ci[0] = cb;

          return;
      }

}