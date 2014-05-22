/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others
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
 *     IBM Corporation - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui;

import java.util.regex.Pattern;

import org.cloudfoundry.ide.eclipse.internal.server.core.AbstractAppStateTracker;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.ui.Logger;
import org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleManager;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * A general implementation class of the org.cloudfoundry.ide.eclipse.internal.server.core.AbstractAppStateTracker
 * that provides easy implementation to track console output to decide whether the app is started or not. In
 * most of the cases, the adopter only need to implement the getAppStartedPattern() method.  In more complex cases, 
 * the adopter can override the createPatternMatchListener() to provide their own pattern match listener.
 * 
 * @author Elson Yuen
 */
public abstract class AbstractConsoleMonitorAppStateTracker extends AbstractAppStateTracker {
	private ConsolePatternMatchListener consoleMonitor;
	
	/**
	 * Tracks text appended to the console and notifies listeners in terms of whole
	 * lines.
	 */
	public class ConsolePatternMatchListener implements IPatternMatchListener {

	    private String appName;
		private int appState = IServer.STATE_STARTING;
	    
	    public ConsolePatternMatchListener(String curAppName) {
	    	appName = curAppName;
	    }

		/* (non-Javadoc)
		 * @see org.eclipse.ui.console.IPatternMatchListenerDelegate#connect(org.eclipse.ui.console.TextConsole)
		 */
		public void connect(TextConsole console) {
			// Do nothing
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.ui.console.IPatternMatchListener#disconnect()
		 */
		public synchronized void disconnect() {
			// Do nothing
	    }

	    /* (non-Javadoc)
	     * @see org.eclipse.ui.console.IPatternMatchListener#matchFound(org.eclipse.ui.console.PatternMatchEvent)
	     */
	    public void matchFound(PatternMatchEvent event) {
	    	if (Logger.INFO) {
	    		Logger.println(Logger.INFO_LEVEL, this, "matchFound", "Application start detected: " + appName);
	    	}
	    	appState = IServer.STATE_STARTED;
	    }
	    
	    /* (non-Javadoc)
	     * @see org.eclipse.ui.console.IPatternMatchListener#getPattern()
	     */
	    public String getPattern() {
	        return getAppStartedPattern();
	    }

	    /* (non-Javadoc)
	     * @see org.eclipse.ui.console.IPatternMatchListener#getCompilerFlags()
	     */
	    public int getCompilerFlags() {
	        return Pattern.CASE_INSENSITIVE;
	    }

	    /* (non-Javadoc)
	     * @see org.eclipse.ui.console.IPatternMatchListener#getLineQualifier()
	     */
	    public String getLineQualifier() {
	        return "\\n|\\r"; //$NON-NLS-1$
	    }
	    
	    protected int getApplicationState() {
	    	return appState;
	    }
	}
	
	protected ConsolePatternMatchListener createPatternMatchListener(CloudFoundryApplicationModule appModule) {
		return new ConsolePatternMatchListener(((IModule)appModule).getName());
	}
	
	@Override
	public int getApplicationState(CloudFoundryApplicationModule appModule) {
		if (Logger.DETAILS) {
			 Logger.println(Logger.DETAILS_LEVEL, this, "getApplicationState", "Waiting for app to start: " + ((IModule)appModule).getName() + ", state=" + consoleMonitor.getApplicationState());
		}
		return consoleMonitor.getApplicationState();
	}

	/**
	 * Get the regex pattern that defines that pattern string that matches console messages on each line to 
	 * decide if the application is started.
	 * @return regex pattern for the match pattern.
	 */
    protected abstract String getAppStartedPattern();

	@Override
	public void startTracking(CloudFoundryApplicationModule appModule) {
		if (server == null || appModule == null) {
			return;
		}

		MessageConsole console = ConsoleManager.getInstance().findCloudFoundryConsole(server, appModule);
		if (console != null) {
			if (Logger.INFO) {
				 Logger.println(Logger.INFO_LEVEL, this, "isApplicationStarted", "Start app state tracking: " + ((IModule)appModule).getName());
			}
			consoleMonitor = createPatternMatchListener(appModule);
			console.addPatternMatchListener(consoleMonitor);
		}
	}

	@Override
	public void stopTracking(CloudFoundryApplicationModule appModule) {
		if (server == null || consoleMonitor == null || appModule == null) {
			return;
		}
		if (Logger.INFO) {
			 Logger.println(Logger.INFO_LEVEL, this, "stopTracking", "Stop app state tracking: " + ((IModule)appModule).getName());
		}
		MessageConsole console = ConsoleManager.getInstance().findCloudFoundryConsole(server, appModule);
		if (console != null) {
			console.removePatternMatchListener(consoleMonitor);
		}
	}

}
