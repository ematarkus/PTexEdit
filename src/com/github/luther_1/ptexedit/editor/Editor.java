/*
 *  This file is part of PTexEdit 
 * 
 *  Texture editor for Planetary Annihilation's papa files.
 *  Copyright (C) 2020 Marcus Der <marcusder@hotmail.com>
 * 
 *  PTexEdit is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  PTexEdit is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with PTexEdit.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.github.luther_1.ptexedit.editor;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import com.github.luther_1.ptexedit.editor.FileHandler.*;
import com.github.luther_1.ptexedit.editor.FileHandler.ImportInfo.ActivityListener;
import com.github.luther_1.ptexedit.papafile.*;
import com.github.luther_1.ptexedit.papafile.PapaTexture.*;
import com.github.luther_1.ptexedit.resources.MyIcons;
import com.github.memo33.jsquish.Squish.CompressionMethod;

import java.io.*;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

public class Editor extends JFrame {
	
	public 	static boolean ALLOW_EMPTY_FILES = false;
	public 	static boolean SUPPRESS_WARNINGS = false;
	private static final long serialVersionUID = 894467903207605180L;
	
	public static final String VERSION_STRING = "0.3";
	public static final String BUILD_DATE = "August 1, 2020";
	
	public static final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	//private static int maxThreads;
	private JPanel contentPane;
	
	public PapaFile activeFile = null;
	public PapaTexture activeTexture = null;
	public Set<PapaComponent> dependencies = Collections.newSetFromMap(new IdentityHashMap<>()), dependents = Collections.newSetFromMap(new IdentityHashMap<>());
	
	public ImagePanel imagePanel;
	private ConfigPanelTop configSection1;
	private ConfigPanelBottom configSection2;
	public ConfigPanelSelector configSelector;
	public RibbonPanel ribbonPanel;
	 
	public ClipboardListener clipboardOwner;
	
	private JPanel config, canvas;
	public JSplitPane mainPanel;
	JScrollBar horizontal;
	public JScrollBar vertical;
	private JPanel box;
	public MenuBar menu;
	
	public PapaOptions papaOptions;
	public BatchConvertDialog batchConvert;
	
	private int loadCounter = 0;
	
	private FileHandler fileHandler = null;
	
	public Editor() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		setIconImages(Arrays.asList(MyIcons.getIcon(),MyIcons.getIconSmall()));
		
		contentPane = new JPanel();
		setContentPane(contentPane);
		SpringLayout contentPaneLayout = new SpringLayout();
		contentPane.setLayout(contentPaneLayout);
		
		menu = new MenuBar(this);
		setJMenuBar(menu);
		
		ribbonPanel = new RibbonPanel(this); // TODO
		contentPaneLayout.putConstraint(SpringLayout.WEST, ribbonPanel, 0, SpringLayout.WEST, contentPane);
		contentPaneLayout.putConstraint(SpringLayout.NORTH, ribbonPanel, -25, SpringLayout.SOUTH, contentPane);
		contentPaneLayout.putConstraint(SpringLayout.SOUTH, ribbonPanel, 0, SpringLayout.SOUTH, contentPane);
		contentPaneLayout.putConstraint(SpringLayout.EAST, ribbonPanel, 0, SpringLayout.EAST, contentPane);
		contentPane.add(ribbonPanel);
		
		// left panel
		config = new JPanel();
		// right panel
		canvas = new JPanel();
		
		mainPanel = new JSplitPane();
		mainPanel.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		contentPaneLayout.putConstraint(SpringLayout.NORTH, mainPanel, 0, SpringLayout.NORTH, contentPane);
		contentPaneLayout.putConstraint(SpringLayout.SOUTH, mainPanel, 0, SpringLayout.NORTH, ribbonPanel);
		contentPaneLayout.putConstraint(SpringLayout.EAST, mainPanel, 0, SpringLayout.EAST, contentPane);
		contentPaneLayout.putConstraint(SpringLayout.WEST, mainPanel, 0, SpringLayout.WEST, contentPane);
		contentPane.add(mainPanel);
		
		mainPanel.setLeftComponent(config);
		mainPanel.setRightComponent(canvas);
		mainPanel.setDividerLocation(230);
		
		SpringLayout configLayout = new SpringLayout();
		config.setLayout(configLayout);
		
		SpringLayout canvasLayout = new SpringLayout();
		canvas.setLayout(canvasLayout);
		
		// RIGHT SIDE
		
		box = new JPanel();
		canvasLayout.putConstraint(SpringLayout.NORTH, box, -15, SpringLayout.SOUTH, canvas);
		canvasLayout.putConstraint(SpringLayout.SOUTH, box, 0, SpringLayout.SOUTH, canvas);
		canvasLayout.putConstraint(SpringLayout.WEST, box, -15, SpringLayout.EAST, canvas);
		canvasLayout.putConstraint(SpringLayout.EAST, box, 0, SpringLayout.EAST, canvas);
		canvas.add(box);
		
		AdjustmentListener scrollListener = new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				imagePanel.repaint();
			}
		};
		
		horizontal = new JScrollBar();
		canvasLayout.putConstraint(SpringLayout.NORTH, horizontal, 0, SpringLayout.NORTH, box);
		canvasLayout.putConstraint(SpringLayout.SOUTH, horizontal, 0, SpringLayout.SOUTH, box);
		canvasLayout.putConstraint(SpringLayout.WEST, horizontal, 0, SpringLayout.WEST, canvas);
		canvasLayout.putConstraint(SpringLayout.EAST, horizontal, 0, SpringLayout.WEST, box);
		horizontal.setOrientation(JScrollBar.HORIZONTAL);
		horizontal.setEnabled(false);
		horizontal.addAdjustmentListener(scrollListener);
		canvas.add(horizontal);
		
		vertical = new JScrollBar();
		canvasLayout.putConstraint(SpringLayout.NORTH, vertical, 0, SpringLayout.NORTH, canvas);
		canvasLayout.putConstraint(SpringLayout.SOUTH, vertical, 0, SpringLayout.NORTH, box);
		canvasLayout.putConstraint(SpringLayout.EAST, vertical, 0, SpringLayout.EAST, box);
		canvasLayout.putConstraint(SpringLayout.WEST, vertical, 0, SpringLayout.WEST, box);
		vertical.setOrientation(JScrollBar.VERTICAL);
		vertical.setEnabled(false);
		vertical.addAdjustmentListener(scrollListener);
		canvas.add(vertical);
		
		imagePanel = new ImagePanel(this);
		canvasLayout.putConstraint(SpringLayout.NORTH, imagePanel, 0, SpringLayout.NORTH, canvas);
		canvasLayout.putConstraint(SpringLayout.WEST, imagePanel, 0, SpringLayout.WEST, canvas);
		canvasLayout.putConstraint(SpringLayout.SOUTH, imagePanel, 0, SpringLayout.NORTH, box);
		canvasLayout.putConstraint(SpringLayout.EAST, imagePanel, 0, SpringLayout.WEST, box);
		imagePanel.setBackground(new Color(160,160,160));
		canvas.add(imagePanel);
		
		// LEFT SIDE
		
		// top panel
		configSection1 = new ConfigPanelTop(this);
		configLayout.putConstraint(SpringLayout.NORTH, configSection1, 5, SpringLayout.NORTH, config);
		configLayout.putConstraint(SpringLayout.WEST, configSection1, 5, SpringLayout.WEST, config);
		configLayout.putConstraint(SpringLayout.SOUTH, configSection1, 160, SpringLayout.NORTH, config); // 195 with buttons enabled
		configLayout.putConstraint(SpringLayout.EAST, configSection1, -5, SpringLayout.EAST, config);
		config.add(configSection1);
		
		//bottom panel
		configSection2 = new ConfigPanelBottom();
		configLayout.putConstraint(SpringLayout.NORTH, configSection2, 5, SpringLayout.SOUTH, configSection1);
		configLayout.putConstraint(SpringLayout.WEST, configSection2, 5, SpringLayout.WEST, config);
		configLayout.putConstraint(SpringLayout.SOUTH, configSection2, 270, SpringLayout.SOUTH, configSection1);
		configLayout.putConstraint(SpringLayout.EAST, configSection2, -5, SpringLayout.EAST, config);
		config.add(configSection2);
		
		//selector panel
		configSelector = new ConfigPanelSelector(this);
		configLayout.putConstraint(SpringLayout.NORTH, configSelector, 5, SpringLayout.SOUTH, configSection2);
		configLayout.putConstraint(SpringLayout.WEST, configSelector, 5, SpringLayout.WEST, config);
		configLayout.putConstraint(SpringLayout.SOUTH, configSelector, -5, SpringLayout.SOUTH, config);
		configLayout.putConstraint(SpringLayout.EAST, configSelector, -5, SpringLayout.EAST, config);
		config.add(configSelector);
		
		clipboardOwner = new ClipboardListener();
	}
	
	public final Thread onExit = new Thread(new Runnable() {
		
		public void run() {
			if(!Main.settingsFile.exists())
				return;
			
			Properties prop = Main.prop;
			
			int state = Editor.this.getExtendedState();
			if(state!=JFrame.MAXIMIZED_BOTH && state!=JFrame.MAXIMIZED_HORIZ && state!=JFrame.MAXIMIZED_VERT) {
				prop.setProperty("Application.Location.X", 			""+(int)getX());
				prop.setProperty("Application.Location.Y", 			""+(int)getY());
				prop.setProperty("Application.Size.Width",			""+(int)getWidth());
				prop.setProperty("Application.Size.Height", 		""+(int)getHeight());
			}
			prop.setProperty("Application.SplitPane.Location", 				""+(int)mainPanel.getDividerLocation());
			prop.setProperty("Application.State", 							""+getExtendedState());
			prop.setProperty("Application.Menu.View.Channels", 				""+menu.getSelectedRadioButton());
			prop.setProperty("Application.Menu.View.Luminance", 			""+menu.viewLuminance.isSelected());
			prop.setProperty("Application.Menu.View.Alpha", 				""+menu.viewNoAlpha.isSelected());
			prop.setProperty("Application.Menu.View.Tile", 					""+menu.viewTile.isSelected());
			prop.setProperty("Application.Menu.View.DXT", 					""+menu.viewDXT.isSelected());
			prop.setProperty("Application.Menu.Options.ShowRoot",			""+menu.optionsShowRoot.isSelected());
			prop.setProperty("Application.Menu.Options.AllowEmpty",			""+menu.optionsAllowEmpty.isSelected());
			prop.setProperty("Application.Menu.Options.SuppressWarnings",	""+menu.optionsSuppressWarnings.isSelected());
			//prop.setProperty("Application.Config.MaxThreads", 	""+e.maxThreads);
			
			ImmutableTextureSettings settings = papaOptions.getCurrentSettings();
			
			prop.setProperty("PapaOptions.Format", 				""+settings.format);
			prop.setProperty("PapaOptions.DxtMethod", 			""+settings.method);
			prop.setProperty("PapaOptions.GenMipmaps", 			""+settings.generateMipmaps);
			prop.setProperty("PapaOptions.MipmapResizeMethod", 	""+settings.mipmapResizeMethod);
			prop.setProperty("PapaOptions.SRGB", 				""+settings.SRGB);
			prop.setProperty("PapaOptions.Resize", 				""+settings.resize);
			prop.setProperty("PapaOptions.ResizeMethod", 		""+settings.resizeMethod);
			prop.setProperty("PapaOptions.ResizeMode", 			""+settings.resizeMode);
			
			BatchConvertDialog b = batchConvert;
			prop.setProperty("BatchConvert.Recursive", 			""+b.isRecursive());
			prop.setProperty("BatchConvert.WriteLinked", 		""+b.isWritingLinkedFiles());
			prop.setProperty("BatchConvert.Overwrite", 			""+b.isOverwrite());
			prop.setProperty("BatchConvert.IgnoreHierarchy", 	""+b.isIgnoreHierarchy());
			
			if(PapaFile.getPlanetaryAnnihilationDirectory()!=null)
				prop.setProperty("PapaFile.PADirectory", 			PapaFile.getPlanetaryAnnihilationDirectory().getAbsolutePath());
			
			try {
				prop.store(new FileOutputStream(Main.settingsFile), null);
			} catch (IOException e2) { e2.printStackTrace();}
		}
	});
	
	public FileHandler getFileHandler() {
		if(this.fileHandler == null) {
			fileHandler = new FileHandler(this);
		}
		return this.fileHandler;
	}
	
	private class FileWorker extends SwingWorker<Void,PapaFile> {

		private File[] files;
		protected ImmutableTextureSettings settings = null;
		protected ImportInterface importInterface = null;
		private boolean ignoreReprompt = false;
		private float subProgress = 0f;
		private float totalSubFiles = 0f;
		private AtomicInteger processedFiles = new AtomicInteger();
		private boolean optimize = false;
		private int optimizeCounter = 0, optimizeFactor=0;
		
		public FileWorker(File...files) {
			this.files = files;
			papaOptions.setMultiMode(files.length>1);
		}
		
		public float getSubProgress() {
			return subProgress;
		}
		
		@Override
		protected Void doInBackground() throws Exception {
			long time = System.nanoTime();
			SwingUtilities.invokeAndWait(() -> setReadingFiles(true));
			processedFiles.set(0);
			for(int i =0;i<files.length;i++) {
				setProgress((int)(100f/(float)files.length * i));
				
				importInterface = getFileHandler().determineInterface(files[i]);
				if(importInterface==null)
					continue;
				
				if(importInterface == getFileHandler().IMAGE_INTERFACE){
					if(! ignoreReprompt) {
						final int index = i;
						SwingUtilities.invokeAndWait(() -> settings = getTextureImportSettings(files[index]));
						ignoreReprompt = papaOptions.ignoreReprompt(); 
					}
					if(settings==null)
						continue;
				}
				
				ImportInfo info = new ImportInfo();
				info.setTextureSettings(settings);
				info.setActivityListener(new ActivityListener() {
					@Override
					public void onFoundAcceptableFile(File f, int currentTotal) {
						ribbonPanel.setNumberOfFoundFiles(currentTotal);
					}
					@Override
					public void onGotTotalFiles(int totalFiles) {
						ribbonPanel.startImport(totalFiles);
						totalSubFiles = totalFiles;
						if(totalSubFiles>=50) {
							optimize = true; // reloading the tree is expensive
							optimizeFactor = Math.min((int) ((totalSubFiles+100) / 50), 8); // min 3 files per reload, max 8
						}
					}
					@Override
					public void onEndProcessFile(File f, String threadName, boolean success) {
						int count = processedFiles.getAndAdd(1);
						subProgress = (float) count/ totalSubFiles;
						ribbonPanel.setNumberProcessedFiles(count);
					}
					@Override
					public void onRejectFile(File f, String reason) {}
					@Override
					public void onAcceptFile(PapaFile p) {
						publish(p);
					}
				});
				boolean wait = importInterface == getFileHandler().IMAGE_INTERFACE || i == files.length-1;
				ribbonPanel.setNumberOfFoundFiles(0);
				getFileHandler().readFiles(files[i], importInterface, info, true, wait);
				// TODO this is a bit of a band aid solution. Currently the file handler has no information on the progress of an import.
				// FileHandler must encapsulate the import into a class for better info. (this will also allow for >1 import at a time)
				if(wait)
					ribbonPanel.endImport();
			}
			
			SwingUtilities.invokeLater(() -> setReadingFiles(false));
			papaOptions.setMultiMode(false);
			System.out.println("Time: "+(double)(System.nanoTime()-time)/1000000d+" ms");
			return null;
		}
		
		@Override
		protected void process(List<PapaFile> chunks) {
			if(chunks.size()!=0) {
				for(PapaFile p : chunks)
					configSelector.addToTreeOrSelect(p,!optimize || optimizeCounter++%optimizeFactor==0 || optimizeCounter == totalSubFiles);
			}
		}
		
		@Override
		protected void done() {
			try {
				get();
			} catch (Exception e) {
				if(e.getClass()!=CancellationException.class)
					throw new RuntimeException(e);
			}
		}
	}
	
	private class ImageFileWorker extends FileWorker {
		private Image image;
		public ImageFileWorker(Image image) {
			this.image = image;
			papaOptions.setMultiMode(false);
		}
		
		@Override
		protected Void doInBackground() throws Exception {
			long time = System.nanoTime();
			SwingUtilities.invokeLater(() -> setReadingFiles(true));
			
			SwingUtilities.invokeAndWait(() -> settings = getTextureImportSettings(null));
			
			if(settings!=null) {
				
				ImportInfo info = new ImportInfo();
				info.setTextureSettings(settings);
				info.setInternalMode(true);
				info.setActivityListener(new ActivityListener() {
					@Override
					public void onRejectFile(File f, String reason) {}
					@Override
					public void onAcceptFile(PapaFile p) {
						p.setFileLocation(null);
						publish(p);
					}
				});
				
				File f = File.createTempFile("PTexEditImport", ".png");
				ImageIO.write((RenderedImage) image, "png", f);
				try {
					getFileHandler().readFiles(f, getFileHandler().IMAGE_INTERFACE, info, false, true);
				} finally {
					f.delete();
				}
				
				if(info.getNumAcceptedFiles()==0)
					showError("Unable to paste image: " + info.getRejectedFileReasons()[0], "Paste error", new Object[] {"Ok"}, "Ok");
			}
			
			SwingUtilities.invokeLater(() -> setReadingFiles(false));
			System.out.println("Time: "+(double)(System.nanoTime()-time)/1000000d+" ms");
			return null;
		}
	}
	
	public synchronized void startOperation() {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		loadCounter++;
	}
	
	public synchronized void endOperation() {
		loadCounter--;
		if(loadCounter==0)
			setCursor(Cursor.getDefaultCursor());
	}
	
	public void readAll(File...files) {
		FileWorker fw = new FileWorker(files);
		fw.execute();
	}
	
	public void readImage(Image image) {
		FileWorker fw = new ImageFileWorker(image);
		fw.execute();
	}
	
	private void setReadingFiles(boolean b) {
		if(b && ! menu.readingFiles)
			startOperation();
		else if(!b && menu.readingFiles)
			endOperation();
		menu.setReadingFiles(b);
		boolean enable = !b;
		imagePanel.dragDropEnabled = enable;
	}

	public ImmutableTextureSettings getTextureImportSettings(File f) {
		papaOptions.setActiveFile(f);
		papaOptions.updateLinkOptions(getTargetablePapaFiles());
		papaOptions.showAt(getX() + getWidth() / 2, getY() + getHeight() / 2); // this blocks code execution at this point. neat!
		if(papaOptions.getWasAccepted())
			return papaOptions.getCurrentSettings();
		return null;
	}
	
	public void setActiveFile(PapaFile p) {
		if(p==null) {
			unloadFileFromConfig();
			return;
		}
		PapaFile old = activeFile;
		activeFile = p;
		int index = Math.max(0, configSection1.getActiveTextureIndex(activeFile));
		activeTexture = activeFile.getNumTextures()!=0 ? activeFile.getTexture(index) : null;
		
		boolean changed = old!=activeFile;
		menu.applySettings(activeFile, index, activeTexture ,changed);
		configSection1.applySettings(activeFile, index, activeTexture ,changed);
		configSection2.applySettings(activeFile, index, activeTexture ,changed);
		configSelector.applySettings(activeFile, index, activeTexture ,changed);
	}
	
	/*private void refreshActiveFile() {
		setActiveFile(activeFile);
		configSelector.selectedNodeChanged();
	}*/
	
	public void setActiveTexture(PapaTexture p) {
		activeTexture = p;
		setActiveFile(p.getParent());
	}
	
	public void unloadFileFromConfig() {
		activeFile = null;
		activeTexture = null;
		dependents.clear();
		dependencies.clear();
		menu.unload();
		configSection1.unload();
		configSection2.unload();
		configSelector.unload();
		imagePanel.unload();
		configSelector.fileTree.repaint();
	}
	
	private PapaFile[] getTargetablePapaFiles() {
		return configSelector.getTargetablePapaFiles();
	}

	
	
	
	
	private class ClipboardListener implements ClipboardOwner {
		@Override
		public void lostOwnership(Clipboard clipboard, Transferable contents) {
			// TODO
		}
	}
	
	public int showError(Object message, String title, Object[] options, Object Default) {
		exclamationSound();
		System.err.println(message);
		return JOptionPane.showOptionDialog(this, message,
	             title, JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
	             null, options, Default);
	}
	
	public int optionBox(Object message, String title, Object[] options, Object Default) {
		return JOptionPane.showOptionDialog(this, message,
	             title, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
	             null, options, Default);
	}
	
	public final UncaughtExceptionHandler onThreadException = new UncaughtExceptionHandler() {
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			Thread.setDefaultUncaughtExceptionHandler(null); // prevent other errors from cascading if total failure
			getFileHandler().cancelActiveTask();
			String message = "Fatal error in thread "+t.getName()+":\n"+e.getClass().getName()+": "+e.getMessage()+"\n";
			Throwable cause = e;
			for(StackTraceElement element : e.getStackTrace())
				message+="\n"+element.toString();
			while(cause.getCause()!=null) {
				cause = cause.getCause();
				message+="\n\nCaused by:\n"+cause.getClass().getName()+": "+cause.getMessage()+"\n";
				for(StackTraceElement element : cause.getStackTrace())
					message+="\n"+element.toString();
			}
			
			
			JTextArea jte = new JTextArea();
			jte.setEditable(false);
			jte.setText(message);
			jte.setCaretPosition(0);
			
			JScrollPane jsp = new JScrollPane(jte,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			jsp.setPreferredSize(new Dimension(650,350));
			
			showError(jsp, "Fatal Error", new Object[] {"Exit"}, "Exit");
			System.err.println(message);
			System.exit(-1);
		}
	};
		
	public static void exclamationSound() {
		Toolkit.getDefaultToolkit().beep();
	}
}
