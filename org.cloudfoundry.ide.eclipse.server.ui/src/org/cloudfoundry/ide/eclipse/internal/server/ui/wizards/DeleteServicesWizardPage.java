/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;



/**
 * @author Terry Denney
 */
public class DeleteServicesWizardPage extends WizardPage {

	private final List<String> services;

	private CheckboxTableViewer viewer;
	
	private Object[] checkedElements;

	public DeleteServicesWizardPage(CloudFoundryServer cloudServer, List<String> services) {
		super("service");
		this.services = services;
		setTitle("Delete Services");
		setDescription("Select which services associated with the application you would like to be deleted.");
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
	}
	
	@Override
	public boolean isPageComplete() {
//		return checkedElements != null && checkedElements.length > 0;
		return true;
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		
		Table table = new Table(container, SWT.CHECK | SWT.BORDER);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		viewer = new CheckboxTableViewer(table);
		
		viewer.addCheckStateListener(new ICheckStateListener() {
			
			public void checkStateChanged(CheckStateChangedEvent event) {
				checkedElements = viewer.getCheckedElements();
				getWizard().getContainer().updateButtons();
			}
		});
		
		LabelProvider labelProvider = new LabelProvider();
		ITreeContentProvider contentProvider = new ITreeContentProvider() {
			
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				// ignore
			}
			
			public void dispose() {
				// ignore
			}
			
			public Object[] getElements(Object inputElement) {
				return services.toArray();
			}
			
			public boolean hasChildren(Object element) {
				return false;
			}
			
			public Object getParent(Object element) {
				return null;
			}
			
			public Object[] getChildren(Object parentElement) {
				return null;
			}
		};
		
		viewer.setLabelProvider(labelProvider);
		viewer.setContentProvider(contentProvider);
		viewer.setInput(services);
		
		setControl(container);
		
		getWizard().getContainer().updateButtons();
	}

	public List<String> getSelectedServices() {
		List<String> result = new ArrayList<String>();
		if (checkedElements != null) {
			for(Object checkedElement: checkedElements) {
				result.add((String) checkedElement);
			}
		}
		return result;
	}

}
