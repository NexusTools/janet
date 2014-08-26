/*
 * janxutils is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 or any later version.
 * 
 * janxutils is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with janxutils.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package net.nexustools.janet;

import net.nexustools.data.annote.FieldStream;

/**
 *
 * @author kate
 */
public abstract class RefPacket<T, C extends Client, S extends Server> extends Packet<T, C, S> {
	
	@FieldStream(staticField = true)
	protected short refID;
	
	public RefPacket(short id) {
		refID = id;
	}
	public RefPacket() {}
	
	public static String refStr(short id) {
		String str = Integer.toHexString(id).toUpperCase();
		while(str.length() < 4)
			str = "0" + str;
		return "0x" + str;
	}
	
}
