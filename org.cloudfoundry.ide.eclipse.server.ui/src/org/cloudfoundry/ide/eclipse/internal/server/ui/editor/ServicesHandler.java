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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.CloudService;
import org.eclipse.jface.viewers.IStructuredSelection;

public class ServicesHandler {

	private List<String> services;

	private final List<CloudService> cloudServices;

	public ServicesHandler(IStructuredSelection selection) {
		Object[] objects = selection.toArray();
		cloudServices = new ArrayList<CloudService>();

		for (Object obj : objects) {
			if (obj instanceof CloudService) {
				cloudServices.add((CloudService) obj);
			}

		}
	}

	public List<String> getServiceNames() {

		if (services == null) {
			services = new ArrayList<String>();

			for (CloudService service : cloudServices) {
				services.add(service.getName());
			}
		}

		return services;
	}

	public String toString() {
		StringBuilder serviceNames = new StringBuilder();
		for (String service : getServiceNames()) {
			if (serviceNames.length() > 0) {
				serviceNames.append(", ");
			}
			serviceNames.append(service);
		}
		return serviceNames.toString();
	}
}
