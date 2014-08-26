/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.net.web;

import java.io.IOException;
import net.nexustools.DefaultAppDelegate;
import net.nexustools.net.Server.Protocol;
import net.nexustools.net.web.handlers.CGIRequestHandler;
import net.nexustools.net.web.handlers.FileRequestHandler;
import net.nexustools.net.web.handlers.MatchRequestHandler;
import net.nexustools.net.web.http.HTTPServer;
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
        new JaWebSrv(args, 8080/*, Protocol.SSLvTCP*/).mainLoop();
    }

    // while true;do curl -s -w "%{time_total}\n" -o /dev/null 'http://localhost:8080/?key=S0up_!_S0up'; done
    
	final RunQueue runQueue;
    final WebServer webServer;
    public JaWebSrv(String[] args, int port, Protocol protocol, RunQueue runQueue) throws IOException {
        super(args, "JaWebSrv", "NexusTools", runQueue);
		//MatchRequestHandler matchModule = new MatchRequestHandler(new CGIRequestHandler("/var/www/parked", "index.php", "/usr/bin/php5-cgi"));
		
		
		
        webServer = new HTTPServer(/*matchModule*/new FileRequestHandler("/"), port, protocol, runQueue);
		this.runQueue = runQueue;
    }
    public JaWebSrv(String[] args, int port, Protocol protocol) throws IOException {
		this(args, port, protocol, new ThreadedRunQueue("JaWebSrv", ThreadedRunQueue.Delegator.Fair, 4f));
    }
    public JaWebSrv(String[] args, int port) throws IOException {
		this(args, port, Protocol.TCP);
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
