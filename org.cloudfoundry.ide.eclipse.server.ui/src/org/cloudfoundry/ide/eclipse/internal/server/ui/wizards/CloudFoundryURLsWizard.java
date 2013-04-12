/*******************************************************************************
 * Copyright (c) 2012, 2013 VMware, Inc.
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

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationRegistry;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.IApplicationDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ICoreRunnable;
import org.cloudfoundry.ide.eclipse.internal.server.ui.RepublishApplicationHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.wst.server.core.IModule;

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

	private ApplicationModule applicationModule;

	private CloudFoundryURLsWizardPage page;

	public CloudFoundryURLsWizard(CloudFoundryServer cloudServer, ApplicationModule applicationModule,
			List<String> existingURIs) {
		this.cloudServer = cloudServer;
		this.appName = applicationModule.getApplicationId();
		this.applicationModule = applicationModule;
		this.existingURIs = existingURIs;

		setWindowTitle("Modify Mapped URLs");
		setNeedsProgressMonitor(false);
	}

	public CloudFoundryURLsWizard(CloudFoundryServer cloudServer, ApplicationModule applicationModule,
			List<String> existingURIs, boolean isPublished) {
		this(cloudServer, applicationModule, existingURIs);
		this.isPublished = isPublished;
	}

	@Override
	public void addPages() {
		page = new CloudFoundryURLsWizardPage(cloudServer, existingURIs, getAppModule());
		addPage(page);
	}

	public List<String> getURLs() {
		return page.getURLs();
	}

	public boolean requiresURL() {
		IModule localModule = applicationModule.getLocalModule();

		if (localModule == null) {
			return true;
		}

		IApplicationDelegate delegate = ApplicationRegistry.getApplicationDelegate(localModule, null);
		if (delegate == null) {
			return true;
		}

		return delegate.requiresURL();
	}

	@Override
	public boolean performFinish() {
		page.setErrorMessage(null);
		final boolean shouldRepublish = page.shouldRepublish();

		final IStatus[] result = new IStatus[1];
		if (shouldRepublish) {
			final ApplicationModule appModule = getAppModule();
			// In the republish case, finish the URL wizard whether republish
			// succeeds or not, as error conditions may result
			// in the publish wizard opening.
			if (appModule == null) {
				String url = page.getURLs() != null && page.getURLs().size() > 0 ? page.getURLs().get(0) : null;
				result[0] = CloudFoundryPlugin.getErrorStatus("Unable to find application module"
						+ (url != null ? " for " + url : "") + ". Please republish application manually.");
			}
			else {
				// Launch a job to execute the republish after the wizard
				// completes
				Job job = new Job("Republishing " + appModule.getApplicationId()) {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						IStatus status = null;
						try {
							new RepublishApplicationHandler(appModule, page.getURLs(), cloudServer).republish(monitor);
						}
						catch (CoreException e) {
							status = CloudFoundryPlugin.getErrorStatus(e);
							StatusManager.getManager().handle(status, StatusManager.LOG);
						}
						return status != null ? status : Status.OK_STATUS;
					}

				};
				job.setSystem(false);
				job.setPriority(Job.INTERACTIVE);
				job.schedule();

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
			IStatus status = CloudFoundryPlugin.getErrorStatus(e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}
		return null;
	}

	public boolean isPublished() {
		return isPublished;
	}

}
