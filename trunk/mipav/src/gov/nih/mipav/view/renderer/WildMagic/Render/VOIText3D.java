package gov.nih.mipav.view.renderer.WildMagic.Render;

import gov.nih.mipav.model.structures.VOI;
import gov.nih.mipav.model.structures.VOIText;
import gov.nih.mipav.view.renderer.WildMagic.VOI.ScreenCoordinateListener;
import gov.nih.mipav.view.renderer.WildMagic.VOI.VOIManager;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Vector;

import WildMagic.LibFoundation.Mathematics.Vector3f;
import WildMagic.LibGraphics.Rendering.Renderer;

public class VOIText3D extends LocalVolumeVOI
{
    /**  */
    private static final long serialVersionUID = 9019280375854415276L;
    

    /**
     * The descriptors for the font which start at PLAIN and are additive (PLAIN + BOLD = bold) (BOLD+ITALIC = bold and
     * italic) etc.
     */
    private int fontDescriptors = Font.PLAIN;

    /** The name (or type) of the font. */
    private String fontName = "Serif";

    /** The size of the font (half-sizes not allowed...int). */
    private int fontSize = 12;

    /** This must be kept separate (but parallel) to the VOI color. */
    private Color textColor = Color.WHITE;

    /** The color used to draw behind the main text (so that the text will stand out)*/
    private Color backgroundColor = Color.BLACK;
    
    /**
     * The font is stored here so that it does not have to be reallocated on each redraw. It is only new'd at the
     * beginning or if the fontDescriptors variable changes
     */
    private Font textFont = new Font(fontName, fontDescriptors, fontSize);

    /** The String to be displayed. */
    private String textString = new String();
    
    /** The note stored in VOIText, has same font as textString*/
    private String noteString = new String();
    
    /** If this is set to true, a draggable arrow will be displayed */
    private boolean useMarker = true;
    

    public VOIText3D( VOIManager parent, ScreenCoordinateListener kContext, int iOrientation, int iType, int iSType, Vector<Vector3f> kLocal, int iZ )
    {
        super(parent, kContext, iOrientation, iType, iSType, kLocal, iZ );
        m_iVOIType = VOI.ANNOTATION;
        m_bClosed = false;
    }  

    public VOIText3D(VOIManager parent, ScreenCoordinateListener kContext, int iOrientation, int iType, Vector<Vector3f> kLocal, boolean bIsFile)
    {
        super(parent,kContext,iOrientation,iType,kLocal,bIsFile);
        m_iVOIType = VOI.ANNOTATION;
        m_bClosed = false;
    }
    
    private static int xCor(int len, double dir) {return (int)(len * Math.sin(dir));}

    private static int yCor(int len, double dir) {return (int)(len * Math.cos(dir));}

    public void add( VOIManager parent, int iPos, Vector3f kNewPoint, boolean bIsFile  ) {}
    public void add(VOIManager parent, Vector3f kNewPoint, boolean bIsFile) {}

    public VOIText3D Clone( )
    {
        return new VOIText3D( m_kParent, m_kDrawingContext, m_iOrientation, m_iVOIType, this, true );
    }

    public VOIText3D Clone( int iZ )
    {
        return new VOIText3D( m_kParent, m_kDrawingContext, m_iOrientation, m_iVOIType, m_iVOISpecialType, this, iZ );
    }


    public boolean contains(int iOrientation, int iX, int iY, int iZ ) {
        if ( iZ != slice() || iOrientation != m_iOrientation )
        {
            return false;
        }
        if ( nearLine( iX, iY, iZ ) )
        {
            return true;
        }
        return nearPoint( iX, iY, iZ );
    }


    public void copyInfo( VOIText vt )
    {
        fontDescriptors = vt.getFontDescriptors();
        fontName = vt.getFontName();
        fontSize = vt.getFontSize();
        textColor = vt.getColor();
        backgroundColor = vt.getBackgroundColor();
        textString = vt.getText();
        noteString = vt.getNote();
        useMarker = vt.useMarker();
    }

    public void drawSelf(float[] resols, int[] unitsOfMeasure, Graphics g, int slice, int orientation ) {

        Vector3f kScreen = m_kDrawingContext.fileToScreen( get(0) );
        int xS = Math.round(kScreen.X);
        int yS = Math.round(kScreen.Y);

        kScreen = m_kDrawingContext.fileToScreen( get(1) );
        int xS2 = Math.round(kScreen.X);
        int yS2 = Math.round(kScreen.Y);

        // draw the arrow if useMarker is true
        if (useMarker) {
            // determine the width/height of the TEXT (for marker line location)
            int width = (g.getFontMetrics(textFont).stringWidth(textString));
            int ascentValue = (int) (g.getFontMetrics(textFont).getStringBounds(textString, g).getHeight() / 2);

            int markerX = xS;
            int markerY = yS;

            if (xS2 > (xS + width)) {
                markerX = xS + width;
            } else if (xS2 <= xS) {
                markerX = xS - 2;
            } else {
                markerX = xS + width/2;
            }

            if (yS2 > yS) {
                markerY = yS + 3;
            } else if (yS2 <= (yS - ascentValue)) {
                markerY = yS - ascentValue - 5;
            } else {
                markerY = yS - ascentValue/2;
            }

            this.drawArrow((Graphics2D)g, markerX, markerY, xS2, yS2, .1f);
        } //arrow not off
        if ((textFont != null) && (textFont.getName() == fontName) && (textFont.getStyle() == fontDescriptors)) {
            textFont = textFont.deriveFont(fontSize);

        } else {
            textFont = new Font(fontName, fontDescriptors, (fontSize));
        }

        Font previousFont = g.getFont();

        g.setFont(textFont);

        if (active == true) {
            g.setColor(Color.RED);            
            g.drawString(textString, xS, yS + 1);
            g.drawString(textString, xS + 1, yS);
        } else {
            g.setColor(backgroundColor);
            g.drawString(textString, xS + 1, yS);
            g.drawString(textString, xS - 1, yS);
            g.drawString(textString, xS, yS - 1);
            g.drawString(textString, xS, yS + 1);
        }


        g.setColor(textColor);
        g.drawString(textString, xS, yS);
        g.setFont(previousFont);

    }
    public int getType()
    {
        return m_iVOIType;
    }
    public LocalVolumeVOI split ( Vector3f kStartPt, Vector3f kEndPt )
    {
        return null;
    }

    protected void drawVOI( Renderer kRenderer, int iSlice, float[] afResolutions, int[] aiUnits, VolumeVOI kVolumeVOI, Vector3f kVolumeScale, Vector3f kTranslate, int iOrientation, int[] aiAxisOrder )
    {             
        if ( iSlice == slice() )
        {
            drawVOIText( kRenderer, kVolumeVOI );
        }
    }

    private void drawArrow(Graphics2D g2d, int xCenter, int yCenter, int x, int y, float stroke) {
        double aDir=Math.atan2(xCenter-x,yCenter-y);
        
        g2d.setColor(backgroundColor);
        g2d.drawLine(x + 1, y + 1, xCenter + 1, yCenter + 1);
        g2d.drawLine(x - 1, y - 1, xCenter - 1, yCenter - 1);
                
                        // make the arrow head solid even if dash pattern has been specified
        Polygon tmpPoly=new Polygon();
        Polygon backPoly1 = new Polygon();
        Polygon backPoly2 = new Polygon();
        Polygon backPoly3 = new Polygon();
        Polygon backPoly4 = new Polygon();
        
        
        int i1=12+(int)(stroke*2);
        int i2=6+(int)stroke;                           // make the arrow head the same size regardless of the length length
        tmpPoly.addPoint(x,y);                          // arrow tip
        backPoly1.addPoint(x + 1, y);
        backPoly2.addPoint(x - 1, y);
        backPoly3.addPoint(x, y + 1);
        backPoly4.addPoint(x, y - 1);
        
        int x2 = x+xCor(i1,aDir+.5);
        int y2 = y+yCor(i1,aDir+.5);
        tmpPoly.addPoint(x2, y2);
        backPoly1.addPoint(x2 + 1, y2);
        backPoly2.addPoint(x2 - 1, y2);
        backPoly3.addPoint(x2, y2 + 1);
        backPoly4.addPoint(x2, y2 - 1);
        
        
        int x3 = x+xCor(i2,aDir);
        int y3 = y+yCor(i2,aDir);
        tmpPoly.addPoint(x3, y3);
        backPoly1.addPoint(x3 + 1, y3);
        backPoly2.addPoint(x3 - 1, y3);
        backPoly3.addPoint(x3, y3 + 1);
        backPoly4.addPoint(x3, y3 - 1);
        
        int x4 = x+xCor(i1,aDir-.5);
        int y4 = y+yCor(i1,aDir-.5);
        tmpPoly.addPoint(x4, y4);
        backPoly1.addPoint(x4 + 1, y4 + 1);
        backPoly2.addPoint(x4 - 1, y4 - 1);
        backPoly1.addPoint(x4, y4 + 1);
        backPoly2.addPoint(x4, y4 - 1);
        
        tmpPoly.addPoint(x,y);                          // arrow tip
        backPoly1.addPoint(x + 1, y + 1);
        backPoly2.addPoint(x - 1, y - 1);
        backPoly3.addPoint(x, y + 1);
        backPoly4.addPoint(x, y - 1);        
        
        g2d.setStroke(new BasicStroke(1f)); 
        g2d.drawPolygon(backPoly1);
        //g2d.fillPolygon(backPoly1);
        g2d.drawPolygon(backPoly2);
        //g2d.fillPolygon(backPoly2);
        g2d.drawPolygon(backPoly3);
        g2d.drawPolygon(backPoly4);
        
        
        g2d.setColor(textColor);
        
        g2d.drawLine(x,y,xCenter,yCenter);
        
        
        g2d.drawPolygon(tmpPoly);
        g2d.fillPolygon(tmpPoly);                       // remove this line to leave arrow head unpainted
     }
    
    private void drawVOIText( Renderer kRenderer, VolumeVOI kVolumeVOI )
    {
        Vector3f kScreen = m_kDrawingContext.fileToScreen( get(1) );
        char[] acText = label.toCharArray();
        int[] aiSize = kRenderer.GetSizeOnScreen( acText );
        drawText( kRenderer, (int)kScreen.X - aiSize[0]/2, (int)kScreen.Y, kVolumeVOI.getColor(), acText );      
     }

}
