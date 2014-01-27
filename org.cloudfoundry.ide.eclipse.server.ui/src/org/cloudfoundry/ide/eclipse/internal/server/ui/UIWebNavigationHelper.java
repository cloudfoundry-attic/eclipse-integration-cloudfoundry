/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.internal.browser.WebBrowserPreference;
import org.eclipse.ui.progress.UIJob;

@SuppressWarnings("restriction")
public class UIWebNavigationHelper {

	private String label;

	private String location;

	public UIWebNavigationHelper(String location, String label) {
		super();
		this.location = location;
		this.label = label;

	}

	public String getLocation() {
		return location;
	}

	public void navigate() {
		UIJob job = new UIJob(label) {

			public IStatus runInUIThread(IProgressMonitor monitor) {
				CloudUiUtil.openUrl(location);
				return Status.OK_STATUS;
			}
		};

		job.schedule();
	}

	public void navigateExternal() {
		UIJob job = new UIJob(label) {

			public IStatus runInUIThread(IProgressMonitor monitor) {
				CloudUiUtil.openUrl(location, WebBrowserPreference.EXTERNAL);
				return Status.OK_STATUS;
			}
		};

		job.schedule();
	}
}
