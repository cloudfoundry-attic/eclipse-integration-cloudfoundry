/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc.
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

/**
 * Console content for local standard output and error. It is meant write
 * content to the console via the {@link #write(String, IProgressMonitor)} API,
 * instead of fetching content remotely to be displayed to the console. A use
 * case for this is an CF Eclipse component that wishes to display content to
 * the console.
 * 
 */
public abstract class LocalConsoleStream extends CloudFoundryConsoleStream {

	public LocalConsoleStream(int swtColour) {
		super(null, swtColour, null, -1);
	}

	protected String getContent(IProgressMonitor monitor) throws CoreException {
		return null;
	}

}
