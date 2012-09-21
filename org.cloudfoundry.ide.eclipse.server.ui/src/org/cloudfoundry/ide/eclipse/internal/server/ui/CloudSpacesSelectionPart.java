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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudSpaceDescriptor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

public class CloudSpacesSelectionPart {

	private static final String DEFAULT_DESCRIPTION = "Selected an organization and space";

	private TableViewer orgsTable;

	private TableViewer spacesTable;

	private CloudSpaceDescriptor spacesDescriptor;

	private CloudSpace selectedSpace;

	private final CloudFoundryServer cloudServer;

	protected enum CloudEntityType {
		ORG, SPACE
	}

	public CloudSpacesSelectionPart(CloudSpaceDescriptor spacesDescriptor, CloudFoundryServer cloudServer,
			WizardPage wizardPage) {
		this.spacesDescriptor = spacesDescriptor;
		this.cloudServer = cloudServer;

		String serverTypeId = cloudServer.getServer().getServerType().getId();

		wizardPage.setTitle("Organizations and Spaces");
		wizardPage.setDescription(DEFAULT_DESCRIPTION);
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(serverTypeId);
		if (banner != null) {
			wizardPage.setImageDescriptor(banner);
		}
	}

	public CloudSpacesSelectionPart(CloudSpaceDescriptor spacesDescriptor, CloudFoundryServer cloudServer,
			IWizardHandle wizardHandle) {
		this.spacesDescriptor = spacesDescriptor;
		this.cloudServer = cloudServer;

		String serverTypeId = cloudServer.getServer().getServerType().getId();

		wizardHandle.setTitle("Organizations and Spaces");
		wizardHandle.setDescription(DEFAULT_DESCRIPTION);
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(serverTypeId);
		if (banner != null) {
			wizardHandle.setImageDescriptor(banner);
		}
	}

	public Composite createComposite(Composite parent) {
		Composite tableArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(true).applyTo(tableArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableArea);

		// TODO: ADD scrolling
		Composite orgTableComposite = new Composite(tableArea, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(orgTableComposite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(orgTableComposite);

		Label orgLabel = new Label(orgTableComposite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(orgLabel);
		orgLabel.setText("Organizations:");

		Table orgTable = new Table(orgTableComposite, SWT.BORDER | SWT.SINGLE);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(orgTable);

		orgsTable = new TableViewer(orgTable);

		orgsTable.setContentProvider(new TableContentProvider());
		orgsTable.setLabelProvider(new SpacesLabelProvider());
		orgsTable.setSorter(new SpacesSorter());

		orgsTable.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				refresh(CloudEntityType.ORG);
			}
		});

		Composite spaceTableComposite = new Composite(tableArea, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(spaceTableComposite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(spaceTableComposite);

		Label label = new Label(spaceTableComposite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(label);
		label.setText("Spaces:");
		Table spaceTable = new Table(spaceTableComposite, SWT.BORDER | SWT.SINGLE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(spaceTable);
		spacesTable = new TableViewer(spaceTable);

		spacesTable.setContentProvider(new TableContentProvider());
		spacesTable.setLabelProvider(new SpacesLabelProvider());
		spacesTable.setSorter(new SpacesSorter());

		spacesTable.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				refresh(CloudEntityType.SPACE);
			}
		});

		setInput();
		return tableArea;
	}

	protected void setInput() {
		if (spacesDescriptor != null && orgsTable != null && spacesTable != null) {
			List<CloudOrganization> orgInput = spacesDescriptor.getOrgs();
			if (orgInput != null && orgInput.size() > 0) {
				orgsTable.setInput(orgInput);
				selectedSpace = spacesDescriptor.getDefaultCloudSpace();
				if (selectedSpace != null) {
					IStructuredSelection selection = new StructuredSelection(selectedSpace.getOrganization());
					orgsTable.setSelection(selection);
					setSpaceSelection(selectedSpace);
					selection = new StructuredSelection(selectedSpace);
					spacesTable.setSelection(selection);
				}
				refreshUI();
			}
		}
	}

	public void setInput(CloudSpaceDescriptor spacesDescriptor) {
		this.spacesDescriptor = spacesDescriptor;
		setInput();
	}

	protected void setSpaceSelection(CloudSpace selectedSpace) {
		this.selectedSpace = selectedSpace;
		cloudServer.setSpace(selectedSpace);
	}

	public boolean isComplete() {
		return getSpace() != null;
	}

	public CloudSpace getSpace() {
		return selectedSpace;
	}

	protected void refresh(CloudEntityType entityType) {
		if (orgsTable != null && spacesTable != null && entityType != null) {
			CloudSpace selCloudSpace = null;
			switch (entityType) {
			case ORG:
				ISelection orgSel = orgsTable.getSelection();
				if (orgSel instanceof IStructuredSelection) {
					CloudOrganization selectedOrg = (CloudOrganization) ((IStructuredSelection) orgSel)
							.getFirstElement();
					if (selectedOrg != null) {
						List<CloudSpace> selSpaces = spacesDescriptor.getOrgSpaces(selectedOrg.getName());
						if (selSpaces != null && selSpaces.size() > 0) {
							spacesTable.setInput(selSpaces);
							selCloudSpace = selSpaces.get(0);
							IStructuredSelection selection = new StructuredSelection(selectedSpace);
							spacesTable.setSelection(selection);
						}
					}
				}
				break;
			case SPACE:
				ISelection spaceSel = spacesTable.getSelection();
				if (spaceSel instanceof IStructuredSelection) {
					selCloudSpace = (CloudSpace) ((IStructuredSelection) spaceSel).getFirstElement();
				}
				break;
			}

			if (selCloudSpace != null) {
				setSpaceSelection(selCloudSpace);
			}
		}
	}

	protected void refreshUI() {
		if (orgsTable != null) {
			orgsTable.refresh(true);
		}
		if (spacesTable != null) {
			spacesTable.refresh(true);
		}
	}

	static class SpacesSorter extends ViewerSorter {

		public SpacesSorter() {

		}

		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof CloudEntity && e1 instanceof CloudEntity) {
				String name1 = ((CloudEntity) e1).getName();
				String name2 = ((CloudEntity) e2).getName();
				return name1.compareTo(name2);
			}

			return super.compare(viewer, e1, e2);
		}

	}

	static class TableContentProvider implements ITreeContentProvider {
		private Object[] elements;

		public TableContentProvider() {
		}

		public void dispose() {
		}

		public Object[] getChildren(Object parentElement) {
			return null;
		}

		public Object[] getElements(Object inputElement) {
			return elements;
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return false;
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof List<?>) {
				elements = ((List) newInput).toArray();
			}
		}
	}

	static class SpacesLabelProvider extends LabelProvider {

		public SpacesLabelProvider() {

		}

		public String getText(Object element) {
			if (element instanceof CloudEntity) {
				CloudEntity cloudEntity = (CloudEntity) element;
				return cloudEntity.getName();
			}
			return super.getText(element);
		}

	}

}
