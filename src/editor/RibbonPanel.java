package editor;

import java.awt.Color;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class RibbonPanel extends JPanel {
	
	private static final long serialVersionUID = 8964510055023743821L;
	
	private final int SLIDER_TICKS = 12;
	private double zoomScale=1;
	
	private JSlider zoomSlider;
	private JLabel zoomLabel, locationLabel,colourLabelRed,colourLabelGreen,colourLabelBlue,colourLabelAlpha, colourLabel, importLabel;
	
	private int importFileCount=0;
	
	private final Editor editor;
	
	public RibbonPanel(Editor editor) {
		this.editor = editor;
		
		setBorder(BorderFactory.createLineBorder(new Color(192, 192, 192)));
		
		SpringLayout layout = new SpringLayout();
		setLayout(layout);
		
		JButton plusButton = new JButton();
		plusButton.setIcon(editor.plusIcon);
		layout.putConstraint(SpringLayout.NORTH, plusButton, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, plusButton, 0, SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.SOUTH, plusButton, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, plusButton, -25, SpringLayout.EAST, this);
		plusButton.setOpaque(false);
		plusButton.setBorderPainted(false);
		plusButton.setFocusPainted(false);
		plusButton.setBackground(new Color(0f,0f,0f,0f));
		plusButton.setForeground(new Color(0f,0f,0f,0f));
		plusButton.addMouseListener(new ButtonMouseListener(plusButton));
		add(plusButton);
		
		plusButton.addActionListener((ActionEvent e) -> {
			changeZoom(1);
		});
		
		zoomSlider = new JSlider();
		layout.putConstraint(SpringLayout.NORTH, zoomSlider, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, zoomSlider, 0, SpringLayout.WEST, plusButton);
		layout.putConstraint(SpringLayout.SOUTH, zoomSlider, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, zoomSlider, -5 - SLIDER_TICKS*10, SpringLayout.EAST, this);
		zoomSlider.setMajorTickSpacing(1);
		zoomSlider.setMaximum(SLIDER_TICKS);
		zoomSlider.setValue(SLIDER_TICKS/2);
		zoomSlider.setPaintTicks(true);
		zoomSlider.setSnapToTicks(true);
		add(zoomSlider);
		
		zoomSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updateZoomFromSlider();
			}
		});
		
		JButton minusButton = new JButton();
		minusButton.setIcon(editor.minusIcon);
		layout.putConstraint(SpringLayout.NORTH, minusButton, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, minusButton, 0, SpringLayout.WEST, zoomSlider);
		layout.putConstraint(SpringLayout.SOUTH, minusButton, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, minusButton, -25, SpringLayout.WEST, zoomSlider);
		minusButton.setOpaque(false);
		minusButton.setBorderPainted(false);
		minusButton.setFocusPainted(false);
		minusButton.setBackground(new Color(0f,0f,0f,0f));
		minusButton.setForeground(new Color(0f,0f,0f,0f));
		minusButton.addMouseListener(new ButtonMouseListener(minusButton));
		add(minusButton);
		
		minusButton.addActionListener((ActionEvent e) -> {
			changeZoom(-1);
		});
		
		JSeparator sep1 = new JSeparator();
		layout.putConstraint(SpringLayout.NORTH, sep1, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, sep1, 0, SpringLayout.WEST, minusButton); // -20
		layout.putConstraint(SpringLayout.SOUTH, sep1, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, sep1, -2, SpringLayout.WEST, minusButton);
		sep1.setOrientation(JSlider.VERTICAL);
		add(sep1);
		
		zoomLabel = new JLabel();
		layout.putConstraint(SpringLayout.NORTH, zoomLabel, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, zoomLabel, 0, SpringLayout.WEST, sep1);
		layout.putConstraint(SpringLayout.SOUTH, zoomLabel, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, zoomLabel, -60, SpringLayout.WEST, sep1);
		zoomLabel.setText("100%");
		add(zoomLabel);
		
		JSeparator sep2 = new JSeparator();
		layout.putConstraint(SpringLayout.NORTH, sep2, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, sep2, -5, SpringLayout.WEST, zoomLabel);
		layout.putConstraint(SpringLayout.SOUTH, sep2, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, sep2, -7, SpringLayout.WEST, zoomLabel);
		sep2.setOrientation(JSlider.VERTICAL);
		add(sep2);
		
		locationLabel = new JLabel();
		layout.putConstraint(SpringLayout.NORTH, locationLabel, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, locationLabel, 0, SpringLayout.WEST, sep2);
		layout.putConstraint(SpringLayout.SOUTH, locationLabel, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, locationLabel, -68, SpringLayout.WEST, sep2);
		add(locationLabel);
		
		JSeparator sep3 = new JSeparator();
		layout.putConstraint(SpringLayout.NORTH, sep3, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, sep3, -5, SpringLayout.WEST, locationLabel);
		layout.putConstraint(SpringLayout.SOUTH, sep3, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, sep3, -7, SpringLayout.WEST, locationLabel);
		sep3.setOrientation(JSlider.VERTICAL);
		add(sep3);
		
		final JPopupMenu popupColour = new JPopupMenu();
        JMenuItem item = new JMenuItem("Copy as Hex");
        item.setMnemonic(KeyEvent.VK_H);
        item.addActionListener((ActionEvent e)-> {
        	Color c = colourLabel.getBackground();
        	int i = (c.getRed()<<16) | (c.getGreen()<<8) | c.getBlue();
        	String hex = Integer.toHexString(i);
        	hex = "000000".substring(hex.length()) + hex;
        	StringSelection s = new StringSelection(hex);
        	editor.clipboard.setContents(s, editor.clipboardOwner);
        });
        popupColour.add(item);
        
        item = new JMenuItem("Copy as RGB");
        item.setMnemonic(KeyEvent.VK_R);
        item.addActionListener((ActionEvent e)-> {
        	Color c = colourLabel.getBackground();
        	String rgb = c.getRed()+" "+c.getGreen()+" "+c.getBlue();
        	StringSelection s = new StringSelection(rgb);
        	editor.clipboard.setContents(s, editor.clipboardOwner);
        });
        popupColour.add(item);
		
		colourLabel = new JLabel();
		layout.putConstraint(SpringLayout.NORTH, colourLabel, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, colourLabel, 0, SpringLayout.WEST, sep3);
		layout.putConstraint(SpringLayout.SOUTH, colourLabel, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, colourLabel, -20, SpringLayout.WEST, sep3);
		colourLabel.setOpaque(true);
		//colourLabel.setBorder(BorderFactory.createLineBorder(new Color(192, 192, 192)));;
		add(colourLabel);
		
		colourLabel.addMouseListener(new MouseListener() {
			boolean armed = false;
			@Override
			public void mouseReleased(MouseEvent e) {
				if(armed) {
					Point m = colourLabel.getMousePosition();
					popupColour.show(colourLabel, m.x ,m.y);
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				armed = true;
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				armed = false;
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {}
			
			@Override
			public void mouseClicked(MouseEvent e) {}
		});
		
		// i hated all the monospaced fonts so i'm just making 4 JLabels instead.
		
		colourLabelAlpha = new JLabel();
		layout.putConstraint(SpringLayout.NORTH, colourLabelAlpha, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, colourLabelAlpha, 0, SpringLayout.WEST, colourLabel);
		layout.putConstraint(SpringLayout.SOUTH, colourLabelAlpha, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, colourLabelAlpha, -45, SpringLayout.WEST, colourLabel);
		add(colourLabelAlpha);
		
		colourLabelBlue = new JLabel();
		layout.putConstraint(SpringLayout.NORTH, colourLabelBlue, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, colourLabelBlue, 0, SpringLayout.WEST, colourLabelAlpha);
		layout.putConstraint(SpringLayout.SOUTH, colourLabelBlue, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, colourLabelBlue, -45, SpringLayout.WEST, colourLabelAlpha);
		add(colourLabelBlue);
		
		colourLabelGreen = new JLabel();
		layout.putConstraint(SpringLayout.NORTH, colourLabelGreen, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, colourLabelGreen, 0, SpringLayout.WEST, colourLabelBlue);
		layout.putConstraint(SpringLayout.SOUTH, colourLabelGreen, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, colourLabelGreen, -45, SpringLayout.WEST, colourLabelBlue);
		add(colourLabelGreen);
		
		colourLabelRed = new JLabel();
		layout.putConstraint(SpringLayout.NORTH, colourLabelRed, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, colourLabelRed, 0, SpringLayout.WEST, colourLabelGreen);
		layout.putConstraint(SpringLayout.SOUTH, colourLabelRed, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, colourLabelRed, -45, SpringLayout.WEST, colourLabelGreen);
		add(colourLabelRed);
		
		JSeparator sep4 = new JSeparator();
		layout.putConstraint(SpringLayout.NORTH, sep4, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, sep4, -5, SpringLayout.WEST, colourLabelRed);
		layout.putConstraint(SpringLayout.SOUTH, sep4, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, sep4, -7, SpringLayout.WEST, colourLabelRed);
		sep4.setOrientation(JSlider.VERTICAL);
		add(sep4);
		
		importLabel = new JLabel("");
		layout.putConstraint(SpringLayout.NORTH, importLabel, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, importLabel, -5, SpringLayout.WEST, sep4);
		layout.putConstraint(SpringLayout.SOUTH, importLabel, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, importLabel, 5, SpringLayout.WEST, this);
		add(importLabel);
		
		setColourLabel(Color.black);
	}
	
	private void updateZoomFromSlider() {
		zoomScale = Math.pow(2, (zoomSlider.getValue()-SLIDER_TICKS/2));
		if(zoomScale>=0.25) // no decimals
			zoomLabel.setText((int)(100*zoomScale)+"%");
		else
			zoomLabel.setText((100*zoomScale)+"%");
		editor.imagePanel.updateZoom();
	}
	
	public void changeZoom(int change) {
		zoomSlider.setValue(zoomSlider.getValue() + change);
	}
	
	public double getZoomScale() {
		return zoomScale;
	}
	
	public void updateMouseLocation() {
		if(editor.imagePanel.isMouseInBounds()) {
			locationLabel.setText(editor.imagePanel.getMouseX()+", "+editor.imagePanel.getMouseY());
			if(editor.imagePanel.isMouseHeld()) {
				Color c = editor.imagePanel.getColourUnderMouse();
				setColourLabel(c);
			}
		} else
			locationLabel.setText("");
		
	}
	
	private void setColourLabel(Color c) {
		colourLabelRed.setText("R="+c.getRed());
		colourLabelGreen.setText("G="+c.getGreen());
		colourLabelBlue.setText("B="+c.getBlue());
		colourLabelAlpha.setText("A="+c.getAlpha());
		colourLabel.setBackground(new Color(c.getRGB(),false));
	}
	
	private class ButtonMouseListener implements MouseListener {
		private JButton j;
		public ButtonMouseListener(JButton j) {
			this.j=j;
		}
		public void mouseReleased(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {}
		public void mouseClicked(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {
			j.setBorderPainted(false);
		}
		
		@Override
		public void mouseEntered(MouseEvent e) {
			j.setBorderPainted(true);
		}
		
	};
	
	public void setNumberOfFoundFiles(int number) {
		importFileCount=number;
		importLabel.setText("Scanning... "+number+" files found");
	}
	
	public void startImport(int number) {
		importFileCount = number;
		setNumberProcessedFiles(0);
	}
	
	public void setNumberProcessedFiles(int number) {
		if(number > importFileCount)
			number = 0; // parallel processing fix (next import starts before the last finished on multi import)
		importLabel.setText("Processed: "+String.format("%s", number)+" of "+String.format("%s", importFileCount));
	}
	
	public void endImport() {
		importLabel.setText("");
	}
}