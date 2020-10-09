package editor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.TransferHandler;

import papafile.PapaTexture;

public class ImagePanel extends JPanel {
	private static final long serialVersionUID = 341369068060762310L;
	
	private boolean ignoreAlpha,luminance, tile, mouseInBounds, mouseHeld, showDXT;
	private int width, height, index, mouseX, mouseY;
	private PapaTexture image;
	private double scale=1;
	
	private BufferedImage ignoreAlphaCache;
	private int ignoreAlphaIndexCache = 0,ignoreAlphaModeCache = 1;
	private boolean ignoreAlphaLuminanceCache = false;
	
	public boolean dragDropEnabled = true;
	
	public final static int RGBA = 0, RED = 1, GREEN = 2, BLUE = 3, ALPHA = 4;
	private int mode = 0;
	
	private final Editor editor;
	
	public ImagePanel(Editor editor) {
		super();
		this.editor = editor;
		initialzeListeners();
		initializeTransferHandler();
	}

	private void initializeTransferHandler() {
		setTransferHandler(new TransferHandler() {

			private static final long serialVersionUID = 6432655799671782224L;

			public boolean canImport(TransferHandler.TransferSupport support) {
				if(!dragDropEnabled)
					return false;
	            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) || ! support.isDrop()) {
	                return false;
	            }
	            boolean moveSupported = (COPY & support.getSourceDropActions()) == COPY;

	            if (!moveSupported)
	                return false;
	            support.setDropAction(TransferHandler.COPY);
	            return true;
	        }

	        public boolean importData(TransferHandler.TransferSupport support) {
	            if (!canImport(support)) {
	                return false;
	            }
	            Transferable t = support.getTransferable();
	            try {
					@SuppressWarnings("unchecked")
					List<File> l =(List<File>)t.getTransferData(DataFlavor.javaFileListFlavor);
					editor.readAll(l.toArray(new File[l.size()]));
					
	            } catch (UnsupportedFlavorException e) {
	            	editor.showError("An unexpected error orccured:\n"+e.getMessage(),"Error", new Object[] {"Ok"},"Ok");
	            	e.printStackTrace();
	                return false;
	            } catch (IOException e) {
	            	editor.showError(e.getMessage(),"IO Error", new Object[] {"Ok"},"Ok");
	            	e.printStackTrace();
	                return false;
	            } catch (Exception ex) { //TODO this is only for testing
	            	editor.showError(ex.getMessage()+", "+ex.getClass(),"Error", new Object[] {"Ok"},"Ok");
	            	ex.printStackTrace();
	            	return false;
	            }

	            return true;
	        }
	    });
	}
	
	private void initialzeListeners() {
		addMouseWheelListener(new MouseWheelListener() { // mouse zoom support
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int sign = e.getWheelRotation()>0 ? -1 : 1;
				if(e.isControlDown()) 
					editor.ribbonPanel.changeZoom(sign);
				else if(e.isShiftDown()) {
					editor.horizontal.setValue((int) (editor.horizontal.getValue() + editor.horizontal.getVisibleAmount() / 2 * -sign));
					updateMouseLocation(e.getX(), e.getY());
				} else {
					editor.vertical.setValue((int) (editor.vertical.getValue() + editor.vertical.getVisibleAmount() / 2 *  -sign));
					updateMouseLocation(e.getX(), e.getY());
				}
			}
		});
		
		addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				mouseHeld=false;
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				if(e.getButton()==MouseEvent.BUTTON1)
					mouseHeld=true;
				updateMouseLocation(e.getX(), e.getY());
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				updateMouseLocation(-1, -1);
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {}
			
			@Override
			public void mouseClicked(MouseEvent e) {}
		});
		
		addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent e) {
				updateMouseLocation(e.getX(), e.getY());
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				mouseMoved(e);
			}
		});
		
		addComponentListener(new ComponentListener() {
			
			@Override
			public void componentShown(ComponentEvent e) {}
			
			@Override
			public void componentResized(ComponentEvent e) {
				updateScrollBars();
				
			}
			
			@Override
			public void componentMoved(ComponentEvent e) {}
			
			@Override
			public void componentHidden(ComponentEvent e) {}
		});
	}

	private void updateMouseLocation(int mouseX, int mouseY) {
		if(mouseX >= getTotalDrawWidth() || mouseX<0 || mouseY>=getTotalDrawHeight() || mouseY<0)
			mouseInBounds=false;
		else {
			mouseInBounds=true;
			this.mouseX = (int) (mouseX / scale + valueToPixels(editor.horizontal)) % width;
			this.mouseY = (int) (mouseY / scale + valueToPixels(editor.vertical)) % height;
		}
		editor.ribbonPanel.updateMouseLocation();
	}
	
	private int getTotalDrawWidth() {
		if(!isImageLoaded())
			return 0;
		return (int) (scale * width * (tile ? 2 : 1));
	}
	
	private int getTotalDrawHeight() {
		if(!isImageLoaded())
			return 0;
		return (int) (scale * height * (tile ? 2 : 1));
	}
	
	private void updateScrollBars() {
		updateScrollBars(valueToPixels(editor.horizontal),valueToPixels(editor.vertical));
	}
	
	private void updateScrollBars(double lastX, double lastY) {
		if(!isImageLoaded()) {
			editor.horizontal.setEnabled(false);
			editor.vertical.setEnabled(false);
			return;
		}
		int imageWidth = getTotalDrawWidth();
		int imageHeight = getTotalDrawHeight();

		if(imageWidth > getWidth()) {
			editor.horizontal.setEnabled(true);
			editor.horizontal.setMaximum(imageWidth);
			editor.horizontal.setVisibleAmount(getWidth());
			editor.horizontal.setValue(pixelsToValue(lastX));
			editor.horizontal.setUnitIncrement((int)Math.max(10*scale, 1));
		} else {
			editor.horizontal.setEnabled(false);
			editor.horizontal.setValue(0);
			editor.horizontal.setMaximum(0);
		}
		
		if(imageHeight > getHeight()) {
			editor.vertical.setEnabled(true);
			editor.vertical.setMaximum(imageHeight);
			editor.vertical.setVisibleAmount(getHeight());
			editor.vertical.setValue(pixelsToValue(lastY));
			editor.vertical.setUnitIncrement((int)Math.max(10*scale, 1));
		} else {
			editor.vertical.setEnabled(false);
			editor.vertical.setValue(0);
			editor.vertical.setMaximum(0);
		}
	}
	
	public void updateZoom() {
		int mouseX = (int) (this.mouseX * scale); // undo the transform from the previous scale
		int mouseY = (int) (this.mouseY * scale);
		double lastXVal = valueToPixels(editor.horizontal);
		double lastYVal = valueToPixels(editor.vertical);
		scale = editor.ribbonPanel.getZoomScale();
		updateMouseLocation(mouseX,mouseY);
		updateScrollBars(lastXVal,lastYVal);
		repaint();
	}

	public Color getColourUnderMouse() {
		if(!mouseInBounds)
			return new Color(1f,1f,1f,1f);
		return new Color(image.getImage(index).getRGB(mouseX % image.getImage(index).getWidth(), mouseY % image.getImage(index).getHeight()),true);
	}
	
	public int getMouseX() {
		return mouseX;
	}
	
	public int getMouseY() {
		return mouseY;
	}
	
	public boolean isMouseInBounds() {
		return mouseInBounds;
	}
	
	public boolean isMouseHeld() {
		return mouseHeld;
	}
	
	public boolean isImageLoaded() {
		return image != null;
	}
	
	public void unload() {
		setImage(null);
	}
	
	public void setImage(PapaTexture tex) {
		this.image = tex;
		this.ignoreAlphaCache=null;
		if(image!=null) {
			this.width = image.getWidth();
			this.height= image.getHeight();
		}
		updateScrollBars();
		repaint();
	}
	
	public void setMode(int mode) {
		this.mode = mode;
		repaint();
	}
	
	public void setLuminance(boolean b) {
		this.luminance = b;
		repaint();
	}
	
	public void setIgnoreAlpha(boolean b) {
		this.ignoreAlpha = b;
		repaint();
	}
	
	public void showDXT(boolean selected) {
		this.showDXT = selected;
		repaint();
	}
	
	public void setTile(boolean b) {
		this.tile = b;
		updateScrollBars();
		repaint();
	}
	
	public void setImageIndex(int index) {
		this.index=index;
		this.width=image.getWidth(index);
		this.height=image.getHeight(index);
		updateScrollBars();
		repaint();
	}
	
	private double valueToPixels(JScrollBar scroll) {
		return scroll.getValue() / scale;
	}
	
	private int pixelsToValue(double pixels) {
		return (int) (pixels * scale);
	}
	
	private BufferedImage getImageFromTexture() {
		if(luminance)
			return image.asLuminance(index);
		
		switch(mode) {
			case RGBA:
				return image.getImage(index);
			case RED:
				return image.asRed(index);
			case GREEN:
				return image.asGreen(index);
			case BLUE:
				return image.asBlue(index);
			case ALPHA:
				return image.asAlpha(index);
			default:
				throw new IndexOutOfBoundsException("Invalid mode, "+mode+" is not within bounds [0, 4]");
		}
	}
	
	public BufferedImage getImage() {
		BufferedImage draw = getImageFromTexture();
		if (ignoreAlpha && image.supportsAlpha()) {
			if(ignoreAlphaChacheInvalid()) {
				 // copy the data into a new BufferedImage with no alpha.
				BufferedImage tmp = removeAlpha(draw);
				
				ignoreAlphaCache = tmp;
				
				ignoreAlphaIndexCache = index;
				ignoreAlphaModeCache = mode;
				ignoreAlphaLuminanceCache = luminance;
			}
			draw = ignoreAlphaCache;
		}
		return draw;
	}
	
	public BufferedImage getFullImage() {
		BufferedImage draw = getImage();
		if(tile) {
			int w = draw.getWidth();
			int h = draw.getHeight();
			BufferedImage out = new BufferedImage(w * 2, h * 2, draw.getType());
			Graphics2D g2d = (Graphics2D) out.getGraphics();
			g2d.drawImage(draw, 0, 0, null);
			g2d.drawImage(draw, w, 0, null);
			g2d.drawImage(draw, 0, h, null);
			g2d.drawImage(draw, w, h, null);
			draw = out;
		}
		return draw;
	}
	
	private boolean ignoreAlphaChacheInvalid() {
		return ignoreAlphaCache==null || ignoreAlphaIndexCache!=index || ignoreAlphaModeCache!=mode || ignoreAlphaLuminanceCache!=luminance;
	}
	
	private BufferedImage removeAlpha(BufferedImage input) {
		BufferedImage tmp = new BufferedImage(input.getWidth(),input.getHeight(),BufferedImage.TYPE_INT_RGB);
		int[] data = new int[input.getWidth()*input.getHeight()];
		input.getRGB(0, 0, input.getWidth(), input.getHeight(), data, 0, input.getWidth());
		tmp.setRGB(0, 0, input.getWidth(), input.getHeight(), data, 0, input.getWidth());
		return tmp;
	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		if(image == null)
			return;
		
		Graphics2D g2d = (Graphics2D)g.create();
		g2d.setClip(0, 0, Math.min(getTotalDrawWidth(),getWidth()), Math.min(getTotalDrawHeight(),getHeight()));
		
		BufferedImage draw = getImage();
		
		if(! ignoreAlpha && image.supportsAlpha())
			drawCheckerboardGrid(g2d);
		
		AffineTransform newTransform = g2d.getTransform();
		newTransform.scale(scale, scale);
		newTransform.translate(-valueToPixels(editor.horizontal), -valueToPixels(editor.vertical));
		g2d.setTransform(newTransform);
		g2d.drawImage(draw, 0, 0, null);
		if(tile) {
			g2d.drawImage(draw, image.getWidth(index), 0, null);
			g2d.drawImage(draw, 0, image.getHeight(index), null);
			g2d.drawImage(draw, image.getWidth(index), image.getHeight(index), null);
		}
		
		if(showDXT)
			drawDXTZones((Graphics2D)g.create());
	}
	
	private void drawDXTZones(Graphics2D g) {
		g.setColor(new Color(0.1f,0.1f,0.1f));
		g.setXORMode(new Color(1f,1f,1f));
		if(scale<1)
			return;
		float spacing = (float) (4 * scale);
		
		int horizontal = (int) (image.getWidth(index) * scale);
		int vertical = (int) (image.getHeight(index) * scale);
		
		int xOrig = (int) (valueToPixels(editor.horizontal) * scale); // the origin of the image, corrected for scale
		int yOrig = (int) (valueToPixels(editor.vertical) * scale);
		
		int canvasWidth = getWidth();
		int canvasHeight = getHeight();
		// converting between coordinate spaces is hard.
		drawDXTZone(g, (int)(-xOrig % spacing),  (int) (-yOrig % spacing), Math.min(horizontal - xOrig,canvasWidth), Math.min(vertical - yOrig,canvasHeight), spacing);
		if(tile) {
			drawDXTZone(g,	Math.max((int)((horizontal - xOrig) % spacing), horizontal - xOrig),(int) (-yOrig % spacing),
							Math.min(2 * horizontal - xOrig,canvasWidth), Math.min(vertical - yOrig,canvasHeight), spacing);
			drawDXTZone(g,	(int)(-xOrig % spacing), Math.max((int)((vertical - yOrig) % spacing), vertical - yOrig),
							Math.min(horizontal - xOrig,canvasWidth), Math.min(2 * vertical - yOrig,canvasHeight), spacing);
			drawDXTZone(g,	Math.max((int)((horizontal - xOrig) % spacing), horizontal - xOrig), Math.max((int)((vertical - yOrig) % spacing), vertical - yOrig),
							Math.min(2 * horizontal - xOrig,canvasWidth), Math.min(2 * vertical - yOrig,canvasHeight), spacing);
		}
		
	}
	
	private void drawDXTZone(Graphics2D g2d,int startX, int startY, int endX, int endY, float change) {
		if(endY <= startY)
			return;
		
		for(float x = startX;x<endX;x+=change) {
			int xx = (int)x;
			g2d.drawLine(xx, startY, xx, endY);
		}
		for(float y =startY;y<endY;y+=change) {
			int yy = (int) y;
			g2d.drawLine(startX, yy, endX, yy);
		}
	}

	private void drawCheckerboardGrid(Graphics2D g) {
		int drawWidth = Math.min(getTotalDrawWidth(),getWidth());
		int drawHeight = Math.min(getTotalDrawHeight(),getHeight());
		
		for(int x = (int) (-valueToPixels(editor.horizontal) % 64);x<drawWidth;x+=64)
			for(int y = (int) (-valueToPixels(editor.vertical)% 64);y<drawHeight;y+=64)
				g.drawImage(editor.checkerboard, x, y, null);
	}
}
