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
package org.cloudfoundry.ide.eclipse.server.ui.internal;

import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryBrandingExtensionPoint.CloudServerURL;
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
	
	private final CloudServerURL cloudUrl;
	
	private final String wildcard;
		
	private String value;
	
	private String name;
	
	private String url;

	private Text wildcardText;

	private Text nameText;

	private final List<CloudServerURL> allCloudUrls;

	public TargetURLDialog(Shell parentShell, CloudServerURL cloudUrl, String wildcard, List<CloudServerURL> allCloudUrls) {
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
		for(CloudServerURL url: allCloudUrls) {
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
					CloudFoundryPlugin.getCloudFoundryClientFactory().getCloudFoundryOperations(url).getCloudInfo();
					shouldProceed[0] = true;
				}
				catch (Exception e) {
					shouldProceed[0] = MessageDialog.openQuestion(getParentShell(), "Invalid Cloud URL", "Connection to " + url + " failed. Would you like to keep the URL anyway?");
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
