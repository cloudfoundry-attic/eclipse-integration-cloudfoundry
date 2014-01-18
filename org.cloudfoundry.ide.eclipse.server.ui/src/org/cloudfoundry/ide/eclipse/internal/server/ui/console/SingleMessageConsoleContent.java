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
import org.eclipse.swt.SWT;

/**
 * Simple console content that displays a message to a console associated with a
 * particular deployed application instance.
 * 
 */
public class SingleMessageConsoleContent extends AbstractConsoleContent {

	private final String message;

	public SingleMessageConsoleContent(String message) {
		super(null, SWT.COLOR_DARK_MAGENTA, null, -1);
		this.message = message;
	}

	protected String getContent(IProgressMonitor monitor) throws CoreException {
		return message;
	}

	@Override
	public String write(IProgressMonitor monitor) throws CoreException {
		// Write content once, then close.
		final String content = super.write(monitor);

		close();
		return content;
	}

}
