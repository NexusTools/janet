/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.nexustools.web;

import java.io.IOException;
import net.nexustools.DefaultAppDelegate;
import net.nexustools.concurrent.Prop;
import net.nexustools.janet.Server.Protocol;
import net.nexustools.runtime.RunQueue;
import net.nexustools.runtime.ThreadedRunQueue;
import net.nexustools.utils.NXUtils;
import net.nexustools.utils.Testable;
import net.nexustools.web.handlers.FileRequestHandler;
import net.nexustools.web.handlers.JoomlaRequestHandler;
import net.nexustools.web.handlers.MatchRequestHandler;
import net.nexustools.web.handlers.PHPFileRequestHandler;
import net.nexustools.web.handlers.PHPRequestHandler;
import net.nexustools.web.handlers.RedirectRequestHandler;
import net.nexustools.web.http.HTTPServer;

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
    
	final int port;
	final Protocol protocol;
	final Prop<Runnable> mainLoop = new Prop();
    public JaWebSrv(String[] args, int port, Protocol protocol, RunQueue runQueue) throws IOException {
        super(args, "JaWebSrv", "NexusTools", runQueue);
		
		this.protocol = protocol;
		this.port = port;
    }
    public JaWebSrv(String[] args, int port, Protocol protocol) throws IOException {
		this(args, port, protocol, new ThreadedRunQueue("JaWebSrv", ThreadedRunQueue.Delegator.Fair, 4f));
    }
    public JaWebSrv(String[] args, int port) throws IOException {
		this(args, port, Protocol.TCP);
    }

    @Override
    protected void launch(String[] args) {
		MatchRequestHandler matchModule = new MatchRequestHandler(new PHPRequestHandler("/var/www/parked", "index.php"));
		
		try {
			final HTTPServer webServer = new HTTPServer(matchModule/*new FileRequestHandler("/")*/, port, protocol, queue());
			mainLoop.set(new Runnable() {
				public void run() {
					while(true)
						try {
							webServer.join();
							break;
						} catch (InterruptedException ex) {}
				}
			});
		} catch (IOException ex) {
			mainLoop.set(new Runnable() {
				public void run() {}
			});
			throw NXUtils.wrapRuntime(ex);
		}
	}

    public boolean needsMainLoop() {
        return false;
    }

	@Override
    public void mainLoop() {
		while(!mainLoop.isTrue())
			try {
				Thread.sleep(5000);
			} catch (InterruptedException ex) {}
		
        mainLoop.get().run();
    }
    
}
