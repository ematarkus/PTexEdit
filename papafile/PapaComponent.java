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
package papafile;

public abstract class PapaComponent {
	
	protected ByteWriter header = null, data = null; // it is vital that the size of data is the same size that getBody() returns.
	
	protected int ceilEight(int value) {
		double val = value; // in the interest of matching PA, always make sure that this operation returns the next non equal multiple of eight
		val/=8;
		val = Math.ceil(val);
		return (int)val * 8;
	}
	
	protected int ceilNextEight(int value) {
		return ceilEight(value + 1);
	}
	
	protected abstract void validate();
	
	protected abstract int headerSize();
	
	protected abstract int bodySize();
	
	protected int componentSize() {
		return headerSize() + bodySize();
	}
	
	protected abstract void build();
	
	protected abstract void applyOffset(int offset);
	
	protected byte[] getHeaderBytes() {
		return header.getData();
	}
	
	protected byte[] getDataBytes() {
		return data.getData();
	}
	
	public void overwrite(PapaComponent other) {
		if(other.getClass() != this.getClass())
			throw new IllegalArgumentException("Cannot overwrite "+this.getClass().getName()+" with "+other.getClass().getName());
		overwriteHelper(other);
	}
	
	protected abstract void overwriteHelper(PapaComponent other);
	
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
	
	public abstract void flush();
	
	
	
}