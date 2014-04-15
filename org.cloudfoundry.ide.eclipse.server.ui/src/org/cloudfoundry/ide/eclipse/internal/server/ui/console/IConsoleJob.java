/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

interface IConsoleJob {

	/**
	 * 
	 * @return non-null {@link IContentType}
	 */
	public IContentType getContentType();

	/**
	 * Writes a message to the console.
	 * @param message
	 * @param monitor
	 * @throws CoreException
	 */
	public void write(String message, IProgressMonitor monitor) throws CoreException;

}