package com.github.luther_1.ptexedit.editor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.github.luther_1.ptexedit.papafile.PapaFile;
import com.github.luther_1.ptexedit.papafile.PapaTexture.TextureSettings;
import com.github.memo33.jsquish.Squish.CompressionMethod;

public class Main {

	private static final String APPLICATION_NAME = "PTexEdit";
	public static final File settingsFile = new File(System.getProperty("user.home") + 
			File.separatorChar+APPLICATION_NAME+File.separatorChar+APPLICATION_NAME+".properties");
	public static final Properties prop = new Properties();
	
	public static void main(String[] args) {
		applyPlatformChanges();
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Editor editor = new Editor();
				
				readAndApplyConfig(editor);
				addShutdownHooks(editor);
				addUncaughtExceptionHandler(editor);
				
				editor.setTitle(APPLICATION_NAME);
				editor.setVisible(true);
			}
		});
	}
	
	private static void readAndApplyConfig(Editor editor) {
		if(!settingsFile.exists()) {
			settingsFile.getParentFile().mkdirs();
			try {
				settingsFile.createNewFile();
			} catch (IOException ex) {}
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
			} catch (IOException ex) {}
		}
		
		editor.setLocation(Integer.valueOf(prop.getProperty("Application.Location.X", "100")), Integer.valueOf(prop.getProperty("Application.Location.Y", "100")));
		editor.setBounds(Integer.valueOf(prop.getProperty("Application.Location.X", "100")), Integer.valueOf(prop.getProperty("Application.Location.Y", "100")),
					Integer.valueOf(prop.getProperty("Application.Size.Width", "1060")), Integer.valueOf(prop.getProperty("Application.Size.Height", "800")));
		editor.mainPanel.setDividerLocation(Integer.valueOf(prop.getProperty("Application.SplitPane.Location", "395")));
		editor.setExtendedState(Integer.valueOf(prop.getProperty("Application.State", ""+JFrame.NORMAL)));
		
		editor.menu.setSelectedRadioButton(Integer.valueOf(prop.getProperty("Application.Menu.View.Channels", "0")));
		if(Boolean.valueOf(prop.getProperty("Application.Menu.View.Luminance", "false")))
			editor.menu.mViewLuminance.doClick(0); // The fastest click in the west.
		if(Boolean.valueOf(prop.getProperty("Application.Menu.View.Alpha", "false")))
			editor.menu.mViewNoAlpha.doClick(0);
		if(Boolean.valueOf(prop.getProperty("Application.Menu.View.Tile", "false")))
			editor.menu.mViewTile.doClick(0);
		if(Boolean.valueOf(prop.getProperty("Application.Menu.View.DXT", "false")))
			editor.menu.mViewDXT.doClick(0);
		if(Boolean.valueOf(prop.getProperty("Application.Menu.Options.ShowRoot", "false")))
			editor.menu.mOptionsShowRoot.doClick(0);
		if(Boolean.valueOf(prop.getProperty("Application.Menu.Options.AllowEmpty", "false")))
			editor.menu.mOptionsAllowEmpty.doClick(0);
		if(Boolean.valueOf(prop.getProperty("Application.Menu.Options.SuppressWarnings", "false")))
			editor.menu.mOptionsAllowEmpty.doClick(0);
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
		
		editor.papaOptions = new PapaOptions(editor, t.immutable());
		editor.batchConvert = new BatchConvert(editor, editor.papaOptions);
		
		editor.batchConvert.setRecursive(Boolean.valueOf(prop.getProperty("BatchConvert.Recursive", ""+true)));
		editor.batchConvert.setWriteLinkedFiles(Boolean.valueOf(prop.getProperty("BatchConvert.WriteLinked", ""+false)));
		editor.batchConvert.setOverwrite(Boolean.valueOf(prop.getProperty("BatchConvert.Overwrite", ""+false)));
		editor.batchConvert.setIgnoreHierarchy(Boolean.valueOf(prop.getProperty("BatchConvert.IgnoreHierarchy", ""+false)));
	}
	
	private static void addShutdownHooks(Editor editor) {
		Runtime.getRuntime().addShutdownHook(editor.onExit);
	}
	
	private static void addUncaughtExceptionHandler(Editor editor) {
		Thread.setDefaultUncaughtExceptionHandler(editor.onThreadException);
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
}
