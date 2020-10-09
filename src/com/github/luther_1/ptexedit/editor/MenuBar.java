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

public class MenuBar extends JMenuBar {
	
	private static final long serialVersionUID = 275294845979597235L;

	public ButtonGroup mViewChannelItems;
	public JCheckBoxMenuItem mViewLuminance, mViewNoAlpha, mViewTile, mViewDXT, mOptionsShowRoot, mOptionsAllowEmpty, mOptionsSuppressWarnings;
	public JRadioButtonMenuItem mViewChannelRGB, mViewChannelR, mViewChannelG, mViewChannelB, mViewChannelA;
	public JMenuItem mFileOpen,mFileImport, mFileSave, mFileSaveAs, mFileExport, mToolsConvertFolder, mToolsShowInFileBrowser, mToolsReloadLinked,
						mEditCopy, mEditPaste;
	public boolean clipboardHasImage, readingFiles;
	
	private final Editor editor;
	
	public MenuBar(Editor editor) {
		this.editor = editor;
		
		JMenu mFile = new JMenu("File");
		mFile.setMnemonic('f');
		add(mFile);
		
		mFileOpen = new JMenuItem("Open");
		mFileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		mFile.add(mFileOpen);
		mFileOpen.setMnemonic('o');
		
		mFileOpen.addActionListener((ActionEvent e) -> {
			JFileChooser j = new JFileChooser();
			
			j.setFileSelectionMode(JFileChooser.FILES_ONLY);
			j.setAcceptAllFileFilterUsed(false);
			j.setFileFilter(FileHandler.getPapaFilter());
			if (j.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				File file = j.getSelectedFile();
				editor.readAll(file);
			}
		});
		
		JSeparator separator_1 = new JSeparator();
		mFile.add(separator_1);
		
		mFileSave = new JMenuItem("Save");
		mFileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		mFile.add(mFileSave);
		mFileSave.setMnemonic('s');
		mFileSave.setEnabled(false);
		mFileSave.addActionListener((ActionEvent e) -> {
			saveFile(editor.activeFile);
		});
		
		mFileSaveAs = new JMenuItem("Save As...");
		mFileSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mFile.add(mFileSaveAs);
		mFileSaveAs.setMnemonic('A');
		mFileSaveAs.setEnabled(false);
		mFileSaveAs.addActionListener((ActionEvent e) -> {
			saveFileAs(editor.activeFile);
		});
		
		JSeparator separator = new JSeparator();
		mFile.add(separator);
		
		mFileImport = new JMenuItem("Import");
		mFileImport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
		mFile.add(mFileImport);
		mFileImport.setMnemonic('i');
		mFileImport.addActionListener((ActionEvent e) -> { // this is identical to mFileOpen and just changes the accepted file types. Fight me.
			JFileChooser j = new JFileChooser();
			
			j.setFileSelectionMode(JFileChooser.FILES_ONLY);
			j.setAcceptAllFileFilterUsed(false);
			j.setDialogTitle("Import File");
			
			for(FileNameExtensionFilter f : FileHandler.getImageFilters())
				j.addChoosableFileFilter(f);
			
			if (j.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				File file = j.getSelectedFile();
				editor.readAll(file);
			}
		});
		
		mFileExport = new JMenuItem("Export");
		mFileExport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		mFile.add(mFileExport);
		mFileExport.setMnemonic('e');
		mFileExport.setEnabled(false);
		mFileExport.addActionListener((ActionEvent e) -> {
			
			JFileChooser j = new JFileChooser();
			
			j.setFileSelectionMode(JFileChooser.FILES_ONLY);
			j.setAcceptAllFileFilterUsed(false);
			j.setDialogTitle("Export File");
			for(FileNameExtensionFilter f : FileHandler.getImageFilters())
				j.addChoosableFileFilter(f);
			
			j.setFileFilter(FileHandler.getImageFilter("png"));
			
			if(editor.activeFile.getFile()!=null) {
				if(editor.activeFile.getFile().exists())
					j.setSelectedFile(FileHandler.replaceExtension(editor.activeFile.getFile(), FileHandler.getImageFilter("png")));
				else
					j.setCurrentDirectory(getLowestExistingDirectory(editor.activeFile.getFile()));
			}
			
			PapaTexture tex = editor.activeTexture;
			if(tex.isLinked() && tex.linkValid())
				tex = tex.getLinkedTexture();
			
			editor.startOperation();
			if (j.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
				try {
					FileHandler.exportImageTo(tex, j.getSelectedFile(), (FileNameExtensionFilter)j.getFileFilter());
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
		
		mEditCopy = new JMenuItem("Copy");
		mEditCopy.setMnemonic('c');
		mEditCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mEditCopy.setEnabled(false);
		mEdit.add(mEditCopy);
		mEditCopy.addActionListener((ActionEvent e)-> {
			transferToClipboard(editor.imagePanel.getFullImage());
		});
		
		mEditPaste = new JMenuItem("Paste");
		mEditPaste.setMnemonic('p');
		mEditPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mEdit.add(mEditPaste);
		mEditPaste.addActionListener((ActionEvent e)-> {
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
				mEditPaste.setEnabled(clipboardHasImage && ! readingFiles);
			}
		});
		clipboardChanged();
		mEditPaste.setEnabled(clipboardHasImage);
		
		JMenu mView = new JMenu("View");
		mView.setMnemonic('v');
		add(mView);
		
		
		JMenu mViewChannel = new JMenu("Channels");
		mView.add(mViewChannel);
		mViewChannel.setMnemonic('C');
		
		mViewChannelItems = new ButtonGroup();
		
		mViewChannelRGB = new JRadioButtonMenuItem("RGB",true);
		mViewChannelRGB.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mViewChannel.add(mViewChannelRGB);
		mViewChannelItems.add(mViewChannelRGB);
		mViewChannelRGB.addActionListener((ActionEvent e) -> {
			if(mViewChannelRGB.isSelected())
				editor.imagePanel.setMode(ImagePanel.RGBA);
		});
		
		mViewChannelR = new JRadioButtonMenuItem("R");
		mViewChannelR.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mViewChannel.add(mViewChannelR);
		mViewChannelItems.add(mViewChannelR);
		mViewChannelR.addActionListener((ActionEvent e) -> {
			if(mViewChannelR.isSelected())
				editor.imagePanel.setMode(ImagePanel.RED);
		});
		
		
		mViewChannelG = new JRadioButtonMenuItem("G");
		mViewChannelG.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mViewChannel.add(mViewChannelG);
		mViewChannelItems.add(mViewChannelG);
		mViewChannelG.addActionListener((ActionEvent e) -> {
			if(mViewChannelG.isSelected())
				editor.imagePanel.setMode(ImagePanel.GREEN);
		});
		
		
		mViewChannelB = new JRadioButtonMenuItem("B");
		mViewChannelB.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mViewChannel.add(mViewChannelB);
		mViewChannelItems.add(mViewChannelB);
		mViewChannelB.addActionListener((ActionEvent e) -> {
			if(mViewChannelB.isSelected())
				editor.imagePanel.setMode(ImagePanel.BLUE);
		});
		
		
		mViewChannelA = new JRadioButtonMenuItem("A");
		mViewChannelA.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mViewChannel.add(mViewChannelA);
		mViewChannelItems.add(mViewChannelA);
		mViewChannelA.addActionListener((ActionEvent e) -> {
			if(mViewChannelA.isSelected())
				editor.imagePanel.setMode(ImagePanel.ALPHA);
		});
		
		
		mViewLuminance = new JCheckBoxMenuItem("Luminance");
		mViewLuminance.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mViewLuminance.setMnemonic('l');
		mView.add(mViewLuminance);
		mViewLuminance.addActionListener((ActionEvent e) -> {
			editor.imagePanel.setLuminance(mViewLuminance.isSelected());
		});
		
		
		mViewNoAlpha = new JCheckBoxMenuItem("Ignore Alpha");
		mViewNoAlpha.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mViewNoAlpha.setMnemonic('i');
		mView.add(mViewNoAlpha);
		mViewNoAlpha.addActionListener((ActionEvent e) -> {
			editor.imagePanel.setIgnoreAlpha(mViewNoAlpha.isSelected());
		});
		
		
		mViewTile = new JCheckBoxMenuItem("Tile");
		mViewTile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mViewTile.setMnemonic('t');
		mView.add(mViewTile);
		mViewTile.addActionListener((ActionEvent e) -> {
			editor.imagePanel.setTile(mViewTile.isSelected());
		});
		
		mView.add(new JSeparator());
		
		mViewDXT = new JCheckBoxMenuItem("DXT Grid");
		mViewDXT.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mViewDXT.setMnemonic('d');
		mView.add(mViewDXT);
		mViewDXT.addActionListener((ActionEvent e) -> {
			editor.imagePanel.showDXT(mViewDXT.isSelected());
		});
		
		
		JMenu mTools = new JMenu("Tools");
		mTools.setMnemonic('t');
		add(mTools);
		
		mToolsConvertFolder = new JMenuItem("Convert Folder");
		mToolsConvertFolder.setMnemonic('c');
		mTools.add(mToolsConvertFolder);
		mToolsConvertFolder.addActionListener((ActionEvent e) -> {
			editor.batchConvert.showAt(editor.APPLICATION_WINDOW.getX() + editor.APPLICATION_WINDOW.getWidth() / 2, editor.APPLICATION_WINDOW.getY() + editor.APPLICATION_WINDOW.getHeight() / 2);
		});
		
		mToolsShowInFileBrowser = new JMenuItem("Show in File Manager");
		mToolsShowInFileBrowser.setEnabled(false);
		mToolsShowInFileBrowser.setMnemonic('s');
		boolean supported = Desktop.getDesktop().isSupported(Desktop.Action.OPEN);
		if(supported)
			mTools.add(mToolsShowInFileBrowser);
		mToolsShowInFileBrowser.addActionListener((ActionEvent e) -> {
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
		
		mToolsReloadLinked = new JMenuItem("Reload Linked Files");
		mToolsReloadLinked.setEnabled(false);
		mToolsReloadLinked.setMnemonic('c');
		mTools.add(mToolsReloadLinked);
		mToolsReloadLinked.addActionListener((ActionEvent e) -> {
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
		
		mOptionsShowRoot = new JCheckBoxMenuItem("Always Show Root");
		mOptions.add(mOptionsShowRoot);
		mOptionsShowRoot.addActionListener((ActionEvent e) -> {
			editor.configSelector.setAlwaysShowRoot(mOptionsShowRoot.isSelected());
		});
		mOptionsShowRoot.setMnemonic('a');
		
		mOptionsAllowEmpty = new JCheckBoxMenuItem("Allow Non-Image Files");
		mOptions.add(mOptionsAllowEmpty);
		mOptionsAllowEmpty.addActionListener((ActionEvent e) -> {
			editor.ALLOW_EMPTY_FILES = mOptionsAllowEmpty.isSelected();
			if(!editor.ALLOW_EMPTY_FILES)
				editor.configSelector.removeEmptyFiles();
		});
		mOptionsAllowEmpty.setMnemonic('n');
		
		mOptionsSuppressWarnings = new JCheckBoxMenuItem("Suppress Warnings");
		mOptions.add(mOptionsSuppressWarnings);
		mOptionsSuppressWarnings.addActionListener((ActionEvent e) -> {
			editor.SUPPRESS_WARNINGS = mOptionsSuppressWarnings.isSelected();
		});
		mOptionsSuppressWarnings.setMnemonic('s');
		
		JMenu mHelp = new JMenu("Help");
		mHelp.setMnemonic('h');
		add(mHelp);
		
		JMenuItem mHelpAbout = new JMenuItem("About");
		mHelp.add(mHelpAbout);
		mHelpAbout.addActionListener((ActionEvent e) -> {
			JOptionPane.showMessageDialog(editor.APPLICATION_WINDOW, "PTexEdit version: " + editor.VERSION_STRING + "\n"
					+ "Date: "+editor.BUILD_DATE, "About PTexEdit", JOptionPane.INFORMATION_MESSAGE, editor.imageIcon);
		});
		mHelpAbout.setMnemonic('a');
	}
	
	public int getSelectedRadioButton() { // I hate ButtonGroup.
		Enumeration<AbstractButton> i = mViewChannelItems.getElements();
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
		mFileImport.setEnabled(enable);
		mFileOpen.setEnabled(enable);
		mToolsConvertFolder.setEnabled(enable);
		mEditPaste.setEnabled(enable && clipboardHasImage);
	}

	public void unload() {
		mFileSave.setEnabled(false);
		mFileSaveAs.setEnabled(false);
		mFileExport.setEnabled(false);
		mEditCopy.setEnabled(false);
		mToolsShowInFileBrowser.setEnabled(false);
		mToolsReloadLinked.setEnabled(false);
	}

	public void applySettings(PapaFile activeFile, int index, PapaTexture tex, boolean same) {
		boolean hasTextures = !(activeFile.getNumTextures() == 0 || tex == null);
		boolean linkValid = hasTextures && (!tex.isLinked() || tex.linkValid());
		mFileSave.setEnabled(true);
		mFileSaveAs.setEnabled(true);
		mToolsShowInFileBrowser.setEnabled(activeFile.getFile()!=null && activeFile.getFile().exists());
		
		mFileExport.setEnabled(hasTextures && linkValid);
		mEditCopy.setEnabled(hasTextures && linkValid);
		mToolsReloadLinked.setEnabled(hasTextures && activeFile!=null);
	}

	public void setSelectedRadioButton(int index) {
		Enumeration<AbstractButton> i = mViewChannelItems.getElements();
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
		j.setFileFilter(FileHandler.getPapaFilter());
		if(target.getFile()!=null) {
			if(!target.getFile().exists())
				j.setCurrentDirectory(getLowestExistingDirectory(target.getFile()));
			j.setSelectedFile(FileHandler.replaceExtension(target.getFile(), FileHandler.getPapaFilter()));
		}
		editor.startOperation();
		if (j.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
			try {
				FileHandler.saveFileTo(target, j.getSelectedFile());
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
				Editor.optionBox("The selected directory does not include the /pa subdirectory.\n Are you sure this is the correct directory?",
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