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

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Defines a stream that writes content to an Eclipse console output stream
 */
public interface ICloudFoundryConsoleOutputStream {

	public String write(IProgressMonitor monitor) throws CoreException;

	public void close() throws IOException;

	/**
	 * Requests the console manager to close the stream at the next available
	 * chance.
	 * 
	 * @return True if console manager should attempt to close the stream.
	 * False, to keep stream open. Other criteria determined by the manager may
	 * result in the stream closing (e.g. continuous errors during streaming),
	 * even if false is returned.
	 */
	public boolean shouldCloseStream();
}
