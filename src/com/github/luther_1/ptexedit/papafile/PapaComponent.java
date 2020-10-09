/*
 *  This file is part of PapaFile
 * 
 *  File IO tools for Planetary Annihilation's papa files.
 *  Copyright (C) 2020 Marcus Der <marcusder@hotmail.com>
 * 
 *  PapaFile is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  PapaFile is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with PapaFile.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.github.luther_1.ptexedit.papafile;

import java.nio.ByteBuffer;

import com.github.luther_1.ptexedit.papafile.PapaFile.BuildNotification;

public abstract class PapaComponent {
	
	protected ByteBuffer header = null, data = null; // it is vital that the size of data is the same size that getBody() returns.
	
	protected int ceilEight(int value) {
		double val = value;
		val/=8;
		val = Math.ceil(val);
		return (int)val * 8;
	}
	
	protected int ceilNextEight(int value) {
		return ceilEight(value + 1);
	}
	
	protected abstract BuildNotification[] validate();
	
	protected abstract int headerSize();
	
	protected abstract int bodySize();
	
	protected int componentSize() {
		return headerSize() + bodySize();
	}
	
	protected abstract void build();
	
	protected abstract void applyOffset(int offset);
	
	protected byte[] getHeaderBytes() {
		return header.array();
	}
	
	protected byte[] getDataBytes() {
		return data.array();
	}
	
	public void overwrite(PapaComponent other) {
		if(other.getClass() != this.getClass())
			throw new IllegalArgumentException("Cannot overwrite "+this.getClass().getName()+" with "+other.getClass().getName());
		overwriteHelper(other);
	}
	
	protected abstract void overwriteHelper(PapaComponent other);
	
	protected abstract boolean isDependentOn(PapaComponent other);
	
	public abstract void detach();
	
	public void attach(PapaFile newParent) {
		if(getParent() != null) {
			detach();
		}
		setParent(newParent);
	}
	
	protected abstract void setParent(PapaFile newParent);
	
	public abstract PapaComponent duplicate();
	
	public abstract PapaFile getParent();
	
	public abstract PapaComponent[] getDependencies();
	
	public abstract PapaComponent[] getDependents();
	
	public abstract void flush();
	
	
	
}
