/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.work;

import java.io.IOException;
import net.nexustools.data.AdaptorException;
import net.nexustools.data.annote.FieldStream;
import net.nexustools.io.DataInputStream;
import net.nexustools.io.DataOutputStream;
import net.nexustools.io.net.Packet;

/**
 *
 * @author kate
 */
public abstract class BaseWorkPacket<C extends WorkClient, S extends WorkServer> extends Packet<C, S> {
    
    @FieldStream(staticField = true)
    long workId;
    
    protected final long workId() {
        return workId;
    }

    @Override
    public void read(DataInputStream dataInput, C client) throws UnsupportedOperationException, IOException {
        // Reads @StreamField fields
        super.read(dataInput, client);
    }

    @Override
    public void write(DataOutputStream dataOutput, C client) throws UnsupportedOperationException, IOException {
        // Writes @StreamField CΩ
        super.write(dataOutput, client);
    }
    
}
