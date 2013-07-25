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


public enum ServiceViewColumn {
	Name(150), Version(100), Vendor(100), Tunnel(80), Plan(50), Provider(100);
	private int width;

	private ServiceViewColumn(int width) {
		this.width = width;
	}

	public int getWidth() {
		return width;
	}

	public static ServiceViewColumnDescriptor getServiceViewColumnDescriptor() {
		return new ServiceViewColumnDescriptor();
	}

	public static class ServiceViewColumnDescriptor {

		public ServiceViewColumnDescriptor() {
		}

		public ServiceViewColumn[] getServiceViewColumn() {
			return new ServiceViewColumn[] { Name, Vendor, Provider, Version, Plan, Tunnel };

		}

	}

}
