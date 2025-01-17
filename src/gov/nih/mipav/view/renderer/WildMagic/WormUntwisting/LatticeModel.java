package gov.nih.mipav.view.renderer.WildMagic.WormUntwisting;


import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.terracotta.utilities.io.Files;

import WildMagic.LibFoundation.Containment.ContBox3f;
import WildMagic.LibFoundation.Curves.NaturalSpline3;
import WildMagic.LibFoundation.Distance.DistanceSegment3Segment3;
import WildMagic.LibFoundation.Distance.DistanceVector3Segment3;
import WildMagic.LibFoundation.Intersection.IntrSegment3Box3f;
import WildMagic.LibFoundation.Mathematics.Box3f;
import WildMagic.LibFoundation.Mathematics.ColorRGBA;
import WildMagic.LibFoundation.Mathematics.Ellipsoid3f;
import WildMagic.LibFoundation.Mathematics.Line3f;
import WildMagic.LibFoundation.Mathematics.Mathf;
import WildMagic.LibFoundation.Mathematics.Matrix3f;
import WildMagic.LibFoundation.Mathematics.Segment3f;
import WildMagic.LibFoundation.Mathematics.Vector3d;
import WildMagic.LibFoundation.Mathematics.Vector3f;
import WildMagic.LibGraphics.SceneGraph.IndexBuffer;
import WildMagic.LibGraphics.SceneGraph.TriMesh;
import WildMagic.LibGraphics.SceneGraph.VertexBuffer;
import gov.nih.mipav.model.algorithms.AlgorithmBase;
import gov.nih.mipav.model.algorithms.AlgorithmRegionGrow;
import gov.nih.mipav.model.algorithms.filters.AlgorithmFFT;
import gov.nih.mipav.model.algorithms.filters.OpenCL.filters.OpenCLAlgorithmGaussianBlur;
import gov.nih.mipav.model.file.FileIO;
import gov.nih.mipav.model.file.FileVOI;
import gov.nih.mipav.model.structures.CubeBounds;
import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.model.structures.ModelStorageBase;
import gov.nih.mipav.model.structures.Point3D;
import gov.nih.mipav.model.structures.VOI;
import gov.nih.mipav.model.structures.VOIBase;
import gov.nih.mipav.model.structures.VOIBaseVector;
import gov.nih.mipav.model.structures.VOIContour;
import gov.nih.mipav.model.structures.VOILine;
import gov.nih.mipav.model.structures.VOIText;
import gov.nih.mipav.model.structures.VOIVector;
import gov.nih.mipav.model.structures.ModelStorageBase.DataType;
import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.Preferences;
import gov.nih.mipav.view.ViewJFrameImage;
import gov.nih.mipav.view.ViewUserInterface;
import gov.nih.mipav.view.ViewVOIVector;
import gov.nih.mipav.view.dialogs.JDialogAnnotation;
import gov.nih.mipav.view.dialogs.JDialogBase;
import gov.nih.mipav.view.renderer.WildMagic.Interface.FileSurface_WM;
import gov.nih.mipav.view.renderer.WildMagic.Interface.SurfaceState;
import gov.nih.mipav.view.renderer.WildMagic.Render.VolumeImage;
import gov.nih.mipav.view.renderer.WildMagic.Render.VolumeSurface;
import gov.nih.mipav.view.renderer.WildMagic.VOI.VOILatticeManagerInterface;


/**
 * Supports the worm-straightening algorithms that use a 3D lattice as the basis of the straightening process.
 */
public class LatticeModel {

	protected static final int SampleLimit = 5;
	
	public static void checkParentDir( String parentDir )
	{
		File parentFileDir = new File(parentDir);
		if (parentFileDir.exists() && parentFileDir.isDirectory()) { // do nothing
		} else if (parentFileDir.exists() && !parentFileDir.isDirectory()) { // do nothing
		} else { // voiFileDir does not exist
			//			System.err.println( "LatticeModel:checkParentDir" + parentDir);
			parentFileDir.mkdir();
		}
	}

	static private String getImageName(ModelImage image) {
		return JDialogBase.makeImageName(image.getImageFileName(),  "");
	}

	static public boolean match( Color c1, Color c2 )
	{
		return ( (c1.getRed() == c2.getRed()) && (c1.getGreen() == c2.getGreen()) && (c1.getBlue() == c2.getBlue()) );
	}
	
	public static boolean checkName(String name, VOI annotationVOIs) {
		if ( annotationVOIs == null ) return false;
		if ( annotationVOIs.getCurves() == null ) return false;
		if ( annotationVOIs.getCurves().size() == 0 ) return false;
		
		for ( int i = 0; i < annotationVOIs.getCurves().size(); i++ ) {
			VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
			if ( text.getText().contentEquals(name) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Read a list of annotations from a CSV file: name,x,y,z,radius (optional)
	 * @param fileName
	 * @return VOI containing list of annotations.
	 */
	public static VOI readAnnotationsCSV( String fileName )
	{
		File file = new File(fileName);
		if ( file.exists() )
		{		
			short sID = 0;
			//        	System.err.println( fileName );
			FileReader fr;
			try {
				fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);
				String line = br.readLine();
				line = br.readLine();
				String renameString = "";
				VOI annotationVOIs = new VOI( sID, "annotationVOIs", VOI.ANNOTATION, 0 );
				int count = 1;
				while ( line != null && (line.length() > 1) )
				{
					String[] parsed = line.split( "," );
					if ( parsed.length != 0 )
					{
						VOIWormAnnotation text = new VOIWormAnnotation();
						text.setUseMarker(false);
						text.setNote(fileName);
						float x, y, z, r, g, b;
						if ( parsed.length > 6 )
						{
							// name, position and color
							int parsedIndex = 0;
							String name = String.valueOf( parsed[parsedIndex++] );
							text.setText(name);
							if ( checkName(name, annotationVOIs) ) {
								renameString += name + "\n";
							}
							x    = (parsed.length > parsedIndex+0) ? (parsed[parsedIndex+0].length() > 0) ? Float.valueOf( parsed[parsedIndex+0] ) : 0 : 0; 
							y    = (parsed.length > parsedIndex+1) ? (parsed[parsedIndex+1].length() > 0) ? Float.valueOf( parsed[parsedIndex+1] ) : 0 : 0; 
							z    = (parsed.length > parsedIndex+2) ? (parsed[parsedIndex+2].length() > 0) ? Float.valueOf( parsed[parsedIndex+2] ) : 0 : 0;
							r    = (parsed.length > parsedIndex+3) ? (parsed[parsedIndex+3].length() > 0) ? Float.valueOf( parsed[parsedIndex+3] ) : 1 : 1;
							g    = (parsed.length > parsedIndex+4) ? (parsed[parsedIndex+4].length() > 0) ? Float.valueOf( parsed[parsedIndex+4] ) : 1 : 1;
							b    = (parsed.length > parsedIndex+5) ? (parsed[parsedIndex+5].length() > 0) ? Float.valueOf( parsed[parsedIndex+5] ) : 1 : 1;
							//							System.err.println( name + " " + x + " " + y + " " + z + " " + r );
							if ( (parsedIndex + 6) < parsed.length ) {
								// read the lattice segment:
								int segment = (parsed.length > parsedIndex+6) ? (parsed[parsedIndex+6].length() > 0) ? Integer.valueOf( parsed[parsedIndex+6] ) : 1 : 1;
								//								System.err.println( name + "  " + segment );
								text.setLatticeSegment(segment);
							}
							text.add( new Vector3f( x, y, z ) );
							text.add( new Vector3f( x, y, z ) );
							text.setColor( new Color(r/255f,g/255f,b/255f) );
							annotationVOIs.getCurves().add(text);
						}
						else if ( parsed.length >= 4 )
						{
							// name, position and radius:
							int parsedIndex = 0;
							String name = String.valueOf( parsed[parsedIndex++] );
							text.setText(name);

							if ( checkName(name, annotationVOIs) ) {
								renameString += name + "\n";
							}
							x    = (parsed.length > parsedIndex+0) ? (parsed[parsedIndex+0].length() > 0) ? Float.valueOf( parsed[parsedIndex+0] ) : 0 : 0; 
							y    = (parsed.length > parsedIndex+1) ? (parsed[parsedIndex+1].length() > 0) ? Float.valueOf( parsed[parsedIndex+1] ) : 0 : 0; 
							z    = (parsed.length > parsedIndex+2) ? (parsed[parsedIndex+2].length() > 0) ? Float.valueOf( parsed[parsedIndex+2] ) : 0 : 0;
							r    = (parsed.length > parsedIndex+3) ? (parsed[parsedIndex+3].length() > 0) ? Float.valueOf( parsed[parsedIndex+3] ) : 1 : 1;
							//							System.err.println( name + " " + x + " " + y + " " + z + " " + r );
							text.add( new Vector3f( x, y, z ) );
							text.add( new Vector3f( x+r, y, z ) );
							annotationVOIs.getCurves().add(text);
						}
						else if ( parsed.length == 3 )
						{
							if ( count > 1 ) {
							// position only
							x    = (parsed.length > 0) ? (parsed[0].length() > 0) ? Float.valueOf( parsed[0] ) : 0 : 0; 
							y    = (parsed.length > 1) ? (parsed[1].length() > 0) ? Float.valueOf( parsed[1] ) : 0 : 0; 
							z    = (parsed.length > 2) ? (parsed[2].length() > 0) ? Float.valueOf( parsed[2] ) : 0 : 0; 
							text.setText( "pt_" + count );
							text.add( new Vector3f( x, y, z ) );
							text.add( new Vector3f( x+1, y, z ) );
							annotationVOIs.getCurves().add(text);
							}
						}
						count++;
					}
					line = br.readLine();
				}
				if ( renameString.length() > 0 ) {
					MipavUtil.displayError( "Duplicate annotations:\n" + renameString );
				}
				fr.close();
				if ( count > 1 )
				{
					return annotationVOIs;
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return null;
	}

	public static VOIVector readLatticeCSV(String fileName) {
		return readLatticeCSV(fileName, false);
	}

	public static VOIVector readLatticeCSV(String fileName, boolean saveSeamCells ) {
		File file = new File(fileName);
		if ( file.exists() )
		{		
			VOIVector lattice = new VOIVector();
			short sID = 0;
			FileReader fr;
			try {
				fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);
				String line = br.readLine();
				//				System.err.println(line);
				line = br.readLine();
				//				System.err.println(line);

				VOI left = new VOI(sID, "left", VOI.ANNOTATION, (float) Math.random());
				left.setColor(new Color(0, 0, 255));
				VOI right = new VOI(sID++, "right", VOI.ANNOTATION, (float) Math.random());
				right.setColor(new Color(0, 0, 255));
				lattice.add(left);
				lattice.add(right);
				int count = 1;
				
				while ( line != null && (line.length() > 1) )
				{
					String[] parsed = line.split( "," );
					if ( parsed.length != 0 )
					{
						int parsedIndex = 0;
						String name = String.valueOf( parsed[parsedIndex++] );
						float x    = (parsed.length > parsedIndex+0) ? (parsed[parsedIndex+0].length() > 0) ? Float.valueOf( parsed[parsedIndex+0] ) : 0 : 0; 
						float y    = (parsed.length > parsedIndex+1) ? (parsed[parsedIndex+1].length() > 0) ? Float.valueOf( parsed[parsedIndex+1] ) : 0 : 0; 
						float z    = (parsed.length > parsedIndex+2) ? (parsed[parsedIndex+2].length() > 0) ? Float.valueOf( parsed[parsedIndex+2] ) : 0 : 0;
						
						VOIWormAnnotation leftAnnotation = new VOIWormAnnotation( new Vector3f(x,y,z) );
						leftAnnotation.setText(name);
						if ( name.contains("H") || name.contains("V") || name.contains("Q") || name.contains("T") ) {
							leftAnnotation.setSeamCell(true);
						}
						left.getCurves().add( leftAnnotation );
						count++;
					}
					line = br.readLine();
					//					System.err.println(line);

					parsed = line.split( "," );
					if ( parsed.length != 0 )
					{
						int parsedIndex = 0;
						String name = String.valueOf( parsed[parsedIndex++] );
						float x    = (parsed.length > parsedIndex+0) ? (parsed[parsedIndex+0].length() > 0) ? Float.valueOf( parsed[parsedIndex+0] ) : 0 : 0; 
						float y    = (parsed.length > parsedIndex+1) ? (parsed[parsedIndex+1].length() > 0) ? Float.valueOf( parsed[parsedIndex+1] ) : 0 : 0; 
						float z    = (parsed.length > parsedIndex+2) ? (parsed[parsedIndex+2].length() > 0) ? Float.valueOf( parsed[parsedIndex+2] ) : 0 : 0;
						
						VOIWormAnnotation rightAnnotation = new VOIWormAnnotation( new Vector3f(x,y,z) );
						rightAnnotation.setText(name);
						if ( name.contains("H") || name.contains("V") || name.contains("T") ) {
							rightAnnotation.setSeamCell(true);
						}
						right.getCurves().add( rightAnnotation );
						count++;
					}
					line = br.readLine();
					//					System.err.println(line);
				}
				fr.close();
				if ( count > 1 )
				{
					// create contour lines & pairs

					VOI leftLattice = new VOI(sID++, "leftContour", VOI.POLYLINE, (float) Math.random());					
					VOIContour leftC = new VOIContour(false);
					leftLattice.getCurves().add(leftC);
					lattice.add(leftLattice);
					
					VOI rightLattice = new VOI(sID++, "rightContour", VOI.POLYLINE, (float) Math.random());
					VOIContour rightC = new VOIContour(false);
					rightLattice.getCurves().add(rightC);
					lattice.add(rightLattice);

					for ( int i = 0; i < left.getCurves().size(); i++ ) {
						leftC.add(left.getCurves().elementAt(i).elementAt(0));
						rightC.add(left.getCurves().elementAt(i).elementAt(0));
						
						
						final VOI marker = new VOI(sID++, "pair_" + (i+1), VOI.POLYLINE, (float) Math.random());
						final VOIContour mainAxis = new VOIContour(false);
						mainAxis.add(left.getCurves().elementAt(i).elementAt(0));
						mainAxis.add(left.getCurves().elementAt(i).elementAt(0));
						marker.getCurves().add(mainAxis);
						marker.setColor(new Color(255, 255, 0));
						mainAxis.update(new ColorRGBA(1, 1, 0, 1));
						if (i == 0) {
							marker.setColor(new Color(0, 255, 0));
							mainAxis.update(new ColorRGBA(0, 1, 0, 1));
						}
						lattice.add(marker);
					}
				}
				return lattice;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return null;
	}




	/**
	 * Saves all VOIs to the specified file.
	 * 
	 * @param voiDir
	 * @param image
	 */
	public static void saveAllVOIsTo(final String voiDir, final ModelImage image) {
		try {
			final ViewVOIVector VOIs = image.getVOIs();

			final File voiFileDir = new File(voiDir);

			if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
				final String[] list = voiFileDir.list();
				for (int i = 0; i < list.length; i++) {
					final File lrFile = new File(voiDir + list[i]);
					lrFile.delete();
				}
			} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { 
				voiFileDir.delete();
			} else { // voiFileDir does not exist
				//				System.err.println( "saveAllVOIsTo " + voiDir);
				voiFileDir.mkdir();
			}

			final int nVOI = VOIs.size();
			for (int i = 0; i < nVOI; i++) {
				if ( VOIs.VOIAt(i).getCurves().size() > 0 ) {
					if (VOIs.VOIAt(i).getCurveType() != VOI.ANNOTATION) {
						final FileVOI fileVOI = new FileVOI(VOIs.VOIAt(i).getName() + ".xml", voiDir, image);
						fileVOI.writeXML(VOIs.VOIAt(i), true, true);
					} else {
						final FileVOI fileVOI = new FileVOI(VOIs.VOIAt(i).getName() + ".lbl", voiDir, image);
						fileVOI.writeAnnotationInVoiAsXML(VOIs.VOIAt(i).getName(), true);
					}
				}
			}

		} catch (final IOException error) {
			MipavUtil.displayError("Error writing all VOIs to " + voiDir + ": " + error);
		}

	} // end saveAllVOIsTo()
	
	

	public static void saveAnnotationsAsCSV(final String dir, final String fileName, VOI annotations )
	{
		LatticeModel.saveAnnotationsAsCSV(dir,fileName, annotations, false);
	}
	
	
	
	/**
	 * Saves the input annotations to the CSV file in the following format:
	 * name,x,y,z
	 * @param dir
	 * @param fileName
	 * @param annotations
	 */
	public static void saveAnnotationsAsCSV(final String dir, final String fileName, VOI annotations, boolean isCurve )
	{		
		// load the existing annotation file - use it to determine which annotations need untwisting.
		VOI originalAnnotation = LatticeModel.readAnnotationsCSV(dir + File.separator + fileName );
//		System.err.println("saveAnnotationsAsCSV reading orig: " + (dir + File.separator + fileName));

		if ( originalAnnotation != null ) {
			if ( originalAnnotation.getCurves().size() == annotations.getCurves().size() ) {
				// look for changes in annotations - build list to untwist:
				VOI changed = annotationChanged( annotations, originalAnnotation );
				if ( changed.getCurves().size() == 0 ) 
				{
//					System.err.println("saveAnnotationsAsCSV no changes - no save required");
					return;
				}
			}
		}
		
//		Preferences.debug("Saving annotations list: " + "\n", Preferences.DEBUG_ALGORITHM );
		//		System.err.println("Saving annotations list: " + dir + "  " + fileName );
		int numSaved = 0;
		// check files, create new directories and delete any existing files:
		final File fileDir = new File(dir);

		if (fileDir.exists() && fileDir.isDirectory()) {} 
		else if (fileDir.exists() && !fileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			fileDir.mkdir();
		}
		
		File file = new File(fileDir + File.separator + fileName);
		if (file.exists()) {
			if ( annotations == null || annotations.getCurves().size() == 0 ) {
	            int result = JOptionPane.showConfirmDialog(null, "No annotations found, delete annotations File from: " + fileDir + "?", "Delete Annotations", JOptionPane.YES_NO_OPTION);
//	            System.err.println( "Delete answer " + (result == JOptionPane.YES_OPTION) );
	            if (result != JOptionPane.YES_OPTION) {
	            	return;
	            }
			}
			file.delete();
			file = new File(fileDir + File.separator + fileName);
		}
		
		if ( annotations == null ) {
			System.err.println("annotations null");
			return;
		}
		if ( annotations.getCurves().size() == 0 ) {
			System.err.println("annotations size = 0");
			return;
		}
		
		try {
			boolean saveLatticeSegment = false;
			int curveAnnotationCount = 0;
			for ( int i = 0; i < annotations.getCurves().size(); i++ ) {
				VOIWormAnnotation annotation = (VOIWormAnnotation)annotations.getCurves().elementAt(i);
				if ( !annotation.isCurveAnnotation() && annotation.getLatticeSegment() != -1 ) {
					saveLatticeSegment = true;
				}
				if ( annotation.isCurveAnnotation() ) {
					curveAnnotationCount++;
				}
			}
			// only write out annotations that aren't part of a curve segment:
			if ( !isCurve && (curveAnnotationCount == annotations.getCurves().size()) ) return;
			

			
//			System.err.println( "saveAnnotationsAsCSV " + (annotations.getCurves().size() - curveAnnotationCount) );
			
			final FileWriter fw = new FileWriter(file);
			final BufferedWriter bw = new BufferedWriter(fw);
			if ( saveLatticeSegment ) {
				bw.write("name" + "," + "x_voxels" + "," + "y_voxels" + "," + "z_voxels" + "," + "R" + "," + "G" + "," + "B" + "," + "lattice segment" + "\n");
			}
			else {
				bw.write("name" + "," + "x_voxels" + "," + "y_voxels" + "," + "z_voxels" + "," + "R" + "," + "G" + "," + "B" + "\n");
			}
			
			for (int i = 0; i < annotations.getCurves().size(); i++) {

				VOIWormAnnotation annotation = (VOIWormAnnotation)annotations.getCurves().elementAt(i);
				if ( !isCurve && annotation.isCurveAnnotation() ) continue;
				if ( isCurve && !annotation.isCurveAnnotation() ) continue;
				if ( annotation.size() == 0 ) {
					System.err.println("error");
				}
				Vector3f position = annotation.elementAt(0);
				if ( saveLatticeSegment ) {
					int segment = annotation.getLatticeSegment();
					String text = annotation.getText();
					if ( text.length() == 0 ) {
						// generate the name:
					}
					if ( segment != -1 ) {
						bw.write(annotation.getText() + "," + position.X + "," + position.Y + ","	+ position.Z + "," + annotation.getColorString() + "," + segment + "\n");
					}
					else {
						bw.write(annotation.getText() + "," + position.X + "," + position.Y + ","	+ position.Z + "," + annotation.getColorString() + "\n");
					}
				}
				else {
					bw.write(annotation.getText() + "," + position.X + "," + position.Y + ","	+ position.Z + "," + annotation.getColorString() + "\n");
				}
				numSaved++;
				Preferences.debug(numSaved + "   " + annotation.getText() + "\n", Preferences.DEBUG_ALGORITHM );
				//				System.err.println( numSaved++ + "   " + annotation.getText() );
			}
			bw.newLine();
			bw.close();
		} catch (final Exception e) {
			System.err.println("CAUGHT EXCEPTION WITHIN saveSeamCellsTo");
			e.printStackTrace();
		}
		Preferences.debug("Annotation written: " + numSaved + "\n", Preferences.DEBUG_ALGORITHM );
//		System.err.println( "saveAnnotationsAsCSV " + fileName + "  " + numSaved );
	}

	private static Vector3f inverseDiagonal( ModelImage image, int slice, int extent, Vector3f[] verts, Vector3f target ) 
	{
		final int[] dimExtents = image.getExtents();
		final int iBound = extent;
		final int jBound = extent;

		final Vector3f center = new Vector3f();
		for (int i = 0; i < verts.length; i++) {
			center.add(verts[i]);
		}
		center.scale(1f / verts.length);

		/* Calculate the slopes for traversing the data in x,y,z: */
		float xSlopeX = verts[1].X - verts[0].X;
		float ySlopeX = verts[1].Y - verts[0].Y;
		float zSlopeX = verts[1].Z - verts[0].Z;

		float xSlopeY = verts[3].X - verts[0].X;
		float ySlopeY = verts[3].Y - verts[0].Y;
		float zSlopeY = verts[3].Z - verts[0].Z;

		float x0 = verts[0].X;
		float y0 = verts[0].Y;
		float z0 = verts[0].Z;

		xSlopeX /= (iBound);
		ySlopeX /= (iBound);
		zSlopeX /= (iBound);

		xSlopeY /= (jBound);
		ySlopeY /= (jBound);
		zSlopeY /= (jBound);

		/* loop over the 2D image (values) we're writing into */
		float x = x0;
		float y = y0;
		float z = z0;

		float minDistance = Float.MAX_VALUE;
		Vector3f closest = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		Vector3f pt = new Vector3f();
		for (int j = 0; j < jBound; j++) {

			/* Initialize the first diagonal point(x,y,z): */
			x = x0;
			y = y0;
			z = z0;

			for (int i = 0; i < iBound; i++) {

				final int iIndex = Math.round(x);
				final int jIndex = Math.round(y);
				final int kIndex = Math.round(z);

				// Bounds checking:
				if ( ( (iIndex < 0) || (iIndex >= dimExtents[0])) || ( (jIndex < 0) || (jIndex >= dimExtents[1]))
						|| ( (kIndex < 0) || (kIndex >= dimExtents[2])) ) {

					// do nothing
				} else {
					pt.set(i, j, slice);
					float dist = pt.distance(target);
					if ( dist < minDistance )
					{
						minDistance = dist;
						closest.set(iIndex, jIndex, kIndex);
					}
				}
				//				}
				/*
				 * Inner loop: Move to the next diagonal point along the x-direction of the plane, using the xSlopeX,
				 * ySlopeX and zSlopeX values:
				 */
				x = x + xSlopeX;
				y = y + ySlopeX;
				z = z + zSlopeX;
			}

			/*
			 * Outer loop: Move to the next diagonal point along the y-direction of the plane, using the xSlopeY,
			 * ySlopeY and zSlopeY values:
			 */
			x0 = x0 + xSlopeY;
			y0 = y0 + ySlopeY;
			z0 = z0 + zSlopeY;
		}
		//		System.err.println( "inverseDiagonal " + target + "     " + minDistance);
		//		System.err.println( "                " + closest );
		return closest;
	}

	protected ModelImage imageA;
	private Vector3f imageDims;
	private String outputDirectory;
	private String sharedOutputDir;
	
	// Set of two contours, each connected to the other pair-wise
	protected VOIVector lattice = null;
	// left side of the lattice:
	protected VOI left;
	protected VOI leftContour;
	// right side of the lattice:
	protected VOI right;
	protected VOI rightContour;
	// horizontal bars connecting left and right lattice pairs:
	private VOIVector latticeGrid;
	// center points half-way between left and right sides of the lattice:
	protected VOIContour center;

	// positions in time along the center-line spline for each lattice point
	protected int[] latticeSlices;

	// time positions for the center spline running through the center points:
	protected float[] afTimeC;
	// interpolated spline times
	protected float[] allTimes;
	// index into the interpolated curve points, each position corresponds to 
	// a point on the center curve:
	protected int[] splineRangeIndex;
	// spline through the center points, passes through each point
	protected NaturalSpline3 centerSpline;
	// contour for displaying the center spline - interpolated with 1-voxel stepsize
	protected VOIContour centerPositions;
	// contour for displaying the left spline - interpolated with 1-voxel stepsize
	protected VOIContour leftPositions;

	// contour for displaying the right spline - interpolated with 1-voxel stepsize
	protected VOIContour rightPositions;
	// VOI for displaying the left spline - interpolated with 1-voxel stepsize
	private VOI leftLine;

	// VOI for displaying the right spline - interpolated with 1-voxel stepsize
	private VOI rightLine;
	// VOI for displaying the center spline - interpolated with 1-voxel stepsize
	private VOI centerLine;
	// distance between left and right splines per voxel step
	protected Vector<Float> wormDiameters;

	// maximum diameter plus buffer:
	protected int extent = -1;
	// The following vector create an orthogonal frame at each position along
	// the center-line curve of the lattice - at each interpolated point along the curve:
	// right vector per voxel step
	protected Vector<Vector3f> rightVectors;

	// normal vector tangent to the spline per voxel step
	protected Vector<Vector3f> normalVectors;
	// up vector per voxel step
	protected Vector<Vector3f> upVectors;

	Vector<Vector3f> curvatureNormals;
	Vector<Float> curvature;

	// old / used for testing:
	// bounding box containing the ellipses:
	protected Vector<Box3f> boxBounds;

	// ellipse bounds around the worm mode;
	protected Vector<Ellipsoid3f> ellipseBounds;

	// Set when the sampling planes, etc are initialized/updated:
	private boolean latticeInterpolationInit = false;

	// planes that sweep through the worm volume, following the splines, used to sample the volume and untwist:
	protected VOI samplingPlanes;

	private int maxSplineLength = -1;

	private VOI[] displayContours;
	private VOI displayContours2;
	
	private VOI ellipseCurvesVOI;
	protected VOIContour[] ellipseCurves;
	/*
	 * relativeCrossSections
	 * 
	 * relativeCrossSections are a an abstract form of ellipseCurves above.
	 * The contours are relative to the center of the cross section, formerly the center of the ellipse.
	 * Each cross section should be of length numEllipsePts 
	 */
	protected VOIContour[] relativeCrossSections;
	
	private VOI selectedSectionVOI;
	private int selectedSectionIndex = -1;
	private int selectedSectionIndex2 = -1;

	protected VOI displayInterpolatedContours;

	private Vector3f pickedPoint = null;

	private VOI showSelectedVOI = null;

	private VOIContour[] showSelected = null;

	protected final int DiameterBuffer = 30;

	protected final float minRange = .025f;

	private VOI leftMarker;
	private VOI rightMarker;
	private VOI sectionMarker;

	protected VOI annotationVOIs;
	protected Vector3f wormOrigin = null;
	protected Vector3f transformedOrigin = new Vector3f();

	private Vector<VOI> neuriteData;

	protected Short voiID = 0;

	private int[][] seamCellIDs = null;
	private int[][] allSeamCellIDs = null;

	private boolean colorAnnotations = false;

	private Vector<AnnotationListener> annotationListeners;
	private Vector<CurveListener> curveListeners;
	private Vector<LatticeListener> latticeListeners;

	private String annotationPrefix = "A";

	private int paddingFactor = 0;


	private static final int numEllipsePts = 32;

	private VOI latticeContours = null;

	private Vector<String> markerNames;

	private Vector<Vector3f> markerCenters;

	private Vector<Integer> markerLatticeSegments;
	private Vector<Integer> markerSlices;

	private VOI annotationsStraight = null;

	private VOIVector latticeStraight = null;

	// some of these may be view states
	private boolean previewMode = false;
	private boolean editingCrossSections = false;
	private int crossSectionSamples = 8;
	private static float[][] crossSectionBases;
	
	static {
		crossSectionBases = precalculateCrossSectionBases(numEllipsePts);
	}
	
	private boolean updateCrossSectionOnDrag = true;
	// cross section editing state
	private boolean[] editedCrossSections = {};

	private Vector<boolean[]> clipMask = null;

	
	// spline data members:
	private class AnnotationSplineControlPts  {
		public String name;
		public String prefix;
		public Vector<VOIWormAnnotation> annotations;
		public VOIContour curve;
		public VOI curveVOI;
		public boolean selected = true;
		public AnnotationSplineControlPts() {}
	}
	protected Vector<AnnotationSplineControlPts> splineControlPtsList;

	/**
	 * Creates a new LatticeModel
	 * 
	 * @param imageA
	 */
	public LatticeModel(final ModelImage image) {
		setImage(image);
	}

	/**
	 * Creats a new LatticeModel with the given input lattice.
	 * 
	 * @param imageA
	 * @param lattice
	 */
	public LatticeModel(final ModelImage image, final VOIVector lattice) {
		setImage(image);
		this.lattice = lattice;

		// Assume image is isotropic (square voxels).
		if (lattice.size() < 2) {
			return;
		}
		left = (VOI) lattice.elementAt(0);	left.setName("left");
		right = (VOI) lattice.elementAt(1);	right.setName("right");
		if (left.getCurves().size() != right.getCurves().size()) {
			return;
		}

		this.imageA.registerVOI(left);
		this.imageA.registerVOI(right);
		showLatticeLabels(false);
		
		updateLattice(true);
	}

	/**
	 * Add an annotation to the annotation list.
	 * 
	 * @param textVOI
	 */
	public void addAnnotation(final VOI textVOI, boolean multiSelect ) {
		if (annotationVOIs == null) {
			final int colorID = 0;
			annotationVOIs = new VOI((short) colorID, "annotationVOIs", VOI.ANNOTATION, -1.0f);
			imageA.registerVOI(annotationVOIs);
		}
		VOIWormAnnotation newText = new VOIWormAnnotation( (VOIText)textVOI.getCurves().firstElement());
		addAnnotation(newText, multiSelect);
	}
	
	public void addAnnotation( VOIWormAnnotation newText, boolean multiSelect ) {
		newText.firstElement().X = Math.round( newText.firstElement().X );	newText.firstElement().Y = Math.round( newText.firstElement().Y );  newText.firstElement().Z = Math.round( newText.firstElement().Z );
		newText.lastElement().X  = Math.round( newText.lastElement().X );	newText.lastElement().Y  = Math.round( newText.lastElement().Y );   newText.lastElement().Z  = Math.round( newText.lastElement().Z );
		Color c = newText.getColor();
		newText.update(new ColorRGBA(c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, 1f));
		newText.setUseMarker(false);
		newText.retwist(previewMode);
		annotationVOIs.getCurves().add(newText);
		annotationVOIs.setColor(c);

		// checks if the annotation is the worm nose or the origin position:
		if (newText.getText().equalsIgnoreCase("nose") || newText.getText().equalsIgnoreCase("origin")) {
			if (wormOrigin == null) {
				wormOrigin = new Vector3f(newText.elementAt(0));
			} else {
				wormOrigin.copy(newText.elementAt(0));
			}
		}
		// set the annotation colors, if necessary
		colorAnnotations();
		// update which annotations are selected:
		newText.setSelected(true);
		newText.updateSelected(imageA);
		Vector3f pt = newText.elementAt(0);
		for ( int i = 0; i < annotationVOIs.getCurves().size(); i++ )
		{
			VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
			if ( text != newText ) {
				// set selection to false of not using multiSelect
				text.setSelected( text.isSelected() && multiSelect );
				text.updateSelected(imageA);
			}
			if ( text.isSelected() ) {
				text.setSelectionOffset( Vector3f.sub(pt, text.elementAt(0)) );
			}
		}
		imageA.notifyImageDisplayListeners();

		// update the annotation listeners to changes:
		updateAnnotationListeners();
	}

	public void removeListeners()
	{
		if ( annotationListeners != null ) annotationListeners.clear();
		if ( curveListeners != null ) curveListeners.clear();
		if ( latticeListeners != null ) latticeListeners.clear();
	}
	
	/**
	 * Adds an annotation listener. The annotation listeners are updated when
	 * the annotations change in any way.
	 * @param listener
	 */
	public void addAnnotationListener( AnnotationListener listener )
	{
		if ( annotationListeners == null ) {
			annotationListeners = new Vector<AnnotationListener>();
		}
		if ( !annotationListeners.contains(listener ) ) {
			annotationListeners.add(listener);
		}
	}

	/**
	 * Add a curve to the list.
	 * 
	 * @param textVOI
	 */
	public void addSplineControlPts( Vector<VOIWormAnnotation> controlPts ) {
		if ( controlPts.size() < 2 ) return;
		
		if ( splineControlPtsList == null ) {
			splineControlPtsList = new Vector<AnnotationSplineControlPts>();
		}
		
		// make the curve a spline and display:
		AnnotationSplineControlPts annotationSpline = new AnnotationSplineControlPts();
		splineControlPtsList.add(annotationSpline);
		
		annotationSpline.annotations = controlPts;
		// generate curve:
		NaturalSpline3 spline = smoothCurve(annotationSpline.annotations);

		//display:
		annotationSpline.prefix = getPrefix(controlPts.elementAt(0).getText());
		annotationSpline.curve = new VOIContour(false);
		float length = spline.GetLength(0, 1);
		int maxLength = (int)Math.ceil(length);
		float step = 1;
		if ( maxLength != length )
		{
			step = (length / maxLength);
		}
		for (int i = 0; i <= maxLength; i++) {
			final float t = spline.GetTime(i*step);
			annotationSpline.curve.add(spline.GetPosition(t));
		}
		annotationSpline.name = new String( annotationSpline.prefix );
		annotationSpline.curveVOI = new VOI((short)splineControlPtsList.size(), annotationSpline.name );
		annotationSpline.curveVOI.getCurves().add(annotationSpline.curve);
		annotationSpline.curveVOI.setColor(Color.red);
		annotationSpline.curve.update(new ColorRGBA(1, 0, 0, 1));
		imageA.registerVOI(annotationSpline.curveVOI);
		
		imageA.notifyImageDisplayListeners();

		// update the annotation listeners to changes:
		updateCurveListeners();
	}

	public void addCurveListener( CurveListener listener )
	{
		if ( curveListeners == null ) {
			curveListeners = new Vector<CurveListener>();
		}
		if ( !curveListeners.contains(listener ) ) {
			curveListeners.add(listener);
		}
	}

	public void addLatticeListener( LatticeListener listener )
	{
		if ( latticeListeners == null ) {
			latticeListeners = new Vector<LatticeListener>();
		}
		if ( !latticeListeners.contains(listener ) ) {
			latticeListeners.add(listener);
		}
	}

	/**
	 * Adds a new left/right marker to the worm image.
	 * 
	 * @param pt
	 */
	public void addLeftRightMarker(final Vector3f pt, boolean isSeam ) {
		if (lattice == null) {
			lattice = new VOIVector();
		}
		
		if (editingCrossSections) {
			// do not add leftRight markers when editing cross sections
			return;
		}
		
		if ( left == null ) {
			short id = (short) imageA.getVOIs().getUniqueID();
			left = new VOI(id, "left", VOI.ANNOTATION, (float) Math.random());
			right = new VOI(id++, "right", VOI.ANNOTATION, (float) Math.random());
			lattice.add(left);
			lattice.add(right);

			imageA.registerVOI(left);
			imageA.registerVOI(right);
			showLatticeLabels(false);
		}
		int seamCount = 0;
		int otherCount = 0;
		for ( int i = 0; i < left.getCurves().size(); i++ ) {
			if ( ((VOIWormAnnotation)left.getCurves().elementAt(i)).isSeamCell() ) {
				seamCount++;
			}
			else {
				otherCount++;
			}
		}
		if (left.getCurves().size() == right.getCurves().size()) {
			VOIWormAnnotation annotation = new VOIWormAnnotation(new Vector3f(pt));
			annotation.setSeamCell(isSeam);
			if ( isSeam ) {
				String name = seamCount < 3 ? ("H" + seamCount) : (seamCount < 9) ? ("V" + (seamCount - 2)) : "T";
				annotation.setText(name + "L");
			}
			else {
				String name = "a" + otherCount;
				annotation.setText(name + "L");
			}
			left.getCurves().add(annotation);
			pickedPoint = left.getCurves().lastElement().elementAt(0);
			// System.err.println( pt );

			if (leftMarker == null) {
				final short id = (short) imageA.getVOIs().getUniqueID();
				leftMarker = new VOI(id, "leftMarker", VOI.POINT, (float) Math.random());
				this.imageA.registerVOI(leftMarker);
				leftMarker.importPoint(pt);
			} else {
				leftMarker.getCurves().elementAt(0).elementAt(0).copy(pt);
				leftMarker.update();
			}
			return;
		} else {
			// adding to the right side - keep the last count values:
			seamCount--;
			otherCount--;
			VOIWormAnnotation annotation = new VOIWormAnnotation(new Vector3f(pt));
			annotation.setSeamCell(isSeam);
			if ( isSeam ) {
				String name = seamCount < 3 ? ("H" + seamCount) : (seamCount < 9) ? ("V" + (seamCount - 2)) : "T";
				annotation.setText(name + "R");
			}
			else {
				String name = "a" + otherCount;
				annotation.setText(name + "R");
			}
			right.getCurves().add(annotation);
			pickedPoint = right.getCurves().lastElement().elementAt(0);
			// System.err.println( pt );

			if (rightMarker == null) {
				final short id = (short) imageA.getVOIs().getUniqueID();
				rightMarker = new VOI(id, "rightMarker", VOI.POINT, (float) Math.random());
				this.imageA.registerVOI(rightMarker);
				rightMarker.importPoint(pt);
			} else {
				rightMarker.getCurves().elementAt(0).elementAt(0).copy(pt);
				rightMarker.update();
			}
		}
		// rename cells if seam count > 10 to include Q cells:
		updateSeamCount();
		updateLattice(true);
		showLatticeLabels(displayLatticeLabels);
	}

	/**
	 * Generates a natural spline curve to fit the input set of annotation points to model a neurite.
	 */
	public void addNeurite( VOI annotionVOI, String name ) {
		short sID;

		// 1. The center line of the worm is calculated from the midpoint between the left and right points of the
		// lattice.
		VOIContour neurite = new VOIContour(false);
		for (int i = 0; i < annotionVOI.getCurves().size(); i++) {
			VOIWormAnnotation text = (VOIWormAnnotation) annotionVOI.getCurves().elementAt(i);
			neurite.add( new Vector3f( text.elementAt(0) ) );
		}
		float[] time = new float[neurite.size()];
		NaturalSpline3 neuriteSpline = smoothCurve(neurite, time);

		VOIContour neuriterPositions = new VOIContour(false);

		float length = neuriteSpline.GetLength(0, 1);
		for (int i = 0; i <= length; i++) {
			final float t = neuriteSpline.GetTime(i);
			neuriterPositions.add(neuriteSpline.GetPosition(t));
		}

		sID = (short) (imageA.getVOIs().getUniqueID());
		VOI neuriteVOI = new VOI(sID, name, VOI.POLYLINE, (float) Math.random() );
		neuriteVOI.getCurves().add(neuriterPositions);
		neuriteVOI.setColor(Color.white);
		neuriterPositions.update(new ColorRGBA(1, 1, 1, 1));

		if ( neuriteData == null )
		{
			neuriteData = new Vector<VOI>();
		}
		for ( int i = 0; i < neuriteData.size(); i++ )
		{
			if ( neuriteData.elementAt(i).getName().equals(name) )
			{
				imageA.unregisterVOI( neuriteData.remove(i) );
				break;
			}
		}
		neuriteData.add(neuriteVOI);
		imageA.registerVOI(neuriteVOI);
	}


	/**
	 * Clears the selected VOI or Annotation point.
	 */
	public void clear3DSelection() {
		if ( (annotationVOIs != null) && (annotationVOIs.getCurves() != null) ) {
			for ( int i = 0; i < annotationVOIs.getCurves().size(); i++ ) {
				final VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
				text.setSelected(false);
				text.updateSelected(imageA);
			}
		}
		pickedPoint = null;
		if (showSelected != null) {
			imageA.unregisterVOI(showSelectedVOI);
		}
		if (showSelected != null) {
			for (int i = 0; i < showSelected.length; i++) {
				showSelected[i].dispose();
			}
			showSelected = null;
		}
		showSelectedVOI = null;
		imageA.notifyImageDisplayListeners();
	}

	/**
	 * Enables user to start editing the lattice.
	 */
	public void clearAddLeftRightMarkers() {
		imageA.unregisterVOI(leftMarker);
		imageA.unregisterVOI(rightMarker);
		if (leftMarker != null) {
			leftMarker.dispose();
			leftMarker = null;
		}
		if (rightMarker != null) {
			rightMarker.dispose();
			rightMarker = null;
		}
	}

	public void colorAnnotations( boolean setColor )
	{
		colorAnnotations = setColor;
		colorAnnotations();
	}

	public void deleteAnnotations() {
		clear3DSelection();
		if (annotationVOIs != null) {
			imageA.unregisterVOI(annotationVOIs);
		}
		annotationVOIs = null;
		updateAnnotationListeners();
	}

	/**
	 * Deletes the selected annotation or lattice point.
	 * 
	 * @param doAnnotation
	 */
	public void deleteSelectedPoint(final boolean doAnnotation) {
		if (doAnnotation)
		{
			for ( int i = annotationVOIs.getCurves().size()-1; i >= 0; i-- ) {
				final VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
				if ( text.isSelected() ) {
					annotationVOIs.getCurves().remove(i);
					text.setSelected(false);
					text.updateSelected(imageA);

					if (text.getText().equalsIgnoreCase("nose") || text.getText().equalsIgnoreCase("origin")) {
						wormOrigin = null;
					}
				}
			}
			colorAnnotations();
			updateAnnotationListeners();
		}
		else if ( !doAnnotation && !previewMode )
		{
			boolean deletedLeft = false;
			boolean deletedRight = false;
			if ( (rightMarker != null) && pickedPoint.equals(rightMarker.getCurves().elementAt(0).elementAt(0))) {
				imageA.unregisterVOI(rightMarker);
				rightMarker.dispose();
				rightMarker = null;

				deletedRight = true;
			}

			if ( (leftMarker != null) && pickedPoint.equals(leftMarker.getCurves().elementAt(0).elementAt(0))) {
				imageA.unregisterVOI(leftMarker);
				leftMarker.dispose();
				leftMarker = null;
				deletedLeft = true;

				if (rightMarker != null) {
					imageA.unregisterVOI(rightMarker);
					rightMarker.dispose();
					rightMarker = null;
					deletedRight = true;
				}
			}
			if (deletedLeft || deletedRight) {
				if (deletedLeft) {
					left.getCurves().remove(left.getCurves().lastElement());
				}
				if (deletedRight) {
					right.getCurves().remove(right.getCurves().lastElement());
				}
			} else {
				final int leftIndex = findPoint(left, pickedPoint);
				final int rightIndex = findPoint(right, pickedPoint);
				if (leftIndex != -1) {
					left.getCurves().remove(leftIndex);
					right.getCurves().remove(leftIndex);
					deletedLeft = true;
					deletedRight = true;
				} else if (rightIndex != -1) {
					left.getCurves().remove(rightIndex);
					right.getCurves().remove(rightIndex);
					deletedLeft = true;
					deletedRight = true;
				}
			}
			clear3DSelection();
			updateLattice(deletedLeft | deletedRight);
		}
		pickedPoint = null;
	}

	public void displayAnnotation( String name, boolean display ) {
		if ( annotationVOIs == null ) {
			return;
		}
		for ( int i = 0; i < annotationVOIs.getCurves().size(); i++ )
		{
			VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
			if ( text.getText().equals(name) ) {
				text.display(display);
			}
		}
		updateAnnotationListeners();
	}


	/**
	 * Deletes this LatticeModel
	 */
	public void dispose() {
		if ( imageA != null )
		{
			if (latticeGrid != null) {
				for (int i = latticeGrid.size() - 1; i >= 0; i--) {
					final VOI marker = latticeGrid.remove(i);
					imageA.unregisterVOI(marker);
				}
			}
			
			for ( int i = 0; i < lattice.size(); i++ ) {
				imageA.unregisterVOI(lattice.elementAt(i));
			}
			for ( int i = 0; i < displayContours.length; i++ ) {
				imageA.unregisterVOI(displayContours[i]);
			}
			imageA.unregisterVOI(leftLine);
			imageA.unregisterVOI(rightLine);
			imageA.unregisterVOI(centerLine);
			imageA.unregisterVOI(leftContour);
			imageA.unregisterVOI(rightContour);
			clear3DSelection();
		}

		imageA = null;
		latticeGrid = null;
		lattice = null;
		left = null;
		right = null;
		leftContour = null;
		rightContour = null;
		center = null;
		afTimeC = null;
		allTimes = null;
		centerSpline = null;
		centerPositions = null;
		leftPositions = null;
		rightPositions = null;
		leftLine = null;
		rightLine = null;
		centerLine = null;

		// if ( centerTangents != null )
		// centerTangents.clear();
		// centerTangents = null;

		if (wormDiameters != null) {
			wormDiameters.clear();
		}
		wormDiameters = null;

		if (rightVectors != null) {
			rightVectors.clear();
		}
		rightVectors = null;

		if (upVectors != null) {
			upVectors.clear();
		}
		upVectors = null;

		if (boxBounds != null) {
			boxBounds.clear();
		}
		boxBounds = null;

		if (ellipseBounds != null) {
			ellipseBounds.clear();
		}
		ellipseBounds = null;

		samplingPlanes = null;
		displayContours = null;
		pickedPoint = null;
		showSelectedVOI = null;
		showSelected = null;
	}

	public void flipLattice() {
		for ( int i = left.getCurves().size() - 1; i >= 0; i-- ) {
			VOIWormAnnotation textL = (VOIWormAnnotation) left.getCurves().remove(i);
			VOIWormAnnotation textR = (VOIWormAnnotation) right.getCurves().remove(i);
			
			String labelL2R = textL.getText();
			labelL2R = labelL2R.replace("L", "R");
			textL.setText(labelL2R);
			textL.updateText();
			textL.update();
			
			String labelR2L = textR.getText();
			labelR2L = labelR2L.replace("R", "L");			
			textR.setText(labelR2L);
			textR.updateText();
			textR.update();
			
			textL.retwist(previewMode);
			textR.retwist(previewMode);
			
			left.getCurves().add(i, textR );
			right.getCurves().add(i, textL );
		}
		
		updateLattice(true);
	}
	
	public static void flipLattice(VOI left, VOI right) {
		for ( int i = left.getCurves().size() - 1; i >= 0; i-- ) {
			VOIWormAnnotation textL = (VOIWormAnnotation) left.getCurves().remove(i);
			VOIWormAnnotation textR = (VOIWormAnnotation) right.getCurves().remove(i);
			
			String labelL2R = textL.getText();
			labelL2R = labelL2R.replace("L", "R");
			textL.setText(labelL2R);
			textL.updateText();
			textL.update();
			
			String labelR2L = textR.getText();
			labelR2L = labelR2L.replace("R", "L");			
			textR.setText(labelR2L);
			textR.updateText();
			textR.update();
						
			left.getCurves().add(i, textR );
			right.getCurves().add(i, textL );
		}
	}

	
	private int numSurfaceSegments = -1;
	private TriMesh currentMesh = null;
	public TriMesh generateTriMesh( boolean returnMesh, boolean saveMesh ) {		
		maxSplineLength = displayContours2.getCurves().size();
		
		TriMesh mesh = null;
		if ( (numSurfaceSegments == maxSplineLength) && (currentMesh != null) && (currentMesh.VBuffer != null) ) {
			// modify existing mesh:
			mesh = currentMesh;
			VertexBuffer vBuf = mesh.VBuffer;
			int positionCount = 1;
			System.err.println("Modify mesh " + maxSplineLength );

			for ( int i = 0; i < maxSplineLength; i++ ) {
				VOIContour contour = (VOIContour) displayContours2.getCurves().elementAt(i);
				Vector3f center = new Vector3f();
				int contourSize = contour.size();
				for ( int j = 0; j < contourSize; j++ )
				{
					if ( (i == 0) || (i == (maxSplineLength-1) ) ) {
						center.add(contour.elementAt(j));
					}
					vBuf.SetPosition3( positionCount, contour.elementAt(j) );
					vBuf.SetTCoord3(0, positionCount, contour.elementAt(j) );
					positionCount++;
				}
				if ( i == 0 ) {
					center.scale(1.0f/contourSize);
					// first positions is center of 'head'
					vBuf.SetPosition3( 0, center );
					vBuf.SetTCoord3(0, 0, center );
				}
				if ( i == (maxSplineLength-1) ) {
					center.scale(1.0f/contourSize);
					// last position is center of 'tail'
					vBuf.SetPosition3( positionCount, center );
					vBuf.SetTCoord3(0, positionCount, center );
				}
			}
			System.err.println("Reload TriMesh");

			mesh.Reload(true);
		}
		else {
			// rebuild mesh:
			numSurfaceSegments = maxSplineLength;

			Vector<Vector3f> positions = new Vector<Vector3f>();
			for ( int i = 0; i < maxSplineLength; i++ ) {
				VOIContour contour = (VOIContour) displayContours2.getCurves().elementAt(i);
				Vector3f center = new Vector3f();
				int contourSize = contour.size();
				for ( int j = 0; j < contourSize; j++ )
				{
					if ( (i == 0) || (i == (maxSplineLength-1) ) ) {
						center.add(contour.elementAt(j));
					}
					positions.add(contour.elementAt(j));
				}
				if ( i == 0 ) {
					center.scale(1.0f/contourSize);
					// first positions is center of 'head'
					positions.add(0, center);
				}
				if ( i == (maxSplineLength-1) ) {
					center.scale(1.0f/contourSize);
					// last position is center of 'tail'
					positions.add(center);
				}
			}
			VertexBuffer vBuf = new VertexBuffer(positions);

			Vector<Integer> indexList = new Vector<Integer>();
			for ( int i = 0; i < maxSplineLength; i++ ) {
				for ( int j = 0; j < numEllipsePts; j++ ) {
                    if ( i == 0 ) {
						int index = 0;
						int sliceIndex1 = 1 + j;
						int sliceIndex2 = 1 + (j+1)%numEllipsePts;
						indexList.add(index);
						indexList.add(sliceIndex2);
						indexList.add(sliceIndex1);
					}
					if ( i < (maxSplineLength - 1) ) {
						int sliceIndexJ = 1 + (i)*numEllipsePts + j;
						int nextSliceIndexJ = 1 + (i+1)*numEllipsePts + j;
						int sliceIndexJP1 = 1 + (i)*numEllipsePts + (j+1)%numEllipsePts;
						int nextSliceIndexJP1 = 1 + (i+1)*numEllipsePts + (j+1)%numEllipsePts;
						// tri 1:
						indexList.add(sliceIndexJ);
						indexList.add(nextSliceIndexJP1);
						indexList.add(nextSliceIndexJ);
						// tri 2:
						indexList.add(sliceIndexJ);
						indexList.add(sliceIndexJP1);
						indexList.add(nextSliceIndexJP1);
					}
					if ( i == (maxSplineLength-1) ) {
						int index = positions.size()-1;
						int sliceIndex1 = 1 + (i)*numEllipsePts + j;
						int sliceIndex2 = 1 + (i)*numEllipsePts + (j+1)%numEllipsePts;
						indexList.add(index);
						indexList.add(sliceIndex1);
						indexList.add(sliceIndex2);
					}
				}
			}

			int[] indexInput = new int[indexList.size()];
			for ( int i = 0; i < indexList.size(); i++ ) {
				indexInput[i] = indexList.elementAt(i);
			}
			IndexBuffer iBuf = new IndexBuffer(indexInput);
			System.err.println("Rebuild TriMesh " + vBuf.GetVertexQuantity() + " " + iBuf.GetIndexQuantity() );
			currentMesh = new TriMesh(vBuf, iBuf);	
            mesh = currentMesh;			
		}

		if ( returnMesh ) {
			return mesh;
		}
		return null;
	}
		
	public TriMesh generateTriMesh2( boolean returnMesh, boolean saveMesh, int stepSize ) {
		
		saveLattice( sharedOutputDir + File.separator + WormData.editLatticeOutput + File.separator, "lattice.csv" );
		readMeshContoursCSV();
		
		Vector<Vector3f> positions = new Vector<Vector3f>();
		int numContours = displayContours2.getCurves().size();
		for ( int i = 0; i < numContours; i++ ) {
			VOIContour contour = (VOIContour) displayContours2.getCurves().elementAt(i);
			Vector3f min = new Vector3f( contour.elementAt(0) );
			Vector3f max = new Vector3f( contour.elementAt(0) );
			Vector3f center = new Vector3f();
			int contourSize = contour.size();
			for ( int j = 0; j < contourSize; j++ )
			{
				min.min(contour.elementAt(j));
				max.max(contour.elementAt(j));
				if ( (i == 0) || (i == (numContours-1) ) ) {
					center.add(contour.elementAt(j));
				}
				positions.add(contour.elementAt(j));
			}
			if ( i == 0 ) {
				center.scale(1.0f/contourSize);
				// first positions is center of 'head'
				positions.add(0, center);
			}
			if ( i == (numContours-1) ) {
				center.scale(1.0f/contourSize);
				// last position is center of 'tail'
				positions.add(center);
			}
		}
		VertexBuffer vBuf = new VertexBuffer(positions);

		Box3f[] contourBoxes = new Box3f[numContours-1];
		Box3f[][] boxes = new Box3f[numContours-1][numEllipsePts];
		System.err.println( "TriMesh " + numContours );
		Vector<Integer> indexList = new Vector<Integer>();
		for ( int i = 0; i < numContours; i++ ) {
			VOIContour contour1 = (VOIContour) displayContours2.getCurves().elementAt(i);
			int contourSize1 = contour1.size();
			Vector3f[] boxPts = new Vector3f[contourSize1 * 2];
			for ( int j = 0; j < contourSize1; j++ )
			{
				if ( i == 0 ) {
					int index = 0;
					int sliceIndex1 = positions.indexOf(contour1.elementAt(j) );
					int sliceIndex2 = positions.indexOf( contour1.elementAt((j+1)%contourSize1) );
					indexList.add(index);
					indexList.add(sliceIndex2);
					indexList.add(sliceIndex1);
				}
				if ( i == (numContours-1) ) {
					int index = positions.size()-1;
					int sliceIndex1 = positions.indexOf(contour1.elementAt(j) );
					int sliceIndex2 = positions.indexOf( contour1.elementAt((j+1)%contourSize1) );
					indexList.add(index);
					indexList.add(sliceIndex1);
					indexList.add(sliceIndex2);
				}
				if ( i < (numContours - 1) ) {
					VOIContour contour2 = (VOIContour) displayContours2.getCurves().elementAt(i+1);
					int contourSize2 = contour2.size();

					for ( int k = 0; k < contourSize1; k++ ) {
						boxPts[k] = contour1.elementAt(k);
					}
					for ( int k = 0; k < contourSize2; k++ ) {
						boxPts[contourSize1 + k] = contour2.elementAt(k);
					}
					contourBoxes[i] = ContBox3f.ContAlignedBox( boxPts.length, boxPts );
					
					//					System.err.println( i + "   " + (contourSize1 == contourSize2) );
					int sliceIndexJ = positions.indexOf(contour1.elementAt(j) );
					int nextSliceIndexJ = positions.indexOf( contour2.elementAt(j) );
					int sliceIndexJP1 = positions.indexOf(contour1.elementAt((j+1)%contourSize1) );
					int nextSliceIndexJP1 = positions.indexOf( contour2.elementAt((j+1)%contourSize2) );
					// tri 1:
					indexList.add(sliceIndexJ);
					indexList.add(nextSliceIndexJP1);
					indexList.add(nextSliceIndexJ);
					// tri 2:
					indexList.add(sliceIndexJ);
					indexList.add(sliceIndexJP1);
					indexList.add(nextSliceIndexJP1);

					Vector3f center = new Vector3f();
					center.add(contour1.elementAt(j));
					center.add(contour2.elementAt(j));
					center.add(contour2.elementAt((j+1)%contourSize2));
					center.add(contour1.elementAt((j+1)%contourSize1));
					center.scale(0.25f);


					Vector3f edge1 = Vector3f.sub(contour1.elementAt((j+1)%contourSize1), contour1.elementAt(j));
					float dist1 = edge1.normalize();

					Vector3f edge2 = Vector3f.sub(contour2.elementAt((j+1)%contourSize2), contour1.elementAt(j));
					float dist2 = edge2.normalize();

					Vector3f edge3 = Vector3f.sub(contour2.elementAt(j), contour1.elementAt(j));
					float dist3 = edge3.normalize();

					Vector3f edge4 = Vector3f.sub(contour2.elementAt((j+1)%contourSize2), contour1.elementAt((j+1)%contourSize1));
					float dist4 = edge4.normalize();

					Vector3f edge5 = Vector3f.sub(contour2.elementAt((j+1)%contourSize2), contour2.elementAt(j));
					float dist5 = edge5.normalize();

					Vector3f normal1 = edge1.cross(edge2); normal1.normalize();
					Vector3f normal2 = edge2.cross(edge3); normal2.normalize();
					Vector3f normal = Vector3f.add(normal1, normal2);
					normal.scale(0.5f);

					Vector3f axis1 = Vector3f.add(edge1, edge5);
					axis1.scale(0.5f);
					float extent1 = (dist1 + dist5)/4f;

					Vector3f axis2 = Vector3f.add(edge3, edge4);
					axis2.scale(0.5f);
					float extent2 = (dist3 + dist4)/4f;

					Box3f box = new Box3f(center, new Vector3f[] {axis1, axis2, normal }, new float[] {extent1, extent2, 1} );
					boxes[i][j] = box;
				}
			}
		}
		
		// save contour boxes:
		saveContours( imageA, contourBoxes );


		clipMask = new Vector<boolean[]>();
		int boxIndex = 0;
		boolean[] currentMask = null;
		boolean[] nextMask = null;
		for ( int i = 0; i < numContours; i++ ) {
			VOIContour contour1 = (VOIContour) displayContours2.getCurves().elementAt(i);
			int contourSize1 = contour1.size();
			if ( i == 0 )
			{
				currentMask = new boolean[contourSize1];
				nextMask = new boolean[contourSize1];
			}
			else
			{
				currentMask = nextMask;
				nextMask = new boolean[contourSize1];
			}
			for ( int j = 0; j < contourSize1; j++ )
			{				
				if ( i < (numContours - 1) ) {
					VOIContour contour2 = (VOIContour) displayContours2.getCurves().elementAt(i+1);
					int contourSize2 = contour2.size();

					Vector3f posJ = contour1.elementAt(j);
					Vector3f posJNext = contour2.elementAt(j);
					Vector3f posJP1 = contour1.elementAt((j+1)%contourSize1);
					Vector3f posJP1Next =contour2.elementAt((j+1)%contourSize2);

					int sliceIndexJ = positions.indexOf( posJ );
					int nextSliceIndexJ = positions.indexOf( posJNext );
					int sliceIndexJP1 = positions.indexOf( posJP1 );
					int nextSliceIndexJP1 = positions.indexOf( posJP1Next );

					boolean test = false;
					if ( !test ) {
						Vector3f center = new Vector3f();
						center.add(contour1.elementAt(j));
						center.add(contour2.elementAt(j));
						center.add(contour2.elementAt((j+1)%contourSize2));
						center.add(contour1.elementAt((j+1)%contourSize1));
						center.scale(0.25f);

						Box3f box = boxes[i][j];
						Vector3f normal = box.Axis[2];

						Segment3f segment = new Segment3f();
						segment.Center.scaleAdd(50, normal, center);
						segment.Direction = normal;
						segment.Extent = 50;
						for ( int k = 0; k < boxes.length; k++ ) {
							if ( (k < (i-1)) || (k > (i+1)) ) 
							{
								for ( int b = 0; b < boxes[k].length; b++ ) {
									Box3f testBox = boxes[k][b];
									if ( testBox != box ) {
										IntrSegment3Box3f testSB = new IntrSegment3Box3f(segment,testBox,true);
										if ( testSB.Test() ) {
											vBuf.SetColor3(0, sliceIndexJ, 1, 0, 0);
											vBuf.SetColor3(0, nextSliceIndexJ, 1, 0, 0);
											vBuf.SetColor3(0, sliceIndexJP1, 1, 0, 0);
											vBuf.SetColor3(0, nextSliceIndexJP1, 1, 0, 0);
											currentMask[j] = true;
											currentMask[(j+1)%contourSize1] = true;
											nextMask[j] = true;
											nextMask[(j+1)%contourSize1] = true;
											break;
										}
									}
								}
							}
						}
					}
				}
			}
			for ( int j = 0; j < contourSize1; j++ )
			{				
				if ( currentMask[((j-1)+contourSize1)%contourSize1] && currentMask[(j+1)%contourSize1] ) 
				{
					currentMask[j] = true;
					nextMask[j] = true;

					if ( i < (numContours - 1) ) {
						VOIContour contour2 = (VOIContour) displayContours2.getCurves().elementAt(i+1);
						int contourSize2 = contour2.size();
						//					System.err.println( i + "   " + (contourSize1 == contourSize2) );
						int sliceIndexJ = positions.indexOf(contour1.elementAt(j) );
						int nextSliceIndexJ = positions.indexOf( contour2.elementAt(j) );
						int sliceIndexJP1 = positions.indexOf(contour1.elementAt((j+1)%contourSize1) );
						int nextSliceIndexJP1 = positions.indexOf( contour2.elementAt((j+1)%contourSize2) );

						vBuf.SetColor3(0, sliceIndexJ, 1, 0, 0);
						vBuf.SetColor3(0, nextSliceIndexJ, 1, 0, 0);
						vBuf.SetColor3(0, sliceIndexJP1, 1, 0, 0);
						vBuf.SetColor3(0, nextSliceIndexJP1, 1, 0, 0);
					}
				}
			}
			clipMask.add(currentMask);
		}

		if ( saveMesh || returnMesh ) {
			int[] indexInput = new int[indexList.size()];
			for ( int i = 0; i < indexList.size(); i++ ) {
				indexInput[i] = indexList.elementAt(i);
			}
			IndexBuffer iBuf = new IndexBuffer(indexInput);
			System.err.println("TriMesh " + vBuf.GetVertexQuantity() + " " + iBuf.GetIndexQuantity() );
			TriMesh mesh = new TriMesh(vBuf, iBuf);		
			
			if ( saveMesh ) {
				saveTriMesh( imageA, sharedOutputDir, "model", "_mesh_" + stepSize, mesh );
				
				// save the surface and interior images:
				ModelImage surfaceMaskImage = getInsideMeshImage(mesh);
				ModelImage.saveImage(surfaceMaskImage, getImageName(surfaceMaskImage) + ".tif", sharedOutputDir + File.separator + "model" + File.separator, false);

//				LatticeModel.saveImage(imageA, surfaceMaskImage, "model", "" );
				
//	        	SurfaceState surface = new SurfaceState( mesh, mesh.GetName() );
//	        	VolumeImage volImage = new VolumeImage(false, imageA, "", null, 0);
//				VolumeSurface volumeSurface = new VolumeSurface(volImage,
//						null, new Vector3f(), volImage.GetScaleX(), volImage.GetScaleY(), volImage.GetScaleZ(), surface, true);
//				BitSet surfaceMask = volumeSurface.computeSurfaceMask();
//				System.err.println("surfaceMask " + surfaceMask.cardinality() );
//				
//				ModelImage surfaceMaskImage = new ModelImage(ModelStorageBase.FLOAT, imageA.getExtents(), JDialogBase.makeImageName(imageA.getImageName(),  "_surface"));
//	            JDialogBase.updateFileInfo(imageA, surfaceMaskImage);
//
//	    		int dimX = imageA.getExtents().length > 0 ? imageA.getExtents()[0] : 1;
//	    		int dimY = imageA.getExtents().length > 1 ? imageA.getExtents()[1] : 1;		
//	    		int dimZ = imageA.getExtents().length > 2 ? imageA.getExtents()[2] : 1;		
//	    		for ( int z = 0; z < dimZ; z++ ) {
//	    			for ( int y = 0; y < dimY; y++ ) {
//	    				for ( int x = 0; x < dimX; x++ ) {
//	    					int index = x + (dimX * (y + (dimY * z)));
//	    					if ( surfaceMask.get(index) ) {
//								surfaceMaskImage.set(x, y, z, 1);
//								surfaceMaskImage.setMax(1);
//	    					}
//	    				}
//	    			}
//	    		}
//
//				surfaceMaskImage.setMin(0);
//				LatticeModel.saveImage(imageA, surfaceMaskImage, "model", "" );
				//							new ViewJFrameImage(surfaceMaskImage);

//				ModelImage surfaceBlur = WormSegmentation.blur(surfaceMaskImage, 1);
//				VOIContour centerCurve = getCenter();
//				Vector3f pt = centerCurve.elementAt( centerCurve.size() / 2 );
//
//				try {
//					AlgorithmRegionGrow regionGrowAlgo = new AlgorithmRegionGrow(surfaceBlur, 1.0f, 1.0f);
//
//					regionGrowAlgo.setRunningInSeparateThread(false);
//					CubeBounds regionGrowBounds= new CubeBounds(dimX, 0, dimY, 0, dimZ, 0);
//					BitSet seedPaintBitmap = new BitSet( dimX * dimY * dimZ );
//
//					int count = regionGrowAlgo.regionGrow3D(seedPaintBitmap, new Point3D((int)pt.X, (int)pt.Y, (int)pt.Z), -1,
//							false, false, null, 0, 0, -1, -1,
//							false, 0, regionGrowBounds);
//
//
//
//					ModelImage volMaskImage = new ModelImage(ModelStorageBase.FLOAT, imageA.getExtents(), JDialogBase.makeImageName(imageA.getImageName(),  "_interior"));
//					JDialogBase.updateFileInfo(imageA, volMaskImage);
//
//					for ( int z = 0; z < dimZ; z++ ) {
//						for ( int y = 0; y < dimY; y++ ) {
//							for ( int x = 0; x < dimX; x++ ) {
//								int index = x + (dimX * (y + (dimY * z)));
//								if ( seedPaintBitmap.get(index) ) {
//									volMaskImage.set(x, y, z, 1);
//									volMaskImage.setMax(1);
//								}
//							}
//						}
//					}
//
//					volMaskImage.setMin(0);
//					volMaskImage.setMax(1);
//					LatticeModel.saveImage(imageA, volMaskImage, "model", "" );
//					//									new ViewJFrameImage(volMaskImage);
//
//					if ( volMaskImage != null ) {
//						volMaskImage.disposeLocal(false);
//						volMaskImage = null;
//					}
//
//					regionGrowAlgo = null;
//
//
//				} catch (final OutOfMemoryError error) {
//		            System.gc();
//		            MipavUtil.displayError("Out of memory: ComponentEditImage.regionGrow");
//		        }
//
//		        if ( surfaceBlur != null ) {
//		        	surfaceBlur.disposeLocal(false);
//		        	surfaceBlur = null;
//		        }

		        if ( surfaceMaskImage != null ) {
		        	surfaceMaskImage.disposeLocal(false);
		        	surfaceMaskImage = null;
		        }

			}
			if ( returnMesh ) {
				return mesh;
			}
		}
		
		return null;
	}


	public String getAnnotationPrefix()
	{
//		System.err.println("getAnnotationPrefix " + annotationPrefix);
		return annotationPrefix;
	}

	public VOI getAnnotations() {
		return annotationVOIs;
	}

	public VOI getAnnotationsStraight()
	{
		return annotationsStraight;
	}
	
	public Vector<String> getSplineCurves() {
		if ( splineControlPtsList == null ) return null;
		Vector<String> curveNames = new Vector<String>();
		for ( int i = 0; i < splineControlPtsList.size(); i++ )
		{
			curveNames.add(splineControlPtsList.elementAt(i).name);
		}
		return curveNames;
	}

	public void deleteSelectedCurve()
	{
		if ( splineControlPtsList == null ) return;
		for ( int i = splineControlPtsList.size() - 1; i >= 0; i-- )
		{
			if ( splineControlPtsList.elementAt(i).selected )
			{
				AnnotationSplineControlPts splinePts = splineControlPtsList.remove(i);
				imageA.unregisterVOI(splinePts.curveVOI);
				splinePts.curveVOI.dispose();
				splinePts.curveVOI = null;
			}
		}
		updateAnnotationListeners();
		updateCurveListeners();
	}
	
	public boolean isCurveSelected(String name)
	{
		if ( splineControlPtsList == null ) return false;
		for ( int i = 0; i < splineControlPtsList.size(); i++ )
		{
			if ( name.equals(splineControlPtsList.elementAt(i).name) )
			{
				return splineControlPtsList.elementAt(i).selected;
			}
		}
		return false;
	}
	
	public void setCurveName(String oldName, String newName)
	{
		if ( splineControlPtsList == null ) return;
		for ( int i = 0; i < splineControlPtsList.size(); i++ )
		{
			if ( oldName.equals(splineControlPtsList.elementAt(i).name) )
			{
				splineControlPtsList.elementAt(i).name = new String(newName);
				splineControlPtsList.elementAt(i).curveVOI.setName(newName);
				break;
			}
		}
	}
	
	public void setCurveSelected(String name, boolean selected)
	{
		if ( splineControlPtsList == null ) return;
		for ( int i = 0; i < splineControlPtsList.size(); i++ )
		{
			if ( name.equals(splineControlPtsList.elementAt(i).name) )
			{
				splineControlPtsList.elementAt(i).selected = selected;
				for ( int j = 0; j < splineControlPtsList.elementAt(i).annotations.size(); j++ )
				{
					VOIWormAnnotation text = splineControlPtsList.elementAt(i).annotations.elementAt(j);
					text.setSelected( selected );
					text.updateSelected( imageA );
				}
				updateAnnotationListeners();
				break;
			}
		}
	}
	
	public void setCurveVisible(String name, boolean visible)
	{
		if ( splineControlPtsList == null ) return;
		for ( int i = 0; i < splineControlPtsList.size(); i++ )
		{
			if ( name.equals(splineControlPtsList.elementAt(i).name) )
			{
				VOIContour curve = splineControlPtsList.elementAt(i).curve;
				if ( curve.getVolumeVOI() != null ) {
					curve.getVolumeVOI().SetDisplay(visible);
				}
				break;
			}
		}
	}

	public int getCurrentIndex()
	{

		if ( annotationVOIs == null )
		{
			return 0;
		}
		int highestIndex = 0;
		for ( int i = 0; i < annotationVOIs.getCurves().size(); i++ )
		{
			VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);

			if ( !(text.getText().contains("nose") || text.getText().contains("Nose")) && !text.getText().equalsIgnoreCase("origin"))
			{
				int value = 0;
				String name = new String(text.getText());
				String prefix = JPanelAnnotations.getPrefix(name);
				if ( prefix.equals(annotationPrefix) ) {
					for ( int j = 0; j < name.length(); j++ )
					{
						if ( Character.isDigit(name.charAt(j)) )
						{
							value *= 10;
							value += Integer.valueOf(name.substring(j,j+1));
							//						System.err.println( name + " " + value + " " + name.substring(j,j+1) + " " + Integer.valueOf(name.substring(j,j+1)));
						}
					}
					highestIndex = Math.max( highestIndex, value );
				}
			}
		}

		return (highestIndex + 1);
	}


	public VOIVector getLattice() {
		return lattice;
	}

	public VOIVector getLatticeStraight()
	{
		return latticeStraight;
	}

	public VOIContour getCenter()
	{
		return centerPositions;
	}
	
	public VOI getPlanes()
	{
		if ( samplingPlanes == null ) {
			generateEllipses( extent / 2 );
		}
		return samplingPlanes;
	}
	
	public VOIContour[] getEllipseCurves()
	{
		return ellipseCurves;
	}

	
	public int getLatticeCurveLength()
	{
		if ( centerPositions == null ) return 0;
//		System.err.println("getLatticeCurveLength " + centerPositions.size());
		return centerPositions.size();
	}

	// return position
	public Vector3f getCenter(int i) {
		if ( centerPositions == null ) return null;
		return new Vector3f(centerPositions.elementAt(i));
	}
	
	private VOIWormAnnotation latticepositionMarker = null;
	public void showMarker(int i) {
		Vector3f pos = new Vector3f(centerPositions.elementAt(i));
		if ( latticepositionMarker == null ) {

			latticepositionMarker = new VOIWormAnnotation();
			latticepositionMarker.setText("latticeCenterLine");
			latticepositionMarker.add( new Vector3f(pos) );
			annotationVOIs.getCurves().add(latticepositionMarker);
		}
		latticepositionMarker.firstElement().copy(pos);
		latticepositionMarker.update();
	}
	
	public int getDiameter(int i) {
		return (int) (wormDiameters.elementAt(i).intValue());
	}
	
	// return lookat, up, right
	public Vector3f[] getBasisVectors(int i) {
		if ( centerPositions == null ) return null;
		return new Vector3f[] { 
				new Vector3f(rightVectors.elementAt(i)),
				new Vector3f(upVectors.elementAt(i)),
				new Vector3f(normalVectors.elementAt(i))};
	}

	public VOIContour getLeftCurve()
	{
		return leftPositions;
	}

	public VOIContour getRightCurve()
	{
		return rightPositions;
	}

	public VOIContour getCenterControlPoints()
	{
		return center;
	}

	public Vector<Vector3f> getNormalVectors()
	{
		return normalVectors;
	}

	public Vector<Vector3f> getRightVectors()
	{
		return rightVectors;
	}

	public Vector<Vector3f> getUpVectors()
	{
		return upVectors;
	}

	/**
	 * Returns the currently selected point, either on the lattice or from the
	 * annotation list. 
	 * @return
	 */
	public Vector3f getPicked() {
		return pickedPoint;
	}


	/**
	 * Finds the closest point to the input point and sets it as the currently selected lattice or annotation point.
	 * 
	 * @param pt
	 * @param doAnnotation
	 * @return
	 */
	public Vector3f getPicked(final Vector3f pt, final boolean doAnnotation) {
		pickedPoint = null;

		if (left == null) {
			return pickedPoint;
		}
		int closestL = -1;
		float minDistL = Float.MAX_VALUE;
		for (int i = 0; i < left.getCurves().size(); i++) {
			final float distance = pt.distance(left.getCurves().elementAt(i).elementAt(0));
			if (distance < minDistL) {
				minDistL = distance;
				if (minDistL <= 12) {
					closestL = i;
				}
			}
		}
		int closestR = -1;
		float minDistR = Float.MAX_VALUE;
		if (right != null) {
			for (int i = 0; i < right.getCurves().size(); i++) {
				final float distance = pt.distance(right.getCurves().elementAt(i).elementAt(0));
				if (distance < minDistR) {
					minDistR = distance;
					if (minDistR <= 12) {
						closestR = i;
					}
				}
			}
		}

		// System.err.println( minDistL + " " + minDistR );
		if ( (closestL != -1) && (closestR != -1)) {
			if (minDistL < minDistR) {
				// System.err.println( "Picked Lattice Left " + closestL );
				pickedPoint = left.getCurves().elementAt(closestL).elementAt(0);
			} else {
				// System.err.println( "Picked Lattice Right " + closestR );
				pickedPoint = right.getCurves().elementAt(closestR).elementAt(0);
			}
		} else if (closestL != -1) {
			// System.err.println( "Picked Lattice Left " + closestL );
			pickedPoint = left.getCurves().elementAt(closestL).elementAt(0);
		} else if (closestR != -1) {
			// System.err.println( "Picked Lattice Right " + closestR );
			pickedPoint = right.getCurves().elementAt(closestR).elementAt(0);
		}

		if (pickedPoint != null) {
			updateLattice(false);
		}

		return pickedPoint;
	}

	public Vector<VOIWormAnnotation> getPickedAnnotation() {
		if (annotationVOIs == null) {
			return null;
		}
		if (annotationVOIs.getCurves() == null) {
			return null;
		}
		Vector<VOIWormAnnotation> selectedAnnotations = null;
		for (int i = 0; i < annotationVOIs.getCurves().size(); i++) {
			VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
			if ( text.isSelected() ) {
				if ( selectedAnnotations == null ) {
					selectedAnnotations = new Vector<VOIWormAnnotation>();
				}
				selectedAnnotations.add(text);
			}
		}
		return selectedAnnotations;
	}
	
	public VOI getSamplingPlanes( boolean scale )
	{
		final short sID = (short) (imageA.getVOIs().getUniqueID());
		VOI samplingPlanes = new VOI(sID, "samplingPlanes");
		int localExtent = scale ? extent + 10 : extent;
		for (int i = 0; i < centerPositions.size(); i++) {
			final Vector3f rkEye = centerPositions.elementAt(i);
			final Vector3f rkRVector = rightVectors.elementAt(i);
			final Vector3f rkUVector = upVectors.elementAt(i);

			final Vector3f[] output = new Vector3f[4];
			final Vector3f rightV = Vector3f.scale(localExtent, rkRVector);
			final Vector3f upV = Vector3f.scale(localExtent, rkUVector);
			output[0] = Vector3f.add(Vector3f.neg(rightV), Vector3f.neg(upV));
			output[1] = Vector3f.add(rightV, Vector3f.neg(upV));
			output[2] = Vector3f.add(rightV, upV);
			output[3] = Vector3f.add(Vector3f.neg(rightV), upV);
			for (int j = 0; j < 4; j++) {
				output[j].add(rkEye);
			}
			final VOIContour kBox = new VOIContour(true);
			for (int j = 0; j < 4; j++) {
				kBox.addElement(output[j].X, output[j].Y, output[j].Z);
			}
			kBox.update(new ColorRGBA(0, 0, 1, 1));
			{
				samplingPlanes.getCurves().add(kBox);
				//				samplingPlanes.importCurve(kBox);
			}
		}
		return samplingPlanes;
	}

	public boolean hasPicked() {
		return ( (pickedPoint != null) || (getPickedAnnotation() != null) ) ;
	}

	public void initializeInterpolation( boolean saveStats) {
		latticeInterpolationInit = true;

		// The algorithm interpolates between the lattice points, creating two smooth curves from head to tail along
		// the left and right-hand sides of the worm body. A third curve down the center-line of the worm body is
		// also generated. Eventually, the center-line curve will be used to determine the number of sample points
		// along the length of the straightened worm, and therefore the final length of the straightened worm volume.
		generateCurves(1);
		generateEllipses(extent);
		if ( saveStats ) {
			saveLatticeStatistics();
		}
	}

	/**
	 * Entry point in the lattice-based straightening algorithm. At this point a lattice must be defined, outlining how
	 * the worm curves in 3D. A lattice is defined ad a VOI with two curves of equal length marking the left-hand and
	 * right-hand sides or the worm.
	 * 
	 * @param displayResult, when true intermediate volumes and results are displayed as well as the final straightened
	 *            image.
	 */
	public void interpolateLattice( final boolean displayResult, final boolean useModel, final boolean untwistImage, final boolean untwistMarkers) {

		if ( !latticeInterpolationInit ) {
			initializeInterpolation(true);
		}

		final int[] resultExtents = new int[] {((2 * extent)), ((2 * extent)), samplingPlanes.getCurves().size()};
		if ( untwistImage )
		{
			untwist(imageA, resultExtents, true);
			untwistLattice(imageA, resultExtents);
		}
		if ( untwistMarkers && (markerCenters != null) )
		{
			//			untwistMarkers(imageA);
			//			untwistMarkers(imageA, resultExtents, true);
			untwistMarkers(imageA, resultExtents);
		}
	}

	/**
	 * Enables the user to move an annotation point with the mouse.
	 * 
	 * @param startPt 3D start point of a ray intersecting the volume.
	 * @param endPt 3D end point of a ray intersecting the volume.
	 * @param pt point along the ray with the maximum intensity value.
	 */
	public boolean modifyAnnotation( final Vector3f pt ) {
		if ( annotationVOIs == null )
		{
			return false;
		}
		if ( annotationVOIs.getCurves() == null )
		{
			return false;
		}
		if ( annotationVOIs.getCurves().size() == 0 )
		{
			return false;
		}

		boolean modified = false;
		for ( int i = 0; i < annotationVOIs.getCurves().size(); i++ ) {
			final VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
			if ( text.isSelected() ) {
				Vector3f diff = text.getSelectionOffset();
				
				Vector3f newPt = Vector3f.sub(pt,  diff);
				newPt.max( Vector3f.ZERO );
				newPt.min( imageDims );

				text.elementAt(0).copy(newPt);
				text.elementAt(1).copy(newPt);
				
				
				text.update();
				text.updateSelected(imageA);
				// point was modified so set the retwist flag:
				text.retwist(previewMode);
				modified = true;
				
			}
		}
		
		if ( modified ) {
			updateAnnotationListeners();
		}

		return modified;
	}

	/**
	 * Enables the user to modify the lattice point with the mouse by dragging.
	 * 
	 * @param startPt 3D start point of a ray intersecting the volume.
	 * @param endPt 3D end point of a ray intersecting the volume.
	 * @param pt point along the ray with the maximum intensity value.
	 */
	public boolean modifyLattice(final Vector3f startPt, final Vector3f endPt, final Vector3f pt) {
		if ( lattice == null )
		{
			return false;
		}

		// if preview constrain lattice points to x,0,z coordinates:
		if (pickedPoint != null) {
			if ( previewMode ) {
				final int leftIndex = findPoint(left, pickedPoint);
				final int rightIndex = findPoint(right, pickedPoint);
				if ( leftIndex != -1 ) {
					((VOIWormAnnotation)left.getCurves().elementAt(leftIndex)).retwist(true);
				}
				if ( rightIndex != -1 ) {
					((VOIWormAnnotation)right.getCurves().elementAt(rightIndex)).retwist(true);
				}
				pickedPoint.X = pt.X;
				pickedPoint.Y = pt.Y;
				pickedPoint.Z = pt.Z;
				
			}
			else {
				if( editingCrossSections ) {
					Segment3f segment = new Segment3f(pickedPoint, pt);
					Vector3f direction = segment.Direction;
					//direction.normalize();
					Segment3f radialSegment = new Segment3f(center.elementAt(selectedSectionIndex), pickedPoint);
					Vector3f radialDirection = radialSegment.Direction;
					radialDirection.scale(radialDirection.dot(direction));
					radialDirection.scale(direction.length() / radialDirection.length());

					pickedPoint.add(radialDirection);
					
					/*
					sectionMarker.getCurves().clear();
					sectionMarker.importPoint(pickedPoint);
					sectionMarker.getCurves().elementAt(0).update(new ColorRGBA(1.0f, 0.0f, 1.0f, 1.0f));
					*/
					VOIBase selectedVOI = sectionMarker.getCurves().elementAt(0);
					selectedVOI.elementAt(0).copy(pickedPoint);
					colorSectionMarkerByCrossSectionSamples();
				} else {
//					pickedPoint.X = pt.X;
					pickedPoint.copy(pt);
					


				}
			}
			if(!editingCrossSections || updateCrossSectionOnDrag) {
				updateLattice(false);
			}
			return true;
		}
		return false;
	}

	/**
	 * Sets the currently selected point (lattice or annotation).
	 * 
	 * @param pt
	 * @param doAnnotation
	 */
	public void modifyLeftRightMarker(final Vector3f pt) {
		if (pickedPoint == null) {
			return;
		}
		pt.X = Math.round( pt.X );		pt.Y = Math.round( pt.Y );		pt.Z = Math.round( pt.Z );

		if ( (leftMarker != null) && pickedPoint.equals(leftMarker.getCurves().elementAt(0).elementAt(0))) {
			leftMarker.getCurves().elementAt(0).elementAt(0).copy(pt);
			leftMarker.update();
		}
		if ( (rightMarker != null) && pickedPoint.equals(rightMarker.getCurves().elementAt(0).elementAt(0))) {
			rightMarker.getCurves().elementAt(0).elementAt(0).copy(pt);
			rightMarker.update();
		}
		pickedPoint.copy(pt);
		updateLattice(false);
	}

	/**
	 * Enables the user to move the selected point (lattice or annotation) with the arrow keys.
	 * 
	 * @param direction
	 * @param doAnnotation
	 */
	public void moveSelectedPoint(final Vector3f direction, final boolean doAnnotation) {
		if ( doAnnotation ) {
			boolean updateImage = false;
			for ( int i = 0; i < annotationVOIs.getCurves().size(); i++ ) {
				final VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
				if ( text.isSelected() ) {
					text.elementAt(0).add(direction);
					text.elementAt(1).add(direction);
					text.update();

					text.updateSelected(imageA);
					text.retwist(previewMode);
					updateImage = true;
				}				
			}
			if ( updateImage )
			{
				imageA.notifyImageDisplayListeners();
			}
			updateAnnotationListeners();
		}
		else if ( pickedPoint != null ) {

			if ( previewMode ) {
				final int leftIndex = findPoint(left, pickedPoint);
				final int rightIndex = findPoint(right, pickedPoint);
				if ( leftIndex != -1 ) {
					((VOIWormAnnotation)left.getCurves().elementAt(leftIndex)).retwist(true);
				}
				if ( rightIndex != -1 ) {
					((VOIWormAnnotation)right.getCurves().elementAt(rightIndex)).retwist(true);
				}
//				pickedPoint.X += direction.X;
				pickedPoint.add(direction);
			} else if (editingCrossSections) {
				if(selectedSectionIndex != -1)
				{
					/*
					Vector3f up = new Vector3f();
					up.copy(upVectors.elementAt(selectedSectionIndex)).normalize();
					Vector3f right = new Vector3f();
					right.copy(rightVectors.elementAt(selectedSectionIndex)).normalize();
					*/
					
					Segment3f radialSegment = new Segment3f(center.elementAt(selectedSectionIndex), pickedPoint);
					Vector3f radialDirection = radialSegment.Direction;
					radialDirection.scale(radialDirection.dot(direction));
					radialDirection.scale(direction.length() / radialDirection.length());

					
					/*
					Vector3f sectionDirection = new Vector3f();
					sectionDirection.scaleAdd(up.dot(direction), up, right.scale(right.dot(direction)));
					*/
					pickedPoint.add(radialDirection);
				}
				
				if (sectionMarker.getCurves() != null) {
					VOIBase selectedVOI = sectionMarker.getCurves().elementAt(0);
					selectedVOI.elementAt(0).copy(pickedPoint);
					colorSectionMarkerByCrossSectionSamples();
					sectionMarker.update();
				}
				
				/*
				sectionMarker.getCurves().clear();
				sectionMarker.importPoint(pickedPoint);
				sectionMarker.getCurves().elementAt(0).update(new ColorRGBA(1.0f, 0.0f, 1.0f, 1.0f));
				/*
				
				/*
				
				Segment3f test = new Segment3f(centerPositions.elementAt(selectedSectionIndex), pickedPoint);
				//VOILine testVOILine = new VOILine(new Vector<Vector3f>(Arrays.asList(test.Direction)));
				//testVOILine.update(new ColorRGBA(0f,1f,1f,1f));
				//VOI testVOI = new VOI((short) 0, "testVOI", VOI.LINE, 0.5f);
				VOIContour testVOIContour = new VOIContour(false);
				testVOIContour.add(center.elementAt(selectedSectionIndex));
				testVOIContour.add(pickedPoint);
				VOI testVOI = new VOI((short) 0, "testVOI");
				testVOI.getCurves().add(testVOIContour);
				imageA.registerVOI(testVOI);
				
				*/
			}
			else {
				pickedPoint.add(direction);
			}
			updateLattice(false);
		}
	}

	/**
	 * VOI operation redo
	 */
	public void redo() {
		updateLinks();
	}


	public VOI retwistAnnotations( VOIVector lattice) {	
		
		setLattice(lattice);
		if ( annotationVOIs == null ) return null;
		for ( int i = annotationVOIs.getCurves().size() - 1; i >=0; i-- ) {

			VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
			if ( text.retwist() ) {
				if ( !latticeInterpolationInit ) {
					initializeInterpolation(false);
				}
				Vector3f retwistedPt = retwistAnnotation(text);
				text.firstElement().copy(retwistedPt);
				text.lastElement().copy(retwistedPt);
				text.modified(true);
				System.err.println( "RETWIST ANNOTATIONS " + text.getText() );
			}
		}	
		return annotationVOIs;
	}

	public VOIVector retwistLattice(VOIVector lattice) {
		
		VOIVector newLattice = null;
		VOI left3D = null;
		VOI right3D = null;

		short id = (short) imageA.getVOIs().getUniqueID();
		// retwist current lattice - using the input lattice:		
		for ( int i = 0; i < left.getCurves().size(); i++ ) {			
			boolean retwistLeft = ((VOIWormAnnotation)left.getCurves().elementAt(i)).retwist();
			boolean retwistRight = ((VOIWormAnnotation)right.getCurves().elementAt(i)).retwist();
			if ( retwistLeft || retwistRight) {
				if ( newLattice == null ) {
					// create a copy of the lattice so we can change it:
					newLattice = new VOIVector();
					left3D = new VOI(id, "left", VOI.ANNOTATION, (float) Math.random());
					right3D = new VOI(id++, "right", VOI.ANNOTATION, (float) Math.random());
					newLattice.add(left3D);
					newLattice.add(right3D);
				}
			}
		}
		if ( newLattice == null ) return null;
		
		for ( int i = 0; i < left.getCurves().size(); i++ ) {
			left3D.getCurves().add(new VOIWormAnnotation((VOIWormAnnotation)left.getCurves().elementAt(i)));
			right3D.getCurves().add(new VOIWormAnnotation((VOIWormAnnotation)right.getCurves().elementAt(i)));
		}
		
		// set the lattice:
		setLattice(lattice);
		// retwist copy (modified lattice):	
		for ( int i = 0; i < left3D.getCurves().size(); i++ ) {

			VOIWormAnnotation text = (VOIWormAnnotation) left3D.getCurves().elementAt(i);
//			System.err.print( i + "  " + text.getText() + "  " );
			if ( text.retwist() ) {
				if ( !latticeInterpolationInit ) {
					initializeInterpolation(false);
				}
				Vector3f retwistedPt = retwistAnnotation(text);
				text.clear();
				text.add(retwistedPt);
				text.modified(true);
			}
			else
			{
				text.clear();
				text.add(left.getCurves().elementAt(i).elementAt(0));
			}

			text = (VOIWormAnnotation) right3D.getCurves().elementAt(i);
//			System.err.println( text.getText() );
			if ( text.retwist() ) {
				if ( !latticeInterpolationInit ) {
					initializeInterpolation(false);
				}
				Vector3f retwistedPt = retwistAnnotation(text);
				text.clear();
				text.add(retwistedPt);
				text.modified(true);
			}
			else
			{
				text.clear();
				text.add(right.getCurves().elementAt(i).elementAt(0));
			}
		}
		
		return newLattice;
	}

	/**
	 * Enables the user to save the lattice to a user-selected file.
	 */
	public void saveLattice() {
		final JFileChooser chooser = new JFileChooser();

		if (ViewUserInterface.getReference().getDefaultDirectory() != null) {
			chooser.setCurrentDirectory(new File(ViewUserInterface.getReference().getDefaultDirectory()));
		} else {
			chooser.setCurrentDirectory(new File(System.getProperties().getProperty("user.dir")));
		}

		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		final int returnVal = chooser.showSaveDialog(null);

		String fileName = null, directory = null, voiDir;
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			fileName = chooser.getSelectedFile().getName();
			directory = String.valueOf(chooser.getCurrentDirectory()) + File.separatorChar;
			Preferences.setProperty(Preferences.PREF_VOI_LPS_SAVE, "true");
			Preferences.setProperty(Preferences.PREF_IMAGE_DIR, chooser.getCurrentDirectory().toString());
		}

		if (fileName != null) {
			voiDir = new String(directory + fileName + File.separator);

			clear3DSelection();

			VOI latticePoints = new VOI( (short)0, "lattice", VOI.ANNOTATION, 0);
			for ( int j = 0; j < left.getCurves().size(); j++ )
			{
				latticePoints.getCurves().add(left.getCurves().elementAt(j));
				latticePoints.getCurves().add(right.getCurves().elementAt(j));
			}
			LatticeModel.saveAnnotationsAsCSV(voiDir + File.separator, "lattice.csv", latticePoints);

			imageA.unregisterAllVOIs();
			
			if (leftMarker != null) {
				imageA.registerVOI(leftMarker);
			}
			if (rightMarker != null) {
				imageA.registerVOI(rightMarker);
			}
			if (annotationVOIs != null) {
				imageA.registerVOI(annotationVOIs);
			}
			updateLattice(true);
		}

	}

	/**
	 * Saves the lattice to the specified file and directory.
	 * 
	 * @param directory
	 * @param fileName
	 */
	public void saveLattice(final String directory, final String fileName)
	{
		if ( left == null || right == null ) return;
		boolean contourFile = getContourFile();
		boolean changed = latticeChanged();
		if ( changed  ) {
			saveLattice(directory, fileName, lattice );
		}
		
		if ( relativeCrossSections != null) {
			saveCrossSections();
		}
		//save cross sections here

		if ( (displayContours2 != null) || !contourFile) {
			generateCurves(1);
			saveMeshContoursCSV();
		}
		

	}
	
	/**
	 * Enables the user to save the lattice to a file.
	 * 
	 */
	public void saveCrossSections() {
		final String dir = sharedOutputDir + File.separator + "model_crossSections" + File.separator;

		saveCrossSections(dir);
	}
	
	public void saveCrossSections(final String dir) {
		System.out.println("Saving cross sections");
		updateRelativeCrossSectionsFromDisplayContours();
		for (int j = 0; j < latticeSlices.length; j++) {
			String outFileName = "latticeCrossSection_" + j + ".csv";
			if (editedCrossSections[j]) {
				LatticeModel.saveContourAsCSV(dir, outFileName , relativeCrossSections[j]);
			} else {
				// Removed unused crossSection
				File file = new File(dir + File.separator + outFileName);
				file.delete();
			}
		}
	}
	
	public boolean readCrossSections() {
		final String dir = sharedOutputDir + File.separator + "model_crossSections" + File.separator;
		return readCrossSections(dir);
	}
	public boolean readCrossSections(final String dir) {
		boolean readSomething = false;
		updateRelativeCrossSectionsFromDisplayContours();
		
		System.out.println("Read in cross sections.");
		
		for (int j = 0; j < latticeSlices.length; j++) {
			String outFileName = "latticeCrossSection_" + j + ".csv";
			
			VOIContour contour = LatticeModel.loadContourFromCSV(dir, outFileName);
			if (contour != null) {
				readSomething = true;
				editedCrossSections[j] = true;
				relativeCrossSections[j] = contour;
			}
		}
		return readSomething;
	}
	
	private boolean getContourFile() {
		String dir = sharedOutputDir + File.separator + "model_contours" + File.separator;
		File fileDir = new File(dir);
		if ( !fileDir.exists() ) return false;
		if ( !fileDir.isDirectory() ) return false;
		dir = sharedOutputDir + File.separator + "model_contours" + File.separator + "wormContours.csv";
		File file = new File(dir);
		return file.exists();
	}
	
	private void saveMeshContoursCSV() {
		String dir = sharedOutputDir + File.separator + "model_contours" + File.separator;
		File fileDir = new File(dir);

		if ( !fileDir.exists() ) { // voiFileDir does not exist
			fileDir.mkdir();
		}
		
		File file = new File(dir + File.separator +  "wormContours.csv");
		if (file.exists()) {
			file.delete();
			file = new File(dir + File.separator +  "wormContours.csv");
		}
		
		try {			
			final FileWriter fw = new FileWriter(file);
			final BufferedWriter bw = new BufferedWriter(fw);
			bw.write( displayContours2.getCurves().size() + "\n" );
			for ( int i = 0; i < displayContours2.getCurves().size(); i++ ) {
				VOIContour contour = (VOIContour) displayContours2.getCurves().elementAt(i);
				bw.write( contour.size() + "\n" );
				for ( int j = 0; j < contour.size(); j++ ) {
					Vector3f position = contour.elementAt(j);
					bw.write(position.X + "," + position.Y + ","	+ position.Z + "\n");
				}
			}
			bw.newLine();
			bw.close();
		} catch (final Exception e) {
			System.err.println("CAUGHT EXCEPTION WITHIN saveSeamCellsTo");
			e.printStackTrace();
		}

	}
	
	private void readMeshContoursCSV() {
		String dir = sharedOutputDir + File.separator + "model_contours" + File.separator;
		File file = new File(dir + File.separator +  "wormContours.csv");
		if ( file.exists() )
		{		
			
			displayContours2 = new VOI((short)0, "wormContours2");

			FileReader fr;
			try {
				fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);
				String line = br.readLine();
				String[] parsed = line.split( "," );

                int numContours = Integer.valueOf( parsed[0] );
                for ( int i = 0; i < numContours; i++ ) {		
        			VOIContour ellipse = new VOIContour(true);

                	line = br.readLine();
    				parsed = line.split( "," );
                    int numPts = Integer.valueOf( parsed[0] );
                    for ( int j = 0; j < numPts; j++ ) {

                    	line = br.readLine();
                    	parsed = line.split( "," );
                    	int parsedIndex = 0;
						float x    = (parsed.length > parsedIndex+0) ? (parsed[parsedIndex+0].length() > 0) ? Float.valueOf( parsed[parsedIndex+0] ) : 0 : 0; 
						float y    = (parsed.length > parsedIndex+1) ? (parsed[parsedIndex+1].length() > 0) ? Float.valueOf( parsed[parsedIndex+1] ) : 0 : 0; 
						float z    = (parsed.length > parsedIndex+2) ? (parsed[parsedIndex+2].length() > 0) ? Float.valueOf( parsed[parsedIndex+2] ) : 0 : 0;
						
                    	ellipse.add(new Vector3f(x,y,z));
                    }
                    displayContours2.getCurves().add(ellipse);
                }
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void openNeuriteCurves(String outputDirectory)
	{
		String fileName = WormData.neuriteOutput;
		
		final String voiDir = new String(outputDirectory + File.separator + fileName + File.separator);
		File voiFileDir = new File(voiDir);
		
		if (voiFileDir.exists() && voiFileDir.isDirectory()) {
			final String[] list = voiFileDir.list();
			for (int i = 0; i < list.length; i++) {
				final File lrFile = new File(voiDir + list[i]);
				System.err.println( lrFile.getName() );
				if ( lrFile.getName().contains("controlPts") ) {
					VOI annotations = LatticeModel.readAnnotationsCSV(voiDir + list[i]);
					if ( annotations != null ) {
						addAnnotations(annotations);
						Vector<VOIWormAnnotation> splineControlPts = new Vector<VOIWormAnnotation>();
						for ( int j = 0; j < annotations.getCurves().size(); j++ ) {
							VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(j);
							text.setCurveAnnotation(true);
							splineControlPts.add(text);
						}
						addSplineControlPts(splineControlPts);
					}
				}
			}
		} 
	}

	public void saveNeuriteCurves()
	{
		if ( splineControlPtsList == null ) return;
		if ( splineControlPtsList.size() <= 0 ) return;
		
		String fileName = WormData.neuriteOutput;

		String imageName = JDialogBase.makeImageName(imageA.getImageFileName(), "");
		
		
		final String voiDir = new String(sharedOutputDir + fileName + File.separator);
		for ( int i = 0; i < splineControlPtsList.size(); i++ ) {
			AnnotationSplineControlPts annotationSpline = splineControlPtsList.elementAt(i);
			
			for ( int j = 0; j < annotationSpline.annotations.size(); j++ ) {
				annotationSpline.annotations.elementAt(j).setCurveAnnotation(true);
			}

			VOI curveVOI = new VOI((short)0, annotationSpline.name );
			for ( int j = 0; j < annotationSpline.annotations.size(); j++ ) curveVOI.getCurves().add(new VOIWormAnnotation(annotationSpline.annotations.elementAt(j)) );

			LatticeModel.saveAnnotationsAsCSV(voiDir + File.separator, imageName + "_" + annotationSpline.name + "_controlPts.csv", curveVOI, true );
			LatticeModel.saveContourAsCSV( imageA, fileName, "_" + annotationSpline.name + "_spline", annotationSpline.curve );
		}
		
	}
	

	
	public void untwistNeuriteCurves( boolean useLatticeModel)
	{		
		final String voiDir = new String(sharedOutputDir + File.separator + WormData.neuriteOutput + File.separator);
		File voiFileDir = new File(voiDir);
		
		if (voiFileDir.exists() && voiFileDir.isDirectory()) {
			final String[] list = voiFileDir.list();
			for (int i = 0; i < list.length; i++) {
				final File lrFile = new File(voiDir + list[i]);
				System.err.println( lrFile.getName() );
				VOI annotations = LatticeModel.readAnnotationsCSV(voiDir + list[i]);
				if ( annotations != null ) {
					setMarkers(annotations);
					untwistMarkers(true);	
					saveAnnotationsAsCSV(sharedOutputDir + File.separator + "straightened_neurites" + File.separator, 
							"straightened_" + list[i], annotationsStraight);
				}
			}
		} 
	}
	
	public static void openStraightNeuriteCurves(ModelImage image, String outputDirectory) {
	
		final String voiDir = new String(outputDirectory + File.separator + "straightened_neurites" + File.separator );
		File voiFileDir = new File(voiDir);
		
		if (voiFileDir.exists() && voiFileDir.isDirectory()) {
			final String[] list = voiFileDir.list();
			for (int i = 0; i < list.length; i++) {
				final File lrFile = new File(voiDir + list[i]);
				System.err.println( lrFile.getName() );
				if ( lrFile.getName().contains("controlPts") ) {
					VOI annotations = LatticeModel.readAnnotationsCSV(voiDir + list[i]);
					image.registerVOI(annotations);					
				}
				else {

					VOI annotations = LatticeModel.readAnnotationsCSV(voiDir + list[i]);
					VOIContour curve = new VOIContour(false);
					Vector<VOIContour> curveList = null;
					System.err.println( annotations.getCurves().size() );
					for (int j = 0; j < annotations.getCurves().size(); j++ ) {
						curve.add( annotations.getCurves().elementAt(j).elementAt(0) );
						if ( curve.size() > 1 ) {
							float dist = curve.elementAt(curve.size() -1 ).distance(curve.elementAt( curve.size() -2 ) );
							if ( dist > 5 ) // should be one voxel spacing... 
							{
//								System.err.println(dist);
								if ( curveList == null ) {
									curveList = new Vector<VOIContour>();
								}
								curveList.add(curve);
								// remove the point with the big jump:
								Vector3f lastPt = curve.lastElement();
								curve.remove( curve.lastElement() );
								curve = new VOIContour(false);
								curve.add( lastPt );
							}
						}
					}
//					System.err.println("");
//					System.err.println("");
					if ( curveList != null ) {
						curveList.add(curve);
						curve = null;
						
						int maxSize = -1;
						int maxSizeIndex = -1;
						for ( int j = 0; j < curveList.size(); j++ ) {
							if ( curveList.elementAt(j).size() > maxSize ) {
								maxSize = curveList.elementAt(j).size();
								maxSizeIndex = j;
							}
						}
						if ( maxSizeIndex != -1 ) {
							VOIContour newCurve = new VOIContour(false);
							newCurve.addAll(curveList.elementAt(maxSizeIndex));
//							System.err.println( newCurve.size() );
							for ( int j = maxSizeIndex + 1; j < curveList.size(); j++ ) {
//								System.err.println( curveList.elementAt(j).size() );
								float skipDist = newCurve.lastElement().distance( curveList.elementAt(j).firstElement());
//								System.err.println(skipDist);
								if ( skipDist < 20 ) {
									newCurve.addAll( curveList.elementAt(j) );
								}
							}
							for ( int j = maxSizeIndex - 1; j >= 0; j-- ) {
//								System.err.println( curveList.elementAt(j).size() );
								float skipDist = curveList.elementAt(j).lastElement().distance( newCurve.firstElement());
//								System.err.println(skipDist);
								if ( skipDist < 20 ) {
									for ( int k = 0; k < curveList.elementAt(j).size(); k++ ) {
										newCurve.add(k, curveList.elementAt(j).elementAt(k) );
									}
								}
							}
//							System.err.println("");
//							System.err.println("");
//							System.err.println( newCurve.size() );
							curve = newCurve;
						}
					}
					if ( curve != null ) {
						VOI curveVOI = new VOI((short)0, list[i]);
						curveVOI.getCurves().add(curve);
						image.registerVOI(curveVOI);
					}
				}
			}
		} 
	}
	

	public ModelImage segmentLattice(final ModelImage image, boolean saveContourImage, int paddingFactor, boolean segmentLattice )
	{
		String imageName = getImageName(image);
		
		String voiDir = sharedOutputDir + File.separator + "contours" + File.separator;
		final File voiFileDir = new File(voiDir);

		if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
			final String[] list = voiFileDir.list();
			for (int i = 0; i < list.length; i++) {
				final File lrFile = new File(voiDir + list[i]);
				lrFile.delete();
			}
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			//			System.err.println( "segmentLattice" + voiDir);
			voiFileDir.mkdir();
		}

		System.err.println( "Segment Lattice " + paddingFactor );
		long time = System.currentTimeMillis();

		FileIO fileIO = new FileIO();

		ModelImage resultImage = fileIO.readImage( outputDirectory + File.separator + "output_images" + File.separator + imageName + "_straight_unmasked.xml" );
		int dimZ = resultImage.getExtents().length > 2 ? resultImage.getExtents()[2] : 1;
		int[] resultExtents = resultImage.getExtents();

		ModelImage contourImage = new ModelImage( ModelStorageBase.FLOAT, resultImage.getExtents(), imageName + "_straight_contour.xml" );
		contourImage.setResolutions( resultImage.getResolutions(0) );
		System.err.println( "   new images " + AlgorithmBase.computeElapsedTime(time) );
		time = System.currentTimeMillis();

		int dimX = (resultImage.getExtents()[0]);
		int dimY = (resultImage.getExtents()[1]);
		Vector3f center = new Vector3f( dimX/2, dimY/2, 0 );
		float maxDist = Vector3f.normalize(center);

		//		System.err.println( dimX + " " + dimY + " " + dimZ );

		if ( !segmentLattice ) {
			generateTriMesh2( false, true, 1);
		}
		latticeContours = new VOI( (short)1, "contours", VOI.POLYLINE, (float) Math.random());
		resultImage.registerVOI( latticeContours );
		
		VOIBase[] edgePoints = new VOIBase[numEllipsePts];
		for(int j = 0; j < numEllipsePts; ++j) {
			edgePoints[j] = displayContours[latticeSlices.length + j].getCurves().elementAt(0);
		}
				
		for (int i = 0; i < dimZ; i++)
		{			
			VOIContour contour = new VOIContour(true);


			float radius = (float) (1.05 * rightPositions.elementAt(i).distance(leftPositions.elementAt(i))/(2f));
			radius += paddingFactor;
			
			//mkitti 2023/04/17: use relative contours to build contour
			Vector3f displayCenter = centerPositions.get(i);
			float[] radii = new float[numEllipsePts];
			for(int j = 0; j < numEllipsePts; ++j) {
				radii[j] = Math.max(Vector3f.sub(edgePoints[j].get(i), displayCenter).length(), radius);
			}
			makeEllipse2DA(Vector3f.UNIT_X, Vector3f.UNIT_Y, center, radii, contour);		

			if ( !segmentLattice && (clipMask != null) && (clipMask.size() == dimZ) ) {
				//				System.err.println( "use clip mask " + (i/10) + "  " + clipMask.size() );
//				boolean[] mask = clipMask.elementAt(i);
//				for ( int j = 0; j < mask.length; j++ ) {
//					if ( !mask[j] ) {
//						// extend contour out to edge:
//						Vector3f dir = Vector3f.sub( contour.elementAt(j), center );
//						dir.normalize();
//						dir.scale(maxDist);
//						dir.add(center);
//						contour.elementAt(j).copy(dir);
//					}
//				}
				for ( int y = 0; y < dimY; y++ ) {
					for ( int x = 0; x < dimX; x++ ) {
						if ( contour.contains(x,y) ) {
							contourImage.set(x,  y, i, 10 );
						}
					}
				}
			}
			else
			{
				float radiusSq = radius*radius;
				for ( int y = (int)Math.max(0, center.Y - radius); y < Math.min(dimY, center.Y + radius + 1); y++ )
				{
					for ( int x = (int)Math.max(0, center.X - radius); x < Math.min(dimX, center.X + radius + 1); x++ )
					{
						float dist = (x - center.X) * (x - center.X) + (y - center.Y) * (y - center.Y); 
						if ( dist <= radiusSq )
						{
							contourImage.set(x,  y, i, 10 );
						}
					}
				}
			}
			for ( int j = 0; j < contour.size(); j++ )
			{
				contour.elementAt(j).Z = i;
			}
			latticeContours.getCurves().add( contour );
		}

		System.err.println( "   contours " + AlgorithmBase.computeElapsedTime(time) );
		time = System.currentTimeMillis();

		// Optional VOI interpolation & smoothing:
		ModelImage contourImageBlur = blur(contourImage, 3);
		System.err.println( "   blur " + AlgorithmBase.computeElapsedTime(time) );
		time = System.currentTimeMillis();
		contourImage.disposeLocal(false);
		contourImage = null;

		for (int z = 0; z < dimZ; z++)
		{			
			for ( int y = 0; y < dimY; y++ )
			{
				for ( int x = 0; x < dimX; x++ )
				{
					if ( contourImageBlur.getFloat(x,y,z) <= 1 )
					{
						if ( resultImage.isColorImage() )
						{
							resultImage.setC(x, y, z, 0, 0);	
							resultImage.setC(x, y, z, 1, 0);	
							resultImage.setC(x, y, z, 2, 0);	
							resultImage.setC(x, y, z, 3, 0);							
						}
						else
						{
							resultImage.set(x, y, z, 0);
						}
					}
				}
			}			
		}
		System.err.println( "   clip images " + AlgorithmBase.computeElapsedTime(time) );
		time = System.currentTimeMillis();

		resultImage.setImageName( imageName + "_straight.xml" );
		saveImage(imageName, resultImage, true);

		// Save the contour vois to file.
		voiDir = sharedOutputDir + File.separator + "contours" + File.separator;
		saveAllVOIsTo(voiDir, resultImage);
		resultImage.unregisterAllVOIs();
		System.err.println( "   save vois " + AlgorithmBase.computeElapsedTime(time) );
		time = System.currentTimeMillis();

		

		
//		time = System.currentTimeMillis();
//		untwistAll(imageA, resultExtents);
//		System.err.println( "untwist ALL elapsed time =  " + AlgorithmBase.computeElapsedTime(time) );
		
		
		
		resultImage.disposeLocal(false);
		resultImage = null;

		if ( saveContourImage ) {
			contourImageBlur.setImageName( imageName + "_contours.xml" );
			ModelImage.saveImage(contourImageBlur, contourImageBlur.getImageName() + ".xml", sharedOutputDir + File.separator + "output_images" + File.separator, false);
			System.err.println( "   save image " + AlgorithmBase.computeElapsedTime(time) );
			time = System.currentTimeMillis();
			return contourImageBlur;
		}
		contourImageBlur.disposeLocal(false);
		contourImageBlur = null;
		
		
		
		return null;
	}

	public void segmentLattice(final ModelImage image, final ModelImage contourImageBlur) {

		String imageName = getImageName(image);
		
		FileIO fileIO = new FileIO();
		ModelImage resultImage = fileIO.readImage( outputDirectory + File.separator + "output_images" + File.separator + imageName + "_straight_unmasked.xml" );
		int dimX = resultImage.getExtents().length > 0 ? resultImage.getExtents()[0] : 1;
		int dimY = resultImage.getExtents().length > 1 ? resultImage.getExtents()[1] : 1;
		int dimZ = resultImage.getExtents().length > 2 ? resultImage.getExtents()[2] : 1;

		for (int z = 0; z < dimZ; z++)
		{			
			for ( int y = 0; y < dimY; y++ )
			{
				for ( int x = 0; x < dimX; x++ )
				{
					if ( contourImageBlur.getFloat(x,y,z) <= 1 )
					{
						if ( resultImage.isColorImage() )
						{
							resultImage.setC(x, y, z, 0, 0);	
							resultImage.setC(x, y, z, 1, 0);	
							resultImage.setC(x, y, z, 2, 0);	
							resultImage.setC(x, y, z, 3, 0);							
						}
						else
						{
							resultImage.set(x, y, z, 0);
						}
					}
				}
			}			
		}
		resultImage.setImageName( imageName + "_straight.xml" );
		saveImage(imageName, resultImage, true);
		resultImage.disposeLocal(false);
		resultImage = null;		
	}

	public boolean selectAnnotation(final Vector3f startPt, final Vector3f endPt, final Vector3f pt, boolean rightMouse, boolean multiSelect ) {
		if ( annotationVOIs == null )
		{
			return false;
		}
		if ( annotationVOIs.getCurves() == null )
		{
			return false;
		}
		if ( annotationVOIs.getCurves().size() == 0 )
		{
			return false;
		}
		VOIWormAnnotation nearest = (VOIWormAnnotation)VOILatticeManagerInterface.findNearestAnnotation(annotationVOIs, startPt, endPt, pt);
//		System.err.println("selectAnnotation " + nearest );
		if ( nearest == null ) {
			return false;
		}
		boolean repeat = false;
		if ( !rightMouse ) {
			// toggle nearest selection:
			nearest.setSelected( !nearest.isSelected() );
			nearest.updateSelected(imageA);
			for ( int i = 0; i < annotationVOIs.getCurves().size(); i++ )
			{
				VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
				if ( text != nearest ) {
					// set selection to false of not using multiSelect
					text.setSelected( text.isSelected() && multiSelect );
					text.updateSelected(imageA);
				}
				if ( text.isSelected() ) {
					text.setSelectionOffset( Vector3f.sub(pt, text.elementAt(0)) );
				}
			}
			imageA.notifyImageDisplayListeners();
			updateAnnotationListeners();
			return true;
		}
		if ( rightMouse ) {

			Vector<VOIWormAnnotation> selectedAnnotations = getPickedAnnotation();
			if ( selectedAnnotations == null ) {
				return true;
			}

			if ( nearest.isSelected() && (selectedAnnotations.size() == 1) )
			{
				// rename add notes, etc.:
				String oldName = new String(nearest.getText());
				new JDialogAnnotation(imageA, annotationVOIs, annotationVOIs.getCurves().indexOf(nearest), true, true);
				String newName = new String(nearest.getText());
				for ( int i = 0; i < annotationVOIs.getCurves().size(); i++ ) {
					if ( annotationVOIs.getCurves().elementAt(i) != nearest ) {
						VOIWormAnnotation current = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
						if ( current.getText().contentEquals(newName) ) {
							MipavUtil.displayError( "Name already exists " + newName );
							nearest.setText(oldName);
							repeat = true;
						}
					}
				}
				nearest.updateText();
				colorAnnotations();
			}
			else if ( selectedAnnotations.size() > 1 )
			{
				// open dialog to group annotations
				String groupName = JOptionPane.showInputDialog("Create group: ");
				if ( groupName != null ) {
					for ( int i = 0; i < selectedAnnotations.size(); i++ ) {
						VOIWormAnnotation text = selectedAnnotations.elementAt(i);
						text.setText( groupName + JPanelAnnotations.getPostfix(text.getText() ) );
						text.updateText();
					}
				}
			}
			if ( repeat ) {
				return selectAnnotation(startPt, endPt, pt, rightMouse, multiSelect);
			}
			imageA.notifyImageDisplayListeners();
			updateAnnotationListeners();
			return true;
		}

		return false;
	}

	//add selection for sections here
	public boolean selectLattice(final Vector3f startPt, final Vector3f endPt, final Vector3f pt, boolean isSeam) {
		if ( editingCrossSections )
		{
			return selectLatticeSection(startPt, endPt, pt, isSeam);
		}
		if ( (lattice == null) || (left == null) || (right == null) ) {
			return false;
		}
		pickedPoint = null;
		int closestL = -1;
		float minDistL = Float.MAX_VALUE;
		for (int i = 0; i < left.getCurves().size(); i++) {
			final float distance = pt.distance(left.getCurves().elementAt(i).elementAt(0));
			if (distance < minDistL) {
				minDistL = distance;
				if (minDistL <= 12) {
					closestL = i;
				}
			}
		}
		int closestR = -1;
		float minDistR = Float.MAX_VALUE;
		for (int i = 0; i < right.getCurves().size(); i++) {
			final float distance = pt.distance(right.getCurves().elementAt(i).elementAt(0));
			if (distance < minDistR) {
				minDistR = distance;
				if (minDistR <= 12) {
					closestR = i;
				}
			}
		}
		// System.err.println( minDistL + " " + minDistR );
		if ( (closestL != -1) && (closestR != -1)) {
			if (minDistL < minDistR) {
				// System.err.println( "Picked Lattice Left " + closestL );
				pickedPoint = left.getCurves().elementAt(closestL).elementAt(0);
			} else {
				// System.err.println( "Picked Lattice Right " + closestR );
				pickedPoint = right.getCurves().elementAt(closestR).elementAt(0);
			}
		} else if (closestL != -1) {
			// System.err.println( "Picked Lattice Left " + closestL );
			pickedPoint = left.getCurves().elementAt(closestL).elementAt(0);
		} else if (closestR != -1) {
			// System.err.println( "Picked Lattice Right " + closestR );
			pickedPoint = right.getCurves().elementAt(closestR).elementAt(0);
		}
		if (pickedPoint != null) {
			updateLattice(false);
			return true;
		}
		if ( startPt == null || endPt == null ) {
			return false;
		}
		
		
		// look at the vector under the mouse and see which lattice point is closest...
		final Segment3f mouseVector = new Segment3f(startPt, endPt);
		float minDist = Float.MAX_VALUE;
		for (int i = 0; i < left.getCurves().size(); i++) {
			DistanceVector3Segment3 dist = new DistanceVector3Segment3(left.getCurves().elementAt(i).elementAt(0), mouseVector);
			float distance = dist.Get();
			if (distance < minDist) {
				minDist = distance;
				pickedPoint = left.getCurves().elementAt(i).elementAt(0);
			}
		}
		for ( int i = 0; i < right.getCurves().size(); i++ ) {
			DistanceVector3Segment3 dist = new DistanceVector3Segment3(right.getCurves().elementAt(i).elementAt(0), mouseVector);
			float distance = dist.Get();
			if (distance < minDist) {
				minDist = distance;
				pickedPoint = right.getCurves().elementAt(i).elementAt(0);
			}
		}
		if ( (pickedPoint != null) && (minDist <= 12)) {
			updateLattice(false);
			return true;
		}
		if ( !previewMode ) {
			return addInsertionPoint(startPt, endPt, pt, isSeam);
		}
		else {
			return false;
		}
	}
	
	public boolean selectLatticeSection(final Vector3f startPt, final Vector3f endPt, final Vector3f pt, boolean isSeam)
	{
		if (ellipseCurves == null) {
			return false;
		}
		if ( startPt == null || endPt == null ) {
			return false;
		}
		
		final Segment3f mouseVector = new Segment3f(startPt, endPt);
		
		int closestSection = -1;
		int closestPoint = -1;
		float minDistSection = Float.MAX_VALUE;
		
		pickedPoint = null;

		/*
		 * Assumes that the first latticeSlices.length displayContours correspond to the "ellipses" cross sections
		 */
		for (int s = 0; s < latticeSlices.length; s++ ) {
			VOIBase section = displayContours[s].getCurves().elementAt(0);
			for (int i = 0; i < section.size(); i++) {
				Vector3f sectionPoint = section.elementAt(i);
				DistanceVector3Segment3 dist = new DistanceVector3Segment3(sectionPoint, mouseVector);
				final float distance = dist.Get();
				//final float distance = pt.distance(sectionPoint);
				if( distance < minDistSection )
				{
					minDistSection = distance;
					closestSection = s;
					closestPoint = i;
					//pickedPoint = sectionPoint;
					pickedPoint = new Vector3f();
					pickedPoint.copy(sectionPoint);
				}
			}
		}
		
		for (int s = 0; s < latticeSlices.length; s++ ) {
			VOIBase section = displayContours[s].getCurves().elementAt(0);
			if(s == closestSection)
			{
				section.update(new ColorRGBA(1.0f, 1.0f, 0f, 1.0f));
			}
			else
			{
				section.update(new ColorRGBA(0.5f, 0.5f, 0.5f, 0.7f));
			}
		}
		
		for(int s = 0; s < numEllipsePts; s++ ) {
			VOIBase section = displayContours[s + latticeSlices.length].getCurves().elementAt(0);
			if(s ==  closestPoint) {
				section.update(new ColorRGBA(1.0f, 1.0f, 0f, 1.0f));
			}
			else
			{
				section.update(new ColorRGBA(0.5f, 0.5f, 0.5f, 0.75f));
			}
		}
		
		if(pickedPoint != null)
		{
			selectedSectionIndex = closestSection;
			selectedSectionIndex2 = closestPoint;
			
			System.out.println("selectedSectionIndex2: " + selectedSectionIndex2);
			
			
			final short id = (short) imageA.getVOIs().getUniqueID();
			if(sectionMarker == null) {
				sectionMarker = new VOI(id, "sectionMarker", VOI.POINT, (float) Math.random());
				this.imageA.registerVOI(sectionMarker);
			}
			sectionMarker.getCurves().clear();
			sectionMarker.importPoint(pickedPoint);
			colorSectionMarkerByCrossSectionSamples();
			//why return here?
			//return true;
		}
		
		
		
		
		/*
		ellipseCurves[closestSection].update(new ColorRGBA(1.0f, 1.0f, 0.5f, 0.7f));
		
		if(imageA.isRegistered(selectedSectionVOI) != -1)
		{
			imageA.unregisterVOI(selectedSectionVOI);
		}

		
		selectedSectionVOI.getCurves().clear();
		selectedSectionVOI.getCurves().add(ellipseCurves[closestSection]);
		selectedSectionVOI.setColor(Color.CYAN);
		
		if(imageA.isRegistered(selectedSectionVOI) == -1)
		{
			imageA.registerVOI(selectedSectionVOI);
		}
		*/
		
		/*
		final boolean display = true;
		for(int i=0; i < displayContours.length; i++)
		{
			if ( displayContours[i] == null ) continue;
			
			System.out.println(displayContours[i].getColor());
			
			displayContours[i].setColor(Color.red);
			displayContours[i].update();
			
			if ( display && (imageA.isRegistered(displayContours[i]) == -1) ) {
				imageA.registerVOI(displayContours[i]);
			}
			if ( !display && (imageA.isRegistered(displayContours[i]) != -1) ) {
				imageA.unregisterVOI(displayContours[i]);
			}
			
			
		}
		*/
		imageA.notifyImageDisplayListeners();

		
		return true;
	}

	public void setAnnotationPrefix(String s)
	{
		annotationPrefix = s;
	}

	/**
	 * Called when new annotations are loaded from file, replaces current annotations.
	 * 
	 * @param newAnnotations
	 */
	public void setAnnotations(final VOI newAnnotations) {
		if (annotationVOIs != null) {
			imageA.unregisterVOI(annotationVOIs);
			if ( (annotationsStraight != null) && imageA.isRegistered(annotationsStraight) != -1 ) {
				imageA.unregisterVOI(annotationsStraight);
			}
		}
		clear3DSelection();

		if (showSelected != null) {
			for (int i = 0; i < showSelected.length; i++) {
				showSelected[i].dispose();
			}
			showSelected = null;
		}
		showSelectedVOI = null;
		clearAddLeftRightMarkers();		

		annotationVOIs = newAnnotations;
		if ( annotationVOIs == null )
		{
			return;
		}
		
		annotationVOIs.setName("annotationVOIs");
		if ( imageA.isRegistered(annotationVOIs) == -1 ) {
			imageA.registerVOI(annotationVOIs);
		}
		for (int i = 0; i < annotationVOIs.getCurves().size(); i++) {
			final VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
			//			text.setColor(Color.blue);
			final Color c = text.getColor();
			//			System.err.println( text.getText() + "  " + text.getColor() );
			text.update(new ColorRGBA(c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, 1f));
			//			text.elementAt(1).add(6, 0, 0);
			text.setUseMarker(false);

			if (text.getText().equalsIgnoreCase("nose") || text.getText().equalsIgnoreCase("origin")) {
				if (wormOrigin == null) {
					wormOrigin = new Vector3f(text.elementAt(0));
					// updateLattice(true);
				} else {
					wormOrigin.copy(text.elementAt(0));
					// updateLattice(false);
				}
			}
		}
		colorAnnotations();
		updateAnnotationListeners();
	}

	/**
	 * Called when new annotations are loaded from file, replaces current annotations.
	 * 
	 * @param newAnnotations
	 */
	public void addAnnotations(final VOI newAnnotations) {
		if ( newAnnotations.getCurves().size() == 0 ) return;
		if (annotationVOIs == null) {
			annotationVOIs = newAnnotations;
			annotationVOIs.setName("annotationVOIs");
			imageA.registerVOI(annotationVOIs);
		}
		else {
			annotationVOIs.getCurves().addAll( newAnnotations.getCurves() );
		}
		clear3DSelection();

		if (showSelected != null) {
			for (int i = 0; i < showSelected.length; i++) {
				showSelected[i].dispose();
			}
			showSelected = null;
		}
		showSelectedVOI = null;
		clearAddLeftRightMarkers();		

		if ( annotationVOIs == null )
		{
			return;
		}
		if ( imageA.isRegistered(annotationVOIs) == -1 ) {
			imageA.registerVOI(annotationVOIs);
		}
		for (int i = 0; i < annotationVOIs.getCurves().size(); i++) {
			final VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
			//			text.setColor(Color.blue);
			final Color c = text.getColor();
			//			System.err.println( text.getText() + "  " + text.getColor() );
			text.update(new ColorRGBA(c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, 1f));
			text.elementAt(1).copy(text.elementAt(0));
			//			text.elementAt(1).add(6, 0, 0);
			text.setUseMarker(false);

			if (text.getText().equalsIgnoreCase("nose") || text.getText().equalsIgnoreCase("origin")) {
				if (wormOrigin == null) {
					wormOrigin = new Vector3f(text.elementAt(0));
					// updateLattice(true);
				} else {
					wormOrigin.copy(text.elementAt(0));
					// updateLattice(false);
				}
			}
		}
		colorAnnotations();
		updateAnnotationListeners();
	}

	/**
	 * Change the underlying image for the latticeModel. Update the output directories.
	 * @param image
	 */
	public void setImage(ModelImage image)
	{
		imageA = image;
		if ( imageA != null )
		{			
			imageDims = new Vector3f( imageA.getExtents()[0] - 1, imageA.getExtents()[1] - 1, imageA.getExtents()[2] - 1 );
			outputDirectory = new String(imageA.getImageDirectory() + JDialogBase.makeImageName(imageA.getImageFileName(), "") + File.separator);
			boolean isStraight = image.getImageFileName().contains("_straight");
			File file = new File(outputDirectory);
			if ( !file.exists() && !isStraight ) file.mkdir();
			outputDirectory += JDialogBase.makeImageName(imageA.getImageFileName(), "_results") + File.separator;
			file = new File(outputDirectory);
			if ( !file.exists() && !isStraight ) file.mkdir();
		}
	}

	public void setSharedDirectory( String dir ) {
		sharedOutputDir = new String(dir);
		File file = new File(sharedOutputDir + File.separator + "output_images" + File.separator);
		if ( !file.exists() ) file.mkdir();
	}
	

	/**
	 * Called when a new lattice is loaded from file, replaces the current lattice.
	 * 
	 * @param newLattice
	 */
	public void setLattice(final VOIVector newLattice) {
		
		if ( lattice != null ) {
			for ( int i = 0; i < lattice.size(); i++ ) {
				imageA.unregisterVOI(lattice.elementAt(i));
			}
			if ( leftContour != null )  imageA.unregisterVOI(leftContour);
			if ( rightContour != null ) imageA.unregisterVOI(rightContour);
//			if ( lattice != newLattice ) {
//				lattice.removeAllElements();
//			}
		}		
		
		this.lattice = newLattice;
		left = null;
		right = null;
		leftContour = null;
		rightContour = null;
		clearCurves(true);
		if ( this.lattice == null )
		{
			updateLattice(true);
			return;
		}

		// Assume image is isotropic (square voxels).
		if (lattice.size() < 2) {
			updateLattice(true);
			return;
		}
		left = (VOI) lattice.elementAt(0);	left.setName("left");
		right = (VOI) lattice.elementAt(1);	right.setName("right");
		if (left.getCurves().size() != right.getCurves().size()) {
			updateLattice(true);
			return;
		}

		imageA.registerVOI(left);
		imageA.registerVOI(right);
		showLatticeLabels(false);

		clear3DSelection();
		clearAddLeftRightMarkers();
		updateLattice(true);
		showLatticeLabels(displayLatticeLabels);
	}

	public void setMarkers(VOI markerVOIs)
	{
		markerCenters = new Vector<Vector3f>();
		markerNames = new Vector<String>();
		markerLatticeSegments = new Vector<Integer>();
		markerSlices = new Vector<Integer>();
		for ( int i = 0; i < markerVOIs.getCurves().size(); i++ )
		{
			VOIWormAnnotation text = (VOIWormAnnotation)markerVOIs.getCurves().elementAt(i);
			Vector3f center = new Vector3f(text.elementAt(0));
			markerCenters.add(center);
			markerNames.add(text.getText());

			int value = text.getLatticeSegment();
			markerLatticeSegments.add(value);
			
			int slice = text.getSlice();
			markerSlices.add(slice);
		}
	}

	public void setPaddingFactor( int padding ) {
		paddingFactor = padding;
		boolean display = false;

		if ( displayContours == null ) return;
		
		for ( int i = 0; i < displayContours.length; i++ ) {
			display |= (imageA.isRegistered(displayContours[i]) != -1);
		}

		if ( display )
		{
			generateCurves(5);
			for ( int i = 0; i < displayContours.length; i++ ) {
				imageA.registerVOI(displayContours[i]);
			}
		}
	}

	public boolean isPreview()
	{
		return previewMode;
	}

	public void setPreviewMode( boolean preview, VOIVector lattice, VOI annotations )
	{
		this.previewMode = preview;
		setLattice(lattice);
		setAnnotations(annotations);
	}

	/**
	 * Enables the user to visualize the simple ellipse-based model of the worm during lattice construction.
	 */
	private boolean modelDisplayed = false;
	public void showModel(boolean display) {
		modelDisplayed = display;
		for ( int i = 0; i < displayContours.length; i++ ) {
			if ( displayContours[i] == null ) continue;
			if ( display && (imageA.isRegistered(displayContours[i]) == -1) ) {
				imageA.registerVOI(displayContours[i]);
			}
			if ( !display && (imageA.isRegistered(displayContours[i]) != -1) ) {
				imageA.unregisterVOI(displayContours[i]);
			}
		}
		imageA.notifyImageDisplayListeners();
//		if ( samplingPlanes == null ) generateEllipses();
//		if ( display && (imageA.isRegistered(samplingPlanes) == -1) ) {
//			imageA.registerVOI(samplingPlanes);
//			imageA.notifyImageDisplayListeners();
//		}
//		if ( !display && (imageA.isRegistered(samplingPlanes) != -1) ) {
//			imageA.unregisterVOI(samplingPlanes);
//			imageA.notifyImageDisplayListeners();
//		}
	}
	
	public void editCrossSections(boolean display) {
		if(!display)
		{
			selectedSectionIndex = -1;
			if(sectionMarker != null) {
				if(imageA.isRegistered(sectionMarker) != -1) {
					imageA.unregisterVOI(sectionMarker);
				}
				if(sectionMarker.getCurves() != null) {
					sectionMarker.getCurves().clear();
				}
				sectionMarker.dispose();
				sectionMarker = null;
			}
			for ( int i = 0; i < displayContours.length; i++ ) {
				if ( displayContours[i] == null ) continue;
				float red = 1.0f - ((float) crossSectionSamples) / 32.0f;
				displayContours[i].getCurves().elementAt(0).update(new ColorRGBA(red, 1.0f, 1.0f, 0.7f));
			}
		}
		showModel(display);
		editingCrossSections = display;
	}
	
	public boolean isModelDisplayed()
	{
		return modelDisplayed;
	}
	
	private boolean splineModel = true;
	private boolean ellipseCross = false; 
	private float ellipseScale = 1;
	public void updateCrossSection( boolean useSpline, boolean ellipse, float percent ) {
		splineModel = useSpline;
		ellipseCross = ellipse;
		ellipseScale = percent;
		updateLattice(false);
	}

	private boolean displayLatticeLabels = false;
	public void showLatticeLabels(boolean display) {
		if ( left == null ) return;
		displayLatticeLabels = display;
		for ( int i = 0; i < left.getCurves().size(); i++ ) {
			VOIWormAnnotation text = (VOIWormAnnotation) left.getCurves().elementAt(i);
			text.display(displayLatticeLabels);
			
			if ( i < right.getCurves().size() ) {
				text = (VOIWormAnnotation) right.getCurves().elementAt(i);
				text.display(displayLatticeLabels);
			}
		}
	}
	
	
	/**
	 * Turns on/off lattice display:
	 */
	public void showLattice(boolean display) {		
		if ( display ) {
			updateLattice(true);
		}
		if ( !display ) {
			if ( leftContour != null ) {
				imageA.unregisterVOI(leftContour);
			}
			if ( rightContour != null ) {
				imageA.unregisterVOI(rightContour);
			}
			if (latticeGrid != null) {
				for (int i = latticeGrid.size() - 1; i >= 0; i--) {
					final VOI marker = latticeGrid.remove(i);
					imageA.unregisterVOI(marker);
				}
			}
			if (centerLine != null) {
				imageA.unregisterVOI(centerLine);
			}
			if (rightLine != null) {
				imageA.unregisterVOI(rightLine);
			}
			if (leftLine != null) {
				imageA.unregisterVOI(leftLine);
			}
			imageA.notifyImageDisplayListeners();
		}
//		VOIVector vois = imageA.getVOIs();
//		for (int i = 0; i < vois.size(); i++) {
//			System.err.println( vois.elementAt(i).getName() );
//		}
	}


	/**
	 * VOI operation undo.
	 */
	public void undo() {
		updateLinks();
	}

	/**
	 * Entry point in the lattice-based straightening algorithm. At this point a lattice must be defined, outlining how
	 * the worm curves in 3D. A lattice is defined ad a VOI with two curves of equal length marking the left-hand and
	 * right-hand sides or the worm.
	 * 
	 * @param displayResult, when true intermediate volumes and results are displayed as well as the final straightened
	 *            image.
	 */
	public void untwistImage( final boolean mainImage ) {

		if ( !latticeInterpolationInit ) {
			initializeInterpolation(mainImage);
		}

		final int[] resultExtents = new int[] {((2 * extent)), ((2 * extent)), samplingPlanes.getCurves().size()};
		long time = System.currentTimeMillis();
		untwist(imageA, resultExtents, true);
		System.err.println( "untwist elapsed time =  " + AlgorithmBase.computeElapsedTime(time) );
		time = System.currentTimeMillis();
		if ( mainImage ) {
			time = System.currentTimeMillis();
			untwistLattice(imageA, resultExtents);
			System.err.println( "untwistLattice elapsed time =  " + AlgorithmBase.computeElapsedTime(time) );
		}
	}


	/**
	 * Entry point in the lattice-based straightening algorithm. At this point a lattice must be defined, outlining how
	 * the worm curves in 3D. A lattice is defined ad a VOI with two curves of equal length marking the left-hand and
	 * right-hand sides or the worm.
	 * 
	 * @param displayResult, when true intermediate volumes and results are displayed as well as the final straightened
	 *            image.
	 */
	public void untwistMarkers(boolean untwistAll) {
		if ( markerCenters == null ) {
			return;
		}

		if ( !latticeInterpolationInit ) {
			initializeInterpolation(false);
		}

		final int[] resultExtents = new int[] {((2 * extent)), ((2 * extent)), samplingPlanes.getCurves().size()};
		//			untwistMarkers(imageA);
		//		untwistMarkers(imageA, resultExtents, !useLatticeModel);
		untwistMarkers(imageA, resultExtents);
	}
	
	
	/**
	 * Untwists the worm image quickly for the preview mode - without saving any images or statistics
	 * @return untwisted image.
	 */
	public ModelImage[] untwistTest(VolumeImage[] stack)
	{
		initializeInterpolation(false);
		final int[] resultExtents = new int[] {((2 * extent)), ((2 * extent)), samplingPlanes.getCurves().size()};
		return untwistTest(stack, resultExtents);

	}

	public void updateLattice( boolean isLeft, VOIWormAnnotation text, VOIWormAnnotation newText) {
		VOI side = right;
		if ( isLeft ) {
			side = left;
		}
		if ( side == null ) return;
		boolean found = false;
		for ( int i = 0; i < side.getCurves().size(); i++ ) {
			if ( side.getCurves().elementAt(i) == text ) {
				VOIWormAnnotation selected = ((VOIWormAnnotation)side.getCurves().elementAt(i));
				selected.copy(newText);
				selected.updateText();
				selected.update();

				selected.setSelected(true);
				selected.updateSelected(imageA);
				pickedPoint = selected.elementAt(0);
				found = true;
				break;
			}
		}
		if ( found ) {
			updateLattice(false);
		}
	}
	
	public void updateAnnotation( VOIWormAnnotation annotation )
	{
		int pickedAnnotation = -1;
		for ( int i = 0; i < annotationVOIs.getCurves().size(); i++ )
		{
			if ( annotationVOIs.getCurves().elementAt(i) == annotation )
			{
				pickedAnnotation = i;
				break;
			}
		}
		if (pickedAnnotation != -1)
		{
			final VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(pickedAnnotation);
			text.update();
			text.retwist(previewMode);
			text.setSelected(true);
			text.updateSelected(imageA);
			pickedPoint = text.elementAt(0);
			//			updateSelected();

			if ( text.getText().equalsIgnoreCase("nose") || text.getText().equalsIgnoreCase("origin") )
			{
				if ( wormOrigin == null )
				{
					wormOrigin = new Vector3f(pickedPoint);
				}
				wormOrigin.copy(pickedPoint);
				// updateLattice(false);
			}
		}
		else
		{
			System.err.println("No matching VOI");
		}		
		updateAnnotationListeners();
	}

	public void updateSelectedPoint( Color color )
	{
		Vector<VOIWormAnnotation> selectedAnnotations = getPickedAnnotation();
		if ( selectedAnnotations != null ) {
			for ( int i = 0; i < selectedAnnotations.size(); i++ ) {
				final VOIWormAnnotation text = selectedAnnotations.elementAt(i);

				if ( !match(text.getColor(), color) )
				{
					text.setColor(color);
				}
				else
				{
					text.setColor(Color.blue);
				}
				text.updateText();
				colorAnnotations();
			}
			updateAnnotationListeners();
		}
	}


	/**
	 * Generates the set of natural spline curves to fit the current lattice.
	 */
	protected boolean generateCurves( float stepSize ) {
		
		//TODO: temporary, keep relative cross sections
		updateRelativeCrossSectionsFromDisplayContours();
		
		clearCurves(false);

		// 1. The center line of the worm is calculated from the midpoint between the left and right points of the
		// lattice.
		center = new VOIContour(false);
		float maxWormWidth = -1;
		for (int i = 0; i < left.getCurves().size(); i++)
		{
			final Vector3f centerPt = Vector3f.add(left.getCurves().elementAt(i).elementAt(0), right.getCurves().elementAt(i).elementAt(0));
			centerPt.scale(0.5f);
			center.add(centerPt);
			
			float width = left.getCurves().elementAt(i).elementAt(0).distance(right.getCurves().elementAt(i).elementAt(0));			
			if ( width > maxWormWidth ) {
				maxWormWidth = width;
			}
		}

		// 2. The center curve is generated from the center points using natural splines
		// to fit the points. Natural splines generate curves that pass through the control points, have continuous
		// first and second derivatives and minimize the bending between points.
		afTimeC = new float[center.size()];
		centerSpline = smoothCurve(center, afTimeC);

		centerPositions = new VOIContour(false);
		leftPositions = new VOIContour(false);
		rightPositions = new VOIContour(false);

		wormDiameters = new Vector<Float>();
		rightVectors = new Vector<Vector3f>();
		upVectors = new Vector<Vector3f>();
		normalVectors = new Vector<Vector3f>();
		
		curvatureNormals = new Vector<Vector3f>();
		curvature = new Vector<Float>();
		
		ellipseCurvesVOI = new VOI((short)0, "ellipseCurvesVOI");;
		ellipseCurves = new VOIContour[numEllipsePts];
//		ellipseControlPts = new VOIContour[numEllipsePts];
		for ( int i = 0; i < numEllipsePts; i++ ) {
			ellipseCurves[i] = new VOIContour(false);
			ellipseCurvesVOI.getCurves().add(ellipseCurves[i]);
		}
		
		selectedSectionVOI = new VOI((short)1, "selectedSectionVOI");
		selectedSectionVOI.setColor(Color.CYAN);


		// 3. The center curve is uniformly sampled along the length of the curve.
		// The step size is set to be one voxel. This determines the length of the final straightened
		// image and ensures that each slice in the straightened image is equally spaced. The points
		// along the curve are the center-points of the output slices.
		// 4. Each spline can be parametrized with a parameter t, where the start of the curve has t = 0
		// and the end of the curve has t = 1. This parameter t is calculated on the center curve, and
		// used to determine the corresponding locations on the left and right hand curves, which may be
		// longer or shorter than the center curve, depending on how the worm bends. Using the parametrization
		// ensures that the left and right hand curves are sampled the same number of times as the center curve
		// and that the points from start to end on all curves are included.
		// 5. Given the current point on the center curve and the corresponding positions on the left and right hand
		// curves, the 2D sampling plane can be defined. The center point of the plane is the current point on the
		// center curve.
		// The plane normal is the first derivative of the center line spline. The plane horizontal axis is the vector
		// from the position on the left hand curve to the position on the right hand curve.
		// The plane vertical axis is the cross-product of the plane normal with the plane horizontal axis.
		// This method fully defines the sample plane location and orientation as it sweeps through the 3D volume of the
		// worm.

		float length = centerSpline.GetLength(0, 1) / stepSize;
		int maxLength = (int)Math.ceil(length);
		float step = stepSize;
		if ( maxLength != length )
		{
			step = stepSize * (length / maxLength);
		}
		allTimes = new float[maxLength + 1];
		for (int i = 0; i <= maxLength; i++) {
			final float t = centerSpline.GetTime(i*step);
			centerPositions.add(centerSpline.GetPosition(t));
			allTimes[i] = t;
		}

		extent = -1;
		float minDiameter = Float.MAX_VALUE;

		int maxIndex = -1;
		splineRangeIndex = new int[center.size()];
		for ( int i = 0; i < center.size(); i++ )
		{
			splineRangeIndex[i] = -1;
			int minIndex = -1;
			float minDist = Float.MAX_VALUE;
			for ( int j = 0; j <= maxLength; j++ )
			{	
				//				final float t = centerSpline.GetTime(j*step);
				//				Vector3f centerPt = centerSpline.GetPosition(t);


				Vector3f centerPt = centerPositions.elementAt(j);
				if ( centerPt.isEqual(center.elementAt(i)) )
				{
					splineRangeIndex[i] = j;
					break;
				}
				float distance = centerPt.distance(center.elementAt(i) );
				if ( distance < minDist )
				{
					minDist = distance;
					minIndex = j;
				}
			}
			if ( (splineRangeIndex[i] == -1) && (minIndex != -1 ))
			{
				splineRangeIndex[i] = minIndex;
			}
			//			System.err.println( i + " " + indexes[i] );

			if ( i == 0 )
			{
				maxIndex = splineRangeIndex[i];
			}
			if ( splineRangeIndex[i] < maxIndex )
			{
				return false;
			}
			if ( splineRangeIndex[i] > maxIndex )
			{
				maxIndex = splineRangeIndex[i];
			}
		}

		if ( seamCellIDs != null )
		{
			allSeamCellIDs = new int[maxLength+1][4];
		}
		float[] diameters = new float[maxLength+1];
		Vector3f[] rightVectorsInterp = new Vector3f[maxLength+1];
		for ( int i = 0; i < center.size()-1; i++ )
		{
			int startIndex = splineRangeIndex[i];
			int endIndex = splineRangeIndex[i+1];
			//			System.err.println( startIndex + "   " + endIndex );
			float startRadius = (left.getCurves().elementAt(i).elementAt(0).distance(right.getCurves().elementAt(i).elementAt(0)))/2f;
			float endRadius = (left.getCurves().elementAt(i+1).elementAt(0).distance(right.getCurves().elementAt(i+1).elementAt(0)))/2f;
			for ( int j = startIndex; j <= endIndex; j++ )
			{
				float interp = (j - startIndex) / (float)(endIndex - startIndex);
				diameters[j] = (1 - interp) * startRadius + interp * endRadius;

				//				System.err.println( j + "     " + diameters[j] );
			}

			Vector3f startRight = Vector3f.sub( right.getCurves().elementAt(i).elementAt(0), left.getCurves().elementAt(i).elementAt(0) );
			startRight.normalize();
			Vector3f endRight = Vector3f.sub( right.getCurves().elementAt(i+1).elementAt(0), left.getCurves().elementAt(i+1).elementAt(0) );
			endRight.normalize();

			//			startRight.copy(Vector3f.UNIT_X);
			//			endRight.copy(Vector3f.UNIT_Y);
			Vector3f rotationAxis = Vector3f.cross(startRight, endRight);
			rotationAxis.normalize();
			float angle = startRight.angle(endRight);
			int steps = endIndex - startIndex;
			float fAngle = angle / steps;
			Matrix3f mat = new Matrix3f(true);
			mat.fromAxisAngle(rotationAxis, angle);
			//			System.err.println( rotationAxis + "   " + angle );


			for ( int j = startIndex; j <= endIndex; j++ )
			{
				float interp = (j - startIndex) / (float)(endIndex - startIndex);
				diameters[j] = (1 - interp) * startRadius + interp * endRadius;

				if ( allSeamCellIDs != null )
				{
					// left ID:
					allSeamCellIDs[j][0] = seamCellIDs[i][0];
					allSeamCellIDs[j][1] = seamCellIDs[i+1][0];
					// right ID:
					allSeamCellIDs[j][2] = seamCellIDs[i][1];
					allSeamCellIDs[j][3] = seamCellIDs[i+1][1];
				}


				mat.fromAxisAngle(rotationAxis, (j - startIndex)*fAngle);
				rightVectorsInterp[j] = mat.multRight(startRight);
				rightVectorsInterp[j].normalize();

				//				System.err.println( j + "    " + startRadius + " " + diameters[j] + " " + endRadius );
				//				System.err.println( j + "    " + startRight + "    " + rightVectorsInterp[j] + "    " + endRight );
			}
			float diffAngle = rightVectorsInterp[endIndex].angle(endRight);
			//			System.err.println(i + " " + rightVectorsInterp[endIndex]);
			//			System.err.println(i + " " + endRight + "    " + diffAngle);
			if ( (diffAngle/Math.PI)*180 > 2 )
			{
				angle = (float) ((2*Math.PI) - angle); 				
				fAngle = angle / steps;
				//				System.err.println( "TRYING AGAIN" );
				//				System.err.println( rotationAxis + "   " + angle );

				for ( int j = startIndex; j <= endIndex; j++ )
				{					
					mat.fromAxisAngle(rotationAxis, (j - startIndex)*fAngle);
					rightVectorsInterp[j] = mat.multRight(startRight);
					rightVectorsInterp[j].normalize();
					//					System.err.println( j + "    " + startRadius + " " + diameters[j] + " " + endRadius );
					//					System.err.println( j + "    " + startRight + "    " + rightVectorsInterp[j] + "    " + endRight );
				}
			}
			diffAngle = rightVectorsInterp[endIndex].angle(endRight);
			if ( (diffAngle/Math.PI)*180 > 2 )
			{
				//				System.err.println("ERROR");
			}
		}

		for (int i = 0; i <= maxLength; i++) {
			final float t = allTimes[i];
			curvature.add(centerSpline.GetCurvature(t));
			curvatureNormals.add(centerSpline.GetNormal(t));
			
			Vector3f normal = centerSpline.GetTangent(t);
			final Vector3f rightDir = rightVectorsInterp[i];
			
			float diameter = diameters[i];
			// interpolate diameter from head->max->tail
//			if ( i < splineMaxWidthIndex ) 
//			{
//				float interp = (i) / (float)(splineMaxWidthIndex);
//				diameter = (1 - interp) * headWidth + interp * maxWormWidth;
//			}
//			if ( i == splineMaxWidthIndex ) 
//			{
//				diameter = maxWormWidth;
//			}
//			if ( i > splineMaxWidthIndex ) 
//			{
//				float interp = (i - splineMaxWidthIndex) / (float)(maxLength - splineMaxWidthIndex);
//				diameter = (1 - interp) * maxWormWidth + interp * tailWidth;
//			}
//			diameter /= 2.0f;
			if (diameter > extent) {
				extent = (int) Math.ceil(diameter);
			}
			if ( (diameter > 0) && (diameter < minDiameter) )
			{
				minDiameter = diameter;
			}
			wormDiameters.add(diameter);
			rightVectors.add(rightDir);
			if (rightDir == null || normal == null )
			{
				System.err.println("error");
			}

			final Vector3f upDir = Vector3f.cross(normal, rightDir);
			upDir.normalize();
			upVectors.add(upDir);

			Vector3f normalTest = Vector3f.cross(rightDir, upDir);
			normalTest.normalize();
			normalVectors.add(normalTest);

			Vector3f rightPt = new Vector3f(rightDir);
			rightPt.scale(diameter);
			rightPt.add(centerPositions.elementAt(i));
			rightPositions.add(rightPt);
			//			
			Vector3f leftPt = new Vector3f(rightDir);
			leftPt.scale(-diameter);
			leftPt.add(centerPositions.elementAt(i));
			leftPositions.add(leftPt);


			//			System.err.println( i + "     " + diameters[i] + "     " + leftPt.distance(rightPt));
		}		

		extent += DiameterBuffer;
		for ( int i = 0; i < wormDiameters.size(); i++ )
		{
			if ( wormDiameters.elementAt(i) < minDiameter )
			{
				wormDiameters.set(i, minDiameter);
			}
		}


		/*
		 * For each afTimeC (center spline times) search for nearest allTimes index
		 */
		latticeSlices = new int[left.getCurves().size()];
		for ( int i = 0; i < afTimeC.length; i++ )
		{
			float min = Float.MAX_VALUE;
			int minIndex = -1;
			for ( int j = 0; j < allTimes.length; j++ )
			{
				float diff = Math.abs(allTimes[j] - afTimeC[i]);
				if ( diff < min )
				{
					min = diff;
					minIndex = j;
				}
			}
			latticeSlices[i] = minIndex;
		}

		// 6. Once the sample planes are defined, the worm cross-section within each plane needs to be determined.
		// Without a model of the worm cross-section the sample planes will overlap in areas where the worm folds
		// back on top of itself. The first step in modeling the worm cross-section is to define an ellipse
		// within each sample plane, centered in the plane. The long axis of the ellipse is parallel to the
		// horizontal axis of the sample plane. The length is the distance between the left and right hand points.
		// The ellipse short axis is in the direction of the plane vertical axis; the length is set to 1/2 the length
		// of the ellipse long axis. This ellipse-based model approximates the overall shape of the worm, however
		// it cannot model how the worm shape changes where sections of the worm press against each other.
		// The next step of the algorithm attempts to solve this problem.
		updateEllipseModel(stepSize);
		
		// Load in cross sections, update again if they exist
		if(readCrossSections()) {
			updateEllipseModel(stepSize);
		}

		

		
		short sID = (short) 1;		
		sID++;
		centerLine = new VOI(sID, "center line");
		centerLine.getCurves().add(centerPositions);
		centerLine.setColor(Color.red);
		centerPositions.update(new ColorRGBA(1, 0, 0, 1));

		sID++;
		leftLine = new VOI(sID, "left line");
		leftLine.getCurves().add(leftPositions);
		leftLine.setColor(Color.magenta);
		leftPositions.update(new ColorRGBA(1, 0, 1, 1));

		sID++;
		rightLine = new VOI(sID, "right line");
		rightLine.getCurves().add(rightPositions);
		rightLine.setColor(Color.green);
		rightPositions.update(new ColorRGBA(0, 1, 0, 1));

		imageA.registerVOI(leftLine);
		imageA.registerVOI(rightLine);
		imageA.registerVOI(centerLine);

		
//		checkModel(stepSize);
		
		return true;
	}
	
	private void updateEllipseModel(float stepSize) {
		
		/*
		 * We only want to create ellipses that we have not started editing.
		 * 
		 * Otherwise, we want to re-center the cross sections.
		 */

        short sID = 1;
        
        VOI[] oldDisplayContours = displayContours;
        
//		numEllipsePts = 16;
		displayContours = new VOI[latticeSlices.length + numEllipsePts];
		displayContours2 = new VOI((short)0, "wormContours2");
		
		// Assumption: The first few cross sections correspond to the old ones
		// We only remove and add cross sections at the end
		// TODO: Validate this assumption
		if(editedCrossSections.length != latticeSlices.length) {
			editedCrossSections = Arrays.copyOf(editedCrossSections, latticeSlices.length);
		}

		int contourCount = 0;
		for (int i = 0; i < centerPositions.size(); i++ ) {
//			System.err.print( i + "," + curvature.elementAt(i) );
			
			Vector3f rkEye = centerPositions.elementAt(i);
			Vector3f rkRVector = rightVectors.elementAt(i);
			Vector3f rkUVector = upVectors.elementAt(i);

			VOIContour ellipse = new VOIContour(true);
			ellipse.setVolumeDisplayRange(minRange);
			
			float radius = wormDiameters.elementAt(i);
			radius += paddingFactor;

			makeEllipse2DA(rkRVector, rkUVector, rkEye, radius, curvatureNormals.elementAt(i), curvature.elementAt(i), ellipse, ellipseCross, ellipseScale );	

			for ( int j = 0; j < numEllipsePts; j++ )
			{
				// ellipse curves are longitudinal contours
				ellipseCurves[j].add( new Vector3f(ellipse.elementAt(j)) );
			}
			
			if ( !splineModel) {
				displayContours2.getCurves().add(ellipse);
			}


//			boolean contourAdded = false;
			for ( int j = 0; j < latticeSlices.length; j++ ) {
				if ( i == latticeSlices[j]) 
				{
					String name = "wormContours_" + contourCount;
					
					if(editedCrossSections[contourCount]) {
						System.out.println("Cross section " + contourCount + " has been edited!");
						VOIContour crossSection = new VOIContour(true);
						makeCrossSectionFromRelative(rkRVector, rkUVector, rkEye, crossSection, j);
						
						displayContours[contourCount] = new VOI(sID, name, VOI.CONTOUR, (float) Math.random());
						displayContours[contourCount].getCurves().add(crossSection);
					} else { 
						displayContours[contourCount] = new VOI(sID, name, VOI.CONTOUR, (float) Math.random());
						displayContours[contourCount].getCurves().add(ellipse);
					}
					contourCount++;					
//					displayContours2.getCurves().add(ellipse);
//					contourAdded = true;
					break;
				}
			}
//			if ( !contourAdded && (i%contourSteps == 0) ) {
//				displayContours2.getCurves().add(ellipse);
//			}
		}

		for ( int i = 0; i < numEllipsePts; i++ ) {
			ellipseCurves[i].update(new ColorRGBA(0,.3f,.7f,1));
		}

//		System.err.println("");
//		System.err.println("");
//		for ( int j = 0; j < latticeSlices.length; j++ ) {
//			System.err.println( latticeSlices[j] );
//		}
//		System.err.println("");
//		System.err.println("");
		
		if ( splineModel) {
			int numContours = contourCount;
			NaturalSpline3[] edgeSplines = new NaturalSpline3[numEllipsePts];
			for ( int i = 0; i < numEllipsePts; i++ ) {
				float[] tempTime = new float[numContours];
				VOIContour temp = new VOIContour(false);
				final Vector3f[] akPoints = new Vector3f[numContours];

				for ( int j = 0; j < numContours; j++ ) {

					temp.add( new Vector3f(displayContours[j].getCurves().elementAt(0).elementAt(i)) );
					
					akPoints[j] = new Vector3f(displayContours[j].getCurves().elementAt(0).elementAt(i));

				}
				
				edgeSplines[i] = new NaturalSpline3(NaturalSpline3.BoundaryType.BT_FREE, numContours - 1, afTimeC, akPoints); // smoothCurve(temp, tempTime);

//				float tp = 0;
//				float steps = 0;
//				for ( int j = 1; j < latticeSlices.length; j++ ) {
//					float t = allTimes[latticeSlices[j]];
//					steps = latticeSlices[j] - latticeSlices[j-1];
//					System.err.println( latticeSlices[j] + "   " + edgeSplines[i].GetLength( tp, t ) + "   " + (edgeSplines[i].GetLength( tp, t )/steps) );
//					tp = t;
//				}		
//				System.err.println( edgeSplines[i].GetLength(0,1) );

			}
			maxSplineLength = centerPositions.size();
			for ( int i = 0; i < numEllipsePts; i++ ) {
				VOIContour tempContour = new VOIContour(false);
				for (int j = 0; j < maxSplineLength; j++) {
					final float t = j*(1f/(float)(maxSplineLength-1));
					tempContour.add(edgeSplines[i].GetPosition(t));
				}					
				String name = "wormContours_" + contourCount;
				displayContours[contourCount] = new VOI(sID, name, VOI.CONTOUR, (float) Math.random());
				displayContours[contourCount].getCurves().add(tempContour);
				contourCount++;
			}
//			System.err.println( numContours + " " + displayContours.length );
			for ( int i = 0; i < maxSplineLength; i++ ) {
				VOIContour ellipse = new VOIContour(true);

				for ( int j = 0; j < numEllipsePts; j++ ) {
					ellipse.add( displayContours[numContours + j].getCurves().elementAt(0).elementAt(i) );
				}
				displayContours2.getCurves().add(ellipse);
			}
			maxSplineLength = displayContours2.getCurves().size();
		}
		else {		
			maxSplineLength = displayContours2.getCurves().size();
			for ( int i = 0; i < numEllipsePts; i++ ) {
				VOIContour tempContour = new VOIContour(false);
				for (int j = 0; j < maxSplineLength; j++) {
					tempContour.add(displayContours2.getCurves().elementAt(j).elementAt(i));
				}					
				String name = "wormContours_" + contourCount;
				displayContours[contourCount] = new VOI(sID, name, VOI.CONTOUR, (float) Math.random());
				displayContours[contourCount].getCurves().add(tempContour);
				contourCount++;
			}
		}
	
		
		
//		System.err.println( "generateCurves " + maxSplineLength + "  " + left.getCurves().size() );
	}
	
	public void updateSplinesOnly()
	{
        short sID = 1;
		int contourCount = latticeSlices.length;
		int numContours = contourCount;
		
		displayContours2 = new VOI((short)0, "wormContours2");
		
		NaturalSpline3[] edgeSplines = new NaturalSpline3[numEllipsePts];
		
		
		for ( int i = numContours; i < displayContours.length; i++ ) {
			imageA.unregisterVOI(displayContours[i]);
			displayContours[i].dispose();
		}
		
		for ( int i = 0; i < numEllipsePts; i++ ) {
			float[] tempTime = new float[numContours];
			VOIContour temp = new VOIContour(false);
			final Vector3f[] akPoints = new Vector3f[numContours];

			for ( int j = 0; j < numContours; j++ ) {

				temp.add( new Vector3f(displayContours[j].getCurves().elementAt(0).elementAt(i)) );
				
				akPoints[j] = new Vector3f(displayContours[j].getCurves().elementAt(0).elementAt(i));

			}
			
			edgeSplines[i] = new NaturalSpline3(NaturalSpline3.BoundaryType.BT_FREE, numContours - 1, afTimeC, akPoints); // smoothCurve(temp, tempTime);

//			float tp = 0;
//			float steps = 0;
//			for ( int j = 1; j < latticeSlices.length; j++ ) {
//				float t = allTimes[latticeSlices[j]];
//				steps = latticeSlices[j] - latticeSlices[j-1];
//				System.err.println( latticeSlices[j] + "   " + edgeSplines[i].GetLength( tp, t ) + "   " + (edgeSplines[i].GetLength( tp, t )/steps) );
//				tp = t;
//			}		
//			System.err.println( edgeSplines[i].GetLength(0,1) );

		}
		maxSplineLength = centerPositions.size();
		for ( int i = 0; i < numEllipsePts; i++ ) {
			VOIContour tempContour = new VOIContour(false);
			for (int j = 0; j < maxSplineLength; j++) {
				final float t = j*(1f/(float)(maxSplineLength-1));
				tempContour.add(edgeSplines[i].GetPosition(t));
			}					
			String name = "wormContours_" + contourCount;
			displayContours[contourCount] = new VOI(sID, name, VOI.CONTOUR, (float) Math.random());
			displayContours[contourCount].getCurves().add(tempContour);
			contourCount++;
		}
//		System.err.println( numContours + " " + displayContours.length );
		for ( int i = 0; i < maxSplineLength; i++ ) {
			VOIContour ellipse = new VOIContour(true);

			for ( int j = 0; j < numEllipsePts; j++ ) {
				ellipse.add( displayContours[numContours + j].getCurves().elementAt(0).elementAt(i) );
			}
			displayContours2.getCurves().add(ellipse);
		}
		maxSplineLength = displayContours2.getCurves().size();
		
		// color appropriately if editing cross sections
		if (editingCrossSections) {
			ColorRGBA c;
			for( int i = 0; i < displayContours.length; i++ ) {
				if( i == selectedSectionIndex || i == selectedSectionIndex2 + latticeSlices.length) {
					c = new ColorRGBA(1.0f, 1.0f, 0f, 1.0f);
				} else {
					c = new ColorRGBA(0.5f, 0.5f, 0.5f, 0.5f);
				}
				displayContours[i].getCurves().elementAt(0).update(c);
			}
		}
		
		for ( int i = numContours; i < displayContours.length; i++ ) {
			imageA.registerVOI(displayContours[i]);
		}
	}
	
	public void updateSelectedSplinesOnly(int selectedEllipsePt)
	{
        short sID = 1;
		int contourCount = latticeSlices.length;
		int numContours = contourCount;

		displayContours2 = new VOI((short)0, "wormContours2");

		NaturalSpline3[] edgeSplines = new NaturalSpline3[numEllipsePts];


		for ( int i = numContours; i < displayContours.length; i++ ) {
			imageA.unregisterVOI(displayContours[i]);
			displayContours[i].dispose();
		}
		
		for ( int i = 0; i < numEllipsePts; i++ ) {
			float[] tempTime = new float[numContours];
			VOIContour temp = new VOIContour(false);
			final Vector3f[] akPoints = new Vector3f[numContours];

			for ( int j = 0; j < numContours; j++ ) {

				temp.add( new Vector3f(displayContours[j].getCurves().elementAt(0).elementAt(i)) );
				
				akPoints[j] = new Vector3f(displayContours[j].getCurves().elementAt(0).elementAt(i));

			}
			
			edgeSplines[i] = new NaturalSpline3(NaturalSpline3.BoundaryType.BT_FREE, numContours - 1, afTimeC, akPoints); // smoothCurve(temp, tempTime);

//			float tp = 0;
//			float steps = 0;
//			for ( int j = 1; j < latticeSlices.length; j++ ) {
//				float t = allTimes[latticeSlices[j]];
//				steps = latticeSlices[j] - latticeSlices[j-1];
//				System.err.println( latticeSlices[j] + "   " + edgeSplines[i].GetLength( tp, t ) + "   " + (edgeSplines[i].GetLength( tp, t )/steps) );
//				tp = t;
//			}		
//			System.err.println( edgeSplines[i].GetLength(0,1) );

		}
		maxSplineLength = centerPositions.size();
		for ( int i = 0; i < numEllipsePts; i++ ) {
			VOIContour tempContour = new VOIContour(false);
			for (int j = 0; j < maxSplineLength; j++) {
				final float t = j*(1f/(float)(maxSplineLength-1));
				tempContour.add(edgeSplines[i].GetPosition(t));
			}					
			String name = "wormContours_" + contourCount;
			displayContours[contourCount] = new VOI(sID, name, VOI.CONTOUR, (float) Math.random());
			displayContours[contourCount].getCurves().add(tempContour);
			contourCount++;
		}
//		System.err.println( numContours + " " + displayContours.length );
		for ( int i = 0; i < maxSplineLength; i++ ) {
			VOIContour ellipse = new VOIContour(true);

			for ( int j = 0; j < numEllipsePts; j++ ) {
				ellipse.add( displayContours[numContours + j].getCurves().elementAt(0).elementAt(i) );
			}
			displayContours2.getCurves().add(ellipse);
		}
		maxSplineLength = displayContours2.getCurves().size();
		
		for ( int i = numContours; i < displayContours.length; i++ ) {
			imageA.registerVOI(displayContours[i]);
		}
	}
	
	protected void updateRelativeCrossSectionsFromDisplayContours() {
		Vector3f centerPoint;
		Vector3f absolutePoint;
		Vector3f relativePoint;
		
		if(latticeSlices == null || center == null || displayContours == null)
			return;
		
		relativeCrossSections = new VOIContour[latticeSlices.length];
		
		System.out.println("Updating relative cross section from display contours");
		
		for ( int i = 0; i < latticeSlices.length; i++ ) {
			relativeCrossSections[i] = new VOIContour(true);
			centerPoint = center.elementAt(i); 
			for ( int j = 0; j < numEllipsePts; j++ ) {
				absolutePoint = displayContours[i].getCurves().elementAt(0).elementAt(j);
				relativePoint = Vector3f.sub(absolutePoint, centerPoint);
				relativeCrossSections[i].add(relativePoint);
				//if (i== 0)
				//	System.out.println("i: " + i + " j: " + j + " relativePoint:" + relativePoint + " magnitude: " + relativePoint.length());
			}
		}
		// displayContours
	}
	
	protected double GetSquared ( Vector3f point, Ellipsoid3f ellipsoid )
	{
		// compute coordinates of point in ellipsoid coordinate system
		Vector3d kDiff = new Vector3d( point.X - ellipsoid.Center.X, point.Y - ellipsoid.Center.Y, point.Z - ellipsoid.Center.Z);
		Vector3d kEPoint = new Vector3d( 
				(kDiff.X * ellipsoid.Axis[0].X + kDiff.Y * ellipsoid.Axis[0].Y + kDiff.Z * ellipsoid.Axis[0].Z),
				(kDiff.X * ellipsoid.Axis[1].X + kDiff.Y * ellipsoid.Axis[1].Y + kDiff.Z * ellipsoid.Axis[1].Z),
				(kDiff.X * ellipsoid.Axis[2].X + kDiff.Y * ellipsoid.Axis[2].Y + kDiff.Z * ellipsoid.Axis[2].Z) );

		final float[] afExtent = ellipsoid.Extent;
		double fA2 = afExtent[0]*afExtent[0];
		double fB2 = afExtent[1]*afExtent[1];
		double fC2 = afExtent[2]*afExtent[2];
		double fU2 = kEPoint.X*kEPoint.X;
		double fV2 = kEPoint.Y*kEPoint.Y;
		double fW2 = kEPoint.Z*kEPoint.Z;
		double fA2U2 = fA2*fU2, fB2V2 = fB2*fV2, fC2W2 = fC2*fW2;

		// initial guess
		double fURatio = kEPoint.X/afExtent[0];
		double fVRatio = kEPoint.Y/afExtent[1];
		double fWRatio = kEPoint.Z/afExtent[2];
		double fT;
		if (fURatio*fURatio+fVRatio*fVRatio+fWRatio*fWRatio < 1.0f)
		{
			fT = 0.0f;
		}
		else
		{
			double fMax = afExtent[0];
			if (afExtent[1] > fMax)
			{
				fMax = afExtent[1];
			}
			if (afExtent[2] > fMax)
			{
				fMax = afExtent[2];
			}

			fT = fMax*kEPoint.length();
		}

		// Newton's method
		final int iMaxIteration = 64;
		double fP = 1.0f, fQ = 1.0f, fR = 1.0f;
		for (int i = 0; i < iMaxIteration; i++)
		{
			fP = fT+fA2;
			fQ = fT+fB2;
			fR = fT+fC2;
			double fP2 = fP*fP;
			double fQ2 = fQ*fQ;
			double fR2 = fR*fR;
			double fS = fP2*fQ2*fR2-fA2U2*fQ2*fR2-fB2V2*fP2*fR2-fC2W2*fP2*fQ2;
			if (Math.abs(fS) < Mathf.ZERO_TOLERANCE)
			{
				break;
			}

			double fPQ = fP*fQ, fPR = fP*fR, fQR = fQ*fR, fPQR = fP*fQ*fR;
			double fDS = (2.0f)*(fPQR*(fQR+fPR+fPQ)-fA2U2*fQR*(fQ+fR)-
					fB2V2*fPR*(fP+fR)-fC2W2*fPQ*(fP+fQ));
			fT -= fS/fDS;
		}

		Vector3d kClosest = new Vector3d(fA2*kEPoint.X/fP,
				fB2*kEPoint.Y/fQ,
				fC2*kEPoint.Z/fR);
		kDiff = Vector3d.sub( kClosest, kEPoint );
		double fSqrDistance = kDiff.squaredLength();

		return fSqrDistance;
	}


	/**
	 * Interpolates the input contour so that the spacing between contour points is <= 1 voxel.
	 * 
	 * @param contour
	 */
	protected void interpolateContour(final VOIContour contour) {
		int index = 0;
		while (index < contour.size()) {
			final Vector3f p1 = contour.elementAt(index);
			final Vector3f p2 = contour.elementAt( (index + 1) % contour.size());
			// System.err.println( index + " " + (index+1)%contour.size() );
			final float distance = p1.distance(p2);
			if (distance > 1) {
				final Vector3f dir = Vector3f.sub(p2, p1);
				dir.normalize();
				final int count = (int) distance;
				final float stepSize = distance / (count + 1);
				float currentStep = stepSize;
				index++;
				for (int i = 0; i < count; i++) {
					final Vector3f newPt = new Vector3f();
					newPt.scaleAdd(currentStep, dir, p1);
					contour.add(index++, newPt);
					// System.err.println( "    adding pt at " + (index-1) + " " + newPt.distance(p1) + " " +
					// newPt.distance(p2) );
					currentStep += stepSize;
				}
			} else {
				index++;
			}
		}
	}

	protected Vector<Float> loadDiameters( String imageDir ) {

		String voiDir = imageDir;
		File voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			return null;
		}
		voiDir = imageDir + "statistics" + File.separator;

		voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) {
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) {} else { // voiFileDir does not exist
			return null;
		}

		File file = new File(voiDir + "Diameters.csv");
		if (file.exists()) {
			return null;
		}

		try {
			Vector<Float> diameters = new Vector<Float>();
			final FileReader fr = new FileReader(file);
			final BufferedReader br = new BufferedReader(fr);

			String line = br.readLine(); // first line is header
			line = br.readLine();

			while ( line != null )
			{
				StringTokenizer st = new StringTokenizer(line, ",");
				if (st.hasMoreTokens()) {
					diameters.add(Float.valueOf(st.nextToken()));
				}
				line = br.readLine();
			}

			br.close();

			return diameters;
		} catch (final Exception e) {
			System.err.println("CAUGHT EXCEPTION WITHIN saveNucleiInfo");
			e.printStackTrace();
		}
		return null;
	}
	
	protected VOIContour loadPositions( String imageDir, String name ) {

		String voiDir = imageDir;
		File voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			return null;
		}
		voiDir = imageDir + "statistics" + File.separator;

		voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) {
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) {} else { // voiFileDir does not exist
			return null;
		}

		File file = new File(voiDir + name + "Positions.csv");
		if ( !file.exists()) {
			return null;
		}

		try {
			VOIContour contour = new VOIContour(false);
			final FileReader fr = new FileReader(file);
			final BufferedReader br = new BufferedReader(fr);

			String line = br.readLine(); // first line is header
			line = br.readLine();

			while ( line != null )
			{
				Vector3f pos = new Vector3f();
				StringTokenizer st = new StringTokenizer(line, ",");
				if (st.hasMoreTokens()) {
					pos.X = Float.valueOf(st.nextToken());
				}
				if (st.hasMoreTokens()) {
					pos.Y = Float.valueOf(st.nextToken());
				}
				if (st.hasMoreTokens()) {
					pos.Z = Float.valueOf(st.nextToken());
				}

				contour.add(pos);
				line = br.readLine();
			}

			br.close();

			return contour;
		} catch (final Exception e) {
			System.err.println("CAUGHT EXCEPTION WITHIN saveNucleiInfo");
			e.printStackTrace();
		}
		return null;
	}

	protected VOI loadSamplePlanes( String imageDir ) {
		String voiDir = imageDir;
		File voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			return null;
		}
		voiDir = imageDir + "statistics" + File.separator;

		voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) {
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) {} else { // voiFileDir does not exist
			return null;
		}

		File file = new File(voiDir + "SamplePlanes.csv");
		if (file.exists()) {
			return null;
		}

		try {
			final FileReader fr = new FileReader(file);
			final BufferedReader br = new BufferedReader(fr);

			String line = br.readLine(); // first line is header
			line = br.readLine();

			final short sID = voiID++;
			VOI planes = new VOI(sID, "samplingPlanes");
			while ( line != null )
			{
				VOIContour contour = new VOIContour(true);
				StringTokenizer st = new StringTokenizer(line, ",");
				for ( int i = 0; i < 4; i++ )
				{
					Vector3f pos = new Vector3f();
					if (st.hasMoreTokens()) {
						pos.X = Float.valueOf(st.nextToken());
					}
					if (st.hasMoreTokens()) {
						pos.Y = Float.valueOf(st.nextToken());
					}
					if (st.hasMoreTokens()) {
						pos.Z = Float.valueOf(st.nextToken());
					}
					contour.add(pos);
				}

				planes.getCurves().add(contour);
				line = br.readLine();
			}

			br.close();

			return planes;
		} catch (final Exception e) {
			System.err.println("CAUGHT EXCEPTION WITHIN saveNucleiInfo");
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Generates the 3D 1-voxel thick ellipsoids used in the intial worm model.
	 * 
	 * @param right
	 * @param up
	 * @param center
	 * @param diameterA
	 * @param diameterB
	 * @param ellipse
	 * @return
	 */
	protected Ellipsoid3f makeEllipse(final Vector3f right, final Vector3f up, final Vector3f center, final float diameterA, final float diameterB,
			final VOIContour ellipse) {
		final double[] adCos = new double[32];
		final double[] adSin = new double[32];
		for (int i = 0; i < numEllipsePts; i++) {
			adCos[i] = Math.cos(Math.PI * 2.0 * i / numEllipsePts);
			adSin[i] = Math.sin(Math.PI * 2.0 * i / numEllipsePts);
		}
		for (int i = 0; i < numEllipsePts; i++) {
			final Vector3f pos1 = Vector3f.scale((float) (diameterA * adCos[i]), right);
			final Vector3f pos2 = Vector3f.scale((float) (diameterB * adSin[i]), up);
			final Vector3f pos = Vector3f.add(pos1, pos2);
			pos.add(center);
			ellipse.addElement(pos);
		}
		final float[] extents = new float[] {diameterA, diameterB, 1};
		final Vector3f[] axes = new Vector3f[] {right, up, Vector3f.cross(right, up)};
		return new Ellipsoid3f(center, axes, extents);
	}
	
	protected void makeEllipse2DA(final Vector3f right, final Vector3f up, final Vector3f center, final float[] radii,
			final VOIContour ellipse) {
		// does not actually make an ellipse, just makes a closed shape sampled along the central angle
		for (int i = 0; i < numEllipsePts; i++) {
			final double c = Math.cos(Math.PI * 2.0 * i / numEllipsePts);
			final double s = Math.sin(Math.PI * 2.0 * i / numEllipsePts);
			final Vector3f pos1 = Vector3f.scale((float) (radii[i] * c), right);
			final Vector3f pos2 = Vector3f.scale((float) (radii[i] * s), up);
			final Vector3f pos = Vector3f.add(pos1, pos2);
			pos.add(center);
			ellipse.addElement(pos);
			//			System.err.println(pos);
		}
	}


	protected void makeEllipse2DA(final Vector3f right, final Vector3f up, final Vector3f center, final float radius,
			final VOIContour ellipse) {
		for (int i = 0; i < numEllipsePts; i++) {
			final double c = Math.cos(Math.PI * 2.0 * i / numEllipsePts);
			final double s = Math.sin(Math.PI * 2.0 * i / numEllipsePts);
			final Vector3f pos1 = Vector3f.scale((float) (radius * c), right);
			final Vector3f pos2 = Vector3f.scale((float) (radius * s), up);
			final Vector3f pos = Vector3f.add(pos1, pos2);
			pos.add(center);
			ellipse.addElement(pos);
			//			System.err.println(pos);
		}
	}


	protected void makeEllipse2DA(final Vector3f right, final Vector3f up, final Vector3f center, final float radius, final Vector3f normal,
			final float curvature, final VOIContour ellipse, boolean ellipseCross, float scale) {
		
		float area = (float) (Math.PI * radius * radius);
		
		int curve = (int) (100 * curvature);
		float radiusC = curve / 100f;
//		float max = -Float.MAX_VALUE;		
		
		radiusC = 1 - radiusC;
		
		radiusC = Math.min(1, radiusC);
		radiusC *= radius;
		
		float radiusR = radius; // (float) (area / (Math.PI * radiusC));
		
//		System.err.println( "," + radius + "," + radiusC + "," + radiusR );
		
		if ( !ellipseCross ) {
			radiusC = radiusR = (radius * scale);
		}
		else {
			radiusC *= scale;
		}
		
		for (int i = 0; i < numEllipsePts; i++) {
			final double c = Math.cos(Math.PI * 2.0 * i / numEllipsePts);
			final double s = Math.sin(Math.PI * 2.0 * i / numEllipsePts);
			final Vector3f pos1 = Vector3f.scale((float) (radiusR * c), right);
			final Vector3f pos2 = Vector3f.scale((float) (radiusC * s), up);
			final Vector3f pos = Vector3f.add(pos1, pos2);
			
			
			
			
			
//			final Vector3f normalNeg = new Vector3f(normal); normalNeg.neg(); normalNeg.normalize();
//			final Vector3f posNorm = new Vector3f(pos); posNorm.normalize();
//			float dot = posNorm.dot(normalNeg);
//			float dotRight = Math.abs(posNorm.dot(right));
//			System.err.println( dotRight + "   " + right );
//			float dotRightScale = 1 - dotRight;
//			float scale = Math.abs(10 * curvature * dot * dotRightScale);
//            Vector3f posScale = new Vector3f(pos); posScale.scale((float) (0.5 + dotRight * 0.5));	
////			System.err.print((10*curvature) + "      " + dot + "      " +  + scale + "      " + pos + "  =>  " );
////            Vector3f temp = new Vector3f(pos);
////            
////            pos.sub(posScale);	
//            
//            if ( print ) System.err.println(i + "," + pos.length());
////            System.err.println((5*curvature) + "," + 100 * (temp.distance(pos) / radius));
////			System.err.println(pos);
//            
//            pos.copy(posScale);
            
//            float dist = 100 * (temp.distance(pos) / radius);
//            if ( dist > max ) {
//            	max = dist;
//            }
            
			pos.add(center);
//			posScale.add(center);
//			pos.scale(0.5f);
			ellipse.addElement(pos);
			//			System.err.println(pos);
		}
//		if ( !print ) System.err.println("," + max);
	}
	
	protected void makeCrossSectionFromRelative(final Vector3f right, final Vector3f up, final Vector3f center, final VOIContour crossSection, int sectionIndex) {
		
		for (int i = 0; i < numEllipsePts; i++) {
			final double c = Math.cos(Math.PI * 2.0 * i / numEllipsePts);
			final double s = Math.sin(Math.PI * 2.0 * i / numEllipsePts);
			final Vector3f old_pos = relativeCrossSections[sectionIndex].get(i);
			//Get old radius
			final double radius = old_pos.length();
			//System.out.println("Making cross section from relative: " + radius);
			final Vector3f pos1 = Vector3f.scale((float) (radius * c), right);
			final Vector3f pos2 = Vector3f.scale((float) (radius * s), up);
			final Vector3f pos = Vector3f.add(pos1, pos2);
			pos.add(center);
			crossSection.addElement(pos);
		}

	}


	/**
	 * Given a point in the twisted volume, calculates and returns the corresponding point in the straightened image.
	 * 
	 * @param model
	 * @param originToStraight
	 * @param pt
	 * @param text
	 * @return
	 */
	protected Vector3f originToStraight(final ModelImage model, final ModelImage originToStraight, final Vector3f pt, final String text) {
		final int x = Math.round(pt.X);
		final int y = Math.round(pt.Y);
		final int z = Math.round(pt.Z);

		final float outputA = originToStraight.getFloatC(x, y, z, 0);
		final float outputX = originToStraight.getFloatC(x, y, z, 1);
		final float outputY = originToStraight.getFloatC(x, y, z, 2);
		final float outputZ = originToStraight.getFloatC(x, y, z, 3);

		if (outputA == 0) {
			final float m = model.getFloat(x, y, z);
			if (m != 0) {
				final int dimX = model.getExtents().length > 0 ? model.getExtents()[0] : 1;
				final int dimY = model.getExtents().length > 1 ? model.getExtents()[1] : 1;
				final int dimZ = model.getExtents().length > 2 ? model.getExtents()[2] : 1;

				int count = 0;
				final Vector3f pts = new Vector3f();
				for (int z1 = Math.max(0, z - 2); z1 < Math.min(dimZ, z + 2); z1++) {
					for (int y1 = Math.max(0, y - 2); y1 < Math.min(dimY, y + 2); y1++) {
						for (int x1 = Math.max(0, x - 2); x1 < Math.min(dimX, x + 2); x1++) {
							final float a1 = originToStraight.getFloatC(x1, y1, z1, 0);
							final int m1 = model.getInt(x1, y1, z1);
							if ( (a1 != 0) && (m1 == m)) {
								final float x2 = originToStraight.getFloatC(x1, y1, z1, 1);
								final float y2 = originToStraight.getFloatC(x1, y1, z1, 2);
								final float z2 = originToStraight.getFloatC(x1, y1, z1, 3);
								pts.add(x2, y2, z2);
								count++;
							}
						}
					}
				}
				if (count != 0) {
					// System.err.println( imageA.getImageName() + " originToStraight " + text + " " + pt + " OK ");
					pts.scale(1f / count);
					return pts;
				}
			} else {
				//				System.err.println(imageA.getImageName() + " originToStraight " + text + " " + pt);
			}
		}

		return new Vector3f(outputX, outputY, outputZ);
	}

	/**
	 * Saves the annotation statistics to a file.
	 * 
	 * @param image
	 * @param model
	 * @param originToStraight
	 * @param outputDim
	 * @param postFix
	 * @return
	 */
	protected VOI saveAnnotationStatistics(final String imageDir, final ModelImage model, final ModelImage originToStraight, final int[] outputDim,
			final String postFix) {
		if (annotationVOIs == null) {
			return null;
		}
		if (annotationVOIs.getCurves().size() == 0) {
			return null;
		}
		
		String voiDir = imageDir;
		File voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			voiFileDir.mkdir();
		}
		voiDir = imageDir + "statistics" + File.separator;


		voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) {} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) {} else { // voiFileDir
			voiFileDir.mkdir();
		}

		File file = new File(voiDir + "AnnotationInfo" + postFix + ".csv");
		if (file.exists()) {
			file.delete();
			file = new File(voiDir + "AnnotationInfo" + postFix + ".csv");
		}

		VOI transformedAnnotations = null;
		try {
			if (originToStraight != null) {
				transformedAnnotations = new VOI(annotationVOIs);
			}

			final FileWriter fw = new FileWriter(file);
			final BufferedWriter bw = new BufferedWriter(fw);
			bw.write("name" + "," + "x_voxels" + "," + "y_voxels" + "," + "z_voxels" + "," + "x_um" + "," + "y_um" + "," + "z_um" + "\n");
			for (int i = 0; i < annotationVOIs.getCurves().size(); i++) {
				final VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
				Vector3f position = text.elementAt(0);
				if ( (model != null) && (originToStraight != null)) {
					position = originToStraight(model, originToStraight, position, text.getText());

					transformedAnnotations.getCurves().elementAt(i).elementAt(0).copy(position);
					transformedAnnotations.getCurves().elementAt(i).elementAt(1).set(position.X + 5, position.Y, position.Z);
				}
				bw.write(text.getText() + "," + (position.X - transformedOrigin.X) + "," + (position.Y - transformedOrigin.Y) + ","
						+ (position.Z - transformedOrigin.Z) + "," +

                        VOILatticeManagerInterface.VoxelSize * (position.X - transformedOrigin.X) + "," + VOILatticeManagerInterface.VoxelSize
                        * (position.Y - transformedOrigin.Y) + "," + VOILatticeManagerInterface.VoxelSize * (position.Z - transformedOrigin.Z) + "\n");
			}
			bw.newLine();
			bw.close();
		} catch (final Exception e) {
			System.err.println("CAUGHT EXCEPTION WITHIN writeXML() of FileVOI");
			e.printStackTrace();
		}

		return transformedAnnotations;
	}


	protected void saveDiameters( Vector<Float> diameters, String imageDir ) {

		if ( diameters == null )
		{
			return;
		}
		String voiDir = imageDir;
		File voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			//			System.err.println( "saveDiameters " + voiDir);
			voiFileDir.mkdir();
		}
		voiDir = imageDir + "statistics" + File.separator;

		voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) {
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) {} else { // voiFileDir does not exist
			//			System.err.println( "saveDiameters " + voiDir);
			voiFileDir.mkdir();
		}

		File file = new File(voiDir + "Diameters.csv");
		if (file.exists()) {
			file.delete();
			file = new File(voiDir + "Diameters.csv");
		}

		try {

			final FileWriter fw = new FileWriter(file);
			final BufferedWriter bw = new BufferedWriter(fw);

			bw.write("diameter" + "\n");
			for ( int i = 0; i < diameters.size(); i++ )
			{
				bw.write(diameters.elementAt(i) + "\n");				
			}

			bw.newLine();
			bw.close();
		} catch (final Exception e) {
			System.err.println("CAUGHT EXCEPTION WITHIN saveNucleiInfo");
			e.printStackTrace();
		}
	}


	/**
	 * Saves the lattice statistics to a file.
	 * 
	 * @param image
	 * @param length
	 * @param left
	 * @param right
	 * @param leftPairs
	 * @param rightPairs
	 * @param postFix
	 */
	protected void saveLatticeStatistics( String imageDir, final float length, final VOI left, final VOI right, final float[] leftPairs,
			final float[] rightPairs, final String postFix) {

		String voiDir = imageDir;
		File voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			voiFileDir.mkdir();
		}
		voiDir = imageDir + "statistics" + File.separator;

		voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) {
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) {} else { // voiFileDir does not exist
			voiFileDir.mkdir();
		}

		File file = new File(voiDir + "LatticeInfo" + postFix + ".csv");
		if (file.exists()) {
			file.delete();
			file = new File(voiDir + "LatticeInfo" + postFix + ".csv");
		}

		try {

			final FileWriter fw = new FileWriter(file);
			final BufferedWriter bw = new BufferedWriter(fw);
			bw.write("Total Length:," + VOILatticeManagerInterface.VoxelSize * length + "\n");
			bw.newLine();
			bw.write("pair" + "," + "diameter" + "," + "left distance" + "," + "right distance" + "\n");
			for (int i = 0; i < left.getCurves().size(); i++) {
				bw.write(i + "," + VOILatticeManagerInterface.VoxelSize * left.getCurves().elementAt(i).elementAt(0).distance(right.getCurves().elementAt(i).elementAt(0)) + ","
						+ VOILatticeManagerInterface.VoxelSize * leftPairs[i] + "," + VOILatticeManagerInterface.VoxelSize * rightPairs[i] + "\n");
			}
			bw.newLine();
			bw.close();
		} catch (final Exception e) {
			System.err.println("CAUGHT EXCEPTION WITHIN writeXML() of FileVOI");
			e.printStackTrace();
		}
	}


	protected void savePositions( VOIContour contour, String imageDir, String name ) {

		if ( contour == null )
		{
			return;
		}
		String voiDir = imageDir;
		File voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			//			System.err.println( "savePositions " + voiDir);
			voiFileDir.mkdir();
		}
		voiDir = imageDir + "statistics" + File.separator;

		voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) {
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) {} else { // voiFileDir does not exist
			//			System.err.println( "savePositions " + voiDir);
			voiFileDir.mkdir();
		}

		File file = new File(voiDir + name + "Positions.csv");
		if (file.exists()) {
			file.delete();
			file = new File(voiDir + name + "Positions.csv");
		}

		try {

			final FileWriter fw = new FileWriter(file);
			final BufferedWriter bw = new BufferedWriter(fw);

			bw.write("X" + "," + "Y" + "," + "Z" + "\n");
			for ( int i = 0; i < contour.size(); i++ )
			{
				Vector3f pos = contour.elementAt(i);
				bw.write(pos.X + "," + pos.Y + "," + pos.Z + "\n");				
			}

			bw.newLine();
			bw.close();
		} catch (final Exception e) {
			System.err.println("CAUGHT EXCEPTION WITHIN saveNucleiInfo");
			e.printStackTrace();
		}
	}


	protected void saveSamplePlanes( VOI planes, String imageDir ) {

		if ( planes == null )
		{
			return;
		}
		String voiDir = imageDir;
		File voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			//			System.err.println( "saveSamplePlanes " + voiDir);
			voiFileDir.mkdir();
		}
		voiDir = imageDir + "statistics" + File.separator;

		voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) {
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) {} else { // voiFileDir does not exist
			//			System.err.println( "saveSamplePlanes " + voiDir);
			voiFileDir.mkdir();
		}

		File file = new File(voiDir + "SamplePlanes.csv");
		if (file.exists()) {
			file.delete();
			file = new File(voiDir + "SamplePlanes.csv");
		}

		try {

			final FileWriter fw = new FileWriter(file);
			final BufferedWriter bw = new BufferedWriter(fw);

			bw.write("X1" + "," + "Y1" + "," + "Z1" + "X2" + "," + "Y2" + "," + "Z2" + "X3" + "," + "Y3" + "," + "Z3" + "X4" + "," + "Y4" + "," + "Z4" + "\n");
			for ( int i = 0; i < planes.getCurves().size(); i++ )
			{
				VOIContour kBox = (VOIContour) planes.getCurves().elementAt(i);
				for (int j = 0; j < 4; j++) {
					Vector3f pos = kBox.elementAt(j);
					if ( j < (4-1) )
					{
						bw.write(pos.X + "," + pos.Y + "," + pos.Z + ",");
					}
					else
					{
						bw.write(pos.X + "," + pos.Y + "," + pos.Z + "\n");
					}
				}
			}

			bw.newLine();
			bw.close();
		} catch (final Exception e) {
			System.err.println("CAUGHT EXCEPTION WITHIN saveNucleiInfo");
			e.printStackTrace();
		}
	}




	/**
	 * Generates the Natural Spline for the lattice center-line curve. Sets the time values for each point on the curve.
	 * 
	 * @param curve
	 * @param time
	 * @return
	 */
	protected NaturalSpline3 smoothCurve(final VOIContour curve, final float[] time) {
		float totalDistance = 0;
		for (int i = 0; i < curve.size() - 1; i++) {
			totalDistance += curve.elementAt(i).distance(curve.elementAt(i + 1));
		}

		final Vector3f[] akPoints = new Vector3f[curve.size()];
		float distance = 0;
		for (int i = 0; i < curve.size(); i++) {
			if (i > 0) {
				distance += curve.elementAt(i).distance(curve.elementAt(i - 1));
				time[i] = distance / totalDistance;
				akPoints[i] = new Vector3f(curve.elementAt(i));
			} else {
				time[i] = 0;
				akPoints[i] = new Vector3f(curve.elementAt(i));
			}
		}

		return new NaturalSpline3(NaturalSpline3.BoundaryType.BT_FREE, curve.size() - 1, time, akPoints);
	}

	public static NaturalSpline3 smoothCurve(final Vector<VOIWormAnnotation> controlPts) {
		float totalDistance = 0;
		for (int i = 0; i < controlPts.size() - 1; i++) {
			totalDistance += controlPts.elementAt(i).elementAt(0).distance(controlPts.elementAt(i + 1).elementAt(0));
		}

		final Vector3f[] akPoints = new Vector3f[controlPts.size()];
		float[] time = new float[controlPts.size()];
		float distance = 0;
		for (int i = 0; i < controlPts.size(); i++) {
			if (i > 0) {
				distance += controlPts.elementAt(i).elementAt(0).distance(controlPts.elementAt(i - 1).elementAt(0));
				time[i] = distance / totalDistance;
				akPoints[i] = new Vector3f(controlPts.elementAt(i).elementAt(0));
			} else {
				time[i] = 0;
				akPoints[i] = new Vector3f(controlPts.elementAt(i).elementAt(0));
			}
		}

		return new NaturalSpline3(NaturalSpline3.BoundaryType.BT_FREE, controlPts.size() - 1, time, akPoints);
	}


	protected Vector3f[] straightenFrame(final ModelImage image, int slice,
			final int[] extents, final Vector3f[] verts, Vector3f centerPos, Vector3f leftPos, Vector3f rightPos ) 
	{
		final int iBound = extents[0];
		final int jBound = extents[1];
		final int[] dimExtents = image.getExtents();

		final Vector3f center = new Vector3f();
		for (int i = 0; i < verts.length; i++) {
			center.add(verts[i]);
		}
		center.scale(1f / verts.length);

		/* Calculate the slopes for traversing the data in x,y,z: */
		float xSlopeX = verts[1].X - verts[0].X;
		float ySlopeX = verts[1].Y - verts[0].Y;
		float zSlopeX = verts[1].Z - verts[0].Z;

		float xSlopeY = verts[3].X - verts[0].X;
		float ySlopeY = verts[3].Y - verts[0].Y;
		float zSlopeY = verts[3].Z - verts[0].Z;

		float x0 = verts[0].X;
		float y0 = verts[0].Y;
		float z0 = verts[0].Z;

		xSlopeX /= (iBound);
		ySlopeX /= (iBound);
		zSlopeX /= (iBound);

		xSlopeY /= (jBound);
		ySlopeY /= (jBound);
		zSlopeY /= (jBound);

		/* loop over the 2D image (values) we're writing into */
		float x = x0;
		float y = y0;
		float z = z0;

		float minDistanceCenterPos = Float.MAX_VALUE;
		float minDistanceLeftPos = Float.MAX_VALUE;
		float minDistanceRightPos = Float.MAX_VALUE;
		Vector3f[] closest = new Vector3f[]{new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE),
				new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE),
				new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)	};

		Vector3f pt = new Vector3f();
		for (int j = 0; j < jBound; j++) {

			/* Initialize the first diagonal point(x,y,z): */
			x = x0;
			y = y0;
			z = z0;

			for (int i = 0; i < iBound; i++) {

				final int iIndex = Math.round(x);
				final int jIndex = Math.round(y);
				final int kIndex = Math.round(z);

				// Bounds checking:
				if ( ( (iIndex < 0) || (iIndex >= dimExtents[0])) || ( (jIndex < 0) || (jIndex >= dimExtents[1]))
						|| ( (kIndex < 0) || (kIndex >= dimExtents[2])) ) {

					// do nothing
				} else {
					pt.set(x, y, z);
					float dist = pt.distance(centerPos);
					if ( dist < minDistanceCenterPos )
					{
						minDistanceCenterPos = dist;
						closest[0].set(i, j, slice);
					}
					dist = pt.distance(leftPos);
					if ( dist < minDistanceLeftPos )
					{
						minDistanceLeftPos = dist;
						closest[1].set(i, j, slice);
					}
					dist = pt.distance(rightPos);
					if ( dist < minDistanceRightPos )
					{
						minDistanceRightPos = dist;
						closest[2].set(i, j, slice);
					}
				}
				/*
				 * Inner loop: Move to the next diagonal point along the x-direction of the plane, using the xSlopeX,
				 * ySlopeX and zSlopeX values:
				 */
				x = x + xSlopeX;
				y = y + ySlopeX;
				z = z + zSlopeX;
			}

			/*
			 * Outer loop: Move to the next diagonal point along the y-direction of the plane, using the xSlopeY,
			 * ySlopeY and zSlopeY values:
			 */
			x0 = x0 + xSlopeY;
			y0 = y0 + ySlopeY;
			z0 = z0 + zSlopeY;
		}
		return closest;
	}


	protected Vector3f writeDiagonal(final ModelImage image, int slice,
			final int[] extents, final Vector3f[] verts, Vector3f target, float[] minDistance ) 
	{
		final int iBound = extents[0];
		final int jBound = extents[1];
		final int[] dimExtents = image.getExtents();

		final Vector3f center = new Vector3f();
		for (int i = 0; i < verts.length; i++) {
			center.add(verts[i]);
		}
		center.scale(1f / verts.length);

		/* Calculate the slopes for traversing the data in x,y,z: */
		float xSlopeX = verts[1].X - verts[0].X;
		float ySlopeX = verts[1].Y - verts[0].Y;
		float zSlopeX = verts[1].Z - verts[0].Z;

		float xSlopeY = verts[3].X - verts[0].X;
		float ySlopeY = verts[3].Y - verts[0].Y;
		float zSlopeY = verts[3].Z - verts[0].Z;

		float x0 = verts[0].X;
		float y0 = verts[0].Y;
		float z0 = verts[0].Z;

		xSlopeX /= (iBound);
		ySlopeX /= (iBound);
		zSlopeX /= (iBound);

		xSlopeY /= (jBound);
		ySlopeY /= (jBound);
		zSlopeY /= (jBound);

		/* loop over the 2D image (values) we're writing into */
		float x = x0;
		float y = y0;
		float z = z0;

		minDistance[0] = Float.MAX_VALUE;
		Vector3f closest = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		Vector3f pt = new Vector3f();
		for (int j = 0; j < jBound; j++) {

			/* Initialize the first diagonal point(x,y,z): */
			x = x0;
			y = y0;
			z = z0;

			for (int i = 0; i < iBound; i++) {

				final int iIndex = Math.round(x);
				final int jIndex = Math.round(y);
				final int kIndex = Math.round(z);

				// Bounds checking:
				if ( ( (iIndex < 0) || (iIndex >= dimExtents[0])) || ( (jIndex < 0) || (jIndex >= dimExtents[1]))
						|| ( (kIndex < 0) || (kIndex >= dimExtents[2])) ) {

					// do nothing
				} else {
					pt.set(x, y, z);
					float dist = pt.distance(target);
					if ( dist < minDistance[0] )
					{
						minDistance[0] = dist;
						closest.set(i, j, slice);
					}
				}
				/*
				 * Inner loop: Move to the next diagonal point along the x-direction of the plane, using the xSlopeX,
				 * ySlopeX and zSlopeX values:
				 */
				x = x + xSlopeX;
				y = y + ySlopeX;
				z = z + zSlopeX;
			}

			/*
			 * Outer loop: Move to the next diagonal point along the y-direction of the plane, using the xSlopeY,
			 * ySlopeY and zSlopeY values:
			 */
			x0 = x0 + xSlopeY;
			y0 = y0 + ySlopeY;
			z0 = z0 + zSlopeY;
		}
		//		System.err.println( minDistance );
		return closest;
	}

	protected void writeDiagonal(final ModelImage image, final ModelImage result, final int tSlice, final int slice,
			final int[] extents, final Vector3f[] verts) {
		final int iBound = extents[0];
		final int jBound = extents[1];
		final int[] dimExtents = image.getExtents();

		/*
		 * Get the loop multiplication factors for indexing into the 1D array with 3 index variables: based on the
		 * coordinate-systems: transformation:
		 */
		final int iFactor = 1;
		final int jFactor = dimExtents[0];
		final int kFactor = dimExtents[0] * dimExtents[1];
		final int tFactor = dimExtents[0] * dimExtents[1] * dimExtents[2];

		int buffFactor = 1;

		if ( (image.getType() == ModelStorageBase.ARGB) || (image.getType() == ModelStorageBase.ARGB_USHORT)
				|| (image.getType() == ModelStorageBase.ARGB_FLOAT)) {
			buffFactor = 4;
		}

		final Vector3f center = new Vector3f();
		for (int i = 0; i < verts.length; i++) {
			center.add(verts[i]);
		}
		center.scale(1f / verts.length);

		/* Calculate the slopes for traversing the data in x,y,z: */
		float xSlopeX = verts[1].X - verts[0].X;
		float ySlopeX = verts[1].Y - verts[0].Y;
		float zSlopeX = verts[1].Z - verts[0].Z;

		float xSlopeY = verts[3].X - verts[0].X;
		float ySlopeY = verts[3].Y - verts[0].Y;
		float zSlopeY = verts[3].Z - verts[0].Z;

		float x0 = verts[0].X;
		float y0 = verts[0].Y;
		float z0 = verts[0].Z;

		xSlopeX /= (iBound);
		ySlopeX /= (iBound);
		zSlopeX /= (iBound);

		xSlopeY /= (jBound);
		ySlopeY /= (jBound);
		zSlopeY /= (jBound);

		/* loop over the 2D image (values) we're writing into */
		float x = x0;
		float y = y0;
		float z = z0;

		for (int j = 0; j < jBound; j++) {

			/* Initialize the first diagonal point(x,y,z): */
			x = x0;
			y = y0;
			z = z0;

			for (int i = 0; i < iBound; i++) {
				// Initialize to 0:
				if (buffFactor == 4) {						
					result.setC(i, j, slice, 0, 0 );
					result.setC(i, j, slice, 1, 0 );
					result.setC(i, j, slice, 2, 0 );
					result.setC(i, j, slice, 3, 0 );
				}
				else {
					result.set(i, j, slice, 0 );
				}		

				final int iIndex = Math.round(x);
				final int jIndex = Math.round(y);
				final int kIndex = Math.round(z);

				/* calculate the ModelImage space index: */
				final int index = ( (iIndex * iFactor) + (jIndex * jFactor) + (kIndex * kFactor) + (tSlice * tFactor));

				// Bounds checking:
				if ( ( (iIndex < 0) || (iIndex >= dimExtents[0])) || ( (jIndex < 0) || (jIndex >= dimExtents[1]))
						|| ( (kIndex < 0) || (kIndex >= dimExtents[2])) || ( (index < 0) || ( (index * buffFactor) > image.getSize()))) {

					// do nothing
				} else {
					/* if color: */
					if (buffFactor == 4) {						
						result.setC(i, j, slice, 0, image.getFloatC(iIndex, jIndex, kIndex, 0) );
						result.setC(i, j, slice, 1, image.getFloatC(iIndex, jIndex, kIndex, 1) );
						result.setC(i, j, slice, 2, image.getFloatC(iIndex, jIndex, kIndex, 2) );
						result.setC(i, j, slice, 3, image.getFloatC(iIndex, jIndex, kIndex, 3) );
					}
					/* not color: */
					else {
						result.set(i, j, slice, image.getFloat(iIndex, jIndex, kIndex));
					}
				}

				/*
				 * Inner loop: Move to the next diagonal point along the x-direction of the plane, using the xSlopeX,
				 * ySlopeX and zSlopeX values:
				 */
				x = x + xSlopeX;
				y = y + ySlopeX;
				z = z + zSlopeX;
			}

			/*
			 * Outer loop: Move to the next diagonal point along the y-direction of the plane, using the xSlopeY,
			 * ySlopeY and zSlopeY values:
			 */
			x0 = x0 + xSlopeY;
			y0 = y0 + ySlopeY;
			z0 = z0 + zSlopeY;
		}

		//		if ( (xSlopeX > 1) || (ySlopeX > 1) || (zSlopeX > 1) || (xSlopeY > 1) || (ySlopeY > 1) || (zSlopeY > 1)) {
		//			System.err.println("writeDiagonal " + xSlopeX + " " + ySlopeX + " " + zSlopeX);
		//			System.err.println("writeDiagonal " + xSlopeY + " " + ySlopeY + " " + zSlopeY);
		//		}
	}

	protected void writeDiagonalSampleCount(final ModelImage image, final ModelImage result, final int tSlice, final int slice,
			final int[] extents, final Vector3f[] verts) {
		final int iBound = extents[0];
		final int jBound = extents[1];
		final int[] dimExtents = image.getExtents();

		/*
		 * Get the loop multiplication factors for indexing into the 1D array with 3 index variables: based on the
		 * coordinate-systems: transformation:
		 */
		final int iFactor = 1;
		final int jFactor = dimExtents[0];
		final int kFactor = dimExtents[0] * dimExtents[1];
		final int tFactor = dimExtents[0] * dimExtents[1] * dimExtents[2];

		int buffFactor = 1;

		final Vector3f center = new Vector3f();
		for (int i = 0; i < verts.length; i++) {
			center.add(verts[i]);
		}
		center.scale(1f / verts.length);

		/* Calculate the slopes for traversing the data in x,y,z: */
		float xSlopeX = verts[1].X - verts[0].X;
		float ySlopeX = verts[1].Y - verts[0].Y;
		float zSlopeX = verts[1].Z - verts[0].Z;

		float xSlopeY = verts[3].X - verts[0].X;
		float ySlopeY = verts[3].Y - verts[0].Y;
		float zSlopeY = verts[3].Z - verts[0].Z;

		float x0 = verts[0].X;
		float y0 = verts[0].Y;
		float z0 = verts[0].Z;

		xSlopeX /= (iBound);
		ySlopeX /= (iBound);
		zSlopeX /= (iBound);

		xSlopeY /= (jBound);
		ySlopeY /= (jBound);
		zSlopeY /= (jBound);

		/* loop over the 2D image (values) we're writing into */
		float x = x0;
		float y = y0;
		float z = z0;

		for (int j = 0; j < jBound; j++) {

			/* Initialize the first diagonal point(x,y,z): */
			x = x0;
			y = y0;
			z = z0;

			for (int i = 0; i < iBound; i++) {
				final int iIndex = Math.round(x);
				final int jIndex = Math.round(y);
				final int kIndex = Math.round(z);

				/* calculate the ModelImage space index: */
				final int index = ( (iIndex * iFactor) + (jIndex * jFactor) + (kIndex * kFactor) + (tSlice * tFactor));

				// Bounds checking:
				if ( ( (iIndex < 0) || (iIndex >= dimExtents[0])) || ( (jIndex < 0) || (jIndex >= dimExtents[1]))
						|| ( (kIndex < 0) || (kIndex >= dimExtents[2])) || ( (index < 0) || ( (index * buffFactor) > image.getSize()))) {

					// do nothing
				} else {
					int count = result.getInt(iIndex, jIndex, kIndex );
					count++;
					result.set(iIndex, jIndex, kIndex, count );
//					count = result.getInt(iIndex, jIndex, kIndex );
//					System.err.println( iIndex + "  " + jIndex + "  " + kIndex + "  " + count );
				}

				/*
				 * Inner loop: Move to the next diagonal point along the x-direction of the plane, using the xSlopeX,
				 * ySlopeX and zSlopeX values:
				 */
				x = x + xSlopeX;
				y = y + ySlopeX;
				z = z + zSlopeX;
			}

			/*
			 * Outer loop: Move to the next diagonal point along the y-direction of the plane, using the xSlopeY,
			 * ySlopeY and zSlopeY values:
			 */
			x0 = x0 + xSlopeY;
			y0 = y0 + ySlopeY;
			z0 = z0 + zSlopeY;
		}

		//		if ( (xSlopeX > 1) || (ySlopeX > 1) || (zSlopeX > 1) || (xSlopeY > 1) || (ySlopeY > 1) || (zSlopeY > 1)) {
		//			System.err.println("writeDiagonal " + xSlopeX + " " + ySlopeX + " " + zSlopeX);
		//			System.err.println("writeDiagonal " + xSlopeY + " " + ySlopeY + " " + zSlopeY);
		//		}
	}

	protected void writeDiagonalTest(final ModelImage image, final ModelImage result, final int tSlice, final int slice,
			final int[] extents, final Vector3f[] verts, float radiusSq ) {
		final int iBound = extents[0];
		final int jBound = extents[1];
		final int[] dimExtents = image.getExtents();

		/*
		 * Get the loop multiplication factors for indexing into the 1D array with 3 index variables: based on the
		 * coordinate-systems: transformation:
		 */
		final int iFactor = 1;
		final int jFactor = dimExtents[0];
		final int kFactor = dimExtents[0] * dimExtents[1];
		final int tFactor = dimExtents[0] * dimExtents[1] * dimExtents[2];

		int buffFactor = 1;

		if ( (image.getType() == ModelStorageBase.ARGB) || (image.getType() == ModelStorageBase.ARGB_USHORT)
				|| (image.getType() == ModelStorageBase.ARGB_FLOAT)) {
			buffFactor = 4;
		}

		final Vector3f center = new Vector3f();
		for (int i = 0; i < verts.length; i++) {
			center.add(verts[i]);
		}
		center.scale(1f / verts.length);

		/* Calculate the slopes for traversing the data in x,y,z: */
		float xSlopeX = verts[1].X - verts[0].X;
		float ySlopeX = verts[1].Y - verts[0].Y;
		float zSlopeX = verts[1].Z - verts[0].Z;

		float xSlopeY = verts[3].X - verts[0].X;
		float ySlopeY = verts[3].Y - verts[0].Y;
		float zSlopeY = verts[3].Z - verts[0].Z;

		float x0 = verts[0].X;
		float y0 = verts[0].Y;
		float z0 = verts[0].Z;

		xSlopeX /= (iBound);
		ySlopeX /= (iBound);
		zSlopeX /= (iBound);

		xSlopeY /= (jBound);
		ySlopeY /= (jBound);
		zSlopeY /= (jBound);

		float centerI = iBound / 2f;
		float centerJ = jBound / 2f;

		/* loop over the 2D image (values) we're writing into */
		float x = x0;
		float y = y0;
		float z = z0;

		float fMin = Float.MAX_VALUE;
		float fMax = -Float.MAX_VALUE;
		float fMinG = Float.MAX_VALUE;
		float fMaxG = -Float.MAX_VALUE;
		float fMinB = Float.MAX_VALUE;
		float fMaxB = -Float.MAX_VALUE;
		for (int j = 0; j < jBound; j++) {

			/* Initialize the first diagonal point(x,y,z): */
			x = x0;
			y = y0;
			z = z0;

			for (int i = 0; i < iBound; i++) {
				// Initialize to 0:
				if (buffFactor == 4) {						
					result.setC(i, j, slice, 0, 0 );
					result.setC(i, j, slice, 1, 0 );
					result.setC(i, j, slice, 2, 0 );
					result.setC(i, j, slice, 3, 0 );
				}
				else {
					result.set(i, j, slice, 0 );
				}		

				final int iIndex = Math.round(x);
				final int jIndex = Math.round(y);
				final int kIndex = Math.round(z);

				/* calculate the ModelImage space index: */
				final int index = ( (iIndex * iFactor) + (jIndex * jFactor) + (kIndex * kFactor) + (tSlice * tFactor));

				// Bounds checking:
				if ( ( (iIndex < 0) || (iIndex >= dimExtents[0])) || ( (jIndex < 0) || (jIndex >= dimExtents[1]))
						|| ( (kIndex < 0) || (kIndex >= dimExtents[2])) || ( (index < 0) || ( (index * buffFactor) > image.getSize()))) {

					// do nothing
				} 
				else {
					float dist = (i - centerI) * (i - centerI) + (j - centerJ) * (j - centerJ); 
					if ( dist <= radiusSq )
					{
						/* if color: */
						if (buffFactor == 4) {						
							float value = image.getFloatC(iIndex, jIndex, kIndex, 1);
							float valueG = image.getFloatC(iIndex, jIndex, kIndex, 2);
							float valueB = image.getFloatC(iIndex, jIndex, kIndex, 3);
							result.setC(i, j, slice, 0, image.getFloatC(iIndex, jIndex, kIndex, 0) );
							result.setC(i, j, slice, 1, value );
							result.setC(i, j, slice, 2, valueG );
							result.setC(i, j, slice, 3, valueB );
							if ( value < fMin ) fMin = value;
							if ( value > fMax ) fMax = value;
							if ( valueG < fMinG ) fMinG = valueG;
							if ( valueG > fMaxG ) fMaxG = valueG;
							if ( valueB < fMinB ) fMinB = valueB;
							if ( valueB > fMaxB ) fMaxB = valueB;
						}
						/* not color: */
						else {
							float value = image.getFloat(iIndex, jIndex, kIndex);
							result.set(i, j, slice, value);
							if ( value < fMin ) fMin = value;
							if ( value > fMax ) fMax = value;
						}					
					}
				}

				/*
				 * Inner loop: Move to the next diagonal point along the x-direction of the plane, using the xSlopeX,
				 * ySlopeX and zSlopeX values:
				 */
				x = x + xSlopeX;
				y = y + ySlopeX;
				z = z + zSlopeX;
			}

			/*
			 * Outer loop: Move to the next diagonal point along the y-direction of the plane, using the xSlopeY,
			 * ySlopeY and zSlopeY values:
			 */
			x0 = x0 + xSlopeY;
			y0 = y0 + ySlopeY;
			z0 = z0 + zSlopeY;
		}

		//		if ( (xSlopeX > 1) || (ySlopeX > 1) || (zSlopeX > 1) || (xSlopeY > 1) || (ySlopeY > 1) || (zSlopeY > 1)) {
		//			System.err.println("writeDiagonal " + xSlopeX + " " + ySlopeX + " " + zSlopeX);
		//			System.err.println("writeDiagonal " + xSlopeY + " " + ySlopeY + " " + zSlopeY);
		//		}

		result.setMax( Math.max(fMax, result.getMax()) );
		result.setMin( Math.min(fMin, result.getMin()) );
		if (buffFactor == 4) {	
			result.setMaxR(Math.max(fMax, result.getMaxR()));
			result.setMinR(Math.min(fMin, result.getMinR()));
			result.setMaxG(Math.max(fMaxG, result.getMaxG()));
			result.setMinG(Math.min(fMinG, result.getMinG()));
			result.setMaxB(Math.max(fMaxB, result.getMaxB()));
			result.setMinB(Math.min(fMinB, result.getMinB()));
		}
	}

	/**
	 * Adds a point to the lattice.
	 * 
	 * @param startPt
	 * @param endPt
	 * @param maxPt
	 */
	private boolean addInsertionPoint(final Vector3f startPt, final Vector3f endPt, final Vector3f maxPt, boolean isSeam) {
		final Segment3f mouseVector = new Segment3f(startPt, endPt);
		float minDistL = Float.MAX_VALUE;
		int minIndexL = -1;
		int minSeamL = -1;
		Vector3f newLeft = null;
		int seamCount = 0;
		int otherCount = 0;
		for (int i = 0; i < left.getCurves().size() - 1; i++) {
			if ( ((VOIWormAnnotation)left.getCurves().elementAt(i)).isSeamCell() ) {
				seamCount++;
			}
			else {
				otherCount++;
			}
			
			VOIWormAnnotation leftPt = (VOIWormAnnotation) left.getCurves().elementAt(i);
			VOIWormAnnotation leftPtP1 = (VOIWormAnnotation) left.getCurves().elementAt(i+1);
			final Segment3f leftS = new Segment3f( leftPt.elementAt(0), leftPtP1.elementAt(0));
			final DistanceSegment3Segment3 dist = new DistanceSegment3Segment3(mouseVector, leftS);
			final float distance = dist.Get();
			if (distance < minDistL) {
				minDistL = distance;
				if (minDistL <= 12) {
					// System.err.println( dist.GetSegment0Parameter() + " " + dist.GetSegment1Parameter() );
					minIndexL = i;
					minSeamL = seamCount;
					//					newLeft = Vector3f.add(leftS.Center, Vector3f.scale(dist.GetSegment1Parameter(), leftS.Direction));
					newLeft = new Vector3f(maxPt);
				}
			}
		}
		float minDistR = Float.MAX_VALUE;
		int minIndexR = -1;
		int minSeamR = -1;
		Vector3f newRight = null;
		for (int i = 0; i < right.getCurves().size() - 1; i++) {
			VOIWormAnnotation rightPt = (VOIWormAnnotation) right.getCurves().elementAt(i);
			VOIWormAnnotation rightPtP1 = (VOIWormAnnotation) right.getCurves().elementAt(i+1);
			final Segment3f rightS = new Segment3f(rightPt.elementAt(0), rightPtP1.elementAt(0));
			final DistanceSegment3Segment3 dist = new DistanceSegment3Segment3(mouseVector, rightS);
			final float distance = dist.Get();
			if (distance < minDistR) {
				minDistR = distance;
				if (minDistR <= 12) {
					// System.err.println( dist.GetSegment0Parameter() + " " + dist.GetSegment1Parameter() );
					minIndexR = i;
					minSeamR = seamCount;
					//					newRight = Vector3f.add(rightS.Center, Vector3f.scale(dist.GetSegment1Parameter(), rightS.Direction));
					newRight = new Vector3f(maxPt);
				}
			}
		}
		if ( (minIndexL != -1) && (minIndexR != -1)) {
			if (minDistL < minDistR) {
				seamCount = minSeamL + 1;
				// System.err.println( "Add to left " + (minIndexL+1) );
				VOIWormAnnotation newLeftAnnotation = new VOIWormAnnotation(newLeft);
				newLeftAnnotation.setSeamCell(isSeam);
				if ( isSeam ) {
					String name = seamCount < 3 ? ("H" + seamCount) : (seamCount < 9) ? ("V" + (seamCount - 2)) : "T";
					newLeftAnnotation.setText(name + "L");
				}
				else {
					String name = "a" + otherCount;
					newLeftAnnotation.setText(name + "L");
				}
				left.getCurves().add(minIndexL + 1, newLeftAnnotation);
				pickedPoint = left.getCurves().elementAt(minIndexL + 1).elementAt(0);
				
				newRight = Vector3f.add(right.getCurves().elementAt(minIndexL).elementAt(0), right.getCurves().elementAt(minIndexL + 1).elementAt(0));
				newRight.scale(0.5f);
				VOIWormAnnotation newRightAnnotation = new VOIWormAnnotation(newRight);
				newRightAnnotation.setSeamCell(isSeam);
				if ( isSeam ) {
					String name = seamCount < 3 ? ("H" + seamCount) : (seamCount < 9) ? ("V" + (seamCount - 2)) : "T";
					newRightAnnotation.setText(name + "RL");
				}
				else {
					String name = "a" + otherCount;
					newRightAnnotation.setText(name + "R");
				}
				right.getCurves().add(minIndexL + 1, newRightAnnotation);
				updateSeamCount();
				updateLattice(true);
				showLatticeLabels(displayLatticeLabels);
			} else {
				seamCount = minSeamR + 1;
				// System.err.println( "Add to right " + (minIndexR+1) );
				VOIWormAnnotation newRightAnnotation = new VOIWormAnnotation(newRight);
				newRightAnnotation.setSeamCell(isSeam);
				if ( isSeam ) {
					String name = seamCount < 3 ? ("H" + seamCount) : (seamCount < 9) ? ("V" + (seamCount - 2)) : "T";
					newRightAnnotation.setText(name + "RL");
				}
				else {
					String name = "a" + otherCount;
					newRightAnnotation.setText(name + "R");
				}
				right.getCurves().add(minIndexR + 1, newRightAnnotation);
				pickedPoint = right.getCurves().elementAt(minIndexR + 1).elementAt(0);
				
				
				newLeft = Vector3f.add(left.getCurves().elementAt(minIndexR).elementAt(0), left.getCurves().elementAt(minIndexR + 1).elementAt(0));
				newLeft.scale(0.5f);
				VOIWormAnnotation newLeftAnnotation = new VOIWormAnnotation(newLeft);
				newLeftAnnotation.setSeamCell(isSeam);
				if ( isSeam ) {
					String name = seamCount < 3 ? ("H" + seamCount) : (seamCount < 9) ? ("V" + (seamCount - 2)) : "T";
					newLeftAnnotation.setText(name + "L");
				}
				else {
					String name = "a" + otherCount;
					newLeftAnnotation.setText(name + "L");
				}
				left.getCurves().add(minIndexR + 1, newLeftAnnotation);
				updateSeamCount();
				updateLattice(true);
				showLatticeLabels(displayLatticeLabels);
			}
		} else if ((minIndexL != -1) && ((minIndexL + 1) < right.getCurves().size())) {
			seamCount = minSeamL + 1;
			// System.err.println( "Add to left " + (minIndexL+1) );
			VOIWormAnnotation newLeftAnnotation = new VOIWormAnnotation(newLeft);
			newLeftAnnotation.setSeamCell(isSeam);
			if ( isSeam ) {
				String name = seamCount < 3 ? ("H" + seamCount) : (seamCount < 9) ? ("V" + (seamCount - 2)) : "T";
				newLeftAnnotation.setText(name + "L");
			}
			else {
				String name = "a" + otherCount;
				newLeftAnnotation.setText(name + "L");
			}
			left.getCurves().add(minIndexL + 1, newLeftAnnotation);
			pickedPoint = left.getCurves().elementAt(minIndexL + 1).elementAt(0);
			newRight = Vector3f.add(right.getCurves().elementAt(minIndexL).elementAt(0), right.getCurves().elementAt(minIndexL + 1).elementAt(0));
			newRight.scale(0.5f);
			VOIWormAnnotation newRightAnnotation = new VOIWormAnnotation(newRight);
			newRightAnnotation.setSeamCell(isSeam);
			if ( isSeam ) {
				String name = seamCount < 3 ? ("H" + seamCount) : (seamCount < 9) ? ("V" + (seamCount - 2)) : "T";
				newRightAnnotation.setText(name + "RL");
			}
			else {
				String name = "a" + otherCount;
				newRightAnnotation.setText(name + "R");
			}
			right.getCurves().add(minIndexL + 1, newRightAnnotation);
			updateSeamCount();
			updateLattice(true);
			showLatticeLabels(displayLatticeLabels);
		} else if (minIndexR != -1 && ((minIndexR + 1) < left.getCurves().size())) {
			seamCount = minSeamR + 1;
			// System.err.println( "Add to right " + (minIndexR+1) );
			VOIWormAnnotation newRightAnnotation = new VOIWormAnnotation(newRight);
			newRightAnnotation.setSeamCell(isSeam);
			if ( isSeam ) {
				String name = seamCount < 3 ? ("H" + seamCount) : (seamCount < 9) ? ("V" + (seamCount - 2)) : "T";
				newRightAnnotation.setText(name + "RL");
			}
			else {
				String name = "a" + otherCount;
				newRightAnnotation.setText(name + "R");
			}
			right.getCurves().add(minIndexR + 1, newRightAnnotation);
			pickedPoint = right.getCurves().elementAt(minIndexR + 1).elementAt(0);
			
			
			newLeft = Vector3f.add(left.getCurves().elementAt(minIndexR).elementAt(0), left.getCurves().elementAt(minIndexR + 1).elementAt(0));
			newLeft.scale(0.5f);
			VOIWormAnnotation newLeftAnnotation = new VOIWormAnnotation(newLeft);
			newLeftAnnotation.setSeamCell(isSeam);
			if ( isSeam ) {
				String name = seamCount < 3 ? ("H" + seamCount) : (seamCount < 9) ? ("V" + (seamCount - 2)) : "T";
				newLeftAnnotation.setText(name + "L");
			}
			else {
				String name = "a" + otherCount;
				newLeftAnnotation.setText(name + "L");
			}
			left.getCurves().add(minIndexR + 1, newLeftAnnotation);

			// rename cells if seam count > 10 to include Q cells:
			updateSeamCount();
			updateLattice(true);
			showLatticeLabels(displayLatticeLabels);
		}
		else
		{
			pickedPoint = null;
			return false;
		}
		return true;
	}
	/**
	 * Resets the natural spline curves when the lattice changes.
	 */
	private void clearCurves( boolean clearGrid )
	{
		if (center != null) {
			center.dispose();
			center = null;
		}
		afTimeC = null;
		centerSpline = null;

		centerPositions = null;
		leftPositions = null;
		rightPositions = null;

		if (wormDiameters != null) {
			wormDiameters.removeAllElements();
			wormDiameters = null;
		}
		if (rightVectors != null) {
			rightVectors.removeAllElements();
			rightVectors = null;
		}
		if (upVectors != null) {
			upVectors.removeAllElements();
			upVectors = null;
		}

		allTimes = null;

		if (centerLine != null) {
			imageA.unregisterVOI(centerLine);
			centerLine.dispose();
			centerLine = null;
		}
		if (rightLine != null) {
			imageA.unregisterVOI(rightLine);
			rightLine.dispose();
			rightLine = null;
		}
		if (leftLine != null) {
			imageA.unregisterVOI(leftLine);
			leftLine.dispose();
			leftLine = null;
		}

		if (displayContours != null) {
			for ( int i = 0; i < displayContours.length; i++ ) {
				if ( displayContours[i] != null ) {
					imageA.unregisterVOI(displayContours[i]);
					displayContours[i].dispose();
				}
			}
			displayContours = null;
		}
		if ( clearGrid )
		{
			if (latticeGrid != null) {
				for (int i = latticeGrid.size() - 1; i >= 0; i--) {
					final VOI marker = latticeGrid.remove(i);
					imageA.unregisterVOI(marker);
				}
			} 
		}
	}
	/**
	 * Counts the annotations and colors them
	 * based on the number of annotations. This is only used
	 * when the user is labeling seam-cells as an aid in determining if 20
	 * pairs of seam cells have been found.
	 */
	private void colorAnnotations()
	{
		// count markers (not nose or origin)
		// even = all blue
		// odd = yellow
		// some colors are now used to designate the first and last pairs of seam cells - first = green; last = red
		if ( annotationVOIs == null )
		{
			return;
		}

		int count = 0;
		for (int i = 0; i < annotationVOIs.getCurves().size(); i++)
		{
			VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
			if ( !(text.getText().contains("nose") || text.getText().contains("Nose")) && !text.getText().equalsIgnoreCase("origin"))
			{
				count++;
			}
		}
		Color c = Color.yellow;
		if ( (count % 2) == 0 )
		{
			c = Color.blue;
		}
		if ( !colorAnnotations )
		{
			c = Color.white;
		}


		for (int i = 0; i < annotationVOIs.getCurves().size(); i++)
		{
			VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
			if ( !(text.getText().contains("nose") || text.getText().contains("Nose")) && !text.getText().equalsIgnoreCase("origin"))
			{
				//				System.err.println( text.getText() + "   " + text.getColor() );
				if ( !match(text.getColor(), Color.red) && !match(text.getColor(), Color.green)  )
				{
					text.setColor(c);
					text.updateText();
				}
			}
		}
		updateAnnotationListeners();
	}

	private void generateEllipses( int extent )
	{
		boxBounds = new Vector<Box3f>();
		ellipseBounds = new Vector<Ellipsoid3f>();
		final short sID = (short)10;
		samplingPlanes = new VOI(sID, "samplingPlanes");
		System.err.println("generate planes " + extent);
		for (int i = 0; i < centerPositions.size(); i++) {
			final Vector3f rkEye = centerPositions.elementAt(i);
			final Vector3f rkRVector = rightVectors.elementAt(i);
			final Vector3f rkUVector = upVectors.elementAt(i);

			final Vector3f[] output = new Vector3f[4];
			final Vector3f rightV = Vector3f.scale(extent, rkRVector);
			final Vector3f upV = Vector3f.scale(extent, rkUVector);
//			System.err.println(i + "  " + rightV);
			output[0] = Vector3f.add(Vector3f.neg(rightV), Vector3f.neg(upV));
			output[1] = Vector3f.add(rightV, Vector3f.neg(upV));
			output[2] = Vector3f.add(rightV, upV);
			output[3] = Vector3f.add(Vector3f.neg(rightV), upV);
			for (int j = 0; j < 4; j++) {
				output[j].add(rkEye);
			}
			final VOIContour kBox = new VOIContour(true);
			for (int j = 0; j < 4; j++) {
				kBox.addElement(output[j].X, output[j].Y, output[j].Z);
			}
			kBox.update(new ColorRGBA(0, 0, 1, 1));
			{
				samplingPlanes.getCurves().add(kBox);
				//				samplingPlanes.importCurve(kBox);
			}

			//			final float curve = centerSpline.GetCurvature(allTimes[i]);
			//			final float scale = curve;
			final VOIContour ellipse = new VOIContour(true);
			final Ellipsoid3f ellipsoid = makeEllipse(rkRVector, rkUVector, rkEye, wormDiameters.elementAt(i), wormDiameters.elementAt(i)/2f, ellipse);
			ellipseBounds.add(ellipsoid);

			final Box3f box = new Box3f(ellipsoid.Center, ellipsoid.Axis, new float[] {extent, extent, 1});
			boxBounds.add(box);
		}

		saveSamplePlanes( samplingPlanes, sharedOutputDir + File.separator );
		saveDiameters( wormDiameters, sharedOutputDir + File.separator );
	}


	/**
	 * Generates the VOI that highlights which point (lattice or annotation) is currently selected by the user.
	 * 
	 * @param right
	 * @param up
	 * @param center
	 * @param diameter
	 * @param ellipse
	 */
	private void makeSelectionFrame(final Vector3f right, final Vector3f up, final Vector3f center, final float diameter, final VOIContour ellipse) {
		final int numPts = 12;
		for (int i = 0; i < numPts; i++) {
			final double c = Math.cos(Math.PI * 2.0 * i / numPts);
			final double s = Math.sin(Math.PI * 2.0 * i / numPts);
			final Vector3f pos1 = Vector3f.scale((float) (diameter * c), right);
			final Vector3f pos2 = Vector3f.scale((float) (diameter * s), up);
			final Vector3f pos = Vector3f.add(pos1, pos2);
			pos.add(center);
			ellipse.addElement(pos);
		}
	}


	private Vector3f retwistAnnotation(VOIWormAnnotation annotation)
	{
		// this is the untwisting code:
		final Vector3f[] corners = new Vector3f[4];
		int slice = (int) annotation.firstElement().Z;
		VOIContour kBox = (VOIContour) samplingPlanes.getCurves().elementAt(slice);
		for (int j = 0; j < 4; j++) {
			corners[j] = kBox.elementAt(j);
		}

		return inverseDiagonal(imageA, slice, 2*extent, corners, annotation.firstElement() );
	}


	private void saveImage(final String imageName, final ModelImage image, final boolean saveAsTif) {
		String voiDir = outputDirectory + File.separator;
		File voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			//			System.err.println( "saveImage " + voiDir);
			voiFileDir.mkdir();
		}
		voiDir = outputDirectory + File.separator + "output_images" + File.separator;
		
		voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) {} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) {} else { // voiFileDir
			voiFileDir.mkdir();
		}

		final File file = new File(voiDir + imageName);
		if (file.exists()) {
			file.delete();
		}
		ModelImage.saveImage(image, getImageName(image) + ".xml", voiDir, false);
		if (saveAsTif) {
			ModelImage.saveImage(image, getImageName(image) + ".tif", voiDir, false);
		}
	}
	



	public static void saveImage(final ModelImage originalImage, final ModelImage image, String subDir, String postScript ) {
		
		String outputDirectory = new String(originalImage.getImageDirectory() + JDialogBase.makeImageName(originalImage.getImageFileName(), "") + File.separator + JDialogBase.makeImageName(originalImage.getImageFileName(), "_results") );
		String parentDir = new String(originalImage.getImageDirectory() + JDialogBase.makeImageName(originalImage.getImageFileName(), "") + File.separator);
		checkParentDir(parentDir);	
		
		String voiDir = outputDirectory + File.separator;
		File voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			//			System.err.println( "saveImage " + voiDir);
			voiFileDir.mkdir();
		}
		voiDir = outputDirectory + File.separator + subDir + File.separator;
		
		String imageName = JDialogBase.makeImageName(image.getImageFileName(), "");
		imageName = imageName + postScript;
		
//		int maxVal = -1;
//		voiFileDir = new File(voiDir);
//		if (voiFileDir.exists() && voiFileDir.isDirectory()) { 
//			final String[] list = voiFileDir.list();
//			for (int i = 0; i < list.length; i++) {
//				int fileVal = -1;
////				System.err.println(list[i] + "   " + imageName + "   " + list[i].contains(imageName) );
//				if ( list[i].contains(imageName) ) {
//					String sub = list[i].substring( list[i].indexOf(imageName) + imageName.length(), list[i].length() );
//					for ( int j = 0; j < sub.length(); j++ ) {
//						String test = new String( "" + sub.charAt(j) );
//						int val;
//						try {
//							val = Integer.valueOf(test);
//							if ( fileVal == -1 ) {
//								fileVal = val;
//							}
//							else
//							{
//								fileVal = fileVal * 10 + val;
//							}
//						} catch(NumberFormatException e) {
//						}
//					}
////					System.err.println( "    " + fileVal );
//				}
//				if ( fileVal > maxVal )
//				{
//					maxVal = fileVal;
//				}
//			}
//		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
//		} else { // voiFileDir does not exist
//			//			System.err.println( "saveImage " + voiDir);
//			voiFileDir.mkdir();
//		}
//
//		maxVal++;
//		imageName = imageName + "_" + maxVal;
		

		imageName = imageName + ".tif";
		File imageFile = new File(voiDir + imageName);
		if (imageFile.exists() && !imageFile.isDirectory()) { 
			imageFile.delete();
		}

    	System.err.println(voiDir + imageName );
		ModelImage.saveImage(image, imageName, voiDir, false);

	}

	private void saveLatticeStatistics()
	{
		// Determine the distances between points on the lattice
		// distances are along the curve, not straight-line distances:			
		final float[] leftDistances = new float[left.getCurves().size()];
		final float[] rightDistances = new float[right.getCurves().size()];
		for ( int i = 0; i < left.getCurves().size(); i++ )
		{
			leftDistances[i] = 0;
			rightDistances[i] = 0;
		}
		for ( int i = 0; i < left.getCurves().size() - 1; i++ )
		{
			leftDistances[i] = 0;
			rightDistances[i] = 0;
			for ( int j = latticeSlices[i]; j < latticeSlices[i+1]; j++ )
			{
				leftDistances[i] += leftPositions.elementAt(j).distance(leftPositions.elementAt(j+1));
				rightDistances[i] += rightPositions.elementAt(j).distance(rightPositions.elementAt(j+1));
			}
		}

		saveLatticeStatistics(sharedOutputDir + File.separator, centerSpline.GetLength(0, 1), left, right, leftDistances, rightDistances, "_before");
	}

	public static void saveContourAsCSV( ModelImage image, String subDir, String postScript, VOIContour contour )
	{
		
		String outputDirectory = new String(image.getImageDirectory() + JDialogBase.makeImageName(image.getImageFileName(), "") + File.separator + JDialogBase.makeImageName(image.getImageFileName(), "_results") );
		String parentDir = new String(image.getImageDirectory() + JDialogBase.makeImageName(image.getImageFileName(), "") + File.separator);
		checkParentDir(parentDir);	
		
		String voiDir = outputDirectory + File.separator;
		File voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			//			System.err.println( "saveImage " + voiDir);
			voiFileDir.mkdir();
		}
		voiDir = outputDirectory + File.separator + subDir + File.separator;		
		
		String imageName = JDialogBase.makeImageName(image.getImageFileName(), "");
		imageName = imageName + postScript + ".csv";
		
		saveContourAsCSV(voiDir, imageName, contour);
	}
	


	public static void saveContourAsCSV( final String voiDir, final String fileName, VOIContour contour ) {
		
		// check files, create new directories and delete any existing files:
		final File fileDir = new File(voiDir);

		if (fileDir.exists() && fileDir.isDirectory()) {} 
		else if (fileDir.exists() && !fileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			fileDir.mkdir();
		}
		File file = new File(fileDir + File.separator + fileName);
		if (file.exists()) {
			file.delete();
			file = new File(fileDir + File.separator + fileName);
		}


		if ( contour == null )
			return;
		if ( contour.size() == 0 )
			return;

		try {

			final FileWriter fw = new FileWriter(file);
			final BufferedWriter bw = new BufferedWriter(fw);
			bw.write("Contour" + "\n");
			bw.write("x_voxels" + "," + "y_voxels" + "," + "z_voxels" + "\n");
			for (int i = 0; i < contour.size(); i++) {
				Vector3f position = contour.elementAt(i);
				bw.write(position.X + "," + position.Y + "," + position.Z + "\n");

			}
			bw.newLine();
			bw.close();
		} catch (final Exception e) {
			System.err.println("CAUGHT EXCEPTION WITHIN saveContourAsCSV");
			e.printStackTrace();
		}
	}
	
	public static VOIContour loadContourFromCSV( final String voiDir, final String fileName) {
		
		final File dir = new File(voiDir);
		File file = new File(dir + File.separator + fileName);

		if ( file.exists() )
		{		

			FileReader fr;
			try {
				fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);
				String line = br.readLine();
            	line = br.readLine();
				String[] parsed = line.split( "," );

    			VOIContour ellipse = new VOIContour(true);

                while ( true ) {

                	line = br.readLine();
                	
                	if (line.isEmpty())
                		break;
                	
                	parsed = line.split( "," );
                	int parsedIndex = 0;
					float x    = (parsed.length > parsedIndex+0) ? (parsed[parsedIndex+0].length() > 0) ? Float.valueOf( parsed[parsedIndex+0] ) : 0 : 0; 
					float y    = (parsed.length > parsedIndex+1) ? (parsed[parsedIndex+1].length() > 0) ? Float.valueOf( parsed[parsedIndex+1] ) : 0 : 0; 
					float z    = (parsed.length > parsedIndex+2) ? (parsed[parsedIndex+2].length() > 0) ? Float.valueOf( parsed[parsedIndex+2] ) : 0 : 0;
					
                	ellipse.add(new Vector3f(x,y,z));
                }
                br.close();
                fr.close();
                
                return ellipse;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	

	public static void saveBasisVectorsAsCSV( ModelImage image, String subDir, String postScript, VOIContour positions,
			Vector<Vector3f> normals, Vector<Vector3f> rightVectors, Vector<Vector3f> upVectors )
	{
		String outputDirectory = new String(image.getImageDirectory() + JDialogBase.makeImageName(image.getImageFileName(), "") + File.separator + JDialogBase.makeImageName(image.getImageFileName(), "_results") );
		String parentDir = new String(image.getImageDirectory() + JDialogBase.makeImageName(image.getImageFileName(), "") + File.separator);
		checkParentDir(parentDir);	
		
		String voiDir = outputDirectory + File.separator;
		File voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			//			System.err.println( "saveImage " + voiDir);
			voiFileDir.mkdir();
		}
		voiDir = outputDirectory + File.separator + subDir + File.separator;		
		
		String imageName = JDialogBase.makeImageName(image.getImageFileName(), "");
		imageName = imageName + postScript + ".csv";
		
		// check files, create new directories and delete any existing files:
		final File fileDir = new File(voiDir);

		if (fileDir.exists() && fileDir.isDirectory()) {} 
		else if (fileDir.exists() && !fileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			fileDir.mkdir();
		}
		File file = new File(fileDir + File.separator + imageName);
		if (file.exists()) {
			file.delete();
			file = new File(fileDir + File.separator + imageName);
		}


		if ( positions == null || normals == null || rightVectors == null || upVectors == null )
			return;
		if ( positions.size() == 0 || normals.size() == 0 || rightVectors.size() == 0 || upVectors.size() == 0 )
			return;

		try {

			final FileWriter fw = new FileWriter(file);
			final BufferedWriter bw = new BufferedWriter(fw);
			bw.write("x" + "," + "y" + "," + "z" + "," + "xN" + "," + "yN" + "," + "zN" + "," + "xR" + "," + "yR" + "," + "zR" + "," + "xU" + "," + "yU" + "," + "zU" + "\n");
			for (int i = 0; i < positions.size(); i++) {
				Vector3f position = positions.elementAt(i);
				Vector3f normal = normals.elementAt(i);      normal.normalize();
				Vector3f right  = rightVectors.elementAt(i); right.normalize();
				Vector3f up     = upVectors.elementAt(i);    up.normalize();
				bw.write(position.X + "," + position.Y + "," + position.Z + "," + normal.X + "," + normal.Y + "," + normal.Z + "," + right.X + "," + right.Y + "," + right.Z + "," + up.X + "," + up.Y + "," + up.Z + "\n");

			}
			bw.newLine();
			bw.close();
		} catch (final Exception e) {
			System.err.println("CAUGHT EXCEPTION WITHIN saveSeamCellsTo");
			e.printStackTrace();
		}
	}

    private void saveContours( ModelImage image, Box3f[] contours ) {
		
    	String voiDir = sharedOutputDir + File.separator + "model" + File.separator;	
    	File voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			voiFileDir.mkdir();
		}			
		String imageName = JDialogBase.makeImageName(image.getImageFileName(), "");
		imageName = imageName + "_contours";

		String contourCSV = imageName + ".csv";
		File contourFile = new File(voiDir + contourCSV);
		if (contourFile.exists() && !contourFile.isDirectory()) { 
			contourFile.delete();
		}		

		try {

			final FileWriter fw = new FileWriter(contourFile);
			final BufferedWriter bw = new BufferedWriter(fw);
			bw.write(contours.length + "\n");
			for (int i = 0; i < contours.length; i++) {
				Box3f box = contours[i];
				bw.write( box.Center.X + "," + box.Center.Y + "," + box.Center.Z + "," + 
						box.Axis[0].X + "," + box.Axis[0].Y + "," + box.Axis[0].Z + "," + 
						box.Axis[1].X + "," + box.Axis[1].Y + "," + box.Axis[1].Z + "," + 
						box.Axis[2].X + "," + box.Axis[2].Y + "," + box.Axis[2].Z + "," + 
						box.Extent[0] + "," + box.Extent[1] + "," + box.Extent[2] +	"\n");
			}
			bw.newLine();
			bw.close();
		} catch (final Exception e) {
			System.err.println("CAUGHT EXCEPTION WITHIN saveSeamCellsTo");
			e.printStackTrace();
		}
    }
    
    public static Box3f[] readContours( ModelImage image, String subDir, String postScript  ) {
    	Box3f[] contours = null;
    	
		String outputDirectory = new String(image.getImageDirectory() + JDialogBase.makeImageName(image.getImageFileName(), "") + File.separator + JDialogBase.makeImageName(image.getImageFileName(), "_results") );
		String parentDir = new String(image.getImageDirectory() + JDialogBase.makeImageName(image.getImageFileName(), "") + File.separator);
		checkParentDir(parentDir);	
		
		String voiDir = outputDirectory + File.separator;
		File voiFileDir = new File(voiDir);
		if ( !voiFileDir.exists() ) {
			return null;
		}
		voiDir = outputDirectory + File.separator + subDir + File.separator;	
		voiFileDir = new File(voiDir);
		if ( !voiFileDir.exists() ) {
			return null;
		}
		
		String imageName = JDialogBase.makeImageName(image.getImageFileName(), "");
		imageName = imageName + postScript;

		String contourCSV = imageName + ".csv";
		File contourFile = new File(voiDir + contourCSV);
		if ( !contourFile.exists() ) {
			return null;
		}

		FileReader fr;
		try {
			fr = new FileReader(contourFile);
			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();
			int count = Integer.valueOf(line);
			line = br.readLine();
			contours = new Box3f[count];
			count = 0;
			while ( line != null && (line.length() > 1) && count < contours.length )
			{
				String[] parsed = line.split( "," );
				if ( parsed.length == 15 )
				{
					Vector3f Center = new Vector3f();
					Center.X = Float.valueOf( parsed[0] ); 
					Center.Y = Float.valueOf( parsed[1] ); 
					Center.Z = Float.valueOf( parsed[2] ); 
					
					Vector3f[] Axis = new Vector3f[3];
					Axis[0] = new Vector3f();
					Axis[0].X = Float.valueOf( parsed[3] ); 
					Axis[0].Y = Float.valueOf( parsed[4] ); 
					Axis[0].Z = Float.valueOf( parsed[5] ); 

					Axis[1] = new Vector3f();
					Axis[1].X = Float.valueOf( parsed[6] ); 
					Axis[1].Y = Float.valueOf( parsed[7] ); 
					Axis[1].Z = Float.valueOf( parsed[8] ); 

					Axis[2] = new Vector3f();
					Axis[2].X = Float.valueOf( parsed[9] ); 
					Axis[2].Y = Float.valueOf( parsed[10] ); 
					Axis[2].Z = Float.valueOf( parsed[11] ); 
					
					float[] Extents = new float[3];
					Extents[0] = Float.valueOf( parsed[12] ); 
					Extents[1] = Float.valueOf( parsed[13] ); 
					Extents[2] = Float.valueOf( parsed[14] ); 

					contours[count++] = new Box3f(Center, Axis, Extents);
				}

				line = br.readLine();
			}
			fr.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return contours;
    }
    
    public static void saveTriMesh( ModelImage image, String outputDirectory, String subDir, String postScript, TriMesh mesh ) {
		
		String voiDir = outputDirectory + File.separator;
		File voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			//			System.err.println( "saveImage " + voiDir);
			voiFileDir.mkdir();
		}
		voiDir = outputDirectory + File.separator + subDir + File.separator;	
		voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			//			System.err.println( "saveImage " + voiDir);
			voiFileDir.mkdir();
		}	
		
		String imageName = JDialogBase.makeImageName(image.getImageFileName(), "");
		imageName = imageName + postScript;

		String modelXML = imageName + ".xml";
		File meshFile = new File(voiDir + modelXML);
		if (meshFile.exists() && !meshFile.isDirectory()) { 
			meshFile.delete();
		}
        try {
        	System.err.println(voiDir + modelXML );
            FileSurface_WM.save( voiDir + modelXML, mesh, image, false);
        } catch (IOException error) {
        	MipavUtil.displayError("Error while trying to save single mesh " + error);
        }
        
		String modelPLY = imageName + ".ply";
		meshFile = new File(voiDir + modelPLY);
		if (meshFile.exists() && !meshFile.isDirectory()) { 
			meshFile.delete();
		}
        try {
        	System.err.println(voiDir + modelPLY );
            FileSurface_WM.save( voiDir + modelPLY, mesh, 0, mesh.VBuffer, false, null, null, null, null);
        } catch (IOException error) {
        	MipavUtil.displayError("Error while trying to save single mesh " + error);
        }
    }



	private void saveSpline(String outputDirectory, VOI data, Vector3f transformedOrigin, final String postFix) {

		VOIContour spline = (VOIContour) data.getCurves().elementAt(0);
		String voiDir = outputDirectory + File.separator;
		File voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
		} else { // voiFileDir does not exist
			//			System.err.println( "saveSpline " + voiDir);
			voiFileDir.mkdir();
		}
		voiDir = outputDirectory + File.separator + "statistics" + File.separator;
		voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) {} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) {} else { // voiFileDir
			// does
			// not
			// exist
			//			System.err.println( "saveSpline " + voiDir);
			voiFileDir.mkdir();
		}

		File file = new File(voiDir + data.getName() + postFix + ".csv");
		if (file.exists()) {
			file.delete();
			file = new File(voiDir + data.getName() + postFix + ".csv");
		}

		try {			
			final FileWriter fw = new FileWriter(file);
			final BufferedWriter bw = new BufferedWriter(fw);
			bw.write("index" + "," + "x_voxels" + "," + "y_voxels" + "," + "z_voxels" + "," + "x_um" + "," + "y_um" + "," + "z_um" + "\n");
			for (int i = 0; i < spline.size(); i++) {
				Vector3f position = spline.elementAt(i);
				bw.write(i + "," + (position.X - transformedOrigin.X) + "," + (position.Y - transformedOrigin.Y) + ","
						+ (position.Z - transformedOrigin.Z) + "," +

                        VOILatticeManagerInterface.VoxelSize * (position.X - transformedOrigin.X) + "," + VOILatticeManagerInterface.VoxelSize
                        * (position.Y - transformedOrigin.Y) + "," + VOILatticeManagerInterface.VoxelSize * (position.Z - transformedOrigin.Z) + "\n");
			}
			bw.newLine();
			bw.close();
		} catch (final Exception e) {
			System.err.println("CAUGHT EXCEPTION WITHIN writeXML() of FileVOI");
			e.printStackTrace();
		}
	}


	/**
	 * Straightens the worm image based on the input lattice positions. The image is straightened without first building a worm model.
	 * The image is segmented after clipping based on surface markers or the lattice shape.
	 * @param image
	 * @param resultExtents
	 */
	private void untwist(final ModelImage image, final int[] resultExtents, boolean saveTif)
	{
		long time = System.currentTimeMillis();
		int size = samplingPlanes.getCurves().size();

		String imageName = getImageName(image);
		ModelImage resultImage;
		if ( image.isColorImage() )
		{
			resultImage = new ModelImage( ModelStorageBase.ARGB, resultExtents, imageName + "_straight_unmasked.xml");
		}
		else
		{
			resultImage = new ModelImage( ModelStorageBase.FLOAT, resultExtents, imageName + "_straight_unmasked.xml");
		}	
		resultImage.setResolutions(new float[] {1, 1, 1});


		System.err.println( "starting untwist..." );

		int dimX = (resultImage.getExtents()[0]);
		int dimY = (resultImage.getExtents()[1]);
		int dimZ = size;
		Vector3f center = new Vector3f( dimX/2, dimY/2, 0 );

		// this is the untwisting code:
		final Vector3f[] corners = new Vector3f[4];
		for (int i = 0; i < size; i++)
		{			
			VOIContour kBox = (VOIContour) samplingPlanes.getCurves().elementAt(i);
			for (int j = 0; j < 4; j++) {
				corners[j] = kBox.elementAt(j);
			}

			writeDiagonal(image, resultImage, 0, i, resultExtents, corners);
			

//			System.err.println("");
//			System.err.println("");
//			System.err.println(i + "  " + corners[0] );
//			System.err.println(i + "  " + corners[1] );
//			System.err.println(i + "  " + corners[2] );
//			System.err.println(i + "  " + corners[3] );
		}

		System.err.println( "saving image " + imageName + " " + resultImage.getImageName() );
		saveImage(imageName, resultImage, saveTif);
		resultImage.disposeLocal(false);
		resultImage = null;


		System.err.println( "writeDiagonal " + AlgorithmBase.computeElapsedTime(time) );
		time = System.currentTimeMillis();
	}


	private void untwistSampleCount(final ModelImage image, final int[] resultExtents, boolean saveTif)
	{
		long time = System.currentTimeMillis();
		int size = samplingPlanes.getCurves().size();

		String imageName = getImageName(image);
		
		ModelImage resultImage = new ModelImage( ModelStorageBase.INTEGER, image.getExtents(), imageName + "_sampleCounts.xml");
		resultImage.setResolutions(new float[] {1, 1, 1});


		System.err.println( "starting untwist..." );

		// this is the untwisting code:
		final Vector3f[] corners = new Vector3f[4];
		for (int i = 0; i < size; i++)
		{			
			VOIContour kBox = (VOIContour) samplingPlanes.getCurves().elementAt(i);
			for (int j = 0; j < 4; j++) {
				corners[j] = kBox.elementAt(j);
			}

			writeDiagonalSampleCount(image, resultImage, 0, i, resultExtents, corners);
		}
		
		int max = -1;
		for ( int i = 0; i < resultImage.getDataSize(); i++ ) {
			int val = resultImage.getInt(i);
			System.err.println(i + "  " + val );
			if ( val > max ) max = val;
			if ( val == 1 ) {
				resultImage.set(i, 0);
			}
		}
		resultImage.setMin(0);
		resultImage.setMax(max);

		System.err.println( "saving image " + imageName + " " + resultImage.getImageName() + "  " + max );
		saveImage(imageName, resultImage, saveTif);
//		resultImage.disposeLocal(false);
//		resultImage = null;
		
		new ViewJFrameImage(resultImage);


		System.err.println( "writeDiagonal " + AlgorithmBase.computeElapsedTime(time) );
		time = System.currentTimeMillis();
	}

	
	/**
	 * Untwists the lattice and lattice interpolation curves, writing the
	 * straightened data to spreadsheet format .csv files and the straightened lattice
	 * to a VOI file.  The target slices for the lattice and curve data is generated
	 * from the distance along the curve. No segmentation or interpolation is necessary:
	 * @param image
	 * @param resultExtents
	 */
	private void untwistLattice(final ModelImage image, final int[] resultExtents)
	{
		String imageName = getImageName(image);

		// Straightens the interpolation curves:
		Vector3f[][] averageCenters = new Vector3f[centerPositions.size()][];
		final Vector3f[] corners = new Vector3f[4];		
		for (int i = 0; i < centerPositions.size(); i++)
		{			
			VOIContour kBox = (VOIContour) samplingPlanes.getCurves().elementAt(i);
			for (int j = 0; j < 4; j++) {
				corners[j] = kBox.elementAt(j);
			}
			averageCenters[i] = straightenFrame(image, i, resultExtents, corners, centerPositions.elementAt(i), leftPositions.elementAt(i), rightPositions.elementAt(i) );
		}

		// saves the data to the output directory:
		String voiDir = sharedOutputDir + File.separator + "statistics" + File.separator;

		File voiFileDir = new File(voiDir);
		if (voiFileDir.exists() && voiFileDir.isDirectory()) {
		} else if (voiFileDir.exists() && !voiFileDir.isDirectory()) {} else { // voiFileDir does not exist
			//			System.err.println( "untwistLattice" + voiDir);
			voiFileDir.mkdir();
		}

		File file = new File(voiDir + imageName + "_Frame_Straight.csv");
		if (file.exists()) {
			file.delete();
			file = new File(voiDir + imageName + "_Frame_Straight.csv");
		}

		try {

			final FileWriter fw = new FileWriter(file);
			final BufferedWriter bw = new BufferedWriter(fw);

			bw.write("Center" + "," + "X" + "," + "Y" + "," + "Z" + "," + "Left" + "," + "X" + "," + "Y" + "," + "Z" + "," + "Right" + "," + "X" + "," + "Y" + "," + "Z" + "\n");

			for ( int i = 0; i < centerPositions.size(); i++ )
			{
				if ( averageCenters[i] != null )
				{
					Vector3f pt = averageCenters[i][0];
					// calculate the output in worm coordinates:
					pt.X -= resultExtents[0] / 2f;
					pt.Y -= resultExtents[1] / 2f;
					//					pt.scale( resX, resY, 1 );
					bw.write("C" + i + "," + pt.X + "," + pt.Y + "," + pt.Z + ",");

					pt = averageCenters[i][1];
					// calculate the output in worm coordinates:
					pt.X -= resultExtents[0] / 2f;
					pt.Y -= resultExtents[1] / 2f;
					//					pt.scale( resX, resY, 1 );
					bw.write("L" + i + "," + pt.X + "," + pt.Y + "," + pt.Z + ",");

					pt = averageCenters[i][2];
					// calculate the output in worm coordinates:
					pt.X -= resultExtents[0] / 2f;
					pt.Y -= resultExtents[1] / 2f;
					//					pt.scale( resX, resY, 1 );
					bw.write("R" + i + "," + pt.X + "," + pt.Y + "," + pt.Z + "\n");
				}
			}

			bw.newLine();
			bw.close();
		} catch (final Exception e) {
			System.err.println("CAUGHT EXCEPTION WITHIN saveNucleiInfo");
			e.printStackTrace();
		}



		// Straightens and saves the lattice:
		FileIO fileIO = new FileIO();

		ModelImage resultImage = fileIO.readImage( outputDirectory + File.separator + "output_images" + File.separator + imageName + "_straight_unmasked.xml" );
		int dimX = (resultImage.getExtents()[0]);
		int dimY = (resultImage.getExtents()[1]);
		Vector3f center = new Vector3f( dimX/2, dimY/2, 0 );

		short id = (short) image.getVOIs().getUniqueID();
		final VOIVector lattice = new VOIVector();
		VOI leftSide = new VOI(id, "left", VOI.ANNOTATION, (float) Math.random());
		VOI rightSide = new VOI(id++, "right", VOI.ANNOTATION, (float) Math.random());
		lattice.add(leftSide);
		lattice.add(rightSide);
		float[] minDistance = new float[1];
		for (int i = 0; i < left.getCurves().size(); i++) {

			VOIContour kBox = (VOIContour) samplingPlanes.getCurves().elementAt(latticeSlices[i]);
			for (int j = 0; j < 4; j++) {
				corners[j] = kBox.elementAt(j);
			}

			final Vector3f leftPt = writeDiagonal(image, latticeSlices[i], resultExtents, corners, left.getCurves().elementAt(i).elementAt(0), minDistance );
			final Vector3f rightPt = writeDiagonal(image, latticeSlices[i], resultExtents, corners, right.getCurves().elementAt(i).elementAt(0), minDistance );

			//			System.err.println( i + " " + latticeSlices[i] + "    " + leftPt + "    " + rightPt );

			VOIWormAnnotation leftAnnotation = new VOIWormAnnotation((VOIWormAnnotation)left.getCurves().elementAt(i));
			leftAnnotation.clear();
			leftAnnotation.add(leftPt); 
			leftSide.getCurves().add(leftAnnotation);

			VOIWormAnnotation rightAnnotation = new VOIWormAnnotation((VOIWormAnnotation)right.getCurves().elementAt(i));
			rightAnnotation.clear();
			rightAnnotation.add(rightPt);
			rightSide.getCurves().add(rightAnnotation);
		}
		final float[] leftDistances = new float[leftSide.getCurves().size()];
		final float[] rightDistances = new float[leftSide.getCurves().size()];
		for (int i = 0; i < leftSide.getCurves().size(); i++) {
			leftDistances[i] = 0;
			rightDistances[i] = 0;
		}
		for (int i = 0; i < leftSide.getCurves().size() -1; i++) {
			leftDistances[i] = leftSide.getCurves().elementAt(i).elementAt(0).distance(leftSide.getCurves().elementAt(i + 1).elementAt(0));
			rightDistances[i] = rightSide.getCurves().elementAt(i).elementAt(0).distance(rightSide.getCurves().elementAt(i + 1).elementAt(0));
		}
//
//		resultImage.registerVOI(lattice);
//		lattice.setColor(new Color(0, 0, 255));
//		lattice.getCurves().elementAt(0).update(new ColorRGBA(0, 0, 1, 1));
//		lattice.getCurves().elementAt(1).update(new ColorRGBA(0, 0, 1, 1));
//		lattice.getCurves().elementAt(0).setClosed(false);
//		lattice.getCurves().elementAt(1).setClosed(false);
//
//		id = (short) image.getVOIs().getUniqueID();
//		for (int j = 0; j < leftSide.size(); j++) {
//			id = (short) image.getVOIs().getUniqueID();
//			final VOI marker = new VOI(id, "pair_" + j, VOI.POLYLINE, (float) Math.random());
//			final VOIContour mainAxis = new VOIContour(false);
//			mainAxis.add(leftSide.elementAt(j));
//			mainAxis.add(rightSide.elementAt(j));
//			marker.getCurves().add(mainAxis);
//			marker.setColor(new Color(255, 255, 0));
//			mainAxis.update(new ColorRGBA(1, 1, 0, 1));
//			if (j == 0) {
//				marker.setColor(new Color(0, 255, 0));
//				mainAxis.update(new ColorRGBA(0, 1, 0, 1));
//			}
//			resultImage.registerVOI(marker);
//		}

		voiDir = sharedOutputDir + File.separator + "straightened_lattice" + File.separator;
		//		saveAllVOIsTo(voiDir, resultImage);


		VOI latticePoints = new VOI( (short)0, "lattice", VOI.ANNOTATION, 0);
		for ( int j = 0; j < leftSide.getCurves().size(); j++ )
		{
			latticePoints.getCurves().add(leftSide.getCurves().elementAt(j));
			latticePoints.getCurves().add(rightSide.getCurves().elementAt(j));
		}
		LatticeModel.saveAnnotationsAsCSV(voiDir, "straightened_lattice.csv", latticePoints);
		saveLatticeStatistics(sharedOutputDir + File.separator, resultExtents[2], leftSide, rightSide, leftDistances, rightDistances, "_after");


		String voiSeamDir = sharedOutputDir + File.separator + "straightened_seamcells" + File.separator;
		latticePoints.getCurves().clear();
		for ( int j = 0; j < leftSide.getCurves().size(); j++ )
		{
			VOIWormAnnotation leftAnnotation = (VOIWormAnnotation) leftSide.getCurves().elementAt(j);
			VOIWormAnnotation rightAnnotation = (VOIWormAnnotation) rightSide.getCurves().elementAt(j);
			if ( leftAnnotation.isSeamCell() || rightAnnotation.isSeamCell() ) {
				latticePoints.getCurves().add(leftAnnotation);
				latticePoints.getCurves().add(rightAnnotation);
			}
		}
		LatticeModel.saveAnnotationsAsCSV(voiSeamDir, "straightened_seamcells.csv", latticePoints);
		
		transformedOrigin = new Vector3f( center );

		resultImage.disposeLocal(false);
		resultImage = null;
	}

	private void untwistMarkers(ModelImage image, final int[] resultExtents )
	{
		System.err.println( "untwistMarkers NEW " + paddingFactor + "  " + image.getImageFileName() );

		long time = System.currentTimeMillis();
		if ( samplingPlanes == null )
		{
			samplingPlanes = loadSamplePlanes( sharedOutputDir + File.separator );
		}
		if ( wormDiameters == null )
		{
			wormDiameters = loadDiameters( sharedOutputDir + File.separator );
		}
		int size = samplingPlanes.getCurves().size();
		
		String imageName = getImageName(image);

        ModelImage contourImage = null;
		VOIVector contourVector = new VOIVector();
		if ( latticeContours != null ) {
			System.err.println("using lattice contours");
			contourVector.add(latticeContours);
		}
		else {
			// Load skin marker contours:
			System.err.println(sharedOutputDir + File.separator + "output_images" + File.separator + imageName + "_contours.xml" );
			FileIO fileIO = new FileIO();
			contourImage = fileIO.readImage( sharedOutputDir + File.separator + "output_images" + File.separator + imageName + "_contours.xml" );
		}
		
//		FileIO fileIO = new FileIO();
//		ModelImage insideImage = fileIO.readImage( outputDirectory + File.separator + "model" + File.separator + imageName + "_interior.tif" );

		VOIContour[] contours = null;
		if ( contourVector.size() > 0 ) {
			for ( int i = 0; i < contourVector.elementAt(0).getCurves().size(); i++ )
			{	
				if ( contours == null ) {
					contours = new VOIContour[size];
				}
				VOIContour contour = (VOIContour)contourVector.elementAt(0).getCurves().elementAt(i);
				int index = (int) contour.elementAt(0).Z;
				contours[index] =  contour;
			}
		}

		
//		Box3f[] clipBoxes = new Box3f[size];
//		for ( int i = 0; i < size; i++ ) {
//			Vector3f pos = getCenter(i);
//			Vector3f[] axes = getBasisVectors(i);
//			float diameter = getDiameter(i);
//			clipBoxes[i] = new Box3f(pos, axes, new float[] {diameter,diameter,2});
//		}

		final Vector3f[] corners = new Vector3f[4];		
		float[] minDistance = new float[1];
		annotationsStraight = new VOI( (short)0, "straightened annotations", VOI.ANNOTATION, 0 );
		for ( int i = 0; i < markerCenters.size(); i++ )
		{
			int latticeSegment = markerLatticeSegments.elementAt(i);
			int startIndex = 0;
			int endIndex = size;
			if ( (latticeSegment > 0) && (latticeSegment < splineRangeIndex.length) ) {
				startIndex = splineRangeIndex[latticeSegment-1];
				endIndex = splineRangeIndex[latticeSegment];
			}
//			System.err.println( markerNames.elementAt(i) + "  " + latticeSegment + "  " + startIndex + "  " + endIndex );

//			for ( int j = 0; j < clipBoxes.length; j++ ) {
//	    		if ( ContBox3f.InBox( markerCenters.elementAt(i), clipBoxes[j] ) ) {
//	    			startIndex = Math.max(0, i - 2);
//	    			endIndex = Math.min(size -1, i + 2);
//	    			break;
//	    		}
//			}
			
			float minUntwist = Float.MAX_VALUE;
			int minSlice = -1;
			Vector3f minPt = new Vector3f();
			int tryCount = 0;
			int slice = markerSlices.elementAt(i);
			if (slice != -1 ) {
				startIndex = slice;
				endIndex = slice+1;
			}
			while ( minSlice == -1 && tryCount < 3 ) {
				for ( int j = startIndex; j < endIndex; j++ )
				{			
					// Calculate the straightened marker location:
					VOIContour kBox = (VOIContour) samplingPlanes.getCurves().elementAt(j);
					for (int k = 0; k < 4; k++) {
						corners[k] = kBox.elementAt(k);
					}
					Vector3f markerPt = writeDiagonal(image, j, resultExtents, corners, markerCenters.elementAt(i), minDistance );

					// If it is inside the skin marker contour:
					if ( contours == null && contourImage != null )
					{
						if ( ( minDistance[0] < (tryCount * 5) ) && 
								contourImage.getFloat( (int)markerPt.X, (int)markerPt.Y, j) != 0 )
						{
							if ( minDistance[0] < minUntwist ) {
								minUntwist = minDistance[0];
								minSlice = j;
								minPt.copy(markerPt);
							}
						}						

					}
					else
					{
						if ( ( minDistance[0] < (tryCount * 5) ) && ((contours[j] == null) || contours[j].contains( markerPt.X, markerPt.Y )) )
						{
							if ( minDistance[0] < minUntwist ) {
								minUntwist = minDistance[0];
								minSlice = j;
								minPt.copy(markerPt);
							}
						}						
					}

				}
				if ( minSlice != -1 ) {
					VOIWormAnnotation text = new VOIWormAnnotation();
					text.setText(markerNames.elementAt(i));
					text.add( new Vector3f(minPt) );
					text.add( new Vector3f(minPt) );
					annotationsStraight.getCurves().add(text);
				}
				tryCount++;
			}
			if ( minSlice == -1 && tryCount >= 3 ) {
				Vector3f testInterior = markerCenters.elementAt(i);
				System.err.println( "FAILED " + markerNames.elementAt(i) + "   " + startIndex + "  " + endIndex + "  " + markerCenters.elementAt(i) + "    " + minSlice + "   " + minUntwist );
//				if ( insideImage != null ) {
//					float insideValue = insideImage.getFloat( Math.round(testInterior.X), Math.round(testInterior.Y), Math.round(testInterior.Z));
//					System.err.println( "     " + insideValue );
//				}
			}

		}

		if ( contourImage != null ) {
			contourImage.disposeLocal(false);
			contourImage = null;
		}
//		if ( insideImage != null ) {
//			insideImage.disposeLocal(false);
//			insideImage = null;
//		}
		
		System.err.println( "TEST 2021: untwist markers (skin segmentation) " + AlgorithmBase.computeElapsedTime(time) );
		time = System.currentTimeMillis();

	}

	public ModelImage untwistAnnotations(String dir, ModelImage contourImage)
	{
		String imageName = getImageName(contourImage);
		
		String voiDir = dir + File.separator + "straightened_annotations" + File.separator + "straightened_annotations.csv";
		File file = new File(voiDir);
		boolean untwistAll = !file.exists();
		
		// if the lattice has changed untwist the image and generate the contours and contour image again:
		File lattice = new File(dir + File.separator + WormData.editLatticeOutput + File.separator + "lattice.csv");
		long latticeChanged = lattice.lastModified();
		File contourFile = new File( dir + File.separator + "output_images" + File.separator + imageName + "_contours.xml" );
		long contourChanged = contourFile.lastModified();
		
		ModelImage currentContourImage = contourImage;
		if ( latticeChanged > contourChanged ) {			
			// lattice was changed more recently than the contour image - restraighten all.
			untwistAll = true;
		}		
		if ( untwistAll || latticeChanged() ) 
		{
			initializeInterpolation(true);
			untwistImage( true );
			segmentLattice(imageA, true, paddingFactor, false);
			System.err.println( dir + File.separator + WormData.editLatticeOutput );
			saveLattice( dir + File.separator, WormData.editLatticeOutput );
			untwistAll = true;
			
			FileIO fileIO = new FileIO();
			currentContourImage = fileIO.readImage( dir + File.separator + "output_images" + File.separator + imageName + "_contours.xml" );
		}		
		else if ( !latticeInterpolationInit ) {
			initializeInterpolation(false);
		}
		if ( currentContourImage == null ) {
			FileIO fileIO = new FileIO();
			currentContourImage = fileIO.readImage( dir + File.separator + "output_images" + File.separator + imageName + "_contours.xml" );	
		}


		// load the existing annotation file - use it to determine which annotations need untwisting.
		VOI originalAnnotation = LatticeModel.readAnnotationsCSV(dir + File.separator + WormData.integratedAnnotationOutput + File.separator + "annotations.csv");

		// look for changes in annotations - build list to untwist:
		VOI changed = annotationChanged( annotationVOIs, originalAnnotation );

		// if the lattice has changed untwist the image and generate the contours and contour image again:
		File annotationTwisted = new File(dir + File.separator + WormData.integratedAnnotationOutput + File.separator + "annotations.csv");
		long twistedChanged = annotationTwisted.lastModified();
		File annotationStraight = new File( dir + File.separator + "straightened_annotations" + File.separator + "straightened_annotations.csv" );
		long straightChanged = annotationStraight.lastModified();

		VOI originalStraight = null;
		// if the twisted file has changed since straightened - then straighten all:
		if ( twistedChanged > straightChanged ) {
			untwistAll = true;
			System.err.println("annotations changed");
		}		
		// load the straightened annotations - if they exist.
		if ( file.exists() && !untwistAll ) {	
			if ( changed.getCurves().size() == 0 ) {
				// no changes just return:
				System.err.println("untwisting none...");

				return currentContourImage;
			}
			originalStraight = LatticeModel.readAnnotationsCSV(voiDir);
		}

		
		
		final int[] resultExtents = currentContourImage.getExtents();
		int size = resultExtents[2];

		final Vector3f[] corners = new Vector3f[4];		
		float[] minDistance = new float[1];
		annotationsStraight = new VOI( (short)0, "straightened annotations", VOI.ANNOTATION, 0 );
		
		if ( untwistAll ) {
			System.err.println("untwisting all...");
			for ( int i = 0; i < annotationVOIs.getCurves().size(); i++ )
			{
				VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);

				int latticeSegment = -1; //markerLatticeSegments.elementAt(i);
				int startIndex = 0;
				int endIndex = size;
				if ( (latticeSegment > 0) && (latticeSegment < splineRangeIndex.length) ) {
					startIndex = splineRangeIndex[latticeSegment-1];
					endIndex = splineRangeIndex[latticeSegment];
				}

				float minUntwist = Float.MAX_VALUE;
				int minSlice = -1;
				Vector3f minPt = new Vector3f();
				int tryCount = 0;
				int slice = -1; //markerSlices.elementAt(i);
				if (slice != -1 ) {
					startIndex = slice;
					endIndex = slice+1;
				}
				while ( minSlice == -1 && tryCount < 3 ) {
					for ( int j = startIndex; j < endIndex; j++ )
					{			
						// Calculate the straightened marker location:
						VOIContour kBox = (VOIContour) samplingPlanes.getCurves().elementAt(j);
						for (int k = 0; k < 4; k++) {
							corners[k] = kBox.elementAt(k);
						}
						Vector3f markerPt = writeDiagonal(imageA, j, resultExtents, corners, text.elementAt(0), minDistance );

						// If it is inside the skin marker contour:
						if ( contourImage != null )
						{
							if ( ( minDistance[0] < (tryCount * 5) ) && 
									contourImage.getFloat( (int)markerPt.X, (int)markerPt.Y, j) != 0 )
							{
								if ( minDistance[0] < minUntwist ) {
									minUntwist = minDistance[0];
									minSlice = j;
									minPt.copy(markerPt);
								}
							}						

						}
					}
					if ( minSlice != -1 ) {
						VOIWormAnnotation textStraight = new VOIWormAnnotation();
						textStraight.setSeamCell(text.isSeamCell());
						textStraight.setText(text.getText());
						textStraight.add( new Vector3f(minPt) );
						textStraight.add( new Vector3f(minPt) );
						annotationsStraight.getCurves().add(textStraight);
					}
					tryCount++;
				}
				//			System.err.println( markerNames.elementAt(i) + "   " + minSlice + "   " + minUntwist );

			}
		}
		else {
			System.err.println("untwisting some... " + changed.getCurves().size() );

			// remove change from original straight:
			for ( int i = 0; i < changed.getCurves().size(); i++ ) {      
				VOIWormAnnotation newA = (VOIWormAnnotation) changed.getCurves().elementAt(i);

	    		for ( int j = 0; j < originalStraight.getCurves().size(); j++ ) {
	            	VOIWormAnnotation orig = (VOIWormAnnotation) originalStraight.getCurves().elementAt(j);
	            	if ( newA.getText().equals(orig.getText()) ) {
	            		originalStraight.getCurves().remove(j);
	            		break;
	            	}
	    		}				
			}
			// untwist change -> original straight:

			for ( int i = 0; i < changed.getCurves().size(); i++ )
			{
				VOIWormAnnotation text = (VOIWormAnnotation) changed.getCurves().elementAt(i);

				int latticeSegment = -1; //markerLatticeSegments.elementAt(i);
				int startIndex = 0;
				int endIndex = size;
				if ( (latticeSegment > 0) && (latticeSegment < splineRangeIndex.length) ) {
					startIndex = splineRangeIndex[latticeSegment-1];
					endIndex = splineRangeIndex[latticeSegment];
				}

				float minUntwist = Float.MAX_VALUE;
				int minSlice = -1;
				Vector3f minPt = new Vector3f();
				int tryCount = 0;
				int slice = -1; //markerSlices.elementAt(i);
				if (slice != -1 ) {
					startIndex = slice;
					endIndex = slice+1;
				}
				while ( minSlice == -1 && tryCount < 3 ) {
					for ( int j = startIndex; j < endIndex; j++ )
					{			
						// Calculate the straightened marker location:
						VOIContour kBox = (VOIContour) samplingPlanes.getCurves().elementAt(j);
						for (int k = 0; k < 4; k++) {
							corners[k] = kBox.elementAt(k);
						}
						Vector3f markerPt = writeDiagonal(imageA, j, resultExtents, corners, text.elementAt(0), minDistance );

						// If it is inside the skin marker contour:
						if ( contourImage != null )
						{
							if ( ( minDistance[0] < (tryCount * 5) ) && 
									contourImage.getFloat( (int)markerPt.X, (int)markerPt.Y, j) != 0 )
							{
								if ( minDistance[0] < minUntwist ) {
									minUntwist = minDistance[0];
									minSlice = j;
									minPt.copy(markerPt);
								}
							}						

						}
					}
					if ( minSlice != -1 ) {
						VOIWormAnnotation textStraight = new VOIWormAnnotation();
						textStraight.setSeamCell(text.isSeamCell());
						textStraight.setText(text.getText());
						textStraight.add( new Vector3f(minPt) );
						textStraight.add( new Vector3f(minPt) );
						originalStraight.getCurves().add(textStraight);
					}
					tryCount++;
				}
			}
			
			// double check all current annotations are in original straight:
			if ( annotationVOIs.getCurves().size() < originalStraight.getCurves().size() ) {
				for ( int i = originalStraight.getCurves().size() - 1; i >= 0; i-- ) {
	            	VOIWormAnnotation orig = (VOIWormAnnotation) originalStraight.getCurves().elementAt(i);
	            	boolean found = false;
		    		for ( int j = 0; j < annotationVOIs.getCurves().size(); j++ ) {
		            	VOIWormAnnotation annotation = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(j);
		            	if ( annotation.getText().equals(orig.getText()) ) {
		            		found = true;
		            		break;
		            	}
		    		}
		    		if ( !found ) {
		    			originalStraight.getCurves().remove(i);
		    		}
				}
			}
			if ( annotationVOIs.getCurves().size() != originalStraight.getCurves().size() ) {
				MipavUtil.displayError("untwist annotations error" );
			}

			
			// copy into annotationsStraight:
			annotationsStraight.getCurves().clear();
			annotationsStraight.getCurves().addAll(originalStraight.getCurves());
		}
		
		// save annotations:
		saveAnnotationsAsCSV(sharedOutputDir + File.separator + WormData.integratedAnnotationOutput + File.separator, "annotations.csv", annotationVOIs);
		// save straight annotations:
		saveAnnotationsAsCSV(sharedOutputDir + File.separator + "straightened_annotations" + File.separator, "straightened_annotations.csv", annotationsStraight);
		
		return currentContourImage;
	}


	/**
	 * Untwists the worm image quickly for the preview mode - without saving any images or statistics
	 * Straightens the worm image based on the input lattice positions. The image is straightened without first building a worm model.
	 * The image is segmented after clipping based on surface markers or the lattice shape.
	 * @param image
	 * @param resultExtents
	 */
	private ModelImage[] untwistTest(final VolumeImage[] stack, final int[] resultExtents)
	{
		System.err.println("untwistTest "  + paddingFactor );
		long time = System.currentTimeMillis();
		int size = samplingPlanes.getCurves().size();

		String imageName = getImageName(stack[0].GetImage());
		ModelImage[] resultImage = new ModelImage[stack.length];
		for ( int i = 0; i < stack.length; i++ ) 
		{
			if ( stack[i].GetImage().isColorImage() )
			{
				resultImage[i] = new ModelImage( ModelStorageBase.ARGB, resultExtents, imageName + "_straight_unmasked.xml");
			}
			else
			{
				resultImage[i] = new ModelImage( ModelStorageBase.FLOAT, resultExtents, imageName + "_straight_unmasked.xml");
			}	
			resultImage[i].setResolutions(new float[] {1, 1, 1});
		}


		int dimX = resultExtents[0];
		int dimY = resultExtents[1];
		int dimZ = size;
		Vector3f center = new Vector3f( dimX/2f, dimY/2f, 0f );

		if ( latticeContours != null ) {
			latticeContours.dispose();
			latticeContours = null;
		}
		latticeContours = new VOI( (short)1, "contours", VOI.POLYLINE, (float) Math.random());

		float maxDist = Vector3f.normalize(center);
		// this is the untwisting code:
		final Vector3f[] corners = new Vector3f[4];
		
		VOIBase[] edgePoints = new VOIBase[numEllipsePts];
		for(int j = 0; j < numEllipsePts; ++j) {
			edgePoints[j] = displayContours[latticeSlices.length + j].getCurves().elementAt(0);
		}

		
		for (int i = 0; i < size; i++)
		{			
			VOIContour kBox = (VOIContour) samplingPlanes.getCurves().elementAt(i);
			for (int j = 0; j < 4; j++) {
				corners[j] = kBox.elementAt(j);
			}	

			float radius = (float) (1.05 * rightPositions.elementAt(i).distance(leftPositions.elementAt(i))/(2f));
			radius += paddingFactor;

			VOIContour contour = new VOIContour(true);
			
			//TODO: modify this to modify preview mode
			//makeEllipse2DA(Vector3f.UNIT_X, Vector3f.UNIT_Y, center, radius, contour);
			//mkitti 2023/04/17: use relative contours to build contour
			Vector3f displayCenter = centerPositions.get(i);
			float[] radii = new float[numEllipsePts];
			for(int j = 0; j < numEllipsePts; ++j) {
				radii[j] = Math.max(Vector3f.sub(edgePoints[j].get(i), displayCenter).length(), radius);
			}
			makeEllipse2DA(Vector3f.UNIT_X, Vector3f.UNIT_Y, center, radii, contour);		

			if ( (clipMask != null) && (clipMask.size() == dimZ) ) {
				//					System.err.println( "use clip mask " + i + "  " + clipMask.size() );
				boolean[] mask = clipMask.elementAt(i);
				for ( int j = 0; j < mask.length; j++ ) {
					if ( !mask[j] ) {
						// extend contour out to edge:
						Vector3f dir = Vector3f.sub( contour.elementAt(j), center );
						dir.normalize();
						dir.scale(maxDist);
						dir.add(center);
						contour.elementAt(j).copy(dir);
					}
				}
			}
			for ( int j = 0; j < contour.size(); j++ )
			{
				contour.elementAt(j).Z = i;
			}
			latticeContours.getCurves().add( contour );

			float radiusSq = radius*radius;
			for ( int j = 0; j < stack.length; j++ ) {
				writeDiagonalTest(stack[j].GetImage(), resultImage[j], 0, i, resultExtents, corners, radiusSq);
			}
		}

		for ( int i = 0; i < stack.length; i++ ) {
			if ( resultImage[i].getMin() > 0 ) {
				resultImage[i].setMin(0);
			}
		}
		
		// straighten lattice:
		short id = (short) imageA.getVOIs().getUniqueID();
		latticeStraight = new VOIVector();
		VOI leftSide = new VOI(id, "left", VOI.ANNOTATION, (float) Math.random());
		VOI rightSide = new VOI(id++, "right", VOI.ANNOTATION, (float) Math.random());
		latticeStraight.add(leftSide);
		latticeStraight.add(rightSide);
		float[] minDistance = new float[1];
		for (int i = 0; i < left.getCurves().size(); i++) {

			VOIContour kBox = (VOIContour) samplingPlanes.getCurves().elementAt(latticeSlices[i]);
			for (int j = 0; j < 4; j++) {
				corners[j] = kBox.elementAt(j);
			}

			final Vector3f leftPt = writeDiagonal(imageA, latticeSlices[i], resultExtents, corners, left.getCurves().elementAt(i).elementAt(0), minDistance );
			VOIWormAnnotation leftTemp = (VOIWormAnnotation)left.getCurves().elementAt(i);
			VOIWormAnnotation leftAnnotation = new VOIWormAnnotation(leftTemp);
			leftAnnotation.clear();
			leftAnnotation.add(leftPt);			
			leftSide.getCurves().add(leftAnnotation);
			
			final Vector3f rightPt = writeDiagonal(imageA, latticeSlices[i], resultExtents, corners, right.getCurves().elementAt(i).elementAt(0), minDistance );
			VOIWormAnnotation rightTemp = (VOIWormAnnotation)right.getCurves().elementAt(i);
			VOIWormAnnotation rightAnnotation = new VOIWormAnnotation(rightTemp);
			rightAnnotation.clear();
			rightAnnotation.add(rightPt);
			rightSide.getCurves().add(rightAnnotation);
			
//			System.err.println( leftTemp.getText() + "  " + rightTemp.getText() + "         " + leftAnnotation.getText() + "  " + rightAnnotation.getText() );
		}

		// straighten markers:
		if ( annotationVOIs != null ) {

			VOIContour[] contours = new VOIContour[size];
			for ( int i = 0; i < latticeContours.getCurves().size(); i++ )
			{	
				VOIContour contour = (VOIContour)latticeContours.getCurves().elementAt(i);
				int index = (int) contour.elementAt(0).Z;
				contours[index] =  contour;
			}

			annotationsStraight = new VOI( (short)0, "annotationVOIs", VOI.ANNOTATION, 0 );
			String failList = new String("The following annotations are outside the worm bounds: \n" );


			boolean allFound = false;
			boolean[] found = new boolean[annotationVOIs.getCurves().size()];
			for ( int i = 0; i < found.length; i++ ) {
				found[i] = false;
			}
			int tryCount = 0;
			while ( !allFound && (tryCount < 3) )
			{
				for ( int i = 0; i < size; i++ )
				{
					for ( int j = 0; j < annotationVOIs.getCurves().size(); j++ )
					{
						VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(j);
						// if first loop or still no matching point:
						if ( (tryCount == 0) || (text.getUntwistTest() == null) )
						{
							int startIndex = 0;
							int endIndex = size;
							int latticeSegment = text.getLatticeSegment();
							if ( (latticeSegment > 0) && (latticeSegment < splineRangeIndex.length) ) {
								startIndex = splineRangeIndex[latticeSegment-1];
								endIndex = splineRangeIndex[latticeSegment];
							}
							int slice = text.getSlice();
							if ( slice != -1 ) {
								startIndex = slice;
								endIndex = slice;
							}
							if ( i >= startIndex && i <= endIndex ) 
							{
								// untwist this annotation:
								// Calculate the straightened marker location:
								VOIContour kBox = (VOIContour) samplingPlanes.getCurves().elementAt(i);
								for (int k = 0; k < 4; k++) {
									corners[k] = kBox.elementAt(k);
								}
								Vector3f untwistPt = writeDiagonal(imageA, i, resultExtents, corners, text.firstElement(), minDistance );

								// If it is inside the skin marker contour:
								if ( ( minDistance[0] < (tryCount * 5) ) && ((contours[i] == null) || contours[i].contains( untwistPt.X, untwistPt.Y )) )
								{
									text.untwistTest( untwistPt, minDistance[0] );
									found[j] = true;
								}
								text.untwistTestNoBounds( untwistPt, minDistance[0] );
							}
						}
					}
				}
				allFound = true;
				for ( int j = 0; j < found.length; j++ )
				{
					allFound &= found[j];
				}
				tryCount++;
			}

			int failCount = 0;
			for ( int i = 0; i < annotationVOIs.getCurves().size(); i++ )
			{
				VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
				// No matching point:
				if ( text.getUntwistTest() == null )
				{
					failList += text.getText() + "\n";
					failCount++;
					System.err.println( text.getText() + " FAIL" );
					VOIWormAnnotation newText = new VOIWormAnnotation(text);
					newText.firstElement().copy(text.getUntwistTestNoBounds());
					newText.lastElement().copy(text.getUntwistTestNoBounds());
					newText.setColor(Color.red);
					annotationsStraight.getCurves().add(newText);
				}
				else
				{
					VOIWormAnnotation newText = new VOIWormAnnotation(text);
					newText.firstElement().copy(text.getUntwistTest());
					newText.lastElement().copy(text.getUntwistTest());
					annotationsStraight.getCurves().add(newText);
				}
			}
			if ( failCount > 0 ) {
				MipavUtil.displayInfo(failList);
			}
		}
		System.err.println( "Untwisting TEST elapsed time =  " + AlgorithmBase.computeElapsedTime(time) );

		return resultImage;
	}

	private static String getPrefix(String name) {
		String prefix = new String();
		for ( int j = 0; j < name.length(); j++ ) {
			if ( Character.isLetter(name.charAt(j) ) ) {
				prefix += name.charAt(j);
			}
			else {
				break;
			}
		}
		return prefix;
	}
	
	private boolean containsAnnotation( VOI annotationsList, VOIWormAnnotation text ) {
		for ( int i = 0; i < annotationsList.getCurves().size(); i++ ) {
			if ( annotationsList.getCurves().elementAt(i) == text ) {
				return true;
			}
		}
		return false;
	}
	
	private void updateSplineControlPoints() {
		if ( splineControlPtsList == null ) return;
		if ( splineControlPtsList.size() == 0 ) return;
		
		// check for added / removed points:
		// update spline:
		for ( int i = splineControlPtsList.size() - 1; i >= 0; i-- )
		{
			AnnotationSplineControlPts annotationSpline = splineControlPtsList.elementAt(i);
			// remove deleted points:
			for ( int j = annotationSpline.annotations.size() - 1; j >= 0; j-- ) {
				if ( !containsAnnotation(annotationVOIs, annotationSpline.annotations.elementAt(j) ) )
				{
					annotationSpline.annotations.remove(j);
				}
				else if ( !annotationSpline.prefix.equals( getPrefix(annotationSpline.annotations.elementAt(j).getText()) ) ) 
				{
					annotationSpline.annotations.remove(j);
				}
			}
			// add new points w/same prefix:
			for ( int j = 0; j < annotationVOIs.getCurves().size(); j++ ) {
				VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(j);
				if ( getPrefix(text.getText()).equals( annotationSpline.prefix ) ) {
					if ( !annotationSpline.annotations.contains(text) ) {
						// find two closest pts and add it...
						int nearest1 = -1;
						float minDist1 = Float.MAX_VALUE;
						for ( int k = 0; k < annotationSpline.annotations.size(); k++ ) {
							float dist = text.elementAt(0).distance( annotationSpline.annotations.elementAt(k).elementAt(0) );
							if ( dist < minDist1 ) {
								minDist1 = dist;
								nearest1 = k;
							}
						}
						System.err.println(text.getText() + "  " + nearest1 + "  " + annotationSpline.annotations.size() );
						if ( nearest1 == 0 ) {
							// place either before of after first element:
							float distBetween = annotationSpline.annotations.elementAt(nearest1).elementAt(0).distance( annotationSpline.annotations.elementAt(nearest1+1).elementAt(0) );
							float distNext = text.elementAt(0).distance( annotationSpline.annotations.elementAt(1).elementAt(0) );
							if ( distNext > distBetween ) {
								annotationSpline.annotations.add(0, text);
							}
							else {
								annotationSpline.annotations.add(1, text);
							}							
						}
						else if ( nearest1 == annotationSpline.annotations.size() -1 ) {
							// place either before of after last element:
							float distBetween = annotationSpline.annotations.elementAt(nearest1).elementAt(0).distance( annotationSpline.annotations.elementAt(nearest1-1).elementAt(0) );
							float distNext = text.elementAt(0).distance( annotationSpline.annotations.elementAt(nearest1-1).elementAt(0) );
							if ( distNext > distBetween ) {
								annotationSpline.annotations.add(text);
							}
							else {
								annotationSpline.annotations.add(nearest1-1, text);
							}							
						}
						else if ( nearest1 != -1 ) {
							float distPrev = text.elementAt(0).distance( annotationSpline.annotations.elementAt(nearest1-1).elementAt(0) );						
							float distNext = text.elementAt(0).distance( annotationSpline.annotations.elementAt(nearest1+1).elementAt(0) );
							if ( distPrev <= distNext ) {
								annotationSpline.annotations.add(nearest1, text);
							}
							else {
								annotationSpline.annotations.add(nearest1+1, text);
							}
						}
					}
				}
			}
			if ( annotationSpline.annotations.size() < 2 ) {
				imageA.unregisterVOI(annotationSpline.curveVOI);
				splineControlPtsList.remove(i);
				annotationSpline.curveVOI.dispose();
				annotationSpline.curveVOI = null;

				updateCurveListeners();
			}
			else {
				NaturalSpline3 spline = smoothCurve(annotationSpline.annotations);

				//display:
				imageA.unregisterVOI(annotationSpline.curveVOI);

				annotationSpline.curveVOI.getCurves().clear();
				annotationSpline.curve.dispose();
				annotationSpline.curve = null;
				annotationSpline.curve = new VOIContour(false);

				annotationSpline.curveVOI.dispose();
				annotationSpline.curveVOI = null;
				annotationSpline.curveVOI = new VOI((short)splineControlPtsList.size(), annotationSpline.name );
				annotationSpline.curveVOI.getCurves().add(annotationSpline.curve);


				float length = spline.GetLength(0, 1);
				int maxLength = (int)Math.ceil(length);
				float step = 1;
				if ( maxLength != length )
				{
					step = (length / maxLength);
				}
				for (int j = 0; j <= maxLength; j++) {
					final float t = spline.GetTime(j*step);
					annotationSpline.curve.add(spline.GetPosition(t));
				}
				annotationSpline.curveVOI.setColor(Color.red);
				annotationSpline.curve.update(new ColorRGBA(1, 0, 0, 1));

				imageA.registerVOI(annotationSpline.curveVOI);
			}
			imageA.notifyImageDisplayListeners();
		}
		
		for ( int i = 0; i < annotationVOIs.getCurves().size(); i++ ) {
			VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
			text.setCurveAnnotation(false);
		}
		for ( int i = 0; i < splineControlPtsList.size(); i++ )
		{
			AnnotationSplineControlPts annotationSpline = splineControlPtsList.elementAt(i);
			// update annotations with isCurveAnnotation flag:
			for ( int j = 0; j < annotationSpline.annotations.size(); j++ ) {
				annotationSpline.annotations.elementAt(j).setCurveAnnotation(true);
			}
		}
	}
	
	/**
	 * Updates the list of listeners that the annotations have
	 * changed. This is how the latticeModel communicates changes
	 * to the different plugins that display lists of annotations, etc.
	 */
	private void updateAnnotationListeners() {
		updateSplineControlPoints();
		if ( annotationListeners != null ) {
			for ( int i = 0; i < annotationListeners.size(); i++ ) {
				annotationListeners.elementAt(i).annotationChanged();
			}
		}
	}
	
	private void updateCurveListeners() {
		if ( curveListeners != null ) {
			for ( int i = 0; i < curveListeners.size(); i++ ) {
				curveListeners.elementAt(i).curveChanged();
			}
		}
	}

	private void updateLatticeListeners() {
		if ( latticeListeners != null ) {
			for ( int i = 0; i < latticeListeners.size(); i++ ) {
				latticeListeners.elementAt(i).latticeChanged();
			}
		}
	}
		
	private void makeLatticeContours() {
		if ( leftContour != null ) {
			imageA.unregisterVOI(leftContour);
			if ( leftContour.getCurves() != null ) {
				leftContour.getCurves().clear();
			}
		}
		else {
			final short id = (short) imageA.getVOIs().getUniqueID();
			leftContour = new VOI(id, "leftContour", VOI.POLYLINE, (float) Math.random());
		}
		if ( rightContour != null ) {
			imageA.unregisterVOI(rightContour);
			if ( leftContour.getCurves() != null ) {
				rightContour.getCurves().clear();
			}
		}
		else {
			final short id = (short) imageA.getVOIs().getUniqueID();
			rightContour = new VOI(id, "rightContour", VOI.POLYLINE, (float) Math.random());
		}

		VOIContour leftC = new VOIContour(false);
		leftContour.getCurves().add(leftC);
		
		VOIContour rightC = new VOIContour(false);
		rightContour.getCurves().add(rightC);

		for ( int i = 0; i < left.getCurves().size(); i++ ) {
			leftC.add(left.getCurves().elementAt(i).elementAt(0));
		}
		for ( int i = 0; i < right.getCurves().size(); i++ ) {
			rightC.add(right.getCurves().elementAt(i).elementAt(0));
		}

		leftContour.setColor(new Color(0, 0, 255));
		leftC.update(new ColorRGBA(0, 0, 1, 1));
		
		rightContour.setColor(new Color(0, 0, 255));
		rightC.update(new ColorRGBA(0, 0, 1, 1));

		imageA.registerVOI(leftContour);
		imageA.registerVOI(rightContour);
			
		imageA.notifyImageDisplayListeners();
	}
	
	private static float[] precalculateCrossSectionBasis(final int nMaxFFTDim, final int nSamples) {
		//VOIBase currentSectionCurve = displayContours[selectedSectionIndex].getCurves().elementAt(0);
		//final int nMaxFFTDim = numEllipsePts;
		//final int nSamples = crossSectionSamples;
		final int ratio_nMaxFFTDim_nSamples = nMaxFFTDim/nSamples;
		final int fftShiftOffset = (nMaxFFTDim-nSamples)/2;
		float[] radialDistances = new float[nSamples];
		System.out.println("ratio_nMaxFFTDim_nSamples: " + ratio_nMaxFFTDim_nSamples);
		System.out.println("fftShiftOffset: " + fftShiftOffset);
		radialDistances[0] = 1.0f;
		for(int i = 1; i < nSamples; i += 1) {
			radialDistances[i] = 0.0f;
		}
		ModelImage img = new ModelImage(ModelStorageBase.DataType.FLOAT, new int[] { nSamples }, "radialDistances");
		ModelImage padded_image = new ModelImage(DataType.COMPLEX, new int[] { nMaxFFTDim }, "padded");
		AlgorithmFFT fft;
		//Vector3f delta;
		//float currentRadius = 0.0f;
		float [] fourierR = new float[nSamples];
		float [] fourierI = new float[nSamples];
		float [] paddingR = new float[nMaxFFTDim];
		float [] paddingI = new float[nMaxFFTDim];
		Arrays.fill(paddingR, 0.0f);
		Arrays.fill(paddingI, 0.0f);
		try {
			img.importData(0, radialDistances, false);
			fft = new AlgorithmFFT(img, AlgorithmFFT.FORWARD, false, true, false, false);
			fft.runAlgorithm();
			img.exportComplexData(0, nSamples, fourierR, fourierI);
			System.arraycopy(fourierR, 0, paddingR, fftShiftOffset, fourierR.length);
			System.arraycopy(fourierI, 0, paddingI, fftShiftOffset, fourierI.length);
			padded_image.importComplexData(0, paddingR, paddingI, false, false);
			fft = new AlgorithmFFT(padded_image, AlgorithmFFT.INVERSE, false, true, false, false);
			fft.runAlgorithm();
			radialDistances = fft.getRealData();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Radial distance for nSamples " + nSamples + ":");
		for(int i = 0; i < nMaxFFTDim; i++) {
			System.out.println(radialDistances[i]);
		}
		System.out.println("End Radial distance for nSamples " + nSamples + ":");
		
		return radialDistances;
	}
	
	private static float[][] precalculateCrossSectionBases(final int  numEllipsePts) {
		final int nBases = 6;
		float[][] bases = new float[nBases][];
		// 1, 2, 4, 8, 16, 32
		int nSamples = 1;
		for(int nSamplesPower = 0; nSamplesPower < nBases; ++nSamplesPower) {
			bases[nSamplesPower] = precalculateCrossSectionBasis(numEllipsePts, nSamples);
			nSamples *= 2;
		}
		return bases;
	}
	
	/**
	 * Updates the lattice data structures for rendering whenever the user changes the lattice.
	 * 
	 * @param rebuild
	 */
	private void updateLattice(final boolean rebuild) {
		if (left == null || right == null) {
			updateLatticeListeners();
			return;
		}
		if (right.getCurves().size() == 0) {
			updateLatticeListeners();
			return;
		}
		
		makeLatticeContours();
		
		if (rebuild) {
			// System.err.println( "new pt added" );
			if (latticeGrid != null) {
				for (int i = latticeGrid.size() - 1; i >= 0; i--) {
					final VOI marker = latticeGrid.remove(i);
					imageA.unregisterVOI(marker);
				}
			} else {
				latticeGrid = new VOIVector();
			}
			for (int j = 0; j < Math.min(left.getCurves().size(), right.getCurves().size()); j++) {
				final short id = (short) imageA.getVOIs().getUniqueID();
				final VOI marker = new VOI(id, "pair_" + j, VOI.POLYLINE, (float) Math.random());
				final VOIContour mainAxis = new VOIContour(false);
				mainAxis.add(left.getCurves().elementAt(j).elementAt(0));
				mainAxis.add(right.getCurves().elementAt(j).elementAt(0));
				marker.getCurves().add(mainAxis);
				marker.setColor(new Color(255, 255, 0));
				mainAxis.update(new ColorRGBA(1, 1, 0, 1));
				if (j == 0) {
					marker.setColor(new Color(0, 255, 0));
					mainAxis.update(new ColorRGBA(0, 1, 0, 1));
				}
				imageA.registerVOI(marker);
				latticeGrid.add(marker);
			}
		} else {
			for (int i = 0; i < latticeGrid.size(); i++) {
				final VOI marker = latticeGrid.elementAt(i);
				marker.getCurves().elementAt(0).elementAt(0).copy(left.getCurves().elementAt(i).elementAt(0));
				marker.getCurves().elementAt(0).elementAt(1).copy(right.getCurves().elementAt(i).elementAt(0));
				marker.update();
			}
		}
		left.update();
		right.update();

		if (centerLine != null) {
			imageA.unregisterVOI(centerLine);
		}
		if (rightLine != null) {
			imageA.unregisterVOI(rightLine);
		}
		if (leftLine != null) {
			imageA.unregisterVOI(leftLine);
		}
		boolean showContours = false;
		if (displayContours != null) {
			for ( int i = 0; i < displayContours.length; i++ ) {
				boolean show = (imageA.isRegistered(displayContours[i]) != -1);
				if (show) {
					imageA.unregisterVOI(displayContours[i]);
				}
				showContours |= show;				
			}
		}

		if ( (left.getCurves().size() == right.getCurves().size()) && (left.getCurves().size() >= 2)) {
			if(editingCrossSections && selectedSectionIndex != -1 && center != null) {
				editedCrossSections[selectedSectionIndex] = true;
				
				Vector3f currentCenter = center.elementAt(selectedSectionIndex); 				//null pointer exception happened here
				Vector3f current = displayContours[selectedSectionIndex].getCurves().elementAt(0).elementAt(selectedSectionIndex2);
				Vector3f first = displayContours[selectedSectionIndex].getCurves().elementAt(0).elementAt(selectedSectionIndex2);
				Vector3f changed = sectionMarker.getCurves().elementAt(0).elementAt(0);
				
				float deltaLength = Vector3f.sub(currentCenter, changed).length() - Vector3f.sub(currentCenter, current).length();
				
				final boolean quickGaussian = false;
				final boolean fourier = false;
				final boolean fourierPrecalculated = true;
				
				if (numEllipsePts == crossSectionSamples) {
					current.copy(changed);
					if( updateCrossSectionOnDrag ) {
						updateSplinesOnly();
					}
				} else if(quickGaussian) {
				
					int prevIndex = selectedSectionIndex2-1 % numEllipsePts;
					Vector3f prev = displayContours[selectedSectionIndex].getCurves().elementAt(0).elementAt(prevIndex);
					Vector3f prevDelta = Vector3f.sub(currentCenter, prev);
					prevDelta.normalize();
					prevDelta.scale(deltaLength*0.37f);
					prev.sub(prevDelta);
					
					prevIndex = selectedSectionIndex2-2 % numEllipsePts;
					prev = displayContours[selectedSectionIndex].getCurves().elementAt(0).elementAt(prevIndex);
					prevDelta = Vector3f.sub(currentCenter, prev);
					prevDelta.normalize();
					prevDelta.scale(deltaLength*0.14f);
					prev.sub(prevDelta);
					
					int nextIndex = selectedSectionIndex2+1 % numEllipsePts;
					Vector3f next = displayContours[selectedSectionIndex].getCurves().elementAt(0).elementAt(nextIndex);
					Vector3f nextDelta = Vector3f.sub(currentCenter, next);
					nextDelta.normalize();
					nextDelta.scale(deltaLength*0.37f);
					next.sub(nextDelta);
					
					nextIndex = selectedSectionIndex2+1 % numEllipsePts;
					next = displayContours[selectedSectionIndex].getCurves().elementAt(0).elementAt(nextIndex);
					nextDelta = Vector3f.sub(currentCenter, next);
					nextDelta.normalize();
					nextDelta.scale(deltaLength*0.14f);
					next.sub(nextDelta);	
					
					displayContours[prevIndex].update();
					displayContours[nextIndex].update();
					
					current.copy(changed);
				
				} else if(fourier) {
					VOIBase currentSectionCurve = displayContours[selectedSectionIndex].getCurves().elementAt(0);
					final int nMaxFFTDim = numEllipsePts;
					final int nSamples = crossSectionSamples;
					final int ratio_nMaxFFTDim_nSamples = nMaxFFTDim/nSamples;
					final int fftShiftOffset = (nMaxFFTDim-nSamples)/2;
					float[] radialDistances = new float[nSamples];
					System.out.println("ratio_nMaxFFTDim_nSamples: " + ratio_nMaxFFTDim_nSamples);
					System.out.println("fftShiftOffset: " + fftShiftOffset);
					radialDistances[0] = Vector3f.sub(currentCenter, changed).length();
					for(int i = ratio_nMaxFFTDim_nSamples; i < numEllipsePts; i += ratio_nMaxFFTDim_nSamples) {
						radialDistances[i/ratio_nMaxFFTDim_nSamples] = Vector3f.sub(currentCenter, currentSectionCurve.elementAt((selectedSectionIndex2 + i) % numEllipsePts)).length();
					}
					ModelImage img = new ModelImage(ModelStorageBase.DataType.FLOAT, new int[] { nSamples }, "radialDistances");
					ModelImage padded_image = new ModelImage(DataType.COMPLEX, new int[] { 32 }, "padded");
					AlgorithmFFT fft;
					Vector3f delta;
					float currentRadius = 0.0f;
					float [] fourierR = new float[nSamples];
					float [] fourierI = new float[nSamples];
					float [] paddingR = new float[nMaxFFTDim];
					float [] paddingI = new float[nMaxFFTDim];
					Arrays.fill(paddingR, 0.0f);
					Arrays.fill(paddingI, 0.0f);
					try {
						img.importData(0, radialDistances, false);
						fft = new AlgorithmFFT(img, AlgorithmFFT.FORWARD, false, true, false, false);
						fft.runAlgorithm();
						img.exportComplexData(0, nSamples, fourierR, fourierI);
						System.arraycopy(fourierR, 0, paddingR, fftShiftOffset, fourierR.length);
						System.arraycopy(fourierI, 0, paddingI, fftShiftOffset, fourierI.length);
						padded_image.importComplexData(0, paddingR, paddingI, false, false);
						fft = new AlgorithmFFT(padded_image, AlgorithmFFT.INVERSE, false, true, false, false);
						fft.runAlgorithm();
						radialDistances = fft.getRealData();
						
						Vector3f firstDelta = Vector3f.sub(currentCenter, first);
						firstDelta.normalize();
						float angle = 0.0f;
						int idx = 0;
						for(int i = 0; i < numEllipsePts; i += 1) {
							idx = (selectedSectionIndex2 + i) % numEllipsePts;
							current = currentSectionCurve.elementAt(idx);
							delta = Vector3f.sub(currentCenter, current);
							currentRadius = delta.length();
							//System.out.println("Delta: " + delta);
							//System.out.print(currentRadius + ", " + radialDistances[i]*ratio_nMaxFFTDim_nSamples + ", ");
							delta.normalize();
							angle = Vector3f.angle(delta, firstDelta);
							//System.out.println(radialDistances[i]*ratio_nMaxFFTDim_nSamples - currentRadius);
							delta.scale(radialDistances[i]*ratio_nMaxFFTDim_nSamples - currentRadius);
							//System.out.println(delta.length());
							current.sub(delta);
							//System.out.println(Vector3f.sub(currentCenter, current).length());
							//System.out.println("Angle " + idx + ": " + angle);
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if(fourierPrecalculated) {
					VOIBase currentSectionCurve = displayContours[selectedSectionIndex].getCurves().elementAt(0);
					final int nMaxFFTDim = numEllipsePts;
					final int nSamples = crossSectionSamples;
					final int ratio_nMaxFFTDim_nSamples = nMaxFFTDim/nSamples;
					//final int fftShiftOffset = (nMaxFFTDim-nSamples)/2;
					float[] radialDistances = new float[nSamples];
					/*
					System.out.println("ratio_nMaxFFTDim_nSamples: " + ratio_nMaxFFTDim_nSamples);
					System.out.println("fftShiftOffset: " + fftShiftOffset);
					radialDistances[0] = Vector3f.sub(currentCenter, changed).length();
					for(int i = ratio_nMaxFFTDim_nSamples; i < numEllipsePts; i += ratio_nMaxFFTDim_nSamples) {
						radialDistances[i/ratio_nMaxFFTDim_nSamples] = Vector3f.sub(currentCenter, currentSectionCurve.elementAt((selectedSectionIndex2 + i) % numEllipsePts)).length();
					}
					ModelImage img = new ModelImage(ModelStorageBase.DataType.FLOAT, new int[] { nSamples }, "radialDistances");
					ModelImage padded_image = new ModelImage(DataType.COMPLEX, new int[] { 32 }, "padded");
					AlgorithmFFT fft;*/
					Vector3f delta;
					float currentRadius = 0.0f;
					/*
					img.importData(0, radialDistances, false);
					fft = new AlgorithmFFT(img, AlgorithmFFT.FORWARD, false, true, false, false);
					fft.runAlgorithm();
					img.exportComplexData(0, nSamples, fourierR, fourierI);
					System.arraycopy(fourierR, 0, paddingR, fftShiftOffset, fourierR.length);
					System.arraycopy(fourierI, 0, paddingI, fftShiftOffset, fourierI.length);
					padded_image.importComplexData(0, paddingR, paddingI, false, false);
					fft = new AlgorithmFFT(padded_image, AlgorithmFFT.INVERSE, false, true, false, false);
					fft.runAlgorithm();
					radialDistances = fft.getRealData();
					*/
					//int baseIndex = (int) ( Math.log(numEllipsePts / crossSectionSamples) / Math.log(2.0) );
					int baseIndex = (int) ( Math.log(crossSectionSamples) / Math.log(2.0) );
					System.out.println("Base index: " + baseIndex);
					radialDistances = crossSectionBases[baseIndex].clone();
					
					Vector3f firstDelta = Vector3f.sub(currentCenter, first);
					firstDelta.normalize();
					float angle = 0.0f;
					int idx = 0;
					for(int i = 0; i < numEllipsePts; i += 1) {
						idx = (selectedSectionIndex2 + i) % numEllipsePts;
						current = currentSectionCurve.elementAt(idx);
						delta = Vector3f.sub(currentCenter, current);
						currentRadius = delta.length();
						//System.out.println("Delta: " + delta);
						//System.out.print(currentRadius + ", " + radialDistances[i]*ratio_nMaxFFTDim_nSamples + ", ");
						delta.normalize();
						angle = Vector3f.angle(delta, firstDelta);
						//System.out.println(radialDistances[i]*ratio_nMaxFFTDim_nSamples - currentRadius);
						delta.scale(radialDistances[i]*ratio_nMaxFFTDim_nSamples*deltaLength);
						//System.out.println(delta.length());
						current.sub(delta);
						//System.out.println(Vector3f.sub(currentCenter, current).length());
						//System.out.println("Angle " + idx + ": " + angle);
					}
					
					if( updateCrossSectionOnDrag ) {
						updateSplinesOnly();
					}

				} else {
					// just update the current point
					current.copy(changed);
				}
				displayContours[selectedSectionIndex].update();
			} else {
				generateCurves(5);
			}
			if (showContours) {
				generateTriMesh(false, false);
				for ( int i = 0; i < displayContours.length; i++ ) {
					if ( displayContours[i] != null ) {
						imageA.registerVOI(displayContours[i]);						
					}
				}
//				System.err.println("test lattice against mesh here " + showContours);
//				testLatticeMeshIntersection();
			}				
		}

		updateSelected();

		latticeInterpolationInit = false;

		updateLatticeListeners();

		// when everything's done, notify the image listeners
		imageA.notifyImageDisplayListeners();
	}

	/**
	 * Updates the lattice data structures on undo/redo.
	 */
	private void updateLinks() {
		if (latticeGrid != null) {
			latticeGrid.clear();
		} else {
			latticeGrid = new VOIVector();
		}

		annotationVOIs = null;
		leftMarker = null;
		rightMarker = null;
		VOIVector vois = imageA.getVOIs();
//		System.err.println( "updateLinks " + vois.size() );
		for (int i = 0; i < vois.size(); i++) {
			final VOI voi = vois.elementAt(i);
			final String name = voi.getName();
//			System.err.println( vois.elementAt(i).getName() );
			if (name.equals("left")) {
				left = voi;
			} else if (name.equals("right")) {
				right = voi;
			} else if (name.equals("leftContour")) {
				leftContour = voi;
			} else if (name.equals("rightContour")) {
				rightContour = voi;
			} else if (name.equals("left line")) {
				leftLine = voi;
			} else if (name.equals("right line")) {
				rightLine = voi;
			} else if (name.equals("center line")) {
				centerLine = voi;
			} else if (name.contains("pair_")) {
				latticeGrid.add(voi);
			} else if (name.contains("wormContours")) {
				if ( displayContours == null ) {
					displayContours = new VOI[left.getCurves().size()];
				}
				int index = Integer.valueOf(name.substring(name.indexOf("wormContours") + 13));
				displayContours[index] = voi;
			} else if (name.contains("interpolatedContours")) {
				displayInterpolatedContours = voi;
			} else if (name.contains("showSelected")) {
				showSelectedVOI = voi;
				imageA.unregisterVOI(showSelectedVOI);
				showSelectedVOI = null;
//				System.err.println("updateLinks showSelected ");
			} else if (name.equals("leftMarker")) {
				leftMarker = voi;
			} else if (name.equals("rightMarker")) {
				rightMarker = voi;
			} else if (name.contains("annotationVOIs")) {
//				System.err.println("updateLinks update annotations: " + name + "   " + annotationVOIs );
				annotationVOIs = voi;
			}
		}
		imageA.unregisterVOI(leftContour);
		imageA.unregisterVOI(rightContour);
		
		lattice.clear();
		lattice.add(left);
		lattice.add(right);
		
		clear3DSelection();
		colorAnnotations();
//		System.err.println("updateLinks " + (annotationVOIs != null) );
		if ( annotationVOIs != null ) {
			//			for ( int i = 0; i < annotationVOIs.getCurves().size(); i++ ) {
			//				final VOIWormAnnotation text = (VOIWormAnnotation) annotationVOIs.getCurves().elementAt(i);
			//				if ( text.isSelected() ) {
			//					text.setSelected(false);
			//					text.updateSelected(imageA);
			//				}
			//			}
		}
		updateLattice(true);
		showLatticeLabels(displayLatticeLabels);

		vois = imageA.getVOIs();
		for (int i = 0; i < vois.size(); i++) {
			final VOI voi = vois.elementAt(i);
			final String name = voi.getName(); 
			if (name.contains("showSelected")) {
				imageA.unregisterVOI(voi);
			}
		}
	}

	/**
	 * Updates the VOI displaying which point (lattice or annotation) is currently selected when the selection changes.
	 */
	private void updateSelected() {
		if (pickedPoint != null) {
			if (showSelectedVOI == null) {
				final short id = (short) imageA.getVOIs().getUniqueID();
				showSelectedVOI = new VOI(id, "showSelected", VOI.POLYLINE, (float) Math.random());
				imageA.registerVOI(showSelectedVOI);
				showSelectedVOI.setColor(new Color(0, 255, 255));
			}
			if ((showSelected == null) || (showSelectedVOI.getCurves().size() == 0)) {
				showSelected = new VOIContour[3];
				showSelected[0] = new VOIContour(true);
				makeSelectionFrame(Vector3f.UNIT_X, Vector3f.UNIT_Y, pickedPoint, 4, showSelected[0]);
				showSelectedVOI.getCurves().add(showSelected[0]);
				showSelected[0].update(new ColorRGBA(0, 1, 1, 1));

				showSelected[1] = new VOIContour(true);
				makeSelectionFrame(Vector3f.UNIT_Z, Vector3f.UNIT_Y, pickedPoint, 4, showSelected[1]);
				showSelectedVOI.getCurves().add(showSelected[1]);
				showSelected[1].update(new ColorRGBA(0, 1, 1, 1));

				showSelected[2] = new VOIContour(true);
				makeSelectionFrame(Vector3f.UNIT_Z, Vector3f.UNIT_X, pickedPoint, 4, showSelected[2]);
				showSelectedVOI.getCurves().add(showSelected[2]);
				showSelected[2].update(new ColorRGBA(0, 1, 1, 1));

				showSelectedVOI.setColor(new Color(0, 255, 255));
			} else {
				for (int i = 0; i < showSelected.length; i++) {
					final Vector3f center = new Vector3f();
					for (int j = 0; j < showSelected[i].size(); j++) {
						center.add(showSelected[i].elementAt(j));
					}
					center.scale(1f / showSelected[i].size());
					final Vector3f diff = Vector3f.sub(pickedPoint, center);
					for (int j = 0; j < showSelected[i].size(); j++) {
						showSelected[i].elementAt(j).add(diff);
					}
				}
				showSelectedVOI.update();
			}
			if (imageA.isRegistered(showSelectedVOI) == -1) {
				imageA.registerVOI(showSelectedVOI);
			}

			if ( (left != null) && (pickedPoint == left.getCurves().lastElement().elementAt(0)) ) {
				if ( leftMarker != null )
				{
					leftMarker.getCurves().elementAt(0).elementAt(0).copy(pickedPoint);
					leftMarker.update();
				}
			}
			else if ( (right != null) && (pickedPoint == right.getCurves().lastElement().elementAt(0)) ) {
				if ( rightMarker != null )
				{
					rightMarker.getCurves().elementAt(0).elementAt(0).copy(pickedPoint);
					rightMarker.update();
				}
			}
		}
	}
	
	private int findPoint( VOI left, Vector3f pickedPoint) 
	{
		for ( int i = 0; i < left.getCurves().size(); i++ ) {
			if ( pickedPoint == left.getCurves().elementAt(i).elementAt(0) )
				return i;
		}
		return -1;
	}
	
	private void updateSeamCount() {
		int totalSeamCount = 0;
		int seamCount = 0;
		int nonSeamCount = 0;
		if (left.getCurves().size() == right.getCurves().size()) {
			totalSeamCount = 0;
			for ( int i = 0; i < left.getCurves().size(); i++ ) {
				VOIWormAnnotation leftAnnotation = (VOIWormAnnotation) left.getCurves().elementAt(i);
				if ( leftAnnotation.isSeamCell() ) {
					totalSeamCount++;
				}
			}
			for ( int i = 0; i < left.getCurves().size(); i++ ) {
				VOIWormAnnotation leftAnnotation = ((VOIWormAnnotation)left.getCurves().elementAt(i));
				VOIWormAnnotation rightAnnotation = ((VOIWormAnnotation)right.getCurves().elementAt(i));
				if ( leftAnnotation.isSeamCell() ) {
					seamCount++;
				}
				else {
					nonSeamCount++;
				}
				String name = "";
				if ( totalSeamCount <= 10 ) {
					// H0-H1-H2-V1-V2-V3-V4-V5-V6-T 
					if ( leftAnnotation.isSeamCell() ) {
						name = seamCount < 4 ? ("H" + (seamCount-1)) : (seamCount <= 9) ? ("V" + (seamCount - 3)) : "T";
					}
					else {
						name = "a" + (nonSeamCount-1);
					}
				}
				else if ( totalSeamCount > 10 ) {
					// H0-H1-H2-V1-V2-V3-V4-Q-V5-V6-T 
					if ( leftAnnotation.isSeamCell() ) {
						name = seamCount < 4 ? ("H" + (seamCount-1)) : (seamCount < 8) ? ("V" + (seamCount - 3)) : (seamCount == 8) ? "Q" : (seamCount < 11) ? ("V" + (seamCount - 4)) : "T";
					}
					else {
						name = "a" + (nonSeamCount-1);
					}
				}
				leftAnnotation.setText( name + "L" );
				leftAnnotation.updateText();
				leftAnnotation.update();
				
				rightAnnotation.setText( name + "R" );
				rightAnnotation.updateText();
				rightAnnotation.update();
			}
		}
	}
	
	public static boolean renameLatticeOnLoad( ModelImage image, VOIVector latticeVector)
	{
		if ( latticeVector == null ) return false;
		if ( latticeVector.size() < 2 ) return false;
		
		VOI left = latticeVector.elementAt(0);
		VOI right = latticeVector.elementAt(1);
		if ( left == null || right == null ) return false;
		
		// check if lattice is already named, if so return as is:
		for ( int i = 0; i < left.getCurves().size(); i++ ) {
			VOIWormAnnotation text = (VOIWormAnnotation) left.getCurves().elementAt(i);
			if ( text.getText().contains("H") || text.getText().contains("V") || text.getText().contains("T") ) {
				return false;
			}
			text = (VOIWormAnnotation) right.getCurves().elementAt(i);
			if ( text.getText().contains("H") || text.getText().contains("V") || text.getText().contains("T") ) {
				return false;
			}
		}

		boolean rename = false;
		float[] pairSort = new float[left.getCurves().size()];
		// decide which 10 points are seam cells by taking max pair values
		for ( int i = 0; i < left.getCurves().size() -1; i++ )
		{
			VOIWormAnnotation text = (VOIWormAnnotation) left.getCurves().elementAt(i);
			int x = (int) text.elementAt(0).X;
			int y = (int) text.elementAt(0).Y;
			int z = (int) text.elementAt(0).Z;
			float value;
			if ( image.isColorImage() ) 
			{
				value = image.getFloatC(x,y,z,2);
			}
			else
			{
				value = image.getFloat(x,y,z);
			}

			text = (VOIWormAnnotation) right.getCurves().elementAt(i);
			x = (int) text.elementAt(0).X;
			y = (int) text.elementAt(0).Y;
			z = (int) text.elementAt(0).Z;
			if ( image.isColorImage() ) 
			{
				value += image.getFloatC(x,y,z,2);
			}
			else
			{
				value += image.getFloat(x,y,z);
			}
			pairSort[i] = value;
		}
		Arrays.sort(pairSort);

		int pairCount = 0;
		int extraCount = 0;
		for ( int i = 0; i < left.getCurves().size(); i++ )
		{
			VOIWormAnnotation text = (VOIWormAnnotation) left.getCurves().elementAt(i);
			int x = (int) text.elementAt(0).X;
			int y = (int) text.elementAt(0).Y;
			int z = (int) text.elementAt(0).Z;
			float value;
			if ( image.isColorImage() ) 
			{
				value = image.getFloatC(x,y,z,2);
			}
			else
			{
				value = image.getFloat(x,y,z);
			}
			
			text = (VOIWormAnnotation) right.getCurves().elementAt(i);
			x = (int) text.elementAt(0).X;
			y = (int) text.elementAt(0).Y;
			z = (int) text.elementAt(0).Z;
			if ( image.isColorImage() ) 
			{
				value += image.getFloatC(x,y,z,2);
			}
			else
			{
				value += image.getFloat(x,y,z);
			}

			boolean isSeamPair = left.getCurves().size() <= 10;
			if ( !isSeamPair )
			{
				// check if this pair has a high enough value to be added to the list of seam cells:
				for ( int j = 0; j < Math.min(10, pairSort.length); j++ )
				{
					if ( value >= pairSort[(pairSort.length -1) - j] )
					{
//						System.err.println( value + "  " + pairSort[(pairSort.length -1) - j]);
						isSeamPair = true;
						break;
					}
				}
			}
			if ( isSeamPair )
			{
				rename = true;
				String name = pairCount < 3 ? ("H" + pairCount) : (pairCount < 9) ? ("V" + (pairCount - 2)) : "T";
				pairCount++;

				// left seam cell:
				text = (VOIWormAnnotation) left.getCurves().elementAt(i);
				text.setText( name + "L" );
				text.setSeamCell(true);

				// right seam cell:
				text = (VOIWormAnnotation) right.getCurves().elementAt(i);
				text.setText( name + "R" );
				text.setSeamCell(true);
			}
			else
			{
				String name = "a" + extraCount++;

				// left seam cell:
				text = (VOIWormAnnotation) left.getCurves().elementAt(i);
				text.setText( name + "L" );

				// right seam cell:
				text = (VOIWormAnnotation) right.getCurves().elementAt(i);
				text.setText( name + "R" );
			}
		}
		return rename;
	}

	public static void saveLattice(final String directory, final String fileName, VOIVector latticeVector )
	{
		saveLattice(directory, fileName, "lattice.csv", latticeVector);
	}
	
	public static void saveLattice(final String directory, final String fileName, final String latticeFileName, VOIVector latticeVector )
	{
		if ( (latticeVector == null) || (latticeVector.size() < 2) ) return;
		
		VOI left = latticeVector.elementAt(0);
		VOI right = latticeVector.elementAt(1);
		
		if ( left == null || right == null )
		{
			return;
		}
		if ( left.getCurves().size() != right.getCurves().size() )
		{
			return;
		}
		if (fileName != null)
		{
			final String voiDir = new String(directory + fileName + File.separator);

			VOI latticePoints = new VOI( (short)0, "lattice", VOI.ANNOTATION, 0);
			for ( int j = 0; j < left.getCurves().size(); j++ )
			{
				latticePoints.getCurves().add(new VOIWormAnnotation((VOIWormAnnotation)left.getCurves().elementAt(j)));
				latticePoints.getCurves().add(new VOIWormAnnotation((VOIWormAnnotation)right.getCurves().elementAt(j)));
			}
			if ( latticePoints.getCurves().size() == 0 ) {
//				System.err.println( "saveLattice " + latticePoints.getCurves().size() );
				return;
			}
			LatticeModel.saveAnnotationsAsCSV(voiDir + File.separator, latticeFileName, latticePoints);
			
			// save seam-cells derived from lattice:
			String voiSeamDir = directory + "seam_cell_final" + File.separator;
			latticePoints.getCurves().clear();
			for ( int j = 0; j < left.getCurves().size(); j++ )
			{
				VOIWormAnnotation leftAnnotation = (VOIWormAnnotation) left.getCurves().elementAt(j);
				VOIWormAnnotation rightAnnotation = (VOIWormAnnotation) right.getCurves().elementAt(j);
				if ( leftAnnotation.isSeamCell() || rightAnnotation.isSeamCell() ) {
					latticePoints.getCurves().add(leftAnnotation);
					latticePoints.getCurves().add(rightAnnotation);
				}
			}
			LatticeModel.saveAnnotationsAsCSV(voiSeamDir, "seam_cells.csv", latticePoints);
		}
	}
	

	private static VOI annotationChanged( VOI annotationsNew, VOI annotationOld ) {
		VOI annotationsChangeList = new VOI( (short)0, "changed annotations", VOI.ANNOTATION, 0 );
//    	System.err.println("annotationChanged");

		for ( int i = 0; i < annotationsNew.getCurves().size(); i++ ) {
        	VOIWormAnnotation annotation = (VOIWormAnnotation) annotationsNew.getCurves().elementAt(i);
//        	System.err.println("  " + annotation.getText());
        	// find match by name:
        	boolean found = false;
    		for ( int j = 0; j < annotationOld.getCurves().size(); j++ ) {
            	VOIWormAnnotation orig = (VOIWormAnnotation) annotationOld.getCurves().elementAt(j);
            	if ( annotation.getText().equals(orig.getText()) ) {

                	Vector3f ptO = new Vector3f(annotation.elementAt(0));
                	ptO.X = Math.round(ptO.X);
                	ptO.Y = Math.round(ptO.Y);
                	ptO.Z = Math.round(ptO.Z);
                	
                	Vector3f pt = new Vector3f(orig.elementAt(0));
                	pt.X = Math.round(pt.X);
                	pt.Y = Math.round(pt.Y);
                	pt.Z = Math.round(pt.Z);
                	
                	if ( !pt.equals(ptO) ) {
                		// point changed position - add to change list: 
                		annotationsChangeList.getCurves().add(annotation);
//                		System.err.println("     " + "moved annotation " + i + "  " + annotation.getText() + "   " + pt + "   " + ptO );
                	}
            		found = true;
            		break;
            	}
    		}
    		if ( !found ) {
        		// new point - add to change list: 
        		annotationsChangeList.getCurves().add(annotation);
//        		System.err.println("     " + "new annotation " + annotation.getText() );
    		}
		}
		
		return annotationsChangeList;
	}
	
	private boolean latticeChanged() {
		
		VOIVector finalLattice = readLatticeCSV(sharedOutputDir + File.separator + WormData.editLatticeOutput + File.separator + "lattice.csv");
        if ( finalLattice == null ) 
        	return true;
		if ( finalLattice.size() < 2 ) 
        	return true;

		VOI leftOrig = (VOI) finalLattice.elementAt(0);
		VOI rightOrig = (VOI) finalLattice.elementAt(1);
		
		if ( leftOrig.getCurves().size() != left.getCurves().size() || rightOrig.getCurves().size() != right.getCurves().size() ) 
			return true;
		
		for ( int i = 0; i < leftOrig.getCurves().size(); i++ ) {
        	VOIWormAnnotation leftPtOrig = (VOIWormAnnotation) leftOrig.getCurves().elementAt(i);
        	VOIWormAnnotation leftPt = (VOIWormAnnotation) left.getCurves().elementAt(i);
        	
        	if ( !leftPtOrig.getText().equals(leftPt.getText()) ) return true;

        	Vector3f ptO = new Vector3f(leftPtOrig.elementAt(0));
        	ptO.X = Math.round(ptO.X);
        	ptO.Y = Math.round(ptO.Y);
        	ptO.Z = Math.round(ptO.Z);
        	
        	Vector3f pt = new Vector3f(leftPt.elementAt(0));
        	pt.X = Math.round(pt.X);
        	pt.Y = Math.round(pt.Y);
        	pt.Z = Math.round(pt.Z);
        	
        	if ( !pt.equals(ptO) ) 
        		return true;

        	VOIWormAnnotation rightPtOrig = (VOIWormAnnotation) rightOrig.getCurves().elementAt(i);
        	VOIWormAnnotation rightPt = (VOIWormAnnotation) right.getCurves().elementAt(i);
        	
        	if ( !rightPtOrig.getText().equals(rightPt.getText()) ) return true;

        	ptO = new Vector3f(rightPtOrig.elementAt(0));
        	ptO.X = Math.round(ptO.X);
        	ptO.Y = Math.round(ptO.Y);
        	ptO.Z = Math.round(ptO.Z);
        	
        	pt = new Vector3f(rightPt.elementAt(0));
        	pt.X = Math.round(pt.X);
        	pt.Y = Math.round(pt.Y);
        	pt.Z = Math.round(pt.Z);
        	
        	if ( !pt.equals(ptO) ) 
        		return true;
        }
		return false;
	}
	
	private void testLatticeMeshIntersection() {
        if ( currentMesh == null ) return;
        if ( currentMesh.VBuffer == null ) return;
		
		Vector3f[] directions = new Vector3f[5];
		
		for ( int i = 0; i < left.getCurves().size(); i++ ) {
			Vector3f pos = left.getCurves().elementAt(i).elementAt(0);
			
			directions[0] = new Vector3f( (float)Math.random(), (float)Math.random(), (float)Math.random() );
			directions[1] = new Vector3f( -(float)Math.random(), (float)Math.random(), (float)Math.random() );
			directions[2] = new Vector3f( (float)Math.random(), -(float)Math.random(), (float)Math.random() );
			directions[3] = new Vector3f( (float)Math.random(), (float)Math.random(), -(float)Math.random() );
			directions[4] = new Vector3f( -(float)Math.random(), (float)Math.random(), -(float)Math.random() );
			Line3f[] akLines = new Line3f[directions.length];
			for ( int j = 0; j < directions.length; j++ )
			{
				directions[j].normalize();
				akLines[j] = new Line3f( pos, directions[j]);
			}
			
			boolean intersection = false;
			if ( VolumeSurface.testIntersections( currentMesh, pos, akLines ) )
			{
				intersection = true;
			}
			
			pos = right.getCurves().elementAt(i).elementAt(0);
			if ( VolumeSurface.testIntersections( currentMesh, pos, akLines ) )
			{
				intersection = true;
			}
			if ( intersection ) {
				displayContours[i].getCurves().elementAt(0).update(new ColorRGBA(1,0,0,1));
			}
			else {
				displayContours[i].getCurves().elementAt(0).update(new ColorRGBA(1,1,1,1));
			}
		}
	}
	



	private ModelImage getInsideMeshImage(BitSet surfaceMask) 
	{		
		ModelImage surfaceMaskImage = new ModelImage(ModelStorageBase.FLOAT, imageA.getExtents(), JDialogBase.makeImageName(imageA.getImageFileName(),  "_interior"));
		JDialogBase.updateFileInfo(imageA, surfaceMaskImage);

		int dimX = imageA.getExtents().length > 0 ? imageA.getExtents()[0] : 1;
		int dimY = imageA.getExtents().length > 1 ? imageA.getExtents()[1] : 1;		
		int dimZ = imageA.getExtents().length > 2 ? imageA.getExtents()[2] : 1;		
		for ( int z = 0; z < dimZ; z++ ) {
			for ( int y = 0; y < dimY; y++ ) {
				for ( int x = 0; x < dimX; x++ ) {
					int index = x + (dimX * (y + (dimY * z)));
					if ( surfaceMask.get(index) ) {
						surfaceMaskImage.set(x, y, z, 1);
						surfaceMaskImage.setMax(1);
					}
				}
			}
		}

		surfaceMaskImage.setMin(0);

		VOIContour centerCurve = getCenter();
		Vector3f pt = centerCurve.elementAt( centerCurve.size() / 2 );

		BitSet seedPaintBitmap = null;
		ModelImage volMaskImage = null;
		try {
			AlgorithmRegionGrow regionGrowAlgo = new AlgorithmRegionGrow(surfaceMaskImage, 1.0f, 1.0f);

			regionGrowAlgo.setRunningInSeparateThread(false);
			CubeBounds regionGrowBounds= new CubeBounds(dimX, 0, dimY, 0, dimZ, 0);
			seedPaintBitmap = new BitSet( dimX * dimY * dimZ );

			int count = regionGrowAlgo.regionGrow3D(seedPaintBitmap, new Point3D((int)pt.X, (int)pt.Y, (int)pt.Z), -1,
					false, false, null, 0, 0, -1, -1,
					false, 0, regionGrowBounds);

			volMaskImage = new ModelImage(ModelStorageBase.ARGB_FLOAT, imageA.getExtents(), JDialogBase.makeImageName(imageA.getImageFileName(),  "_interior"));
			JDialogBase.updateFileInfo(imageA, volMaskImage);

			for ( int z = 0; z < dimZ; z++ ) {
				for ( int y = 0; y < dimY; y++ ) {
					for ( int x = 0; x < dimX; x++ ) {
						int index = x + (dimX * (y + (dimY * z)));
						if ( seedPaintBitmap.get(index) ) {
							volMaskImage.setC(x, y, z, 2, 1);
							volMaskImage.setMaxG(1);
							volMaskImage.setMax(1);
						}
						if ( surfaceMask.get(index) ) {
							volMaskImage.setC(x, y, z, 1, 1);
							volMaskImage.setMaxR(1);
							volMaskImage.setMax(1);
						}
					}
				}
			}

			volMaskImage.setMin(0);
			seedPaintBitmap = null;
			regionGrowAlgo = null;


		} catch (final OutOfMemoryError error) {
			System.gc();
			MipavUtil.displayError("Out of memory: ComponentEditImage.regionGrow");
		}

		if ( surfaceMaskImage != null ) {
			surfaceMaskImage.disposeLocal(false);
			surfaceMaskImage = null;
		}

		return volMaskImage;
	}

	

	private ModelImage getInsideMeshImage(TriMesh mesh) 
	{		
		// save the surface and interior images:
		SurfaceState surface = new SurfaceState( mesh, mesh.GetName() );
		VolumeImage volImage = new VolumeImage(false, imageA, "", null, 0);
		VolumeSurface volumeSurface = new VolumeSurface(volImage,
				null, new Vector3f(), volImage.GetScaleX(), volImage.GetScaleY(), volImage.GetScaleZ(), surface, true);
		BitSet surfaceMask = volumeSurface.computeSurfaceMask();
		return getInsideMeshImage(surfaceMask);
	}
	/**
	 * Returns a blurred image of the input image.
	 * 
	 * @param image
	 * @param sigma
	 * @return
	 */
	private static ModelImage blur(final ModelImage image, final int sigma) {
		String imageName = image.getImageName();
		if (imageName.contains("_clone")) {
			imageName = imageName.replaceAll("_clone", "");
		}
		imageName = imageName + "_gblur";

		float[] sigmas = new float[] {sigma, sigma};
		if ( image.getNDims() == 3 )
		{
			sigmas = new float[] {sigma, sigma, sigma * getCorrectionFactor(image)};
		}
		OpenCLAlgorithmGaussianBlur blurAlgo;

		final ModelImage resultImage = new ModelImage(image.getType(), image.getExtents(), imageName);
		JDialogBase.updateFileInfo(image, resultImage);
		blurAlgo = new OpenCLAlgorithmGaussianBlur(resultImage, image, sigmas, true, true, false);

		blurAlgo.setRed(true);
		blurAlgo.setGreen(true);
		blurAlgo.setBlue(true);
		blurAlgo.run();

		return blurAlgo.getDestImage();
	}

	/**
	 * Returns the amount of correction which should be applied to the z-direction sigma (assuming that correction is
	 * requested).
	 * 
	 * @return the amount to multiply the z-sigma by to correct for resolution differences
	 */
	private static float getCorrectionFactor(final ModelImage image) {
		final int index = image.getExtents()[2] / 2;
		final float xRes = image.getFileInfo(index).getResolutions()[0];
		final float zRes = image.getFileInfo(index).getResolutions()[2];

		return xRes / zRes;
	}

	
	private static int fill(final BitSet surfaceMask, final Vector<Vector3f> seedList, BitSet visited, Vector3f min, Vector3f max, int dimX, int dimY, int dimZ) {

		int count = 0;
		int size = dimX*dimY*dimZ;
		while (seedList.size() > 0) {
			final Vector3f seed = seedList.lastElement();
			seedList.remove(seedList.lastElement());

			final int z = Math.round(seed.Z);
			final int y = Math.round(seed.Y);
			final int x = Math.round(seed.X);
			
			if ( x < min.X || y < min.Y || z < min.Z || x > max.X || y > max.Y || z > max.Z ) continue;
			
			int index = z*dimY*dimX + y*dimX + x;			
			

			if ( (count % 1000) == 0 )
				System.err.println( seedList.size() + "   " + visited.cardinality() + "   (" + x + "  " + y + "  " + z + ")   " + size );
			
			if ( visited.get(index) )
			{
				continue;
			}
			visited.set(index);
			count++;
			
			boolean surfaceTouched = false;
			for (int z1 = Math.max(0, z - 1); z1 <= Math.min(dimZ - 1, z + 1); z1++)
			{
				for (int y1 = Math.max(0, y - 1); y1 <= Math.min(dimY - 1, y + 1); y1++)
				{
					for (int x1 = Math.max(0, x - 1); x1 <= Math.min(dimX - 1, x + 1); x1++)
					{
						index = z1*dimY*dimX + y1*dimX + x1;
						if ( surfaceMask.get(index) ) {
							visited.set(index);
							surfaceTouched = true;
						}
					}
				}
			}
			
			if ( surfaceTouched ) continue;

			for (int z1 = Math.max(0, z - 1); z1 <= Math.min(dimZ - 1, z + 1); z1++)
			{
				for (int y1 = Math.max(0, y - 1); y1 <= Math.min(dimY - 1, y + 1); y1++)
				{
					for (int x1 = Math.max(0, x - 1); x1 <= Math.min(dimX - 1, x + 1); x1++)
					{
						if ( ! ( (x == x1) && (y == y1) && (z == z1))) {
							index = z1*dimY*dimX + y1*dimX + x1;
							if ( surfaceMask.get(index) ) {
								visited.set(index);
							}
							else if ( !visited.get(index) )
							{
								seedList.add( new Vector3f(x1,y1,z1) );
							}
						}
					}
				}
			}							
		}
		return count;
	}

	public void increaseCrossSectionSamples() {
		if(crossSectionSamples < 32) {
			crossSectionSamples *= 2;
			crossSectionSamples = Math.min(crossSectionSamples, numEllipsePts);
		}
		colorSectionMarkerByCrossSectionSamples();
	}
	
	public void decreaseCrossSectionSamples() {
		if(crossSectionSamples > 4) {
			crossSectionSamples /= 2;
			crossSectionSamples = Math.max(crossSectionSamples, 4);
		}
		colorSectionMarkerByCrossSectionSamples();
	}
	
	public void toggleUpdateCrossSectionOnDrag() {
		updateCrossSectionOnDrag = !updateCrossSectionOnDrag;
	}

	private void selectNeighborInCrossSection(int newSelectedSectionIndex2) {
		if (selectedSectionIndex == -1) {
			selectedSectionIndex = 0;
		}
		VOIBase section = displayContours[selectedSectionIndex].getCurves().elementAt(0);
		Vector3f sectionPoint = section.elementAt(newSelectedSectionIndex2);
		pickedPoint = new Vector3f();
		pickedPoint.copy(sectionPoint);
		
		//update transverse section colors swap gray / yellow
		if (selectedSectionIndex2 != -1) {
			displayContours[selectedSectionIndex2 + latticeSlices.length].getCurves().elementAt(0).update(new ColorRGBA(0.5f, 0.5f, 0.5f, 0.75f));
		}
		displayContours[newSelectedSectionIndex2 + latticeSlices.length].getCurves().elementAt(0).update(new ColorRGBA(1.0f, 1.0f, 0f, 1.0f));
		
		selectedSectionIndex2 = newSelectedSectionIndex2;
		
		final short id = (short) imageA.getVOIs().getUniqueID();
		if(sectionMarker == null) {
			sectionMarker = new VOI(id, "sectionMarker", VOI.POINT, (float) Math.random());
			this.imageA.registerVOI(sectionMarker);
		}
		sectionMarker.getCurves().clear();
		sectionMarker.importPoint(pickedPoint);
		colorSectionMarkerByCrossSectionSamples();
				
		imageA.notifyImageDisplayListeners();
	}
	
	public void selectLeftNeighborInCrossSection() {
		int newSelectedSectionIndex2 = (selectedSectionIndex2 - 1) % numEllipsePts;
		if (newSelectedSectionIndex2 < 0) {
			newSelectedSectionIndex2 += numEllipsePts;
		}
		selectNeighborInCrossSection(newSelectedSectionIndex2);
	}

	public void selectRightNeighborInCrossSection() {
		int newSelectedSectionIndex2 = (selectedSectionIndex2 + 1) % numEllipsePts;
		selectNeighborInCrossSection(newSelectedSectionIndex2);
	}
	
	private void selectCrossSection(int newSelectedSectionIndex) {
		if (selectedSectionIndex2 == -1) {
			selectedSectionIndex2 = 0;
		}
		VOIBase section = displayContours[newSelectedSectionIndex].getCurves().elementAt(0);
		Vector3f sectionPoint = section.elementAt(selectedSectionIndex2);
		pickedPoint = new Vector3f();
		pickedPoint.copy(sectionPoint);
		
		if (selectedSectionIndex != -1) {
			displayContours[selectedSectionIndex].getCurves().elementAt(0).update(new ColorRGBA(0.5f, 0.5f, 0.5f, 0.75f));
		}
		displayContours[newSelectedSectionIndex].getCurves().elementAt(0).update(new ColorRGBA(1.0f, 1.0f, 0f, 1.0f));
		
		selectedSectionIndex = newSelectedSectionIndex;
		
		final short id = (short) imageA.getVOIs().getUniqueID();
		if(sectionMarker == null) {
			sectionMarker = new VOI(id, "sectionMarker", VOI.POINT, (float) Math.random());
			this.imageA.registerVOI(sectionMarker);
		}
		sectionMarker.getCurves().clear();
		sectionMarker.importPoint(pickedPoint);
		colorSectionMarkerByCrossSectionSamples();
				
		imageA.notifyImageDisplayListeners();
	}

	public void selectNextCrossSection() {
		int newSelectedSectionIndex = (selectedSectionIndex + 1) % latticeSlices.length;
		selectCrossSection(newSelectedSectionIndex);
	}

	public void selectPrevCrossSection() {
		int newSelectedSectionIndex = (selectedSectionIndex - 1) % latticeSlices.length;;
		if (newSelectedSectionIndex < 0) {
			newSelectedSectionIndex += latticeSlices.length;
		}
		selectCrossSection(newSelectedSectionIndex);
	}
	
	private void colorSectionMarkerByCrossSectionSamples() {
		if(sectionMarker != null) {
			float red = 1.0f - ((float) crossSectionSamples) / ((float) numEllipsePts);
			sectionMarker.getCurves().elementAt(0).update(new ColorRGBA(red, 0.0f, 1.0f, 1.0f));
			sectionMarker.update();
		}
	}

}
