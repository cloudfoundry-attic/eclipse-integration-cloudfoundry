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
		
		updateUrlCombo();
				
		urlCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = urlCombo.getSelectionIndex();
				
				if (index >= 0 && index != comboIndex) {
					CloudURL cloudUrl = CloudUiUtil.getAllUrls(serverTypeId).get(index);
					if (cloudUrl.getUrl().contains("{")) {
						CloudURL newUrl = CloudUiUtil.getWildcardUrl(cloudUrl, CloudUiUtil.getAllUrls(serverTypeId), parent.getShell());
						if (newUrl != null) {
							List<CloudURL> userDefinedUrls = CloudUiUtil.getUserDefinedUrls(serverTypeId);
							userDefinedUrls.add(newUrl);
							CloudUiUtil.storeUserDefinedUrls(serverTypeId, userDefinedUrls);
							String newUrlName = newUrl.getName();
							
							updateUrlCombo();
							for(int i=0; i<urlCombo.getItemCount(); i++) {
								if (urlCombo.getItem(i).startsWith(newUrlName + " - ")) {
									urlCombo.select(i);
									comboIndex = i;
									break;
								}
							}
						} else {
							urlCombo.select(comboIndex);
						}
					}
				}
			}
		});
		
		final Button manageUrlButton = new Button(urlComposite, SWT.PUSH);
		manageUrlButton.setText("Manage Cloud...");
		manageUrlButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		manageUrlButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int result = new ManageCloudDialog(manageUrlButton.getShell(), serverTypeId).open();
				if (result == Dialog.OK) {
					updateUrlCombo();
				}
			}
		});
	}
	
	private void updateUrlCombo() {
		String url = null;
		if (urlCombo.getSelectionIndex() >= 0) {
			url = urlCombo.getItem(urlCombo.getSelectionIndex());
		} else {
			if (cfServer != null && cfServer.getUrl() != null) {
				url = cfServer.getUrl();
			}
		}
		
		List<CloudURL> cloudUrls = CloudUiUtil.getAllUrls(serverTypeId);
		String[] urls = new String[cloudUrls.size()];
		
		int index = -1;
		for(int i=0; i<cloudUrls.size(); i++) {
			String currUrl = cloudUrls.get(i).getUrl();
			urls[i] = cloudUrls.get(i).getName() + " - " + currUrl;
			if (url != null && urls[i].contains(url)) {
				index = i;
			}
		}
		
		if (index < 0 && cloudUrls.size() > 0) {
			index = 0;
		}
		
		urlCombo.setItems(urls);
		
		if (index < 0) {
			urlCombo.deselectAll();
		} else {
			urlCombo.select(index);
		}
		
		comboIndex = index;
	}

	public Combo getUrlCombo() {
		return urlCombo;
	}
}
