package gov.nih.mipav.view.renderer.WildMagic.ProstateFramework;

import gov.nih.mipav.model.algorithms.*;
import gov.nih.mipav.model.file.FileIO;
import gov.nih.mipav.model.file.FileUtility;
import gov.nih.mipav.model.file.FileVOI;
import gov.nih.mipav.model.structures.*;
import gov.nih.mipav.view.dialogs.*;
import gov.nih.mipav.view.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import java.net.URLDecoder;

/**
 * This class attempts to generate the VOI from the HED predicted edge map (boundary map). 
 * 
 * Just for implementation only, never get a chance to run. 
 * 
 * ---------------------------------------------------------------
 * May consider to delete it after SVN check-in.  For backup only
 * ---------------------------------------------------------------
 * 
 * @author Ruida Cheng
 * 
 */
public class JDialogProstate2DSlicesAtlasPngConverter3DSurfaceEdgeMap extends JDialogBase implements AlgorithmInterface {

	private static final long serialVersionUID = -7360089445417194259L;

	/** The main user interface. */
	private ViewUserInterface UI;

	/** key image directory. */
	private JLabel labelKeyImage;
	private JTextField textFieldKeyImage;
	private JButton buttonKeyImage;

	private JPanel imageSelectionPanel;

	private Vector<String> keyImageVector5 = new Vector<String>();
	private Vector<String> keyImageVOIVector5 = new Vector<String>();

	/** saved 2D slices atlas dir. */
	private JLabel labelSaveImage;
	private JTextField textFieldSaveImage;
	private JButton buttonSaveImage;
	private JFileChooser saveImageChooser = new JFileChooser();
	private String saveImageDirectory;
	
	Hashtable<String, Hashtable<String, Vector<String>>> imageNameHashtableExtra = new Hashtable<String, Hashtable<String, Vector<String>>>();
	Hashtable<String, Hashtable<String, ModelImage>> imageHashtableExtra = new Hashtable<String, Hashtable<String, ModelImage>>();
	
	
	Hashtable<String, Vector<String>> imageTable = new Hashtable<String, Vector<String>>();
	Hashtable<String, Vector<String>> newImageTable = new Hashtable<String, Vector<String>>();
	Hashtable<String, Vector<ModelImage>> maskTable = new Hashtable<String, Vector<ModelImage>>();
	Hashtable<String, Vector<String>> maskImageTable = new Hashtable<String, Vector<String>>();
	
	/**
	 * Constructor.
	 * 
	 * @param theParentFrame
	 */
	public JDialogProstate2DSlicesAtlasPngConverter3DSurfaceEdgeMap(Frame theParentFrame) {
		super(theParentFrame, false);
		UI = ViewUserInterface.getReference();
		init();
		setVisible(true);

	}

	/**
	 * dispose memory
	 * */
	public void disposeLocal() {

	}

	/**
	 * Dialog local actionPerformed handler.
	 */
	public void actionPerformed(ActionEvent event) {

		String command = event.getActionCommand();
		if (command.equals("OK")) {
			callAlgorithm();
		} else if (command.equals("Cancel")) {
			dispose();
		} else if (command.equals("Help")) {
			
		} else if (command.equals("ChooseKeyImageDir")) {
			readKeyImageDir();
			sortImageTable();
		} else if (command.equals("ChooseSaveImageDir")) {
			recordSaveImageDir();
		}
	}
	
	private void sortImageTable() {
		Set<String> keys = imageTable.keySet();
		for ( String hashID : keys ) {
		Vector<String> stringVec = imageTable.get(hashID);
			Vector<String> newStringVect = new Vector<String>();
			Hashtable<Integer, String> hash = new Hashtable<Integer, String>();
			for (int j = 0; j < stringVec.size(); j++) {
				String fullName = stringVec.get(j);
				int stopIndex = fullName.lastIndexOf("-");
				String subName = fullName.substring(0, stopIndex - 1);
				stopIndex = subName.lastIndexOf("-");
				int startIndex = fullName.lastIndexOf("_");
				String numString = fullName.substring(startIndex + 1, stopIndex);
				// System.err.println(numString);
				int num = Integer.valueOf(numString);
				// System.err.println("num = " + num);
				hash.put(num, fullName);
			}

			for (int j = 0; j < 1000; j++) {
				String name = hash.get(j);
				if (name != null) {
					newStringVect.add(name);
					System.err.println("name = " + name);
				}
			}
			System.err.println("ruida hashID = " + hashID);
			maskImageTable.put(hashID, newStringVect);
		}
		// pause();
		System.err.println(" finish inti read dir");
	}

	/**
	 * Let user specify the saved 2D slices atlas, record the save directory.
	 */
	private void recordSaveImageDir() {
		String saveImageName;
		saveImageChooser.setDialogTitle("Open Saved Images Directory");
		saveImageChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		final int returnValue = saveImageChooser.showOpenDialog(UI.getMainFrame());

		if (returnValue == JFileChooser.APPROVE_OPTION) {
			saveImageName = saveImageChooser.getSelectedFile().getName();

			saveImageDirectory = String.valueOf(saveImageChooser.getCurrentDirectory()) + File.separatorChar + saveImageName + File.separatorChar;
			textFieldSaveImage.setText(saveImageDirectory);

		} else {
			return;
		}
	}

	/**
	 * Read 3D images atlas directory.
	 */
	private void readKeyImageDir() {
	
		File fileDir_2 = new File("/scratch/ISBI2017/first50");
		traverse_Layer_extra(fileDir_2);
		File fileDir_5_test = new File("/scratch/ISBI2017/resultPNG/hed_prostate_mask_iter_23000_coronal");
		traverse_folder_5(fileDir_5_test);
		
	}


	private void traverse_Layer_extra(File dir) {
		
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {    
				traverse_patientID(new File(dir + File.separator + children[i]), dir + File.separator + children[i]);
			}
			
		}
	}

	private void traverse_patientID(File dir, String patientID ) {
		if ( dir.isDirectory() ) {
			String[] children = dir.list();
			for ( int i = 0; i < children.length; i++ ) {
				traverse_date(new File(dir + File.separator + children[i]), patientID, children[i]);
			}
		}
	}
	
	private void traverse_date(File dir, String patientID, String date) {
		String hashID = patientID + File.separator + date;
		imageNameHashtableExtra.put(hashID, new Hashtable<String, Vector<String>>());
		traverse_orientation(dir, hashID);
	}
	
	private void traverse_orientation(File dir, String hashID) {
		if ( dir.isDirectory() ) {
			String[] children = dir.list();
			for ( int i = 0; i < children.length; i++ ) {
				
		        String decodeChildName = MipavUtil.decodeStr(children[i]);
		        if ( decodeChildName.toLowerCase().contains("ax")) {
		        	imageNameHashtableExtra.get(hashID).put("axial", new Vector<String>());
		        	traverse_serial(new File(dir + File.separator + decodeChildName), hashID, "axial");
		        } else if ( decodeChildName.toLowerCase().contains("sag")) {
		        	imageNameHashtableExtra.get(hashID).put("sagittal", new Vector<String>());
		        	traverse_serial(new File(dir + File.separator + decodeChildName), hashID, "sagittal");
		        } else if ( decodeChildName.toLowerCase().contains("cor")) {
		        	imageNameHashtableExtra.get(hashID).put("coronal", new Vector<String>());
		        	traverse_serial(new File(dir + File.separator + decodeChildName), hashID, "coronal");
		        }
			}
		}
	}
	
	private void traverse_serial(File dir, String hashID, String orientation) {
		
		Hashtable<String, Vector<Integer>> serialTable = new Hashtable<String, Vector<Integer>>();
		serialTable.put(orientation, new Vector<Integer>());
		if ( dir.isDirectory() ) {
			String[] children = dir.list(); 
			int min = Integer.MAX_VALUE;
			int serialNum;
			for ( int i = 0; i < children.length; i++ ) {
				serialNum = Integer.valueOf(children[i]);
				if ( serialNum < min ) {
					min = serialNum;
				}				
			}
			
			String decodedPathChild = MipavUtil.decodeStr(dir + File.separator + min+ File.separator);	
			traverse_dicom(new File(decodedPathChild), hashID, orientation );
			
		}
	}
	
	private void traverse_dicom(File dir, String hashID, String orientation ) {
		
		if ( dir.isDirectory() ) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++ ) {
				String decodedPathChild = MipavUtil.decodeStr(dir + File.separator + children[i]);	
				System.err.println(decodedPathChild);
				imageNameHashtableExtra.get(hashID).get(orientation).add(decodedPathChild);
			}
		}
		
	}
	

	private void traverse_folder_5(File dir) {
		// processDir_folder_5(dir);
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				traverse_folder_5(new File(dir, children[i]), children[i]);
			}
		}

	}
	

	private void traverse_folder_5(File dir, String hashID) {
		processDir_folder_5(dir);

		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				processDir_folder_5(new File(dir, children[i]), hashID);
			}
		}

	}
	

	private void processDir_folder_5(File dir, String hashID) {
		String dirName = dir.toString();
		// System.err.println("dirName = " + dirName);
		int begin = dirName.lastIndexOf(File.separator) + 1;
		int end = dirName.length();

		if (dirName.substring(begin, end).startsWith("image") && 
				dirName.substring(begin, end).endsWith(".png") && 
				dirName.contains("fuse")) {
			// System.err.println("file = " + dir.toString());
			String fullName = dir.toString();
			int idxEnd = fullName.lastIndexOf("/");
			String subName = fullName.substring(0, idxEnd);
			int idxStart = subName.lastIndexOf("/");
			String numString = fullName.substring(idxStart + 1, idxEnd);
			int hashIndex = Integer.valueOf(numString);
			// System.err.println("numString = " + numString);
			if (imageTable.get(hashID) == null) {
				imageTable.put(hashID, new Vector<String>());
			}
			imageTable.get(hashID).add(dir.toString());
		}
	}

	private void processDir_folder_5(File dir) {
		String dirName = dir.toString();
		// System.err.println("dirName = " + dirName);
		int begin = dirName.lastIndexOf(File.separator) + 1;
		int end = dirName.length();

		if (dirName.substring(begin, end).startsWith("image") && dirName.substring(begin, end).endsWith(".xml")) {
			// System.err.println(dir.toString());
			keyImageVector5.add(dir.toString());
		}

	}

	public void sortKeyImage_5() {
		int i;
		int len = keyImageVOIVector5.size();
		String imageName;
		String voiName;
		int start, end;
		Hashtable<Integer, String> imageNameTable = new Hashtable<Integer, String>();
		Hashtable<Integer, String> imageVOITable = new Hashtable<Integer, String>();
		int index;

		for (i = 0; i < len; i++) {
			imageName = keyImageVector5.get(i);
			start = imageName.lastIndexOf(File.separator) + 1;
			end = imageName.lastIndexOf(".");
			// System.err.println(imageName.substring(start+5, end));
			index = Integer.valueOf(imageName.substring(start + 5, end));
			// System.err.println("index = " + index);
			imageNameTable.put(index, imageName);
		}

		keyImageVector5.clear();
		for (i = 0; i <= 49; i++) {
			imageName = imageNameTable.get(i);
			if (imageName != null) {
				keyImageVector5.add(imageName);
			}
		}

		for (i = 0; i < len; i++) {
			voiName = keyImageVOIVector5.get(i);
			start = voiName.lastIndexOf(File.separator) + 1;
			end = voiName.lastIndexOf(".");
			// System.err.println(voiName.substring(start+3, end));
			index = Integer.valueOf(voiName.substring(start + 3, end));
			// System.err.println("index = " + index);
			imageVOITable.put(index, voiName);
		}

		keyImageVOIVector5.clear();
		for (i = 0; i <= 49; i++) {
			voiName = imageVOITable.get(i);
			if (voiName != null) {
				keyImageVOIVector5.add(voiName);
			}
		}

		// test for printing
		i = 0;
		for (String entry : keyImageVector5) {
			System.err.println(i + " = " + entry);
			i++;
		}
		System.err.println("VOI:");
		i = 0;
		for (String entry : keyImageVOIVector5) {
			System.err.println(i + " = " + entry);
			i++;
		}

	}


	/**
	 * This method is required if the AlgorithmPerformed interface is
	 * implemented. It is called by the algorithms when it has completed or
	 * failed to to complete, so that the dialog can be display the result image
	 * and/or clean up.
	 * 
	 * @param algorithm
	 *            Algorithm that caused the event.
	 */
	public void algorithmPerformed(AlgorithmBase algorithm) {

	}
	
	private void generateContours() {
		
		Set<String> hashKeys = imageHashtableExtra.keySet();
		
		int readIndex = 0;
		
	    for ( String hashID : hashKeys ) {
	    	
	    	Hashtable<String, ModelImage> imageHash = imageHashtableExtra.get(hashID);
	    	ModelImage srcImage = imageHash.get("coronal");
	    	
	    	int idx = hashID.lastIndexOf(File.separator);
	    	String upperDir = hashID.substring(0, idx);
	    	String lowerDir = hashID.substring(idx+1, hashID.length());
	    	idx = upperDir.lastIndexOf(File.separator);
	    	upperDir = upperDir.substring(idx+1, upperDir.length());
	    	
	    	String subDir = saveImageDirectory + File.separator + "coronal" + File.separator;
	    	File subDirFile = new File(subDir);
			if (!subDirFile.isDirectory())
				subDirFile.mkdir();
			subDir += upperDir + File.separator;
			subDirFile = new File(subDir);
			if (!subDirFile.isDirectory())
				subDirFile.mkdir();
			subDir += lowerDir + File.separator;
			subDirFile = new File(subDir);
			if (!subDirFile.isDirectory())
				subDirFile.mkdir();
			System.err.println("upper = " + upperDir + "   lower = " + lowerDir);
			System.err.println("subDir " + subDir);
		
	    	
			Vector<ModelImage> imageVec = maskTable.get(upperDir);
			VOI voiNewFinal = new VOI((short)0, "voi_reuslt_" + upperDir);
			int[] extents = new int[2];
			for (int j = 0; j < imageVec.size(); j++) {
				ModelImage image = imageVec.get(j);
				extents = image.getExtents();
				String name = "binaryMask_" + image.getImageName();
				// new ViewJFrameImage(image);
				ModelImage resultImage = new ModelImage(ModelImage.BOOLEAN, extents, name);
				float[] thresholds = new float[2];
				thresholds[0] = 210;
				thresholds[1] = 255;
				float fillValue = 0f;
				boolean isInverse = false;
				boolean regionFlag = true;
				int outputType = 1;

				AlgorithmThresholdDual thresholdAlgo = new AlgorithmThresholdDual(resultImage, image, thresholds, fillValue, outputType, regionFlag, isInverse);
				thresholdAlgo.run();
                
				boolean wholeImage = true;

				
				AlgorithmMorphology2D idObjectsAlgo2D;
				int method = AlgorithmMorphology2D.ID_OBJECTS;

				idObjectsAlgo2D = new AlgorithmMorphology2D(resultImage, 0, 0, method, 0, 0, 0, 0, wholeImage);
				idObjectsAlgo2D.setMinMax(1, Integer.MAX_VALUE);
				idObjectsAlgo2D.run();
				idObjectsAlgo2D.finalize();
				idObjectsAlgo2D = null;
				
				resultImage.calcMinMax();
				final AlgorithmVOIExtraction VOIExtractionAlgo = new AlgorithmVOIExtraction(resultImage);
				VOIExtractionAlgo.run();
				
				// new ViewJFrameImage(resultImage);
				VOIVector v = resultImage.getVOIs();
				if ( v.size() == 0 ) continue;
				
				if (resultImage.getVOIs() != null  ) {
					
					int maxVOIs = 0;
					int voiIndex = 0;
					int contourIndex = 0;
					for ( int k = 0; k < resultImage.getVOIs().size(); k++ ) {
						VOIBaseVector current_va = resultImage.getVOIs().VOIAt(k).getCurves();
						if (current_va != null && current_va.size() > 0) {
							for ( int r = 0; r < current_va.size(); r++ ) {
								VOIBase temp_v = current_va.get(r);
								if ( temp_v.size() > maxVOIs ) {
									maxVOIs = temp_v.size();
									contourIndex = r;
									voiIndex = k;
								}
							}
						}	
					}
					VOIBaseVector current_va = resultImage.getVOIs().VOIAt(voiIndex).getCurves();
						VOIBase current_v = current_va.get(contourIndex);
						VOIBase vTemp = (VOIBase) current_v.clone();
						int nPtsCurrent = current_v.size();

					    // if ( nPtsCurrent != 30 ) continue;

						float[] xPts = new float[nPtsCurrent];
						float[] yPts = new float[nPtsCurrent];
						float[] zPts = new float[nPtsCurrent];

						current_v.exportArrays(xPts, yPts, zPts);

						for (int k = 0; k < nPtsCurrent; k++) {
							zPts[k] = j;
						}

						vTemp.importArrays(xPts, yPts, zPts, nPtsCurrent);
						voiNewFinal.importCurve(vTemp);
						// vTemp = null;
					// }
				}
			
			}  // end for j loop
			
			voiNewFinal.update();
			// save VOI contours
			
			
			
			int[] newExtents = new int[3];		
			newExtents[0] = extents[0];
			newExtents[1] = extents[1];
			newExtents[2] = imageVec.size();
			
			
			srcImage.registerVOI(voiNewFinal);
			srcImage.setImageName(hashID);
			new ViewJFrameImage(srcImage);
			smoothVOI30(srcImage, srcImage);
			
			try {
				FileVOI fileVOI = new FileVOI("voi_" + readIndex + ".voi", subDir, srcImage);
				fileVOI.writeVOI(voiNewFinal, true);
				srcImage.saveImage(subDir, "image" + readIndex + ".xml" , FileUtility.XML, false);	
			} catch (IOException e ) {
				e.printStackTrace();
			}
			readIndex++;
		
		}
		
	}
	
	public void smoothVOI30(ModelImage maskImage, ModelImage resultImage) {

		VOIVector v = maskImage.getVOIs();
		if (v.size() == 0)
			return;
		v.VOIAt(0).setActive(true);
		v.VOIAt(0).setAllActive(true);

		// new ViewJFrameImage(maskImage);
		try {
			AlgorithmBSmooth smoothAlgo = new AlgorithmBSmooth(maskImage,
					v.VOIAt(0), 30, false);
			smoothAlgo.addListener(this);
			smoothAlgo.run();

			VOIVector resultVOIs = resultImage.getVOIs();
			VOI resultVOI = smoothAlgo.getResultVOI();
			resultVOIs.VOIAt(0).removeCurves();
			resultVOIs.VOIAt(0).setCurves(resultVOI.getCurves());
			resultImage.notifyImageDisplayListeners(null, true);
			// new ViewJFrameImage(resultImage);
			smoothAlgo.setCompleted(true);
			smoothAlgo.finalize();
			smoothAlgo = null;
		} catch (OutOfMemoryError x) {
			MipavUtil
					.displayError("Dialog Smooth: unable to allocate enough memory");

			return;
		}
	}

	
	/**
	 * Driver function to read image and VOIs, and convert each 3D image to 2D
	 * slices.
	 */
	public void callAlgorithm() {
		
		long startTime = System.currentTimeMillis();

		readImagesExra();
		
		generateContours();

		long endTime = System.currentTimeMillis();
		int min = (int) ((endTime - startTime) / 1000f / 60f);
		int sec = (int) ((endTime - startTime) / 1000f % 60f);
		System.err.println("time elapse = " + min + "  mins  " + sec + "  sec");


	}

	
	public void readImagesExra() {

		String imageFullName;
		boolean multiFile = true;
		int count = 0;
		Set<String> keys = imageNameHashtableExtra.keySet();
		try {
		for (String hashID : keys) {
  
			imageHashtableExtra.put(hashID, new Hashtable<String, ModelImage>());
			Hashtable<String, Vector<String>> imageHashtable = imageNameHashtableExtra.get(hashID);
			
			Vector<String> axialNames = imageHashtable.get("axial");
		 
		    FileIO fileIO = new FileIO();
			 imageFullName = axialNames.get(0);
			int index = imageFullName.lastIndexOf(File.separator);
			String fileName = imageFullName.substring(index + 1, imageFullName.length());
			String directory = imageFullName.substring(0, index + 1);
			System.err.println("filename = " + fileName);
			System.err.println("directory = " + directory);
			ModelImage imageAxial = fileIO.readImage(fileName, directory, multiFile, null);
			
			imageHashtableExtra.get(hashID).put("axial", imageAxial);
		
		    
			Vector<String> sagittalNames = imageHashtable.get("sagittal");
			imageFullName = sagittalNames.get(0);
			index = imageFullName.lastIndexOf(File.separator);
			fileName = imageFullName.substring(index + 1, imageFullName.length());
			directory = imageFullName.substring(0, index + 1);
			System.err.println("filename = " + fileName);
			System.err.println("directory = " + directory);
			ModelImage imageSagittal = fileIO.readImage(fileName, directory, multiFile, null);
			imageHashtableExtra.get(hashID).put("sagittal", imageSagittal);
			
			Vector<String> coronalNames = imageHashtable.get("coronal");
			imageFullName = coronalNames.get(0);
			index = imageFullName.lastIndexOf(File.separator);
			fileName = imageFullName.substring(index + 1, imageFullName.length());
			directory = imageFullName.substring(0, index + 1);
			System.err.println("filename = " + fileName);
			System.err.println("directory = " + directory);
			ModelImage imageCoronal = fileIO.readImage(fileName, directory, multiFile, null);
			
			imageHashtableExtra.get(hashID).put("coronal", imageCoronal);
			 
			count++;
			if ( count == 50 ) break;
          
		}	
	
		Set<String> keyset = maskImageTable.keySet();
		for (String key : keyset ) {
			Vector<String> stringVec = maskImageTable.get(key);
			Vector<ModelImage> imageVec = new Vector<ModelImage>();

			for (int j = 0; j < stringVec.size(); j++) {
				String dir = stringVec.get(j);
				int index = dir.lastIndexOf(File.separator);
				String directory = new String(dir.substring(0, index + 1));
				String fileName = new String(dir.substring(index + 1, dir.length()));
				// System.err.println("Key Image: fileName = " + fileName +
				// "  directory = " + directory);
				FileIO keyImageIO = new FileIO();
				imageVec.add(keyImageIO.readImage(fileName, directory));
			}

			maskTable.put(key, imageVec);

		}

		
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
	}
	

	/**
	 * Smooth VOIs to 60 points.
	 * 
	 * @param maskImage
	 * @param resultImage
	 */
	public void smoothVOI60(ModelImage maskImage, ModelImage resultImage) {

		VOIVector v = maskImage.getVOIs();
		if (v.size() == 0)
			return;
		v.VOIAt(0).setActive(true);
		v.VOIAt(0).setAllActive(true);

		// new ViewJFrameImage(maskImage);
		try {
			AlgorithmBSmooth smoothAlgo = new AlgorithmBSmooth(maskImage, v.VOIAt(0), 60, false);
			smoothAlgo.addListener(this);
			smoothAlgo.run();

			VOIVector resultVOIs = resultImage.getVOIs();
			VOI resultVOI = smoothAlgo.getResultVOI();
			resultVOIs.VOIAt(0).removeCurves();
			resultVOIs.VOIAt(0).setCurves(resultVOI.getCurves());
			resultImage.notifyImageDisplayListeners(null, true);
			// new ViewJFrameImage(resultImage);
			smoothAlgo.setCompleted(true);
			smoothAlgo.finalize();
			smoothAlgo = null;
		} catch (OutOfMemoryError x) {
			MipavUtil.displayError("Dialog Smooth: unable to allocate enough memory");

			return;
		}
	}

	

	/**
	 * Initial panel
	 */
	public void init() {

		JPanel mainPanel;

		mainPanel = new JPanel();
		mainPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		mainPanel.setLayout(new BorderLayout());

		buildKeyImagePanel();

		mainPanel.add(imageSelectionPanel, BorderLayout.CENTER);
		mainPanel.add(buildButtons(), BorderLayout.SOUTH);

		getContentPane().add(mainPanel);
		pack();
		setResizable(true);
	}

	/**
	 * Panel contains both the 3D image dir and saved 2D slices atlas dir.
	 */
	public void buildKeyImagePanel() {

		final GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.EAST;

		imageSelectionPanel = new JPanel();
		imageSelectionPanel.setLayout(new GridLayout(2, 3));
		imageSelectionPanel.setBorder(buildTitledBorder("Auto Train"));

		// Key image directory
		gbc.gridx = 0;
		gbc.gridy = 1;
		labelKeyImage = new JLabel("Key Image Directory: ");
		labelKeyImage.setFont(serif12);
		labelKeyImage.setForeground(Color.black);

		imageSelectionPanel.add(labelKeyImage, gbc);

		textFieldKeyImage = new JTextField(20);
		textFieldKeyImage.setFont(serif12);

		gbc.gridx = 1;
		imageSelectionPanel.add(textFieldKeyImage, gbc);

		buttonKeyImage = new JButton("Choose");
		buttonKeyImage.addActionListener(this);
		buttonKeyImage.setActionCommand("ChooseKeyImageDir");
		buttonKeyImage.setFont(serif12B);
		buttonKeyImage.setPreferredSize(MipavUtil.defaultButtonSize);

		gbc.gridx = 2;
		imageSelectionPanel.add(buttonKeyImage, gbc);

		// Save image directory
		gbc.gridx = 0;
		gbc.gridy = 2;
		labelSaveImage = new JLabel("Saved Image Directory: ");
		labelSaveImage.setFont(serif12);
		labelSaveImage.setForeground(Color.black);

		imageSelectionPanel.add(labelSaveImage, gbc);

		textFieldSaveImage = new JTextField(20);
		textFieldSaveImage.setFont(serif12);

		gbc.gridx = 1;
		imageSelectionPanel.add(textFieldSaveImage, gbc);

		buttonSaveImage = new JButton("Choose");
		buttonSaveImage.addActionListener(this);
		buttonSaveImage.setActionCommand("ChooseSaveImageDir");
		buttonSaveImage.setFont(serif12B);
		buttonSaveImage.setPreferredSize(MipavUtil.defaultButtonSize);

		gbc.gridx = 2;
		imageSelectionPanel.add(buttonSaveImage, gbc);

	}

}