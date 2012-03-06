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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint.CloudURL;
import org.eclipse.jface.dialogs.Dialog;
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
public class TargetURLDialog extends Dialog {
	
	private final CloudURL cloudUrl;
	
	private final String wildcard;
		
	private String value;
	
	private String name;
	
	private String url;

	private Text wildcardText;

	private Text nameText;

	private final List<CloudURL> allCloudUrls;

	public TargetURLDialog(Shell parentShell, CloudURL cloudUrl, String wildcard, List<CloudURL> allCloudUrls) {
		super(parentShell);
		this.cloudUrl = cloudUrl;
		this.wildcard = wildcard;
		this.allCloudUrls = allCloudUrls;
		this.url = cloudUrl.getUrl();
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText("Create " + cloudUrl.getName() + " Target");
		
		Composite control = (Composite) super.createDialogArea(parent);
		
		Composite composite = new Composite(control, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(composite);
		GridLayoutFactory.fillDefaults().applyTo(composite);
		
		Label wildcardLabel = new Label(composite, SWT.NONE);
		wildcardLabel.setText("Enter the value to replace {" + wildcard + "}:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).hint(300, SWT.DEFAULT).applyTo(wildcardLabel);
		
		wildcardText = new Text(composite, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(wildcardText);
		wildcardText.setEnabled(true);
		wildcardText.setText(wildcard);
		value = wildcard;
		wildcardText.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				if (nameText.getText().equals(cloudUrl.getName() + " (" + value + ")")) {
					nameText.setText(cloudUrl.getName() + " (" + wildcardText.getText() + ")");
				}
				value = wildcardText.getText();
			}
		});
		
		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText("Enter the name for this Cloud URL:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).indent(0, 5).applyTo(nameLabel);
		
		nameText = new Text(composite, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(nameText);
		nameText.setEnabled(true);
		name = cloudUrl.getName() + " (" + value + ")";
		nameText.setText(name);
		nameText.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				name = nameText.getText();
			}
		});
		
		return control;
	}
	
	@Override
	protected void okPressed() {
//		List<CloudURL> allUrls = CloudUiUtil.getAllUrls(serverTypeId);
		for(CloudURL url: allCloudUrls) {
			if (url.getName().equals(name)) {
				MessageDialog.openError(getParentShell(), "Duplicate Cloud URL Name", "There is already a cloud URL with the name " + name + ". Please enter a new name.");
				return;
			}
		}
		
		final boolean[] shouldProceed = new boolean[] {false};
		
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			
			public void run() {
				url = replaceWildcard(cloudUrl.getUrl(), wildcard, value);
				try {
					CloudFoundryPlugin.getDefault().getCloudFoundryClientFactory().getCloudFoundryClient(url).getCloudInfo();
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
	
	private static String replaceWildcard(String url, String wildcard, String value) {
		return url.replaceAll("\\{" + wildcard + "\\}", value);
	}
	
}
