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
	private static final String APPLICATION_NAME = "PTexEdit";
	public static final String VERSION_STRING = "0.3";
	public static final String BUILD_DATE = "August 1, 2020";
	private static final File settingsFile = new File(System.getProperty("user.home") + 
						File.separatorChar+APPLICATION_NAME+File.separatorChar+APPLICATION_NAME+".properties");
	public static Editor APPLICATION_WINDOW;
	
	private static final Properties prop = new Properties();
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
	private JSplitPane mainPanel;
	JScrollBar horizontal;
	public JScrollBar vertical;
	private JPanel box;
	public MenuBar menu;
	
	private PapaOptions papaOptions;
	public BatchConvert batchConvert;
	
	private int loadCounter = 0;
	
	public static void main(String[] args) {
		applyPlatformChanges();
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				APPLICATION_WINDOW = new Editor();
				
				readAndApplyConfig();
				addShutdownHooks();
				addUncaughtExceptionHandler();
				
				APPLICATION_WINDOW.setTitle(APPLICATION_NAME);
				APPLICATION_WINDOW.setVisible(true);
			}
		});
	}
	
	
	
	private static void readAndApplyConfig() {
		if(!settingsFile.exists()) {
			settingsFile.getParentFile().mkdirs();
			try {
				settingsFile.createNewFile();
			} catch (IOException e) {}
		}
		
		FileInputStream fis = null;
		
		try {
			fis = new FileInputStream(settingsFile);
			prop.load(fis);
		} catch (IOException e) {
			System.err.println("Could not load settings file.");
		} finally {
			try {
				fis.close();
			} catch (IOException e) {}
		}
		
		Editor e = APPLICATION_WINDOW;
		e.setLocation(Integer.valueOf(prop.getProperty("Application.Location.X", "100")), Integer.valueOf(prop.getProperty("Application.Location.Y", "100")));
		e.setBounds(Integer.valueOf(prop.getProperty("Application.Location.X", "100")), Integer.valueOf(prop.getProperty("Application.Location.Y", "100")),
					Integer.valueOf(prop.getProperty("Application.Size.Width", "1060")), Integer.valueOf(prop.getProperty("Application.Size.Height", "800")));
		e.mainPanel.setDividerLocation(Integer.valueOf(prop.getProperty("Application.SplitPane.Location", "395")));
		e.setExtendedState(Integer.valueOf(prop.getProperty("Application.State", ""+JFrame.NORMAL)));
		
		e.menu.setSelectedRadioButton(Integer.valueOf(prop.getProperty("Application.Menu.View.Channels", "0")));
		if(Boolean.valueOf(prop.getProperty("Application.Menu.View.Luminance", "false")))
			e.menu.mViewLuminance.doClick(0); // The fastest click in the west.
		if(Boolean.valueOf(prop.getProperty("Application.Menu.View.Alpha", "false")))
			e.menu.mViewNoAlpha.doClick(0);
		if(Boolean.valueOf(prop.getProperty("Application.Menu.View.Tile", "false")))
			e.menu.mViewTile.doClick(0);
		if(Boolean.valueOf(prop.getProperty("Application.Menu.View.DXT", "false")))
			e.menu.mViewDXT.doClick(0);
		if(Boolean.valueOf(prop.getProperty("Application.Menu.Options.ShowRoot", "false")))
			e.menu.mOptionsShowRoot.doClick(0);
		if(Boolean.valueOf(prop.getProperty("Application.Menu.Options.AllowEmpty", "false")))
			e.menu.mOptionsAllowEmpty.doClick(0);
		if(Boolean.valueOf(prop.getProperty("Application.Menu.Options.SuppressWarnings", "false")))
			e.menu.mOptionsAllowEmpty.doClick(0);
		//e.maxThreads = Integer.valueOf(prop.getProperty("Application.Config.MaxThreads", "4"));
		
		TextureSettings t = new TextureSettings();
		TextureSettings def = TextureSettings.defaultSettings();
		
		t.setFormat(prop.getProperty("PapaOptions.Format", def.getFormat()));
		t.setCompressionMethod(CompressionMethod.valueOf(prop.getProperty("PapaOptions.DxtMethod", ""+def.getCompressionMethod())));
		t.setGenerateMipmaps(Boolean.valueOf(prop.getProperty("PapaOptions.GenMipmaps", ""+def.getGenerateMipmaps())));
		t.setMipmapResizeMethod(Integer.valueOf(prop.getProperty("PapaOptions.MipmapResizeMethod", ""+def.getMipmapResizeMethod())));
		t.setSRGB(Boolean.valueOf(prop.getProperty("PapaOptions.SRGB", ""+def.getSRGB())));
		t.setResize(Boolean.valueOf(prop.getProperty("PapaOptions.Resize", ""+def.getResize())));
		t.setResizeMethod(Integer.valueOf(prop.getProperty("PapaOptions.ResizeMethod", ""+def.getResizeMethod())));
		t.setResizeMode(Integer.valueOf(prop.getProperty("PapaOptions.ResizeMode", ""+def.getResizeMode())));
		
		PapaFile.setPADirectory(prop.getProperty("PapaFile.PADirectory",null)!=null ? new File(prop.getProperty("PapaFile.PADirectory")) : null);
		
		e.papaOptions = new PapaOptions(e, t.immutable());
		e.batchConvert = new BatchConvert(e, e.papaOptions);
		
		e.batchConvert.setRecursive(Boolean.valueOf(prop.getProperty("BatchConvert.Recursive", ""+true)));
		e.batchConvert.setWriteLinkedFiles(Boolean.valueOf(prop.getProperty("BatchConvert.WriteLinked", ""+false)));
		e.batchConvert.setOverwrite(Boolean.valueOf(prop.getProperty("BatchConvert.Overwrite", ""+false)));
		e.batchConvert.setIgnoreHierarchy(Boolean.valueOf(prop.getProperty("BatchConvert.IgnoreHierarchy", ""+false)));
	}
	
	private static void addShutdownHooks() {
		Runtime.getRuntime().addShutdownHook(onExit);
	}
	
	private static void addUncaughtExceptionHandler() {
		Thread.setDefaultUncaughtExceptionHandler(onThreadException);
	}
	
	private static void applyPlatformChanges() {
		float size = 12f;
		String name = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
		if(name.contains("linux") || name.contains("unix"))
			size = 10f;
		else if(name.contains("mac"))
			size = 11f;
		try {
			UIManager.put("Label.font", UIManager.getFont("Label.font").deriveFont(size));
			UIManager.put("CheckBox.font", UIManager.getFont("CheckBox.font").deriveFont(size));
			UIManager.put("ComboBox.font", UIManager.getFont("ComboBox.font").deriveFont(size));
			UIManager.put("Button.font", UIManager.getFont("Button.font").deriveFont(size));
			UIManager.put("RadioButton.font", UIManager.getFont("RadioButton.font").deriveFont(size));
		} catch (NullPointerException e) {
			System.err.println("Failed to set font");
		}
		
	}
	
	private static final UncaughtExceptionHandler onThreadException = new UncaughtExceptionHandler() {
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			Thread.setDefaultUncaughtExceptionHandler(null); // prevent other errors from cascading if total failure
			FileHandler.cancelActiveTask();
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
			
			Editor.showError(jsp, "Fatal Error", new Object[] {"Exit"}, "Exit");
			System.err.println(message);
			System.exit(-1);
		}
	};
	
	private static final Thread onExit = new Thread(new Runnable() {
		
		public void run() {
			if(!settingsFile.exists())
				return;
			
			Editor e = APPLICATION_WINDOW;
			int state = e.getExtendedState();
			if(state!=JFrame.MAXIMIZED_BOTH && state!=JFrame.MAXIMIZED_HORIZ && state!=JFrame.MAXIMIZED_VERT) {
				prop.setProperty("Application.Location.X", 			""+(int)e.getX());
				prop.setProperty("Application.Location.Y", 			""+(int)e.getY());
				prop.setProperty("Application.Size.Width",			""+(int)e.getWidth());
				prop.setProperty("Application.Size.Height", 		""+(int)e.getHeight());
			}
			prop.setProperty("Application.SplitPane.Location", 				""+(int)e.mainPanel.getDividerLocation());
			prop.setProperty("Application.State", 							""+e.getExtendedState());
			prop.setProperty("Application.Menu.View.Channels", 				""+e.menu.getSelectedRadioButton());
			prop.setProperty("Application.Menu.View.Luminance", 			""+e.menu.mViewLuminance.isSelected());
			prop.setProperty("Application.Menu.View.Alpha", 				""+e.menu.mViewNoAlpha.isSelected());
			prop.setProperty("Application.Menu.View.Tile", 					""+e.menu.mViewTile.isSelected());
			prop.setProperty("Application.Menu.View.DXT", 					""+e.menu.mViewDXT.isSelected());
			prop.setProperty("Application.Menu.Options.ShowRoot",			""+e.menu.mOptionsShowRoot.isSelected());
			prop.setProperty("Application.Menu.Options.AllowEmpty",			""+e.menu.mOptionsAllowEmpty.isSelected());
			prop.setProperty("Application.Menu.Options.SuppressWarnings",	""+e.menu.mOptionsSuppressWarnings.isSelected());
			//prop.setProperty("Application.Config.MaxThreads", 	""+e.maxThreads);
			
			ImmutableTextureSettings settings = e.papaOptions.getCurrentSettings();
			
			prop.setProperty("PapaOptions.Format", 				""+settings.format);
			prop.setProperty("PapaOptions.DxtMethod", 			""+settings.method);
			prop.setProperty("PapaOptions.GenMipmaps", 			""+settings.generateMipmaps);
			prop.setProperty("PapaOptions.MipmapResizeMethod", 	""+settings.mipmapResizeMethod);
			prop.setProperty("PapaOptions.SRGB", 				""+settings.SRGB);
			prop.setProperty("PapaOptions.Resize", 				""+settings.resize);
			prop.setProperty("PapaOptions.ResizeMethod", 		""+settings.resizeMethod);
			prop.setProperty("PapaOptions.ResizeMode", 			""+settings.resizeMode);
			
			BatchConvert b = e.batchConvert;
			prop.setProperty("BatchConvert.Recursive", 			""+b.isRecursive());
			prop.setProperty("BatchConvert.WriteLinked", 		""+b.isWritingLinkedFiles());
			prop.setProperty("BatchConvert.Overwrite", 			""+b.isOverwrite());
			prop.setProperty("BatchConvert.IgnoreHierarchy", 	""+b.isIgnoreHierarchy());
			
			if(PapaFile.getPlanetaryAnnihilationDirectory()!=null)
				prop.setProperty("PapaFile.PADirectory", 			PapaFile.getPlanetaryAnnihilationDirectory().getAbsolutePath());
			
			try {
				prop.store(new FileOutputStream(settingsFile), null);
			} catch (IOException e2) { e2.printStackTrace();}
		}
	});
	
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
			SwingUtilities.invokeAndWait(() -> APPLICATION_WINDOW.setReadingFiles(true));
			processedFiles.set(0);
			for(int i =0;i<files.length;i++) {
				setProgress((int)(100f/(float)files.length * i));
				
				importInterface = FileHandler.determineInterface(files[i]);
				if(importInterface==null)
					continue;
				
				if(importInterface == FileHandler.IMAGE_INTERFACE){
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
				boolean wait = importInterface == FileHandler.IMAGE_INTERFACE || i == files.length-1;
				ribbonPanel.setNumberOfFoundFiles(0);
				FileHandler.readFiles(files[i], importInterface, info, true, wait);
				// TODO this is a bit of a band aid solution. Currently the file handler has no information on the progress of an import.
				// FileHandler must encapsulate the import into a class for better info. (this will also allow for >1 import at a time)
				if(wait)
					ribbonPanel.endImport();
			}
			
			SwingUtilities.invokeLater(() -> APPLICATION_WINDOW.setReadingFiles(false));
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
			SwingUtilities.invokeLater(() -> APPLICATION_WINDOW.setReadingFiles(true));
			
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
					FileHandler.readFiles(f, FileHandler.IMAGE_INTERFACE, info, false, true);
				} finally {
					f.delete();
				}
				
				if(info.getNumAcceptedFiles()==0)
					showError("Unable to paste image: " + info.getRejectedFileReasons()[0], "Paste error", new Object[] {"Ok"}, "Ok");
			}
			
			SwingUtilities.invokeLater(() -> APPLICATION_WINDOW.setReadingFiles(false));
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
	
	
	
	private class ClipboardListener implements ClipboardOwner {
		@Override
		public void lostOwnership(Clipboard clipboard, Transferable contents) {
			// TODO
		}
	}
	
	public static int showError(Object message, String title, Object[] options, Object Default) {
		exclamationSound();
		System.err.println(message);
		return JOptionPane.showOptionDialog(APPLICATION_WINDOW, message,
	             title, JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
	             null, options, Default);
	}
	
	public static int optionBox(Object message, String title, Object[] options, Object Default) {
		return JOptionPane.showOptionDialog(APPLICATION_WINDOW, message,
	             title, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
	             null, options, Default);
	}
		
	public static void exclamationSound() {
		Toolkit.getDefaultToolkit().beep();
	}
}
