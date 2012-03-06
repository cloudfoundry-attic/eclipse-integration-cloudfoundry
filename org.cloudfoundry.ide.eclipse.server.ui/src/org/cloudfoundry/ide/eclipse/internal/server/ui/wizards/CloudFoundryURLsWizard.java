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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ICoreRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.Wizard;


/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class CloudFoundryURLsWizard extends Wizard {

	private final String appName;

	private final CloudFoundryServer cloudServer;

	private final List<String> existingURIs;

	private CloudFoundryURLsWizardPage page;

	public CloudFoundryURLsWizard(CloudFoundryServer cloudServer, String appName, List<String> existingURIs) {
		this.cloudServer = cloudServer;
		this.appName = appName;
		this.existingURIs = existingURIs;

		setWindowTitle("Modify Mapped URLs");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		page = new CloudFoundryURLsWizardPage(cloudServer, existingURIs);
		addPage(page);
	}

	public List<String> getURLs() {
		return page.getURLs();
	}

	@Override
	public boolean performFinish() {
		page.setErrorMessage(null);
		IStatus result = CloudUiUtil.runForked(new ICoreRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				cloudServer.getBehaviour().updateApplicationUrls(appName, page.getURLs(), monitor);
			}
		}, this);
		return result.isOK();
	}

}
