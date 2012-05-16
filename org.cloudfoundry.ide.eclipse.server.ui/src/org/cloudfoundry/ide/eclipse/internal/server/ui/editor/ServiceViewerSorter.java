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

import org.cloudfoundry.client.lib.CloudService;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TableColumn;

public class ServiceViewerSorter extends CloudFoundryViewerSorter {
	private final TableViewer tableViewer;

	public ServiceViewerSorter(TableViewer tableViewer) {
		this.tableViewer = tableViewer;
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		TableColumn sortColumn = tableViewer.getTable().getSortColumn();
		if (sortColumn != null) {
			ServiceViewColumn serviceColumn = (ServiceViewColumn) sortColumn.getData();
			int result = 0;
			int sortDirection = tableViewer.getTable().getSortDirection();
			if (serviceColumn != null) {
				if (e1 instanceof CloudService && e2 instanceof CloudService) {
					CloudService service1 = (CloudService) e1;
					CloudService service2 = (CloudService) e2;

					switch (serviceColumn) {
					case Name:
						result = super.compare(tableViewer, e1, e2);
						break;
					default:
						result = compare(service1, service2, serviceColumn);
						break;
					}

				}
			}
			return sortDirection == SWT.UP ? result : -result;
		}

		return super.compare(viewer, e1, e2);
	}

	protected int compare(CloudService service1, CloudService service2, ServiceViewColumn sortColumn) {
		int result = 0;
		switch (sortColumn) {
		case Type:
			result = service1.getType().compareTo(service2.getType());
			break;
		case Vendor:
			result = service1.getVendor().compareTo(service2.getVendor());
			break;
		}
		return result;
	}
}