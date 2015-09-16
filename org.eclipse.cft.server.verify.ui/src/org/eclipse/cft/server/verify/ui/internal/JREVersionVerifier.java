/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
 *     IBM - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.verify.ui.internal;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;

public class JREVersionVerifier implements IStartup {	
	private static Shell parent = null;

	@Override
	public void earlyStartup() {
		final String verStr = System.getProperty("java.version"); //$NON-NLS-1$
		if (verStr != null) {
			// Set the last plugin version check.
			final String curVersionStr = Activator.getDefault().getBundle().getVersion().toString();
			final String lastVersionStr = Activator.getDefault().getLastPluginJavaVersionCheck();
			boolean isLastVersionMatch = lastVersionStr.equals(curVersionStr);
			
			if (verStr.compareTo("1.7") < 0) { //$NON-NLS-1$
				// Force the message to display if the plugin has changed since the last time the message
				// has been display. This mechanism is to display the message again after an update on the install.
				if (!isLastVersionMatch || Activator.getDefault().getIsCheckJavaVersion()) {
	                Display.getDefault().syncExec(new Runnable() {
	                    @Override
	                    public void run() {
	        				MessageDialog.openError(getParent(), Messages.CloudFoundryEclipseToolsErrorTitle, NLS.bind(Messages.UnsupportedJavaVersion, verStr));
	        				
	        				// Only show once unless java version or plugin version has been changed.
	        				Activator.getDefault().setIsCheckJavaVersion(false);
	        				Activator.getDefault().setLastPluginJavaVersionCheck(curVersionStr);
	                    }
	                });				
				}
			} else {
				// The tool has switched to use a supported java. Reset the check java version flag to 
				// display the error when the user switches between supported and unsupported version of
				// Java, e.g. switch from Java 6 to Java 7 and then to Java 6 again.
				if (!Activator.getDefault().getIsCheckJavaVersion()) {
					Activator.getDefault().setIsCheckJavaVersion(true);
				}
			}
		}
	}
	
	private static Shell getActiveWorkbenchShell() {
		Shell shell = null;
		try {
			shell = Activator.getDefault().getWorkbench().getActiveWorkbenchWindow()
					.getShell();
		} catch (Exception e) {
			// Do nothing.
		}
		return shell == null ? getCurrentDisplay().getActiveShell() : shell;
	}
	
	private static Display getCurrentDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}
	
	private static Shell getFocusShell() {
		Shell focusShell = null;
		try {
			focusShell = getActiveWorkbenchShell();
		} catch (Exception e) {
			// Error on getting shell, do nothing.
		}

		return focusShell;
	}

	private static Shell getParent() {
		if (parent == null) {
			// Set the current window that gets the focus as the parent.
			parent = getFocusShell();
		}
		return parent;
	}
}
