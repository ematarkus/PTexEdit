package com.github.luther_1.ptexedit.editor;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.github.luther_1.ptexedit.papafile.PapaFile;
import com.github.luther_1.ptexedit.papafile.PapaTexture;

public class ConfigPanelTop extends JPanel {
	
	private static final long serialVersionUID = 14053510615893605L;
	private JTextField imageName, filePath;
	
	private JSpinner spinnerImage, spinnerMipmap;
	
	private JCheckBox srgb;
	
	private JButton resizeButton, changeFormatButton;
	
	private JLabel filePathLabel;
	
	private final Border defaultButtonBorder;
	
	private final Editor editor;
	
	public ConfigPanelTop(Editor editor) {
		this.editor = editor;
		
		setBackground(new Color(240, 240, 240));
		setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(192, 192, 192)),"Settings"));
		
		SpringLayout layout = new SpringLayout();
		setLayout(layout);
		
		spinnerImage = new JSpinner();
		layout.putConstraint(SpringLayout.NORTH, spinnerImage, 5, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, spinnerImage, -10, SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.SOUTH, spinnerImage, 25, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, spinnerImage, 70, SpringLayout.WEST, this);
		add(spinnerImage);
		spinnerImage.setEnabled(false);
		
		spinnerMipmap = new JSpinner();
		layout.putConstraint(SpringLayout.NORTH, spinnerMipmap, 5, SpringLayout.SOUTH, spinnerImage);
		layout.putConstraint(SpringLayout.WEST, spinnerMipmap, 70, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.SOUTH, spinnerMipmap, 25, SpringLayout.SOUTH, spinnerImage);
		layout.putConstraint(SpringLayout.EAST, spinnerMipmap, -10, SpringLayout.EAST, this);
		spinnerMipmap.setEnabled(false);
		add(spinnerMipmap);
		
		imageName = new JTextField();
		layout.putConstraint(SpringLayout.NORTH, imageName, 5, SpringLayout.SOUTH, spinnerMipmap);
		layout.putConstraint(SpringLayout.WEST, imageName, 70, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.SOUTH, imageName, 25, SpringLayout.SOUTH, spinnerMipmap);
		layout.putConstraint(SpringLayout.EAST, imageName, -10, SpringLayout.EAST, this);
		imageName.setColumns(1);
		imageName.setEnabled(false);
		add(imageName);
		
		imageName.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				update();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				update();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				update();
			}
			
			private void update() {
				if(editor.activeTexture==null)
					return;
				String name = imageName.getText();
				if(name.equals(""))
					name = "<no name>";
				editor.activeTexture.setName(name);
				refreshActiveTexture();
			}
		});
		
		defaultButtonBorder = imageName.getBorder();
		
		filePath = new JTextField();
		layout.putConstraint(SpringLayout.NORTH, filePath, 5, SpringLayout.SOUTH, imageName);
		layout.putConstraint(SpringLayout.WEST, filePath, 70, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.SOUTH, filePath, 25, SpringLayout.SOUTH, imageName);
		layout.putConstraint(SpringLayout.EAST, filePath, -10, SpringLayout.EAST, this);
		filePath.setColumns(1);
		filePath.setEnabled(false);
		add(filePath);
		
		filePath.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				update();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				update();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				update();
			}
			
			private void update() {
				if(editor.activeFile==null || ! filePath.isEnabled())
					return;
				String path = filePath.getText();
				if(path.equals(""))
					path = "<no name>";
				editor.activeFile.setLocationRelative(path);
				refreshActiveFileLinks();
			}
		});
		
		srgb = new JCheckBox("SRGB");
		layout.putConstraint(SpringLayout.NORTH, srgb, 5, SpringLayout.SOUTH, filePath);
		layout.putConstraint(SpringLayout.WEST, srgb, 15, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.SOUTH, srgb, 25, SpringLayout.SOUTH, filePath);
		layout.putConstraint(SpringLayout.EAST, srgb, -10, SpringLayout.EAST, this);
		srgb.setEnabled(false);
		srgb.addActionListener((ActionEvent e) -> {
			editor.activeTexture.setSRGB(srgb.isSelected());
		});
		add(srgb);
		
		//TODO this requires changes to the papafile package
		resizeButton = new JButton("Resize"); 
		layout.putConstraint(SpringLayout.NORTH, resizeButton, 5, SpringLayout.SOUTH, srgb);
		layout.putConstraint(SpringLayout.WEST, resizeButton, 15, SpringLayout.WEST, this);
		resizeButton.setEnabled(false);
		//add(resizeButton);
		
		changeFormatButton = new JButton("Change Format");
		layout.putConstraint(SpringLayout.NORTH, changeFormatButton, 5, SpringLayout.SOUTH, srgb);
		layout.putConstraint(SpringLayout.EAST, changeFormatButton, -10, SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.WEST, changeFormatButton, 10, SpringLayout.EAST, resizeButton);
		changeFormatButton.setEnabled(false);
		//add(changeFormatButton);
		
		JLabel imageSpinnerLabel = new JLabel("Image:");
		layout.putConstraint(SpringLayout.NORTH, imageSpinnerLabel, 3, SpringLayout.NORTH, spinnerImage);
		layout.putConstraint(SpringLayout.WEST, imageSpinnerLabel, 20, SpringLayout.WEST, this);
		add(imageSpinnerLabel);
		
		JLabel mipmapSpinerLabel = new JLabel("Mipmap:");
		layout.putConstraint(SpringLayout.NORTH, mipmapSpinerLabel, 3, SpringLayout.NORTH, spinnerMipmap);
		layout.putConstraint(SpringLayout.WEST, mipmapSpinerLabel, 20, SpringLayout.WEST, this);
		add(mipmapSpinerLabel);
		
		JLabel imageNameLabel = new JLabel("Name:");
		layout.putConstraint(SpringLayout.NORTH, imageNameLabel, 3, SpringLayout.NORTH, imageName);
		layout.putConstraint(SpringLayout.WEST, imageNameLabel, 20, SpringLayout.WEST, this);
		add(imageNameLabel);
		
		filePathLabel = new JLabel("Filepath:");
		layout.putConstraint(SpringLayout.NORTH, filePathLabel, 3, SpringLayout.NORTH, filePath);
		layout.putConstraint(SpringLayout.WEST, filePathLabel, 20, SpringLayout.WEST, this);
		add(filePathLabel);
		
		filePathLabel.setEnabled(false);
		
		spinnerMipmap.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if(editor.activeFile!=null)
					editor.imagePanel.setImageIndex(getSelectedMipmapIndex());
			}
		});
		
		spinnerImage.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if(editor.activeFile!=null && editor.activeFile.getNumTextures()!=0) {
					PapaTexture target = editor.activeFile.getTexture(getSelectedFileImageIndex());
					if(target!=editor.activeTexture)
						editor.setActiveTexture(target);
				}
			}
		});
	}
	
	public int getActiveTextureIndex(PapaFile p) {
		if(p==null || editor.activeTexture == null)
			return -1;
		for(int i =0;i<p.getNumTextures();i++)
			if(p.getTexture(i)==editor.activeTexture)
				return i;
		return -1;
	}
	
	public void applySettings(PapaFile pf, int image, PapaTexture tex, boolean changed) {
		if(changed) {
			setTextureCount(pf.getNumTextures());
		}
		
		filePath.setBorder(defaultButtonBorder);
		if(pf.isLinkedFile()) {
			filePathLabel.setEnabled(true);
			filePath.setEnabled(true);
			filePath.setText(pf.getRelativeFileName());
			if(! pf.getParent().isLinkedFileReferenced(pf))
				filePath.setBorder(BorderFactory.createLineBorder(Color.red));
		} else {
			filePathLabel.setEnabled(false);
			filePath.setEnabled(false);
			filePath.setText("");
		}
		spinnerImage.setValue(image);
		
		applySettings(tex);
	}
	
	private void applySettings(PapaTexture tex) {
		if(tex==null) {
			spinnerImage.setEnabled(false);
			spinnerMipmap.setEnabled(false);
			imageName.setEnabled(false);
			imageName.setText("");
			filePath.setEnabled(false);
			filePathLabel.setEnabled(false);
			srgb.setEnabled(false);
			srgb.setSelected(false);
			resizeButton.setEnabled(false);
			changeFormatButton.setEnabled(false);
			editor.imagePanel.setImage(null);
			return;
		}
		
		PapaTexture t = tex;
		boolean enable = tex!=null;
		
		spinnerMipmap.setEnabled(enable);
		imageName.setEnabled(enable);
		srgb.setEnabled(enable);
		resizeButton.setEnabled(enable);
		changeFormatButton.setEnabled(enable);
		
		imageName.setBorder(defaultButtonBorder);
		if(!enable) {
			editor.imagePanel.unload();
			return;
		}
		
		if(t.isLinked()) {
			spinnerMipmap.setEnabled(false);
			srgb.setEnabled(false);
			resizeButton.setEnabled(false);
			changeFormatButton.setEnabled(false);
			imageName.setEnabled(true);
			imageName.setText(t.getName());
			if(!t.linkValid())
				imageName.setBorder(BorderFactory.createLineBorder(Color.red));
			t = t.getLinkedTexture();
		} else {
			imageName.setText(t.getName());
		}

		setMipmapCount(t.getMips());
		srgb.setSelected(t.getSRGB());
		
		editor.imagePanel.setImage(t);
		editor.imagePanel.setImageIndex(0);
		
	}
	
	private void refreshActiveTexture() {
		if(editor.activeTexture.isLinked() && ! editor.activeTexture.linkValid()) {
			if(imageName.getBorder()==defaultButtonBorder)
				editor.imagePanel.setImage(editor.activeTexture.getLinkedTexture());
			imageName.setBorder(BorderFactory.createLineBorder(Color.red));
		} else {
			if(imageName.getBorder()!=defaultButtonBorder)
				editor.imagePanel.setImage(editor.activeTexture.getLinkedTexture());
			imageName.setBorder(defaultButtonBorder);
		}
		editor.configSelector.selectedNodeChanged();
		editor.configSelector.fileTree.repaint();
	}
	
	private void refreshActiveFileLinks() {
		if(editor.activeFile.isLinkedFile() && ! editor.activeFile.getParent().isLinkedFileReferenced(editor.activeFile))
			filePath.setBorder(BorderFactory.createLineBorder(Color.red));
		else 
			filePath.setBorder(defaultButtonBorder);
		editor.configSelector.selectedNodeChanged();
	}
	
	public void unload() {
		spinnerImage.setEnabled(false);
		spinnerImage.setValue(0);
		spinnerMipmap.setEnabled(false);
		spinnerMipmap.setValue(0);
		srgb.setEnabled(false);
		srgb.setSelected(false);
		imageName.setEnabled(false);
		imageName.setText("");
		filePathLabel.setEnabled(false);
		filePath.setEnabled(false);
		filePath.setText("");
		resizeButton.setEnabled(false);
		changeFormatButton.setEnabled(false);
		editor.imagePanel.setImage(null);
		imageName.setBorder(defaultButtonBorder);
	}
	
	private void setTextureCount(int newMax) {
		if(newMax>1) {
			spinnerImage.setEnabled(true);
			spinnerImage.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), new Integer(newMax-1), new Integer(1)));
		}
		else
			spinnerImage.setEnabled(false);
		
	}
	
	public int getSelectedFileImageIndex() {
		return (int)spinnerImage.getValue();
	}
	
	private void setMipmapCount(int mips) {
		if(mips == 0)
			spinnerMipmap.setEnabled(false);
		else
			spinnerMipmap.setEnabled(true);
		spinnerMipmap.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), new Integer(mips), new Integer(1)));
	}
	
	public int getSelectedMipmapIndex() {
		return (int)spinnerMipmap.getValue();
	}
}