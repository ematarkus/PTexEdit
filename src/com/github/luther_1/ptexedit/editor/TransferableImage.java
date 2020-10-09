package com.github.luther_1.ptexedit.editor;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class TransferableImage implements Transferable {
	Image image;
	
	public TransferableImage(Image image) {
		this.image = image;
	}
	
	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] {DataFlavor.imageFlavor};
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		DataFlavor[] flavors = getTransferDataFlavors();
		for(DataFlavor d : flavors)
			if(d.equals(flavor))
				return true;
		return false;
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if(flavor.equals(DataFlavor.imageFlavor))
			if(image!=null)
				return image;
			else
				throw new IOException("Image is null");
		throw new UnsupportedFlavorException(flavor);
	}
	
}