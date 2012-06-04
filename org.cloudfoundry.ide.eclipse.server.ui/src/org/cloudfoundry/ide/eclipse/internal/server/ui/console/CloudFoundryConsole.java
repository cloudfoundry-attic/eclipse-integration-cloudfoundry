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

import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IOConsoleOutputStream;
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

	private CloudApplication app;

	private int failureCount;

	private int instanceIndex;

	/** How frequently to check for log changes; defaults to 1 seconds */
	private long sampleInterval = 5000;

	private final CloudFoundryServer server;

	private IOConsoleOutputStream stderr;

	private int stderrOffset = 0;

	private String stderrPath = "logs/stderr.log";

	private IOConsoleOutputStream stdout;

	private int stdoutOffset = 0;

	private String stdoutPath = "logs/stdout.log";

	/** Is the tailer currently tailing? */
	private boolean tailing = true;

	private final MessageConsole console;

	public CloudFoundryConsole(CloudFoundryServer server, CloudApplication app, int instanceIndex,
			MessageConsole console) {
		super(getConsoleName(app));
		this.server = server;
		this.app = app;
		this.instanceIndex = instanceIndex;
		this.console = console;
		this.stdout = console.newOutputStream();
		this.stderr = console.newOutputStream();
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				stderr.setColor(Display.getDefault().getSystemColor(SWT.COLOR_RED));
			}
		});
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
		console.clearConsole();
		this.stderrOffset = 0;
		this.stdoutOffset = 0;
		this.failureCount = 0;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (this.tailing) {
			try {
				String content;
				content = server.getBehaviour().getFile(app.getName(), instanceIndex, stderrPath, monitor);
				if (content != null && content.length() > stderrOffset) {
					stderr.write(content.substring(stderrOffset));
					stderrOffset = content.length();
				}
				content = server.getBehaviour().getFile(app.getName(), instanceIndex, stdoutPath, monitor);
				if (content != null && content.length() > stdoutOffset) {
					stdout.write(content.substring(stdoutOffset));
					stdoutOffset = content.length();
				}
			}
			catch (IOException e) {
				// console was closed
				return Status.CANCEL_STATUS;
			}
			catch (CoreException e) {
				failureCount++;
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
