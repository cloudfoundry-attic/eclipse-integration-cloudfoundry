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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.CloudService;
import org.cloudfoundry.client.lib.DeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;


/**
 * Action for modifying list of services for an application. Subclasses are
 * responsible for defining the list of services to add or to remove.
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public abstract class ModifyServicesForApplicationAction extends CloudFoundryEditorAction {

	private final ApplicationModule appModule;

	private final CloudFoundryServerBehaviour serverBehaviour;

	public ModifyServicesForApplicationAction(ApplicationModule appModule, CloudFoundryServerBehaviour serverBehaviour,
			CloudFoundryApplicationsEditorPage editorPage) {
		super(editorPage, RefreshArea.DETAIL);

		this.appModule = appModule;
		this.serverBehaviour = serverBehaviour;
	}

	abstract public List<String> getServicesToAdd();

	abstract public List<String> getServicesToRemove();

	@Override
	public IStatus performAction(IProgressMonitor monitor) throws CoreException {
		CloudApplication cloudApplication = appModule.getApplication();
		List<String> existingServices = new ArrayList<String>();
		List<String> updatedServices = new ArrayList<String>();

		DeploymentInfo deploymentInfo = appModule.getLastDeploymentInfo();
		if (deploymentInfo == null) {
			deploymentInfo = new DeploymentInfo();
			appModule.setLastDeploymentInfo(deploymentInfo);
			if (cloudApplication != null) {
				existingServices = cloudApplication.getServices();
			}
		}
		else {
			existingServices = deploymentInfo.getServices();
		}

		if (existingServices != null) {
			updatedServices.addAll(existingServices);
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
			// update services right away, if app is already deployed
			if (appModule.getApplication() != null) {
				serverBehaviour.updateServices(appModule.getApplicationId(), updatedServices, monitor);
			}

			// DeploymentInfo deploymentInfo =
			// appModule.getLastDeploymentInfo();
			// if (deploymentInfo == null) {
			// deploymentInfo = new DeploymentInfo();
			// appModule.setLastDeploymentInfo(deploymentInfo);
			// }
			deploymentInfo.setServices(updatedServices);
		}

		return Status.OK_STATUS;
	}

	@Override
	protected void display404Error(IStatus status) {
		// FIXME: do we ever want to show 404?
	}

	@Override
	protected boolean shouldLogException(CoreException e) {
		return !CloudUtil.isNotFoundException(e);
	}

	protected List<String> getServiceNames(IStructuredSelection selection) {
		Object[] objects = selection.toArray();
		List<String> services = new ArrayList<String>();

		for (Object object : objects) {
			if (object instanceof CloudService) {
				services.add(((CloudService) object).getName());
			}
		}
		return services;
	}

}
