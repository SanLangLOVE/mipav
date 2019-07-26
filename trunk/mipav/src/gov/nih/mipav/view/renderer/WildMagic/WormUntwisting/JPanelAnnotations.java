package gov.nih.mipav.view.renderer.WildMagic.WormUntwisting;


import gov.nih.mipav.model.algorithms.OpenCLAlgorithmBase;
import gov.nih.mipav.model.algorithms.filters.OpenCL.filters.OpenCLAlgorithmMarchingCubes;

//MIPAV is freely available from http://mipav.cit.nih.gov

//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
//EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
//OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
//NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
//HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
//WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
//FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE 
//OR OTHER DEALINGS IN THE SOFTWARE. 

/*****************************************************************
 ******************************************************************

The MIPAV application is intended for research use only.
This application has NOT been approved for ANY diagnostic use 
by the Food and Drug Administration. There is currently no 
approval process pending. 

This software may NOT be used for diagnostic purposes.

 ******************************************************************
 ******************************************************************/
import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.model.structures.ModelLUT;
import gov.nih.mipav.model.structures.ModelStorageBase;
import gov.nih.mipav.model.structures.VOI;
import gov.nih.mipav.util.MipavCoordinateSystems;
import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.Preferences;
import gov.nih.mipav.view.ViewJFrameImage;
import gov.nih.mipav.view.ViewUserInterface;
import gov.nih.mipav.view.dialogs.JDialogBase;
import gov.nih.mipav.view.renderer.WildMagic.VolumeTriPlanarRender;
import gov.nih.mipav.view.renderer.WildMagic.Interface.JInterfaceBase;
import gov.nih.mipav.view.renderer.WildMagic.Interface.JPanelLights_WM;
import gov.nih.mipav.view.renderer.WildMagic.Interface.SurfaceExtractorCubes;
import gov.nih.mipav.view.renderer.WildMagic.Interface.SurfaceState;
import gov.nih.mipav.view.renderer.WildMagic.Render.VolumeImage;
import gov.nih.mipav.view.renderer.WildMagic.VOI.VOILatticeManagerInterface;
import gov.nih.mipav.view.renderer.WildMagic.WormUntwisting.AnnotationListener;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.BitSet;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;

import WildMagic.LibFoundation.Mathematics.ColorRGB;
import WildMagic.LibFoundation.Mathematics.Vector3f;
import WildMagic.LibGraphics.SceneGraph.IndexBuffer;
import WildMagic.LibGraphics.SceneGraph.TriMesh;
import WildMagic.LibGraphics.SceneGraph.VertexBuffer;


public class JPanelAnnotations extends JInterfaceBase implements ActionListener, AnnotationListener, TableModelListener, ListSelectionListener, KeyListener, ChangeListener, MouseListener {

	private static final long serialVersionUID = -9056581285643263551L;

	private VolumeImage imageA;
	

	// on 'start' the images are loaded and the VolumeTriPlanarInterface is created:
	private VOILatticeManagerInterface voiManager;
	private VolumeTriPlanarRender volumeRenderer = null;
	// annotation panel displayed in the VolumeTriPlanarInterface:
	private JPanel annotationPanel;
	// turns on/off displaying individual annotations
	private JCheckBox volumeClip;
	private JSlider volumeRadius;
	private JCheckBox displayLabel;
	private JCheckBox displayGroupLabel;
	// table user-interface for editing the positions of the annotations:
	private ListSelectionModel annotationList;
	private JTable annotationTable;
	private DefaultTableModel annotationTableModel;
	private boolean useLatticeMarkers = false;

	// table user-interface for editing the positions of the annotations:
	private ListSelectionModel annotationGroupList;
	private JTable annotationGroupTable;
	private DefaultTableModel annotationGroupTableModel;
	private String selectedPrefix = null;
	
	private int displayChannel = 1;
	private JTextField thresholdMin;
	private JTextField thresholdMax;
	private ModelImage mask;
	private TriMesh mesh;

	public JPanelAnnotations( VOILatticeManagerInterface voiInterface, VolumeTriPlanarRender renderer, VolumeImage imageA ) {
		voiManager = voiInterface;
		volumeRenderer = renderer;
		this.imageA = imageA;
		voiManager.addAnnotationListener(this);
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event)
	{
		String command = event.getActionCommand();
		Object source = event.getSource();
		if ( command.equals("displayAll") )
		{
			// display all annotations in the list:
			VOI annotations = voiManager.getAnnotations();
			if ( (annotations != null) ) {
				if ( annotations.getCurves().size() > 0 ) {
					for ( int i = 0; i < annotations.getCurves().size(); i++ )
					{
						VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(i);
						text.display(true);
					}
				}
			}
			displayLabel.setSelected(true);
		}
		else if ( command.equals("displayNone") )
		{
			// display none of the annotations in the list:
			VOI annotations = voiManager.getAnnotations();
			if ( (annotations != null) ) {
				if ( annotations.getCurves().size() > 0 ) {
					for ( int i = 0; i < annotations.getCurves().size(); i++ )
					{
						VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(i);
						text.display(false);
					}
				}
			}
			displayLabel.setSelected(false);
		}
		else if ( command.equals("openAnnotations" ) ) {

            // get the voi directory
            String fileName = null;
            String directory = null;
            String voiDir = null;

            final JFileChooser chooser = new JFileChooser();

            if (ViewUserInterface.getReference().getDefaultDirectory() != null) {
                chooser.setCurrentDirectory(new File(ViewUserInterface.getReference().getDefaultDirectory()));
            } else {
                chooser.setCurrentDirectory(new File(System.getProperties().getProperty("user.dir")));
            }

            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            final int returnVal = chooser.showOpenDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                fileName = chooser.getSelectedFile().getName();
                directory = String.valueOf(chooser.getCurrentDirectory()) + File.separatorChar;
                Preferences.setProperty(Preferences.PREF_IMAGE_DIR, chooser.getCurrentDirectory().toString());
            }

            if (fileName != null) {
                voiDir = new String(directory + fileName);
                System.err.println("Opening csv file " + voiDir );
                VOI annotations = LatticeModel.readAnnotationsCSV(voiDir);
                if ( annotations != null && annotations.getCurves().size() > 0 ) {
                	for ( int i = 0; i < annotations.getCurves().size(); i++) {

						VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(i);
						text.retwist(previewMode);
                	}
                	voiManager.setAnnotations(annotations);
                }
            }
		}
		else if ( command.equals("thresholdSegmentation") ) {

			VOI annotations = voiManager.getAnnotations();
			if ( (annotations != null) ) {
				if ( annotations.getCurves().size() > 0 ) {
					Vector<Vector3f> seedList = new Vector<Vector3f>();
					float maxValue = -Float.MAX_VALUE;
					float minValue =  Float.MAX_VALUE;
					for ( int i = 0; i < annotations.getCurves().size(); i++ )
					{
						VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(i);
						if ( text.isSelected() ) {
							// read positions and values and set up segmentation:
							Vector3f pos = text.elementAt(0);
							seedList.add(new Vector3f(pos));

							float value = imageA.GetImage().getFloat( (int)pos.X, (int)pos.Y, (int)pos.Z );
							if ( value > maxValue ) maxValue = value;
							if ( value < minValue ) minValue = value;						
						}
					}
					if ( seedList.size() == 0 ) {
						MipavUtil.displayInfo( "No annotations selected, please select annotations for segmentation." );
						return;
					}
					BitSet visited = new BitSet(imageA.GetImage().getVolumeSize());
					if ( mask != null ) {
						mask.disposeLocal(false);
						mask = null;
					}
					mask = new ModelImage(ModelStorageBase.FLOAT, imageA.GetImage().getExtents(), "NerveRing" );
					JDialogBase.updateFileInfo(imageA.GetImage(), mask);

					float minThreshold = minValue;
					try {
						minThreshold = Float.valueOf(thresholdMin.getText().trim());
					} catch(NumberFormatException e) {}
					float maxThreshold = (float)imageA.GetImage().getMax();
					try {
						maxThreshold = Float.valueOf(thresholdMax.getText().trim());
					} catch(NumberFormatException e) {}
					
					int count = WormSegmentation.fill(imageA.GetImage(), minThreshold, maxThreshold, seedList, visited, mask);

					
					System.err.println("Segmentation results " + imageA.GetImage().getImageName() + "  " + count );

					// remove previous segmentation:
					volumeRenderer.removeSurface("nerveRing");
					volumeRenderer.displaySurface(false);	
					
					if ( count > 0 ) {
				    	if ( (Preferences.isGpuCompEnabled() && OpenCLAlgorithmBase.isOCLAvailable()) )
				    	{
				    		OpenCLAlgorithmMarchingCubes cubesAlg = new OpenCLAlgorithmMarchingCubes(mask, 0, false,
				    				false, false, 0, null );
				    		cubesAlg.setRunningInSeparateThread(false);
				    		cubesAlg.run();
				    		mesh = cubesAlg.getMesh();
				    		if ( mesh != null ) {
				    			System.err.println("extracted mesh " + mesh.VBuffer.GetVertexQuantity() );
				    			SurfaceState surface = new SurfaceState( new TriMesh(new VertexBuffer(mesh.VBuffer), new IndexBuffer(mesh.IBuffer)), "segmentation" );
				    			volumeRenderer.addSurface( surface, true );
				    			volumeRenderer.displaySurface(true);
				    			JPanelLights_WM lightsPanel = new JPanelLights_WM(volumeRenderer);
				    			lightsPanel.enableLight(0, true);
				    			lightsPanel.enableLight(1, true);
				    			volumeRenderer.updateLighting(lightsPanel.getAllLights());
				    		}
				    	}
//						ModelImage origImage = imageA.GetImage();	
					}
				}
			}
		}
		else if ( command.equals("deleteSegmentation") ) {
			// remove segmentation:
			volumeRenderer.removeSurface("segmentation");
			volumeRenderer.displaySurface(false);	
		}
		else if ( command.equals("saveSegmentation") ) {
			if ( mask != null ) {
				LatticeModel.saveImage( imageA.GetImage(), mask, "_segmentation" );
			}
			if ( mesh != null ) {
				LatticeModel.saveTriMesh( imageA.GetImage(), "segmentation", "", mesh );
			}
		}
		else if ( source == displayLabel )
		{	
			// find the selected annotation and turn it's display on/off:
			if ( voiManager != null )
			{
				VOI annotations = voiManager.getAnnotations();
				// selected row:
				int row = annotationTable.getSelectedRow();		        
				if ( (annotations != null) && (row >= 0) ) {
					if ( row < annotations.getCurves().size() ) {
						VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(row);
						text.display(((JCheckBox)source).isSelected());
					}
				}
			}
		}
		else if ( source == displayGroupLabel )
		{	
			// find the selected annotation and turn it's display on/off:
			if ( (voiManager != null) && (selectedPrefix != null) )
			{
				//				System.err.println("Display Group " + selectedPrefix );
				VOI annotations = voiManager.getAnnotations();
				for ( int i = 0; i < annotations.getCurves().size(); i++ ) {
					VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(i);
					String prefix = getPrefix(text.getText());
					if ( prefix.equals(selectedPrefix) ) {
						text.display(((JCheckBox)source).isSelected());
					}
				}
			}
		}
		else if ( source == volumeClip )
		{
			boolean clip = volumeClip.isSelected();
			//			System.err.println("Clip annotation " + clip );
			// find the selected annotation and turn it's display on/off:
			if ( voiManager != null )
			{
				VOI annotations = voiManager.getAnnotations();
				// turn off clipping for all rows:
				for ( int i = 0; i < annotations.getCurves().size(); i++ ) 
				{
					VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(i);
					text.getVolumeVOI().setVolumeClip(false);					
				}
				// clip radius:
				float value = volumeRadius.getValue();
				// selected row:
				int row = annotationTable.getSelectedRow();		        
				if ( (annotations != null) && (row >= 0) ) {
					if ( row < annotations.getCurves().size() ) {
						VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(row);
						text.getVolumeVOI().setVolumeClip(clip);
						text.getVolumeVOI().setVolumeClipRadius(value);
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * Called from the LatticeModel when any annotations are changed.
	 * Updates the annotation table with the current annotations.
	 */
	public void annotationChanged() {
//		System.err.println("annotationChanged");

		if ( voiManager != null )
		{
			// get current annotations and update table:
			VOI annotations = voiManager.getAnnotations();
			// remove table listener during updates:
			annotationTableModel.removeTableModelListener(this);
			annotationList.removeListSelectionListener(this);
			int numRows = annotationTableModel.getRowCount();
			for ( int i = numRows -1; i >= 0; i-- ) {
				annotationTableModel.removeRow(i);
			}		
			// remove table listener during updates:
			annotationGroupTableModel.removeTableModelListener(this);
			annotationGroupList.removeListSelectionListener(this);
			numRows = annotationGroupTableModel.getRowCount();
			for ( int i = numRows -1; i >= 0; i-- ) {
				annotationGroupTableModel.removeRow(i);
			}		
			if ( annotations != null ) {
				if ( annotations.getCurves().size() > 0 ) {
					int[] segments = new int[annotations.getCurves().size()];
					Vector<String> names = new Vector<String>();
					for ( int i = 0; i < annotations.getCurves().size(); i++ ) {
						VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(i);
						names.add(text.getText());
						Vector3f pos = text.elementAt(0);
						float value = imageA.GetImage().getFloat((int)pos.X, (int)pos.Y, (int)pos.Z);
						if ( imageA.GetImage().isColorImage() )
							value = imageA.GetImage().getFloatC((int)pos.X, (int)pos.Y, (int)pos.Z, displayChannel == 1 ? 1 : 2 );
						
						if ( useLatticeMarkers ) {
							int latticeSegment = text.getLatticeSegment();
							//							System.err.println(i + "  " + note);
							if ( latticeSegment != -1 ) {
								//								System.err.println(note + "   " + value );
								annotationTableModel.addRow( new Object[]{text.getText(), pos.X, pos.Y, pos.Z, latticeSegment, value } );
							}
							else {
								annotationTableModel.addRow( new Object[]{text.getText(), pos.X, pos.Y, pos.Z, "", value } );
								segments[i] = -1;
							}
						}
						else
						{
							annotationTableModel.addRow( new Object[]{text.getText(), pos.X, pos.Y, pos.Z, value } );
							segments[i] = -1;
						}
					}
					Vector<String> prefixList = new Vector<String>();
					String[] prefixes = new String[names.size()];
					for ( int i = 0; i < names.size(); i++ ) {
						String name = names.elementAt(i);
						String prefix = getPrefix(name);
						//						System.err.println( name + "  " + prefix + "  " + getPostfix(name) );
						prefixes[i] = prefix;
						if ( prefix.length() > 0 && !prefixList.contains(prefix) ) {
							prefixList.add(prefix);
							//							System.err.println(prefix);
						}
					}
					for ( int i = 0; i < prefixList.size(); i++ ) {
						String prefix = prefixList.elementAt(i);
						boolean segmentMatch = true;
						int segment = -1;
						for ( int j = 0; j < prefixes.length; j++ ) {
							if ( prefixes[j].equals(prefix) ) {
								if ( segments[j] == -1 ) {
									segmentMatch = false;
									break;
								}
								else if ( segment == -1 ) {
									segment = segments[j];
								}
								else if ( segment != segments[j] ) {
									segmentMatch = false;
									break;
								}
							}
						}
						if ( segmentMatch ) {
							annotationGroupTableModel.addRow( new Object[]{ prefix, segment } );
						}
						else {
							annotationGroupTableModel.addRow( new Object[]{ prefix } );
						}
					}
					//					System.err.println( selectedPrefix + "  " + prefixList.contains(selectedPrefix) );
					//					if ( !prefixList.contains(selectedPrefix) ) {
					//						annotationGroupTableModel.addRow( new Object[]{ selectedPrefix } );
					//					}
					

					annotationList.clearSelection();
					for ( int i = 0; i < annotations.getCurves().size(); i++ ) {
						VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(i);
						if ( text.isSelected() ) {
							annotationList.addSelectionInterval(i, i);
						}
					}
				}

			}
			// restore table listener:
			annotationTableModel.addTableModelListener(this);
			annotationList.addListSelectionListener(this);
			annotationGroupTableModel.addTableModelListener(this);
			annotationGroupList.addListSelectionListener(this);
		}		
		annotationPanel.validate();
	}

	private boolean previewMode = false;
	public void setPreviewMode( boolean preview ) 
	{
		previewMode = preview;
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.TableModelListener#tableChanged(javax.swing.event.TableModelEvent)
	 */
	public void tableChanged(TableModelEvent e) {
		System.err.println("tableChanged");
		// Track updates to the table and update the corresponding annotation.
		// The user can change the annotation name and position (x,y,z) with table edits.
		// Does not currently check type.
		int column = e.getColumn();
		boolean isChecked = false;
		boolean isClipped = false;
		if ( voiManager != null && (e.getSource() == annotationTableModel) )
		{
			int row = e.getFirstRow();
			VOI annotations = voiManager.getAnnotations();
			if ( (row >= 0) && (row < annotations.getCurves().size()) )
			{
				VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(row);
				if ( column == 0 )
				{
					text.setText( annotationTableModel.getValueAt(row, column).toString() );
					text.updateText();
				}
				else if ( column < 4 )
				{
					float value = Float.valueOf(annotationTableModel.getValueAt(row, column).toString());
					if ( value >= 0 ) {
						if ( column == 1 ) {
							text.elementAt(0).X = value;
							text.elementAt(1).X = value;
						}
						else if ( column == 2 ) {
							text.elementAt(0).Y = value;
							text.elementAt(1).Y = value;
						}
						else if ( column == 3 ) {
							text.elementAt(0).Z = value;
							text.elementAt(1).Z = value;
						}
						text.retwist(previewMode);
					}
				}
				else if ( annotationTableModel.getColumnName(column).equals("segment") )
				{
					try {
						int value = Integer.valueOf(annotationTableModel.getValueAt(row, column).toString());
						text.setLatticeSegment(value);
					} catch ( java.lang.NumberFormatException error ) {
						text.setLatticeSegment(-1);
					}
				}
				text.update();
				if ( text.getVolumeVOI() != null ) {
					isChecked = text.getVolumeVOI().GetDisplay();
					isClipped = text.getVolumeVOI().GetClipped();
				}
			}

			displayLabel.setSelected(isChecked);
			volumeClip.setSelected(isClipped);
		}
		if ( voiManager != null && (e.getSource() == annotationGroupTableModel) )
		{
			int row = annotationGroupTable.getSelectedRow();
			int segment = -1;
			String newPrefix = "";
			boolean nameChanged = false;
			boolean segmentChanged = false;
			if ( column == 0 ) {
				// new name:
				if ( annotationGroupTableModel.getValueAt(row, 0) != null ) {
					newPrefix = new String ( annotationGroupTableModel.getValueAt(row, 0).toString() );
					if ( selectedPrefix.equals("new row") ) 
					{
						selectedPrefix = new String(newPrefix);
						voiManager.setAnnotationPrefix( selectedPrefix );
					}
					else 
					{
						nameChanged = true;
					}
				}
			}
			else if ( column == 1 ) {
				// change the segment:
				try {
					segment = Integer.valueOf(annotationGroupTableModel.getValueAt(row, column).toString());
				} catch ( java.lang.NumberFormatException error ) {
					// value erased:
				}
				segmentChanged = true;
			}
			if ( nameChanged || segmentChanged ) 
			{
				VOI annotations = voiManager.getAnnotations();
				for ( int i = 0; i < annotations.getCurves().size(); i++ ) {
					VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(i);
					String prefix = getPrefix(text.getText());
					if ( segmentChanged && prefix.equals(selectedPrefix) ) {
						text.setLatticeSegment(segment);
					}
					if ( nameChanged && prefix.equals(selectedPrefix) ) {
						text.setText( newPrefix + getPostfix(text.getText() ) );
						text.updateText();
					}
				}
				if ( nameChanged ) {
					selectedPrefix = new String(newPrefix);
					voiManager.setAnnotationPrefix( selectedPrefix );
				}
				annotationChanged();
			}
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent e) {

		if ( e.getSource() == annotationList && e.getValueIsAdjusting() )
			return;

		updateTableSelection(e);

		if ( e.getSource() == annotationList ) {
			if ( annotationTable.getRowCount() > 0 ) {
				// Updates the displayLabel checkbox based on which row of the table is current:
				VOI annotations = voiManager.getAnnotations();
				int row = annotationTable.getSelectedRow();

				boolean isChecked = true;
				boolean isClipped = true;
				if ( (annotations != null) && (row >= 0) ) {
					if ( row < annotations.getCurves().size() ) {
						VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(row);
						if ( text.getVolumeVOI() != null )
						{
							isChecked = text.getVolumeVOI().GetDisplay();
							isClipped = text.getVolumeVOI().GetClipped();
						}
					}
				}
				displayLabel.setSelected(isChecked);
				volumeClip.setSelected(isClipped);
			}
		}
		else if ( e.getSource() == annotationGroupList ) {
			if ( annotationGroupTable.getRowCount() > 0 ) {
				int row = annotationGroupTable.getSelectedRow();
				if ( row != -1 ) {
					if ( annotationGroupTableModel.getValueAt(row, 0) != null ) {
						selectedPrefix = new String ( annotationGroupTableModel.getValueAt(row, 0).toString() );
						//						System.err.println("valueChanged " + row + "  " + selectedPrefix );
						voiManager.setAnnotationPrefix( selectedPrefix );
						boolean displayAll = true;
						VOI annotations = voiManager.getAnnotations();
						for ( int i = 0; i < annotations.getCurves().size(); i++ ) {
							VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(i);
							String prefix = getPrefix(text.getText());
							if ( prefix.equals(selectedPrefix) ) {
								if ( text.getVolumeVOI() != null )
								{
									displayAll &= text.getVolumeVOI().GetDisplay();
									if ( !displayAll ) {
										break;
									}
								}
							}
						}
						displayGroupLabel.setSelected(displayAll);						
					}
					else {
						selectedPrefix = "new row";
					}
				}
			}
		}

		imageA.GetImage().notifyImageDisplayListeners();
	}

	/**
	 * Creates the table that displays the annotation information.
	 * The user can edit the annotations directly in the table.
	 */
	private void buildAnnotationTable(boolean latticeMarkers) {
		if ( annotationTable == null )
		{
			annotationTableModel = new DefaultTableModel() {

			    @Override
			    public boolean isCellEditable(int row, int column) {
			    	String columnName = getColumnName(column);
			    	if ( columnName.equals("value") ) return false;
			       return true;
			    }
			};
			
			annotationTableModel.addColumn("Name");
			annotationTableModel.addColumn("x");
			annotationTableModel.addColumn("y");
			annotationTableModel.addColumn("z");
			if ( latticeMarkers ) {
				annotationTableModel.addColumn("lattice segment");
				useLatticeMarkers = true;
			}
			annotationTableModel.addColumn("value");
			
			annotationTableModel.addTableModelListener(this);

			annotationTable = new JTable(annotationTableModel);
			annotationTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			annotationTable.addKeyListener(this);
			annotationTable.addMouseListener(this);

			annotationTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			annotationTable.getColumn("Name").setMinWidth(100);
			annotationTable.getColumn("Name").setMaxWidth(100);
			annotationList = annotationTable.getSelectionModel();
			annotationList.addListSelectionListener(this);


			annotationGroupTableModel = new DefaultTableModel();
			annotationGroupTableModel.addColumn("Group Name");
			annotationGroupTableModel.addColumn("lattice segment");
			annotationGroupTableModel.addTableModelListener(this);

			annotationGroupTable = new JTable(annotationGroupTableModel);
			annotationGroupTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			annotationGroupTable.addKeyListener(this);

			annotationGroupTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			//			annotationGroupTable.getColumn("Group Name").setMinWidth(100);
			//			annotationGroupTable.getColumn("Group Name").setMaxWidth(100);
			//			annotationGroupTable.getColumn("lattice segment").setMinWidth(100);
			//			annotationGroupTable.getColumn("lattice segment").setMaxWidth(100);
			annotationGroupList = annotationGroupTable.getSelectionModel();
			annotationGroupList.addListSelectionListener(this);
		}
	}

	public JPanel getAnnotationsPanel() {
		return annotationPanel;
	}


    private static GridBagConstraints gbc = new GridBagConstraints();
    private static GridBagLayout gbLayout = new GridBagLayout();
    
    private static void initGB() {
    	gbc = new GridBagConstraints();
        gbLayout = new GridBagLayout();
    	gbc.gridx = 0;
    	gbc.gridy = 0;
    	gbc.weightx = 1;
    	gbc.weighty = 1;
    	gbc.gridheight = 1;
    	gbc.gridwidth = 1;
    	gbc.anchor = GridBagConstraints.WEST;
    	gbc.fill = GridBagConstraints.HORIZONTAL;
    }
	
	/**
	 * The annotations panel is added to the VolumeTriPlanarInterface for display.
	 */
	public JPanel initDisplayAnnotationsPanel( VOILatticeManagerInterface voiInterface, VolumeImage image, boolean latticeMarkers )
	{		
		voiManager = voiInterface;
		imageA = image;
		voiManager.addAnnotationListener(this);
		if ( annotationPanel == null )
		{			
			annotationPanel = new JPanel();
			annotationPanel.setLayout(new BorderLayout());
			
			initGB();
			JPanel displayPanel = new JPanel(new BorderLayout());
			JPanel thresholdPanel = new JPanel(gbLayout);
			thresholdPanel.setBorder(JDialogBase.buildTitledBorder("Threshold Segmentation"));

			thresholdPanel.add( new JLabel("min value:"), gbc );
			thresholdMin = new JTextField( String.valueOf(imageA.GetImage().getMax()/2) );
			gbc.gridx++;
			thresholdPanel.add( thresholdMin, gbc );
			
			gbc.gridx = 0;
			gbc.gridy++;
			thresholdPanel.add( new JLabel("max value:"), gbc );
			thresholdMax = new JTextField( String.valueOf(imageA.GetImage().getMax()) );
			gbc.gridx++;
			thresholdPanel.add( thresholdMax, gbc );
			
			// Segment button:
			gbc.gridx = 0;
			gbc.gridy++;
			JButton segment = new JButton("threshold segmentation" );
			segment.addActionListener(this);
			segment.setActionCommand("thresholdSegmentation");
			segment.setPreferredSize(MipavUtil.defaultButtonSize);
			thresholdPanel.add( segment, gbc );
			gbc.gridx++;
			JButton deleteSegmentation = new JButton("remove" );
			deleteSegmentation.addActionListener(this);
			deleteSegmentation.setActionCommand("deleteSegmentation");
			deleteSegmentation.setPreferredSize(MipavUtil.defaultButtonSize);
			thresholdPanel.add( deleteSegmentation, gbc );
			gbc.gridx++;
			JButton saveSegmentation = new JButton("save" );
			saveSegmentation.addActionListener(this);
			saveSegmentation.setActionCommand("saveSegmentation");
			saveSegmentation.setPreferredSize(MipavUtil.defaultButtonSize);
			thresholdPanel.add( saveSegmentation, gbc );
			

			initGB();
			JPanel labelPanel = new JPanel(gbLayout);
			// Display checkbox for displaying individual annotations:
			displayLabel = new JCheckBox("display", true);
			displayLabel.addActionListener(this);
			displayLabel.setActionCommand("displayLabel");

			// Display checkbox for displaying individual annotations:
			displayGroupLabel = new JCheckBox("display group", true);
			displayGroupLabel.addActionListener(this);
			displayGroupLabel.setActionCommand("displayGroupLabel");

			gbc.gridx = 0;			gbc.gridy = 0;
			labelPanel.add( new JLabel("Annotation: " ), gbc );
			gbc.gridx++;			gbc.gridy = 0;
			labelPanel.add(displayLabel, gbc);

			gbc.gridx++;			gbc.gridy = 0;
			labelPanel.add(displayGroupLabel, gbc);

			// Display all button:
			JButton displayAll = new JButton("Display all" );
			displayAll.addActionListener(this);
			displayAll.setActionCommand("displayAll");
			gbc.gridx++;			gbc.gridy = 0;
			labelPanel.add( displayAll, gbc );

			// Display none button:
			JButton displayNone = new JButton("Display none" );
			displayNone.addActionListener(this);
			displayNone.setActionCommand("displayNone");
			gbc.gridx++;			gbc.gridy = 0;
			labelPanel.add( displayNone, gbc );

			// Open CSV button:
			JButton openAnnotations = new JButton("Open annotation .csv" );
			openAnnotations.addActionListener(this);
			openAnnotations.setActionCommand("openAnnotations");
			gbc.gridx = 0;			gbc.gridy++;
			labelPanel.add( openAnnotations, gbc );

			// volume clip checkbox for clipping around individual annotations:
			volumeClip = new JCheckBox("volume clip", true);
			volumeClip.setSelected(false);
			volumeClip.addActionListener(this);
			volumeClip.setActionCommand("volumeClip");
			gbc.gridx = 1;			gbc.gridy++;
			labelPanel.add( volumeClip, gbc );

			volumeRadius = new JSlider(0, 70, 30);
			volumeRadius.addChangeListener(this);
			gbc.gridx++;
			labelPanel.add( volumeRadius, gbc );
			
			labelPanel.setBorder(JDialogBase.buildTitledBorder("Display and Clipping"));

			// build the annotation table for the list of annotations:
			buildAnnotationTable(latticeMarkers);
			// add annotation table to a scroll pane:
			JScrollPane kScrollPane = new JScrollPane(annotationTable);
			Dimension size = kScrollPane.getPreferredSize();
			//			System.err.println( size.width + " " + size.height );
			size.height /= 2;
			kScrollPane.setPreferredSize( size );
			kScrollPane.setBorder(JDialogBase.buildTitledBorder("Annotation list"));


			// add annotation table to a scroll pane:
			JScrollPane kScrollPaneGroup = new JScrollPane(annotationGroupTable);
			size = kScrollPaneGroup.getPreferredSize();
			//			System.err.println( size.width + " " + size.height );
			size.height /= 2;
			kScrollPaneGroup.setPreferredSize( size );
			kScrollPaneGroup.setBorder(JDialogBase.buildTitledBorder("Group list"));

			displayPanel.add(thresholdPanel, BorderLayout.NORTH);
			displayPanel.add(labelPanel, BorderLayout.SOUTH);
			
			annotationPanel.add(kScrollPane, BorderLayout.NORTH);
			annotationPanel.add(kScrollPaneGroup, BorderLayout.CENTER);
			annotationPanel.add(displayPanel, BorderLayout.SOUTH);
			//			annotationPanel.setBorder(JDialogBase.buildTitledBorder("Annotation list"));
		}

		// Add the list of annotations to the table:
		annotationChanged();

		return annotationPanel;
	}

	private boolean ctrlKey = false;
	public void keyTyped(KeyEvent e) {

		ctrlKey = e.isControlDown();

		System.err.println("keyTyped");
		if ( e.getKeyChar() == KeyEvent.VK_TAB ) {
			if ( voiManager != null )
			{
				if ( (e.getSource() == annotationTable) ) {
					// add a new annotation by tabbing:
					int row = annotationTable.getSelectedRow();
					int col = annotationTable.getSelectedColumn();
					if ( (row == 0)  && (col == 0) ) {

						VOIWormAnnotation text = new VOIWormAnnotation();
						text.setText("center" );
						int dimX = imageA.GetImage().getExtents()[0];
						int dimY = imageA.GetImage().getExtents()[1];
						int dimZ = imageA.GetImage().getExtents()[2];
						text.add( new Vector3f( dimX/2, dimY/2, dimZ/2 ) );
						text.add( new Vector3f( dimX/2, dimY/2, dimZ/2 ) );

						short id = (short) imageA.GetImage().getVOIs().getUniqueID();
						int colorID = 0;
						VOI newTextVOI = new VOI((short) colorID, "annotation3d_" + id, VOI.ANNOTATION, -1.0f);
						newTextVOI.getCurves().add(text);

						voiManager.clear3DSelection();
						voiManager.addAnnotation( newTextVOI );
						voiManager.clear3DSelection();
						int nRows = annotationTable.getRowCount();
						annotationTable.setRowSelectionInterval(nRows-1, nRows-1);
					}
				}
				else if ( e.getSource() == annotationGroupTable ) {
					int row = annotationGroupTable.getSelectedRow();
					int col = annotationGroupTable.getSelectedColumn();
					if ( (row == 0)  && (col == 0) ) {
						annotationGroupTableModel.removeTableModelListener(this);
						annotationGroupTableModel.addRow( new Object[]{  } );
						annotationGroupTableModel.addTableModelListener(this);

						int nRows = annotationGroupTable.getRowCount();
						annotationGroupTable.setRowSelectionInterval(nRows-1, nRows-1);
					}	
				}
			}
		}
		else if ( e.getKeyChar() == KeyEvent.VK_DELETE ) {
			System.err.println("delete");
			if ( (e.getSource() == annotationTable) ) {
				int row = annotationTable.getSelectedRow();
				int col = annotationTable.getSelectedColumn();
				if ( col == 0 && row >= 0 )
				{
					TableCellEditor editor = annotationTable.getCellEditor();
					if ( editor != null )
						editor.stopCellEditing();
					voiManager.deleteSelectedPoint();
					int nRows = annotationTable.getRowCount();
					if ( row < nRows ) {
						annotationTable.setRowSelectionInterval(row, row);
					}
					else if ( nRows > 0 ) {
						annotationTable.setRowSelectionInterval(nRows-1, nRows-1);
					}
				}
			}
			else if ( (e.getSource() == annotationGroupTable) ) {
				int row = annotationGroupTable.getSelectedRow();
				int col = annotationGroupTable.getSelectedColumn();
				if ( col == 0 && row >= 0 )
				{
					TableCellEditor editor = annotationGroupTable.getCellEditor();
					if ( editor != null )
						editor.stopCellEditing();

					voiManager.deleteSelectedPoint();
					int nRows = annotationGroupTable.getRowCount();
					if ( row < nRows ) {
						annotationGroupTable.setRowSelectionInterval(row, row);
					}
					else if ( nRows > 0 ) {
						annotationGroupTable.setRowSelectionInterval(nRows-1, nRows-1);
					}
				}
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		ctrlKey = e.isControlDown();
	}

	@Override
	public void keyReleased(KeyEvent e) {
		ctrlKey = e.isControlDown();
	}

	public void stateChanged(ChangeEvent arg0) {
		Object source = arg0.getSource();
		if ( source == volumeRadius ) {
			float value = volumeRadius.getValue();
			if ( voiManager != null )
			{
				VOI annotations = voiManager.getAnnotations();
				// selected row:
				int row = annotationTable.getSelectedRow();		        
				if ( (annotations != null) && (row >= 0) ) {
					if ( row < annotations.getCurves().size() ) {
						VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(row);
						text.getVolumeVOI().setVolumeClipRadius(value);
					}
				}
			}
		}
	}

	public static String getPrefix(String name) {
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

	public static String getPostfix(String name) {
		String prefix = new String();
		for ( int j = 0; j < name.length(); j++ ) {
			if ( Character.isLetter(name.charAt(j) ) ) {
				prefix += name.charAt(j);
			}
			else {
				break;
			}
		}
		//		System.err.println( name + "  " + prefix + "   " + (name.indexOf(prefix) + prefix.length()));
		return name.substring( name.indexOf(prefix) + prefix.length() );
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent e) {
		//		System.err.println("mousePressed " + ctrlKey );
		//		updateTableSelection();
	}

	@Override
	public void mouseReleased(MouseEvent e) {}

	private void updateTableSelection(ListSelectionEvent e) {

		if ( e.getSource() == annotationList ) {
			if ( annotationTable.getRowCount() > 0 ) {
				int[] rows = annotationTable.getSelectedRows();

				// Updates the displayLabel checkbox based on which row of the table is current:
				VOI annotations = voiManager.getAnnotations();

				for ( int i = 0; i < annotations.getCurves().size(); i++ ) {
					VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(i);
					text.setSelected( false );
					text.updateSelected( imageA.GetImage() );
				}

				for ( int i = 0; i < rows.length; i++ ) {
					VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(rows[i]);
					text.setSelected( true );
					text.updateSelected( imageA.GetImage() );
				}
				imageA.GetImage().notifyImageDisplayListeners();
			}
		}
		else if ( e.getSource() == annotationGroupList ) {
			int[] rows = annotationGroupTable.getSelectedRows();

			// Updates the displayLabel checkbox based on which row of the table is current:
			VOI annotations = voiManager.getAnnotations();

			for ( int i = 0; i < annotations.getCurves().size(); i++ ) {
				VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(i);
				text.setSelected( false );
				text.updateSelected( imageA.GetImage() );
			}
			annotationList.removeListSelectionListener(this);
			annotationList.clearSelection();
			for ( int i = 0; i < rows.length; i++ ) {
				selectedPrefix = new String ( annotationGroupTableModel.getValueAt(rows[i], 0).toString() );

				for ( int j = 0; j < annotations.getCurves().size(); j++ ) {
					VOIWormAnnotation text = (VOIWormAnnotation) annotations.getCurves().elementAt(j);
					String prefix = getPrefix(text.getText());
					if ( prefix.equals(selectedPrefix) ) {
						text.setSelected(true);
						text.updateSelected(imageA.GetImage());
						annotationList.addSelectionInterval(j, j);
					}
				}
			}
			annotationList.addListSelectionListener(this);
			imageA.GetImage().notifyImageDisplayListeners();
		}
	}
	
	private int segmentationCount = 1;
    public void create3DVOI( ModelImage mask)
    {
    	int[] aiExtents = mask.getExtents();
        int length = aiExtents[0] * aiExtents[1] * aiExtents[2];
        int[] buffer = new int[length];

        for (int i = 0; i < length; i++) {
            buffer[i] = (int)mask.getFloat(i);
        }

        float[] afResolutions = mask.getResolutions(0);
        int[] direction = MipavCoordinateSystems.getModelDirections(mask);
        float[] startLocation = mask.getFileInfo(0).getOrigin();
        SurfaceExtractorCubes kExtractor = 
            new SurfaceExtractorCubes(aiExtents[0], 
                    aiExtents[1], 
                    aiExtents[2], buffer,
                    afResolutions[0], 
                    afResolutions[1], 
                    afResolutions[2], direction,
                    startLocation, null);
        TriMesh mesh = kExtractor.getLevelSurface( (int)mask.getMax(), false );
        if ( mesh != null )
        {
        	SurfaceState surface = new SurfaceState( mesh, "worm" );
			volumeRenderer.addSurface( surface, true );
			volumeRenderer.displaySurface(true);
			updateSurfacePanels();
			System.err.println("TriMesh " + mesh.GetTriangleQuantity());
        }
        else
        {
			System.err.println("TriMesh null");
        }
        kExtractor = null;
    }
    
	private JPanelLights_WM lightsPanel = null;
	//	private JPanelSurface_WM surfaceGUI = null;
	private void updateSurfacePanels() {
		if ( lightsPanel == null ) {
			lightsPanel = new JPanelLights_WM(volumeRenderer);
			lightsPanel.enableLight(0, true);
			lightsPanel.enableLight(1, true);
			volumeRenderer.updateLighting(lightsPanel.getAllLights());
		}
//		if ( surfaceGUI == null ) {
//			surfaceGUI = new JPanelSurface_WM(volumeRenderer);
//			tabbedPane.addTab("Surface", null, lightsPanel);
//		}
	}

}
