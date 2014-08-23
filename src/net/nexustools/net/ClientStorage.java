/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net;

import net.nexustools.concurrent.AbstractProp;
import net.nexustools.concurrent.Prop;

/**
 *
 * @author kate
 */
public class ClientStorage<T> extends AbstractProp<T> {
	
	final Class<?> ext;
	final Client target;
	public ClientStorage(Class<?> ext) {
		this(null, ext);
	}
	public ClientStorage(Client client, Class<?> ext) {
		this.ext = ext;
		target = client;
	}

	@Override
	public Prop directAccessor() {
		return (target == null ? Client.currentClient.get() : target).clientStorage.get(ext);
	}
	
}
