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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.DeploymentInfoWorkingCopy;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.LocalCloudService;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Action for modifying list of services for an application. Subclasses are
 * responsible for defining the list of services to add or to remove.
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public abstract class ModifyServicesForApplicationAction extends CloudFoundryEditorAction {

	private CloudFoundryApplicationModule appModule;

	private final CloudFoundryServerBehaviour serverBehaviour;

	public ModifyServicesForApplicationAction(CloudFoundryApplicationModule appModule,
			CloudFoundryServerBehaviour serverBehaviour, CloudFoundryApplicationsEditorPage editorPage) {
		super(editorPage, RefreshArea.DETAIL);

		this.appModule = appModule;
		this.serverBehaviour = serverBehaviour;
	}

	abstract public List<String> getServicesToAdd();

	abstract public List<String> getServicesToRemove();

	protected void setApplicationModule(CloudFoundryApplicationModule appModule) {
		this.appModule = appModule;
	}

	@Override
	protected ICloudFoundryOperation getOperation() throws CoreException {
		List<String> existingServices = null;

		final List<String> updatedServices = new ArrayList<String>();

		DeploymentInfoWorkingCopy workingCopy = appModule.getDeploymentInfoWorkingCopy();

		// Check the deployment information to see if it has an existing list of
		// bound services.
		existingServices = workingCopy.asServiceBindingList();

		// Must iterate rather than passing to constructor or using
		// addAll, as some
		// of the entries in existing services may be null.
		if (existingServices != null) {
			for (String existingService : existingServices) {
				if (existingService != null) {
					updatedServices.add(existingService);
				}
			}
		}

		// This leads to duplicate services, as a user could drop an existing
		// service already
		// added to an application
		boolean serviceChanges = false;
		List<String> servicesToAdd = getServicesToAdd();
		for (String serviceToAdd : servicesToAdd) {
			if (!updatedServices.contains(serviceToAdd)) {
				updatedServices.add(serviceToAdd);
				serviceChanges = true;
			}
		}

		serviceChanges |= updatedServices.removeAll(getServicesToRemove());

		if (serviceChanges) {
			// Save the changes even if an app is not deployed
			List<CloudService> boundServices = new ArrayList<CloudService>();
			for (String serName : updatedServices) {
				boundServices.add(new LocalCloudService(serName));
			}
			workingCopy.setServices(boundServices);
			workingCopy.save();

			if (appModule.getApplication() != null) {
				// update services right away, if app is already deployed
				return new EditorOperation() {
					protected void performEditorOperation(IProgressMonitor monitor) throws CoreException {
						ModifyServicesForApplicationAction.this.updateServicesInClient(monitor, appModule,
								serverBehaviour, updatedServices);
					}
				};
			}
		}
		return null;
	}

	/**
	 * Performs the actual services update through the CF server behaviour
	 * @param monitor
	 * @param appModule
	 * @param serverBehaviour
	 * @param updatedServices
	 * @throws CoreException
	 */
	protected abstract void updateServicesInClient(IProgressMonitor monitor, CloudFoundryApplicationModule appModule,
			CloudFoundryServerBehaviour serverBehaviour, List<String> updatedServices) throws CoreException;

	@Override
	protected boolean shouldLogException(CoreException e) {
		return !CloudErrorUtil.isNotFoundException(e);
	}

	public static List<String> getServiceNames(IStructuredSelection selection) {
		Object[] objects = selection.toArray();
		List<String> services = new ArrayList<String>();

		for (Object object : objects) {
			if (object instanceof CloudService) {
				services.add(((CloudService) object).getName());
			}
		}
		return services;
	}

	public static List<CloudService> getServices(IStructuredSelection selection) {
		Object[] objects = selection.toArray();
		List<CloudService> services = new ArrayList<CloudService>();

		for (Object object : objects) {
			if (object instanceof CloudService) {
				services.add(((CloudService) object));
			}
		}
		return services;

	}

}
