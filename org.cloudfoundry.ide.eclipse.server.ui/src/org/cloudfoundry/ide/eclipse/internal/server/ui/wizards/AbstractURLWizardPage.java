/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudApplicationUrlLookup;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ICoreRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.UIJob;

/**
 * 
 * This page handles the lookup of domains for application URLs. It fetches the
 * list of domains once per session, when the controls are made visible.
 */
public abstract class AbstractURLWizardPage extends PartsWizardPage {

	protected AbstractURLWizardPage(String pageName, String title, ImageDescriptor titleImage,
			CloudApplicationUrlLookup urlLookup) {
		super(pageName, title, titleImage);
		this.urlLookup = urlLookup;
	}

	private boolean refreshedDomains = false;

	// Assign only after the UI controls are visible for URL selection
	protected CloudApplicationUrlLookup urlLookup;

	@Override
	protected void performWhenPageVisible() {
		refreshApplicationURL();
	}

	protected void refreshApplicationURL() {
		// Refresh the application URL (since the URL host tends to the the
		// application name, if the application name has changed
		// make sure it gets updated in the UI. Also fetch the list of domains
		// ONCE per session.
		// Run all URL refresh and fetch in the same UI Job to ensure that
		// domain updates occur first before the UI is refreshed.
		if (!refreshedDomains) {
			update(false, CloudFoundryPlugin.getStatus("Fetching list of domains. Please wait while it completes.",
					IStatus.INFO));
			final String jobLabel = "Fetching list of domains.";
			UIJob job = new UIJob(jobLabel) {

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					try {

						ICoreRunnable coreRunner = new ICoreRunnable() {
							public void run(IProgressMonitor coreRunnerMonitor) throws CoreException {
								SubMonitor subProgress = SubMonitor.convert(coreRunnerMonitor, jobLabel, 100);
								try {
									urlLookup.refreshDomains(subProgress);
								}
								finally {
									subProgress.done();
								}

								// Must launch this again in the UI thread AFTER
								// the refresh occurs.
								Display.getDefault().asyncExec(new Runnable() {

									public void run() {
										// Clear any info in the dialogue
										setMessage(null);
										update(false, Status.OK_STATUS);
										refreshURLUI();
									}

								});
								refreshedDomains = true;

							}
						};
						CloudUiUtil.runForked(coreRunner, getWizard().getContainer());

					}
					catch (OperationCanceledException e) {
						update(true, CloudFoundryPlugin.getErrorStatus(e));
					}
					catch (CoreException ce) {
						update(true, ce.getStatus());
					}

					return Status.OK_STATUS;
				}

			};
			job.setSystem(true);
			job.schedule();
		}
	}

	/**
	 * Refresh any URL UI after the list of domains has been fetched from the
	 * server
	 */
	abstract protected void refreshURLUI();

}
