/*******************************************************************************
 * Copyright (c) 2012, 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class CloudFoundryURLsWizardPage extends WizardPage {

	private Button addButton;

	private Button removeButton;

	private Button editButton;

	private Button shouldRepublishButton;

	private List<String> urls;

	private TableViewer viewer;

	public CloudFoundryURLsWizardPage(CloudFoundryServer cloudServer, List<String> existingURIs,
			ApplicationModule appModule) {
		super("Mapped URIs");

		urls = new ArrayList<String>();
		if (existingURIs != null) {
			urls.addAll(existingURIs);
		}

		setTitle("Mapped URIs Configuration");
		setDescription("Finish to modify the mapped URIs.");
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(10, LayoutConstants.getSpacing().y).applyTo(composite);

		Label label = new Label(composite, SWT.NONE);
		label.setText("Application URIs:");
		GridDataFactory.fillDefaults().span(2, 1).applyTo(label);

		Table table = new Table(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		viewer = new TableViewer(table);
		viewer.setContentProvider(new URIsContentProvider());
		viewer.setInput(urls);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				removeButton.setEnabled(!selection.isEmpty());
				editButton.setEnabled(!selection.isEmpty() && selection instanceof IStructuredSelection
						&& ((IStructuredSelection) selection).toArray().length == 1);
			}
		});

		Composite buttonComposite = new Composite(composite, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(0, 0).applyTo(buttonComposite);
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.FILL).applyTo(buttonComposite);

		addButton = new Button(buttonComposite, SWT.PUSH);
		addButton.setText("Add...");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(false, false).applyTo(addButton);
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				InputDialog dialog = new InputDialog(addButton.getShell(), "Add URL Mapping", "URL:", "",
						new IInputValidator() {

							public String isValid(String newText) {
								if (newText == null || newText.length() == 0) {
									return "URL cannot be empty";
								}
								return null;
							}
						});
				if (dialog.open() == Dialog.OK) {
					String newURI = dialog.getValue();
					urls.add(newURI);
					update();
				}
			}
		});

		editButton = new Button(buttonComposite, SWT.PUSH);
		editButton.setText("Edit...");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(false, false).applyTo(editButton);
		editButton.setEnabled(false);
		editButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				String url = (String) selection.getFirstElement();

				InputDialog dialog = new InputDialog(addButton.getShell(), "Unpdate URL Mapping", "URL:", url,
						new IInputValidator() {

							public String isValid(String newText) {
								if (newText == null || newText.length() == 0) {
									return "URL cannot be empty";
								}
								return null;
							}
						});
				if (dialog.open() == Dialog.OK) {
					String newURI = dialog.getValue();
					urls.remove(url);
					urls.add(newURI);
					update();
				}
			}
		});

		removeButton = new Button(buttonComposite, SWT.PUSH);
		removeButton.setText("Remove");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(false, false).applyTo(addButton);
		removeButton.setEnabled(false);
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				Object[] selectedURLs = selection.toArray();
				for (Object selectedURL : selectedURLs) {
					urls.remove(selectedURL);
					update();
				}
			}
		});

		boolean isPublished = ((CloudFoundryURLsWizard) getWizard()).isPublished();

		if (!isPublished) {
			shouldRepublishButton = new Button(composite, SWT.CHECK);
			shouldRepublishButton.setText("Republish");
			GridDataFactory.fillDefaults().span(2, 1).applyTo(shouldRepublishButton);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(false, false).applyTo(shouldRepublishButton);
			shouldRepublishButton.setEnabled(false);
			shouldRepublishButton.setSelection(true);
		}

		Dialog.applyDialogFont(composite);
		setControl(composite);
	}

	private void update() {
		viewer.refresh(true);
		getWizard().getContainer().updateButtons();
	}

	public boolean isPageComplete() {
		return !((CloudFoundryURLsWizard) getWizard()).requiresURL() || !urls.isEmpty();
	}

	public List<String> getURLs() {
		return urls;
	}

	public boolean shouldRepublish() {
		return shouldRepublishButton != null && !shouldRepublishButton.isDisposed()
				&& shouldRepublishButton.getSelection();
	}

	private class URIsContentProvider implements ITreeContentProvider {

		public void dispose() {
		}

		public Object[] getChildren(Object parentElement) {
			return null;
		}

		public Object[] getElements(Object inputElement) {
			return urls.toArray();
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return false;
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

}
