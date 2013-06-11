/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import java.io.IOException;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.MessageConsole;

/**
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
class CloudFoundryConsole extends Job {

	static final String ATTRIBUTE_SERVER = "org.cloudfoundry.ide.eclipse.server.Server";

	static final String ATTRIBUTE_APP = "org.cloudfoundry.ide.eclipse.server.CloudApp";

	static final String ATTRIBUTE_INSTANCE = "org.cloudfoundry.ide.eclipse.server.CloudInstance";

	static final String CONSOLE_TYPE = "org.cloudfoundry.ide.eclipse.server.appcloud";

	private int failureCount;

	private final ConsoleStreamContent content;

	/** How frequently to check for log changes; defaults to 1 seconds */
	private long sampleInterval = 5000;

	/** Is the tailer currently tailing? */
	private boolean tailing = true;

	private final MessageConsole console;

	public CloudFoundryConsole(ConsoleStreamContent content, CloudApplication app, int instanceIndex,
			MessageConsole console) {
		super(getConsoleName(app));

		this.console = console;

		this.content = content;

		setSystem(true);
	}

	public void startTailing() {
		tailing = true;
		schedule();
	}

	public void stopTailing() {
		tailing = false;
	}

	public void resetConsole() {
		content.reset();
		this.failureCount = 0;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (this.tailing) {
			try {
				content.getFileContent(monitor);
			}
			catch (CoreException e) {
				Throwable t = e.getCause();
				if (t instanceof IOException) {
					return Status.CANCEL_STATUS;
				}
				else {
					failureCount++;
				}
			}
			if (failureCount < 5) {
				schedule(sampleInterval);
			}
			else {
				stopTailing();
			}
		}
		return Status.OK_STATUS;
	}

	static String getConsoleName(CloudApplication app) {
		String name = (app.getUris() != null && app.getUris().size() > 0) ? app.getUris().get(0) : app.getName();
		return name;
	}

	public MessageConsole getConsole() {
		return console;
	}

}
