/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.console.IOConsoleOutputStream;

/**
 * Simple console content that simply notifies the user that an application is
 * about to start. It does not perform any network I/O.
 * 
 */
public class PreApplicationStartConsoleContent implements IConsoleContent {

	public ICloudFoundryConsoleOutputStream getOutputStream(IOConsoleOutputStream consoleOutputStream) {

		ConsoleOutputStream cfStream = new ConsoleOutputStream(consoleOutputStream, null) {
			private boolean once = true;

			@Override
			protected String getContent(IProgressMonitor monitor) throws CoreException {
				if (once) {
					once = false;
					requestStreamClose(true);
					return getStagingInitialContent();
				}
				return null;
			}

			protected int getStreamColour() {
				return SWT.COLOR_DARK_MAGENTA;
			}
		};

		cfStream.initialiseStream();

		return cfStream;
	}

	public static String getStagingInitialContent() {
		StringBuffer initialContent = new StringBuffer();
		initialContent.append("Staging application");
		initialContent.append('\n');
		initialContent.append("Please wait while staging completes...");
		initialContent.append('\n');
		initialContent.append('\n');

		return initialContent.toString();
	}

}
