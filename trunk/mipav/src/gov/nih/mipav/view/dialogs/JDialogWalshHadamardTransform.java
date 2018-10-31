package gov.nih.mipav.view.dialogs;


import gov.nih.mipav.model.algorithms.*;
import gov.nih.mipav.model.algorithms.filters.BiorthogonalWavelets;
import gov.nih.mipav.model.scripting.*;
import gov.nih.mipav.model.scripting.parameters.ParameterFactory;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;


/**
 * Dialog to get user input, then call the algorithm.
 */
public class JDialogWalshHadamardTransform extends JDialogScriptableBase implements AlgorithmInterface{

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Use serialVersionUID for interoperability. */
    //private static final long serialVersionUID;
		
	private ModelImage srcImage;
	
    private ModelImage transformImage;
    
    private ModelImage inverseImage;
    
    private WalshHadamardTransform whAlgo;
    
    private WalshHadamardTransform2 wh2Algo;
    
    private ButtonGroup algoGroup;
    private JRadioButton twoDButton;
    private JRadioButton oneDButton;
    private boolean twoD = false;
	
	/**
     * Empty constructor needed for dynamic instantiation.
     */
    public JDialogWalshHadamardTransform() { }

    /**
     * Construct the Walsh Hadamard Transform dialog.
     *
     * @param  theParentFrame  Parent frame.
     * @param  im              Source image.
     */
    public JDialogWalshHadamardTransform(Frame theParentFrame, ModelImage im) {
        super(theParentFrame, false);
        srcImage = im;
        init();
        setVisible(true);
    }

    //~ Methods --------------------------------------------------------------------------------------------------------
    
    /**
     * Closes dialog box when the OK button is pressed and calls the algorithm.
     *
     * @param  event  Event that triggers function.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();

        if (command.equals("OK")) {

            if (setVariables()) {
                callAlgorithm();
            }
        } else if (command.equals("Cancel")) {
            dispose();
        } else if (command.equals("Help")) {
            //MipavUtil.showHelp("");
        } else {
            super.actionPerformed(event);
        }
    }
    
    /**
     * Sets up the GUI (panels, buttons, etc) and displays it on the screen.
     */
    private void init() {
        setForeground(Color.black);

        setTitle("Walsh Hadamard Trqansform");
        getContentPane().setLayout(new BorderLayout());
        JPanel paramsPanel = new JPanel(new GridBagLayout());
        paramsPanel.setBorder(buildTitledBorder("Transform parameters"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;

        gbc.gridx = 0;
        gbc.gridy = 0;
        
        algoGroup = new ButtonGroup();
        oneDButton = new JRadioButton("Separate 1D row and column transforms", true);
        oneDButton.setFont(serif12);
        algoGroup.add(oneDButton);
        paramsPanel.add(oneDButton, gbc);

        
        twoDButton = new JRadioButton("Unified 2D transform", false);
        twoDButton.setFont(serif12);
        algoGroup.add(twoDButton);

        gbc.gridx = 0;
        gbc.gridy = 1;
        paramsPanel.add(twoDButton, gbc);
        
        getContentPane().add(paramsPanel, BorderLayout.CENTER);
        getContentPane().add(buildButtons(), BorderLayout.SOUTH);
        pack();
        setResizable(true);

        System.gc();
    }
    
    private boolean setVariables() {
    	
        twoD = twoDButton.isSelected();    
    	return true;
    }
	
	protected void callAlgorithm() {
		try {
			if (twoD) {
		        transformImage = new ModelImage(ModelStorageBase.DOUBLE, srcImage.getExtents(), srcImage.getImageName() + "_transform");
			}
			else {
				transformImage = new ModelImage(ModelStorageBase.INTEGER, srcImage.getExtents(), srcImage.getImageName() + "_transform");	
			}
		    inverseImage = new ModelImage(ModelStorageBase.INTEGER, srcImage.getExtents(), srcImage.getImageName() + "_inverse");
		    
		    if (twoD) {
		    	// Unified two dimensional transform
			    whAlgo = new WalshHadamardTransform(transformImage, inverseImage, srcImage);
			    
			    // This is very important. Adding this object as a listener allows the algorithm to
			    // notify this object when it has completed of failed. See algorithm performed event.
			    // This is made possible by implementing AlgorithmedPerformed interface
			    whAlgo.addListener(this);
			
			
			    createProgressBar(srcImage.getImageName(), whAlgo);
			
			    // Hide dialog
			    setVisible(false);
			
			    if (isRunInSeparateThread()) {
			
			        // Start the thread as a low priority because we wish to still have user interface work fast.
			        if (whAlgo.startMethod(Thread.MIN_PRIORITY) == false) {
			            MipavUtil.displayError("A thread is already running on this object");
			        }
			    } else {
			
			        whAlgo.run();
			    }
		    }
		    else {
		    	// Separate 1D row and column transforms
                wh2Algo = new WalshHadamardTransform2(transformImage, inverseImage, srcImage);
			    
			    // This is very important. Adding this object as a listener allows the algorithm to
			    // notify this object when it has completed of failed. See algorithm performed event.
			    // This is made possible by implementing AlgorithmedPerformed interface
			    wh2Algo.addListener(this);
			
			
			    createProgressBar(srcImage.getImageName(), wh2Algo);
			
			    // Hide dialog
			    setVisible(false);
			
			    if (isRunInSeparateThread()) {
			
			        // Start the thread as a low priority because we wish to still have user interface work fast.
			        if (wh2Algo.startMethod(Thread.MIN_PRIORITY) == false) {
			            MipavUtil.displayError("A thread is already running on this object");
			        }
			    } else {
			
			        wh2Algo.run();
			    }  
		    }
	    } catch (OutOfMemoryError x) {
	    	
	    	if (transformImage != null) {
	    		transformImage.disposeLocal();
	    		transformImage = null;
	    	}
	    	
	        if (inverseImage != null) {
	    		inverseImage.disposeLocal();
	    		inverseImage = null;
	    	}
	
	        System.gc();
	        MipavUtil.displayError("Dialog Walsh Hadamard Transform: unable to allocate enough memory");
	        return;
	    }

       
    }
    
 // ************************************************************************
    // ************************** Algorithm Events ****************************
    // ************************************************************************

    /**
     * This method is required if the AlgorithmPerformed interface is implemented. It is called by the algorithms when
     * it has completed or failed to to complete, so that the dialog can be display the result image and/or clean up.
     *
     * @param  algorithm  Algorithm that caused the event.
     */
    public void algorithmPerformed(AlgorithmBase algorithm) {

        if (algorithm instanceof WalshHadamardTransform) {
        	
        	if (whAlgo.isCompleted()) {
        		if (transformImage != null) {
        			updateFileInfo(srcImage, transformImage);
        			
        			try {
                        new ViewJFrameImage(transformImage, null, new Dimension(610, 200));
                    } catch (OutOfMemoryError error) {
                        System.gc();
                        JOptionPane.showMessageDialog(null, "Out of memory: unable to open new transformImage frame",
                                                      "Error", JOptionPane.ERROR_MESSAGE);
                    }
        		} // if (transformImage != null)
        		
        		if (inverseImage != null) {
        			updateFileInfo(srcImage, inverseImage);
        			
        			try {
                        new ViewJFrameImage(inverseImage, null, new Dimension(610, 220));
                    } catch (OutOfMemoryError error) {
                        System.gc();
                        JOptionPane.showMessageDialog(null, "Out of memory: unable to open new inverseImage frame",
                                                      "Error", JOptionPane.ERROR_MESSAGE);
                    }
        		} // if (inverseImage != null)
        	} // if (whAlgo.isCompleted())
        	else {
        		if (transformImage != null) {
    	    		transformImage.disposeLocal();
    	    		transformImage = null;
    	    	}
    	    	
    	    	if (inverseImage != null) {
    	    		inverseImage.disposeLocal();
    	    		inverseImage = null;
    	    	}	

                System.gc();
        	}
        }
        else if (algorithm instanceof WalshHadamardTransform2) {
        	
        	if (wh2Algo.isCompleted()) {
        		if (transformImage != null) {
        			updateFileInfo(srcImage, transformImage);
        			
        			try {
                        new ViewJFrameImage(transformImage, null, new Dimension(610, 200));
                    } catch (OutOfMemoryError error) {
                        System.gc();
                        JOptionPane.showMessageDialog(null, "Out of memory: unable to open new transformImage frame",
                                                      "Error", JOptionPane.ERROR_MESSAGE);
                    }
        		} // if (transformImage != null)
        		
        		if (inverseImage != null) {
        			updateFileInfo(srcImage, inverseImage);
        			
        			try {
                        new ViewJFrameImage(inverseImage, null, new Dimension(610, 220));
                    } catch (OutOfMemoryError error) {
                        System.gc();
                        JOptionPane.showMessageDialog(null, "Out of memory: unable to open new inverseImage frame",
                                                      "Error", JOptionPane.ERROR_MESSAGE);
                    }
        		} // if (inverseImage != null)
        	} // if (wh2Algo.isCompleted())
        	else {
        		if (transformImage != null) {
    	    		transformImage.disposeLocal();
    	    		transformImage = null;
    	    	}
    	    	
    	    	if (inverseImage != null) {
    	    		inverseImage.disposeLocal();
    	    		inverseImage = null;
    	    	}	

                System.gc();
        	}
        }

        if (algorithm.isCompleted()) {
            insertScriptLine();
        }

        dispose();
    }
	
	protected void setGUIFromParams() {
		srcImage = scriptParameters.retrieveInputImage();
		twoD = scriptParameters.getParams().getBoolean("two_D");
	}
	
	protected void storeParamsFromGUI() throws ParserException {
		scriptParameters.storeInputImage(srcImage);	
		scriptParameters.storeImageInRecorder(transformImage);
		scriptParameters.storeImageInRecorder(inverseImage);
		scriptParameters.getParams().put(ParameterFactory.newParameter("two_D", twoD));
	}
}
