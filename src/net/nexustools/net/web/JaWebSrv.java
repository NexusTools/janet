/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.web;

import java.io.IOException;
import net.nexustools.DefaultAppDelegate;
import net.nexustools.net.Server.Transport;
import net.nexustools.net.web.http.HTTPServer;
import net.nexustools.net.web.modules.FileModule;
import net.nexustools.runtime.RunQueue;
import net.nexustools.runtime.ThreadedRunQueue;

/**
 *
 * @author kate
 */
public class JaWebSrv extends DefaultAppDelegate {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        new JaWebSrv(args, 8080).mainLoop();
    }

    final WebServer webServer;
	final RunQueue threadedRunQueue;
    public JaWebSrv(String[] args, int port, Transport protocol, RunQueue runQueue) throws IOException {
        super(args, "JaWebSrv", "NexusTools", runQueue);
        webServer = new HTTPServer(new FileModule("/"), port, protocol, runQueue);
		threadedRunQueue = runQueue;
    }
    public JaWebSrv(String[] args, int port, Transport protocol) throws IOException {
		this(args, port, protocol, new ThreadedRunQueue("JaWebSrv", 4f));
    }
    public JaWebSrv(String[] args, int port) throws IOException {
		this(args, port, Transport.TCP);
    }

    @Override
    protected void launch(String[] args) {}

    public boolean needsMainLoop() {
        return false;
    }

	@Override
    public void mainLoop() {
        try {
            webServer.join();
        } catch (InterruptedException ex) {}
    }
    
}