package com.github.luther_1.ptexedit.resources;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import com.github.luther_1.ptexedit.editor.Editor;

public class MyIcons {
	private static final BufferedImage CHECKERBOARD = loadImageFromResources("checkerboard64x64.png");
	private static final BufferedImage ICON = loadImageFromResources("icon.png");
	private static final BufferedImage ICON_SMALL = loadImageFromResources("iconSmall.png");
	private static final ImageIcon IMAGE_ICON = new ImageIcon(ICON);
	private static final ImageIcon IMG_PAPAFILE = loadIconFromResources("papafile.png");
	private static final ImageIcon IMG_PAPAFILE_IMAGE = loadIconFromResources("papafileImage.png");
	private static final ImageIcon IMG_PAPAFILE_LINKED = loadIconFromResources("papafileLinked.png");
	private static final ImageIcon IMG_PAPAFILE_ERROR = loadIconFromResources("papafileError.png");
	private static final ImageIcon IMG_PAPAFILE_NO_LINKS = loadIconFromResources("papafileNoLinks.png");
	private static final ImageIcon IMG_PAPAFILE_UNSAVED = loadIconFromResources("papafileUnsaved.png");
	private static final ImageIcon PLUS_ICON = loadIconFromResources("plus.png");
	private static final ImageIcon MINUS_ICON = loadIconFromResources("minus.png");
	private static final ImageIcon UP_ARROW_ICON = loadIconFromResources("upArrow.png");
	
	private static BufferedImage loadImageFromResources(String name) {
		URL imageURL = getResourceURL(name);
		if (imageURL != null) {
		    try {
				return ImageIO.read(imageURL);
			} catch (IOException e) {}
		}
		return new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
	}
	
	private static ImageIcon loadIconFromResources(String name) {
		return new ImageIcon(getResourceURL(name));
	}
	
	private static URL getResourceURL(String name) {
		return Editor.class.getResource("/com/github/luther_1/ptexedit/resources/"+name);
	}

	public static final BufferedImage getCheckerboard() {
		return CHECKERBOARD;
	}

	public static final BufferedImage getIcon() {
		return ICON;
	}

	public static final BufferedImage getIconSmall() {
		return ICON_SMALL;
	}

	public static final ImageIcon getImageIcon() {
		return IMAGE_ICON;
	}

	public static final ImageIcon getImgPapafile() {
		return IMG_PAPAFILE;
	}

	public static final ImageIcon getImgPapafileImage() {
		return IMG_PAPAFILE_IMAGE;
	}

	public static final ImageIcon getImgPapafileLinked() {
		return IMG_PAPAFILE_LINKED;
	}

	public static final ImageIcon getImgPapafileError() {
		return IMG_PAPAFILE_ERROR;
	}

	public static final ImageIcon getImgPapafileNoLinks() {
		return IMG_PAPAFILE_NO_LINKS;
	}

	public static final ImageIcon getImgPapafileUnsaved() {
		return IMG_PAPAFILE_UNSAVED;
	}

	public static final ImageIcon getPlusIcon() {
		return PLUS_ICON;
	}

	public static final ImageIcon getMinusIcon() {
		return MINUS_ICON;
	}

	public static final ImageIcon getUpArrowIcon() {
		return UP_ARROW_ICON;
	}
}
