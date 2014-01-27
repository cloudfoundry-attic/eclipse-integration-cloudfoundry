/*******************************************************************************
 * Copyright (c) 2012, 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationRegistry;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.IApplicationDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.ui.RepublishApplicationHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.wst.server.core.IModule;
import org.springframework.util.Assert;

/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class MappedURLsWizard extends Wizard {

	private final String appName;

	private final CloudFoundryServer cloudServer;

	private List<String> existingURIs;

	private boolean isPublished = true;

	private CloudFoundryApplicationModule applicationModule;

	private MappedURLsWizardPage page;

	public MappedURLsWizard(CloudFoundryServer cloudServer, CloudFoundryApplicationModule applicationModule,
			List<String> existingURIs) {
		Assert.notNull(applicationModule);
		this.cloudServer = cloudServer;
		this.appName = applicationModule.getDeployedApplicationName();
		this.applicationModule = applicationModule;
		this.existingURIs = existingURIs;

		setWindowTitle("Modify Mapped URLs");
		setNeedsProgressMonitor(true);
	}

	public MappedURLsWizard(CloudFoundryServer cloudServer, CloudFoundryApplicationModule applicationModule,
			List<String> existingURIs, boolean isPublished) {
		this(cloudServer, applicationModule, existingURIs);
		this.isPublished = isPublished;
	}

	@Override
	public void addPages() {
		page = new MappedURLsWizardPage(cloudServer, existingURIs, applicationModule);
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

		IApplicationDelegate delegate = ApplicationRegistry.getApplicationDelegate(localModule);
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

			Job job = new Job("Republishing " + applicationModule.getDeployedApplicationName()) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					IStatus status = null;
					try {
						new RepublishApplicationHandler(applicationModule, page.getURLs(), cloudServer)
								.republish(monitor);
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
		else {
			try {
				page.setMessage("Updating URLs. Please wait while the process completes.");
				getContainer().run(true, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						try {
							cloudServer.getBehaviour().updateApplicationUrls(appName, page.getURLs(), monitor);
						}
						catch (CoreException e) {
							result[0] = e.getStatus();
						}
					}
				});
				page.setMessage(null);
			}
			catch (InvocationTargetException e) {
				result[0] = CloudFoundryPlugin.getErrorStatus(e);
			}
			catch (InterruptedException e) {
				result[0] = CloudFoundryPlugin.getErrorStatus(e);

			}

		}
		if (result[0] != null && !result[0].isOK()) {
			page.setErrorMessage("URL may not have changed correctly due to: " + result[0].getMessage());
			return false;
		}
		else {
			return true;
		}

	}

	public boolean isPublished() {
		return isPublished;
	}

}
