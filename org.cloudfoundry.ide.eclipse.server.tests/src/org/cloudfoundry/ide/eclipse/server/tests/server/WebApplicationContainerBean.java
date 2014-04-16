/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.tests.server;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

//FIXNS: Commented out because of STS-3159
//import winstone.Launcher;

/**
 * @author Terry Denney
 * @author Steffen Pingel
 */
public class WebApplicationContainerBean {

	// FIXNS: Commented out because of STS-3159
	// private Launcher winstone;

	private int port;

	private final File webRoot;

	public WebApplicationContainerBean(File webRoot) {
		if (webRoot == null) {
			throw new IllegalArgumentException();
		}
		this.webRoot = webRoot;
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		// FIXNS: Commented out because of STS-3159
		// File webRoot = new File("webapp");
		// WebApplicationContainerBean container = new
		// WebApplicationContainerBean(webRoot);
		// container.start();
		//
		// while (System.in.available() == 0) {
		// Thread.sleep(1000);
		// }
		//
		// System.in.read();
		// // Thread.sleep(10000000);
		// container.stop();
	}

	public int getPort() {
		return port;
	}

	// FIXNS: Commented out because of STS-3159
	// public void start() {
	// if (winstone == null) {
	// int i = 0;
	// do {
	// port = 8100 + ++i;
	// } while (!isLocalPortFree(port));
	//
	// Logger log =
	// Logger.getLogger(WebApplicationContainerBean.class.getName());
	// log.fine("Starting web container on http://localhost:" + port + "/");
	//
	// Map<String, Object> args = new HashMap<String, Object>();
	// try {
	// args.put("webroot", webRoot.getAbsolutePath());
	// args.put("httpPort", String.valueOf(port));
	// args.put("ajp13Port", Integer.toString(port + 1));
	//
	// Launcher.initLogger(args);
	// winstone = new Launcher(args);
	//
	// }
	// catch (IOException e) {
	// throw new IllegalStateException(e);
	// }
	// }
	// }
	//
	// public void stop() {
	// if (winstone != null) {
	// winstone.shutdown();
	// winstone = null;
	// }
	// }

	private boolean isLocalPortFree(int port) {
		try {
			Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
			socket.close();
			return false;
		}
		catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		catch (IOException e) {
			return true;
		}
	}

}
