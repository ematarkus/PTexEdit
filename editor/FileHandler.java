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
package editor;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.table.DefaultTableModel;

import papafile.*;
import papafile.PapaTexture.*;

public class FileHandler {
	
	private static LinkedBlockingQueue<Runnable> queue;
	private static ThreadPoolExecutor executor;
	private static Object lock = new Object();
	private static Vector<Future<Void>> tasks;
	private static FileNameExtensionFilter[] imageFilters;
	private static FileNameExtensionFilter papaFilter = new FileNameExtensionFilter("Planetary Annihilation File (*.papa)", "papa");
	
	private static void initializeThreadPool() {
		 queue = new LinkedBlockingQueue<Runnable>();
		 executor = new ThreadPoolExecutor(4, 4, 500, TimeUnit.MILLISECONDS, queue);
		 tasks = new Vector<Future<Void>>();
	}
	
	static {
		initializeThreadPool();
		generateFileNameFilters();
	}
	
	
	public static void readFiles(File f, ImportInterface importInterface, ImportInfo info) throws InterruptedException {
		readFiles(f, importInterface, info, true, false);
	}
	
	public static void readFiles(File f, ImportInterface importInterface, ImportInfo info, boolean recursive, boolean wait) throws InterruptedException {
		synchronized(executor) { // do not let more than one thread call readFiles at the same time.
			if(!f.exists()) {
				Editor.showError("File does not exist", "IO Error", new Object[] {"Ok"}, "Ok");
				return;
			}
			readFilesInternal(f, importInterface, info,recursive, wait);
		}
	}
	
	private static int determineMode(File f) {
		File[] files = f.listFiles();
		int papa = 0;
		int image = 0;
		int unknown = 0;
		int total = 0;
		for(File file : files) {
			total++;
			if(! file.isDirectory()) {
				if(PAPA_INTERFACE.filter(file))
					papa++;
				else if(IMAGE_INTERFACE.filter(file)){
					image++;
				} else {
					unknown++;
				}
			}
		}
		if(total == unknown) // nothing but unrecognized
			return 0;
		if(unknown!=0)
			return 1;
		if(papa!=0 && image==0)
			return 2;
		if(papa==0 && image!=0)
			return 3;
		
		return 1;
	}
	
	private static void readFilesInternal(File f, ImportInterface importInterface, ImportInfo info,boolean recursive, boolean wait) throws InterruptedException {
		
		info.resetStatistics();  // in case of reuse
		ArrayList<File> toParse = getValidFiles(f, importInterface, info, recursive);
		info.setTotalFileCount(toParse.size());
		
		if(f.isDirectory()) {
			if(! info.isInternalMode() && ! checkMassFiles(f,toParse,importInterface))
				return;
			info.setDirectoryMode();
		} else {
			if(toParse.size()==0)
				importInterface.rejectFile(f, info, f.getName()+" is not a"+(importInterface == IMAGE_INTERFACE ? "n ":" ")+importInterface.getType() + " file");
		}
		
		toParse.stream().forEach((File f2) -> tasks.add(executor.submit(importInterface.getCallable(f2, info))));
		
		if(wait || info.isDirectoryMode()) {
			try {
				for(Future<Void> result : tasks)
					result.get();
			} catch (InterruptedException e) {
				throw e;
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			} catch (CancellationException e) {} 
			finally {
				synchronized(tasks) {
					tasks.clear();
				}
			}
			if( ! info.isInternalMode() && info.isDirectoryMode() && (info.getNumRejectedFiles()!=0 || info.getNumAcceptedFiles() == 0)) {
				JComponent t = getErrorTable(info.getRejectedFiles(),info.getRejectedFileReasons());
				if(info.getNumAcceptedFiles()==0) {
					if(info.getNumRejectedFiles()!=0)
						Editor.showError(t, "Unable to read any "+importInterface.getType()+" files from the folder", new Object[] {"Ok"}, "Ok");
					else
						Editor.showError("Unable to find any "+importInterface.getType()+" files in the folder", "Invalid Input", new Object[] {"Ok"}, "Ok");
				} else {
					Editor.showError(t, "Import Error Summary", new Object[] {"Ok"}, "Ok");
				}
			}
		}
	}
	
	private static JComponent getErrorTable(File[] rejectedFiles, String[] reasons) {
		JTable j = new JTable();
		JScrollPane jsp = new JScrollPane(j, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		DefaultTableModel model = (DefaultTableModel) j.getModel();
		model.addColumn("File");
		model.addColumn("Reason");
		jsp.setPreferredSize(new Dimension(950,150));
		j.getColumnModel().getColumn(0).setPreferredWidth(300);
		j.getColumnModel().getColumn(1).setPreferredWidth(650);
		
		for(int i =0;i<rejectedFiles.length;i++)
			model.addRow(new Object[] {rejectedFiles[i].getName(),reasons[i]});
		
		return jsp;
		
	}

	private static ArrayList<File> getValidFiles(File f, ImportInterface importInterface, ImportInfo info, boolean recursive) throws InterruptedException {
		try {
			ArrayList<File> list = new ArrayList<File>();
			getValidFiles(list,f,importInterface, info,recursive);
			return list;
		} catch (InterruptedException e) {
			throw e;
		}
	}
	
	private static void getValidFiles(ArrayList<File> list, File f, ImportInterface importInterface, ImportInfo info, boolean recursive) throws InterruptedException {
		
		if(Thread.currentThread().isInterrupted())
			throw new InterruptedException();
		
		if(f.isDirectory() && recursive) {
			File [] files = f.listFiles();
			
			for(File f2 : files)
				getValidFiles(list,f2,importInterface, info, recursive);
		} else if(importInterface.filter(f)) {
			list.add(f);
			info.activityListener.onFoundAcceptableFile(f, list.size());
		}
		
	}
	
	public static void cancelActiveTask() {
		queue.clear();
		executor.shutdownNow();
		try {
			executor.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {}
		
		synchronized(tasks) {
			for(Future<Void> f : tasks)
				f.cancel(false);
		}
		
		initializeThreadPool();
	}
	
	private static boolean checkMassFiles(File source, ArrayList<File> files, ImportInterface importInterface) {
		int count = files.size();
		if(count > 100) {
			synchronized(lock) {
				int result = Editor.showError("You are attempting to open " + count + " " + importInterface.getType()
						+ " files.\nAre you sure you want to continue?\n(you may want to use tools => convert folder)", source.getAbsolutePath(), new Object[] {"Yes","No" }, "No");
				if(result!=0)
					return false;
			}
		}
		return true;
	}
	
	private static String getExtension(File f) {
		String s = f.getPath();
		int index = s.lastIndexOf('.');
		
		if(index ==-1)
			return "";
		return s.substring(index+1).toLowerCase();
	}
	
	public static final ImportInterface PAPA_INTERFACE = new ImportInterface() {

		@Override
		public boolean filter(File file) {
			return file.getPath().endsWith("papa");
		}

		@Override
		public Callable<Void> getCallable(File file, ImportInfo info) {
			return new Callable<Void>() {
				@Override
				public Void call() {
					info.onStartProcessFile(file, Thread.currentThread().getName());
					String path = file.getPath();
					PapaFile papaFile;
					try {
						papaFile = new PapaFile(path);
					} catch (IOException e) {
						rejectFile(file, info, e.getMessage());
						return null;
					}
					
					if(papaFile.getNumTextures()==0) {
						rejectFile(file, info, "Papa file contains no images");
						return null;
					}
					
					info.accept(file, papaFile);
					return null;
				}
				
			};
		}
		
		@Override
		public String getType() {
			return "papa";
		}
	};
	
	public static final ImportInterface IMAGE_INTERFACE = new ImportInterface() {
		
		private String[] extensions = ImageIO.getReaderFileSuffixes();

		{
			Arrays.sort(extensions);
		}
		
		@Override
		public boolean filter(File file) {
			return Arrays.binarySearch(extensions, getExtension(file)) >= 0;
		}

		@Override
		public Callable<Void> getCallable(File file, ImportInfo info) {
			return new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					info.onStartProcessFile(file, Thread.currentThread().getName());
					BufferedImage b;
					PapaFile p;
					
					ImmutableTextureSettings settings = info.getTextureSettings();
					boolean hasTarget = settings.linkEnabled;
					boolean link = settings.linkMethod==TextureSettings.LINK_TYPE_REFERENCE;
					p = new PapaFile();
					p.setFileLocation(file);
					
					try {
						b = ImageIO.read(file);
						p.addTexture(b, info.getTextureSettings());
						
						if(hasTarget) { // this is a bit messy
							PapaTexture t = p.getTexture(0);
							p = settings.linkTarget;
							if(link)
								p.generateLinkedTexture(t);
							else
								t.attach(p);
						}
						
					} catch (IOException | NullPointerException e) { // NPE is quick solution for unload while converting
						e.printStackTrace();
						rejectFile(file, info, e.getClass().getName()+": "+e.getMessage());
						return null;
					}
					
					info.accept(file, p);
					return null;
				}
				
			};
		}

		@Override
		public String getType() {
			return "image";
		}
	};
	
	public static abstract class ImportInterface {
		public abstract boolean filter(File file);
		public abstract Callable<Void> getCallable(File file, ImportInfo info);
		public abstract String getType();
		public final void rejectFile(File file, ImportInfo info, String reason) {
			info.reject(file, reason);
			if(!info.isDirectoryMode()) {
				synchronized(lock) {
					Editor.showError(reason, "Error: " + file.getName(), new Object[] {"Ok"}, "Ok");
				}
			}
		}
	}
	
	public static class ImportInfo {
		private static ActivityListener defaultResultManager = new ActivityListener() {
			@Override
			public void onRejectFile(File f, String reason) {}
			@Override
			public void onAcceptFile(PapaFile p) {}
		};
		
		private boolean directoryMode, internalMode;
		private AtomicInteger acceptedFiles = new AtomicInteger();
		private AtomicInteger rejectedFiles  = new AtomicInteger();
		private ImmutableTextureSettings importSettings;
		private ActivityListener activityListener = ImportInfo.defaultResultManager;
		
		private Vector<File> rejectedFileList = new Vector<File>();
		private Vector<String> rejectedFileReasons = new Vector<String>();
		
		public void setTextureSettings(ImmutableTextureSettings t) {
			if(importSettings!=null)
				throw new IllegalStateException("Cannot modify TextureImportSettings");
			importSettings = t;
		}
		
		public void onStartProcessFile(File file, String name) {
			activityListener.onStartProcessFile(file, name);
		}

		public void setTotalFileCount(int size) {
			activityListener.onGotTotalFiles(size);
		}

		public ImmutableTextureSettings getTextureSettings() {
			return importSettings;
		}
		
		public void accept(File f, PapaFile p) {
			activityListener.onEndProcessFile(f, Thread.currentThread().getName(), true);
			activityListener.onAcceptFile(p);
			acceptedFiles.addAndGet(1);
		}
		
		public void reject(File f, String reason) {
			rejectedFileList.add(f);
			rejectedFileReasons.add(reason);
			activityListener.onEndProcessFile(f, Thread.currentThread().getName(), false);
			activityListener.onRejectFile(f, reason);
			rejectedFiles.addAndGet(1);
		}
		
		public void setDirectoryMode() {
			directoryMode = true;
		}
		
		public boolean isDirectoryMode() {
			return directoryMode;
		}
		
		public void setActivityListener(ActivityListener m) {
			this.activityListener = m;
		}
		
		public int getNumRejectedFiles() {
			return rejectedFiles.get();
		}
		
		public int getNumAcceptedFiles() {
			return acceptedFiles.get();
		}
		
		public File[] getRejectedFiles() {
			return rejectedFileList.toArray(new File[rejectedFileList.size()]);
		}
		
		public String[] getRejectedFileReasons() {
			return rejectedFileReasons.toArray(new String[rejectedFileReasons.size()]);
		}
		
		public void setInternalMode(boolean internal) {
			this.internalMode = internal;
		}
		
		public boolean isInternalMode() {
			return this.internalMode;
		}
		
		private void resetStatistics() {
			directoryMode = false;
			acceptedFiles.set(0);
			rejectedFiles.set(0);
			rejectedFileList.clear();
			rejectedFileReasons.clear();
		}
		
		public static abstract class ActivityListener {
			public void onFoundAcceptableFile(File f, int currentTotal) {};
			public void onGotTotalFiles(int totalFiles) {};
			public void onStartProcessFile(File f, String threadName) {};
			public void onEndProcessFile(File f, String threadName, boolean success) {}
			public abstract void onAcceptFile(PapaFile p);
			public abstract void onRejectFile(File f, String reason);
		}
	}
	
	public static boolean checkIsInPA(File f) {
		try {
			return f.getCanonicalPath().contains(PapaFile.getPlanetaryAnnihilationDirectory().getCanonicalPath() + File.separator);
		} catch (IOException e) {
			return false;
		}
	}
	
	public static boolean isPapa(File f) {
		return PAPA_INTERFACE.filter(f);
	}
	
	public static ImportInterface determineInterface(File f) {
		if(!f.exists())
			throw new IllegalArgumentException("File does not exist");
		if(f.isFile())
			if(isPapa(f))
				return PAPA_INTERFACE;
			else
				return IMAGE_INTERFACE;
		
		int mode = determineMode(f);
		
		if(mode==0) {
			Editor.showError("No recognized files in the directory", "Invalid Directory", new Object[] {"Ok"}, "Ok");
			return null;
		}
		if(mode==1) {
			int result = Editor.optionBox("What mode would you like to open the directory in?", f.getAbsolutePath(), new Object[] {"Papa","Image","Cancel"}, "Cancel");
			if(result==-1 || result == 2)
				return null;
			mode = result + 2;
		}
		if(mode==2)
			return PAPA_INTERFACE;
		else
			return IMAGE_INTERFACE;
	}
	
	private static File enforceExtension(File input, FileNameExtensionFilter filter) {
		String[] extensions = filter.getExtensions();
		String selectedExtension = null;
		String path = input.getPath();
		String pathLower = path.toLowerCase();
		for(String s : extensions)
			if(pathLower.endsWith(s))
				selectedExtension = "."+s;
		
		if(selectedExtension == null) {
			if(extensions.length!=1)
				return null;
			selectedExtension = "."+extensions[0];
		}
		
		if(!path.endsWith(selectedExtension)) {
			if(pathLower.endsWith(selectedExtension))
				return new File(path.substring(0,path.length()-5)+selectedExtension);
			else
				return new File(path+selectedExtension);
		}
		return input;
		
	}

	public static void writeFile(PapaFile target, File location) throws IOException {
		
		FileOutputStream fos = null;
		try {
			location.createNewFile();
			fos = new FileOutputStream(location);
			target.build();
			byte[] data = target.getFileBytes();
			fos.write(data);
		} catch (IOException e1) {
			throw e1;
		} finally {
			try {
				fos.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public static void exportImage(PapaTexture tex, File file) throws IOException {
		String ext = file.getName().substring(file.getName().lastIndexOf(".") + 1);
		BufferedImage toWrite = tex.getImage();
		if(ext.equals("jpeg") || ext.equals("jpg") || ext.equals("bmp") || ext.equals("wbmp")) {
			toWrite = toModel(toWrite,BufferedImage.TYPE_INT_RGB);
		}
		try {
			if(!ImageIO.write(toWrite, ext, file))
				throw new IOException("File extension "+ext+" is not supported for export.");
		} catch (IOException e) {
			throw e;
		}
	}
	
	private static BufferedImage toModel(BufferedImage input, int model) {
		BufferedImage tmp = new BufferedImage(input.getWidth(),input.getHeight(),model);
		int[] data = new int[input.getWidth()*input.getHeight()];
		input.getRGB(0, 0, input.getWidth(), input.getHeight(), data, 0, input.getWidth());
		tmp.setRGB(0, 0, input.getWidth(), input.getHeight(), data, 0, input.getWidth());
		return tmp;
	}
	
	public static FileNameExtensionFilter[] getImageFilters() {
		return imageFilters;
	}
	
	public static FileNameExtensionFilter getPapaFilter() {
		return papaFilter;
	}
	
	private static void generateFileNameFilters() {
		String[] filters = ImageIO.getWriterFileSuffixes();
		imageFilters = new FileNameExtensionFilter[filters.length + 1];
		imageFilters[0] = new FileNameExtensionFilter("Image Files (*."+String.join(", *.", ImageIO.getReaderFileSuffixes())+")", ImageIO.getReaderFileSuffixes());
		for(int i = 0;i<filters.length;i++)
			imageFilters[i + 1] = new FileNameExtensionFilter(filters[i].toUpperCase()+" Files (*."+filters[i]+")", filters[i]);
	}

	public static void saveFileTo(PapaFile target, File selectedFile) throws IOException {
		File file = enforceExtension(selectedFile, papaFilter);
		
		if(file.exists())
			if(Editor.showError(file.getName() +" already exists.\nDo you want to replace it?", "Confirm Save As", new Object[] {"Yes","No"}, "No") != 0)
				return;
		
		if(!confirmDataLoss(target))
			return;
		
		target.setFileLocation(file);
		writeFile(target,target.getFile());
	}
	
	public static boolean confirmDataLoss(PapaFile target) {
		if(target.containsNonImageData()) //TODO temporary workaround
			if(Editor.showError(target.getFileName()+" contains non image data which is not supported."
					+ "\nSaving will erase all non image data! Are you sure you want to continue?", 
					"Unsupported Operations", new Object[] {"Yes","No"}, "No") != 0)
				return false;
		return true;
	}

	public static void exportImageTo(PapaTexture activeTexture, File selectedFile, FileNameExtensionFilter filter) throws IOException {
		File file = enforceExtension(selectedFile,filter);
		
		int index = selectedFile.getName().lastIndexOf(".") + 1;
		String ext = "";
		if(index!=0)
			ext = selectedFile.getName().substring(index).toLowerCase();
		
		if(file==null) {
			Editor.showError("Extension "+ext+" does not match any known extensions", "Export error", new Object[] {"Ok"}, "Ok");
			return;
		}
		if(file.exists())
			if(Editor.showError(file.getName() +" already exists.\nDo you want to replace it?", "Confirm Export", new Object[] {"Yes","No"}, "No") != 0)
				return;
		exportImage(activeTexture,file);
	}
	
}
