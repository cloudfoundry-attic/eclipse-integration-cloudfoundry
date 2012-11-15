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

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint.CloudURL;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * @author Terry Denney
 */
public class CloudUrlWidget {

	private Combo urlCombo;

	private final String serverTypeId;

	private final CloudFoundryServer cfServer;

	private int comboIndex;

	public CloudUrlWidget(CloudFoundryServer cfServer) {
		this.cfServer = cfServer;
		this.serverTypeId = cfServer.getServer().getServerType().getId();
	}

	public void createControls(final Composite parent) {
		Label urlLabel = new Label(parent, SWT.NONE);
		urlLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		urlLabel.setText("URL:");

		Composite urlComposite = new Composite(parent, SWT.NONE);
		urlComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		GridLayout urlCompositelayout = new GridLayout(2, false);
		urlCompositelayout.marginHeight = 0;
		urlCompositelayout.marginWidth = 0;
		urlComposite.setLayout(urlCompositelayout);

		urlCombo = new Combo(urlComposite, SWT.BORDER | SWT.READ_ONLY);
		urlCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		updateUrlCombo(null);

		urlCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = urlCombo.getSelectionIndex();

				if (index >= 0 && index != comboIndex) {
					CloudURL cloudUrl = CloudUiUtil.getAllUrls(serverTypeId).get(index);
					if (cloudUrl.getUrl().contains("{")) {
						CloudURL newUrl = CloudUiUtil.getWildcardUrl(cloudUrl, CloudUiUtil.getAllUrls(serverTypeId),
								parent.getShell());
						if (newUrl != null) {
							List<CloudURL> userDefinedUrls = CloudUiUtil.getUserDefinedUrls(serverTypeId);
							userDefinedUrls.add(newUrl);
							CloudUiUtil.storeUserDefinedUrls(serverTypeId, userDefinedUrls);
							String newUrlName = newUrl.getName();

							updateUrlCombo(null);
							for (int i = 0; i < urlCombo.getItemCount(); i++) {
								if (urlCombo.getItem(i).startsWith(newUrlName + " - ")) {
									urlCombo.select(i);
									comboIndex = i;
									break;
								}
							}
						}
						else {
							urlCombo.select(comboIndex);
						}
					}
				}
				setUpdatedSelectionInServer();
			}
		});

		final Button manageUrlButton = new Button(urlComposite, SWT.PUSH);
		manageUrlButton.setText("Manage Cloud...");
		manageUrlButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		manageUrlButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ManageCloudDialog dialog = new ManageCloudDialog(manageUrlButton.getShell(), serverTypeId);
				if (dialog.open() == Dialog.OK) {
					CloudURL lastAddedEditedURL = dialog.getLastAddedOrEditedURL();
					updateUrlCombo(lastAddedEditedURL);
					setUpdatedSelectionInServer();
				}
			}
		});
	}

	public String getURLSelection() {
		if (urlCombo != null) {
			int index = urlCombo.getSelectionIndex();
			return index < 0 ? null : urlCombo.getItem(index);
		}
		return null;
	}
	
	protected String getComboURLDisplay(CloudURL url) {
		return url.getName() + " - " + url.getUrl();
	}

	protected void updateUrlCombo(CloudURL lastAddedEditedUrl) {
		String newSelection = null;
		String oldSelection = null;

		// First grab the old selection before setting the new list of URLs
		if (urlCombo.getSelectionIndex() >= 0) {
			oldSelection = urlCombo.getItem(urlCombo.getSelectionIndex());
		}
		else {
			if (cfServer != null && cfServer.getUrl() != null) {
				oldSelection = cfServer.getUrl();
			}
		}

		// Get updated list of URLs
		List<CloudURL> cloudUrls = CloudUiUtil.getAllUrls(serverTypeId);
		String[] updatedUrls = new String[cloudUrls.size()];

		// If there is a last edited URL, set that as the selection in the combo
		if (lastAddedEditedUrl != null) {
			newSelection = getComboURLDisplay(lastAddedEditedUrl);
		}

		int selectionIndex = -1;

		// Get all the updated URLs, and also check if the last added url is
		// among them.
		// If so, find it's index to select it in the combo
		for (int i = 0; i < cloudUrls.size(); i++) {
			updatedUrls[i] = getComboURLDisplay(cloudUrls.get(i));
			if (newSelection != null && updatedUrls[i].equals(newSelection)) {
				selectionIndex = i;
			}
		}

		// Otherwise, if no last added url is specified, see if the old
		// selection is still available in the
		// list of updated URLs. Find the first one that matches.
		if ((newSelection == null || selectionIndex < 0) && oldSelection != null) {
			for (int i = 0; i < updatedUrls.length; i++) {
				if (updatedUrls[i].contains(oldSelection)) {
					selectionIndex = i;
					break;
				}
			}
		}

		if (selectionIndex < 0 && cloudUrls.size() > 0) {
			selectionIndex = 0;
		}

		urlCombo.setItems(updatedUrls);

		if (selectionIndex < 0) {
			urlCombo.deselectAll();
		}
		else {
			urlCombo.select(selectionIndex);
		}

		comboIndex = selectionIndex;
	}

	/**
	 * This gets invoked any time there is a URL selection change. It sets the
	 * newly selected URL in the server, if selected URL is not null.
	 */
	protected void setUpdatedSelectionInServer() {

		String url = getURLSelection();
		if (url != null) {
			url = CloudUiUtil.getUrlFromDisplayText(url);

			cfServer.setUrl(url);
		}
	}

	public Combo getUrlCombo() {
		return urlCombo;
	}
}
