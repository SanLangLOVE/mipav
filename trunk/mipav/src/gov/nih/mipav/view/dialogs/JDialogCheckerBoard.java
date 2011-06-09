package gov.nih.mipav.view.dialogs;


import gov.nih.mipav.view.*;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;





/**
 * Dialog to get the row and column numbers of checkerboard squares
 *
 * @see  ViewJComponentEditImage
 */
public class JDialogCheckerBoard extends JDialogBase implements ChangeListener {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Use serialVersionUID for interoperability. */
    private static final long serialVersionUID = 4180573157937289440L;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** DOCUMENT ME! */
    private JButton closeButton;

    /** DOCUMENT ME! */
    private ViewJComponentEditImage compImage;

    /** DOCUMENT ME! */
    //private JCheckBox doCheckbox;
    
    private JComboBox checkerboardTypesCB;
    
    private JLabel checkerboardTypesLabel;
    
    

    /** DOCUMENT ME! */
    private boolean doReg = false;

    /** DOCUMENT ME! */
    private JLabel labelColumnNumber, labelRowNumber, speedLabel;

    /** DOCUMENT ME! */
    private Hashtable<Integer,JLabel> labelTable, labelTable2, speedLabelTable;

    /** DOCUMENT ME! */
    private int maxColumn;

    /** DOCUMENT ME! */
    private int maxRow;

    /** DOCUMENT ME! */
    private ViewJComponentRegistration regImage;

    /** DOCUMENT ME! */
    private JSlider slider, slider2, speedSlider;
    
    private JButton animateButton;

    /** DOCUMENT ME! */
    private JTextField textRowNumber, textColumnNumber;
    

    
    public Thread animateThread;
    
    private int cc = 0;
    
    private int[] pixBufferB;
    
    private int[] cleanImageBufferB;
    
    private int rowNumber, columnNumber;
    
    private int ySep, xSep;
    
    private int yMod, xMod;
    
    private int[] maxExtents;
    
    private boolean isStopped = false;
    
    long animateTime = 40;
    
    private boolean checkerboardApplied = false;
    
    int bandSpacingCounter = 0;
    
    private boolean animating = false;
    
 

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Creates new dialog and sets up GUI components.
     *
     * @param  theParentFrame  Parent frame.
     * @param  compImg         Source image.
     */
    public JDialogCheckerBoard(Frame theParentFrame, ViewJComponentEditImage compImg) {
        super(theParentFrame, false);
        compImage = compImg;
        maxRow = Math.min(compImage.getImageA().getExtents()[1] / 4, 50);
        maxColumn = Math.min(compImage.getImageA().getExtents()[0] / 4, 50);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setup();
    }

    

	

	/**
     * Creates new dialog and sets up GUI components.
     *
     * @param  theParentFrame  Parent frame.
     * @param  regImg          Source image.
     */
    public JDialogCheckerBoard(Frame theParentFrame, ViewJComponentRegistration regImg) {
        super(theParentFrame, false);
        regImage = regImg;
        maxRow = Math.min(regImage.getImageA().getExtents()[1] / 4, 50);
        maxColumn = Math.min(regImage.getImageA().getExtents()[0] / 4, 50);
        doReg = true;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setup();
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Sets parameters in ViewJComponentEditImage when Apply is pressed. Closes dialog box in response to both Apply and
     * Cancel buttons.
     *
     * @param  event  Event that triggers function.
     */
    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent event) {
        

        String command = event.getActionCommand();
        Object source = event.getSource();

        if ((command.equals("OK")) || (command.equals("Close"))) {

            if ((command.equals("OK"))) {
                rowNumber = slider.getValue();
                columnNumber = slider2.getValue();
                setCheckerboardApplied(true);
            } else { // no checkerboarding
                rowNumber = -1;
                columnNumber = -1;
                setAnimating(false);
                setCheckerboardApplied(false);
            }

            if (doReg) {
                regImage.setCheckerboard(rowNumber, columnNumber);
                regImage.repaint();
            } else {

            	cc = 0;
            	
                compImage.setCheckerboard(rowNumber, columnNumber);
                compImage.setMakingCheckerboard(true);
                compImage.paintComponent(compImage.getGraphics());
                compImage.setMakingCheckerboard(false);

                if(compImage.isCheckerboarded()) {
                	if(rowNumber == 1 || columnNumber ==1) {
                		speedLabel.setEnabled(true);
                		speedSlider.setEnabled(true);
                		animateButton.setEnabled(true);
                	}else {
                		speedLabel.setEnabled(false);
                		speedSlider.setEnabled(false);
                		animateButton.setEnabled(false);
                	}
                }
            }
            
            

            if (command.equals("Close")) {

                if (doReg) {
                    regImage.checkerDialog = null;
                } else {
                    compImage.checkerDialog = null;
                }

                dispose();
            }
        } else if (command.equals("Cancel")) {

            if (doReg == true) {
                regImage.checkerDialog = null;
            } else {
                compImage.checkerDialog = null;
            }

            dispose();
        }else if (command.equals("animate")) {
        	if(animateButton.getText().equals("Start")) {

        		setAnimating(true);
        		animateButton.setText("Stop");
        		compImage.setCheckerboardAnimate(true);
        		OKButton.setEnabled(false);
        		closeButton.setEnabled(false);
        		slider.setEnabled(false);
        		slider2.setEnabled(false);
        		compImage.removeMouseListener(compImage);
        		compImage.removeMouseMotionListener(compImage);
        		checkerboardTypesLabel.setEnabled(false);
        		checkerboardTypesCB.setEnabled(false);
        		labelRowNumber.setEnabled(false);
        		labelColumnNumber.setEnabled(false);

        		animateThread = new Animate();
    	    	try {
    	    		animateThread.start();
    	    	}catch (Exception e) {
    				e.printStackTrace();
    				return;
    			}

        		
        	}else {
        		setAnimating(false);
        		animateButton.setText("Start");
        		compImage.setCheckerboardAnimate(false);
        		OKButton.setEnabled(true);
        		closeButton.setEnabled(true);
        		slider.setEnabled(true);
        		slider2.setEnabled(true);
        		compImage.addMouseListener(compImage);
        		compImage.addMouseMotionListener(compImage);
        		checkerboardTypesLabel.setEnabled(true);
        		checkerboardTypesCB.setEnabled(true);
        		labelRowNumber.setEnabled(true);
        		labelColumnNumber.setEnabled(true);
        	}
        	
        	
        	
        }
    }

    /**
     * Sets values based on knob along slider.
     *
     * @param  e  Event that triggered this function.
     */
    public void stateChanged(ChangeEvent e) {
        int rowNumber, columnNumber;
        Object source = e.getSource();
        int type = checkerboardTypesCB.getSelectedIndex();

        if (source == slider) {
            rowNumber = slider.getValue();
            
            
            if(type == 0) {
            	if(rowNumber == 1) {
            		slider.setValue(2);
                	slider.updateUI();
            	}
            	
            	
            }else if(type == 1) {
            	if(rowNumber == 1) {
            		slider.setValue(2);
                	slider.updateUI();
            	}
            }

            textRowNumber.setText(String.valueOf(rowNumber));
        }

        if (source == slider2) {
            columnNumber = slider2.getValue();
            
            if(type == 0) {
            	if(columnNumber == 1) {
            		slider2.setValue(2);
                	slider2.updateUI();
            	}
            	
            	
            }else if(type == 2) {
            	if(columnNumber == 1) {
            		slider2.setValue(2);
                	slider2.updateUI();
            	}
            }

            textColumnNumber.setText(String.valueOf(columnNumber));
        }
        
        
        
        
        if(source == speedSlider) {
        	int value = speedSlider.getValue();

        	if(value == 1) {

        		setAnimateTime(100);
        		
        	}else if(value == 2) {

        		setAnimateTime(90);
        		
        	}else if(value == 3) {
 
        		setAnimateTime(80);
        		
        	}else if(value == 4) {

        		setAnimateTime(70);
        		
        	}else if(value == 5) {

        		setAnimateTime(60);
        		
        	}else if(value == 6) {

        		setAnimateTime(50);
        		
        	}else if(value == 7) {

        		setAnimateTime(40);
        		
        	}else if(value == 8) {

        		setAnimateTime(30);
        		
        	}else if(value == 9) {

        		setAnimateTime(20);
        		
        	}else if(value == 10) {

        		setAnimateTime(10);
        		
        	}
        	
        	
        }
    }
    
    
    
    

    public synchronized long getAnimateTime() {
		return animateTime;
	}

	public synchronized void setAnimateTime(long animateTime) {
		this.animateTime = animateTime;
	}

	/**
     * Sets up the GUI components of the dialog.
     */
    private void setup() {
        setForeground(Color.black);

        setTitle("Checkerboard pattern");

        JPanel paramPanel = new JPanel();

        paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.Y_AXIS));
        paramPanel.setBorder(buildTitledBorder("Parameters"));

        JPanel cbPanel = new JPanel();
        
        checkerboardTypesLabel = new JLabel("Type ");
        checkerboardTypesCB = new JComboBox();
        checkerboardTypesCB.addItem("Checkerboard");
        checkerboardTypesCB.addItem("Horizontal");
        checkerboardTypesCB.addItem("Vertical");
        checkerboardTypesCB.setSelectedIndex(0);
        checkerboardTypesCB.addItemListener(this);
        cbPanel.add(checkerboardTypesLabel);
        cbPanel.add(checkerboardTypesCB);
        cbPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        paramPanel.add(cbPanel);

        JPanel rowPanel = new JPanel();

        labelRowNumber = new JLabel("Rows");
        labelRowNumber.setForeground(Color.black);
        labelRowNumber.setFont(serif12);
        labelRowNumber.setEnabled(true);
        rowPanel.add(labelRowNumber);

        rowPanel.add(Box.createHorizontalStrut(10));

        slider = new JSlider(1, maxRow, 2);
        slider.setFont(serif12);
        slider.setEnabled(true);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(true);
        slider.addChangeListener(this);
        slider.setVisible(true);
        labelTable = new Hashtable<Integer,JLabel>();
        labelTable.put(new Integer(1), createLabel("1"));
        labelTable.put(new Integer(maxRow), createLabel(String.valueOf(maxRow)));
        slider.setLabelTable(labelTable);
        slider.setPaintLabels(true);
        rowPanel.add(slider);

        textRowNumber = new JTextField(String.valueOf(2), 4);
        textRowNumber.setFont(serif12);
        textRowNumber.setEditable(false);
        textRowNumber.setForeground(Color.black);
        textRowNumber.addFocusListener(this);
        rowPanel.add(textRowNumber);

        rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        paramPanel.add(rowPanel);

        JPanel columnPanel = new JPanel();

        labelColumnNumber = new JLabel("Columns");
        labelColumnNumber.setForeground(Color.black);
        labelColumnNumber.setFont(serif12);
        labelColumnNumber.setEnabled(true);
        columnPanel.add(labelColumnNumber);

        slider2 = new JSlider(1, maxColumn, 2);
        slider2.setFont(serif12);
        slider2.setEnabled(true);
        slider2.setMinorTickSpacing(5);
        slider2.setPaintTicks(true);
        slider2.addChangeListener(this);
        slider2.setVisible(true);
        labelTable2 = new Hashtable<Integer,JLabel>();
        labelTable2.put(new Integer(1), createLabel("1"));
        labelTable2.put(new Integer(maxColumn), createLabel(String.valueOf(maxColumn)));
        slider2.setLabelTable(labelTable2);
        slider2.setPaintLabels(true);
        columnPanel.add(slider2);

        textColumnNumber = new JTextField(String.valueOf(2), 4);
        textColumnNumber.setFont(serif12);
        textColumnNumber.setEditable(false);
        textColumnNumber.setForeground(Color.black);
        textColumnNumber.addFocusListener(this);
        columnPanel.add(textColumnNumber);

        columnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        paramPanel.add(columnPanel);
        
        
        
        JPanel speedPanel = new JPanel();
        speedPanel.setLayout(new BoxLayout(speedPanel, BoxLayout.Y_AXIS));
        speedPanel.setBorder(buildTitledBorder("Animate"));
        
        JPanel panel = new JPanel();
        
        speedLabel = new JLabel("Speed");
        speedLabel.setForeground(Color.black);
        speedLabel.setFont(serif12);
        speedLabel.setEnabled(false);
        
        panel.add(speedLabel);
        
        speedSlider = new JSlider(1, 10, 7);
        speedSlider.setFont(serif12);
        speedSlider.setEnabled(false);
        speedSlider.setMinorTickSpacing(1);
        speedSlider.setPaintTicks(true);
        speedSlider.setSnapToTicks(true);
        speedSlider.addChangeListener(this);
        speedSlider.setVisible(true);
        speedLabelTable = new Hashtable<Integer,JLabel>();
        speedLabelTable.put(new Integer(1), createLabel("1"));
        speedLabelTable.put(new Integer(10), createLabel("10"));
        speedSlider.setLabelTable(speedLabelTable);
        speedSlider.setPaintLabels(true);
        
        panel.add(speedSlider);
        
        
        animateButton = new JButton("Start");
        animateButton.setActionCommand("animate");
        animateButton.addActionListener(this);
        animateButton.setEnabled(false);
        
        panel.add(animateButton);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        speedPanel.add(panel);

        getContentPane().add(paramPanel, BorderLayout.NORTH);
        getContentPane().add(speedPanel, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();

        buildOKButton();
        OKButton.setText("Apply");
        OKButton.setActionCommand("OK");
        buttonPanel.add(OKButton);

        closeButton = new JButton("Close");
        closeButton.setMinimumSize(MipavUtil.defaultButtonSize);
        closeButton.setPreferredSize(MipavUtil.defaultButtonSize);
        closeButton.setFont(serif12B);
        buttonPanel.add(closeButton);
        closeButton.addActionListener(this);
        closeButton.setActionCommand("Close");

        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setVisible(true);
    }
    
    /**
     * item state changed
     */
    public void itemStateChanged(ItemEvent event) {
        Object source = event.getSource();

        if(checkerboardTypesCB.getSelectedIndex() == 0) {
        	//checkerboard
        	if(slider.getValue() == 1) {
	        	slider.setValue(2);
	        	slider.updateUI();
        	}
        	slider.setEnabled(true);
        	
        	if(slider2.getValue() == 1) {
	        	slider2.setValue(2);
	        	slider2.updateUI();
        	}
        	slider2.setEnabled(true);
        	
        	speedLabel.setEnabled(false);
    		speedSlider.setEnabled(false);
    		animateButton.setEnabled(false);
    		labelColumnNumber.setEnabled(true);
    		labelRowNumber.setEnabled(true);
        	
        }else if(checkerboardTypesCB.getSelectedIndex() == 1) {
        	//horizontal
        	if(slider.getValue() == 1) {
        		slider.setValue(2);
        		slider.updateUI();
        	}
        	slider.setEnabled(true);
        	
        	slider2.setValue(1);
        	slider2.updateUI();
        	slider2.setEnabled(false);
        	
        	speedLabel.setEnabled(true);
    		speedSlider.setEnabled(true);
    		animateButton.setEnabled(false);
    		labelColumnNumber.setEnabled(false);
    		labelRowNumber.setEnabled(true);
        	
        	
        }else if(checkerboardTypesCB.getSelectedIndex() == 2) {
        	//vertical
        	slider.setValue(1);
        	slider.updateUI();
        	slider.setEnabled(false);
        	
        	if(slider2.getValue() == 1) {
        		slider2.setValue(2);
        		slider2.updateUI();
        	}
        	slider2.setEnabled(true);
        	
        	speedLabel.setEnabled(true);
    		speedSlider.setEnabled(true);
    		animateButton.setEnabled(false);
    		labelRowNumber.setEnabled(false);
    		labelColumnNumber.setEnabled(true);
        	
        	
        }
        
        
    }
    


	public synchronized int getCc() {
		return cc;
	}

	public synchronized void setCc(int cc) {
		this.cc = cc;
	}




	public synchronized int getBandSpacingCounter() {
		return bandSpacingCounter;
	}



	public synchronized void setBandSpacingCounter(int bandSpacingCounter) {
		this.bandSpacingCounter = bandSpacingCounter;
	}



	public synchronized boolean isCheckerboardApplied() {
		return checkerboardApplied;
	}



	public synchronized void setCheckerboardApplied(boolean checkerboardApplied) {
		this.checkerboardApplied = checkerboardApplied;
	}


	

	public synchronized boolean isThreadStopped() {
		return isStopped;
	}
	
	
	
	 public synchronized boolean isAnimating() {
		return animating;
	}





	public synchronized void setAnimating(boolean animating) {
		this.animating = animating;
	}





	/**
     * Cleans up the frame before closing.
     * 
     * @param event the window event that triggered this method
     */
    public void windowClosing(final WindowEvent event) {
    	if(animateThread != null && animateThread.isAlive()) {

    		setAnimating(false);
    		setCheckerboardApplied(false);
    		while(!isThreadStopped()) {
				//do nothing
			}
    		compImage.addMouseListener(compImage);
    		compImage.addMouseMotionListener(compImage);
    		
    		
    		rowNumber = -1;
            columnNumber = -1;
            cc = 0;
            compImage.setCheckerboardAnimate(false);
            compImage.setCheckerboard(rowNumber, columnNumber);
            compImage.setMakingCheckerboard(true);
            compImage.paintComponent(compImage.getGraphics());
            compImage.setMakingCheckerboard(false);
            
    		
    		dispose();
    	
    		
    	}
    }







    /**
     * 
     * @author pandyan
     *
     *
     * this animate thread will animate the checkerboard if its in horizontal or vertical mode
     */
	public class Animate extends Thread {
		
		
		
		
		
		public void run() {

			pixBufferB = compImage.getPixBufferB();
			cleanImageBufferB = compImage.getCleanImageBufferB();
		
			ySep = compImage.getySep();
			yMod = compImage.getyMod();
			
			xSep = compImage.getxSep();
			xMod = compImage.getxMod();

			maxExtents = compImage.getMaxExtents();
			isStopped = false;


    		while(isAnimating()) {
    			animateCheckerboard();
    			compImage.paintComponent(compImage.getGraphics());
    			cc = cc + 1;
    			
    			if(columnNumber == 1) {
    				if(yMod == 0) {
    	    			if(cc == ySep) {
    	    				cc = 0;
    	    			}
        			}else {

        				int lastIndex = compImage.getBandSpacing().length - 1;
        				if(cc == compImage.getBandSpacing()[lastIndex]) {
        					cc = 0;
        					compImage.loopBandSpacing();
        				}

        			}
    			}else if(rowNumber == 1) {
    				if(xMod == 0) {
    	    			if(cc == xSep) {
    	    				cc = 0;
    	    			}
        			}else {

        				int lastIndex = compImage.getBandSpacing().length - 1;
        				if(cc == compImage.getBandSpacing()[lastIndex]) {
        					cc = 0;
        					compImage.loopBandSpacing();
        				}

        			}
    			}
    			
    			
    			

    			try{
    				Thread.sleep(getAnimateTime());
    			}catch(InterruptedException e) {
    				break;
    			}
    		}
    		
    		isStopped = true;

    		
		}
		
		
		
		/**
		 * flip
		 * @param x
		 * @param y
		 * @param dim
		 */
		 private void flip(int x, int y, int dim) {

		    	if(pixBufferB[x + (y * dim)] == 0) {
		        	pixBufferB[x + (y * dim)] = cleanImageBufferB[x + (y * dim)];
		        }else {
		        	pixBufferB[x + (y * dim)] = 0;
		        }
		    }
		    
		    
		 
		 
		 
		    private void animateCheckerboard() {
		    	int xDim, yDim;
		    	xDim = maxExtents[0];
		        yDim = maxExtents[1];

		        int y =0;
		        int x = 0;

		        
		    	if(columnNumber == 1) {
		    		if(yMod == 0) {
			    		for (y = 0; y < yDim;) {
			                for (x = 0; x < xDim; x++) {	
			                	if(y <= cc) {
			                		if(y == cc) {
			                			flip(x,y,xDim);
			                		}
			                	}else {
			                		flip(x,y,xDim);
			                	}
			                } 
			                if(y < cc) {
		                		y++;
		                	}else {
		                		y = y + ySep;
		                	}
			    		}
		    		}else {
		    			setBandSpacingCounter(0);
		    			for (y = 0; y < yDim;) {
			                for (x = 0; x < xDim; x++) {	
			                	if(y <= cc) {
			                		if(y == cc) {
			                			flip(x,y,xDim);
			                		}
			                	}else {
			                		flip(x,y,xDim);
			                	}
			                }
			                if(y < cc) {
		                		y++;
		                	}else {
		                		y = y + compImage.getBandSpacing()[getBandSpacingCounter()];
		                		int counter = getBandSpacingCounter() + 1;
		                		setBandSpacingCounter(counter);
		                	} 
			    		}
		    		}	
		    	}else if(rowNumber == 1) {
		    		if(xMod == 0) {
			    		for (x = 0; x < xDim;) {
			                for (y = 0; y < yDim; y++) {	
			                	if(x <= cc) {
			                		if(x == cc) {
			                			flip(x,y,xDim);
			                		}
			                	}else {
			                		flip(x,y,xDim);
			                	}
			                } 
			                if(x < cc) {
		                		x++;
		                	}else {
		                		x = x + xSep;
		                	}
			    		}
		    		}else {
		    			setBandSpacingCounter(0);
		    			for (x = 0; x < xDim;) {
			                for (y = 0; y < yDim; y++) {	
			                	if(x <= cc) {
			                		if(x == cc) {
			                			flip(x,y,xDim);
			                		}
			                	}else {
			                		flip(x,y,xDim);
			                	}
			                }
			                if(x < cc) {
		                		x++;
		                	}else {
		                		x = x + compImage.getBandSpacing()[getBandSpacingCounter()];
		                		int counter = getBandSpacingCounter() + 1;
		                		setBandSpacingCounter(counter);
		                	} 
			    		}
		    		}
		    	}
		    	
		    }
    	
    }

}
