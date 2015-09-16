/*******************************************************************************
 * Copyright (c) 2014, 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
 *     Steven Hung, IBM - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.cft.server.ui.internal.editor.CloudFoundryViewerSorter;
import org.eclipse.cft.server.ui.internal.editor.ServicesHandler;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Allows Cloud routes to be viewed and edited.
 */
public class ServiceToApplicationsBindingPart extends UIPart {
	private CheckboxTableViewer viewer;
	ServicesHandler servicesHandler;
	private HashMap<String,ApplicationToService> applicationToServiceMapping = new HashMap<String,ApplicationToService>();	
	
	public ServiceToApplicationsBindingPart(ServicesHandler servicesHandler){
		this.servicesHandler = servicesHandler;
	}
	
	public List<ApplicationToService> getApplicationToService(){
		List<ApplicationToService> returnList = new ArrayList<ApplicationToService>();
		Iterator<Map.Entry<String,ApplicationToService>> iterator = applicationToServiceMapping.entrySet().iterator(); 
		while(iterator.hasNext()){
			Map.Entry<String,ApplicationToService> pairs = iterator.next();
			returnList.add(pairs.getValue());
		}
		return returnList;
	}
	
	@Override
	public Control createPart(Composite parent) {
		Composite generalArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(generalArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(generalArea);

		createTableArea(generalArea);

		return generalArea;
	}
	
	protected void createTableArea(Composite parent) {
		Composite tableArea = new Composite(parent, SWT.NONE);
		Label l = new Label(tableArea, SWT.NONE);
		l.setBackground(parent.getBackground());
		l.setText(Messages.MANAGE_SERVICES_TO_APPLICATIONS_SELECTION_DESCRIPTION);
		
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tableArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableArea);

		final Table table = new Table(tableArea, SWT.BORDER | SWT.CHECK);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		viewer = new CheckboxTableViewer(table);
		
		viewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof Collection) {
					return ((Collection<?>) inputElement).toArray(new Object[0]);
				}
				return null;
			}

			public void dispose() {
				// Do nothing
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				// Do nothing
			}
		});

		viewer.setLabelProvider(new BindServiceToApplicationLabelProvider(viewer));

		// Sort the applications so it is consistent with the application list that shows up in the Applications and Services editor
		viewer.setSorter(new CloudFoundryViewerSorter());
		
		viewer.getTable().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				Widget item = event.item;
				
				if (event.detail == SWT.CHECK && item != null && item instanceof TableItem) {
					TableItem tableItem = (TableItem) item;
					String appName = tableItem.getText();
					
					ApplicationToService curr = applicationToServiceMapping.get(appName);
					
					if (curr != null){
						curr.setBoundToServiceAfter(tableItem.getChecked());
					}
				}
			}
		});
		
		new TableColumn(table, SWT.NONE, 0);
		
		TableResizeHelper resizer = new TableResizeHelper(viewer);
		resizer.enableResizing();
	}	
	
	/*
	 * Populates the table with the application names
	 */
	public void setInput(List<CloudApplication> cloudApplications) {
		if (viewer != null){
			viewer.setInput(cloudApplications);
			
			Table table = viewer.getTable();
			if (table != null){
				// Process the applications
				int len = table.getItemCount();
				String serviceName = servicesHandler.toString();
				for (int i=0;i<len;i++){
					TableItem tableItem = table.getItem(i);
					if (tableItem != null){
						Object objCloudApplication = tableItem.getData();
						if (objCloudApplication instanceof CloudApplication){
							CloudApplication cloudApp = (CloudApplication) objCloudApplication;
							List<String> serviceList = cloudApp.getServices();
							
							tableItem.setChecked(serviceList.contains(serviceName));
							ApplicationToService applicationToService = 
									new ApplicationToService(cloudApp,serviceList.contains(serviceName));

							applicationToServiceMapping.put(cloudApp.getName(), applicationToService);
						}
					}
				}
			}
		}
	}	
	
	/*
	 * Keeps track of whether the application was bound to the selected service and
	 * if it is still bound to the selected service
	 */
	public class ApplicationToService {
		private boolean boundToServiceBefore = false;
		private boolean boundToServiceAfter = false;
		private CloudApplication cloudApplication = null;
		
		public ApplicationToService(CloudApplication cloudApplication, boolean boundToServiceBefore){
			this.cloudApplication = cloudApplication;
			this.boundToServiceBefore = boundToServiceBefore;
			
			// Initialize this variable to be the same as the "before" variable in
			// order to detect if changes had taken place
			this.boundToServiceAfter = boundToServiceBefore;
		}
		
		public void setBoundToServiceAfter(boolean boundToServiceAfter){
			this.boundToServiceAfter = boundToServiceAfter;
		}
		
		public boolean getBoundToServiceBefore(){
			return boundToServiceBefore;
		}
		
		public boolean getBoundToServiceAfter(){
			return boundToServiceAfter;
		}
		
		public CloudApplication getCloudApplication(){
			return cloudApplication;
		}
	}
	
	protected class BindServiceToApplicationLabelProvider extends LabelProvider implements ITableLabelProvider {

		private final TableViewer viewer;

		public BindServiceToApplicationLabelProvider(TableViewer viewer) {
			this.viewer = viewer;
		}

		@Override
		public Image getImage(Object element) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			String result = null;
			TableColumn column = viewer.getTable().getColumn(columnIndex);
			if (column != null) {
				
				CloudApplication app = (CloudApplication) element;
				result = app.getName();
			}
			return result;
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}
}
