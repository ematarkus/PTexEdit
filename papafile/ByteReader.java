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

import java.util.LinkedList;

public class ByteReader {
	
	private LinkedList<Integer> location = new LinkedList<Integer>();
	private int index = 0;
	private byte[] data;
	public ByteReader(byte[] data) {
		this.data = data;
	}
	
	public void skip(int bytes) {
		index+=bytes;
	}
	
	public void seek(int location) {
		index = location;
	}
	
	public void push() {
		location.push(index);
	}
	
	public void pop() {
		index = location.pop();
	}
	
	public int read() {
		return (int)data[index++] & 0b11111111;
	}
	
	public int read(int bytes) {
		int ret = 0;
		for(int i=0;i<bytes;i++)
			ret |= (read()<<(i*8));
		return ret;
	}
	
	public byte[] readBytes(int bytes) {
		byte[] ret = new byte[bytes];
		System.arraycopy(data, index, ret, 0, bytes);
		index+=bytes;
		return ret;
	}
	
	public String readString(int length) {
		byte[] bytes = readBytes(length);
		return new String(bytes);
	}
	
	public byte[] data() {
		return data;
	}
	
	public int size() {
		return data.length;
	}
	
	public int remaining() {
		return data.length-index;
	}
	
	public int index() {
		return index;
	}
}