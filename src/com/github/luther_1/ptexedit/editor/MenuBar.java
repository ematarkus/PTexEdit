package com.github.luther_1.ptexedit.editor;

import java.awt.Desktop;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.github.luther_1.ptexedit.papafile.PapaFile;
import com.github.luther_1.ptexedit.papafile.PapaTexture;
import com.github.luther_1.ptexedit.resources.MyIcons;

public class MenuBar extends JMenuBar {
	
	private static final long serialVersionUID = 275294845979597235L;

	public ButtonGroup viewChannelItems;
	public JCheckBoxMenuItem viewLuminance, viewNoAlpha, viewTile, viewDXT, optionsShowRoot, optionsAllowEmpty, optionsSuppressWarnings;
	public JRadioButtonMenuItem viewChannelRGB, viewChannelR, viewChannelG, viewChannelB, viewChannelA;
	public JMenuItem fileOpen,fileImport, fileSave, fileSaveAs, fileExport, toolsConvertFolder, toolsShowInFileBrowser, toolsReloadLinked,
						editCopy, editPaste;
	public boolean clipboardHasImage, readingFiles;
	
	private JFileChooser fileChooser = null;
	
	private final Editor editor;
	
	public MenuBar(Editor editor) {
		this.editor = editor;
		
		JMenu mFile = new JMenu("File");
		mFile.setMnemonic('f');
		add(mFile);
		
		fileOpen = new JMenuItem("Open");
		fileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		mFile.add(fileOpen);
		fileOpen.setMnemonic('o');
		
		fileOpen.addActionListener((ActionEvent e) -> {
			if(fileChooser == null) {
				fileChooser = new JFileChooser();
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.setFileFilter(getFileHandler().getPapaFilter());
			}
			
			if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				editor.readAll(file);
			}
		});
		
		JSeparator separator_1 = new JSeparator();
		mFile.add(separator_1);
		
		fileSave = new JMenuItem("Save");
		fileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		mFile.add(fileSave);
		fileSave.setMnemonic('s');
		fileSave.setEnabled(false);
		fileSave.addActionListener((ActionEvent e) -> {
			saveFile(editor.activeFile);
		});
		
		fileSaveAs = new JMenuItem("Save As...");
		fileSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mFile.add(fileSaveAs);
		fileSaveAs.setMnemonic('A');
		fileSaveAs.setEnabled(false);
		fileSaveAs.addActionListener((ActionEvent e) -> {
			saveFileAs(editor.activeFile);
		});
		
		JSeparator separator = new JSeparator();
		mFile.add(separator);
		
		fileImport = new JMenuItem("Import");
		fileImport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
		mFile.add(fileImport);
		fileImport.setMnemonic('i');
		fileImport.addActionListener((ActionEvent e) -> { // this is identical to mFileOpen and just changes the accepted file types. Fight me.
			JFileChooser j = new JFileChooser();
			
			j.setFileSelectionMode(JFileChooser.FILES_ONLY);
			j.setAcceptAllFileFilterUsed(false);
			j.setDialogTitle("Import File");
			
			for(FileNameExtensionFilter f : getFileHandler().getImageFilters())
				j.addChoosableFileFilter(f);
			
			if (j.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				File file = j.getSelectedFile();
				editor.readAll(file);
			}
		});
		
		fileExport = new JMenuItem("Export");
		fileExport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		mFile.add(fileExport);
		fileExport.setMnemonic('e');
		fileExport.setEnabled(false);
		fileExport.addActionListener((ActionEvent e) -> {
			
			JFileChooser j = new JFileChooser();
			
			j.setFileSelectionMode(JFileChooser.FILES_ONLY);
			j.setAcceptAllFileFilterUsed(false);
			j.setDialogTitle("Export File");
			for(FileNameExtensionFilter f : getFileHandler().getImageFilters())
				j.addChoosableFileFilter(f);
			
			j.setFileFilter(getFileHandler().getImageFilter("png"));
			
			if(editor.activeFile.getFile()!=null) {
				if(editor.activeFile.getFile().exists())
					j.setSelectedFile(FileHandler.replaceExtension(editor.activeFile.getFile(), getFileHandler().getImageFilter("png")));
				else
					j.setCurrentDirectory(getLowestExistingDirectory(editor.activeFile.getFile()));
			}
			
			PapaTexture tex = editor.activeTexture;
			if(tex.isLinked() && tex.linkValid())
				tex = tex.getLinkedTexture();
			
			editor.startOperation();
			if (j.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
				try {
					getFileHandler().exportImageTo(tex, j.getSelectedFile(), (FileNameExtensionFilter)j.getFileFilter());
				} catch (IOException e1) {
					editor.showError(e1.getMessage(), "Export Error", new Object[] {"Ok"}, "Ok");
				}
			editor.endOperation();
		});
		
		JSeparator separator_2 = new JSeparator();
		mFile.add(separator_2);
		
		JMenuItem mFileExit = new JMenuItem("Exit");
		mFileExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK));
		mFile.add(mFileExit);
		mFileExit.setMnemonic('x');
		
		mFileExit.addActionListener((ActionEvent e) -> {
			System.exit(0);
		});
		
		JMenu mEdit = new JMenu("Edit");
		mEdit.setMnemonic('e');
		add(mEdit);
		
		editCopy = new JMenuItem("Copy");
		editCopy.setMnemonic('c');
		editCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		editCopy.setEnabled(false);
		mEdit.add(editCopy);
		editCopy.addActionListener((ActionEvent e)-> {
			transferToClipboard(editor.imagePanel.getFullImage());
		});
		
		editPaste = new JMenuItem("Paste");
		editPaste.setMnemonic('p');
		editPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mEdit.add(editPaste);
		editPaste.addActionListener((ActionEvent e)-> {
			Image i = getImageFromClipboard();
			if(i==null) {
				editor.showError("Clipboard does not contain an image.", "Invalid input", new Object[] {"Ok"}, "Ok");
				return;
			}
			editor.readImage(i);
		});
		
		editor.clipboard.addFlavorListener(new FlavorListener() {
			@Override
			public void flavorsChanged(FlavorEvent e) {
				clipboardChanged();
				editPaste.setEnabled(clipboardHasImage && ! readingFiles);
			}
		});
		clipboardChanged();
		editPaste.setEnabled(clipboardHasImage);
		
		JMenu mView = new JMenu("View");
		mView.setMnemonic('v');
		add(mView);
		
		
		JMenu mViewChannel = new JMenu("Channels");
		mView.add(mViewChannel);
		mViewChannel.setMnemonic('C');
		
		viewChannelItems = new ButtonGroup();
		
		viewChannelRGB = new JRadioButtonMenuItem("RGB",true);
		viewChannelRGB.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mViewChannel.add(viewChannelRGB);
		viewChannelItems.add(viewChannelRGB);
		viewChannelRGB.addActionListener((ActionEvent e) -> {
			if(viewChannelRGB.isSelected())
				editor.imagePanel.setMode(ImagePanel.RGBA);
		});
		
		viewChannelR = new JRadioButtonMenuItem("R");
		viewChannelR.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mViewChannel.add(viewChannelR);
		viewChannelItems.add(viewChannelR);
		viewChannelR.addActionListener((ActionEvent e) -> {
			if(viewChannelR.isSelected())
				editor.imagePanel.setMode(ImagePanel.RED);
		});
		
		
		viewChannelG = new JRadioButtonMenuItem("G");
		viewChannelG.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mViewChannel.add(viewChannelG);
		viewChannelItems.add(viewChannelG);
		viewChannelG.addActionListener((ActionEvent e) -> {
			if(viewChannelG.isSelected())
				editor.imagePanel.setMode(ImagePanel.GREEN);
		});
		
		
		viewChannelB = new JRadioButtonMenuItem("B");
		viewChannelB.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mViewChannel.add(viewChannelB);
		viewChannelItems.add(viewChannelB);
		viewChannelB.addActionListener((ActionEvent e) -> {
			if(viewChannelB.isSelected())
				editor.imagePanel.setMode(ImagePanel.BLUE);
		});
		
		
		viewChannelA = new JRadioButtonMenuItem("A");
		viewChannelA.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mViewChannel.add(viewChannelA);
		viewChannelItems.add(viewChannelA);
		viewChannelA.addActionListener((ActionEvent e) -> {
			if(viewChannelA.isSelected())
				editor.imagePanel.setMode(ImagePanel.ALPHA);
		});
		
		
		viewLuminance = new JCheckBoxMenuItem("Luminance");
		viewLuminance.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		viewLuminance.setMnemonic('l');
		mView.add(viewLuminance);
		viewLuminance.addActionListener((ActionEvent e) -> {
			editor.imagePanel.setLuminance(viewLuminance.isSelected());
		});
		
		
		viewNoAlpha = new JCheckBoxMenuItem("Ignore Alpha");
		viewNoAlpha.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		viewNoAlpha.setMnemonic('i');
		mView.add(viewNoAlpha);
		viewNoAlpha.addActionListener((ActionEvent e) -> {
			editor.imagePanel.setIgnoreAlpha(viewNoAlpha.isSelected());
		});
		
		
		viewTile = new JCheckBoxMenuItem("Tile");
		viewTile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		viewTile.setMnemonic('t');
		mView.add(viewTile);
		viewTile.addActionListener((ActionEvent e) -> {
			editor.imagePanel.setTile(viewTile.isSelected());
		});
		
		mView.add(new JSeparator());
		
		viewDXT = new JCheckBoxMenuItem("DXT Grid");
		viewDXT.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		viewDXT.setMnemonic('d');
		mView.add(viewDXT);
		viewDXT.addActionListener((ActionEvent e) -> {
			editor.imagePanel.showDXT(viewDXT.isSelected());
		});
		
		
		JMenu mTools = new JMenu("Tools");
		mTools.setMnemonic('t');
		add(mTools);
		
		toolsConvertFolder = new JMenuItem("Convert Folder");
		toolsConvertFolder.setMnemonic('c');
		mTools.add(toolsConvertFolder);
		toolsConvertFolder.addActionListener((ActionEvent e) -> {
			editor.batchConvert.showAt(editor.getX() + editor.getWidth() / 2, editor.getY() + editor.getHeight() / 2);
		});
		
		toolsShowInFileBrowser = new JMenuItem("Show in File Manager");
		toolsShowInFileBrowser.setEnabled(false);
		toolsShowInFileBrowser.setMnemonic('s');
		boolean supported = Desktop.getDesktop().isSupported(Desktop.Action.OPEN);
		if(supported)
			mTools.add(toolsShowInFileBrowser);
		toolsShowInFileBrowser.addActionListener((ActionEvent e) -> {
			try {
				File target = editor.activeFile.getFile().getParentFile();
				if(!target.exists())
					throw new IOException("Directory does not exist");
				Desktop.getDesktop().open(target);
			} catch (Exception e1) {
				editor.showError("Failed to open file in file manager: "+e1.getMessage(), "Error", new Object[] {"Ok"}, "Ok");
			}
		});
		
		mTools.add(new JSeparator());
		
		toolsReloadLinked = new JMenuItem("Reload Linked Files");
		toolsReloadLinked.setEnabled(false);
		toolsReloadLinked.setMnemonic('c');
		mTools.add(toolsReloadLinked);
		toolsReloadLinked.addActionListener((ActionEvent e) -> {
			editor.activeFile.reloadLinkedTextures();
			editor.configSelector.reloadTopLevelSelectedNode();
			editor.setActiveFile(editor.activeFile);
		});
		
		
		//JMenuItem mToolsReload = new JMenuItem("Reload File"); TODO
		//mTools.add(mToolsReload);
		
		JMenu mOptions = new JMenu("Options");
		mOptions.setMnemonic('o');
		add(mOptions);
		
		JMenuItem mOptionsSetDirectory = new JMenuItem("Set Media Directory...");
		mOptionsSetDirectory.setToolTipText("This is the base directory that will be used when finding linked textures.");
		mOptionsSetDirectory.setMnemonic('s');
		mOptions.add(mOptionsSetDirectory);
		mOptionsSetDirectory.addActionListener((ActionEvent e) -> {
			changeMediaDirectory();
		});
		
		JMenuItem mOptionsImageSettings = new JMenuItem("View Import Settings");
		mOptions.add(mOptionsImageSettings);
		mOptionsImageSettings.addActionListener((ActionEvent e) -> {
			editor.getTextureImportSettings(null);
		});
		mOptionsImageSettings.setMnemonic('v');
		
		JSeparator optionsSeparator = new JSeparator();
		mOptions.add(optionsSeparator);
		
		//JMenuItem mOptionsCacheInfo = new JMenuItem("Remember Image Settings"); TODO
		//mOptions.add(mOptionsCacheInfo);
		
		optionsShowRoot = new JCheckBoxMenuItem("Always Show Root");
		mOptions.add(optionsShowRoot);
		optionsShowRoot.addActionListener((ActionEvent e) -> {
			editor.configSelector.setAlwaysShowRoot(optionsShowRoot.isSelected());
		});
		optionsShowRoot.setMnemonic('a');
		
		optionsAllowEmpty = new JCheckBoxMenuItem("Allow Non-Image Files");
		mOptions.add(optionsAllowEmpty);
		optionsAllowEmpty.addActionListener((ActionEvent e) -> {
			editor.ALLOW_EMPTY_FILES = optionsAllowEmpty.isSelected();
			if(!editor.ALLOW_EMPTY_FILES)
				editor.configSelector.removeEmptyFiles();
		});
		optionsAllowEmpty.setMnemonic('n');
		
		optionsSuppressWarnings = new JCheckBoxMenuItem("Suppress Warnings");
		mOptions.add(optionsSuppressWarnings);
		optionsSuppressWarnings.addActionListener((ActionEvent e) -> {
			editor.SUPPRESS_WARNINGS = optionsSuppressWarnings.isSelected();
		});
		optionsSuppressWarnings.setMnemonic('s');
		
		JMenu mHelp = new JMenu("Help");
		mHelp.setMnemonic('h');
		add(mHelp);
		
		JMenuItem mHelpAbout = new JMenuItem("About");
		mHelp.add(mHelpAbout);
		mHelpAbout.addActionListener((ActionEvent e) -> {
			JOptionPane.showMessageDialog(editor, "PTexEdit version: " + editor.VERSION_STRING + "\n"
					+ "Date: "+editor.BUILD_DATE, "About PTexEdit", JOptionPane.INFORMATION_MESSAGE, MyIcons.getImageIcon());
		});
		mHelpAbout.setMnemonic('a');
	}
	
	public FileHandler getFileHandler() {
		return editor.getFileHandler();
	}
	
	public int getSelectedRadioButton() { // I hate ButtonGroup.
		Enumeration<AbstractButton> i = viewChannelItems.getElements();
		int j=0;
		while(i.hasMoreElements()) {
			if(i.nextElement().isSelected())
				return j;
			j++;
		}
		return 0;
	}
	
	public void setReadingFiles(boolean b) {
		boolean enable = !b;
		readingFiles = b;
		fileImport.setEnabled(enable);
		fileOpen.setEnabled(enable);
		toolsConvertFolder.setEnabled(enable);
		editPaste.setEnabled(enable && clipboardHasImage);
	}

	public void unload() {
		fileSave.setEnabled(false);
		fileSaveAs.setEnabled(false);
		fileExport.setEnabled(false);
		editCopy.setEnabled(false);
		toolsShowInFileBrowser.setEnabled(false);
		toolsReloadLinked.setEnabled(false);
	}

	public void applySettings(PapaFile activeFile, int index, PapaTexture tex, boolean same) {
		boolean hasTextures = !(activeFile.getNumTextures() == 0 || tex == null);
		boolean linkValid = hasTextures && (!tex.isLinked() || tex.linkValid());
		fileSave.setEnabled(true);
		fileSaveAs.setEnabled(true);
		toolsShowInFileBrowser.setEnabled(activeFile.getFile()!=null && activeFile.getFile().exists());
		
		fileExport.setEnabled(hasTextures && linkValid);
		editCopy.setEnabled(hasTextures && linkValid);
		toolsReloadLinked.setEnabled(hasTextures && activeFile!=null);
	}

	public void setSelectedRadioButton(int index) {
		Enumeration<AbstractButton> i = viewChannelItems.getElements();
		int j=0;
		AbstractButton a;
		while(i.hasMoreElements()) {
			a=i.nextElement();
			if(j++==index) {
				a.doClick(0);
				return;
			}
		}
	}
	
	private void saveFile(PapaFile target) {
		if( ! target.isPapaFile()) {
			saveFileAs(target);
			return;
		}
		
		editor.startOperation();
		try {
			FileHandler.writeFile(target,target.getFile());
			editor.configSelector.selectedPapaFileNodeChanged();
		} catch (IOException e1) {
			editor.showError(e1.getMessage(), "Save", new Object[] {"Ok"}, "Ok");
		}
		editor.endOperation();
	}
	
	private void saveFileAs(PapaFile target) {
		JFileChooser j = new JFileChooser();
		
		j.setFileSelectionMode(JFileChooser.FILES_ONLY);
		j.setDialogTitle("Save Papa File");
		j.setFileFilter(getFileHandler().getPapaFilter());
		if(target.getFile()!=null) {
			if(!target.getFile().exists())
				j.setCurrentDirectory(getLowestExistingDirectory(target.getFile()));
			j.setSelectedFile(FileHandler.replaceExtension(target.getFile(), getFileHandler().getPapaFilter()));
		}
		editor.startOperation();
		if (j.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
			try {
				getFileHandler().saveFileTo(target, j.getSelectedFile());
			} catch (IOException e1) {
				editor.showError(e1.getMessage(), "Save As...", new Object[] {"Ok"}, "Ok");
			}
			editor.configSelector.selectedPapaFileNodeChanged();
		}
		editor.endOperation();
	}
	
	private File getLowestExistingDirectory(File file) {
		while(file!=null && !file.exists()) {
			file = file.getParentFile();
		}
		return file;
	}

	private void clipboardChanged() {
		try {
			for(DataFlavor flavor : editor.clipboard.getAvailableDataFlavors())
				if(flavor.equals(DataFlavor.imageFlavor)) {
					clipboardHasImage = true;
					return;
				}
		} catch (IllegalStateException e) {
			clipboardHasImage = true; // rely on future checks if we can't determine right now
			return;
		}
		clipboardHasImage = false;
	}
	
	public void changeMediaDirectory() {
		JFileChooser j = new JFileChooser();
		j.setDialogTitle("Set Media Directory");
		
		j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if(PapaFile.getPlanetaryAnnihilationDirectory() != null)
			j.setSelectedFile(PapaFile.getPlanetaryAnnihilationDirectory());
		
		if (j.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			File file = j.getSelectedFile();
			if(file.listFiles((File dir, String name)-> name.equals("pa")).length==0 && 
				editor.optionBox("The selected directory does not include the /pa subdirectory.\n Are you sure this is the correct directory?",
								"Confirm Directory", new Object[] {"Yes","Cancel"}, "Cancel") != 0) {
					return;
			}
			PapaFile.setPADirectory(file);
			
		}
	}
	
	private Image getImageFromClipboard() {
		try {
			return (Image)editor.clipboard.getData(DataFlavor.imageFlavor);
		} catch (UnsupportedFlavorException | IllegalStateException e) {
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	// https://coderanch.com/t/333565/java/BufferedImage-System-Clipboard
	private void transferToClipboard(BufferedImage image) {
		TransferableImage t = new TransferableImage(image);
		try {
			editor.clipboard.setContents(t, editor.clipboardOwner);
		} catch (IllegalStateException e) {
			editor.showError("Failed to copy image to clipboad. Clipboard is unavailable.", "Copy error", new Object[] {"Ok"}, "Ok");
		}
	}
	
	
}