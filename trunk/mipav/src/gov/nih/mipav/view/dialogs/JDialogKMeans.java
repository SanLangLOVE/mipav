package gov.nih.mipav.view.dialogs;

import gov.nih.mipav.model.algorithms.AlgorithmKMeans;
import gov.nih.mipav.model.algorithms.AlgorithmBase;
import gov.nih.mipav.model.algorithms.AlgorithmInterface;
import gov.nih.mipav.model.file.FileIO;
import gov.nih.mipav.model.scripting.ParserException;
import gov.nih.mipav.model.scripting.parameters.ParameterFactory;
import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.model.structures.ModelStorageBase;
import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.ViewFileChooserBase;
import gov.nih.mipav.view.ViewImageFileFilter;
import gov.nih.mipav.view.ViewJFrameImage;
import gov.nih.mipav.view.ViewUserInterface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class JDialogKMeans extends JDialogScriptableBase implements AlgorithmInterface {
	
	/** source image. **/
    private ModelImage image;
    
    /** result image **/
    private ModelImage resultImage;
    
    /** handle to algorithm **/
    private AlgorithmKMeans alg;
      
    /** boolean isMultifile **/
    private boolean isMultifile;
    
    private int nDims;
    
    private int extents[];
    
    private String directoryPoints;
    
    private String fileNamePoints;
    
    private File filePoints;
    
    private BufferedReader br;
    
    private int nPoints;
    
    private int groupNum[];
    
    private int xPos[];
    
    private int yPos[];
    
    private int zPos[];
    
    private int tPos[];
    
    private JTextField textImage;
    
    private JButton buttonImage;
    
    private JTextField textPointsFile;
    
    private JButton buttonPointsFile;
    
    private JTextField textClusters;
    
    private int numberClusters;
	
	
	public JDialogKMeans() {
		super(ViewUserInterface.getReference().getMainFrame(), false);
		init();
	}
	
	
	
	/**
	 *  action performed
	 */
	public void actionPerformed(ActionEvent event) {
		int dimPt;
		float buffer[];
		boolean havePoints = false;
		int length;
		int i;
		int x, y, z, t;
		int xDim, yDim, zDim, tDim;
		int sliceSize;
		int volume;
		int index;
		int nval;
		String command = event.getActionCommand();
		 if (command.equals("OK")) {
			 if (havePoints) {
			     callAlgorithm();
			 }
	     } 
		 else if (command.equals("Cancel")) {
	    	 if (image != null) {
	    		 image.disposeLocal();
	    		 image = null;
	    	 }
	         dispose();
	     } else if (command.equals("Help")) {
	            MipavUtil.showHelp("");
	     } else if (command.equals("AddImageBrowse")) {
	    	 ViewFileChooserBase fileChooser = new ViewFileChooserBase(true, false);
	         JFileChooser chooser = fileChooser.getFileChooser();
	         if (ViewUserInterface.getReference().getDefaultDirectory() != null) {
                 chooser.setCurrentDirectory(new File(ViewUserInterface.getReference().getDefaultDirectory()));
             } else {
                 chooser.setCurrentDirectory(new File(System.getProperties().getProperty("user.dir")));
             }
	         chooser.addChoosableFileFilter(new ViewImageFileFilter(ViewImageFileFilter.TECH));
	         chooser.setDialogTitle("Choose image");
	         int returnValue = chooser.showOpenDialog(this);
	         if (returnValue == JFileChooser.APPROVE_OPTION) { 	
	         	FileIO fileIO = new FileIO();
	         	isMultifile = fileChooser.isMulti();
	         	image = fileIO.readImage(chooser.getSelectedFile().getName(),chooser.getCurrentDirectory() + File.separator, isMultifile, null);
         		if (image.isColorImage()) {
         		    MipavUtil.displayError("Image cannot be a color image");
         		    image.disposeLocal();
         		    image = null;
         		    return;
         		}
         		else if (image.isComplexImage()) {
         			MipavUtil.displayError("Image cannot be a complex image");
         		    image.disposeLocal();
         		    image = null;
         		    return;	
         		}
	         } 
	         nDims = image.getNDims();
	         extents = image.getExtents();
	         length = extents[0];
	         for (i = 1; i < nDims; i++) {
	        	 length = length * extents[i];
	         }
	         buffer = new float[length];
	         try {
	        	 image.exportData(0, length, buffer);
	         }
	         catch (IOException e) {
	        	 MipavUtil.displayError("IOException " + e + " on image.exportData(0, length, buffer)");
	        	 image.disposeLocal();
      		     image = null;
      		     return;	
	         }
	         nPoints = 0;
	         for (i = 0; i < length; i++) {
	        	 if (buffer[i] > 0) {
	        		 nPoints++;
	        	 }
	         }
	         if (nPoints == 0) {
	        	 MipavUtil.displayError("No set of point values found in " + image.getImageFileName());
	        	 image.disposeLocal();
	        	 image = null;
                 return;	 
	         }
             textImage.setText(image.getImageFileName());
	         groupNum = new int[nPoints];
             xPos = new int[nPoints];
             if (nDims >= 2) {
             	yPos = new int[nPoints];
             	if (nDims >= 3) {
             		zPos = new int[nPoints];
             		if (nDims >= 4) {
             			tPos = new int[nPoints];
             		}
             	}
             } // if (nDims >= 2)
             if (nDims >= 4) {
            	 tDim = extents[3];
             }
             else {
            	 tDim = 1;
             }
	         if (nDims >= 3) {
	        	 zDim = extents[2];
	         }
	         else {
	        	 zDim = 2;
	         }
	         if (nDims >= 2) {
	        	 yDim = extents[1];
	         }
	         else {
	        	 yDim = 1;
	         }
	         xDim = extents[0];
	         sliceSize = xDim * yDim;
	         volume = sliceSize * zDim;
	         nval = 0;
	         for (t = 0; t < tDim; t++) {
	        	 for (z = 0; z < zDim; z++) {
	        		 for (y = 0; y < yDim; y++) {
	        			 for (x = 0; x < xDim; x++) {
	        			     index = x + y*xDim + z*sliceSize + t*volume;
	        			     if (buffer[index] > 0) {
	        			         xPos[nval] = x;
	        			         if (nDims >= 2) {
	        			        	 yPos[nval] = y;
	        			        	 if (nDims >= 3) {
	        			        		 zPos[nval] = z;
	        			        		 if (nDims >= 4) {
	        			        			 tPos[nval] = t;
	        			        		 }
	        			        	 }
	        			         }
	        			         nval++;
	        			     } // if (buffer[index] > 0)
	        			 }
	        		 }
	        	 }
	         } // for (t = 0; t < tDim; t++)
	         buffer = null;
	         havePoints = true;
	     } else if (command.equals("PointFile")) {

	            try {
	                JFileChooser chooser = new JFileChooser();

	                if (ViewUserInterface.getReference().getDefaultDirectory() != null) {
	                    File file = new File(ViewUserInterface.getReference().getDefaultDirectory());

	                    if (file != null) {
	                        chooser.setCurrentDirectory(file);
	                    } else {
	                        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
	                    }
	                } else {
	                    chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
	                }

	                chooser.addChoosableFileFilter(new ViewImageFileFilter(ViewImageFileFilter.GEN));
	                chooser.addChoosableFileFilter(new ViewImageFileFilter(ViewImageFileFilter.TECH));
	                chooser.addChoosableFileFilter(new ViewImageFileFilter(ViewImageFileFilter.MICROSCOPY));
	                chooser.addChoosableFileFilter(new ViewImageFileFilter(ViewImageFileFilter.MISC));

	                chooser.setDialogTitle("Open file of point locations");
	                directoryPoints = String.valueOf(chooser.getCurrentDirectory()) + File.separator;

	                int returnValue = chooser.showOpenDialog(ViewUserInterface.getReference().getMainFrame());

	                if (returnValue == JFileChooser.APPROVE_OPTION) {
	                    fileNamePoints = chooser.getSelectedFile().getName();
	                    directoryPoints = String.valueOf(chooser.getCurrentDirectory()) + File.separator;
	                    ViewUserInterface.getReference().setDefaultDirectory(directoryPoints);
	                } else {
	                    fileNamePoints = null;

	                    return;
	                }

	                if (fileNamePoints != null) {
	                	filePoints = new File(directoryPoints + fileNamePoints);
	                    
	                    try {
	                        br = new BufferedReader(new InputStreamReader(new FileInputStream(filePoints)));
	                    }
	                    catch (FileNotFoundException e) {
	                        MipavUtil.displayError((directoryPoints + fileNamePoints) + " was not found");
	                        return;
	                    }
	                    
	                    // Read lines until first character is not blank and not #
	                    int ii = 0;
	                    String line = null;
	                    do {
	                        try {
	                            // Contains the contents of the line not including line termination characters
	                            line = br.readLine();  
	                        }
	                        catch(IOException e) {
	                            MipavUtil.displayError("IOException on br.readLine");
	                            br.close();
	                            return;
	                        }
	                        // have reached end of stream
	                        if (line == null) {
	                            MipavUtil.displayError("Have reached end of stream on br.readLine");
	                            br.close();
	                            return;
	                        }
	                        for (ii = 0; ((ii < line.length()) && (Character.isSpaceChar(line.charAt(ii)))); ii++);
	                    } while ((ii == line.length()) || (line.charAt(ii) == '#'));
	                    
	                    int start = 0;
	                    int end = 0;
	                    for (; ((start < line.length()) && (Character.isSpaceChar(line.charAt(start)))); start++);
	                    end = start;
	                    for (; ((end < line.length()) && (Character.isDigit(line.charAt(end)))); end++);
	                    if (start == end) {
	                        MipavUtil.displayError("No digit starts line which should contain number of dimensions");
	                        br.close();
	                        return;
	                    }
	                    nDims = Integer.valueOf(line.substring(start, end)).intValue();
	                    extents = new int[nDims];
	                    nval = 0;
	                   l1: while (true) {
	                    	try {
	                            // Contains the contents of the line not including line termination characters
	                            line = br.readLine();  
	                        }
	                        catch(IOException e) {
	                            MipavUtil.displayError("IOException on br.readLine");
	                            br.close();
	                            return;
	                        }
	                        // have reached end of stream
	                        if (line == null) {
	                            MipavUtil.displayError("Have reached end of stream on br.readLine");
	                            break;
	                        }
	                    	start = 0;
	                    	end = 0;
	                    	while (start < line.length()) {
		                    	for (; ((start < line.length()) && (Character.isSpaceChar(line.charAt(start)))); start++);
		                        end = start;
		                        for (; ((end < line.length()) && ((Character.isDigit(line.charAt(end))))); end++);
		                        if (start == end) {
		                            continue l1;
		                        }
		                        extents[nval++] = Integer.valueOf(line.substring(start, end)).intValue();
		                        if (nval ==  nDims) {
		                            break l1;
		                        }
		                        start = end;
	                    	} // while (start < line.length())
	                    } // while (true)
	                    if (nval < 1) {
	                        MipavUtil.displayError("No extent values found in " + fileNamePoints);
	                        return;
	                    }
	                    if (nval < nDims) {
	                    	MipavUtil.displayError("Only " + nval + " of " + nDims + " required dimensions found");
	                    	return;
	                    }
	                    start = 0;
	                    end = 0;
	                    for (; ((start < line.length()) && (Character.isSpaceChar(line.charAt(start)))); start++);
	                    end = start;
	                    for (; ((end < line.length()) && (Character.isDigit(line.charAt(end)))); end++);
	                    if (start == end) {
	                        MipavUtil.displayError("No digit starts line which should contain number of points");
	                        br.close();
	                        return;
	                    }
	                    nPoints = Integer.valueOf(line.substring(start, end)).intValue();
	                    groupNum = new int[nPoints];
	                    xPos = new int[nPoints];
	                    if (nDims >= 2) {
	                    	yPos = new int[nPoints];
	                    	if (nDims >= 3) {
	                    		zPos = new int[nPoints];
	                    		if (nDims >= 4) {
	                    			tPos = new int[nPoints];
	                    		}
	                    	}
	                    } // if (nDims >= 2)
	                    nval = 0;
	                    dimPt = 0;
	                    l2: while (true) {
	                    	try {
	                            // Contains the contents of the line not including line termination characters
	                            line = br.readLine();  
	                        }
	                        catch(IOException e) {
	                            MipavUtil.displayError("IOException on br.readLine");
	                            br.close();
	                            return;
	                        }
	                        // have reached end of stream
	                        if (line == null) {
	                            MipavUtil.displayError("Have reached end of stream on br.readLine");
	                            break;
	                        }
	                    	start = 0;
	                    	end = 0;
	                    	while (start < line.length()) {
		                    	for (; ((start < line.length()) && (Character.isSpaceChar(line.charAt(start)))); start++);
		                        end = start;
		                        for (; ((end < line.length()) && ((Character.isDigit(line.charAt(end))))); end++);
		                        if (start == end) {
		                            continue l2;
		                        }
		                        if (dimPt == 0) {
		                            xPos[nval] = Integer.valueOf(line.substring(start, end)).intValue();
		                        }
		                        else if (dimPt == 1) {
		                        	yPos[nval] = Integer.valueOf(line.substring(start, end)).intValue();	
		                        }
		                        else if (dimPt == 2) {
		                        	zPos[nval] = Integer.valueOf(line.substring(start, end)).intValue();
		                        }
		                        else if (dimPt == 3) {
		                        	tPos[nval] = Integer.valueOf(line.substring(start, end)).intValue();
		                        }
		                        if (dimPt == nDims-1) {
		                        	nval++;
		                        }
		                        if (dimPt < nDims-1) {
		                        	dimPt++;
		                        }
		                        else {
		                        	dimPt = 0;
		                        }
		                        if (nval ==  nPoints) {
		                            break l2;
		                        }
		                        start = end;
	                    	} // while (start < line.length())
	                    } // while (true)
	                    br.close();
	                    if (nval < 1) {
	                        MipavUtil.displayError("No set of point values found in " + fileNamePoints);
	                        return;
	                    }
	                    if (nval < nPoints) {
	                    	MipavUtil.displayError("Only " + nval + " of " + nPoints + " required points found");
	                    	return;
	                    }
                        havePoints = true;
	                	textPointsFile.setText(fileNamePoints);
	                }
	            } catch (OutOfMemoryError e) {
	                MipavUtil.displayError("Out of memory in JDialogKMeans.");

	                return;
	            } catch (IOException e) {
	            	MipavUtil.displayError("IOException on BufferedReader");
	            	return;
	            }

	    }
	     

	}
	
	/**
	 *  call algorithm
	 */
	protected void callAlgorithm() {

		if ((nDims >= 1) && (nDims <= 4)  && (image == null)) {
		    image = new ModelImage(ModelStorageBase.BYTE, extents, 
		    		                     makeImageName(fileNamePoints, "_kmeans"));
		    try {
                new ViewJFrameImage(image, null, new Dimension(610, 240));
            } catch (OutOfMemoryError error) {
                System.gc();
                MipavUtil.displayError("Out of memory: unable to open new frame");
            }
		}
		
		
		 try {
		
			 alg = new AlgorithmKMeans(image,groupNum,xPos,yPos,zPos,tPos,numberClusters);
			 
			 
			 //This is very important. Adding this object as a listener allows the algorithm to
             // notify this object when it has completed of failed. See algorithm performed event.
             // This is made possible by implementing AlgorithmedPerformed interface
             alg.addListener(this);


             // Hide dialog
             setVisible(false);

             if (isRunInSeparateThread()) {

                 // Start the thread as a low priority because we wish to still have user interface work fast.
                 if (alg.startMethod(Thread.MIN_PRIORITY) == false) {
                     MipavUtil.displayError("A thread is already running on this object");
                 }
             } else {
                 alg.run();
             }
         } catch (OutOfMemoryError x) {

             if (resultImage != null) {
                 resultImage.disposeLocal(); // Clean up memory of result image
                 resultImage = null;
             }

             System.gc();
             MipavUtil.displayError("Dialog KMeans: unable to allocate enough memory");

             return;
         }
		
		
		
	}
	
	
	/**
	 *  algorithm performed
	 */
	public void algorithmPerformed(AlgorithmBase algorithm) {
		if (algorithm instanceof AlgorithmKMeans) {

            if ((alg.isCompleted() == true) && (image != null)) {

                
               
            }
		}
		
		if (algorithm.isCompleted()) {
            insertScriptLine();
        }
		alg.finalize();
        alg = null;
		dispose();
	}
	
	
	
	/**
	 * init
     * Sets up the GUI (panels, buttons, etc) and displays it on the screen.
     */
    private void init() {

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridx = 0;
    	setForeground(Color.black);
        setTitle("K-Means Clustering");
        
        JLabel choiceLabel = new JLabel("Choose an image or a points file");
        choiceLabel.setForeground(Color.black);
        choiceLabel.setFont(serif12);
        mainPanel.add(choiceLabel, gbc);
        
        buttonImage = new JButton("Choose an image with points");
        buttonImage.setForeground(Color.black);
        buttonImage.setFont(serif12B);
        buttonImage.addActionListener(this);
        buttonImage.setActionCommand("AddImageBrowse");
        buttonImage.setPreferredSize(new Dimension(235, 30));
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 1;
        mainPanel.add(buttonImage, gbc);

        textImage = new JTextField();
        textImage.setFont(serif12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        mainPanel.add(textImage, gbc);
        
        buttonPointsFile = new JButton("Open a file of point locations");
        buttonPointsFile.setForeground(Color.black);
        buttonPointsFile.setFont(serif12B);
        buttonPointsFile.addActionListener(this);
        buttonPointsFile.setActionCommand("PointsFile");
        buttonPointsFile.setPreferredSize(new Dimension(225, 30));
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(buttonPointsFile, gbc);
        
        textPointsFile = new JTextField();
        textPointsFile.setFont(serif12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        mainPanel.add(textPointsFile, gbc);
        
        JLabel clustersLabel = new JLabel("Choose the number of clusters");
        clustersLabel.setForeground(Color.black);
        clustersLabel.setFont(serif12);
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 3;
        mainPanel.add(clustersLabel, gbc);
        
        textClusters = new JTextField(10);
        textClusters.setText("3");
        textClusters.setForeground(Color.black);
        textClusters.setFont(serif12);
        gbc.gridx = 1;
        mainPanel.add(textClusters, gbc);
    
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(buildButtons(), BorderLayout.SOUTH);

        pack();
        setVisible(true);
        
    }
    
    
    
    
	

	

	/**
	 * set GUI from params
	 */
	protected void setGUIFromParams(){
		
	}

	/**
	 * store params from gui
	 */
	protected void storeParamsFromGUI() throws ParserException {
		
	}
	
	 /**
     * get result image
     *
     * @return  The result image.
     */
    public ModelImage getResultImage() {
        return resultImage;
    }
    
    
	
	/**
     * item staate changed
     *
     * @param  event  DOCUMENT ME!
     */
    public void itemStateChanged(ItemEvent event) {
        
    }



    
    /**
     *  windoe closing
     */
    public void windowClosing(WindowEvent event) {
        if (image != null) {
        	image.disposeLocal();
        	image = null;
        }
        dispose();
    }

	


	
}
