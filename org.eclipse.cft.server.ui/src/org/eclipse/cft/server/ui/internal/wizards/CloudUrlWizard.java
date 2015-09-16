/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
 *     IBM - Switching to use the more generic AbstractCloudFoundryUrl
 *     		instead concrete CloudServerURL
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cft.server.core.AbstractCloudFoundryUrl;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryBrandingExtensionPoint.CloudServerURL;
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.UserDefinedCloudFoundryUrl;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

/**
 * Creates or edits a Cloud URL. Validation of the URL is also performed as a
 * long running operation. Users have the option of canceling URL validations,
 * as well as keeping URLs that failed to be validated (either due to an
 * validation error or validation cancellation).
 * 
 * @author Nieraj Singh
 */
public class CloudUrlWizard extends Wizard {

	private final String serverID;

	private final List<AbstractCloudFoundryUrl> allCloudUrls;

	private CloudUrlWizardPage page;

	private String url;

	private String name;
	
	private boolean selfSigned;

	/**
	 * @deprecated use {@link #CloudUrlWizard(String, String, String, boolean, List)} instead.
	 */
	public CloudUrlWizard(String serverID, List<CloudServerURL> allCloudUrls, String url, String name, boolean selfSigned) {
		this.serverID = serverID;
		if (allCloudUrls == null) {
			this.allCloudUrls = null;
		} else {
			this.allCloudUrls = new ArrayList <AbstractCloudFoundryUrl> ();
			for (CloudServerURL serverUrl : allCloudUrls) {
				 this.allCloudUrls.add(serverUrl);
			}
		}
		this.url = url;
		this.name = name;
		this.selfSigned = selfSigned;
		setWindowTitle(Messages.CloudUrlWizard_TITLE_ADD_VALIDATE);

		// Does not require a modal progress monitor.
		// Long running application are run using specialised non-blocking
		// execution
		setNeedsProgressMonitor(true);
	}
	
	public CloudUrlWizard(String serverID, String url, String name, boolean selfSigned, List<AbstractCloudFoundryUrl> allCloudUrls) {
		this.serverID = serverID;
		this.allCloudUrls = allCloudUrls;
		this.url = url;
		this.name = name;
		this.selfSigned = selfSigned;
		setWindowTitle(Messages.CloudUrlWizard_TITLE_ADD_VALIDATE);

		// Does not require a modal progress monitor.
		// Long running application are run using specialised non-blocking
		// execution
		setNeedsProgressMonitor(true);
	}


	@Override
	public void addPages() {
		page = new CloudUrlWizardPage(CloudFoundryImages.getWizardBanner(serverID), url, name, selfSigned, allCloudUrls);
		addPage(page);
	}

	/**
	 * @deprecated use {@link #getCloudFoundryUrl()} instead.
	 */
	public CloudServerURL getCloudUrl() {
		String dURL = page != null ? page.getUrl() : url;
		String dName = page != null ? page.getName() : name;
		boolean selfSigned = page != null ? page.getSelfSigned() : this.selfSigned;
		return new CloudServerURL(dName, dURL, true, selfSigned);
	}
	
	public AbstractCloudFoundryUrl getCloudFoundryUrl () {
		String dURL = page != null ? page.getUrl() : url;
		String dName = page != null ? page.getName() : name;
		boolean selfSigned = page != null ? page.getSelfSigned() : this.selfSigned;
		return new UserDefinedCloudFoundryUrl(dName, dURL, selfSigned);
	}

	public boolean performFinish() {
		return validateURL();
	}

	/**
	 * Synchronises between the long-running validation job and the progress
	 * monitor updates
	 * 
	 */
	static class Synchroniser {

		private boolean isComplete = false;

		public synchronized void completed() {
			isComplete = true;
		}

		public synchronized boolean isComplete() {
			return isComplete;
		}
	}

	protected boolean launchURLValidation(final Exception[] exception, final IProgressMonitor monitor) {

		this.url = page.getUrl();
		this.selfSigned = page.getSelfSigned();
		final String jobName = Messages.CloudUrlWizard_JOB_VALIDATE_URL;

		final boolean shouldProceed[] =  { false };
		try {

			int useAnimatedProgress = 0;
			monitor.beginTask(jobName, useAnimatedProgress);

			try {

				// As this involves a server request, fork it
				// synchronously, until notified that it has been
				// executed or cancel operation is
				// received
				exception[0] = null;

				final Synchroniser synchroniser = new Synchroniser();

				// fork thread as a Job
				Job job = new Job(jobName) {

					protected IStatus run(IProgressMonitor arg0) {

						try {
							CloudFoundryPlugin.getCloudFoundryClientFactory()
									.getCloudFoundryOperations(url, CloudUrlWizard.this.selfSigned).getCloudInfo();
							shouldProceed[0] = true;

						}
						catch (Exception e) {
							exception[0] = e;
						}
						finally {
							synchroniser.completed();
						}

						return Status.OK_STATUS;
					}

				};
				job.setPriority(Job.INTERACTIVE);
				job.schedule();

				// FIXNS: Could also add timeout, although consideration needs
				// to be taken for long-running validations that do validate a
				// URL
				// rather than generate an error, in particular if the timeout
				// occurs
				// before the positive validation
				while (!monitor.isCanceled() && !synchroniser.isComplete()) {
					try {
						Thread.sleep(100);
					}
					catch (InterruptedException e) {
						// Nothing, continue;
					}
				}
			}
			catch (OperationCanceledException e) {
				throw new InterruptedException(e.getLocalizedMessage());
			}
		}
		catch (InterruptedException e) {
			shouldProceed[0] = false;
			exception[0] = e;
		}
		return shouldProceed[0];
	}

	protected boolean validateURL() {

		final Exception[] exception = new Exception[1];
		final Boolean[] shouldProceed = new Boolean[1];
		IRunnableWithProgress runnable = new IRunnableWithProgress() {

			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				shouldProceed[0] = launchURLValidation(exception, monitor);
			}
		};

		try {
			// Must both fork and set cancellable to true in order to enable
			// cancellation of long-running validations
			getContainer().run(true, true, runnable);
		}
		catch (InvocationTargetException e) {
			exception[0] = e;
		}
		catch (InterruptedException e) {
			exception[0] = e;
		}
		if (!shouldProceed[0]) {
			String errorMessage = getErrorMessage(exception[0], url);
			shouldProceed[0] = MessageDialog.openQuestion(getShell(), Messages.CloudUrlWizard_ERROR_KEEP_TITLE, errorMessage
					+ Messages.CloudUrlWizard_ERROR_KEEP_BODY);
		}
		return shouldProceed[0];

	}

	protected String getErrorMessage(Exception exception, String url) {
		StringBuilder builder = new StringBuilder();
		builder.append(Messages.CloudUrlWizard_ERROR_VALIDATE);
		builder.append(url);

		if (exception != null) {
			String errorMessage = exception.getMessage() != null ? exception.getMessage() : exception
					.toString();
			if (errorMessage != null) {
				builder.append(Messages.CloudUrlWizard_ERROR_VALIDATE_DUE_TO);
				builder.append(errorMessage);
			}

		}
		return builder.toString();
	}

}
