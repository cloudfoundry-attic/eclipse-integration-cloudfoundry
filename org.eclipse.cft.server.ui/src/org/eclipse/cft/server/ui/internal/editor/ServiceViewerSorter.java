/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.editor;

import org.cloudfoundry.client.lib.domain.CloudService;
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
		case Version:
			result = service1.getVersion() != null ? service1.getVersion().compareTo(service2.getVersion()) : 0;
			break;
		case Vendor:
			result = service1.getLabel() != null ? service1.getLabel().compareTo(service2.getLabel()) : 0;
			break;
		case Plan:
			result = service1.getPlan() != null ? service1.getPlan().compareTo(service2.getPlan()) : 0;
			break;
		case Provider:
			result = service1.getProvider() != null ? service1.getProvider().compareTo(service2.getProvider()) : 0;
			break;
		}
		return result;
	}
}