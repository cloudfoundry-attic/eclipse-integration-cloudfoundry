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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;

public enum ApplicationInstanceServiceColumn {

	Name(125), Service(100), Vendor(100), Plan(75), Version(75);

	private int width;

	private ApplicationInstanceServiceColumn(int width) {
		this.width = width;
	}

	public int getWidth() {
		return width;
	}

	public static ServiceColumnDescriptor getServiceColumnDescriptor(CloudFoundryServer cloudServer) {
		return new ServiceColumnDescriptor(cloudServer);
	}

	public static class ServiceColumnDescriptor {

		private CloudFoundryServer cloudServer;

		public ServiceColumnDescriptor(CloudFoundryServer cloudServer) {
			this.cloudServer = cloudServer;

		}

		public ApplicationInstanceServiceColumn[] getServiceViewColumn() {
			if (cloudServer.supportsCloudSpaces()) {
				return new ApplicationInstanceServiceColumn[] { Name, Vendor, Plan, Version };
			}
			else {
				return new ApplicationInstanceServiceColumn[] { Name, Service, Vendor, Version };
			}
		}

	}

}
