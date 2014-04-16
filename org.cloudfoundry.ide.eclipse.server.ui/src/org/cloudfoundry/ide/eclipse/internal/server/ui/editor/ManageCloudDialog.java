/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint.CloudServerURL;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudUrlWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;

/**
 * @author Terry Denney
 */
public class ManageCloudDialog extends Dialog {

	private final String serverTypeId;

	private List<CloudServerURL> cloudUrls;

	private Set<String> urlsToDelete;

	private Set<CloudFoundryServer> serversToDelete;

	private CloudServerURL lastAddedEditedURL;

	protected ManageCloudDialog(Shell parentShell, String serverTypeId) {
		super(parentShell);
		this.serverTypeId = serverTypeId;
		serversToDelete = new HashSet<CloudFoundryServer>();
		urlsToDelete = new HashSet<String>();
	}

	private TableViewer createTableViewer(Composite parent, String[] columnNames, int[] columnWeights) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).hint(600, 200).applyTo(container);
		TableColumnLayout layout = new TableColumnLayout();
		container.setLayout(layout);

		Table table = new Table(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		table.setHeaderVisible(true);

		for (int i = 0; i < columnNames.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(columnNames[i]);
			layout.setColumnData(column, new ColumnWeightData(columnWeights[i]));
		}

		TableViewer tableViewer = new TableViewer(table);
		tableViewer.setColumnProperties(columnNames);
		return tableViewer;
	}

	/**
	 * Prompts a user for a cloud URL. If successfully prompted and user enters
	 * a cloud URL, the latter is returned. Otherwise, null is returned.
	 * @param serverID
	 * @param shell
	 * @param allURLs
	 * @param existingURL
	 * @param existingName
	 * @return Cloud URL if successfully prompted and entered by user. Null
	 * otherwise
	 */
	protected CloudServerURL promptForCloudURL(String serverID, Shell shell, List<CloudServerURL> allURLs,
			String existingURL, String existingName) {
		CloudUrlWizard wizard = new CloudUrlWizard(serverID, allURLs, existingURL, existingName);
		WizardDialog dialog = new WizardDialog(shell, wizard);
		if (dialog.open() == Dialog.OK) {
			return wizard.getCloudUrl();
		}
		return null;

	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText("Manage Cloud URLs");
		Composite composite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);
		GridLayoutFactory.fillDefaults().margins(10, 10).numColumns(2).equalWidth(false).applyTo(composite);

		final TableViewer viewer = createTableViewer(composite, new String[] { "Server Type", "URL" }, new int[] { 35,
				55 });

		viewer.setContentProvider(new IStructuredContentProvider() {

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

			public void dispose() {
			}

			public Object[] getElements(Object inputElement) {
				Collections.sort(cloudUrls, new Comparator<CloudServerURL>() {
					public int compare(CloudServerURL o1, CloudServerURL o2) {
						return o1.getName().compareTo(o2.getName());
					}
				});
				return cloudUrls.toArray();
			}
		});
		cloudUrls = CloudUiUtil.getAllUrls(serverTypeId);
		viewer.setInput(cloudUrls.toArray());

		viewer.setLabelProvider(new ITableLabelProvider() {

			public void removeListener(ILabelProviderListener listener) {
			}

			public boolean isLabelProperty(Object element, String property) {
				return false;
			}

			public void dispose() {
			}

			public void addListener(ILabelProviderListener listener) {
			}

			public String getColumnText(Object element, int columnIndex) {
				if (element instanceof CloudServerURL) {
					CloudServerURL cloudUrl = (CloudServerURL) element;
					if (columnIndex == 0) {
						return cloudUrl.getName();
					}
					else if (columnIndex == 1) {
						return cloudUrl.getUrl();
					}
				}

				return null;
			}

			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}
		});

		Composite buttonComposite = new Composite(composite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(buttonComposite);
		GridLayoutFactory.fillDefaults().margins(0, 0).applyTo(buttonComposite);

		final Button addButton = new Button(buttonComposite, SWT.PUSH);
		GridDataFactory.fillDefaults().applyTo(addButton);
		addButton.setText("Add...");
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CloudServerURL cloudURL = promptForCloudURL(serverTypeId, e.display.getActiveShell(), cloudUrls, null,
						null);
				if (cloudURL != null) {
					addURL(cloudURL);
					viewer.refresh(true);
				}
			}
		});

		final Button editButton = new Button(buttonComposite, SWT.PUSH);
		GridDataFactory.fillDefaults().applyTo(editButton);
		editButton.setText("Edit...");
		editButton.setEnabled(false);
		editButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ISelection selection = viewer.getSelection();
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection sSelection = (IStructuredSelection) selection;
					Object element = sSelection.getFirstElement();
					if (element instanceof CloudServerURL) {
						CloudServerURL cloudUrl = (CloudServerURL) element;

						if (cloudUrl.getUserDefined()) {
							cloudUrls.remove(cloudUrl);
							CloudServerURL newUrl = promptForCloudURL(serverTypeId, e.display.getActiveShell(),
									cloudUrls, cloudUrl.getUrl(), cloudUrl.getName());
							if (newUrl != null) {

								if (cloudUrl.getUrl().equals(newUrl.getUrl()) || canUpdateUrl(cloudUrl, newUrl)) {
									addURL(newUrl);
								}
								else {
									addURL(cloudUrl);
								}
							}
							else {
								addURL(cloudUrl);
							}
						}
						else {
							CloudServerURL url = CloudUiUtil.getWildcardUrl(cloudUrl, cloudUrls, getShell());
							if (url != null) {
								addURL(url);
							}
						}
						viewer.refresh(true);
					}
				}
			}
		});

		final Button removeButton = new Button(buttonComposite, SWT.PUSH);
		GridDataFactory.fillDefaults().applyTo(removeButton);
		removeButton.setText("Remove");
		removeButton.setEnabled(false);
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ISelection selection = viewer.getSelection();
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection sSelection = (IStructuredSelection) selection;
					Object[] selectedItems = sSelection.toArray();
					for (Object selectedItem : selectedItems) {
						if (selectedItem instanceof CloudServerURL) {
							CloudServerURL cloudUrl = (CloudServerURL) selectedItem;
							removeCloudUrl(cloudUrl);
						}
					}
				}
				viewer.refresh(true);
			}

		});

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof IStructuredSelection) {
					boolean editEnabled = true;
					boolean removeEnabled = true;

					IStructuredSelection sSelection = (IStructuredSelection) selection;
					Object[] selectedItems = sSelection.toArray();
					for (Object selectedItem : selectedItems) {
						if (selectedItem instanceof CloudServerURL) {
							CloudServerURL cloudUrl = (CloudServerURL) selectedItem;
							if (!cloudUrl.getUserDefined()) {
								String url = cloudUrl.getUrl();
								if (!url.contains("{")) {
									editEnabled = false;
								}

								removeEnabled = false;
							}
						}
					}

					editButton.setEnabled(selectedItems.length == 1 && editEnabled);
					removeButton.setEnabled(selectedItems.length > 0 && removeEnabled);
				}
			}
		});

		return composite;
	}

	protected void addURL(CloudServerURL urlToAdd) {
		if (cloudUrls != null) {
			cloudUrls.add(urlToAdd);
			if (urlsToDelete != null) {
				urlsToDelete.remove(urlToAdd.getUrl());
			}
			lastAddedEditedURL = urlToAdd;
		}
	}

	protected void removeCloudUrl(CloudServerURL cloudUrl) {
		if (cloudUrl != null && cloudUrl.getUserDefined() && canUpdateUrl(cloudUrl, null)) {
			cloudUrls.remove(cloudUrl);
			if (urlsToDelete != null) {
				urlsToDelete.add(cloudUrl.getUrl());
			}
		}
	}

	public CloudServerURL getLastAddedOrEditedURL() {
		return lastAddedEditedURL;
	}

	private boolean canUpdateUrl(CloudServerURL url, CloudServerURL newUrl) {
		IServer[] servers = ServerCore.getServers();
		Set<CloudFoundryServer> matchedServers = new HashSet<CloudFoundryServer>();
		for (IServer server : servers) {
			CloudFoundryServer cfServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
			if (cfServer != null && cfServer.getUrl().equals(url.getUrl())) {
				matchedServers.add(cfServer);
			}
		}

		if (matchedServers.isEmpty()) {
			return true;
		}

		if (newUrl == null) {
			if (MessageDialog.openQuestion(getShell(), "URL used in existing server", "The URL you are "
					+ "removing is used in an existing server. Removing the URL will also remove the server."
					+ " Are you sure you want to continue?")) {
				for (CloudFoundryServer matchedServer : matchedServers) {
					serversToDelete.add(matchedServer);
				}
				return true;
			}
		}
		else {
			EditUrlConfirmationDialog dialog = new EditUrlConfirmationDialog(getShell());
			int answer = dialog.open();
			if (answer == 0) {
				if (dialog.getAction() == EditUrlConfirmationDialog.Action.REMOVE_SERVER) {
					for (CloudFoundryServer matchedServer : matchedServers) {
						serversToDelete.add(matchedServer);
					}
					return true;
				}
				else {
					addURL(newUrl);
				}
			}
		}

		return false;
	}

	private static class EditUrlConfirmationDialog extends MessageDialog {

		public enum Action {
			REMOVE_SERVER, ADD_URL
		};

		private Action action;

		public EditUrlConfirmationDialog(Shell parentShell) {
			super(parentShell, "URL used in existing server", null,
					"The URL you are modifying is used in an existing server. "
							+ "Choose the desired action to complete editing:", MessageDialog.QUESTION, new String[] {
							"OK", "Cancel" }, 0);
		}

		@Override
		protected Control createMessageArea(Composite composite) {
			Control control = super.createMessageArea(composite);

			new Label(composite, SWT.NONE);

			final Button removeServerButton = new Button(composite, SWT.RADIO);
			removeServerButton.setText("Remove server(s) with old URL");
			removeServerButton.setSelection(true);
			action = Action.REMOVE_SERVER;
			removeServerButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (removeServerButton.getSelection()) {
						action = Action.REMOVE_SERVER;
					}
				}
			});

			new Label(composite, SWT.NONE);

			final Button addUrlButton = new Button(composite, SWT.RADIO);
			addUrlButton.setText("Keep old URL and add modified URL as new URL");
			removeServerButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (addUrlButton.getSelection()) {
						action = Action.ADD_URL;
					}
				}
			});

			return control;
		}

		public Action getAction() {
			return action;
		}

	}

	@Override
	protected void okPressed() {
		CloudUiUtil.storeUserDefinedUrls(serverTypeId, cloudUrls);
		// Servers to delete are servers that were previously created using a
		// URL that has been deleted.
		for (CloudFoundryServer server : serversToDelete) {
			try {
				IServer serverOriginal = server.getServerOriginal();
				serverOriginal.delete();
			}
			catch (CoreException e) {
				CloudFoundryPlugin.getDefault().getLog()
						.log(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, "Unable to delete server", e));
			}
		}

		// Also remove the self-signed settings for cloud URL
		if (urlsToDelete != null) {
			for (String url : urlsToDelete) {
				CloudFoundryServer.setSelfSignedCertificate(false, url);
			}
		}

		super.okPressed();
	}
}
