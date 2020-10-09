package com.github.luther_1.ptexedit.editor;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SpringLayout;

import com.github.luther_1.ptexedit.papafile.PapaFile;
import com.github.luther_1.ptexedit.papafile.PapaTexture;


public class ConfigPanelBottom extends JPanel {

	private static final long serialVersionUID = 1721448070098959177L;
	
	private JLabel versionValueLabel, fileSizeValueLabel, imagesValueLabel, widthValueLabel, heightValueLabel, formatValueLabel, mipmapsValueLabel;
	
	private final Color defaultTextColour;
	
	public ConfigPanelBottom() {

		setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(192, 192, 192)),"Info"));
		
		SpringLayout layout = new SpringLayout();
		setLayout(layout);
		
		JLabel fileInfoLabel = new JLabel("File Info:");
		layout.putConstraint(SpringLayout.NORTH, fileInfoLabel, 5, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, fileInfoLabel, 10, SpringLayout.WEST, this);
		add(fileInfoLabel);
		
		JLabel versionLabel = new JLabel("Version:");
		layout.putConstraint(SpringLayout.NORTH, versionLabel, 30, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, versionLabel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, versionLabel, 90, SpringLayout.WEST, this);
		add(versionLabel);
		
		versionValueLabel = new JLabel("");
		layout.putConstraint(SpringLayout.NORTH, versionValueLabel, 0, SpringLayout.NORTH, versionLabel);
		layout.putConstraint(SpringLayout.WEST, versionValueLabel, 100, SpringLayout.WEST, this);
		add(versionValueLabel);
		
		JLabel fileSizeLabel = new JLabel("File Size:");
		layout.putConstraint(SpringLayout.NORTH, fileSizeLabel, 25, SpringLayout.NORTH, versionValueLabel);
		layout.putConstraint(SpringLayout.WEST, fileSizeLabel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, fileSizeLabel, 90, SpringLayout.WEST, this);
		add(fileSizeLabel);
		
		fileSizeValueLabel = new JLabel("");
		layout.putConstraint(SpringLayout.NORTH, fileSizeValueLabel, 0, SpringLayout.NORTH, fileSizeLabel);
		layout.putConstraint(SpringLayout.WEST, fileSizeValueLabel, 100, SpringLayout.WEST, this);
		add(fileSizeValueLabel);
		
		JLabel imagesLabel = new JLabel("Images:");
		layout.putConstraint(SpringLayout.NORTH, imagesLabel, 25, SpringLayout.NORTH, fileSizeLabel);
		layout.putConstraint(SpringLayout.WEST, imagesLabel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, imagesLabel, 90, SpringLayout.WEST, this);
		add(imagesLabel);
		
		imagesValueLabel = new JLabel("");
		layout.putConstraint(SpringLayout.NORTH, imagesValueLabel, 0, SpringLayout.NORTH, imagesLabel);
		layout.putConstraint(SpringLayout.WEST, imagesValueLabel, 100, SpringLayout.WEST, this);
		add(imagesValueLabel);
		
		JSeparator separator_3 = new JSeparator();
		layout.putConstraint(SpringLayout.NORTH, separator_3, 13, SpringLayout.SOUTH, imagesLabel);
		layout.putConstraint(SpringLayout.WEST, separator_3, 5, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.SOUTH, separator_3, 15, SpringLayout.SOUTH, imagesLabel);
		layout.putConstraint(SpringLayout.EAST, separator_3, -5, SpringLayout.EAST, this);
		add(separator_3);
		
		JLabel imageInfoLabel = new JLabel("Image Info:");
		layout.putConstraint(SpringLayout.NORTH, imageInfoLabel, 10, SpringLayout.SOUTH, separator_3);
		layout.putConstraint(SpringLayout.WEST, imageInfoLabel, 10, SpringLayout.WEST, this);
		add(imageInfoLabel);
		
		JLabel widthLabel = new JLabel("Width:");
		layout.putConstraint(SpringLayout.NORTH, widthLabel, 35, SpringLayout.NORTH, separator_3);
		layout.putConstraint(SpringLayout.WEST, widthLabel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, widthLabel, 90, SpringLayout.WEST, this);
		add(widthLabel);
		
		widthValueLabel = new JLabel("");
		layout.putConstraint(SpringLayout.NORTH, widthValueLabel, 0, SpringLayout.NORTH, widthLabel);
		layout.putConstraint(SpringLayout.WEST, widthValueLabel, 100, SpringLayout.WEST, this);
		add(widthValueLabel);
		
		JLabel heightLabel = new JLabel("Height:");
		layout.putConstraint(SpringLayout.NORTH, heightLabel, 25, SpringLayout.NORTH, widthLabel);
		layout.putConstraint(SpringLayout.WEST, heightLabel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, heightLabel, 90, SpringLayout.WEST, this);
		add(heightLabel);
		
		heightValueLabel = new JLabel("");
		layout.putConstraint(SpringLayout.NORTH, heightValueLabel, 0, SpringLayout.NORTH, heightLabel);
		layout.putConstraint(SpringLayout.WEST, heightValueLabel, 100, SpringLayout.WEST, this);
		add(heightValueLabel);
		
		JLabel formatLabel = new JLabel("Format:");
		layout.putConstraint(SpringLayout.NORTH, formatLabel, 25, SpringLayout.NORTH, heightLabel);
		layout.putConstraint(SpringLayout.WEST, formatLabel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, formatLabel, 90, SpringLayout.WEST, this);
		add(formatLabel);
		
		formatValueLabel = new JLabel("");
		layout.putConstraint(SpringLayout.NORTH, formatValueLabel, 0, SpringLayout.NORTH, formatLabel);
		layout.putConstraint(SpringLayout.WEST, formatValueLabel, 100, SpringLayout.WEST, this);
		add(formatValueLabel);
		
		JLabel mipmapsLabel = new JLabel("Mipmaps:");
		layout.putConstraint(SpringLayout.NORTH, mipmapsLabel, 25, SpringLayout.NORTH, formatLabel);
		layout.putConstraint(SpringLayout.WEST, mipmapsLabel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, mipmapsLabel, 90, SpringLayout.WEST, this);
		add(mipmapsLabel);
		
		mipmapsValueLabel = new JLabel("");
		layout.putConstraint(SpringLayout.NORTH, mipmapsValueLabel, 0, SpringLayout.NORTH, mipmapsLabel);
		layout.putConstraint(SpringLayout.WEST, mipmapsValueLabel, 100, SpringLayout.WEST, this);
		add(mipmapsValueLabel);
		
		defaultTextColour = mipmapsValueLabel.getForeground();
		
	}
	
	public void applySettings(PapaFile pf, int image, PapaTexture tex, boolean changed) {
		if(changed) {
			versionValueLabel.setText(pf.getVersion());
			if(pf.getFileSize()<1024)
				fileSizeValueLabel.setText(pf.getFileSize()+" bytes");
			else if (pf.getFileSize()<1048576)
				fileSizeValueLabel.setText(pf.getFileSize()/1024+" KB");
			else
				fileSizeValueLabel.setText(String.format("%.2f",(double)pf.getFileSize()/1048576d)+" MB");
			imagesValueLabel.setText(""+pf.getNumTextures());
		}
		applySettings(tex);
	}
	
	private void applySettings(PapaTexture tex) {
		if(tex==null) {
			widthValueLabel.setText("");
			heightValueLabel.setText("");
			formatValueLabel.setText("");
			mipmapsValueLabel.setText("");
			return;
		}
		PapaTexture t = tex;
		setTextColour(defaultTextColour);
		if(t.isLinked()) {
			if(t.linkValid()) {
				t = t.getLinkedTexture();
			} else {
				widthValueLabel.setText("Invalid Link");
				heightValueLabel.setText("Invalid Link");
				formatValueLabel.setText("Invalid Link");
				mipmapsValueLabel.setText("Invalid Link");
				setTextColour(Color.red);
				return;
			}
		}
		
		widthValueLabel.setText(""+t.getWidth());
		heightValueLabel.setText(""+t.getHeight());
		formatValueLabel.setText(t.getFormat());
		mipmapsValueLabel.setText(""+t.getMips());
	}
	
	private void setTextColour(Color c) {
		widthValueLabel.setForeground(c);
		heightValueLabel.setForeground(c);
		formatValueLabel.setForeground(c);
		mipmapsValueLabel.setForeground(c);
	}
	
	public void unload() {
		versionValueLabel.setText("");
		fileSizeValueLabel.setText("");
		imagesValueLabel.setText("");
		widthValueLabel.setText("");
		heightValueLabel.setText("");
		formatValueLabel.setText("");
		mipmapsValueLabel.setText("");
	}
}
