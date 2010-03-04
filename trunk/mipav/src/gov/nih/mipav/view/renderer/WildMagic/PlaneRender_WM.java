package gov.nih.mipav.view.renderer.WildMagic;

import gov.nih.mipav.MipavCoordinateSystems;
import gov.nih.mipav.MipavMath;
import gov.nih.mipav.model.file.FileInfoBase;
import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.model.structures.ModelLUT;
import gov.nih.mipav.model.structures.ModelRGB;
import gov.nih.mipav.model.structures.ModelStorageBase;
import gov.nih.mipav.model.structures.VOI;
import gov.nih.mipav.model.structures.VOIBase;
import gov.nih.mipav.model.structures.VOIVector;
import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.Preferences;
import gov.nih.mipav.view.WindowLevel;
import gov.nih.mipav.view.renderer.WildMagic.Render.LocalVolumeVOI;
import gov.nih.mipav.view.renderer.WildMagic.Render.VolumeImage;
import gov.nih.mipav.view.renderer.WildMagic.Render.VolumeSlices;
import gov.nih.mipav.view.renderer.WildMagic.Render.VolumeVOI;
import gov.nih.mipav.view.renderer.WildMagic.VOI.ScreenCoordinateListener;
import gov.nih.mipav.view.renderer.WildMagic.VOI.VOIManager;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.FileNotFoundException;
import java.util.Vector;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;

import WildMagic.LibFoundation.Mathematics.ColorRGB;
import WildMagic.LibFoundation.Mathematics.ColorRGBA;
import WildMagic.LibFoundation.Mathematics.Matrix3f;
import WildMagic.LibFoundation.Mathematics.Matrix4f;
import WildMagic.LibFoundation.Mathematics.Vector3f;
import WildMagic.LibFoundation.Mathematics.Vector4f;
import WildMagic.LibGraphics.Effects.ShaderEffect;
import WildMagic.LibGraphics.Effects.VertexColor3Effect;
import WildMagic.LibGraphics.Rendering.Camera;
import WildMagic.LibGraphics.Rendering.Renderer;
import WildMagic.LibGraphics.Rendering.WireframeState;
import WildMagic.LibGraphics.Rendering.ZBufferState;
import WildMagic.LibGraphics.SceneGraph.Attributes;
import WildMagic.LibGraphics.SceneGraph.IndexBuffer;
import WildMagic.LibGraphics.SceneGraph.Polyline;
import WildMagic.LibGraphics.SceneGraph.StandardMesh;
import WildMagic.LibGraphics.SceneGraph.TriMesh;
import WildMagic.LibGraphics.SceneGraph.VertexBuffer;
import WildMagic.LibGraphics.Shaders.Program;
import WildMagic.LibRenderers.OpenGLRenderer.OpenGLRenderer;

import com.sun.opengl.util.Animator;



/**
 * Class PlaneRenderWM: renders a single dimension of the ModelImage data as a
 * texture-mapped polygon. The PlaneRenderWM class keeps track of whether it is
 * rendering the Axial, Sagittal, or Coronal view of the data.
 *
 */
public class PlaneRender_WM extends GPURenderBase
    implements GLEventListener, ScreenCoordinateListener
{

    /** Use serialVersionUID for interoperability. */
    private static final long serialVersionUID = 2025132936439496099L;
    /** Camera Locations, for rendering the different Axial, Sagittal and Coronal views. */
    Vector3f[] m_akCLoc = { new Vector3f(-1.0f,0.0f,0.0f), new Vector3f(0.0f,-1.0f,0.0f), new Vector3f(0.0f,0.0f,-1.0f) };

    /** Camera Direction, UP, and Right vectors, for rendering the different Axial, Sagittal and Coronal views. */
    Vector3f[] m_akCoords = { new Vector3f(Vector3f.UNIT_X), new Vector3f(Vector3f.UNIT_Y), new Vector3f(Vector3f.UNIT_Z) };

    /** Actual image orientation. */
    protected boolean m_bPatientOrientation = true;

    /** Which dimension of the ModelImage to render. */
    protected int m_iPlaneOrientation = 0;

    /** Window-level interface. */
    protected WindowLevel m_kWinLevel;

    /** The image dimensions in x,y,z:. */
    private int[] m_aiLocalImageExtents;

    private float[] m_afResolutions;
    private int[] m_aiUnits;

    /** Set of colors used to draw the axis labels. */
    private ColorRGB[][] m_aakColors = { { new ColorRGB(1, 1, 0), new ColorRGB(0, 1, 0), new ColorRGB(1, 0, 0) },
                                         { new ColorRGB(1, 1, 0), new ColorRGB(1, 0, 0), new ColorRGB(0, 1, 0) },
                                         { new ColorRGB(0, 1, 0), new ColorRGB(1, 0, 0), new ColorRGB(1, 1, 0) } };

    /** Axis labels color assignments.  */
    private int[][] m_aaiColorSwap = { { 2, 1, 0 }, { 1, 2, 0 }, { 1, 0, 2 } };

    /** when true, the axis labels (P-> L-> S->) will be drawn */
    private boolean m_bDrawAxes = true;

    /** Turns on drawing of the X,Y bars and the Axis labels:. */
    private boolean m_bDrawXHairs = true;

    /** Change the mouse cursor with the first mouseDrag event */
    private boolean m_bFirstDrag = true;

    /** True when the left mouse has been pressed, set to false when the left
     * mouse button is released. */
    private boolean m_bLeftMousePressed = false;

    /** Flag indicating if the right mouse button is currently pressed
     * down: */
    private boolean m_bRightMousePressed = false;

    /** The current active lookup table: */
    private ModelStorageBase m_kActiveLookupTable;

    /** lower x-bound of the texture-mapped polygon: */
    private float m_fX0;

    /** upper x-bound of the texture-mapped polygon: */
    private float m_fX1;

    /** Numbers dictating the size of the plane based on the extents and
     * resolutions of the image. */
    private float m_fXBox, m_fYBox, m_fMaxBox;

    /** Width of the texture-mapped polygon: */
    private float m_fXRange;

    /** lower y-bound of the texture-mapped polygon: */
    private float m_fY0;
    /** upper y-bound of the texture-mapped polygon: */
    private float m_fY1;
    /** Height of the texture-mapped polygon: */
    private float m_fYRange;

    /** Image scaling from Zoom:. */
    private float m_fZoomScale = 1.0f;
    /** Which slice is currently displayed in the XY plane. */
    private int m_iSlice;

    /** Current active image for manipulating the LUT by dragging with the
     * right-mouse down. */
    private ModelImage m_kActiveImage;

    /** x-axis label: */
    private String m_kLabelX = new String("X");
    private String m_kLabelXDisplay = new String("X");
    
    /** y-axis label: */
    private String m_kLabelY = new String("Y");
    /** x-axis arrow */
    private TriMesh[] m_kXArrow;
    /** y-axis arrow */
    private TriMesh[] m_kYArrow;
    /** Drawing the axis arrows in screen-space. */
    private Camera m_spkScreenCamera;
    private int m_iLabelX_SpacingX;
    private int m_iLabelX_SpacingY;

    private int m_iLabelY_SpacingX;
    private int m_iLabelY_SpacingY;
    private boolean m_bUpdateSpacing = false;
    /** ModelImage axis orientation. */
    private int[] m_aiAxisOrder;
    
    /** ModelImage axis flip. */
    private boolean[] m_abAxisFlip;

    /** For zooming with the mouse. */
    private float m_fMouseY;
    private boolean m_bShowSurface = false;
    
    private Vector3f m_kPatientPt = new Vector3f();
    private Vector3f m_kVolumeScale = new Vector3f();
    private Vector3f m_kVolumeScaleInv = new Vector3f();
    
    private Vector3f m_kCenter = new Vector3f();
    private boolean m_bIsMouseActive = true;
    private Matrix4f m_kPVWMatrix = new Matrix4f();
    /**
     * Default PlaneRender interface.
     */
    public PlaneRender_WM ()
    {
        super();
    }

    
    /**
     * @param kParent
     * @param kAnimator
     * @param kVolumeImageA
     * @param iPlane
     */
    public PlaneRender_WM(VolumeTriPlanarInterface kParent, Animator kAnimator, 
                          VolumeImage kVolumeImageA, VolumeImage kVolumeImageB,
                          int iPlane)
    {
        super();
        m_pkRenderer = new OpenGLRenderer( m_eFormat, m_eDepth, m_eStencil,
                                          m_eBuffering, m_eMultisampling,
                                           m_iWidth, m_iHeight );
        ((OpenGLRenderer)m_pkRenderer).GetCanvas().addGLEventListener( this );       
        ((OpenGLRenderer)m_pkRenderer).GetCanvas().addKeyListener( this );       
        ((OpenGLRenderer)m_pkRenderer).GetCanvas().addMouseListener( this );       
        ((OpenGLRenderer)m_pkRenderer).GetCanvas().addMouseMotionListener( this );        
        ((OpenGLRenderer)m_pkRenderer).GetCanvas().addMouseWheelListener( this );   

        m_kAnimator = kAnimator;
        m_kVolumeImageA = kVolumeImageA;
        m_kVolumeImageB = kVolumeImageB;
        m_kParent = kParent;
        m_iPlaneOrientation = iPlane;

        setOrientation();
        m_kWinLevel = new WindowLevel(); 
    }

    
    /**
     * Adds the VolumeSlices object to the display list for rendering.
     * @param kVolumeSlice.
     */
    public void addSlices( VolumeSlices kVolumeSlice  )
    {
        m_kDisplayList.add(kVolumeSlice);
        m_kTranslate = kVolumeSlice.GetTranslate();
        updateDisplay();
    }

       
    /**
     * Closes the frame.
     */
    public void close() {
        disposeLocal();
    }

     
    /* (non-Javadoc)
     * @see javax.media.opengl.GLEventListener#display(javax.media.opengl.GLAutoDrawable)
     */
    public void display(GLAutoDrawable arg0) {
        if ( !m_bModified )
        {
            return;
        }
        
        if ( m_kVolumeImageA == null ) {
        	return;
        }
        if ( m_pkRenderer != null )
        {
            ((OpenGLRenderer)m_pkRenderer).SetDrawable( arg0 );
        }
        if ( !m_bInit )
        {
            init(arg0);
        }

        m_bModified = false;   
        m_pkRenderer.ClearBuffers();
        if (m_pkRenderer.BeginScene())
        {
            for ( int i = 0; i < m_kDisplayList.size(); i++ )
            {                              
                if ( m_kDisplayList.get(i) instanceof VolumeSlices )
                {
                    boolean bDisplaySave = m_kDisplayList.get(i).GetDisplay();
                    Matrix3f kSave = new Matrix3f(m_kDisplayList.get(i).GetScene().Local.GetRotate());
                    m_kDisplayList.get(i).GetScene().Local.SetRotateCopy(Matrix3f.IDENTITY);
                    m_kDisplayList.get(i).SetDisplay(true);
                    
                    float fBlend = 1;
                    boolean[] bShowBoundingBox = new boolean[]{ true, true, true };
                    boolean[] bShowSlice = new boolean[]{ true, true, true };
                    VolumeSlices kSlices = null;
                    
                    
                    kSlices = (VolumeSlices)m_kDisplayList.get(i);
                    fBlend = kSlices.GetSliceOpacity(m_iPlaneOrientation);
                    kSlices.SetSliceOpacity(m_iPlaneOrientation, 1);
                    kSlices.ShowSurface(m_bShowSurface);
                    for ( int j = 0; j < 3; j++ )
                    {
                        bShowBoundingBox[j] = kSlices.GetShowBoundingBox(j);
                        bShowSlice[j] = kSlices.GetShowSlice(j);
                        kSlices.ShowBoundingBox(j, m_bDrawXHairs);
                        kSlices.ShowSlice(j, true);
                    }
                    m_kDisplayList.get(i).Render( m_pkRenderer, m_kCuller, false, true );

                    m_kDisplayList.get(i).GetScene().Local.SetRotateCopy(kSave);
                    m_kDisplayList.get(i).SetDisplay(bDisplaySave);

                    if ( kSlices != null )
                    {
                        kSlices.SetSliceOpacity(m_iPlaneOrientation, fBlend);     
                        for ( int j = 0; j < 3; j++ )
                        {
                            kSlices.ShowBoundingBox(j, bShowBoundingBox[j]);
                            kSlices.ShowSlice(j, bShowSlice[j]);
                        }
                        kSlices.ShowSurface(false);
                    }
                }
            }
            drawVOIs( m_kVolumeImageA.GetImage().get3DVOIs() );
            drawAxes();
            m_pkRenderer.EndScene();
        }
        m_pkRenderer.DisplayBackBuffer();

    }

    /* (non-Javadoc)
     * @see gov.nih.mipav.view.renderer.WildMagic.GPURenderBase#displayChanged(javax.media.opengl.GLAutoDrawable, boolean, boolean)
     */
    public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2)
    {      
        if ( m_pkRenderer != null )
        {
            ((OpenGLRenderer)m_pkRenderer).SetDrawable( arg0 );
        }
        m_bModified = true;
    }

    public void displaySurface( boolean bOn )
    {
        m_bShowSurface = bOn;
    }

    /**
     * Clean memory.
     */
    public void disposeLocal() {
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                m_aakColors[i][j] = null;
            }
            m_aakColors[i] = null;
        }
        m_aakColors = null;

        m_akCLoc = null;
        m_akCoords = null;
    
        if ( m_kWinLevel != null ) {
        	m_kWinLevel.disposeLocal();
        	m_kWinLevel = null;
        }

        m_aiLocalImageExtents = null;

        m_aaiColorSwap = null;

        m_kActiveLookupTable = null;

        m_kActiveImage = null;
        m_kLabelX = null;
        m_kLabelXDisplay = null;
        m_kLabelY = null;

        if ( m_kXArrow != null ) {
	        for ( int i = 0; i < m_kXArrow.length; i++ )
	        {
	            m_kXArrow[i].dispose();
	        }
	        m_kXArrow = null;
        }
        
        if ( m_kYArrow != null ) {
	        for ( int i = 0; i < m_kYArrow.length; i++ )
	        {
	            m_kYArrow[i].dispose();
	        }
	        m_kYArrow = null;
        }
        
        if ( m_spkScreenCamera != null ) {
	        m_spkScreenCamera.dispose();
	        m_spkScreenCamera = null;
        }
        m_aiAxisOrder = null;
        m_abAxisFlip = null;

        super.dispose();
    }
    

    public Vector3f upSlice()
    {
        if ( m_iSlice+1 < m_aiLocalImageExtents[2] )
        {
            m_kPatientPt.Z++;
            Vector3f volumePt = new Vector3f();                
            MipavCoordinateSystems.patientToFile( m_kPatientPt, volumePt, m_kVolumeImageA.GetImage(), m_iPlaneOrientation );
            m_kParent.setSliceFromPlane( volumePt );
            return volumePt;
        }
        return null;
    }
    
    public Vector3f downSlice()
    {
        if ( m_iSlice-1 >= 0 )
        {
            m_kPatientPt.Z--;
            Vector3f volumePt = new Vector3f();                
            MipavCoordinateSystems.patientToFile( m_kPatientPt, volumePt, m_kVolumeImageA.GetImage(), m_iPlaneOrientation );
            m_kParent.setSliceFromPlane( volumePt );
            return volumePt;
        }
        return null;
    } 

    /* (non-Javadoc)
     * @see gov.nih.mipav.view.renderer.WildMagic.GPURenderBase#GetCanvas()
     */
    public GLCanvas GetCanvas()
    {
        return ((OpenGLRenderer)m_pkRenderer).GetCanvas();
    }
    
    public int getOrientation() 
    {
        return m_iPlaneOrientation;
    }
    
    /* (non-Javadoc)
     * @see javax.media.opengl.GLEventListener#init(javax.media.opengl.GLAutoDrawable)
     */
    public void init(GLAutoDrawable arg0) {
    	if ( m_kVolumeImageA == null ) {
            return;
        }      
        if ( m_pkRenderer != null )
        {
            ((OpenGLRenderer)m_pkRenderer).SetDrawable( arg0 );
        }

        m_bInit = true;        
        m_pkRenderer.InitializeState();
        super.OnInitialize();

        // set up camera
        m_spkCamera.SetFrustum(60.0f,1.0f,0.1f,100.0f);
        m_pkRenderer.OnFrustumChange();
        
        Vector3f kCLoc = new Vector3f(m_akCLoc[m_aiAxisOrder[2]]);
        Vector3f kCDir = new Vector3f(m_akCoords[m_aiAxisOrder[2]]);
        Vector3f kCUp = new Vector3f(m_akCoords[m_aiAxisOrder[1]]);
        Vector3f kCRight = new Vector3f(m_akCoords[m_aiAxisOrder[0]]);
        if ( m_abAxisFlip[2] )
        {
            kCLoc.Scale(-1);
            kCDir.Scale(-1);
        }
        if ( m_abAxisFlip[1] )
        {
            kCUp.Scale(-1);
        }
        if ( m_abAxisFlip[0] )
        {
            kCRight.Scale(-1);
        }
        //invert y-axis
        kCUp.Scale(-1);
        m_spkCamera.SetFrame( kCLoc, kCDir, kCUp, kCRight );
        CreateScene();

        // initial culling of scene
        m_kCuller.SetCamera(m_spkCamera);

        InitializeCameraMotion(.05f,0.001f);
        InitializeObjectMotion(m_spkScene);

        m_kAnimator.add( GetCanvas() );
        m_kParent.setSliceFromPlane( new Vector3f( (m_kVolumeImageA.GetImage().getExtents()[0] - 1)/2.0f,
                (m_kVolumeImageA.GetImage().getExtents()[1] - 1)/2.0f,
                (m_kVolumeImageA.GetImage().getExtents()[2] - 1)/2.0f ) );
    }
    

    /* (non-Javadoc)
     * @see WildMagic.LibApplications.OpenGLApplication.JavaApplication3D#mouseDragged(java.awt.event.MouseEvent)
     */
    public void mouseDragged(MouseEvent kEvent) {
        //super.mouseDragged(kEvent);
        
        /* If the right mouse button is pressed and
         * dragged. processRightMouseDrag updates the HistoLUT window and
         * level (contrast and brightness) */
        
        //Vector4f kMouseVec = new Vector4f( kEvent.getX()-m_iWidth/2, kEvent.getY()-m_iHeight/2, m_iSlice, 0 );
        Vector4f kMouseVec = new Vector4f( kEvent.getX(), kEvent.getY(), m_iSlice, 1 );
        Vector4f kMouseWorld = new Vector4f();
        m_kPVWMatrix.Mult( kMouseVec, kMouseWorld );
        kMouseWorld.Scale( 1.0f/kMouseWorld.W );
        //System.err.println( kMouseVec.ToString() + " " + kMouseWorld.ToString() );

        if ( !m_bIsMouseActive )
        {
            return;
        }
        if (m_bRightMousePressed && !kEvent.isShiftDown()) {
            processRightMouseDrag(kEvent);
            updateDisplay ();

        }

        /* Dragging the mouse with the left-mouse button held down changes the
         * positions of the X and Y cross bars, and therefor the ZSlice positions of the associated PlaneRenderWM objects
         * and the TriPlanar Surface. The new positions are calculated and passed onto the parent frame.
         */

        else if (m_bLeftMousePressed && !kEvent.isShiftDown()) {
            processLeftMouseDrag(kEvent);
        }
        else
        {
            if ( kEvent.getY() < m_fMouseY )
            {
                m_fZoomScale += 0.05;
            }
            else if ( kEvent.getY() > m_fMouseY )
            {
                m_fZoomScale -= 0.05;
            }
            zoom( );            
        }
    }

    /* (non-Javadoc)
     * @see WildMagic.LibApplications.OpenGLApplication.JavaApplication3D#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent kEvent) {
        super.mousePressed(kEvent);
        m_fMouseY = kEvent.getY();
        if ( !m_bIsMouseActive )
        {
            return;
        }
        //System.err.println( m_fMouseX + " " + m_fMouseY );
        /* If the button pressed is the left mouse button: */
        if ((kEvent.getButton() == MouseEvent.BUTTON1) && !kEvent.isShiftDown()) {
            m_bLeftMousePressed = true;
            processLeftMouseDrag( kEvent );
            m_bModified = true;
            GetCanvas().display();
        }

        if ((kEvent.getButton() == MouseEvent.BUTTON3) && !kEvent.isShiftDown()) {
            m_bRightMousePressed = true;
        }
    }



    /* (non-Javadoc)
     * @see WildMagic.LibApplications.OpenGLApplication.JavaApplication3D#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent kEvent) {

        super.mousePressed(kEvent); 
        if ( !m_bIsMouseActive )
        {
            return;
        }
        /* If the button pressed is the left mouse button turn off the
         * m_bLeftMousePressed flag: */
        if ((kEvent.getButton() == MouseEvent.BUTTON3) && !kEvent.isShiftDown()) {
            m_bRightMousePressed = false;
            m_kParent.setDefaultCursor();
        }

        if ((kEvent.getButton() == MouseEvent.BUTTON1) && !kEvent.isShiftDown()) {
            processLeftMouseDrag( kEvent );
            m_bLeftMousePressed = false;
        }
        m_bFirstDrag = true;
    }
    

    /* (non-Javadoc)
     * @see java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.MouseWheelEvent)
     */
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        if ( e.getWheelRotation() == 1 )
        {
            m_fZoomScale -= 0.05;
        }
        else 
        {
            m_fZoomScale += 0.05; 
        }
        zoom( );    
    }
    

    /* (non-Javadoc)
     * @see gov.nih.mipav.view.renderer.WildMagic.GPURenderBase#reshape(javax.media.opengl.GLAutoDrawable, int, int, int, int)
     */
    public void reshape(GLAutoDrawable arg0, int iX, int iY, int iWidth, int iHeight) {
    	if ( m_kVolumeImageA == null ) {
        	return;
        }      
        if ( m_pkRenderer != null )
        {
            ((OpenGLRenderer)m_pkRenderer).SetDrawable( arg0 );
        }
    	
        if (iWidth > 0 && iHeight > 0)
        {            
            if ( m_bUpdateSpacing )
            {
                m_iLabelX_SpacingX *= (float)iWidth/(float)m_iWidth;
                m_iLabelX_SpacingY *= (float)iHeight/(float)m_iHeight;
                m_iLabelY_SpacingX *= (float)iWidth/(float)m_iWidth;
                m_iLabelY_SpacingY *= (float)iHeight/(float)m_iHeight;
            }
            m_bUpdateSpacing = true;
            
            
            if (m_pkRenderer != null)
            {
                m_pkRenderer.Resize(iWidth,iHeight);
            }
            
            m_iWidth = iWidth;
            m_iHeight = iHeight;
            m_bModified = true;
            m_spkCamera.Perspective = false;
            float fRMax = (m_fZoomScale*Math.max(m_fX, m_fY))/2.0f;
            float fUMax = fRMax * iHeight / iWidth;
            m_spkCamera.SetFrustum(-fRMax, fRMax,-fUMax, fUMax,1f,5.0f);
            m_pkRenderer.OnFrustumChange();          
            

            float[] afData = new float[16];
            m_pkRenderer.SetConstantWVPMatrix(2, afData);
            m_kPVWMatrix.Set( afData[0], afData[1], afData[2], afData[3],
                    afData[4], afData[5], afData[6], afData[7], 
                    afData[8], afData[9], afData[10], afData[11], 
                    afData[12], afData[13], afData[14], afData[15] );
            //System.err.println( m_kPVWMatrix.ToString() );
        }
        
    }



    
    /**
     * Sets the background color for the frame and rendered image.
     *
     * @param  color  RGBA color to use as the background color.
     */
    public void setBackgroundColor( ColorRGBA kColor )
    {
        m_kBackgroundColor = kColor;
    }
    

    /**
     * setCenter sets the cursor and slice position for this PlaneRenderWM
     * object, based on the 3D location of the three intersecting ModelImage
     * planes.
     * @param center the 3D center in FileCoordinates of the three
     * intersecting ModelImage planes.
     */
    public void setCenter( Vector3f center )
    {
        ModelImage kImage = m_kVolumeImageA.GetImage();
        m_bModified = true;
        MipavCoordinateSystems.fileToPatient( center, m_kPatientPt, kImage, m_iPlaneOrientation );
        setSlice( m_kPatientPt.Z );
        m_kCenter.Mult( center, m_kVolumeScale );
        GetCanvas().display();
    }

    /**
     * Causes re-display.
     */
    public void updateDisplay ( )
    {
        m_bModified = true;
        GetCanvas().display();
    }

    public void setMouseActive( boolean bActive )
    {
        m_bIsMouseActive = bActive;
    }
    
    /**
     * Sets the view to Radiological (true) or Neurological (false) view.
     * @param bOn
     */
    public void setRadiologicalView( boolean bOn )
    {
        if ( m_iPlaneOrientation == FileInfoBase.SAGITTAL )
        {
            return;
        }
        
        Vector3f kCLoc = new Vector3f(m_akCLoc[m_aiAxisOrder[2]]);
        Vector3f kCDir = new Vector3f(m_akCoords[m_aiAxisOrder[2]]);
        Vector3f kCUp = new Vector3f(m_akCoords[m_aiAxisOrder[1]]);
        Vector3f kCRight = new Vector3f(m_akCoords[m_aiAxisOrder[0]]);
        if ( m_abAxisFlip[2] )
        {
            kCLoc.Scale(-1);
            kCDir.Scale(-1);
        }
        if ( m_abAxisFlip[1] )
        {
            kCUp.Scale(-1);
        }
        if ( m_abAxisFlip[0] )
        {
            kCRight.Scale(-1);
        }
        //invert y-axis
        kCUp.Scale(-1);
        if ( !bOn )
        {
            kCLoc.Scale(-1);
            kCDir.Scale(-1);
            kCRight.Scale(-1);
        }
        m_spkCamera.SetFrame( kCLoc, kCDir, kCUp, kCRight );
    }
    

    /**
     * Sets the color for the PlaneRender iView (AXIAL, SAGITTAL, CORONAL) slice.
     *
     * @param  iView  (AXIAL, SAGITTAL, CORONAL)
     * @param  kColor  the new axis color attribute.
     */
    public void setSliceHairColor(int iView, ColorRGB kColor) {
        int iX = 0;
        int iY = 1;
        m_bModified = true;

        m_aakColors[m_iPlaneOrientation][m_aaiColorSwap[m_iPlaneOrientation][iView]] = kColor;
        ColorRGB kXSliceHairColor = m_aakColors[m_iPlaneOrientation][iX];
        ColorRGB kYSliceHairColor = m_aakColors[m_iPlaneOrientation][iY];

        for ( int j = 0; j < 4; j++ )
        {
            m_kXArrow[0].VBuffer.SetColor3( 0, j, kXSliceHairColor );
            m_kYArrow[0].VBuffer.SetColor3( 0, j, kYSliceHairColor );
        }
        for ( int j = 0; j < 3; j++ )
        {
            m_kXArrow[1].VBuffer.SetColor3( 0, j, kXSliceHairColor );
            m_kYArrow[1].VBuffer.SetColor3( 0, j, kYSliceHairColor );
        }
        for ( int i = 0; i < 2; i++ )
        {
            m_kXArrow[i].VBuffer.Release();
            m_kYArrow[i].VBuffer.Release();
        }
    }
    /**
     * Turns displaying the Axis labels on or off:
     *
     * @param bShow when true display the axis lablels, when false hide the
     * axis labels
     */
    public void showAxes(boolean bShow) {
        m_bDrawAxes = bShow;
    }

    /**
     * Turns displaying the X and Y bars on or off:
     *
     * @param  bShow when true show the cross-hairs when false hide the cross-hairs
     */
    public void showXHairs(boolean bShow) {
        if ( m_bDrawXHairs != bShow )
        {
            m_bDrawXHairs = bShow;
        }
    }
    
    
    /**
     * Based on the orientation of the ModelImage, sets up the index
     * parameters, m_aiLocalImageExtents[0], m_aiLocalImageExtents[1], and
     * m_aiLocalImageExtents[2], the drawing colors for the z box, x and y
     * bars, and the invert flags.
     *
     * <p>Once setup everything is rendered into an x,y plane where x,y may be
     * any of the original x,y, or z dimensions in the original
     * ModelImage.</p>
     */
    protected void setOrientation() 
    {
        ModelImage kImage = m_kVolumeImageA.GetImage();
        m_aiAxisOrder = MipavCoordinateSystems.getAxisOrder(kImage, m_iPlaneOrientation);
        m_abAxisFlip = MipavCoordinateSystems.getAxisFlip(kImage, m_iPlaneOrientation);
        m_aiLocalImageExtents = kImage.getExtents( m_iPlaneOrientation );
        m_aiUnits = kImage.getUnitsOfMeasure(0, m_iPlaneOrientation);
        m_afResolutions = kImage.getResolutions( 0, m_iPlaneOrientation );

        if ((m_afResolutions[0] == 0.0f) || (m_afResolutions[1] == 0.0f) || (m_afResolutions[2] == 0.0f)) {
            m_afResolutions[0] = 1.0f;
            m_afResolutions[1] = 1.0f;
            m_afResolutions[2] = 1.0f;
        }

        m_fXBox = (m_aiLocalImageExtents[0] - 1) * m_afResolutions[0];
        m_fYBox = (m_aiLocalImageExtents[1] - 1) * m_afResolutions[1];

        m_fMaxBox = m_fXBox;

        if (m_fYBox > m_fMaxBox) {
            m_fMaxBox = m_fYBox;
        }
        
        float fMaxZ = (m_aiLocalImageExtents[2] - 1) * m_afResolutions[2];
        float fMax = m_fMaxBox;
        if (fMaxZ > fMax) {
            fMax = fMaxZ;
        }
        m_fX = m_fXBox/fMax;
        m_fY = m_fYBox/fMax;
        m_fZ = fMaxZ/fMax;
        

        if ( kImage.getImageOrientation() != FileInfoBase.UNKNOWN_ORIENT )
        {
            if ((m_iPlaneOrientation == FileInfoBase.AXIAL) ||
                (m_iPlaneOrientation == FileInfoBase.CORONAL)) {
                m_kLabelX = new String("L");
            } else {
                m_kLabelX = new String("P");
            }

            if ((m_iPlaneOrientation == FileInfoBase.SAGITTAL) ||
                (m_iPlaneOrientation == FileInfoBase.CORONAL)) {
                m_kLabelY = new String("S");
            } else {
                m_kLabelY = new String("P");
            }
        }
        else
        {
            m_bPatientOrientation = false;
            if ( m_iPlaneOrientation == FileInfoBase.SAGITTAL )
            {
                m_kLabelX = new String("Z");
                m_kLabelY = new String("Y");
            }
            else if ( m_iPlaneOrientation == FileInfoBase.CORONAL )
            {
                m_kLabelX = new String("X");
                m_kLabelY = new String("Z");
            }
        }

        m_kLabelXDisplay = new String( m_kLabelX );
        if ( !m_kVolumeImageA.GetImage().getRadiologicalView() && (m_iPlaneOrientation != FileInfoBase.SAGITTAL) )
        {
            if ( !m_bPatientOrientation )
            {
                m_kLabelXDisplay = new String( "-X" );
            }
            else
            {
                m_kLabelXDisplay = new String( "R" );
            }
        }
        if ( m_iPlaneOrientation == FileInfoBase.AXIAL) 
        {
            m_iLabelX_SpacingX = 50;
            m_iLabelX_SpacingY = 20;
            m_iLabelY_SpacingX = 10;
            m_iLabelY_SpacingY = 68;
        }
        else
        {     
            m_iLabelX_SpacingX = 50;
            m_iLabelX_SpacingY = 10;
            m_iLabelY_SpacingX = 10;
            m_iLabelY_SpacingY = 55;
        }

        ModelImage kImageA = m_kVolumeImageA.GetImage();
        //System.err.println( m_iPlaneOrientation + " " + m_fX + " " + m_fY + " " + m_fZ + " " + fMax );
        
        
        m_kVolumeScale.Set(m_kVolumeImageA.GetScaleX()/(kImageA.getExtents()[0] - 1), 
                m_kVolumeImageA.GetScaleY()/(kImageA.getExtents()[1] - 1), 
                m_kVolumeImageA.GetScaleZ()/(kImageA.getExtents()[2] - 1)  );
        m_kVolumeScaleInv.Copy( m_kVolumeScale );
        m_kVolumeScaleInv.Invert();

        m_kCenter.Mult( m_kVolumeScale );
        
    }
    
    /**
     * Creates the TriMesh data structures for the axis arrows.
     */
    private void CreateLabels()
    {
        // The screen camera is designed to map (x,y,z) in [0,1]^3 to (x',y,'z')
        // in [-1,1]^2 x [0,1].
        m_spkScreenCamera = new Camera();
        m_spkScreenCamera.Perspective = false;
        m_spkScreenCamera.SetFrustum(0.0f,1.0f,0.0f,1.0f,0.0f,1.0f);
        m_spkScreenCamera.SetFrame(Vector3f.ZERO,Vector3f.UNIT_Z,
                Vector3f.UNIT_Y,Vector3f.UNIT_X);
        
        ZBufferState kZState = new ZBufferState();
        kZState.Compare = ZBufferState.CompareMode.CF_ALWAYS;

        m_kXArrow = new TriMesh[2];
        m_kYArrow = new TriMesh[2];

        Attributes kAttr = new Attributes();
        kAttr.SetPChannels(3);
        kAttr.SetCChannels(0,3);
        VertexBuffer pkVBuffer = new VertexBuffer(kAttr,4);
        pkVBuffer.SetPosition3(0, 0.05f,0.05f,0.5f);
        pkVBuffer.SetPosition3(1, 0.15f,0.05f,0.5f);
        pkVBuffer.SetPosition3(2, 0.15f,0.06f,0.5f);
        pkVBuffer.SetPosition3(3, 0.05f,0.06f,0.5f);
        pkVBuffer.SetColor3(0,0,m_aakColors[m_iPlaneOrientation][0] );
        pkVBuffer.SetColor3(0,1,m_aakColors[m_iPlaneOrientation][0] );
        pkVBuffer.SetColor3(0,2,m_aakColors[m_iPlaneOrientation][0] );
        pkVBuffer.SetColor3(0,3,m_aakColors[m_iPlaneOrientation][0] );
        IndexBuffer pkIBuffer = new IndexBuffer(6);
        int[] aiIndex = pkIBuffer.GetData();
        aiIndex[0] = 0;  aiIndex[1] = 1;  aiIndex[2] = 2;
        aiIndex[3] = 0;  aiIndex[4] = 2;  aiIndex[5] = 3;
        m_kXArrow[0] = new TriMesh( new VertexBuffer(pkVBuffer),new IndexBuffer(pkIBuffer));
        m_kXArrow[0].AttachEffect( new VertexColor3Effect() );
        m_kXArrow[0].AttachGlobalState( kZState );
        m_kXArrow[0].UpdateGS();
        m_kXArrow[0].UpdateRS();
        m_pkRenderer.LoadResources(m_kXArrow[0]);

        pkVBuffer = new VertexBuffer(kAttr,3);
        pkVBuffer.SetPosition3(0, 0.15f,0.04f,0.5f);
        pkVBuffer.SetPosition3(1, 0.18f,0.055f,0.5f);
        pkVBuffer.SetPosition3(2, 0.15f,0.07f,0.5f);
        pkVBuffer.SetColor3(0,0,m_aakColors[m_iPlaneOrientation][0] );
        pkVBuffer.SetColor3(0,1,m_aakColors[m_iPlaneOrientation][0] );
        pkVBuffer.SetColor3(0,2,m_aakColors[m_iPlaneOrientation][0] );
        pkIBuffer = new IndexBuffer(3);
        aiIndex = pkIBuffer.GetData();
        aiIndex[0] = 0;  aiIndex[1] = 1;  aiIndex[2] = 2;
        m_kXArrow[1] = new TriMesh( new VertexBuffer(pkVBuffer),new IndexBuffer(pkIBuffer));
        m_kXArrow[1].AttachEffect( new VertexColor3Effect() );
        m_kXArrow[1].AttachGlobalState( kZState );
        m_kXArrow[1].UpdateGS();
        m_kXArrow[1].UpdateRS();
        m_pkRenderer.LoadResources(m_kXArrow[1]);

        // YArrow:

        pkVBuffer = new VertexBuffer(kAttr,4);
        pkVBuffer.SetPosition3(0, 0.05f,0.05f,0.5f);
        pkVBuffer.SetPosition3(1, 0.06f,0.05f,0.5f);
        pkVBuffer.SetPosition3(2, 0.06f,0.15f,0.5f);
        pkVBuffer.SetPosition3(3, 0.05f,0.15f,0.5f);
        pkVBuffer.SetColor3(0,0,m_aakColors[m_iPlaneOrientation][1] );
        pkVBuffer.SetColor3(0,1,m_aakColors[m_iPlaneOrientation][1] );
        pkVBuffer.SetColor3(0,2,m_aakColors[m_iPlaneOrientation][1] );
        pkVBuffer.SetColor3(0,3,m_aakColors[m_iPlaneOrientation][1] );
        pkIBuffer = new IndexBuffer(6);
        aiIndex = pkIBuffer.GetData();
        aiIndex[0] = 0;  aiIndex[1] = 1;  aiIndex[2] = 2;
        aiIndex[3] = 0;  aiIndex[4] = 2;  aiIndex[5] = 3;
        m_kYArrow[0] = new TriMesh( new VertexBuffer(pkVBuffer),new IndexBuffer(pkIBuffer));
        m_kYArrow[0].AttachEffect( new VertexColor3Effect() );
        m_kYArrow[0].AttachGlobalState( kZState );
        m_kYArrow[0].UpdateGS();
        m_kYArrow[0].UpdateRS();
        m_pkRenderer.LoadResources(m_kYArrow[0]);

        pkVBuffer = new VertexBuffer(kAttr,3);
        pkVBuffer.SetPosition3(0, 0.04f,0.15f,0.5f);
        pkVBuffer.SetPosition3(1, 0.07f,0.15f,0.5f);
        pkVBuffer.SetPosition3(2, 0.055f,0.18f,0.5f);
        pkVBuffer.SetColor3(0,0,m_aakColors[m_iPlaneOrientation][1] );
        pkVBuffer.SetColor3(0,1,m_aakColors[m_iPlaneOrientation][1] );
        pkVBuffer.SetColor3(0,2,m_aakColors[m_iPlaneOrientation][1] );
        pkIBuffer = new IndexBuffer(3);
        aiIndex = pkIBuffer.GetData();
        aiIndex[0] = 0;  aiIndex[1] = 1;  aiIndex[2] = 2;
        m_kYArrow[1] = new TriMesh( new VertexBuffer(pkVBuffer),new IndexBuffer(pkIBuffer));
        m_kYArrow[1].AttachEffect( new VertexColor3Effect() );
        m_kYArrow[1].AttachGlobalState( kZState );
        m_kYArrow[1].UpdateGS();
        m_kYArrow[1].UpdateRS();
        m_pkRenderer.LoadResources(m_kYArrow[1]);

        if ( m_iPlaneOrientation == FileInfoBase.AXIAL) 
        {
            Vector3f kPosition = new Vector3f();
            Vector3f kDiff = new Vector3f( 0f, 0.9f, 0f );
            for ( int j = 0; j < 4; j++ )
            {
                m_kXArrow[0].VBuffer.GetPosition3(j, kPosition);
                kPosition.Add(kDiff);
                m_kXArrow[0].VBuffer.SetPosition3(j, kPosition );
            }
            for ( int j = 0; j < 3; j++ )
            {
                m_kXArrow[1].VBuffer.GetPosition3(j, kPosition);
                kPosition.Add(kDiff);
                m_kXArrow[1].VBuffer.SetPosition3(j, kPosition );
            }
            for ( int i = 0; i < 2; i++ )
            {
                m_kXArrow[i].VBuffer.Release();
                m_kXArrow[i].UpdateGS();
                m_kXArrow[i].UpdateRS();
                m_pkRenderer.LoadResources(m_kXArrow[i]);
            }

            pkVBuffer = m_kYArrow[0].VBuffer;
            pkVBuffer.SetPosition3(0, 0.05f,0.85f,0.5f);
            pkVBuffer.SetPosition3(1, 0.06f,0.85f,0.5f);
            pkVBuffer.SetPosition3(2, 0.06f,0.95f,0.5f);
            pkVBuffer.SetPosition3(3, 0.05f,0.95f,0.5f);
            pkVBuffer.Release();
            m_kYArrow[0].UpdateGS();
            m_kYArrow[0].UpdateRS();
            m_pkRenderer.LoadResources(m_kYArrow[0]);

            pkVBuffer = m_kYArrow[1].VBuffer;
            pkVBuffer.SetPosition3(0, 0.04f,0.85f,0.5f);
            pkVBuffer.SetPosition3(1, 0.055f,0.82f,0.5f);
            pkVBuffer.SetPosition3(2, 0.07f,0.85f,0.5f);
            pkVBuffer.Release();
            m_kYArrow[1].UpdateGS();
            m_kYArrow[1].UpdateRS();
            m_pkRenderer.LoadResources(m_kYArrow[1]);
        }
    }    
    
    
    /**
     * Initializes the display parameters.
     */
    private void CreateScene ()
    {        
        m_fX0 = -m_fXBox / m_fMaxBox;
        m_fX1 = m_fXBox / m_fMaxBox;
        m_fY0 = -m_fYBox / m_fMaxBox;
        m_fY1 = m_fYBox / m_fMaxBox;
        
        m_fXRange = m_fX1 - m_fX0;
        m_fYRange = m_fY1 - m_fY0;

        m_iSlice = (m_aiLocalImageExtents[2]) / 2;
        m_kPatientPt.X = (m_aiLocalImageExtents[0]) / 2;
        m_kPatientPt.Y = (m_aiLocalImageExtents[1]) / 2;
        m_kPatientPt.Z = m_iSlice;
              
        CreateLabels();
    }
    
    /**
     * Called from the display function. Draws the axis arrows.
     */
    private void drawAxes()
    {
        if ( m_bDrawAxes )
        {     

            ColorRGBA kXSliceHairColor =
                new ColorRGBA( m_aakColors[m_iPlaneOrientation][0].R,
                               m_aakColors[m_iPlaneOrientation][0].G,
                               m_aakColors[m_iPlaneOrientation][0].B, 1.0f );

            ColorRGBA kYSliceHairColor =
                new ColorRGBA( m_aakColors[m_iPlaneOrientation][1].R,
                               m_aakColors[m_iPlaneOrientation][1].G,
                               m_aakColors[m_iPlaneOrientation][1].B, 1.0f );
            
            if ( !m_kVolumeImageA.GetImage().getRadiologicalView() && (m_iPlaneOrientation != FileInfoBase.SAGITTAL) )
            {
                if ( !m_bPatientOrientation )
                {
                    m_kLabelXDisplay = new String( "-X" );
                }
                else
                {
                    m_kLabelXDisplay = new String( "R" );
                }
            }
            else if ( m_iPlaneOrientation != FileInfoBase.SAGITTAL )
            {
                if ( !m_bPatientOrientation )
                {
                    m_kLabelXDisplay = new String( "X" );
                }
                else
                {
                    m_kLabelXDisplay = new String( "L" );
                }
            }
            if ( m_iPlaneOrientation == FileInfoBase.AXIAL) 
            {
                drawText( m_iLabelX_SpacingX, m_iLabelX_SpacingY, kXSliceHairColor,m_kLabelXDisplay.toCharArray());
                drawText( m_iLabelY_SpacingX, m_iLabelY_SpacingY, kYSliceHairColor,m_kLabelY.toCharArray());
            }
            else
            {
                drawText( m_iLabelX_SpacingX, m_iHeight - m_iLabelX_SpacingY, kXSliceHairColor,m_kLabelXDisplay.toCharArray());
                drawText( m_iLabelY_SpacingX, m_iHeight - m_iLabelY_SpacingY, kYSliceHairColor,m_kLabelY.toCharArray());               
            }
            m_pkRenderer.SetCamera(m_spkScreenCamera);  
            m_pkRenderer.Draw(m_kXArrow[0]);
            m_pkRenderer.Draw(m_kXArrow[1]);
            m_pkRenderer.Draw(m_kYArrow[0]);
            m_pkRenderer.Draw(m_kYArrow[1]);
            m_pkRenderer.SetCamera(m_spkCamera);
        }
    }

    
    protected void drawText( int iX, int iY, ColorRGBA kColor, char[] acText )
    {
        m_pkRenderer.Draw( iX, iY-1, ColorRGBA.BLACK, acText);
        m_pkRenderer.Draw( iX, iY+1, ColorRGBA.BLACK, acText);
        m_pkRenderer.Draw( iX-1, iY, ColorRGBA.BLACK, acText);
        m_pkRenderer.Draw( iX+1, iY, ColorRGBA.BLACK, acText);

        m_pkRenderer.Draw( iX, iY, kColor, acText);        
    }
    private void drawVOIs( VOIVector kVOIs )
    {
        for ( int i = 0; i < kVOIs.size(); i++ )
        {
            VOI kVOI = kVOIs.get(i);
            Vector<VOIBase>[] kCurves = kVOI.getCurves();
            for ( int j = 0; j < kCurves.length; j++ )
            {
                if ( kCurves[j] != null )
                {
                    for ( int k = 0; k < kCurves[j].size(); k++ )
                    {
                        VOIBase kVOI3D = kCurves[j].get(k);
                        if ( kVOI3D instanceof LocalVolumeVOI )
                        {
                            ((LocalVolumeVOI)kVOI3D).draw( this, m_pkRenderer, m_kCuller,                                     
                                    m_afResolutions, m_aiUnits, 
                                    m_aiAxisOrder, m_kCenter, m_iSlice, m_iPlaneOrientation,
                                    m_kVolumeScale, m_kTranslate );
                        }
                    }
                }
            }
        }
    }
    
    /* Convert the position in LocalCoordinates (rendering space) into
     * PatientCoordinates:
     * @param localPt the current point in LocalCoordinates
     * @param patientPt transformed localPt in PatientCoordinates
     */
    public void LocalToPatient( Vector3f localPt, Vector3f patientPt )
    {
        patientPt.X = Math.round(localPt.X * (m_aiLocalImageExtents[0] - 1));
        patientPt.Y = Math.round(localPt.Y * (m_aiLocalImageExtents[1] - 1));
        patientPt.Z = Math.round(localPt.Z * (m_aiLocalImageExtents[2] - 1));
    }

    
    public void PatientToLocal( Vector3f patientPt, Vector3f localPt  )
    {
        localPt.X = patientPt.X / (m_aiLocalImageExtents[0] - 1);
        localPt.Y = patientPt.Y / (m_aiLocalImageExtents[1] - 1);
        localPt.Z = patientPt.Z / (m_aiLocalImageExtents[2] - 1);
    }
    
    /**
     * Dragging the mouse with the left-mouse button held down changes the
     * positions of the X and Y cross bars, and therefore the ZSlice positions
     * of the associated PlaneRenderWM objects and the TriPlanar Surface. The
     * new positions are calculated and passed onto the parent frame.
     *
     * @param  kEvent  the mouse event generated by a mouse drag
     */
    private void processLeftMouseDrag(MouseEvent kEvent) {

        /* Calculate the center of the mouse in local coordinates, taking into
         * account zoom and translate: */
        Vector3f localPt = new Vector3f();
        this.ScreenToLocal(kEvent.getX(), kEvent.getY(), m_iSlice, localPt);

        /* Tell the ViewJFrameVolumeView parent to update the other
         * PlaneRenderWMs and the SurfaceRender with the changed Z position
         * of the planes with color matching the moved bar: */
        Vector3f patientPt = new Vector3f();
        this.LocalToPatient( localPt, patientPt );
        Vector3f volumePt = new Vector3f();
        MipavCoordinateSystems.patientToFile( patientPt, volumePt, m_kVolumeImageA.GetImage(), m_iPlaneOrientation );
        m_kParent.setSliceFromPlane( volumePt );
    }


    public boolean screenToFile(int iX, int iY, int iZ, Vector3f volumePt)
    {
        Vector3f localPt = new Vector3f();
        boolean bClipped = this.ScreenToLocal(iX, iY, iZ, localPt);

        /* Tell the ViewJFrameVolumeView parent to update the other
         * PlaneRenderWMs and the SurfaceRender with the changed Z position
         * of the planes with color matching the moved bar: */
        Vector3f patientPt = new Vector3f();
        this.LocalToPatient( localPt, patientPt );
        MipavCoordinateSystems.patientToFile( patientPt, volumePt, m_kVolumeImageA.GetImage(), m_iPlaneOrientation );
        return bClipped;
    }

    public Vector3f fileToScreen(Vector3f volumePt)
    {
        Vector3f patientPt = new Vector3f();
        MipavCoordinateSystems.fileToPatient( volumePt, patientPt, m_kVolumeImageA.GetImage(), m_iPlaneOrientation );
        return patientToScreen(patientPt);
    }
    
    public Vector3f patientToScreen(Vector3f patientPt)
    {
        Vector3f localPt = new Vector3f();
        PatientToLocal( patientPt, localPt );
        
        Vector3f screenPt = new Vector3f();
        LocalToScreen( localPt, screenPt );
        return screenPt;
    }
    
    /**
     * If the right mouse button is pressed and dragged. processRightMouseDrag
     * updates the HistoLUT window and level (contrast and brightness)
     *
     * @param  kEvent  the mouse event generated by a mouse drag
     */
    private void processRightMouseDrag(MouseEvent kEvent) {
        // make the LUT panel the active panel.
        m_kParent.actionPerformed(new ActionEvent(this, 0, "HistoLUT"));
        /* Get the coordinates of the mouse position in local coordinates: */
        Vector3f localPt = new Vector3f();
        this.ScreenToLocal(kEvent.getX(), kEvent.getY(), m_iSlice, localPt);
        m_kActiveLookupTable = null;

        /* Get which image is active, either m_kImageA or m_kImageB: */
        m_kActiveImage = m_kParent.getHistoLUTActiveImage();

        if (m_kActiveImage == null) {
            m_kActiveImage = m_kParent.getHistoRGBActiveImage();
        }
        if (m_kActiveImage == null) {
            m_kActiveImage = m_kVolumeImageA.GetImage();
        }
        
        m_kActiveLookupTable = m_kParent.getActiveLookupTable(m_kActiveImage);
        
        if ( m_kWinLevel.updateWinLevel( localPt.X, localPt.Y, m_bFirstDrag, m_kActiveLookupTable, m_kActiveImage ) )
        {
            if ( m_kActiveImage == m_kVolumeImageA.GetImage() )
            {
                if ( m_kVolumeImageA.GetImage().isColorImage() )
                {
                    m_kParent.getRGBDialog().setRGBTA((ModelRGB)m_kActiveLookupTable);
                    m_kParent.getRGBDialog().update( );
                }
                else
                {
                    m_kParent.getLUTDialog().setLUTA((ModelLUT)m_kActiveLookupTable);
                }
            }
            else if ( (m_kVolumeImageB.GetImage() != null) && (m_kActiveImage == m_kVolumeImageB.GetImage()) )
            {
                if ( m_kVolumeImageB.GetImage().isColorImage() )
                {
                    m_kParent.getRGBDialog().setRGBTB((ModelRGB)m_kActiveLookupTable);
                    m_kParent.getRGBDialog().update( );
                }
                else
                {
                    m_kParent.getLUTDialog().setLUTB((ModelLUT)m_kActiveLookupTable);
                }
            }
        }
        if (m_bFirstDrag) {
            try {
                Image kImg = MipavUtil.getIconImage("qkwinlevel.gif");
                Cursor kWinLevelCursor = Toolkit.getDefaultToolkit().createCustomCursor(kImg, new Point(12, 12),
                                                                                        "WinLevel");
                /* Set the cursor icon: */
                m_kParent.setCursor(kWinLevelCursor);
            } catch (FileNotFoundException error) { }
            m_bFirstDrag = false;
        }
    }
    

    
    
    /**
     * Calculate the position of the mouse in the Local Coordinates, taking
     * into account zoom and translate:
     *
     * @param iX mouse x coordinate value
     * @param iY mouse y coordinate value
     * @param kLocal mouse position in Local Coordinates
     */
    public boolean ScreenToLocal(int iX, int iY, int iZ, Vector3f kLocal )
    {
        boolean bClipped = false;
        if ( (iX < 0 ) || (iX > m_iWidth) || (iY < 0 ) || (iY > m_iHeight) )
        {
            bClipped = true;
        }
        //System.err.println( "ScreenToLocal " + iX + " " + iY );
        iX = Math.min( m_iWidth,  Math.max( 0, iX ) );
        iY = Math.min( m_iHeight, Math.max( 0, iY ) );
        //System.err.println( "              " + iX + " " + iY );
        float fHalfWidth = ((float) m_iWidth-1) / 2.0f;
        float fHalfHeight = ((float) m_iHeight-1) / 2.0f;

        kLocal.X = (iX - fHalfWidth) / fHalfWidth;
        kLocal.Y = (iY - fHalfHeight) / fHalfWidth;
        //System.err.println( "              " + kLocal.X + " " + kLocal.Y );

        kLocal.X *= m_fZoomScale;
        kLocal.Y *= m_fZoomScale;
        //System.err.println( "              " + kLocal.X + " " + kLocal.Y );

        /* Bounds checking: */
        if ( (kLocal.X < m_fX0) || (kLocal.X > m_fX1) || (kLocal.Y < m_fY0) || (kLocal.Y > m_fY1))
        {
            bClipped = true;
        }
        //System.err.println( bClipped + " " + kLocal.Y + " " + m_fY0 + " " + m_fY1 );
        kLocal.X = Math.min( Math.max( kLocal.X, m_fX0 ), m_fX1 );
        kLocal.Y = Math.min( Math.max( kLocal.Y, m_fY0 ), m_fY1 );
        //System.err.println( "              " + kLocal.X + " " + kLocal.Y + " " + m_fX0 + " " + m_fX1 + " " + m_fY0 + " " + m_fY1 );

        /* Normalize: */
        kLocal.X = (kLocal.X - m_fX0) / m_fXRange;
        kLocal.Y = (kLocal.Y - m_fY0) / m_fYRange;
        //System.err.println( "              " + kLocal.X + " " + kLocal.Y );
        kLocal.Z = iZ / (float)(m_aiLocalImageExtents[2] - 1);
        
        return bClipped;
    }


    public void LocalToScreen(Vector3f kLocal, Vector3f kScreen)
    {
        kScreen.X = kLocal.X * m_fXRange + m_fX0;
        kScreen.Y = kLocal.Y * m_fYRange + m_fY0;
        kScreen.Z = kLocal.Z * (m_aiLocalImageExtents[2] - 1);      

        kScreen.X /= m_fZoomScale;
        kScreen.Y /= m_fZoomScale;

        float fHalfWidth = ((float) m_iWidth-1) / 2.0f;
        float fHalfHeight = ((float) m_iHeight-1) / 2.0f;
        float fX = kScreen.X * fHalfWidth + fHalfWidth;
        float fY = kScreen.Y * fHalfWidth + fHalfHeight;
        kScreen.Set( Math.round(fX), Math.round(fY), Math.round(kScreen.Z) );
    }

    public int getSlice()
    {
        return m_iSlice;
    }
    
    /**
     * Sets the local slice value.
     * @param fSlice
     */
    private void setSlice(float fSlice) {
        int iSlice = (int)fSlice;

        /* Check bounds: */
        if (iSlice > (m_aiLocalImageExtents[2] - 1)) {
            iSlice = m_aiLocalImageExtents[2] - 1;
        }

        if (iSlice < 0) {
            iSlice = 0;
        }

        if (iSlice != m_iSlice) {
            m_iSlice = iSlice;
        }        
    }

    private void zoom()
    {
        m_fZoomScale = Math.max( 0, m_fZoomScale );
        float fRMax = (m_fZoomScale*Math.max(m_fX, m_fY))/2.0f;
        float fUMax = fRMax * m_iHeight / m_iWidth;
        m_spkCamera.SetFrustum(-fRMax, fRMax,-fUMax, fUMax,1f,5.0f);
        m_pkRenderer.OnFrustumChange();
        float[] afData = new float[16];
        m_pkRenderer.SetConstantWVPMatrix(2, afData);
        m_kPVWMatrix.Set( afData[0], afData[1], afData[2], afData[3],
                afData[4], afData[5], afData[6], afData[7], 
                afData[8], afData[9], afData[10], afData[11], 
                afData[12], afData[13], afData[14], afData[15] );
        //System.err.println( m_kPVWMatrix.ToString() );
        //m_kParent.zoom(this);
        m_bModified = true;
        GetCanvas().display();
    }
    
    
    public float getZoom()
    {
        return m_fZoomScale;
    }
    
    public void setZoom( float value )
    {
        m_fZoomScale = value;
        zoom();
    }
    
    public int getWidth()
    {
        return m_iWidth;
    }
    
    public int getHeight()
    {
        return m_iHeight;
    }
    
}
