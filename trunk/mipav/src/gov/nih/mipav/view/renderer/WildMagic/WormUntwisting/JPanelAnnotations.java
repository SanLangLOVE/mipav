package gov.nih.mipav.view.renderer.WildMagic.WormUntwisting;


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
import gov.nih.mipav.model.structures.VOI;
import gov.nih.mipav.model.structures.VOIText;
import gov.nih.mipav.plugins.JDialogStandalonePlugin;
import gov.nih.mipav.view.dialogs.GuiBuilder;
import gov.nih.mipav.view.dialogs.JDialogBase;
import gov.nih.mipav.view.renderer.WildMagic.VolumeTriPlanarInterface;
import gov.nih.mipav.view.renderer.WildMagic.Interface.JInterfaceBase;
import gov.nih.mipav.view.renderer.WildMagic.VOI.VOILatticeManagerInterface;
import gov.nih.mipav.view.renderer.WildMagic.VOI.VOIManagerInterface;
import gov.nih.mipav.view.renderer.WildMagic.WormUntwisting.AnnotationListener;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;


import WildMagic.LibFoundation.Mathematics.Vector3f;


public class JPanelAnnotations extends JInterfaceBase implements ActionListener, AnnotationListener, TableModelListener, ListSelectionListener, KeyListener, ChangeListener {

	private static final long serialVersionUID = -9056581285643263551L;

	private ModelImage imageA;
	
	// on 'start' the images are loaded and the VolumeTriPlanarInterface is created:
	private VOILatticeManagerInterface voiManager;
	// annotation panel displayed in the VolumeTriPlanarInterface:
	private JPanel annotationPanel;
	// turns on/off displaying individual annotations
	private JCheckBox volumeClip;
	private JSlider volumeRadius;
	private JCheckBox displayLabel;
	// table user-interface for editing the positions of the annotations:
	private ListSelectionModel annotationList;
	private JTable annotationTable;
	private DefaultTableModel annotationTableModel;
	private boolean useLatticeMarkers = false;
	
	public JPanelAnnotations( VOILatticeManagerInterface voiInterface, ModelImage image ) {
		voiManager = voiInterface;
		imageA = image;
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
						VOIText text = (VOIText) annotations.getCurves().elementAt(i);
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
						VOIText text = (VOIText) annotations.getCurves().elementAt(i);
						text.display(false);
					}
				}
			}
			displayLabel.setSelected(false);
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
						VOIText text = (VOIText) annotations.getCurves().elementAt(row);
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
					VOIText text = (VOIText) annotations.getCurves().elementAt(i);
					text.getVolumeVOI().setVolumeClip(false);					
				}
				// clip radius:
	        	float value = volumeRadius.getValue();
				// selected row:
				int row = annotationTable.getSelectedRow();		        
				if ( (annotations != null) && (row >= 0) ) {
					if ( row < annotations.getCurves().size() ) {
						VOIText text = (VOIText) annotations.getCurves().elementAt(row);
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

		if ( voiManager != null )
		{
			// get current annotations and update table:
			VOI annotations = voiManager.getAnnotations();
			// remove table listener durning updates:
			annotationTableModel.removeTableModelListener(this);
			int numRows = annotationTableModel.getRowCount();
			for ( int i = numRows -1; i >= 0; i-- ) {
				annotationTableModel.removeRow(i);
			}		
			if ( annotations != null ) {
				if ( annotations.getCurves().size() > 0 ) {
					for ( int i = 0; i < annotations.getCurves().size(); i++ ) {
						VOIText text = (VOIText) annotations.getCurves().elementAt(i);

						if ( useLatticeMarkers ) {
							String note = new String(text.getNote());
							System.err.println(i + "  " + note);
							if ( note.contains("lattice segment:") ) {
								int index = note.indexOf("lattice segment:");
								note = note.substring(index + 17, note.length() );
								int value = Integer.valueOf(note);
								System.err.println(note + "   " + value );
								annotationTableModel.addRow( new Object[]{text.getText(), text.elementAt(0).X, text.elementAt(0).Y, text.elementAt(0).Z, value } );
							}
							else {
								annotationTableModel.addRow( new Object[]{text.getText(), text.elementAt(0).X, text.elementAt(0).Y, text.elementAt(0).Z } );
							}
						}
						else
						{
							annotationTableModel.addRow( new Object[]{text.getText(), text.elementAt(0).X, text.elementAt(0).Y, text.elementAt(0).Z } );
						}
					}
				}
			}
			// restore table listener:
			annotationTableModel.addTableModelListener(this);
		}		
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
		if ( voiManager != null )
		{
			int row = e.getFirstRow();
			VOI annotations = voiManager.getAnnotations();
			if ( (row >= 0) && (row < annotations.getCurves().size()) )
			{
				VOIText text = (VOIText) annotations.getCurves().elementAt(row);
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
					}
				}
				else
				{
					try {
						int value = Integer.valueOf(annotationTableModel.getValueAt(row, column).toString());
						String note = text.getNote();
						if ( note.contains("lattice segment:") ) {
							int index = note.indexOf("lattice segment:");
							note = note.substring(0, index);
						}
						note = note + "\n" + "lattice segment: " + value;
						System.err.println(text.getText() + " " + note);
						text.setNote(note);
					} catch ( java.lang.NumberFormatException error ) {
						// value erased:
						String note = text.getNote();
						if ( note.contains("lattice segment:") ) {
							int index = note.indexOf("lattice segment:");
							note = note.substring(0, index);
						}
						System.err.println(text.getText() + " " + note);
						text.setNote(note);
					}
				}
				text.update();
				isChecked = text.getVolumeVOI().GetDisplay();
				isClipped = text.getVolumeVOI().GetClipped();
			}
		}
		displayLabel.setSelected(isChecked);
		volumeClip.setSelected(isClipped);
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent e) {
		if ( e.getSource() == annotationList && e.getValueIsAdjusting() )
			return;
		
		// Updates the displayLabel checkbox based on which row of the table is current:
		VOI annotations = voiManager.getAnnotations();

		int row = annotationTable.getSelectedRow();
		boolean isChecked = true;
		boolean isClipped = true;
		if ( (annotations != null) && (row >= 0) ) {
			if ( row < annotations.getCurves().size() ) {
				VOIText text = (VOIText) annotations.getCurves().elementAt(row);
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

	/**
	 * Creates the table that displays the annotation information.
	 * The user can edit the annotations directly in the table.
	 */
	private void buildAnnotationTable(boolean latticeMarkers) {
		if ( annotationTable == null )
		{
			annotationTableModel = new DefaultTableModel();
			annotationTableModel.addColumn("Name");
			annotationTableModel.addColumn("x");
			annotationTableModel.addColumn("y");
			annotationTableModel.addColumn("z");
			if ( latticeMarkers ) {
				annotationTableModel.addColumn("lattice segment");
				useLatticeMarkers = true;
			}
			annotationTableModel.addTableModelListener(this);

			annotationTable = new JTable(annotationTableModel);
			annotationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			annotationTable.addKeyListener(this);

			annotationTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			annotationTable.getColumn("Name").setMinWidth(100);
			annotationTable.getColumn("Name").setMaxWidth(100);
			annotationList = annotationTable.getSelectionModel();
			annotationList.addListSelectionListener(this);
		}
	}

	public JPanel getAnnotationsPanel() {
		return annotationPanel;
	}
	
	/**
	 * The annotations panel is added to the VolumeTriPlanarInterface for display.
	 */
	public JPanel initDisplayAnnotationsPanel( VOILatticeManagerInterface voiInterface, ModelImage image, VOI annotations, boolean latticeMarkers )
	{		
		voiManager = voiInterface;
		imageA = image;
		voiManager.addAnnotationListener(this);
		if ( annotationPanel == null )
		{			
			annotationPanel = new JPanel();
			annotationPanel.setLayout(new BorderLayout());
			
			GridBagLayout gbc = new GridBagLayout();
	        GridBagConstraints gbcC = new GridBagConstraints();
	        
			JPanel labelPanel = new JPanel(gbc);
			// Display checkbox for displaying individual annotations:
			displayLabel = new JCheckBox("display", true);
			displayLabel.addActionListener(this);
			displayLabel.setActionCommand("displayLabel");

			gbcC.gridx = 0;			gbcC.gridy = 0;
			labelPanel.add( new JLabel("Annotation: " ), gbcC );
			gbcC.gridx++;			gbcC.gridy = 0;
			labelPanel.add(displayLabel, gbcC);
			
			// Display all button:
			JButton displayAll = new JButton("Display all" );
			displayAll.addActionListener(this);
			displayAll.setActionCommand("displayAll");
			gbcC.gridx++;			gbcC.gridy = 0;
			labelPanel.add( displayAll, gbcC );
			
			// Display none button:
			JButton displayNone = new JButton("Display none" );
			displayNone.addActionListener(this);
			displayNone.setActionCommand("displayNone");
			gbcC.gridx++;			gbcC.gridy = 0;
			labelPanel.add( displayNone, gbcC );

			// volume clip checkbox for clipping around individual annotations:
			volumeClip = new JCheckBox("volume clip", true);
			volumeClip.setSelected(false);
			volumeClip.addActionListener(this);
			volumeClip.setActionCommand("volumeClip");
			gbcC.gridx = 1;			gbcC.gridy = 1;
			labelPanel.add( volumeClip, gbcC );

			volumeRadius = new JSlider(0, 70, 30);
			volumeRadius.addChangeListener(this);
			gbcC.gridx++;			gbcC.gridy = 1;
			labelPanel.add( volumeRadius, gbcC );
			
			// build the annotation table for the list of annotations:
			buildAnnotationTable(latticeMarkers);
			// add annotation table to a scroll pane:
			JScrollPane kScrollPane = new JScrollPane(annotationTable);
			Dimension size = kScrollPane.getPreferredSize();
			System.err.println( size.width + " " + size.height );
			size.height /= 2;
			kScrollPane.setPreferredSize( size );
			annotationPanel.add(kScrollPane, BorderLayout.NORTH);
			annotationPanel.add(labelPanel, BorderLayout.CENTER);
			annotationPanel.setBorder(JDialogBase.buildTitledBorder("Annotation list"));
		}

		// Add the list of annotations to the table:
		annotationChanged();

		return annotationPanel;
	}

	public void keyTyped(KeyEvent e) {
		if ( e.getKeyChar() == KeyEvent.VK_TAB ) {
			int row = annotationTable.getSelectedRow();
			int col = annotationTable.getSelectedColumn();
			if ( voiManager != null )
			{
				if ( (row == 0)  && (col == 0) ) {
					
					VOIText text = new VOIText();
					text.setText("center" );
					int dimX = imageA.getExtents()[0];
					int dimY = imageA.getExtents()[1];
					int dimZ = imageA.getExtents()[2];
					text.add( new Vector3f( dimX/2, dimY/2, dimZ/2 ) );
					text.add( new Vector3f( dimX/2, dimY/2, dimZ/2 ) );
					
					short id = (short) imageA.getVOIs().getUniqueID();
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
		}
		if ( e.getKeyChar() == KeyEvent.VK_DELETE ) {
			int row = annotationTable.getSelectedRow();
			int col = annotationTable.getSelectedColumn();
			if ( col == 0 && row >= 0 )
			{
				TableCellEditor editor = annotationTable.getCellEditor();
				if ( editor != null )
					editor.stopCellEditing();
				VOI annotations = voiManager.getAnnotations();
				annotations.getCurves().remove(row);
				annotationChanged();
				int nRows = annotationTable.getRowCount();
				if ( row < nRows ) {
					annotationTable.setRowSelectionInterval(row, row);
				}
				else if ( nRows > 0 ) {
					annotationTable.setRowSelectionInterval(nRows-1, nRows-1);
				}
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {}

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
						VOIText text = (VOIText) annotations.getCurves().elementAt(row);
						text.getVolumeVOI().setVolumeClipRadius(value);
					}
				}
			}
        }
	}
}
