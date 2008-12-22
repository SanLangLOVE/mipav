import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import gov.nih.mipav.model.algorithms.AlgorithmBase;
import gov.nih.mipav.model.algorithms.AlgorithmInterface;
import gov.nih.mipav.plugins.JDialogStandaloneScriptablePlugin;
import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.Preferences;
import gov.nih.mipav.view.dialogs.JDialogScriptableBase;
import javax.swing.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

/**
 * @author joshim2
 *
 */
public class PlugInDialogCreateXML extends JDialogStandaloneScriptablePlugin implements AlgorithmInterface {

	// ~ Static fields -------------------------------------------------------------------------
	
	public static final String BROWSE_FILE = "Browse file";

	public static final String REMOVE = "Remove file";
	
	public static final String REMOVE_ALL = "Remove all file";
	
	// ~ Instance fields ------------------------------------------------------------------------
	
	/** GridBagLayout * */
    private GridBagLayout mainPanelGridBagLayout;
    
    /** GridBagConstraints * */
    private GridBagConstraints mainPanelConstraints;
    
    /** panels * */
    private JPanel mainPanel, OKCancelPanel;
    
    /** Labels **/
    private JLabel inputFileLabel;
    
    /** Buttons **/
    private JButton inputFileBrowseButton;
    
    private JButton removeFileButton, removeAllFileButton;
    
    /**All files to generate image information for*/
    private JList inputFileList;

	/** Textfields **/
    private JTextField anatField; 
   	
	/** File chooser object */
	private JFileChooser fileChooser;
	
	/** Files selected by the user */
	private File[] selectedFiles;
	
	/** List of current files and folders to work with */
	private Vector<String> fileList;
	
	/** Algorithm instance */
    private PlugInAlgorithmCreateXML algoCreateXML;

	private JLabel anatLabel;

	private JLabel tagListSampleLabel;

	private JLabel sourceNameLabel;

	private JTextField sourceNameField;

	private JLabel sourceOrgLabel;

	private JTextField sourceOrgField;

	private JLabel sourceProjLabel;

	private JTextField sourceProjField;

	private JLabel testingLabel;

	private JTextArea testingArea;

	private JLabel detailLabel;

	private JTextArea detailArea;
	
	
	//	~ Constructors --------------------------------------------------------------------------

    /**
     * Empty constructor needed for dynamic instantiation (used during scripting).
     */
    public PlugInDialogCreateXML() { }
    
    /**
     * Sets up variables but does not show dialog.
     *
     * @param  theParentFrame  Parent frame.
     * @param  im              Source image.
     */
    public PlugInDialogCreateXML(boolean modal) {
        super(modal); 
        fileList = new Vector<String>();
        
    	init();
    }
    
    // ~ Methods ----------------------------------------------------------------------------------
    
    public void init() {
    	setForeground(Color.black);
        setTitle("Image XML Creation Tool");
        
        mainPanelGridBagLayout = new GridBagLayout();
        mainPanelConstraints = new GridBagConstraints();
        mainPanelConstraints.anchor = GridBagConstraints.NORTH;

        mainPanel = new JPanel(mainPanelGridBagLayout);
        
        // Input file
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 0;
        mainPanelConstraints.gridheight = 3;
        mainPanelConstraints.insets = new Insets(15, 5, 15, 0);
        inputFileLabel = new JLabel(" Input files : ");
        mainPanel.add(inputFileLabel, mainPanelConstraints);

        mainPanelConstraints.gridx = 1;
        mainPanelConstraints.gridy = 0;
        mainPanelConstraints.gridheight = 3;
        mainPanelConstraints.insets = new Insets(15, 5, 15, 0);
        mainPanelConstraints.fill = GridBagConstraints.BOTH;
        inputFileList = new JList(fileList);
        inputFileList.setVisibleRowCount(4);
        inputFileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        inputFileList.setMinimumSize(new Dimension(300, 94));
        inputFileList.setMaximumSize(new Dimension(300, 500));
        JScrollPane scrollPaneFile = new JScrollPane(inputFileList);
        scrollPaneFile.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPaneFile.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);    
        scrollPaneFile.setMinimumSize(new Dimension(300, 94));
        scrollPaneFile.setPreferredSize(new Dimension(300, 94));
        mainPanel.add(scrollPaneFile, mainPanelConstraints);

        mainPanelConstraints.gridx = 2;
        mainPanelConstraints.gridy = 0;
        mainPanelConstraints.gridheight = 1;
        mainPanelConstraints.insets = new Insets(15, 5, 5, 5);
        mainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        inputFileBrowseButton = new JButton("Browse");
        inputFileBrowseButton.addActionListener(this);
        inputFileBrowseButton.setActionCommand(BROWSE_FILE);
        mainPanel.add(inputFileBrowseButton, mainPanelConstraints);
        
        mainPanelConstraints.gridx = 2;
        mainPanelConstraints.gridy = 1;
        mainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanelConstraints.insets = new Insets(5, 5, 5, 5);
        removeFileButton = new JButton("Remove");
        removeFileButton.addActionListener(this);
        removeFileButton.setActionCommand(REMOVE);
        mainPanel.add(removeFileButton, mainPanelConstraints);
        
        mainPanelConstraints.gridx = 2;
        mainPanelConstraints.gridy = 2;
        mainPanelConstraints.fill = GridBagConstraints.NONE;
        mainPanelConstraints.insets = new Insets(5, 5, 15, 5);
        removeAllFileButton = new JButton("Remove All");
        removeAllFileButton.addActionListener(this);
        removeAllFileButton.setActionCommand(REMOVE_ALL);
        mainPanel.add(removeAllFileButton, mainPanelConstraints);
        
        // Anatomical area
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 3;
        mainPanelConstraints.insets = new Insets(15, 5, 15, 5);
        anatLabel = new JLabel(" Anatomical Area : ");
        mainPanel.add(anatLabel, mainPanelConstraints);
        
        mainPanelConstraints.gridx = 1;
        mainPanelConstraints.gridy = 3;
        mainPanelConstraints.insets = new Insets(15, 5, 0, 0);
        anatField = new JTextField(45);
        mainPanel.add(anatField, mainPanelConstraints);
        
        //source name
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 4;
        mainPanelConstraints.insets = new Insets(15, 5, 15, 5);
        sourceNameLabel = new JLabel(" Source name:  ");
        mainPanel.add(sourceNameLabel, mainPanelConstraints);
        
        mainPanelConstraints.gridx = 1;
        mainPanelConstraints.gridy = 4;
        mainPanelConstraints.insets = new Insets(15, 5, 0, 0);
        sourceNameField = new JTextField(45);
        mainPanel.add(sourceNameField, mainPanelConstraints);
        
        //source organization
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 5;
        mainPanelConstraints.insets = new Insets(15, 5, 15, 5);
        sourceOrgLabel = new JLabel(" Source organization:  ");
        mainPanel.add(sourceOrgLabel, mainPanelConstraints);
        
        mainPanelConstraints.gridx = 1;
        mainPanelConstraints.gridy = 5;
        mainPanelConstraints.insets = new Insets(15, 5, 0, 0);
        sourceOrgField = new JTextField(45);
        mainPanel.add(sourceOrgField, mainPanelConstraints);
        
        //source project
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 6;
        mainPanelConstraints.insets = new Insets(15, 5, 15, 5);
        sourceProjLabel = new JLabel(" Source project:  ");
        mainPanel.add(sourceProjLabel, mainPanelConstraints);
        
        mainPanelConstraints.gridx = 1;
        mainPanelConstraints.gridy = 6;
        mainPanelConstraints.insets = new Insets(15, 5, 0, 0);
        sourceProjField = new JTextField(45);
        mainPanel.add(sourceProjField, mainPanelConstraints);
        
        //Testing information
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 7;
        mainPanelConstraints.insets = new Insets(15, 5, 15, 5);
        testingLabel = new JLabel(" Testing information:  ");
        mainPanel.add(testingLabel, mainPanelConstraints);
        
        mainPanelConstraints.gridx = 1;
        mainPanelConstraints.gridy = 7;
        mainPanelConstraints.insets = new Insets(15, 5, 0, 0);
        testingArea = new JTextArea();
        testingArea.setMinimumSize(new Dimension(500, 94));
        testingArea.setMaximumSize(new Dimension(500, 500));
        JScrollPane scrollPaneTesting = new JScrollPane(testingArea);
        scrollPaneTesting.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPaneTesting.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);    
        scrollPaneTesting.setMinimumSize(new Dimension(500, 94));
        scrollPaneTesting.setPreferredSize(new Dimension(500, 94));
        mainPanel.add(scrollPaneTesting, mainPanelConstraints);
        
        //Details
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 8;
        mainPanelConstraints.insets = new Insets(15, 5, 15, 5);
        detailLabel = new JLabel(" Details:  ");
        mainPanel.add(detailLabel, mainPanelConstraints);
        
        mainPanelConstraints.gridx = 1;
        mainPanelConstraints.gridy = 8;
        mainPanelConstraints.insets = new Insets(15, 5, 0, 0);
        detailArea = new JTextArea();
        detailArea.setMinimumSize(new Dimension(500, 94));
        detailArea.setMaximumSize(new Dimension(500, 500));
        JScrollPane scrollPaneDetail = new JScrollPane(detailArea);
        scrollPaneDetail.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPaneDetail.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);    
        scrollPaneDetail.setMinimumSize(new Dimension(500, 94));
        scrollPaneDetail.setPreferredSize(new Dimension(500, 94));
        mainPanel.add(scrollPaneDetail, mainPanelConstraints);
        
        // OK,Cancel 
        OKCancelPanel = new JPanel();
        buildOKButton();
        OKButton.setActionCommand("ok");
        OKCancelPanel.add(OKButton, BorderLayout.WEST);
        buildCancelButton();
        cancelButton.setActionCommand("cancel");
        OKCancelPanel.add(cancelButton, BorderLayout.EAST);

        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(OKCancelPanel, BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setVisible(true);
        requestFocus();
        
    }
    
    /**
     * Once all the necessary variables are set, call the DICOM anonymizer algorithm.
     */
    
    protected void callAlgorithm() {
    
    	try{
    		System.gc();
    		selectedFiles = new File[fileList.size()];
    		for(int i=0; i<fileList.size(); i++) {
    			System.out.println("Working with file "+fileList.get(i));
    			selectedFiles[i] = new File(fileList.get(i));
    		}
    		
    		//map keys exact xml tag to particular text
    		HashMap<String, String> map = new HashMap();
    		map.put("anatomical-area", anatField.getText());
    		map.put("source-name", sourceNameField.getText());
    		map.put("source-org", sourceOrgField.getText());
    		map.put("source-project", sourceProjField.getText());
    		map.put("testing-information", testingArea.getText());
    		map.put("details", detailArea.getText());
    		
    		//Make algorithm.
    		algoCreateXML = new PlugInAlgorithmCreateXML(selectedFiles, map);
    		
    		// This is very important. Adding this object as a listener allows the algorithm to
            // notify this object when it has completed of failed. See algorithm performed event.
            // This is made possible by implementing AlgorithmedPerformed interface
            algoCreateXML.addListener(this);
            
            createProgressBar("DICOM Anonymization Tool", algoCreateXML);
            
            setVisible(false);
            
            if (isRunInSeparateThread()) {

                // Start the thread as a low priority because we wish to still have user interface work fast.
                if (algoCreateXML.startMethod(Thread.MIN_PRIORITY) == false) {
                    MipavUtil.displayError("A thread is already running on this object");
                }
            } else {
                algoCreateXML.run();
            }
    		
    	} catch (OutOfMemoryError x) {
            System.gc();
            MipavUtil.displayError("Dialog Plugin Anonymize DICOM: unable to allocate enough memory");

            return;
    	}
    
    }
    
    /**
     * Closes dialog box when the OK button is pressed and calls the algorithm.
     *
     * @param  event  Event that triggers function.
     */
    public void actionPerformed(ActionEvent event) {
    	
    	String command = event.getActionCommand();
    	if (command.equalsIgnoreCase(BROWSE_FILE)) {

    		fileChooser = new JFileChooser(Preferences.getImageDirectory());
        	fileChooser.setFont(MipavUtil.defaultMenuFont);
        	fileChooser.setMultiSelectionEnabled(true);
        	fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        	
        	Dimension d = new Dimension(700, 400);
            fileChooser.setMinimumSize(d);
            fileChooser.setPreferredSize(d);
            
            int returnVal = fileChooser.showOpenDialog(null);
                        
            if (returnVal == JFileChooser.APPROVE_OPTION) {
            	selectedFiles = fileChooser.getSelectedFiles();
            	Preferences.setImageDirectory(fileChooser.getCurrentDirectory());
            	for(int i=0; i<selectedFiles.length; i++) {
            		boolean fileExists = false;
            		for(int j=0; j<fileList.size(); j++) {
            			if(selectedFiles[i].getAbsolutePath().equals(fileList.get(j))) {
            				fileExists = true;
            			}
            		}
            		if(!fileExists) {
            			if(selectedFiles[i].isDirectory()) {
            				for(File f : selectedFiles[i].listFiles()) {
            					fileList.add(f.getAbsolutePath());
            				}
            			} else {
            				fileList.add(selectedFiles[i].getAbsolutePath());
            			}
            		}
            	}
            	inputFileList.updateUI();
            }
        } else if (command.equalsIgnoreCase("Cancel")) {
        	dispose();
        } else if (command.equalsIgnoreCase("OK")) {
        	callAlgorithm();
        } else if(command.equalsIgnoreCase(REMOVE_ALL)) {
        	fileList.removeAllElements();
        	inputFileList.updateUI();
        } else if(command.equals(REMOVE)) {
        	Object[] objAr = inputFileList.getSelectedValues();
        	for(Object obj : objAr) {
        		fileList.remove(obj);
        	}
        	inputFileList.updateUI();
        } 
    }

	/**
     * This method is required if the AlgorithmPerformed interface is implemented. It is called by the algorithms when
     * it has completed or failed to complete, so that the dialog can display the result image and/or clean up.
     *
     * @param  algorithm  Algorithm that caused the event.
     */
    public void algorithmPerformed(AlgorithmBase algorithm) {
    	ArrayList<String> xmlList = algoCreateXML.getXmlList();
    	String totalStr = new String();
    	for(int i=0; i<xmlList.size(); i++) {
    		totalStr += xmlList.get(i) + "\n";
    	}
    	if(totalStr.length() > 0) {
    		MipavUtil.displayInfo("The following XML files have been written:\n"+totalStr);
    	} else {
    		MipavUtil.displayError("No XML files have been written.");
    	}
    	
    	progressBar.dispose();
    	algoCreateXML.finalize();
    	algoCreateXML = null;
    	
    }
    
    protected void setGUIFromParams() {
    	
    }
    protected void storeParamsFromGUI() {
    	
    }
    
    public static void main(String[] args) {
    	new PlugInDialogCreateXML(true);
    }
    
}
