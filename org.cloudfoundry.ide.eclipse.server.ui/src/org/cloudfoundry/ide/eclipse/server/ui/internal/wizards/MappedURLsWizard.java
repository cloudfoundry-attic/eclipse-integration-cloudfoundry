/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.AbstractApplicationDelegate;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.ApplicationRegistry;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.ui.internal.RepublishApplicationHandler;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.wst.server.core.IModule;

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
		Assert.isNotNull(applicationModule);
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

		AbstractApplicationDelegate delegate = ApplicationRegistry.getApplicationDelegate(localModule);

		return delegate == null || delegate.requiresURL();
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
