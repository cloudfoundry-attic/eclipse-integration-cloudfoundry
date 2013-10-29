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
import org.cloudfoundry.ide.eclipse.internal.server.ui.ICoreRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;

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

	protected AbstractURLWizardPage(String pageName, String title, ImageDescriptor titleImage) {
		this(pageName, title, titleImage, null);
	}

	protected boolean refreshedDomains = false;

	private CloudApplicationUrlLookup urlLookup;

	protected CloudApplicationUrlLookup getApplicationUrlLookup() {
		return urlLookup;
	}

	@Override
	protected void performWhenPageVisible() {

		// Refresh the application URL (since the URL host tends to the the
		// application name, if the application name has changed
		// make sure it gets updated in the UI. Also fetch the list of domains
		// ONCE per session.
		// Run all URL refresh and fetch in the same UI Job to ensure that
		// domain updates occur first before the UI is refreshed.

		if (!refreshedDomains) {
			refreshApplicationUrlDomains();
		}
	}

	protected void refreshApplicationUrlDomains() {

		final CloudApplicationUrlLookup urlLookup = getApplicationUrlLookup();
		if (urlLookup == null) {
			update(false,
					CloudFoundryPlugin
							.getStatus(
									"No Cloud application URL handler found. Possible error with the application delegate. Application may not deploy correctly.",
									IStatus.WARNING));
			return;

		}

		final String operationLabel = "Fetching list of domains";
		ICoreRunnable runnable = new ICoreRunnable() {
			public void run(IProgressMonitor coreRunnerMonitor) throws CoreException {
				SubMonitor subProgress = SubMonitor.convert(coreRunnerMonitor, operationLabel, 100);
				try {
					urlLookup.refreshDomains(subProgress);
					refreshedDomains = true;
					// Must launch this again in the UI thread AFTER
					// the refresh occurs.
					Display.getDefault().asyncExec(new Runnable() {

						public void run() {
							// Clear any info in the dialogue
							setMessage(null);
							update(false, Status.OK_STATUS);
							postDomainsRefreshedOperation();
						}

					});

				}
				finally {
					subProgress.done();
				}

			}
		};
		runAsynchWithWizardProgress(runnable, operationLabel);

	}

	/**
	 * UI callback after the domains have been successfully refreshed.
	 */
	abstract protected void postDomainsRefreshedOperation();

}
