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

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ICoreRunnable;
import org.cloudfoundry.ide.eclipse.internal.server.ui.RepublishApplicationHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class CloudFoundryURLsWizard extends Wizard {

	private final String appName;

	private final CloudFoundryServer cloudServer;

	private List<String> existingURIs;

	private boolean isPublished = true;

	private CloudFoundryURLsWizardPage page;

	public CloudFoundryURLsWizard(CloudFoundryServer cloudServer, String appName, List<String> existingURIs) {
		this.cloudServer = cloudServer;
		this.appName = appName;
		this.existingURIs = existingURIs;

		setWindowTitle("Modify Mapped URLs");
		setNeedsProgressMonitor(true);
	}

	public CloudFoundryURLsWizard(CloudFoundryServer cloudServer, String appName, List<String> existingURIs,
			boolean isPublished) {
		this(cloudServer, appName, existingURIs);
		this.isPublished = isPublished;
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
		final boolean shouldRepublish = page.shouldRepublish();

		final IStatus[] result = new IStatus[1];
		if (shouldRepublish) {
			final ApplicationModule appModule = getAppModule();
			if (appModule != null) {
				IRunnableWithProgress runnable = new IRunnableWithProgress() {

					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							new RepublishApplicationHandler(appModule, page.getURLs(), cloudServer).republish(monitor);
						}
						catch (CoreException e) {
							CloudFoundryPlugin.logError(e);
							result[0] = CloudFoundryPlugin.getErrorStatus(e);
						}

					}
				};
				try {
					// Must both fork and set cancellable to true in order to
					// enable
					// cancellation of long-running validations
					getContainer().run(true, true, runnable);
				}
				catch (InvocationTargetException e) {
					result[0] = CloudFoundryPlugin.getErrorStatus(e);
				}
				catch (InterruptedException e) {
					result[0] = CloudFoundryPlugin.getErrorStatus(e);
				}
			}
		}
		else {
			result[0] = CloudUiUtil.runForked(new ICoreRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					cloudServer.getBehaviour().updateApplicationUrls(appName, page.getURLs(), monitor);
				}
			}, this);
		}

		return result[0] != null ? result[0].isOK() : true;
	}

	public ApplicationModule getAppModule() {

		try {
			return cloudServer.getApplicationModule(appName);
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return null;
	}

	public boolean isPublished() {
		return isPublished;
	}

}
