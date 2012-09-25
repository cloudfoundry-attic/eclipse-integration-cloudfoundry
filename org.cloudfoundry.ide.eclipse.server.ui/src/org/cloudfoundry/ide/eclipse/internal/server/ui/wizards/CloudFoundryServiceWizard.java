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

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @author Steffen Pingel
 * @author Christian Dupuis
 * @author Terry Denney
 */
public class CloudFoundryServiceWizard extends Wizard {

	private final CloudFoundryServer cloudServer;

	private AbstractCloudFoundryServiceWizardPage page;

	/**
	 * Set true if service should not be added during wizard completion.
	 */
	private final boolean deferServiceAddition;

	/**
	 * Use this constructor if service should be added automatically upon wizard
	 * completion.
	 * @param cloudServer
	 */
	public CloudFoundryServiceWizard(CloudFoundryServer cloudServer) {
		this(cloudServer, false);
	}

	/**
	 * User this constructor if caller decides whether service should be added
	 * automatically upon wizard completion
	 * @param cloudServer
	 * @param deferServiceAddition
	 */
	public CloudFoundryServiceWizard(CloudFoundryServer cloudServer, boolean deferServiceAddition) {
		this.cloudServer = cloudServer;
		setWindowTitle("Add Service");
		setNeedsProgressMonitor(true);
		this.deferServiceAddition = deferServiceAddition;
	}

	@Override
	public void addPages() {
		page = cloudServer.supportsCloudSpaces() ? new CloudFoundryServicePlanWizardPage(cloudServer)
				: new CloudFoundryServiceWizardPage(cloudServer);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		if (!deferServiceAddition) {
			try {

				getContainer().run(true, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							cloudServer.getBehaviour().createService(new CloudService[] { page.getService() }, monitor);
						}
						catch (CoreException e) {
							throw new InvocationTargetException(e);
						}
						catch (OperationCanceledException e) {
							throw new InterruptedException();
						}
						finally {
							monitor.done();
						}
					}
				});
				return true;
			}
			catch (InvocationTargetException e) {
				if (e.getCause() != null) {
					Status status = new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind(
							"Adding of service failed for {0}: {1}", cloudServer.getServer().getName(), e.getCause()
									.getMessage() != null ? e.getCause().getMessage() : e.getCause().toString()), e);
					StatusManager.getManager().handle(status,
							StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
				}
			}
			catch (InterruptedException e) {
				// ignore
			}
			return false;
		}

		return true;
	}

	/**
	 * Returns the service added by this wizard, or possibly null if wizard
	 * hasn't completed yet or was cancelled.
	 * @return added service or null if nothing added at the time of the call
	 */
	public CloudService getService() {
		return page != null ? page.getService() : null;
	}

}
