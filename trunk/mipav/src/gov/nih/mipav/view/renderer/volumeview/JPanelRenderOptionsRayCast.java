package gov.nih.mipav.view.renderer.volumeview;


import gov.nih.mipav.view.renderer.*;
import gov.nih.mipav.view.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 *   Dialog to turn bounding box of surface renderer on and off,
 *   and to change the color of the frame.
 */
public class JPanelRenderOptionsRayCast extends JPanelRendererBase {

    /** Color button for changing color. */
    private JButton colorButton;

    /** Color button for changing z color. */
    private JButton colorButtonBackground;

    /** Color chooser dialog. */
    private ViewJColorChooser colorChooser;

    /** Flag indicating if box is on or off. */
    private boolean flag = false;

    /** Radio Button for Orthographic rendering. */
    private JRadioButton radioButtonOrthographic;

    /** Radio Button for Perspective rendering. */
    private JRadioButton radioButtonPerspective;

    /** Button group for projections */
    private ButtonGroup radioButtonGroupProjections;

    /* Radio Button Group for the Render Image Target size of the raycast
     * rendered image. Target sizes options are preset to be typical texture
     * sizes:  */
    private JRadioButton m_kRadioMaxRenExtent_64;
    private JRadioButton m_kRadioMaxRenExtent_128;
    private JRadioButton m_kRadioMaxRenExtent_256;
    private JRadioButton m_kRadioMaxRenExtent_512;
    private JRadioButton m_kRadioMaxRenExtent_1024;
    private ButtonGroup m_kRadioGroupMaxRenExtent;


    /** Text field of the ray trace step size.  */
    private JTextField stepText;

    /** Text field of the ray trace space size. */
    private JTextField spaceText;

    /** Volume rendering parent frame */
    private VolumeRendererRayCast myParent;

    /** Scroll panel that holding the all the control components */
     private DrawingPanel scrollPanel;

     /** Scroll pane */
     private JScrollPane scroller;

     /** Button Panel */
     private JPanel buttonPanel;

     /** Blur image check box. */
     private JCheckBox blurBox;

    /**
     *   Creates new dialog for turning bounding box
     *   frame on and off.
     *   @param parent  parent reference
     */
    public JPanelRenderOptionsRayCast( VolumeRendererRayCast parent ) {
        super( parent );
        myParent = parent;
        init();
    }

    /**
     *   Initializes GUI components.
     */
    private void init() {

      // Scroll panel that hold the control panel layout in order to use JScrollPane
      scrollPanel = new DrawingPanel();
      scrollPanel.setLayout( new BorderLayout() );

      // Put the drawing area in a scroll pane.
      scroller = new JScrollPane( scrollPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
              JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );


        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.anchor = gbc.WEST;
        gbc.gridx = 1;
        gbc.insets = new Insets(5,5,5,5);

        colorButtonBackground = new JButton();
        colorButtonBackground.setPreferredSize( new Dimension( 25, 25 ) );
        colorButtonBackground.setToolTipText( "Change background color" );
        colorButtonBackground.addActionListener( this );
        colorButtonBackground.setBackground( Color.black );

        JLabel backgroundLabel = new JLabel( "Background color" );
        backgroundLabel.setFont( serif12 );
        backgroundLabel.setForeground( Color.black );
        JPanel panel2 = new JPanel();
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel2.add( colorButtonBackground, gbc );
        gbc.gridx = 1;
        panel2.add( backgroundLabel, gbc );
        panel2.setBorder( buildTitledBorder( "Background" ) );

        JPanel projectionTypePanel = new JPanel();

        projectionTypePanel.setBorder( buildTitledBorder( "Projection Type" ) );
        Box projectionTypeBox = new Box( BoxLayout.X_AXIS );

        radioButtonPerspective = new JRadioButton();
        radioButtonOrthographic = new JRadioButton();
        radioButtonGroupProjections = new ButtonGroup();

        radioButtonOrthographic.setText( "Orthographic View" );
        radioButtonPerspective.setText( "Perspective View   " );
        radioButtonGroupProjections.add( radioButtonPerspective );
        radioButtonGroupProjections.add( radioButtonOrthographic );
        projectionTypeBox.add( radioButtonPerspective );
        projectionTypeBox.add( radioButtonOrthographic );
        projectionTypePanel.add( projectionTypeBox );

        boolean parallel = true;

        parallel = myParent.getParallel();
        radioButtonOrthographic.setSelected( parallel );
        radioButtonPerspective.setSelected( !parallel );

        radioButtonOrthographic.addActionListener( this );
        radioButtonPerspective.addActionListener( this );

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.fill = gbc.HORIZONTAL;
        gbc.weightx = 1;
        gbc.anchor = gbc.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);

        JPanel stepPanel = new JPanel( new GridBagLayout() );

        stepPanel.setBorder( buildTitledBorder( "Ray trace optimization during mouse rotation" ) );
        JLabel stepLabel = new JLabel( "Step size " );

        int stepSize = 1;

        stepSize = myParent.getStepSize();
        stepText = new JTextField( Integer.toString( stepSize ) );
        stepPanel.add( stepLabel, gbc );
        gbc.gridx = 1;
        stepPanel.add( stepText, gbc );

        JLabel spaceLabel = new JLabel( "Space size " );
        int spaceSize = 1;

        spaceSize = myParent.getSpaceSize();
        spaceText = new JTextField( Integer.toString( spaceSize ) );
        gbc.gridx = 0;
        gbc.gridy = 1;
        stepPanel.add( spaceLabel, gbc );
        gbc.gridx = 1;
        gbc.gridy = 1;
        stepPanel.add( spaceText, gbc );

        /* Initialize the Render Target Image size radio buttons: */
        m_kRadioMaxRenExtent_64 = new JRadioButton( "64x64" );
        m_kRadioMaxRenExtent_128 = new JRadioButton( "128x128" );
        m_kRadioMaxRenExtent_256 = new JRadioButton( "256x256" );
        m_kRadioMaxRenExtent_512 = new JRadioButton( "512x512" );
        m_kRadioMaxRenExtent_1024 = new JRadioButton( "1024x1024" );
        m_kRadioGroupMaxRenExtent = new ButtonGroup();
        m_kRadioGroupMaxRenExtent.add( m_kRadioMaxRenExtent_64 );
        m_kRadioGroupMaxRenExtent.add( m_kRadioMaxRenExtent_128 );
        m_kRadioGroupMaxRenExtent.add( m_kRadioMaxRenExtent_256 );
        m_kRadioGroupMaxRenExtent.add( m_kRadioMaxRenExtent_512 );
        m_kRadioGroupMaxRenExtent.add( m_kRadioMaxRenExtent_1024 );
        /* The current (default) texture size of the rendered image is used to
         * set which radio button is initially selected: */
        int iMaxRenExtent = myParent.getMaxRenExtent();
        if ( iMaxRenExtent <= 64 )
        {
            m_kRadioMaxRenExtent_64.setSelected( true );
        }
        if ( iMaxRenExtent <= 128 )
        {
            m_kRadioMaxRenExtent_128.setSelected( true );
        }
        else if ( iMaxRenExtent <= 256 )
        {
            m_kRadioMaxRenExtent_256.setSelected( true );
        }
        else if ( iMaxRenExtent <= 512 )
        {
            m_kRadioMaxRenExtent_512.setSelected( true );
        }
        else if ( iMaxRenExtent <= 1024 )
        {
            m_kRadioMaxRenExtent_1024.setSelected( true );
        }
        m_kRadioMaxRenExtent_64.addActionListener( this );
        m_kRadioMaxRenExtent_128.addActionListener( this );
        m_kRadioMaxRenExtent_256.addActionListener( this );
        m_kRadioMaxRenExtent_512.addActionListener( this );
        m_kRadioMaxRenExtent_1024.addActionListener( this );
        JPanel kMaxRenExtentPanel = new JPanel( );
        kMaxRenExtentPanel.setBorder( buildTitledBorder( "Render Target Image Size" ) );
        kMaxRenExtentPanel.add( m_kRadioMaxRenExtent_64 );
        kMaxRenExtentPanel.add( m_kRadioMaxRenExtent_128 );
        kMaxRenExtentPanel.add( m_kRadioMaxRenExtent_256 );
        kMaxRenExtentPanel.add( m_kRadioMaxRenExtent_512 );
        kMaxRenExtentPanel.add( m_kRadioMaxRenExtent_1024 );

        JPanel blurPanel = new JPanel();
        blurPanel.setBorder(buildTitledBorder("Smooth Surface"));
        blurBox = new JCheckBox();
        blurBox.setSelected( true );
        blurBox.addActionListener( this );
        blurBox.setActionCommand( "Blur" );
        blurBox.setText( "Smooth" );
        blurBox.setFont( serif12 );
        blurPanel.add(blurBox);

        Box contentBox = new Box( BoxLayout.Y_AXIS );

        contentBox.add( panel2 );
        contentBox.add( projectionTypePanel );
        contentBox.add( stepPanel );
        contentBox.add( kMaxRenExtentPanel );
        contentBox.add( blurPanel );

        buttonPanel = new JPanel();

        buildApplyButton();
        buttonPanel.add( applyButton );

        contentBox.add( buttonPanel );
        // make Apply button
        scrollPanel.add(contentBox, BorderLayout.NORTH);
        mainPanel.add( scroller, BorderLayout.NORTH );
    }

    /**
     * Get the main control panel
     * @return JPanel  main panel
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     *   Changes color of box frame and button if color
     *   button was pressed; turns bounding box on and off
     *   if checkbox was pressed; and closes dialog if
     *   "Close" button was pressed.
     *   @param event    Event that triggered function.
     */
    public void actionPerformed( ActionEvent event ) {
        Object source = event.getSource();

        if ( source == colorButton || source == colorButtonBackground ) {
            colorChooser = new ViewJColorChooser( new Frame(), "Pick color", new OkColorListener( (JButton) source ),
                    new CancelListener() );
        } else if ( source == radioButtonOrthographic ) {
            setRenderPerspective( true );
        } else if ( source == radioButtonPerspective ) {
            setRenderPerspective( false );
        }
        /* Set the MaxRenExtent value depending on which radio button is
         * selected: */
        else if ( source == m_kRadioMaxRenExtent_64 ) {
            myParent.setMaxRenExtent( 64 );
        } else if ( source == m_kRadioMaxRenExtent_128 ) {
            myParent.setMaxRenExtent( 128 );
        } else if ( source == m_kRadioMaxRenExtent_256 ) {
            myParent.setMaxRenExtent( 256 );
        } else if ( source == m_kRadioMaxRenExtent_512 ) {
            myParent.setMaxRenExtent( 512 );
        } else if ( source == m_kRadioMaxRenExtent_1024 ) {
            myParent.setMaxRenExtent( 1024 );
        }
        else if ( source == applyButton ) {
            setStepSize();
            setSpaceSize();
        } else if ( source == closeButton ) {
            setVisible( false );
        }
        else if ( source == blurBox ) {
           if ( !blurBox.isSelected() ) {
              myParent.setBlurFlag( false );
           } else {
              myParent.setBlurFlag( true );
           }
       }


    }

    /**
     *   Sets the flags for the checkboxes and resets labels.
     *   @param event       Event that triggered this function.
     */
    public synchronized void itemStateChanged( ItemEvent event ) {
    }

    /**
     *   Calls the appropriate method in the parent frame.
     *   @param button   color button reference.
     *   @param color    Color to set box frame to.
     */
    protected void setBoxColor( JButton button, Color color ) {
        if ( button == colorButton ) {
            myParent.setBoxColor( color );
        } else if ( button == colorButtonBackground ) {
            myParent.setBackgroundColor( color );
        }
    }

    /**
     * Enable perspective projection rendering; otherwise use orthographic
     * projection.
     * @param bEnable true to enable perspective projection
     */
    public void setRenderPerspective( boolean bEnable ) {
        myParent.setParallel( bEnable );
        myParent.updateImages( true );
    }

    /**
     * Enable perspective projection rendering; otherwise use orthographic
     * projection.
     * @param bEnable true to enable perspective projection
     */
    public void setStepSize() {
        String tmpStr;
        int stepSize = 1;

        tmpStr = stepText.getText();
        if ( testParameter( tmpStr, 1, 8 ) ) {
            stepSize = Integer.valueOf( tmpStr ).intValue();
        } else {
            stepText.requestFocus();
            stepText.selectAll();
        }

        myParent.setStepSize( stepSize );
        // myParent.updateImages(true);
    }

    /**
     * Enable perspective projection rendering; otherwise use orthographic
     * projection.
     * @param bEnable true to enable perspective projection
     */
    public void setSpaceSize() {
        String tmpStr;
        int spaceSize = 1;

        tmpStr = spaceText.getText();
        if ( testParameter( tmpStr, 1, 8 ) ) {
            spaceSize = Integer.valueOf( tmpStr ).intValue();
        } else {
            spaceText.requestFocus();
            spaceText.selectAll();
        }

        myParent.setSpaceSize( spaceSize );
        // myParent.updateImages(true);
    }

    /**
     *	Makes the dialog visible next to the parent frame.  If this makes it go off the
     *	screen, puts the dialog in the center of the screen.
     *	@param status      Flag indicating if the dialog should be visible.
     */
    public void setVisible( boolean status ) {
        Point location = new Point();

        location.x = renderBase.getLocation().x + renderBase.getWidth();
        location.y = renderBase.getLocation().y; // + parentFrame.getHeight() - this.getHeight();

        if ( location.x + getWidth() < Toolkit.getDefaultToolkit().getScreenSize().width
                && location.y + getHeight() < Toolkit.getDefaultToolkit().getScreenSize().height ) {
            setLocation( location );
        } else {
            Rectangle dialogBounds = getBounds();

            setLocation( Toolkit.getDefaultToolkit().getScreenSize().width / 2 - dialogBounds.width / 2,
                    Toolkit.getDefaultToolkit().getScreenSize().height / 2 - dialogBounds.height / 2 );
        }
        super.setVisibleStandard( status );
    }

    /**
     *    Pick up the selected color and call method to change the VOI color
     *
     */
    class OkColorListener
        implements ActionListener {

        JButton button;

        OkColorListener( JButton _button ) {
            super();
            button = _button;
        }

        /**
         *   Get color from chooser and set button
         *   and VOI color.
         *   @param e    Event that triggered function.
         */
        public void actionPerformed( ActionEvent e ) {
            Color color = colorChooser.getColor();

            button.setBackground( color );
            setBoxColor( button, color );
        }
    }


    /**
     *  Calls dispose
     */
    protected void finalize() throws Throwable {
       disposeLocal(false);
       super.finalize();
    }

    /**
     * Dispose global variables.
     * @param flag   dispose super or not.
     */
    public void disposeLocal(boolean flag) {
       colorButton = null;
       colorButtonBackground = null;
       if (colorChooser != null) {
          colorChooser = null;
       }
       radioButtonOrthographic = null;
        radioButtonPerspective = null;
        radioButtonGroupProjections = null;
        stepText = null;

        if (flag == true) {
           super.disposeLocal();
        }

    }

    /**
     *    Does nothing.
     */
    class CancelListener
        implements ActionListener {

        /**
         *   Does nothing.
         */
        public void actionPerformed( ActionEvent e ) {}
    }

    /**
      * Resizig the control panel with ViewJFrameVolumeView's frame width and height
      */
     public void resizePanel( int panelWidth, int frameHeight ) {
         scroller.setPreferredSize( new Dimension( panelWidth, frameHeight - buttonPanel.getHeight() ) );
         scroller.setSize( new Dimension( panelWidth, frameHeight - buttonPanel.getHeight() ) );
         scroller.revalidate();

     }


    /**
     * Wrapper in order to hold the control panel layout in the JScrollPane
     */
    class DrawingPanel extends JPanel {
        protected void paintComponent( Graphics g ) {
            super.paintComponent( g );

        }
    }


}
