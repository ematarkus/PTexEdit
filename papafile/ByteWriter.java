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

public class ByteWriter {
	
	private int index = 0;
	private byte[] data;
	public ByteWriter(byte[] data) {
		this.data = data;
	}
	
	public void skip(int number) {
		index+=number;
	}
	
	public void seek(int loc) {
		index = loc;
	}
	
	public void write(byte[] bytes) {
		System.arraycopy(bytes, 0, data, index, bytes.length);
		index+=bytes.length;
	}
	
	public void write(long bytes, int amount) {
		for(int i=0;i<amount;i++)
			data[index++] = (byte) ((bytes>>>(i*8) & 0b11111111));
	}
	
	public void write(long bytes) {
		write(bytes,8);
	}
	
	public void write(int bytes) {
		write(bytes,4);
	}
	
	public void write(short bytes) {
		write(bytes,2);
	}
	
	public void write(byte bytes) {
		data[index++] = bytes;
	}
	
	public void write(String s) {
		byte[] bytes = s.getBytes();
		write(bytes);
	}
	
	public void writeTo(byte[] bytes, int loc) {
		System.arraycopy(bytes, 0, data, loc, bytes.length);
		index+=bytes.length;
	}
	
	public void writeTo(long bytes, int amount, int loc) {
		int ind = loc;
		for(int i=0;i<amount;i++)
			data[ind++] = (byte) ((bytes>>>(i*8) & 0b11111111));
	}
	
	public void writeTo(long bytes, int loc) {
		writeTo(bytes,8,loc);
	}
	
	public void writeTo(int bytes, int loc) {
		writeTo(bytes,4,loc);
	}
	
	public void writeTo(short bytes, int loc) {
		writeTo(bytes,2,loc);
	}
	
	public void writeTo(byte bytes, int loc) {
		data[loc] = bytes;
	}
	
	public void writeTo(String s, int loc) {
		byte[] bytes = s.getBytes();
		writeTo(bytes,loc);
	}
	
	public int index() {
		return index;
	}

	public int getSize() {
		return data.length;
	}
	public byte[] getData() {
		return data;
	}
}