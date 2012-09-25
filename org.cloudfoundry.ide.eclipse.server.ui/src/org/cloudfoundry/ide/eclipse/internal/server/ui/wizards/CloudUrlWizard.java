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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint.CloudURL;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
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

	private final List<CloudURL> allCloudUrls;

	private CloudUrlWizardPage page;

	private String url;

	private String name;

	public CloudUrlWizard(String serverID, List<CloudURL> allCloudUrls, String url, String name) {
		this.serverID = serverID;
		this.allCloudUrls = allCloudUrls;
		this.url = url;
		this.name = name;
		setWindowTitle("Add and validate a Cloud URL");

		// Does not require a modal progress monitor.
		// Long running application are run using specialised non-blocking
		// execution
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {

		page = new CloudUrlWizardPage(allCloudUrls, CloudFoundryImages.getWizardBanner(serverID), url, name);
		addPage(page);
	}

	public CloudURL getCloudUrl() {
		String dURL = page != null ? page.getUrl() : url;
		String dName = page != null ? page.getName() : name;
		return new CloudURL(dName, dURL, true);
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

		url = page.getUrl();
		final String jobName = "Validating URL";

		final Boolean shouldProceed[] = new Boolean[] { new Boolean(false) };
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
							CloudFoundryPlugin.getDefault().getCloudFoundryClientFactory()
									.getCloudFoundryOperations(url).getCloudInfo();
							shouldProceed[0] = new Boolean(true);

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
			shouldProceed[0] = MessageDialog.openQuestion(getShell(), "Keep URL", errorMessage
					+ ". Would you like to keep the URL anyways?");
		}
		return shouldProceed[0];

	}

	protected String getErrorMessage(Exception exception, String url) {
		StringBuilder builder = new StringBuilder();
		builder.append("Unable to validate Cloud URL: ");
		builder.append(url);

		if (exception != null) {
			String errorMessage = exception.getMessage() != null ? exception.getMessage() : exception
					.toString();
			if (errorMessage != null) {
				builder.append(" due to ");
				builder.append(errorMessage);
			}

		}
		return builder.toString();
	}

}
