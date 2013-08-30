/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint.CloudServerURL;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


/**
 * @author Terry Denney
 */
public class CloudUrlDialog extends Dialog {

	private String name;
	
	private String url;

	private Label messageLabel;

	private final List<CloudServerURL> allCloudUrls;
	
	public CloudUrlDialog(Shell parentShell, List<CloudServerURL> allCloudUrls) {
		super(parentShell);
		this.allCloudUrls = allCloudUrls;
	}
	
	public CloudUrlDialog(Shell parentShell, String name, String url, List<CloudServerURL> allCloudUrls) {
		super(parentShell);
		this.name = name;
		this.url = url;
		this.allCloudUrls = allCloudUrls;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText("Cloud URL");
		
		Composite composite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).hint(400, SWT.DEFAULT).applyTo(composite);
		GridLayoutFactory.fillDefaults().margins(10, 10).numColumns(2).applyTo(composite);
		
		messageLabel = new Label(composite, SWT.NONE);
		messageLabel.setText("Enter cloud URL name");
		GridDataFactory.fillDefaults().span(2, 1).applyTo(messageLabel);
		
		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText("Name: ");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(nameLabel);
		
		final Text nameText = new Text(composite, SWT.BORDER);
		nameText.setEditable(true);
		if (name != null) {
			nameText.setText(name);
		}
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(nameText);
		nameText.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				name = nameText.getText();
				update();
			}
		});
		
		Label urlLabel = new Label(composite, SWT.NONE);
		urlLabel.setText("URL: ");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(urlLabel);
		
		final Text urlText = new Text(composite, SWT.BORDER);
		urlText.setEditable(true);
		if (url != null) {
			urlText.setText(url);
		}
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(urlText);
		urlText.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				url = urlText.getText();
				update();
			}
		});
		
		return super.createDialogArea(parent);
	}
	
	private void update() {
		boolean canFinish = false;
		
		if (name == null || name.length() == 0 ) {
			messageLabel.setText("Enter a cloud URL name.");
		} else if (url == null || url.length() == 0) {
			messageLabel.setText("Enter a cloud URL.");
		} else {
			canFinish = true;
			
//			List<CloudURL> cloudUrls = CloudUiUtil.getAllUrls(serverTypeId);
			for(CloudServerURL cloudUrl: allCloudUrls) {
				if (! cloudUrl.getUrl().contains("{")) {
					if (cloudUrl.getName().equals(name)) {
						canFinish = false;
						messageLabel.setText("A URL with name " + name + " already exists. Enter a different url name.");
					}
				}
			}
			
			if (canFinish) {
				try {
					URL urlObject = new URL(url);
					String host = urlObject.getHost();
					if (host == null || host.length() == 0) {
						canFinish = false;
						messageLabel.setText("Enter a valid URL.");
					}
				} catch (MalformedURLException e) {
					messageLabel.setText("Enter a valid URL.");
					canFinish = false;
				} catch (CloudFoundryException e) {
					messageLabel.setText("Enter a valid cloud controller URL.");
					canFinish = false;
				}
				
				if (canFinish) {
					messageLabel.setText("Create a new cloud URL.");
				}
			}
		}		
		getButton(IDialogConstants.OK_ID).setEnabled(canFinish);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		update();
	}
	
	public String getName() {
		return name;
	}
	
	public String getUrl() {
		return url;
	}
	
	@Override
	protected void okPressed() {
		final boolean[] shouldProceed = new boolean[] {false};
		
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			
			public void run() {
				try {
					CloudFoundryPlugin.getDefault().getCloudFoundryClientFactory().getCloudFoundryOperations(url).getCloudInfo();
					shouldProceed[0] = true;
				}
				catch (Exception e) {
					shouldProceed[0] = MessageDialog.openQuestion(getParentShell(), "Invalid Cloud URL", "Connection to " + url + " failed. Would you like to keep the URL anyways?");
				}
			}
		});

		if (shouldProceed[0]) {
			super.okPressed();
		}
	}

}
